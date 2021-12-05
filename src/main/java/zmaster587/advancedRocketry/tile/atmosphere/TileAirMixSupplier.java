package zmaster587.advancedRocketry.tile.atmosphere;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.api.AdvancedRocketryFluids;
import zmaster587.advancedRocketry.api.AdvancedRocketryTileEntityType;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;
import zmaster587.libVulpes.api.LibvulpesGuiRegistry;
import zmaster587.libVulpes.inventory.ContainerModular;
import zmaster587.libVulpes.inventory.GuiHandler;
import zmaster587.libVulpes.inventory.modules.IModularInventory;
import zmaster587.libVulpes.inventory.modules.ModuleBase;
import zmaster587.libVulpes.inventory.modules.ModuleLiquidIndicator;
import zmaster587.libVulpes.inventory.modules.ModuleSlotArray;
import zmaster587.libVulpes.tile.IComparatorOverride;
import zmaster587.libVulpes.util.FluidUtils;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

public class TileAirMixSupplier extends TileOxygenSystemBase implements IComparatorOverride, IModularInventory {

    public TileAirMixSupplier() {
        super(AdvancedRocketryTileEntityType.TILE_AIR_MIX_SUPPLIER);
    }

    public boolean canPerformFunction(AtmosphereHandler handler, World world, int remainingPower, float currentTotalPressure) {
        return currentTotalPressure < 100 && remainingPower >= 40;
    }

    public int performFunction(AtmosphereHandler handler, World world, int addedVolumeThisTick) {
        return (!tank.drain(addedVolumeThisTick * 4, FluidAction.SIMULATE).isEmpty() ? tank.drain(addedVolumeThisTick * 4, FluidAction.EXECUTE).getAmount() : 0)/10;
    }

    @Override
    public boolean canFill(Fluid fluid) {
        return FluidUtils.areFluidsSameType(fluid, AdvancedRocketryFluids.nitrogenStill.get()) && super.canFill(fluid);
    }

    @Override
    public String getModularInventoryName() {
        return "block.advancedrocketry.airmixsupplier";
    }

    @Override
    public boolean canInteractWithContainer(PlayerEntity entity) {
        return true;
    }

    @Override
    public List<ModuleBase> getModules(int ID, PlayerEntity player) {
        ArrayList<ModuleBase> modules = new ArrayList<>();

        modules.add(new ModuleSlotArray(52, 20, this, 0, 1));
        modules.add(new ModuleSlotArray(52, 57, this, 1, 2));
        modules.add(new ModuleLiquidIndicator(32, 20, this));

        return modules;
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