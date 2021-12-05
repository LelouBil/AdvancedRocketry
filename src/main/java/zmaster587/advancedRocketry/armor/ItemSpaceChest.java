package zmaster587.advancedRocketry.armor;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import zmaster587.advancedRocketry.api.AdvancedRocketryFluids;
import zmaster587.advancedRocketry.api.AdvancedRocketryItems;
import zmaster587.advancedRocketry.api.IAtmosphere;
import zmaster587.advancedRocketry.api.armor.IArmorComponentChestLarge;
import zmaster587.advancedRocketry.api.armor.IFillableArmor;
import zmaster587.advancedRocketry.inventory.TextureResources;
import zmaster587.advancedRocketry.item.components.ItemJetpack;
import zmaster587.libVulpes.util.EmbeddedInventory;
import zmaster587.libVulpes.util.FluidUtils;
import zmaster587.libVulpes.util.IconResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedList;
import java.util.List;

public class ItemSpaceChest extends ItemPoweredSpaceArmor implements IFillableArmor {

	public ItemSpaceChest(Item.Properties props, IArmorMaterial material, EquipmentSlotType component, int numModules) {
		super(props, material, component, numModules);
	}

	@Override
	@ParametersAreNonnullByDefault
	public void addInformation(@Nonnull ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag p_77624_4_) {
		super.addInformation(stack, world, list, p_77624_4_);

		list.add(new StringTextComponent("Oxygen: " + getAirRemaining(stack) + "mB"));
	}



	@Override
	public boolean isItemValidForSlot(@Nonnull ItemStack stack, int slot) {
		if(slot >= 3 && !(stack.getItem() instanceof IArmorComponentChestLarge))
			return true;
		else if (slot == 2)
			return true;

		FluidStack fstack;
		
		LazyOptional<IFluidHandlerItem> cap = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY);
		
		if(stack.isEmpty() || !cap.isPresent())
			return false;
		
		fstack = cap.orElse(null).getFluidInTank(0);
		
