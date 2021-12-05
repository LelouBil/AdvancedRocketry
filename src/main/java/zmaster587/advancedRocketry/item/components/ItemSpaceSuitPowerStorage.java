package zmaster587.advancedRocketry.item.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import zmaster587.advancedRocketry.api.armor.IArmorComponentHeavy;
import zmaster587.libVulpes.client.ResourceIcon;
import zmaster587.libVulpes.util.UniversalBattery;

import javax.annotation.Nonnull;
import java.util.List;

public class ItemSpaceSuitPowerStorage extends Item implements IArmorComponentHeavy {

	//Equiavalent to 100 FE, or 30.5 MJ -> this is a 20cm x 20cm x 30cm block of lithium-ion battery or so, manageable but large
	private static final int batterySizeMicroFE = 1200000;

	public ItemSpaceSuitPowerStorage(Properties props) {
		super(props);
	}

	@Override
	public void onTick(World world, PlayerEntity player, @Nonnull ItemStack armorStack, IInventory modules, @Nonnull ItemStack componentStack) {}

	@Override
	public int getTickedPowerConsumption(ItemStack component, Entity entity) {return 0;}

	@Override
	public boolean onComponentAdded(World world, @Nonnull ItemStack armorStack) {
		return true;
	}

	@Override
	public ItemStack onComponentRemoved(World world, @Nonnull ItemStack componentStack) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean isAllowedInSlot(@Nonnull ItemStack componentStack, EquipmentSlotType targetSlot) {
		return targetSlot == EquipmentSlotType.LEGS || targetSlot == EquipmentSlotType.CHEST;
	}

	@Override
	public ResourceIcon getComponentIcon(@Nonnull ItemStack armorStack) {
		return null;
	}

	@Override
	@OnlyIn(value= Dist.CLIENT)
	public void renderScreen(MatrixStack mat, ItemStack componentStack, List<ItemStack> modules, RenderGameOverlayEvent event, Screen gui) { }

	protected UniversalBattery loadBattery(@Nonnull ItemStack stack) {
		if(stack.hasTag()) {
			UniversalBattery battery = new UniversalBattery(0);
			battery.readFromNBT(stack.getTag());
			return battery;
		}
		return new UniversalBattery(0);
	}

	protected void saveBattery(@Nonnull ItemStack stack, UniversalBattery battery) {
		if(stack.hasTag()) {
			battery.write(stack.getTag());
		} else {
			CompoundNBT nbt = new CompoundNBT();
			battery.write(nbt);
			stack.setTag(nbt);
		}
	}

	public int transferEnergy(ItemStack stack, int amount) {
		if (stack.getItem() instanceof ItemSpaceSuitPowerStorage) {
			if (stack.hasTag()){
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
			if (stack.hasTag()){
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
			if (stack.hasTag()){
				//Get battery
				UniversalBattery battery = ((ItemSpaceSuitPowerStorage) stack.getItem()).loadBattery(stack);
				totalPower += battery.getUniversalEnergyStored();
			}
		}
		return totalPower;
	}

}