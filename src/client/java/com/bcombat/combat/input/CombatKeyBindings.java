package com.bcombat.combat.input;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Registers the keybindings this framework needs: the Combat Mode
 * hold-key (default: right mouse button, per the design spec) and the
 * Block hold-key. Deliberately not registering placeholder keybindings
 * for other future systems (chamber blocks, feints) since they have no
 * code to drive them yet.
 * <p>
 * Note: binding Combat Mode to the right mouse button by default overlaps
 * with vanilla's "Use Item" key. This is a known, intentional trade-off
 * called for by the design spec; players can rebind in the controls menu,
 * and a later phase may add priority handling (e.g. suppressing combat
 * mode while actively using an item).
 * <p>
 * Block defaults to the middle mouse button rather than left or right
 * click, since those are already spoken for by vanilla's attack key and
 * Combat Mode respectively; this keeps attack, Combat Mode, and block as
 * three independently-held inputs with no default overlap. Players can
 * rebind in the controls menu.
 */
public final class CombatKeyBindings {

    private static final String CATEGORY = "key.categories.bcombat";

    public static KeyBinding combatMode;
    public static KeyBinding block;

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

        block = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bcombat.block",
                InputUtil.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_3,
                CATEGORY
        ));
    }
}