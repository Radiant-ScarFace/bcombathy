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
            return EnumSet.of(PREPARING_ATTACK, BLOCKING, EXITING_COMBAT);
        }
    },

    /** Reserved for the future attack system: wind-up before a strike. */
    PREPARING_ATTACK {
        @Override
        public Set<CombatState> allowedNextStates() {
            return EnumSet.of(ATTACKING, FEINT, COMBAT_IDLE);
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

    /** Reserved for the future blocking system. */
    BLOCKING {
        @Override
        public Set<CombatState> allowedNextStates() {
            return EnumSet.of(CHAMBER, COMBAT_IDLE);
        }
    },

    /** Reserved for the future feint system: a cancelled attack wind-up. */
    FEINT {
        @Override
        public Set<CombatState> allowedNextStates() {
            return EnumSet.of(COMBAT_IDLE);
        }
    },

    /** Reserved for the future chamber-block system. */
    CHAMBER {
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
