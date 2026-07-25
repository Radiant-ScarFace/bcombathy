package com.bcombat.combat.collision;

/**
 * Approximate body region a confirmed hit landed on, as classified by
 * {@link CollisionDetector#classifyHitLocation}. This is purely
 * descriptive metadata for future systems (location-based damage
 * modifiers, hit reactions, dedicated body hitboxes) — nothing in this
 * phase reads it to affect damage, which does not exist yet.
 * <p>
 * Classification in this phase is a geometric approximation derived from
 * the attacker's eye height and look direction relative to the target's
 * hitbox, not a real per-limb hitbox. See {@link CollisionDetector} for
 * the exact method; a future dedicated body-hitbox phase is expected to
 * replace the approximation without changing this enum's contract.
 */
public enum HitLocation {

    /** No hit occurred, or location could not be determined (e.g. non-hit outcomes). */
    UNKNOWN,

    HEAD,
    TORSO,
    ARMS,
    LEGS
}