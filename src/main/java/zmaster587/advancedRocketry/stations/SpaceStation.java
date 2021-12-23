package zmaster587.advancedRocketry.stations;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Direction.AxisDirection;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Dimension;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.common.thread.EffectiveSide;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.Constants;
import zmaster587.advancedRocketry.api.body.planet.PlanetProperties;
import zmaster587.advancedRocketry.api.body.solar.StellarBody;
import zmaster587.advancedRocketry.api.body.station.IStorageChunk;
import zmaster587.advancedRocketry.api.body.PlanetManager;
import zmaster587.advancedRocketry.inventory.IPlanetDefiner;
import zmaster587.advancedRocketry.network.PacketSpaceStationInfo;
import zmaster587.advancedRocketry.network.PacketStationUpdate;
import zmaster587.advancedRocketry.network.PacketStationUpdate.Type;
import zmaster587.advancedRocketry.tile.station.TileDockingPort;
import zmaster587.advancedRocketry.util.StationLandingLocation;
import zmaster587.libVulpes.block.BlockFullyRotatable;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;
import zmaster587.libVulpes.util.ZUtils;

import java.util.*;
import java.util.Map.Entry;

public class SpaceStation extends StationBase implements IPlanetDefiner {

	private final int MAX_FUEL = 1000;

	//Warp Properties
	private Set<ResourceLocation> knownPlanetList;
	private int fuel;
	private long transition;
	private long lastTimeModification = 0;
	private ResourceLocation destination;
	private List<HashedBlockPosition> cores;

	public SpaceStation() {
		orbitalDistance = 4.0f;
		targetOrbitalDistance = 4;
		targetRotationsPerHour = new int[]{0, 0, 0};
		targetGravity = 10;
		spawnLocations = new LinkedList<>();
		warpCoreLocation = new LinkedList<>();
		dockingPoints = new HashMap<>();
		transitionEta = -1;
		destinationLocation = Dimension.OVERWORLD.getLocation();
		created = false;
		knownPlanetList = new HashSet<>();
		angularVelocity = new double[3];
		rotation = new double[3];
	}

	public long getExpireTime() { 
		return Long.MAX_VALUE;
	}

	public void beginTransition(long time) {
		if(time > 0)
			transitionEta = time;

		//Hack because somehow created ends up being false
		created = true;
	}

	public boolean isWarping() {
		return getOrbitingPlanetId() == SpaceObjectManager.WARPDIMID;
	}

	public long getTransitionTime() {
		return transitionEta;
	}

	public void setTargetRotationsPerHour(int index, int rotations) {
		targetRotationsPerHour[index] = rotations;
	}

	public void discoverPlanet(ResourceLocation pid) {
		knownPlanetList.add(pid);
		PacketHandler.sendToAll(new PacketSpaceStationInfo(getId(), this));
	}
	
	/**
	 * @return the insolation relative to Earth ground of the station - returns 0 for warping!
	 */
	public double getInsolation() {
		return (isWarping()) ? 0.0 : PlanetManager.getInstance().getPlanetProperties(orbitingLocation).getInsolationWithoutAtmosphere();
	}

	/**
	 * @return the DIMID of the planet the object is currently orbiting, Constants.INVALID_PLANET if none
	 */
	@Override
	public ResourceLocation getOrbitingPlanetId() {
		return created ? orbitingLocation : Constants.INVALID_PLANET;
	}

	/**
	 * Sets the forward Facing direction of the object.  Mostly used for warpships
	 * @param direction
	 */
	public void setForwardDirection(Direction direction) {
		this.direction = direction;
	}

	/**
	 * Gets the forward facing direction of the ship.  Direction is not garunteed to be set
	 * @return direction of the ship, or UNKNOWN if none exists
	 */
	public Direction getForwardDirection() {
		if(direction == null)
			return Direction.NORTH;
		return direction;
	}