		return (fstack.isEmpty() || FluidUtils.areFluidsSameType(fstack.getFluid(), AdvancedRocketryFluids.oxygenFlowing.get()));
	}

	@Override
	public boolean canBeExternallyModified(@Nonnull ItemStack armor, int slot) {
		return slot >= 2;
	}

	@Override
	public IconResource getResourceForSlot(int slot) {
		if(slot < 2)
			return TextureResources.slotO2;
		return null;
	}

	/**
	 * gets the amount of air remaining in the suit.
	 * @param stack stack from which to get an amount of air
	 * @return the amount of air in the stack
	 */
	@Override
	public int getAirRemaining(@Nonnull ItemStack stack) {

		List<ItemStack> list = getComponents(stack);

		int airRemaining = 0;

		for(ItemStack component : list) {
			if(FluidUtils.containsFluid(component, AdvancedRocketryFluids.oxygenStill.get())) {
				airRemaining += FluidUtils.getFluidForItem(component).getAmount();
			}
		}

		return airRemaining;
	}

	/**
	 * Sets the amount of air remaining in the suit (WARNING: DOES NOT BOUNDS CHECK!)
	 * @param stack the stack to operate on
	 * @param amt amount of air to set the suit to
	 */
	@Override
	public void setAirRemaining(@Nonnull ItemStack stack, int amt) { }

	/**
	 * Decrements air in the suit by amt
	 * @param stack the item stack to operate on
	 * @param amt amount of air by which to decrement
	 * @return The amount of air extracted from the suit
	 */
	@Override
	public int decrementAir(@Nonnull ItemStack stack, int amt) {
		if(stack.hasTag()) {
			EmbeddedInventory inv = new EmbeddedInventory(getNumSlots(stack));
			inv.readFromNBT(stack.getTag());
			List<ItemStack> list = new LinkedList<>();

			for(int i = 0; i < inv.getSizeInventory(); i++) {
				if(!inv.getStackInSlot(i).isEmpty())
					list.add(inv.getStackInSlot(i));
			}
			int amtDrained = amt;
			for(ItemStack component : list) {
				
				LazyOptional<IFluidHandlerItem> cap = component.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY);
				
				if(cap.isPresent()) {
					IFluidHandlerItem fluidItem = cap.orElse(null);
					FluidStack fluidStack = FluidUtils.getFluidForItem(component);

					FluidStack fluidDrained = null;

					if(fluidStack != null && FluidUtils.areFluidsSameType(fluidStack.getFluid(), AdvancedRocketryFluids.oxygenStill.get()))
						fluidDrained = fluidItem.drain(amtDrained, FluidAction.EXECUTE);

					if(fluidDrained != null)
						amtDrained -= fluidDrained.getAmount();

					if(amtDrained == 0)
						break;
				}
			}
			saveEmbeddedInventory(stack, inv);
			return amt - amtDrained;

		}
		return 0;
	}

	/**
	 * Increments air in the suit by amt
	 * @param stack the item stack to operate on
	 * @param amt amount of air by which to decrement
	 * @return The amount of air inserted into the suit
	 */
	@Override
	public int increment(@Nonnull ItemStack stack, int amt) {
		if(stack.hasTag()) {
			EmbeddedInventory inv = new EmbeddedInventory(getNumSlots(stack));
			inv.readFromNBT(stack.getTag());
			List<ItemStack> list = new LinkedList<>();

			//Fill up extra oxygen tanks IF there is no jetpack
			boolean jetpack = !inv.getStackInSlot(2).isEmpty() && inv.getStackInSlot(2).getItem() instanceof ItemJetpack;
			for(int i = 0; i < inv.getSizeInventory(); i++) {
				if(!inv.getStackInSlot(i).isEmpty()) {
					
					LazyOptional<IFluidHandlerItem> cap = inv.getStackInSlot(i).getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY);
					
					if(i < 2 || !jetpack) {
						final int count = i;
						cap.ifPresent(value -> list.add(inv.getStackInSlot(count)));
					} else if(FluidUtils.containsFluid(inv.getStackInSlot(i))) {
						FluidStack fstack = FluidUtils.getFluidForItem(inv.getStackInSlot(i));
						if(!fstack.isEmpty() && FluidUtils.areFluidsSameType(fstack.getFluid(), AdvancedRocketryFluids.oxygenStill.get()))
							list.add(inv.getStackInSlot(i));
					}
					
				}
			}

			int amtDrained = amt;
			//At this point the list contains ONLY capable items
			for(ItemStack component : list) {
				IFluidHandlerItem fHandler = component.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).orElse(null);
				FluidStack fluidStack = fHandler.getFluidInTank(0);

				if(fluidStack.isEmpty() || FluidUtils.areFluidsSameType(fluidStack.getFluid(), AdvancedRocketryFluids.oxygenStill.get()))
					amtDrained -= fHandler.fill(new FluidStack(AdvancedRocketryFluids.oxygenStill.get(), amtDrained), FluidAction.EXECUTE);

				if(amtDrained == 0)
					break;
			}
			saveEmbeddedInventory(stack, inv);

			return amt - amtDrained;
		}
		return 0;
	}

	/**
	 * @return the maximum amount of air allowed in this suit
	 */
	@Override
	public int getMaxAir(@Nonnull ItemStack stack) {

		if(stack.hasTag()) {
			EmbeddedInventory inv = new EmbeddedInventory(getNumSlots(stack));
			inv.readFromNBT(stack.getTag());
			List<ItemStack> list = new LinkedList<>();

			//Fill up extra oxygen tanks IF there is no jetpack
			boolean jetpack = !inv.getStackInSlot(2).isEmpty() && inv.getStackInSlot(2).getItem() instanceof ItemJetpack;
			for(int i = 0; i < inv.getSizeInventory(); i++) {
				if(!inv.getStackInSlot(i).isEmpty()) {
					LazyOptional<IFluidHandlerItem> cap = inv.getStackInSlot(i).getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY);
					
					if(i < 2 || !jetpack) {
						final int count = i;
						cap.ifPresent(value -> list.add(inv.getStackInSlot(count)));
					} else if(cap.isPresent()) {
						FluidStack fstack = FluidUtils.getFluidForItem(inv.getStackInSlot(i));
						if(fstack != null && FluidUtils.areFluidsSameType(fstack.getFluid(), AdvancedRocketryFluids.oxygenStill.get()))
							list.add(inv.getStackInSlot(i));
					}
				}
			}
			
			int maxAir = 0;
			for(ItemStack component : list) {
				LazyOptional<IFluidHandlerItem> cap = component.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY);
					
				IFluidHandlerItem fHandler = cap.orElse(null);
				FluidStack fluidStack = fHandler.getFluidInTank(0);

				if(fluidStack.isEmpty() || FluidUtils.areFluidsSameType(fluidStack.getFluid(), AdvancedRocketryFluids.oxygenStill.get()))
					maxAir += fHandler.getTankCapacity(0);
			}
			return maxAir;
		}
		return 0;
	}

	public boolean protectsFromSubstance(@Nonnull IAtmosphere atmosphere, @Nonnull ItemStack stack, Entity entity, boolean commitProtection) {
		if(!super.protectsFromSubstance(atmosphere, stack, entity, commitProtection))
		
		// Assume for now that the space suit has a built in O2 extractor and can magically handle pressure
		if(atmosphere.allowsCombustion())
			return true;
		
		// If the atmosphere allows for combustion, it probably has O2, TODO: atmosphere with non O2 oxidizers
		boolean commitAndDecrement = commitProtection && ((IFillableArmor)AdvancedRocketryItems.itemSpaceSuitChestpiece).decrementAir(stack, 1) > 0;
		boolean noncommitAndHasAir = !commitProtection && ((IFillableArmor)AdvancedRocketryItems.itemSpaceSuitChestpiece).getAirRemaining(stack) > 0;
		return noncommitAndHasAir || commitAndDecrement;
	}
}
