package com.bcombat.combat.weapon;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static registry mapping a {@link Item} to the {@link WeaponProperties}
 * that should govern combat behavior while it's held. This is the single
 * extension point future weapon content is expected to use: registering
 * a new weapon is exactly one {@link #register} call and requires no
 * change anywhere in the combat framework itself.
 * <p>
 * Kept as an explicit registry (rather than e.g. an interface every
 * weapon {@code Item} must implement) so vanilla items and items from
 * other mods can be assigned weapon behavior without needing to control
 * their class hierarchy — the same reasoning {@link
 * com.bcombat.combat.controller.CombatControllerManager} documents for
 * why it keys by id rather than attaching state via mixin.
 * <p>
 * Unregistered items (including an empty hand) resolve to {@link
 * WeaponProperties#unarmed()}, so the combat framework always has a
 * valid, neutral set of stats to read regardless of what — if anything —
 * a player is holding.
 */
public final class WeaponRegistry {

    private static final Map<Item, WeaponProperties> REGISTRY = new ConcurrentHashMap<>();

    private WeaponRegistry() {
        // Static registry, no instances.
    }

    /**
     * Registers {@code properties} to govern combat behavior whenever
     * {@code item} is held in the main hand. Overwrites any previous
     * registration for the same item, so a later call (e.g. a datapack
     * or config reload) can freely re-tune an already-registered weapon.
     */
    public static void register(Item item, WeaponProperties properties) {
        Objects.requireNonNull(item, "item must not be null");
        Objects.requireNonNull(properties, "properties must not be null");
        REGISTRY.put(item, properties);
    }

    /** @return true if {@code item} has been registered with explicit weapon properties. */
    public static boolean isRegistered(Item item) {
        return item != null && REGISTRY.containsKey(item);
    }

    /**
     * @return the registered {@link WeaponProperties} for {@code item},
     * or {@link WeaponProperties#unarmed()} if {@code item} is {@code
     * null} or has no registration.
     */
    public static WeaponProperties resolve(Item item) {
        if (item == null) {
            return WeaponProperties.unarmed();
        }
        return REGISTRY.getOrDefault(item, WeaponProperties.unarmed());
    }

    /**
     * Convenience overload for resolving directly from a held stack.
     *
     * @return {@link WeaponProperties#unarmed()} if {@code stack} is
     * {@code null} or empty, otherwise the same result as {@link
     * #resolve(Item)} for {@code stack.getItem()}.
     */
    public static WeaponProperties resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return WeaponProperties.unarmed();
        }
        return resolve(stack.getItem());
    }
}