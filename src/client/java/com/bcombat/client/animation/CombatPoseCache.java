package com.bcombat.client.animation;

import com.bcombat.combat.animation.AnimationController;
import com.bcombat.combat.animation.AnimationState;
import com.bcombat.combat.controller.CombatController;
import com.bcombat.combat.controller.CombatControllerManager;
import com.bcombat.combat.util.CombatConstants;
import net.minecraft.entity.LivingEntity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-entity, per-game-tick cache of the blended {@link CombatPose} the
 * combat framework wants applied to a combatant's rendered model this
 * tick — one entry captured at the <em>end</em> of every client tick
 * (mirroring vanilla's own prevX/x interpolation fields), so {@link
 * CombatAnimationApplier} only ever needs to linearly interpolate
 * between two already-computed poses at render time using the frame's
 * partial-tick fraction, rather than recomputing the full procedural
 * pose — up to two {@link CombatPoseLibrary} calls, one per blended
 * state — as often as the game renders frames.
 * <p>
 * Populated by {@link CombatAnimationTicker}, which calls {@link
 * #onEndClientTick()} once per client tick for every combatant {@link
 * CombatControllerManager#clientControllers()} tracks: the local
 * player, every remote player's mirror, and every AI-controlled
 * combatant's mirror once {@code ServerCombatNetworking} synchronizes
 * one — see that class's docs.
 */
final class CombatPoseCache {

    private static final class Entry {
        final CombatPose previous = CombatPose.neutral();
        final CombatPose current = CombatPose.neutral();
    }

    private static final Map<UUID, Entry> CACHE = new ConcurrentHashMap<>();

    private CombatPoseCache() {
        // Static cache, no instances.
    }

    /**
     * Advances every tracked combatant's cached pose by one tick: the
     * pose computed last tick becomes {@code previous}, and a freshly
     * computed pose — reflecting this tick's just-advanced {@link
     * CombatController} state — becomes {@code current}. A combatant
     * currently outside every combat sub-state ({@link
     * com.bcombat.combat.state.CombatState#isCombatActive()} false) is
     * pruned so {@link CombatAnimationApplier} correctly falls back to
     * leaving vanilla's own model angles completely untouched the
     * instant combat mode is fully exited.
     */
    static void onEndClientTick() {
        Set<UUID> stillRelevant = new HashSet<>();

        for (CombatController controller : CombatControllerManager.clientControllers()) {
            if (!controller.getCombatState().isCombatActive()) {
                continue;
            }
            LivingEntity entity = controller.getEntity();
            UUID id = entity.getUuid();
            stillRelevant.add(id);

            Entry entry = CACHE.computeIfAbsent(id, k -> new Entry());
            entry.previous.setFrom(entry.current);
            entry.current.setFrom(computeTickPose(controller));
        }

        CACHE.keySet().retainAll(stillRelevant);
    }

    /**
     * @return the pose to render for {@code entity} this frame, blended
     * between last tick's and this tick's cached pose using {@code
     * tickDelta} (Minecraft's standard 0-1 partial-tick fraction), or
     * {@code null} if this combatant has no active combat pose override
     * right now.
     */
    static CombatPose sample(LivingEntity entity, float tickDelta) {
        Entry entry = CACHE.get(entity.getUuid());
        if (entry == null) {
            return null;
        }
        CombatPose out = CombatPose.neutral();
        CombatPose.lerp(entry.previous, entry.current, tickDelta, out);
        return out;
    }

    /**
     * Computes this tick's fully-resolved pose for {@code controller}:
     * the current {@link AnimationState}'s procedural pose (see {@link
     * CombatPoseLibrary}), cross-faded against the outgoing state's own
     * settled pose using {@link AnimationController#getBlendWeight()} —
     * exactly the blend {@link AnimationController}/{@code
     * AnimationBlender} already track for a future GeckoLib layer to
     * consume, reused here unchanged.
     */
    private static CombatPose computeTickPose(CombatController controller) {
        AnimationController anim = controller.getAnimationController();
        AnimationState currentState = anim.getCurrentState();
        AnimationState previousState = anim.getPreviousState();
        float blend = anim.getBlendWeight();

        CombatPoseLibrary.WeaponGrip grip = CombatPoseLibrary.WeaponGrip.of(controller.getWeaponProperties().category());
        boolean mounted = controller.isMounted();

        CombatPose currentPose = CombatPoseLibrary.pose(currentState, progressFor(controller, currentState), grip, mounted);

        if (blend >= 1.0f || previousState == currentState) {
            return currentPose;
        }

        // The outgoing state is sampled at its own fully-settled pose
        // (progress 1.0) since, by definition, it is on its way out —
        // AnimationBlender's own blend weight, not a second progress
        // value, is what carries it the rest of the way into
        // currentPose.
        CombatPose previousPose = CombatPoseLibrary.pose(previousState, 1.0f, grip, mounted);
        CombatPose blended = CombatPose.neutral();
        CombatPose.lerp(previousPose, currentPose, blend, blended);
        return blended;
    }

    /**
     * Maps an {@link AnimationState} to the 0-1 progress value {@link
     * CombatPoseLibrary#pose} expects for it, reusing whichever of
     * {@link CombatController}'s existing timing accessors already
     * tracks that state's duration rather than duplicating any of that
     * bookkeeping here. States with no meaningful progress (static
     * stances, locomotion, {@code COUCH_PREPARE}/{@code COUCH_ACTIVE})
     * fall through to {@code 0.0f}, which {@link CombatPoseLibrary}
     * either ignores entirely or treats as a fixed intensity.
     */
    private static float progressFor(CombatController controller, AnimationState state) {
        return switch (state) {
            case WIND_UP_LEFT, WIND_UP_RIGHT, WIND_UP_OVERHEAD, WIND_UP_THRUST -> controller.getWindUpProgress();
            case RELEASE_LEFT, RELEASE_RIGHT, RELEASE_OVERHEAD, RELEASE_THRUST -> controller.getReleaseProgress();
            case RECOVERY -> controller.getRecoveryProgress();
            case ENTER_COMBAT -> controller.getGenericTransitionProgress(CombatConstants.ENTER_COMBAT_TRANSITION_TICKS);
            case EXIT_COMBAT -> controller.getGenericTransitionProgress(CombatConstants.EXIT_COMBAT_TRANSITION_TICKS);
            case ENTER_BLOCK -> controller.getGenericTransitionProgress(CombatConstants.ENTER_BLOCK_TRANSITION_TICKS);
            case EXIT_BLOCK -> controller.getGenericTransitionProgress(CombatConstants.EXIT_BLOCK_TRANSITION_TICKS);
            case PERFECT_BLOCK -> controller.getGenericTransitionProgress(CombatConstants.PERFECT_BLOCK_STATE_DURATION_TICKS);
            case PARRY -> controller.getGenericTransitionProgress(CombatConstants.PARRY_STATE_DURATION_TICKS);
            case CHAMBER_PREPARE -> controller.getGenericTransitionProgress(CombatConstants.CHAMBER_PREPARE_DURATION_TICKS);
            case CHAMBER_SUCCESS -> controller.getGenericTransitionProgress(CombatConstants.CHAMBER_SUCCESS_DURATION_TICKS);
            // Couched-lance sub-states correlate with the underlying
            // CombatState's own ATTACKING/RECOVERY resolution - see
            // CouchState's class docs on how COUCH_IMPACT/COUCH_RECOVERY
            // ride along the exact same attack pipeline as an ordinary
            // strike, so no dedicated couch progress accessor is needed.
            case COUCH_IMPACT -> controller.getReleaseProgress();
            case COUCH_RECOVERY -> controller.getRecoveryProgress();
            default -> 0.0f;
        };
    }
}
