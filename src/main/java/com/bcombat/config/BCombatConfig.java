package com.bcombat.config;

import com.bcombat.BannerlordCombat;
import com.bcombat.combat.util.CombatConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads and saves {@code config/bcombat.json}, the single file a server
 * operator (or singleplayer host) edits to rebalance the combat
 * framework without touching code. This is the ONLY class that reads or
 * writes that file, and the ONLY class that mutates {@link
 * CombatConstants}'s fields after startup — every other class keeps
 * reading {@code CombatConstants.FIELD} exactly as it always has, so no
 * other call site needs to know config exists at all.
 * <p>
 * {@link #load()} must be called exactly once, as the very first thing
 * {@code BannerlordCombat#onInitialize()} does — before {@code
 * DefaultWeaponRegistrations}, {@code DefaultArmorRegistrations}, or any
 * other system that might read a {@link CombatConstants} value during
 * its own setup. If {@code config/bcombat.json} doesn't exist yet (first
 * run), the compiled-in defaults already sitting in {@link
 * CombatConstants} are treated as the config and immediately written out
 * as a fully-populated, human-editable file — so an operator always has
 * a complete file to edit rather than an empty/missing one.
 * <p>
 * {@link #reload()} re-reads the file and re-applies it — the backing
 * implementation for {@code /bcombat config reload} — and is safe to call
 * at any point after startup, including mid-game; every {@link
 * CombatConstants} field it touches is read fresh every time it's used
 * (wind-up ticks, stamina costs, etc.), so a reload takes effect on the
 * very next tick with no restart required.
 */
public final class BCombatConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "bcombat.json";

    private static Path configPath;

    private BCombatConfig() {
        // Static utility, no instances.
    }

    /**
     * Loads {@code config/bcombat.json} into {@link CombatConstants},
     * creating the file with the current (compiled-in default) values if
     * it doesn't exist yet. Must be called exactly once, at the very
     * start of {@code BannerlordCombat#onInitialize()}.
     */
    public static void load() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
        BCombatConfigData data = readFromDisk();
        applyToConstants(data);
        // Always write back out: fills in any fields a missing/older
        // file didn't have with their now-applied defaults, and
        // normalizes formatting, so config/bcombat.json is always a
        // complete, current reference for an operator to edit.
        writeToDisk(captureFromConstants());
        BannerlordCombat.LOGGER.info("[bcombat] Loaded combat configuration from {}", configPath);
    }

    /**
     * Re-reads {@code config/bcombat.json} from disk and re-applies it to
     * {@link CombatConstants}. Backing implementation for {@code
     * /bcombat config reload}. Does not recreate the file if it's been
     * deleted since {@link #load()} — that case falls back to the
     * compiled-in defaults for this reload, exactly like a first run
     * would, but does not overwrite the file with them.
     */
    public static void reload() {
        if (configPath == null) {
            load();
            return;
        }
        BCombatConfigData data = readFromDisk();
        applyToConstants(data);
        BannerlordCombat.LOGGER.info("[bcombat] Reloaded combat configuration from {}", configPath);
    }

    /**
     * Writes {@link CombatConstants}'s current values back out to {@code
     * config/bcombat.json}. Useful after any future in-game tuning
     * command mutates a {@link CombatConstants} field directly and wants
     * that change to persist across restarts.
     */
    public static void save() {
        if (configPath == null) {
            configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
        }
        writeToDisk(captureFromConstants());
    }

    /** @return the resolved path to {@code config/bcombat.json}, for diagnostics. */
    public static Path getConfigPath() {
        return configPath;
    }

    // ------------------------------------------------------------------
    // Disk I/O
    // ------------------------------------------------------------------

    private static BCombatConfigData readFromDisk() {
        if (!Files.exists(configPath)) {
            return new BCombatConfigData();
        }
        try (Reader reader = Files.newBufferedReader(configPath)) {
            BCombatConfigData data = GSON.fromJson(reader, BCombatConfigData.class);
            return data != null ? data : new BCombatConfigData();
        } catch (IOException | JsonSyntaxException e) {
            BannerlordCombat.LOGGER.warn("[bcombat] Failed to read {}, falling back to defaults", configPath, e);
            return new BCombatConfigData();
        }
    }

    private static void writeToDisk(BCombatConfigData data) {
        try {
            if (configPath.getParent() != null) {
                Files.createDirectories(configPath.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            BannerlordCombat.LOGGER.error("[bcombat] Failed to write {}", configPath, e);
        }
    }

    // ------------------------------------------------------------------
    // Explicit two-way mapping - BCombatConfigData <-> CombatConstants
    // ------------------------------------------------------------------

    private static void applyToConstants(BCombatConfigData d) {
        CombatConstants.COMBAT_WALK_SPEED_MODIFIER = d.combatWalkSpeedModifier;
        CombatConstants.COMBAT_SPRINT_SPEED_MODIFIER = d.combatSprintSpeedModifier;
        CombatConstants.WIND_UP_SPEED_MODIFIER = d.windUpSpeedModifier;
        CombatConstants.RECOVERY_SPEED_MODIFIER = d.recoverySpeedModifier;

        CombatConstants.ENTER_COMBAT_TRANSITION_TICKS = d.enterCombatTransitionTicks;
        CombatConstants.EXIT_COMBAT_TRANSITION_TICKS = d.exitCombatTransitionTicks;

        CombatConstants.ANIMATION_BLEND_DURATION_TICKS = d.animationBlendDurationTicks;
        CombatConstants.WALK_ANIMATION_SPEED_THRESHOLD = d.walkAnimationSpeedThreshold;
        CombatConstants.RUN_ANIMATION_SPEED_THRESHOLD = d.runAnimationSpeedThreshold;

        CombatConstants.MIN_ATTACK_PREPARATION_TICKS = d.minAttackPreparationTicks;
        CombatConstants.ATTACK_RELEASE_DURATION_TICKS = d.attackReleaseDurationTicks;
        CombatConstants.RECOVERY_DURATION_TICKS = d.recoveryDurationTicks;

        CombatConstants.ATTACK_DIRECTION_DEADZONE_DEGREES = d.attackDirectionDeadzoneDegrees;
        CombatConstants.ATTACK_MOUSE_SENSITIVITY_THRESHOLD = d.attackMouseSensitivityThreshold;
        CombatConstants.ATTACK_FLICK_DEADZONE_RELIEF_RATIO = d.attackFlickDeadzoneReliefRatio;

        CombatConstants.ENTER_BLOCK_TRANSITION_TICKS = d.enterBlockTransitionTicks;
        CombatConstants.EXIT_BLOCK_TRANSITION_TICKS = d.exitBlockTransitionTicks;

        CombatConstants.GUARD_DIRECTION_DEADZONE_DEGREES = d.guardDirectionDeadzoneDegrees;
        CombatConstants.GUARD_DIRECTION_SENSITIVITY = d.guardDirectionSensitivity;
        CombatConstants.GUARD_MOUSE_SENSITIVITY_THRESHOLD = d.guardMouseSensitivityThreshold;
        CombatConstants.GUARD_FLICK_DEADZONE_RELIEF_RATIO = d.guardFlickDeadzoneReliefRatio;
        CombatConstants.GUARD_SWITCH_DELAY_TICKS = d.guardSwitchDelayTicks;

        CombatConstants.PERFECT_BLOCK_WINDOW_TICKS = d.perfectBlockWindowTicks;
        CombatConstants.PARRY_WINDOW_TICKS = d.parryWindowTicks;
        CombatConstants.PERFECT_BLOCK_STATE_DURATION_TICKS = d.perfectBlockStateDurationTicks;
        CombatConstants.PARRY_STATE_DURATION_TICKS = d.parryStateDurationTicks;
        CombatConstants.CHAMBER_WINDOW_TICKS = d.chamberWindowTicks;
        CombatConstants.CHAMBER_PREPARE_DURATION_TICKS = d.chamberPrepareDurationTicks;
        CombatConstants.CHAMBER_SUCCESS_DURATION_TICKS = d.chamberSuccessDurationTicks;
        CombatConstants.DEFENSE_ANIMATION_BLEND_DURATION_TICKS = d.defenseAnimationBlendDurationTicks;

        CombatConstants.DEFAULT_MAX_STAMINA = d.defaultMaxStamina;
        CombatConstants.STAMINA_REGEN_RATE_PER_TICK = d.staminaRegenRatePerTick;
        CombatConstants.STAMINA_REGEN_DELAY_TICKS = d.staminaRegenDelayTicks;
        CombatConstants.ATTACK_STAMINA_COST = d.attackStaminaCost;
        CombatConstants.BLOCK_ENTER_STAMINA_COST = d.blockEnterStaminaCost;
        CombatConstants.BLOCK_HOLD_STAMINA_COST_PER_TICK = d.blockHoldStaminaCostPerTick;
        CombatConstants.PERFECT_BLOCK_STAMINA_COST = d.perfectBlockStaminaCost;
        CombatConstants.PARRY_STAMINA_COST = d.parryStaminaCost;
        CombatConstants.CHAMBER_STAMINA_COST = d.chamberStaminaCost;
        CombatConstants.SPRINT_COMBAT_STAMINA_COST_PER_TICK = d.sprintCombatStaminaCostPerTick;
        CombatConstants.EXHAUSTION_RECOVERY_THRESHOLD_RATIO = d.exhaustionRecoveryThresholdRatio;
        CombatConstants.EXHAUSTED_MOVEMENT_SPEED_MODIFIER = d.exhaustedMovementSpeedModifier;

        CombatConstants.MOUNTED_REACH_MODIFIER = d.mountedReachModifier;
        CombatConstants.MOUNTED_WIND_UP_MODIFIER = d.mountedWindUpModifier;
        CombatConstants.MOUNTED_RELEASE_MODIFIER = d.mountedReleaseModifier;
        CombatConstants.MOUNTED_RECOVERY_MODIFIER = d.mountedRecoveryModifier;
        CombatConstants.MOUNTED_STAMINA_COST_MODIFIER = d.mountedStaminaCostModifier;
        CombatConstants.MOUNTED_STAMINA_REGEN_RATE_MODIFIER = d.mountedStaminaRegenRateModifier;
        CombatConstants.MOUNTED_DAMAGE_MULTIPLIER = d.mountedDamageMultiplier;
        CombatConstants.MOUNTED_CHARGE_SPEED_THRESHOLD = d.mountedChargeSpeedThreshold;
        CombatConstants.MOUNTED_CHARGE_DAMAGE_BONUS = d.mountedChargeDamageBonus;
        CombatConstants.MOUNTED_COMBAT_ENABLED = d.mountedCombatEnabled;
        CombatConstants.MOUNTED_REQUIRE_RECOGNIZED_MOUNT = d.mountedRequireRecognizedMount;

        CombatConstants.COUCH_LANCE_ENABLED = d.couchLanceEnabled;
        CombatConstants.COUCH_MIN_HORSE_SPEED = d.couchMinHorseSpeed;
        CombatConstants.COUCH_MAX_SPEED_BONUS = d.couchMaxSpeedBonus;
        CombatConstants.COUCH_DAMAGE_MULTIPLIER = d.couchDamageMultiplier;
        CombatConstants.COUCH_MOMENTUM_SPEED_RATIO = d.couchMomentumSpeedRatio;
        CombatConstants.COUCH_MOMENTUM_MULTIPLIER = d.couchMomentumMultiplier;
        CombatConstants.COUCH_MAX_DAMAGE_CAP = d.couchMaxDamageCap;
        CombatConstants.COUCH_PREPARE_TICKS = d.couchPrepareTicks;
        CombatConstants.COUCH_RECOVERY_TICKS = d.couchRecoveryTicks;
        CombatConstants.COUCH_MIN_RECOVERY_TICKS = d.couchMinRecoveryTicks;
        CombatConstants.COUCH_COOLDOWN_TICKS = d.couchCooldownTicks;
        CombatConstants.COUCH_IMPACT_FORCE = d.couchImpactForce;
        CombatConstants.COUCH_TERRAIN_CHECK_DISTANCE = d.couchTerrainCheckDistance;
        CombatConstants.COUCH_REQUIRE_DRY_TERRAIN = d.couchRequireDryTerrain;
        CombatConstants.COUCH_AI_ENGAGE_DISTANCE = d.couchAiEngageDistance;
        CombatConstants.COUCH_AI_COUCH_CHANCE = d.couchAiCouchChance;
    }

    private static BCombatConfigData captureFromConstants() {
        BCombatConfigData d = new BCombatConfigData();

        d.combatWalkSpeedModifier = CombatConstants.COMBAT_WALK_SPEED_MODIFIER;
        d.combatSprintSpeedModifier = CombatConstants.COMBAT_SPRINT_SPEED_MODIFIER;
        d.windUpSpeedModifier = CombatConstants.WIND_UP_SPEED_MODIFIER;
        d.recoverySpeedModifier = CombatConstants.RECOVERY_SPEED_MODIFIER;

        d.enterCombatTransitionTicks = CombatConstants.ENTER_COMBAT_TRANSITION_TICKS;
        d.exitCombatTransitionTicks = CombatConstants.EXIT_COMBAT_TRANSITION_TICKS;

        d.animationBlendDurationTicks = CombatConstants.ANIMATION_BLEND_DURATION_TICKS;
        d.walkAnimationSpeedThreshold = CombatConstants.WALK_ANIMATION_SPEED_THRESHOLD;
        d.runAnimationSpeedThreshold = CombatConstants.RUN_ANIMATION_SPEED_THRESHOLD;

        d.minAttackPreparationTicks = CombatConstants.MIN_ATTACK_PREPARATION_TICKS;
        d.attackReleaseDurationTicks = CombatConstants.ATTACK_RELEASE_DURATION_TICKS;
        d.recoveryDurationTicks = CombatConstants.RECOVERY_DURATION_TICKS;

        d.attackDirectionDeadzoneDegrees = CombatConstants.ATTACK_DIRECTION_DEADZONE_DEGREES;
        d.attackMouseSensitivityThreshold = CombatConstants.ATTACK_MOUSE_SENSITIVITY_THRESHOLD;
        d.attackFlickDeadzoneReliefRatio = CombatConstants.ATTACK_FLICK_DEADZONE_RELIEF_RATIO;

        d.enterBlockTransitionTicks = CombatConstants.ENTER_BLOCK_TRANSITION_TICKS;
        d.exitBlockTransitionTicks = CombatConstants.EXIT_BLOCK_TRANSITION_TICKS;

        d.guardDirectionDeadzoneDegrees = CombatConstants.GUARD_DIRECTION_DEADZONE_DEGREES;
        d.guardDirectionSensitivity = CombatConstants.GUARD_DIRECTION_SENSITIVITY;
        d.guardMouseSensitivityThreshold = CombatConstants.GUARD_MOUSE_SENSITIVITY_THRESHOLD;
        d.guardFlickDeadzoneReliefRatio = CombatConstants.GUARD_FLICK_DEADZONE_RELIEF_RATIO;
        d.guardSwitchDelayTicks = CombatConstants.GUARD_SWITCH_DELAY_TICKS;

        d.perfectBlockWindowTicks = CombatConstants.PERFECT_BLOCK_WINDOW_TICKS;
        d.parryWindowTicks = CombatConstants.PARRY_WINDOW_TICKS;
        d.perfectBlockStateDurationTicks = CombatConstants.PERFECT_BLOCK_STATE_DURATION_TICKS;
        d.parryStateDurationTicks = CombatConstants.PARRY_STATE_DURATION_TICKS;
        d.chamberWindowTicks = CombatConstants.CHAMBER_WINDOW_TICKS;
        d.chamberPrepareDurationTicks = CombatConstants.CHAMBER_PREPARE_DURATION_TICKS;
        d.chamberSuccessDurationTicks = CombatConstants.CHAMBER_SUCCESS_DURATION_TICKS;
        d.defenseAnimationBlendDurationTicks = CombatConstants.DEFENSE_ANIMATION_BLEND_DURATION_TICKS;

        d.defaultMaxStamina = CombatConstants.DEFAULT_MAX_STAMINA;
        d.staminaRegenRatePerTick = CombatConstants.STAMINA_REGEN_RATE_PER_TICK;
        d.staminaRegenDelayTicks = CombatConstants.STAMINA_REGEN_DELAY_TICKS;
        d.attackStaminaCost = CombatConstants.ATTACK_STAMINA_COST;
        d.blockEnterStaminaCost = CombatConstants.BLOCK_ENTER_STAMINA_COST;
        d.blockHoldStaminaCostPerTick = CombatConstants.BLOCK_HOLD_STAMINA_COST_PER_TICK;
        d.perfectBlockStaminaCost = CombatConstants.PERFECT_BLOCK_STAMINA_COST;
        d.parryStaminaCost = CombatConstants.PARRY_STAMINA_COST;
        d.chamberStaminaCost = CombatConstants.CHAMBER_STAMINA_COST;
        d.sprintCombatStaminaCostPerTick = CombatConstants.SPRINT_COMBAT_STAMINA_COST_PER_TICK;
        d.exhaustionRecoveryThresholdRatio = CombatConstants.EXHAUSTION_RECOVERY_THRESHOLD_RATIO;
        d.exhaustedMovementSpeedModifier = CombatConstants.EXHAUSTED_MOVEMENT_SPEED_MODIFIER;

        d.mountedReachModifier = CombatConstants.MOUNTED_REACH_MODIFIER;
        d.mountedWindUpModifier = CombatConstants.MOUNTED_WIND_UP_MODIFIER;
        d.mountedReleaseModifier = CombatConstants.MOUNTED_RELEASE_MODIFIER;
        d.mountedRecoveryModifier = CombatConstants.MOUNTED_RECOVERY_MODIFIER;
        d.mountedStaminaCostModifier = CombatConstants.MOUNTED_STAMINA_COST_MODIFIER;
        d.mountedStaminaRegenRateModifier = CombatConstants.MOUNTED_STAMINA_REGEN_RATE_MODIFIER;
        d.mountedDamageMultiplier = CombatConstants.MOUNTED_DAMAGE_MULTIPLIER;
        d.mountedChargeSpeedThreshold = CombatConstants.MOUNTED_CHARGE_SPEED_THRESHOLD;
        d.mountedChargeDamageBonus = CombatConstants.MOUNTED_CHARGE_DAMAGE_BONUS;
        d.mountedCombatEnabled = CombatConstants.MOUNTED_COMBAT_ENABLED;
        d.mountedRequireRecognizedMount = CombatConstants.MOUNTED_REQUIRE_RECOGNIZED_MOUNT;

        d.couchLanceEnabled = CombatConstants.COUCH_LANCE_ENABLED;
        d.couchMinHorseSpeed = CombatConstants.COUCH_MIN_HORSE_SPEED;
        d.couchMaxSpeedBonus = CombatConstants.COUCH_MAX_SPEED_BONUS;
        d.couchDamageMultiplier = CombatConstants.COUCH_DAMAGE_MULTIPLIER;
        d.couchMomentumSpeedRatio = CombatConstants.COUCH_MOMENTUM_SPEED_RATIO;
        d.couchMomentumMultiplier = CombatConstants.COUCH_MOMENTUM_MULTIPLIER;
        d.couchMaxDamageCap = CombatConstants.COUCH_MAX_DAMAGE_CAP;
        d.couchPrepareTicks = CombatConstants.COUCH_PREPARE_TICKS;
        d.couchRecoveryTicks = CombatConstants.COUCH_RECOVERY_TICKS;
        d.couchMinRecoveryTicks = CombatConstants.COUCH_MIN_RECOVERY_TICKS;
        d.couchCooldownTicks = CombatConstants.COUCH_COOLDOWN_TICKS;
        d.couchImpactForce = CombatConstants.COUCH_IMPACT_FORCE;
        d.couchTerrainCheckDistance = CombatConstants.COUCH_TERRAIN_CHECK_DISTANCE;
        d.couchRequireDryTerrain = CombatConstants.COUCH_REQUIRE_DRY_TERRAIN;
        d.couchAiEngageDistance = CombatConstants.COUCH_AI_ENGAGE_DISTANCE;
        d.couchAiCouchChance = CombatConstants.COUCH_AI_COUCH_CHANCE;

        return d;
    }
}