package zmaster587.advancedRocketry.api.armor;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import zmaster587.advancedRocketry.api.IAtmosphere;
import zmaster587.libVulpes.api.IArmorComponent;

import javax.annotation.Nonnull;

public interface IArmorComponentHeavy extends IArmorComponent {
	//This just exists to differentiate heavy modules from non-heavy ones (ie ones that don't make the player slower)
}
