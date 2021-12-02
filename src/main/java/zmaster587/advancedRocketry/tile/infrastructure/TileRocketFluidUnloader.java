package zmaster587.advancedRocketry.tile.infrastructure;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import zmaster587.advancedRocketry.api.AdvancedRocketryTileEntityType;
import zmaster587.libVulpes.util.ZUtils.RedstoneState;

import java.util.List;

public class TileRocketFluidUnloader extends TileRocketFluidLoader {

	public TileRocketFluidUnloader() {
		super(AdvancedRocketryTileEntityType.TILE_FLUID_UNLOADER);
	}

	public TileRocketFluidUnloader(int size) {
		super(AdvancedRocketryTileEntityType.TILE_FLUID_UNLOADER, size);
	}

	@Override
	public String getModularInventoryName() {
		return "block.advancedrocketry.rocketfluidunloader";
	}
	
	@Override
	public void tick() {

		//Move a stack of items
		if( !world.isRemote && rocket != null ) {

			boolean isAllowToOperate = (inputstate == RedstoneState.OFF || isStateActive(inputstate, getStrongPowerForSides(world, getPos())));

			List<TileEntity> tiles = rocket.storage.getFluidTiles();
			boolean rocketFluidFull = false;

			//Function returns if something can be moved
			for(TileEntity tile : tiles) {
				IFluidHandler handler = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null).resolve().get();

				//See if we have anything to fill because redstone output
				FluidStack rocketFluid = handler.drain(1, FluidAction.SIMULATE);
				if(handler.fill(rocketFluid, FluidAction.SIMULATE) > 0)
					rocketFluidFull = true;

				if(isAllowToOperate) {
					boolean shouldOperate;
					if (!getFluidTank().getFluid().isEmpty())
						shouldOperate = getFluidTank().fill(handler.drain(new FluidStack(getFluidTank().getFluid(), getFluidTank().getCapacity() - getFluidTank().getFluidAmount()), FluidAction.SIMULATE), FluidAction.SIMULATE) > 0;
					else
						shouldOperate = getFluidTank().fill(handler.drain(getFluidTank().getCapacity(), FluidAction.SIMULATE), FluidAction.SIMULATE) > 0;

					if (shouldOperate)
						getFluidTank().fill(handler.drain(getFluidTank().getCapacity() - getFluidTank().getFluidAmount(), FluidAction.EXECUTE), FluidAction.EXECUTE);
				}
			}

			//Update redstone state
			setRedstoneState(!rocketFluidFull);

		}
	}
}
