package com.bcombat.combat.input;

import com.bcombat.combat.controller.CombatController;
import com.bcombat.combat.controller.CombatControllerManager;
import com.bcombat.combat.state.CombatState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

/**
 * Polls raw input state and translates it into combat controller requests.
 * Contains zero combat logic itself: it does not decide whether a
 * transition is legal, it only calls {@code requestEnterCombat()} /
 * {@code requestExitCombat()} / {@code requestPrepareAttack()} / {@code
 * releaseAttack()} and lets {@link CombatController} decide.
 * <p>
 * Attack input reuses vanilla's {@code attackKey} binding (left click by
 * default, but respects rebinds) rather than a new keybinding, and is
 * only forwarded while the player is in Combat Mode
 * ({@code COMBAT_IDLE}/{@code PREPARING_ATTACK}) — outside combat mode,
 * vanilla's own attack handling is left completely alone.
 */
public final class CombatInputHandler {

    private boolean wasCombatKeyDown = false;
    private boolean wasAttackKeyDown = false;
    private final AttackDirectionTracker attackDirectionTracker = new AttackDirectionTracker();

    private CombatInputHandler() {
    }

    /**
     * Registers this handler against Fabric's end-of-client-tick event.
     */
    public static void register() {
        CombatInputHandler handler = new CombatInputHandler();
        ClientTickEvents.END_CLIENT_TICK.register(handler::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null) {
            wasCombatKeyDown = false;
            wasAttackKeyDown = false;
            attackDirectionTracker.end();
            return;
        }

        CombatController controller = CombatControllerManager.get(client.player);

        boolean isCombatKeyDown = CombatKeyBindings.combatMode.isPressed();

        if (isCombatKeyDown && !wasCombatKeyDown) {
            controller.requestEnterCombat();
        } else if (!isCombatKeyDown && wasCombatKeyDown) {
            controller.requestExitCombat();
        }
        wasCombatKeyDown = isCombatKeyDown;

        handleAttackInput(client, controller);

        controller.tick();
    }

    private void handleAttackInput(MinecraftClient client, CombatController controller) {
        CombatState state = controller.getCombatState();

        // Recovery input is remembered, not dropped: holding/pressing attack
        // here queues the next wind-up to begin the instant Recovery ends,
        // without ever starting it early. Spam is still prevented — this
        // only affects what happens once Recovery naturally completes.
        if (state == CombatState.RECOVERY) {
            if (client.options.attackKey.isPressed()) {
                controller.bufferNextAttack();
            }
            wasAttackKeyDown = false;
            return;
        }

        // Sneak cancels an in-progress wind-up and returns to COMBAT_IDLE
        // without attacking, per the "allow cancelling" requirement. Chosen
        // over left/right click since both are already spoken for (attack,
        // Combat Mode toggle) and sneak is a free, low-conflict input that
        // reads naturally as "pull back" during a wind-up.
        if (state == CombatState.PREPARING_ATTACK && client.player.isSneaking()) {
            controller.cancelPrepareAttack();
            attackDirectionTracker.end();
            wasAttackKeyDown = false;
            return;
        }

        boolean inAttackEligibleState = state == CombatState.COMBAT_IDLE || state == CombatState.PREPARING_ATTACK;

        boolean isAttackKeyDown = inAttackEligibleState && client.options.attackKey.isPressed();

        if (isAttackKeyDown && !wasAttackKeyDown) {
            controller.requestPrepareAttack();
            if (controller.getCombatState() == CombatState.PREPARING_ATTACK) {
                attackDirectionTracker.begin(client.player);
            }
        } else if (!isAttackKeyDown && wasAttackKeyDown && state == CombatState.PREPARING_ATTACK) {
            controller.releaseAttack();
        }

        if (state == CombatState.PREPARING_ATTACK) {
            controller.updateAttackDirection(attackDirectionTracker.resolve(client.player));
        } else {
            attackDirectionTracker.end();
        }

        wasAttackKeyDown = isAttackKeyDown;
    }
}
