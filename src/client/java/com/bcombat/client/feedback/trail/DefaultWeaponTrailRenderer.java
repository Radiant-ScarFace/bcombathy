package com.bcombat.client.feedback.trail;

import com.bcombat.client.feedback.config.FeedbackConstants;
import com.bcombat.client.feedback.particles.CombatParticleEmitter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Shipped default {@link WeaponTrailRenderer}: interpolates a small
 * particle along the line between each consecutive pair of recorded
 * trail samples, spaced by {@code FeedbackConstants#WEAPON_TRAIL_PARTICLE_SPACING}.
 * Deliberately simple - no custom rendering pipeline - so it works
 * everywhere immediately; see this class's package docs on {@link
 * WeaponTrailRenderer} for how a future phase replaces it with a real
 * ribbon/mesh trail without touching sampling code.
 */
public final class DefaultWeaponTrailRenderer implements WeaponTrailRenderer {

    @Override
    public void render(LivingEntity attacker, List<WeaponTrailManager.TrailSample> samples) {
        if (samples.size() < 2) {
            return;
        }
        double spacing = Math.max(0.02, FeedbackConstants.WEAPON_TRAIL_PARTICLE_SPACING);
        for (int i = 1; i < samples.size(); i++) {
            Vec3d from = samples.get(i - 1).position();
            Vec3d to = samples.get(i).position();
            double distance = from.distanceTo(to);
            if (distance < 1.0E-4) {
                continue;
            }
            int steps = Math.max(1, (int) Math.floor(distance / spacing));
            for (int s = 0; s <= steps; s++) {
                double t = (double) s / steps;
                Vec3d point = from.lerp(to, t);
                CombatParticleEmitter.emitTrailSample(point);
            }
        }
    }

    @Override
    public String id() {
        return "bcombat:default_particle_trail";
    }
}