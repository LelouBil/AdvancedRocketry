package zmaster587.advancedRocketry.tile.atmosphere;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;
import zmaster587.libVulpes.tile.IComparatorOverride;
import zmaster587.libVulpes.tile.TileInventoriedRFConsumerTank;
import zmaster587.libVulpes.util.FluidUtils;

import javax.annotation.Nonnull;

public class TileOxygenVentSystemBase extends TileInventoriedRFConsumerTank implements IComparatorOverride {

	public TileOxygenVentSystemBase() {
		super(1000, 2, 4000);
		inventory.setCanInsertSlot(0, true);
		inventory.setCanExtractSlot(0, false);
		inventory.setCanInsertSlot(1, false);
		inventory.setCanExtractSlot(1, true);
	}

	@Override
	public void update() {};

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
	public int[] getSlotsForFace(EnumFacing face) {
		return new int[0];
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		return false;
	}

	@Override
	public void setInventorySlotContents(int slot, @Nonnull ItemStack stack) {
		super.setInventorySlotContents(slot, stack);

		while(FluidUtils.attemptDrainContainerIInv(inventory, this.tank, getStackInSlot(0), 0, 1));
	}
}
