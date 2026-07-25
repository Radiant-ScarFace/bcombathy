package com.bcombat.combat.ai.group;

import com.bcombat.combat.weapon.WeaponCategory;
import com.bcombat.combat.weapon.WeaponProperties;

/**
 * Weapon-aware tactical bias for the Advanced AI Behaviors &amp; Group
 * Combat Framework — reads only the already-existing {@link
 * WeaponProperties#category()} (part of the existing Weapon Framework)
 * and turns it into small multipliers {@code AICombatController} stacks
 * on top of its {@link com.bcombat.combat.ai.AIDifficultyPreset} and
 * {@link com.bcombat.combat.ai.CombatRole} decisions. No weapon stat
 * (reach, damage, stamina cost, supported directions) is duplicated or
 * reinterpreted here — {@link WeaponProperties#reach()} and {@link
 * com.bcombat.combat.controller.CombatController#getEffectiveReach()}
 * remain the single source of truth for actual fighting distance; this
 * class only ever nudges the AI's *preference* around that same reach.
 * <p>
 * Static/stateless by design, exactly like {@code DirectionCompatibility}
 * — a pure function of {@link WeaponCategory}, safe to call from any
 * thread ticking any number of {@code AICombatController} instances.
 */
public final class WeaponTactics {

    private WeaponTactics() {
        // Static utility, no instances.
    }

    /**
     * Multiplier stacked on top of an AI's already-computed ideal
     * fighting distance. Reach-forward weapons (spears, polearms) bias
     * the AI to fight further out and exploit their reach advantage;
     * short weapons (daggers, one-handed swords) bias it to close the
     * distance instead of standing at the edge of a longer weapon's
     * reach where they can't threaten back.
     */
    public static double preferredDistanceMultiplier(WeaponProperties weapon) {
        return switch (weapon.category()) {
            case SPEAR, POLEARM -> 1.15;
            case TWO_HANDED_SWORD -> 1.05;
            case DAGGER -> 0.80;
            case ONE_HANDED_SWORD -> 0.95;
            case AXE, MACE -> 1.00;
            case UNARMED -> 0.75;
        };
    }

    /**
     * Multiplier stacked on top of an AI's already-computed attack
     * initiation chance. Heavier/slower-recovering weapon classes bias
     * the AI toward being more selective about committing (a whiffed
     * swing costs more time to recover from), while fast light weapons
     * bias it toward initiating more freely.
     */
    public static double attackInitiationMultiplier(WeaponProperties weapon) {
        return switch (weapon.category()) {
            case DAGGER -> 1.20;
            case ONE_HANDED_SWORD -> 1.05;
            case SPEAR -> 1.00;
            case POLEARM, AXE -> 0.90;
            case TWO_HANDED_SWORD, MACE -> 0.80;
            case UNARMED -> 0.70;
        };
    }
}