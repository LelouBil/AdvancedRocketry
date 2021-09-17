package zmaster587.advancedRocketry.armor;

import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.item.components.ItemSpaceSuitPowerStorage;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.util.EmbeddedInventory;

import javax.annotation.Nonnull;
import java.util.List;

public class ItemPoweredSpaceArmor extends ItemSpaceArmor {

	public ItemPoweredSpaceArmor(ArmorMaterial material, EntityEquipmentSlot component, int numModules) {
		super(material, component, numModules);
	}

	@Override
	public void addInformation(@Nonnull ItemStack stack, World p_77624_2_, List<String> list, ITooltipFlag p_77624_4_) {
		super.addInformation(stack, p_77624_2_, list, p_77624_4_);

		list.add(LibVulpes.proxy.getLocalizedString("msg.energy") + " " + ((ItemPoweredSpaceArmor)stack.getItem()).getTotalPower(stack)/1000 + "FE");
	}

	public int transferEnergy(@Nonnull ItemStack armor, int energyDelta) {
		if(armor.hasTagCompound()) {
			EmbeddedInventory inv = loadEmbeddedInventory(armor);
			for(int i = 0; i < inv.getSizeInventory(); i++ ) {
				ItemStack stack = inv.getStackInSlot(i);
				if(!stack.isEmpty() && stack.getItem() instanceof ItemSpaceSuitPowerStorage && energyDelta != 0) {
					ItemSpaceSuitPowerStorage item = (ItemSpaceSuitPowerStorage)stack.getItem();
					energyDelta -= item.transferEnergy(stack, energyDelta);
				}
			}
			saveEmbeddedInventory(armor, inv);
			return energyDelta;
		}
		return 0;
	}

	public boolean armorPieceHasEnergy(@Nonnull ItemStack armor) {
		if(armor.hasTagCompound()) {
			EmbeddedInventory inv = loadEmbeddedInventory(armor);
			for(int i = 0; i < inv.getSizeInventory(); i++ ) {
				ItemStack stack = inv.getStackInSlot(i);
				if(!stack.isEmpty() && stack.getItem() instanceof ItemSpaceSuitPowerStorage && ((ItemSpaceSuitPowerStorage) stack.getItem()).stackContainsPower(stack)) {
					return true;
				}
			}
		}
		return false;
	}

	public int getTotalPower(@Nonnull ItemStack armor) {
		int totalPower = 0;
		if(armor.hasTagCompound()) {
			EmbeddedInventory inv = loadEmbeddedInventory(armor);
			for(int i = 0; i < inv.getSizeInventory(); i++ ) {
				ItemStack stack = inv.getStackInSlot(i);
				if(!stack.isEmpty() && stack.getItem() instanceof ItemSpaceSuitPowerStorage) {
					totalPower += ((ItemSpaceSuitPowerStorage) stack.getItem()).getStackContainedPower(stack);
				}
			}
		}
		return totalPower;
	}

	public boolean isArmorAtTotalPower(@Nonnull ItemStack armor) {
		int totalPower = 0;
		if(armor.hasTagCompound()) {
			EmbeddedInventory inv = loadEmbeddedInventory(armor);
			for(int i = 0; i < inv.getSizeInventory(); i++ ) {
				ItemStack stack = inv.getStackInSlot(i);
				if(!stack.isEmpty() && stack.getItem() instanceof ItemSpaceSuitPowerStorage) {
					totalPower += ((ItemSpaceSuitPowerStorage) stack.getItem()).getStackMaxPower(stack);
				}
			}
		}
		return totalPower == getTotalPower(armor);
	}

}
