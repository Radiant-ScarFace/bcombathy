package com.bcombat.combat.ai.group;

import com.bcombat.combat.ai.AICombatController;
import com.bcombat.combat.util.CombatConstants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of every {@link CombatSquad}, keyed by squad id — the group
 * combat analogue of {@code com.bcombat.combat.ai.AICombatManager},
 * which it sits directly alongside (never inside, and never duplicating
 * its per-entity registry). {@code AICombatManager} still owns every
 * {@link AICombatController} instance and still ticks every one of them
 * exactly as it always has; this class only adds an optional, purely
 * additive layer of shared awareness between members that opt into the
 * same squad id via {@code AICombatManager#enable(net.minecraft.entity.mob.MobEntity,
 * com.bcombat.combat.ai.AIDifficultyPreset, com.bcombat.combat.ai.CombatRole, String)}.
 * An {@link AICombatController} with a {@code null} squad id is never
 * tracked here and behaves exactly as the original solo AI Combat
 * Framework always has.
 */
public final class SquadManager {

    private static final Map<String, CombatSquad> SQUADS = new ConcurrentHashMap<>();

    private SquadManager() {
        // Static registry, no instances.
    }

    /**
     * Adds {@code member} to the squad named {@code squadId}, creating
     * that squad if it doesn't exist yet. A no-op (returns {@code null})
     * if {@code squadId} is {@code null} or blank — the "no squad, solo
     * behavior" case.
     */
    public static CombatSquad join(AICombatController member, String squadId) {
        if (squadId == null || squadId.isBlank()) {
            return null;
        }
        CombatSquad squad = SQUADS.computeIfAbsent(squadId, CombatSquad::new);
        squad.addMember(member);
        return squad;
    }

    /**
     * Removes {@code member} from the squad named {@code squadId}, and
     * removes the squad itself once it becomes empty. A no-op if {@code
     * squadId} is {@code null} or that squad no longer exists.
     */
    public static void leave(AICombatController member, String squadId) {
        if (squadId == null) {
            return;
        }
        CombatSquad squad = SQUADS.get(squadId);
        if (squad == null) {
            return;
        }
        squad.removeMember(member);
        if (squad.isEmpty()) {
            SQUADS.remove(squadId, squad);
        }
    }

    /** @return the squad named {@code squadId}, or {@code null} if it doesn't currently exist (including a {@code null} id). */
    public static CombatSquad get(String squadId) {
        return squadId == null ? null : SQUADS.get(squadId);
    }

    /** @return every currently tracked squad, for observability/debugging (e.g. {@code /bcombat ai list}). */
    public static Map<String, CombatSquad> squads() {
        return SQUADS;
    }

    /**
     * Advances every tracked squad's shared awareness by one tick.
     * Must be called once per server tick, before {@code
     * AICombatManager} ticks its individual {@link AICombatController}
     * instances, so each member acts on this tick's freshly computed
     * squad state rather than last tick's — see {@code
     * AICombatManager#tickAll()} for the call site and ordering.
     */
    public static void tickAll() {
        if (!CombatConstants.GROUP_AI_ENABLED) {
            return;
        }
        for (CombatSquad squad : SQUADS.values()) {
            squad.update();
        }
        SQUADS.values().removeIf(CombatSquad::isEmpty);
    }

    /** Clears every tracked squad. Intended for server shutdown/world unload, mirroring {@code AICombatManager#clear()}. */
    public static void clear() {
        SQUADS.clear();
    }
}