	/**
	 * @return if the object is anchored in place by anything
	 */
	@Override
	public boolean isAnchored() {
		return isAnchored;}

	/**
	 * Sets if the object is anchored or not
	 */
	@Override
	public void setIsAnchored(boolean anchored) {isAnchored = anchored; }

	/**
	 * @return rotation of the station in degrees
	 */
	public double getRotation(Direction dir) {

		return (rotation[getIDFromDir(dir)] + getDeltaRotation(dir)*(AdvancedRocketry.proxy.getWorldTimeUniversal() - lastTimeModification)) % (360D);
	}

	/**
	 * @return whether the bottom of the station is facing the planet or not, this is if a laser would hit the planet at all if shined straight down
	 */
	public boolean isStationFacingPlanet () {
		//They use 0 to 1.0 so we need to convert to that, and to to check angle <150 degrees
		return Math.abs(rotation[0] - (int)rotation[0] - 0.5) > 0.40 && Math.abs(rotation[2] - (int)rotation[2] - 0.5) > 0.40;
	}

	/**
	 * @return whether the station's current rotation would break the tether
	 */
	public boolean wouldStationBreakTether () {
		//0.47 here is approximately between 10 and 15 degrees from the horizontal
		return 0.47 > Math.abs(rotation[0] - (int)rotation[0] - 0.5) || 0.47 > Math.abs(rotation[2] - (int)rotation[2] - 0.5) || Math.abs(getDeltaRotation(Direction.UP)) > 0 || Math.abs(getDeltaRotation(Direction.NORTH)) > 0 || Math.abs(getDeltaRotation(Direction.EAST)) > 0;
	}

	private int getIDFromDir(Direction facing){
		if(facing == Direction.EAST)
			return 0;
		else if(facing == Direction.UP)
			return 1;
		else
			return 2;
	}

	/**
	 * @param rotation rotation of the station in degrees
	 */
	public void setRotation(double rotation, Direction facing) {
		this.rotation[getIDFromDir(facing)] = rotation;
	}

	/**
	 * @return anglarVelocity of the station in degrees per tick
	 */
	public double getDeltaRotation(Direction facing) {
		return this.angularVelocity[getIDFromDir(facing)];
	}

	/**
	 * @param rotation anglarVelocity of the station in degrees per tick
	 */
	public void setDeltaRotation(double rotation, Direction facing) {
		if (!isAnchored()) {
			this.rotation[getIDFromDir(facing)] = getRotation(facing);
			this.lastTimeModification = AdvancedRocketry.proxy.getWorldTimeUniversal();

			this.angularVelocity[getIDFromDir(facing)] = rotation;
		}
	}

	public double getMaxRotationalAcceleration() {
		return 0.000005D;
	}

	/**
	 * @return the spawn location of the object
	 */
	public HashedBlockPosition getSpawnLocation() {
		return spawnLocation;
	}

	public void addWarpCore(HashedBlockPosition position) {
		warpCoreLocation.add(position);
	}
	public void removeWarpCore(HashedBlockPosition position) {
		warpCoreLocation.remove(position);
	}

	public List<HashedBlockPosition> getWarpCoreLocations() {
		return warpCoreLocation;
	}

	public boolean hasUsableWarpCore() {
		return warpCoreLocation.size() != 0 && !SpaceObjectManager.WARPDIMID.equals(orbitingLocation) && !getOrbitingPlanetId().equals(getDestOrbitingBody());
	}

	public int getFuelAmount() {
		return fuelAmount;
	}

	public int getMaxFuelAmount() {
		return MAX_FUEL;
	}

	public void setFuelAmount(int amt) {
		fuelAmount = amt;
	}

	/**
	 * Adds the passed amount of fuel to the space station
	 * @param amt
	 * @return amount of fuel used
	 */
	public int addFuel(int amt) {
		if(amt < 0)
			return amt;

		int oldFuelAmt = fuelAmount;
		fuelAmount = Math.min(fuelAmount + amt, MAX_FUEL);

		amt = fuelAmount - oldFuelAmt;

		if(EffectiveSide.get().isServer())
			PacketHandler.sendToAll(new PacketStationUpdate(this, Type.FUEL_UPDATE));
		return amt;
	}

