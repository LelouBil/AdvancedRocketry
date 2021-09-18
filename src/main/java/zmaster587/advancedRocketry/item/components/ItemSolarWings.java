package zmaster587.advancedRocketry.item.components;

import net.minecraft.client.gui.Gui;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import zmaster587.advancedRocketry.api.armor.IArmorComponentChestLarge;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.inventory.TextureResources;
import zmaster587.libVulpes.client.ResourceIcon;

import javax.annotation.Nonnull;
import java.util.List;

public class ItemSolarWings extends Item implements IArmorComponentChestLarge  {

	public ItemSolarWings() {}

	@Override
	public void onTick(World world, EntityPlayer player, @Nonnull ItemStack armorStack, IInventory inv, @Nonnull ItemStack componentStack) {
		if (world.canBlockSeeSky(player.getPosition()) && world.isDaytime() && !world.isRaining()) {
			//Extend to be able to produce power
			if (((Math.abs(player.motionY) < 0.1f) && (0 == player.motionX) && (0 == player.motionZ))) {
				componentStack.setItemDamage(Math.min(componentStack.getItemDamage() + 1, componentStack.getMaxDamage()));
			} else if (!((Math.abs(player.motionY) < 0.09) && (0 == player.motionX) && (0 == player.motionZ)))
				componentStack.setItemDamage(Math.max(componentStack.getItemDamage() - 1, 0));
		} else {
			componentStack.setItemDamage(Math.max(componentStack.getItemDamage() - 1, 0));
		}
	}

	@Override
	public int getTickedPowerConsumption(ItemStack component, Entity entity) {
		//Due to 6m2 of solar panels, produce 20% of 1,000W * 6 m2 panels * 3.6s per tick * 1/25400 FE/t, times solar rate
		return component.getItemDamage() == component.getMaxDamage() ? (int)(180 * DimensionManager.getEffectiveDimId(entity.world, entity.getPosition()).getPeakInsolationMultiplier()) : 0;
	}

	@Override
	public boolean onComponentAdded(World world, @Nonnull ItemStack armorStack) {
		return true;
	}

	@Override
	public void onComponentRemoved(World world, @Nonnull ItemStack componentStack) {
		componentStack.setItemDamage(0);
	}

	@Override
	public void onArmorDamaged(EntityLivingBase entity, @Nonnull ItemStack armorStack, @Nonnull ItemStack componentStack, DamageSource source, int damage) {}

	@Override
	public boolean isAllowedInSlot(@Nonnull ItemStack stack, EntityEquipmentSlot slot) {
		return slot == EntityEquipmentSlot.CHEST;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void renderScreen(@Nonnull ItemStack componentStack, List<ItemStack> modules, RenderGameOverlayEvent event, Gui gui) {}

	@Override
	public ResourceIcon getComponentIcon(@Nonnull ItemStack armorStack) {
		return armorStack.getItemDamage() == armorStack.getMaxDamage() ? new ResourceIcon(TextureResources.solarIconOpen) : new ResourceIcon(TextureResources.solarIconClosed);
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack) {
		return (double)(stack.getMaxDamage() - stack.getItemDamage()) / (double)stack.getMaxDamage();
	}
}
