package com.bcombat.combat.animation;

/**
 * Every generic locomotion animation state supported in this phase, plus
 * the directional attack wind-up/release states, the directional block
 * states, generic recovery, and the Perfect Block / Parry / Chamber
 * defensive reaction states. These four are placeholders in this phase —
 * a future GeckoLib model layer wires them to real animation clips; a
 * dedicated feint animation is not declared here yet since the feint
 * system itself remains unimplemented.
 */
public enum AnimationState {
    IDLE,
    COMBAT_IDLE,
    WALK,
    COMBAT_WALK,
    RUN,
    COMBAT_RUN,
    SPRINT,
    COMBAT_SPRINT,
    JUMP,
    COMBAT_JUMP,
    ENTER_COMBAT,
    EXIT_COMBAT,

    WIND_UP_LEFT,
    WIND_UP_RIGHT,
    WIND_UP_OVERHEAD,
    WIND_UP_THRUST,

    RELEASE_LEFT,
    RELEASE_RIGHT,
    RELEASE_OVERHEAD,
    RELEASE_THRUST,

    RECOVERY,

    ENTER_BLOCK,
    BLOCK_IDLE,
    EXIT_BLOCK,

    GUARD_LEFT,
    GUARD_RIGHT,
    GUARD_UP,
    GUARD_THRUST,

    PERFECT_BLOCK,
    PARRY,
    CHAMBER_PREPARE,
    CHAMBER_SUCCESS,

    /**
     * Couch Lance sub-states, layered on top of whatever generic/combat
     * state {@link AnimationController} would otherwise resolve to —
     * see {@link AnimationController#resolveTargetState} for the
     * priority this takes over ordinary locomotion/attack states while
     * {@code CouchLanceController} reports anything other than {@code
     * INACTIVE}. Mirrors {@code CouchState}'s own flow but stays a
     * distinct enum for the same reason {@code CouchState} itself is
     * kept separate from {@code CombatState} — see that class's docs.
     */
    COUCH_PREPARE,
    COUCH_ACTIVE,
    COUCH_IMPACT,
    COUCH_RECOVERY,

    /**
     * Brief, purely client-side reactive pose layered over whatever the
     * combatant is otherwise doing when they take a hit that doesn't
     * otherwise trigger a defensive state (Perfect Block/Parry) — see
     * the client-only {@code HitReactionManager}. Not driven by {@link
     * AnimationController#resolveTargetState}; applied as a short-lived
     * overlay by the renderer directly.
     */
    HIT_REACT
}
