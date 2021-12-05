package zmaster587.advancedRocketry.tile.atmosphere;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import zmaster587.advancedRocketry.api.AdvancedRocketryTileEntityType;
import zmaster587.advancedRocketry.armor.ItemPoweredSpaceArmor;
import zmaster587.libVulpes.api.LibvulpesGuiRegistry;
import zmaster587.libVulpes.gui.CommonResources;
import zmaster587.libVulpes.inventory.ContainerModular;
import zmaster587.libVulpes.inventory.GuiHandler;
import zmaster587.libVulpes.inventory.modules.IModularInventory;
import zmaster587.libVulpes.inventory.modules.ModuleBase;
import zmaster587.libVulpes.inventory.modules.ModuleImage;
import zmaster587.libVulpes.inventory.modules.ModulePower;
import zmaster587.libVulpes.tile.TileEntityFEConsumer;
import zmaster587.libVulpes.util.IconResource;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

public class TileChargePad extends TileEntityFEConsumer implements IModularInventory {

	public TileChargePad() {
		super(AdvancedRocketryTileEntityType.TILE_CHARGE_PAD, 16000);
	}

	@Override
	public int getPowerPerOperation() {
		return 0;
	}

	@Override
	public boolean canPerformFunction() {
		return !world.isRemote && energy.getUniversalEnergyStored() > 0;
	}

	@Override
	public void performFunction() {
		for(PlayerEntity player : this.world.getEntitiesWithinAABB(PlayerEntity.class, new AxisAlignedBB(pos, pos.add(1,2,1)))) {
			ItemStack stack = player.getItemStackFromSlot(EquipmentSlotType.CHEST);
			ItemStack stack2 = player.getItemStackFromSlot(EquipmentSlotType.LEGS);

			if(!stack.isEmpty()) {
				if(stack.getItem() instanceof ItemPoweredSpaceArmor) {
					if (((ItemPoweredSpaceArmor)stack.getItem()).transferEnergy(stack, 1000) == 0 && stack2.getItem() instanceof ItemPoweredSpaceArmor) {
						((ItemPoweredSpaceArmor)stack.getItem()).transferEnergy(stack2, 1000);
					}
					extractEnergy(1, false);
				}
			}
		}

	}

	@Override
	public List<ModuleBase> getModules(int ID, PlayerEntity player) {
		ArrayList<ModuleBase> modules = new ArrayList<>();
		modules.add(new ModulePower(18, 20, this.energy));
		if(world.isRemote)
			modules.add(new ModuleImage(49, 38, new IconResource(194, 0, 18, 18, CommonResources.genericBackground)));

		//modules.add(toggleSwitch = new ModuleToggleSwitch(160, 5, 0, "", this, TextureResources.buttonToggleImage, 11, 26, getMachineEnabled()));
		return modules;
	}

	@Override
	public String getModularInventoryName() {
		return "block.advancedrocketry.chargepad";
	}

	@Override
	public boolean canInteractWithContainer(PlayerEntity entity) {
		return true;
	}

	@Override
	public GuiHandler.guiId getModularInvType() {
		return GuiHandler.guiId.MODULAR;
	}

	@Nonnull
	@Override
	public ITextComponent getDisplayName() {
		return new TranslationTextComponent(getModularInventoryName());
	}

	@Override
	@ParametersAreNonnullByDefault
	public Container createMenu(int id, PlayerInventory inv, PlayerEntity player) {
		return new ContainerModular(LibvulpesGuiRegistry.CONTAINER_MODULAR_TILE, id, player, getModules(getModularInvType().ordinal(), player), this, getModularInvType());
	}
}