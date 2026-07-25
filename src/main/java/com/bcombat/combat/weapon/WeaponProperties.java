package com.bcombat.combat.weapon;

import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.block.GuardDirection;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable, fully-configurable description of a single weapon's combat
 * stats. This is the entire contract between a weapon and the combat
 * framework: {@code CombatController} reads these values instead of
 * hardcoding any weapon-specific behavior, so adding a new weapon is a
 * matter of building one of these and {@link WeaponRegistry#register
 * registering} it — no combat code changes required.
 * <p>
 * Instances are built via {@link #builder(WeaponCategory)} rather than a
 * public constructor, so every property has an explicit, documented
 * default and future properties can be added without breaking existing
 * call sites.
 * <p>
 * Nothing here models collision or hit detection — those stay out of
 * scope for this framework. {@link #reach()} exists purely as data for
 * the collision system to read; this class does not act on it.
 * <p>
 * {@link #baseDamage()}, {@link #cutDamage()}, {@link #pierceDamage()},
 * and {@link #bluntDamage()} are the sole source of a weapon's damage
 * output for the damage framework (see {@code
 * com.bcombat.combat.damage.DamageCalculator}) — nothing in that
 * framework hardcodes a damage value; it only ever reads these four
 * properties plus body/armor modifiers. A weapon is free to leave any
 * of the three typed components at its default of {@code 0.0} (e.g. a
 * pure slashing sword needs no {@link #bluntDamage()}) since {@link
 * #baseDamage()} alone already guarantees every weapon deals some
 * damage.
 */
public final class WeaponProperties {

    private static final WeaponProperties UNARMED = builder(WeaponCategory.UNARMED).build();

    private final WeaponCategory category;
    private final double reach;
    private final double weight;
    private final double handling;
    private final double swingSpeedModifier;
    private final double thrustSpeedModifier;
    private final double recoveryModifier;
    private final double windUpModifier;
    private final double staminaModifier;
    private final double staminaRegenDelayModifier;
    private final double staminaRegenRateModifier;
    private final double baseDamage;
    private final double cutDamage;
    private final double pierceDamage;
    private final double bluntDamage;
    private final Set<GuardDirection> supportedGuardDirections;
    private final Set<AttackDirection> supportedAttackDirections;

    private WeaponProperties(Builder builder) {
        this.category = builder.category;
        this.reach = builder.reach;
        this.weight = builder.weight;
        this.handling = builder.handling;
        this.swingSpeedModifier = builder.swingSpeedModifier;
        this.thrustSpeedModifier = builder.thrustSpeedModifier;
        this.recoveryModifier = builder.recoveryModifier;
        this.windUpModifier = builder.windUpModifier;
        this.staminaModifier = builder.staminaModifier;
        this.staminaRegenDelayModifier = builder.staminaRegenDelayModifier;
        this.staminaRegenRateModifier = builder.staminaRegenRateModifier;
        this.baseDamage = builder.baseDamage;
        this.cutDamage = builder.cutDamage;
        this.pierceDamage = builder.pierceDamage;
        this.bluntDamage = builder.bluntDamage;
        this.supportedGuardDirections = Collections.unmodifiableSet(EnumSet.copyOf(builder.supportedGuardDirections));
        this.supportedAttackDirections = Collections.unmodifiableSet(EnumSet.copyOf(builder.supportedAttackDirections));
    }

    /**
     * @return the shared baseline used whenever no weapon (empty hand) or
     * an unregistered item is held. Every modifier is neutral (1.0) and
     * every direction is supported, so combat behaves exactly as it did
     * before the weapon framework existed.
     */
    public static WeaponProperties unarmed() {
        return UNARMED;
    }

    public static Builder builder(WeaponCategory category) {
        return new Builder(category);
    }

    public WeaponCategory category() {
        return category;
    }

    /**
     * Reach of this weapon, in blocks. Pure data in this phase — no
     * collision or hit-detection system yet reads it — reserved as the
     * extension point a future melee-range system will use.
     */
    public double reach() {
        return reach;
    }

    /** Relative weight of this weapon. Reserved for future systems (e.g. stamina cost, movement feel). */
    public double weight() {
        return weight;
    }

    /** Relative handling/agility of this weapon. Reserved for future systems (e.g. direction-switch responsiveness). */
    public double handling() {
        return handling;
    }

    /**
     * Multiplier applied to slashing/overhead swing timing (wind-up
     * release duration for every {@link AttackDirection} other than
     * {@link AttackDirection#THRUST}). Below 1.0 is faster than the
     * baseline in {@code CombatConstants}, above 1.0 is slower.
     */
    public double swingSpeedModifier() {
        return swingSpeedModifier;
    }

    /**
     * Multiplier applied to {@link AttackDirection#THRUST} release
     * timing specifically. Kept independent of {@link
     * #swingSpeedModifier()} since thrusting weapons (spears, polearms)
     * are typically much faster to thrust than to swing.
     */
    public double thrustSpeedModifier() {
        return thrustSpeedModifier;
    }

    /** Multiplier applied to {@code CombatConstants#RECOVERY_DURATION_TICKS}. */
    public double recoveryModifier() {
        return recoveryModifier;
    }

    /** Multiplier applied to {@code CombatConstants#MIN_ATTACK_PREPARATION_TICKS}. */
    public double windUpModifier() {
        return windUpModifier;
    }

    /**
     * Multiplier applied to every stamina cost this weapon's wielder
     * incurs — attacking, entering/holding a block, Perfect Blocks,
     * Parries, Chambers, and sprinting in combat all scale by this value
     * (see {@code CombatConstants}' Stamina section for the unscaled
     * base costs). Below 1.0 makes a weapon cheaper to fight with, above
     * 1.0 more taxing — e.g. a heavy two-handed weapon might use 1.3
     * while a light dagger might use 0.7.
     */
    public double staminaModifier() {
        return staminaModifier;
    }

    /**
     * Multiplier applied to {@code CombatConstants#STAMINA_REGEN_DELAY_TICKS}
     * — how long stamina regeneration stays paused after this weapon's
     * wielder last consumed stamina. Below 1.0 means stamina resumes
     * regenerating sooner after use than the baseline.
     */
    public double staminaRegenDelayModifier() {
        return staminaRegenDelayModifier;
    }

    /**
     * Multiplier applied to {@code CombatConstants#STAMINA_REGEN_RATE_PER_TICK}
     * while this weapon is equipped. Above 1.0 means stamina regenerates
     * faster than the baseline once regeneration is active.
     */
    public double staminaRegenRateModifier() {
        return staminaRegenRateModifier;
    }

    /**
     * Flat baseline damage this weapon deals on every confirmed hit,
     * independent of {@link #cutDamage()}/{@link #pierceDamage()}/
     * {@link #bluntDamage()}. Guarantees every registered weapon (and
     * unarmed strikes) deal at least some damage even if every typed
     * component is left at its default of {@code 0.0}.
     */
    public double baseDamage() {
        return baseDamage;
    }

    /** Slashing damage component. Reduced by a target's cut resistance; see the damage framework. */
    public double cutDamage() {
        return cutDamage;
    }

    /** Piercing/thrust damage component. Reduced by a target's pierce resistance; see the damage framework. */
    public double pierceDamage() {
        return pierceDamage;
    }

    /** Blunt/impact damage component. Reduced by a target's blunt resistance; see the damage framework. */
    public double bluntDamage() {
        return bluntDamage;
    }

    public Set<GuardDirection> supportedGuardDirections() {
        return supportedGuardDirections;
    }

    public Set<AttackDirection> supportedAttackDirections() {
        return supportedAttackDirections;
    }

    public boolean supportsGuardDirection(GuardDirection direction) {
        return direction != null && supportedGuardDirections.contains(direction);
    }

    public boolean supportsAttackDirection(AttackDirection direction) {
        return direction != null && supportedAttackDirections.contains(direction);
    }

    @Override
    public String toString() {
        return "WeaponProperties{category=" + category
                + ", reach=" + reach
                + ", weight=" + weight
                + ", handling=" + handling
                + ", swingSpeedModifier=" + swingSpeedModifier
                + ", thrustSpeedModifier=" + thrustSpeedModifier
                + ", recoveryModifier=" + recoveryModifier
                + ", windUpModifier=" + windUpModifier
                + ", staminaModifier=" + staminaModifier
                + ", staminaRegenDelayModifier=" + staminaRegenDelayModifier
                + ", staminaRegenRateModifier=" + staminaRegenRateModifier
                + ", baseDamage=" + baseDamage
                + ", cutDamage=" + cutDamage
                + ", pierceDamage=" + pierceDamage
                + ", bluntDamage=" + bluntDamage
                + '}';
    }

    /**
     * Builder for {@link WeaponProperties}. Every property defaults to a
     * neutral value (modifiers at 1.0, every direction supported), so a
     * future developer only needs to override the properties that make a
     * given weapon distinct.
     */
    public static final class Builder {

        private final WeaponCategory category;
        private double reach = 3.0;
        private double weight = 1.0;
        private double handling = 1.0;
        private double swingSpeedModifier = 1.0;
        private double thrustSpeedModifier = 1.0;
        private double recoveryModifier = 1.0;
        private double windUpModifier = 1.0;
        private double staminaModifier = 1.0;
        private double staminaRegenDelayModifier = 1.0;
        private double staminaRegenRateModifier = 1.0;
        private double baseDamage = 1.0;
        private double cutDamage = 0.0;
        private double pierceDamage = 0.0;
        private double bluntDamage = 0.0;
        private Set<GuardDirection> supportedGuardDirections = EnumSet.of(
                GuardDirection.LEFT_GUARD, GuardDirection.RIGHT_GUARD, GuardDirection.UP_GUARD, GuardDirection.THRUST_GUARD);
        private Set<AttackDirection> supportedAttackDirections = EnumSet.of(
                AttackDirection.LEFT_SLASH, AttackDirection.RIGHT_SLASH, AttackDirection.OVERHEAD, AttackDirection.THRUST);

        private Builder(WeaponCategory category) {
            this.category = Objects.requireNonNull(category, "category must not be null");
        }

        public Builder reach(double reach) {
            this.reach = reach;
            return this;
        }

        public Builder weight(double weight) {
            this.weight = weight;
            return this;
        }

        public Builder handling(double handling) {
            this.handling = handling;
            return this;
        }

        public Builder swingSpeedModifier(double swingSpeedModifier) {
            this.swingSpeedModifier = swingSpeedModifier;
            return this;
        }

        public Builder thrustSpeedModifier(double thrustSpeedModifier) {
            this.thrustSpeedModifier = thrustSpeedModifier;
            return this;
        }

        public Builder recoveryModifier(double recoveryModifier) {
            this.recoveryModifier = recoveryModifier;
            return this;
        }

        public Builder windUpModifier(double windUpModifier) {
            this.windUpModifier = windUpModifier;
            return this;
        }

        public Builder staminaModifier(double staminaModifier) {
            this.staminaModifier = staminaModifier;
            return this;
        }

        /** @see WeaponProperties#staminaRegenDelayModifier() */
        public Builder staminaRegenDelayModifier(double staminaRegenDelayModifier) {
            this.staminaRegenDelayModifier = staminaRegenDelayModifier;
            return this;
        }

        /** @see WeaponProperties#staminaRegenRateModifier() */
        public Builder staminaRegenRateModifier(double staminaRegenRateModifier) {
            this.staminaRegenRateModifier = staminaRegenRateModifier;
            return this;
        }

        /** @see WeaponProperties#baseDamage() */
        public Builder baseDamage(double baseDamage) {
            this.baseDamage = baseDamage;
            return this;
        }

        /** @see WeaponProperties#cutDamage() */
        public Builder cutDamage(double cutDamage) {
            this.cutDamage = cutDamage;
            return this;
        }

        /** @see WeaponProperties#pierceDamage() */
        public Builder pierceDamage(double pierceDamage) {
            this.pierceDamage = pierceDamage;
            return this;
        }

        /** @see WeaponProperties#bluntDamage() */
        public Builder bluntDamage(double bluntDamage) {
            this.bluntDamage = bluntDamage;
            return this;
        }

        /**
         * Restricts which guard positions this weapon can hold. An empty
         * set means the weapon supports no directional guard at all
         * (block will remain in the neutral {@code NONE} stance).
         */
        public Builder supportedGuardDirections(GuardDirection... directions) {
            this.supportedGuardDirections = directions.length == 0
                    ? EnumSet.noneOf(GuardDirection.class)
                    : EnumSet.copyOf(Arrays.asList(directions));
            return this;
        }

        /**
         * Restricts which attack directions this weapon can commit to. An
         * empty set means the weapon can only ever attack as {@link
         * AttackDirection#NONE} (undirected).
         */
        public Builder supportedAttackDirections(AttackDirection... directions) {
            this.supportedAttackDirections = directions.length == 0
                    ? EnumSet.noneOf(AttackDirection.class)
                    : EnumSet.copyOf(Arrays.asList(directions));
            return this;
        }

        public WeaponProperties build() {
            return new WeaponProperties(this);
        }
    }
}