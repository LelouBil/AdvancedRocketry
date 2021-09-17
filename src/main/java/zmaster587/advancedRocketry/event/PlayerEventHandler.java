package zmaster587.advancedRocketry.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayer.SleepResult;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerConnectionFromClientEvent;
import zmaster587.advancedRocketry.advancements.ARAdvancements;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.stations.ISpaceObject;
import zmaster587.advancedRocketry.armor.ItemSpaceArmor;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.entity.EntityRocket;
import zmaster587.advancedRocketry.network.PacketConfigSync;
import zmaster587.advancedRocketry.network.PacketDimInfo;
import zmaster587.advancedRocketry.network.PacketSpaceStationInfo;
import zmaster587.advancedRocketry.network.PacketStellarInfo;
import zmaster587.advancedRocketry.stations.SpaceObjectManager;
import zmaster587.advancedRocketry.stations.SpaceStationObject;
import zmaster587.advancedRocketry.world.provider.WorldProviderPlanet;
import zmaster587.advancedRocketry.world.util.TeleporterNoPortal;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;

import javax.annotation.Nonnull;

public class PlayerEventHandler {

	public static long time = 0;

	//Handle gravity
	@SubscribeEvent
	public void playerTick(LivingUpdateEvent event) {

		if(event.getEntity().world.isRemote && event.getEntity().posY > 260 && event.getEntity().posY < 270 && event.getEntity().motionY < -.1) {
			RocketEventHandler.destroyOrbitalTextures(event.getEntity().world);
		}
		if(event.getEntity().isInWater()) {
			if(AtmosphereType.LOWOXYGEN.isImmune(event.getEntityLiving()))
				event.getEntity().setAir(300);
		}

		if(!event.getEntity().world.isRemote && event.getEntity().world.getTotalWorldTime() % 20 ==0 && event.getEntity() instanceof EntityPlayer) {
			if(DimensionManager.getInstance().getDimensionProperties(event.getEntity().world.provider.getDimension()).getName().equals("Luna") && 
					event.getEntity().getPosition().distanceSq(2347,80, 67) < 512 ) {
				ARAdvancements.WENT_TO_THE_MOON.trigger((EntityPlayerMP)event.getEntity());
			}
		}

		if(event.getEntity() instanceof EntityPlayer && event.getEntity().world.provider.getDimension() == ARConfiguration.getCurrentConfig().spaceDimId && SpaceObjectManager.getSpaceManager().getSpaceStationFromBlockCoords(event.getEntity().getPosition()) == null && !(event.getEntity().getRidingEntity() instanceof EntityRocket)) {
			double distance = 0;
			HashedBlockPosition teleportPosition = null;
			for (ISpaceObject spaceObject : SpaceObjectManager.getSpaceManager().getSpaceObjects()) {
				if (spaceObject instanceof SpaceStationObject) {
					SpaceStationObject station = ((SpaceStationObject) spaceObject);
					double distanceTo = event.getEntity().getPosition().getDistance(station.getSpawnLocation().x, station.getSpawnLocation().y, station.getSpawnLocation().z);
					if (distanceTo > distance) {
						distance = distanceTo;
						teleportPosition = station.getSpawnLocation();
					}
				}
			}
			if (teleportPosition != null) {
				event.getEntity().sendMessage(new TextComponentString(LibVulpes.proxy.getLocalizedString("msg.chat.nostation1")));
				event.getEntity().sendMessage(new TextComponentString(LibVulpes.proxy.getLocalizedString("msg.chat.nostation2")));
				event.getEntity().setPositionAndUpdate(teleportPosition.x, teleportPosition.y, teleportPosition.z);
			} else {
				event.getEntity().sendMessage(new TextComponentString(LibVulpes.proxy.getLocalizedString("msg.chat.nostation3")));
				event.getEntity().getServer().getPlayerList().transferPlayerToDimension((EntityPlayerMP)event.getEntity(), 0, new TeleporterNoPortal( net.minecraftforge.common.DimensionManager.getWorld(0) ));
			}

		}

		//GravityHandler.applyGravity(event.getEntity());
	}

	@SubscribeEvent
	public void sleepEvent(@Nonnull PlayerSleepInBedEvent event) {

		if(event.getEntity().world.provider instanceof WorldProviderPlanet) {
			WorldProvider provider = event.getEntity().world.provider;
			AtmosphereHandler atmhandler = AtmosphereHandler.getOxygenHandler(provider.getDimension());

			if (!ARConfiguration.getCurrentConfig().forcePlayerRespawnInSpace && AtmosphereHandler.hasAtmosphereHandler(provider.getDimension()) && atmhandler != null &&
					!atmhandler.getAtmosphereType(event.getPos()).isBreathable()) {
				event.setResult(SleepResult.OTHER_PROBLEM);
			}
		}
	}

	@SubscribeEvent
	public void disconnected(ClientDisconnectionFromServerEvent event) {
		// Reload configs from disk
		ARConfiguration.useClientDiskConfig();
		//zmaster587.advancedRocketry.dimension.DimensionManager.getInstance().unregisterAllDimensions();
	}

	//Make sure the player receives data about the dimensions
	@SubscribeEvent
	public void playerLoggedInEvent(ServerConnectionFromClientEvent event) {

		//Send config first
		if(!event.isLocal())
			PacketHandler.sendToDispatcher(new PacketConfigSync(), event.getManager());

		//Make sure stars are sent next
		for(int i : DimensionManager.getInstance().getStarIds()) {
			PacketHandler.sendToDispatcher(new PacketStellarInfo(i, DimensionManager.getInstance().getStar(i)), event.getManager());
		}

		for(int i : DimensionManager.getInstance().getRegisteredDimensions()) {
			PacketHandler.sendToDispatcher(new PacketDimInfo(i, DimensionManager.getInstance().getDimensionProperties(i)), event.getManager());
		}

		for(ISpaceObject spaceObject : SpaceObjectManager.getSpaceManager().getSpaceObjects()) {
			PacketHandler.sendToDispatcher(new PacketSpaceStationInfo(spaceObject.getId(), spaceObject), event.getManager());
		}

		PacketHandler.sendToDispatcher(new PacketDimInfo(0, DimensionManager.getInstance().getDimensionProperties(0)), event.getManager());
	}

	public void connectToServer(ClientConnectedToServerEvent event) {
		DimensionManager.getInstance().unregisterAllDimensions();
	}

	@SubscribeEvent
	public void LivingEquipmentChangeEvent(LivingEquipmentChangeEvent event) {
		if (event.getFrom().getItem() instanceof ItemSpaceArmor && event.getEntity() instanceof EntityPlayer) {
			ItemSpaceArmor.removeWalkSpeedModifier((EntityPlayer)event.getEntity());
		}

	}

	@SubscribeEvent
	public void LivingHurtEvent(LivingHurtEvent event) {
		event.setCanceled(ItemSpaceArmor.doesArmorProtectFromDamageType(event.getSource(), event.getEntityLiving()));
	}
}
