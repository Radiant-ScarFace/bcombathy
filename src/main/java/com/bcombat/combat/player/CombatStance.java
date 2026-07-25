package com.bcombat.combat.player;

/**
 * Describes the player's current combat posture at a level a renderer can
 * consume. This class intentionally does NOT touch entity body/head yaw
 * itself — applying a stance visually (body rotation, arm pose, head
 * clamp) is a rendering concern that belongs to a future GeckoLib model
 * layer, which is out of scope for this phase ("no weapon-specific
 * animations yet"). This class exists now so that future layer has a
 * single, stable source of truth to read instead of re-deriving posture
 * from raw combat state.
 */
public final class CombatStance {

    /** Generic, weapon-agnostic combat posture. Future phases may add e.g. ONE_HANDED, TWO_HANDED, SHIELD. */
    public enum Posture {
        NONE,
        GENERIC_COMBAT
    }

    private final Posture posture;

    private CombatStance(Posture posture) {
        this.posture = posture;
    }

    public static CombatStance none() {
        return new CombatStance(Posture.NONE);
    }

    public static CombatStance genericCombat() {
        return new CombatStance(Posture.GENERIC_COMBAT);
    }

    public Posture getPosture() {
        return posture;
    }

    public boolean isActive() {
        return posture != Posture.NONE;
    }
}
