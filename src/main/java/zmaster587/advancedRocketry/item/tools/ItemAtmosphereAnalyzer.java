package zmaster587.advancedRocketry.item.tools;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.inventory.TextureResources;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.api.IArmorComponent;
import zmaster587.libVulpes.client.ResourceIcon;
import zmaster587.libVulpes.render.RenderHelper;
import zmaster587.libVulpes.util.ZUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;


public class ItemAtmosphereAnalyzer extends Item implements IArmorComponent {

	public ItemAtmosphereAnalyzer(Properties properties) {
		super(properties);
	}

	private static ResourceIcon icon;
	private static ResourceLocation eyeCandySpinner = new ResourceLocation("advancedrocketry:textures/gui/eyecandy/spinnything.png");
	
	private static String breathable = LibVulpes.proxy.getLocalizedString("msg.atmanal.canbreathe");
	private static String atmtype = LibVulpes.proxy.getLocalizedString("msg.atmanal.atmtype");
	private static String yes = LibVulpes.proxy.getLocalizedString("msg.yes");
	private static String no = LibVulpes.proxy.getLocalizedString("msg.no");

	@Override
	public void onTick(World world, PlayerEntity player, ItemStack armorStack, IInventory modules, ItemStack componentStack) { }

	private List<ITextComponent> getAtmosphereReadout(@Nonnull ItemStack stack, @Nullable AtmosphereType atm, @Nonnull World world) {
		if(atm == null)
			atm = AtmosphereType.AIR;
		

		List<ITextComponent> str = new LinkedList<>();
		
		str.add(new TranslationTextComponent("%s %s %s",
				new TranslationTextComponent("msg.atmanal.atmtype"),
				new TranslationTextComponent(atm.getUnlocalizedName()),
				new StringTextComponent((AtmosphereHandler.currentPressure == -1 ? (DimensionManager.getInstance().isDimensionCreated(ZUtils.getDimensionIdentifier(world)) ? DimensionManager.getInstance().getDimensionProperties(world).getAtmosphereDensity()/100f : 1) : AtmosphereHandler.currentPressure/100f) + " atm")
				));
		str.add(new TranslationTextComponent("%s %s", 
				new TranslationTextComponent("msg.atmanal.canbreathe"),
				atm.isBreathable() ? new TranslationTextComponent("msg.yes") : new TranslationTextComponent("msg.no")));
		
		return str;
	}

	@Nonnull
	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, @Nonnull Hand hand) {
		ItemStack stack = playerIn.getHeldItem(hand);
		if(!worldIn.isRemote) {
			List<ITextComponent> str = getAtmosphereReadout(stack, (AtmosphereType) AtmosphereHandler.getOxygenHandler(worldIn).getAtmosphereType(playerIn),worldIn);
			for(ITextComponent str1 : str)
				playerIn.sendMessage(str1, Util.DUMMY_UUID);
		}
		return super.onItemRightClick(worldIn, playerIn, hand);
	}

	@Override
	public boolean onComponentAdded(World world, @Nonnull ItemStack armorStack) {
		return true;
	}

	@Override
	public ItemStack onComponentRemoved(World world, @Nonnull ItemStack componentStack) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean isAllowedInSlot(ItemStack componentStack, EquipmentSlotType targetSlot) {
		return targetSlot == EquipmentSlotType.HEAD;
	}
	
	
	@OnlyIn(value=Dist.CLIENT)
	@Override
	public void renderScreen(MatrixStack matrix, ItemStack componentStack, List<ItemStack> modules, RenderGameOverlayEvent event, Screen gui) {
		//Generic setup stuff
		int screenX = 10;
		int screenY = event.getWindow().getScaledHeight() - 36; // 26 tall + 10 for padding

		//Text stuff
		FontRenderer fontRenderer = Minecraft.getInstance().fontRenderer;
		List<ITextComponent> str = getAtmosphereReadout(componentStack, (AtmosphereType) AtmosphereHandler.currentAtm, Minecraft.getInstance().world);
		fontRenderer.drawText(matrix, str.get(0), screenX + 32, screenY, 0xaaffff);
		fontRenderer.drawText(matrix, str.get(1), screenX + 32, screenY + fontRenderer.FONT_HEIGHT*4/3, 0xaaffff);
	
		//Render Eyecandy
		RenderSystem.color3f(1f, 1f, 1f);
		matrix.push();
		Minecraft.getInstance().getTextureManager().bindTexture(eyeCandySpinner);
		matrix.translate(screenX + 12, screenY + 8, 0);

		//Set up proper render styles for the screen, we need transparency
		RenderSystem.enableAlphaTest();
		RenderSystem.defaultAlphaFunc();
		RenderSystem.enableBlend();

		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		matrix.rotate(new Quaternion(0, 0,  (Minecraft.getInstance().world.getGameTime() ) % 360, true));
		RenderHelper.renderNorthFaceWithUV(matrix, buffer, -1, -16,  -16, 16,  16, 0, 1, 0, 1);
		Tessellator.getInstance().draw();
		matrix.pop();

		//Set up proper render styles for the screen, we need transparency
		RenderSystem.enableAlphaTest();
		RenderSystem.defaultAlphaFunc();
		RenderSystem.enableBlend();

		//Draw BG
		Minecraft.getInstance().getTextureManager().bindTexture(TextureResources.frameHUDBG);
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		RenderHelper.renderNorthFaceWithUV(matrix, buffer, -1, screenX - 8,  screenY - 12, screenX + 8,  screenY + 26, 0, 0.25f, 0, 1);
		RenderHelper.renderNorthFaceWithUV(matrix, buffer, -1, screenX + 8,  screenY - 12, screenX + 212,  screenY + 26, 0.5f, 0.5f, 0, 1);
		RenderHelper.renderNorthFaceWithUV(matrix, buffer, -1, screenX + 212,  screenY - 12, screenX + 228,  screenY + 26, 0.75f, 1f, 0, 1);
		Tessellator.getInstance().draw();
	}

	@Override
	public ResourceIcon getComponentIcon(@Nonnull ItemStack armorStack) {
		return null;
	}

	@Override
	public int getTickedPowerConsumption(ItemStack component, Entity entity) {return 30;}

}
