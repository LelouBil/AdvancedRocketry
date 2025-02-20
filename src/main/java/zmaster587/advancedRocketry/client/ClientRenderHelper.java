package zmaster587.advancedRocketry.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class ClientRenderHelper {

	static int oldRenderDistance;
	static boolean renderDistanceOverriden = false;
	static final int INTERPLANETARY_RENDER_DISTANCE = 16;


	public static void setOverridenRenderDistance(int distance)
	{
		if(!renderDistanceOverriden)
			oldRenderDistance = Minecraft.getInstance().gameSettings.renderDistanceChunks;
		Minecraft.getInstance().gameSettings.renderDistanceChunks = distance;
		try {
			ObfuscationReflectionHelper.setPrivateValue(EntityRendererManager.class, Minecraft.getInstance().getRenderManager(), distance, "renderDistanceChunks");
		}
		catch(Exception e)
		{
			
		}
		
		
		renderDistanceOverriden = true;

	}

	public static void RestoreRenderDistance()
	{
		if(renderDistanceOverriden)
		{
			renderDistanceOverriden = false;
			Minecraft.getInstance().gameSettings.renderDistanceChunks = oldRenderDistance;
		}
	}

}
