package zmaster587.advancedRocketry.atmosphere;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import zmaster587.advancedRocketry.api.AreaBlob;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.IAtmosphere;
import zmaster587.advancedRocketry.api.event.AtmosphereEvent;
import zmaster587.advancedRocketry.api.util.IBlobHandler;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.network.PacketAtmSync;
import zmaster587.advancedRocketry.util.AtmosphereBlob;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class AtmosphereHandler {
	public static final DamageSource vacuumDamage = new DamageSource("Vacuum").setDamageBypassesArmor().setDamageIsAbsolute();
	public static final DamageSource lowOxygenDamage = new DamageSource("LowOxygen").setDamageBypassesArmor().setDamageIsAbsolute();
	public static final DamageSource heatDamage = new DamageSource("Heat").setDamageBypassesArmor().setDamageIsAbsolute();
	public static final DamageSource oxygenToxicityDamage = new DamageSource("OxygenToxicity").setDamageBypassesArmor().setDamageIsAbsolute();

	public static long lastSuffocationTime = Integer.MIN_VALUE;
	private static final int MAX_BLOB_RADIUS = ((ARConfiguration.getCurrentConfig().atmosphereHandleBitMask & 1) == 1) ? 256 : ARConfiguration.getCurrentConfig().oxygenVentSize;
	private static HashMap<Integer, AtmosphereHandler> dimensionOxygen = new HashMap<>();
	private static HashMap<EntityPlayer, IAtmosphere> prevAtmosphere = new HashMap<>();

	private HashMap<IBlobHandler,AreaBlob> blobs;
	private int dimId;

	//Stores current Atm on the CLIENT
	public static IAtmosphere currentAtm;
	public static int currentPressure;

	/**
	 * Registers the Atmosphere handler for the dimension given
	 * @param dimId the dimension id to register the dimension for
	 */
	public static void registerWorld(int dimId) {

		//If O2 is allowed and
		DimensionProperties dimProp = DimensionManager.getInstance().getDimensionProperties(dimId);
		if(ARConfiguration.getCurrentConfig().enableOxygen && dimProp.hasSurface() && (ARConfiguration.getCurrentConfig().overrideGCAir || dimId != ARConfiguration.getCurrentConfig().MoonId || dimProp.isNativeDimension)) {
			dimensionOxygen.put(dimId, new AtmosphereHandler(dimId));
			MinecraftForge.EVENT_BUS.register(dimensionOxygen.get(dimId));
		}
	}

	/**
	 * Unregisters the Atmosphere handler for the dimension given
	 * @param dimId the dimension id to register the dimension for
	 */
	public static void unregisterWorld(int dimId) {
		AtmosphereHandler handler = dimensionOxygen.remove(dimId);
		if(ARConfiguration.getCurrentConfig().enableOxygen && handler != null) {

			MinecraftForge.EVENT_BUS.unregister(handler);
			FMLCommonHandler.instance().bus().unregister(handler);
		}
	}

	private AtmosphereHandler(int dimId) {
		this.dimId = dimId;
		blobs = new HashMap<>();
	}

	@SubscribeEvent
	public void onTick(LivingUpdateEvent event) {
		Entity entity = event.getEntity();
		if(!entity.world.isRemote && entity.world.provider.getDimension() == this.dimId) {
			IAtmosphere atmosType = getAtmosphereType(entity);

			if(entity instanceof EntityPlayer && atmosType != prevAtmosphere.get(entity)) {
				PacketHandler.sendToPlayer(new PacketAtmSync(atmosType.getUnlocalizedName(), (int)getAtmospherePressure(entity)), (EntityPlayer)entity);
				prevAtmosphere.put((EntityPlayer)entity, atmosType);
			}

			if(atmosType.canTick() && !(event.getEntityLiving().isInLava() || event.getEntityLiving().isInsideOfMaterial(Material.WATER)) ) {
				AtmosphereEvent event2 = new AtmosphereEvent.AtmosphereTickEvent(entity, atmosType);
				MinecraftForge.EVENT_BUS.post(event2);
				if(!event2.isCanceled() && !atmosType.isImmune(event.getEntity().getClass()))
					atmosType.onTick(event.getEntityLiving());
			}
		}
	}

	@SubscribeEvent
	public void onDeath(LivingDeathEvent event) {
		Entity entity = event.getEntity();
		if(ARConfiguration.getCurrentConfig().enableOxygen) {
			HashedBlockPosition pos = new HashedBlockPosition((int)Math.floor(entity.posX), (int)Math.ceil(entity.posY), (int)Math.floor(entity.posZ));
			for(AreaBlob blob : blobs.values()) {
				if(blob.contains(pos)) {
					blob.removeUUIDFromHandlerList(entity.getUniqueID());
				}

			}
		}
	}

	@SubscribeEvent
	public void onPlayerChangeDim(PlayerChangedDimensionEvent event) {
		prevAtmosphere.remove(event.player);

	}

	@SubscribeEvent
	public void onPlayerLogoutEvent(PlayerLoggedOutEvent event) {
		prevAtmosphere.remove(event.player);
	}

	private void onBlockRemove(HashedBlockPosition pos) {
		List<AreaBlob> blobs = getBlobWithinRadius(pos, MAX_BLOB_RADIUS);
		for(AreaBlob blob : blobs) {
			//Make sure that a block can actually be attached to the blob
			for(EnumFacing dir : EnumFacing.VALUES)
				if(blob.contains(pos.getPositionAtOffset(dir))) {
					blob.addBlock(pos, blobs);
					break;
				}
		}
	}

	/**
	 * @return true if the dimension has an AtmosphereHandler Object associated with it
	 */
	public static boolean hasAtmosphereHandler(int dimId) {
		return dimensionOxygen.containsKey(dimId);
	}

	//Called from World.setBlockMetaDataWithNotify
	/*public static void onBlockMetaChange(World world, int x , int y, int z) {
		if(Configuration.enableOxygen && !world.isRemote && world.getChunkFromBlockCoords(new BlockPos(x, y, z)).isLoaded()) {
			AtmosphereHandler handler = getOxygenHandler(world.provider.getDimension());
			HashedBlockPosition pos = new HashedBlockPosition(x, y, z);


			if(handler == null)
				return; //WTF

			for(AreaBlob blob : handler.getBlobWithinRadius(pos, MAX_BLOB_RADIUS)) {

				if(blob.contains(pos) && !blob.isPositionAllowed(world, pos))
					blob.removeBlock(x, y, z);
				else if(!blob.contains(pos) && blob.isPositionAllowed(world, pos))
					handler.onBlockRemove(pos);
				else if(!blob.contains(pos) && !blob.isPositionAllowed(world, pos) && blob.getBlobSize() == 0) {
					blob.addBlock(blob.getRootPosition());
				}
			}
		}
	}*/

	//Called from setBlock in World.class
	public static void onBlockChange(@Nonnull World world, @Nonnull BlockPos bpos) {

		if(ARConfiguration.getCurrentConfig().enableOxygen && !world.isRemote && world.getChunkFromBlockCoords(new BlockPos(bpos)).isLoaded()) {
			HashedBlockPosition pos = new HashedBlockPosition(bpos);

			AtmosphereHandler handler = getOxygenHandler(world.provider.getDimension());

			//Bonus chests cause world gen to begin before loading the world
			//Because atmosphere handlers are created at world load time
			//there is a possibility handler can be null here
			if(handler == null)
				return; //WTF

			//Grab stuff so we don't call functions 1600 times
			IBlockState state = world.getBlockState(bpos);
			Material material = state.getMaterial();
			IAtmosphere type = handler.getAtmosphereType(bpos);

			//Block handling for what should and shouldn't exist or what should be on fire
			//Things should be on fire
			if (type == AtmosphereType.SUPERHEATED) {
				if (material == Material.CACTUS) {
					world.setBlockState(bpos, Blocks.FIRE.getDefaultState());
				} else if (material == Material.PLANTS) {
					world.setBlockState(bpos, Blocks.FIRE.getDefaultState());
				} else if (material == Material.VINE) {
					world.setBlockState(bpos, Blocks.FIRE.getDefaultState());
				} else if (state.getBlock().isLeaves(state, world, bpos)) {
					world.setBlockState(bpos, Blocks.FIRE.getDefaultState());
				}else if (material == Material.WOOD) {
					world.setBlockState(bpos, Blocks.FIRE.getDefaultState());
				} else if (material == Material.WEB) {
					world.setBlockState(bpos, Blocks.FIRE.getDefaultState());
				} else if (material == Material.CARPET) {
					world.setBlockState(bpos, Blocks.FIRE.getDefaultState());
				} else if (material == Material.CLOTH) {
					world.setBlockState(bpos, Blocks.FIRE.getDefaultState());
				} else if (material == Material.GOURD) {
					world.setBlockState(bpos, Blocks.FIRE.getDefaultState());
				}
			}
			//Plants should die
			else if(!type.allowsCombustion()) {
				if(state.getBlock().isLeaves(state, world, bpos)) {
					world.setBlockToAir(bpos);
				} else if (material == Material.FIRE) {
					world.setBlockToAir(bpos);
				} else if (material == Material.CACTUS) {
					world.setBlockToAir(bpos);
				} else if (material == Material.PLANTS && state.getBlock() != Blocks.DEADBUSH) {
					world.setBlockState(bpos, Blocks.DEADBUSH.getDefaultState());
				} else if (material == Material.VINE) {
					world.setBlockToAir(bpos);
				} else if (material == Material.GRASS) {
					world.setBlockState(bpos, Blocks.DIRT.getDefaultState());
				}
			}
			//Gasses should automatically vaporize and dissipate
			if (type == AtmosphereType.VACUUM) {
				 if (material == Material.WATER && state.getBlock() instanceof IFluidBlock) {
					 IFluidBlock fluidblock = (IFluidBlock)state.getBlock();
					 if (fluidblock.getFluid().isGaseous())
					      world.setBlockToAir(bpos);
				 }
			}
			//Water blocks should also vaporize and disappear
			if (type == AtmosphereType.SUPERHEATED || type == AtmosphereType.SUPERHEATEDNOO2 || type == AtmosphereType.VERYHOT || type == AtmosphereType.VERYHOTNOO2) {
				if (material == Material.WATER) {
					world.setBlockToAir(bpos);
				}
			}


			List<AreaBlob> nearbyBlobs = handler.getBlobWithinRadius(pos, MAX_BLOB_RADIUS);
			for(AreaBlob blob : nearbyBlobs) {

				if(blob.getBlobMaxRadius() > pos.getDistance(blob.getRootPosition())) {
					if(world.isAirBlock(bpos))
						handler.onBlockRemove(pos);
					else {
						//Place block
						if( blob.contains(pos) && !blob.isPositionAllowed(world, pos, nearbyBlobs)) {
							blob.removeBlock(pos);
						}
						else if(!blob.contains(blob.getRootPosition())) {
							blob.addBlock(blob.getRootPosition(), nearbyBlobs);
						}
						else if(!blob.contains(pos) && blob.isPositionAllowed(world, pos, nearbyBlobs))//isFulBlock(world, pos.getBlockPos()))
							blob.addBlock(pos, nearbyBlobs);
					}
				}
			}
		}
	}

	/**
	 * Gets a list of AreaBlobs within a radius
	 * @param pos position
	 * @param radius distance from the position to find blobs within
	 * @return List of AreaBlobs within the radius from the position
	 */
	@Nonnull
	protected List<AreaBlob> getBlobWithinRadius(@Nonnull HashedBlockPosition pos, int radius) {
		LinkedList<AreaBlob> list = new LinkedList<>();
		for(AreaBlob blob : blobs.values()) {
			if(blob.getRootPosition().getDistance(pos) - radius <= 0) {
				list.add(blob);
			}
		}
		return list;
	}

	/**
	 * @param dimNumber dimension number for which to get the oxygenhandler
	 * @return the oxygen handler for the planet or null if none exists
	 */
	@Nullable
	public static AtmosphereHandler getOxygenHandler(int dimNumber) {
		//Get your oxyclean!
		return dimensionOxygen.get(dimNumber);
	}

	/**
	 * Registers a Blob with the atmosphere handler.  
	 * Must be called before use
	 * @param handler IBlobHander to register with
	 * @param pos
	 */
	public void registerBlob(@Nonnull IBlobHandler handler, BlockPos pos) {
		AreaBlob blob = blobs.get(handler);
		if(blob == null) {
			blob = new AtmosphereBlob(handler);
			blobs.put(handler, blob);
			blob.setData(AtmosphereType.PRESSURIZEDAIR);
		}
	}

	/**
	 * Registers a Blob with provided blob type
	 * Must be called before use
	 * @param handler IBlobHander to register with
	 * @param pos
	 * @param blob2
	 */
	public void registerBlob(@Nonnull IBlobHandler handler, BlockPos pos, @Nonnull AreaBlob blob2) {
		AreaBlob blob = blobs.get(handler);
		if(blob == null) {
			blob = blob2;
			blobs.put(handler, blob);
			blob.setData(AtmosphereType.PRESSURIZEDAIR);
		}
	}

	/**
	 * Unregisters a blob from the atmosphere handler
	 * @param handler IBlobHandlerObject the blob is associated with
	 */
	public void unregisterBlob(@Nonnull IBlobHandler handler) {
		blobs.remove(handler);
	}

	/**
	 * Removes all blocks from the blob associated with this handler
	 * @param handler the handler associated with this blob
	 */
	public void clearBlob(@Nonnull IBlobHandler handler) {

		if(blobs.containsKey(handler)) {
			blobs.get(handler).clearBlob();
		}
	}

	/**
	 * Adds a block to the blob
	 * @param handler
	 * @param x
	 * @param y
	 * @param z
	 */
	public void addBlock(@Nonnull IBlobHandler handler, int x, int y, int z){
		addBlock(handler, new HashedBlockPosition(x, y, z));
	}

	/**
	 * Adds a block to the blob
	 * @param handler
	 * @return true if blob addition is successful
	 */
	public boolean addBlock(@Nonnull IBlobHandler handler, @Nonnull HashedBlockPosition pos){
		AreaBlob blob = blobs.get(handler);
		blob.addBlock(pos, getBlobWithinRadius(pos, MAX_BLOB_RADIUS));
		return !blob.getLocations().isEmpty();
	}

	/**
	 * @param pos2
	 * @return AtmosphereType at this location
	 */
	@Nonnull
	public IAtmosphere getAtmosphereType(@Nonnull BlockPos pos2) {
		if(ARConfiguration.getCurrentConfig().enableOxygen) {
			HashedBlockPosition pos = new HashedBlockPosition(pos2);

			for(AreaBlob blob : blobs.values()) {
				if(blob.contains(pos)) {
					IAtmosphere atmosphere = (IAtmosphere)blob.getData();

					if(atmosphere != null)
						return atmosphere;
				}
			}

			return getDefaultAtmosphereType();
		}

		return AtmosphereType.AIR;
	}

	/**
	 * @return the default atmosphere type used by this planet
	 */
	@Nonnull
	public IAtmosphere getDefaultAtmosphereType() {
		return DimensionManager.getInstance().getDimensionProperties(dimId).getAtmosphere();
	}

	/**
	 * Gets the atmosphere type at the location of this entity
	 * @param entity the entity to check against
	 * @return The atmosphere type this entity is inside of
	 */
	@Nullable
	public IAtmosphere getAtmosphereType(@Nonnull Entity entity) {
		if(ARConfiguration.getCurrentConfig().enableOxygen) {
			HashedBlockPosition pos = new HashedBlockPosition((int)Math.floor(entity.posX), (int)Math.ceil(entity.posY), (int)Math.floor(entity.posZ));
			for(AreaBlob blob : blobs.values()) {
				if(blob.contains(pos)) {
					blob.addUUIDToHandlerList(entity.getUniqueID());
					return (IAtmosphere)blob.getData();
				} else
					blob.removeUUIDFromHandlerList(entity.getUniqueID());
			}

			return DimensionManager.getInstance().getDimensionProperties(dimId).getAtmosphere();
		}
		return AtmosphereType.AIR;
	}

	/**
	 * Gets the pressure at the location of this entity
	 * @param entity the entity to check against
	 * @return The atmosphere pressure this entity is inside of, or -1 to use default
	 */
	public float getAtmospherePressure(@Nonnull Entity entity) {
		if(ARConfiguration.getCurrentConfig().enableOxygen) {
			HashedBlockPosition pos = new HashedBlockPosition((int)Math.floor(entity.posX), (int)Math.ceil(entity.posY), (int)Math.floor(entity.posZ));
			for(AreaBlob blob : blobs.values()) {
				if(blob.contains(pos) && blob instanceof AtmosphereBlob) {
					return ((AtmosphereBlob)blob).getPressure();
				}
			}
		}
		return -1;
	}

	/**
	 * Set the pressure of the handler's blob
	 * @param handler the handler to check against
	 * @return The atmosphere pressure this handler has
	 */
	public float getAtmospherePressure(IBlobHandler handler) {
		AtmosphereBlob blob = (AtmosphereBlob) blobs.get(handler);
		return blob.getPressure();
	}

	/**
	 * Set the pressure of the handler's blob
	 * @param handler the handler to check against
	 * @return The atmosphere pressure this entity is inside of
	 */
	public void setAtmospherePressure(IBlobHandler handler, float pressure) {
		AtmosphereBlob blob = (AtmosphereBlob) blobs.get(handler);
		blob.setPressure(pressure);
	}

	/**
	 * Set the CO2 of the handler's blob
	 * @param handler the handler to check against
	 * @return The atmosphere CO2 this handler has
	 */
	public int getAtmosphereCO2(IBlobHandler handler) {
		AtmosphereBlob blob = (AtmosphereBlob) blobs.get(handler);
		return blob.getCarbonDioxide();
	}

	/**
	 * Set the pressure of the CO2's blob
	 * @param handler the handler to check against
	 * @return The CO2 pressure this entity is inside of
	 */
	public void setAtmosphereCO2(IBlobHandler handler, int amount) {
		AtmosphereBlob blob = (AtmosphereBlob) blobs.get(handler);
		blob.setCarbonDioxide(amount);
	}

	/**
	 * @param entity entity to check against
	 * @return true if the entity can breathe in the this atmosphere
	 */
	public boolean canEntityBreathe(@Nonnull EntityLiving entity) {
		if(ARConfiguration.getCurrentConfig().enableOxygen) {
			HashedBlockPosition pos = new HashedBlockPosition((int)Math.floor(entity.posX), (int)Math.ceil(entity.posY), (int)Math.floor(entity.posZ));
			for(AreaBlob blob : blobs.values()) {
				IAtmosphere atmosphere = (IAtmosphere)blob.getData();
				if(blob.contains(pos) && atmosphere != null && atmosphere.isImmune(entity)) {
					return true;
				}
			}
			return DimensionManager.getInstance().getDimensionProperties(dimId).getAtmosphere().isImmune(entity);
		}

		return true;
	}

	/**
	 * @param handler the handler registered to this blob
	 * @return The current size of the blob
	 */
	public int getBlobSize(@Nonnull IBlobHandler handler) {
		return blobs.get(handler).getBlobSize();
	}

	/**
	 * Changes the atmosphere type of this blob
	 * @param handler the handler for the blob
	 * @param data the AtmosphereType to set this blob to.
	 */
	public void setAtmosphereType(@Nonnull IBlobHandler handler, @Nonnull IAtmosphere data) {
		blobs.get(handler).setData(data);
	}

	/**
	 * Gets the atmosphere type of this blob
	 * @param handler the handler for the blob
	 */
	@Nonnull
	public IAtmosphere getAtmosphereType(@Nonnull IBlobHandler handler) {
		if(ARConfiguration.getCurrentConfig().enableOxygen) {
			IAtmosphere atmosphere = (IAtmosphere) blobs.get(handler).getData();
			if(atmosphere != null)
				return atmosphere;
			else
				return getDefaultAtmosphereType();
		}

		return AtmosphereType.AIR;
	}
}
