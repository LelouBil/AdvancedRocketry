package zmaster587.advancedRocketry.client.render.multiblocks;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Quaternion;
import zmaster587.advancedRocketry.backwardCompat.ModelFormatException;
import zmaster587.advancedRocketry.backwardCompat.WavefrontObject;
import zmaster587.advancedRocketry.tile.multiblock.machine.TileLiquefactionPlant;
import zmaster587.libVulpes.block.RotatableBlock;
import zmaster587.libVulpes.render.RenderHelper;

import javax.annotation.ParametersAreNonnullByDefault;


public class RendererLiquefactionPlant extends TileEntityRenderer<TileLiquefactionPlant> {

	WavefrontObject model;
	ResourceLocation texture =  new ResourceLocation("advancedrocketry:textures/models/liquefactionplant.png");

	public RendererLiquefactionPlant(TileEntityRendererDispatcher tile) {
		super(tile);
		try {
			model = new WavefrontObject(new ResourceLocation("advancedrocketry:models/liquefactionplant.obj"));
		} catch (ModelFormatException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	@ParametersAreNonnullByDefault
	public void render(TileLiquefactionPlant tile, float partialTicks, MatrixStack matrix, IRenderTypeBuffer buffer, int combinedLightIn, int combinedOverlayIn) {
		if(!tile.canRender())
			return;

		if (tile.getWorld() != null) {
			combinedLightIn = WorldRenderer.getCombinedLight(tile.getWorld(), tile.getPos().add(0, 1, 0));
		} else {
			combinedLightIn = 15728880;
		}
		
		matrix.push();
		

		//Initial setup
		matrix.translate(0.5f, -0.5f, 0.5f);
		//Rotate and move the model into position
		Direction front = RotatableBlock.getFront(tile.getWorld().getBlockState(tile.getPos()));
		matrix.rotate(new Quaternion(0, (front.getXOffset() == 1 ? 180 : 0) + front.getZOffset()*90f, 0, true));
		matrix.translate(-1.5f, 0f, -0.5f);
		IVertexBuilder entitySolidBuilder = buffer.getBuffer(RenderHelper.getSolidEntityModelRenderType(texture));

		if(tile.isRunning()) {
			model.renderOnly(matrix, combinedLightIn, combinedOverlayIn, entitySolidBuilder, "base");

			matrix.push();
			matrix.translate(0f, 1.875f, 2f);
			matrix.push();
			matrix.rotate(new Quaternion(tile.getWorld().getGameTime() * -75f, 0, 0, true));
			model.renderOnly(matrix, combinedLightIn, combinedOverlayIn, entitySolidBuilder, "rod");
			matrix.pop();
			matrix.pop();
		} else {
			model.renderOnly(matrix, combinedLightIn, combinedOverlayIn, entitySolidBuilder, "base");
			matrix.push();
			matrix.translate(0f, 1.875f, 2f);
			model.renderOnly(matrix, combinedLightIn, combinedOverlayIn, entitySolidBuilder, "rod");
			matrix.pop();
		}

		matrix.pop();
	}
}