	/**
	 * Used the amount of fuel passed
	 * @param amt
	 * @return amount of fuel consumed
	 */
	public int useFuel(int amt) {
		if(amt > getFuelAmount())
			return 0;

		fuelAmount -= amt;

		if(EffectiveSide.get().isServer())
			PacketHandler.sendToAll(new PacketStationUpdate(this, Type.FUEL_UPDATE));
		return amt;
	}

	public void setLandingPadAutomatic(BlockPos pos, boolean status) {
		setLandingPadAutoLandStatus(pos.getX(), pos.getZ(), status);
	}

	public void setLandingPadAutoLandStatus(int x, int z, boolean status) {
		HashedBlockPosition pos = new HashedBlockPosition(x, 0, z);

		for (StationLandingLocation loc : spawnLocations) {
			if (loc.getPos().equals(pos))
				loc.setAllowedForAutoLand(status);
		}
	}

	public void addLandingPad(BlockPos pos, String name) {
		addLandingPad(pos.getX(), pos.getZ(), name);
	}

	/**
	 * Adds a landing pad to the station
	 * @param x
	 * @param z
	 */
	public void addLandingPad(int x, int z, String name) {
		StationLandingLocation pos = new StationLandingLocation(new HashedBlockPosition(x, 0, z), name);
		if(!spawnLocations.contains(pos)) {
			spawnLocations.add(pos);
			pos.setOccupied(false);
		}
	}

	public void removeLandingPad(BlockPos pos) {
		removeLandingPad(pos.getX(), pos.getZ());
	}

	/**
	 * Removes an existing landing pad from the station
	 * @param x
	 * @param z
	 */
	public void removeLandingPad(int x, int z) {
		HashedBlockPosition pos = new HashedBlockPosition(x, 0, z);

		spawnLocations.removeIf(loc -> loc.getPos().equals(pos));
		//spawnLocations.remove(pos);
	}

	/**
	 * Adds a docking location to the station
	 * @param pos
	 * @param str
	 */
	public void addDockingPosition(BlockPos pos, String str) {
		HashedBlockPosition pos2 = new HashedBlockPosition(pos);
		dockingPoints.put(pos2, str);
	}
	/**
	 * Removes a docking location from the station
	 * @param pos
	 */
	public void removeDockingPosition(BlockPos pos) {
		HashedBlockPosition pos2 = new HashedBlockPosition(pos);
		dockingPoints.remove(pos2);
	}

	/**
	 * @return next viable place to land
	 */
	public HashedBlockPosition getNextAutomaticLandingPad(boolean commit) {
		for(StationLandingLocation pos : spawnLocations) {
			if(!pos.getOccupied() && pos.getAllowedForAutoLand()) {
				if(commit)
					pos.setOccupied(true);
				return pos.getPos();
			}
		}
		return null;
	}

	public List<StationLandingLocation> getLandingPads() {
		return spawnLocations;
	}

	/**
	 * @return true if there is an empty pad to land on
	 */
	public boolean hasFreeLandingPad() {
		for(StationLandingLocation pos : spawnLocations) {
			if(!pos.getOccupied()) {
				return true;
			}
		}
		return false;
	}

	public void setLandingPadFull(BlockPos pos, boolean full) {
		setPadStatus(pos.getX(), pos.getZ(), full);
	}

	public StationLandingLocation getLandingPadAtLocation(HashedBlockPosition pos) {
		pos.y = 0;
		for(StationLandingLocation loc : spawnLocations) {
			if(loc.getPos().equals(pos))
				return loc;
		}
		return null;
	}

