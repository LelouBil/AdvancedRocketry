package zmaster587.advancedRocketry.world.gen;

import net.minecraft.world.gen.trunkplacer.StraightTrunkPlacer;

public class WorldGenCharredTree extends StraightTrunkPlacer {
	
    /** The minimum height of a generated tree. */
    private final int minTreeHeight;
	
    public WorldGenCharredTree(int i, int j, int k, int minHeight) {
        super(i,j,k);
        this.minTreeHeight = minHeight;
    }
}
