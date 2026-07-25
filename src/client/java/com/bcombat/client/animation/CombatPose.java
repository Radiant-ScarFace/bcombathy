package com.bcombat.client.animation;

/**
 * A single procedural pose for the humanoid model bones the combat
 * renderer drives — all angles in radians, all relative to vanilla's
 * own rest pose (i.e. {@code 0} on every field reproduces the model's
 * neutral T/A-pose contribution, exactly like {@code BipedEntityModel}'s
 * own default angles).
 * <p>
 * Deliberately a plain mutable holder rather than a record: {@link
 * CombatAnimationApplier} blends two of these (previous/current state)
 * every single tick for every combat-tracked entity on screen, and
 * building a fresh immutable object per field, per entity, per tick is
 * needless allocation churn for a hot render-thread path. {@link
 * #lerp(CombatPose, CombatPose, float)} is the only way an instance's
 * fields are ever mutated after construction.
 */
public final class CombatPose {

    public float headPitch;
    public float headYaw;
    public float bodyYaw;
    public float bodyPitch;

    public float rightArmPitch;
    public float rightArmYaw;
    public float rightArmRoll;

    public float leftArmPitch;
    public float leftArmYaw;
    public float leftArmRoll;

    /** Neutral rest pose — every angle at {@code 0}, i.e. no contribution over vanilla's own base pose. */
    public static CombatPose neutral() {
        return new CombatPose();
    }

    /**
     * Overwrites every field of this instance with {@code other}'s,
     * without allocating a new object — the in-place counterpart to
     * {@link #copy()}, used by {@link CombatPoseCache} to update its
     * cached previous/current poses once per tick on the hot render
     * thread path documented above.
     */
    public void setFrom(CombatPose other) {
        this.headPitch = other.headPitch;
        this.headYaw = other.headYaw;
        this.bodyYaw = other.bodyYaw;
        this.bodyPitch = other.bodyPitch;
        this.rightArmPitch = other.rightArmPitch;
        this.rightArmYaw = other.rightArmYaw;
        this.rightArmRoll = other.rightArmRoll;
        this.leftArmPitch = other.leftArmPitch;
        this.leftArmYaw = other.leftArmYaw;
        this.leftArmRoll = other.leftArmRoll;
    }

    public CombatPose copy() {
        CombatPose p = new CombatPose();
        p.headPitch = headPitch;
        p.headYaw = headYaw;
        p.bodyYaw = bodyYaw;
        p.bodyPitch = bodyPitch;
        p.rightArmPitch = rightArmPitch;
        p.rightArmYaw = rightArmYaw;
        p.rightArmRoll = rightArmRoll;
        p.leftArmPitch = leftArmPitch;
        p.leftArmYaw = leftArmYaw;
        p.leftArmRoll = leftArmRoll;
        return p;
    }

    /**
     * Writes the linear blend of {@code from} (weight 0) and {@code to}
     * (weight 1) at {@code weight} into {@code out}, avoiding an
     * allocation on the per-tick, per-entity hot path.
     */
    public static void lerp(CombatPose from, CombatPose to, float weight, CombatPose out) {
        out.headPitch = lerp(from.headPitch, to.headPitch, weight);
        out.headYaw = lerp(from.headYaw, to.headYaw, weight);
        out.bodyYaw = lerp(from.bodyYaw, to.bodyYaw, weight);
        out.bodyPitch = lerp(from.bodyPitch, to.bodyPitch, weight);
        out.rightArmPitch = lerp(from.rightArmPitch, to.rightArmPitch, weight);
        out.rightArmYaw = lerp(from.rightArmYaw, to.rightArmYaw, weight);
        out.rightArmRoll = lerp(from.rightArmRoll, to.rightArmRoll, weight);
        out.leftArmPitch = lerp(from.leftArmPitch, to.leftArmPitch, weight);
        out.leftArmYaw = lerp(from.leftArmYaw, to.leftArmYaw, weight);
        out.leftArmRoll = lerp(from.leftArmRoll, to.leftArmRoll, weight);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
