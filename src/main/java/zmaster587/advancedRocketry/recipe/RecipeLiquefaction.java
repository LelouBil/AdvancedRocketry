package zmaster587.advancedRocketry.recipe;

import zmaster587.advancedRocketry.tile.multiblock.machine.TileLiquefactionPlant;
import zmaster587.libVulpes.recipe.RecipeMachineFactory;

public class RecipeLiquefaction extends RecipeMachineFactory {

	
	public static final RecipeLiquefaction INSTANCE = new RecipeLiquefaction();
	
	@Override
	public Class getMachine() {
		return TileLiquefactionPlant.class;
	}
}
