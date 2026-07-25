package com.bcombat.combat.util;

/**
 * Central location for every tunable value used by the combat framework.
 * <p>
 * No class outside this package/framework should hardcode timing, speed,
 * or blend values directly — everything lives here so balance passes and
 * future tuning never require hunting through unrelated classes.
 * <p>
 * Fields whose live value can meaningfully be rebalanced without touching
 * combat logic (timing windows, speed modifiers, blend durations, stamina
 * costs) are intentionally {@code public static} rather than {@code
 * public static final} — {@code com.bcombat.config.BCombatConfig} loads
 * {@code config/bcombat.json} on startup and overwrites these fields with
 * whatever a server operator has configured, before any combat code runs.
 * Every call site keeps reading {@code CombatConstants.FIELD} exactly as
 * before; only the field's mutability changed, not how it's consumed.
 * Values that are still genuinely reserved/unused placeholders for a
 * future phase (documented as such below) are left {@code final}.
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
    public static double COMBAT_WALK_SPEED_MODIFIER = -0.15;

    /**
     * Additional multiplier stacked on top of {@link #COMBAT_WALK_SPEED_MODIFIER}
     * only while the player is sprinting in Combat Mode.
     */
    public static double COMBAT_SPRINT_SPEED_MODIFIER = -0.20;

    /**
     * Additional multiplier stacked on top of {@link #COMBAT_WALK_SPEED_MODIFIER}
     * only while the player is in {@code CombatState.PREPARING_ATTACK} (winding
     * up a strike). Represents the player committing weight/focus to the swing
     * rather than being fully mobile. Applied and removed by
     * {@link com.bcombat.combat.movement.MovementModifierManager}; future
     * weapon classes are expected to scale this via a modifier rather than
     * replace it outright, same convention as {@link #DEFAULT_RECOVERY_DURATION_MODIFIER}.
     */
    public static double WIND_UP_SPEED_MODIFIER = -0.10;

    /**
     * Additional multiplier stacked on top of {@link #COMBAT_WALK_SPEED_MODIFIER}
     * only while the player is in {@code CombatState.RECOVERY} — the brief
     * follow-through after a swing before the player is fully reset to a
     * ready stance. Deliberately lighter than {@link #WIND_UP_SPEED_MODIFIER}
     * (recovering from a committed swing is less restrictive than actively
     * winding one up), but present so recovery reads as a genuine momentary
     * commitment rather than a movement-free window. Applied and removed by
     * {@link com.bcombat.combat.movement.MovementModifierManager}.
     */
    public static double RECOVERY_SPEED_MODIFIER = -0.08;

    // ------------------------------------------------------------------
    // State transition timing (in ticks, 20 ticks = 1 second)
    // ------------------------------------------------------------------

    /** Ticks spent in ENTERING_COMBAT before the state machine reaches COMBAT_IDLE. */
    public static int ENTER_COMBAT_TRANSITION_TICKS = 6;

    /** Ticks spent in EXITING_COMBAT before the state machine returns to NORMAL. */
    public static int EXIT_COMBAT_TRANSITION_TICKS = 6;

    // ------------------------------------------------------------------
    // Animation blending
    // ------------------------------------------------------------------

    /** Number of ticks a blend between two animation states takes to complete. */
    public static int ANIMATION_BLEND_DURATION_TICKS = 5;

    // ------------------------------------------------------------------
    // Animation state selection thresholds (horizontal speed, blocks/tick)
    // ------------------------------------------------------------------

    /** Minimum horizontal speed for the WALK/COMBAT_WALK animation state to be selected over IDLE. */
    public static double WALK_ANIMATION_SPEED_THRESHOLD = 0.02;

    /** Minimum horizontal speed for the RUN/COMBAT_RUN animation state to be selected over WALK. */
    public static double RUN_ANIMATION_SPEED_THRESHOLD = 0.13;

    // ------------------------------------------------------------------
    // Attack: preparation (wind-up), release, and recovery timing
    // ------------------------------------------------------------------

    /**
     * Minimum ticks the player must remain in {@code PREPARING_ATTACK}
     * before a release request is honored. Releases requested earlier are
     * buffered and applied the instant this elapses, rather than dropped,
     * so a very fast click still produces an attack.
     */
    public static int MIN_ATTACK_PREPARATION_TICKS = 4;

    /**
     * Ticks spent in {@code ATTACKING} before transitioning to
     * {@code RECOVERY}. Animation-only in this phase; no damage or hit
     * window is derived from this value yet.
     */
    public static int ATTACK_RELEASE_DURATION_TICKS = 8;

    /**
     * Base ticks spent in {@code RECOVERY} before returning to
     * {@code COMBAT_IDLE}. Future weapon classes are expected to scale
     * this via a multiplier rather than replace it outright.
     */
    public static int RECOVERY_DURATION_TICKS = 10;

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
    public static float ATTACK_DIRECTION_DEADZONE_DEGREES = 6.0f;

    /**
     * Minimum single-tick yaw/pitch delta (in degrees) for that tick's
     * mouse movement to be classified as a deliberate flick rather than a
     * slow drift, by {@code AttackDirectionTracker}. A flick temporarily
     * relieves the direction deadzone (see {@link
     * #ATTACK_FLICK_DEADZONE_RELIEF_RATIO}) for that resolution only, so
     * a fast, decisive mouse snap commits a direction sooner than a slow
     * drift crossing the same cumulative deviation would — this is what
     * "prevent unnecessary input delay" means for attack direction
     * selection without shrinking the deadzone for everyone and
     * reintroducing jitter-flicking on a slow drift.
     */
    public static float ATTACK_MOUSE_SENSITIVITY_THRESHOLD = 2.0f;

    /**
     * Multiplier applied to {@link #ATTACK_DIRECTION_DEADZONE_DEGREES}
     * during a tick classified as a deliberate flick (see {@link
     * #ATTACK_MOUSE_SENSITIVITY_THRESHOLD}). Below 1.0 shrinks the
     * effective deadzone for that resolution, letting a fast, decisive
     * flick commit a direction before the full deadzone would otherwise
     * allow — never widens it.
     */
    public static float ATTACK_FLICK_DEADZONE_RELIEF_RATIO = 0.5f;

    // ------------------------------------------------------------------
    // Block: transition timing (in ticks)
    // ------------------------------------------------------------------

    /** Ticks spent in ENTER_BLOCK before the state machine reaches BLOCK_IDLE. */
    public static int ENTER_BLOCK_TRANSITION_TICKS = 5;

    /** Ticks spent in EXIT_BLOCK before the state machine returns to COMBAT_IDLE. */
    public static int EXIT_BLOCK_TRANSITION_TICKS = 5;

    // ------------------------------------------------------------------
    // Block: guard direction detection
    // ------------------------------------------------------------------

    /**
     * Minimum combined look-direction deviation (in degrees, yaw or
     * pitch) since block began before a direction other than
     * {@code GuardDirection.NONE} is proposed. Kept as its own constant
     * rather than reusing {@link #ATTACK_DIRECTION_DEADZONE_DEGREES}
     * since block and attack direction detection are tuned independently.
     */
    public static float GUARD_DIRECTION_DEADZONE_DEGREES = 5.0f;

    /**
     * Multiplier applied to raw yaw/pitch deviation before deadzone and
     * direction classification. Values above 1.0 make guard direction
     * detection more sensitive to small mouse movements; values below
     * 1.0 require larger movements before a direction registers.
     */
    public static float GUARD_DIRECTION_SENSITIVITY = 1.0f;

    /**
     * Minimum single-tick yaw/pitch delta (in degrees, pre-{@link
     * #GUARD_DIRECTION_SENSITIVITY}) for that tick's mouse movement to be
     * classified as a deliberate flick rather than a slow drift, by
     * {@code GuardDirectionTracker}. Mirrors {@link
     * #ATTACK_MOUSE_SENSITIVITY_THRESHOLD} — see {@link
     * #GUARD_FLICK_DEADZONE_RELIEF_RATIO} for what a flick actually does.
     */
    public static float GUARD_MOUSE_SENSITIVITY_THRESHOLD = 2.0f;

    /**
     * Multiplier applied to {@link #GUARD_DIRECTION_DEADZONE_DEGREES}
     * during a tick classified as a deliberate flick (see {@link
     * #GUARD_MOUSE_SENSITIVITY_THRESHOLD}). Mirrors {@link
     * #ATTACK_FLICK_DEADZONE_RELIEF_RATIO} — a snappy guard switch reads
     * sooner than a slow drift into the same zone, without lowering the
     * deadzone for everyone.
     */
    public static float GUARD_FLICK_DEADZONE_RELIEF_RATIO = 0.5f;

    /**
     * Minimum ticks that must elapse after an accepted guard direction
     * change before another change is accepted. Does not delay the
     * initial lock when block is first entered (from {@code NONE}) —
     * only subsequent switches away from an already-locked direction.
     * This is what prevents small mouse jitter near a directional
     * boundary from rapidly flipping the guard.
     */
    public static int GUARD_SWITCH_DELAY_TICKS = 4;

    // ------------------------------------------------------------------
    // Defense: Perfect Block, Parry, and Chamber (Bannerlord-inspired
    // skill-based defense). All timing here is measured against
    // CombatController#notifyIncomingAttack's ticksUntilImpact — the
    // extension point a future hit-detection/AI/networking system calls
    // the instant an attack is about to connect.
    // ------------------------------------------------------------------

    /**
     * The window, in ticks, around an incoming attack's impact during
     * which a correctly-directed guard counts as a Perfect Block. Was
     * previously reserved and unused until this phase; now the active
     * timing source for {@code CombatState.PERFECT_BLOCK}.
     */
    public static int PERFECT_BLOCK_WINDOW_TICKS = 6;

    /**
     * A tighter subset of {@link #PERFECT_BLOCK_WINDOW_TICKS}. A Perfect
     * Block landing inside this narrower window upgrades to
     * {@code CombatState.PARRY} instead. Must be less than or equal to
     * {@link #PERFECT_BLOCK_WINDOW_TICKS}.
     */
    public static int PARRY_WINDOW_TICKS = 3;

    /** Ticks held in {@code CombatState.PERFECT_BLOCK} before returning to {@code BLOCK_IDLE}. */
    public static int PERFECT_BLOCK_STATE_DURATION_TICKS = 6;

    /** Ticks held in {@code CombatState.PARRY} before control returns to {@code COMBAT_IDLE}. */
    public static int PARRY_STATE_DURATION_TICKS = 5;

    /**
     * The window, in ticks, around an incoming attack's impact during
     * which a defender's matching committed {@code AttackDirection}
     * counts as a successful Chamber.
     */
    public static int CHAMBER_WINDOW_TICKS = 5;

    /**
     * Ticks spent in {@code CombatState.CHAMBER_PREPARE} while the
     * timing outcome resolves, before advancing to
     * {@code CHAMBER_SUCCESS} or reverting to {@code PREPARING_ATTACK}.
     */
    public static int CHAMBER_PREPARE_DURATION_TICKS = 4;

    /** Ticks held in {@code CombatState.CHAMBER_SUCCESS} before returning to {@code COMBAT_IDLE}. */
    public static int CHAMBER_SUCCESS_DURATION_TICKS = 6;

    /**
     * Blend duration, in ticks, used specifically for the Perfect Block /
     * Parry / Chamber reaction animations. Kept independent of
     * {@link #ANIMATION_BLEND_DURATION_TICKS} so these snappier defensive
     * reactions can be tuned without affecting locomotion/attack blending.
     */
    public static int DEFENSE_ANIMATION_BLEND_DURATION_TICKS = 3;

    /**
     * Reserved multiplier for future weapon-specific adjustment of
     * defense timing windows (e.g. a heavier weapon narrowing its
     * Perfect Block window, or a buckler widening it). Unused until
     * weapon stats exist; kept at 1.0 (no change).
     */
    public static final double DEFAULT_DEFENSE_TIMING_MODIFIER = 1.0;

    // ------------------------------------------------------------------
    // Collision & hit detection. All timing here is measured against
    // ticks elapsed since CombatState.ATTACKING (the release phase) was
    // entered; all distance/angle values are read by
    // com.bcombat.combat.collision.CollisionDetector. Weapon reach
    // itself is never hardcoded here — it always comes from the
    // equipped weapon's WeaponProperties#reach(); only the *modifier*
    // applied on top of that reach, and the fixed detection tolerance,
    // are configured in this class.
    // ------------------------------------------------------------------

    /**
     * Fraction (0-1) of the {@code ATTACKING} state's weapon-scaled
     * duration at which the collision detection window opens. Kept
     * below 1.0 alongside {@link #COLLISION_WINDOW_END_RATIO} so the
     * check only runs during the "business end" of the swing rather
     * than the very first frame of the animation, before the weapon
     * would plausibly have reached anything.
     */
    public static final float COLLISION_WINDOW_START_RATIO = 0.25f;

    /**
     * Fraction (0-1) of the {@code ATTACKING} state's weapon-scaled
     * duration at which the collision detection window closes. If no
     * target is found by this point, the swing resolves as a miss. Must
     * be greater than or equal to {@link #COLLISION_WINDOW_START_RATIO}.
     */
    public static final float COLLISION_WINDOW_END_RATIO = 0.75f;

    /**
     * Extra distance, in blocks, added on top of a weapon's effective
     * reach before a target is considered out of range. Accounts for
     * hitbox/collision-check imprecision (bounding-box centers vs.
     * actual model surfaces) so a swing that should plausibly connect
     * isn't rejected by a hairline distance difference.
     */
    public static final double COLLISION_REACH_TOLERANCE = 0.5;

    /**
     * Half-angle, in degrees, of the forward cone (measured from the
     * attacker's look direction) within which a target may be struck.
     * A target directly ahead is at 0 degrees; a target directly beside
     * the attacker is at 90 degrees.
     */
    public static final double COLLISION_CONE_HALF_ANGLE_DEGREES = 60.0;

    /**
     * Global multiplier applied to every weapon's {@code
     * WeaponProperties#reach()} before collision range is computed.
     * Reserved as a single future-config/balance knob (e.g. a server
     * config toggling all reach up or down) that doesn't require
     * touching every individual weapon registration. Kept at 1.0 (no
     * change) until such configuration exists.
     */
    public static final double DEFAULT_WEAPON_REACH_MODIFIER = 1.0;

    /**
     * Minimum vertical-placement fraction (0 = target's feet, 1 = the
     * top of its hitbox) for a hit to classify as {@code
     * HitLocation.HEAD}. See {@code CollisionDetector#classifyHitLocation}.
     */
    public static final double HEAD_HITBOX_HEIGHT_RATIO = 0.85;

    /**
     * Maximum vertical-placement fraction for a hit to classify as
     * {@code HitLocation.LEGS}. Everything between this and {@link
     * #HEAD_HITBOX_HEIGHT_RATIO} is torso-height and further split into
     * {@code TORSO}/{@code ARMS} by {@link #ARM_HITBOX_WIDTH_RATIO}.
     */
    public static final double LEG_HITBOX_HEIGHT_RATIO = 0.35;

    /**
     * Minimum lateral offset from the attacker's look line — normalized
     * by half the target's hitbox width, where 0 is dead-center and 1
     * is the target's silhouette edge — for a torso-height hit to
     * classify as {@code HitLocation.ARMS} instead of {@code TORSO}.
     */
    public static final double ARM_HITBOX_WIDTH_RATIO = 0.55;

    /**
     * Vertical-placement bias applied for {@code AttackDirection.OVERHEAD}
     * strikes when estimating hit location — an overhead strike lands
     * higher on the target than the attacker's raw eye-height alone
     * would suggest.
     */
    public static final double OVERHEAD_HIT_HEIGHT_BIAS = 0.15;

    /**
     * Vertical-placement bias applied for {@code AttackDirection.LEFT_SLASH}/
     * {@code RIGHT_SLASH} strikes when estimating hit location — a
     * horizontal slash lands slightly lower than a neutral thrust.
     */
    public static final double SLASH_HIT_HEIGHT_BIAS = -0.05;

    /**
     * Vertical-placement bias applied for {@code AttackDirection.THRUST}
     * (and {@code NONE}) strikes when estimating hit location. Zero —
     * the neutral baseline the other directions bias away from.
     */
    public static final double THRUST_HIT_HEIGHT_BIAS = 0.0;

    /**
     * Reserved multiplier for a future dedicated body-hitbox phase to
     * scale detection tolerance per body region (e.g. a narrower
     * effective hitbox for a called head-shot mechanic). Unused until
     * that phase exists; kept at 1.0 (no change).
     */
    public static final double DEFAULT_BODY_HITBOX_MODIFIER = 1.0;

    // ------------------------------------------------------------------
    // Stamina & combat fatigue (Bannerlord-inspired). All base costs are
    // scaled per-action by the equipped weapon's {@code
    // WeaponProperties#staminaModifier()}; all regen timing is scaled by
    // {@code WeaponProperties#staminaRegenDelayModifier()}/{@code
    // #staminaRegenRateModifier()}. See {@code
    // com.bcombat.combat.stamina.StaminaController} for the framework
    // itself and {@code CombatController} for where each cost below is
    // actually applied.
    // ------------------------------------------------------------------

    /** Default maximum stamina for a player with no perks/equipment adjustments. */
    public static double DEFAULT_MAX_STAMINA = 100.0;

    /** Stamina restored per tick while regeneration is active and not delayed. */
    public static double STAMINA_REGEN_RATE_PER_TICK = 0.5;

    /**
     * Ticks that must elapse after the most recent stamina consumption
     * before regeneration resumes. Independent of (and stacks with) the
     * explicit suspension applied while attacking or holding a block —
     * see {@code CombatController#isStaminaRegenSuspended()}.
     */
    public static int STAMINA_REGEN_DELAY_TICKS = 30;

    /** Stamina cost of committing an attack wind-up into its release (the swing itself). */
    public static double ATTACK_STAMINA_COST = 8.0;

    /** Stamina cost of raising a guard, charged once on entering {@code ENTER_BLOCK}. */
    public static double BLOCK_ENTER_STAMINA_COST = 3.0;

    /** Stamina drained per tick while a guard is actively held ({@code ENTER_BLOCK}/{@code BLOCK_IDLE}/{@code PERFECT_BLOCK}). */
    public static double BLOCK_HOLD_STAMINA_COST_PER_TICK = 0.15;

    /**
     * Stamina cost of landing a Perfect Block. Deliberately cheaper than
     * a mistimed block absorbing a full hit would be (that full-hit cost
     * is reserved future work, since damage-to-blocker isn't modeled
     * yet), rewarding precise timing.
     */
    public static double PERFECT_BLOCK_STAMINA_COST = 3.0;

    /**
     * Stamina cost of landing a Parry — the tightest-timed defensive
     * mechanic, and consequently the cheapest, matching Bannerlord's own
     * skill-rewards-efficiency philosophy.
     */
    public static double PARRY_STAMINA_COST = 1.0;

    /** Stamina cost of committing to a Chamber attempt, charged regardless of whether the timing succeeds. */
    public static double CHAMBER_STAMINA_COST = 5.0;

    /** Stamina drained per tick while sprinting in Combat Mode. */
    public static double SPRINT_COMBAT_STAMINA_COST_PER_TICK = 0.2;

    /**
     * Reserved stamina cost for a future dodge mechanic. Unused in this
     * phase — no dodge system exists yet — kept here purely as the
     * configuration extension point {@code CombatConstants} is meant to
     * provide ahead of the system that will consume it.
     */
    public static final double DODGE_STAMINA_COST = 6.0;

    /**
     * Fraction (0-1) of maximum stamina that must be regenerated before
     * a player automatically leaves {@code ExhaustionState#EXHAUSTED}.
     * Kept above zero so exhaustion isn't shrugged off after a single
     * tick of regeneration.
     */
    public static double EXHAUSTION_RECOVERY_THRESHOLD_RATIO = 0.25;

    /**
     * Additional movement speed multiplier applied on top of the
     * standing Combat Mode penalty while exhausted. Expressed the same
     * way as {@link #COMBAT_WALK_SPEED_MODIFIER} (an ADD_MULTIPLIED_TOTAL
     * operand), applied/removed by {@code MovementModifierManager}.
     */
    public static double EXHAUSTED_MOVEMENT_SPEED_MODIFIER = -0.25;
}