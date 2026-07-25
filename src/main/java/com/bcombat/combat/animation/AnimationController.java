package com.bcombat.combat.animation;

import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.movement.MovementMode;
import com.bcombat.combat.state.CombatState;
import com.bcombat.combat.util.CombatConstants;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Determines which {@link AnimationState} a player should be playing based
 * on their {@link CombatState} and physical movement, and drives an
 * {@link AnimationBlender} so transitions never snap.
 * <p>
 * This class produces data only — {@code getCurrentState()} and
 * {@code getBlendWeight()} — and does not touch any renderer. Wiring
 * these into a GeckoLib model (or vanilla model layer) is an explicit
 * extension point for a future phase, once weapon-specific animations
 * are in scope.
 */
public final class AnimationController {

    private final AnimationBlender blender = new AnimationBlender(AnimationState.IDLE);

    /**
     * Recomputes the target animation state from current combat state and
     * player physics, and advances the blend. Must be called once per tick.
     */
    public void tick(PlayerEntity player, CombatState combatState, MovementMode movementMode, AttackDirection attackDirection) {
        AnimationState target = resolveTargetState(player, combatState, movementMode, attackDirection);
        blender.setTargetState(target);
        blender.tick();
    }

    private AnimationState resolveTargetState(PlayerEntity player, CombatState combatState, MovementMode movementMode, AttackDirection attackDirection) {
        if (combatState == CombatState.ENTERING_COMBAT) {
            return AnimationState.ENTER_COMBAT;
        }
        if (combatState == CombatState.EXITING_COMBAT) {
            return AnimationState.EXIT_COMBAT;
        }
        if (combatState == CombatState.PREPARING_ATTACK) {
            return windUpStateFor(attackDirection);
        }
        if (combatState == CombatState.ATTACKING) {
            return releaseStateFor(attackDirection);
        }
        if (combatState == CombatState.RECOVERY) {
            return AnimationState.RECOVERY;
        }

        boolean inCombat = movementMode == MovementMode.COMBAT;

        if (!player.isOnGround()) {
            return inCombat ? AnimationState.COMBAT_JUMP : AnimationState.JUMP;
        }

        double horizontalSpeed = horizontalSpeed(player);

        if (player.isSprinting() && horizontalSpeed >= CombatConstants.RUN_ANIMATION_SPEED_THRESHOLD) {
            return inCombat ? AnimationState.COMBAT_SPRINT : AnimationState.SPRINT;
        }
        if (horizontalSpeed >= CombatConstants.RUN_ANIMATION_SPEED_THRESHOLD) {
            return inCombat ? AnimationState.COMBAT_RUN : AnimationState.RUN;
        }
        if (horizontalSpeed >= CombatConstants.WALK_ANIMATION_SPEED_THRESHOLD) {
            return inCombat ? AnimationState.COMBAT_WALK : AnimationState.WALK;
        }
        return inCombat ? AnimationState.COMBAT_IDLE : AnimationState.IDLE;
    }

    /**
     * Maps a committed (or not-yet-committed) direction to a wind-up
     * pose. {@code NONE} falls back to the thrust pose — a neutral,
     * forward-facing wind-up — since the player is holding the attack
     * but hasn't moved the mouse enough to commit to a side yet.
     */
    private static AnimationState windUpStateFor(AttackDirection direction) {
        return switch (direction) {
            case LEFT_SLASH -> AnimationState.WIND_UP_LEFT;
            case RIGHT_SLASH -> AnimationState.WIND_UP_RIGHT;
            case OVERHEAD -> AnimationState.WIND_UP_OVERHEAD;
            case THRUST, NONE -> AnimationState.WIND_UP_THRUST;
        };
    }

    private static AnimationState releaseStateFor(AttackDirection direction) {
        return switch (direction) {
            case LEFT_SLASH -> AnimationState.RELEASE_LEFT;
            case RIGHT_SLASH -> AnimationState.RELEASE_RIGHT;
            case OVERHEAD -> AnimationState.RELEASE_OVERHEAD;
            case THRUST, NONE -> AnimationState.RELEASE_THRUST;
        };
    }

    private static double horizontalSpeed(PlayerEntity player) {
        double dx = player.getVelocity().x;
        double dz = player.getVelocity().z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public AnimationState getCurrentState() {
        return blender.getCurrentState();
    }

    public AnimationState getPreviousState() {
        return blender.getPreviousState();
    }

    public float getBlendWeight() {
        return blender.getBlendWeight();
    }
}
