package com.bcombat.combat.damage;

/**
 * The three typed damage components a weapon can deal, mirroring {@code
 * WeaponProperties#cutDamage()}/{@code #pierceDamage()}/{@code
 * #bluntDamage()}. Each type is reduced independently by a target's
 * corresponding {@link ArmorProperties} resistance, so a weapon that
 * mixes types (e.g. a sword with both cut and a small pierce component
 * from its point) has each component mitigated on its own terms rather
 * than by one blended armor value.
 * <p>
 * Purely descriptive metadata — this enum drives no behavior on its
 * own; {@link DamageCalculator} is what reads weapon/armor values keyed
 * by it.
 */
public enum DamageType {
    CUT,
    PIERCE,
    BLUNT
}