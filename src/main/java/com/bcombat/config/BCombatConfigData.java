package com.bcombat.config;

import com.bcombat.combat.util.CombatConstants;

/**
 * Plain-data mirror of every {@code public static} (non-{@code final})
 * tunable field in {@link CombatConstants}, serialized to/from
 * {@code config/bcombat.json} by {@link BCombatConfig}.
 * <p>
 * Field names deliberately use lowerCamelCase (rather than matching
 * {@link CombatConstants}'s SCREAMING_SNAKE_CASE constants 1:1 in name)
 * since this is the wire/file format, not the runtime constant surface —
 * Gson serializes exactly the field names declared here. See {@link
 * BCombatConfig#applyToConstants} and {@link
 * BCombatConfig#captureFromConstants} for the explicit two-way mapping.
 * <p>
 * Every field is initialized to {@link CombatConstants}'s own current
 * default, so a freshly created instance (no file on disk yet) always
 * round-trips to identical behavior before any operator has changed
 * anything, and so any field a future version of this class doesn't yet
 * know about (an older config file) simply keeps the compiled-in
 * default rather than becoming a needs-null-check case.
 */
public final class BCombatConfigData {

    // ---------------- Movement ----------------
    public double combatWalkSpeedModifier = CombatConstants.COMBAT_WALK_SPEED_MODIFIER;
    public double combatSprintSpeedModifier = CombatConstants.COMBAT_SPRINT_SPEED_MODIFIER;
    public double windUpSpeedModifier = CombatConstants.WIND_UP_SPEED_MODIFIER;
    public double recoverySpeedModifier = CombatConstants.RECOVERY_SPEED_MODIFIER;

    // ---------------- Combat mode transition timing ----------------
    public int enterCombatTransitionTicks = CombatConstants.ENTER_COMBAT_TRANSITION_TICKS;
    public int exitCombatTransitionTicks = CombatConstants.EXIT_COMBAT_TRANSITION_TICKS;

    // ---------------- Animation ----------------
    public int animationBlendDurationTicks = CombatConstants.ANIMATION_BLEND_DURATION_TICKS;
    public double walkAnimationSpeedThreshold = CombatConstants.WALK_ANIMATION_SPEED_THRESHOLD;
    public double runAnimationSpeedThreshold = CombatConstants.RUN_ANIMATION_SPEED_THRESHOLD;

    // ---------------- Attack timing ----------------
    public int minAttackPreparationTicks = CombatConstants.MIN_ATTACK_PREPARATION_TICKS;
    public int attackReleaseDurationTicks = CombatConstants.ATTACK_RELEASE_DURATION_TICKS;
    public int recoveryDurationTicks = CombatConstants.RECOVERY_DURATION_TICKS;

    // ---------------- Attack direction / flick detection ----------------
    public float attackDirectionDeadzoneDegrees = CombatConstants.ATTACK_DIRECTION_DEADZONE_DEGREES;
    public float attackMouseSensitivityThreshold = CombatConstants.ATTACK_MOUSE_SENSITIVITY_THRESHOLD;
    public float attackFlickDeadzoneReliefRatio = CombatConstants.ATTACK_FLICK_DEADZONE_RELIEF_RATIO;

    // ---------------- Block transition timing ----------------
    public int enterBlockTransitionTicks = CombatConstants.ENTER_BLOCK_TRANSITION_TICKS;
    public int exitBlockTransitionTicks = CombatConstants.EXIT_BLOCK_TRANSITION_TICKS;

    // ---------------- Guard direction / flick detection ----------------
    public float guardDirectionDeadzoneDegrees = CombatConstants.GUARD_DIRECTION_DEADZONE_DEGREES;
    public float guardDirectionSensitivity = CombatConstants.GUARD_DIRECTION_SENSITIVITY;
    public float guardMouseSensitivityThreshold = CombatConstants.GUARD_MOUSE_SENSITIVITY_THRESHOLD;
    public float guardFlickDeadzoneReliefRatio = CombatConstants.GUARD_FLICK_DEADZONE_RELIEF_RATIO;
    public int guardSwitchDelayTicks = CombatConstants.GUARD_SWITCH_DELAY_TICKS;

