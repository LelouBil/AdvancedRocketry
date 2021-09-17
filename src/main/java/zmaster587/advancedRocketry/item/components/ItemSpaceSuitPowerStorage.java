package zmaster587.advancedRocketry.item.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import zmaster587.advancedRocketry.api.armor.IArmorComponentHeavy;
import zmaster587.libVulpes.api.IArmorComponent;
import zmaster587.libVulpes.client.ResourceIcon;
import zmaster587.libVulpes.util.UniversalBattery;

import javax.annotation.Nonnull;
import java.util.List;

public class ItemSpaceSuitPowerStorage extends Item implements IArmorComponentHeavy {

	//Equiavalent to 1000 FE, or 25.4 MJ -> this is a 20cm x 20cm x 30cm block of lithium-ion battery or so, manageable but large
	private static final int batterySizeMicroFE = 1000000;

	public ItemSpaceSuitPowerStorage() {
		super();
		setMaxStackSize(1);
	}


	@Override
	public void onTick(World world, EntityPlayer player, @Nonnull ItemStack armorStack, IInventory modules, @Nonnull ItemStack componentStack) {}

	@Override
	public int getTickedPowerConsumption(ItemStack component, Entity entity) {return 0;}

	@Override
	public boolean onComponentAdded(World world, @Nonnull ItemStack armorStack) {
		return true;
	}

	@Override
	public void onComponentRemoved(World world, @Nonnull ItemStack armorStack) {}

	@Override
	public void onArmorDamaged(EntityLivingBase entity, @Nonnull ItemStack armorStack, @Nonnull ItemStack componentStack, DamageSource source, int damage) {}

	@Override
	public boolean isAllowedInSlot(@Nonnull ItemStack componentStack, EntityEquipmentSlot targetSlot) {
		return targetSlot == EntityEquipmentSlot.LEGS || targetSlot == EntityEquipmentSlot.CHEST;
	}

	@Override
	public ResourceIcon getComponentIcon(@Nonnull ItemStack armorStack) {
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void renderScreen(@Nonnull ItemStack componentStack, List<ItemStack> modules, RenderGameOverlayEvent event, Gui gui) {}

	protected UniversalBattery loadBattery(@Nonnull ItemStack stack) {
		if(stack.hasTagCompound()) {
			UniversalBattery battery = new UniversalBattery(0);
			battery.readFromNBT(stack.getTagCompound());
			return battery;
		}
		return new UniversalBattery(0);
	}

	protected void saveBattery(@Nonnull ItemStack stack, UniversalBattery battery) {
		if(stack.hasTagCompound()) {
			battery.writeToNBT(stack.getTagCompound());
		}
		else {
			NBTTagCompound nbt = new NBTTagCompound();
			battery.writeToNBT(nbt);
			stack.setTagCompound(nbt);
		}
	}

	public int transferEnergy(ItemStack stack, int amount) {
		if (stack.getItem() instanceof ItemSpaceSuitPowerStorage) {
			if (stack.hasTagCompound()){
				//Get battery
				UniversalBattery battery = ((ItemSpaceSuitPowerStorage) stack.getItem()).loadBattery(stack);
				//Transfer and determine transfer amount
				int energy = battery.getUniversalEnergyStored();
				battery.setEnergyStored(Math.max(Math.min(battery.getUniversalEnergyStored(), battery.getMaxEnergyStored() - amount) + amount, 0));
				//Save battery
				saveBattery(stack, battery);

				//Return amount
				return Math.min(battery.getUniversalEnergyStored(), battery.getMaxEnergyStored()) - energy;
			} else {
				saveBattery(stack, new UniversalBattery(batterySizeMicroFE));
			}
		}
		return 0;
	}

	public boolean stackContainsPower(ItemStack stack) {
		if (stack.getItem() instanceof ItemSpaceSuitPowerStorage) {
			if (stack.hasTagCompound()){
				//Get battery
				UniversalBattery battery = ((ItemSpaceSuitPowerStorage) stack.getItem()).loadBattery(stack);
				return battery != null && battery.getUniversalEnergyStored() > 0;
			}
		}
		return false;
	}

	public int getStackContainedPower(ItemStack stack) {
		int totalPower = 0;
		if (stack.getItem() instanceof ItemSpaceSuitPowerStorage) {
			if (stack.hasTagCompound()){
				//Get battery
				UniversalBattery battery = ((ItemSpaceSuitPowerStorage) stack.getItem()).loadBattery(stack);
				totalPower += battery.getUniversalEnergyStored();
			}
		}
		return totalPower;
	}

	public int getStackMaxPower(ItemStack stack) {
		int totalPower = 0;
		if (stack.getItem() instanceof ItemSpaceSuitPowerStorage) {
			if (stack.hasTagCompound()){
				//Get battery
				UniversalBattery battery = ((ItemSpaceSuitPowerStorage) stack.getItem()).loadBattery(stack);
				totalPower += battery.getMaxEnergyStored();
			}
		}
		return totalPower;
	}

}
