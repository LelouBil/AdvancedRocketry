package zmaster587.advancedRocketry.inventory.modules;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.math.vector.Quaternion;
import zmaster587.advancedRocketry.api.Constants;
import zmaster587.advancedRocketry.client.render.planet.RenderPlanetarySky;
import zmaster587.libVulpes.inventory.modules.ModuleBase;

public class ModulePlanetImage extends ModuleBase {

	PlanetProperties properties;
	float width;

	public ModulePlanetImage(int locX, int locY, float size, PlanetProperties icon) {
		super(locX, locY);
		width = size;
	}


	@Override
	public void renderBackground(ContainerScreen<? extends Container> gui, MatrixStack matrix, int x, int y, int mouseX,
			int mouseY, FontRenderer font) {
		super.renderBackground(gui, matrix, x, y, mouseX, mouseY, font);

		if(Constants.INVALID_STAR.equals(properties.getLocation().star))
			return;
		
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder vertexbuffer = tessellator.getBuffer();
		matrix.push();
		matrix.rotate(new Quaternion(-90, 0, 0, true));
		//matrix.translate(offsetX, 100, offsetY);
		//GL11.glTranslatef(xPosition, 100 + this.zLevel, yPosition);
		float newWidth = width/2f;

		RenderPlanetarySky.renderPlanetPubHelper(vertexbuffer, matrix, properties.getRender().texture, (int)(x + this.offsetX + newWidth), (int)(y + this.offsetY + newWidth), -0.1, newWidth, 1f, properties.getLocation().orbitalTheta, properties.getPressure() > 0, properties.getRender().skyColor, properties.getRender().ringColor, properties.getGaseous(), properties.getRinged(), true, new float[]{0, 0, 0}, 1f);
		matrix.pop();
	}
	
	public void setDimProperties(PlanetProperties location) {
		properties = location;
	}
}
