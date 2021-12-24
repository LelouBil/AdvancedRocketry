package zmaster587.advancedRocketry.stations;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Dimension;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.Constants;
import zmaster587.advancedRocketry.api.body.SpaceObjectManager;
import zmaster587.advancedRocketry.api.body.planet.PlanetProperties;
import zmaster587.advancedRocketry.api.body.solar.StellarBody;
import zmaster587.advancedRocketry.api.body.PlanetManager;
import zmaster587.advancedRocketry.api.body.station.IWarpship;
import zmaster587.advancedRocketry.inventory.IPlanetDefiner;
import zmaster587.advancedRocketry.network.PacketSpaceStationInfo;
import zmaster587.advancedRocketry.network.PacketStationUpdate;
import zmaster587.advancedRocketry.network.PacketStationUpdate.Type;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;

import java.util.*;

public class SpaceStation extends StationBase implements IPlanetDefiner, IWarpship {

	private static final int MAX_FUEL = 1000;

	//Warp Properties
	private Set<ResourceLocation> knownPlanetList;
	private int fuel;
	private long transition;
	private ResourceLocation destination;
	private List<HashedBlockPosition> cores;

	public SpaceStation(ResourceLocation location) {
		super(location, 0);

		cores = new LinkedList<>();
		transition = -1;
		destination = Dimension.OVERWORLD.getLocation();
		knownPlanetList = new HashSet<>();
	}

	/**
	 * All the standard getters & setters for the station properties
	 */

	public ResourceLocation getDestination() {
		return destination;
	}

	public void setDestination(ResourceLocation location) {
		destination = location;
		if(EffectiveSide.get().isServer()) PacketHandler.sendToAll(new PacketStationUpdate(this, PacketStationUpdate.Type.DEST_ORBIT_UPDATE));
	}

	public long getTransitionTime() {
		return transition;
	}
	public void beginTransition(long time) {
		if(time > 0) transition = time;
	}

	public boolean isWarping() {
		return getOrbit().equals(Constants.WARPDIMID);
	}
	public List<HashedBlockPosition> getWarpCoreLocations() {
		return cores;
	}
	public void addWarpCoreLocation(HashedBlockPosition position) {
		cores.add(position);
	}
	public void removeWarpCoreLocation(HashedBlockPosition position) {
		cores.remove(position);
	}
	public boolean hasUsableWarpCore() {
		return cores.size() != 0 && !isWarping() && !getOrbit().equals(getDestination());
	}

	public int getFuelAmount() {
		return fuel;
	}
	public int getMaxFuelAmount() {
		return MAX_FUEL;
	}
	public void setFuelAmount(int amt) {
		fuel = amt;
	}
	public int addFuel(int amt) {
		if(amt <= 0) return amt;
		int oldFuelAmt = fuel;
		fuel = Math.min(fuel + amt, MAX_FUEL);
		if(EffectiveSide.get().isServer()) PacketHandler.sendToAll(new PacketStationUpdate(this, Type.FUEL_UPDATE));
		return fuel - oldFuelAmt;
	}
	public int useFuel(int amt) {
		if(amt > getFuelAmount()) return 0;
		fuel -= amt;
		if(EffectiveSide.get().isServer()) PacketHandler.sendToAll(new PacketStationUpdate(this, Type.FUEL_UPDATE));
		return amt;
	}

	/**
	 * Nonstandard getters & setters for the station & planet definer properties
	 */

	@Override
	public void setAltitude(int altitude) {
		if (!getAnchored()) super.setAltitude(MathHelper.clamp(altitude, 100, 40000));
	}

	@Override
	public void setOrbit(ResourceLocation location) {
		if(this.getOrbit().equals(location)) return;
		super.setOrbit(location);
		if(!Constants.WARPDIMID.equals(location)) destination = location;
	}

	@Override
	public boolean isPlanetKnown(PlanetProperties properties) {
		return !ARConfiguration.getCurrentConfig().planetsMustBeDiscovered.get() || knownPlanetList.contains(properties.getLocation().dimension) || PlanetManager.getInstance().knownPlanets.contains(properties.getLocation().dimension);
	}

	@Override
	public boolean isStarKnown(StellarBody body) {
		return true;
	}

	public void discoverPlanet(ResourceLocation pid) {
		knownPlanetList.add(pid);
		PacketHandler.sendToAll(new PacketSpaceStationInfo(getLocation(), this));
	}



	@Override
	public void writeToNbt(CompoundNBT nbt) {
		super.writeToNbt(nbt);

		nbt.putString("destination", destination.toString());
		nbt.putInt("fuel", fuel);
		if(transition > -1) nbt.putLong("transition", transition);

		//Set known planets
		ListNBT planetList = new ListNBT();
		for(ResourceLocation i : knownPlanetList)
			planetList.add(StringNBT.valueOf(i.toString()));
		nbt.put("knownplanets", planetList);

		//Set warp cores
		ListNBT list = new ListNBT();
		for(HashedBlockPosition pos : this.cores) {
			CompoundNBT tag = new CompoundNBT();
			tag.putIntArray("pos", new int[] {pos.x, pos.y, pos.z});
			list.add(tag);
		}
		nbt.put("cores", list);

	}

	@Override
	public void readFromNbt(CompoundNBT nbt) {
		super.readFromNbt(nbt);

		destination = new ResourceLocation(nbt.getString("destination"));
		fuel = nbt.getInt("fuel");
		if(nbt.contains("transition")) transition = nbt.getLong("transition");

		//Get known planets
		ListNBT planetList = nbt.getList("knownplanets", NBT.TAG_STRING);
		for( int i =0; i < planetList.size(); i++)
			knownPlanetList.add( new ResourceLocation(planetList.getString(i)));

		//Get warp cores
		ListNBT list = nbt.getList("cores", NBT.TAG_COMPOUND);
		cores.clear();
		for(int i = 0; i < list.size(); i++) {
			CompoundNBT tag = list.getCompound(i);
			int[] posInt = tag.getIntArray("pos");
			HashedBlockPosition pos = new HashedBlockPosition(posInt[0], posInt[1], posInt[2]);
			cores.add(pos);
		}
	}
}
