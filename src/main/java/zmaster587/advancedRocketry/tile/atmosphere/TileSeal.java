package zmaster587.advancedRocketry.tile.atmosphere;

import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.AdvancedRocketryTileEntityType;
import zmaster587.advancedRocketry.block.BlockSeal;

import java.util.Iterator;

public class TileSeal extends TileEntity implements ITickableTileEntity {

	public TileSeal() {
		super(AdvancedRocketryTileEntityType.TILE_SEAL);
	}

	boolean ticked = false;

	@Override
	public void onChunkUnloaded() {
		((BlockSeal) AdvancedRocketryBlocks.blockSeal).removeSeal(getWorld(), getPos());
		ticked = false;
	}
	
	@Override
	public void tick() {
		if(!world.isRemote && !ticked && !isRemoved()) {
			for(Direction dir : Direction.values()) {
				((BlockSeal) AdvancedRocketryBlocks.blockSeal).fireCheckAllDirections(getWorld(), pos.offset(dir), dir);
			}
			ticked = true;
		}
		for (Biome biome : DynamicRegistries.func_239770_b_().getRegistry(Registry.BIOME_KEY))
			System.out.println(biome.getRegistryName());
	}
}
