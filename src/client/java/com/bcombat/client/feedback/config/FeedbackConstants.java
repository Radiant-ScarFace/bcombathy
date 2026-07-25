package com.bcombat.client.feedback.config;

/**
 * Every tunable value the Combat Effects &amp; Feedback Framework reads,
 * mirroring the role {@code com.bcombat.combat.util.CombatConstants}
 * plays for core combat: every feedback subsystem (hit stop, camera
 * shake, particles, sound, HUD flashes, debug visualization) reads a
 * {@code public static} field here instead of hardcoding a value, and
 * {@link FeedbackConfig} is the only class that mutates these fields
 * after startup.
 * <p>
 * This is entirely client-side and entirely presentational — nothing
 * here is read by any class under {@code com.bcombat.combat}, and
 * nothing here ever mutates combat state. Toggling every field to its
 * most inert value (durations at 0, intensities at 0, every {@code
 * *Enabled} flag false) reduces this framework to a complete no-op
 * without touching a single combat class.
 */
public final class FeedbackConstants {

    private FeedbackConstants() {
        // Static tunable holder, no instances.
    }

    // ---------------- Master toggles ----------------
    public static boolean FEEDBACK_ENABLED = true;
    public static boolean HIT_STOP_ENABLED = true;
    public static boolean CAMERA_SHAKE_ENABLED = true;
    public static boolean WEAPON_TRAILS_ENABLED = true;
    public static boolean IMPACT_PARTICLES_ENABLED = true;
    public static boolean DEFENSE_PARTICLES_ENABLED = true;
    public static boolean COMBAT_SOUNDS_ENABLED = true;
    public static boolean SCREEN_FLASH_ENABLED = true;
    public static boolean HIT_DIRECTION_INDICATOR_ENABLED = true;
    public static boolean EXHAUSTION_VIGNETTE_ENABLED = true;
    public static boolean DEBUG_VISUALIZATION_ENABLED = false;

    // ---------------- Hit stop (freeze frames) ----------------
    /** Ticks the render loop's interpolation is frozen for a normal confirmed hit. */
    public static int HIT_STOP_NORMAL_TICKS = 2;
    /** Ticks frozen for a heavy-weapon hit (see {@link #HEAVY_WEAPON_WEIGHT_THRESHOLD}). */
    public static int HIT_STOP_HEAVY_TICKS = 4;
    /** Ticks frozen for a critical hit; stacks are not additive, the largest applicable value wins. */
    public static int HIT_STOP_CRITICAL_TICKS = 5;
    /** Ticks frozen for a Perfect Block / Parry / successful Chamber. */
    public static int HIT_STOP_DEFENSE_SUCCESS_TICKS = 3;
    /** Ticks frozen when the local player's own attack is blocked/parried/chambered by a foe. */
    public static int HIT_STOP_ATTACK_DENIED_TICKS = 2;

    // ---------------- Camera shake ----------------
    /** Base shake trauma (0..1) added for a normal confirmed hit. */
    public static double CAMERA_SHAKE_NORMAL_TRAUMA = 0.25;
    /** Shake trauma added for a heavy-weapon hit. */
    public static double CAMERA_SHAKE_HEAVY_TRAUMA = 0.45;
    /** Shake trauma added for a critical hit. */
    public static double CAMERA_SHAKE_CRITICAL_TRAUMA = 0.6;
    /** Shake trauma added for a Perfect Block / Parry. */
    public static double CAMERA_SHAKE_DEFENSE_TRAUMA = 0.3;
    /** Shake trauma added when the local player is struck (defender-side shake). */
    public static double CAMERA_SHAKE_TAKEN_HIT_TRAUMA = 0.35;
    /** Maximum pitch/yaw offset in degrees at full (1.0) trauma. */
    public static double CAMERA_SHAKE_MAX_ANGLE_DEGREES = 4.0;
    /** Trauma decay per tick (trauma -= this value every tick). */
    public static double CAMERA_SHAKE_DECAY_PER_TICK = 0.05;
    /** Shake frequency multiplier; higher values shake faster/jitterier. */
    public static double CAMERA_SHAKE_FREQUENCY = 1.6;

    // ---------------- Weapon trails ----------------
    /** How many recent swing-sample positions the trail keeps before the oldest ages out. */
    public static int WEAPON_TRAIL_SAMPLE_COUNT = 8;
    /** Ticks a single trail sample stays visible before fully fading. */
    public static int WEAPON_TRAIL_FADE_TICKS = 6;
    /** Trail particle spacing along the sampled swing arc; lower is denser. */
    public static double WEAPON_TRAIL_PARTICLE_SPACING = 0.15;

    // ---------------- Impact particles ----------------
    public static int IMPACT_PARTICLE_COUNT_LIGHT = 6;
    public static int IMPACT_PARTICLE_COUNT_HEAVY = 14;
    public static int BLOCK_PARTICLE_COUNT = 8;
    public static int PERFECT_BLOCK_PARTICLE_COUNT = 14;
    public static int PARRY_PARTICLE_COUNT = 18;
    public static int CHAMBER_PARTICLE_COUNT = 12;
    public static int LANDING_IMPACT_PARTICLE_COUNT = 20;

    // ---------------- Heavy vs light / flesh vs armor thresholds ----------------
    /** {@code WeaponProperties#weight()} at/above which a hit is classified "heavy" impact feedback. */
    public static double HEAVY_WEAPON_WEIGHT_THRESHOLD = 1.4;
    /** Minimum {@code DamageResult#armorReductionAmount()} to classify a hit as an armor impact rather than flesh. */
    public static double ARMOR_IMPACT_MIN_REDUCTION = 0.5;

    // ---------------- Landing impact (heavy attacks) ----------------
    public static boolean LANDING_IMPACT_ENABLED = true;
    public static double LANDING_IMPACT_SHAKE_TRAUMA = 0.4;
    public static int LANDING_IMPACT_HIT_STOP_TICKS = 3;

    // ---------------- Stamina exhaustion feedback ----------------
    public static float EXHAUSTION_VIGNETTE_MAX_ALPHA = 0.35f;
    public static int EXHAUSTION_VIGNETTE_FADE_TICKS = 20;

    // ---------------- Screen flash (defensive actions) ----------------
    public static int SCREEN_FLASH_DURATION_TICKS = 8;
    public static float SCREEN_FLASH_SUCCESS_ALPHA = 0.18f;
    public static float SCREEN_FLASH_FAILURE_ALPHA = 0.22f;

    // ---------------- Hit direction indicator ----------------
    public static int HIT_DIRECTION_INDICATOR_DURATION_TICKS = 25;

    // ---------------- Debug visualization ----------------
    public static double DEBUG_HITBOX_EXPANSION = 0.05;
    public static int DEBUG_VISUALIZATION_RANGE_BLOCKS = 24;

    // ---------------- Sound volume/pitch defaults ----------------
    public static float SOUND_VOLUME_MASTER = 1.0f;
    public static float SOUND_PITCH_VARIANCE = 0.1f;
}