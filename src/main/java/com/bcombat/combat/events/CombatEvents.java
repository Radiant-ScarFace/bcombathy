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

    public static final Event<MountedStateChangedCallback> MOUNTED_STATE_CHANGED =
            EventFactory.createArrayBacked(MountedStateChangedCallback.class,
                    callbacks -> event -> {
                        for (MountedStateChangedCallback callback : callbacks) {
                            callback.onMountedStateChanged(event);
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

    public static final Event<WeaponEquippedCallback> WEAPON_EQUIPPED =
            EventFactory.createArrayBacked(WeaponEquippedCallback.class,
                    callbacks -> event -> {
                        for (WeaponEquippedCallback callback : callbacks) {
                            callback.onWeaponEquipped(event);
                        }
                    });

    public static final Event<WeaponUnequippedCallback> WEAPON_UNEQUIPPED =
            EventFactory.createArrayBacked(WeaponUnequippedCallback.class,
                    callbacks -> event -> {
                        for (WeaponUnequippedCallback callback : callbacks) {
                            callback.onWeaponUnequipped(event);
                        }
                    });

    public static final Event<WeaponChangedCallback> WEAPON_CHANGED =
            EventFactory.createArrayBacked(WeaponChangedCallback.class,
                    callbacks -> event -> {
                        for (WeaponChangedCallback callback : callbacks) {
                            callback.onWeaponChanged(event);
                        }
                    });

    public static final Event<CollisionDetectedCallback> COLLISION_DETECTED =
            EventFactory.createArrayBacked(CollisionDetectedCallback.class,
                    callbacks -> event -> {
                        for (CollisionDetectedCallback callback : callbacks) {
                            callback.onCollisionDetected(event);
                        }
                    });

    public static final Event<AttackHitCallback> ATTACK_HIT =
            EventFactory.createArrayBacked(AttackHitCallback.class,
                    callbacks -> event -> {
                        for (AttackHitCallback callback : callbacks) {
                            callback.onAttackHit(event);
                        }
                    });

    public static final Event<AttackMissCallback> ATTACK_MISS =
            EventFactory.createArrayBacked(AttackMissCallback.class,
                    callbacks -> event -> {
                        for (AttackMissCallback callback : callbacks) {
                            callback.onAttackMiss(event);
                        }
                    });

    public static final Event<AttackBlockedCallback> ATTACK_BLOCKED =
            EventFactory.createArrayBacked(AttackBlockedCallback.class,
                    callbacks -> event -> {
                        for (AttackBlockedCallback callback : callbacks) {
                            callback.onAttackBlocked(event);
                        }
                    });

    public static final Event<DamageCalculatedCallback> DAMAGE_CALCULATED =
            EventFactory.createArrayBacked(DamageCalculatedCallback.class,
                    callbacks -> event -> {
                        for (DamageCalculatedCallback callback : callbacks) {
                            callback.onDamageCalculated(event);
                        }
                    });

    public static final Event<ArmorReducedDamageCallback> ARMOR_REDUCED_DAMAGE =
            EventFactory.createArrayBacked(ArmorReducedDamageCallback.class,
                    callbacks -> event -> {
                        for (ArmorReducedDamageCallback callback : callbacks) {
                            callback.onArmorReducedDamage(event);
                        }
                    });

    public static final Event<CriticalHitCallback> CRITICAL_HIT =
            EventFactory.createArrayBacked(CriticalHitCallback.class,
                    callbacks -> event -> {
                        for (CriticalHitCallback callback : callbacks) {
                            callback.onCriticalHit(event);
                        }
                    });

    public static final Event<DamageAppliedCallback> DAMAGE_APPLIED =
            EventFactory.createArrayBacked(DamageAppliedCallback.class,
                    callbacks -> event -> {
                        for (DamageAppliedCallback callback : callbacks) {
                            callback.onDamageApplied(event);
                        }
                    });

    public static final Event<StaggerTriggeredCallback> STAGGER_TRIGGERED =
            EventFactory.createArrayBacked(StaggerTriggeredCallback.class,
                    callbacks -> event -> {
                        for (StaggerTriggeredCallback callback : callbacks) {
                            callback.onStaggerTriggered(event);
                        }
                    });

    public static final Event<StaminaChangedCallback> STAMINA_CHANGED =
            EventFactory.createArrayBacked(StaminaChangedCallback.class,
                    callbacks -> event -> {
                        for (StaminaChangedCallback callback : callbacks) {
                            callback.onStaminaChanged(event);
                        }
                    });

    public static final Event<StaminaDepletedCallback> STAMINA_DEPLETED =
            EventFactory.createArrayBacked(StaminaDepletedCallback.class,
                    callbacks -> event -> {
                        for (StaminaDepletedCallback callback : callbacks) {
                            callback.onStaminaDepleted(event);
                        }
                    });

    public static final Event<StaminaRegeneratedCallback> STAMINA_REGENERATED =
            EventFactory.createArrayBacked(StaminaRegeneratedCallback.class,
                    callbacks -> event -> {
                        for (StaminaRegeneratedCallback callback : callbacks) {
                            callback.onStaminaRegenerated(event);
                        }
                    });

    public static final Event<ExhaustionStartedCallback> EXHAUSTION_STARTED =
            EventFactory.createArrayBacked(ExhaustionStartedCallback.class,
                    callbacks -> event -> {
                        for (ExhaustionStartedCallback callback : callbacks) {
                            callback.onExhaustionStarted(event);
                        }
                    });

    public static final Event<ExhaustionEndedCallback> EXHAUSTION_ENDED =
            EventFactory.createArrayBacked(ExhaustionEndedCallback.class,
                    callbacks -> event -> {
                        for (ExhaustionEndedCallback callback : callbacks) {
                            callback.onExhaustionEnded(event);
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

    @FunctionalInterface
    public interface WeaponEquippedCallback {
        void onWeaponEquipped(WeaponEquippedEvent event);
    }

    @FunctionalInterface
    public interface WeaponUnequippedCallback {
        void onWeaponUnequipped(WeaponUnequippedEvent event);
    }

    @FunctionalInterface
    public interface WeaponChangedCallback {
        void onWeaponChanged(WeaponChangedEvent event);
    }

    @FunctionalInterface
    public interface CollisionDetectedCallback {
        void onCollisionDetected(CollisionDetectedEvent event);
    }

    @FunctionalInterface
    public interface AttackHitCallback {
        void onAttackHit(AttackHitEvent event);
    }

    @FunctionalInterface
    public interface AttackMissCallback {
        void onAttackMiss(AttackMissEvent event);
    }

    @FunctionalInterface
    public interface AttackBlockedCallback {
        void onAttackBlocked(AttackBlockedEvent event);
    }

    @FunctionalInterface
    public interface DamageCalculatedCallback {
        void onDamageCalculated(DamageCalculatedEvent event);
    }

    @FunctionalInterface
    public interface ArmorReducedDamageCallback {
        void onArmorReducedDamage(ArmorReducedDamageEvent event);
    }

    @FunctionalInterface
    public interface CriticalHitCallback {
        void onCriticalHit(CriticalHitEvent event);
    }

    @FunctionalInterface
    public interface DamageAppliedCallback {
        void onDamageApplied(DamageAppliedEvent event);
    }

    @FunctionalInterface
    public interface StaggerTriggeredCallback {
        void onStaggerTriggered(StaggerTriggeredEvent event);
    }

    @FunctionalInterface
    public interface StaminaChangedCallback {
        void onStaminaChanged(StaminaChangedEvent event);
    }

    @FunctionalInterface
    public interface StaminaDepletedCallback {
        void onStaminaDepleted(StaminaDepletedEvent event);
    }

    @FunctionalInterface
    public interface StaminaRegeneratedCallback {
        void onStaminaRegenerated(StaminaRegeneratedEvent event);
    }

    @FunctionalInterface
    public interface ExhaustionStartedCallback {
        void onExhaustionStarted(ExhaustionStartedEvent event);
    }

    @FunctionalInterface
    public interface ExhaustionEndedCallback {
        void onExhaustionEnded(ExhaustionEndedEvent event);
    }

    @FunctionalInterface
    public interface MountedStateChangedCallback {
        void onMountedStateChanged(MountedStateChangedEvent event);
    }
}