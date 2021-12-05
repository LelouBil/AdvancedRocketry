package zmaster587.advancedRocketry.tile.atmosphere;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fluids.FluidStack;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.AdvancedRocketryFluids;
import zmaster587.advancedRocketry.api.AdvancedRocketryTileEntityType;
import zmaster587.advancedRocketry.api.atmosphere.AreaBlob;
import zmaster587.advancedRocketry.api.atmosphere.IBlobHandler;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.inventory.TextureResources;
import zmaster587.advancedRocketry.util.AudioRegistry;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.api.IToggleableMachine;
import zmaster587.libVulpes.api.LibvulpesGuiRegistry;
import zmaster587.libVulpes.client.RepeatingSound;
import zmaster587.libVulpes.inventory.ContainerModular;
import zmaster587.libVulpes.inventory.GuiHandler;
import zmaster587.libVulpes.inventory.GuiHandler.guiId;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.network.PacketMachine;
import zmaster587.libVulpes.tile.TileInventoriedFEConsumerTank;
import zmaster587.libVulpes.util.FluidUtils;
import zmaster587.libVulpes.util.HashedBlockPosition;
import zmaster587.libVulpes.util.IAdjBlockUpdate;
import zmaster587.libVulpes.util.INetworkMachine;
import zmaster587.libVulpes.util.ZUtils.RedstoneState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TileOxygenVent extends TileInventoriedFEConsumerTank implements IBlobHandler, IModularInventory, INetworkMachine, IAdjBlockUpdate, IToggleableMachine, IButtonInventory, IToggleButton {

	//Misc. variables
	private boolean firstRun;
	private boolean soundInit;
	private boolean allowTrace;
	private List<UUID> entityList;
	private List<TileOxygenSystemBase> secondaryTiles;
	//Seal variables
	private boolean isSealed;
	private int radius = 0;
	private int atmBlobCO22LStart = -1;
	private float atmBlobPressureStart = -1;
	private int currentBlobVolume;
	//Packet variables
	private final static byte PACKET_REDSTONE_ID = 2;
	private final static byte PACKET_TRACE_ID = 3;
	//GUI and redstone variables
	private RedstoneState state;
	private ModuleRedstoneOutputButton redstoneControl;
	private ModuleToggleSwitch traceToggle;

	
	public TileOxygenVent() {
		super(AdvancedRocketryTileEntityType.TILE_OXYGEN_VENT,1000,2, 2000);
		isSealed = false;
		firstRun = true;
		soundInit = false;
		allowTrace = false;
		secondaryTiles = new ArrayList<>();
		entityList = new ArrayList<>();
		state = RedstoneState.ON;
		redstoneControl = (ModuleRedstoneOutputButton) new ModuleRedstoneOutputButton(174, 4, "", this).setAdditionalData(PACKET_REDSTONE_ID);
		traceToggle = (ModuleToggleSwitch) new ModuleToggleSwitch(80, 20, LibVulpes.proxy.getLocalizedString("msg.vent.trace"), this, TextureResources.buttonGeneric, 80, 18, false).setAdditionalData(PACKET_TRACE_ID);
	}

	public TileOxygenVent(int energy, int invSize, int tankSize) {
		super(AdvancedRocketryTileEntityType.TILE_OXYGEN_VENT, energy, invSize, tankSize);
		isSealed = false;
		firstRun = true;
		soundInit = false;
		allowTrace = false;
		secondaryTiles = new ArrayList<>();
		entityList = new ArrayList<>();
		state = RedstoneState.ON;
		redstoneControl = (ModuleRedstoneOutputButton) new ModuleRedstoneOutputButton(174, 4, "", this).setAdditionalData(0);
		traceToggle = (ModuleToggleSwitch) new ModuleToggleSwitch(80, 20, LibVulpes.proxy.getLocalizedString("msg.vent.trace"), this, TextureResources.buttonGeneric, 80, 18, false).setAdditionalData(5);
	}

	@Override
	public int getPowerPerOperation() {
		return 10;
	}

	@Override
	public void performFunction() {
		if(!world.isRemote) {
			AtmosphereHandler atmhandler = AtmosphereHandler.getOxygenHandler(world);
			if(atmhandler == null)
				return;

			//Do first run stuff to make sure everything we need is set up for functions
			if(firstRun) {
				atmhandler.registerBlob(this, pos);
				atmhandler.setAtmospherePressure(this, atmBlobPressureStart == -1 ? DimensionManager.getInstance().getDimensionProperties(world).getAtmosphereDensity() : atmBlobPressureStart);
				atmhandler.setAtmosphereCO2(this, atmBlobCO22LStart == -1 ? 0 : atmBlobCO22LStart);
				onAdjacentBlockUpdated();
				firstRun = false;
			}

			//Turn off & deactivation stuff, due to miniscule blob
			if(isSealed && (!isTurnedOn() || atmhandler.getBlobSize(this) == 0)) {
				setSealed(false);
				//Activation if we do not have a seal on record but should be making one
			} else if(!isSealed && isTurnedOn() && hasEnoughEnergy(getPowerPerOperation())) {
				//Activation, but only sometimes so we don't lag the server to death trying to calculate blobs
				if(world.getGameTime() % 100 == 0 || !firstRun) {
					setSealed(atmhandler.addBlock(this, new HashedBlockPosition(pos)));
					currentBlobVolume = atmhandler.getBlobSize(this);
					//Otherwise, we show where the oxygen is leaking to with the oxygen trace option
				}else if(world.getGameTime() % 10 == 0 && allowTrace) {
					radius++;
					if(radius > 128)
						radius = 0;
				}
			}

			//Handle pressure updates due to blob updates
			if (currentBlobVolume != atmhandler.getBlobSize(this) && world.getGameTime() == 100) {
				if (atmhandler.getBlobSize(this) == 0) {
					atmhandler.setAtmospherePressure(this, 0);
					return;
				}

				//Get current atmosphere pressure
				float currentPressure = atmhandler.getAtmospherePressure(this);
				//Equalize pressure with the newly added volume, so we can lose pressure venting to airlocks (there's no _good_ way to add pressure that I can tell, though... how do you know if the broken block adds or decreases pressure?)
				atmhandler.setAtmospherePressure(this, currentPressure * Math.min((float) currentBlobVolume /(atmhandler.getBlobSize(this) * DimensionManager.getInstance().getDimensionProperties(world).getAtmosphereDensity()/100f), 1f));
				currentBlobVolume = atmhandler.getBlobSize(this);
			}

			//All this stuff relies on us actually having an atmosphere
			if (atmhandler.getBlobSize(this) > 0) {
				//If we have a below-average atmosphere, add to it with oxygen
				int addedFluidEqualization = 0;
				if (atmhandler.getAtmospherePressure(this) < 100) {
					FluidStack fluidStack;
					if (!(fluidStack = tank.drain(100, FluidAction.EXECUTE)).isEmpty()) {
						atmhandler.setAtmospherePressure(this, Math.min(atmhandler.getAtmospherePressure(this) + fluidStack.getAmount() * 1.6f * (100f / atmhandler.getBlobSize(this)), 100f));
						addedFluidEqualization = fluidStack.getAmount();
					}
				}

				//Handle atmosphere loss due to size
				atmhandler.setAtmospherePressure(this, atmhandler.getAtmospherePressure(this) + (int) (((atmhandler.getAtmospherePressure(this) - 100) / 100) * Math.sqrt(atmhandler.getBlobSize(this) / 512f) * 1.6f * (100f / atmhandler.getBlobSize(this))));

				//Handle atmosphere carbon dioxide cost due to living entities
				//Forty ticks between checks means oxy vents are 1.333x as oxygen efficient as suits
				int numEntities = entityList.size();
				int drainedOxygen = 0;
				if (atmhandler.getAtmospherePressure(this) <= 110 && atmhandler.getAtmospherePressure(this) > 25 && numEntities > 0 && world.getGameTime() % 40 == 0) {
					drainedOxygen = tank.getFluid().isEmpty() ? 0 : tank.drain(10 * numEntities, FluidAction.EXECUTE).getAmount();
					atmhandler.setAtmosphereCO2(this, atmhandler.getAtmosphereCO2(this) + numEntities);
				}

				//Run all the connected components in the list, including incrementing with nitrogen or rectifying air amounts
				for (TileOxygenSystemBase tile : secondaryTiles) {
					if (tile instanceof TilePressureEqualizer) {
						if (tile.canPerformFunction(atmhandler, world, energy.getUniversalEnergyStored(), atmhandler.getAtmospherePressure(this))) {
							int energyConsumption = tile.performFunction(atmhandler, world, 0);
							energy.extractEnergy(energyConsumption, false);
							atmhandler.setAtmospherePressure(this, Math.max(atmhandler.getAtmospherePressure(this) - (energyConsumption * 1.6f * (100f / atmhandler.getBlobSize(this))), 100f));
						}
					} else if (tile instanceof TileSpentAirVent && atmhandler.getAtmosphereCO2(this) != 0) {
						if (tile.canPerformFunction(atmhandler, world, energy.getUniversalEnergyStored(), atmhandler.getAtmospherePressure(this))) {
							int energyConsumption = tile.performFunction(atmhandler, world, Math.min(atmhandler.getAtmosphereCO2(this) * 10, drainedOxygen));
							energy.extractEnergy(energyConsumption, false);
							atmhandler.setAtmosphereCO2(this, (int)(atmhandler.getAtmosphereCO2(this) - 0.01*energyConsumption));
						}
					} else if (tile instanceof TileAirMixSupplier) {
						if (tile.canPerformFunction(atmhandler, world, energy.getUniversalEnergyStored(), atmhandler.getAtmospherePressure(this))) {
							int energyConsumption = tile.performFunction(atmhandler, world, addedFluidEqualization);
							energy.extractEnergy(energyConsumption, false);
							atmhandler.setAtmospherePressure(this, Math.min(atmhandler.getAtmospherePressure(this) + (energyConsumption * 10f) * 1.6f * (100f / atmhandler.getBlobSize(this)), 100f));
						}
					}
				}
			}

			//Set the atmosphere type of the blob
			if (isSealed) {
				float atmPressure = atmhandler.getAtmospherePressure(this);
				if (atmPressure >= 200 || atmPressure <= 25)
					atmhandler.setAtmosphereType(this, DimensionManager.getInstance().getDimensionProperties(world).getAtmosphere());
				else if (atmPressure > 75)
					atmhandler.setAtmosphereType(this, isAtmosphereOverCO2Capacity(atmhandler) ? AtmosphereType.NOO2 : AtmosphereType.PRESSURIZEDAIR);
				else if (atmPressure <= 75)
					atmhandler.setAtmosphereType(this, AtmosphereType.LOWOXYGEN);
			} else
				atmhandler.setAtmosphereType(this, DimensionManager.getInstance().getDimensionProperties(world).getAtmosphere());

		}
	}

	@Override
	public void tick() {
		if(canPerformFunction()) {
			performFunction();
			if(!world.isRemote && isSealed) this.energy.extractEnergy(getPowerPerOperation(), false);
		}
		if(!soundInit && world.isRemote) {
			LibVulpes.proxy.playSound(new RepeatingSound(AudioRegistry.airHissLoop, SoundCategory.BLOCKS, this));
		}
		soundInit = true;
	}

	private boolean isAtmosphereOverCO2Capacity(AtmosphereHandler handler) {
		return 0.05 <= (handler.getAtmosphereCO2(this) * 1.6f * (100f/handler.getBlobSize(this)))/handler.getAtmospherePressure(this);
	}
	
	@Override
	public int getTraceDistance() {
		return allowTrace ? radius : -1;
	}

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
			if(tile instanceof TileOxygenSystemBase && !((tile instanceof  TileSpentAirVent && hasSAV) || (tile instanceof TileAirMixSupplier && hasAMS) || (tile instanceof  TilePressureEqualizer && hasAPE))) {
				secondaryTiles.add((TileOxygenSystemBase) tile);
			}
		}
	}

	@Override
	public void remove() {
		super.remove();

		AtmosphereHandler atmhandler = AtmosphereHandler.getOxygenHandler(world);
		if(atmhandler != null) atmhandler.unregisterBlob(this);
		entityList.clear();
	}

	@Override
	public boolean canPerformFunction() {
		return AtmosphereHandler.hasAtmosphereHandler(world);
	}

	@Override
	public boolean canFill(Fluid fluid) {
		return FluidUtils.areFluidsSameType(fluid, AdvancedRocketryFluids.oxygenStill.get()) && super.canFill(fluid);
	}

	public boolean isTurnedOn() {
		if (state == RedstoneState.OFF)
			return true;

		boolean state2 = world.getRedstonePowerFromNeighbors(pos) > 0;

		if (state == RedstoneState.INVERTED)
			state2 = !state2;
		return state2;
	}

	@Override
	public World getWorldObj() {
		return world;
	}

	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		return new SUpdateTileEntityPacket(pos,0, getUpdateTag());

	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
		handleUpdateTag(getBlockState(), pkt.getNbtCompound());
	}

	@Override
	public CompoundNBT getUpdateTag() {
		CompoundNBT tag = super.getUpdateTag();
		tag.putBoolean("isSealed", isSealed);
		
		return tag;
	}

	@Override
	public void handleUpdateTag(BlockState state, CompoundNBT tag) {
		super.handleUpdateTag(state, tag);
		isSealed = tag.getBoolean("isSealed");
	}
	
	@Override
	@Nonnull
	public int[] getSlotsForFace(@Nullable Direction side) {
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
		return ARConfiguration.getCurrentConfig().oxygenVentSize.get();
	}

	@Override
	@Nonnull
	public HashedBlockPosition getRootPosition() {
		return new HashedBlockPosition(pos);
	}

	@Override
	public List<ModuleBase> getModules(int ID, PlayerEntity player) {
		ArrayList<ModuleBase> modules = new ArrayList<>();

		modules.add(new ModuleSlotArray(52, 20, this, 0, 1));
		modules.add(new ModuleSlotArray(52, 57, this, 1, 2, false));
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
		FluidUtils.attemptDrainContainerIInv(inventory, this.tank, getStackInSlot(0), 0, 1);
	}

	@Override
	public String getModularInventoryName() {
		return "block.advancedrocketry.oxygenvent";
	}

	@Override
	public boolean canInteractWithContainer(PlayerEntity entity) {
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
	public void onInventoryButtonPressed(ModuleButton buttonId) {
		if(buttonId == redstoneControl) {
			state = redstoneControl.getState();
			PacketHandler.sendToServer(new PacketMachine(this, PACKET_REDSTONE_ID));
		}
		if(buttonId == traceToggle) {
			allowTrace = traceToggle.getState();
			PacketHandler.sendToServer(new PacketMachine(this, PACKET_TRACE_ID));
		}
	}
	
	@Override
	public void writeDataToNetwork(PacketBuffer out, byte id) {
		if(id == PACKET_REDSTONE_ID)
			out.writeByte(state.ordinal());
		else if(id == PACKET_TRACE_ID)
			out.writeBoolean(allowTrace);
	}

	@Override
	public void readDataFromNetwork(PacketBuffer in, byte packetId, CompoundNBT nbt) {
		if(packetId == PACKET_REDSTONE_ID)
			nbt.putByte("state", in.readByte());
		else if(packetId == PACKET_TRACE_ID)
			nbt.putBoolean("trace", in.readBoolean());
	}

	@Override
	public void useNetworkData(PlayerEntity player, Dist side, byte id, CompoundNBT nbt) {
		if(id == PACKET_REDSTONE_ID)
			state = RedstoneState.values()[nbt.getByte("state")];
		else if(id == PACKET_TRACE_ID) {
			allowTrace = nbt.getBoolean("trace");
			if(!allowTrace)
				radius = -1;
		}
	}
	
	@Override
	public void read(BlockState blkstate, CompoundNBT nbt) {
		super.read(blkstate, nbt);
		
		state = RedstoneState.values()[nbt.getByte("redstoneState")];
		redstoneControl.setRedstoneState(state);
		allowTrace = nbt.getBoolean("allowtrace");

		if (firstRun) {
			currentBlobVolume = nbt.getInt("blobVolume");
			atmBlobPressureStart = nbt.getFloat("blobPressure");
			atmBlobCO22LStart = nbt.getInt("blobCO2");
		}
	}
	
	@Override
	public CompoundNBT write(CompoundNBT nbt) {
		super.write(nbt);
		nbt.putByte("redstoneState", (byte) state.ordinal());
		nbt.putBoolean("allowtrace", allowTrace);
		if (currentBlobVolume != 0) nbt.putInt("blobVolume", currentBlobVolume);
		if (AtmosphereHandler.getOxygenHandler(world) != null) {
			nbt.putFloat("blobPressure", AtmosphereHandler.getOxygenHandler(world).getAtmospherePressure(this));
			nbt.putInt("blobCO2", AtmosphereHandler.getOxygenHandler(world).getAtmosphereCO2(this));
		}
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

	@Nonnull
	@Override
	public ITextComponent getDisplayName() {
		return new TranslationTextComponent(getModularInventoryName());
	}

	@Override
	@ParametersAreNonnullByDefault
	public Container createMenu(int id, PlayerInventory inv, PlayerEntity player) {
		return new ContainerModular(LibvulpesGuiRegistry.CONTAINER_MODULAR_TILE, id, player, getModules(getModularInvType().ordinal(), player), this, getModularInvType());
	}

	@Override
	public GuiHandler.guiId getModularInvType() {
		return guiId.MODULAR;
	}
}