	public void setLandingPadName(World world, HashedBlockPosition pos, String name) {
		StationLandingLocation loc = getLandingPadAtLocation(pos);
		if(loc != null)
			loc.setName(name);

		//Make sure our remote uses get the data
		if(!world.isRemote)
			PacketHandler.sendToAll(new PacketSpaceStationInfo(getId(), this));
	}

	/**
	 * @param x
	 * @param z
	 * @param full true if the pad is avalible to use
	 */
	public void setPadStatus(int x, int z, boolean full) {
		StationLandingLocation pos = new StationLandingLocation(new HashedBlockPosition(x, 0, z));

		for(StationLandingLocation loc : spawnLocations) {
			if(loc.equals(pos))
				loc.setOccupied(full);
		}
	}

	/**
	 * @param id the space object id of this object (NOT DIMID)
	 */
	@Override
	public void setId(ResourceLocation id) {
		orbitingLocation = id;
	}

	/**
	 * Sets the spawn location for the space object
	 * @param x
	 * @param y
	 * @param z
	 */
	@Override
	public void setSpawnLocation(int x, int y, int z) {
		spawnLocation = new HashedBlockPosition(x,y,z);
	}

	/**
	 * Sets the orbiting planet for the space object but does NOT register it with the planet
	 * @param id
	 */
	@Override
	public void setOrbitingBody(ResourceLocation id) {
		if(this.getOrbitingPlanetId().equals(id))
			return;

		orbitingLocation = id;
		if(!SpaceObjectManager.WARPDIMID.equals(id))
			destinationLocation = id;
	}

	@Override
	public void setDestOrbitingBody(ResourceLocation id) {
		destinationLocation = id;
		if(EffectiveSide.get().isServer()) {
			PacketHandler.sendToAll(new PacketStationUpdate(this, PacketStationUpdate.Type.DEST_ORBIT_UPDATE));
		}
	}

	@Override
	public ResourceLocation getDestOrbitingBody() {
		return destinationLocation;
	}

	/**
	 * When the space stations are first created they are 'unpacked' from the storage chunk they reside in
	 * @param chunk
	 */
	public void onModuleUnpack(IStorageChunk chunk) {
		if(ZUtils.isWorldLoaded(PlanetManager.spaceDimensionID) && ZUtils.getWorld(PlanetManager.spaceDimensionID) == null)
			ZUtils.initDimension(PlanetManager.spaceDimensionID);
		World worldObj = ZUtils.getWorld(PlanetManager.spaceDimensionID);
		if(!created) {
			chunk.pasteInWorld(worldObj, spawnLocation.x - chunk.getSizeX()/2, spawnLocation.y - chunk.getSizeY()/2, spawnLocation.z - chunk.getSizeZ()/2);

			created = true;
		}
		else {
			List<TileEntity> tiles = chunk.getTileEntityList();
			List<String> targetIds = new LinkedList<>();
			List<TileEntity> myPoss = new LinkedList<>();
			HashedBlockPosition pos;
			TileDockingPort destTile = null;
			TileDockingPort srcTile = null;

			//Iterate though all docking ports on the module in the chunk being launched
			for(TileEntity tile : tiles) {
				if(tile instanceof TileDockingPort) {
					targetIds.add(((TileDockingPort)tile).getTargetId());
					myPoss.add(tile);
				}
			}

			//Find the first docking port on the station that matches the id in the new chunk
			for(Entry<HashedBlockPosition, String> map : dockingPoints.entrySet()) {
				if(targetIds.contains(map.getValue())) {
					int loc = targetIds.indexOf(map.getValue());
					pos = map.getKey();
					TileEntity tile;
					if((tile = worldObj.getTileEntity(pos.getBlockPos())) instanceof TileDockingPort) {
						destTile = (TileDockingPort)tile;
						srcTile = (TileDockingPort) myPoss.get(loc);
						break;
					}
				}
			}

			if(destTile != null) {
				Direction stationFacing = destTile.getBlockState().get(BlockFullyRotatable.FACING);
				Direction moduleFacing = srcTile.getBlockState().get(BlockFullyRotatable.FACING);


				Direction cross = rotateAround(moduleFacing, stationFacing.getAxis());

				if(stationFacing.getAxisDirection() == AxisDirection.NEGATIVE)
					cross = cross.getOpposite();

				if(cross == moduleFacing) {
					if(moduleFacing == stationFacing) {
						if(cross == Direction.DOWN || cross == Direction.UP) {
							chunk.rotateBy(Direction.NORTH);
							chunk.rotateBy(Direction.NORTH);
						}
						else {
							chunk.rotateBy(Direction.UP);
							chunk.rotateBy(Direction.UP);
						}
					}
				}
				else if(cross.getOpposite() != moduleFacing)
					chunk.rotateBy(stationFacing.getYOffset() == 0 ? cross : cross.getOpposite());

				int xCoord = (stationFacing.getXOffset() == 0 ? -srcTile.getPos().getX() : srcTile.getPos().getX()*stationFacing.getXOffset()) + stationFacing.getXOffset() + destTile.getPos().getX();
				int yCoord = (stationFacing.getYOffset() == 0 ? -srcTile.getPos().getY() : srcTile.getPos().getY()*stationFacing.getYOffset()) + stationFacing.getYOffset() + destTile.getPos().getY();
				int zCoord = (stationFacing.getZOffset() == 0 ? -srcTile.getPos().getZ() : srcTile.getPos().getZ()*stationFacing.getZOffset()) + stationFacing.getZOffset() + destTile.getPos().getZ();
				chunk.pasteInWorld(worldObj, xCoord, yCoord, zCoord);
				worldObj.removeBlock(destTile.getPos().offset(stationFacing), false);
				worldObj.removeBlock(destTile.getPos(), false);
			}
		}
	}


