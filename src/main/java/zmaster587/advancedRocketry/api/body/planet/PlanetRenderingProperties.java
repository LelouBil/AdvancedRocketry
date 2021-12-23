package zmaster587.advancedRocketry.api.body.planet;

import net.minecraft.util.ResourceLocation;

public class PlanetRenderingProperties {
    //Rendering technical properties
    public final boolean hasCustomSky;
    public final boolean hasAsteroidSky;
    public final ResourceLocation texture;
    public final ResourceLocation textureOrbit;
    //Planetary properties (rendering)
    public final float[] skyColor;
    public final float[] fogColor;
    public final float[] ringColor;

    public PlanetRenderingProperties(boolean hasCustomSky, boolean hasAsteroidSky, ResourceLocation texture, ResourceLocation textureOrbit, float[] skyColor, float[] fogColor, float[] ringColor) {
        this.hasCustomSky = hasCustomSky;
        this.hasAsteroidSky = hasAsteroidSky;
        this.texture = texture;
        this.textureOrbit = textureOrbit;

        this.skyColor = skyColor;
        this.fogColor = fogColor;
        this.ringColor = ringColor;
    }
}
