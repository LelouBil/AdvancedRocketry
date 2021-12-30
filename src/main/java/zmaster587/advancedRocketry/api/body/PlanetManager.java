package zmaster587.advancedRocketry.api.body;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Dimension;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.*;
import zmaster587.advancedRocketry.api.body.planet.PlanetProperties;
import zmaster587.advancedRocketry.api.body.solar.StellarBody;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.api.body.station.IStation;
import zmaster587.advancedRocketry.network.PacketSatellite;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;

import javax.annotation.Nonnull;
import java.util.*;


public class PlanetManager {

	//Stat tracking & constants
	public static boolean hasReachedWarp;
	public static ResourceLocation spaceDimensionID = new ResourceLocation(Constants.modId, "space");

	//Lists of all the bodies/locations in space and what they're tied to
	//Bodies
	private final HashMap<ResourceLocation, StellarBody> stars;
	private final HashMap<ResourceLocation, PlanetProperties> planets;
	public Set<ResourceLocation> knownPlanets;
	//Non-bodies
	private final HashMap<ResourceLocation, HashMap<Long,SatelliteBase>> satellites;
	private final HashMap<ResourceLocation, HashSet<HashedBlockPosition>> beaconLocations;
	private static long nextSatelliteId;



	public static PlanetManager getInstance() {
		return AdvancedRocketry.proxy.getDimensionManager();
	}

	public PlanetManager(HashMap<ResourceLocation, PlanetProperties> planets, HashMap<ResourceLocation, StellarBody> stars) {
		this.planets = planets;
		this.stars = stars;
		knownPlanets = new HashSet<>();

		this.satellites = new HashMap<>();
		this.beaconLocations = new HashMap<>();
	}



	public HashMap<ResourceLocation, StellarBody> getStars() {
		return stars;
	}

	public Set<ResourceLocation> getStarIDs() {
		return stars.keySet();
	}

	public StellarBody getStar(ResourceLocation star) {
		return stars.get(star);
	}

	public HashMap<ResourceLocation, PlanetProperties> getPlanets() {
		return planets;
	}

	public Set<ResourceLocation> getPlanetIDs() {
		return planets.keySet();
	}

	public PlanetProperties getPlanetProperties(ResourceLocation planet) {
		return planets.get(planet) == null && !planet.equals(PlanetManager.spaceDimensionID) ? planets.get(Dimension.OVERWORLD.getLocation()): planets.get(planet);
	}

	public PlanetProperties getPlanetPropertiesExact(ResourceLocation planet, BlockPos pos) {
		if(isBodyStation(planet)) {
			IStation spaceObject = AdvancedRocketryManagers.station.getSpaceStationFromPosition(pos);
			if(spaceObject != null)
				return PlanetManager.getInstance().getPlanetProperties(spaceObject.getOrbit());
			else
				return null;
		} else
			return getInstance().getPlanetProperties(planet);
	}

	public GenericProperties getGenericProperties(ResourceLocation planet, BlockPos pos) {
		if(isBodyStation(planet)) {
			IStation spaceObject = AdvancedRocketryManagers.station.getSpaceStationFromPosition(pos);
			if(spaceObject != null)
				return spaceObject.getPropertiesCopy();
			else
				return null;
		} else
			return getInstance().getPlanetProperties(planet);
	}

	public HashMap<Long, SatelliteBase> getSatellites(ResourceLocation planet) {
	    return satellites.get(planet);
	}

	public SatelliteBase getSatellite(ResourceLocation planet, long id) {
		return getSatellites(planet).get(id);
	}

	public void addSatelliteServer(@Nonnull SatelliteBase satellite, ResourceLocation world) {
		satellites.get(world).put(satellite.getId(), satellite);
		satellite.setDimensionId(world);
		PacketHandler.sendToAll(new PacketSatellite(satellite));
	}

	public void addSatelliteClient(@Nonnull SatelliteBase satellite) {
		satellites.get(satellite.getDimensionId()).put(satellite.getId(), satellite);
	}

	public long getNextSatelliteId() {
	    return nextSatelliteId++;
	}

	public void tickSatellites() {
		for(ResourceLocation planet : PlanetManager.getInstance().getPlanetIDs()) {
			for(long id : PlanetManager.getInstance().getSatellites(planet).keySet()) {
                PlanetManager.getInstance().getSatellite(planet, id).tickEntity();
			}
		}
	}

	public HashSet<HashedBlockPosition> getBeaconLocations(ResourceLocation planet) {
		return beaconLocations.get(planet);
	}

	public static boolean isBodyStation(ResourceLocation planet) {
		return planet != null && planet.equals(PlanetManager.spaceDimensionID);
	}

	public static boolean hasSurface(ResourceLocation planet) {
		return !PlanetManager.getInstance().getPlanetProperties(planet).getGaseous();
	}
}
