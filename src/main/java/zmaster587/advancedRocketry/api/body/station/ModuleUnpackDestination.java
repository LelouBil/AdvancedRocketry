package zmaster587.advancedRocketry.api.body.station;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.api.body.PlanetManager;
import zmaster587.advancedRocketry.tile.station.TileDockingPort;
import zmaster587.libVulpes.block.BlockFullyRotatable;
import zmaster587.libVulpes.util.HashedBlockPosition;
import zmaster587.libVulpes.util.ZUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ModuleUnpackDestination {

    /**
     * Unpack module onto the given coordinates. Only meant to be used with stations & station coordinates. You're intended to make a public method with only an IStorageChunk for proper access
     * @param chunk the IStorageChunk the blocks to unpack are contained in
     * @param creation whether the destination is getting its first structure
     * @param position HashedBlockPosition of the place to unpack, usually the spawn of the station
     * @param gantries HashMap of HashedBlockPosition:String that are the unpackable-onto station docking ports in the destination
     */
    protected void unpackModule(IStorageChunk chunk, boolean creation, HashedBlockPosition position, HashMap<HashedBlockPosition, String> gantries) {
        if(ZUtils.isWorldLoaded(PlanetManager.spaceDimensionID) && ZUtils.getWorld(PlanetManager.spaceDimensionID) == null)
            ZUtils.initDimension(PlanetManager.spaceDimensionID);
        World world = ZUtils.getWorld(PlanetManager.spaceDimensionID);
        if(creation) {
            chunk.pasteInWorld(world, position.x - chunk.getSizeX()/2, position.y - chunk.getSizeY()/2, position.z - chunk.getSizeZ()/2);
        } else {
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
            for(Map.Entry<HashedBlockPosition, String> map : gantries.entrySet()) {
                if(targetIds.contains(map.getValue())) {
                    int loc = targetIds.indexOf(map.getValue());
                    pos = map.getKey();
                    TileEntity tile;
                    if((tile = world.getTileEntity(pos.getBlockPos())) instanceof TileDockingPort) {
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

                if(stationFacing.getAxisDirection() == Direction.AxisDirection.NEGATIVE)
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
                chunk.pasteInWorld(world, xCoord, yCoord, zCoord);
                world.removeBlock(destTile.getPos().offset(stationFacing), false);
                world.removeBlock(destTile.getPos(), false);
            }
        }
    }

    /**
     * Rotate the input Direction about the input Axis
     * @param input direction the module starts out as
     * @param axis the axis to rotate around
     * @return the final direction after rotation
     */
    private static Direction rotateAround(Direction input, Direction.Axis axis) {
        switch (axis) {
            case X:
                if (input != Direction.WEST && input != Direction.EAST) {
                    return rotateX(input);
                }
                return input;
            case Y:
                if (input != Direction.UP && input != Direction.DOWN) {
                    return rotateY(input);
                }
                return input;
            case Z:
                if (input != Direction.NORTH && input != Direction.SOUTH) {
                    return rotateZ(input);
                }
                return input;
            default:
                throw new IllegalStateException("Unable to get CW facing for axis " + axis);
        }
    }


    /**
     * Rotate this Direction around the X axis (NORTH => DOWN => SOUTH => UP => NORTH)
     * @param input direction the module starts out as
     * @return the final direction after rotation
     */
    private static Direction rotateX(Direction input) {
        switch (input) {
            case NORTH:
                return Direction.DOWN;
            case EAST:
            case WEST:
            default:
                throw new IllegalStateException("Unable to get X-rotated facing of " + input);
            case SOUTH:
                return Direction.UP;
            case UP:
                return Direction.NORTH;
            case DOWN:
                return Direction.SOUTH;
        }
    }

    /**
     * Rotate this Direction around the Z axis (EAST => DOWN => WEST => UP => EAST)
     * @param input direction the module starts out as
     * @return the final direction after rotation
     */
    private static Direction rotateZ(Direction input) {
        switch (input) {
            case EAST:
                return Direction.DOWN;
            case SOUTH:
            default:
                throw new IllegalStateException("Unable to get Z-rotated facing of " + input);
            case WEST:
                return Direction.UP;
            case UP:
                return Direction.EAST;
            case DOWN:
                return Direction.WEST;
        }
    }

    /**
     * Rotate this Direction around the Y axis clockwise (NORTH => EAST => SOUTH => WEST => NORTH)
     * @param input direction the module starts out as
     * @return the final direction after rotation
     */
    private static Direction rotateY(Direction input) {
        switch (input) {
            case NORTH:
                return Direction.EAST;
            case EAST:
                return Direction.SOUTH;
            case SOUTH:
                return Direction.WEST;
            case WEST:
                return Direction.NORTH;
            default:
                throw new IllegalStateException("Unable to get Y-rotated facing of " + input);
        }
    }
}