	private static Direction rotateAround(Direction inputDir, Axis axis)
	{
		switch (axis)
		{
		case X:

			if (inputDir != Direction.WEST && inputDir != Direction.EAST)
			{
				return rotateX(inputDir);
			}

			return inputDir;
		case Y:

			if (inputDir != Direction.UP && inputDir != Direction.DOWN)
			{
				return rotateY(inputDir);
			}

			return inputDir;
		case Z:

			if (inputDir != Direction.NORTH && inputDir != Direction.SOUTH)
			{
				return rotateZ(inputDir);
			}

			return inputDir;
		default:
			throw new IllegalStateException("Unable to get CW facing for axis " + axis);
		}
	}
	

    /**
     * Rotate this Facing around the X axis (NORTH => DOWN => SOUTH => UP => NORTH)
     */
    private static Direction rotateX(Direction inputDir)
    {
        switch (inputDir)
        {
            case NORTH:
                return Direction.DOWN;
            case EAST:
            case WEST:
            default:
                throw new IllegalStateException("Unable to get X-rotated facing of " + inputDir);
            case SOUTH:
                return Direction.UP;
            case UP:
                return Direction.NORTH;
            case DOWN:
                return Direction.SOUTH;
        }
    }

    /**
     * Rotate this Facing around the Z axis (EAST => DOWN => WEST => UP => EAST)
     */
    private static Direction rotateZ(Direction inputDir)
    {
        switch (inputDir)
        {
            case EAST:
                return Direction.DOWN;
            case SOUTH:
            default:
                throw new IllegalStateException("Unable to get Z-rotated facing of " + inputDir);
            case WEST:
                return Direction.UP;
            case UP:
                return Direction.EAST;
            case DOWN:
                return Direction.WEST;
        }
    }
    
    /**
     * Rotate this Facing around the Y axis clockwise (NORTH => EAST => SOUTH => WEST => NORTH)
     */
    private static Direction rotateY(Direction inputDir)
    {
        switch (inputDir)
        {
            case NORTH:
                return Direction.EAST;
            case EAST:
                return Direction.SOUTH;
            case SOUTH:
                return Direction.WEST;
            case WEST:
                return Direction.NORTH;
            default:
                throw new IllegalStateException("Unable to get Y-rotated facing of " + inputDir);
        }
    }

