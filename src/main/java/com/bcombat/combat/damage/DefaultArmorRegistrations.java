package com.bcombat.combat.damage;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

/**
 * Registers vanilla armor items against the armor framework so it is
 * exercisable in-game without waiting on dedicated armor content — the
 * armor-side equivalent of {@code
 * com.bcombat.combat.weapon.DefaultWeaponRegistrations}.
 * <p>
 * Values are a simple material-tier example pass (leather weakest,
 * netherite strongest), evenly split across cut/pierce/blunt resistance
 * via {@link ArmorProperties.Builder#armorValue}. Tuning individual
 * resistances per material (e.g. chainmail being comparatively weaker
 * against blunt trauma) is explicitly future balancing work; every
 * piece here uses one uniform {@link ArmorProperties#armorValue()} for
 * all three types.
 * <p>
 * {@link ArmorSlot#GLOVES} has no vanilla item to register against — see
 * {@link ArmorSlot}'s class docs — so no gloves are registered here.
 * <p>
 * Call {@link #register()} once, from the mod's common entrypoint. Not
 * client-only: armor resolution runs through {@link ArmorResolver},
 * which reads live entity equipment and has no client-only dependency.
 */
public final class DefaultArmorRegistrations {

    private DefaultArmorRegistrations() {
        // Static registration holder, no instances.
    }

    public static void register() {
        registerTier(ArmorSlot.HELMET, 1.0,
                Items.LEATHER_HELMET, Items.GOLDEN_HELMET, Items.CHAINMAIL_HELMET,
                Items.IRON_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET);

        registerTier(ArmorSlot.CHESTPLATE, 2.0,
                Items.LEATHER_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE,
                Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE);

        registerTier(ArmorSlot.LEGGINGS, 1.5,
                Items.LEATHER_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.CHAINMAIL_LEGGINGS,
                Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS);

        registerTier(ArmorSlot.BOOTS, 1.0,
                Items.LEATHER_BOOTS, Items.GOLDEN_BOOTS, Items.CHAINMAIL_BOOTS,
                Items.IRON_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS);
    }

    /**
     * Registers {@code items} for {@code slot}, scaling {@code
     * baseArmorValue} up through the six vanilla material tiers
     * (leather, gold, chainmail, iron, diamond, netherite) in the exact
     * order Minecraft conventionally lists them. {@code items} must
     * contain exactly six entries in that order.
     */
    private static void registerTier(ArmorSlot slot, double baseArmorValue, Item... items) {
        double[] tierMultipliers = {1.0, 1.5, 2.0, 3.0, 4.0, 4.5};
        for (int i = 0; i < items.length && i < tierMultipliers.length; i++) {
            ArmorProperties properties = ArmorProperties.builder()
                    .armorValue(baseArmorValue * tierMultipliers[i])
                    .build();
            ArmorRegistry.register(items[i], slot, properties);
        }
    }
}