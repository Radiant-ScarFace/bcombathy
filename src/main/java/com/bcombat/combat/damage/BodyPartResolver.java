package com.bcombat.combat.damage;

import com.bcombat.combat.collision.HitLocation;
import com.bcombat.combat.collision.HitResult;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Stateless utility that turns a confirmed hit's coarse {@link
 * HitLocation} (as classified by {@code
 * com.bcombat.combat.collision.CollisionDetector}) into a full six-way
 * {@link BodyPart}, by additionally determining which side of the
 * target — left or right, from the attacker's perspective — the hit
 * landed on.
 * <p>
 * This class deliberately does not duplicate {@code
 * CollisionDetector#classifyHitLocation}'s vertical-placement or
 * lateral-<em>magnitude</em> logic (head/torso/leg height bands, or
 * torso-vs-arms width threshold) — it only ever reads the already
 * resolved {@link HitLocation} for that. The one piece of new geometry
 * here is the lateral <em>sign</em> (which side, not how far off-center)
 * since {@code classifyHitLocation} never needed to distinguish a
 * target's left from their right and so never computed it. Keeping that
 * narrowly-scoped calculation here, rather than adding it to {@code
 * CollisionDetector}, leaves the collision framework's existing,
 * documented approximation untouched — exactly the separation the
 * damage framework is required to maintain from hit detection.
 */
public final class BodyPartResolver {

    private BodyPartResolver() {
        // Stateless utility, no instances.
    }

    /**
     * @return the resolved {@link BodyPart} for {@code hitResult}, or
     * {@link BodyPart#UNKNOWN} if {@code hitResult} is not a confirmed,
     * unblocked hit (see {@link HitResult#hit()}) or its {@link
     * HitResult#hitLocation()} could not be classified.
     */
    public static BodyPart resolve(HitResult hitResult) {
        if (hitResult == null || !hitResult.hit() || hitResult.target() == null) {
            return BodyPart.UNKNOWN;
        }
        return resolve(hitResult.attacker(), hitResult.target(), hitResult.hitLocation());
    }

    /**
     * @return the resolved {@link BodyPart} for a hit already classified
     * as {@code location} by the collision framework.
     */
    public static BodyPart resolve(PlayerEntity attacker, LivingEntity target, HitLocation location) {
        if (attacker == null || target == null || location == null) {
            return BodyPart.UNKNOWN;
        }

        return switch (location) {
            case HEAD -> BodyPart.HEAD;
            case TORSO -> BodyPart.TORSO;
            case ARMS -> isRightSide(attacker, target) ? BodyPart.RIGHT_ARM : BodyPart.LEFT_ARM;
            case LEGS -> isRightSide(attacker, target) ? BodyPart.RIGHT_LEG : BodyPart.LEFT_LEG;
            case UNKNOWN -> BodyPart.UNKNOWN;
        };
    }

    /**
     * @return true if {@code target} sits to {@code attacker}'s right,
     * determined by the sign of the cross product between the
     * attacker's horizontal look direction and the horizontal vector
     * toward the target. The exact sign convention is an internal
     * implementation detail — only its consistency across calls
     * matters, since it's used purely to split one lateral band into
     * two independently-configurable {@link BodyPart}s.
     */
    private static boolean isRightSide(PlayerEntity attacker, LivingEntity target) {
        Vec3d lookVec = attacker.getRotationVec(1.0f);
        Vec3d lookHorizontal = new Vec3d(lookVec.x, 0.0, lookVec.z);
        if (lookHorizontal.lengthSquared() < 1.0E-6) {
            // No reliable horizontal look direction (looking straight up
            // or down) - fall back to a stable, arbitrary side rather
            // than dividing by ~zero.
            return true;
        }
        lookHorizontal = lookHorizontal.normalize();

        Vec3d eyePos = attacker.getEyePos();
        Vec3d toTargetHorizontal = new Vec3d(target.getX() - eyePos.x, 0.0, target.getZ() - eyePos.z);

        double cross = lookHorizontal.x * toTargetHorizontal.z - lookHorizontal.z * toTargetHorizontal.x;
        return cross < 0.0;
    }
}