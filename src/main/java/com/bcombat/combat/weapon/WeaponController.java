package com.bcombat.combat.weapon;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * The dedicated per-combatant controller for weapon detection. Owned by
 * {@code CombatController} exactly the same way it owns {@link
 * com.bcombat.combat.block.BlockController} and {@link
 * com.bcombat.combat.attack.ChamberController} — this class has no
 * knowledge of {@code CombatStateManager} or combat timing, only of
 * which item is currently held and what {@link WeaponProperties} that
 * resolves to, which keeps it trivially testable.
 * <p>
 * Widened to {@link LivingEntity} (rather than {@code PlayerEntity})
 * for the same reason {@code CombatController} itself is — {@code
 * LivingEntity#getMainHandStack()} is available on both a real player
 * and an AI-controlled mob, so the exact same weapon-detection logic
 * drives both without any AI-specific branch.
 * <p>
 * Detection is polling-based (compared once per tick against the
 * previous main-hand item) rather than event-driven, since Minecraft has
 * no vanilla "item equipped" callback for the main hand — the same
 * approach {@code AttackDirectionTracker}/{@code GuardDirectionTracker}
 * use for mouse-movement polling. {@code CombatController} is
 * responsible for turning a detected change into the actual {@code
 * WeaponEquippedEvent}/{@code WeaponUnequippedEvent}/{@code
 * WeaponChangedEvent} notifications; this class only detects and
 * resolves.
 */
public final class WeaponController {

    private Item currentItem;
    private WeaponProperties currentWeapon = WeaponProperties.unarmed();

    /**
     * Re-reads {@code entity}'s main-hand item and, if it differs from
     * what was held last tick, resolves the new item's {@link
     * WeaponProperties} via {@link WeaponRegistry} and updates state.
     * Must be called once per tick while this controller is relevant
     * (i.e. every tick {@code CombatController} is active for this
     * combatant).
     *
     * @return true if the held item changed since the last call.
     */
    public boolean tick(LivingEntity entity) {
        ItemStack stack = entity.getMainHandStack();
        Item item = stack.isEmpty() ? null : stack.getItem();

        if (item == currentItem) {
            return false;
        }

        currentItem = item;
        currentWeapon = WeaponRegistry.resolve(item);
        return true;
    }

    /** @return the item currently held in the main hand, or {@code null} for an empty hand. */
    public Item getCurrentItem() {
        return currentItem;
    }

    /**
     * @return the {@link WeaponProperties} governing combat behavior
     * right now — either a registered weapon's properties, or {@link
     * WeaponProperties#unarmed()} if the main hand is empty or holds an
     * unregistered item. Never {@code null}.
     */
    public WeaponProperties getCurrentWeapon() {
        return currentWeapon;
    }
}