package com.bcombat.combat.ai;

import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.block.GuardDirection;
import com.bcombat.combat.controller.CombatController;
import com.bcombat.combat.controller.CombatControllerManager;
import com.bcombat.combat.defense.DirectionCompatibility;
import com.bcombat.combat.state.CombatState;
import com.bcombat.combat.weapon.WeaponProperties;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * The AI decision layer that lets a {@link MobEntity} fight through the
 * exact same {@link CombatController} public API a real player's {@code
 * CombatInputHandler} drives — requesting combat mode, committing wind-ups,
 * choosing attack/guard directions, releasing swings, raising and
 * lowering guards. This class contains no combat rules of its own: every
 * legality check (can this state be entered, does the weapon support
 * this direction, is stamina sufficient) is already enforced by {@link
 * CombatController} itself, exactly as it is for a human player. If a
 * decision made here is illegal, the corresponding {@code request*}/
 * {@code update*} call is simply a no-op — the same way a player
 * mashing an input at the wrong time is a no-op — so this class can
 * never bypass the state machine, attack pipeline, blocking, stamina,
 * collision, or damage systems.
 * <p>
 * What this class does NOT do:
 * <ul>
 *     <li>Target acquisition — it reads whatever {@link MobEntity#getTarget()}
 *     vanilla's own goal selector already picked. There is no duplicate
 *     "who to fight" system here.</li>
 *     <li>Damage, collision, or hit resolution — those remain exactly
 *     {@code CollisionController}/{@code CollisionDetector}/{@code
 *     DamageService}'s responsibility, unchanged and unaware this class
 *     exists.</li>
 *     <li>Animation, movement-speed penalties, or stamina accounting —
 *     all still owned by {@link CombatController} and its sub-controllers.</li>
 * </ul>
 * What it does do, once per server tick via {@link #tick()}:
 * <ol>
 *     <li>Tactical positioning/distance management — approach, hold, or
 *     retreat via vanilla's own {@code EntityNavigation}, based on the
 *     equipped weapon's reach and the configured {@link AIDifficultyPreset}.</li>
 *     <li>Facing — turns to face the target via vanilla's own {@code
 *     LookControl}, since {@code CollisionDetector#findTarget} requires
 *     the attacker to actually be looking at the target's forward cone,
 *     exactly like a player physically aiming their camera.</li>
 *     <li>Combat decisions — attacking, blocking, and defensive
 *     direction-matching, all expressed purely as calls into {@link
 *     CombatController}'s existing public API.</li>
 * </ol>
 * One instance exists per AI-controlled combatant, owned by {@link
 * AICombatManager} — never instantiated per-tick, and never holding a
 * second reference to combat state outside what {@link CombatController}
 * itself already exposes.
 */
public final class AICombatController {

    private static final double NAVIGATION_SPEED = 1.0;
    private static final double RETREAT_SPEED = 0.9;
    private static final double APPROACH_SLACK = 0.75;
    private static final double RETREAT_TRIGGER_RATIO = 0.55;
    private static final double ENGAGEMENT_ENTRY_SLACK = 1.15;

    private static final Set<CombatState> COMMITTED_STATES = Set.of(
            CombatState.ATTACKING,
            CombatState.PERFECT_BLOCK,
            CombatState.PARRY,
            CombatState.CHAMBER_PREPARE,
            CombatState.CHAMBER_SUCCESS
    );

    private final MobEntity entity;
    private final CombatController combatController;
    private final AIDifficultyPreset difficulty;
    private final Random random = new Random();

    private AITacticalIntent tacticalIntent = AITacticalIntent.IDLE;

    // Wind-up (PREPARING_ATTACK) bookkeeping.
    private boolean attackDirectionChosen = false;
    private int windUpTicksWaited = 0;

    // Reactive-block (COMBAT_IDLE) bookkeeping.
    private int opponentThreatTicks = 0;
    private boolean blockDecisionMade = false;

    // RECOVERY chain-attack bookkeeping.
    private boolean chainDecisionMade = false;

    public AICombatController(MobEntity entity, AIDifficultyPreset difficulty) {
        this.entity = Objects.requireNonNull(entity, "entity must not be null");
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty must not be null");
        // Same registry, same authoritative/non-authoritative rules as
        // every player - see CombatControllerManager's class docs.
        this.combatController = CombatControllerManager.get(entity);
    }

    public MobEntity getEntity() {
        return entity;
    }

    /** @return the same {@link CombatController} a player's input handler would drive for this combatant. */
    public CombatController getCombatController() {
        return combatController;
    }

    public AIDifficultyPreset getDifficulty() {
        return difficulty;
    }

    /** @return this AI's last tactical positioning decision, for observability/debugging only. */
    public AITacticalIntent getTacticalIntent() {
        return tacticalIntent;
    }

    /**
     * Advances this AI's decision-making by one tick. Must be called
     * once per server tick by {@link AICombatManager} - separately from,
     * and before or after, {@link CombatController#tick()} itself (both
     * are ticked every server tick by the server tick loop; ordering
     * between the two does not matter since this class only ever issues
     * {@code request*}/{@code update*} calls that are queued/resolved by
     * {@link CombatController}'s own tick, never anything synchronous).
     */
    public void tick() {
        if (!entity.isAlive()) {
            return;
        }

        LivingEntity target = acquireTarget();
        if (target == null) {
            handleNoTarget();
            return;
        }

        CombatController opponentController = CombatControllerManager.getIfPresent(target);

        updateTacticalPositioning(target);
        updateFacing(target);
        driveCombatDecisions(target, opponentController);
    }

    // ------------------------------------------------------------------
    // Target acquisition - deliberately NOT reimplemented. Vanilla's own
    // goal selector (already present on every hostile MobEntity) decides
    // *who* to fight; this class only ever reads that decision.
    // ------------------------------------------------------------------

    private LivingEntity acquireTarget() {
        LivingEntity target = entity.getTarget();
        return (target != null && target.isAlive()) ? target : null;
    }

    private void handleNoTarget() {
        if (combatController.getCombatState() != CombatState.NORMAL) {
            combatController.requestExitCombat();
        }
        if (!entity.getNavigation().isIdle()) {
            entity.getNavigation().stop();
        }
        resetEngagementBookkeeping();
        tacticalIntent = AITacticalIntent.IDLE;
    }

    private void resetEngagementBookkeeping() {
        attackDirectionChosen = false;
        windUpTicksWaited = 0;
        opponentThreatTicks = 0;
        blockDecisionMade = false;
        chainDecisionMade = false;
    }

    // ------------------------------------------------------------------
    // Tactical positioning & distance management
    // ------------------------------------------------------------------

    private void updateTacticalPositioning(LivingEntity target) {
        CombatState state = combatController.getCombatState();

        if (COMMITTED_STATES.contains(state)) {
            // Mid-swing or mid-reaction-animation: never reposition out
            // from under the current action, exactly like a player
            // can't strafe-cancel a swing already committed.
            if (!entity.getNavigation().isIdle()) {
                entity.getNavigation().stop();
            }
            tacticalIntent = AITacticalIntent.COMMITTED;
            return;
        }

        double distance = entity.distanceTo(target);
        double reach = combatController.getWeaponProperties().reach();
        double idealDistance = Math.max(1.0, reach * difficulty.preferredDistanceRatio());
        double approachThreshold = idealDistance + APPROACH_SLACK;
        double retreatThreshold = idealDistance * RETREAT_TRIGGER_RATIO;

        boolean shouldRetreat = distance < retreatThreshold
                && (combatController.isExhausted()
                || combatController.getStaminaRatio() < difficulty.staminaCautionThreshold());

        if (shouldRetreat) {
            retreatFrom(target);
            tacticalIntent = AITacticalIntent.RETREATING;
        } else if (distance > approachThreshold) {
            entity.getNavigation().startMovingTo(target, NAVIGATION_SPEED);
            tacticalIntent = AITacticalIntent.APPROACHING;
        } else {
            if (!entity.getNavigation().isIdle()) {
                entity.getNavigation().stop();
            }
            tacticalIntent = AITacticalIntent.HOLDING;
        }

        // Entering combat mode is itself a tactical decision gated by
        // engagement range - done here (rather than unconditionally)
        // so a target that's merely visible, but far away, doesn't
        // immediately snap the AI into a combat stance.
        if (state == CombatState.NORMAL && distance <= difficulty.engagementRange()) {
            combatController.requestEnterCombat();
        }
    }

    private void retreatFrom(LivingEntity target) {
        Vec3d away = entity.getPos().subtract(target.getPos());
        if (away.lengthSquared() < 1.0E-4) {
            // Directly overlapping - pick an arbitrary retreat direction
            // rather than dividing by ~zero length.
            away = new Vec3d(1.0, 0.0, 0.0);
        }
        away = away.normalize();
        Vec3d destination = entity.getPos().add(away.multiply(3.0));
        entity.getNavigation().startMovingTo(destination.x, destination.y, destination.z, RETREAT_SPEED);
    }

    private void updateFacing(LivingEntity target) {
        // CollisionDetector#findTarget requires the attacker's actual
        // rotation to point at the target's forward cone - exactly like
        // a player physically aiming their camera - so this is not
        // optional cosmetic behavior: without it, this AI's own swings
        // would never register a hit.
        entity.getLookControl().lookAt(target, 30.0F, 30.0F);
    }

    // ------------------------------------------------------------------
    // Combat decision-making - every branch below only ever calls
    // CombatController's existing public API; every legality/timing/
    // stamina check already lives there and is never duplicated here.
    // ------------------------------------------------------------------

    private void driveCombatDecisions(LivingEntity target, CombatController opponentController) {
        CombatState state = combatController.getCombatState();

        switch (state) {
            case COMBAT_IDLE -> handleCombatIdle(target, opponentController);
            case PREPARING_ATTACK -> handlePreparingAttack(opponentController);
            case RECOVERY -> handleRecovery();
            case ENTER_BLOCK, BLOCK_IDLE -> handleBlocking(opponentController);
            default -> {
                // NORMAL/ENTERING_COMBAT/EXITING_COMBAT/ATTACKING/EXIT_BLOCK/
                // PERFECT_BLOCK/PARRY/CHAMBER_PREPARE/CHAMBER_SUCCESS/FEINT:
                // either already handled above (NORMAL's combat-entry is
                // decided in updateTacticalPositioning) or purely
                // time-driven - CombatController's own tick() advances
                // these without any AI input required, exactly as it
                // does for a player mid-animation.
            }
        }

        if (state != CombatState.COMBAT_IDLE) {
            opponentThreatTicks = 0;
            blockDecisionMade = false;
        }
        if (state != CombatState.PREPARING_ATTACK) {
            attackDirectionChosen = false;
            windUpTicksWaited = 0;
        }
        if (state != CombatState.RECOVERY) {
            chainDecisionMade = false;
        }
    }

    private boolean isOpponentThreatening(CombatController opponentController) {
        if (opponentController == null) {
            return false;
        }
        CombatState oppState = opponentController.getCombatState();
        return oppState == CombatState.PREPARING_ATTACK || oppState == CombatState.ATTACKING;
    }

    private void handleCombatIdle(LivingEntity target, CombatController opponentController) {
        boolean threatening = isOpponentThreatening(opponentController);

        if (threatening) {
            opponentThreatTicks++;
            if (!blockDecisionMade && opponentThreatTicks >= difficulty.reactionDelayTicks()) {
                blockDecisionMade = true;
                if (!combatController.isExhausted() && random.nextDouble() < difficulty.blockReactionChance()) {
                    combatController.requestEnterBlock();
                }
            }
            return;
        }

        opponentThreatTicks = 0;
        blockDecisionMade = false;

        if (combatController.isExhausted()
                || combatController.getStaminaRatio() < difficulty.staminaCautionThreshold()) {
            return;
        }

        double distance = entity.distanceTo(target);
        double reach = combatController.getWeaponProperties().reach();
        double idealDistance = Math.max(1.0, reach * difficulty.preferredDistanceRatio());

        if (distance <= idealDistance * ENGAGEMENT_ENTRY_SLACK
                && random.nextDouble() < difficulty.attackInitiationChance()) {
            combatController.requestPrepareAttack();
        }
    }

    private void handlePreparingAttack(CombatController opponentController) {
        WeaponProperties weapon = combatController.getWeaponProperties();

        if (!attackDirectionChosen) {
            AttackDirection direction = chooseAttackDirection(weapon, opponentController);
            combatController.updateAttackDirection(direction);
            attackDirectionChosen = true;
            windUpTicksWaited = 0;
            return;
        }

        windUpTicksWaited++;
        if (windUpTicksWaited >= difficulty.reactionDelayTicks()) {
            // Safe to call every tick thereafter - CombatController
            // simply buffers this until its own minimum wind-up
            // duration elapses, exactly like a player clicking early.
            combatController.releaseAttack();
        }
    }

    private void handleRecovery() {
        if (chainDecisionMade) {
            return;
        }
        chainDecisionMade = true;
        if (!combatController.isExhausted() && random.nextDouble() < difficulty.chainAttackChance()) {
            combatController.bufferNextAttack();
        }
    }

    private void handleBlocking(CombatController opponentController) {
        boolean threatening = isOpponentThreatening(opponentController);

        GuardDirection desired = chooseGuardDirection(combatController.getWeaponProperties(), opponentController);
        if (desired != combatController.getGuardDirection()) {
            combatController.updateGuardDirection(desired);
        }

        if (combatController.getCombatState() == CombatState.BLOCK_IDLE
                && !threatening
                && random.nextDouble() < difficulty.exitBlockEagerness()) {
            combatController.requestExitBlock();
        }
    }

    // ------------------------------------------------------------------
    // Directional decision-making - reads the opponent's own
    // CombatController (if tracked) purely as information, exactly the
    // way a player visually reads an opponent's stance; never mutates it.
    // ------------------------------------------------------------------

    private AttackDirection chooseAttackDirection(WeaponProperties weapon, CombatController opponentController) {
        List<AttackDirection> supported = new ArrayList<>(weapon.supportedAttackDirections());
        if (supported.isEmpty()) {
            return AttackDirection.NONE;
        }

        if (opponentController != null) {
            CombatState oppState = opponentController.getCombatState();
            AttackDirection oppDirection = opponentController.getAttackDirection();

            // Chamber-trade: deliberately match the opponent's own
            // committed swing direction, per Bannerlord-style chamber
            // mechanics (CombatController#attemptChamber).
            if ((oppState == CombatState.PREPARING_ATTACK || oppState == CombatState.ATTACKING)
                    && oppDirection != AttackDirection.NONE
                    && supported.contains(oppDirection)
                    && random.nextDouble() < difficulty.chamberAttemptChance()) {
                return oppDirection;
            }

            // Weak-spot targeting: avoid whichever guard the opponent is
            // already holding.
            GuardDirection oppGuard = opponentController.getGuardDirection();
            if ((oppState == CombatState.ENTER_BLOCK || oppState == CombatState.BLOCK_IDLE)
                    && oppGuard != GuardDirection.NONE
                    && random.nextDouble() < difficulty.directionAccuracy()) {
                List<AttackDirection> unblocked = supported.stream()
                        .filter(direction -> DirectionCompatibility.matchingGuard(direction) != oppGuard)
                        .toList();
                if (!unblocked.isEmpty()) {
                    return unblocked.get(random.nextInt(unblocked.size()));
                }
            }
        }

        return supported.get(random.nextInt(supported.size()));
    }

    private GuardDirection chooseGuardDirection(WeaponProperties weapon, CombatController opponentController) {
        List<GuardDirection> supported = new ArrayList<>(weapon.supportedGuardDirections());
        if (supported.isEmpty()) {
            return GuardDirection.NONE;
        }

        if (opponentController != null) {
            CombatState oppState = opponentController.getCombatState();
            AttackDirection oppDirection = opponentController.getAttackDirection();

            if ((oppState == CombatState.PREPARING_ATTACK || oppState == CombatState.ATTACKING)
                    && oppDirection != AttackDirection.NONE
                    && random.nextDouble() < difficulty.directionAccuracy()) {
                GuardDirection matching = DirectionCompatibility.matchingGuard(oppDirection);
                if (supported.contains(matching)) {
                    return matching;
                }
            }
        }

        return supported.get(random.nextInt(supported.size()));
    }
}