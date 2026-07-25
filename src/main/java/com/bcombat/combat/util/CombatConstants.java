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

    // ------------------------------------------------------------------
    // Mounted Combat (Bannerlord-inspired). All base timing/costs above
    // remain the ground-combat baseline; every field below is a
    // *multiplier* stacked on top of that baseline only while {@code
    // com.bcombat.combat.mounted.MountedCombatController#isMounted()}
    // is true — see {@code
    // com.bcombat.combat.mounted.MountedCombatModifiers} for the single
    // place these are actually combined with weapon/base values.
    // ------------------------------------------------------------------

    /** Multiplier applied to a weapon's effective reach while mounted. */
    public static double MOUNTED_REACH_MODIFIER = 1.35;

    /** Multiplier applied to wind-up duration while mounted. */
    public static double MOUNTED_WIND_UP_MODIFIER = 1.15;

    /** Multiplier applied to the ATTACKING (release) duration while mounted. */
    public static double MOUNTED_RELEASE_MODIFIER = 1.10;

    /** Multiplier applied to RECOVERY duration while mounted. */
    public static double MOUNTED_RECOVERY_MODIFIER = 1.20;

    /** Multiplier applied to every stamina cost (attack, block, perfect block, parry, chamber) while mounted. */
    public static double MOUNTED_STAMINA_COST_MODIFIER = 1.25;

    /** Multiplier applied to stamina regeneration rate while mounted. */
    public static double MOUNTED_STAMINA_REGEN_RATE_MODIFIER = 0.85;

    /** Multiplier applied to a mounted attacker's final damage output. */
    public static double MOUNTED_DAMAGE_MULTIPLIER = 1.20;

    /**
     * Minimum horizontal speed (blocks/tick) the mount must be moving at
     * for {@link #MOUNTED_CHARGE_DAMAGE_BONUS} to additionally apply.
     */
    public static double MOUNTED_CHARGE_SPEED_THRESHOLD = 0.25;

    /** Additional flat multiplier stacked on top of {@link #MOUNTED_DAMAGE_MULTIPLIER} during a charge. */
    public static double MOUNTED_CHARGE_DAMAGE_BONUS = 1.15;

    /** Global on/off switch for the entire mounted combat framework. */
    public static boolean MOUNTED_COMBAT_ENABLED = true;

    /** If true, only vehicles recognized as combat mounts count as "mounted" for combat purposes. */
    public static boolean MOUNTED_REQUIRE_RECOGNIZED_MOUNT = true;

    // ------------------------------------------------------------------
    // Couch Lance Combat (Bannerlord-inspired). Extends Mounted Combat
    // above: every field here only ever applies on top of an already
    // {@code MountedCombatController#isMounted()} combatant, and only
    // while wielding a weapon whose {@code WeaponProperties#isCouchCapable()}
    // is true - see {@code com.bcombat.combat.couch.CouchLanceController}
    // for the eligibility/state-machine logic that reads these, and
    // {@code com.bcombat.combat.couch.CouchLanceModifiers} for the single
    // place the damage-facing values below are actually combined with
    // mounted/weapon values.
    // ------------------------------------------------------------------

    /** Global on/off switch for the entire couch lance framework. */
    public static boolean COUCH_LANCE_ENABLED = true;

    /**
     * Minimum mount horizontal speed (blocks/tick) required for a rider
     * to become eligible to ready ("couch") a couch-capable weapon at
     * all. Below this, a couch-capable weapon behaves like any other
     * mounted weapon - normal mounted attacks, no couch bonuses.
     */
    public static double COUCH_MIN_HORSE_SPEED = 0.20;

    /**
     * Additional fractional damage bonus (on top of {@link
     * #COUCH_DAMAGE_MULTIPLIER}) available purely from speed, reached in
     * full once the mount is moving at double {@link
     * #COUCH_MIN_HORSE_SPEED}. E.g. {@code 0.5} means a fully-charging
     * mount deals up to 50% more than a mount only just barely eligible.
     * Scales linearly between {@link #COUCH_MIN_HORSE_SPEED} (0% bonus)
     * and double that speed (100% of this value), then holds flat -
     * see {@code CouchLanceModifiers#damageMultiplier}.
     */
    public static double COUCH_MAX_SPEED_BONUS = 0.50;

    /** Base damage multiplier applied to a successful couched-lance impact, before the speed/momentum bonuses. */
    public static double COUCH_DAMAGE_MULTIPLIER = 1.75;

    /**
     * Speed, as a multiple of {@link #COUCH_MIN_HORSE_SPEED}, at which
     * the flat {@link #COUCH_MOMENTUM_MULTIPLIER} bonus additionally
     * applies - representing the mount having built up enough momentum
     * for the impact to be meaningfully harder, distinct from (and
     * stacked on top of) the continuous speed-scaled bonus above.
     */
    public static double COUCH_MOMENTUM_SPEED_RATIO = 1.5;

    /** Flat multiplier stacked on top of every other couch damage bonus once {@link #COUCH_MOMENTUM_SPEED_RATIO} is met. */
    public static double COUCH_MOMENTUM_MULTIPLIER = 1.15;

    /**
     * Absolute ceiling on final damage for a hit that received any
     * couch bonus at all (never applied to ordinary, non-couched hits),
     * so stacking every bonus at maximum charge speed can never produce
     * an unbounded one-shot.
     */
    public static double COUCH_MAX_DAMAGE_CAP = 40.0;

    /** Ticks a rider must hold couch-eligible conditions while {@code PREPARING} before the lance becomes {@code ACTIVE} (ready to strike). */
    public static int COUCH_PREPARE_TICKS = 8;

    /**
     * Ticks spent in the couch state machine's {@code RECOVERY} state
     * after an impact, interrupt, or resolved (missed/blocked) couched
     * strike, before couching can be attempted again - independent of,
     * and stacked alongside, {@code CombatState.RECOVERY}'s own
     * weapon/mounted-scaled duration for the underlying attack itself.
     * Directly scaled down by mount speed at the moment recovery began,
     * per the design requirement that horse velocity influence recovery
     * time - see {@code CouchLanceModifiers#recoveryTicks}.
     */
    public static int COUCH_RECOVERY_TICKS = 30;

    /** Minimum ticks after entering {@code RECOVERY}, regardless of speed-based reduction, so recovery can never collapse to zero. */
    public static int COUCH_MIN_RECOVERY_TICKS = 10;

    /** Additional cooldown, in ticks, after {@code RECOVERY} completes before the rider is eligible to begin couching again. */
    public static int COUCH_COOLDOWN_TICKS = 20;

    /**
     * Base knockback strength applied to the target on a successful
     * couch impact (representing the physical force of a mounted
     * charge), scaled further by the attacker's speed ratio at impact -
     * see {@code CouchLanceModifiers#impactForce}.
     */
    public static double COUCH_IMPACT_FORCE = 1.4;

    /**
     * Maximum distance, in blocks, an obstacle-safety raycast checks
     * directly ahead of the mount for terrain safety - a wall or cliff
     * edge within this distance cancels/blocks couching, per the design
     * requirement that terrain safety gate activation.
     */
    public static double COUCH_TERRAIN_CHECK_DISTANCE = 2.0;

    /** If true, a mount currently in any fluid (water/lava) is never terrain-safe to couch in. */
    public static boolean COUCH_REQUIRE_DRY_TERRAIN = true;

    /**
     * Preferred approach distance, in blocks, a couch-eligible mounted
     * AI tries to reach before triggering its charge - analogous to
     * {@code AIDifficultyPreset#preferredDistanceRatio()} but specific
     * to the longer effective reach of a couched charge.
     */
    public static double COUCH_AI_ENGAGE_DISTANCE = 6.0;

    /** Per-decision probability a couch-eligible mounted AI actually commits to charging rather than fighting normally. */
    public static double COUCH_AI_COUCH_CHANCE = 0.5;

    // ------------------------------------------------------------------
    // Group AI / Squad Tactics (Advanced AI Behaviors & Group Combat
    // Framework). Every field below is read by {@code
    // com.bcombat.combat.ai.group.CombatSquad}/{@code SquadManager} and
    // by {@code AICombatController}'s group-tactics integration only -
    // none of it is read by, or changes the behavior of, the solo
    // AI Combat Framework, {@code CombatController}, or any player-facing
    // system. Per-role and per-difficulty multipliers themselves remain
    // hardcoded enum constants on {@code CombatRole}/{@code
    // AIDifficultyPreset} (same convention those two already use); only
    // the global thresholds/radii/weights shared by every role and
    // difficulty live here, exactly like {@link #COUCH_AI_ENGAGE_DISTANCE}
    // and {@link #COUCH_AI_COUCH_CHANCE} already do for mounted AI.
    // ------------------------------------------------------------------

    /** Global on/off switch for group/squad tactics. When false, every AI-controlled combatant behaves exactly as the solo AI Combat Framework always has. */
    public static boolean GROUP_AI_ENABLED = true;

    /**
     * Radius, in blocks, within which another squad member counts as
     * "nearby" for shared combat awareness, spacing, and friendly-fire
     * checks - the group-tactics analogue of {@code
     * AIDifficultyPreset#engagementRange()}.
     */
    public static double SQUAD_AWARENESS_RADIUS = 16.0;

    /** Minimum distance, in blocks, a squad member tries to keep from any other squad member while holding a flank/surround position. */
    public static double SQUAD_MIN_ALLY_SPACING = 2.25;

    /** Multiplier applied to a member's own ideal fighting distance to get the radius squad flank/surround slots are placed at around the focus target. */
    public static double SQUAD_FLANK_RADIUS_RATIO = 1.05;

    /**
     * Fractional score margin (e.g. 0.20 = 20%) a candidate threat must
     * exceed the squad's current focus target's score by before {@code
     * com.bcombat.combat.ai.group.CombatSquad} switches focus to it -
     * prevents rapid thrashing between near-equal threats.
     */
    public static double SQUAD_TARGET_SWITCH_MARGIN = 0.20;

    /** Minimum ticks between one squad-wide target switch and the next. */
    public static int SQUAD_TARGET_SWITCH_COOLDOWN_TICKS = 40;

    /** Threat-score weight for inverse distance (closer candidates score higher). */
    public static double SQUAD_THREAT_WEIGHT_PROXIMITY = 3.0;

    /** Threat-score weight for a candidate's missing health fraction (finishing a wounded enemy scores higher). */
    public static double SQUAD_THREAT_WEIGHT_LOW_HEALTH = 2.0;

    /** Threat-score weight added for a candidate that is currently mid-wind-up/mid-swing against any squad member. */
    public static double SQUAD_THREAT_WEIGHT_ACTIVE_THREAT = 4.0;

    /**
     * Own health ratio (0-1), before {@code CombatRole#retreatReluctance()}
     * is applied, below which an AI-controlled combatant treats itself as
     * critically wounded and prioritizes retreating over any offensive
     * action - independent of, and stacked alongside, the existing
     * stamina-based retreat trigger in {@code AICombatController}.
     */
    public static double SQUAD_LOW_HEALTH_RETREAT_RATIO = 0.30;

    /** Own health ratio (0-1) above which a retreating squad member still counts as "healthy" when the squad computes its regroup point. */
    public static double SQUAD_REGROUP_HEALTHY_RATIO = 0.50;

    /**
     * Minimum dot product (of the normalized attacker-to-ally and
     * attacker-to-target vectors) for an ally standing roughly between an
     * AI-controlled attacker and its target to be treated as a
     * friendly-fire risk and suppress that tick's attack initiation. 1.0
     * would require the ally to be exactly on the line; lower values
     * widen the cone that counts as "in the way".
     */
    public static double SQUAD_FRIENDLY_FIRE_CONE_COS = 0.92;

    /** Radius, in blocks, an unmounted squad member tries to stay clear of a nearby mounted ally that is actively charging, to avoid being run down. */
    public static double SQUAD_MOUNTED_CHARGE_DANGER_RADIUS = 4.0;

    /**
     * Extra 0..N random ticks added on top of {@code
     * AIDifficultyPreset#reactionDelayTicks()} for group-tactics
     * decisions specifically (squad target adoption, retreat/regroup
     * commitment) so a whole squad reacting to the same event doesn't
     * visibly do so in perfect lockstep. Purely a randomness knob - does
     * not affect solo (non-squad) AI decision timing.
     */
    public static int AI_REACTION_JITTER_TICKS = 4;

    // ---------------- Directional combat indicator (HUD) ----------------

    /**
     * When {@code false} (the default), the directional combat indicator
     * only fades in while the local player's {@code CombatState} is
     * combat-active (see {@code CombatState#isCombatActive()}) and fades
     * back out the instant Combat Mode is exited. When {@code true}, it
     * stays visible at all times (still fading out only while the HUD
     * itself is hidden), for players/packs that want the indicator as a
     * permanent HUD element rather than a combat-only affordance.
     */
    public static boolean DIRECTIONAL_INDICATOR_ALWAYS_VISIBLE = false;
}