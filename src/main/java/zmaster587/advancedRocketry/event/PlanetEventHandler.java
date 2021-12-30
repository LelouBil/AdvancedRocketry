package zmaster587.advancedRocketry.event;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PortalInfo;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity.SleepResult;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedOutEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent.FogColors;
import net.minecraftforge.client.event.EntityViewRenderEvent.RenderFogEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import zmaster587.advancedRocketry.advancements.ARAdvancements;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.AdvancedRocketryManagers;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.AdvancedRocketryItems;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.advancedRocketry.api.body.PlanetManager;
import zmaster587.advancedRocketry.api.body.planet.PlanetProperties;
import zmaster587.advancedRocketry.util.TransitionEntity;
import zmaster587.advancedRocketry.world.util.TeleporterNoPortal;
import zmaster587.advancedRocketry.world.util.WorldDummy;
import zmaster587.libVulpes.api.IModularArmor;
import zmaster587.libVulpes.util.ZUtils;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class PlanetEventHandler {

	public static long time = 0;
	private static long endTime, duration;
	private static List<TransitionEntity> transitionMap = new LinkedList<>();

	public static void addDelayedTransition(TransitionEntity entity) {
		transitionMap.add(entity);
	}

	//Handle gravity
	@SubscribeEvent
	public void playerTick(LivingUpdateEvent event) {

		if(event.getEntity().world.isRemote && event.getEntity().getPosY() > 260 && event.getEntity().getPosY() < 270 && event.getEntity().getMotion().y < -.1) {
			RocketEventHandler.destroyOrbitalTextures(event.getEntity().world);
		}
		if(event.getEntity().isInWater()) {
			if(AtmosphereType.LOWOXYGEN.isImmune(event.getEntityLiving()))
				event.getEntity().setAir(300);
		}
	}

	@SubscribeEvent
	public void sleepEvent(@Nonnull PlayerSleepInBedEvent event) {
		PlanetProperties props = PlanetManager.getInstance().getPlanetProperties(ZUtils.getDimensionIdentifier(event.getEntity().getEntityWorld()));
		if(!props.getLocation().dimension.equals(PlanetManager.spaceDimensionID)) {
			if (!ARConfiguration.getCurrentConfig().forcePlayerRespawnInSpace.get() && AtmosphereHandler.hasAtmosphereHandler(event.getEntity().world) && !AtmosphereHandler.getOxygenHandler(event.getEntity().world).getAtmosphereType(event.getPos()).isBreathable()) {
				event.setResult(SleepResult.OTHER_PROBLEM);
			}
		}
	}

	@SubscribeEvent
	public void blockPlacedEvent(BlockEvent.EntityPlaceEvent event)
	{
		World world =event.getEntity().getEntityWorld(); 
		if(!world.isRemote  && AtmosphereHandler.getOxygenHandler(world) != null &&
				!AtmosphereHandler.getOxygenHandler(world).getAtmosphereType(event.getPos()).allowsCombustion()) {

			//TODO: move hardcoded torches to config
			if(event.getPlacedBlock().getBlock() == Blocks.TORCH) {
				event.getWorld().setBlockState(event.getPos(), AdvancedRocketryBlocks.blockUnlitTorch.getDefaultState(),20);
			}
			else if(event.getPlacedBlock().getBlock() == Blocks.WALL_TORCH) {


				BlockState stateToPlace = AdvancedRocketryBlocks.blockUnlitTorchWall.getDefaultState().with(
						WallTorchBlock.HORIZONTAL_FACING, event.getPlacedBlock().get(WallTorchBlock.HORIZONTAL_FACING));

				event.getWorld().setBlockState(event.getPos(), stateToPlace,20);
			}
			else if(zmaster587.advancedRocketry.api.ARConfiguration.getCurrentConfig().torchBlocks.contains(event.getPlacedBlock().getBlock()))
			{
				event.setResult(Result.DENY);
				event.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void blockRightClicked(@Nonnull RightClickBlock event) {
		Direction direction = event.getFace();
		AtmosphereHandler atmhandler = AtmosphereHandler.getOxygenHandler(event.getWorld());

		if(!event.getWorld().isRemote && direction != null  && event.getPlayer() != null  && AtmosphereHandler.getOxygenHandler(event.getWorld()) != null && atmhandler != null &&
				!atmhandler.getAtmosphereType(event.getPos().offset(direction)).allowsCombustion()) {

			if(!event.getPlayer().getHeldItem(event.getHand()).isEmpty()) {
				if(event.getPlayer().getHeldItem(event.getHand()).getItem() == Items.FLINT_AND_STEEL || event.getPlayer().getHeldItem(event.getHand()).getItem() == Items.FIRE_CHARGE|| event.getPlayer().getHeldItem(event.getHand()).getItem() == Items.BLAZE_POWDER || event.getPlayer().getHeldItem(event.getHand()).getItem() == Items.BLAZE_ROD )
					event.setCanceled(true);
			}
		}

		if(!event.getWorld().isRemote && !event.getItemStack().isEmpty() && event.getItemStack().getItem() == AdvancedRocketryItems.itemSeat && event.getWorld().getBlockState(event.getPos()).getBlock() == Blocks.TNT) {
			ARAdvancements.triggerAdvancement(ARAdvancements.BEER, (ServerPlayerEntity)event.getEntity());
		}
	}

	@SubscribeEvent
	@OnlyIn(value=Dist.CLIENT)
	public void disconnected(LoggedOutEvent event) {
		// Reload configs from disk
		ARConfiguration.useClientDiskConfig();
		//zmaster587.advancedRocketry.dimension.DimensionManager.getInstance().unregisterAllDimensions();
	}


	//TODO: more robust way of inv checking
	/*@SubscribeEvent
	public void containerOpen(PlayerContainerEvent event) {
		//event.getEntity()Player.openContainer
		if(RocketInventoryHelper.canPlayerBypassInvChecks(event.getEntityPlayer()) && event instanceof PlayerContainerEvent.Close)
			RocketInventoryHelper.removePlayerFromInventoryBypass(event.getEntityPlayer());
		if(event instanceof PlayerContainerEvent.Open) {

		}
	}*/

	//Tick dimensions, needed for satellites, and GUIs
	@SubscribeEvent
	public void tick(ServerTickEvent event) {
		//Tick satellites
		if(event.phase == TickEvent.Phase.END) {
			PlanetManager.getInstance().tickSatellites();
			time++;

			if(!transitionMap.isEmpty()) {
				Iterator<TransitionEntity> itr = transitionMap.iterator();

				while(itr.hasNext()) {
					TransitionEntity ent = itr.next();
					if(ent.entity.world.getGameTime() >= ent.time) {
						ServerWorld newWorld = ent.dimId;
						if(ent.entity instanceof ServerPlayerEntity)
						{
							((ServerPlayerEntity)ent.entity).teleport(newWorld, ent.location.getX(), ent.location.getY(), ent.location.getZ(), ent.entity.rotationYaw, ent.entity.rotationPitch);
						}
						else
						{
							PortalInfo info = new PortalInfo(new Vector3d(ent.location.getX(), ent.location.getY(), ent.location.getZ()), ent.entity.getMotion(), ent.entity.rotationYaw, ent.entity.rotationPitch);
							ent.entity.changeDimension(newWorld, new TeleporterNoPortal(newWorld, info));
						}
						//should be loaded by now
						Entity rocket = newWorld.getEntityByUuid(ent.entity2.getUniqueID());
						if(rocket != null)
							ent.entity.startRiding(rocket);
						itr.remove();
					}
				}
			}
		}
	}

	//Make sure the player receives data about the dimensions
	@SubscribeEvent
	public void playerLoggedInEvent(PlayerLoggedInEvent event) {
		AdvancedRocketryManagers.station.notifyClientOfStations(event.getPlayer());
	}

	@SubscribeEvent
	public void worldLoadEvent(WorldEvent.Load event) {
		if(!event.getWorld().isRemote())
			AtmosphereHandler.registerWorld((World)event.getWorld());
	}

	@SubscribeEvent
	public void worldUnloadEvent(WorldEvent.Unload event) {
		if(!event.getWorld().isRemote())
			AtmosphereHandler.unregisterWorld((World) event.getWorld());
	}

	/**
	 * Starts a burst, used for move to warp effect
	 * @param endTime
	 * @param duration
	 */
	@OnlyIn(value=Dist.CLIENT)
	public static void runBurst(long endTime, long duration) {
		PlanetEventHandler.endTime = endTime;
		PlanetEventHandler.duration = duration;
	}

	//Handle fog density and color
	@SubscribeEvent
	@OnlyIn(value=Dist.CLIENT)
	public void fogColor(FogColors event) {

		Entity entity = event.getInfo().getRenderViewEntity();
		World world = entity.world;
		BlockState state = event.getInfo().getBlockAtCamera();

		if(state.getMaterial() == Material.WATER)
			return;

		PlanetProperties properties = PlanetManager.getInstance().getPlanetProperties(ZUtils.getDimensionIdentifier(world));
		if(properties != null) {
			float[] color =  properties.getRender().fogColor;
			event.setRed(Math.min(event.getRed()*color[0]*1.0f,1f));
			event.setGreen(Math.min(event.getGreen()*color[1]*1.0f, 1f));
			event.setBlue(Math.min(event.getBlue()*color[2]*1.0f, 1f));

			//Make sure fog doesn't happen on zero atmospheres
			if (properties.getPressure() == 0) {
				event.setRed(0);
				event.setGreen(0);
				event.setBlue(0);
			}

			if(endTime > 0) {
				double amt = (endTime - Minecraft.getInstance().world.getGameTime()) / (double)duration;
				if(amt < 0) {
					endTime = 0;
				}
				else {
					event.setRed((float) amt);
					event.setGreen((float) amt);
					event.setBlue((float) amt);
				}

			}
		}
	}

	private static final ItemStack component = new ItemStack(AdvancedRocketryItems.itemAntiFogVisorUpgrade, 1);
	@SubscribeEvent
	@OnlyIn(value=Dist.CLIENT)
	public void fogColor(RenderFogEvent event) {

		if(event.getInfo().getRenderViewEntity().getEntityWorld() instanceof WorldDummy)
			return;

		PlanetProperties properties = PlanetManager.getInstance().getPlanetProperties(ZUtils.getDimensionIdentifier(event.getInfo().getRenderViewEntity().getEntityWorld()));
		if(properties != null && event.getInfo().getBlockAtCamera().getBlock() != Blocks.WATER && event.getInfo().getBlockAtCamera().getBlock() != Blocks.LAVA) {//& properties.atmosphereDensity > 125) {
			//GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
			GlStateManager.fogMode(GlStateManager.FogMode.LINEAR.param);



			float f1 = event.getFarPlaneDistance();
			float near;
			float far;

			int atmosphere = Math.min(properties.getPressure(), 200);
			ItemStack armor = Minecraft.getInstance().player.getItemStackFromSlot(EquipmentSlotType.HEAD);

			if(!armor.isEmpty() && armor.getItem() instanceof IModularArmor) {
				for(ItemStack i : ((IModularArmor)armor.getItem()).getComponents(armor)) {
					if(i.isItemEqual(component)) {
						atmosphere = Math.min(atmosphere, 100);
						break;
					}
				}
			}

			//Check environment
			if(AtmosphereHandler.currentPressure != -1) {
				atmosphere = Math.min(AtmosphereHandler.currentPressure, 200);
			}

			if(atmosphere > 100) {
				near = 0.75f*f1*(2.00f - atmosphere*atmosphere/10000f);
				far = f1;
			}
			else {
				near = 0.1f*f1*(2.00f -atmosphere/100f);
				far = f1*(2.002f - atmosphere/100f);
			}

			//RenderSystem.fogStart(near);
			RenderSystem.fogEnd(far);
			RenderSystem.fogDensity(0);
			//event.setCanceled(false);
		}

	}

	//Make sure the player doesn't die on low gravity worlds
	@SubscribeEvent
	public void fallEvent(LivingFallEvent event) {
		if(PlanetManager.getInstance().isDimensionCreated(ZUtils.getDimensionIdentifier(event.getEntity().world))) {
			PlanetProperties planet =  PlanetManager.getInstance().getDimensionProperties(ZUtils.getDimensionIdentifier(event.getEntity().world));
			event.setDistance(event.getDistance() * planet.getGravitationalMultiplier(new BlockPos(event.getEntity().getPositionVec())));
		}
	}
}
