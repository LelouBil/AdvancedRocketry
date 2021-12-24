package zmaster587.advancedRocketry.api.body;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.Constants;
import zmaster587.advancedRocketry.network.PacketSpaceStationInfo;
import zmaster587.advancedRocketry.network.PacketStationUpdate;
import zmaster587.advancedRocketry.stations.SpaceStation;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;
import zmaster587.libVulpes.util.ZUtils;

import javax.annotation.Nonnull;
import java.util.*;

public class SpaceObjectManager {

	private long nextStationTransitionTick = -1;
	private int nextStationID = 1;
	private final int stationSize;
	//Space station map stuff
	private final HashMap<ResourceLocation, SpaceStation> spaceStationLocations;
	private final HashMap<ResourceLocation, List<SpaceStation>> spaceStationOrbitMap;

	public SpaceObjectManager(int size) {
		spaceStationLocations = new HashMap<>();
		spaceStationOrbitMap = new HashMap<>();
		stationSize = size;
	}

	/**
	 * Gets the SpaceStation object for the provided location
	 * @param location the ResourceLocation ID of the station
	 * @return SpaceStation mapped to that ResourceLocation or null
	 */
	public SpaceStation getSpaceStation(ResourceLocation location) {
		return spaceStationLocations.get(location);
	}

	/**
	 * Gets the space station from the provided BlockPos position
	 * @param position the BlockPos WITHIN THE STATION DIMENSION of the space station to get
	 * @return SpaceStation object at the provided position or null
	 */
	public SpaceStation getSpaceStationFromPosition(@Nonnull BlockPos position) {
		int x = position.getX(); int z = position.getZ();
		x = Math.round((x)/(2f*stationSize));
		z = Math.round((z)/(2f*stationSize));
		int radius = Math.max(Math.abs(x), Math.abs(z));

		int index = (int) Math.pow((2*radius-1),2) + x + radius;

		if(Math.abs(z) != radius) {
			index = (int) Math.pow((2*radius-1),2) + z + radius + (4*radius + 2) - 1;
			if(x > 0) index += 2*radius-1;
		} else if(z > 0)
			index += 2*radius+1;

		return getSpaceStation(new ResourceLocation(Constants.STATION_NAMESPACE, String.valueOf(index)));
	}

	/**
	 * Gets and increments the next free station ID
	 * @return ResourceLocation in the station namespace with the next available integer ID
	 */
	private ResourceLocation getNextStationId() {
		return new ResourceLocation(Constants.STATION_NAMESPACE,String.valueOf(nextStationID++));
	}

	/**
	 * Internal method for registering a space station to the dimension, manager, and location maps
	 * @param station the SpaceStation object to be registered at this location
	 * @param planet the ResourceLocation of the planet the station is to orbit at the beginning
	 * @param location the ResourceLocation of the station's ID
	 */
	private void registerSpaceStation(@Nonnull SpaceStation station, ResourceLocation planet, @Nonnull ResourceLocation location) {
		spaceStationLocations.put(location, station);

		/* Calculate the location of a space station along a square spiral
		 * here the top and bottom(including the corner locations) are filled first then the left and right last
		 *  1 2 3
		 *  7 0 8
		 *  4 5 6
		 */

		int integerID = Integer.parseInt(location.getPath());

		int radius = (int) Math.floor(Math.ceil(Math.sqrt(integerID+1))/2);
		int ringIndex = (int) (integerID-Math.pow((radius*2) - 1,2));
		int x,z;

		if(ringIndex < (radius*2 + 1)*2) {
			x = ringIndex % (radius*2 + 1) - radius;
			z = ringIndex < (radius*2 + 1) ? -radius : radius;
		} else {
			int newIndex = ringIndex - (radius*2 + 1)*2;
			z = newIndex % ((radius-1)*2 + 1) - (radius - 1);
			x = newIndex < ((radius-1)*2 + 1) ? -radius : radius;
		}

		station.setSpawn(new HashedBlockPosition(2*stationSize*x + stationSize/2, 129, 2*stationSize*z + stationSize/2));
		station.setOrbit(planet);
		moveStationToPlanet(station, planet, false);

	}

