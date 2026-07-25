package com.bcombat.combat.collision;

import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.util.CombatConstants;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Stateless geometry utility that answers two questions for the collision
 * framework: "did this swing find a valid target" ({@link #findTarget})
 * and "roughly where did it land" ({@link #classifyHitLocation}). Has no
 * knowledge of {@code CombatState}, timing windows, or blocking — those
 * are {@code CollisionController}'s and {@code CombatController}'s
 * responsibility respectively, the same separation of concerns
 * {@code AttackDirectionTracker} (classifies) vs {@code BlockController}
 * (decides) already uses elsewhere in this framework.
 * <p>
 * Detection is a distance + forward-cone check against nearby
 * {@link LivingEntity} bounding-box centers rather than a precise
 * per-frame swept hitbox — a deliberate, documented approximation
 * appropriate for "does this swing connect", not a physics simulation.
 * Every tunable value it reads lives in {@link CombatConstants}, and
 * weapon reach is read entirely from the caller-supplied {@code
 * WeaponProperties#reach()} — nothing here hardcodes a distance.
 */
public final class CollisionDetector {

    private CollisionDetector() {
        // Stateless utility, no instances.
    }

    /**
     * Searches for the nearest valid attack target in front of {@code
     * attacker}, within {@code weaponReach} (scaled by {@link
     * CombatConstants#DEFAULT_WEAPON_REACH_MODIFIER} and padded by
     * {@link CombatConstants#COLLISION_REACH_TOLERANCE}) and inside the
     * forward cone defined by {@link CombatConstants#COLLISION_CONE_HALF_ANGLE_DEGREES}.
     *
     * @return the closest qualifying {@link LivingEntity}, or {@code null} if none qualify.
     */
    public static LivingEntity findTarget(PlayerEntity attacker, double weaponReach) {
        World world = attacker.getWorld();
        double range = Math.max(0.0, weaponReach) * CombatConstants.DEFAULT_WEAPON_REACH_MODIFIER
                + CombatConstants.COLLISION_REACH_TOLERANCE;

        Vec3d eyePos = attacker.getEyePos();
        Vec3d lookVec = attacker.getRotationVec(1.0f).normalize();

        Box searchBox = attacker.getBoundingBox().expand(range);
        List<Entity> candidates = world.getOtherEntities(attacker, searchBox, CollisionDetector::isValidCandidate);

        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity candidate : candidates) {
            Vec3d toCandidate = candidate.getBoundingBox().getCenter().subtract(eyePos);
            double distance = toCandidate.length();
            if (distance > range || distance <= 1.0E-4) {
                continue;
            }

            double cosAngle = lookVec.dotProduct(toCandidate.normalize());
            double angleDegrees = Math.toDegrees(Math.acos(clamp(cosAngle, -1.0, 1.0)));
            if (angleDegrees > CombatConstants.COLLISION_CONE_HALF_ANGLE_DEGREES) {
                continue;
            }

            if (distance < closestDistance) {
                closestDistance = distance;
                closest = (LivingEntity) candidate;
            }
        }

        return closest;
    }

    private static boolean isValidCandidate(Entity entity) {
        if (!(entity instanceof LivingEntity) || !entity.isAlive()) {
            return false;
        }
        return !(entity instanceof PlayerEntity player && player.isSpectator());
    }

    /**
     * Approximates which body region a confirmed hit landed on, from the
     * attacker's eye height and look direction relative to {@code
     * target}'s hitbox — not a real per-limb hitbox (see class docs).
     * <p>
     * Vertical placement is the primary signal, adjusted by a small
     * per-direction bias (an overhead strike lands higher than a thrust,
     * for example) via {@link CombatConstants}. Within the resulting
     * torso-height band, lateral offset from the attacker's look line
     * distinguishes a central ({@link HitLocation#TORSO}) hit from one
     * that landed toward the target's side ({@link HitLocation#ARMS}).
     */
    public static HitLocation classifyHitLocation(PlayerEntity attacker, LivingEntity target, AttackDirection direction) {
        double targetHeight = Math.max(target.getHeight(), 0.1);
        double rawFraction = (attacker.getEyeY() - target.getY()) / targetHeight;
        double biasedFraction = clamp(rawFraction + heightBiasFor(direction), 0.0, 1.0);

        if (biasedFraction >= CombatConstants.HEAD_HITBOX_HEIGHT_RATIO) {
            return HitLocation.HEAD;
        }
        if (biasedFraction <= CombatConstants.LEG_HITBOX_HEIGHT_RATIO) {
            return HitLocation.LEGS;
        }

        double lateralOffsetFraction = lateralOffsetFraction(attacker, target);
        return lateralOffsetFraction >= CombatConstants.ARM_HITBOX_WIDTH_RATIO ? HitLocation.ARMS : HitLocation.TORSO;
    }

    private static double heightBiasFor(AttackDirection direction) {
        return switch (direction) {
            case OVERHEAD -> CombatConstants.OVERHEAD_HIT_HEIGHT_BIAS;
            case LEFT_SLASH, RIGHT_SLASH -> CombatConstants.SLASH_HIT_HEIGHT_BIAS;
            case THRUST, NONE -> CombatConstants.THRUST_HIT_HEIGHT_BIAS;
        };
    }

    /**
     * @return the target's horizontal displacement from the attacker's
     * look line, normalized by half the target's width, so values near
     * 0 are dead-center and values near/above 1 are at the target's
     * silhouette edge.
     */
    private static double lateralOffsetFraction(PlayerEntity attacker, LivingEntity target) {
        Vec3d eyePos = attacker.getEyePos();
        Vec3d lookVec = attacker.getRotationVec(1.0f);

        Vec3d lookHorizontal = new Vec3d(lookVec.x, 0.0, lookVec.z);
        if (lookHorizontal.lengthSquared() < 1.0E-6) {
            return 0.0;
        }
        lookHorizontal = lookHorizontal.normalize();

        Vec3d toTargetHorizontal = new Vec3d(target.getX() - eyePos.x, 0.0, target.getZ() - eyePos.z);
        double forwardComponent = toTargetHorizontal.dotProduct(lookHorizontal);
        Vec3d lateralVector = toTargetHorizontal.subtract(lookHorizontal.multiply(forwardComponent));

        double halfWidth = Math.max(target.getWidth() / 2.0, 0.1);
        return lateralVector.length() / halfWidth;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}