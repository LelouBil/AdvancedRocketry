package zmaster587.advancedRocketry.api.body;

import net.minecraft.util.ResourceLocation;
import zmaster587.advancedRocketry.api.IAtmosphere;
import zmaster587.advancedRocketry.api.body.planet.PlanetLocation;
import zmaster587.advancedRocketry.api.body.planet.PlanetRenderingProperties;
import zmaster587.advancedRocketry.api.body.planet.PlanetResourceProperties;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.advancedRocketry.client.render.planet.ISkyRenderer;
import zmaster587.advancedRocketry.util.AstronomicalBodyHelper;

import java.util.HashSet;

public class GenericProperties implements Cloneable {

	//Planetary properties (defined)
	private final String name;
	private final int pressure;
	private final double gravitation;
	//Post assignables
	public ISkyRenderer sky;

	public GenericProperties(String name, int pressure, double gravitation) {
		this.name = name;
		this.pressure = pressure;
		this.gravitation = gravitation;
	}

	/**
	 * This section has getters for all the default stuff
	 */

    public String getName() { return name; }
    public int getPressure() { return pressure; }
    public double getGravitation() { return gravitation; }
}
