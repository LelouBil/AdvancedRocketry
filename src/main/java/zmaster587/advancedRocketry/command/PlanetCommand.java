package zmaster587.advancedRocketry.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import zmaster587.advancedRocketry.api.AdvancedRocketryItems;
import zmaster587.advancedRocketry.api.Constants;
import zmaster587.advancedRocketry.api.DataStorage.DataType;
import zmaster587.advancedRocketry.api.body.solar.StellarBody;
import zmaster587.advancedRocketry.api.body.station.IStation;
import zmaster587.advancedRocketry.api.body.PlanetManager;
import zmaster587.advancedRocketry.item.ItemDataChip;
import zmaster587.advancedRocketry.item.ItemMultiData;
import zmaster587.advancedRocketry.item.ItemStationChip;
import zmaster587.advancedRocketry.api.body.SpaceObjectManager;
import zmaster587.libVulpes.util.HashedBlockPosition;
import zmaster587.libVulpes.util.ZUtils;

import javax.annotation.Nullable;
import java.util.Locale;

public class PlanetCommand {

	public static void register(CommandDispatcher<CommandSource> dispatcher) {
		

		dispatcher.register(Commands.literal("advancedrocketry").then(Commands.literal("planet")
				.executes((value) -> commandPlanetHelp(value.getSource()))
				.then(Commands.literal("list").executes((value) -> commandPlanetList(value.getSource())))
				.then(Commands.literal("help").executes((value) -> commandPlanetHelp(value.getSource()))))
				.then(Commands.literal("goto").then(Commands.argument("dim", DimensionArgument.getDimension()).executes((value -> commandGoto(value.getSource(), DimensionArgument.getDimensionArgument(value, "dim")))))
				.then(Commands.literal("station").then(Commands.argument("stationid", IntegerArgumentType.integer(1)).executes((value) -> commandGotoStation(value.getSource(), IntegerArgumentType.getInteger(value, "stationid"))))))
				// givestation ID
				.then(Commands.literal("givestation").then(Commands.argument("stationid", StringArgumentType.string()).executes((value) -> commandGiveStation(value.getSource(), null, StringArgumentType.getString(value, "stationid")))
				// givestation ID player
				.then(Commands.argument("player", EntityArgument.player()).executes((value) -> commandGiveStation(value.getSource(), EntityArgument.getPlayer(value, "player"), StringArgumentType.getString(value, "stationid"))))))
				// filldata datatype
				.then(Commands.literal("filldata").then( Commands.argument("datatype", StringArgumentType.word()).executes( (value) -> commandFillData(value.getSource(), StringArgumentType.getString(value, "datatype"), -1))
				// filldata datatype amount
				.then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes( (value) -> commandFillData(value.getSource(), StringArgumentType.getString(value, "datatype"), IntegerArgumentType.getInteger(value, "amount"))) )  ))
				// star stuff, good star lord there's a lot here, and more to come
				.then(Commands.literal("star").then(Commands.literal("list").executes((value) -> commandListStars(value.getSource())) ))
				);
	}
	
	private static int commandListStars(CommandSource sender) {
		for(ResourceLocation stars : PlanetManager.getInstance().getStarIDs()) {
			StellarBody star = PlanetManager.getInstance().getStar(stars);
			sender.sendFeedback(new StringTextComponent(String.format("Star ID: %s Name: %s  Num Planets: %d", star.getStellarPosition().id, star.getName(), star.getPlanets().size())), false);
		}
	
		return 0;
	}
	
	private static int commandGoto(CommandSource sender, ServerWorld world) {
		ServerPlayerEntity player;
		if(sender.getEntity() != null && (player = (ServerPlayerEntity) sender.getEntity()) != null && sender.hasPermissionLevel(3)) {
			player.teleport(world, player.getPosX(), player.getPosY(), player.getPosZ(), 0, 0);
		}					
		else 
			sender.sendFeedback(new StringTextComponent("Must be a player with permission level three to use this command"), true);
		
		return 0;
	}
	
	private static int commandGotoStation(CommandSource sender, int stationidStr) {
		PlayerEntity player;
		ResourceLocation stationid = new ResourceLocation(SpaceObjectManager.STATION_NAMESPACE, String.valueOf(stationidStr));
		ServerWorld world = ZUtils.getWorld(PlanetManager.spaceId);
		if(sender.getEntity() != null && (player = (PlayerEntity) sender.getEntity()) != null && sender.hasPermissionLevel(3)) {
			IStation object = SpaceObjectManager.getSpaceManager().getSpaceStation(stationid);

			if(object != null) {
				HashedBlockPosition vec = object.getSpawnLocation();
				if(!PlanetManager.spaceId.equals(ZUtils.getDimensionIdentifier(player.world)))
					((ServerPlayerEntity) player).teleport(world, vec.x, vec.y, vec.z, 0, 0);
				
				player.setPositionAndUpdate(vec.x, vec.y, vec.z);
			}
			else {
				sender.sendFeedback(new StringTextComponent("Station " + stationid + " does not exist!"), true);
			}
		}					
		else 
			sender.sendFeedback(new StringTextComponent("Must be a player with permission level three to use this command"), true);
		
		return 0;
	}
	
	private static int commandFillData(CommandSource sender, String datatypeStr, int amountFill) throws CommandSyntaxException {
		
		ItemStack stack;
		if(sender.getEntity() != null && sender.hasPermissionLevel(3)) {
			stack = sender.asPlayer().getHeldItem(Hand.MAIN_HAND);

			if(!stack.isEmpty() && stack.getItem() instanceof ItemDataChip) {
				ItemDataChip item = (ItemDataChip) stack.getItem();
				int dataAmount = item.getMaxData(stack);
				DataType datatype = null;

				if(datatypeStr != null) {
					try {
						datatype = DataType.valueOf(datatypeStr.toUpperCase(Locale.ENGLISH));
					} catch (IllegalArgumentException e) {
						sender.sendFeedback(new StringTextComponent("Not a valid datatype"), false);
						StringBuilder value = new StringBuilder();
						for(DataType data : DataType.values())
							if(!data.name().equals("UNDEFINED"))
								value.append(data.name().toLowerCase()).append(", ");

						sender.sendFeedback(new StringTextComponent("Try " + value), false);

						return -1;
					}
				}
				if(amountFill >= -1)
					dataAmount = amountFill;

				if(datatype != null)
					item.setData(stack, dataAmount, datatype);
				else
				{
					for(DataType type : DataType.values())
						item.setData(stack, dataAmount, type);
				}
				sender.sendFeedback(new StringTextComponent("Data filled!"), false);
			}
			else if(stack.isEmpty() && stack.getItem() instanceof ItemMultiData) {
				ItemMultiData item = (ItemMultiData) stack.getItem();
				int dataAmount = item.getMaxData(stack);
				DataType datatype = null;

				if(datatypeStr != null) {
					try {
						datatype = DataType.valueOf(datatypeStr.toUpperCase(Locale.ENGLISH));
					} catch (IllegalArgumentException e) {
						sender.sendFeedback(new StringTextComponent("Not a valid datatype"), false);
						String value = "";
						for(DataType data : DataType.values())
							if(!data.name().equals("UNDEFINED"))
								value += data.name().toLowerCase() + ", ";

						sender.sendFeedback(new StringTextComponent("Try " + value),false);
						return -1;
					}
				}
				if(amountFill >= 0)
					dataAmount = amountFill;

				if(datatype != null)
					item.setData(stack, dataAmount, datatype);
				else
				{
					for(DataType type : DataType.values())
						item.setData(stack, dataAmount, type);
				}

				sender.sendFeedback(new StringTextComponent("Data filled!"), false);
			}
			else
				sender.sendFeedback(new StringTextComponent("Not Holding data item"),false);
		}
		else
			sender.sendFeedback(new StringTextComponent("Must be a player with permission level three to use this command"),false);
		
		return 0;
	}
	
	private static int commandGiveStation(CommandSource sender, @Nullable PlayerEntity player, String stationidStr) {
		ResourceLocation stationid = new ResourceLocation(Constants.modId, stationidStr);
		if(player == null && sender.getEntity() != null && sender.hasPermissionLevel(3)) {
			try {
				player = sender.asPlayer();
			} catch (CommandSyntaxException e) {
				e.printStackTrace();
				return -1;
			}

			ItemStack stack = new ItemStack(AdvancedRocketryItems.itemSpaceStationChip);
			ItemStationChip.setUUID(stack, stationid);
			player.inventory.addItemStackToInventory(stack);
		} else
			sender.sendFeedback(new StringTextComponent("Must be a player with permission level three to use this command"),false);
		
		return 0;
	}

	private static int commandPlanetList(CommandSource sender) {
		sender.sendFeedback(new StringTextComponent("Dimensions:"), false);
		for(ResourceLocation i : PlanetManager.getInstance().getPlanetIDs()) {
			sender.sendFeedback(new StringTextComponent("DIM" + i + ":  " + PlanetManager.getInstance().getPlanetProperties(i).getName()), false);
		}
		
		return 0;
	}

	private static int commandPlanetHelp(CommandSource sender) {
		sender.sendFeedback(new StringTextComponent("Planet:"), false);
		sender.sendFeedback(new StringTextComponent("planet list"), false);
		
		return 0;
	}

}
