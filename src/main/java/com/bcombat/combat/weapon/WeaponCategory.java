package com.bcombat.combat.weapon;

/**
 * The broad archetype a weapon belongs to. Purely descriptive metadata —
 * this enum drives no behavior on its own; every actual gameplay-facing
 * value (reach, speed, supported directions, etc.) lives on {@link
 * WeaponProperties} and is configured independently per weapon. Category
 * exists so future systems (animation variant selection, damage
 * calculations, AI weapon preference) have a stable, coarse label to key
 * off of without needing to inspect individual stats.
 * <p>
 * {@link #UNARMED} is the category used when no weapon (or no registered
 * weapon) is held; see {@link WeaponProperties#unarmed()}.
 */
public enum WeaponCategory {
    UNARMED,
    ONE_HANDED_SWORD,
    TWO_HANDED_SWORD,
    AXE,
    MACE,
    SPEAR,
    POLEARM,
    DAGGER
}