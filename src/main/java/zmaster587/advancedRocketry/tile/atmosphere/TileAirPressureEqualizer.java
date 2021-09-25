package zmaster587.advancedRocketry.tile.atmosphere;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.AdvancedRocketryFluids;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.tile.IComparatorOverride;

import java.util.ArrayList;
import java.util.List;

public class TileAirPressureEqualizer extends TileOxygenVentSystemBase implements IComparatorOverride, IModularInventory {

	public TileAirPressureEqualizer() {}

	public boolean canPerformFunction(AtmosphereHandler handler, World world, int remainingPower, float currentTotalPressure) {
		return currentTotalPressure > 100 && remainingPower >= 50 && (tank.getFluid() == null || tank.getCapacity() - 100 >= tank.getFluid().amount);
	}

	public int performFunction(AtmosphereHandler handler, World world, int addedVolumeThisTick) {
		Fluid producedFluid = DimensionManager.getInstance().getDimensionProperties(this.world.provider.getDimension()).hasOxygen ? AdvancedRocketryFluids.fluidCarbonDioxideRichAir : AdvancedRocketryFluids.fluidUnbreatheableAir;
		return tank.fill(new FluidStack(producedFluid, 50), true);
	}

	@Override
	public String getModularInventoryName() {
		return AdvancedRocketryBlocks.blockAirPressureEqualizer.getLocalizedName();
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
