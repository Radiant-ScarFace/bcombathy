package com.bcombat.combat.events;

import com.bcombat.combat.weapon.WeaponProperties;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;

/**
 * Fired the moment a player's main-hand weapon changes in any direction
 * — equip from empty, unequip to empty, or swap from one item to
 * another — summarizing what {@link WeaponEquippedEvent}/{@link
 * WeaponUnequippedEvent} already reported individually this same tick.
 * Systems that only care about "did the effective weapon change" (e.g.
 * a future animation-set selector) can subscribe to just this event
 * instead of both of the others.
 *
 * @param player          the player whose weapon changed.
 * @param previousItem    the item previously held, or {@code null} if the
 *                        hand was empty.
 * @param previousWeapon  the combat stats that applied before this change.
 * @param newItem         the item now held, or {@code null} if the hand
 *                        is now empty.
 * @param newWeapon       the combat stats that apply after this change —
 *                        {@link WeaponProperties#unarmed()} if {@code
 *                        newItem} is {@code null} or unregistered.
 */
public record WeaponChangedEvent(
        PlayerEntity player,
        Item previousItem,
        WeaponProperties previousWeapon,
        Item newItem,
        WeaponProperties newWeapon) {
}