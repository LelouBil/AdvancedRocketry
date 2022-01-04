package zmaster587.advancedRocketry.api.body.solar;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import zmaster587.advancedRocketry.api.body.PlanetManager;

import java.util.Map;

public class StellarBody {


	private final StellarPosition position;


	private final double temperature;
	private final double size;
	private final boolean blackhole;
	private final String name;
	private final float[] color;

	//post filled
	private ImmutableSet<ResourceLocation> planets;
	private ImmutableSet<ResourceLocation> stars;

	public static Codec<StellarBody> getCODEC(ResourceLocation parent, boolean isSubStar) {
		return RecordCodecBuilder.create(
				stellarBodyInstance -> stellarBodyInstance.group(
						StellarPosition.getCODEC(parent,isSubStar).fieldOf("position").forGetter(StellarBody::getPosition),
						Codec.DOUBLE.fieldOf("temperature").forGetter(StellarBody::getTemperature),
						Codec.DOUBLE.fieldOf("size").forGetter(StellarBody::getSize),
						Codec.BOOL.fieldOf("blackhole").forGetter(StellarBody::getBlackHole),
						Codec.STRING.fieldOf("name").forGetter(StellarBody::getName)
				).apply(stellarBodyInstance,StellarBody::new)
		);
	}


	public StellarBody(StellarPosition position, double temperature, double size, boolean blackhole, String name) {
		this.position = position;
		this.temperature = temperature;
		this.size = size;
		this.blackhole = blackhole;
		this.name = name;
		this.color = calculateColor(temperature);
	}


	/**
	 * This section has getters for all the default stuff
	 */

	public StellarPosition getPosition() { return position; }
	public ImmutableSet<ResourceLocation> getPlanets() { return planets; }
	public ImmutableSet<ResourceLocation> getStars() { return stars; }
	public double getTemperature() { return temperature; }
	public double getSize() { return size; }
	public boolean getBlackHole() { return blackhole; }
	public String getName() { return name; }
	public float[] getColor() { return color; }

	/**
	 * @return the RGB color of this star represented as an int
	 */
	public int getColorRGB8() {
		return (int)(color[0]*0xFF) | ((int)(color[1]*0xFF) << 8) | ((int)(color[2]*0xFF) << 16);
	}

	public int getDisplayRadius() {
		return (int)(100*size);
	}



	/**
	 * @return the color of the star as an array of floats with length 3
	 */
	public float[] calculateColor(double temperature) {
		//Thank you to http://www.tannerhelland.com/4435/convert-temperature-rgb-algorithm-code/
		//Define
		float[] color = new float[3];
		float calculationTemperature = (((float)temperature * .477f) + 10f); //0 -> 10 100 -> 57.7

		//Find red
		if(calculationTemperature < 66)
			color[0] = 1f;
		else {
			color[0] = calculationTemperature - 60;
			color[0] = 329.69f * (float)Math.pow(color[0], -0.1332f);

			color[0] = MathHelper.clamp(color[0]/255f, 0f, 1f);
		}

		//Calc Green
		if(calculationTemperature < 66) {
			color[1] = calculationTemperature;
			color[1] = (float) (99.47f * Math.log(color[1]) - 161.1f);
		}
		else {
			color[1] = calculationTemperature - 60;
			color[1] = 288f * (float)Math.pow(color[1], -0.07551);

		}
		color[1] = MathHelper.clamp(color[1]/255f, 0f, 1f);


		//Calculate Blue
		if(calculationTemperature > 67)
			color[2] = 1f;
		else if(calculationTemperature <= 19){
			color[2] = 0f;
		}
		else {
			color[2] = calculationTemperature - 10;
			color[2] = (float) (138.51f * Math.log(color[2]) - 305.04f);
			color[2] = MathHelper.clamp(color[2]/255f, 0f, 1f);
		}

		return color;
	}

	@SuppressWarnings("UnstableApiUsage")
	public void findSatellites(ResourceLocation selfLocation, PlanetManager manager) {
		this.stars =  manager.getStars().entrySet().stream()
				.filter(entry -> entry.getValue().getPosition().parent == selfLocation)
				.map(Map.Entry::getKey)
				.collect(ImmutableSet.toImmutableSet());

		this.planets =  manager.getPlanets().entrySet().stream()
				.filter(entry -> entry.getValue().getLocation().parent == selfLocation)
				.map(Map.Entry::getKey)
				.collect(ImmutableSet.toImmutableSet());
	}
}