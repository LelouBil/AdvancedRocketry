package zmaster587.advancedRocketry.block.multiblock;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import zmaster587.libVulpes.block.multiblock.BlockMultiBlockComponentVisibleAlphaTexture;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

public class BlockSolarArrayPanel extends BlockMultiBlockComponentVisibleAlphaTexture {

	private static VoxelShape bb = VoxelShapes.create(0, 0.375, 0, 1, 0.625, 1);

	public BlockSolarArrayPanel(Properties mat) {
		super(mat);
	}
	
	@Nonnull
	@Override
	@ParametersAreNonnullByDefault
	public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		return bb;
	}

	@Nonnull
	@Override
	@ParametersAreNonnullByDefault
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		return bb;
	}
}
