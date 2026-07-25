package com.bcombat.combat.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Central registry of every combat event as a Fabric {@link Event}.
 * Future systems (animation, weapons, AI, HUD) subscribe via
 * {@code CombatEvents.COMBAT_ENTER.register(listener)} instead of the
 * controller/state manager exposing direct callback hooks. This keeps
 * the controller decoupled from every listener it might ever have.
 */
public final class CombatEvents {

    private CombatEvents() {
        // Static registry, no instances.
    }

    public static final Event<CombatEnterCallback> COMBAT_ENTER =
            EventFactory.createArrayBacked(CombatEnterCallback.class,
                    callbacks -> event -> {
                        for (CombatEnterCallback callback : callbacks) {
                            callback.onCombatEnter(event);
                        }
                    });

    public static final Event<CombatExitCallback> COMBAT_EXIT =
            EventFactory.createArrayBacked(CombatExitCallback.class,
                    callbacks -> event -> {
                        for (CombatExitCallback callback : callbacks) {
                            callback.onCombatExit(event);
                        }
                    });

    public static final Event<MovementModeChangedCallback> MOVEMENT_MODE_CHANGED =
            EventFactory.createArrayBacked(MovementModeChangedCallback.class,
                    callbacks -> event -> {
                        for (MovementModeChangedCallback callback : callbacks) {
                            callback.onMovementModeChanged(event);
                        }
                    });

    public static final Event<CombatStateChangedCallback> COMBAT_STATE_CHANGED =
            EventFactory.createArrayBacked(CombatStateChangedCallback.class,
                    callbacks -> event -> {
                        for (CombatStateChangedCallback callback : callbacks) {
                            callback.onCombatStateChanged(event);
                        }
                    });

    public static final Event<AttackPreparationStartedCallback> ATTACK_PREPARATION_STARTED =
            EventFactory.createArrayBacked(AttackPreparationStartedCallback.class,
                    callbacks -> event -> {
                        for (AttackPreparationStartedCallback callback : callbacks) {
                            callback.onAttackPreparationStarted(event);
                        }
                    });

    public static final Event<AttackPreparationCancelledCallback> ATTACK_PREPARATION_CANCELLED =
            EventFactory.createArrayBacked(AttackPreparationCancelledCallback.class,
                    callbacks -> event -> {
                        for (AttackPreparationCancelledCallback callback : callbacks) {
                            callback.onAttackPreparationCancelled(event);
                        }
                    });

    public static final Event<AttackReleasedCallback> ATTACK_RELEASED =
            EventFactory.createArrayBacked(AttackReleasedCallback.class,
                    callbacks -> event -> {
                        for (AttackReleasedCallback callback : callbacks) {
                            callback.onAttackReleased(event);
                        }
                    });

    public static final Event<AttackRecoveryStartedCallback> ATTACK_RECOVERY_STARTED =
            EventFactory.createArrayBacked(AttackRecoveryStartedCallback.class,
                    callbacks -> event -> {
                        for (AttackRecoveryStartedCallback callback : callbacks) {
                            callback.onAttackRecoveryStarted(event);
                        }
                    });

    public static final Event<AttackDirectionChangedCallback> ATTACK_DIRECTION_CHANGED =
            EventFactory.createArrayBacked(AttackDirectionChangedCallback.class,
                    callbacks -> event -> {
                        for (AttackDirectionChangedCallback callback : callbacks) {
                            callback.onAttackDirectionChanged(event);
                        }
                    });

    public static final Event<BlockStartedCallback> BLOCK_STARTED =
            EventFactory.createArrayBacked(BlockStartedCallback.class,
                    callbacks -> event -> {
                        for (BlockStartedCallback callback : callbacks) {
                            callback.onBlockStarted(event);
                        }
                    });

    public static final Event<BlockEndedCallback> BLOCK_ENDED =
            EventFactory.createArrayBacked(BlockEndedCallback.class,
                    callbacks -> event -> {
                        for (BlockEndedCallback callback : callbacks) {
                            callback.onBlockEnded(event);
                        }
                    });

