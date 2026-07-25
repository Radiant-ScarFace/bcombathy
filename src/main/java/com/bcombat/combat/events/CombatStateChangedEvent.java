package com.bcombat.combat.events;

import com.bcombat.combat.state.CombatState;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Fired every time {@code CombatStateManager} successfully applies a
 * transition, for any pair of states. This is the general-purpose event;
 * {@link CombatEnterEvent} and {@link CombatExitEvent} are convenience
 * events fired alongside this one for the specific NORMAL/combat boundary.
 */
public record CombatStateChangedEvent(PlayerEntity player, CombatState previousState, CombatState currentState) {
}
