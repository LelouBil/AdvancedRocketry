package zmaster587.advancedRocketry.tile.station;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.AdvancedRocketryManagers;
import zmaster587.advancedRocketry.api.AdvancedRocketryTileEntityType;
import zmaster587.advancedRocketry.api.body.PlanetManager;
import zmaster587.advancedRocketry.inventory.TextureResources;
import zmaster587.advancedRocketry.network.PacketStationUpdate;
import zmaster587.advancedRocketry.stations.SpaceStation;
import zmaster587.advancedRocketry.stations.StationBase;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.api.LibvulpesGuiRegistry;
import zmaster587.libVulpes.inventory.ContainerModular;
import zmaster587.libVulpes.inventory.GuiHandler;
import zmaster587.libVulpes.inventory.GuiHandler.guiId;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.network.PacketMachine;
import zmaster587.libVulpes.tile.IComparatorOverride;
import zmaster587.libVulpes.util.INetworkMachine;
import zmaster587.libVulpes.util.ZUtils;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedList;
import java.util.List;

public class TileStationGravityController extends TileEntity implements IModularInventory, ITickableTileEntity, INetworkMachine, ISliderBar, IButtonInventory, IComparatorOverride {

	private static int minimum = 10;

	private final ModuleText gravity, acceleration, target;
	private final ModuleRedstoneOutputButton redstoneControl;
	private ZUtils.RedstoneState state = ZUtils.RedstoneState.OFF;

	public TileStationGravityController() {
		super(AdvancedRocketryTileEntityType.TILE_STATION_GRAVITY_CONTROLLER);
		gravity = new ModuleText(6, 15, LibVulpes.proxy.getLocalizedString("msg.stationgravctrl.alt"), 0xaa2020);
		acceleration = new ModuleText(6, 25, LibVulpes.proxy.getLocalizedString("msg.stationgravctrl.maxaltrate"), 0xaa2020);
		target = new ModuleText(6, 35, LibVulpes.proxy.getLocalizedString("msg.stationgravctrl.tgtalt"), 0x202020);
		
		minimum = ARConfiguration.getCurrentConfig().allowZeroGSpacestations.get() ? 0 : 10;
		redstoneControl = new ModuleRedstoneOutputButton(174, 4, "", this);
	}

	@Override
	public List<ModuleBase> getModules(int id, PlayerEntity player) {
		List<ModuleBase> modules = new LinkedList<>();
		modules.add(gravity);
		modules.add(acceleration);
		modules.add(target);

		modules.add(redstoneControl);
		modules.add(new ModuleSlider(6, 60, 0, TextureResources.doubleWarningSideBarIndicator, this));

		updateText();
		return modules;
	}

