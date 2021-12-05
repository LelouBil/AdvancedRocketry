package zmaster587.advancedRocketry.integration.jei.liquefaction;

import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import zmaster587.advancedRocketry.integration.jei.ARPlugin;
import zmaster587.advancedRocketry.integration.jei.MachineCategoryTemplate;
import zmaster587.advancedRocketry.integration.jei.MachineRecipe;
import zmaster587.advancedRocketry.inventory.TextureResources;
import zmaster587.libVulpes.LibVulpes;

import javax.annotation.Nonnull;

public class LiquefactionCategory extends MachineCategoryTemplate<MachineRecipe> {

	public LiquefactionCategory(IGuiHelper helper, ItemStack icon) {
		super(helper, TextureResources.crystallizerProgressBar, icon);
	}

	@Nonnull
	@Override
	public ResourceLocation getUid() {
		return ARPlugin.liquefactionPlantUUID;
	}

	@Nonnull
	@Override
	public Class<? extends MachineRecipe> getRecipeClass() {
		return MachineRecipe.class;
	}
	
	@Nonnull
	@Override
	public String getTitle() {
		return LibVulpes.proxy.getLocalizedString("block.advancedrocketry.liquefactionplant");
	}
}
