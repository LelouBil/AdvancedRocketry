package zmaster587.advancedRocketry.api.body.planet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.Optional;

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

    private static final Codec<float[]> HexStringCodec = Codec.STRING.flatXmap(PlanetRenderingProperties::stringToColor, PlanetRenderingProperties::colorToString);


    public static final Codec<PlanetRenderingProperties> CODEC = RecordCodecBuilder.create(
            planetRenderingPropertiesInstance -> planetRenderingPropertiesInstance.group(
                    Codec.BOOL.fieldOf("hasCustomSky").forGetter(o -> o.hasCustomSky),
                    Codec.BOOL.fieldOf("hasAsteroidSky").forGetter(o -> o.hasAsteroidSky),
                    ResourceLocation.CODEC.fieldOf("texture").forGetter(o -> o.texture),
                    ResourceLocation.CODEC.fieldOf("textureOrbit").forGetter(o -> o.textureOrbit),
                    HexStringCodec.fieldOf("skyColor").forGetter(o -> o.skyColor),
                    HexStringCodec.fieldOf("fogColor").forGetter(o -> o.fogColor),
                    HexStringCodec.fieldOf("ringColor").forGetter(o -> o.ringColor)
            ).apply(planetRenderingPropertiesInstance, PlanetRenderingProperties::new)
    );

    private static DataResult<String> colorToString(float[] s) {
        String hexStringWithAlpha = Integer.toHexString(new Color(s[0], s[1], s[2]).getRGB());
        return DataResult.success("0x" + hexStringWithAlpha.substring(2));
    }

    private static DataResult<float[]> stringToColor(String s) {
        try {
            return DataResult.success(Color.decode(s).getRGBColorComponents(null));
        } catch (NumberFormatException ex) {
            return DataResult.error("String was not an hexadecimal color");
        }
    }

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
