package com.bcombat.combat.util;

/**
 * Central location for every tunable value used by the combat framework.
 * <p>
 * No class outside this package/framework should hardcode timing, speed,
 * or blend values directly — everything lives here so balance passes and
 * future tuning never require hunting through unrelated classes.
 */
public final class CombatConstants {

    private CombatConstants() {
        // Utility class, no instances.
    }

    // ------------------------------------------------------------------
    // Movement
    // ------------------------------------------------------------------

    /**
     * Multiplier applied to walking speed while in Combat Mode.
     * Expressed as an ADD_MULTIPLIED_TOTAL attribute modifier operand,
     * e.g. -0.15 means "15% slower than base".
     */
    public static final double COMBAT_WALK_SPEED_MODIFIER = -0.15;

    /**
     * Additional multiplier stacked on top of {@link #COMBAT_WALK_SPEED_MODIFIER}
     * only while the player is sprinting in Combat Mode.
     */
    public static final double COMBAT_SPRINT_SPEED_MODIFIER = -0.20;

    /**
     * Additional multiplier stacked on top of {@link #COMBAT_WALK_SPEED_MODIFIER}
     * only while the player is in {@code CombatState.PREPARING_ATTACK} (winding
     * up a strike). Represents the player committing weight/focus to the swing
     * rather than being fully mobile. Applied and removed by
     * {@link com.bcombat.combat.movement.MovementModifierManager}; future
     * weapon classes are expected to scale this via a modifier rather than
     * replace it outright, same convention as {@link #DEFAULT_RECOVERY_DURATION_MODIFIER}.
     */
    public static final double WIND_UP_SPEED_MODIFIER = -0.10;

    // ------------------------------------------------------------------
    // State transition timing (in ticks, 20 ticks = 1 second)
    // ------------------------------------------------------------------

    /** Ticks spent in ENTERING_COMBAT before the state machine reaches COMBAT_IDLE. */
    public static final int ENTER_COMBAT_TRANSITION_TICKS = 6;

    /** Ticks spent in EXITING_COMBAT before the state machine returns to NORMAL. */
    public static final int EXIT_COMBAT_TRANSITION_TICKS = 6;

    // ------------------------------------------------------------------
    // Animation blending
    // ------------------------------------------------------------------

    /** Number of ticks a blend between two animation states takes to complete. */
    public static final int ANIMATION_BLEND_DURATION_TICKS = 5;

    // ------------------------------------------------------------------
    // Animation state selection thresholds (horizontal speed, blocks/tick)
    // ------------------------------------------------------------------

    /** Minimum horizontal speed for the WALK/COMBAT_WALK animation state to be selected over IDLE. */
    public static final double WALK_ANIMATION_SPEED_THRESHOLD = 0.02;

    /** Minimum horizontal speed for the RUN/COMBAT_RUN animation state to be selected over WALK. */
    public static final double RUN_ANIMATION_SPEED_THRESHOLD = 0.13;

    // ------------------------------------------------------------------
    // Attack: preparation (wind-up), release, and recovery timing
    // ------------------------------------------------------------------

    /**
     * Minimum ticks the player must remain in {@code PREPARING_ATTACK}
     * before a release request is honored. Releases requested earlier are
     * buffered and applied the instant this elapses, rather than dropped,
     * so a very fast click still produces an attack.
     */
    public static final int MIN_ATTACK_PREPARATION_TICKS = 4;

    /**
     * Ticks spent in {@code ATTACKING} before transitioning to
     * {@code RECOVERY}. Animation-only in this phase; no damage or hit
     * window is derived from this value yet.
     */
    public static final int ATTACK_RELEASE_DURATION_TICKS = 8;

    /**
     * Base ticks spent in {@code RECOVERY} before returning to
     * {@code COMBAT_IDLE}. Future weapon classes are expected to scale
     * this via a multiplier rather than replace it outright.
     */
    public static final int RECOVERY_DURATION_TICKS = 10;

    /**
     * Default multiplier applied to {@link #RECOVERY_DURATION_TICKS}.
     * Reserved for future weapon classes (e.g. heavier weapons recover
     * slower). Unused until weapons exist; kept at 1.0 (no change).
     */
    public static final double DEFAULT_RECOVERY_DURATION_MODIFIER = 1.0;

    /**
     * Minimum combined look-direction deviation (in degrees, yaw or
     * pitch) since wind-up began before a direction other than
     * {@code AttackDirection.NONE} is committed. Prevents tiny mouse
     * jitter from flipping the attack direction.
     */
    public static final float ATTACK_DIRECTION_DEADZONE_DEGREES = 6.0f;

    /**
     * Reserved for future refinement of direction detection (e.g.
     * distinguishing a deliberate flick from a slow drift using
     * per-tick mouse delta rather than only cumulative deviation).
     * Not yet consumed by {@code AttackDirectionTracker}.
     */
    public static final float ATTACK_MOUSE_SENSITIVITY_THRESHOLD = 2.0f;
}
