package com.bcombat.combat.input;

import com.bcombat.combat.block.GuardDirection;
import com.bcombat.combat.util.CombatConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Determines {@link GuardDirection} from mouse movement during {@code
 * CombatState.ENTER_BLOCK}/{@code BLOCK_IDLE}.
 * <p>
 * Deliberately mirrors {@link AttackDirectionTracker} rather than reusing
 * it: this tracker remembers the yaw/pitch baseline recorded the instant
 * block began and classifies the cumulative deviation from that baseline,
 * same mixin-free approach, kept as a separate class since attack and
 * block direction detection are independently tunable (see {@link
 * CombatConstants#GUARD_DIRECTION_DEADZONE_DEGREES} vs {@link
 * CombatConstants#ATTACK_DIRECTION_DEADZONE_DEGREES}) and are driven by
 * different lifecycles (a wind-up is momentary; a block can be held
 * indefinitely).
 * <p>
 * This class only classifies raw mouse movement into a candidate
 * direction — it does not decide whether that candidate should actually
 * replace the currently locked guard direction. That decision (the
 * "remain locked until deliberately changed" behavior, with a switching
 * delay) belongs to {@code com.bcombat.combat.block.BlockController},
 * which every candidate this class produces is proposed to via {@code
 * CombatController#updateGuardDirection}.
 */
public final class GuardDirectionTracker {

    private float baselineYaw;
    private float baselinePitch;
    private boolean active;

    /**
     * Call once, the instant {@code CombatState.ENTER_BLOCK} is entered,
     * to record the look direction the block started from.
     */
    public void begin(PlayerEntity player) {
        baselineYaw = player.getYaw();
        baselinePitch = player.getPitch();
        active = true;
    }

    /**
     * Call when leaving block for any reason (release or forced exit) so
     * a stale baseline is never reused for the next block.
     */
    public void end() {
        active = false;
    }

    /**
     * @return the direction candidate implied by look-direction deviation
     * since {@link #begin} was called, or {@code GuardDirection.NONE} if
     * no block is active or the deadzone hasn't been exceeded yet. This
     * is a proposal only — see class-level docs.
     */
    public GuardDirection resolve(PlayerEntity player) {
        if (!active) {
            return GuardDirection.NONE;
        }

        float deltaYaw = MathHelper.wrapDegrees(player.getYaw() - baselineYaw) * CombatConstants.GUARD_DIRECTION_SENSITIVITY;
        float deltaPitch = (player.getPitch() - baselinePitch) * CombatConstants.GUARD_DIRECTION_SENSITIVITY;

        float absYaw = Math.abs(deltaYaw);
        float absPitch = Math.abs(deltaPitch);

        if (absYaw < CombatConstants.GUARD_DIRECTION_DEADZONE_DEGREES
                && absPitch < CombatConstants.GUARD_DIRECTION_DEADZONE_DEGREES) {
            return GuardDirection.NONE;
        }

        if (absYaw >= absPitch) {
            return deltaYaw > 0 ? GuardDirection.RIGHT_GUARD : GuardDirection.LEFT_GUARD;
        }
        // Pitch decreases when looking up in Minecraft's convention,
        // mirroring AttackDirectionTracker's OVERHEAD/THRUST split.
        return deltaPitch < 0 ? GuardDirection.UP_GUARD : GuardDirection.THRUST_GUARD;
    }
}