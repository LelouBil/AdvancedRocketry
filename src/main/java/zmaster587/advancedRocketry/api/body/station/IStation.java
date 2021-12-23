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



	void setAltitude(int altitude);
	void setRotation(int[] rotation);
	void setOrbit(ResourceLocation orbit);
	void setAnchored(boolean anchored);

	void setSpawn(HashedBlockPosition spawn);
	void addDockingPosition(BlockPos pos, String str);
	void removeDockingPosition(BlockPos pos);

	void setGravitation(double gravitation);
	void setDirection(Direction direction);
	void setOmega(int[] omega);
	void setTargetAltitude(int altitude);
	void setTargetGravitation(double gravitation);
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
