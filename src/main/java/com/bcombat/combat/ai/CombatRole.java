package com.bcombat.combat.ai;

/**
 * The tactical role an AI-controlled combatant plays within its {@link
 * com.bcombat.combat.ai.group.CombatSquad}, part of the Advanced AI
 * Behaviors &amp; Group Combat Framework. Exactly like {@link
 * AIDifficultyPreset} scales *how well* an individual AI fights, {@link
 * CombatRole} scales *what job* it does within a group — both are read
 * by {@link AICombatController} purely as multipliers/biases on top of
 * the exact same {@link com.bcombat.combat.controller.CombatController}
 * decisions the solo framework already makes; neither introduces a
 * second combat pipeline.
 * <p>
 * Kept as a plain enum with constructor-supplied multipliers, mirroring
 * {@link AIDifficultyPreset}'s own reasoning: role is a small, fixed,
 * developer-curated set, not something that needs a runtime registry or
 * a config-file entry of its own (global group-tactics *thresholds* —
 * spacing, switch cooldowns, threat weights — live in {@code
 * CombatConstants}/{@code config/bcombat.json} instead; see {@code
 * CombatConstants}'s "Group AI / Squad Tactics" section).
 */
public enum CombatRole {

    /**
     * Presses the engagement: prefers to fight closer than the weapon's
     * ordinary ideal distance, initiates attacks more readily, takes the
     * widest flank angles, and is the most reluctant to retreat.
     */
    AGGRESSOR(
            /* preferredDistanceMultiplier */ 0.85,
            /* attackInitiationMultiplier  */ 1.30,
            /* retreatReluctance           */ 1.35,
            /* flankAngleBias              */ 1.15,
            /* regroupPriority             */ 0.4
    ),

    /**
     * Holds a steadier line: fights at the weapon's ordinary ideal
     * distance, blocks more readily than it swings, stays closer to its
     * allies (tight flank angles) so it can screen them, and retreats
     * more readily than an {@link #AGGRESSOR} to avoid a costly trade.
     */
    DEFENDER(
            /* preferredDistanceMultiplier */ 1.00,
            /* attackInitiationMultiplier  */ 0.70,
            /* retreatReluctance           */ 0.90,
            /* flankAngleBias              */ 0.70,
            /* regroupPriority             */ 0.8
    ),

    /**
     * Hangs back: prefers the largest distance of any role, rarely
     * initiates on its own, and is the quickest to fall back toward the
     * squad's regroup point — representing a combatant a Bannerlord-style
     * formation would keep out of the front line.
     */
    SUPPORT(
            /* preferredDistanceMultiplier */ 1.35,
            /* attackInitiationMultiplier  */ 0.45,
            /* retreatReluctance           */ 0.60,
            /* flankAngleBias              */ 0.55,
            /* regroupPriority             */ 1.0
    ),

    /**
     * Hit-and-run: takes wide, fast-rotating flank angles at a slightly
     * longer distance than {@link #AGGRESSOR}, initiates readily, and
     * backs off quickly after committing — never settling into a
     * prolonged toe-to-toe exchange.
     */
    SKIRMISHER(
            /* preferredDistanceMultiplier */ 1.15,
            /* attackInitiationMultiplier  */ 1.05,
            /* retreatReluctance           */ 0.75,
            /* flankAngleBias              */ 1.30,
            /* regroupPriority             */ 0.55
    );

    private final double preferredDistanceMultiplier;
    private final double attackInitiationMultiplier;
    private final double retreatReluctance;
    private final double flankAngleBias;
    private final double regroupPriority;

    CombatRole(double preferredDistanceMultiplier, double attackInitiationMultiplier,
               double retreatReluctance, double flankAngleBias, double regroupPriority) {
        this.preferredDistanceMultiplier = preferredDistanceMultiplier;
        this.attackInitiationMultiplier = attackInitiationMultiplier;
        this.retreatReluctance = retreatReluctance;
        this.flankAngleBias = flankAngleBias;
        this.regroupPriority = regroupPriority;
    }

    /** Multiplier stacked on top of {@link AIDifficultyPreset#preferredDistanceRatio()}'s resulting ideal distance. */
    public double preferredDistanceMultiplier() {
        return preferredDistanceMultiplier;
    }

    /** Multiplier stacked on top of {@link AIDifficultyPreset#attackInitiationChance()}. */
    public double attackInitiationMultiplier() {
        return attackInitiationMultiplier;
    }

    /**
     * Divides into the effective low-health/low-stamina retreat trigger
     * threshold — above 1.0 means "retreats less readily than baseline"
     * (holds the line longer), below 1.0 means "retreats more readily".
     */
    public double retreatReluctance() {
        return retreatReluctance;
    }

    /** Multiplier on how wide a flank/surround angular slot this role is willing to take around the squad's target. */
    public double flankAngleBias() {
        return flankAngleBias;
    }

    /** Relative weight (0-1) this role gives to falling back toward the squad's regroup point once retreating. */
    public double regroupPriority() {
        return regroupPriority;
    }
}