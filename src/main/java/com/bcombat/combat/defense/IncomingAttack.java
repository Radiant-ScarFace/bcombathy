package com.bcombat.combat.defense;

import com.bcombat.combat.attack.AttackDirection;
import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

/**
 * Describes a single incoming strike about to connect with a defender,
 * as reported by a future hit-detection/AI/networking system to
 * {@code CombatController#notifyIncomingAttack}. This record is the
 * entire contract between that future system and the defensive
 * mechanics implemented in this phase (Perfect Block, Parry, Chamber) —
 * nothing about damage, weapons, or hit boxes is modeled here, only what
 * the defender needs to judge direction and timing.
 * <p>
 * {@link #id()} exists purely so a single physical swing can be safely
 * re-notified (e.g. once per tick as it closes in) without ever being
 * treated as two separate attacks; {@code CombatController} tracks the
 * last resolved id and ignores repeats, which is what satisfies the
 * "prevent duplicate triggering" requirement for Perfect Block.
 *
 * @param id               unique identity of this specific swing.
 * @param attacker         the attacking entity, if known. Nullable —
 *                          reserved for a future AI/networking system;
 *                          nothing in this phase requires it to be non-null.
 * @param direction         the strike direction the attack is coming in on.
 * @param ticksUntilImpact  ticks remaining until the attack connects, where
 *                          0 is the exact tick of impact. May be negative
 *                          if the notification arrives slightly after the
 *                          nominal impact tick; both directions are
 *                          treated symmetrically by the timing windows.
 */
public record IncomingAttack(UUID id, PlayerEntity attacker, AttackDirection direction, int ticksUntilImpact) {

    /**
     * Convenience constructor for callers that don't need to manage
     * identity themselves; generates a fresh id per swing.
     */
    public IncomingAttack(PlayerEntity attacker, AttackDirection direction, int ticksUntilImpact) {
        this(UUID.randomUUID(), attacker, direction, ticksUntilImpact);
    }
}