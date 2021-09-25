package zmaster587.advancedRocketry.integration.jei.liquefaction;

import mezz.jei.api.recipe.IRecipeHandler;
import mezz.jei.api.recipe.IRecipeWrapper;
import zmaster587.advancedRocketry.integration.jei.ARPlugin;

public class LiquefactionRecipeHandler implements IRecipeHandler<LiquefactionWrapper> {

	@Override
	public Class<LiquefactionWrapper> getRecipeClass() {
		return LiquefactionWrapper.class;
	}

	@Override
	public String getRecipeCategoryUid(LiquefactionWrapper recipe) {
		return ARPlugin.liquefactionUUID;
	}

	@Override
	public IRecipeWrapper getRecipeWrapper(LiquefactionWrapper recipe) {
		return recipe;
	}

	@Override
	public boolean isRecipeValid(LiquefactionWrapper recipe) {
		return true;
	}

}
