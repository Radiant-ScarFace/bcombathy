package com.bcombat.combat.damage;

/**
 * The five armor piece slots the armor framework supports. Purely
 * descriptive metadata — this enum drives no behavior on its own; the
 * actual protection values live on {@link ArmorProperties} and are
 * configured independently per registered item via {@link
 * ArmorRegistry}.
 * <p>
 * {@link #HELMET}, {@link #CHESTPLATE}, {@link #LEGGINGS}, and {@link
 * #BOOTS} correspond directly to a vanilla {@code EquipmentSlot} and are
 * read live off a target's equipment by {@link ArmorResolver}.
 * {@link #GLOVES} has no vanilla equivalent (Minecraft has no glove
 * slot) — it exists so a future content/mod-integration phase (e.g. a
 * curio/accessory slot) can register glove items against this framework
 * without any change to this enum or {@link ArmorProperties}'s shape;
 * see {@link ArmorResolver} for exactly how it degrades gracefully today.
 */
public enum ArmorSlot {
    HELMET,
    CHESTPLATE,
    GLOVES,
    LEGGINGS,
    BOOTS
}