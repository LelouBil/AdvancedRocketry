package zmaster587.advancedRocketry.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IDayTimeReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import zmaster587.advancedRocketry.AdvancedRocketry;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.client.render.planet.ISkyRenderer;
import zmaster587.advancedRocketry.client.render.planet.RenderPlanetarySky;
import zmaster587.advancedRocketry.client.render.planet.RenderSpaceSky;
import zmaster587.advancedRocketry.api.body.PlanetManager;
import zmaster587.advancedRocketry.api.body.planet.PlanetProperties;
import zmaster587.libVulpes.util.ZUtils;

public class ClientHelper {

	@OnlyIn(Dist.CLIENT)
	public static boolean callCustomSkyRenderer(MatrixStack matrix, float partialTicks) {
		World world = Minecraft.getInstance().world;
		if(!PlanetManager.getInstance().isDimensionCreated(world))
			return true;

		PlanetProperties properties = PlanetManager.getInstance().getPlanetPropertiesExact(ZUtils.getDimensionIdentifier(world),  new BlockPos(Minecraft.getInstance().player.getPositionVec()));
		ISkyRenderer renderer =  properties.sky;
		
		if(renderer == null) {
			if(PlanetManager.isBodyStation(ZUtils.getDimensionIdentifier(world))) {
				if(!ARConfiguration.getCurrentConfig().stationSkyOverride.get()) {
					properties.sky = null;
					return true;
				}
				properties.sky = new RenderSpaceSky();
			} else {
				if(!ARConfiguration.getCurrentConfig().planetSkyOverride.get()) {
					properties.sky = null;
					return true;
				}
				properties.sky = new RenderPlanetarySky();
			}
			renderer = properties.sky;
		}
		renderer.render(matrix, partialTicks);
		return false;
	}

	public static float callTimeOfDay(float ogTime, IDayTimeReader reader) {
		if(!(reader instanceof World)) return ogTime;

		PlanetProperties properties = PlanetManager.getInstance().getPlanetProperties(ZUtils.getDimensionIdentifier((World)reader));

		if(PlanetManager.isBodyStation(ZUtils.getDimensionIdentifier((World)reader)))
			return AdvancedRocketry.proxy.calculateCelestialAngleSpaceStation();
		
		double d0 = MathHelper.frac((double)reader.func_241851_ab() / ((double)properties.getRotationalPeriod()) - 0.25D);
		double d1 = 0.5D - Math.cos(d0 * Math.PI) / 2.0D;
		return (float)(d0 * 2.0D + d1) / 3.0F;
	}
}
