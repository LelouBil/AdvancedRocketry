package zmaster587.advancedRocketry.tile.atmosphere;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.AdvancedRocketryFluids;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.tile.IComparatorOverride;
import zmaster587.libVulpes.util.FluidUtils;

import java.util.ArrayList;
import java.util.List;

public class TileAirMixSupplier extends TileOxygenVentSystemBase implements IComparatorOverride, IModularInventory {

	public TileAirMixSupplier() {}

	public boolean canPerformFunction(AtmosphereHandler handler, World world, int remainingPower, float currentTotalPressure) {
		return currentTotalPressure < 100 && remainingPower >= 40;
	}

	public int performFunction(AtmosphereHandler handler, World world, int addedVolumeThisTick) {
		return (tank.drain(addedVolumeThisTick * 4, false) != null ? tank.drain(addedVolumeThisTick * 4, true).amount : 0)/10;
	}

	@Override
	public boolean canFill(Fluid fluid) {
		return FluidUtils.areFluidsSameType(fluid, AdvancedRocketryFluids.fluidNitrogen) && super.canFill(fluid);
	}

	@Override
	public String getModularInventoryName() {
		return AdvancedRocketryBlocks.blockAirMixSupplier.getLocalizedName();
	}

	@Override
	public boolean canInteractWithContainer(EntityPlayer entity) {
		return true;
	}

	@Override
	public List<ModuleBase> getModules(int ID, EntityPlayer player) {
		ArrayList<ModuleBase> modules = new ArrayList<>();

		modules.add(new ModuleSlotArray(52, 20, this, 0, 1));
		modules.add(new ModuleSlotArray(52, 57, this, 1, 2));
		modules.add(new ModuleLiquidIndicator(32, 20, this));

		return modules;
	}
}
