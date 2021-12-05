package zmaster587.advancedRocketry.armor;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.item.components.ItemSpaceSuitPowerStorage;
import zmaster587.libVulpes.util.EmbeddedInventory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class ItemPoweredSpaceArmor extends ItemSpaceArmor {

    public ItemPoweredSpaceArmor(Item.Properties props, IArmorMaterial material, EquipmentSlotType component, int numModules) {
        super(props, material, component, numModules);
    }

    @ParametersAreNonnullByDefault
    public void addInformation(@Nonnull ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag p_77624_4_) {
        super.addInformation(stack, world, list, p_77624_4_);

        list.add(new StringTextComponent("Total Energy: " + ((ItemPoweredSpaceArmor)stack.getItem()).getTotalPower(stack)/1000 + "FE"));
    }

    public int transferEnergy(@Nonnull ItemStack armor, int energyDelta) {
        if(armor.hasTag()) {
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
        if(armor.hasTag()) {
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
        if(armor.hasTag()) {
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

}