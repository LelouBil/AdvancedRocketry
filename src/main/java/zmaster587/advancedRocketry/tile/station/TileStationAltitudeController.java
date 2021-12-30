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
import zmaster587.advancedRocketry.api.AdvancedRocketryManagers;
import zmaster587.advancedRocketry.api.AdvancedRocketryTileEntityType;
import zmaster587.advancedRocketry.api.body.station.IStation;
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
import zmaster587.libVulpes.util.ZUtils.RedstoneState;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedList;
import java.util.List;

public class TileStationAltitudeController extends TileEntity implements IModularInventory, ITickableTileEntity, INetworkMachine, ISliderBar, IButtonInventory, IComparatorOverride {

	private final ModuleText altitude, acceleration, target;
	private final ModuleRedstoneOutputButton redstone;
	private RedstoneState state = ZUtils.RedstoneState.OFF;

	public TileStationAltitudeController() {
		super(AdvancedRocketryTileEntityType.TILE_ALT_CONTROLLER);
		altitude = new ModuleText(6, 15, "Altitude: ", 0xaa2020);
		acceleration = new ModuleText(6, 25, LibVulpes.proxy.getLocalizedString("msg.stationaltctrl.maxaltrate"), 0xaa2020);
		target = new ModuleText(6, 35, LibVulpes.proxy.getLocalizedString("msg.stationaltctrl.tgtalt"), 0x202020);
		redstone = new ModuleRedstoneOutputButton(174, 4, "", this);
	}

	@Override
	public List<ModuleBase> getModules(int id, PlayerEntity player) {
		List<ModuleBase> modules = new LinkedList<>();
		modules.add(altitude);
		modules.add(acceleration);
		modules.add(target);

		modules.add(new ModuleSlider(6, 60, 0, TextureResources.doubleWarningSideBarIndicator, this));
		modules.add(redstone);

		updateText();
		return modules;
	}

	@Override
	public void onInventoryButtonPressed(ModuleButton buttonId) {
		if(buttonId == redstone) {
			state = redstone.getState();
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

		this.state = RedstoneState.values()[nbt.getByte("redstone")];
		redstone.setRedstoneState(this.state);
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
			state = RedstoneState.values()[nbt.getByte("state")];
			redstone.setRedstoneState(state);
		}
	}
	
	private void updateText() {
		if(world.isRemote) {
			SpaceStation station = AdvancedRocketryManagers.station.getSpaceStationFromPosition(pos);
			if(station != null) {
				altitude.setText(String.format("%s %.0dKm",LibVulpes.proxy.getLocalizedString("msg.stationaltctrl.alt"), station.getAltitude()));
				acceleration.setText(String.format("%s%.1f", LibVulpes.proxy.getLocalizedString("msg.stationaltctrl.maxaltrate"), 7200D * StationBase.MAX_ACCELERATION));
				target.setText(String.format("%s %d", LibVulpes.proxy.getLocalizedString("msg.stationaltctrl.tgtalt"), station.getTargetAltitude()));
			}
		}
	}

	@Override
	public void tick() {
		if(PlanetManager.isBodyStation(ZUtils.getDimensionIdentifier(this.world))) {
			if(!world.isRemote) {
				SpaceStation station = AdvancedRocketryManagers.station.getSpaceStationFromPosition(pos);
				if(station != null) {
					if (redstone.getState() == RedstoneState.ON)
					    station.setTargetAltitude(world.getStrongPower(pos) * 2500);
					else if (redstone.getState() == RedstoneState.INVERTED)
						station.setTargetAltitude((15 - world.getStrongPower(pos)) * 2500);

					int altitude = station.getAltitude();
					double acc = 0.1*(getTotalProgress(0) - altitude + 1)/(float)getTotalProgress(0);

					double difference = station.getTargetAltitude() - altitude;

					if(difference != 0) {
						double finalVel = altitude;
						if(difference < 0) {
							finalVel = altitude + Math.max(difference, -acc);
						}
						else if(difference > 0) {
							finalVel = altitude + Math.min(difference, acc);
						}

						station.setAltitude((int)finalVel);
						if(!world.isRemote) {
							PacketHandler.sendToAll(new PacketStationUpdate(station, PacketStationUpdate.Type.ALTITUDE_UPDATE));
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
		return "block.advancedrocketry.altitudecontroller";
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
			AdvancedRocketryManagers.station.getSpaceStationFromPosition(this.pos).setTargetAltitude(progress + 100);
		}
	}

	@Override
	public int getProgress(int id) {
		if (PlanetManager.isBodyStation(ZUtils.getDimensionIdentifier(this.world)) && AdvancedRocketryManagers.station.getSpaceStationFromPosition(pos) != null)
		    return AdvancedRocketryManagers.station.getSpaceStationFromPosition(pos).getTargetAltitude();
		else
			return 0;
	}

	@Override
	public int getTotalProgress(int id) {
		return 39000;
	}

	@Override
	public void setTotalProgress(int id, int progress) { }

	@Override
	public int getComparatorOverride() {
		if(PlanetManager.isBodyStation(ZUtils.getDimensionIdentifier(world))) {
			if (!world.isRemote) {
				IStation spaceObject = AdvancedRocketryManagers.station.getSpaceStationFromPosition(pos);
				if (spaceObject != null) {
                    return spaceObject.getAltitude()/2500;
				}
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
