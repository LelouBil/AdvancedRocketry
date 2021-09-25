package zmaster587.advancedRocketry.tile.atmosphere;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.relauncher.Side;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.AdvancedRocketryFluids;
import zmaster587.advancedRocketry.api.AdvancedRocketryItems;
import zmaster587.libVulpes.inventory.modules.*;
import zmaster587.libVulpes.tile.IComparatorOverride;
import zmaster587.libVulpes.tile.TileInventoriedRFConsumerDoubleTank;
import zmaster587.libVulpes.util.FluidUtils;
import zmaster587.libVulpes.util.IFluidHandlerInternal;
import zmaster587.libVulpes.util.INetworkMachine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TileCarbonDioxideScrubber extends TileInventoriedRFConsumerDoubleTank implements IComparatorOverride, IModularInventory, INetworkMachine, IFluidHandlerInternal {

	public TileCarbonDioxideScrubber() {
		super(16000, 5, 4000);
		inventory.setCanInsertSlot(4, false);
		inventory.setCanExtractSlot(4, false);
	}

	public boolean canPerformFunction() {
		return getStackInSlot(4).getItem() == AdvancedRocketryItems.itemCarbonScrubberCartridge && hasEnoughEnergy(getPowerPerOperation()) && tank.getFluidAmount() >= 20 && tank2.getFluidAmount() + 19 <= tank2.getCapacity();
	}

	@Override
	public void update() {
		super.update();
		FluidUtils.attemptDrainContainerIInv(inventory, this.tank, getStackInSlot(0), 0, 1);
		FluidUtils.attemptDrainContainerIInv(inventory, this.tank2, getStackInSlot(2), 2, 3);
	}

	public void performFunction() {
		if (!world.isRemote) {
			ItemStack stack = getStackInSlot(4);
			if (stack.getItemDamage() != stack.getMaxDamage()) {
				tank.drain(20, true);
				tank2.fill(new FluidStack(AdvancedRocketryFluids.fluidOxygen, 19), true);
				extractEnergy(getPowerPerOperation(), false);
				stack.setItemDamage(stack.getItemDamage() + 20);
				if ((32766 - stack.getItemDamage() + 2184) / 2185 != (32766 - stack.getItemDamage() + 1 + 2184) / 2185)
					this.markDirty();
			}
		}
	}

	@Override
	public int getComparatorOverride() {
		ItemStack stack = getStackInSlot(4);
		if (!stack.isEmpty()) {
			return (32766 - stack.getItemDamage() + 2184)/2185;
		}
		return 0;
	}

	@Override
	public int getPowerPerOperation() {
		return 200;
	}

	@Override
	@Nonnull
	public int[] getSlotsForFace(@Nullable EnumFacing side) {
		return new int[]{};
	}

	@Override
	public boolean isItemValidForSlot(int slot, @Nonnull ItemStack itemStack) {
		return slot != 1 && slot != 3 && ((slot != 4 && (itemStack.getItem() instanceof ItemBucket || itemStack.getItem() instanceof IFluidHandlerItem)) || (slot == 4 && itemStack.getItem() == AdvancedRocketryItems.itemCarbonScrubberCartridge));
	}

	@Override
	public int fillInternal(FluidStack resource, boolean doFill) {
		return this.canFill(resource.getFluid()) ? resource.getFluid() == AdvancedRocketryFluids.fluidCarbonDioxideRichAir ? this.tank.fill(resource, doFill) :  this.tank2.fill(resource, doFill): 0;
	}

	public FluidStack drainInternal(FluidStack resource, boolean doDrain) {
		return resource.getFluid() == AdvancedRocketryFluids.fluidCarbonDioxideRichAir ? this.tank.drain(resource, doDrain) : this.tank2.drain(resource.amount, doDrain);
	}

	@Override
	public String getModularInventoryName() {
		return AdvancedRocketryBlocks.blockCarbonDioxideScrubber.getLocalizedName();
	}

	@Override
	public List<ModuleBase> getModules(int ID, EntityPlayer player) {
		ArrayList<ModuleBase> modules = new ArrayList<>();

		modules.add(new ModulePower(18, 20, this));

		//Slots
		modules.add(new ModuleSlotArray(72, 38, this, 4, 5));
		modules.add(new ModuleSlotArray(112, 20, this, 2, 3));
		modules.add(new ModuleSlotArray(112, 57, this, 3, 4));
		modules.add(new ModuleSlotArray(52, 20, this, 0, 1));
		modules.add(new ModuleSlotArray(52, 57, this, 1, 2));
		//Fluids
		modules.add(new ModuleLiquidIndicator(32, 20, this));
		modules.add(new ModuleLiquidIndicator(92, 20, this, 1));

		return modules;
	}

	@Override
	public boolean canInteractWithContainer(EntityPlayer entity) {
		return true;
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos,getBlockMetadata(), getUpdateTag());

	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		handleUpdateTag(pkt.getNbtCompound());
	}

	@Override
	public void writeDataToNetwork(ByteBuf out, byte id) {}

	@Override
	public void readDataFromNetwork(ByteBuf in, byte packetId, NBTTagCompound nbt) {}

	@Override
	public void useNetworkData(EntityPlayer player, Side side, byte id, NBTTagCompound nbt) {}
}