	@Override
	public void onInventoryButtonPressed(ModuleButton buttonId) {
		if(buttonId == redstoneControl) {
			state = redstoneControl.getState();
			PacketHandler.sendToServer(new PacketMachine(this, (byte)2));
		} else
			PacketHandler.sendToServer(new PacketMachine(this, (byte)100) );
	}
	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		CompoundNBT nbt = write(new CompoundNBT());
		return new SUpdateTileEntityPacket(pos, 0, nbt);
	}

	@Nonnull
	@Override
	public CompoundNBT getUpdateTag() {
		return write(new CompoundNBT());
	}

	@Override
	@ParametersAreNonnullByDefault
	public void read(BlockState state, CompoundNBT nbt) {
		super.read(state, nbt);

		this.state = ZUtils.RedstoneState.values()[nbt.getByte("redstone")];
		redstoneControl.setRedstoneState(this.state);
	}

	@Nonnull
	@Override
	@ParametersAreNonnullByDefault
	public CompoundNBT write(CompoundNBT nbt) {
		super.write(nbt);
		nbt.putByte("redstone", (byte) state.ordinal());
		return nbt;
	}

	@Override
	public void writeDataToNetwork(PacketBuffer out, byte id) {
		if(id == 2) out.writeByte(state.ordinal());
	}

	@Override
	public void readDataFromNetwork(PacketBuffer in, byte packetId, CompoundNBT nbt) {
		if(packetId == 1) {
			nbt.putLong("id", in.readLong());
		} else if(packetId == 2) {
			nbt.putByte("state", in.readByte());
		}
	}

	@Override
	public void useNetworkData(PlayerEntity player, Dist side, byte id, CompoundNBT nbt) {
		if(id == 2) {
			state = ZUtils.RedstoneState.values()[nbt.getByte("state")];
			redstoneControl.setRedstoneState(state);
		}
	}
	
	private void updateText() {
		if(world.isRemote) {
			SpaceStation station = AdvancedRocketryManagers.station.getSpaceStationFromPosition(pos);
			if(station != null) {
				gravity.setText(String.format("%s%.2f", LibVulpes.proxy.getLocalizedString("msg.stationgravctrl.alt"), station.getGravitation()*100));
				acceleration.setText(String.format("%s%.1f",LibVulpes.proxy.getLocalizedString("msg.stationgravctrl.maxaltrate"), 7200D* StationBase.MAX_ACCELERATION));
				target.setText(String.format("%s%f", LibVulpes.proxy.getLocalizedString("msg.stationgravctrl.tgtalt"), station.getTargetGravitation()*100));
			}
		}
	}

	@Override
	public void tick() {
		if(PlanetManager.isBodyStation(ZUtils.getDimensionIdentifier(this.world))) {
			if(!world.isRemote) {
				SpaceStation station = AdvancedRocketryManagers.station.getSpaceStationFromPosition(pos);
				if(station != null) {
					if (redstoneControl.getState() == ZUtils.RedstoneState.ON)
						station.setTargetGravitation(world.getStrongPower(pos) * 6.7);
					else if (redstoneControl.getState() == ZUtils.RedstoneState.INVERTED)
						station.setTargetGravitation((15 - world.getStrongPower(pos)) * 6.7);

					int targetMultiplier = (ARConfiguration.getCurrentConfig().allowZeroGSpacestations.get()) ? ((SpaceStation) spaceObject).targetGravity : Math.max(11, ((SpaceStation) spaceObject).targetGravity);
					double targetGravity = targetMultiplier/100D;
					double angVel = spaceObject.getProperties().getGravitationalMultiplier();
					double acc = 0.001;

					double difference = targetGravity - angVel;

					if(Math.abs(difference) >= 0.001) {
						double finalVel = angVel;
						if(difference < 0) {
							finalVel = angVel + Math.max(difference, -acc);
						}
						else if(difference > 0) {
							finalVel = angVel + Math.min(difference, acc);
						}

						spaceObject.getProperties().setGravitationalMultiplier((float)finalVel);
						if(!world.isRemote) {
							PacketHandler.sendToAll(new PacketStationUpdate(spaceObject, PacketStationUpdate.Type.DIM_PROPERTY_UPDATE));
							markDirty();
						} else
							updateText();
					}
				}
			} else
				updateText();
		}
	}
	@Override
	public String getModularInventoryName() {
		return "block.advancedrocketry.gravitycontroller";
	}

	@Override
	public boolean canInteractWithContainer(PlayerEntity entity) {
		return true;
	}

	@Override
	public float getNormallizedProgress(int id) {
		return getProgress(0)/(float)getTotalProgress(0);
	}

	@Override
	public void setProgress(int id, int progress) {
		if (AdvancedRocketryManagers.station.getSpaceStationFromPosition(this.pos) != null) {
			AdvancedRocketryManagers.station.getSpaceStationFromPosition(this.pos).setTargetGravitation((progress + minimum)/100d);
		}
	}

	@Override
	public int getProgress(int id) {
		if (PlanetManager.isBodyStation(ZUtils.getDimensionIdentifier(this.world)) && AdvancedRocketryManagers.station.getSpaceStationFromPosition(pos) != null)
			return (int) AdvancedRocketryManagers.station.getSpaceStationFromPosition(pos).getTargetGravitation()*100 - minimum;
		else
			return 0;
	}

	@Override
	public int getTotalProgress(int id) {
		return 100 - minimum;
	}

	@Override
	public void setTotalProgress(int id, int progress) { }

	@Override
	public int getComparatorOverride() {
		if(PlanetManager.isBodyStation(ZUtils.getDimensionIdentifier(world))) {
			if (!world.isRemote) {
				SpaceStation station = AdvancedRocketryManagers.station.getSpaceStationFromPosition(pos);
				if (station != null) return (int)(station.getGravitation() * 15);
			}
		}
		return 0;
	}

	@Override
	public void setProgressByUser(int id, int progress) {
		setProgress(id, progress);
		PacketHandler.sendToServer(new PacketMachine(this, (byte)0));
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
