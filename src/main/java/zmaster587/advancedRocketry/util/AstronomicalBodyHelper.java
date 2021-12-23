package zmaster587.advancedRocketry.util;

import net.minecraft.util.ResourceLocation;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.body.solar.StellarBody;
import zmaster587.advancedRocketry.api.body.PlanetManager;

public class AstronomicalBodyHelper {
	/**
	 * Returns the size multiplier for a body at the input distance, relative to either 1 AU or the moon's orbital distance, depending on parent body
	 * @param orbitalRho the distance from the parent body, 1.0 is the default (1 AU or one Earth-Luna distance)
	 * @return the double multiplier for size
	 */
	public static double getBodySizeMultiplier(double orbitalRho) {
		//Returns size multiplier relative to Earth standard (1AU = 100 Distance)
		return 1/orbitalRho;
	}

	/**
	 * Returns the orbital period for a body at a given distance around its star
	 * @param orbitalRho the distance from the parent body, in AU
	 * @param solarSize the size of the sun in question
	 * @return the orbital period in MC Days (24000 ticks)
	 */
	public static double getOrbitalPeriod(double orbitalRho, double solarSize) {
		//One MC Year is 48 MC days (16 IRL Hours), one month is 8 MC Days
		return 48d * Math.sqrt((orbitalRho * orbitalRho * orbitalRho)/(solarSize * solarSize * solarSize));
	}

	/**
	 * Returns the orbital period for a body at a given distance around its parent planet
	 * @param orbitalRho the distance from the parent body, 1.0 is the earth's moon's distance
	 * @param planetaryMass the mass of the planet in question
	 * @return the orbital period in MC Days (24000 ticks)
	 */
	public static double getMoonOrbitalPeriod(double orbitalRho, double planetaryMass) {
		//One (lunar) MC month is 8 MC days, so the moon.json orbits in 8
		//The same as the function for planets, but since gravity is directly correlated with mass uses the gravity of the plant for mass
		return 8d * Math.sqrt((orbitalRho * orbitalRho * orbitalRho)/planetaryMass);
	}

	/**
	 * Returns the orbital theta for a body at a given distance around its star, at this current moment
	 * @param orbitalRho the distance from the parent body, in AU
	 * @param solarSize the size of the sun in question
	 * @return the current angle around the star in radians
	 */
	public static double getOrbitalTheta(double orbitalRho, double solarSize) {
		double orbitalPeriod = getOrbitalPeriod(orbitalRho, solarSize);
		//Returns angle, relative to 0, of a planet at any given time
		return ((AdvancedRocketry.proxy.getWorldTimeUniversal() % (24000d*orbitalPeriod))/(24000d*orbitalPeriod))*(2d*Math.PI);
	}

	/**
	 * Returns the orbital theta for a body at a given distance around its parent planet, at this current moment
	 * @param orbitalRho the distance from the parent body, 1.0 is the earth's moon's distance
	 * @param parentGravitationalMultiplier the size of the parent planet in question
	 * @return the current angle around the planet in radians
	 */
	public static double getMoonOrbitalTheta(double orbitalRho, double parentGravitationalMultiplier) {
		//Because the function is still in AU and solar mass, some correctional factors to convert to those units
		double orbitalPeriod = getMoonOrbitalPeriod(orbitalRho, parentGravitationalMultiplier);
		//Returns angle, relative to 0, of a moon.json at any given time
		return ((AdvancedRocketry.proxy.getWorldTimeUniversal() % (24000d*orbitalPeriod))/(24000d*orbitalPeriod))*(2d*Math.PI);
	}

