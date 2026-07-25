package com.bcombat.combat.couch;

import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.controller.CombatController;
import com.bcombat.combat.events.CombatEvents;
import com.bcombat.combat.events.CouchCancelledEvent;
import com.bcombat.combat.events.CouchImpactEvent;
import com.bcombat.combat.events.CouchInterruptedEvent;
import com.bcombat.combat.events.CouchRecoveredEvent;
import com.bcombat.combat.events.CouchStartedEvent;
import com.bcombat.combat.state.CombatState;
import com.bcombat.combat.util.CombatConstants;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Objects;

/**
 * The dedicated per-combatant controller for the Couch Lance Combat
 * framework — owned by {@link CombatController} exactly the same way it
 * owns {@code MountedCombatController}, {@code WeaponController}, and
 * {@code CollisionController}. This class has no knowledge of collision
 * detection, stamina, or damage numbers; it only ever drives the {@link
 * CouchState} state machine and reports state, mirroring {@code
 * MountedCombatController}'s own "report state, never compute gameplay
 * numbers itself" split with {@link CouchLanceModifiers}.
 * <p>
 * See {@link CouchState}'s class docs for the full transition flow this
 * class implements: {@code INACTIVE -> PREPARING -> ACTIVE -> IMPACT ->
 * RECOVERY -> INACTIVE}, with {@code PREPARING}/{@code ACTIVE} able to
 * instead fall through {@code INTERRUPTED} or {@code CANCELLED} into
 * {@code RECOVERY}.
 * <p>
 * <b>Correctness guarantees (see the class-level verification the
 * couch phase specifically calls for):</b> every transition is applied
 * through the single {@link #transitionTo(CouchState)} gate, which is a
 * no-op (including no event) whenever the requested state equals the
 * current one — exactly the guard {@code MountedCombatController#tick()}
 * and {@code CombatStateManager#transitionTo} already use — so a given
 * transition, and therefore its matching {@code COUCH_*} event, can
 * never fire more than once. {@link #onImpactReleased} additionally
 * guards on {@code state == ACTIVE} before transitioning, so even a
 * defensive/duplicate call from {@link CombatController} can never
 * double-fire {@link CouchImpactEvent} for the same strike. Every
 * instance of this class is owned by exactly one {@link
 * CombatController} instance the same way {@code
 * MountedCombatController} is, so a server-authoritative instance and a
 * client-predictive instance for the same player each own an
 * independent {@link CouchLanceController} with independent state —
 * one instance's transition can never double-count against the other's.
 */
public final class CouchLanceController {

    private final LivingEntity player;
    private final CombatController owner;

    private CouchState state = CouchState.INACTIVE;
    private int ticksInState = 0;
    private int cooldownTicksRemaining = 0;

    /**
     * The mount's horizontal speed at the instant of {@link
     * #onImpactReleased}, expressed as a multiple of {@link
     * CombatConstants#COUCH_MIN_HORSE_SPEED} — frozen for the duration
     * of {@link CouchState#IMPACT} so {@link CouchLanceModifiers} scales
     * the damage bonus for a strike by the speed it was actually thrown
     * at, not whatever the mount's speed happens to be by the time the
     * hit resolves a tick or two later.
     */
    private double impactSpeedRatio = 0.0;

    /**
     * The speed ratio {@link #tickRecovery()} scales {@link
     * CouchLanceModifiers#recoveryTicks} against — {@link
     * #impactSpeedRatio} when {@code RECOVERY} was reached from {@link
     * CouchState#IMPACT}, or the mount's speed at the moment {@code
     * INTERRUPTED}/{@code CANCELLED} fell through to {@code RECOVERY}
     * otherwise (0.0, i.e. the longest recovery, if the rider was no
     * longer even mounted at that instant).
     */
    private double recoverySpeedRatio = 0.0;

    public CouchLanceController(LivingEntity player, CombatController owner) {
        this.player = Objects.requireNonNull(player, "player must not be null");
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
    }

    /** @return the current position in the couch state machine. */
    public CouchState getState() {
        return state;
    }

    /** @return true if a couched lance is braced and ready ({@link CouchState#ACTIVE}). */
    public boolean isActive() {
        return state == CouchState.ACTIVE;
    }

    /** @return true if couch eligibility is currently being timed ({@link CouchState#PREPARING}). */
    public boolean isPreparing() {
        return state == CouchState.PREPARING;
    }

    /** @return the frozen speed ratio of the strike currently (or most recently) in {@link CouchState#IMPACT}. */
    public double getImpactSpeedRatio() {
        return impactSpeedRatio;
    }

    /**
     * Advances the couch state machine by one tick. Must be called
     * exactly once per {@link CombatController#tick()}, after that
     * controller's own {@code advanceTransitionTimers()} has run, so
     * {@link #tickImpact()}'s read of {@link CombatController#getCombatState()}
     * reflects this tick's underlying attack progress rather than last
     * tick's.
     */
    public void tick() {
        if (cooldownTicksRemaining > 0) {
            cooldownTicksRemaining--;
        }

        switch (state) {
            case INACTIVE -> tickInactive();
            case PREPARING -> tickPreparing();
            case ACTIVE -> tickActive();
            case IMPACT -> tickImpact();
            case INTERRUPTED -> tickInterrupted();
            case CANCELLED -> tickCancelled();
            case RECOVERY -> tickRecovery();
        }
    }

