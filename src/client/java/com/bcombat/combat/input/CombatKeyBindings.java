package com.bcombat.combat.input;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Registers the keybindings this framework needs. Currently only the
 * Combat Mode hold-key (default: right mouse button, per the design
 * spec). Deliberately not registering placeholder keybindings for future
 * systems (attack, block) since they have no code to drive them yet.
 * <p>
 * Note: binding to the right mouse button by default overlaps with
 * vanilla's "Use Item" key. This is a known, intentional trade-off called
 * for by the design spec; players can rebind in the controls menu, and a
 * later phase may add priority handling (e.g. suppressing combat mode
 * while actively using an item).
 */
public final class CombatKeyBindings {

    private static final String CATEGORY = "key.categories.bcombat";

    public static KeyBinding combatMode;

    private CombatKeyBindings() {
        // Static registration holder, no instances.
    }

    public static void register() {
        combatMode = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bcombat.combat_mode",
                InputUtil.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_2,
                CATEGORY
        ));
    }
}
