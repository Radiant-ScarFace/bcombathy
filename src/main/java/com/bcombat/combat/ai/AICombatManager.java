package com.bcombat.combat.ai;

import com.bcombat.combat.controller.CombatControllerManager;
import net.minecraft.entity.mob.MobEntity;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of {@link AICombatController} instances, keyed by entity
 * UUID - the AI decision-layer analogue of {@link
 * CombatControllerManager}, which it sits directly on top of and never
 * duplicates. Enabling AI combat for a {@link MobEntity} never creates a
 * second combat system: it only ever adds an {@link AICombatController}
 * here, which in turn drives the exact same {@code CombatController}
 * {@link CombatControllerManager#get} would hand back to any other
 * caller for that same entity.
 * <p>
 * Server-only by construction - {@link AICombatController} reads {@code
 * MobEntity#getTarget()} and drives vanilla navigation/look control,
 * neither of which are meaningful (or safe) to run on the logical
 * client - so unlike {@link CombatControllerManager}, this registry has
 * no client-side counterpart.
 */
public final class AICombatManager {

    private static final Map<UUID, AICombatController> CONTROLLERS = new ConcurrentHashMap<>();

    private AICombatManager() {
        // Static registry, no instances.
    }

    /**
     * Enables AI-driven combat for {@code entity} at the given {@link
     * AIDifficultyPreset}, replacing any existing AI controller already
     * tracked for it. This is the single entry point that turns a plain
     * {@link MobEntity} into a combatant that fights through the combat
     * framework - see {@link AICombatController}'s class docs for
     * exactly what that does and does not mean.
     */
    public static AICombatController enable(MobEntity entity, AIDifficultyPreset difficulty) {
        AICombatController controller = new AICombatController(entity, difficulty);
        CONTROLLERS.put(entity.getUuid(), controller);
        return controller;
    }

    /** @return the existing AI controller for {@code entity}, or {@code null} if it isn't AI-combat-enabled. */
    public static AICombatController getIfPresent(MobEntity entity) {
        return CONTROLLERS.get(entity.getUuid());
    }

    /** @return true if {@code entity} is currently AI-combat-enabled. */
    public static boolean isEnabled(MobEntity entity) {
        return CONTROLLERS.containsKey(entity.getUuid());
    }

    /**
     * Disables AI-driven combat for {@code entity}. Does not force the
     * entity out of Combat Mode or otherwise touch its {@code
     * CombatController} - it simply stops being driven, the same way a
     * player alt-tabbing stops sending input without their combat state
     * being reset out from under them. A future caller that also wants
     * an immediate return to {@code NORMAL} should call {@code
     * CombatControllerManager.get(entity).requestExitCombat()} itself.
     */
    public static void disable(MobEntity entity) {
        CONTROLLERS.remove(entity.getUuid());
    }

    /** @return every currently AI-combat-enabled controller, for the server tick loop to drive. */
    public static Collection<AICombatController> controllers() {
        return CONTROLLERS.values();
    }

    /** Clears every tracked AI controller. Intended for server shutdown/world unload. */
    public static void clear() {
        CONTROLLERS.clear();
    }

    /** Ticks every currently AI-combat-enabled controller. Called once per server tick. */
    public static void tickAll() {
        CONTROLLERS.values().forEach(AICombatController::tick);
    }
}