	/**
	 * Returns the visual orbital theta for a body at a given distance around its parent planet, at this current moment, as a value from 0 - 360
	 * @param rotationalPeriod the rotational period of the moon.json we are rendering from
	 * @param orbitalRho the distance from the parent body, 1.0 is the earth's moon's distance
	 * @param parentGravitationalMultiplier the distance from the parent body
	 * @param currentOrbitalTheta the orbital theta of the moon.json we are rendering from
	 * @param baseOrbitalTheta the base orbital theta of the planet in question
	 * @return the current angle around the planet normalized 0 - 360, for GL calls
	 */
	public static double getParentPlanetThetaFromMoon(int rotationalPeriod, double orbitalRho, double parentGravitationalMultiplier, double currentOrbitalTheta, double baseOrbitalTheta) {
		//Convert from radians to degrees for easier math
		double degreeOrbitalTheta = (currentOrbitalTheta * 180/Math.PI);
		//Computer the number of rotations per revolution and use that for how fast the planet would seem to orbit from the moon.json
		//Planet will not move at all if it is tidally locked
		double planetPositionTheta = (((AstronomicalBodyHelper.getMoonOrbitalPeriod(orbitalRho, parentGravitationalMultiplier) * 24000)/rotationalPeriod) - 1) * degreeOrbitalTheta;
		//Add the base orbital theta so the planet is in the correct place
		return (planetPositionTheta + (baseOrbitalTheta * 180/Math.PI)) % 360;
	}

	/**
	 * Returns the average temperature of a planet with the passed parameters
	 * @param star the stellar body that the planet orbits
	 * @param orbitalRho the distance from the star, in AU
	 * @param atmPressure the pressure of the planet's atmosphere
	 * @return the temperature of the planet in Kelvin
	 */
	public static int getAverageTemperature(StellarBody star, double orbitalRho, int atmPressure) {
		double starSurfaceTemperature = 58 * star.getTemperature();
		double starRadius = star.getSize()/215f;
		//Albedo is 0.3f hardcoded because of inability to easily calculate
		double averageWithoutAtmosphere = starSurfaceTemperature * Math.sqrt(starRadius/(2* orbitalRho)) * Math.pow((1f-0.3f), 0.25);
		//Slightly kludgey solution that works out mostly for Venus and well for Earth, without being overly complex
		//Output is in Kelvin
		return (int)(averageWithoutAtmosphere * Math.max(1, (1.125d * Math.pow((atmPressure/100d), 0.25))));
	}

	/**
	 * Returns the average insolation of a planet with the passed parameters
	 * @param star the stellar body that the planet orbits
	 * @param orbitalRho the distance from the star, in AU
	 * @return the insolation of the planet relative to Earth insolation
	 */
	public static double getStellarBrightness(StellarBody star, double orbitalRho) {
		//Normal stars are 1.0 times this value, black holes with accretion discs emit less and so modify it
		double lightMultiplier = 1.0f;
		//Make all values ratios of Earth normal to get ratio compared to Earth
		double normalizedStarTemperature = star.getTemperature()/100f;
		//Check to see if the star is a black hole
		boolean blackHole = star.getBlackHole();
		for(ResourceLocation subStar : star.getStars()) {
			if (!PlanetManager.getInstance().getStar(subStar).getBlackHole()) {
				blackHole = false;
				break;
			}
		}
		//There's no real easy way to get the light emitted by an accretion disc, so this substitutes
		if(blackHole) lightMultiplier *= 0.125 * star.getSize() * star.getSize();
		//Returns ratio compared to a planet at 1 AU for Sol, because the other values in AR are normalized,
		//and this works fairly well for hooking into with other mod's solar panels & such
		return (lightMultiplier * (((star.getSize() * star.getSize()) * (normalizedStarTemperature * normalizedStarTemperature * normalizedStarTemperature * normalizedStarTemperature))/(orbitalRho * orbitalRho)));
	}

	/**
	 * Returns the human-eye-perceivable brightness of this insolation multiplier
	 * @param stellarBrightnessMultiplier the insolation multiplier to use
	 * @return the brightness multiplier perceivable to a human
	 */
	public static double getPlanetaryLightLevelMultiplier(double stellarBrightnessMultiplier) {
		double log2Multiplier = (Math.log10(stellarBrightnessMultiplier)/Math.log10(2.0));
		//Returns the brightness visible to the eye, compared to the actual flux - this is a factor of ~1.5x for every 2x increase in luminosity
        //This is used for planetary light levels, as those would be eyesight based unlike the stellar brightness or similar
		return Math.pow(1.5, log2Multiplier);
	}
}
