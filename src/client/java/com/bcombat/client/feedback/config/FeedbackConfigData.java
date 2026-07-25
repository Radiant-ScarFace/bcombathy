package com.bcombat.client.feedback.config;

/**
 * Plain-data mirror of every tunable field in {@link FeedbackConstants},
 * serialized to/from {@code config/bcombat-feedback.json} by {@link
 * FeedbackConfig} — the client-only, presentation-layer counterpart of
 * {@code com.bcombat.config.BCombatConfigData}. Kept as a separate file
 * from the common {@code bcombat.json} config deliberately: this data
 * only ever needs to exist on a client install (a dedicated server has
 * no rendering, sound, or HUD to configure), and separating it means a
 * server operator's {@code bcombat.json} is never cluttered with
 * client-only presentation knobs.
 * <p>
 * Every field initializes to {@link FeedbackConstants}'s own current
 * default, exactly mirroring {@code BCombatConfigData}'s round-trip
 * guarantee.
 */
public final class FeedbackConfigData {

    public boolean feedbackEnabled = FeedbackConstants.FEEDBACK_ENABLED;
    public boolean hitStopEnabled = FeedbackConstants.HIT_STOP_ENABLED;
    public boolean cameraShakeEnabled = FeedbackConstants.CAMERA_SHAKE_ENABLED;
    public boolean weaponTrailsEnabled = FeedbackConstants.WEAPON_TRAILS_ENABLED;
    public boolean impactParticlesEnabled = FeedbackConstants.IMPACT_PARTICLES_ENABLED;
    public boolean defenseParticlesEnabled = FeedbackConstants.DEFENSE_PARTICLES_ENABLED;
    public boolean combatSoundsEnabled = FeedbackConstants.COMBAT_SOUNDS_ENABLED;
    public boolean screenFlashEnabled = FeedbackConstants.SCREEN_FLASH_ENABLED;
    public boolean hitDirectionIndicatorEnabled = FeedbackConstants.HIT_DIRECTION_INDICATOR_ENABLED;
    public boolean exhaustionVignetteEnabled = FeedbackConstants.EXHAUSTION_VIGNETTE_ENABLED;
    public boolean debugVisualizationEnabled = FeedbackConstants.DEBUG_VISUALIZATION_ENABLED;

    public int hitStopNormalTicks = FeedbackConstants.HIT_STOP_NORMAL_TICKS;
    public int hitStopHeavyTicks = FeedbackConstants.HIT_STOP_HEAVY_TICKS;
    public int hitStopCriticalTicks = FeedbackConstants.HIT_STOP_CRITICAL_TICKS;
    public int hitStopDefenseSuccessTicks = FeedbackConstants.HIT_STOP_DEFENSE_SUCCESS_TICKS;
    public int hitStopAttackDeniedTicks = FeedbackConstants.HIT_STOP_ATTACK_DENIED_TICKS;

    public double cameraShakeNormalTrauma = FeedbackConstants.CAMERA_SHAKE_NORMAL_TRAUMA;
    public double cameraShakeHeavyTrauma = FeedbackConstants.CAMERA_SHAKE_HEAVY_TRAUMA;
    public double cameraShakeCriticalTrauma = FeedbackConstants.CAMERA_SHAKE_CRITICAL_TRAUMA;
    public double cameraShakeDefenseTrauma = FeedbackConstants.CAMERA_SHAKE_DEFENSE_TRAUMA;
    public double cameraShakeTakenHitTrauma = FeedbackConstants.CAMERA_SHAKE_TAKEN_HIT_TRAUMA;
    public double cameraShakeMaxAngleDegrees = FeedbackConstants.CAMERA_SHAKE_MAX_ANGLE_DEGREES;
    public double cameraShakeDecayPerTick = FeedbackConstants.CAMERA_SHAKE_DECAY_PER_TICK;
    public double cameraShakeFrequency = FeedbackConstants.CAMERA_SHAKE_FREQUENCY;

    public int weaponTrailSampleCount = FeedbackConstants.WEAPON_TRAIL_SAMPLE_COUNT;
    public int weaponTrailFadeTicks = FeedbackConstants.WEAPON_TRAIL_FADE_TICKS;
    public double weaponTrailParticleSpacing = FeedbackConstants.WEAPON_TRAIL_PARTICLE_SPACING;

    public int impactParticleCountLight = FeedbackConstants.IMPACT_PARTICLE_COUNT_LIGHT;
    public int impactParticleCountHeavy = FeedbackConstants.IMPACT_PARTICLE_COUNT_HEAVY;
    public int blockParticleCount = FeedbackConstants.BLOCK_PARTICLE_COUNT;
    public int perfectBlockParticleCount = FeedbackConstants.PERFECT_BLOCK_PARTICLE_COUNT;
    public int parryParticleCount = FeedbackConstants.PARRY_PARTICLE_COUNT;
    public int chamberParticleCount = FeedbackConstants.CHAMBER_PARTICLE_COUNT;
    public int landingImpactParticleCount = FeedbackConstants.LANDING_IMPACT_PARTICLE_COUNT;

    public double heavyWeaponWeightThreshold = FeedbackConstants.HEAVY_WEAPON_WEIGHT_THRESHOLD;
    public double armorImpactMinReduction = FeedbackConstants.ARMOR_IMPACT_MIN_REDUCTION;

    public boolean landingImpactEnabled = FeedbackConstants.LANDING_IMPACT_ENABLED;
    public double landingImpactShakeTrauma = FeedbackConstants.LANDING_IMPACT_SHAKE_TRAUMA;
    public int landingImpactHitStopTicks = FeedbackConstants.LANDING_IMPACT_HIT_STOP_TICKS;

    public float exhaustionVignetteMaxAlpha = FeedbackConstants.EXHAUSTION_VIGNETTE_MAX_ALPHA;
    public int exhaustionVignetteFadeTicks = FeedbackConstants.EXHAUSTION_VIGNETTE_FADE_TICKS;

    public int screenFlashDurationTicks = FeedbackConstants.SCREEN_FLASH_DURATION_TICKS;
    public float screenFlashSuccessAlpha = FeedbackConstants.SCREEN_FLASH_SUCCESS_ALPHA;
    public float screenFlashFailureAlpha = FeedbackConstants.SCREEN_FLASH_FAILURE_ALPHA;

    public int hitDirectionIndicatorDurationTicks = FeedbackConstants.HIT_DIRECTION_INDICATOR_DURATION_TICKS;

    public double debugHitboxExpansion = FeedbackConstants.DEBUG_HITBOX_EXPANSION;
    public int debugVisualizationRangeBlocks = FeedbackConstants.DEBUG_VISUALIZATION_RANGE_BLOCKS;

    public float soundVolumeMaster = FeedbackConstants.SOUND_VOLUME_MASTER;
    public float soundPitchVariance = FeedbackConstants.SOUND_PITCH_VARIANCE;
}