	@Override
	public void writeToNbt(CompoundNBT nbt) {
		nbt.putString("id", getId().toString());
		nbt.putBoolean("isAnchored", isAnchored);
		nbt.putBoolean("created", created);

		nbt.putInt("spawnX", spawnLocation.x);
		nbt.putInt("spawnY", spawnLocation.y);
		nbt.putInt("spawnZ", spawnLocation.z);
		nbt.putString("orbitingLocation", orbitingLocation.toString());
		nbt.putString("destinationDimId", destinationLocation.toString());
		nbt.putInt("fuel", fuelAmount);
		nbt.putFloat("orbitalDistance", orbitalDistance);
		nbt.putInt("targetOrbitalDistance", targetOrbitalDistance);
		nbt.putInt("targetGravity", targetGravity);
		nbt.putInt("targetRotationX", targetRotationsPerHour[0]);
		nbt.putInt("targetRotationY", targetRotationsPerHour[1]);
		nbt.putInt("targetRotationZ", targetRotationsPerHour[2]);
		nbt.putDouble("rotationX", rotation[0]);
		nbt.putDouble("rotationY", rotation[1]);
		nbt.putDouble("rotationZ", rotation[2]);
		nbt.putDouble("deltaRotationX", angularVelocity[0]);
		nbt.putDouble("deltaRotationY", angularVelocity[1]);
		nbt.putDouble("deltaRotationZ", angularVelocity[2]);

		//Set known planets
		ListNBT planetList = new ListNBT();
		for(ResourceLocation i : knownPlanetList)
			planetList.add(StringNBT.valueOf(i.toString()));
		nbt.put("knownPlanets", planetList);


		if(direction != null)
			nbt.putInt("direction", direction.ordinal());

		if(transitionEta > -1)
			nbt.putLong("transitionEta", transitionEta);

		ListNBT list = new ListNBT();
		for(StationLandingLocation pos : this.spawnLocations) {
			CompoundNBT tag = new CompoundNBT();
			tag.putBoolean("occupied", pos.getOccupied());
			tag.putBoolean("autoLand", pos.getAllowedForAutoLand());
			tag.putIntArray("pos", new int[] {pos.getPos().x, pos.getPos().z});
			//if(pos.getName() != null && !pos.getName().isEmpty())
			tag.putString("name", pos.getName());
			list.add(tag);
		}
		nbt.put("spawnPositions", list);

		list = new ListNBT();
		for(HashedBlockPosition pos : this.warpCoreLocation) {
			CompoundNBT tag = new CompoundNBT();
			tag.putIntArray("pos", new int[] {pos.x, pos.y, pos.z});
			list.add(tag);
		}
		nbt.put("warpCorePositions", list);

		list = new ListNBT();
		for(Entry<HashedBlockPosition, String> obj : this.dockingPoints.entrySet()) {
			CompoundNBT tag = new CompoundNBT();
			HashedBlockPosition pos = obj.getKey();
			String str = obj.getValue();
			tag.putIntArray("pos", new int[] {pos.x, pos.y, pos.z});
			tag.putString("id", str);
			list.add(tag);
		}
		nbt.put("dockingPositons", list);
	}

