package com.example.client;

import com.example.client.command.MusicCommand;
import net.fabricmc.api.ClientModInitializer;

public class ExampleModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MusicCommand.register();
	}
}