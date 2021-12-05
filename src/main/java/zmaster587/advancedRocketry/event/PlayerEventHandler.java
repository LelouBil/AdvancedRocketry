package zmaster587.advancedRocketry.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerEntity.SleepResult;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import zmaster587.advancedRocketry.advancements.ARAdvancements;
import zmaster587.advancedRocketry.api.ARConfiguration;
import zmaster587.advancedRocketry.api.stations.ISpaceObject;
import zmaster587.advancedRocketry.armor.ItemSpaceArmor;
import zmaster587.advancedRocketry.atmosphere.AtmosphereHandler;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.network.PacketDimInfo;
import zmaster587.advancedRocketry.network.PacketSpaceStationInfo;
import zmaster587.advancedRocketry.network.PacketStellarInfo;
import zmaster587.advancedRocketry.stations.SpaceObjectManager;
import zmaster587.advancedRocketry.stations.SpaceStationObject;
import zmaster587.libVulpes.network.PacketHandler;
import zmaster587.libVulpes.util.HashedBlockPosition;
import zmaster587.libVulpes.util.ZUtils;

import javax.annotation.Nonnull;

public class PlayerEventHandler {

	public static long time = 0;

	//Handle gravity
	@SubscribeEvent
	public void playerTick(LivingUpdateEvent event) {

		if(event.getEntity().world.isRemote && event.getEntity().getPosY() > 260 && event.getEntity().getPosY() < 270 && event.getEntity().getMotion().y < -.1) {
			RocketEventHandler.destroyOrbitalTextures(event.getEntity().world);
		}
		if(event.getEntity().isInWater()) {
			if(AtmosphereType.LOWOXYGEN.isImmune(event.getEntityLiving()))
				event.getEntity().setAir(300);
		}

		if(!event.getEntity().world.isRemote && event.getEntity().world.getGameTime() % 20 ==0 && event.getEntity() instanceof PlayerEntity) {
			if(DimensionManager.getInstance().getDimensionProperties(ZUtils.getDimensionIdentifier(event.getEntity().world)).getName().equals("Luna") &&
					event.getEntity().getPositionVec().squareDistanceTo(2347,80, 67) < 512 ) {
				ARAdvancements.triggerAdvancement(ARAdvancements.WENT_TO_THE_MOON, (ServerPlayerEntity)event.getEntity());
			}
			if(event.getEntity() instanceof PlayerEntity && ZUtils.getDimensionIdentifier(event.getEntity().world).equals(DimensionManager.spaceId) && SpaceObjectManager.getSpaceManager().getSpaceStationFromBlockCoords(event.getEntity().getPosition()) == null) {
				double distance = 0;
				HashedBlockPosition teleportPosition = null;
				for (ISpaceObject spaceObject : SpaceObjectManager.getSpaceManager().getSpaceObjects()) {
					if (spaceObject instanceof SpaceStationObject) {
						SpaceStationObject station = ((SpaceStationObject) spaceObject);
						double distanceTo = event.getEntity().getDistanceSq(station.getSpawnLocation().x, station.getSpawnLocation().y, station.getSpawnLocation().z);
						if (distanceTo > distance) {
							distance = distanceTo;
							teleportPosition = station.getSpawnLocation();
						}
					}
				}
				if (teleportPosition != null) {
					event.getEntity().sendMessage(new TranslationTextComponent("msg.chat.nostation1"), Util.DUMMY_UUID);
					event.getEntity().sendMessage(new TranslationTextComponent("msg.chat.nostation2"), Util.DUMMY_UUID);
					event.getEntity().setPositionAndUpdate(teleportPosition.x, teleportPosition.y, teleportPosition.z);
				}
			}
		}
	}

	@SubscribeEvent
	public void sleepEvent(@Nonnull PlayerSleepInBedEvent event) {
		DimensionProperties props = DimensionManager.getInstance().getDimensionProperties(ZUtils.getDimensionIdentifier(event.getEntity().getEntityWorld()));
		if(props != DimensionManager.defaultSpaceDimensionProperties) {
			if (!ARConfiguration.getCurrentConfig().forcePlayerRespawnInSpace.get() && AtmosphereHandler.hasAtmosphereHandler(event.getEntity().world) && 
					!AtmosphereHandler.getOxygenHandler(event.getEntity().world).getAtmosphereType(event.getPos()).isBreathable()) {
				event.setResult(SleepResult.OTHER_PROBLEM);
			}
		}
	}

	//Make sure the player receives data about the dimensions
	@SubscribeEvent
	public void playerLoggedInEvent(PlayerLoggedInEvent event) {
		PlayerEntity mgr = event.getPlayer();

		//Send config first
		//DistExecutor.runWhenOn(Dist.DEDICATED_SERVER, () -> () -> { PacketHandler.sendToPlayer(new PacketConfigSync(), mgr); } );

		//Make sure stars are sent next
		for(ResourceLocation i : DimensionManager.getInstance().getStarIds()) {
			PacketHandler.sendToPlayer(new PacketStellarInfo(i, DimensionManager.getInstance().getStar(i)), mgr);
		}

		for(ResourceLocation i : DimensionManager.getInstance().getRegisteredDimensions()) {
			PacketHandler.sendToPlayer(new PacketDimInfo(i, DimensionManager.getInstance().getDimensionProperties(i)), mgr);
		}

		for(ISpaceObject obj : SpaceObjectManager.getSpaceManager().getSpaceObjects()) {
			PacketHandler.sendToPlayer(new PacketSpaceStationInfo(obj.getId(), obj), mgr);
		}
	}

	//Make sure the player doesn't die on low gravity worlds
	@SubscribeEvent
	public void fallEvent(LivingFallEvent event) {
		if(DimensionManager.getInstance().isDimensionCreated(ZUtils.getDimensionIdentifier(event.getEntity().world))) {
			DimensionProperties planet =  DimensionManager.getInstance().getDimensionProperties(ZUtils.getDimensionIdentifier(event.getEntity().world));
			event.setDistance(event.getDistance() * planet.getGravitationalMultiplier(new BlockPos(event.getEntity().getPositionVec())));
		}
	}

	@SubscribeEvent
	public void LivingEquipmentChangeEvent(LivingEquipmentChangeEvent event) {
		if (event.getFrom().getItem() instanceof ItemSpaceArmor && event.getEntity() instanceof PlayerEntity) {
			ItemSpaceArmor.removeWalkSpeedModifier((PlayerEntity) event.getEntity());
		}

	}

	@SubscribeEvent
	public void LivingHurtEvent(LivingHurtEvent event) {
		event.setCanceled(ItemSpaceArmor.doesArmorProtectFromDamageType(event.getSource(), event.getEntityLiving()));
	}
}
