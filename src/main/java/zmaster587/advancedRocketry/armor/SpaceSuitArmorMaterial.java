package zmaster587.advancedRocketry.armor;

import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import zmaster587.advancedRocketry.api.Constants;

import javax.annotation.Nonnull;

public class SpaceSuitArmorMaterial implements IArmorMaterial {

   @Override
   public int getDurability(@Nonnull EquipmentSlotType slotIn) {
      switch(slotIn) {
         case FEET:
            return 520;
         case LEGS:
            return 600;
         case CHEST:
            return 640;
         case HEAD:
            return 440;
      }
      return 0;
   }

   @Override
   public int getDamageReductionAmount(EquipmentSlotType slotIn) {
      switch(slotIn) {
         case FEET:
         case HEAD:
            return 1;
         case LEGS:
         case CHEST:
            return 2;
      }
      return 0;
   }

   @Override
   public int getEnchantability() {
      return 0;
   }

   @Nonnull
   @Override
   public SoundEvent getSoundEvent() {
      return SoundEvents.ITEM_ARMOR_EQUIP_ELYTRA;
   }

   @Nonnull
   @Override
   public Ingredient getRepairMaterial() {
      return Ingredient.EMPTY;
   }

   @Nonnull
   @Override
   public String getName() {
      return Constants.modId+":spacesuit";
   }

   @Override
   public float getToughness() {
      return 2.5f;
   }

   @Override
   public float getKnockbackResistance() {
      return 0.2f;
   }
}
