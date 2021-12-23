package zmaster587.advancedRocketry.stations;

import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.api.body.PlanetManager;
import zmaster587.advancedRocketry.api.body.station.*;
import zmaster587.advancedRocketry.network.PacketSpaceStationInfo;
import zmaster587.advancedRocketry.util.StationLandingLocation;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;

import java.util.*;

public class StationBase extends ModuleUnpackDestination implements IStation, ILandingPadHolder {

	public static final double MAX_ACCELERATION = 0.000005d;
	protected StationProperties properties;

	public StationBase(ResourceLocation location, int pressure) {
		properties = new StationProperties(location, pressure);
	}

	/**
	 * All the standard getters for station properties
	 */

	public StationProperties getPropertiesCopy() { return properties.clone(); }
	public ResourceLocation getLocation() { return properties.location; }

	public int getAltitude() { return properties.altitude; }
	public int[] getRotation() { return properties.rotation; }
	public ResourceLocation getOrbit() { return properties.orbit; }
	public boolean getAnchored() { return properties.anchored; }

	public HashedBlockPosition getSpawn() { return properties.spawn; }
	public List<StationLandingLocation> getLandingPads() { return properties.pads; }
	public HashMap<HashedBlockPosition, String> getGantries() { return properties.gantries; }

	//Transformable properties exposed to the user
	public double getGravitation() { return properties.gravitation; }
	public Direction getDirection() { return properties.direction; }
	public int[] getOmega() { return properties.omega; }
	public int getTargetAltitude() { return properties.targetAltitude; }
	public double getTargetGravitation() { return properties.targetGravitation; }
	public int[] getTargetOmega() { return properties.targetOmega; }

	/**
	 * All the standard setters for station properties
	 */

	public void setAltitude(int altitude) { properties.altitude = altitude; }
	public void setRotation(int[] rotation) { properties.rotation = rotation; }
	public void setOrbit(ResourceLocation orbit) { properties.orbit = orbit; }
	public void setAnchored(boolean anchored) { properties.anchored = anchored; }

	public void setSpawn(HashedBlockPosition spawn) { properties.spawn = spawn; }
	public void addLandingPad(BlockPos pos, String name) {
		StationLandingLocation position = new StationLandingLocation(new HashedBlockPosition(pos), name);
		if(!getLandingPads().contains(position)) {
			position.setOccupied(false);
			getLandingPads().add(position);
		}
	}
	public void removeLandingPad(BlockPos pos) {
		HashedBlockPosition position = new HashedBlockPosition(pos);
		getLandingPads().removeIf(loc -> loc.getPos().equals(position));
	}
	public void setLandingPadFull(BlockPos pos, boolean full) {
		StationLandingLocation position = new StationLandingLocation(new HashedBlockPosition(pos));
		for(StationLandingLocation loc : getLandingPads()) {
			if(loc.equals(position)) loc.setOccupied(full);
		}
	}
	public void setLandingPadName(World world, HashedBlockPosition pos, String name) {
		StationLandingLocation loc = getLandingPadAtLocation(pos);
		if(loc != null) loc.setName(name);
		if(!world.isRemote) PacketHandler.sendToAll(new PacketSpaceStationInfo(properties.location, this));
	}
	public void setLandingPadAutomatic(BlockPos pos, boolean status) {
		HashedBlockPosition position = new HashedBlockPosition(pos);
		for (StationLandingLocation loc : getLandingPads()) {
			if (loc.getPos().equals(position)) loc.setAllowedForAutoLand(status);
		}
	}
	public HashedBlockPosition getNextAutomaticLandingPad(boolean commit) {
		for(StationLandingLocation pos : getLandingPads()) {
			if(!pos.getOccupied() && pos.getAllowedForAutoLand()) {
				if(commit) pos.setOccupied(true);
				return pos.getPos();
			}
		}
		return null;
	}
	public StationLandingLocation getLandingPadAtLocation(HashedBlockPosition pos) {
		for(StationLandingLocation loc : getLandingPads()) {
			if(loc.getPos().equals(pos)) return loc;
		}
		return null;
	}
	public void addDockingPosition(BlockPos pos, String str) {
		HashedBlockPosition pos2 = new HashedBlockPosition(pos);
		getGantries().put(pos2, str);
	}
	public void removeDockingPosition(BlockPos pos) {
		HashedBlockPosition pos2 = new HashedBlockPosition(pos);
		getGantries().remove(pos2);
	}

	public void setGravitation(double gravitation) { properties.gravitation = gravitation; }
	public void setDirection(Direction direction) { properties.direction = direction; }
	public void setOmega(int[] omega) { properties.omega = omega; }
	public void setTargetAltitude(int altitude) { properties.targetAltitude = altitude; }
	public void setTargetGravitation(double gravitation) { properties.targetGravitation = gravitation; }
	public void setTargetOmega(int[] omega) { properties.targetOmega = omega; }

    /**
     * Nonstandard getters for station properties
     */

	public double getInsolation() {
		return PlanetManager.getInstance().getPlanetProperties(properties.orbit).getInsolationWithoutAtmosphere();
	}
	public boolean isBottomAbovePlanet() {
		return Math.abs(rotation[0] - (int)rotation[0] - 0.5) > 0.40 && Math.abs(rotation[2] - (int)rotation[2] - 0.5) > 0.40;
	}
	public boolean wouldStationBreakTether() {
		return getOmega()[0] != 0 || getOmega()[2] != 0 || getTargetOmega()[0] != 0 || getTargetOmega()[2] != 0;
	}

	/**
	 * Unpack module onto the given coordinates
	 * @param chunk the IStorageChunk the blocks to unpack are contained in
	 */
	public void unpackModule(IStorageChunk chunk) {
		super.unpackModule(chunk, !properties.created, properties.spawn, properties.gantries);
		if (!properties.created) properties.created = true;
	}
}