    /**
     * Voluntarily backs out of couching while {@link CouchState#PREPARING}
     * or {@link CouchState#ACTIVE}. No-op in every other state, so this
     * is always safe to call speculatively (e.g. from a player releasing
     * whatever input started couching, or an AI abandoning a charge).
     */
    public void cancel() {
        if (state == CouchState.PREPARING || state == CouchState.ACTIVE) {
            transitionTo(CouchState.CANCELLED);
        }
    }

    /**
     * Called exactly once by {@link CombatController#beginCouchAttackRelease()}
     * — and only by it — the instant a braced couched lance is actually
     * released into the normal attack pipeline, i.e. immediately after
     * that method's own {@code PREPARING_ATTACK -> ATTACKING} transition
     * has already succeeded. Applies the {@code ACTIVE -> IMPACT}
     * transition and freezes {@link #impactSpeedRatio} for the strike.
     * A no-op if this controller is not currently {@link CouchState#ACTIVE}
     * (e.g. a defensive/duplicate call), which is what guarantees {@link
     * CouchImpactEvent} can only ever fire once per real strike.
     *
     * @param mountSpeedRatio the mount's horizontal speed at release, as
     *                        a multiple of {@link CombatConstants#COUCH_MIN_HORSE_SPEED}.
     */
    public void onImpactReleased(double mountSpeedRatio) {
        if (state != CouchState.ACTIVE) {
            return;
        }
        this.impactSpeedRatio = mountSpeedRatio;
        transitionTo(CouchState.IMPACT);
    }

    // ------------------------------------------------------------------
    // Per-state tick handlers
    // ------------------------------------------------------------------

    private void tickInactive() {
        if (cooldownTicksRemaining > 0) {
            return;
        }
        if (owner.getCombatState() == CombatState.COMBAT_IDLE && isEligible()) {
            transitionTo(CouchState.PREPARING);
        }
    }

    private void tickPreparing() {
        if (!isEligible()) {
            recoverySpeedRatio = currentSpeedRatio();
            transitionTo(CouchState.INTERRUPTED);
            return;
        }
        ticksInState++;
        if (ticksInState >= CombatConstants.COUCH_PREPARE_TICKS) {
            transitionTo(CouchState.ACTIVE);
        }
    }

    private void tickActive() {
        if (!isEligible()) {
            recoverySpeedRatio = currentSpeedRatio();
            transitionTo(CouchState.INTERRUPTED);
        }
        // Otherwise holds ACTIVE indefinitely, waiting for the next
        // committed attack request - CombatController#requestPrepareAttack
        // intercepts that request and redirects it into
        // beginCouchAttackRelease(), which calls onImpactReleased() above.
    }

    private void tickImpact() {
        CombatState combatState = owner.getCombatState();
        if (combatState != CombatState.ATTACKING && combatState != CombatState.PREPARING_ATTACK) {
            // The underlying attack this couched strike was released
            // into has resolved (hit/miss/blocked and moved on to
            // RECOVERY, or further) - couching's own RECOVERY begins now.
            recoverySpeedRatio = impactSpeedRatio;
            transitionTo(CouchState.RECOVERY);
        }
    }

    private void tickInterrupted() {
        // Momentary state: always advances the very next tick, per
        // CouchState#INTERRUPTED's class docs.
        transitionTo(CouchState.RECOVERY);
    }

    private void tickCancelled() {
        // Momentary state: always advances the very next tick, per
        // CouchState#CANCELLED's class docs.
        transitionTo(CouchState.RECOVERY);
    }

    private void tickRecovery() {
        ticksInState++;
        int recoveryTicks = CouchLanceModifiers.recoveryTicks(recoverySpeedRatio);
        if (ticksInState >= recoveryTicks) {
            cooldownTicksRemaining = CombatConstants.COUCH_COOLDOWN_TICKS;
            transitionTo(CouchState.INACTIVE);
        }
    }

    // ------------------------------------------------------------------
    // Transition gate & event firing
    // ------------------------------------------------------------------

    /**
     * The single gate every transition in this class is applied
     * through. A no-op (including no event) if {@code next} equals the
     * current state, which is what guarantees a given transition - and
     * therefore its matching {@code COUCH_*} event - can never fire more
     * than once. See this class's docs for the full guarantee.
     */
    private void transitionTo(CouchState next) {
        if (next == state) {
            return;
        }
        CouchState previous = state;
        state = next;
        ticksInState = 0;
        fireTransitionEvent(previous, next);
    }

