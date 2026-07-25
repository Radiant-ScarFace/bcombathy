package com.bcombat.combat.events;

import com.bcombat.combat.weapon.WeaponProperties;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;

/**
 * Fired the moment a player's main hand stops holding an item — either
 * because it became empty, or because it changed to a different item (in
 * which case this event fires for the previous item first, followed by
 * {@link WeaponEquippedEvent} for the new one, followed by {@link
 * WeaponChangedEvent} summarizing both).
 *
 * @param player the player who unequipped the weapon.
 * @param item   the item that was previously held. Never {@code null} —
 *               this event does not fire when the hand was already empty.
 * @param weapon the combat stats {@code item} resolved to while it was held.
 */
public record WeaponUnequippedEvent(PlayerEntity player, Item item, WeaponProperties weapon) {
}