package zmaster587.advancedRocketry.block;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.api.body.station.IStation;
import zmaster587.advancedRocketry.api.body.SpaceObjectManager;
import zmaster587.advancedRocketry.stations.SpaceStation;
import zmaster587.libVulpes.block.BlockTile;
import zmaster587.libVulpes.inventory.GuiHandler;

public class BlockWarpController extends BlockTile {

	public BlockWarpController(Properties properties, GuiHandler.guiId guiId) {
		super(properties, guiId);
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, placer, stack);
		
		
		IStation spaceObject = SpaceObjectManager.getSpaceManager().getSpaceStationFromBlockCoords(pos);
		
		if(spaceObject instanceof SpaceStation) {
			((SpaceStation)spaceObject).setForwardDirection(getFront(state).getOpposite());
		}
	}
}
