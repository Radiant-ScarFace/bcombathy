package com.bcombat.combat.couch;

/**
 * The couch lance sub-state-machine a mounted rider wielding a
 * couch-capable weapon (see {@code
 * com.bcombat.combat.weapon.WeaponProperties#isCouchCapable()}) moves
 * through, owned and driven exclusively by {@link CouchLanceController}.
 * <p>
 * This is deliberately a separate state machine from {@code
 * com.bcombat.combat.state.CombatState} rather than new {@code
 * CombatState} constants: couching is a condition layered on top of
 * ordinary mounted combat (a rider can be {@code COMBAT_IDLE} while
 * {@link #PREPARING} or {@link #ACTIVE}, and the underlying {@code
 * CombatState} still drives {@code ATTACKING}/{@code RECOVERY} for the
 * actual strike once thrown), so folding it into {@code CombatState}
 * would force every unrelated system that switches on {@code
 * CombatState} to also understand couching. {@link CouchLanceController}
 * is the only thing allowed to change this state, and only along the
 * flow documented below - every transition is applied through a single
 * internal gate that fires each of {@link
 * com.bcombat.combat.events.CombatEvents}'s five {@code COUCH_*} events
 * exactly once per actual transition, mirroring the guarantee {@code
 * CombatStateManager} and {@code MountedCombatController} already give
 * their own transitions.
 * <p>
 * Flow: {@link #INACTIVE} -&gt; {@link #PREPARING} (eligibility just
 * became true) -&gt; {@link #ACTIVE} (held eligibility for {@code
 * CombatConstants#COUCH_PREPARE_TICKS}, lance now braced and ready to
 * strike) -&gt; {@link #IMPACT} (the couched thrust has been released
 * through the normal attack pipeline) -&gt; {@link #RECOVERY} -&gt;
 * back to {@link #INACTIVE}. {@link #PREPARING} or {@link #ACTIVE} can
 * instead end in {@link #INTERRUPTED} (eligibility lost involuntarily -
 * dismounted, speed dropped, unsafe terrain) or {@link #CANCELLED} (the
 * rider voluntarily backed out), both of which also fall through to
 * {@link #RECOVERY} before returning to {@link #INACTIVE}.
 */
public enum CouchState {

    /** Not couching. The default resting state - eligibility is polled every tick to leave this state. */
    INACTIVE,

    /**
     * Eligibility conditions (mounted, couch-capable weapon equipped,
     * minimum charge speed, safe terrain) are currently held and being
     * timed against {@code CombatConstants#COUCH_PREPARE_TICKS} before
     * the lance becomes {@link #ACTIVE}.
     */
    PREPARING,

    /**
     * The lance is braced and ready: the very next committed attack
     * request is intercepted and redirected into an immediate couched
     * thrust (see {@code CombatController#requestPrepareAttack()}),
     * bypassing the normal wind-up entirely.
     */
    ACTIVE,

    /**
     * The couched thrust has been released into the normal attack
     * pipeline (via {@code CombatController#beginCouchAttackRelease}).
     * Held for exactly as long as the underlying {@code CombatState}
     * stays in its own {@code ATTACKING}/{@code PREPARING_ATTACK}
     * resolution, then falls through to {@link #RECOVERY}.
     */
    IMPACT,

    /**
     * Eligibility was lost involuntarily while {@link #PREPARING} or
     * {@link #ACTIVE} - dismounted, mount speed dropped below {@code
     * CombatConstants#COUCH_MIN_HORSE_SPEED}, or terrain became unsafe.
     * A momentary state: the very next tick always advances into
     * {@link #RECOVERY}.
     */
    INTERRUPTED,

    /**
     * The rider voluntarily cancelled while {@link #PREPARING} or
     * {@link #ACTIVE}. A momentary state: the very next tick always
     * advances into {@link #RECOVERY}.
     */
    CANCELLED,

    /**
     * Post-impact/interrupt/cancel cooldown, independent of and stacked
     * alongside {@code CombatState.RECOVERY}'s own duration for the
     * underlying attack itself. Duration is scaled by the mount's speed
     * at the moment recovery began - see {@code
     * CouchLanceModifiers#recoveryTicks}.
     */
    RECOVERY
}