package com.bcombat.client;

import com.bcombat.combat.controller.CombatControllerManager;
import com.bcombat.combat.input.CombatInputHandler;
import com.bcombat.combat.input.CombatKeyBindings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class BannerlordCombatClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Registers the Combat Mode keybinding and the client-tick handler
		// that translates it into CombatController requests. See
		// com.bcombat.combat.controller.CombatController for the framework
		// entry point every future system should build against.
		CombatKeyBindings.register();
		CombatInputHandler.register();

		// Avoid leaking a stale controller for the local player across
		// disconnect/reconnect or server switches.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			if (client.player != null) {
				CombatControllerManager.remove(client.player);
			}
		});
	}
}
