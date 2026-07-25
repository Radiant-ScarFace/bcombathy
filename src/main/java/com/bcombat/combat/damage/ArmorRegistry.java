package com.bcombat.combat.damage;

import net.minecraft.item.Item;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static registry mapping a {@link Item} to the {@link ArmorSlot} it
 * occupies and the {@link ArmorProperties} that should protect the
 * wearer while it's equipped — the armor-side equivalent of {@code
 * com.bcombat.combat.weapon.WeaponRegistry}. This is the single
 * extension point future armor content is expected to use: registering
 * a new armor piece is exactly one {@link #register} call and requires
 * no change anywhere in the damage framework itself.
 * <p>
 * Kept as an explicit registry (rather than requiring every armor
 * {@code Item} implement an interface) for the exact same reason {@code
 * WeaponRegistry} documents: vanilla items and items from other mods can
 * be assigned armor behavior without controlling their class hierarchy.
 * <p>
 * Unregistered items resolve to an empty {@link Optional}, and {@link
 * ArmorResolver} treats that identically to an empty equipment slot —
 * {@link ArmorProperties#none()} — so the damage framework always has a
 * valid, neutral value to read regardless of what a target is wearing.
 */
public final class ArmorRegistry {

    private static final Map<Item, Registration> REGISTRY = new ConcurrentHashMap<>();

    private ArmorRegistry() {
        // Static registry, no instances.
    }

    /**
     * Registers {@code properties} to protect the wearer whenever {@code
     * item} is equipped in the slot corresponding to {@code slot}.
     * Overwrites any previous registration for the same item.
     */
    public static void register(Item item, ArmorSlot slot, ArmorProperties properties) {
        Objects.requireNonNull(item, "item must not be null");
        Objects.requireNonNull(slot, "slot must not be null");
        Objects.requireNonNull(properties, "properties must not be null");
        REGISTRY.put(item, new Registration(slot, properties));
    }

    /** @return true if {@code item} has been registered with explicit armor properties. */
    public static boolean isRegistered(Item item) {
        return item != null && REGISTRY.containsKey(item);
    }

    /**
     * @return the registered {@link ArmorSlot}/{@link ArmorProperties}
     * pair for {@code item}, or an empty {@link Optional} if {@code
     * item} is {@code null} or has no registration.
     */
    public static Optional<Registration> resolve(Item item) {
        if (item == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(REGISTRY.get(item));
    }

    /**
     * @return the registered {@link ArmorProperties} for {@code item},
     * or {@link ArmorProperties#none()} if unregistered.
     */
    public static ArmorProperties resolveProperties(Item item) {
        return resolve(item).map(Registration::properties).orElse(ArmorProperties.none());
    }

    /** One registration: the slot an item occupies and the protection it grants while worn there. */
    public record Registration(ArmorSlot slot, ArmorProperties properties) {
    }
}