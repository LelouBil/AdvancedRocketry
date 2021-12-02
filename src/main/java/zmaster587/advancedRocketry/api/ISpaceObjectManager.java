package zmaster587.advancedRocketry.api;

import net.minecraft.util.math.BlockPos;
import zmaster587.advancedRocketry.api.stations.ISpaceObject;

import javax.annotation.Nullable;

public interface ISpaceObjectManager {
	
	@Nullable ISpaceObject getSpaceStationFromBlockCoords(BlockPos pos);
}
