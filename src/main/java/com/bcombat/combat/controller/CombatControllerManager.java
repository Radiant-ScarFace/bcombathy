package com.bcombat.combat.controller;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of {@link CombatController} instances keyed by player UUID.
 * Keeping this as an explicit registry (rather than e.g. attaching state
 * via mixin fields) means the framework doesn't need any mixins at all in
 * this phase, and a future networking layer can freely replace how
 * controllers are looked up (e.g. per-world) without touching any other
 * class.
 */
public final class CombatControllerManager {

    private static final Map<UUID, CombatController> CONTROLLERS = new ConcurrentHashMap<>();

    private CombatControllerManager() {
        // Static registry, no instances.
    }

    /**
     * Returns the existing controller for this player, creating one if
     * this is the first time the player has been seen.
     */
    public static CombatController get(PlayerEntity player) {
        return CONTROLLERS.computeIfAbsent(player.getUuid(), id -> new CombatController(player));
    }

    /**
     * Removes a player's controller, e.g. on disconnect, to avoid leaking
     * stale entries across sessions.
     */
    public static void remove(PlayerEntity player) {
        CONTROLLERS.remove(player.getUuid());
    }

    /**
     * Ticks every currently-tracked controller. Intended to be called once
     * per client tick for the local player in this phase; a future
     * networking phase would call this server-side for every player.
     */
    public static void tickAll() {
        CONTROLLERS.values().forEach(CombatController::tick);
    }
}
