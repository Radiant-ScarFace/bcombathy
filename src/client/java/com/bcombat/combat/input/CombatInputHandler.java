package com.bcombat.combat.input;

import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.block.GuardDirection;
import com.bcombat.combat.controller.CombatController;
import com.bcombat.combat.controller.CombatControllerManager;
import com.bcombat.combat.state.CombatState;
import com.bcombat.client.network.ClientCombatNetworking;
import com.bcombat.network.packet.CombatActionType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

/**
 * Polls raw input state and translates it into combat controller requests.
 * Contains zero combat logic itself: it does not decide whether a
 * transition is legal, it only calls {@code requestEnterCombat()} /
 * {@code requestExitCombat()} / {@code requestPrepareAttack()} / {@code
 * releaseAttack()} / {@code requestEnterBlock()} / {@code
 * requestExitBlock()} / {@code updateGuardDirection()} and lets {@link
 * CombatController} decide.
 * <p>
 * Attack input reuses vanilla's {@code attackKey} binding (left click by
 * default, but respects rebinds) rather than a new keybinding, and is
 * only forwarded while the player is in Combat Mode
 * ({@code COMBAT_IDLE}/{@code PREPARING_ATTACK}) — outside combat mode,
 * vanilla's own attack handling is left completely alone. Block input
 * uses the dedicated {@link CombatKeyBindings#block} keybinding, since
 * blocking has no vanilla equivalent to reuse the way attacking does.
 * <p>
 * Attack and block cannot both be active at once: entering either only
 * ever succeeds from {@code COMBAT_IDLE}, and the state machine only
 * ever occupies one {@code CombatState} at a time, so this handler needs
 * no extra bookkeeping to keep the two mutually exclusive — it falls
 * directly out of {@link CombatController}'s guard checks.
 */
public final class CombatInputHandler {

    private boolean wasCombatKeyDown = false;
    private boolean wasAttackKeyDown = false;
    private boolean wasBlockKeyDown = false;
    private final AttackDirectionTracker attackDirectionTracker = new AttackDirectionTracker();
    private final GuardDirectionTracker guardDirectionTracker = new GuardDirectionTracker();
    // TEST-ONLY SCAFFOLDING - see DefenseTestSimulator's class doc.
    private final DefenseTestSimulator defenseTestSimulator = new DefenseTestSimulator();

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
            wasBlockKeyDown = false;
            attackDirectionTracker.end();
            guardDirectionTracker.end();
            defenseTestSimulator.onClientTick(client, null);
            return;
        }

        CombatController controller = CombatControllerManager.get(client.player);

        boolean isCombatKeyDown = CombatKeyBindings.combatMode.isPressed();

        if (isCombatKeyDown && !wasCombatKeyDown) {
            controller.requestEnterCombat();
            ClientCombatNetworking.sendAction(CombatActionType.ENTER_COMBAT);
        } else if (!isCombatKeyDown && wasCombatKeyDown) {
            controller.requestExitCombat();
            ClientCombatNetworking.sendAction(CombatActionType.EXIT_COMBAT);
        }
        wasCombatKeyDown = isCombatKeyDown;

        handleAttackInput(client, controller);
        handleBlockInput(client, controller);

        controller.tick();

        // TEST-ONLY SCAFFOLDING - see DefenseTestSimulator's class doc.
        defenseTestSimulator.onClientTick(client, controller);
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
                ClientCombatNetworking.sendAction(CombatActionType.BUFFER_NEXT_ATTACK);
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
            ClientCombatNetworking.sendAction(CombatActionType.CANCEL_PREPARE_ATTACK);
            attackDirectionTracker.end();
            wasAttackKeyDown = false;
            return;
        }

        boolean inAttackEligibleState = state == CombatState.COMBAT_IDLE || state == CombatState.PREPARING_ATTACK;

        boolean isAttackKeyDown = inAttackEligibleState && client.options.attackKey.isPressed();

        if (isAttackKeyDown && !wasAttackKeyDown) {
            controller.requestPrepareAttack();
            ClientCombatNetworking.sendAction(CombatActionType.PREPARE_ATTACK);
            if (controller.getCombatState() == CombatState.PREPARING_ATTACK) {
                attackDirectionTracker.begin(client.player);
            }
        } else if (!isAttackKeyDown && wasAttackKeyDown && state == CombatState.PREPARING_ATTACK) {
            controller.releaseAttack();
            ClientCombatNetworking.sendAction(CombatActionType.RELEASE_ATTACK);
        }

        if (state == CombatState.PREPARING_ATTACK) {
            AttackDirection previousDirection = controller.getAttackDirection();
            AttackDirection resolved = attackDirectionTracker.resolve(client.player);
            controller.updateAttackDirection(resolved);
            // Only send when the committed direction actually changed,
            // to keep wind-up direction tracking cheap on the wire - see
            // AttackDirectionC2SPacket's class docs.
            if (controller.getAttackDirection() != previousDirection) {
                ClientCombatNetworking.sendAttackDirection(controller.getAttackDirection());
            }
        } else {
            attackDirectionTracker.end();
        }

        wasAttackKeyDown = isAttackKeyDown;
    }

    private void handleBlockInput(MinecraftClient client, CombatController controller) {
        CombatState state = controller.getCombatState();

        // Entering a block only ever succeeds from COMBAT_IDLE (see
        // CombatController#requestEnterBlock), so pressing the block key
        // while winding up, attacking, or recovering is silently ignored,
        // exactly like pressing attack while already blocking is.
        boolean isBlockKeyDown = CombatKeyBindings.block.isPressed();

        if (isBlockKeyDown && !wasBlockKeyDown && state == CombatState.COMBAT_IDLE) {
            controller.requestEnterBlock();
            ClientCombatNetworking.sendAction(CombatActionType.ENTER_BLOCK);
            if (controller.getCombatState() == CombatState.ENTER_BLOCK) {
                guardDirectionTracker.begin(client.player);
            }
        } else if (!isBlockKeyDown && wasBlockKeyDown
                && (state == CombatState.ENTER_BLOCK || state == CombatState.BLOCK_IDLE)) {
            controller.requestExitBlock();
            ClientCombatNetworking.sendAction(CombatActionType.EXIT_BLOCK);
            guardDirectionTracker.end();
        }

        // Re-read state: the branch above may have just transitioned
        // COMBAT_IDLE -> ENTER_BLOCK this same tick, and the tracker's
        // begin() call needs to be followed by a resolve() this same
        // tick rather than one tick later.
        state = controller.getCombatState();
        if (state == CombatState.ENTER_BLOCK || state == CombatState.BLOCK_IDLE) {
            GuardDirection previousGuard = controller.getGuardDirection();
            GuardDirection resolved = guardDirectionTracker.resolve(client.player);
            controller.updateGuardDirection(resolved);
            if (controller.getGuardDirection() != previousGuard) {
                ClientCombatNetworking.sendGuardDirection(controller.getGuardDirection());
            }
        } else {
            guardDirectionTracker.end();
        }

        wasBlockKeyDown = isBlockKeyDown;
    }
}