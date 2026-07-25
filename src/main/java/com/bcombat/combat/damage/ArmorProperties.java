package com.bcombat.combat.damage;

/**
 * Immutable, fully-configurable description of a single armor piece's
 * protective stats — the armor-side equivalent of {@code
 * com.bcombat.combat.weapon.WeaponProperties}. This is the entire
 * contract between an armor item and the damage framework: {@link
 * DamageCalculator} reads these values instead of hardcoding any
 * item-specific protection, so adding new armor is a matter of building
 * one of these and {@link ArmorRegistry#register registering} it — no
 * damage-calculation code changes required.
 * <p>
 * Protection is expressed per {@link DamageType} ({@link
 * #cutResistance()}, {@link #pierceResistance()}, {@link
 * #bluntResistance()}) rather than as one blended value, so a piece can
 * be deliberately lopsided (e.g. plate that shrugs off cuts but
 * transfers blunt trauma) — matching the same Bannerlord-inspired
 * per-type philosophy {@code WeaponProperties} uses for damage output.
 * {@link #armorValue()} is a single overall rating for display/UI or
 * simple systems that don't care about the type breakdown; by default
 * every typed resistance simply equals it.
 * <p>
 * Instances are built via {@link #builder()} rather than a public
 * constructor, exactly like {@code WeaponProperties}, so every property
 * has an explicit, documented default and future properties can be
 * added without breaking existing call sites.
 */
public final class ArmorProperties {

    private static final ArmorProperties NONE = builder().build();

    private final double armorValue;
    private final double cutResistance;
    private final double pierceResistance;
    private final double bluntResistance;

    private ArmorProperties(Builder builder) {
        this.armorValue = builder.armorValue;
        this.cutResistance = builder.cutResistance >= 0 ? builder.cutResistance : builder.armorValue;
        this.pierceResistance = builder.pierceResistance >= 0 ? builder.pierceResistance : builder.armorValue;
        this.bluntResistance = builder.bluntResistance >= 0 ? builder.bluntResistance : builder.armorValue;
    }

    /**
     * @return the shared "no armor" instance, used whenever a body part
     * has no equipped/registered protection. Every resistance is 0, so
     * {@link DamageCalculator} applies zero mitigation, exactly as if
     * armor didn't exist for that hit.
     */
    public static ArmorProperties none() {
        return NONE;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Overall protection rating for this piece. Also the default for any typed resistance not explicitly overridden. */
    public double armorValue() {
        return armorValue;
    }

    /** Resistance applied against {@link DamageType#CUT} damage. */
    public double cutResistance() {
        return cutResistance;
    }

    /** Resistance applied against {@link DamageType#PIERCE} damage. */
    public double pierceResistance() {
        return pierceResistance;
    }

    /** Resistance applied against {@link DamageType#BLUNT} damage. */
    public double bluntResistance() {
        return bluntResistance;
    }

    /** @return the resistance value for {@code type}. */
    public double resistanceFor(DamageType type) {
        if (type == null) {
            return armorValue;
        }
        return switch (type) {
            case CUT -> cutResistance;
            case PIERCE -> pierceResistance;
            case BLUNT -> bluntResistance;
        };
    }

    /**
     * @return true if this instance provides no protection at all
     * (equivalent to {@link #none()}). Used by {@link ArmorResolver}
     * when combining multiple pieces so an empty slot contributes
     * nothing rather than a spurious zero-value piece.
     */
    public boolean isNone() {
        return armorValue <= 0.0 && cutResistance <= 0.0 && pierceResistance <= 0.0 && bluntResistance <= 0.0;
    }

    /**
     * Combines this piece's resistances with {@code other}'s, summing
     * each component. Used by {@link ArmorResolver} to stack protection
     * from multiple equipped pieces covering the same {@link BodyPart}
     * (e.g. leggings and boots both contributing to the legs).
     */
    public ArmorProperties combine(ArmorProperties other) {
        if (other == null || other.isNone()) {
            return this;
        }
        if (this.isNone()) {
            return other;
        }
        return builder()
                .armorValue(this.armorValue + other.armorValue)
                .cutResistance(this.cutResistance + other.cutResistance)
                .pierceResistance(this.pierceResistance + other.pierceResistance)
                .bluntResistance(this.bluntResistance + other.bluntResistance)
                .build();
    }

    @Override
    public String toString() {
        return "ArmorProperties{armorValue=" + armorValue
                + ", cutResistance=" + cutResistance
                + ", pierceResistance=" + pierceResistance
                + ", bluntResistance=" + bluntResistance
                + '}';
    }

    /**
     * Builder for {@link ArmorProperties}. {@link #armorValue(double)}
     * sets the overall rating and, unless a typed resistance is
     * explicitly overridden afterward, all three typed resistances too —
     * so a simple piece only needs one call, while a piece with
     * deliberately uneven protection can still override individual types.
     */
    public static final class Builder {

        private double armorValue = 0.0;
        private double cutResistance = -1.0;
        private double pierceResistance = -1.0;
        private double bluntResistance = -1.0;

        private Builder() {
        }

        public Builder armorValue(double armorValue) {
            this.armorValue = armorValue;
            return this;
        }

        public Builder cutResistance(double cutResistance) {
            this.cutResistance = cutResistance;
            return this;
        }

        public Builder pierceResistance(double pierceResistance) {
            this.pierceResistance = pierceResistance;
            return this;
        }

        public Builder bluntResistance(double bluntResistance) {
            this.bluntResistance = bluntResistance;
            return this;
        }

        public ArmorProperties build() {
            return new ArmorProperties(this);
        }
    }
}