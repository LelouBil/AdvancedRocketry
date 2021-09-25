package zmaster587.advancedRocketry.client.render.multiblocks;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import zmaster587.advancedRocketry.backwardCompat.ModelFormatException;
import zmaster587.advancedRocketry.backwardCompat.WavefrontObject;
import zmaster587.libVulpes.api.material.MaterialRegistry;
import zmaster587.libVulpes.block.RotatableBlock;
import zmaster587.libVulpes.tile.multiblock.TileMultiblockMachine;

public class RendererLiquefactionPlant extends TileEntitySpecialRenderer {
	WavefrontObject model;

	ResourceLocation texture = new ResourceLocation("advancedrocketry:textures/models/liquefactionplant.png");

	public RendererLiquefactionPlant() {
		try {
			model = new WavefrontObject(new ResourceLocation("advancedrocketry:models/liquefactionplant.obj"));
		} catch (ModelFormatException e) {
			e.printStackTrace();
		}

		GL11.glNewList(GL11.glGenLists(1), GL11.GL_COMPILE);
		model.renderOnly("base");
		GL11.glEndList();
	}

	@Override
	public void render(TileEntity tile, double x, double y, double z, float f, int damage, float a) {
		TileMultiblockMachine multiBlockTile = (TileMultiblockMachine)tile;

		if(!multiBlockTile.canRender())
			return;

		GL11.glPushMatrix();

		//Rotate and move the model into position
		GL11.glTranslated(x + .5f, y, z + 0.5f);
		EnumFacing front = RotatableBlock.getFront(tile.getWorld().getBlockState(tile.getPos())); //tile.getWorldObj().getBlockMetadata(tile.xCoord, tile.yCoord, tile.zCoord));
		GL11.glRotatef((front.getFrontOffsetX() == 1 ? 0 : 180) + front.getFrontOffsetZ()*90f - 90f, 0, 1, 0);
		GL11.glTranslated(-1.5f, 0f, -0.5f);


		if(multiBlockTile.isRunning()) {
			bindTexture(texture);
			model.renderPart("base");



			GL11.glPushMatrix();
			GL11.glTranslatef(0f, 1.875f, 2f);
			GL11.glPushMatrix();
			GL11.glRotated(multiBlockTile.getWorld().getTotalWorldTime() * -75f, 1f, 0, 0);
			model.renderOnly("rod");
			GL11.glPopMatrix();
			GL11.glPopMatrix();

		}
		else {
			bindTexture(texture);
			model.renderPart("base");
			GL11.glPushMatrix();
			GL11.glTranslatef(0f, 1.875f, 2f);
			model.renderOnly("rod");
			GL11.glPopMatrix();
		}
		GL11.glPopMatrix();
	}
}
