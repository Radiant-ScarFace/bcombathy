package com.bcombat.client;

import com.bcombat.client.feedback.CombatFeedbackManager;
import com.bcombat.client.hud.DirectionalCombatIndicatorRenderer;
import com.bcombat.client.network.ClientCombatNetworking;
import com.bcombat.combat.controller.CombatControllerManager;
import com.bcombat.combat.input.CombatInputHandler;
import com.bcombat.combat.input.CombatKeyBindings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class BannerlordCombatClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Registers the C2S sender helpers' matching S2C receivers -
		// CombatSyncS2CPacket/StaminaSyncS2CPacket - and applies each
		// incoming snapshot to the matching client-side CombatController
		// (local-prediction reconciliation for the local player, pure
		// mirroring for every remote player). Without this, CombatInputHandler's
		// ClientCombatNetworking.send*() calls still reach the server fine,
		// but nothing ever comes back: the local player's predictive state
		// never reconciles against the server's authoritative outcome, and
		// remote players never visibly enter/exit combat, attack, or block
		// on this client. See ClientCombatNetworking's class docs.
		ClientCombatNetworking.register();

		// Registers the Combat Mode keybinding and the client-tick handler
		// that translates it into CombatController requests. See
		// com.bcombat.combat.controller.CombatController for the framework
		// entry point every future system should build against.
		CombatKeyBindings.register();
		CombatInputHandler.register();

		// Wires every Combat Effects & Feedback Framework subsystem
		// (hit stop, camera shake, sound, particles, weapon trails) to
		// CombatEvents. See CombatFeedbackManager's class docs.
		CombatFeedbackManager.register();

		// Renders the lightweight Bannerlord-inspired directional combat
		// indicator (attack/guard direction wedges around the crosshair),
		// driven entirely by the local player's existing CombatController
		// state - see DirectionalCombatIndicatorRenderer's class docs.
		HudRenderCallback.EVENT.register(DirectionalCombatIndicatorRenderer::render);

		// Avoid leaking a stale controller for the local player across
		// disconnect/reconnect or server switches.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			if (client.player != null) {
				CombatControllerManager.remove(client.player);
			}
		});
	}
}