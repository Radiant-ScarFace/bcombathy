package com.bcombat.combat.ai;

import com.bcombat.combat.ai.group.SquadManager;
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
 * Also owns this AI's optional {@link SquadManager} membership: enabling
 * an entity with a {@link CombatRole}/squad id joins the corresponding
 * {@code CombatSquad}, disabling (or re-enabling at a new role/squad)
 * leaves whatever squad it was previously in, and {@link #clear()} wipes
 * {@link SquadManager}'s registry alongside this one.
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
     * tracked for it. Equivalent to {@link #enable(MobEntity,
     * AIDifficultyPreset, CombatRole, String)} with a {@code null} role
     * and squad id - this entity is never tracked by {@link
     * SquadManager} and behaves exactly as the original solo AI Combat
     * Framework always has.
     */
    public static AICombatController enable(MobEntity entity, AIDifficultyPreset difficulty) {
        return enable(entity, difficulty, null, null);
    }

    /**
     * Group Combat Framework overload: enables AI-driven combat for
     * {@code entity} at the given {@link AIDifficultyPreset}, additionally
     * opting it into the given {@link CombatRole} within the squad named
     * {@code squadId}. Replacing an already-enabled entity first leaves
     * whichever squad its previous controller belonged to (if any),
     * before the new controller joins {@code squadId} (if given) - so an
     * entity can never be left tracked by two squads, or a stale one, at
     * once. A {@code null} or blank {@code squadId} behaves exactly like
     * {@link #enable(MobEntity, AIDifficultyPreset)} - no squad is
     * joined. If {@code squadId} is given but {@code role} is {@code
     * null}, defaults to {@link CombatRole#AGGRESSOR}, since every squad
     * member needs a role for {@code CombatSquad}'s flank/surround slot
     * assignment to work.
     */
    public static AICombatController enable(MobEntity entity, AIDifficultyPreset difficulty,
                                            CombatRole role, String squadId) {
        AICombatController existing = CONTROLLERS.get(entity.getUuid());
        if (existing != null && existing.getSquadId() != null) {
            SquadManager.leave(existing, existing.getSquadId());
        }

        boolean wantsSquad = squadId != null && !squadId.isBlank();
        CombatRole effectiveRole = wantsSquad && role == null ? CombatRole.AGGRESSOR : role;

        AICombatController controller = new AICombatController(entity, difficulty, effectiveRole, squadId);
        CONTROLLERS.put(entity.getUuid(), controller);

        if (controller.getSquadId() != null) {
            SquadManager.join(controller, controller.getSquadId());
        }
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
     * being reset out from under them. If this entity's controller
     * belonged to a squad, leaves that squad (see {@link
     * SquadManager#leave}) so a disabled entity is never left counted
     * toward a squad's shared awareness/spacing/regroup.
     */
    public static void disable(MobEntity entity) {
        AICombatController existing = CONTROLLERS.remove(entity.getUuid());
        if (existing != null && existing.getSquadId() != null) {
            SquadManager.leave(existing, existing.getSquadId());
        }
    }

    /** @return every currently AI-combat-enabled controller, for the server tick loop to drive. */
    public static Collection<AICombatController> controllers() {
        return CONTROLLERS.values();
    }

    /**
     * Clears every tracked AI controller, and every tracked {@link
     * SquadManager} squad alongside it. Intended for server shutdown/
     * world unload.
     */
    public static void clear() {
        CONTROLLERS.clear();
        SquadManager.clear();
    }

    /**
     * Ticks every currently AI-combat-enabled controller. Called once
     * per server tick. Ticks {@link SquadManager} first, so every
     * squad's shared awareness (focus target, regroup point, rotation
     * offset) reflects this tick's freshest state before any individual
     * {@link AICombatController#tick()} acts on it.
     */
    public static void tickAll() {
        SquadManager.tickAll();
        CONTROLLERS.values().forEach(AICombatController::tick);
    }
}