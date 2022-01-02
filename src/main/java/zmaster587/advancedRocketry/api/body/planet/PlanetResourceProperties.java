package zmaster587.advancedRocketry.api.body.planet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;

import java.util.List;

public class PlanetResourceProperties {
    //Planetary properties, resources
    public final List<ItemStack> laserDrillOres;
    public final List<Block> craterOres;
    public final List<Block> geodeOres;
    public final List<Fluid> gasses;
    public final List<ItemStack> requiredArtifacts;

    /**
     * Returns a {@link Codec<T>} for the specified Vanilla registry, but that will always error instead of getting the default value
     * @param registry the {@link DefaultedRegistry<T>} to use as codec
     * @param <T> the type parameter of the registry
     * @return a codec, that will return {@link DataResult<T>#error(String)} on failure instead of the default value of the registry, and {@link DataResult<T>#success(Object)} on success
     */
    private static <T> Codec<T> getRegistryCodecThatCanError(DefaultedRegistry<T> registry) {
        return ResourceLocation.CODEC.flatXmap(r ->
                        registry.getOptional(r)
                                .map(DataResult::success)
                                .orElse(DataResult.error(String.format("Registry %s does not contain item with key %s", registry, r.toString())))
                , b -> DataResult.success(registry.getKey(b)));
    }


    @SuppressWarnings("deprecation")
    public static final Codec<PlanetResourceProperties> CODEC = RecordCodecBuilder
            .create(planetResourcePropertiesInstance ->
                    planetResourcePropertiesInstance.group(
                            ItemStack.CODEC.listOf().fieldOf("laserDrillOres").forGetter(o -> o.laserDrillOres),
                            getRegistryCodecThatCanError(Registry.BLOCK).listOf().fieldOf("craterOres").forGetter(o -> o.craterOres),
                            getRegistryCodecThatCanError(Registry.BLOCK).listOf().fieldOf("geodeOres").forGetter(o -> o.geodeOres),
                            getRegistryCodecThatCanError(Registry.FLUID).listOf().fieldOf("gasses").forGetter(o -> o.gasses),
                            ItemStack.CODEC.listOf().fieldOf("requiredArtifacts").forGetter(o -> o.requiredArtifacts)
                    ).apply(planetResourcePropertiesInstance, PlanetResourceProperties::new));

    public PlanetResourceProperties(List<ItemStack> laserDrillOres, List<Block> craterOres, List<Block> geodeOres, List<Fluid> gasses, List<ItemStack> requiredArtifacts) {
        this.laserDrillOres = laserDrillOres;
        this.craterOres = craterOres;
        this.geodeOres = geodeOres;
        this.gasses = gasses;
        this.requiredArtifacts = requiredArtifacts;
    }
}
