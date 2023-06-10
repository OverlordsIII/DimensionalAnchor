package io.github.overlordsiii.command;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import java.lang.reflect.Field;

import com.google.common.base.Throwables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.overlordsiii.DimensionalAnchor;
import io.github.overlordsiii.config.DimensionalAnchorConfig;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DimensionalAnchorCommand {

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(literal("dimensional-anchor")
			.then(literal("toggle")
				.then(literal("xpTpCosts")
					.executes(context -> executeToggle(context, "xpTpCosts", "Config option xpTpCosts was toggled %s")))
				.then(literal("xpConsequence")
					.executes(context -> executeToggle(context, "xpConsequence", "Config option xpConsequence was toggled %s")))
				.then(literal("allowTpToEnd")
					.executes(context -> executeToggle(context, "allowTpToEnd", "Config option allowTpToEnd was toggled %s"))))
			.then(literal("set")
				.then(literal("xpCostSameDimension")
					.then(argument("xpLevelCost", IntegerArgumentType.integer(0))
						.executes(context -> executeSet(context, "xpCostSameDimension", IntegerArgumentType.getInteger(context, "xpLevelCost"), "xpCostSameDimension has been set to %s"))))
				.then(literal("xpCostDifferentDimension")
					.then(argument("xpLevelCost", IntegerArgumentType.integer(0))
						.executes(context -> executeSet(context, "xpCostDifferentDimension", IntegerArgumentType.getInteger(context, "xpLevelCost"), "xpCostDifferentDimension has been set to %s"))))));
	}

	private static int executeSet(CommandContext<ServerCommandSource> ctx, String literal, int newValue, String displayText) {
		try {
			Field field = DimensionalAnchorConfig.class.getField(literal);
			field.setInt(DimensionalAnchor.CONFIG, newValue);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			logError(ctx, e);
			return -1;
		}

		String text = String.format(displayText, newValue);
		ctx.getSource().sendFeedback(
			() -> Text.literal(text)
				.formatted(Formatting.YELLOW)
				.styled(style -> style
					.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/dimensional-anchor toggle " + literal))
					.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Toggle the " + literal + " option")))), true);

		DimensionalAnchor.CONFIG_MANAGER.save();
		Text opText = Text.literal("The " + literal + " option has been set tp \"" + newValue + "\" by " + (ctx.getSource().getPlayer() == null ? "Server command terminal" : ctx.getSource().getPlayer().getName().getString())).formatted(Formatting.LIGHT_PURPLE);
		sendToOps(ctx, opText);
		return 1;
	}

	private static int executeToggle(CommandContext<ServerCommandSource> ctx, String literal, String displayText) {
		String onOrOff;
		try {
			Field field = DimensionalAnchorConfig.class.getField(literal);
			boolean newValue = !field.getBoolean(DimensionalAnchor.CONFIG);
			field.setBoolean(DimensionalAnchor.CONFIG, newValue);
			onOrOff = onOrOff(newValue);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			logError(ctx, e);
			return -1;
		}

		String text = String.format(displayText, onOrOff);
		ctx.getSource().sendFeedback(
			() -> Text.literal(text)
				.formatted(Formatting.YELLOW)
				.styled(style -> style
					.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/dimensional-anchor toggle " + literal))
					.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Toggle the " + literal + " option")))), true);

		DimensionalAnchor.CONFIG_MANAGER.save();
		Text opText = Text.literal("The " + literal + " option has been toggled \"" + onOrOff + "\" by " + (ctx.getSource().getPlayer() == null ? "Server command terminal" : ctx.getSource().getPlayer().getName().getString())).formatted(Formatting.LIGHT_PURPLE);
		sendToOps(ctx, opText);
		return 1;
	}

	private static String onOrOff(boolean bl) {
		return bl ? "on" : "off";
	}

	private static void logError(CommandContext<ServerCommandSource> ctx, Exception e) {
		if (ctx.getSource().getPlayer() != null) {
			ctx.getSource().getPlayer().sendMessage(Text.literal("Exception Thrown! Exception: " + Throwables.getRootCause(e)), false);
		}
		e.printStackTrace();
	}

	private static void sendToOps(CommandContext<ServerCommandSource> ctx, Text text){
		ctx.getSource().getServer().getPlayerManager().getPlayerList().forEach((serverPlayerEntity -> {
			if (ctx.getSource().getServer().getPlayerManager().isOperator(serverPlayerEntity.getGameProfile())){
				serverPlayerEntity.sendMessage(text, false);
			}
		}));
	}



}