	/**
	 * Registers a space station with the respective maps on the provided planet, and notifies the client of the station creation
	 * @param station the SpaceStation object to register with the maps
	 * @param planet the ResourceLocation of the planet the station is to orbit
	 */
	public void registerSpaceStation(@Nonnull SpaceStation station, ResourceLocation planet) {
        registerSpaceStation(station, planet, getNextStationId());
		PacketHandler.sendToAll(new PacketSpaceStationInfo(station.getLocation(), station));
	}

	/**
	 * Internal, Client-only method to register a station after the client has been updated via packet by the server
	 * @param station the SpaceStation object to register with the maps
	 * @param planet the ResourceLocation of the planet the station is to orbit
	 * @param location the ResourceLocation the station's ID is to be
	 */
	@OnlyIn(Dist.CLIENT)
	public void registerSpaceStationClient(@Nonnull SpaceStation station, ResourceLocation planet, ResourceLocation location) {
		registerSpaceStation(station, planet, location);
	}

	/**
	 * Remove the station at the provided location from the maps and updates clients of the removed station
	 * @param location the ResourceLocation of the station to remove
	 */
	public void unregisterSpaceStation(ResourceLocation location) {
		spaceStationLocations.remove(location);
		spaceStationOrbitMap.remove(location);
		PacketHandler.sendToAll(new PacketSpaceStationInfo(location, null));
	}

	/**
	 * Gets a list of the stations at the provided planet ResourceLocation
	 * @param planet the ResourceLocation of the planet to try to collect station IDs from
	 * @return a List of the station ResourceLocations
	 */
	public List<SpaceStation> getSpaceStationsForPlanet(ResourceLocation planet) {
		return spaceStationOrbitMap.get(planet);
	}

	/**
	 * Gets whether there are any stations registered as orbiting the warp dimension
	 * @return a boolean for if any stations are currently warping
	 */
	private boolean areShipsInWarp() {
		return spaceStationOrbitMap.get(Constants.WARPDIMID) != null && !spaceStationOrbitMap.get(Constants.WARPDIMID).isEmpty();
	}

	@SubscribeEvent
	public void keepPlayerInBounds(PlayerTickEvent event) {
		if(PlanetManager.spaceDimensionID.equals(ZUtils.getDimensionIdentifier(event.player.world))) {
			SpaceStation station = getSpaceStationFromPosition(event.player.getPosition());
			if (station != null) {
				HashedBlockPosition loc = station.getSpawn();
				//Handle player below Y0
			    if (event.player.getPosY() < 0 && !event.player.world.isRemote) {
					event.player.fallDistance = 0;
					event.player.setMotion(event.player.getMotion().mul(0, 0, 0));
					event.player.setPositionAndUpdate(loc.x, loc.y + 5, loc.z);
					event.player.sendStatusMessage(new StringTextComponent("You wake up finding yourself back on the station"), true);
				}
				//Handle player outside of bounds
				if (event.player.getPosition().distanceSq(loc.x, loc.y, loc.z, false) >= ((ARConfiguration.getCurrentConfig().stationSize.get()/128d)*(ARConfiguration.getCurrentConfig().stationSize.get()/128d))) {
					event.player.setMotion(event.player.getMotion().mul(0, 0, 0));
					event.player.setPositionAndUpdate(loc.x, loc.y + 5, loc.z);
					event.player.sendStatusMessage(new StringTextComponent("You wake up finding yourself back on the station"), true);
				}
			} else {
				event.player.respawnPlayer();
				event.player.sendStatusMessage(new StringTextComponent("You must be on a space station to be in this dimension, and none have been created!"), true);
			}
		}
	}

	@SubscribeEvent
	public void tickWarpShips(TickEvent.ServerTickEvent event) {
		long worldTime = AdvancedRocketry.proxy.getWorldTimeUniversal();
		//If no dim undergoing transition then nextTransitionTick = -1
		if((nextStationTransitionTick != -1 && worldTime >= nextStationTransitionTick && areShipsInWarp()) || (nextStationTransitionTick == -1 && areShipsInWarp())) {
			long newNextTransitionTick = -1;
			for(SpaceStation station : spaceStationOrbitMap.get(Constants.WARPDIMID)) {
				if(station.getTransitionTime() <= AdvancedRocketry.proxy.getWorldTimeUniversal()) {
					moveStationToPlanet(station, station.getDestination(), true);
					spaceStationOrbitMap.get(Constants.WARPDIMID).remove(station);
				} else if(newNextTransitionTick == -1 || station.getTransitionTime() < newNextTransitionTick)
					newNextTransitionTick = station.getTransitionTime();
			}
			nextStationTransitionTick = newNextTransitionTick;
		}
	}

