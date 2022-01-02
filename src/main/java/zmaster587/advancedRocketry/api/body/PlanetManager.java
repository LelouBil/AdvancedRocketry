package zmaster587.advancedRocketry.api.body;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zmaster587.advancedRocketry.api.AdvancedRocketryManagers;
import zmaster587.advancedRocketry.api.body.planet.PlanetProperties;
import zmaster587.advancedRocketry.api.body.solar.StellarBody;
import zmaster587.advancedRocketry.api.body.station.IStation;
import zmaster587.advancedRocketry.api.satellite.SatelliteBase;
import zmaster587.advancedRocketry.network.PacketSatellite;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class PlanetManager {


	//Lists of all the bodies/locations in space and what they're tied to
	//Bodies
	private final HashMap<ResourceLocation, StellarBody> stars = new HashMap<>();
	private final HashMap<ResourceLocation, PlanetProperties> planets = new HashMap<>();

	private final Set<ResourceLocation> knownPlanets = new HashSet<>();
	//Non-bodies
	private final HashMap<ResourceLocation, HashMap<Long,SatelliteBase>> satellites = new HashMap<>();
	private final HashMap<ResourceLocation, HashSet<HashedBlockPosition>> beaconLocations = new HashMap<>();

	private static long nextSatelliteId;

	private static final Logger logger = LogManager.getLogger(PlanetManager.class.getName());


	private static final ResourceLocation invalid_star = new ResourceLocation("advancedrocketry", "invalid_star");


	/**
	 * <p>Loads the whole galaxy using the supplied resourceManager.</p>
	 * <p>This will create a {@link PlanetManager} instance and put it in {@link AdvancedRocketryManagers#planet} <b>only if there is none yet already</b></p>
	 * <p>The function will then scan every galaxy folder in all datapacks and load them based on a specific hierarchy.</p>
	 *
	 * @param resourceManager the {@link IResourceManager} used for {@link ResourceLocation} lookup
	 */
	public static void loadGalaxy(IResourceManager resourceManager) {
		if (AdvancedRocketryManagers.planet != null) {
			logger.warn("Tried to reload galaxy but it is already loaded");
			return;
		}
		logger.info("Loading galaxy");
		AdvancedRocketryManagers.planet = new PlanetManager();
		AdvancedRocketryManagers.planet.loadGalaxyInner(resourceManager);
		logger.info("Galaxy loaded");
	}

	/**
	 * The function that actually dos the scanning of galaxies
	 *
	 * @param resourceManager the {@link IResourceManager} used for {@link ResourceLocation} lookup
	 * @see PlanetManager#loadGalaxy(IResourceManager)
	 */
	private void loadGalaxyInner(IResourceManager resourceManager) {
		HashMap<ResourceLocation, IResource> resolvedResources = new HashMap<>();

		//resolve resource locations ahead of time
		for (ResourceLocation location : resourceManager.getAllResourceLocations("galaxy", s -> s.endsWith(".json"))) {
			IResource resolved = null;
			try {
				resolved = resourceManager.getResource(location);
			} catch (IOException e) {
				logger.error("Error while resolving resource {}", location);
				logger.error(e);
			}
			resolvedResources.put(location, resolved);
		}

		//load stars
		logger.info("Loading all stars");
		for (ResourceLocation maybeStar : resolvedResources.keySet()) {
			//only in galaxy folder
			if (ressourceIsSpecificallyInFolder(Paths.get("galaxy"), maybeStar)) {
				loadStar(maybeStar, resolvedResources, invalid_star); // invalid star as parent since they are top level
			}
		}

		//building hashsets of satellites for stars and planets
		stars.forEach((location, stellarBody) -> stellarBody.findSatellites(location, this));

		planets.forEach((location, planetProperties) -> planetProperties.findSatellites(location, this));
	}

	/**
	 * <p>Loads the {@link StellarBody} at the specified location, deserializing it using DataFixerUpper</p>
	 * <p>If a star is a top level, it's parent is {@link PlanetManager#invalid_star}, else it's the parent stars' {@link ResourceLocation}</p>
	 *
	 * @param starResourceLocation The location of the {@link StellarBody} to deserialize
	 * @param locationsInGalaxy    The map of all galaxy objects that were successfully resolved ahead of time
	 * @param parentStarLocation   The {@link ResourceLocation} of the parent of this star, or {@link PlanetManager#invalid_star} if none
	 * @see StellarBody
	 * @see StellarBody#getCODEC(ResourceLocation, boolean)
	 */
	private void loadStar(ResourceLocation starResourceLocation, Map<ResourceLocation, IResource> locationsInGalaxy, ResourceLocation parentStarLocation) {
		logger.debug("Loading star {}", starResourceLocation);

		//tries to get the resource from the map
		IResource starResource = locationsInGalaxy.getOrDefault(starResourceLocation, null);
		if (starResource == null) {
			logger.error("Failed to load non resolved star : {}", starResourceLocation);
			return;
		}

		//the star is a SUB-star if it DOES NOT have a parent, aka it's parent is invalid_star
		boolean isSubStar = parentStarLocation != PlanetManager.invalid_star;

		//JSON parsing using Gson
		try {
			JsonElement parsedJson = new JsonParser().parse(new InputStreamReader(starResource.getInputStream()));

			//parsing using DataFixerUpper, the DataResult is either the object, or a PartialResult with an error message
			DataResult<StellarBody> stellarBodyDataResult = StellarBody.getCODEC(parentStarLocation, isSubStar)
					.parse(JsonOps.INSTANCE, parsedJson);

			//error handling
			if (stellarBodyDataResult.error().isPresent()) {
				logger.error("Error during Deserialization with codec : {}", stellarBodyDataResult.error().get().message());
				logger.error("Error loading star {}", starResourceLocation);
				return;
			}

			//noinspection OptionalGetWithoutIsPresent
			StellarBody stellarBody = stellarBodyDataResult.result().get();

			logger.debug("Deserialized star successfully : {}", stellarBody);

			//adding into stars map
			this.stars.put(starResourceLocation, stellarBody);

			//substars can't have planets or substars
			if (!isSubStar) {
				//planets & substars loading
				String starFolder = FilenameUtils.removeExtension(starResourceLocation.getPath());

				for (ResourceLocation maybePlanet : locationsInGalaxy.keySet()) {
					//if maybePlanet is a substar
					if (PlanetManager.ressourceIsSpecificallyInFolder(Paths.get(starFolder, "substars"), maybePlanet)) {
						loadStar(maybePlanet, locationsInGalaxy, starResourceLocation); //current star as parent
					} else if (ressourceIsSpecificallyInFolder(Paths.get(starFolder), maybePlanet)) {
						//if it's a planet
						loadPlanet(maybePlanet, starResourceLocation, starResourceLocation, locationsInGalaxy);
					}
				}
			}
		} catch (JsonParseException exception){
			logger.error("Star {}is not valid json",starResourceLocation);
			logger.error(exception);
		}

	}

	/**
	 * <p>Loads the {@link PlanetProperties} at the specified location, deserializing it using DataFixerUpper</p>
	 * <p>A planet always has a star, but it can also have a planet parent if it's a moon</p>
	 *
	 * @param planetResourceLocation The location of the {@link PlanetProperties} to deserialize
	 * @param locationsInGalaxy      The map of all galaxy objects that were successfully resolved ahead of time
	 * @param starResourceLocation   The {@link ResourceLocation} of the {@link StellarBody} associated with this planet
	 * @param parentPlanet           The parent {@link PlanetProperties} of this planet, <b>only if the current planet is a moon</b>
	 * @see PlanetProperties
	 * @see PlanetProperties#getCODEC(ResourceLocation, ResourceLocation)
	 */
	private void loadPlanet(ResourceLocation planetResourceLocation, ResourceLocation starResourceLocation, ResourceLocation parentPlanet, Map<ResourceLocation, IResource> locationsInGalaxy) {
		logger.debug("Loading planet {}", planetResourceLocation);

		//tries to get the resource from the map
		IResource planetResource = locationsInGalaxy.get(planetResourceLocation);
		if (planetResource == null) {
			logger.error("Failed to load non resolved planet : {}", planetResourceLocation);
			return;
		}

		try{
			//JSON parsing with Gson
			JsonElement parsedJson = new JsonParser().parse(new InputStreamReader(planetResource.getInputStream()));


			//parsing using DataFixerUpper, the DataResult is either the object, or a PartialResult with an error message
			DataResult<PlanetProperties> planetPropertiesDataResult = PlanetProperties.getCODEC(starResourceLocation, parentPlanet)
					.parse(JsonOps.INSTANCE, parsedJson);

			//error handling
			if (planetPropertiesDataResult.error().isPresent()) {
				logger.error("Error during Deserialization with codec : {}", planetPropertiesDataResult.error().get().message());
				logger.error("Error loading planet {}", planetResourceLocation);
				return;
			}

			//noinspection OptionalGetWithoutIsPresent
			PlanetProperties planetProperties = planetPropertiesDataResult.result().get();

			logger.debug("Deserialized planet successfully : {}", planetProperties);

			//adding to the planets map
			this.planets.put(planetResourceLocation, planetProperties);


			//the planet is a moon if it's parent IS DIFFERENT FROM its star
			boolean isMoon = parentPlanet != starResourceLocation;

			//moons can't have moons
			if (!isMoon) {
				String planetMoonFolder = FilenameUtils.removeExtension(planetResourceLocation.getPath());

				for (ResourceLocation maybeMoon : locationsInGalaxy.keySet()) {
					//if maybeMoon is in moon folder
					if (ressourceIsSpecificallyInFolder(Paths.get(planetMoonFolder), maybeMoon)) {
						//load it but with planetResourceLocation as parent, aka the current planet
						loadPlanet(maybeMoon, starResourceLocation, planetResourceLocation, locationsInGalaxy);
					}
				}
			}
		} catch (JsonParseException exception){
			logger.error("Planet {} is not valid json",planetResourceLocation);
			logger.error(exception);
		}

	}

	/**
	 * <p>Checks if the specified {@link ResourceLocation}'s parent is <b>specifically</b> the folder specified by {@code path}</p>
	 * <p>This will return {@code false} for <b>subfolders</b></p>
	 * <pre>
	 * {@code
	 * //true
	 * ressourceIsSpecificallyInFolder(Paths.get("galaxy"),new ResourceLocation("somemod","galaxy/mystar.json"));
	 *
	 * //true
	 * ressourceIsSpecificallyInFolder(Paths.get("galaxy"),new ResourceLocation("somemod","galaxy/mystar"));
	 *
	 * //false
	 * ressourceIsSpecificallyInFolder(Paths.get("galaxy"),new ResourceLocation("somemod","galaxy/mystar/someplanet.json"));
	 *
	 * //true
	 * ressourceIsSpecificallyInFolder(Paths.get("galaxy","mystar"),new ResourceLocation("somemod","galaxy/mystar/someplanet.json"));}
	 * </pre>
	 * @param path the {@link Path} inside of which the {@code location} needs to be
	 * @param location the {@link ResourceLocation} that is being checked
	 * @return true if the locations' parent is exactly {@code path}
	 */
	private static boolean ressourceIsSpecificallyInFolder(Path path, ResourceLocation location) {
		return Optional.ofNullable(
						Paths.get(location.getPath()).getParent())
				.orElse(Paths.get(""))
				.equals(path);
	}



	public ImmutableMap<ResourceLocation, StellarBody> getStars() {
		return ImmutableMap.copyOf(stars);
	}

	public Set<ResourceLocation> getStarIDs() {
		return stars.keySet();
	}

	public StellarBody getStar(ResourceLocation star) {
		return stars.get(star);
	}

	public ImmutableMap<ResourceLocation, PlanetProperties> getPlanets() {
		return ImmutableMap.copyOf(planets);
	}

	public Set<ResourceLocation> getPlanetIDs() {
		return planets.keySet();
	}

	public PlanetProperties getPlanetProperties(ResourceLocation planet) {
		return planets.get(planet);
	}
	public PlanetProperties getPlanetPropertiesExact(ResourceLocation planet, BlockPos pos) {
		if(isBodyStation(planet)) {
			IStation spaceObject = AdvancedRocketryManagers.station.getSpaceStationFromPosition(pos);
			if(spaceObject != null)
				return getPlanetProperties(spaceObject.getOrbit());
			else
				return null;
		} else
			return getPlanetProperties(planet);
	}

	public GenericProperties getGenericProperties(ResourceLocation planet, BlockPos pos) {
		if(isBodyStation(planet)) {
			IStation spaceObject = AdvancedRocketryManagers.station.getSpaceStationFromPosition(pos);
			if(spaceObject != null)
				return spaceObject.getPropertiesCopy();
			else
				return null;
		} else
			return getPlanetProperties(planet);
	}

	public HashMap<Long, SatelliteBase> getSatellites(ResourceLocation planet) {
		return satellites.get(planet);
	}

	public SatelliteBase getSatellite(ResourceLocation planet, long id) {
		return getSatellites(planet).get(id);
	}

	public void addSatelliteServer(@Nonnull SatelliteBase satellite, ResourceLocation world) {
		satellites.get(world).put(satellite.getId(), satellite);
		satellite.setDimensionId(world);
		PacketHandler.sendToAll(new PacketSatellite(satellite));
	}

	public void addSatelliteClient(@Nonnull SatelliteBase satellite) {
		satellites.get(satellite.getDimensionId()).put(satellite.getId(), satellite);
	}

	public long getNextSatelliteId() {
		return nextSatelliteId++;
	}

	public void tickSatellites() {
		for(ResourceLocation planet : getPlanetIDs()) {
			for(long id : getSatellites(planet).keySet()) {
				getSatellite(planet, id).tickEntity();
			}
		}
	}

	public HashSet<HashedBlockPosition> getBeaconLocations(ResourceLocation planet) {
		return beaconLocations.get(planet);
	}

	public static boolean isBodyStation(ResourceLocation planet) {
		return planet != null && planet.equals(spaceDimensionID);
	}

	public static boolean hasSurface(ResourceLocation planet) {
		return !AdvancedRocketryManagers.planet.getPlanetProperties(planet).getGaseous();
	}
}
