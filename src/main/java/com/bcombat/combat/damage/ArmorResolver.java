package com.bcombat.combat.damage;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

/**
 * Stateless geometry-free utility that answers "how much protection does
 * {@code target} currently have covering this {@link BodyPart}" by
 * reading its live equipped items through {@link ArmorRegistry}. The
 * armor-side equivalent of {@code
 * com.bcombat.combat.collision.CollisionDetector}: no cached state, just
 * a pure read of the target's current equipment resolved on demand at
 * the moment {@link DamageCalculator} needs it.
 * <p>
 * A stateful, per-tick-polled controller (the pattern {@code
 * WeaponController} uses for the attacker's held item) is intentionally
 * not used here — unlike an attacker's held item, which {@code
 * CombatController} needs to track continuously for direction/timing
 * decisions, a target's armor only ever needs to be known at the single
 * instant a hit is being resolved, so resolving on demand is both
 * simpler and cannot go stale.
 * <p>
 * Works against any {@link LivingEntity}, not just {@link
 * net.minecraft.entity.player.PlayerEntity} — so mobs wearing armor via
 * vanilla mechanics are protected by this framework too, with no extra
 * code required.
 */
public final class ArmorResolver {

    private ArmorResolver() {
        // Stateless utility, no instances.
    }

    /**
     * Resolves the combined protection currently covering {@code
     * bodyPart} on {@code target}.
     * <p>
     * {@link BodyPart#HEAD} reads {@link EquipmentSlot#HEAD}; {@link
     * BodyPart#TORSO} reads {@link EquipmentSlot#CHEST}; {@link
     * BodyPart#LEFT_LEG}/{@link BodyPart#RIGHT_LEG} combine {@link
     * EquipmentSlot#LEGS} and {@link EquipmentSlot#FEET} (leggings and
     * boots both cover the legs). {@link BodyPart#LEFT_ARM}/{@link
     * BodyPart#RIGHT_ARM} correspond to {@link ArmorSlot#GLOVES}, which
     * has no vanilla equipment slot to read — see {@link ArmorSlot}'s
     * class docs — so this always resolves to {@link
     * ArmorProperties#none()} for arms today; the {@link ArmorSlot} and
     * {@link ArmorRegistry} plumbing is already in place for a future
     * accessory-slot integration to fill in without any change here.
     *
     * @return the combined {@link ArmorProperties}, or {@link
     * ArmorProperties#none()} if nothing registered protects that part.
     */
    public static ArmorProperties resolveForBodyPart(LivingEntity target, BodyPart bodyPart) {
        if (target == null || bodyPart == null) {
            return ArmorProperties.none();
        }

        return switch (bodyPart) {
            case HEAD -> fromSlot(target, EquipmentSlot.HEAD);
            case TORSO -> fromSlot(target, EquipmentSlot.CHEST);
            case LEFT_LEG, RIGHT_LEG -> fromSlot(target, EquipmentSlot.LEGS).combine(fromSlot(target, EquipmentSlot.FEET));
            // No vanilla equipment slot covers hands/gloves; reserved
            // extension point (see ArmorSlot#GLOVES javadoc).
            case LEFT_ARM, RIGHT_ARM, UNKNOWN -> ArmorProperties.none();
        };
    }

    private static ArmorProperties fromSlot(LivingEntity target, EquipmentSlot slot) {
        ItemStack stack = target.getEquippedStack(slot);
        if (stack == null || stack.isEmpty()) {
            return ArmorProperties.none();
        }
        return ArmorRegistry.resolveProperties(stack.getItem());
    }
}