	/**
	 * Transfers the station from its current orbit to the specified planet and optionally updates clients
	 * @param station the SpaceStation object we want to transfer from one orbit to another
	 * @param planet the ResourceLocation that the station should be transferred to
	 * @param update whether to send a packet to update the client as well as fire a fog burst
	 */
	public void moveStationToPlanet(@Nonnull SpaceStation station, ResourceLocation planet, boolean update) {
		//First remove the station and create the needed list if it's null
        spaceStationOrbitMap.get(station.getOrbit()).remove(station);
		spaceStationOrbitMap.computeIfAbsent(planet, k -> new LinkedList<>());

		//Then we add the orbiting body
		if(!spaceStationOrbitMap.get(planet).contains(station)) spaceStationOrbitMap.get(planet).add(station);
		station.setOrbit(planet);

		//Then we do the final actual move
		if(update) {
			PacketHandler.sendToAll(new PacketStationUpdate(station, PacketStationUpdate.Type.ORBIT_UPDATE));
			AdvancedRocketry.proxy.fireFogBurst(station);
		}
	}

	/**
	 * Begins the transfer process for a warp jump of a station to another planet
	 * @param station the SpaceStation object we want to transfer from one orbit to another
	 * @param planet the ResourceLocation that the station should be transferred to
	 * @param timeDelta the delta time for when the station should arrive at its destination
	 */
	public void beginStationMovementToPlanet(@Nonnull SpaceStation station, ResourceLocation planet, int timeDelta) {
		//First remove the station and create the needed list if it's null
		if(!Constants.WARPDIMID.equals(station.getOrbit())) spaceStationOrbitMap.get(station.getOrbit()).remove(station);
		spaceStationOrbitMap.computeIfAbsent(Constants.WARPDIMID, k -> new LinkedList<>());

		//Then we add the orbiting body
		if(!spaceStationOrbitMap.get(Constants.WARPDIMID).contains(station)) spaceStationOrbitMap.get(Constants.WARPDIMID).add(station);
		station.setOrbit(Constants.WARPDIMID);

		//Then we calculate the next transition tick needed
		nextStationTransitionTick = (long)(ARConfiguration.getCurrentConfig().travelTimeMultiplier.get()*timeDelta + AdvancedRocketry.proxy.getWorldTimeUniversal());
		station.beginTransition(nextStationTransitionTick);

		//Then we're on our way, so we fire a fog burst
		AdvancedRocketry.proxy.fireFogBurst(station);
	}

	public void onServerStopped() {
		spaceStationLocations.clear();
		spaceStationOrbitMap.clear();
		nextStationTransitionTick = -1;
		nextStationID = 1;
	}



	public void writeToNBT(CompoundNBT nbt) {
		nbt.putLong("nexttransitiontick", nextStationTransitionTick);
		nbt.putInt("nextstationid", nextStationID);

		ListNBT nbtList = new ListNBT();
		for (SpaceStation station : spaceStationLocations.values()) {
			CompoundNBT nbtTag = new CompoundNBT();
			station.writeToNbt(nbtTag);
			nbtList.add(nbtTag);
		}
		nbt.put("spacestations", nbtList);
	}

	public void readFromNBT(CompoundNBT nbt) {
		nextStationTransitionTick = nbt.getLong("nexttransitiontick");
		nextStationID = nbt.getInt("nextstationid");

		ListNBT list = nbt.getList("spacestations", net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND);
		for(int i = 0; i < list.size(); i++){
			CompoundNBT tag = list.getCompound(i);
			SpaceStation station = new SpaceStation(new ResourceLocation(tag.getString("location")));
			station.readFromNbt(tag);
			registerSpaceStation(station, station.getOrbit(), station.getLocation());
		}
	}
}
