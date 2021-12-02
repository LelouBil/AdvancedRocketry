package zmaster587.advancedRocketry.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.gen.trunkplacer.StraightTrunkPlacer;

public class WorldGenAlienTree extends StraightTrunkPlacer {
	
   public static final Codec<WorldGenAlienTree> codec = RecordCodecBuilder.create((p_236902_0_) -> getAbstractTrunkCodec(p_236902_0_).apply(p_236902_0_, WorldGenAlienTree::new));

	public WorldGenAlienTree(int i, int j, int k) {
		//3, 11, 0
		super(i, j, k);
	}
}
