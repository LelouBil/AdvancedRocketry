package zmaster587.advancedRocketry.util;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStackSimple;

import javax.annotation.Nonnull;

public class FluidHandlerBucketClone extends FluidHandlerItemStackSimple {

	ItemStack empty;
	
    public FluidHandlerBucketClone(ItemStack container, int capacity, ItemStack empty) {
		super(container, capacity);
		this.empty = empty;
	}

	@Nonnull
    @Override
    public ItemStack getContainer()
    {
        return this.getFluid().isEmpty() ? empty.copy() : container;
    }
}
