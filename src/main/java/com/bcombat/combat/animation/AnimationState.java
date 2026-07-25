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
    CHAMBER_SUCCESS
}