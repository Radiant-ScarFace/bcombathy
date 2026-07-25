package com.bcombat.combat.collision;

import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.defense.DefenseResult;
import com.bcombat.combat.weapon.WeaponProperties;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;

/**
 * The single reusable data container describing the outcome of one
 * attack's collision resolution — a hit, a miss, or a successful block —
 * as reported by {@code CombatController} to {@code AttackHitEvent},
 * {@code AttackMissEvent}, and {@code AttackBlockedEvent}.
 * <p>
 * This record is deliberately outcome-agnostic (one shape for all three
 * results) rather than three different payloads, since "reusable" is an
 * explicit design requirement: any future listener that cares about
 * "what happened with this swing" can consume one type regardless of
 * which of the three events delivered it. Damage, stamina cost, and
 * knockback are explicitly out of scope for this phase — this only
 * describes whether and where a swing connected.
 *
 * @param attacker          the player who threw the attack.
 * @param target            the entity struck, or {@code null} for a miss.
 * @param weaponItem        the item that performed the attack, or {@code null} if unarmed.
 * @param weaponProperties  the resolved combat stats for {@code weaponItem}
 *                          (or {@link WeaponProperties#unarmed()}) at the moment of the swing.
 * @param direction         the attack direction that was committed for this swing.
 * @param hit               true if the attack connected and was not intercepted by a block.
 * @param blocked           true if a valid target was found but the defender's Perfect
 *                          Block/Parry/Chamber intercepted it; mutually exclusive with {@code hit}.
 * @param defenseResult     the defensive mechanic that intercepted the attack, or
 *                          {@link DefenseResult#NONE} if {@code blocked} is false.
 * @param hitLocation       the approximate body region struck, or {@link HitLocation#UNKNOWN}
 *                          for any outcome other than a confirmed, unblocked hit.
 * @param ticksIntoAttack   how many ticks into the {@code ATTACKING} (release) state the
 *                          collision check resolved on — the exact collision timing.
 * @param worldTime         {@code world.getTime()} at the moment of resolution, for
 *                          listeners that need an absolute timestamp rather than a relative one.
 */
public record HitResult(
        LivingEntity attacker,
        LivingEntity target,
        Item weaponItem,
        WeaponProperties weaponProperties,
        AttackDirection direction,
        boolean hit,
        boolean blocked,
        DefenseResult defenseResult,
        HitLocation hitLocation,
        int ticksIntoAttack,
        long worldTime) {

    /** Builds the result for a confirmed, unblocked hit. */
    public static HitResult hit(LivingEntity attacker, LivingEntity target, Item weaponItem,
                                WeaponProperties weaponProperties, AttackDirection direction,
                                HitLocation hitLocation, int ticksIntoAttack, long worldTime) {
        return new HitResult(attacker, target, weaponItem, weaponProperties, direction,
                true, false, DefenseResult.NONE, hitLocation, ticksIntoAttack, worldTime);
    }

    /** Builds the result for an attack that connected geometrically but was intercepted by a defense. */
    public static HitResult blocked(LivingEntity attacker, LivingEntity target, Item weaponItem,
                                    WeaponProperties weaponProperties, AttackDirection direction,
                                    DefenseResult defenseResult, int ticksIntoAttack, long worldTime) {
        return new HitResult(attacker, target, weaponItem, weaponProperties, direction,
                false, true, defenseResult, HitLocation.UNKNOWN, ticksIntoAttack, worldTime);
    }

    /** Builds the result for a swing that found no valid target within its collision window. */
    public static HitResult miss(LivingEntity attacker, Item weaponItem, WeaponProperties weaponProperties,
                                 AttackDirection direction, int ticksIntoAttack, long worldTime) {
        return new HitResult(attacker, null, weaponItem, weaponProperties, direction,
                false, false, DefenseResult.NONE, HitLocation.UNKNOWN, ticksIntoAttack, worldTime);
    }
}