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
 * <p>
 * Flick detection: in addition to the cumulative baseline deviation,
 * this tracker also watches the single-tick yaw/pitch delta. A tick
 * whose delta meets {@link CombatConstants#ATTACK_MOUSE_SENSITIVITY_THRESHOLD}
 * is classified as a deliberate flick, which temporarily relieves the
 * direction deadzone (by {@link CombatConstants#ATTACK_FLICK_DEADZONE_RELIEF_RATIO})
 * for that single {@link #resolve} call only — a fast, decisive mouse
 * snap commits a direction sooner than a slow drift crossing the same
 * cumulative deviation would, without shrinking the deadzone for a slow
 * drift and reintroducing jitter-flicking.
 */
public final class AttackDirectionTracker {

    private float baselineYaw;
    private float baselinePitch;
    private boolean active;

    private float lastSampleYaw;
    private float lastSamplePitch;
    private boolean hasLastSample;

    /**
     * Call once, the instant {@code CombatState.PREPARING_ATTACK} is
     * entered, to record the look direction the wind-up started from.
     */
    public void begin(PlayerEntity player) {
        baselineYaw = player.getYaw();
        baselinePitch = player.getPitch();
        active = true;

        lastSampleYaw = baselineYaw;
        lastSamplePitch = baselinePitch;
        hasLastSample = true;
    }

    /**
     * Call when leaving {@code PREPARING_ATTACK} for any reason (release,
     * cancel, or forced exit) so a stale baseline is never reused.
     */
    public void end() {
        active = false;
        hasLastSample = false;
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

        boolean flick = isFlickTick(player);

        float deltaYaw = MathHelper.wrapDegrees(player.getYaw() - baselineYaw);
        float deltaPitch = player.getPitch() - baselinePitch;

        float absYaw = Math.abs(deltaYaw);
        float absPitch = Math.abs(deltaPitch);

        float deadzone = CombatConstants.ATTACK_DIRECTION_DEADZONE_DEGREES;
        if (flick) {
            deadzone *= CombatConstants.ATTACK_FLICK_DEADZONE_RELIEF_RATIO;
        }

        if (absYaw < deadzone && absPitch < deadzone) {
            return AttackDirection.NONE;
        }

        if (absYaw >= absPitch) {
            return deltaYaw > 0 ? AttackDirection.RIGHT_SLASH : AttackDirection.LEFT_SLASH;
        }
        // Pitch decreases when looking up in Minecraft's convention.
        return deltaPitch < 0 ? AttackDirection.OVERHEAD : AttackDirection.THRUST;
    }

    /**
     * Compares this tick's yaw/pitch against the previous tick's sample
     * to decide whether this tick's mouse movement was a deliberate
     * flick, then updates the sample for the next call. Kept separate
     * from the cumulative baseline deviation used by {@link #resolve} —
     * a flick is about how fast the mouse is currently moving, not how
     * far it has moved in total since the wind-up began.
     */
    private boolean isFlickTick(PlayerEntity player) {
        float currentYaw = player.getYaw();
        float currentPitch = player.getPitch();

        boolean flick = false;
        if (hasLastSample) {
            float tickDeltaYaw = Math.abs(MathHelper.wrapDegrees(currentYaw - lastSampleYaw));
            float tickDeltaPitch = Math.abs(currentPitch - lastSamplePitch);
            flick = tickDeltaYaw >= CombatConstants.ATTACK_MOUSE_SENSITIVITY_THRESHOLD
                    || tickDeltaPitch >= CombatConstants.ATTACK_MOUSE_SENSITIVITY_THRESHOLD;
        }

        lastSampleYaw = currentYaw;
        lastSamplePitch = currentPitch;
        hasLastSample = true;

        return flick;
    }
}