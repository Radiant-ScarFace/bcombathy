package com.bcombat.combat.weapon;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

/**
 * Registers a small set of vanilla items against the weapon framework so
 * the framework is exercisable in-game without waiting on dedicated
 * weapon content.
 * <p>
 * <b>This is category assignment only, not balancing.</b> Every
 * registration below uses {@link WeaponProperties#builder}'s untouched
 * defaults (every modifier at 1.0, every direction supported) — the
 * exact same neutral values {@link WeaponProperties#unarmed()} uses.
 * Combat feel is therefore identical to being unarmed; the only
 * observable difference is {@link WeaponProperties#category()} and the
 * fact that {@code WeaponRegistry.isRegistered} now returns {@code
 * true}. Tuning these values (reach, speed, supported directions, etc.)
 * per weapon is explicitly future balancing work, out of scope here —
 * see the class-level restriction in the framework's design brief.
 * <p>
 * Call {@link #register()} once, from the mod's common entrypoint. Not
 * client-only: weapon resolution runs through {@code CombatController},
 * which is written to be usable server-side once networking exists.
 */
public final class DefaultWeaponRegistrations {

    private DefaultWeaponRegistrations() {
        // Static registration holder, no instances.
    }

    public static void register() {
        registerAll(WeaponCategory.ONE_HANDED_SWORD,
                Items.WOODEN_SWORD, Items.STONE_SWORD, Items.GOLDEN_SWORD,
                Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);

        registerAll(WeaponCategory.AXE,
                Items.WOODEN_AXE, Items.STONE_AXE, Items.GOLDEN_AXE,
                Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE);

        // The trident is the closest vanilla analogue to a thrust-focused
        // polearm/spear, making it a convenient example for that category.
        registerAll(WeaponCategory.SPEAR, Items.TRIDENT);
    }

    private static void registerAll(WeaponCategory category, Item... items) {
        WeaponProperties properties = WeaponProperties.builder(category).build();
        for (Item item : items) {
            WeaponRegistry.register(item, properties);
        }
    }
}