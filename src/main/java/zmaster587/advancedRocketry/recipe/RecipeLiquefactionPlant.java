package zmaster587.advancedRocketry.recipe;

import zmaster587.advancedRocketry.tile.multiblock.machine.TileLiquefactionPlant;
import zmaster587.libVulpes.recipe.RecipeMachineFactory;

public class RecipeLiquefactionPlant extends RecipeMachineFactory {

	@Override
	public Class getMachine() {
		return TileLiquefactionPlant.class;
	}
}
