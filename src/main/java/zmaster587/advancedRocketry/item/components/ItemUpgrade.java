package zmaster587.advancedRocketry.item.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.AdvancedRocketryItems;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.libVulpes.api.IArmorComponent;
import zmaster587.libVulpes.client.ResourceIcon;

import javax.annotation.Nonnull;
import java.util.List;

public class ItemUpgrade extends Item implements IArmorComponent {
	
	public ItemUpgrade(Properties props) {
		super(props);
	}

	@Override
	public void onTick(World world, PlayerEntity player, ItemStack armorStack, IInventory modules, ItemStack componentStack) {
		if(componentStack.getItem() == AdvancedRocketryItems.itemPaddedBootsUpgrade && (!ARConfiguration.getCurrentConfig().lowGravityBoots.get() || DimensionManager.getInstance().getDimensionProperties(world).getGravitationalMultiplier() < 1f))
			player.fallDistance = 0;
	}

	@Override
	public boolean onComponentAdded(World world, @Nonnull ItemStack armorStack) {
		return true;
	}

	@Override
	public ItemStack onComponentRemoved(World world, @Nonnull ItemStack componentStack) {
		return ItemStack.EMPTY;
	}

	@Override
	public int getTickedPowerConsumption(ItemStack component, Entity entity) {
		if (component.getItem() == AdvancedRocketryItems.itemHoverUpgrade)
			return ItemJetpack.isActiveStatic(component, (PlayerEntity) entity) && ItemJetpack.isEnabledStatic(component) ? 20 : 0;
		else if (component.getItem() == AdvancedRocketryItems.itemFlightSpeedUpgrade)
			return ItemJetpack.isActiveStatic(component, (PlayerEntity) entity) ? 60 : 0;
		else if (component.getItem() == AdvancedRocketryItems.itemBionicLegsUpgrade)
			//Handled elsewhere due to speed modification being the primary power draw
			return 0;
		else if (component.getItem() == AdvancedRocketryItems.itemPaddedBootsUpgrade)
			return 0;
		else if (component.getItem() == AdvancedRocketryItems.itemAntiFogVisorUpgrade)
			return 20;
        return 0;
	}

	@Override
	public boolean isAllowedInSlot(ItemStack componentStack, EquipmentSlotType targetSlot) {
		if(componentStack.getItem() == AdvancedRocketryItems.itemBionicLegsUpgrade)
			return targetSlot == EquipmentSlotType.LEGS;
		else if(componentStack.getItem() == AdvancedRocketryItems.itemPaddedBootsUpgrade || componentStack.getItem() == AdvancedRocketryItems.itemFlightSpeedUpgrade)
			return targetSlot == EquipmentSlotType.FEET;
		return targetSlot == EquipmentSlotType.HEAD;
	}

	@Override
	public ResourceIcon getComponentIcon(@Nonnull ItemStack armorStack) {
		return null;
	}

	@Override
	@OnlyIn(value=Dist.CLIENT)
	public void renderScreen(MatrixStack mat, ItemStack componentStack, List<ItemStack> modules, RenderGameOverlayEvent event, Screen gui) { }
}
