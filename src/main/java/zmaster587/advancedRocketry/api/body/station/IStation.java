package zmaster587.advancedRocketry.api.body.station;

import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.api.body.PlanetManager;
import zmaster587.advancedRocketry.network.PacketSpaceStationInfo;
import zmaster587.advancedRocketry.util.StationLandingLocation;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;

import java.util.HashMap;
import java.util.List;

public interface IStation {

	/**
	 * Return a copy of the basic properties of the station
	 * @return StationProperties that is a copy of the ones the station posesses - this is ONLY those of a base station
	 */
	StationProperties getPropertiesCopy();

	/**
	 * @return int representing the height in kilometers the station is above the surface of the planet
	 */
	int getAltitude();

	/**
	 * @return int[] representing the x,y,z rotation theta of the station in degrees
	 */
	int[] getRotation();

	/**
	 * @return ResourceLocation representing the planet the station is orbiting
	 */
	ResourceLocation getOrbit();

	/**
	 * @return boolean representing whether the station is anchored to the planet or not
	 */
	boolean getAnchored();

	/**
	 * @return HashedBlockPosition representing the player's spawn location on this station if they exist it
	 */
	HashedBlockPosition getSpawn();

	/**
	 * @return HashMap representing the locations of all station docking ports on this station
	 */
	HashMap<HashedBlockPosition, String> getGantries();

	/**
	 * @return double representing the gravity in G of this station
	 */
	double getGravitation();

	/**
	 * @return Direction representing the forward-facing direction of this station
	 */
	Direction getDirection();

	/**
	 * @return int[] representing the rotations per minute in degrees of the station
	 */
	int[] getOmega();

	/**
	 * @return int representing the target height in kilometers the station is above the surface of the planet
	 */
	int getTargetAltitude();

	/**
	 * @return double representing the target gravity in G of this station
	 */
	double getTargetGravitation();

	/**
	 * @return int[] representing the target rotations per minute in degrees of the station
	 */
	int[] getTargetOmega();


	/**
	 * Sets the altitude in km of this station above the surface of the planet. Some implementations may have caps on minimum and maximum altitude
	 * @param altitude height in km to try and set the station to
	 */
	void setAltitude(int altitude);

	/**
	 * Sets the rotational angles of the station in degrees from the horizontal [x,z] or forward direction [y]
	 * @param rotation int[x, y, z] to set the angles to
	 */
	void setRotation(int[] rotation);

	/**
	 * Sets the planet the station is orbiting above. Some implementations may have safety checks or restrict to planets only
	 * @param orbit ResourceLocation to set the station to orbit
	 */
	void setOrbit(ResourceLocation orbit);

	/**
	 * Sets the whether this station is anchored in its position in space. An example is a space elevator tether anchors a station
	 * @param anchored boolean whether the station is anchored or not
	 */
	void setAnchored(boolean anchored);

	/**
	 * Sets the spawn location of the station
	 * @param spawn HashedBlockPosition the player spawns at
	 */
	void setSpawn(HashedBlockPosition spawn);

	/**
	 * Adds a docking gantry (Station Docking Port) connection to the station
	 * @param pos the BlockPos position the gantry is added at
	 * @param str the name of the gantry to add
	 */
	void addDockingPosition(BlockPos pos, String str);

	/**
	 * Removes a docking gantry (Station Docking Port) connection from the station
	 * @param pos the BlockPos position the gantry is to be removed from
	 */
	void removeDockingPosition(BlockPos pos);

	/**
	 * Sets the gravitation in G of the station. Some implementations may have a lower or upper cap so the player is still able to move
	 * @param gravitation the gravity to set
	 */
	void setGravitation(double gravitation);

	/**
	 * Sets the 'forward' direction of this station. Some implementations may not use this
	 * @param direction the Direction that the forward end of the station points
	 */
	void setDirection(Direction direction);

	/**
	 * Sets the rotational speed of the station in degrees per minute from the horizontal [x,z] or forward direction [y]
	 * @param omega int[x, y, z] to set the speeds to
	 */
	void setOmega(int[] omega);

	/**
	 * Sets the target altitude for this station above the body in km
	 * @param altitude int representing the target height in kilometers the station is above the surface of the planet
	 */
	void setTargetAltitude(int altitude);

	/**
	 * Sets the target gravitation in G of the station
	 * @param gravitation the target gravity to set
	 */
	void setTargetGravitation(double gravitation);

	/**
	 * Sets the target rotational speed of the station in degrees per minute from the horizontal [x,z] or forward direction [y]
	 * @param omega int[x, y, z] to set the target speeds to
	 */
	void setTargetOmega(int[] omega);

	/**
	 * Gets the relative solar insolation of the planet below the station
	 * @return the insolation relative to Earth ground of the station
	 */
	double getInsolation();

	/**
	 * Checks (approximately) whether a laser shown from the bottom of the station would intercept the planet at any point
	 * @return whether the bottom of the station is facing the planet
	 */
	boolean isBottomAbovePlanet();

	/**
	 * Checks whether this station could sustain the tether of a space elevator - ie, it needs to be perfectly horizontal
	 * @return whether the station's current rotation would break the tether
	 */
	boolean wouldStationBreakTether();
}
