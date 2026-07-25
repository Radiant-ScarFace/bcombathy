package com.bcombat.client.animation;

import com.bcombat.combat.events.AttackHitEvent;
import com.bcombat.combat.events.CombatEvents;
import com.bcombat.combat.events.CriticalHitEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks a brief, purely client-side "flinch" window per struck
 * combatant, mirroring {@code HitStopManager}'s own static-holder shape
 * and the same "presentation only, never touches combat state" contract
 * {@code CombatFeedbackManager}'s subsystems all follow.
 * <p>
 * This is the missing-gameplay-feature "hit reactions" identified
 * against Epic Fight: a struck combatant (player or AI, since both fire
 * the same {@link AttackHitEvent}) visibly flinches — a brief backward
 * head/body jolt layered on top of whatever {@link
 * com.bcombat.combat.animation.AnimationState} they're already in — for
 * a handful of ticks after taking a hit, rather than continuing to
 * silently play its previous pose uninterrupted.
 * <p>
 * {@link CombatAnimationApplier} reads {@link #flinchWeight(UUID)} every
 * tick and blends it into the pose it would otherwise render; this class
 * itself never touches a {@code ModelPart} or any entity directly.
 */
public final class HitReactionManager {

    /** How many ticks a flinch lasts before fully fading back to the underlying pose. */
    private static final int FLINCH_DURATION_TICKS = 6;

    /** Critical hits flinch a little longer/harder, mirroring a heavier stagger. */
    private static final int CRITICAL_FLINCH_DURATION_TICKS = 10;

    private static final Map<UUID, Integer> ACTIVE_FLINCHES = new ConcurrentHashMap<>();

    private HitReactionManager() {
        // Static holder, no instances.
    }

    /** Subscribes to the events that trigger a flinch. Safe to call exactly once. */
    public static void register() {
        CombatEvents.ATTACK_HIT.register(HitReactionManager::onAttackHit);
        CombatEvents.CRITICAL_HIT.register(HitReactionManager::onCriticalHit);
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
    }

    private static void onAttackHit(AttackHitEvent event) {
        if (event.result().target() == null) {
            return;
        }
        trigger(event.result().target().getUuid(), FLINCH_DURATION_TICKS);
    }

    private static void onCriticalHit(CriticalHitEvent event) {
        trigger(event.result().target().getUuid(), CRITICAL_FLINCH_DURATION_TICKS);
    }

    private static void trigger(UUID entityId, int ticks) {
        ACTIVE_FLINCHES.merge(entityId, ticks, Math::max);
    }

    private static void tick() {
        ACTIVE_FLINCHES.replaceAll((id, remaining) -> remaining - 1);
        ACTIVE_FLINCHES.values().removeIf(remaining -> remaining <= 0);
    }

    /**
     * @return 0-1 flinch weight for {@code entityId} this tick — {@code
     * 1.0} the instant the hit lands, smoothly fading to {@code 0.0} by
     * the end of the flinch window. {@code 0.0} if not currently
     * flinching.
     */
    public static float flinchWeight(UUID entityId) {
        Integer remaining = ACTIVE_FLINCHES.get(entityId);
        if (remaining == null || remaining <= 0) {
            return 0.0f;
        }
        return com.bcombat.combat.util.MathUtil.smoothstep(remaining / (float) FLINCH_DURATION_TICKS);
    }
}
