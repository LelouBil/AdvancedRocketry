package zmaster587.advancedRocketry.armor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import zmaster587.advancedRocketry.api.AdvancedRocketryItems;
import zmaster587.advancedRocketry.api.IAtmosphere;
import zmaster587.advancedRocketry.api.armor.IArmorComponentHeavy;
import zmaster587.advancedRocketry.api.armor.IProtectiveArmor;
import zmaster587.advancedRocketry.api.capability.CapabilitySpaceArmor;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.advancedRocketry.client.render.armor.RenderJetPack;
import zmaster587.advancedRocketry.item.components.ItemSolarWings;
import zmaster587.advancedRocketry.item.components.ItemUpgrade;
import zmaster587.libVulpes.api.IArmorComponent;
import zmaster587.libVulpes.api.IJetPack;
import zmaster587.libVulpes.api.IModularArmor;
import zmaster587.libVulpes.util.EmbeddedInventory;
import zmaster587.libVulpes.util.IconResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
/**
 * Space Armor
 * Any class that extends this will gain the ability to store oxygen and will protect players from the vacuum atmosphere type
 *
 */

public class ItemSpaceArmor extends ArmorItem implements ICapabilityProvider, IProtectiveArmor, IModularArmor, IDyeableArmorItem {

	protected final static int BASE_POWER_USAGE = 40;
	private int numModules;
	public static final UUID WALK_SPEED_UUID = new UUID(2319, 9001);
	private BlockPos prev;
	private boolean movement = false;

	//We have to use $@#$@# magic here because the dragon doesn't actually do the correct type of damage
	private static final List<String> protectedDamages = Arrays.asList("indirectMagic", "cramming", "fireworks", "cactus", "inFire", "dragonBreath", "sweetBerryBush");

	public ItemSpaceArmor(Item.Properties props, IArmorMaterial material, EquipmentSlotType component, int numModules) {
		super(material, component, props);
		this.numModules = numModules;
	}

	@Override
	public boolean canBeExternallyModified(@Nonnull ItemStack armor, int slot) {
		return true;
	}

	@Override
	@ParametersAreNonnullByDefault
	public void addInformation(@Nonnull ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag p_77624_4_) {
		super.addInformation(stack, world, list, p_77624_4_);

		list.add(new TranslationTextComponent("msg.modules"));

		for(ItemStack componentStack : getComponents(stack)) {
			list.add(componentStack.getDisplayName());
		}
	}

