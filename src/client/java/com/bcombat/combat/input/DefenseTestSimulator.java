package com.bcombat.combat.input;

import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.block.GuardDirection;
import com.bcombat.combat.controller.CombatController;
import com.bcombat.combat.defense.DefenseResult;
import com.bcombat.combat.defense.DirectionCompatibility;
import com.bcombat.combat.defense.IncomingAttack;
import com.bcombat.combat.state.CombatState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * TEST-ONLY SCAFFOLDING.
 * <p>
 * Hit detection, AI, and networking are all out of scope for this phase,
 * so there is no real system yet that can call {@code
 * CombatController#notifyIncomingAttack}. This class exists purely so
 * Perfect Block, Parry, and Chamber can be verified in-game while that
 * real system doesn't exist yet, driven by {@link
 * CombatKeyBindings#debugSimulateIncomingAttack}. It should be deleted
 * once a real hit-detection system exists to call {@code
 * notifyIncomingAttack} instead.
 * <p>
 * Rather than asking the tester to aim a direction for the simulated
 * strike, this derives a plausible incoming attack from whatever the
 * defender is currently doing, via {@link
 * DirectionCompatibility#matchingAttack}: while blocking, the strike
 * mirrors the currently locked guard direction (so it should always
 * qualify for Perfect Block/Parry); while winding up an attack, the
 * strike mirrors the player's own committed attack direction (so it
 * should always qualify for Chamber). Outside those two states there is
 * nothing meaningful to simulate, and the press is ignored.
 * <p>
 * The simulated impact is always immediate ({@code ticksUntilImpact ==
 * 0}), which falls inside every timing window this framework currently
 * defines ({@code PERFECT_BLOCK_WINDOW_TICKS}, {@code
 * PARRY_WINDOW_TICKS}, {@code CHAMBER_WINDOW_TICKS}). This is
 * deliberate: this tool exists to exercise the defensive pathways, not
 * to tune their timing windows, so it always hits the best-case result
 * for whichever pathway direction matching selects.
 */
final class DefenseTestSimulator {

    /**
     * Polls the debug keybinding and, if pressed and a controller is
     * available, simulates one incoming attack against it. Safe to call
     * with a {@code null} controller (e.g. while {@code client.player}
     * is {@code null}) purely to keep the keybinding's queued-press
     * state from accumulating stale presses.
     */
    void onClientTick(MinecraftClient client, CombatController controller) {
        while (CombatKeyBindings.debugSimulateIncomingAttack.wasPressed()) {
            if (controller != null) {
                simulate(client, controller);
            }
        }
    }

    private void simulate(MinecraftClient client, CombatController controller) {
        AttackDirection direction = deriveDirection(controller);
        if (direction == AttackDirection.NONE) {
            return;
        }

        IncomingAttack incoming = new IncomingAttack(null, direction, 0);
        DefenseResult result = controller.notifyIncomingAttack(incoming);

        if (client.player != null) {
            client.player.sendMessage(Text.literal("[DefenseTestSimulator] " + direction + " -> " + result), true);
        }
    }

    /**
     * @return the direction a simulated strike should arrive on, based
     * on what the defender is currently doing, or {@link
     * AttackDirection#NONE} if the current state has nothing meaningful
     * to test against.
     */
    private AttackDirection deriveDirection(CombatController controller) {
        CombatState state = controller.getCombatState();

        if (state == CombatState.ENTER_BLOCK || state == CombatState.BLOCK_IDLE) {
            GuardDirection guard = controller.getGuardDirection();
            return DirectionCompatibility.matchingAttack(guard);
        }

        if (state == CombatState.PREPARING_ATTACK) {
            return controller.getAttackDirection();
        }

        return AttackDirection.NONE;
    }
}