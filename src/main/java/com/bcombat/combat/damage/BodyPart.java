package com.bcombat.combat.damage;

/**
 * The six independently-multiplied body hitbox regions the damage
 * framework distinguishes, plus {@link #UNKNOWN} for any {@code
 * HitResult} the region cannot be determined for (a miss, a blocked
 * attack, or any other non-confirmed-hit outcome).
 * <p>
 * This is a refinement of {@code
 * com.bcombat.combat.collision.HitLocation} — that enum only
 * distinguishes {@code HEAD}/{@code TORSO}/{@code ARMS}/{@code LEGS}
 * (no left/right side), which is all the collision framework needs to
 * approximate "where did this land". The damage framework needs
 * independently configurable left/right multipliers (per the design
 * brief's explicit six-region requirement), so {@link BodyPartResolver}
 * adds a left/right split on top of {@code HitLocation} without
 * modifying the collision framework's contract.
 * <p>
 * Every value here has its own configurable multiplier in {@link
 * DamageConstants} and its own armor slot in {@link ArmorSlot}.
 */
public enum BodyPart {

    HEAD,
    TORSO,
    LEFT_ARM,
    RIGHT_ARM,
    LEFT_LEG,
    RIGHT_LEG,

    /** No confirmed hit, or the region could not be determined. */
    UNKNOWN
}