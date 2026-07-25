package com.bcombat.client.animation;

import com.bcombat.combat.controller.CombatController;
import com.bcombat.combat.controller.CombatControllerManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;

import java.util.UUID;

/**
 * Drives every client-side {@link CombatController}'s per-tick
 * advancement (state-transition timers, blend weights, ...) so remote
 * players' and AI-controlled mobs' network-mirrored combat state
 * actually animates smoothly between {@code CombatSyncS2CPacket}
 * corrections — exactly the way {@code CombatInputHandler} already
 * drives the local player's own predictive controller every client
 * tick as part of translating input into requests.
 * <p>
 * Without this, {@code CombatControllerManager#get} still creates a
 * client-side mirror controller for a remote player or AI combatant the
 * instant their first {@code CombatSyncS2CPacket} arrives (see {@code
 * ClientCombatNetworking#applyCombatSync}), but that mirror's {@link
 * com.bcombat.combat.animation.AnimationController} never advances -
 * its blend weight sits frozen wherever the very first snapshot left it
 * - since nothing was ever calling {@code tick()} on it. This is the
 * single addition that closes that gap, driving {@link CombatPoseCache}
 * afterward so it always reflects this tick's freshly-advanced state.
 * <p>
 * The local player's own controller is deliberately skipped here -
 * {@code CombatInputHandler} already ticks it once per client tick, and
 * ticking it a second time would double-advance its wind-up/recovery/
 * blend timers.
 */
public final class CombatAnimationTicker {

    private CombatAnimationTicker() {
        // Static registrar, no instances.
    }

    /**
     * Registers this ticker against Fabric's end-of-client-tick event.
     * Must be called exactly once, from {@code
     * BannerlordCombatClient#onInitializeClient()}.
     */
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(CombatAnimationTicker::onEndClientTick);
    }

    private static void onEndClientTick(MinecraftClient client) {
        UUID localPlayerId = client.player != null ? client.player.getUuid() : null;

        for (CombatController controller : CombatControllerManager.clientControllers()) {
            LivingEntity entity = controller.getEntity();

            if (entity.isRemoved()) {
                // Prunes a mirror controller for a combatant that left
                // view/despawned/died, so it doesn't tick (or keep a
                // stale CombatPoseCache entry) forever.
                CombatControllerManager.removeClient(entity.getUuid());
                continue;
            }

            if (localPlayerId != null && entity.getUuid().equals(localPlayerId)) {
                continue;
            }

            controller.tick();
        }

        CombatPoseCache.onEndClientTick();
    }
}
