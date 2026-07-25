package com.bcombat.combat.ai;

/**
 * Configurable difficulty tuning for {@link AICombatController}. Every
 * numeric knob an AI-controlled combatant's decision-making reads lives
 * here, exactly the way every player-facing timing/speed value lives in
 * {@code CombatConstants} — so balancing AI difficulty is a matter of
 * picking (or building) a preset, never editing decision code itself.
 * <p>
 * Kept as a plain enum with constructor-supplied values (rather than a
 * builder + registry like {@code WeaponProperties}/{@code
 * WeaponRegistry}) since difficulty is a small, fixed, developer-curated
 * set rather than something content packs need to register new entries
 * for at runtime.
 */
public enum AIDifficultyPreset {

    /**
     * Slow reactions, cautious, rarely blocks or chambers correctly,
     * frequently picks the wrong attack direction. Suitable for early
     * introductory encounters.
     */
    EASY(
            /* reactionDelayTicks            */ 10,
            /* engagementRange                */ 6.0,
            /* preferredDistanceRatio         */ 0.9,
            /* attackInitiationChance         */ 0.05,
            /* blockReactionChance            */ 0.25,
            /* chamberAttemptChance           */ 0.05,
            /* directionAccuracy              */ 0.35,
            /* staminaCautionThreshold        */ 0.45,
            /* chainAttackChance              */ 0.15,
            /* exitBlockEagerness             */ 0.35
    ),

    /** Baseline, reasonably competent Bannerlord-style opponent. */
    NORMAL(
            /* reactionDelayTicks            */ 6,
            /* engagementRange                */ 7.0,
            /* preferredDistanceRatio         */ 0.85,
            /* attackInitiationChance         */ 0.10,
            /* blockReactionChance            */ 0.55,
            /* chamberAttemptChance           */ 0.15,
            /* directionAccuracy              */ 0.6,
            /* staminaCautionThreshold        */ 0.35,
            /* chainAttackChance              */ 0.35,
            /* exitBlockEagerness             */ 0.25
    ),

    /** Fast reactions, aggressive, reads guard/attack direction well. */
    HARD(
            /* reactionDelayTicks            */ 3,
            /* engagementRange                */ 8.0,
            /* preferredDistanceRatio         */ 0.8,
            /* attackInitiationChance         */ 0.18,
            /* blockReactionChance            */ 0.80,
            /* chamberAttemptChance           */ 0.30,
            /* directionAccuracy              */ 0.85,
            /* staminaCautionThreshold        */ 0.25,
            /* chainAttackChance              */ 0.55,
            /* exitBlockEagerness             */ 0.15
    ),

    /** Near-instant reactions and near-perfect reads. Reserved for boss-tier combatants. */
    NIGHTMARE(
            /* reactionDelayTicks            */ 1,
            /* engagementRange                */ 9.0,
            /* preferredDistanceRatio         */ 0.75,
            /* attackInitiationChance         */ 0.25,
            /* blockReactionChance            */ 0.95,
            /* chamberAttemptChance           */ 0.45,
            /* directionAccuracy              */ 0.97,
            /* staminaCautionThreshold        */ 0.15,
            /* chainAttackChance              */ 0.75,
            /* exitBlockEagerness             */ 0.10
    );

    private final int reactionDelayTicks;
    private final double engagementRange;
    private final double preferredDistanceRatio;
    private final double attackInitiationChance;
    private final double blockReactionChance;
    private final double chamberAttemptChance;
    private final double directionAccuracy;
    private final double staminaCautionThreshold;
    private final double chainAttackChance;
    private final double exitBlockEagerness;

    AIDifficultyPreset(int reactionDelayTicks, double engagementRange, double preferredDistanceRatio,
                       double attackInitiationChance, double blockReactionChance, double chamberAttemptChance,
                       double directionAccuracy, double staminaCautionThreshold, double chainAttackChance,
                       double exitBlockEagerness) {
        this.reactionDelayTicks = reactionDelayTicks;
        this.engagementRange = engagementRange;
        this.preferredDistanceRatio = preferredDistanceRatio;
        this.attackInitiationChance = attackInitiationChance;
        this.blockReactionChance = blockReactionChance;
        this.chamberAttemptChance = chamberAttemptChance;
        this.directionAccuracy = directionAccuracy;
        this.staminaCautionThreshold = staminaCautionThreshold;
        this.chainAttackChance = chainAttackChance;
        this.exitBlockEagerness = exitBlockEagerness;
    }

    /**
     * Ticks an AI decision is delayed after the condition that triggers
     * it becomes true (simulated reaction time), e.g. before raising a
     * block once a wind-up is first noticed. Lower is faster/harder.
     */
    public int reactionDelayTicks() {
        return reactionDelayTicks;
    }

    /** Maximum distance, in blocks, at which the AI will voluntarily enter Combat Mode against its target. */
    public double engagementRange() {
        return engagementRange;
    }

    /**
     * Fraction of the equipped weapon's {@code reach()} the AI tries to
     * hold as its ideal fighting distance — close enough to land a hit,
     * far enough to react to the opponent's own swings.
     */
    public double preferredDistanceRatio() {
        return preferredDistanceRatio;
    }

    /** Per-tick probability, while idle and in range, that the AI commits to a new attack. */
    public double attackInitiationChance() {
        return attackInitiationChance;
    }

    /** Probability the AI reacts to a noticed enemy wind-up/swing by raising a guard at all. */
    public double blockReactionChance() {
        return blockReactionChance;
    }

    /** Probability the AI attempts to match direction for a Chamber rather than simply continuing its own swing. */
    public double chamberAttemptChance() {
        return chamberAttemptChance;
    }

    /**
     * Probability the AI picks the *correct* directional counter (a
     * guard the opponent isn't already holding, or a guard that matches
     * the opponent's telegraphed attack) rather than a random one from
     * the weapon's supported set.
     */
    public double directionAccuracy() {
        return directionAccuracy;
    }

    /**
     * Stamina ratio (0-1) below which the AI stops voluntarily
     * initiating new attacks/blocks and instead prioritizes retreating
     * to recover — mirrors a human player backing off while gassed.
     */
    public double staminaCautionThreshold() {
        return staminaCautionThreshold;
    }

    /** Probability of buffering another attack during {@code RECOVERY} instead of returning to a neutral idle. */
    public double chainAttackChance() {
        return chainAttackChance;
    }

    /** Per-reaction-tick probability of dropping a guard once its target is no longer telegraphing a threat. */
    public double exitBlockEagerness() {
        return exitBlockEagerness;
    }
}