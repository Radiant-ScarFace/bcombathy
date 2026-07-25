package com.bcombat.combat.controller;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of {@link CombatController} instances keyed by player UUID.
 * <p>
 * Two independent registries are kept - {@code SERVER_CONTROLLERS} and
 * {@code CLIENT_CONTROLLERS} - rather than one, because a single JVM can
 * host both logical sides at once (integrated singleplayer server): the
 * server-side {@code ServerPlayerEntity} instance and the client-side
 * {@code ClientPlayerEntity}/{@code OtherClientPlayerEntity} instance for
 * the very same UUID are different objects with independent {@link
 * CombatController}s (one authoritative, one not) and must never share a
 * map entry. Which registry a given {@link PlayerEntity} belongs to is
 * derived automatically from {@code player.getWorld().isClient()}, so
 * every existing call site (which simply calls {@link #get(PlayerEntity)}
 * without needing to know which logical side it's running on) keeps
 * working unchanged.
 * <p>
 * On the server, every entry is authoritative (see {@link
 * CombatController#isAuthoritative()}) - one per currently-connected
 * player. On the client, every entry is non-authoritative: the local
 * player's entry is a predictive mirror corrected by {@link
 * CombatController#applySnapshot}/{@link CombatController#applyStaminaSnapshot},
 * and every other entry is a remote player's purely network-driven
 * mirror. See {@code com.bcombat.network}'s classes for how both
 * registries are driven and kept in sync over the wire.
 */
public final class CombatControllerManager {

    private static final Map<UUID, CombatController> SERVER_CONTROLLERS = new ConcurrentHashMap<>();
    private static final Map<UUID, CombatController> CLIENT_CONTROLLERS = new ConcurrentHashMap<>();

    private CombatControllerManager() {
        // Static registry, no instances.
    }

    /**
     * Returns the existing controller for this player, creating one if
     * this is the first time the player has been seen on this logical
     * side. Which registry (and therefore which {@link
     * CombatController#isAuthoritative()} value) backs the returned
     * controller is derived from {@code player.getWorld().isClient()}.
     */
    public static CombatController get(PlayerEntity player) {
        boolean serverSide = !player.getWorld().isClient();
        Map<UUID, CombatController> registry = serverSide ? SERVER_CONTROLLERS : CLIENT_CONTROLLERS;
        return registry.computeIfAbsent(player.getUuid(), id -> new CombatController(player, serverSide));
    }

    /**
     * Removes a player's controller from whichever registry matches
     * {@code player}'s logical side, e.g. on disconnect, to avoid leaking
     * stale entries across sessions.
     */
    public static void remove(PlayerEntity player) {
        boolean serverSide = !player.getWorld().isClient();
        (serverSide ? SERVER_CONTROLLERS : CLIENT_CONTROLLERS).remove(player.getUuid());
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