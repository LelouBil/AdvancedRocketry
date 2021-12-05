package zmaster587.advancedRocketry.tile.atmosphere;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import zmaster587.advancedRocketry.api.AdvancedRocketryFluids;
import zmaster587.advancedRocketry.api.AdvancedRocketryItems;
import zmaster587.advancedRocketry.api.AdvancedRocketryTileEntityType;
import zmaster587.libVulpes.api.LibvulpesGuiRegistry;
import zmaster587.libVulpes.inventory.ContainerModular;
import zmaster587.libVulpes.inventory.GuiHandler;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.tile.IComparatorOverride;
import zmaster587.libVulpes.tile.TileInventoriedFEConsumerDoubleTank;
import zmaster587.libVulpes.util.FluidUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

public class TileCarbonDioxideScrubber extends TileInventoriedFEConsumerDoubleTank implements IComparatorOverride, IModularInventory {

    public TileCarbonDioxideScrubber() {
        super(AdvancedRocketryTileEntityType.TILE_CARBON_DIOXIDE_SCRUBBER, 16000, 5, 4000);
        inventory.setCanInsertSlot(4, false);
        inventory.setCanExtractSlot(4, false);
    }

    public boolean canPerformFunction() {
        return getStackInSlot(4).getItem() == AdvancedRocketryItems.itemCarbonScrubberCartridge && hasEnoughEnergy(getPowerPerOperation()) && tank.getFluidAmount() >= 20 && tank2.getFluidAmount() + 19 <= tank2.getCapacity();
    }

    public void performFunction() {
        if (!world.isRemote) {
            ItemStack stack = getStackInSlot(4);
            if (stack.getDamage() != stack.getMaxDamage()) {
                tank.drain(20, FluidAction.EXECUTE);
                tank2.fill(new FluidStack(AdvancedRocketryFluids.oxygenStill.get(), 19), FluidAction.EXECUTE);
                extractEnergy(getPowerPerOperation(), false);
                stack.setDamage(stack.getDamage() + 20);
                if ((32766 - stack.getDamage() + 2184) / 2185 != (32766 - stack.getDamage() + 1 + 2184) / 2185)
                    this.markDirty();
            }
        }
    }

    @Override
    public int getComparatorOverride() {
        ItemStack stack = getStackInSlot(4);
        if (!stack.isEmpty()) {
            return (32766 - stack.getDamage() + 2184)/2185;
        }
        return 0;
    }

    @Override
    public int getPowerPerOperation() {
        return 200;
    }

    @Override
    @Nonnull
    public int[] getSlotsForFace(@Nullable Direction side) {
        return new int[]{0, 1, 2, 3};
    }

    @Override
    public boolean isItemValidForSlot(int slot, @Nonnull ItemStack itemStack) {
        return slot != 1 && slot != 3 && ((slot != 4 && (itemStack.getItem() instanceof BucketItem || itemStack.getItem() instanceof IFluidHandlerItem)) || (slot == 4 && itemStack.getItem() == AdvancedRocketryItems.itemCarbonScrubberCartridge));
    }

    @Override
    public void setInventorySlotContents(int slot, @Nonnull ItemStack stack) {
        super.setInventorySlotContents(slot, stack);
        if (slot == 0)
            useBucket(0, getStackInSlot(0));
        else if (slot == 2)
            useBucket(2, getStackInSlot(2));
    }

    /**
     * Handles internal bucket tank interaction
     * @param slot integer slot to insert into
     * @param stack the itemstack to work fluid handling on
     * @return boolean on whether the fluid stack was successfully filled from or not, returns false if the stack cannot be extracted from or has no fluid left
     */
    private boolean useBucket(int slot, @Nonnull ItemStack stack) {
        if (stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).isPresent()) {
            if (slot == 0) {
                IFluidHandlerItem fluidItem = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).orElse(null);
                FluidStack fluidStack = fluidItem.getFluidInTank(0);
                if (!fluidStack.isEmpty() && canFill(fluidStack.getFluid()) && tank.getFluidAmount() + fluidItem.getTankCapacity(0) <= tank.getCapacity()) {
                    return FluidUtils.attemptDrainContainerIInv(inventory, tank, stack, 0, 1);
                }
            } else if (slot == 2) {
                IFluidHandlerItem fluidItem = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).orElse(null);
                FluidStack fluidStack = fluidItem.getFluidInTank(0);
                if (!fluidStack.isEmpty() && canFill(fluidStack.getFluid()) && tank2.getFluidAmount() + fluidItem.getTankCapacity(0) <= tank2.getCapacity()) {
                    return FluidUtils.attemptDrainContainerIInv(inventory, tank2, stack, 2, 3);
                }
            }
        }
        return false;
    }

    @Override
    public String getModularInventoryName() {
        return "block.advancedrocketry.carbondioxidescrubber";
    }

    @Override
    public List<ModuleBase> getModules(int ID, PlayerEntity player) {
        ArrayList<ModuleBase> modules = new ArrayList<>();

        modules.add(new ModulePower(18, 20, this));

        //Slots
        modules.add(new ModuleSlotArray(72, 38, this, 4, 5));
        modules.add(new ModuleSlotArray(112, 20, this, 2, 3));
        modules.add(new ModuleSlotArray(112, 57, this, 3, 4, false));
        modules.add(new ModuleSlotArray(52, 20, this, 0, 1));
        modules.add(new ModuleSlotArray(52, 57, this, 1, 2, false));
        //Fluids
        modules.add(new ModuleLiquidIndicator(32, 20, this, 0));
        modules.add(new ModuleLiquidIndicator(92, 20, this, 1));

        return modules;
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

    @Override
    @Nonnull
    public CompoundNBT getUpdateTag() {
        return write(new CompoundNBT());
    }
}