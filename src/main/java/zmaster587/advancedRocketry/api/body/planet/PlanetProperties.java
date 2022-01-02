package zmaster587.advancedRocketry.api.body.planet;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ResourceLocation;
import zmaster587.advancedRocketry.api.AdvancedRocketryManagers;
import zmaster587.advancedRocketry.api.body.GenericProperties;
import zmaster587.advancedRocketry.api.body.PlanetManager;
import zmaster587.advancedRocketry.util.AstronomicalBodyHelper;

import java.util.Map;

public class PlanetProperties extends GenericProperties implements Cloneable {

	/**
	 * Contains default graphic {@link ResourceLocation} to display for different planet types
	 *
	 */
	public static final ResourceLocation atmosphere = new ResourceLocation("advancedrocketry:textures/planets/atmosphere2.png");
	public static final ResourceLocation atmosphereLEO = new ResourceLocation("advancedrocketry:textures/planets/atmosphereleo.png");
	public static final ResourceLocation atmGlow = new ResourceLocation("advancedrocketry:textures/planets/atmglow.png");
	public static final ResourceLocation planetRings = new ResourceLocation("advancedrocketry:textures/planets/rings.png");
	public static final ResourceLocation planetRingShadow = new ResourceLocation("advancedrocketry:textures/planets/ringshadow.png");

	public static final ResourceLocation shadow = new ResourceLocation("advancedrocketry:textures/planets/shadow.png");
	public static final ResourceLocation shadow3 = new ResourceLocation("advancedrocketry:textures/planets/shadow3.png");


	//Property splits
	private final PlanetLocation location;
	private final PlanetRenderingProperties render;
	private final PlanetResourceProperties resources;
	//Non-orbit
	private final boolean gaseous;
	private final boolean ringed;
	private final boolean oxygenated;
	//Rotation
	private final int rotationalPeriod;
	private final double rotationalPhi;
	private final boolean retrograde;
	//Planetary properties (calculated)
	private final int temperature;
	private final double insolation;
	private final double insolationWithoutAtmosphere;

	//Post filled
	public ImmutableSet<ResourceLocation> satellites;

	public static Codec<PlanetProperties> getCODEC(ResourceLocation star, ResourceLocation parent) {
		return RecordCodecBuilder
				.create(planetPropertiesInstance ->
						planetPropertiesInstance.group(
								PlanetLocation.getCodec(star,parent).fieldOf("location").forGetter(PlanetProperties::getLocation),
								//Property splits
								PlanetRenderingProperties.CODEC.fieldOf("render").forGetter(PlanetProperties::getRender),
								PlanetResourceProperties.CODEC.fieldOf("resources").forGetter(PlanetProperties::getResources),
								//Planetary properties (defined)
								Codec.STRING.fieldOf("name").forGetter(PlanetProperties::getName),
								Codec.INT.fieldOf("pressure").forGetter(PlanetProperties::getPressure),
								Codec.DOUBLE.fieldOf("gravitation").forGetter(PlanetProperties::getGravitation),
								Codec.BOOL.fieldOf("gaseous").forGetter(PlanetProperties::getGaseous),
								Codec.BOOL.fieldOf("ringed").forGetter(PlanetProperties::getRinged),
								Codec.BOOL.fieldOf("oxygenated").forGetter(PlanetProperties::getOxygenated),
								//Rotation
								Codec.INT.fieldOf("rotationalPeriod").forGetter(PlanetProperties::getRotationalPeriod),
								Codec.DOUBLE.fieldOf("rotationalPhi").forGetter(PlanetProperties::getRotationalPhi),
								Codec.BOOL.fieldOf("retrograde").forGetter(PlanetProperties::getRetrograde)
						).apply(planetPropertiesInstance,PlanetProperties::new));
	}

