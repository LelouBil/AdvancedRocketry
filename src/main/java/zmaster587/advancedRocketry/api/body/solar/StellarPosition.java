package zmaster587.advancedRocketry.api.body.solar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ResourceLocation;

public class StellarPosition {
    //Locational technical properties
    public final ResourceLocation parent;

    //Galactic location

    public final double galacticRho;
    public final double galacticTheta;
    //Orbit

    public final double orbitalRho;
    public final double orbitalPhi;
    public final double orbitalTheta;

    //codec used for substars
    public static Codec<StellarPosition> getSubStarCODEC(ResourceLocation parent) {
        return RecordCodecBuilder.create(stellarPositionInstance -> stellarPositionInstance.group(
                Codec.DOUBLE.fieldOf("orbitalRho").forGetter(o -> o.orbitalRho),
                Codec.DOUBLE.fieldOf("orbitalPhi").forGetter(o -> o.orbitalPhi),
                Codec.DOUBLE.fieldOf("orbitalTheta").forGetter(o -> o.orbitalTheta)
        ).apply(stellarPositionInstance, ((orbitalRho, orbitalPhi, orbitalTheta) ->
                new StellarPosition(parent, 0, 0, orbitalRho, orbitalPhi, orbitalTheta))));
    }

    //codec used for top level stars
    public static Codec<StellarPosition> getNormalStarCODEC(ResourceLocation parent) {
        return RecordCodecBuilder.create(stellarPositionInstance ->
                stellarPositionInstance.group(
                        Codec.DOUBLE.fieldOf("galacticRho").forGetter(o -> o.galacticRho),
                        Codec.DOUBLE.fieldOf("galacticTheta").forGetter(o -> o.galacticTheta)
                        //codec building ends
                ).apply(stellarPositionInstance, (galacticRho, galacticTheta) ->
                        new StellarPosition(parent, galacticRho, galacticTheta, 0, 0, 0)
                ));
    }

    public static Codec<StellarPosition> getCODEC(ResourceLocation parent, boolean isSubStar) {
        return isSubStar? getSubStarCODEC(parent) : getNormalStarCODEC(parent);
    }

    public StellarPosition(ResourceLocation parent, double galacticRho, double galacticTheta, double orbitalRho, double orbitalPhi, double orbitalTheta) {
        this.parent = parent;

        this.galacticRho = galacticRho;
        this.galacticTheta = galacticTheta;

        this.orbitalRho = orbitalRho;
        this.orbitalPhi = orbitalPhi;
        this.orbitalTheta = orbitalTheta;
    }
}
