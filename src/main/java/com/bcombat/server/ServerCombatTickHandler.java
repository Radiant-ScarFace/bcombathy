package com.bcombat.server;

import com.bcombat.combat.ai.AICombatController;
import com.bcombat.combat.ai.AICombatManager;
import com.bcombat.combat.controller.CombatController;
import com.bcombat.combat.controller.CombatControllerManager;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;

/**
 * The single server-side per-tick driver for the entire combat
 * framework. Registers exactly once against {@link
 * ServerTickEvents#END_SERVER_TICK} and, every server tick, in order:
 * <ol>
 *     <li>Advances every AI-controlled combatant's decision-making via
 *     {@link AICombatManager#tickAll()} — the same relationship {@code
 *     CombatInputHandler} has with a human player: decisions are made
 *     first, purely by issuing {@code request*}/{@code update*} calls
 *     into {@link CombatController}'s public API, before the state
 *     machine itself advances.</li>
 *     <li>Advances every tracked server-side {@link CombatController} —
 *     one per connected player plus one per AI-enabled mob, all living
 *     in the exact same {@link CombatControllerManager#serverControllers()}
 *     collection — via {@link CombatController#tick()}.</li>
 *     <li>Prunes any AI controller whose entity is no longer alive or has
 *     been removed from the world — see {@link #pruneStaleAiControllers()}.
 *     This is what covers chunk-unload/despawn/forced-removal cleanup
 *     ({@link com.bcombat.server.ServerCombatLifecycleHandler}'s {@code
 *     AFTER_DEATH} hook already covers ordinary combat death) without
 *     depending on a dedicated entity-unload event.</li>
 * </ol>
 * There is no separate "AI tick loop": {@link AICombatManager} only ever
 * issues requests, it never advances a {@link CombatController} itself,
 * so an AI-driven mob's state machine is advanced by exactly the same
 * call, at exactly the same point in the tick, as a real player's — the
 * one place server authority for combat state lives.
 * <p>
 * Client-side ticking is deliberately untouched by this class: the local
 * player's predictive controller is ticked by {@code CombatInputHandler}
 * on {@code ClientTickEvents.END_CLIENT_TICK}, and remote mirrors are
 * ticked wherever {@code com.bcombat.network}'s client-side receiver
 * applies snapshots — neither of which this (server-only) class should
 * ever reach into.
 */
public final class ServerCombatTickHandler {

    private ServerCombatTickHandler() {
        // Static registrar, no instances.
    }

    /**
     * Registers this handler against Fabric's end-of-server-tick event.
     * Must be called exactly once, from {@code
     * BannerlordCombat#onInitialize()}.
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ServerCombatTickHandler::onEndServerTick);
    }

    private static void onEndServerTick(MinecraftServer server) {
        // AI decisions first (mirrors CombatInputHandler's "decide, then
        // tick" ordering), then every tracked controller - players and
        // AI-driven mobs alike - through the exact same call.
        AICombatManager.tickAll();

        for (CombatController controller : CombatControllerManager.serverControllers()) {
            controller.tick();
        }

        pruneStaleAiControllers();
    }

    /**
     * Removes the AI decision layer and underlying {@link
     * CombatController} for any AI-enabled {@link MobEntity} that is no
     * longer alive or has been removed from the world (chunk unload,
     * world change, forced removal, natural despawn) - every way an
     * AI-controlled mob can stop existing that {@code
     * ServerCombatLifecycleHandler}'s {@code AFTER_DEATH} hook, which
     * only fires for an ordinary combat death, would otherwise miss.
     * <p>
     * Safe to run every tick against {@link AICombatManager#controllers()}
     * while removing matched entries from it, since that collection is
     * backed by a {@code ConcurrentHashMap} whose iterators are weakly
     * consistent and never throw {@code ConcurrentModificationException}.
     */
    private static void pruneStaleAiControllers() {
        for (AICombatController ai : AICombatManager.controllers()) {
            MobEntity entity = ai.getEntity();
            if (!entity.isAlive() || entity.isRemoved()) {
                AICombatManager.disable(entity);
                CombatControllerManager.remove(entity);
            }
        }
    }
}

