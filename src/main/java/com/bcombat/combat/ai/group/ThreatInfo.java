package com.bcombat.combat.ai.group;

import net.minecraft.entity.LivingEntity;

/**
 * A single scored threat candidate accumulated by {@link CombatSquad}
 * while it aggregates every member's currently-visible target into a
 * shared picture — the concrete unit of "shared combat awareness
 * between nearby AI" and "threat prioritization". Purely a data holder;
 * all scoring math lives in {@link CombatSquad} so this class has no
 * behavior of its own to duplicate anything {@code CombatController} or
 * {@code AICombatController} already do.
 */
final class ThreatInfo {

    private final LivingEntity target;
    private double score;
    private int reporterCount;

    ThreatInfo(LivingEntity target) {
        this.target = target;
    }

    LivingEntity target() {
        return target;
    }

    double score() {
        return score;
    }

    int reporterCount() {
        return reporterCount;
    }

    void addContribution(double contribution) {
        this.score += contribution;
        this.reporterCount++;
    }
}