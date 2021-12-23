package zmaster587.advancedRocketry.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;
import zmaster587.advancedRocketry.api.Constants;
import zmaster587.advancedRocketry.api.EntityRocketBase;
import zmaster587.advancedRocketry.entity.EntityHoverCraft;
import zmaster587.advancedRocketry.entity.EntityRocket;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.interfaces.INetworkEntity;
import zmaster587.libVulpes.network.PacketChangeKeyState;
import zmaster587.libVulpes.network.PacketEntity;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.InputSyncHandler;

@OnlyIn(value=Dist.CLIENT)
public class KeyBindings {

	boolean prevState;
	@SubscribeEvent
	public void onKeyInput(InputEvent.KeyInputEvent event) {
		final Minecraft minecraft = Minecraft.getInstance();
		final ClientPlayerEntity player = minecraft.player;

		//Prevent control when a GUI is open
		if(Minecraft.getInstance().currentScreen != null) return;

		if(player.getRidingEntity() != null && player.getRidingEntity() instanceof EntityRocket) {
			EntityRocket rocket = (EntityRocket)player.getRidingEntity();
			if(Minecraft.getInstance().isGameFocused() && player.equals(Minecraft.getInstance().player)) {
				if(!rocket.isInFlight() && event.getKey() == GLFW.GLFW_KEY_SPACE && event.getAction() == GLFW.GLFW_PRESS) {
					rocket.prepareLaunch();
				}
			}
		}

		if(player.getRidingEntity() != null && player.getRidingEntity() instanceof EntityHoverCraft) {
			EntityHoverCraft hoverCraft = (EntityHoverCraft)player.getRidingEntity();
			if(Minecraft.getInstance().isGameFocused() && player.equals(Minecraft.getInstance().player)) {
				hoverCraft.onTurnLeft(turnRocketLeft.isKeyDown());
				hoverCraft.onTurnRight(turnRocketRight.isKeyDown());
				hoverCraft.onUp(turnRocketUp.isKeyDown());
				hoverCraft.onDown(turnRocketDown.isKeyDown());
			}
		}
		
		if(toggleJetpack.isPressed()) {
			if(player.isSneaking())
				PacketHandler.sendToServer(new PacketChangeKeyState(1, false));
			else
				PacketHandler.sendToServer(new PacketChangeKeyState(0, false));
		}

		if(openRocketUI.isPressed()) {
			if(player.getRidingEntity() instanceof EntityRocketBase) {
				PacketHandler.sendToServer(new PacketEntity((INetworkEntity) player.getRidingEntity(), (byte)EntityRocket.PacketType.OPENGUI.ordinal()));
			}
		}


		if(event.getKey() == GLFW.GLFW_KEY_SPACE && (event.getAction() == GLFW.GLFW_RELEASE) == prevState) {
			prevState = event.getAction() != GLFW.GLFW_RELEASE;
			InputSyncHandler.updateKeyPress(player, GLFW.GLFW_KEY_SPACE , prevState);
			PacketHandler.sendToServer(new PacketChangeKeyState(GLFW.GLFW_KEY_SPACE , prevState));
		}
	}

	//static KeyBinding launch = new KeyBinding("Launch", Keyboard.KEY_SPACE, "key.controls." + Constants.modId);
	static KeyBinding toggleJetpack = new KeyBinding(LibVulpes.proxy.getLocalizedString("key.togglejetpack"), GLFW.GLFW_KEY_X, LibVulpes.proxy.getLocalizedString("key.controls." + Constants.modId));
	static KeyBinding openRocketUI	= new KeyBinding(LibVulpes.proxy.getLocalizedString("key.openrocketui"), GLFW.GLFW_KEY_C, LibVulpes.proxy.getLocalizedString("key.controls." + Constants.modId));
	static KeyBinding turnRocketLeft		= new KeyBinding(LibVulpes.proxy.getLocalizedString("key.turnrocketleft"), GLFW.GLFW_KEY_A, LibVulpes.proxy.getLocalizedString("key.controls." + Constants.modId));
	static KeyBinding turnRocketRight		= new KeyBinding(LibVulpes.proxy.getLocalizedString("key.turnrocketright"), GLFW.GLFW_KEY_D, LibVulpes.proxy.getLocalizedString("key.controls." + Constants.modId));
	static KeyBinding turnRocketUp		= new KeyBinding(LibVulpes.proxy.getLocalizedString("key.turnrocketup"), GLFW.GLFW_KEY_Z, LibVulpes.proxy.getLocalizedString("key.controls." + Constants.modId));
	static KeyBinding turnRocketDown		= new KeyBinding(LibVulpes.proxy.getLocalizedString("key.turnrocketdown"), GLFW.GLFW_KEY_X, LibVulpes.proxy.getLocalizedString("key.controls." + Constants.modId));

	public static void init() {
		//ClientRegistry.registerKeyBinding(launch);
		ClientRegistry.registerKeyBinding(toggleJetpack);
		ClientRegistry.registerKeyBinding(openRocketUI);
		ClientRegistry.registerKeyBinding(turnRocketRight);
		ClientRegistry.registerKeyBinding(turnRocketLeft);
		ClientRegistry.registerKeyBinding(turnRocketUp);
		ClientRegistry.registerKeyBinding(turnRocketDown);
	}
}