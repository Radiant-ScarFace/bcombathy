package com.bcombat.combat.events;

import com.bcombat.combat.weapon.WeaponProperties;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;

/**
 * Fired the moment a player's main hand comes to hold an item where it
 * was previously empty, or changes from one item to another (in which
 * case {@link WeaponUnequippedEvent} fires for the previous item first,
 * followed by this event for the new one, followed by {@link
 * WeaponChangedEvent} summarizing both). Future systems (animation
 * variant selection, damage calculations, AI) should listen to this
 * instead of polling the main hand every tick.
 *
 * @param player the player who equipped the weapon.
 * @param item   the newly held item. Never {@code null} — this event does
 *               not fire for an empty hand, see {@link WeaponUnequippedEvent}.
 * @param weapon the resolved combat stats for {@code item}, i.e. {@code
 *               WeaponRegistry.resolve(item)} at the moment of equipping.
 */
public record WeaponEquippedEvent(LivingEntity player, Item item, WeaponProperties weapon) {
}