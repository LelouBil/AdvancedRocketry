package zmaster587.advancedRocketry.api.body.planet;

import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;

import java.util.List;

public class PlanetResourceProperties {
    //Planetary properties, resources
    public final List<ItemStack> laserDrillOres;
    public final List<ItemStack> craterOres;
    public final List<ItemStack> geodeOres;
    public final List<Fluid> gasses;
    public final List<ItemStack> requiredArtifacts;

    public PlanetResourceProperties(List<ItemStack> laserDrillOres, List<ItemStack> craterOres, List<ItemStack> geodeOres, List<Fluid> gasses, List<ItemStack> requiredArtifacts) {
        this.laserDrillOres = laserDrillOres;
        this.craterOres = craterOres;
        this.geodeOres = geodeOres;
        this.gasses = gasses;
        this.requiredArtifacts = requiredArtifacts;
    }
}
