package com.example.client.command;

import com.example.client.music.MusicManager;
import com.example.client.music.MusicTrack;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

public final class MusicCommand {
	private static final MusicManager MUSIC_MANAGER = MusicManager.getInstance();

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
			ClientCommands.literal("music")
				.then(ClientCommands.literal("list").executes(MusicCommand::list))
				.then(ClientCommands.literal("play")
					.then(ClientCommands.argument("track", StringArgumentType.greedyString())
						.suggests(MusicCommand::suggestTracks)
						.executes(ctx -> play(ctx, StringArgumentType.getString(ctx, "track")))))
				.then(ClientCommands.literal("pause").executes(MusicCommand::pause))
				.then(ClientCommands.literal("resume").executes(MusicCommand::resume))
				.then(ClientCommands.literal("stop").executes(MusicCommand::stop))
				.then(ClientCommands.literal("next").executes(MusicCommand::next))
				.then(ClientCommands.literal("prev").executes(MusicCommand::prev))
				.then(ClientCommands.literal("volume")
					.then(ClientCommands.argument("value", IntegerArgumentType.integer(0, 100))
						.executes(ctx -> volume(ctx, IntegerArgumentType.getInteger(ctx, "value")))))
				.then(ClientCommands.literal("reload").executes(MusicCommand::reload))
				.then(ClientCommands.literal("now").executes(MusicCommand::now))));
	}

	private static int list(CommandContext<FabricClientCommandSource> context) {
		String result = MUSIC_MANAGER.listTracks();
		return sendMessage(context.getSource(), result) ? 1 : 0;
	}

	private static int play(CommandContext<FabricClientCommandSource> context, String track) {
		String result = MUSIC_MANAGER.play(track);
		return sendMessage(context.getSource(), result) ? 1 : 0;
	}

	private static int pause(CommandContext<FabricClientCommandSource> context) {
		String result = MUSIC_MANAGER.pause();
		return sendMessage(context.getSource(), result) ? 1 : 0;
	}

	private static int resume(CommandContext<FabricClientCommandSource> context) {
		String result = MUSIC_MANAGER.resume();
		return sendMessage(context.getSource(), result) ? 1 : 0;
	}

	private static int stop(CommandContext<FabricClientCommandSource> context) {
		String result = MUSIC_MANAGER.stop();
		return sendMessage(context.getSource(), result) ? 1 : 0;
	}

	private static int next(CommandContext<FabricClientCommandSource> context) {
		String result = MUSIC_MANAGER.next();
		return sendMessage(context.getSource(), result) ? 1 : 0;
	}

	private static int prev(CommandContext<FabricClientCommandSource> context) {
		String result = MUSIC_MANAGER.previous();
		return sendMessage(context.getSource(), result) ? 1 : 0;
	}

	private static int volume(CommandContext<FabricClientCommandSource> context, int value) {
		String result = MUSIC_MANAGER.setVolume(value);
		return sendMessage(context.getSource(), result) ? 1 : 0;
	}

	private static int reload(CommandContext<FabricClientCommandSource> context) {
		String result = MUSIC_MANAGER.reload();
		return sendMessage(context.getSource(), result) ? 1 : 0;
	}

	private static int now(CommandContext<FabricClientCommandSource> context) {
		String result = MUSIC_MANAGER.now();
		return sendMessage(context.getSource(), result) ? 1 : 0;
	}

	private static boolean sendMessage(FabricClientCommandSource source, String message) {
		for (String line : message.split("\n")) {
			source.sendFeedback(Component.literal(line));
		}
		return true;
	}

	private static CompletableFuture<Suggestions> suggestTracks(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
		for (MusicTrack track : MUSIC_MANAGER.getTracks()) {
			builder.suggest(track.getDisplayName());
		}
		return builder.buildFuture();
	}
}
