package zmaster587.advancedRocketry.api;

import net.minecraft.enchantment.Enchantment;
import zmaster587.advancedRocketry.api.atmosphere.IAtmosphereSealHandler;
import zmaster587.advancedRocketry.api.body.PlanetManager;
import zmaster587.advancedRocketry.api.body.SpaceObjectManager;

/**
 * Reference class for any API data
 * Created by Dark(DarkGuardsman, Robert) on 1/6/2016.
 */
public class AdvancedRocketryAPI
{
    /** Used to check if a block is seal-able, including register & handle block sealing support */
    public static IAtmosphereSealHandler atomsphereSealHandler;
    public static SpaceObjectManager spaceObjectManager;
	public static PlanetManager dimensionManager;
	public static IGravityManager gravityManager;
	public static Enchantment enchantmentSpaceProtection;
}
