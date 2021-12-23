package zmaster587.advancedRocketry.inventory;

import zmaster587.advancedRocketry.api.body.planet.PlanetProperties;
import zmaster587.advancedRocketry.api.body.solar.StellarBody;

public interface IPlanetDefiner {
	
	boolean isPlanetKnown(PlanetProperties properties);
	
	boolean isStarKnown(StellarBody body);
}
