package zmaster587.advancedRocketry.api.body.planet;

import net.minecraft.util.ResourceLocation;
import zmaster587.advancedRocketry.api.*;
import zmaster587.advancedRocketry.api.body.GenericProperties;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.advancedRocketry.client.render.planet.ISkyRenderer;
import zmaster587.advancedRocketry.api.body.PlanetManager;
import zmaster587.advancedRocketry.util.AstronomicalBodyHelper;

import java.util.HashSet;

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
	//Post assignables
	public HashSet<ResourceLocation> satellites;



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
		this.temperature = AstronomicalBodyHelper.getAverageTemperature(PlanetManager.getInstance().getStar(location.star), getSolarRho(), pressure);
		this.insolationWithoutAtmosphere = AstronomicalBodyHelper.getStellarBrightness(PlanetManager.getInstance().getStar(location.star), getSolarRho()) * 1.308d;
		this.insolation = AstronomicalBodyHelper.getStellarBrightness(PlanetManager.getInstance().getStar(location.star), getSolarRho()) * Math.pow(Math.E, -(0.0026899d * pressure));
	}

	public boolean setSatellites(HashSet<ResourceLocation> satellites) {
		boolean set = this.satellites == null;
		if (set) this.satellites = satellites;
		return set;
	}



	/**
	 * This section has getters for all the default stuff
	 */

	//Property splits
	public PlanetLocation getLocation() { return location; }
	public HashSet<ResourceLocation> getSatellites() { return satellites; }
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

	public IAtmosphere getAtmosphere() {
		if(oxygenated) {
			if(temperature >= 900)
				return AtmosphereType.SUPERHEATED;
			if(temperature >= 350)
				return AtmosphereType.VERYHOT;
			if(getPressure() >= 800)
				return AtmosphereType.SUPERHIGHPRESSURE;
			if(getPressure() >= 200)
				return AtmosphereType.HIGHPRESSURE;
			if(getPressure() >= 25)
				return AtmosphereType.LOWOXYGEN;
			return AtmosphereType.AIR;
		} else {
			if(temperature >= 900)
				return AtmosphereType.SUPERHEATEDNOO2;
			if(temperature >= 350)
				return AtmosphereType.VERYHOTNOO2;
			if(getPressure() >= 800)
				return AtmosphereType.SUPERHIGHPRESSURENOO2;
			if(getPressure() >= 200)
				return AtmosphereType.HIGHPRESSURENOO2;
			return AtmosphereType.NOO2;
		}
	}

	public int getPathLengthToStar() {
		if(isSatellite())
			return 1 + PlanetManager.getInstance().getPlanetProperties(location.parent).getPathLengthToStar();
		return 1;
	}

	public boolean isSatellite() {
		return !location.star.equals(location.parent);
	}

	public PlanetProperties getParentProperties() {
		return PlanetManager.getInstance().getPlanetProperties(location.parent);
	}

	public double getCurrentOrbitalTheta() {
		if (this.isSatellite()) {
			return (AstronomicalBodyHelper.getMoonOrbitalTheta(location.orbitalRho, PlanetManager.getInstance().getPlanetProperties(location.parent).getGravitation()) + location.orbitalTheta) * (retrograde ? -1 : 1);
		} else if (!this.isSatellite()) {
			return (AstronomicalBodyHelper.getOrbitalTheta(location.orbitalRho, PlanetManager.getInstance().getStar(location.star).getSize()) + location.orbitalTheta) * (retrograde ? -1 : 1);
		}
		return location.orbitalTheta;
	}

	public double getSolarRho() {
		if(isSatellite())
			return PlanetManager.getInstance().getPlanetProperties(location.parent).getSolarRho();
		return location.orbitalRho;
	}

	@Override
	public String toString() {
		return String.format("Dimension ID: %d.  Dimension Name: %s.  Parent Star %d ", location.dimension, getName(), location.star);
	}
}
