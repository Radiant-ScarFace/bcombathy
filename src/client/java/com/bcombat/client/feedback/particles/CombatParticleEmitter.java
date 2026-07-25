package com.bcombat.client.feedback.particles;

import com.bcombat.client.feedback.config.FeedbackConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Central, configurable particle-effect dispatcher for every combat
 * moment this framework decorates - attack impacts (light/heavy, flesh/
 * armor), successful block/perfect-block/parry/chamber, and heavy-attack
 * landing impacts. Every particle type used is looked up through {@link
 * ParticleSpec}, so re-skinning a specific cue never requires touching a
 * call site.
 * <p>
 * Spawns entirely through {@code ClientWorld#addParticle}, the same
 * client-only, non-networked API vanilla effects use - nothing here
 * sends a packet or touches server state.
 */
public final class CombatParticleEmitter {

    private static final Random RANDOM = new Random();

    private CombatParticleEmitter() {
        // Static utility, no instances.
    }

    /** Spawns a light attack-impact burst (weapon below the heavy-weight threshold) at {@code pos}. */
    public static void emitLightImpact(Vec3d pos) {
        if (!enabled(FeedbackConstants.IMPACT_PARTICLES_ENABLED)) {
            return;
        }
        burst(pos, ParticleTypes.CRIT, FeedbackConstants.IMPACT_PARTICLE_COUNT_LIGHT, 0.25, 0.15);
    }

    /** Spawns a heavier attack-impact burst (weapon at/above the heavy-weight threshold) at {@code pos}. */
    public static void emitHeavyImpact(Vec3d pos) {
        if (!enabled(FeedbackConstants.IMPACT_PARTICLES_ENABLED)) {
            return;
        }
        burst(pos, ParticleTypes.SWEEP_ATTACK, Math.max(1, FeedbackConstants.IMPACT_PARTICLE_COUNT_HEAVY / 6), 0.1, 0.05);
        burst(pos, ParticleTypes.CRIT, FeedbackConstants.IMPACT_PARTICLE_COUNT_HEAVY, 0.35, 0.2);
    }

    /** Spawns flesh-impact particles (armor did not meaningfully mitigate the hit) at {@code pos}. */
    public static void emitFleshImpact(Vec3d pos) {
        if (!enabled(FeedbackConstants.IMPACT_PARTICLES_ENABLED)) {
            return;
        }
        burst(pos, ParticleTypes.DAMAGE_INDICATOR, FeedbackConstants.IMPACT_PARTICLE_COUNT_LIGHT, 0.2, 0.15);
    }

    /** Spawns armor-impact particles (the hit was meaningfully mitigated by armor) at {@code pos}. */
    public static void emitArmorImpact(Vec3d pos) {
        if (!enabled(FeedbackConstants.IMPACT_PARTICLES_ENABLED)) {
            return;
        }
        burst(pos, ParticleTypes.CRIT, FeedbackConstants.IMPACT_PARTICLE_COUNT_LIGHT, 0.25, 0.15);
        burst(pos, ParticleTypes.SMOKE, Math.max(1, FeedbackConstants.IMPACT_PARTICLE_COUNT_LIGHT / 3), 0.15, 0.1);
    }

    /** Spawns critical-hit emphasis particles at {@code pos}, layered on top of the normal impact burst. */
    public static void emitCriticalHit(Vec3d pos) {
        if (!enabled(FeedbackConstants.IMPACT_PARTICLES_ENABLED)) {
            return;
        }
        burst(pos, ParticleTypes.ENCHANTED_HIT, 12, 0.3, 0.2);
        burst(pos, ParticleTypes.CRIT, 10, 0.3, 0.2);
    }

    /** Spawns a normal (non-perfect) block spark burst at {@code pos}. */
    public static void emitBlock(Vec3d pos) {
        if (!enabled(FeedbackConstants.DEFENSE_PARTICLES_ENABLED)) {
            return;
        }
        burst(pos, ParticleTypes.CRIT, FeedbackConstants.BLOCK_PARTICLE_COUNT, 0.2, 0.1);
    }

    /** Spawns a Perfect Block spark burst at {@code pos}. */
    public static void emitPerfectBlock(Vec3d pos) {
        if (!enabled(FeedbackConstants.DEFENSE_PARTICLES_ENABLED)) {
            return;
        }
        burst(pos, ParticleTypes.END_ROD, FeedbackConstants.PERFECT_BLOCK_PARTICLE_COUNT, 0.25, 0.15);
    }

    /** Spawns a Parry burst at {@code pos} - the most emphatic of the three defensive particle cues. */
    public static void emitParry(Vec3d pos) {
        if (!enabled(FeedbackConstants.DEFENSE_PARTICLES_ENABLED)) {
            return;
        }
        burst(pos, ParticleTypes.FLASH, 1, 0.0, 0.0);
        burst(pos, ParticleTypes.END_ROD, FeedbackConstants.PARRY_PARTICLE_COUNT, 0.35, 0.2);
    }

    /** Spawns a chamber-attempt-started burst at {@code pos}. */
    public static void emitChamberStarted(Vec3d pos) {
        if (!enabled(FeedbackConstants.DEFENSE_PARTICLES_ENABLED)) {
            return;
        }
        burst(pos, ParticleTypes.WITCH, Math.max(1, FeedbackConstants.CHAMBER_PARTICLE_COUNT / 2), 0.2, 0.1);
    }

    /** Spawns a chamber-succeeded burst at {@code pos}. */
    public static void emitChamberSucceeded(Vec3d pos) {
        if (!enabled(FeedbackConstants.DEFENSE_PARTICLES_ENABLED)) {
            return;
        }
        burst(pos, ParticleTypes.ENCHANTED_HIT, FeedbackConstants.CHAMBER_PARTICLE_COUNT, 0.3, 0.2);
    }

    /** Spawns a ground-level dust burst for a heavy-attack landing impact at {@code pos}. */
    public static void emitLandingImpact(Vec3d pos) {
        if (!enabled(FeedbackConstants.IMPACT_PARTICLES_ENABLED) || !FeedbackConstants.LANDING_IMPACT_ENABLED) {
            return;
        }
        burst(pos, ParticleTypes.CLOUD, FeedbackConstants.LANDING_IMPACT_PARTICLE_COUNT, 0.5, 0.05);
        burst(pos, ParticleTypes.POOF, Math.max(1, FeedbackConstants.LANDING_IMPACT_PARTICLE_COUNT / 3), 0.4, 0.05);
    }

    /** Spawns a single weapon-trail sample particle at {@code pos}. Used by {@code WeaponTrailManager}. */
    public static void emitTrailSample(Vec3d pos) {
        if (!enabled(FeedbackConstants.WEAPON_TRAILS_ENABLED)) {
            return;
        }
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return;
        }
        world.addParticle(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 0.0, 0.0, 0.0);
    }

    // ------------------------------------------------------------------

    private static boolean enabled(boolean specificFlag) {
        return FeedbackConstants.FEEDBACK_ENABLED && specificFlag;
    }

    private static void burst(Vec3d center, ParticleEffect type, int count, double spread, double speed) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null || center == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            double dx = (RANDOM.nextDouble() * 2.0 - 1.0) * spread;
            double dy = (RANDOM.nextDouble() * 2.0 - 1.0) * spread;
            double dz = (RANDOM.nextDouble() * 2.0 - 1.0) * spread;
            double vx = (RANDOM.nextDouble() * 2.0 - 1.0) * speed;
            double vy = RANDOM.nextDouble() * speed;
            double vz = (RANDOM.nextDouble() * 2.0 - 1.0) * speed;
            world.addParticle(type, center.x + dx, center.y + dy, center.z + dz, vx, vy, vz);
        }
    }

    /** Descriptor for a particle cue: currently used only as documentation of the pairing; the emit* methods above are the actual dispatch surface. */
    public record ParticleSpec(ParticleEffect type, int count, double spread, double speed) {
    }
}