    // ---------------- Defense: Perfect Block / Parry / Chamber ----------------
    public int perfectBlockWindowTicks = CombatConstants.PERFECT_BLOCK_WINDOW_TICKS;
    public int parryWindowTicks = CombatConstants.PARRY_WINDOW_TICKS;
    public int perfectBlockStateDurationTicks = CombatConstants.PERFECT_BLOCK_STATE_DURATION_TICKS;
    public int parryStateDurationTicks = CombatConstants.PARRY_STATE_DURATION_TICKS;
    public int chamberWindowTicks = CombatConstants.CHAMBER_WINDOW_TICKS;
    public int chamberPrepareDurationTicks = CombatConstants.CHAMBER_PREPARE_DURATION_TICKS;
    public int chamberSuccessDurationTicks = CombatConstants.CHAMBER_SUCCESS_DURATION_TICKS;
    public int defenseAnimationBlendDurationTicks = CombatConstants.DEFENSE_ANIMATION_BLEND_DURATION_TICKS;

    // ---------------- Stamina & fatigue ----------------
    public double defaultMaxStamina = CombatConstants.DEFAULT_MAX_STAMINA;
    public double staminaRegenRatePerTick = CombatConstants.STAMINA_REGEN_RATE_PER_TICK;
    public int staminaRegenDelayTicks = CombatConstants.STAMINA_REGEN_DELAY_TICKS;
    public double attackStaminaCost = CombatConstants.ATTACK_STAMINA_COST;
    public double blockEnterStaminaCost = CombatConstants.BLOCK_ENTER_STAMINA_COST;
    public double blockHoldStaminaCostPerTick = CombatConstants.BLOCK_HOLD_STAMINA_COST_PER_TICK;
    public double perfectBlockStaminaCost = CombatConstants.PERFECT_BLOCK_STAMINA_COST;
    public double parryStaminaCost = CombatConstants.PARRY_STAMINA_COST;
    public double chamberStaminaCost = CombatConstants.CHAMBER_STAMINA_COST;
    public double sprintCombatStaminaCostPerTick = CombatConstants.SPRINT_COMBAT_STAMINA_COST_PER_TICK;
    public double exhaustionRecoveryThresholdRatio = CombatConstants.EXHAUSTION_RECOVERY_THRESHOLD_RATIO;
    public double exhaustedMovementSpeedModifier = CombatConstants.EXHAUSTED_MOVEMENT_SPEED_MODIFIER;

    // ---------------- Mounted combat ----------------
    public double mountedReachModifier = CombatConstants.MOUNTED_REACH_MODIFIER;
    public double mountedWindUpModifier = CombatConstants.MOUNTED_WIND_UP_MODIFIER;
    public double mountedReleaseModifier = CombatConstants.MOUNTED_RELEASE_MODIFIER;
    public double mountedRecoveryModifier = CombatConstants.MOUNTED_RECOVERY_MODIFIER;
    public double mountedStaminaCostModifier = CombatConstants.MOUNTED_STAMINA_COST_MODIFIER;
    public double mountedStaminaRegenRateModifier = CombatConstants.MOUNTED_STAMINA_REGEN_RATE_MODIFIER;
    public double mountedDamageMultiplier = CombatConstants.MOUNTED_DAMAGE_MULTIPLIER;
    public double mountedChargeSpeedThreshold = CombatConstants.MOUNTED_CHARGE_SPEED_THRESHOLD;
    public double mountedChargeDamageBonus = CombatConstants.MOUNTED_CHARGE_DAMAGE_BONUS;
    public boolean mountedCombatEnabled = CombatConstants.MOUNTED_COMBAT_ENABLED;
    public boolean mountedRequireRecognizedMount = CombatConstants.MOUNTED_REQUIRE_RECOGNIZED_MOUNT;
}