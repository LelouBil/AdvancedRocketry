package zmaster587.advancedRocketry.api.body;

import zmaster587.advancedRocketry.client.render.planet.ISkyRenderer;

public class GenericProperties implements Cloneable {

	//Planetary properties (defined)
	private final String name;
	private final int pressure;
	private final double gravitation;
	//Post assignable
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