    public static final Event<GuardDirectionChangedCallback> GUARD_DIRECTION_CHANGED =
            EventFactory.createArrayBacked(GuardDirectionChangedCallback.class,
                    callbacks -> event -> {
                        for (GuardDirectionChangedCallback callback : callbacks) {
                            callback.onGuardDirectionChanged(event);
                        }
                    });

    public static final Event<PerfectBlockCallback> PERFECT_BLOCK =
            EventFactory.createArrayBacked(PerfectBlockCallback.class,
                    callbacks -> event -> {
                        for (PerfectBlockCallback callback : callbacks) {
                            callback.onPerfectBlock(event);
                        }
                    });

    public static final Event<ParryCallback> PARRY =
            EventFactory.createArrayBacked(ParryCallback.class,
                    callbacks -> event -> {
                        for (ParryCallback callback : callbacks) {
                            callback.onParry(event);
                        }
                    });

    public static final Event<ChamberStartedCallback> CHAMBER_STARTED =
            EventFactory.createArrayBacked(ChamberStartedCallback.class,
                    callbacks -> event -> {
                        for (ChamberStartedCallback callback : callbacks) {
                            callback.onChamberStarted(event);
                        }
                    });

    public static final Event<ChamberSucceededCallback> CHAMBER_SUCCEEDED =
            EventFactory.createArrayBacked(ChamberSucceededCallback.class,
                    callbacks -> event -> {
                        for (ChamberSucceededCallback callback : callbacks) {
                            callback.onChamberSucceeded(event);
                        }
                    });

    @FunctionalInterface
    public interface CombatEnterCallback {
        void onCombatEnter(CombatEnterEvent event);
    }

    @FunctionalInterface
    public interface CombatExitCallback {
        void onCombatExit(CombatExitEvent event);
    }

    @FunctionalInterface
    public interface MovementModeChangedCallback {
        void onMovementModeChanged(MovementModeChangedEvent event);
    }

    @FunctionalInterface
    public interface CombatStateChangedCallback {
        void onCombatStateChanged(CombatStateChangedEvent event);
    }

    @FunctionalInterface
    public interface AttackPreparationStartedCallback {
        void onAttackPreparationStarted(AttackPreparationStartedEvent event);
    }

    @FunctionalInterface
    public interface AttackPreparationCancelledCallback {
        void onAttackPreparationCancelled(AttackPreparationCancelledEvent event);
    }

    @FunctionalInterface
    public interface AttackReleasedCallback {
        void onAttackReleased(AttackReleasedEvent event);
    }

    @FunctionalInterface
    public interface AttackRecoveryStartedCallback {
        void onAttackRecoveryStarted(AttackRecoveryStartedEvent event);
    }

    @FunctionalInterface
    public interface AttackDirectionChangedCallback {
        void onAttackDirectionChanged(AttackDirectionChangedEvent event);
    }

    @FunctionalInterface
    public interface BlockStartedCallback {
        void onBlockStarted(BlockStartedEvent event);
    }

    @FunctionalInterface
    public interface BlockEndedCallback {
        void onBlockEnded(BlockEndedEvent event);
    }

    @FunctionalInterface
    public interface GuardDirectionChangedCallback {
        void onGuardDirectionChanged(GuardDirectionChangedEvent event);
    }

    @FunctionalInterface
    public interface PerfectBlockCallback {
        void onPerfectBlock(PerfectBlockEvent event);
    }

    @FunctionalInterface
    public interface ParryCallback {
        void onParry(ParryEvent event);
    }

    @FunctionalInterface
    public interface ChamberStartedCallback {
        void onChamberStarted(ChamberStartedEvent event);
    }

    @FunctionalInterface
    public interface ChamberSucceededCallback {
        void onChamberSucceeded(ChamberSucceededEvent event);
    }
}