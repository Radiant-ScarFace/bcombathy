package com.bcombat.combat.damage;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Central location for every tunable value used by the damage & armor
 * framework, following the exact convention {@code
 * com.bcombat.combat.util.CombatConstants} already established for the
 * rest of the combat framework: no class outside this package should
 * hardcode a multiplier, threshold, or scaling factor directly —
 * everything lives here so balance passes never require hunting through
 * unrelated classes.
 */
public final class DamageConstants {

    private DamageConstants() {
        // Utility class, no instances.
    }

    // ------------------------------------------------------------------
    // Body part damage multipliers. Applied by DamageCalculator against
    // a weapon's raw (base + typed) damage before armor mitigation.
    // ------------------------------------------------------------------

    /** Damage multiplier for a confirmed {@link BodyPart#HEAD} hit. */
    public static final double HEAD_DAMAGE_MULTIPLIER = 2.0;

    /** Damage multiplier for a confirmed {@link BodyPart#TORSO} hit. */
    public static final double TORSO_DAMAGE_MULTIPLIER = 1.0;

    /**
     * Damage multiplier for a confirmed {@link BodyPart#LEFT_ARM} or
     * {@link BodyPart#RIGHT_ARM} hit. Shared by both sides; if
     * asymmetric arm damage is ever wanted (e.g. a shield-arm penalty),
     * split this into two constants — {@link BodyPartResolver} already
     * distinguishes the sides.
     */
    public static final double ARM_DAMAGE_MULTIPLIER = 0.7;

    /**
     * Damage multiplier for a confirmed {@link BodyPart#LEFT_LEG} or
     * {@link BodyPart#RIGHT_LEG} hit. Shared by both sides; see {@link
     * #ARM_DAMAGE_MULTIPLIER}.
     */
    public static final double LEG_DAMAGE_MULTIPLIER = 0.8;

    /**
     * Fallback multiplier used only if a {@link BodyPart} somehow has no
     * explicit entry above (defensive default; every real enum constant
     * except {@link BodyPart#UNKNOWN} is covered).
     */
    public static final double DEFAULT_BODY_DAMAGE_MULTIPLIER = 1.0;

    /**
     * @return the configured damage multiplier for {@code bodyPart}.
     */
    public static double multiplierFor(BodyPart bodyPart) {
        if (bodyPart == null) {
            return DEFAULT_BODY_DAMAGE_MULTIPLIER;
        }
        return switch (bodyPart) {
            case HEAD -> HEAD_DAMAGE_MULTIPLIER;
            case TORSO -> TORSO_DAMAGE_MULTIPLIER;
            case LEFT_ARM, RIGHT_ARM -> ARM_DAMAGE_MULTIPLIER;
            case LEFT_LEG, RIGHT_LEG -> LEG_DAMAGE_MULTIPLIER;
            case UNKNOWN -> DEFAULT_BODY_DAMAGE_MULTIPLIER;
        };
    }

    // ------------------------------------------------------------------
    // Critical hits. A critical hit is a body-part-driven concept in
    // this phase (e.g. headshots), configured here rather than as a
    // random chance roll, keeping outcomes deterministic and testable.
    // ------------------------------------------------------------------

    /**
     * The set of {@link BodyPart}s a hit against automatically qualifies
     * as a critical hit. Kept as a configurable set rather than a single
     * hardcoded {@code HEAD} check so future tuning (e.g. an assassin
     * playstyle also critting on back-arm hits) doesn't require touching
     * {@link DamageCalculator}.
     */
    public static final Set<BodyPart> CRITICAL_HIT_BODY_PARTS =
            Collections.unmodifiableSet(EnumSet.of(BodyPart.HEAD));

    /**
     * Multiplier stacked on top of the body-part multiplier (and after
     * armor mitigation) for a critical hit.
     */
    public static final double CRITICAL_HIT_MULTIPLIER = 1.5;

    // ------------------------------------------------------------------
    // Armor effectiveness. DamageCalculator reduces each typed damage
    // component independently using a diminishing-returns curve:
    // reduction = resistance / (resistance + ARMOR_EFFECTIVENESS_CONSTANT),
    // capped so armor can never reduce damage below
    // ARMOR_MIN_DAMAGE_RATIO of its pre-armor value. This keeps very
    // high armor values powerful without ever making a body part
    // completely immune to damage.
    // ------------------------------------------------------------------

    /**
     * The resistance value, in the diminishing-returns curve above, at
     * which armor mitigates exactly half of incoming typed damage.
     * Lower values make armor more effective per point; higher values
     * make it less effective per point.
     */
    public static final double ARMOR_EFFECTIVENESS_CONSTANT = 10.0;

    /**
     * The minimum fraction (0-1) of pre-armor typed damage that always
     * passes through regardless of how high a resistance value is. Kept
     * above 0 so no armor combination can ever make a body part
     * completely immune to a damage type.
     */
    public static final double ARMOR_MIN_DAMAGE_RATIO = 0.10;

    // ------------------------------------------------------------------
    // Global damage scaling. Applied uniformly to every hit before
    // body/armor calculations, as a single future config/balance knob
    // (server difficulty setting, PvP-specific scaling, etc.) that
    // doesn't require touching weapon or armor registrations.
    // ------------------------------------------------------------------

    /** Global multiplier applied to every hit's raw weapon damage. Kept at 1.0 (no change) by default. */
    public static final double DEFAULT_DAMAGE_SCALE = 1.0;

    /**
     * Reserved multiplier for a future difficulty system (e.g. "hard"
     * mode scaling player-dealt or player-received damage). Unused
     * until such configuration exists; kept at 1.0 (no change).
     */
    public static final double DEFAULT_DIFFICULTY_DAMAGE_MODIFIER = 1.0;

    /**
     * The absolute floor for a single hit's final damage, applied after
     * every multiplier and armor reduction. Guarantees a confirmed hit
     * is never rounded away to nothing.
     */
    public static final double MINIMUM_DAMAGE = 0.5;

    // ------------------------------------------------------------------
    // Stagger extension point. No stagger behavior is implemented in
    // this phase - DamageCalculator only computes whether a hit
    // qualifies, so future systems have a ready-made, config-driven
    // trigger condition to build on. See CombatEvents#STAGGER_TRIGGERED.
    // ------------------------------------------------------------------

    /**
     * The final-damage threshold, in raw damage points, at which a hit
     * is considered to qualify for a future stagger reaction. Purely a
     * trigger condition in this phase; no stagger effect is applied.
     */
    public static final double STAGGER_DAMAGE_THRESHOLD = 6.0;
}