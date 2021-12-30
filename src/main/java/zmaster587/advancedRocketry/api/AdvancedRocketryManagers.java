package zmaster587.advancedRocketry.api;

import net.minecraft.enchantment.Enchantment;
import zmaster587.advancedRocketry.api.atmosphere.IAtmosphereSealHandler;
import zmaster587.advancedRocketry.api.body.PlanetManager;
import zmaster587.advancedRocketry.api.body.StationManager;

/**
 * Reference class for any API data
 * Created by Dark(DarkGuardsman, Robert) on 1/6/2016.
 */
public class AdvancedRocketryManagers {
    public static IAtmosphereSealHandler atmosphere;
    public static StationManager station;
	public static PlanetManager planet;
	public static IGravityManager gravity;
	public static Enchantment enchantment;
}
