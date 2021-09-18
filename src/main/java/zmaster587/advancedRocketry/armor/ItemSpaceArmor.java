package zmaster587.advancedRocketry.armor;

import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.common.ISpecialArmor;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import zmaster587.advancedRocketry.api.AdvancedRocketryItems;
import zmaster587.advancedRocketry.api.IAtmosphere;
import zmaster587.advancedRocketry.api.armor.IArmorComponentHeavy;
import zmaster587.advancedRocketry.api.armor.IProtectiveArmor;
import zmaster587.advancedRocketry.api.capability.CapabilitySpaceArmor;
import zmaster587.advancedRocketry.atmosphere.AtmosphereType;
import zmaster587.advancedRocketry.client.render.armor.RenderJetPack;
import zmaster587.advancedRocketry.item.components.ItemSolarWings;
import zmaster587.advancedRocketry.item.components.ItemUpgrade;
import zmaster587.libVulpes.LibVulpes;
import zmaster587.libVulpes.api.IArmorComponent;
import zmaster587.libVulpes.api.IJetPack;
import zmaster587.libVulpes.api.IModularArmor;
import zmaster587.libVulpes.util.EmbeddedInventory;
import zmaster587.libVulpes.util.IconResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Space Armor
 * Any class that extends this will gain the ability to store oxygen and will protect players from the vacuum atmosphere type
 *
 */

public class ItemSpaceArmor extends ItemArmor implements ISpecialArmor, ICapabilityProvider, IProtectiveArmor, IModularArmor {

	protected final static int BASE_POWER_USAGE = 40;
	private int numModules;

	public static final IAttribute SPEED_ATTR = SharedMonsterAttributes.MOVEMENT_SPEED;
	public static final UUID WALK_SPEED_UUID = UUID.fromString("0ea6ce8e-d2e8-11e5-ab30-625662870761");

	//We have to use $@#$@# magic here because the dragon doesn't actually do the correct type of damage
	private static final List<String> protectedDamages = Arrays.asList("indirectMagic", "cramming", "fireworks", "cactus", "inFire", "dragonBreath");


	public ItemSpaceArmor(ArmorMaterial material, EntityEquipmentSlot component, int numModules) {
		super(material, 0, component);
		this.numModules = numModules;
	}

	@Override
	public boolean canBeExternallyModified(@Nonnull ItemStack armor, int slot) {
		return true;
	}

