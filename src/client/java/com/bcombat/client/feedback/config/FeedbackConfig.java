package com.bcombat.client.feedback.config;

import com.bcombat.BannerlordCombat;
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
 * Loads and saves {@code config/bcombat-feedback.json}, the client-only
 * counterpart to {@code com.bcombat.config.BCombatConfig}. Same
 * contract, scoped to presentation only: this is the ONLY class that
 * reads or writes that file, and the ONLY class that mutates {@link
 * FeedbackConstants}'s fields after startup.
 * <p>
 * {@link #load()} must be called once from {@code
 * BannerlordCombatClient#onInitializeClient()}, before any feedback
 * subsystem is registered. {@link #reload()} is safe to call at any
 * point afterward and takes effect immediately, since every consumer
 * re-reads {@link FeedbackConstants} fields live rather than caching
 * them.
 */
public final class FeedbackConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "bcombat-feedback.json";

    private static Path configPath;

    private FeedbackConfig() {
        // Static utility, no instances.
    }

    /**
     * Loads {@code config/bcombat-feedback.json} into {@link
     * FeedbackConstants}, creating the file with current (compiled-in
     * default) values if it doesn't exist yet.
     */
    public static void load() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
        FeedbackConfigData data = readFromDisk();
        applyToConstants(data);
        writeToDisk(captureFromConstants());
        BannerlordCombat.LOGGER.info("[bcombat] Loaded combat feedback configuration from {}", configPath);
    }

    /** Re-reads {@code config/bcombat-feedback.json} from disk and re-applies it. */
    public static void reload() {
        if (configPath == null) {
            load();
            return;
        }
        FeedbackConfigData data = readFromDisk();
        applyToConstants(data);
        BannerlordCombat.LOGGER.info("[bcombat] Reloaded combat feedback configuration from {}", configPath);
    }

    /** Writes {@link FeedbackConstants}'s current values back out to disk. */
    public static void save() {
        if (configPath == null) {
            configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
        }
        writeToDisk(captureFromConstants());
    }

    public static Path getConfigPath() {
        return configPath;
    }

    // ------------------------------------------------------------------
    // Disk I/O
    // ------------------------------------------------------------------

    private static FeedbackConfigData readFromDisk() {
        if (!Files.exists(configPath)) {
            return new FeedbackConfigData();
        }
        try (Reader reader = Files.newBufferedReader(configPath)) {
            FeedbackConfigData data = GSON.fromJson(reader, FeedbackConfigData.class);
            return data != null ? data : new FeedbackConfigData();
        } catch (IOException | JsonSyntaxException e) {
            BannerlordCombat.LOGGER.warn("[bcombat] Failed to read {}, falling back to defaults", configPath, e);
            return new FeedbackConfigData();
        }
    }

    private static void writeToDisk(FeedbackConfigData data) {
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
    // Explicit two-way mapping - FeedbackConfigData <-> FeedbackConstants
    // ------------------------------------------------------------------

    private static void applyToConstants(FeedbackConfigData d) {
        FeedbackConstants.FEEDBACK_ENABLED = d.feedbackEnabled;
        FeedbackConstants.HIT_STOP_ENABLED = d.hitStopEnabled;
        FeedbackConstants.CAMERA_SHAKE_ENABLED = d.cameraShakeEnabled;
        FeedbackConstants.WEAPON_TRAILS_ENABLED = d.weaponTrailsEnabled;
        FeedbackConstants.IMPACT_PARTICLES_ENABLED = d.impactParticlesEnabled;
        FeedbackConstants.DEFENSE_PARTICLES_ENABLED = d.defenseParticlesEnabled;
        FeedbackConstants.COMBAT_SOUNDS_ENABLED = d.combatSoundsEnabled;
        FeedbackConstants.SCREEN_FLASH_ENABLED = d.screenFlashEnabled;
        FeedbackConstants.HIT_DIRECTION_INDICATOR_ENABLED = d.hitDirectionIndicatorEnabled;
        FeedbackConstants.EXHAUSTION_VIGNETTE_ENABLED = d.exhaustionVignetteEnabled;
        FeedbackConstants.DEBUG_VISUALIZATION_ENABLED = d.debugVisualizationEnabled;

        FeedbackConstants.HIT_STOP_NORMAL_TICKS = d.hitStopNormalTicks;
        FeedbackConstants.HIT_STOP_HEAVY_TICKS = d.hitStopHeavyTicks;
        FeedbackConstants.HIT_STOP_CRITICAL_TICKS = d.hitStopCriticalTicks;
        FeedbackConstants.HIT_STOP_DEFENSE_SUCCESS_TICKS = d.hitStopDefenseSuccessTicks;
        FeedbackConstants.HIT_STOP_ATTACK_DENIED_TICKS = d.hitStopAttackDeniedTicks;

        FeedbackConstants.CAMERA_SHAKE_NORMAL_TRAUMA = d.cameraShakeNormalTrauma;
        FeedbackConstants.CAMERA_SHAKE_HEAVY_TRAUMA = d.cameraShakeHeavyTrauma;
        FeedbackConstants.CAMERA_SHAKE_CRITICAL_TRAUMA = d.cameraShakeCriticalTrauma;
        FeedbackConstants.CAMERA_SHAKE_DEFENSE_TRAUMA = d.cameraShakeDefenseTrauma;
        FeedbackConstants.CAMERA_SHAKE_TAKEN_HIT_TRAUMA = d.cameraShakeTakenHitTrauma;
        FeedbackConstants.CAMERA_SHAKE_MAX_ANGLE_DEGREES = d.cameraShakeMaxAngleDegrees;
        FeedbackConstants.CAMERA_SHAKE_DECAY_PER_TICK = d.cameraShakeDecayPerTick;
        FeedbackConstants.CAMERA_SHAKE_FREQUENCY = d.cameraShakeFrequency;

        FeedbackConstants.WEAPON_TRAIL_SAMPLE_COUNT = d.weaponTrailSampleCount;
        FeedbackConstants.WEAPON_TRAIL_FADE_TICKS = d.weaponTrailFadeTicks;
        FeedbackConstants.WEAPON_TRAIL_PARTICLE_SPACING = d.weaponTrailParticleSpacing;

        FeedbackConstants.IMPACT_PARTICLE_COUNT_LIGHT = d.impactParticleCountLight;
        FeedbackConstants.IMPACT_PARTICLE_COUNT_HEAVY = d.impactParticleCountHeavy;
        FeedbackConstants.BLOCK_PARTICLE_COUNT = d.blockParticleCount;
        FeedbackConstants.PERFECT_BLOCK_PARTICLE_COUNT = d.perfectBlockParticleCount;
        FeedbackConstants.PARRY_PARTICLE_COUNT = d.parryParticleCount;
        FeedbackConstants.CHAMBER_PARTICLE_COUNT = d.chamberParticleCount;
        FeedbackConstants.LANDING_IMPACT_PARTICLE_COUNT = d.landingImpactParticleCount;

        FeedbackConstants.HEAVY_WEAPON_WEIGHT_THRESHOLD = d.heavyWeaponWeightThreshold;
        FeedbackConstants.ARMOR_IMPACT_MIN_REDUCTION = d.armorImpactMinReduction;

        FeedbackConstants.LANDING_IMPACT_ENABLED = d.landingImpactEnabled;
        FeedbackConstants.LANDING_IMPACT_SHAKE_TRAUMA = d.landingImpactShakeTrauma;
        FeedbackConstants.LANDING_IMPACT_HIT_STOP_TICKS = d.landingImpactHitStopTicks;

        FeedbackConstants.EXHAUSTION_VIGNETTE_MAX_ALPHA = d.exhaustionVignetteMaxAlpha;
        FeedbackConstants.EXHAUSTION_VIGNETTE_FADE_TICKS = d.exhaustionVignetteFadeTicks;

        FeedbackConstants.SCREEN_FLASH_DURATION_TICKS = d.screenFlashDurationTicks;
        FeedbackConstants.SCREEN_FLASH_SUCCESS_ALPHA = d.screenFlashSuccessAlpha;
        FeedbackConstants.SCREEN_FLASH_FAILURE_ALPHA = d.screenFlashFailureAlpha;

        FeedbackConstants.HIT_DIRECTION_INDICATOR_DURATION_TICKS = d.hitDirectionIndicatorDurationTicks;

        FeedbackConstants.DEBUG_HITBOX_EXPANSION = d.debugHitboxExpansion;
        FeedbackConstants.DEBUG_VISUALIZATION_RANGE_BLOCKS = d.debugVisualizationRangeBlocks;

        FeedbackConstants.SOUND_VOLUME_MASTER = d.soundVolumeMaster;
        FeedbackConstants.SOUND_PITCH_VARIANCE = d.soundPitchVariance;
    }

    private static FeedbackConfigData captureFromConstants() {
        FeedbackConfigData d = new FeedbackConfigData();

        d.feedbackEnabled = FeedbackConstants.FEEDBACK_ENABLED;
        d.hitStopEnabled = FeedbackConstants.HIT_STOP_ENABLED;
        d.cameraShakeEnabled = FeedbackConstants.CAMERA_SHAKE_ENABLED;
        d.weaponTrailsEnabled = FeedbackConstants.WEAPON_TRAILS_ENABLED;
        d.impactParticlesEnabled = FeedbackConstants.IMPACT_PARTICLES_ENABLED;
        d.defenseParticlesEnabled = FeedbackConstants.DEFENSE_PARTICLES_ENABLED;
        d.combatSoundsEnabled = FeedbackConstants.COMBAT_SOUNDS_ENABLED;
        d.screenFlashEnabled = FeedbackConstants.SCREEN_FLASH_ENABLED;
        d.hitDirectionIndicatorEnabled = FeedbackConstants.HIT_DIRECTION_INDICATOR_ENABLED;
        d.exhaustionVignetteEnabled = FeedbackConstants.EXHAUSTION_VIGNETTE_ENABLED;
        d.debugVisualizationEnabled = FeedbackConstants.DEBUG_VISUALIZATION_ENABLED;

        d.hitStopNormalTicks = FeedbackConstants.HIT_STOP_NORMAL_TICKS;
        d.hitStopHeavyTicks = FeedbackConstants.HIT_STOP_HEAVY_TICKS;
        d.hitStopCriticalTicks = FeedbackConstants.HIT_STOP_CRITICAL_TICKS;
        d.hitStopDefenseSuccessTicks = FeedbackConstants.HIT_STOP_DEFENSE_SUCCESS_TICKS;
        d.hitStopAttackDeniedTicks = FeedbackConstants.HIT_STOP_ATTACK_DENIED_TICKS;

        d.cameraShakeNormalTrauma = FeedbackConstants.CAMERA_SHAKE_NORMAL_TRAUMA;
        d.cameraShakeHeavyTrauma = FeedbackConstants.CAMERA_SHAKE_HEAVY_TRAUMA;
        d.cameraShakeCriticalTrauma = FeedbackConstants.CAMERA_SHAKE_CRITICAL_TRAUMA;
        d.cameraShakeDefenseTrauma = FeedbackConstants.CAMERA_SHAKE_DEFENSE_TRAUMA;
        d.cameraShakeTakenHitTrauma = FeedbackConstants.CAMERA_SHAKE_TAKEN_HIT_TRAUMA;
        d.cameraShakeMaxAngleDegrees = FeedbackConstants.CAMERA_SHAKE_MAX_ANGLE_DEGREES;
        d.cameraShakeDecayPerTick = FeedbackConstants.CAMERA_SHAKE_DECAY_PER_TICK;
        d.cameraShakeFrequency = FeedbackConstants.CAMERA_SHAKE_FREQUENCY;

        d.weaponTrailSampleCount = FeedbackConstants.WEAPON_TRAIL_SAMPLE_COUNT;
        d.weaponTrailFadeTicks = FeedbackConstants.WEAPON_TRAIL_FADE_TICKS;
        d.weaponTrailParticleSpacing = FeedbackConstants.WEAPON_TRAIL_PARTICLE_SPACING;

        d.impactParticleCountLight = FeedbackConstants.IMPACT_PARTICLE_COUNT_LIGHT;
        d.impactParticleCountHeavy = FeedbackConstants.IMPACT_PARTICLE_COUNT_HEAVY;
        d.blockParticleCount = FeedbackConstants.BLOCK_PARTICLE_COUNT;
        d.perfectBlockParticleCount = FeedbackConstants.PERFECT_BLOCK_PARTICLE_COUNT;
        d.parryParticleCount = FeedbackConstants.PARRY_PARTICLE_COUNT;
        d.chamberParticleCount = FeedbackConstants.CHAMBER_PARTICLE_COUNT;
        d.landingImpactParticleCount = FeedbackConstants.LANDING_IMPACT_PARTICLE_COUNT;

        d.heavyWeaponWeightThreshold = FeedbackConstants.HEAVY_WEAPON_WEIGHT_THRESHOLD;
        d.armorImpactMinReduction = FeedbackConstants.ARMOR_IMPACT_MIN_REDUCTION;

        d.landingImpactEnabled = FeedbackConstants.LANDING_IMPACT_ENABLED;
        d.landingImpactShakeTrauma = FeedbackConstants.LANDING_IMPACT_SHAKE_TRAUMA;
        d.landingImpactHitStopTicks = FeedbackConstants.LANDING_IMPACT_HIT_STOP_TICKS;

        d.exhaustionVignetteMaxAlpha = FeedbackConstants.EXHAUSTION_VIGNETTE_MAX_ALPHA;
        d.exhaustionVignetteFadeTicks = FeedbackConstants.EXHAUSTION_VIGNETTE_FADE_TICKS;

        d.screenFlashDurationTicks = FeedbackConstants.SCREEN_FLASH_DURATION_TICKS;
        d.screenFlashSuccessAlpha = FeedbackConstants.SCREEN_FLASH_SUCCESS_ALPHA;
        d.screenFlashFailureAlpha = FeedbackConstants.SCREEN_FLASH_FAILURE_ALPHA;

        d.hitDirectionIndicatorDurationTicks = FeedbackConstants.HIT_DIRECTION_INDICATOR_DURATION_TICKS;

        d.debugHitboxExpansion = FeedbackConstants.DEBUG_HITBOX_EXPANSION;
        d.debugVisualizationRangeBlocks = FeedbackConstants.DEBUG_VISUALIZATION_RANGE_BLOCKS;

        d.soundVolumeMaster = FeedbackConstants.SOUND_VOLUME_MASTER;
        d.soundPitchVariance = FeedbackConstants.SOUND_PITCH_VARIANCE;

        return d;
    }
}