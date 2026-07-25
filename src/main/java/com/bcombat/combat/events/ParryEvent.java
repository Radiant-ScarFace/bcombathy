package com.bcombat.combat.events;

import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.block.GuardDirection;
import net.minecraft.entity.LivingEntity;

/**
 * Fired the instant a Parry is confirmed: a Perfect Block that also fell
 * within the tighter {@code CombatConstants#PARRY_WINDOW_TICKS}. Always
 * fired alongside a {@link PerfectBlockEvent} for the same notification.
 * <p>
 * Per the design spec, a Parry interrupts the attacker's animation and
 * returns the defender immediately to full combat control ({@code
 * CombatState.PARRY -> COMBAT_IDLE}). No damage or stagger is applied
 * yet — interrupting the attacker's animation and any future stagger/
 * stamina effects are extension points for future phases to hook here.
 */
public record ParryEvent(LivingEntity defender, LivingEntity attacker,
                         GuardDirection guardDirection, AttackDirection attackDirection) {
}