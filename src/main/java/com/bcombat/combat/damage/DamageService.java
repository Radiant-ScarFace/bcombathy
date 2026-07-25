package com.bcombat.combat.damage;

import com.bcombat.combat.collision.HitResult;
import com.bcombat.combat.events.ArmorReducedDamageEvent;
import com.bcombat.combat.events.AttackHitEvent;
import com.bcombat.combat.events.CombatEvents;
import com.bcombat.combat.events.CriticalHitEvent;
import com.bcombat.combat.events.DamageAppliedEvent;
import com.bcombat.combat.events.DamageCalculatedEvent;
import com.bcombat.combat.events.StaggerTriggeredEvent;

/**
 * Wires the damage & armor framework into the rest of the combat system
 * entirely through the existing event bus — the single integration point
 * that turns a confirmed collision into an actual combat result.
 * <p>
 * This is what satisfies the requirement that damage calculation stay
 * completely separated from collision detection: {@code
 * CombatController} and the whole {@code
 * com.bcombat.combat.collision} package have zero references to
 * anything in this package. Instead, {@link #register()} subscribes to
 * {@link CombatEvents#ATTACK_HIT} — an event the collision/combat
 * framework already fires for every confirmed, unblocked hit — and this
 * class reacts to it. The collision framework would behave identically
 * with this class never registered at all.
 * <p>
 * On each {@link AttackHitEvent}, in order:
 * <ol>
 *     <li>{@link DamageCalculator#calculate} computes the full {@link DamageResult} (pure, no side effects).</li>
 *     <li>{@link CombatEvents#DAMAGE_CALCULATED} fires with the result.</li>
 *     <li>{@link CombatEvents#ARMOR_REDUCED_DAMAGE} fires if armor actually reduced the damage.</li>
 *     <li>{@link CombatEvents#CRITICAL_HIT} fires if the hit qualified as a critical.</li>
 *     <li>{@link DamageApplier#apply} applies the final damage to the target's health.</li>
 *     <li>{@link CombatEvents#DAMAGE_APPLIED} fires once the health change has actually happened.</li>
 *     <li>{@link CombatEvents#STAGGER_TRIGGERED} fires if the hit met the stagger threshold — an
 *     extension point only; nothing in this framework reacts to it yet.</li>
 * </ol>
 * Call {@link #register()} once, from the mod's common entrypoint,
 * alongside {@code DefaultWeaponRegistrations.register()} and {@link
 * DefaultArmorRegistrations#register()}.
 */
public final class DamageService {

    private DamageService() {
        // Static wiring holder, no instances.
    }

    /** Subscribes this service to {@link CombatEvents#ATTACK_HIT}. Safe to call exactly once. */
    public static void register() {
        CombatEvents.ATTACK_HIT.register(DamageService::onAttackHit);
    }

    private static void onAttackHit(AttackHitEvent event) {
        HitResult hitResult = event.result();
        if (!hitResult.hit() || hitResult.target() == null) {
            // Defensive guard only - AttackHitEvent is documented to
            // always carry a confirmed hit, but this keeps the service
            // safe against a future caller firing it differently.
            return;
        }

        DamageResult result = DamageCalculator.calculate(hitResult);

        CombatEvents.DAMAGE_CALCULATED.invoker().onDamageCalculated(new DamageCalculatedEvent(result));

        if (result.armorReductionAmount() > 0.0 && result.armorApplied() != null) {
            CombatEvents.ARMOR_REDUCED_DAMAGE.invoker().onArmorReducedDamage(new ArmorReducedDamageEvent(result));
        }

        if (result.critical()) {
            CombatEvents.CRITICAL_HIT.invoker().onCriticalHit(new CriticalHitEvent(result));
        }

        DamageApplier.apply(result);
        CombatEvents.DAMAGE_APPLIED.invoker().onDamageApplied(new DamageAppliedEvent(result));

        if (result.staggerTriggered()) {
            CombatEvents.STAGGER_TRIGGERED.invoker().onStaggerTriggered(new StaggerTriggeredEvent(result));
        }
    }
}