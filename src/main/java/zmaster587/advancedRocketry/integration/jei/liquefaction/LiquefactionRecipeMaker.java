package zmaster587.advancedRocketry.integration.jei.liquefaction;

import mezz.jei.api.IJeiHelpers;
import zmaster587.libVulpes.interfaces.IRecipe;
import zmaster587.libVulpes.recipe.RecipesMachine;

import java.util.LinkedList;
import java.util.List;

public class LiquefactionRecipeMaker {

	public static List<LiquefactionWrapper> getMachineRecipes(IJeiHelpers helpers, Class clazz) {
		
		List<LiquefactionWrapper> list = new LinkedList<>();
		for(IRecipe rec : RecipesMachine.getInstance().getRecipes(clazz)) {
			list.add(new LiquefactionWrapper(rec));
		}
		
		return list;
	}
	
}
