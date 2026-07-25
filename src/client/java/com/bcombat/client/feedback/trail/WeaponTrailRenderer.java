package com.bcombat.client.feedback.trail;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Extension point for rendering a weapon's swing trail. {@link
 * WeaponTrailManager} owns sampling (recording where the weapon tip was
 * each tick during {@code CombatState.ATTACKING}) and lifetime
 * management; this interface owns only turning those samples into
 * something visible, so a future phase can swap in a proper shader-based
 * ribbon/mesh trail without touching sampling or lifetime code at all -
 * register a new implementation via {@link WeaponTrailManager#setRenderer}.
 * <p>
 * {@link DefaultWeaponTrailRenderer} is the shipped implementation: a
 * lightweight particle-arc trail built entirely from {@link
 * com.bcombat.client.feedback.particles.CombatParticleEmitter}, chosen
 * because it needs no custom vertex buffers or shaders and therefore
 * carries the least risk while still giving every swing a visible arc.
 */
public interface WeaponTrailRenderer {

    /**
     * Called once per client tick for every combatant currently mid-swing
     * with at least two recorded trail samples.
     *
     * @param attacker the combatant swinging.
     * @param samples  the currently live trail samples, oldest first, each
     *                 paired with its remaining fade lifetime in ticks.
     */
    void render(LivingEntity attacker, List<WeaponTrailManager.TrailSample> samples);

    /**
     * @return a short, unique identifier for this renderer implementation,
     * for diagnostics/config only.
     */
    String id();

    /** @return an approximate "hold point" in {@code attacker}'s hand this tick, for sampling. */
    static Vec3d approximateWeaponTipPosition(LivingEntity attacker) {
        Vec3d eyePos = attacker.getEyePos();
        Vec3d look = attacker.getRotationVec(1.0f);
        Vec3d right = new Vec3d(-look.z, 0.0, look.x).normalize();
        return eyePos
                .add(look.multiply(1.2))
                .add(right.multiply(0.4))
                .subtract(0.0, 0.4, 0.0);
    }
}