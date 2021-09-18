package zmaster587.advancedRocketry.item.components;

import net.minecraft.client.gui.Gui;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.libVulpes.api.IArmorComponent;
import zmaster587.libVulpes.client.ResourceIcon;
import zmaster587.libVulpes.items.ItemIngredient;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.List;

public class ItemUpgrade extends ItemIngredient implements IArmorComponent {

	public static final int hoverUpgradeDamage = 0;
	public static final int speedUpgradeDamage = 1;
	public static final int legUpgradeDamage = 2;
	public static final int bootsUpgradeDamage = 3;
	public static final int fogUpgradeDamage = 4;
	public static final int brightnessUpgradeDamage = 5;
	
	public ItemUpgrade(int num) {
		super(num);
		setMaxStackSize(1);
	}

	@Override
	public void onTick(World world, EntityPlayer player, @Nonnull ItemStack armorStack, IInventory modules, @Nonnull ItemStack componentStack) {
		if(componentStack.getItemDamage() == bootsUpgradeDamage && (!ARConfiguration.getCurrentConfig().lowGravityBoots || DimensionManager.getInstance().getDimensionProperties(world.provider.getDimension()).getGravitationalMultiplier() < 1f))
			player.fallDistance = 0;
	}

	@Override
	public int getTickedPowerConsumption(ItemStack component, Entity entity) {
		switch (component.getItemDamage()) {
			case hoverUpgradeDamage:
				return ItemJetpack.isActiveStatic(component, (EntityPlayer)entity) && ItemJetpack.isEnabledStatic(component) ? 20 : 0;
			case speedUpgradeDamage:
				return ItemJetpack.isActiveStatic(component, (EntityPlayer)entity) ? 60 : 0;
			case legUpgradeDamage:
				//Handled elsewhere due to speed modification being the primary power draw
				return 0;
			case bootsUpgradeDamage:
				return 0;
			case fogUpgradeDamage:
				return 20;
			case brightnessUpgradeDamage:
				return 50;
		}
		return 0;
	}

	@Override
	public boolean onComponentAdded(World world, @Nonnull ItemStack armorStack) {
		return true;
	}

	@Override
	public void onComponentRemoved(World world, @Nonnull ItemStack componentStack) { }

	@Override
	public void onArmorDamaged(EntityLivingBase entity, @Nonnull ItemStack armorStack, @Nonnull ItemStack componentStack, DamageSource source, int damage) {}

	@Override
	public boolean isAllowedInSlot(@Nonnull ItemStack componentStack, EntityEquipmentSlot targetSlot) {
		if(componentStack.getItemDamage() == legUpgradeDamage)
			return targetSlot == EntityEquipmentSlot.LEGS;
		else if(componentStack.getItemDamage() == bootsUpgradeDamage || componentStack.getItemDamage() == speedUpgradeDamage)
			return targetSlot == EntityEquipmentSlot.FEET;
		return targetSlot == EntityEquipmentSlot.HEAD;
	}

	@Override
	public ResourceIcon getComponentIcon(@Nonnull ItemStack armorStack) {
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void renderScreen(@Nonnull ItemStack componentStack, List<ItemStack> modules, RenderGameOverlayEvent event, Gui gui) {
		// TODO Auto-generated method stub
		
	}
}
