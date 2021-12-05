package zmaster587.advancedRocketry.item.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import zmaster587.advancedRocketry.api.armor.IArmorComponentChestLarge;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.libVulpes.client.ResourceIcon;

import javax.annotation.Nonnull;
import java.util.List;

public class ItemSolarWings extends Item implements IArmorComponentChestLarge  {

	public ItemSolarWings(Properties props) {
		super(props);
	}

	@Override
	public void onTick(World world, PlayerEntity player, @Nonnull ItemStack armorStack, IInventory inv, @Nonnull ItemStack componentStack) {
		if (world.canBlockSeeSky(player.getPosition()) && world.isDaytime() && !world.isRaining()) {
			//Extend to be able to produce power
			if (armorStack != ItemStack.EMPTY) {
				componentStack.setDamage(Math.min(componentStack.getDamage() + 1, componentStack.getMaxDamage()));
			} else
				componentStack.setDamage(Math.max(componentStack.getDamage() - 2, 0));
		} else {
			componentStack.setDamage(Math.max(componentStack.getDamage() - 2, 0));
		}
	}

	@Override
	public int getTickedPowerConsumption(ItemStack component, Entity entity) {
		//Due to 12.5m2 of solar panels, produce 10% of 1,000W * 6 m2 panels * 3.6s per tick * 1/25400 FE/t, times solar rate
		return component.getDamage() == component.getMaxDamage() ? (int)(200 * 1.0005 * DimensionManager.getEffectiveDimId(entity.world, new Vector3d(entity.getPosition().getX(), entity.getPosition().getY(), entity.getPosition().getZ())).getPeakInsolationMultiplier()) : 0;
	}

	@Override
	public boolean onComponentAdded(World world, @Nonnull ItemStack armorStack) {
		return true;
	}

	@Override
	public ItemStack onComponentRemoved(World world, @Nonnull ItemStack componentStack) {
		componentStack.setDamage(0);
		return componentStack;
	}

	@Override
	public boolean isAllowedInSlot(@Nonnull ItemStack stack, EquipmentSlotType slot) {
		return slot == EquipmentSlotType.CHEST;
	}

	@Override
	@OnlyIn(value=Dist.CLIENT)
	public void renderScreen(MatrixStack mat, ItemStack componentStack, List<ItemStack> modules, RenderGameOverlayEvent event, Screen gui) { }

	@Override
	public ResourceIcon getComponentIcon(@Nonnull ItemStack armorStack) {
		return null;
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack) {
		return (double)(stack.getMaxDamage() - stack.getDamage()) / (double)stack.getMaxDamage();
	}
}