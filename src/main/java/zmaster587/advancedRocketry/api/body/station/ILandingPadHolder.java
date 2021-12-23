package zmaster587.advancedRocketry.api.body.station;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.util.StationLandingLocation;
import zmaster587.libVulpes.util.HashedBlockPosition;

import java.util.List;

public interface ILandingPadHolder {

	/**
	 * @return List representing the location of all possible landing pads for this station
	 */
	List<StationLandingLocation> getLandingPads();

	/**
	 * Add a landing pad location to this station
	 * @param pos the BlockPos to register the pad at
	 * @param name the name to register the pad under
	 */
	void addLandingPad(BlockPos pos, String name);

	/**
	 * Remove a landing pad from the station
	 * @param pos the position to remove the landing pad from
	 */
	void removeLandingPad(BlockPos pos);

	/**
	 * Set a landing pad on this station's status as full
	 * @param pos the position to set full or not
	 * @param full whether the pad is full or not
	 */
	void setLandingPadFull(BlockPos pos, boolean full);

	/**
	 * Set the name of the pad at the provided location
	 * @param world world, to check whether the assignment was client or server side
	 * @param pos the HashedBlockPosition at which to set the name of the pad
	 * @param name the name to set for the pad
	 */
	void setLandingPadName(World world, HashedBlockPosition pos, String name);

	/**
	 * Set the pad at this location to automatic mode
	 * @param pos the position to set automatic or not
	 * @param status boolean for whether the pad should be automatic or not
	 */
	void setLandingPadAutomatic(BlockPos pos, boolean status);

	/**
	 * Gets and sets full the next landing pad open for automated landing
	 * @param commit boolean on whether to commit the pad as full or not
	 * @return HashedBlockPosition of the next automatically-assignable pad
	 */
	HashedBlockPosition getNextAutomaticLandingPad(boolean commit);

	/**
	 * Get the landing pad at the provided location
	 * @param pos HashedBlockPosition the position to check
	 * @return StationLandingLocation (or null) at the provided position
	 */
	StationLandingLocation getLandingPadAtLocation(HashedBlockPosition pos);
}