	@Override
	@OnlyIn(value=Dist.CLIENT)
	public <A extends BipedModel<?>> A getArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlotType armorSlot, A _default) {
		if(armorSlot == EquipmentSlotType.CHEST) {
			for(ItemStack stack : getComponents(itemStack)) {
				if(stack.getItem() instanceof IJetPack)
					return (A) new RenderJetPack(_default);
			}
		}
		return super.getArmorModel(entityLiving, itemStack, armorSlot, _default);
	}

	public int getColor(@Nonnull ItemStack stack) {
		CompoundNBT nbttagcompound = stack.getTag();

		if (nbttagcompound != null) {
			CompoundNBT nbttagcompound1 = nbttagcompound.getCompound("display");
			if (nbttagcompound1.contains("color", 3)) {
				return nbttagcompound1.getInt("color");
			}
		}

		return 0xFFFFFF;

	}

	protected EmbeddedInventory loadEmbeddedInventory(ItemStack stack) {
		if(stack.hasTag()) {
			EmbeddedInventory inv = new EmbeddedInventory(numModules);
			inv.readFromNBT(stack.getTag());
			return inv;
		}
		return new EmbeddedInventory(numModules);
	}

	protected void saveEmbeddedInventory(@Nonnull ItemStack stack, EmbeddedInventory inv) {
		if(stack.hasTag()) {
			inv.write(stack.getTag());
		} else {
			CompoundNBT nbt = new CompoundNBT();
			inv.write(nbt);
			stack.setTag(nbt);
		}
	}
	
	@Override
	public void onArmorTick(@Nonnull ItemStack armor, World world, PlayerEntity player) {
		super.onArmorTick(armor, world, player);

		if(armor.hasTag() && world.getGameTime() != armor.getTag().getLong("time")) {
			boolean canFunction = canArmorFunction(player);
			//Whether the player is moving or not, calculated in a central place
			//Base power draw to move air around the suit, this is less than pressure eq.
			//Some upgrades modify player capabilities
			EmbeddedInventory inv = loadEmbeddedInventory(armor);

			//Run special chest stuff from here to prevent tick-repetition problems. I hate that this is here, but it fixes other problems so....
			if (armor.getItem() instanceof ItemSpaceChest) {
				transferEnergy(player, -BASE_POWER_USAGE);
				//Decrement air from damage
				int airLossFromDamage = 0;
				for (ItemStack stack : player.getArmorInventoryList()) {
					airLossFromDamage += stack.getDamage();
				}
				airLossFromDamage = (int) Math.sqrt(airLossFromDamage / 4);
				if (world.getGameTime() % 30 == 0) ((ItemSpaceChest)armor.getItem()).decrementAir(armor, airLossFromDamage);

				if (world.getGameTime() % 20 == 0) {
					movement = player.getPosition() != prev;
					prev = player.getPosition();
				}
			} else if (armor.getItem() == AdvancedRocketryItems.itemSpaceSuitLeggings && world.getGameTime() % 20 == 0) {
				movement = player.getPosition() != prev;
				prev = player.getPosition();
			}

			//Variables needed for leg determinations in loop
			int heavyUpgrades = 0;
			int legs = 0;
			//Use this to determine speed penalty to apply
			for(int i = 0; i < inv.getSizeInventory(); i++ ) {
				ItemStack stack = inv.getStackInSlot(i);
				if(!stack.isEmpty()) {
					IArmorComponent component = (IArmorComponent)stack.getItem();
					component.onTick(world, player, armor, inv, stack);
					if (component instanceof IArmorComponentHeavy) heavyUpgrades++;
					if (component instanceof ItemUpgrade && stack.getItem() == AdvancedRocketryItems.itemBionicLegsUpgrade) legs++;
					if (!(component instanceof ItemSolarWings) && transferEnergy(player, -component.getTickedPowerConsumption(stack, player)) == 0 && canFunction)
						component.onTick(world, player, armor, inv, stack);
					else if (component instanceof ItemSolarWings) {
						component.onTick(world, player, movement ? ItemStack.EMPTY : armor, inv, stack);
						transferEnergy(player, component.getTickedPowerConsumption(stack, player));
					}
					saveEmbeddedInventory(armor, inv);

					armor = player.getItemStackFromSlot(((ItemSpaceArmor)armor.getItem()).slot);
					inv = loadEmbeddedInventory(armor);
				}
			}

			//Handle speed penalty - default with suit is half speed minus 1/5th speed per heavy upgrade, but bionic legs can absolutely override this, and should be used to do so
			if (armor.getItem() == AdvancedRocketryItems.itemSpaceSuitLeggings || (!(player.getItemStackFromSlot(EquipmentSlotType.LEGS).getItem() instanceof ItemSpaceArmor) && armor.getItem() == AdvancedRocketryItems.itemSpaceSuitChestpiece ) && player.world.isRemote)
				handleWalkSpeedChage(player, legs, heavyUpgrades, armor.getItem() == AdvancedRocketryItems.itemSpaceSuitLeggings, movement);

			saveEmbeddedInventory(armor, inv);
			//Handle saving stuff to make sure that we don't load things multiple times per tick
			armor.getTag().putLong("time", world.getGameTime());
		} else if (!armor.hasTag()){
			CompoundNBT nbt = new CompoundNBT();
			nbt.putLong("time", world.getGameTime());
			armor.setTag(nbt);
		}
	}

	private void handleWalkSpeedChage(PlayerEntity player, int legs, int heavyUpgrades, boolean runChestCheck, boolean moving) {
		//Check the chest for this because otherwise we cause problems when both try to set stuff
		if(player.getItemStackFromSlot(EquipmentSlotType.CHEST).hasTag() && runChestCheck) {
			EmbeddedInventory inv = loadEmbeddedInventory(player.getItemStackFromSlot(EquipmentSlotType.CHEST));
			//Use this to determine speed penalty to apply
			for(int i = 0; i < inv.getSizeInventory(); i++ ) {
				ItemStack stack = inv.getStackInSlot(i);
				if(!stack.isEmpty()) {
					IArmorComponent component = (IArmorComponent)stack.getItem();
					if (component instanceof IArmorComponentHeavy) heavyUpgrades++;
				}
			}
		}

		float walkSpeedBase = (float)player.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
		float walkSpeedModifier = 0;
		float walkSpeedModifierSuit = walkSpeedBase/(!player.getItemStackFromSlot(EquipmentSlotType.CHEST).isEmpty() && !player.getItemStackFromSlot(EquipmentSlotType.LEGS).isEmpty() ? 2f : 4f);
		if(player.isSprinting()) {
			//If we have enough speed to boost, do so
			if (legs != 0 && 0 == transferEnergy(player, -60 * legs)) {
				walkSpeedModifier = legs * walkSpeedBase - (walkSpeedBase * (heavyUpgrades/5f));
			} else {
				walkSpeedModifier = -walkSpeedBase * (heavyUpgrades/5f);
			}
		} else {
			//The maximum possible effort the bionic legs can exert if we are to only hit base walk speed
			float maxEffect = -Math.min(legs * walkSpeedBase, walkSpeedModifierSuit + (walkSpeedBase * (heavyUpgrades/5f)));
			//Consume just enough power to boost us to normal speed, or all the power needed to get us as close as possible if we can't get there
			if (legs != 0 && moving) {
				if (transferEnergy(player, (int)(60 * maxEffect/walkSpeedBase)) == 0) walkSpeedModifier = Math.min(legs * walkSpeedBase - (walkSpeedBase * (heavyUpgrades/5f)), walkSpeedModifierSuit);
			} else {
				if (!moving) walkSpeedModifier = Math.min(legs * walkSpeedBase - (walkSpeedBase * (heavyUpgrades/5f)), walkSpeedModifierSuit);
				else walkSpeedModifier = -walkSpeedBase * (heavyUpgrades/5f);
			}
		}
		//Because of the suit itself
		walkSpeedModifier -= walkSpeedModifierSuit;
		//Make sure we don't go over bounds and make the player run backwards
		walkSpeedModifier = Math.max(walkSpeedModifier, -walkSpeedBase);

		//Actually handle application of the walk speed penalties/buffs
		if (player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(WALK_SPEED_UUID) == null) {
			player.getAttribute(Attributes.MOVEMENT_SPEED).applyNonPersistentModifier(new AttributeModifier(WALK_SPEED_UUID, "bioniclegs", walkSpeedModifier, AttributeModifier.Operation.ADDITION));
		} else if (player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(WALK_SPEED_UUID).getAmount() != walkSpeedModifier) {
			player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(WALK_SPEED_UUID));
			player.getAttribute(Attributes.MOVEMENT_SPEED).applyNonPersistentModifier(new AttributeModifier(WALK_SPEED_UUID, "bioniclegs", walkSpeedModifier, AttributeModifier.Operation.ADDITION));
		}
		//Handle jump penalties/buffs
		if (walkSpeedModifier <= -walkSpeedBase) {
			//This is slightly high to allow for jumps the standard way
			player.jumpMovementFactor = 0f;
		}
	}

	public static void removeWalkSpeedModifier(PlayerEntity player) {
		if (player.getAttribute(Attributes.MOVEMENT_SPEED) != null && player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(WALK_SPEED_UUID) != null) {
			player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(WALK_SPEED_UUID);
		}
	}

	public String getArmorTexture(@Nonnull ItemStack stack, Entity entity, EquipmentSlotType slot, String type) {
		//This string ABSOLUTELY can be null and in fact is _passed_ null. IDEA is lying
		if(type != null && type.equals("overlay")) {
			return "advancedrocketry:textures/armor/spacesuit_layer2.png";
		}
		return "advancedrocketry:textures/armor/spacesuit_layer1.png";
	}

	public static boolean doesArmorProtectFromDamageType(DamageSource source, LivingEntity player) {
		//Because apparently comparing damage types don't work, so I have to use strings
		return ItemSpaceArmor.canArmorFunction(player) && ((ItemSpaceArmor.protectedDamages.contains(source.damageType) && doesPlayerHaveEntireSuit(player)) || source.damageType.equals("hotFloor"));
	}

	public static boolean doesPlayerHaveEntireSuit(LivingEntity player) {
		for (ItemStack stack : player.getArmorInventoryList()) {
			if (!(stack.getItem() instanceof ItemSpaceArmor)) return false;
		}
		return true;
	}

	@Override
	public void addArmorComponent(World world, @Nonnull ItemStack armor, @Nonnull ItemStack component, int slot) {
		EmbeddedInventory inv = loadEmbeddedInventory(armor);

		if(((IArmorComponent)component.getItem()).onComponentAdded(world, armor)) {
			inv.setInventorySlotContents(slot, component);
			saveEmbeddedInventory(armor, inv);
		}
	}

	@Nonnull
	public ItemStack removeComponent(World world, ItemStack armor, int index) {
		EmbeddedInventory inv = loadEmbeddedInventory(armor);
		ItemStack stack = inv.getStackInSlot(index);
		inv.setInventorySlotContents(index, ItemStack.EMPTY);

		if(!stack.isEmpty()) {
			IArmorComponent component = (IArmorComponent) stack.getItem();
			ItemStack stack2 = component.onComponentRemoved(world, stack);
			if (!stack2.isEmpty()) stack = stack2;
			saveEmbeddedInventory(armor, inv);
		}

		return stack;
	}

	public List<ItemStack> getComponents(@Nonnull ItemStack armor) {
		List<ItemStack> list = new LinkedList<>();

		if(armor.hasTag()) {
			EmbeddedInventory inv = loadEmbeddedInventory(armor);

			for(int i = 0; i < inv.getSizeInventory(); i++) {
				if(!inv.getStackInSlot(i).isEmpty())
					list.add(inv.getStackInSlot(i));
			}
		}

		return list;
	}
	public boolean protectsFromSubstance(IAtmosphere atmosphere, @Nonnull ItemStack stack, Entity entity, boolean commitProtection) {
		return canArmorFunction((LivingEntity)entity) && (atmosphere == AtmosphereType.SUPERHIGHPRESSURE || atmosphere == AtmosphereType.HIGHPRESSURE || atmosphere == AtmosphereType.VACUUM || atmosphere == AtmosphereType.VERYHOT || atmosphere == AtmosphereType.SUPERHEATED || atmosphere == AtmosphereType.LOWOXYGEN || atmosphere == AtmosphereType.SUPERHIGHPRESSURENOO2 || atmosphere == AtmosphereType.HIGHPRESSURENOO2 || atmosphere == AtmosphereType.VERYHOTNOO2|| atmosphere == AtmosphereType.SUPERHEATEDNOO2  || atmosphere == AtmosphereType.NOO2);
	}

	@OnlyIn(Dist.CLIENT)
	public boolean nullifiesAtmosphereEffect(@Nonnull ItemStack armor) {
		boolean fog = false;
		if(armor.hasTag()) {
			EmbeddedInventory inv = loadEmbeddedInventory(armor);

			for(int i = 0; i < inv.getSizeInventory(); i++) {
				if(!inv.getStackInSlot(i).isEmpty()) {
					if (inv.getStackInSlot(i).getItem() instanceof ItemUpgrade) {
						if (inv.getStackInSlot(i).getItem() == AdvancedRocketryItems.itemAntiFogVisorUpgrade) fog = true;
						if (fog) break;
					}
				}
			}
		}
		return fog && canArmorFunction(Minecraft.getInstance().player);
	}


	//Does the armor have any amount of power in it
	public static boolean canArmorFunction(LivingEntity entity) {
		return (!getFirstPowerStorageArmor(entity).isEmpty() && ((ItemPoweredSpaceArmor)getFirstPowerStorageArmor(entity).getItem()).armorPieceHasEnergy(getFirstPowerStorageArmor(entity)));
	}

	//Returns the energy (or negative energy) un-transferred to the storage of the pieces
	protected int transferEnergy(LivingEntity entity, int energyDelta) {
		//Sorta inefficient but we will only ever check two pieces so it's easier than expandable system
		if (energyDelta != 0) {
			ItemStack stack = getFirstPowerStorageArmor(entity);
			if (!stack.isEmpty()) {
				energyDelta = ((ItemPoweredSpaceArmor) stack.getItem()).transferEnergy(stack, energyDelta);
				if (energyDelta != 0) {
					stack = entity.getItemStackFromSlot(getOppositePoweredSlot(((ItemSpaceArmor)stack.getItem()).slot));
					if (stack.getItem() instanceof ItemPoweredSpaceArmor) {
						energyDelta = ((ItemPoweredSpaceArmor) stack.getItem()).transferEnergy(stack, energyDelta);
					}
				}
			}
		}
		return energyDelta;
	}

	private EquipmentSlotType getOppositePoweredSlot(EquipmentSlotType slot) {
		return EquipmentSlotType.CHEST == slot ? EquipmentSlotType.LEGS : EquipmentSlotType.CHEST;
	}

	protected static ItemStack getFirstPowerStorageArmor(LivingEntity entity) {
		return entity.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() instanceof ItemPoweredSpaceArmor ? entity.getItemStackFromSlot(EquipmentSlotType.CHEST) : entity.getItemStackFromSlot(EquipmentSlotType.LEGS).getItem() instanceof ItemPoweredSpaceArmor ? entity.getItemStackFromSlot(EquipmentSlotType.LEGS) : ItemStack.EMPTY;
	}

	@Override
	public int getNumSlots(@Nonnull ItemStack stack) {
		return loadEmbeddedInventory(stack).getSizeInventory();
	}

	@Override
	@Nonnull
	public ItemStack getComponentInSlot(@Nonnull ItemStack stack, int slot) {
		return loadEmbeddedInventory(stack).getStackInSlot(slot);
	}

	@Override
	public IInventory loadModuleInventory(@Nonnull ItemStack stack) {
		return loadEmbeddedInventory(stack);
	}

	@Override
	public void saveModuleInventory(@Nonnull ItemStack stack, IInventory inv) {
		saveEmbeddedInventory(stack, (EmbeddedInventory)inv);
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
		if(capability == CapabilitySpaceArmor.PROTECTIVEARMOR)
			return LazyOptional.of(() -> this).cast();
		return LazyOptional.empty();
	}

	public boolean isItemValidForSlot(@Nonnull ItemStack stack, int slot) {
		return true;	
	}

	@Override
	public IconResource getResourceForSlot(int slot) {
		return null;
	}
}
