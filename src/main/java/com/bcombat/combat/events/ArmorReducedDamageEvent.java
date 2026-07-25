package com.bcombat.combat.events;

import com.bcombat.combat.damage.DamageResult;

/**
 * Fired whenever a hit's computed damage was actually reduced by armor
 * (i.e. {@code result.armorReductionAmount() > 0}) — a subset of {@link
 * DamageCalculatedEvent} specifically for future systems that only care
 * about armor's effect (e.g. an armor-durability system, or a "your
 * armor absorbed N damage" combat log line) without needing to inspect
 * the full breakdown themselves on every hit.
 *
 * @param result the full computed breakdown for this hit; {@code result.armorApplied()} is never {@code null} here.
 */
public record ArmorReducedDamageEvent(DamageResult result) {
}