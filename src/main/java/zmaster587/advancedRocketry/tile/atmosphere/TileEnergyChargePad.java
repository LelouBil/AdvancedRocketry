package zmaster587.advancedRocketry.tile.atmosphere;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.AdvancedRocketryFluids;
import zmaster587.advancedRocketry.api.armor.IFillableArmor;
import zmaster587.advancedRocketry.armor.ItemPoweredSpaceArmor;
import zmaster587.advancedRocketry.util.ItemAirUtils;
import zmaster587.libVulpes.api.IModularArmor;
import zmaster587.libVulpes.gui.CommonResources;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.tile.TileEntityRFConsumer;
import zmaster587.libVulpes.util.FluidUtils;
import zmaster587.libVulpes.util.IconResource;

import java.util.ArrayList;
import java.util.List;

public class TileEnergyChargePad extends TileEntityRFConsumer implements IModularInventory {
	public TileEnergyChargePad() {
		super(16000);
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
		for( EntityPlayer player : this.world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(pos, pos.add(1,2,1)))) {
			ItemStack stack = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
			ItemStack stack2 = player.getItemStackFromSlot(EntityEquipmentSlot.LEGS);

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
	public List<ModuleBase> getModules(int ID, EntityPlayer player) {
		ArrayList<ModuleBase> modules = new ArrayList<>();
		modules.add(new ModulePower(18, 20, this.energy));
		if(world.isRemote)
			modules.add(new ModuleImage(49, 38, new IconResource(194, 0, 18, 18, CommonResources.genericBackground)));

		//modules.add(toggleSwitch = new ModuleToggleSwitch(160, 5, 0, "", this, TextureResources.buttonToggleImage, 11, 26, getMachineEnabled()));
		return modules;
	}

	@Override
	public String getModularInventoryName() {
		return AdvancedRocketryBlocks.blockEnergyCharger.getLocalizedName();
	}

	@Override
	public boolean canInteractWithContainer(EntityPlayer entity) {
		return true;
	}
}
