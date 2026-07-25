package com.bcombat.combat.input;

import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.util.CombatConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Determines {@link AttackDirection} from mouse movement during
 * {@code CombatState.PREPARING_ATTACK}.
 * <p>
 * Deliberately does not hook GLFW cursor deltas directly. Mouse movement
 * already drives the player's yaw/pitch every tick via vanilla's look
 * controls, so this tracker simply remembers the yaw/pitch at the start
 * of the wind-up and classifies the cumulative deviation from that
 * baseline. This keeps direction detection mixin-free and safe to read
 * from {@link CombatInputHandler} on the client tick.
 */
public final class AttackDirectionTracker {

    private float baselineYaw;
    private float baselinePitch;
    private boolean active;

    /**
     * Call once, the instant {@code CombatState.PREPARING_ATTACK} is
     * entered, to record the look direction the wind-up started from.
     */
    public void begin(PlayerEntity player) {
        baselineYaw = player.getYaw();
        baselinePitch = player.getPitch();
        active = true;
    }

    /**
     * Call when leaving {@code PREPARING_ATTACK} for any reason (release,
     * cancel, or forced exit) so a stale baseline is never reused.
     */
    public void end() {
        active = false;
    }

    /**
     * @return the direction implied by look-direction deviation since
     * {@link #begin} was called, or {@code AttackDirection.NONE} if no
     * wind-up is active or the deadzone hasn't been exceeded yet.
     */
    public AttackDirection resolve(PlayerEntity player) {
        if (!active) {
            return AttackDirection.NONE;
        }

        float deltaYaw = MathHelper.wrapDegrees(player.getYaw() - baselineYaw);
        float deltaPitch = player.getPitch() - baselinePitch;

        float absYaw = Math.abs(deltaYaw);
        float absPitch = Math.abs(deltaPitch);

        if (absYaw < CombatConstants.ATTACK_DIRECTION_DEADZONE_DEGREES
                && absPitch < CombatConstants.ATTACK_DIRECTION_DEADZONE_DEGREES) {
            return AttackDirection.NONE;
        }

        if (absYaw >= absPitch) {
            return deltaYaw > 0 ? AttackDirection.RIGHT_SLASH : AttackDirection.LEFT_SLASH;
        }
        // Pitch decreases when looking up in Minecraft's convention.
        return deltaPitch < 0 ? AttackDirection.OVERHEAD : AttackDirection.THRUST;
    }
}