	@Override
	public void readFromNbt(CompoundNBT nbt) {
		orbitingLocation = new ResourceLocation(nbt.getString("orbitingLocation"));
		destinationLocation = new ResourceLocation(nbt.getString("destinationDimId"));
		isAnchored = nbt.getBoolean("isAnchored");
		created = nbt.getBoolean("created");

		fuelAmount = nbt.getInt("fuel");
		orbitalDistance = nbt.getFloat("orbitalDistance");
		targetOrbitalDistance = nbt.getInt("targetOrbitalDistance");
		targetRotationsPerHour[0] = nbt.getInt("targetRotationX");
		targetRotationsPerHour[1] = nbt.getInt("targetRotationY");
		targetRotationsPerHour[2] = nbt.getInt("targetRotationZ");
		targetGravity = nbt.getInt("targetGravity");
		spawnLocation = new HashedBlockPosition(nbt.getInt("spawnX"), nbt.getInt("spawnY"), nbt.getInt("spawnZ"));
		rotation[0] = nbt.getDouble("rotationX");
		rotation[1] = nbt.getDouble("rotationY");
		rotation[2] = nbt.getDouble("rotationZ");
		angularVelocity[0] = nbt.getDouble("deltaRotationX");
		angularVelocity[1] = nbt.getDouble("deltaRotationY");
		angularVelocity[2] = nbt.getDouble("deltaRotationZ");

		//get known planets

		ListNBT planetList = nbt.getList("knownPlanets", NBT.TAG_STRING);
		for( int i =0; i < planetList.size(); i++)
			knownPlanetList.add( new ResourceLocation(planetList.getString(i)));

		if(nbt.contains("direction"))
			direction = Direction.values()[nbt.getInt("direction")];

		if(nbt.contains("transitionEta"))
			transitionEta = nbt.getLong("transitionEta");

		ListNBT list = nbt.getList("spawnPositions", NBT.TAG_COMPOUND);
		spawnLocations.clear();
		for(int i = 0; i < list.size(); i++) {
			CompoundNBT tag = list.getCompound(i);
			int[] posInt = tag.getIntArray("pos");
			HashedBlockPosition pos = new HashedBlockPosition(posInt[0], 0, posInt[1]);
			StationLandingLocation loc = new StationLandingLocation(pos, tag.getString("name"));
			spawnLocations.add(loc);
			loc.setOccupied(tag.getBoolean("occupied"));
			loc.setAllowedForAutoLand(!tag.contains("occupied") || tag.getBoolean("occupied"));
		}

		list = nbt.getList("warpCorePositions", NBT.TAG_COMPOUND);
		warpCoreLocation.clear();
		for(int i = 0; i < list.size(); i++) {
			CompoundNBT tag = list.getCompound(i);
			int[] posInt = tag.getIntArray("pos");
			HashedBlockPosition pos = new HashedBlockPosition(posInt[0], posInt[1], posInt[2]);
			warpCoreLocation.add(pos);
		}

		list = nbt.getList("dockingPositons", NBT.TAG_COMPOUND);
		dockingPoints.clear();
		for(int i = 0; i < list.size(); i++) {
			CompoundNBT tag = list.getCompound(i);
			int[] posInt = tag.getIntArray("pos");
			HashedBlockPosition pos = new HashedBlockPosition(posInt[0], posInt[1], posInt[2]);
			String str = tag.getString("id");
			dockingPoints.put(pos, str);
		}
	}

	/**
	 * True if the spawn location for this space object is not the default one assigned to it
	 * @return
	 */
	@Override
	public boolean hasCustomSpawnLocation() {
		return false;
	}

	@Override
	public float getOrbitalDistance() {
		return orbitalDistance;
	}

	@Override
	public void setOrbitalDistance(float finalVel) {
		if (!isAnchored()) {
			orbitalDistance = Math.max(4.0f, finalVel);
		}
	}

	@Override
	public boolean isPlanetKnown(PlanetProperties properties) {
		return !ARConfiguration.getCurrentConfig().planetsMustBeDiscovered.get() || knownPlanetList.contains(properties.getLocation().dimension) || PlanetManager.getInstance().knownPlanets.contains(properties.getLocation().dimension);
	}

	@Override
	public boolean isStarKnown(StellarBody body) {
		return true;
	}

	@Override
	public ResourceLocation getId() {
		return properties.getId();
	}
}
