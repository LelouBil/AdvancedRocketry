package zmaster587.advancedRocketry.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.FlyingEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.item.FallingBlockEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.entity.item.minecart.MinecartEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import zmaster587.advancedRocketry.api.AdvancedRocketryAPI;
import zmaster587.advancedRocketry.api.IGravityManager;
import zmaster587.advancedRocketry.dimension.DimensionManager;

import java.util.WeakHashMap;

public class GravityHandler implements IGravityManager {

	public static final float LIVING_OFFSET = 0.0755f;
	public static final float FLUID_LIVING_OFFSET = 0.02f;
	public static final float THROWABLE_OFFSET = 0.03f;
	public static final float OTHER_OFFSET = 0.04f;
	public static final float ARROW_OFFSET = 0.05f;
	
	static {
		AdvancedRocketryAPI.gravityManager = new GravityHandler();
	}
	
	private static WeakHashMap<Entity, Double> entityMap = new WeakHashMap<>();

	public static void applyGravity(Entity entity) {
		if(entity.hasNoGravity()) return;
		    //Because working gravity on elytra-flying players can cause..... severe problems at lower gravity, it is my utter delight to announce to you elytra are now magic!
			//This totally isn't because Mojang decided for some godforsaken @#@#@#% reason to make ALL WAYS TO SET ELYTRA FLIGHT _protected_
			//With no set methods
			//So I cannot, without much more effort than it's worth, set elytra flight. Therefore, they're magic.
			if ((!(entity instanceof PlayerEntity) && !(entity instanceof FlyingEntity)) || (!(entity instanceof FlyingEntity) && !(((PlayerEntity) entity).abilities.isFlying || ((LivingEntity) entity).isElytraFlying()))) {
				Double d;
				if(entityMap.containsKey(entity) && (d = entityMap.get(entity)) != null)  {
					double multiplier = (isOtherEntity(entity) || entity instanceof ItemEntity) ? OTHER_OFFSET * d : (entity instanceof ArrowEntity ) ? ARROW_OFFSET * d : (entity instanceof ThrowableEntity) ? THROWABLE_OFFSET * d : LIVING_OFFSET * d;
					entity.setMotion(entity.getMotion().add(0, multiplier, 0));
					
				} else if (DimensionManager.getInstance().isDimensionCreated(entity.world)) {
					double gravity = DimensionManager.getInstance().getDimensionProperties(entity.world, entity.getPosition()).gravitationalMultiplier;

					if (entity instanceof ItemEntity)
						entity.setMotion(entity.getMotion().add(0, -(gravity * OTHER_OFFSET - OTHER_OFFSET),0));
					else if (isOtherEntity(entity))
						entity.setMotion(entity.getMotion().add(0, -(gravity * OTHER_OFFSET - OTHER_OFFSET),0));
					else if (entity instanceof ThrowableEntity)
						entity.setMotion(entity.getMotion().add(0, -(gravity * THROWABLE_OFFSET - THROWABLE_OFFSET),0));
					else if (entity instanceof ArrowEntity)
						entity.setMotion(entity.getMotion().add(0, -(gravity * ARROW_OFFSET - ARROW_OFFSET),0));
					else if (entity instanceof LivingEntity && entity.isInWater() || entity.isInLava())
						entity.setMotion(entity.getMotion().add(0, -(gravity * FLUID_LIVING_OFFSET - FLUID_LIVING_OFFSET),0));
					else if (entity instanceof  LivingEntity)
						entity.setMotion(entity.getMotion().add(0, -(gravity * LIVING_OFFSET - LIVING_OFFSET),0));
				}
			}
	}

	public static boolean isOtherEntity(Entity entity) {
		return entity instanceof BoatEntity || entity instanceof MinecartEntity || entity instanceof FallingBlockEntity || entity instanceof TNTEntity;
	}

	@Override
	public void setGravityMultiplier(Entity entity, double multiplier) {
		//TODO: packet handling
		entityMap.put(entity, multiplier);
	}

	@Override
	public void clearGravityEffect(Entity entity) {
		entityMap.remove(entity);
	}
}
