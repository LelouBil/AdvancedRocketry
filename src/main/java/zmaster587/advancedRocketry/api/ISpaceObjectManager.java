package zmaster587.advancedRocketry.api;

import net.minecraft.util.math.BlockPos;
import zmaster587.advancedRocketry.api.body.station.IStation;

import javax.annotation.Nullable;

public interface ISpaceObjectManager {
	
	@Nullable
    IStation getSpaceStationFromBlockCoords(BlockPos pos);
}
