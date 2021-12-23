package zmaster587.advancedRocketry.api.body.solar;

import net.minecraft.util.ResourceLocation;

public class StellarPosition {
    //Locational technical properties
    public final ResourceLocation parent;
    public final ResourceLocation id;
    //Galactic location
    public final double galacticRho;
    public final double galacticTheta;
    //Orbit
    public final double orbitalRho;
    public final double orbitalPhi;
    public final double orbitalTheta;

    public StellarPosition(ResourceLocation parent, ResourceLocation id, double galacticRho, double galacticTheta, double orbitalRho, double orbitalPhi, double orbitalTheta) {
        this.parent = parent;
        this.id = id;

        this.galacticRho = galacticRho;
        this.galacticTheta = galacticTheta;

        this.orbitalRho = orbitalRho;
        this.orbitalPhi = orbitalPhi;
        this.orbitalTheta = orbitalTheta;
    }
}
