package com.bcombat.combat.movement;

import com.bcombat.combat.util.CombatConstants;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * Applies and removes the movement speed penalties associated with Combat
 * Mode. This is the ONLY class in the framework that touches
 * {@link EntityAttributes#GENERIC_MOVEMENT_SPEED}, so there is exactly one
 * place future systems (stamina, armor weight, mounts) need to look at or
 * extend if they also need to influence movement speed.
 * <p>
 * Modifiers are added/removed rather than mutating the base attribute
 * value directly, so this can never conflict with potions, enchantments,
 * or other mods that also add speed modifiers.
 * <p>
 * Scope note: in this phase (no networking yet) this is applied to the
 * client-side player only, which is standard practice for local
 * movement-feel changes and is safe because these modifiers only ever
 * slow the player down, never speed them up. Syncing this to the server
 * and other clients is explicit future work.
 */
public final class MovementModifierManager {

    private static final Identifier WALK_MODIFIER_ID = new Identifier("bcombat", "combat_walk_speed");
    private static final Identifier SPRINT_MODIFIER_ID = new Identifier("bcombat", "combat_sprint_speed");
    private static final Identifier WIND_UP_MODIFIER_ID = new Identifier("bcombat", "attack_wind_up_speed");
    private static final Identifier RECOVERY_MODIFIER_ID = new Identifier("bcombat", "attack_recovery_speed");
    private static final Identifier EXHAUSTION_MODIFIER_ID = new Identifier("bcombat", "stamina_exhaustion_speed");

    private static final UUID WALK_MODIFIER_UUID = UUID.nameUUIDFromBytes(WALK_MODIFIER_ID.toString().getBytes());
    private static final UUID SPRINT_MODIFIER_UUID = UUID.nameUUIDFromBytes(SPRINT_MODIFIER_ID.toString().getBytes());
    private static final UUID WIND_UP_MODIFIER_UUID = UUID.nameUUIDFromBytes(WIND_UP_MODIFIER_ID.toString().getBytes());
    private static final UUID RECOVERY_MODIFIER_UUID = UUID.nameUUIDFromBytes(RECOVERY_MODIFIER_ID.toString().getBytes());
    private static final UUID EXHAUSTION_MODIFIER_UUID = UUID.nameUUIDFromBytes(EXHAUSTION_MODIFIER_ID.toString().getBytes());

    private boolean combatModifierApplied = false;
    private boolean sprintModifierApplied = false;
    private boolean windUpModifierApplied = false;
    private boolean recoveryModifierApplied = false;
    private boolean exhaustionModifierApplied = false;

    /**
     * Adds the base combat-mode walk speed penalty. Safe to call repeatedly;
     * it will not double-apply.
     */
    public void enableCombatMovement(LivingEntity player) {
        EntityAttributeInstance speedAttribute = getSpeedAttribute(player);
        if (speedAttribute == null || combatModifierApplied) {
            return;
        }

        speedAttribute.addPersistentModifier(new EntityAttributeModifier(
                WALK_MODIFIER_UUID,
                "Combat mode walk penalty",
                CombatConstants.COMBAT_WALK_SPEED_MODIFIER,
                Operation.MULTIPLY_TOTAL
        ));
        combatModifierApplied = true;
    }

    /**
     * Removes all combat-mode movement modifiers. Safe to call repeatedly.
     */
    public void disableCombatMovement(LivingEntity player) {
        EntityAttributeInstance speedAttribute = getSpeedAttribute(player);
        if (speedAttribute != null) {
            speedAttribute.removeModifier(WALK_MODIFIER_UUID);
            speedAttribute.removeModifier(SPRINT_MODIFIER_UUID);
            speedAttribute.removeModifier(WIND_UP_MODIFIER_UUID);
            speedAttribute.removeModifier(EXHAUSTION_MODIFIER_UUID);
        }
        combatModifierApplied = false;
        sprintModifierApplied = false;
        windUpModifierApplied = false;
        exhaustionModifierApplied = false;
    }

    /**
     * Adds the extra speed penalty applied while winding up an attack
     * ({@code CombatState.PREPARING_ATTACK}), on top of the standing
     * combat-mode penalty. Safe to call repeatedly; will not double-apply.
     * Intended to be called by {@link com.bcombat.combat.controller.CombatController}
     * on entering {@code PREPARING_ATTACK}.
     */
    public void enableWindUpPenalty(LivingEntity player) {
        EntityAttributeInstance speedAttribute = getSpeedAttribute(player);
        if (speedAttribute == null || windUpModifierApplied) {
            return;
        }

        speedAttribute.addPersistentModifier(new EntityAttributeModifier(
                WIND_UP_MODIFIER_UUID,
                "Attack wind-up penalty",
                CombatConstants.WIND_UP_SPEED_MODIFIER,
                Operation.MULTIPLY_TOTAL
        ));
        windUpModifierApplied = true;
    }

    /**
     * Removes the wind-up speed penalty. Safe to call repeatedly, including
     * when no wind-up penalty is currently applied. Intended to be called
     * the moment {@code PREPARING_ATTACK} is left for any reason (release,
     * cancel, or forced exit) so the penalty never outlives the wind-up.
     */
    public void disableWindUpPenalty(LivingEntity player) {
        EntityAttributeInstance speedAttribute = getSpeedAttribute(player);
        if (speedAttribute != null) {
            speedAttribute.removeModifier(WIND_UP_MODIFIER_UUID);
        }
        windUpModifierApplied = false;
    }

    /**
     * Adds the extra speed penalty applied while {@code
     * ExhaustionState#EXHAUSTED} (stamina depleted to zero), on top of
     * the standing combat-mode penalty. Safe to call repeatedly; will
     * not double-apply. Intended to be called by {@link
     * com.bcombat.combat.controller.CombatController} the instant
     * exhaustion begins.
     */
    public void enableExhaustionPenalty(LivingEntity player) {
        EntityAttributeInstance speedAttribute = getSpeedAttribute(player);
        if (speedAttribute == null || exhaustionModifierApplied) {
            return;
        }

        speedAttribute.addPersistentModifier(new EntityAttributeModifier(
                EXHAUSTION_MODIFIER_UUID,
                "Stamina exhaustion penalty",
                CombatConstants.EXHAUSTED_MOVEMENT_SPEED_MODIFIER,
                Operation.MULTIPLY_TOTAL
        ));
        exhaustionModifierApplied = true;
    }

    /**
     * Removes the exhaustion speed penalty. Safe to call repeatedly,
     * including when no exhaustion penalty is currently applied.
     * Intended to be called the instant exhaustion ends for any reason
     * (sufficient stamina regenerated, or Combat Mode itself exited).
     */
    public void disableExhaustionPenalty(LivingEntity player) {
        EntityAttributeInstance speedAttribute = getSpeedAttribute(player);
        if (speedAttribute != null) {
            speedAttribute.removeModifier(EXHAUSTION_MODIFIER_UUID);
        }
        exhaustionModifierApplied = false;
    }

    /**
     * Must be called every tick while in Combat Mode so the sprint penalty
     * tracks the player's actual sprint state (strafing/turning remain
     * smooth and unaffected — only sprint speed is additionally reduced).
     */
    public void tick(LivingEntity player) {
        if (!combatModifierApplied) {
            return;
        }

        EntityAttributeInstance speedAttribute = getSpeedAttribute(player);
        if (speedAttribute == null) {
            return;
        }

        boolean shouldHaveSprintPenalty = player.isSprinting();

        if (shouldHaveSprintPenalty && !sprintModifierApplied) {
            speedAttribute.addPersistentModifier(new EntityAttributeModifier(
                    SPRINT_MODIFIER_UUID,
                    "Combat mode sprint penalty",
                    CombatConstants.COMBAT_SPRINT_SPEED_MODIFIER,
                    Operation.MULTIPLY_TOTAL
            ));
            sprintModifierApplied = true;
        } else if (!shouldHaveSprintPenalty && sprintModifierApplied) {
            speedAttribute.removeModifier(SPRINT_MODIFIER_UUID);
            sprintModifierApplied = false;
        }
    }

    private static EntityAttributeInstance getSpeedAttribute(LivingEntity player) {
        return player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
    }
}