package zmaster587.advancedRocketry.tile.multiblock.machine;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;
import zmaster587.advancedRocketry.api.*;
import zmaster587.advancedRocketry.armor.ItemSpaceArmor;
import zmaster587.advancedRocketry.inventory.TextureResources;
import zmaster587.advancedRocketry.recipe.RecipeChemicalReactor;
import zmaster587.advancedRocketry.util.AudioRegistry;
import zmaster587.libVulpes.api.LibVulpesBlocks;
import zmaster587.libVulpes.interfaces.IRecipe;
import zmaster587.libVulpes.inventory.modules.ModuleBase;
import zmaster587.libVulpes.inventory.modules.ModuleProgress;
import zmaster587.libVulpes.recipe.NumberedOreDictStack;
import zmaster587.libVulpes.recipe.RecipesMachine;
import zmaster587.libVulpes.tile.multiblock.TileMultiblockMachine;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

public class TileChemicalReactor extends TileMultiblockMachine {
	public static final Object[][][] structure = { 
		{{null, 'c',null},
			{'L', 'I','L'}},

			{{'P', LibVulpesBlocks.motors, 'P'}, 
				{'l', 'O', 'l'}},

	};

	public TileChemicalReactor() {
		super(AdvancedRocketryTileEntityType.TILE_CHEMICAL_REACTOR);
	}
	
	@Override
	public boolean shouldHideBlock(World world, BlockPos pos, BlockState tile) { return true; }

	public Object[][][] getStructure() {
		return structure;
	}

	@Override
	public SoundEvent getSound() {
		return AudioRegistry.rollingMachine;
	}

	@Override
	public int getSoundDuration() {
		return 30;
	}

	@Override
	@Nonnull
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(pos.add(-2,-2,-2), pos.add(2,2,2));
	}

	@Override
	public List<ModuleBase> getModules(int ID, PlayerEntity player) {
		List<ModuleBase> modules = super.getModules(ID, player);

		modules.add(new ModuleProgress(100, 4, 0, TextureResources.crystallizerProgressBar, this));
		return modules;
	}

	@Override
	public String getMachineName() {
		return "block.advancedrocketry.chemicalreactor";
	}
}
