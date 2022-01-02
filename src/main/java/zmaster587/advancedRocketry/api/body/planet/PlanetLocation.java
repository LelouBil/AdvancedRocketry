package zmaster587.advancedRocketry.api.body.planet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ResourceLocation;

public class PlanetLocation {
    //Locational technical properties
    public final ResourceLocation star;
    public final ResourceLocation parent;
    public final ResourceLocation dimension;
    //Orbit
    public final double orbitalRho;
    public final double orbitalPhi;
    public final double orbitalTheta;

    public PlanetLocation(ResourceLocation star, ResourceLocation parent, ResourceLocation dimension, double orbitalRho, double orbitalPhi, double orbitalTheta) {
        this.star = star;
        this.parent = parent;
        this.dimension = dimension;

        this.orbitalRho = orbitalRho;
        this.orbitalPhi = orbitalPhi;
        this.orbitalTheta = orbitalTheta;
    }

    public static Codec<PlanetLocation> getCodec(ResourceLocation star, ResourceLocation parent) {
        return RecordCodecBuilder
                .create(planetLocationInstance -> planetLocationInstance.group(

                        ResourceLocation.CODEC.fieldOf("dimension").forGetter(o -> o.dimension),
                        Codec.DOUBLE.fieldOf("orbitalRho").forGetter(o -> o.orbitalRho),
                        Codec.DOUBLE.fieldOf("orbitalPhi").forGetter(o -> o.orbitalPhi),
                        Codec.DOUBLE.fieldOf("orbitalTheta").forGetter(o -> o.orbitalTheta)
                ).apply(planetLocationInstance,((location, aDouble, aDouble2, aDouble3) ->
                        new PlanetLocation(star,parent,location,aDouble,aDouble2,aDouble3))));
    }
}
