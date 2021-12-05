package zmaster587.advancedRocketry.tile.multiblock.machine;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import zmaster587.advancedRocketry.api.AdvancedRocketryBlocks;
import zmaster587.advancedRocketry.api.AdvancedRocketryTileEntityType;
import zmaster587.advancedRocketry.inventory.TextureResources;
import zmaster587.advancedRocketry.util.AudioRegistry;
import zmaster587.libVulpes.api.LibVulpesBlocks;
import zmaster587.libVulpes.inventory.modules.ModuleBase;
import zmaster587.libVulpes.inventory.modules.ModuleProgress;
import zmaster587.libVulpes.tile.multiblock.TileMultiblockMachine;

import java.util.List;

public class TileLiquefactionPlant extends TileMultiblockMachine {

    public static final Object[][][] structure = new Object[][][] {
            {   {Blocks.AIR,                                 Blocks.AIR,                          Blocks.AIR,                          Blocks.AIR},
                    {LibVulpesBlocks.blockMachineStructure,        LibVulpesBlocks.blockMachineStructure, LibVulpesBlocks.blockMachineStructure, LibVulpesBlocks.blockMachineStructure},
                    {AdvancedRocketryBlocks.blockStructureTower, 'L',                                 'l',                                 AdvancedRocketryBlocks.blockStructureTower}},
            {   {Blocks.AIR,                                 Blocks.AIR,                          Blocks.AIR,                          Blocks.AIR},
                    {AdvancedRocketryBlocks.blockStructureTower, LibVulpesBlocks.blockMachineStructure, LibVulpesBlocks.blockMachineStructure, AdvancedRocketryBlocks.blockStructureTower},
                    {LibVulpesBlocks.motors,                     LibVulpesBlocks.blockMachineStructure, LibVulpesBlocks.blockMachineStructure, AdvancedRocketryBlocks.blockStructureTower}},
            {   {LibVulpesBlocks.blockMachineStructure,        LibVulpesBlocks.blockMachineStructure, 'c',                                 'P'},
                    {LibVulpesBlocks.blockMachineStructure,        LibVulpesBlocks.blockMachineStructure, LibVulpesBlocks.blockMachineStructure, LibVulpesBlocks.blockMachineStructure},
                    {AdvancedRocketryBlocks.blockStructureTower, LibVulpesBlocks.blockMachineStructure, 'l',                                 AdvancedRocketryBlocks.blockStructureTower}}
    };

    public TileLiquefactionPlant() {
        super(AdvancedRocketryTileEntityType.TILE_LIQUEFACTION_PLANT);
    }

    @Override
    public Object[][][] getStructure() {
        return structure;
    }

    @Override
    public List<ModuleBase> getModules(int ID, PlayerEntity player) {
        List<ModuleBase> modules = super.getModules(ID, player);

        modules.add(new ModuleProgress(70, 20, 0, TextureResources.rollingMachineProgressBar, this));
        return modules;
    }


    @Override
    public SoundEvent getSound() {
        return AudioRegistry.crystallizer;
    }

    @Override
    public int getSoundDuration() {
        return 30;
    }

    @Override
    public boolean shouldHideBlock(World world, BlockPos pos, BlockState tile) { return true; }

    @Override
    public String getMachineName() {
        return "block.advancedrocketry.liquefactionplant";
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {

        return new AxisAlignedBB(pos.add(-4,-4,-4), pos.add(4,4,4));
    }

}