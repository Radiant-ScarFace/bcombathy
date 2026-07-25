package com.bcombat.combat.controller;

import net.minecraft.entity.LivingEntity;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of {@link CombatController} instances keyed by combatant UUID.
 * <p>
 * Widened to {@link LivingEntity} (rather than {@code PlayerEntity}) so
 * the exact same registry backs both real players and AI-controlled
 * mobs - see {@code com.bcombat.combat.ai.AICombatController}, which
 * calls {@link #get(LivingEntity)} for its mob exactly the way {@code
 * CombatInputHandler} does for the local player. There is no separate
 * "AI registry": an AI-controlled mob's {@link CombatController} lives
 * in {@code SERVER_CONTROLLERS} next to every player's, ticked by the
 * exact same server tick loop, resolved through the exact same {@link
 * #getIfPresent(LivingEntity)} lookup a swing's collision outcome
 * already uses to find a defender's controller.
 * <p>
 * Two independent registries are kept - {@code SERVER_CONTROLLERS} and
 * {@code CLIENT_CONTROLLERS} - rather than one, because a single JVM can
 * host both logical sides at once (integrated singleplayer server): the
 * server-side {@code ServerPlayerEntity} instance and the client-side
 * {@code ClientPlayerEntity}/{@code OtherClientPlayerEntity} instance for
 * the very same UUID are different objects with independent {@link
 * CombatController}s (one authoritative, one not) and must never share a
 * map entry. Which registry a given {@link LivingEntity} belongs to is
 * derived automatically from {@code entity.getWorld().isClient()}, so
 * every existing call site (which simply calls {@link #get(LivingEntity)}
 * without needing to know which logical side it's running on) keeps
 * working unchanged. AI-controlled mobs only ever exist server-side, so
 * they only ever populate {@code SERVER_CONTROLLERS}.
 * <p>
 * On the server, every entry is authoritative (see {@link
 * CombatController#isAuthoritative()}) - one per currently-connected
 * player plus one per currently AI-driven mob. On the client, every
 * entry is non-authoritative: the local player's entry is a predictive
 * mirror corrected by {@link CombatController#applySnapshot}/{@link
 * CombatController#applyStaminaSnapshot}, and every other entry is a
 * remote player's purely network-driven mirror. See {@code
 * com.bcombat.network}'s classes for how both registries are driven and
 * kept in sync over the wire.
 */
public final class CombatControllerManager {

    private static final Map<UUID, CombatController> SERVER_CONTROLLERS = new ConcurrentHashMap<>();
    private static final Map<UUID, CombatController> CLIENT_CONTROLLERS = new ConcurrentHashMap<>();

    private CombatControllerManager() {
        // Static registry, no instances.
    }

    /**
     * Returns the existing controller for this combatant, creating one
     * if this is the first time it has been seen on this logical side.
     * Which registry (and therefore which {@link
     * CombatController#isAuthoritative()} value) backs the returned
     * controller is derived from {@code entity.getWorld().isClient()}.
     * Works identically for a real player or an AI-controlled mob.
     */
    public static CombatController get(LivingEntity entity) {
        boolean serverSide = !entity.getWorld().isClient();
        Map<UUID, CombatController> registry = serverSide ? SERVER_CONTROLLERS : CLIENT_CONTROLLERS;
        return registry.computeIfAbsent(entity.getUuid(), id -> new CombatController(entity, serverSide));
    }

    /**
     * Returns the existing controller for this combatant if one is
     * already tracked, or {@code null} otherwise - never creates one.
     * Used by {@link CombatController#resolveCollisionOutcome} (via its
     * caller) to check whether a struck target has combat-framework
     * defenses (a player, or an AI-controlled mob driven by {@code
     * AICombatController}) without accidentally instantiating a
     * controller for a plain vanilla mob that will never tick one.
     */
    public static CombatController getIfPresent(LivingEntity entity) {
        boolean serverSide = !entity.getWorld().isClient();
        Map<UUID, CombatController> registry = serverSide ? SERVER_CONTROLLERS : CLIENT_CONTROLLERS;
        return registry.get(entity.getUuid());
    }

    /**
     * Removes a combatant's controller from whichever registry matches
     * its logical side, e.g. on disconnect or mob death, to avoid
     * leaking stale entries across sessions.
     */
    public static void remove(LivingEntity entity) {
        boolean serverSide = !entity.getWorld().isClient();
        (serverSide ? SERVER_CONTROLLERS : CLIENT_CONTROLLERS).remove(entity.getUuid());
    }

    /** Removes a specific player's server-side authoritative controller by UUID, e.g. on disconnect. */
    public static void removeServer(UUID playerId) {
        SERVER_CONTROLLERS.remove(playerId);
    }

    /** Removes a specific player's client-side controller by UUID. */
    public static void removeClient(UUID playerId) {
        CLIENT_CONTROLLERS.remove(playerId);
    }

    /**
     * Clears every client-side controller (local prediction + every
     * remote mirror). Called on disconnect/server-switch so no stale
     * mirror survives into the next session.
     */
    public static void clearClient() {
        CLIENT_CONTROLLERS.clear();
    }

    /** Clears every server-side controller. Intended for server shutdown/world unload. */
    public static void clearServer() {
        SERVER_CONTROLLERS.clear();
    }

    /** @return every currently-tracked server-side authoritative controller. */
    public static Collection<CombatController> serverControllers() {
        return SERVER_CONTROLLERS.values();
    }

    /** @return every currently-tracked client-side controller (local prediction + remote mirrors). */
    public static Collection<CombatController> clientControllers() {
        return CLIENT_CONTROLLERS.values();
    }

    /** @return the client-side UUID set currently tracked, for pruning players who've left view/the world. */
    public static java.util.Set<UUID> clientTrackedIds() {
        return CLIENT_CONTROLLERS.keySet();
    }

    /**
     * Ticks every currently-tracked controller in both registries. Kept
     * for compatibility/convenience; prefer ticking each registry
     * explicitly (server tick loop ticks {@link #serverControllers()},
     * client tick loop ticks {@link #clientControllers()}) when the two
     * need different cadences or pruning behavior.
     */
    public static void tickAll() {
        SERVER_CONTROLLERS.values().forEach(CombatController::tick);
        CLIENT_CONTROLLERS.values().forEach(CombatController::tick);
    }
}