package zmaster587.advancedRocketry.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.fluid.FlowingFluid;

import java.util.function.Supplier;

public class BlockEnrichedLava extends FlowingFluidBlock {

	public BlockEnrichedLava(Supplier<FlowingFluid> fluidEnrichedLava, AbstractBlock.Properties properties) {
		super(fluidEnrichedLava, properties);
	}
	
	//TODO: add eyecandy

}