	public PlanetProperties(PlanetLocation location, PlanetRenderingProperties render, PlanetResourceProperties resources, String name, int pressure, double gravitation, boolean gaseous, boolean ringed, boolean oxygenated, int rotationalPeriod, double rotationalPhi, boolean retrograde) {
		super(name, pressure, gravitation);
		//Property splits
		this.location = location;
		this.render = render;
		this.resources = resources;
		//Planetary properties (defined)
		this.gaseous = gaseous;
		this.ringed = ringed;
		this.oxygenated = oxygenated;
		//Rotation
		this.rotationalPeriod = rotationalPeriod;
		this.rotationalPhi = rotationalPhi;
		this.retrograde = retrograde;
		//Calculated
		this.temperature = AstronomicalBodyHelper.getAverageTemperature(AdvancedRocketryManagers.planet.getStar(location.star), getSolarRho(), pressure);
		this.insolationWithoutAtmosphere = AstronomicalBodyHelper.getStellarBrightness(AdvancedRocketryManagers.planet.getStar(location.star), getSolarRho()) * 1.308d;
		this.insolation = AstronomicalBodyHelper.getStellarBrightness(AdvancedRocketryManagers.planet.getStar(location.star), getSolarRho()) * Math.pow(Math.E, -(0.0026899d * pressure));
	}



	public void findSatellites(ResourceLocation selfLocation, PlanetManager planetManager){
		//noinspection UnstableApiUsage
		satellites = planetManager.getPlanets().entrySet().stream()
				.filter(entry -> entry.getValue().getLocation().parent == selfLocation)
				.map(Map.Entry::getKey)
				.collect(ImmutableSet.toImmutableSet());
	}



	/**
	 * This section has getters for all the default stuff
	 */

	//Property splits
	public PlanetLocation getLocation() { return location; }
	public ImmutableSet<ResourceLocation> getSatellites() { return satellites; }
	public PlanetRenderingProperties getRender() { return render; }
	public PlanetResourceProperties getResources() { return resources; }
	public boolean getGaseous() { return gaseous; }
	public boolean getRinged() { return ringed; }
	public boolean getOxygenated() { return oxygenated; }
	//Rotation
	public int getRotationalPeriod() { return rotationalPeriod; }
	public double getRotationalPhi() { return rotationalPhi; }
	public boolean getRetrograde() { return retrograde; }
	//Calculated
	public double getTemperature() { return temperature; }
	public double getInsolationWithoutAtmosphere() { return insolationWithoutAtmosphere; }
	public double getInsolation() { return insolation; }

	/**
	 * This section has getters for all the non-value default stuff
	 */



	public int getPathLengthToStar() {
		if(isSatellite())
			return 1 + AdvancedRocketryManagers.planet.getPlanetProperties(location.parent).getPathLengthToStar();
		return 1;
	}

	public boolean isSatellite() {
		return !location.star.equals(location.parent);
	}

	public PlanetProperties getParentProperties() {
		return AdvancedRocketryManagers.planet.getPlanetProperties(location.parent);
	}

	public double getCurrentOrbitalTheta() {
		if (this.isSatellite()) {
			return (AstronomicalBodyHelper.getMoonOrbitalTheta(location.orbitalRho, AdvancedRocketryManagers.planet.getPlanetProperties(location.parent).getGravitation()) + location.orbitalTheta) * (retrograde ? -1 : 1);
		} else if (!this.isSatellite()) {
			return (AstronomicalBodyHelper.getOrbitalTheta(location.orbitalRho, AdvancedRocketryManagers.planet.getStar(location.star).getSize()) + location.orbitalTheta) * (retrograde ? -1 : 1);
		}
		return location.orbitalTheta;
	}

	public double getSolarRho() {
		if(isSatellite())
			return AdvancedRocketryManagers.planet.getPlanetProperties(location.parent).getSolarRho();
		return location.orbitalRho;
	}

	@Override
	public String toString() {
		return String.format("Dimension ID: %s.  Dimension Name: %s.  Parent Star %s ", location.dimension, getName(), location.star);
	}


}
