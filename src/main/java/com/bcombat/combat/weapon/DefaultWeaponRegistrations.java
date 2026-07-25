package com.bcombat.combat.weapon;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

/**
 * Registers a small set of vanilla items against the weapon framework so
 * the framework is exercisable in-game without waiting on dedicated
 * weapon content.
 * <p>
 * <b>Timing/handling values are category assignment only, not
 * balancing</b> — every registration below leaves {@link
 * WeaponProperties#builder}'s speed/recovery/wind-up modifiers at their
 * untouched neutral default of 1.0, the exact same values {@link
 * WeaponProperties#unarmed()} uses, so attack/block/chamber *timing*
 * feel is identical to being unarmed. Tuning those values per weapon is
 * still explicitly future balancing work.
 * <p>
 * <b>Damage values are a real (if modest) example pass</b>, since the
 * damage framework (see {@code com.bcombat.combat.damage}) needs
 * non-zero, per-weapon {@link WeaponProperties#cutDamage()}/{@link
 * WeaponProperties#pierceDamage()}/{@link WeaponProperties#bluntDamage()}
 * to be exercisable in-game at all. Every weapon here keeps {@link
 * WeaponProperties#baseDamage()} at the builder default of {@code 1.0}
 * and layers a category-appropriate typed component on top — swords cut
 * and lightly pierce with their point, axes cut hard, the trident
 * (standing in for a spear/polearm) is a pure thrust/pierce weapon.
 * Material-tier scaling (wood < stone < iron < diamond < netherite) is
 * intentionally not modeled here; every item in a category shares one
 * {@link WeaponProperties} instance, same as before this phase.
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
                properties(WeaponCategory.ONE_HANDED_SWORD).cutDamage(4.0).pierceDamage(1.0).build(),
                Items.WOODEN_SWORD, Items.STONE_SWORD, Items.GOLDEN_SWORD,
                Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);

        registerAll(WeaponCategory.AXE,
                properties(WeaponCategory.AXE).cutDamage(6.0).bluntDamage(1.0).build(),
                Items.WOODEN_AXE, Items.STONE_AXE, Items.GOLDEN_AXE,
                Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE);

        // The trident is the closest vanilla analogue to a thrust-focused
        // polearm/spear, making it a convenient example for that category.
        registerAll(WeaponCategory.SPEAR,
                properties(WeaponCategory.SPEAR).pierceDamage(5.0).build(),
                Items.TRIDENT);
    }

    private static WeaponProperties.Builder properties(WeaponCategory category) {
        return WeaponProperties.builder(category);
    }

    private static void registerAll(WeaponCategory category, WeaponProperties properties, Item... items) {
        for (Item item : items) {
            WeaponRegistry.register(item, properties);
        }
    }
}