package com.bcombat.combat.animation;

import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.block.GuardDirection;
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
    public void tick(PlayerEntity player, CombatState combatState, MovementMode movementMode, AttackDirection attackDirection, GuardDirection guardDirection) {
        AnimationState target = resolveTargetState(player, combatState, movementMode, attackDirection, guardDirection);
        int blendDuration = isDefensiveReactionState(target)
                ? CombatConstants.DEFENSE_ANIMATION_BLEND_DURATION_TICKS
                : CombatConstants.ANIMATION_BLEND_DURATION_TICKS;
        blender.setTargetState(target, blendDuration);
        blender.tick();
    }

    /**
     * Perfect Block, Parry, and Chamber reactions blend in faster than
     * ordinary locomotion/attack states, since they represent a snap
     * response to precise timing rather than an eased movement change.
     * See {@link CombatConstants#DEFENSE_ANIMATION_BLEND_DURATION_TICKS}.
     */
    private static boolean isDefensiveReactionState(AnimationState state) {
        return state == AnimationState.PERFECT_BLOCK
                || state == AnimationState.PARRY
                || state == AnimationState.CHAMBER_PREPARE
                || state == AnimationState.CHAMBER_SUCCESS;
    }

    private AnimationState resolveTargetState(PlayerEntity player, CombatState combatState, MovementMode movementMode, AttackDirection attackDirection, GuardDirection guardDirection) {
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
        if (combatState == CombatState.ENTER_BLOCK) {
            return AnimationState.ENTER_BLOCK;
        }
        if (combatState == CombatState.EXIT_BLOCK) {
            return AnimationState.EXIT_BLOCK;
        }
        if (combatState == CombatState.BLOCK_IDLE) {
            return guardStateFor(guardDirection);
        }
        if (combatState == CombatState.PERFECT_BLOCK) {
            return AnimationState.PERFECT_BLOCK;
        }
        if (combatState == CombatState.PARRY) {
            return AnimationState.PARRY;
        }
        if (combatState == CombatState.CHAMBER_PREPARE) {
            return AnimationState.CHAMBER_PREPARE;
        }
        if (combatState == CombatState.CHAMBER_SUCCESS) {
            return AnimationState.CHAMBER_SUCCESS;
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

    /**
     * Maps a locked guard direction to its pose. {@code NONE} falls back
     * to the neutral {@code BLOCK_IDLE} pose — the player has raised their
     * guard but hasn't moved the mouse past the deadzone yet to commit to
     * a side.
     */
    private static AnimationState guardStateFor(GuardDirection direction) {
        return switch (direction) {
            case LEFT_GUARD -> AnimationState.GUARD_LEFT;
            case RIGHT_GUARD -> AnimationState.GUARD_RIGHT;
            case UP_GUARD -> AnimationState.GUARD_UP;
            case THRUST_GUARD -> AnimationState.GUARD_THRUST;
            case NONE -> AnimationState.BLOCK_IDLE;
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