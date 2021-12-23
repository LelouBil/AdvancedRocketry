package zmaster587.advancedRocketry.api.body.planet;

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
}
