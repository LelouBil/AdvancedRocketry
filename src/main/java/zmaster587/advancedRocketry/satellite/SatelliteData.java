package zmaster587.advancedRocketry.satellite;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerListener;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.DataStorage;
import zmaster587.advancedRocketry.api.SatelliteRegistry;
import zmaster587.advancedRocketry.api.satellite.IDataHandler;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.api.satellite.SatelliteProperties;
import zmaster587.advancedRocketry.util.IDataInventory;
import zmaster587.libVulpes.util.ZUtils;

import javax.annotation.Nonnull;

public abstract class SatelliteData extends SatelliteBase {
	DataStorage data;
	long lastActionTime, prevLastActionTime;
	int collectionTime;
	int powerConsumption;

	public SatelliteData() {
        super();
		powerConsumption = Math.min(160, getPowerPerTick());
		collectionTime =  (int) (200 / Math.sqrt(0.1 * (powerConsumption - 5)));
	}

	@Override
	public String getInfo(World world) {
		//tiles dont update unless ppl reopen
		return "Power: " + battery.getUniversalEnergyStored() + "/" + battery.getMaxEnergyStored() + "\nData Storage: " + ZUtils.formatNumber(data.getMaxData()) +
				"\nData: " + ZUtils.formatNumber(data.getData());
	}

	@Override
	public boolean acceptsItemInConstruction(@Nonnull ItemStack item) {
		int flag = SatelliteRegistry.getSatelliteProperty(item).getPropertyFlag();

		return super.acceptsItemInConstruction(item) || SatelliteProperties.Property.DATA.isOfType(flag);
	}

	@Override
	public void setProperties(@Nonnull ItemStack satelliteProperties) {
		super.setProperties(satelliteProperties);
		data.setMaxData(this.satelliteProperties.getMaxDataStorage());
	}


	@Override
	public boolean performAction(PlayerEntity player, World world, BlockPos pos) {
		//Grab the tile to sync with
		TileEntity tile = world.getTileEntity(pos);
        //Remove data from the satellite and add it to the buffer of said tile, provided the tile can accept it
		if(tile instanceof IDataHandler) {
			IDataInventory dataInv = (IDataInventory)tile;

			data.removeData(dataInv.addData(data.getData(), data.getDataType(), Direction.DOWN, true), true);
		}

		return false;
	}

	private int getDataCreated() {
		//If collection time is somehow 0, fix it before it causes problems
		if (collectionTime == 0)
			collectionTime = 200;

		if (data.getMaxData() > data.getData()) {
			//Provided the satellite has enough power, produce some data every 200t (10 seconds), modified by the amount of power available to the satellite, but the power much be over or equal to 10
			//Think of it like scanning takes < 5 FE/t, and base consumption is 5. So you need more than 10 FE/t, and with more power you can scan better
			//Power consumption maxes out at 160 FE/t, or four large solar panels. This corresponds to a 4x reduction in data collection time
			battery.extractEnergy(powerConsumption - 5, false);
			//Actually collect the unit of data
			if (AdvancedRocketry.proxy.getWorldTimeUniversal() % collectionTime == 0 && satelliteProperties.getPowerGeneration() >= 10) {
				return 1;
			}
		}
		return 0;
	}

	@Override
	public void tickEntity() {
		//Standard power stuff
		super.tickEntity();
		//We have a special broadband high-capacity data link, so it needs an extra 4 FE/t to keep open - subtract these 4 here
		battery.extractEnergy(4, false);
		//Add data to the buffer, if the satellite has enough power
		data.addData(getDataCreated(), data.getDataType(), true);
	}

	@Override
	public void setDimensionId(World world) {
		//TODO: send packet on orbit reached
		super.setDimensionId(world);

		lastActionTime = world.getGameTime();
	}

	@Override
	public void readFromNBT(CompoundNBT nbt) {
		super.readFromNBT(nbt);

		this.data.readFromNBT(nbt.getCompound("data"));
		lastActionTime = nbt.getLong("lastActionTime");
		collectionTime = nbt.getInt("collectionMultiplier");
	}

	@Override
	public void writeToNBT(CompoundNBT nbt) {
		super.writeToNBT(nbt);

		CompoundNBT data = new CompoundNBT();
		this.data.writeToNBT(data);
		nbt.put("data", data);

		nbt.putLong("lastActionTime",lastActionTime);
		nbt.putInt("collectionMultiplier", collectionTime);
	}

	@Override
	public int numberChangesToSend() {
		return 4;
	}

	@Override
	public void onChangeReceived(int slot, int value) {
		lastActionTime = ( lastActionTime & ( ~(0xffffL << (slot * 16) ) ) ) | ( (long) value << (slot * 16) );
	}

	@Override
	public void sendChanges(Container container, IContainerListener crafter, int variableId, int localId) {
		crafter.sendWindowProperty(container, variableId, (short)(( lastActionTime >>> (localId * 16) ) & 0xffff));

		if(localId == 3)
			prevLastActionTime=lastActionTime;
	}

	@Override
	public boolean isUpdateRequired(int localId) {
		return lastActionTime != prevLastActionTime;
	}
}
