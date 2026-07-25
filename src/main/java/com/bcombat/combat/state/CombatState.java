package com.bcombat.combat.state;

import java.util.EnumSet;
import java.util.Set;

/**
 * Every possible combat state a player can occupy. Exactly one state is
 * active at a time; {@link CombatStateManager} is the only thing allowed
 * to change it, and only along the edges declared in {@link #allowedNextStates()}.
 * <p>
 * Future systems (attacks, blocking, feints, chambering) already have
 * their states declared here so this enum will not need structural
 * changes when those systems are implemented — only the code that
 * triggers the transitions will be added.
 */
public enum CombatState {

    /** Vanilla movement/behavior. Not in combat. */
    NORMAL {
        @Override
        public Set<CombatState> allowedNextStates() {
            return EnumSet.of(ENTERING_COMBAT);
        }
    },

    /** Transitional state while the combat stance/animation is engaging. */
    ENTERING_COMBAT {
        @Override
        public Set<CombatState> allowedNextStates() {
            return EnumSet.of(COMBAT_IDLE, EXITING_COMBAT);
        }
    },

    /** Idle, ready stance. The default resting state while in combat. */
    COMBAT_IDLE {
        @Override
        public Set<CombatState> allowedNextStates() {
            return EnumSet.of(PREPARING_ATTACK, ENTER_BLOCK, EXITING_COMBAT);
        }
    },

    /** Reserved for the future attack system: wind-up before a strike. */
    PREPARING_ATTACK {
        @Override
        public Set<CombatState> allowedNextStates() {
            return EnumSet.of(ATTACKING, FEINT, COMBAT_IDLE, CHAMBER_PREPARE);
        }
    },

    /** Reserved for the future attack system: the strike itself. */
    ATTACKING {
        @Override
        public Set<CombatState> allowedNextStates() {
            return EnumSet.of(RECOVERY);
        }
    },

    /** Reserved for the future attack system: post-attack cooldown. */
    RECOVERY {
        @Override
        public Set<CombatState> allowedNextStates() {
            return EnumSet.of(COMBAT_IDLE);
        }
    },

    /** Transitional state while the block stance/animation is engaging. */
    ENTER_BLOCK {
        @Override
        public Set<CombatState> allowedNextStates() {
            return EnumSet.of(BLOCK_IDLE, EXIT_BLOCK);
        }
    },

    /** Idle, held guard. The resting state while actively blocking. */
    BLOCK_IDLE {
        @Override
        public Set<CombatState> allowedNextStates() {
            return EnumSet.of(EXIT_BLOCK, PERFECT_BLOCK, PARRY);
        }
    },

    /** Transitional state while the block stance/animation is disengaging. */
    EXIT_BLOCK {
        @Override
        public Set<CombatState> allowedNextStates() {
            return EnumSet.of(COMBAT_IDLE);
        }
    },

    /** Reserved for the future feint system: a cancelled attack wind-up. */
    FEINT {
        @Override
        public Set<CombatState> allowedNextStates() {
            return EnumSet.of(COMBAT_IDLE);
        }
    },

    /**
     * A correctly-directed guard held within the Perfect Block timing
     * window of an incoming attack (see {@code CombatConstants#PERFECT_BLOCK_WINDOW_TICKS}).
     * Reached only via {@code CombatController#notifyIncomingAttack} —
     * the extension point a future hit-detection system will call.
     * Resolves back to {@link #BLOCK_IDLE} once its dedicated animation
     * has played out.
     */
    PERFECT_BLOCK {
        @Override
        public Set<CombatState> allowedNextStates() {
            return EnumSet.of(BLOCK_IDLE, EXIT_BLOCK);
        }
    },

    /**
     * A Perfect Block landed within the even tighter Parry timing window
     * (see {@code CombatConstants#PARRY_WINDOW_TICKS}). Interrupts the
     * attacker's animation (future extension point) and returns the
     * defender directly to full combat control.
     */
    PARRY {
        @Override
        public Set<CombatState> allowedNextStates() {
            return EnumSet.of(COMBAT_IDLE);
        }
    },

    /**
     * The defender's committed attack direction matched an incoming
     * attack's direction while winding up. Holds briefly while the
     * timing outcome is resolved: success advances to
     * {@link #CHAMBER_SUCCESS}, a mistimed attempt returns to
     * {@link #PREPARING_ATTACK} so the swing simply continues.
     */
    CHAMBER_PREPARE {
        @Override
        public Set<CombatState> allowedNextStates() {
            return EnumSet.of(CHAMBER_SUCCESS, PREPARING_ATTACK);
        }
    },

    /**
     * A chamber resolved successfully: direction matched and timing fell
     * within {@code CombatConstants#CHAMBER_WINDOW_TICKS}. No counter
     * damage is applied yet — this is purely a detection/animation state
     * with extension points for a future counter-attack phase.
     */
    CHAMBER_SUCCESS {
        @Override
        public Set<CombatState> allowedNextStates() {
            return EnumSet.of(COMBAT_IDLE);
        }
    },

    /** Transitional state while the combat stance/animation is disengaging. */
    EXITING_COMBAT {
        @Override
        public Set<CombatState> allowedNextStates() {
            return EnumSet.of(NORMAL);
        }
    };

    /**
     * @return the set of states this state is permitted to transition into.
     * {@link CombatStateManager} consults this before applying any change.
     */
    public abstract Set<CombatState> allowedNextStates();

    /**
     * @return true if this state represents "engaged in combat" in any form,
     * as opposed to {@link #NORMAL}. Useful for movement/animation systems
     * that only need a binary in-combat check.
     */
    public boolean isCombatActive() {
        return this != NORMAL;
    }
}