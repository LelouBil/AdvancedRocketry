package zmaster587.advancedRocketry.tile.atmosphere;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.AdvancedRocketryFluids;
import zmaster587.advancedRocketry.api.AreaBlob;
import zmaster587.advancedRocketry.api.util.IBlobHandler;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.inventory.TextureResources;
import zmaster587.advancedRocketry.util.AudioRegistry;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.api.IToggleableMachine;
import zmaster587.libVulpes.client.RepeatingSound;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.network.PacketMachine;
import zmaster587.libVulpes.tile.TileInventoriedRFConsumerTank;
import zmaster587.libVulpes.util.FluidUtils;
import zmaster587.libVulpes.util.HashedBlockPosition;
import zmaster587.libVulpes.util.IAdjBlockUpdate;
import zmaster587.libVulpes.util.INetworkMachine;
import zmaster587.libVulpes.util.ZUtils.RedstoneState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TileOxygenVent extends TileInventoriedRFConsumerTank implements IBlobHandler, IModularInventory, INetworkMachine, IToggleableMachine, IButtonInventory, IToggleButton, IAdjBlockUpdate {

	//Misc. variables
	private boolean firstRun;
	private boolean soundInit;
	private boolean allowTrace;
	private List<UUID> entityList;
	private List<TileOxygenVentSystemBase> secondaryTiles;
	//Seal variables
	private boolean isSealed;
	private int radius = 0;
	private float worldAtmospherePressure = 0;
	private int carbonDioxideContent;
	private int currentBlobVolume;
	//Packet variables
	private final static byte PACKET_REDSTONE_ID = 2;
	private final static byte PACKET_TRACE_ID = 3;
	//GUI and redstone variables
	private RedstoneState state;
	private ModuleRedstoneOutputButton redstoneControl;
	private ModuleToggleSwitch traceToggle;
	
	public TileOxygenVent() {
		super(1000,2, 2000);
		secondaryTiles = null;
		isSealed = false;
		firstRun = true;
		soundInit = false;
		allowTrace = false;
		secondaryTiles = new ArrayList<>();
		entityList = new ArrayList<>();
		state = RedstoneState.ON;
		redstoneControl = new ModuleRedstoneOutputButton(174, 4, PACKET_REDSTONE_ID, "", this);
		traceToggle = new ModuleToggleSwitch(80, 20, PACKET_TRACE_ID, LibVulpes.proxy.getLocalizedString("msg.vent.trace"), this, TextureResources.buttonGeneric, 80, 18, false);
	}

	public TileOxygenVent(int energy, int invSize, int tankSize) {
		super(energy, invSize, tankSize);
		secondaryTiles = null;
		isSealed = false;
		firstRun = true;
		soundInit = false;
		allowTrace = false;
		secondaryTiles = new ArrayList<>();
		entityList = new ArrayList<>();
		state = RedstoneState.ON;
		redstoneControl = new ModuleRedstoneOutputButton(174, 4, 0, "", this);
		traceToggle = new ModuleToggleSwitch(80, 20, 5, LibVulpes.proxy.getLocalizedString("msg.vent.trace"), this, TextureResources.buttonGeneric, 80, 18, false);
	}

	@Override
	public int getPowerPerOperation() {
		return 10;
	}
	
	@Override
	public void performFunction() {
		if(!world.isRemote) {
			AtmosphereHandler atmhandler = AtmosphereHandler.getOxygenHandler(this.world.provider.getDimension());
			if(atmhandler == null)
				return;

			//Do first run stuff to make sure everything we need is set up for functions
			if(firstRun) {
				atmhandler.registerBlob(this, pos);
				worldAtmospherePressure = DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension()).getAtmosphereDensity();
				firstRun = false;
				onAdjacentBlockUpdated();
			}

			//Turn off & deactivation stuff, due to miniscule blob
			if(isSealed && (!isTurnedOn() || atmhandler.getBlobSize(this) == 0)) {
				setSealed(false);
			//Activation if we do not have a seal on record but should be making one
			} else if(!isSealed && isTurnedOn() && hasEnoughEnergy(getPowerPerOperation())) {
                //Activation, but only sometimes so we don't lag the server to death trying to calculate blobs
				if(world.getTotalWorldTime() % 100 == 0)
					setSealed(atmhandler.addBlock(this, new HashedBlockPosition(pos)));
				//Otherwise we show where the oxygen is leaking to with the oxygen trace option
				else if(world.getTotalWorldTime() % 10 == 0 && allowTrace) {
					radius++;
					if(radius > 128)
						radius = 0;
				}
			}

			//Handle pressure updates due to blob updates
			if (currentBlobVolume != atmhandler.getBlobSize(this)) {
				//Equalize pressure with the newly added volume, so we can lose pressure venting to airlocks (there's no _good_ way to add pressure that I can tell, though... how do you know if the broken block adds or decreases pressure?)
				worldAtmospherePressure *= Math.min((float) currentBlobVolume /(atmhandler.getBlobSize(this) * DimensionManager.getInstance().getDimensionProperties(this.world.provider.getDimension()).getAtmosphereDensity()/100f), 1f);
				currentBlobVolume = atmhandler.getBlobSize(this);
			}

			//If we have a below-average atmosphere, add to it with oxygen
			int addedFluidEqualization = 0;
			if (worldAtmospherePressure < 100) {
				FluidStack fluidStack;
				if ((fluidStack = tank.drain(100, true)) != null) {
					worldAtmospherePressure = Math.min(worldAtmospherePressure + fluidStack.amount * 1.6f * (100f / atmhandler.getBlobSize(this)), 100f);
					addedFluidEqualization = fluidStack.amount;
				}
			}
			int numEntities = entityList.size();
			//Handle atmosphere loss due to size
			worldAtmospherePressure += (int)(((worldAtmospherePressure - 100)/100) * Math.sqrt(atmhandler.getBlobSize(this)/512f) * 1.6f * (100f / atmhandler.getBlobSize(this)));
			//Handle atmosphere carbon dioxide cost due to living entities
			//Forty ticks between checks means oxy vents are 1.333x as oxygen efficient as suits
			if (worldAtmospherePressure <= 110 && worldAtmospherePressure > 25 && numEntities > 0 && world.getTotalWorldTime() % 40 == 0) {
				int volume = tank.drain(10 * numEntities, true).amount;
				carbonDioxideContent += (10 * numEntities - volume);
			}

			//Run all the connected components in the list, including incrementing with nitrogen or rectifying air amounts
			for (TileOxygenVentSystemBase tile : secondaryTiles) {
				if(tile instanceof TileAirPressureEqualizer)  {
					if (tile.canPerformFunction(atmhandler, world, energy.getUniversalEnergyStored(), worldAtmospherePressure)) {
						int energyConsumption = tile.performFunction(atmhandler, world, 0);
						energy.extractEnergy(energyConsumption, false);
						worldAtmospherePressure = Math.max(worldAtmospherePressure - (energyConsumption * 1.6f * (100f / atmhandler.getBlobSize(this))), 100f);
					}
				} else if(tile instanceof TileSpentAirVent && carbonDioxideContent != 0)  {
					if (tile.canPerformFunction(atmhandler, world, energy.getUniversalEnergyStored(), worldAtmospherePressure)) {
						int energyConsumption = tile.performFunction(atmhandler, world, carbonDioxideContent);
						energy.extractEnergy(energyConsumption, false);
						carbonDioxideContent -= energyConsumption/10;
					}
				} else if(tile instanceof TileAirMixSupplier)  {
					if (tile.canPerformFunction(atmhandler, world, energy.getUniversalEnergyStored(), worldAtmospherePressure)) {
						int energyConsumption = tile.performFunction(atmhandler, world, addedFluidEqualization);
						energy.extractEnergy(energyConsumption, false);
						worldAtmospherePressure = Math.min(worldAtmospherePressure + (energyConsumption * 10f) * 1.6f * (100f / atmhandler.getBlobSize(this)), 100f);
					}
				}
			}

			//Set the atmosphere type of the blob
			if (isSealed) {
				if (worldAtmospherePressure >= 200 || worldAtmospherePressure <= 25)
					atmhandler.setAtmosphereType(this, DimensionManager.getInstance().getDimensionProperties(this.world.provider.getDimension()).getAtmosphere());
				else if (worldAtmospherePressure > 75)
					atmhandler.setAtmosphereType(this, isAtmosphereOverCO2Capacity() ? AtmosphereType.NOO2 : AtmosphereType.PRESSURIZEDAIR);
				else if (worldAtmospherePressure <= 75)
					atmhandler.setAtmosphereType(this, AtmosphereType.LOWOXYGEN);
			} else
				atmhandler.setAtmosphereType(this, DimensionManager.getInstance().getDimensionProperties(this.world.provider.getDimension()).getAtmosphere());
		}
	}

	@Override
	public void update() {
		if(canPerformFunction()) {
			performFunction();
			if(!world.isRemote && isSealed) this.energy.extractEnergy(getPowerPerOperation(), false);
		}
		if(!soundInit && world.isRemote) {
			LibVulpes.proxy.playSound(new RepeatingSound(AudioRegistry.airHissLoop, SoundCategory.BLOCKS, this));
		}
		soundInit = true;
	}

	private boolean isAtmosphereOverCO2Capacity() {
		return 5 <= (carbonDioxideContent * 1.6f * (100f/AtmosphereHandler.getOxygenHandler(world.provider.getDimension()).getBlobSize(this)))/worldAtmospherePressure;
	}

	//TODO:
	//TODO:
	//TODO:
	//TODO:
	//DO NOT TOUCH BELOW THIS LINE, UNUSED IN FEATURE CHANGE
	//TODO:
	//TODO:
	//TODO:
	//TODO:

	private void setSealed(boolean sealed) {
		boolean prevSealed = isSealed;
		if((prevSealed != sealed)) {
			markDirty();
			world.notifyBlockUpdate(pos, world.getBlockState(pos),  world.getBlockState(pos), 2);

			if(isSealed)
				radius = -1;
		}
		isSealed = sealed;
	}

	@Override
	public void addEntityToList(UUID id) {
		if (!entityList.contains(id)) {
			entityList.add(id);
		}
	}

	@Override
	public void removeEntityFromList(UUID id) {
		entityList.remove(id);
	}

	@Override
	public void onAdjacentBlockUpdated() {
		secondaryTiles.clear();
		TileEntity[] tiles = new TileEntity[6];
		tiles[0] = world.getTileEntity(pos.add(1,0,0));
		tiles[1] = world.getTileEntity(pos.add(-1,0,0));
		tiles[2] = world.getTileEntity(pos.add(0,1,0));
		tiles[3] = world.getTileEntity(pos.add(0,-1,0));
		tiles[4] = world.getTileEntity(pos.add(0,0,1));
		tiles[5] = world.getTileEntity(pos.add(0,0,-1));

		boolean hasSAV = false;
		boolean hasAPE = false;
		boolean hasAMS = false;
		for(TileEntity tile : tiles) {
			//If we have mutliple tiles of the same type, this excludes them. This 'optimizable' code does need to exist and should stay
			if(tile instanceof TileOxygenVentSystemBase && !((tile instanceof  TileSpentAirVent && hasSAV) || (tile instanceof TileAirMixSupplier && hasAMS) || (tile instanceof  TileAirPressureEqualizer && hasAPE))) {
				secondaryTiles.add((TileOxygenVentSystemBase) tile);
			}
		}
	}

	@Override
	public void invalidate() {
		super.invalidate();

		AtmosphereHandler atmhandler = AtmosphereHandler.getOxygenHandler(this.world.provider.getDimension());
		if(atmhandler != null) atmhandler.unregisterBlob(this);
		entityList.clear();
	}

	@Override
	public World getWorldObj() {
		return world;
	}

	@Override
	public boolean canPerformFunction() {
		return AtmosphereHandler.hasAtmosphereHandler(this.world.provider.getDimension());
	}

	@Override
	public boolean canFill(Fluid fluid) {
		return FluidUtils.areFluidsSameType(fluid, AdvancedRocketryFluids.fluidOxygen) && super.canFill(fluid);
	}

	public boolean isTurnedOn() {
		if(state == RedstoneState.OFF)
			return true;

		boolean state2 = world.isBlockIndirectlyGettingPowered(pos) > 0;

		if(state == RedstoneState.INVERTED)
			state2 = !state2;
		return state2;
	}

	@Override
	public int getTraceDistance() {
		return allowTrace ? radius : -1;
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos,getBlockMetadata(), getUpdateTag());

	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		handleUpdateTag(pkt.getNbtCompound());
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		NBTTagCompound tag = super.getUpdateTag();
		tag.setBoolean("isSealed", isSealed);
		
		return tag;
	}

	@Override
	public void handleUpdateTag(NBTTagCompound tag) {
		super.handleUpdateTag(tag);
		isSealed = tag.getBoolean("isSealed");
	}

	@Override
	@Nonnull
	public int[] getSlotsForFace(@Nullable EnumFacing side) {
		return new int[]{};
	}

	@Override
	public boolean isItemValidForSlot(int slot, @Nonnull ItemStack itemStack) {
		return false;
	}

	@Override
	public boolean canBlobsOverlap(HashedBlockPosition blockPosition, AreaBlob blob) {
		return false;
	}

	@Override
	public int getMaxBlobRadius() {
		return ARConfiguration.getCurrentConfig().oxygenVentSize;
	}

	@Override
	@Nonnull
	public HashedBlockPosition getRootPosition() {
		return new HashedBlockPosition(pos);
	}

	@Override
	public List<ModuleBase> getModules(int ID, EntityPlayer player) {
		ArrayList<ModuleBase> modules = new ArrayList<>();

		modules.add(new ModuleSlotArray(52, 20, this, 0, 1));
		modules.add(new ModuleSlotArray(52, 57, this, 1, 2));
		modules.add(new ModulePower(18, 20, this));
		modules.add(new ModuleLiquidIndicator(32, 20, this));
		modules.add(redstoneControl);
		modules.add(traceToggle);
		//modules.add(toggleSwitch = new ModuleToggleSwitch(160, 5, 0, "", this, TextureResources.buttonToggleImage, 11, 26, getMachineEnabled()));
		return modules;
	}
	
	@Override
	public void setInventorySlotContents(int slot, @Nonnull ItemStack stack) {
		super.setInventorySlotContents(slot, stack);
		
		while(FluidUtils.attemptDrainContainerIInv(inventory, this.tank, getStackInSlot(0), 0, 1));
	}

	@Override
	public String getModularInventoryName() {
		return AdvancedRocketryBlocks.blockOxygenVent.getLocalizedName();
	}

	@Override
	public boolean canInteractWithContainer(EntityPlayer entity) {
		return true;
	}

	@Override
	public boolean canFormBlob() {
		return isTurnedOn();
	}
	
	@Override
	public boolean isRunning() {
		return isSealed;
	}

	@Override
	public void onInventoryButtonPressed(int buttonId) {
		if(buttonId == PACKET_REDSTONE_ID) {
			state = redstoneControl.getState();
			PacketHandler.sendToServer(new PacketMachine(this, PACKET_REDSTONE_ID));
		}
		if(buttonId == PACKET_TRACE_ID) {
			allowTrace = traceToggle.getState();
			PacketHandler.sendToServer(new PacketMachine(this, PACKET_TRACE_ID));
		}
	}
	
	@Override
	public void writeDataToNetwork(ByteBuf out, byte id) {
		if(id == PACKET_REDSTONE_ID)
			out.writeByte(state.ordinal());
		else if(id == PACKET_TRACE_ID)
			out.writeBoolean(allowTrace);
	}

	@Override
	public void readDataFromNetwork(ByteBuf in, byte packetId, NBTTagCompound nbt) {
		if(packetId == PACKET_REDSTONE_ID)
			nbt.setByte("state", in.readByte());
		else if(packetId == PACKET_TRACE_ID)
			nbt.setBoolean("trace", in.readBoolean());
	}

	@Override
	public void useNetworkData(EntityPlayer player, Side side, byte id, NBTTagCompound nbt) {
		if(id == PACKET_REDSTONE_ID)
			state = RedstoneState.values()[nbt.getByte("state")];
		else if(id == PACKET_TRACE_ID) {
			allowTrace = nbt.getBoolean("trace");
			if(!allowTrace)
				radius = -1;
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		
		state = RedstoneState.values()[nbt.getByte("redstoneState")];
		redstoneControl.setRedstoneState(state);
		allowTrace = nbt.getBoolean("allowtrace");

	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setByte("redstoneState", (byte) state.ordinal());
		nbt.setBoolean("allowtrace", allowTrace);
		return nbt;
	}

	@Override
	public boolean isEmpty() {
		return inventory.isEmpty();
	}

	@Override
	public void stateUpdated(ModuleBase module) {
		if(module.equals(traceToggle)) {
			allowTrace = ((ModuleToggleSwitch)module).getState();
			PacketHandler.sendToServer(new PacketMachine(this, PACKET_TRACE_ID));
		}
	}
}