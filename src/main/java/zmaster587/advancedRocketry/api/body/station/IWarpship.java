package zmaster587.advancedRocketry.api.body.station;

import net.minecraft.util.ResourceLocation;
import zmaster587.libVulpes.util.HashedBlockPosition;

import java.util.List;

public interface IWarpship {

    /**
     * Gets the planet that the station is travelling to
     * @return ResourceLocation of the planet for the station in transit
     */
    ResourceLocation getDestination();

    /**
     * Sets the planet that the station is to travel to
     * @param location ResourceLocation of the planet for the station in transit
     */
    void setDestination(ResourceLocation location);

    /**
     * Gets the transition time for the warpship to drop out of warp
     * @return long of the world time (Overworld) for the station to drop out of warp at
     */
    long getTransitionTime();

    /**
     * Begins a warp transition
     * @param time the Overworld world time for the warpship to drop out of warp at
     */
    void beginTransition(long time);

    /**
     * Gets whether the station is currently warping
     * @return boolean whether the station is in warp
     */
    boolean isWarping();

    /**
     * Gets a list of all the positions of warp cores on the station
     * @return List of HashedBlockPositions of the warp cores
     */
    List<HashedBlockPosition> getWarpCoreLocations();

    /**
     * Adds a warp core to the station
     * @param position HashedBlockPosition position of the warp core to add
     */
    void addWarpCoreLocation(HashedBlockPosition position);

    /**
     * Removes a warp core from the station
     * @param position HashedBlockPosition position of the warp core to remove
     */
    void removeWarpCoreLocation(HashedBlockPosition position);

    /**
     * Gets whether the station can warp - whether it has an open core, isn't warping, etc
     * @return boolean whether the station has a usable core
     */
    boolean hasUsableWarpCore();

    /**
     * Gets the current amount of fuel this station holds
     * @return int fuel amount
     */
    int getFuelAmount();

    /**
     * Gets the maximum fuel this station can hold
     * @return int maximum fuel amount
     */
    int getMaxFuelAmount();

    /**
     * Sets the fuel of the station to the passed amount
     * @param amt the amount of fuel to set
     */
    void setFuelAmount(int amt);

    /**
     * Adds the passed amount of fuel to the space station
     * @param amt the maximum fuel to add
     * @return amount of fuel used
     */
    int addFuel(int amt);

    /**
     * Used the amount of fuel passed
     * @param amt the maximum fuel to use
     * @return amount of fuel consumed
     */
    int useFuel(int amt);

}
