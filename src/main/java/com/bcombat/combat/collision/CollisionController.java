package com.bcombat.combat.collision;

import com.bcombat.combat.util.CombatConstants;
import net.minecraft.entity.LivingEntity;

import java.util.Optional;

/**
 * The dedicated per-player controller for one attack's collision check.
 * Owned by {@code CombatController} exactly the same way it owns {@link
 * com.bcombat.combat.block.BlockController} and {@link
 * com.bcombat.combat.attack.ChamberController} — this class has no
 * knowledge of {@code CombatStateManager}, blocking, or damage, only of
 * when and whether a swing's collision check should run, which keeps it
 * trivially testable.
 * <p>
 * Per the design requirement that collision checks only happen during
 * the release phase, {@code CombatController} is responsible for calling
 * {@link #beginWindow} only when entering {@code CombatState.ATTACKING}
 * and {@link #tick} only while still in that state — this class trusts
 * its caller for that and does not inspect combat state itself.
 * <p>
 * Within {@code ATTACKING}, the actual geometric check only runs during
 * a configurable sub-window (see {@link
 * com.bcombat.combat.util.CombatConstants#COLLISION_WINDOW_START_RATIO}/
 * {@code COLLISION_WINDOW_END_RATIO}) representing the "business end" of
 * the swing, rather than every tick of the whole release — this avoids
 * registering a hit on the first or last frame of the animation, before
 * or after the weapon would plausibly have reached the target.
 * <p>
 * A given attack resolves at most once: the first tick that either finds
 * a target or reaches the end of the window marks this controller
 * resolved, and every subsequent {@link #tick} call is a no-op until
 * {@link #reset()}. This is what prevents one swing from firing more
 * than one hit/miss outcome.
 */
public final class CollisionController {

    private int windowStartTick;
    private int windowEndTick;
    private int ticksElapsed = -1;
    private boolean active;
    private boolean resolved;

    /**
     * Starts a new collision window sized against {@code
     * attackingDurationTicks} — the same weapon-scaled duration {@code
     * CombatController} uses for the {@code ATTACKING} state itself, so
     * the window automatically tracks weapon speed with no hardcoded
     * tick counts. Called once, the instant {@code CombatState.ATTACKING}
     * is entered.
     */
    public void beginWindow(int attackingDurationTicks) {
        int duration = Math.max(1, attackingDurationTicks);
        windowStartTick = Math.round(duration * CombatConstants.COLLISION_WINDOW_START_RATIO);
        windowEndTick = Math.max(windowStartTick,
                Math.round(duration * CombatConstants.COLLISION_WINDOW_END_RATIO));
        ticksElapsed = -1;
        active = true;
        resolved = false;
    }

    /**
     * Clears all state. Called by {@code CombatController} the instant
     * {@code CombatState.ATTACKING} is left for any reason, so a stale
     * window is never reused for the next attack.
     */
    public void reset() {
        active = false;
        resolved = false;
        ticksElapsed = -1;
    }

    /**
     * Advances the window by one tick and, if now inside the active
     * sub-window, attempts a collision check via {@link
     * CollisionDetector#findTarget}. Safe to call every tick while
     * {@code ATTACKING} is active; a no-op once already resolved.
     *
     * @return a present {@link CollisionOutcome} the one tick this
     * attack's collision check resolves (target found, or window closed
     * with none found); empty every other tick.
     */
    public Optional<CollisionOutcome> tick(LivingEntity attacker, double weaponReach) {
        if (!active || resolved) {
            return Optional.empty();
        }

        ticksElapsed++;
        if (ticksElapsed < windowStartTick) {
            return Optional.empty();
        }

        LivingEntity target = CollisionDetector.findTarget(attacker, weaponReach);
        if (target != null) {
            resolved = true;
            return Optional.of(new CollisionOutcome(target, ticksElapsed));
        }

        if (ticksElapsed >= windowEndTick) {
            resolved = true;
            return Optional.of(new CollisionOutcome(null, ticksElapsed));
        }

        return Optional.empty();
    }

    /**
     * Forces immediate resolution as a miss if this attack's window is
     * still active and unresolved — a safety net for {@code
     * CombatController} to call when leaving {@code ATTACKING} (e.g. a
     * very short weapon-scaled duration) so an attack can never leave
     * the release phase without an outcome. A no-op (empty) if already
     * resolved naturally via {@link #tick}, so a swing never fires two
     * outcomes.
     */
    public Optional<CollisionOutcome> forceResolve() {
        if (!active || resolved) {
            return Optional.empty();
        }
        resolved = true;
        return Optional.of(new CollisionOutcome(null, Math.max(ticksElapsed, 0)));
    }
}