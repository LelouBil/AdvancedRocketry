package zmaster587.advancedRocketry.tile.atmosphere;

import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;
import zmaster587.libVulpes.tile.IComparatorOverride;
import zmaster587.libVulpes.tile.TileInventoriedFEConsumerTank;
import zmaster587.libVulpes.util.FluidUtils;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

public class TileOxygenSystemBase extends TileInventoriedFEConsumerTank implements IComparatorOverride {

    public TileOxygenSystemBase(TileEntityType<?> type) {
        super(type, 0, 2, 4000);
        inventory.setCanInsertSlot(0, true);
        inventory.setCanExtractSlot(0, false);
        inventory.setCanInsertSlot(1, false);
        inventory.setCanExtractSlot(1, true);
    }

    @Override
    public void tick() {
        FluidUtils.attemptDrainContainerIInv(inventory, this.tank, getStackInSlot(0), 0, 1);
    }

    @Override
    public boolean canPerformFunction() {
        return false;
    }

    @Override
    public void performFunction() {};

    /**
     * @param handler the AtmosphereHandler at the location of the oxygen vent
     * @param world the World the oxygen vent is in
     * @param remainingPower the energy left in the oxygen vent
     * @param currentTotalPressure the current pressure of the atmosphere the vent is in
     * @return whether the component can function on this tick
     */
    public boolean canPerformFunction(AtmosphereHandler handler, World world, int remainingPower, float currentTotalPressure) {
        return false;
    }

    /**
     * @param handler the AtmosphereHandler at the location of the oxygen vent
     * @param world the World the oxygen vent is in
     * @param addedVolumeThisTick the mB that was added to the atmosphere this tick
     * @return the energy consumed by the vent system component
     */
    public int performFunction(AtmosphereHandler handler, World world, int addedVolumeThisTick) { return 0; }

    @Override
    public int getComparatorOverride() {
        return 16 * tank.getFluidAmount()/tank.getCapacity();
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public int[] getSlotsForFace(Direction face) {
        return new int[]{0, 1};
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).isPresent() || stack.getItem() instanceof BucketItem;
    }
}