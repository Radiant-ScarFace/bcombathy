package com.bcombat.combat.animation;

/**
 * Every generic locomotion animation state supported in this phase, plus
 * the directional attack wind-up/release states, the directional block
 * states, and generic recovery. Feint and chamber animations are not
 * declared here yet — those belong to future systems and will extend
 * this enum when built.
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
    GUARD_THRUST
}