    private void fireTransitionEvent(CouchState previous, CouchState next) {
        switch (next) {
            case PREPARING -> CombatEvents.COUCH_STARTED.invoker()
                    .onCouchStarted(new CouchStartedEvent(player, owner.getMount()));
            case CANCELLED -> CombatEvents.COUCH_CANCELLED.invoker()
                    .onCouchCancelled(new CouchCancelledEvent(player, previous));
            case INTERRUPTED -> CombatEvents.COUCH_INTERRUPTED.invoker()
                    .onCouchInterrupted(new CouchInterruptedEvent(player, previous));
            case IMPACT -> CombatEvents.COUCH_IMPACT.invoker()
                    .onCouchImpact(new CouchImpactEvent(player, owner.getMount(), impactSpeedRatio));
            case INACTIVE -> {
                if (previous == CouchState.RECOVERY) {
                    CombatEvents.COUCH_RECOVERED.invoker().onCouchRecovered(new CouchRecoveredEvent(player));
                }
            }
            default -> {
                // ACTIVE and RECOVERY entries have no dedicated event of
                // their own - PREPARING's COUCH_STARTED already marks the
                // attempt beginning, and IMPACT/COUCH_STARTED/COUCH_CANCELLED/
                // COUCH_INTERRUPTED/COUCH_RECOVERED are the five events
                // CombatEvents declares for this framework.
            }
        }
    }

    // ------------------------------------------------------------------
    // Eligibility & terrain safety
    // ------------------------------------------------------------------

    /**
     * @return true if every couch eligibility condition (framework
     * enabled, mounted, minimum charge speed, a couch-capable weapon
     * that also supports {@link AttackDirection#THRUST}, and safe
     * terrain ahead) currently holds. Used both to decide whether {@code
     * INACTIVE} may advance to {@code PREPARING} (alongside the
     * additional {@code COMBAT_IDLE} gate in {@link #tickInactive()})
     * and to detect an involuntary loss of eligibility while {@code
     * PREPARING}/{@code ACTIVE}.
     */
    private boolean isEligible() {
        return CombatConstants.COUCH_LANCE_ENABLED
                && owner.isMounted()
                && owner.getMountSpeed() >= CombatConstants.COUCH_MIN_HORSE_SPEED
                && owner.getWeaponProperties().isCouchCapable()
                && owner.getWeaponProperties().supportsAttackDirection(AttackDirection.THRUST)
                && isTerrainSafe();
    }

    /** @return the mount's current speed as a multiple of {@link CombatConstants#COUCH_MIN_HORSE_SPEED}, or 0.0 if not mounted. */
    private double currentSpeedRatio() {
        if (!owner.isMounted()) {
            return 0.0;
        }
        return owner.getMountSpeed() / CombatConstants.COUCH_MIN_HORSE_SPEED;
    }

    /**
     * @return true if the mount is not currently in a fluid (when {@link
     * CombatConstants#COUCH_REQUIRE_DRY_TERRAIN} requires that), has no
     * solid obstacle directly ahead within {@link
     * CombatConstants#COUCH_TERRAIN_CHECK_DISTANCE}, and has solid
     * ground within reach directly ahead (i.e. no cliff edge to charge
     * off of). False (unsafe) whenever not currently mounted.
     */
    private boolean isTerrainSafe() {
        Entity mount = owner.getMount();
        if (mount == null) {
            return false;
        }
        if (CombatConstants.COUCH_REQUIRE_DRY_TERRAIN && (mount.isTouchingWater() || mount.isInLava())) {
            return false;
        }

        Vec3d facing = mount.getRotationVector();
        Vec3d horizontal = new Vec3d(facing.x, 0.0, facing.z);
        if (horizontal.lengthSquared() < 1.0E-4) {
            // No meaningful horizontal facing to raycast (looking
            // straight up/down) - nothing ahead to check against.
            return true;
        }
        horizontal = horizontal.normalize();

        double checkDistance = CombatConstants.COUCH_TERRAIN_CHECK_DISTANCE;
        Vec3d bodyStart = mount.getPos().add(0.0, Math.max(0.5, mount.getHeight() * 0.5), 0.0);
        Vec3d bodyEnd = bodyStart.add(horizontal.multiply(checkDistance));
        if (!isRayClear(mount, bodyStart, bodyEnd)) {
            // A wall/obstacle is directly ahead within range.
            return false;
        }

        Vec3d groundCheckTop = mount.getPos().add(horizontal.multiply(checkDistance)).add(0.0, 0.5, 0.0);
        Vec3d groundCheckBottom = groundCheckTop.add(0.0, -2.0, 0.0);
        // A cliff edge means no ground is found within this short drop -
        // i.e. the ray reaches the bottom without hitting anything.
        return !isRayClear(mount, groundCheckTop, groundCheckBottom);
    }

    private boolean isRayClear(Entity mount, Vec3d start, Vec3d end) {
        RaycastContext context = new RaycastContext(
                start, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mount);
        BlockHitResult result = mount.getWorld().raycast(context);
        return result.getType() == HitResult.Type.MISS;
    }
}