	@Override
	public void addInformation(@Nonnull ItemStack stack, World p_77624_2_, List<String> list, ITooltipFlag p_77624_4_) {
		super.addInformation(stack, p_77624_2_, list, p_77624_4_);

		list.add(LibVulpes.proxy.getLocalizedString("msg.modules"));

		for(ItemStack componentStack : getComponents(stack)) {
			list.add(ChatFormatting.DARK_GRAY + componentStack.getDisplayName());
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public ModelBiped getArmorModel(EntityLivingBase entityLiving, ItemStack itemStack, EntityEquipmentSlot armorSlot, ModelBiped _default) {

		if(armorSlot == EntityEquipmentSlot.CHEST) {
			for(ItemStack stack : getComponents(itemStack)) {
				if(stack.getItem() instanceof IJetPack)
					return new RenderJetPack(_default);
			}
		}
		return super.getArmorModel(entityLiving, itemStack, armorSlot, _default);
	}

	public int getColor(@Nonnull ItemStack stack) {

		NBTTagCompound nbttagcompound = stack.getTagCompound();

		if (nbttagcompound != null) {
			NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("display");

			if (nbttagcompound1.hasKey("color", 3)) {
				return nbttagcompound1.getInteger("color");
			}
		}

		return 0xFFFFFF;

	}

	@Override
	public ArmorProperties getProperties(EntityLivingBase player, @Nonnull ItemStack armor, DamageSource source, double damage, int slot) {
		return new ArmorProperties(0, 1.0, Integer.MAX_VALUE);
	}

	protected EmbeddedInventory loadEmbeddedInventory(@Nonnull ItemStack stack) {
		if(stack.hasTagCompound()) {
			EmbeddedInventory inv = new EmbeddedInventory(numModules);
			inv.readFromNBT(stack.getTagCompound());
			return inv;
		}
		return new EmbeddedInventory(numModules);
	}

	protected void saveEmbeddedInventory(@Nonnull ItemStack stack, EmbeddedInventory inv) {
		if(stack.hasTagCompound()) {
			inv.writeToNBT(stack.getTagCompound());
		}
		else {
			NBTTagCompound nbt = new NBTTagCompound();
			inv.writeToNBT(nbt);
			stack.setTagCompound(nbt);
		}
	}

	@Override
	public void onArmorTick(World world, EntityPlayer player, @Nonnull ItemStack armor) {
		//Run all the super stuff
		super.onArmorTick(world, player, armor);
		if(armor.hasTagCompound() && world.getTotalWorldTime() != armor.getTagCompound().getLong("time")) {
			boolean canFunction = canArmorFunction(player);
			//Base power draw to move air around the suit, this is less than pressure eq.
			//Some upgrades modify player capabilities
			EmbeddedInventory inv = loadEmbeddedInventory(armor);

			//Run special chest stuff from here to prevent tick-repetition problems. I hate that this is here, but it fixes other problems so....
			if (armor.getItem() instanceof ItemSpaceChest) {
				transferEnergy(player, -BASE_POWER_USAGE);

				//Decrement air from damage
				int airLossFromDamage = 0;
				for (ItemStack stack : player.getArmorInventoryList()) {
					airLossFromDamage += stack.getItemDamage();
				}
				airLossFromDamage = (int) Math.sqrt(airLossFromDamage / 4);
				((ItemSpaceChest)armor.getItem()).decrementAir(armor, airLossFromDamage);
			}

			//Variables needed for leg determinations in loop
			int heavyUpgrades = 0;
			int legs = 0;
			//Use this to determine speed penalty to apply
			for(int i = 0; i < inv.getSizeInventory(); i++ ) {
				ItemStack stack = inv.getStackInSlot(i);
				if(!stack.isEmpty()) {
					IArmorComponent component = (IArmorComponent)stack.getItem();
					if (component instanceof IArmorComponentHeavy) heavyUpgrades++;
					if (component instanceof ItemUpgrade && stack.getItemDamage() == ItemUpgrade.legUpgradeDamage) legs++;
					if (!(component instanceof ItemSolarWings) && transferEnergy(player, -component.getTickedPowerConsumption(stack, player)) == 0 && canFunction)
						component.onTick(world, player, armor, inv, stack);
					else if (component instanceof ItemSolarWings) {
						component.onTick(world, player, armor, inv, stack);
						transferEnergy(player, component.getTickedPowerConsumption(stack, player));
					}
					saveEmbeddedInventory(armor, inv);
					armor = player.getItemStackFromSlot(EntityLiving.getSlotForItemStack(armor));
				}
			}

			//Handle speed penalty - default with suit is half speed minus 1/5th speed per heavy upgrade, but bionic legs can absolutely override this, and should be used to do so
			if (armor.getItem() == AdvancedRocketryItems.itemSpaceSuit_Leggings || (!(player.getItemStackFromSlot(EntityEquipmentSlot.LEGS).getItem() instanceof ItemSpaceArmor) && armor.getItem() == AdvancedRocketryItems.itemSpaceSuit_Chest ) && player.world.isRemote)
				handleWalkSpeedChage(player, legs, heavyUpgrades, armor.getItem() == AdvancedRocketryItems.itemSpaceSuit_Leggings);

			saveEmbeddedInventory(armor, inv);
			//Handle saving stuff to make sure that we don't load things multiple times per tick
            armor.getTagCompound().setLong("time", world.getTotalWorldTime());
		} else if (!armor.hasTagCompound()){
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setLong("time", world.getTotalWorldTime());
			armor.setTagCompound(nbt);
		}
	}

	private void handleWalkSpeedChage(EntityPlayer player, int legs, int heavyUpgrades, boolean runChestCheck) {
		//Check the chest for this because otherwise we cause problems when both try to set stuff
		if(player.getItemStackFromSlot(EntityEquipmentSlot.CHEST).hasTagCompound() && runChestCheck) {
			EmbeddedInventory inv = loadEmbeddedInventory(player.getItemStackFromSlot(EntityEquipmentSlot.CHEST));
			//Use this to determine speed penalty to apply
			for(int i = 0; i < inv.getSizeInventory(); i++ ) {
				ItemStack stack = inv.getStackInSlot(i);
				if(!stack.isEmpty()) {
					IArmorComponent component = (IArmorComponent)stack.getItem();
					if (component instanceof IArmorComponentHeavy) heavyUpgrades++;
				}
			}
		}

		float walkSpeedBase = player.capabilities.getWalkSpeed();
		float walkSpeedModifier;
		float walkSpeedModifierSuit = walkSpeedBase/(!player.getItemStackFromSlot(EntityEquipmentSlot.CHEST).isEmpty() && !player.getItemStackFromSlot(EntityEquipmentSlot.LEGS).isEmpty() ? 2f : 4f);
		if(player.isSprinting()) {
			//If we have enough speed to boost, do so
			if (0 == transferEnergy(player, -60 * legs)) {
				walkSpeedModifier = legs * walkSpeedBase - (walkSpeedBase * (heavyUpgrades/5f));
			} else {
				walkSpeedModifier = -walkSpeedBase * (heavyUpgrades/5f);
			}
		} else {
			//The maximum possible effort the bionic legs can exert if we are to only hit base walk speed
			float maxEffect = Math.min(legs * walkSpeedBase, walkSpeedModifierSuit + (walkSpeedBase * (heavyUpgrades/5f)));
			//Consume just enough power to boost us to normal speed, or all the power needed to get us as close as possible if we can't get there
			if (transferEnergy(player, (int)(-60 * maxEffect/walkSpeedBase)) == 0) {
				walkSpeedModifier = maxEffect;
				if (player.motionZ == 0 && player.motionX == 0) transferEnergy(player, (int)(60 * maxEffect/walkSpeedBase));
			} else {
				walkSpeedModifier = -walkSpeedBase * (heavyUpgrades/5f);
			}
		}
		//Because of the suit itself
		walkSpeedModifier -= walkSpeedModifierSuit;
		//Make sure we don't go over bounds and make the player run backwards
        walkSpeedModifier = Math.max(walkSpeedModifier, -walkSpeedBase);

		//Actually handle application of the walk speed penalties/buffs
		if (player.getEntityAttribute(SPEED_ATTR).getModifier(WALK_SPEED_UUID) == null) {
			player.getEntityAttribute(SPEED_ATTR).applyModifier(new AttributeModifier(WALK_SPEED_UUID, SPEED_ATTR.getName(), walkSpeedModifier, 0));
		} else if (player.getEntityAttribute(SPEED_ATTR).getModifier(WALK_SPEED_UUID).getAmount() != walkSpeedModifier) {
			player.getEntityAttribute(SPEED_ATTR).removeModifier(player.getEntityAttribute(SPEED_ATTR).getModifier(WALK_SPEED_UUID));
			player.getEntityAttribute(SPEED_ATTR).applyModifier(new AttributeModifier(WALK_SPEED_UUID, SPEED_ATTR.getName(), walkSpeedModifier, 0));
		}
		//Handle jump penalties/buffs
		if (walkSpeedModifier <= -walkSpeedBase) {
			//This is slightly high to allow for jumps the standard way
			player.jumpMovementFactor = 0f;
		}
	}

	public static void removeWalkSpeedModifier(EntityPlayer player) {
		if (player.getEntityAttribute(SPEED_ATTR).getModifier(WALK_SPEED_UUID) != null) {
			player.getEntityAttribute(SPEED_ATTR).removeModifier(player.getEntityAttribute(SPEED_ATTR).getModifier(WALK_SPEED_UUID));
		}
	}

	@Override
	public String getArmorTexture(@Nonnull ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
		//This string ABSOLUTELY can be null and in fact is _passed_ null. IDEA is lying
		if(type != null && type.equals("overlay")) {
			return "advancedrocketry:textures/armor/spacesuit_layer2.png";
		}
		return "advancedrocketry:textures/armor/spacesuit_layer1.png";
	}

	//No extra armor to display
	@Override
	public int getArmorDisplay(EntityPlayer player, @Nonnull ItemStack armor, int slot) {
		return 0;
	}

	@Override
	public boolean hasOverlay(ItemStack stack) {
		return stack.getItem() instanceof ItemSpaceArmor;
	}

	@Override
	public void damageArmor(EntityLivingBase entity, @Nonnull ItemStack stack, DamageSource source, int damage, int slot) {
		if(stack.hasTagCompound()) {
			EmbeddedInventory inv = loadEmbeddedInventory(stack);
			for(int i = 0; i < inv.getSizeInventory(); i++ ) {
				ItemStack stack2 = inv.getStackInSlot(i);
				if(!stack2.isEmpty()) {
					IArmorComponent component = (IArmorComponent)stack2.getItem();
					component.onArmorDamaged(entity, stack, stack2, source, damage);
				}
			}

			saveEmbeddedInventory(stack, inv);
		}
       if (!doesArmorProtectFromDamageType(source, entity)) stack.damageItem(1, entity);
	}

    public static boolean doesArmorProtectFromDamageType(DamageSource source, EntityLivingBase player) {
		//Because apparently comparing damage types don't work, so I have to use strings
        return ItemSpaceArmor.canArmorFunction(player) && ((ItemSpaceArmor.protectedDamages.contains(source.damageType) && doesPlayerHaveEntireSuit(player)) || source.damageType.equals("hotFloor"));
    }

	public static boolean doesPlayerHaveEntireSuit(EntityLivingBase player) {
		for (ItemStack stack : player.getArmorInventoryList()) {
			if (!(stack.getItem() instanceof ItemSpaceArmor)) return false;
		}
		return true;
	}

    @Override
    public boolean handleUnblockableDamage(EntityLivingBase entity, @Nonnull ItemStack armor, DamageSource source, double damage, int slot) {
        return ItemSpaceArmor.doesArmorProtectFromDamageType(source, entity);
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
	public ItemStack removeComponent(World world, @Nonnull ItemStack armor, int index) {
		EmbeddedInventory inv = loadEmbeddedInventory(armor);
		ItemStack stack = inv.getStackInSlot(index);
		inv.setInventorySlotContents(index, ItemStack.EMPTY);

		if(!stack.isEmpty()) {
			IArmorComponent component = (IArmorComponent) stack.getItem();
			component.onComponentRemoved(world, stack);
			saveEmbeddedInventory(armor, inv);
		}



		return stack;
	}

	public List<ItemStack> getComponents(@Nonnull ItemStack armor) {
		List<ItemStack> list = new LinkedList<>();

		if(armor.hasTagCompound()) {
			EmbeddedInventory inv = loadEmbeddedInventory(armor);

			for(int i = 0; i < inv.getSizeInventory(); i++) {
				if(!inv.getStackInSlot(i).isEmpty())
					list.add(inv.getStackInSlot(i));
			}
		}

		return list;
	}

	@Override
	public boolean protectsFromSubstance(IAtmosphere atmosphere, @Nonnull ItemStack stack, Entity entity, boolean commitProtection) {
		return canArmorFunction((EntityLivingBase)entity) && (atmosphere == AtmosphereType.SUPERHIGHPRESSURE || atmosphere == AtmosphereType.HIGHPRESSURE || atmosphere == AtmosphereType.VACUUM || atmosphere == AtmosphereType.VERYHOT || atmosphere == AtmosphereType.SUPERHEATED || atmosphere == AtmosphereType.LOWOXYGEN || atmosphere == AtmosphereType.SUPERHIGHPRESSURENOO2 || atmosphere == AtmosphereType.HIGHPRESSURENOO2 || atmosphere == AtmosphereType.VERYHOTNOO2|| atmosphere == AtmosphereType.SUPERHEATEDNOO2  || atmosphere == AtmosphereType.NOO2);
	}


	@SideOnly(Side.CLIENT)
	public boolean nullifiesAtmosphereEffect(boolean fog, boolean brightness, @Nonnull ItemStack armor) {
		fog = !fog;
		brightness = !brightness;

		if(armor.hasTagCompound()) {
			EmbeddedInventory inv = loadEmbeddedInventory(armor);

			for(int i = 0; i < inv.getSizeInventory(); i++) {
				if(!inv.getStackInSlot(i).isEmpty()) {
					if (inv.getStackInSlot(i).getItem() == AdvancedRocketryItems.itemUpgrade) {
						if (inv.getStackInSlot(i).getItemDamage() == ItemUpgrade.fogUpgradeDamage) fog = true;
                        if (inv.getStackInSlot(i).getItemDamage() == ItemUpgrade.brightnessUpgradeDamage) brightness = true;
						if (fog && brightness) break;
					}
				}
			}
		}
		return fog && brightness && canArmorFunction(Minecraft.getMinecraft().player);
	}


	//Does the armor have any amount of power in it
	public static boolean canArmorFunction(EntityLivingBase entity) {
		return (!getFirstPowerStorageArmor(entity).isEmpty() && ((ItemPoweredSpaceArmor)getFirstPowerStorageArmor(entity).getItem()).armorPieceHasEnergy(getFirstPowerStorageArmor(entity)));
	}

	//Returns the energy (or negative energy) un-transferred to the storage of the pieces
	protected int transferEnergy(EntityLivingBase entity, int energyDelta) {
		int start = energyDelta;
		//Sorta inefficient but we will only ever check two pieces so it's easier than expandable system
		if (energyDelta != 0) {
			ItemStack stack = getFirstPowerStorageArmor(entity);
			if (!stack.isEmpty()) {
				energyDelta = ((ItemPoweredSpaceArmor) stack.getItem()).transferEnergy(stack, energyDelta);
				if (energyDelta != 0) {
					stack = entity.getItemStackFromSlot(getOppositePoweredSlot(EntityLiving.getSlotForItemStack(stack)));
					if (stack.getItem() instanceof ItemPoweredSpaceArmor) {
						energyDelta = ((ItemPoweredSpaceArmor) stack.getItem()).transferEnergy(stack, energyDelta);
					}
				}
			}
		}
		return energyDelta;
	}

	private EntityEquipmentSlot getOppositePoweredSlot(EntityEquipmentSlot slot) {
		return EntityEquipmentSlot.CHEST == slot ? EntityEquipmentSlot.LEGS : EntityEquipmentSlot.CHEST;
	}

	protected static ItemStack getFirstPowerStorageArmor(EntityLivingBase entity) {
		return entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem() instanceof ItemPoweredSpaceArmor ? entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST) : entity.getItemStackFromSlot(EntityEquipmentSlot.LEGS).getItem() instanceof ItemPoweredSpaceArmor ? entity.getItemStackFromSlot(EntityEquipmentSlot.LEGS) : ItemStack.EMPTY;
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

	@Override
	public boolean hasCapability(@Nullable Capability<?> capability, EnumFacing facing) {

		return capability == CapabilitySpaceArmor.PROTECTIVEARMOR;
	}

	@Override
	public <T> T getCapability(@Nullable Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilitySpaceArmor.PROTECTIVEARMOR)
			return (T) this;
		return null;
	}

	public boolean isItemValidForSlot(@Nonnull ItemStack stack, int slot) {
		return true;	
	}

	@Override
	public IconResource getResourceForSlot(int slot) {
		return null;
	}
}
