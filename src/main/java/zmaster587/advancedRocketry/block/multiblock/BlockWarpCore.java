package zmaster587.advancedRocketry.block.multiblock;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.api.body.station.IStation;
import zmaster587.advancedRocketry.api.body.PlanetManager;
import zmaster587.advancedRocketry.api.body.SpaceObjectManager;
import zmaster587.advancedRocketry.stations.SpaceStation;
import zmaster587.libVulpes.block.multiblock.BlockMultiblockMachine;
import zmaster587.libVulpes.inventory.GuiHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;
import zmaster587.libVulpes.util.ZUtils;

public class BlockWarpCore extends BlockMultiblockMachine {

	public BlockWarpCore(Properties property,
			GuiHandler.guiId guiId) {
		super(property, guiId);
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, BlockState state,
			LivingEntity placer, ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, placer, stack);
		
		if(!world.isRemote && PlanetManager.spaceId.equals(ZUtils.getDimensionIdentifier(world))) {
			IStation spaceObj = SpaceObjectManager.getSpaceManager().getSpaceStationFromBlockCoords(pos);
		
			if(spaceObj instanceof SpaceStation)
				((SpaceStation)spaceObj).addWarpCoreLocation(new HashedBlockPosition(pos));
		}
	}
	
	@Override
	public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		if(state.getBlock() != newState.getBlock() && PlanetManager.spaceId.equals(ZUtils.getDimensionIdentifier(world))) {

			IStation spaceObj = SpaceObjectManager.getSpaceManager().getSpaceStationFromBlockCoords(pos);
			if(spaceObj instanceof SpaceStation)
				((SpaceStation)spaceObj).removeWarpCoreLocation(new HashedBlockPosition(pos));
		}
		
		super.onReplaced(state, world, pos, newState, isMoving);
	}
}
