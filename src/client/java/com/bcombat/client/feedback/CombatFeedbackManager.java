package com.bcombat.client.feedback;

import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.collision.HitResult;
import com.bcombat.combat.damage.DamageResult;
import com.bcombat.combat.defense.DefenseResult;
import com.bcombat.combat.events.CombatEvents;
import com.bcombat.combat.weapon.WeaponProperties;
import com.bcombat.client.feedback.camera.CameraShakeManager;
import com.bcombat.client.feedback.config.FeedbackConstants;
import com.bcombat.client.feedback.hitstop.HitStopManager;
import com.bcombat.client.feedback.particles.CombatParticleEmitter;
import com.bcombat.client.feedback.sound.CombatSoundPlayer;
import com.bcombat.client.feedback.sound.CombatSoundPlayer.CombatSoundKey;
import com.bcombat.client.feedback.trail.WeaponTrailManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Central wiring point for the Combat Effects &amp; Feedback Framework.
 * <p>
 * This class contains no combat logic of its own — every decision it
 * makes ("was this hit heavy?", "did armor absorb it?", "which way did
 * the swing come from?") is read directly off the {@code HitResult}/
 * {@code DamageResult} data the core combat framework already computed
 * and published via {@link CombatEvents}. Its only job is to subscribe
 * to that event surface once, translate each event into calls against
 * the already-implemented presentation subsystems ({@link
 * HitStopManager}, {@link CameraShakeManager}, {@link
 * CombatSoundPlayer}, {@link CombatParticleEmitter}, {@link
 * WeaponTrailManager}), and drive the two subsystems ({@code
 * HitStopManager}/{@code CameraShakeManager}) that need a per-tick
 * pulse to decay.
 * <p>
 * {@link #register()} must be called exactly once, from {@code
 * BannerlordCombatClient#onInitializeClient()}. Every listener here is
 * client-side only and purely presentational: nothing in this class
 * mutates combat state, applies damage, or sends a packet, matching the
 * "entirely visual" contract every subsystem it drives already
 * documents. Toggling {@link FeedbackConstants#FEEDBACK_ENABLED} (or
 * any of its per-subsystem flags) to false reduces every call this
 * class makes to a no-op, since each subsystem already guards itself.
 * <p>
 * HUD overlays and debug rendering (hit-direction indicators, screen
 * flashes, exhaustion vignette, hitbox visualization) are explicitly out
 * of scope for this manager — {@link FeedbackConstants} reserves fields
 * for them, but no renderer exists yet for this phase to trigger.
 */
public final class CombatFeedbackManager {

    private CombatFeedbackManager() {
        // Static holder, no instances.
    }

    /**
     * Subscribes every listener in this class to its {@link
     * CombatEvents} counterpart, drives {@code WeaponTrailManager}'s own
     * per-tick sampling loop, and registers the per-client-tick pulse
     * {@code HitStopManager}/{@code CameraShakeManager} need to decay.
     * Safe to call exactly once.
     */
    public static void register() {
        // Combat-mode / movement / mounted-state transitions — no
        // presentation asset exists for these yet; kept subscribed so
        // this manager owns the entire CombatEvents surface, matching
        // how the rest of the framework treats every event as a stable
        // extension point regardless of whether a phase acts on it yet.
        CombatEvents.COMBAT_ENTER.register(event -> {
        });
        CombatEvents.COMBAT_EXIT.register(event -> {
        });
        CombatEvents.MOVEMENT_MODE_CHANGED.register(event -> {
        });
        CombatEvents.MOUNTED_STATE_CHANGED.register(event -> {
        });
        CombatEvents.COMBAT_STATE_CHANGED.register(event -> {
        });

        // Attack wind-up / recovery lifecycle — animation-facing, no
        // feedback cue defined for this phase.
        CombatEvents.ATTACK_PREPARATION_STARTED.register(event -> {
        });
        CombatEvents.ATTACK_PREPARATION_CANCELLED.register(event -> {
        });
        CombatEvents.ATTACK_RELEASED.register(event -> {
        });
        CombatEvents.ATTACK_RECOVERY_STARTED.register(event -> {
        });
        CombatEvents.ATTACK_DIRECTION_CHANGED.register(event -> {
        });

        // Defense lifecycle.
        CombatEvents.BLOCK_STARTED.register(event -> {
        });
        CombatEvents.BLOCK_ENDED.register(event -> {
        });
        CombatEvents.GUARD_DIRECTION_CHANGED.register(event -> {
        });
        CombatEvents.PERFECT_BLOCK.register(CombatFeedbackManager::onPerfectBlock);
        CombatEvents.PARRY.register(CombatFeedbackManager::onParry);
        CombatEvents.CHAMBER_STARTED.register(CombatFeedbackManager::onChamberStarted);
        CombatEvents.CHAMBER_SUCCEEDED.register(CombatFeedbackManager::onChamberSucceeded);

        // Weapon equip/unequip — no feedback cue defined for this phase.
        CombatEvents.WEAPON_EQUIPPED.register(event -> {
        });
        CombatEvents.WEAPON_UNEQUIPPED.register(event -> {
        });
        CombatEvents.WEAPON_CHANGED.register(event -> {
        });

        // Collision / hit / miss / block resolution.
        CombatEvents.COLLISION_DETECTED.register(event -> {
        });
        CombatEvents.ATTACK_HIT.register(event -> {
            // Full impact presentation (heavy/light, flesh/armor,
            // direction, hit stop, shake) is driven off DAMAGE_APPLIED
            // below, once the actual damage breakdown exists — reacting
            // here too would double every cue for the same swing.
            // Kept subscribed as the collision-confirmation extension
            // point every other listener in this class also occupies.
        });
        CombatEvents.ATTACK_MISS.register(CombatFeedbackManager::onAttackMiss);
        CombatEvents.ATTACK_BLOCKED.register(CombatFeedbackManager::onAttackBlocked);

        // Damage pipeline — the primary source of impact feedback.
        CombatEvents.DAMAGE_CALCULATED.register(event -> {
        });
        CombatEvents.ARMOR_REDUCED_DAMAGE.register(event -> {
        });
        CombatEvents.CRITICAL_HIT.register(CombatFeedbackManager::onCriticalHit);
        CombatEvents.DAMAGE_APPLIED.register(CombatFeedbackManager::onDamageApplied);
        CombatEvents.STAGGER_TRIGGERED.register(CombatFeedbackManager::onStaggerTriggered);

        // Stamina / exhaustion.
        CombatEvents.STAMINA_CHANGED.register(event -> {
        });
        CombatEvents.STAMINA_DEPLETED.register(CombatFeedbackManager::onStaminaDepleted);
        CombatEvents.STAMINA_REGENERATED.register(event -> {
        });
        CombatEvents.EXHAUSTION_STARTED.register(CombatFeedbackManager::onExhaustionStarted);
        CombatEvents.EXHAUSTION_ENDED.register(event -> {
        });

        // Couch lance — actual impact feedback rides the same
        // AttackHit/DamageApplied pipeline every other weapon resolves
        // through (see CouchImpactEvent's class docs), so these are
        // left as lifecycle extension points rather than duplicating it.
        CombatEvents.COUCH_STARTED.register(event -> {
        });
        CombatEvents.COUCH_CANCELLED.register(event -> {
        });
        CombatEvents.COUCH_INTERRUPTED.register(event -> {
        });
        CombatEvents.COUCH_IMPACT.register(event -> {
        });
        CombatEvents.COUCH_RECOVERED.register(event -> {
        });

        // Drive WeaponTrailManager's own per-tick sampling loop.
        WeaponTrailManager.register();

        // Per-client-tick pulse HitStopManager/CameraShakeManager need
        // to advance their freeze countdown / trauma decay.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            HitStopManager.onClientTick();
            CameraShakeManager.onClientTick();
        });
    }

    // ------------------------------------------------------------------
    // Damage pipeline
    // ------------------------------------------------------------------

    /**
     * Primary impact-feedback listener. Fires once per confirmed,
     * unblocked hit whose damage has actually been applied — the single
     * point in the pipeline where every classification this manager
     * needs (heavy/light, flesh/armor, direction, critical) is already
     * fully resolved on {@link DamageResult}.
     */
    private static void onDamageApplied(com.bcombat.combat.events.DamageAppliedEvent event) {
        DamageResult result = event.result();
        HitResult hitResult = result.hitResult();
        LivingEntity target = result.target();
        if (target == null) {
            return;
        }

        Vec3d basePos = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0);
        Vec3d impactPos = basePos.add(directionalOffset(result.attacker(), hitResult.direction()));

        boolean heavy = isHeavyImpact(hitResult.weaponProperties());
        boolean armorImpact = isArmorImpact(result);

        // Heavy vs light impact classification.
        if (heavy) {
            CombatParticleEmitter.emitHeavyImpact(impactPos);
        } else {
            CombatParticleEmitter.emitLightImpact(impactPos);
        }
        CombatSoundPlayer.play(heavy ? CombatSoundKey.HIT_HEAVY : CombatSoundKey.HIT_LIGHT, impactPos);

        // Flesh vs armor classification.
        if (armorImpact) {
            CombatParticleEmitter.emitArmorImpact(impactPos);
            CombatSoundPlayer.play(CombatSoundKey.HIT_ARMOR, impactPos);
        } else {
            CombatParticleEmitter.emitFleshImpact(impactPos);
            CombatSoundPlayer.play(CombatSoundKey.HIT_FLESH, impactPos);
        }

        // Hit stop + camera shake, scaled by weight; struck-local-player
        // hits use the dedicated "taken hit" trauma floor.
        HitStopManager.trigger(heavy ? FeedbackConstants.HIT_STOP_HEAVY_TICKS : FeedbackConstants.HIT_STOP_NORMAL_TICKS);
        double trauma = heavy ? FeedbackConstants.CAMERA_SHAKE_HEAVY_TRAUMA : FeedbackConstants.CAMERA_SHAKE_NORMAL_TRAUMA;
        if (isLocalPlayer(target)) {
            trauma = Math.max(trauma, FeedbackConstants.CAMERA_SHAKE_TAKEN_HIT_TRAUMA);
        }
        CameraShakeManager.addTrauma(trauma);

        // Landing-impact feedback: a heavy overhead strike connecting
        // reads as a ground-slam, so it gets the dedicated landing-impact
        // dust/sound/shake accent layered on top of the normal heavy hit.
        if (heavy && hitResult.direction() == AttackDirection.OVERHEAD) {
            Vec3d groundPos = new Vec3d(target.getX(), target.getY(), target.getZ());
            CombatParticleEmitter.emitLandingImpact(groundPos);
            CombatSoundPlayer.play(CombatSoundKey.LANDING_IMPACT, groundPos);
            CameraShakeManager.addTrauma(FeedbackConstants.LANDING_IMPACT_SHAKE_TRAUMA);
            HitStopManager.trigger(FeedbackConstants.LANDING_IMPACT_HIT_STOP_TICKS);
        }
    }

    /** Critical-hit feedback: layered emphasis on top of the normal impact cue above. */
    private static void onCriticalHit(com.bcombat.combat.events.CriticalHitEvent event) {
        DamageResult result = event.result();
        LivingEntity target = result.target();
        if (target == null) {
            return;
        }
        Vec3d pos = target.getPos().add(0.0, target.getHeight() * 0.5, 0.0);
        CombatParticleEmitter.emitCriticalHit(pos);
        CombatSoundPlayer.play(CombatSoundKey.CRITICAL_HIT, pos);
        HitStopManager.trigger(FeedbackConstants.HIT_STOP_CRITICAL_TICKS);
        CameraShakeManager.addTrauma(FeedbackConstants.CAMERA_SHAKE_CRITICAL_TRAUMA);
    }

    /** Stagger feedback: an extra jolt of shake on top of whatever the base hit already applied. */
    private static void onStaggerTriggered(com.bcombat.combat.events.StaggerTriggeredEvent event) {
        LivingEntity target = event.result().target();
        if (target == null) {
            return;
        }
        CameraShakeManager.addTrauma(FeedbackConstants.CAMERA_SHAKE_HEAVY_TRAUMA * 0.5);
    }

    private static void onAttackMiss(com.bcombat.combat.events.AttackMissEvent event) {
        HitResult result = event.result();
        CombatSoundPlayer.play(CombatSoundKey.ATTACK_MISS, result.attacker());
    }

    private static void onAttackBlocked(com.bcombat.combat.events.AttackBlockedEvent event) {
        HitResult result = event.result();
        LivingEntity defender = result.target();
        if (defender == null) {
            return;
        }
        // Perfect Block / Parry / Chamber each carry their own dedicated
        // feedback via PERFECT_BLOCK/PARRY/CHAMBER_STARTED below; this
        // covers the plain-block case only, to avoid layering a second
        // cue on top of those more specific ones.
        if (result.defenseResult() == DefenseResult.NONE) {
            CombatParticleEmitter.emitBlock(defender.getPos());
            CombatSoundPlayer.play(CombatSoundKey.BLOCK, defender.getPos());
        }
        if (isLocalPlayer(result.attacker())) {
            HitStopManager.trigger(FeedbackConstants.HIT_STOP_ATTACK_DENIED_TICKS);
        }
    }

    // ------------------------------------------------------------------
    // Defense
    // ------------------------------------------------------------------

    private static void onPerfectBlock(com.bcombat.combat.events.PerfectBlockEvent event) {
        LivingEntity defender = event.defender();
        CombatParticleEmitter.emitPerfectBlock(defender.getPos());
        CombatSoundPlayer.play(CombatSoundKey.PERFECT_BLOCK, defender.getPos());
        HitStopManager.trigger(FeedbackConstants.HIT_STOP_DEFENSE_SUCCESS_TICKS);
        CameraShakeManager.addTrauma(FeedbackConstants.CAMERA_SHAKE_DEFENSE_TRAUMA);
    }

    private static void onParry(com.bcombat.combat.events.ParryEvent event) {
        LivingEntity defender = event.defender();
        CombatParticleEmitter.emitParry(defender.getPos());
        CombatSoundPlayer.play(CombatSoundKey.PARRY, defender.getPos());
        // A Parry always accompanies a PerfectBlockEvent for the same
        // notification; HitStopManager already takes the longer of the
        // two triggers and CameraShakeManager's trauma is clamped to
        // 1.0, so layering the same defense-success cue here is safe.
        HitStopManager.trigger(FeedbackConstants.HIT_STOP_DEFENSE_SUCCESS_TICKS);
        CameraShakeManager.addTrauma(FeedbackConstants.CAMERA_SHAKE_DEFENSE_TRAUMA);
    }

    private static void onChamberStarted(com.bcombat.combat.events.ChamberStartedEvent event) {
        LivingEntity defender = event.defender();
        CombatParticleEmitter.emitChamberStarted(defender.getPos());
        CombatSoundPlayer.play(CombatSoundKey.CHAMBER_START, defender.getPos());
    }

    private static void onChamberSucceeded(com.bcombat.combat.events.ChamberSucceededEvent event) {
        LivingEntity defender = event.defender();
        CombatParticleEmitter.emitChamberSucceeded(defender.getPos());
        CombatSoundPlayer.play(CombatSoundKey.CHAMBER_SUCCESS, defender.getPos());
        HitStopManager.trigger(FeedbackConstants.HIT_STOP_DEFENSE_SUCCESS_TICKS);
        CameraShakeManager.addTrauma(FeedbackConstants.CAMERA_SHAKE_DEFENSE_TRAUMA);
    }

    // ------------------------------------------------------------------
    // Stamina exhaustion
    // ------------------------------------------------------------------

    private static void onStaminaDepleted(com.bcombat.combat.events.StaminaDepletedEvent event) {
        CombatSoundPlayer.play(CombatSoundKey.STAMINA_DEPLETED, event.player());
    }

    private static void onExhaustionStarted(com.bcombat.combat.events.ExhaustionStartedEvent event) {
        CombatSoundPlayer.play(CombatSoundKey.EXHAUSTION_STARTED, event.player());
    }

    // ------------------------------------------------------------------
    // Classification helpers — read existing combat data, decide nothing new.
    // ------------------------------------------------------------------

    /** @return true if {@code weapon}'s configured weight qualifies this hit for "heavy" impact feedback. */
    private static boolean isHeavyImpact(WeaponProperties weapon) {
        return weapon != null && weapon.weight() >= FeedbackConstants.HEAVY_WEAPON_WEIGHT_THRESHOLD;
    }

    /** @return true if armor meaningfully mitigated {@code result}'s damage, per the configured threshold. */
    private static boolean isArmorImpact(DamageResult result) {
        return result.armorReductionAmount() >= FeedbackConstants.ARMOR_IMPACT_MIN_REDUCTION;
    }

    /**
     * Direction-based hit reaction: offsets an impact effect's spawn
     * point relative to the attacker's facing so the cue reads as coming
     * from the direction the swing actually traveled, using only the
     * {@link AttackDirection} the collision framework already resolved.
     */
    private static Vec3d directionalOffset(LivingEntity attacker, AttackDirection direction) {
        if (attacker == null || direction == null) {
            return Vec3d.ZERO;
        }
        Vec3d forward = attacker.getRotationVector();
        Vec3d right = forward.crossProduct(new Vec3d(0.0, 1.0, 0.0)).normalize();
        switch (direction) {
            case LEFT_SLASH:
                return right.multiply(-0.3);
            case RIGHT_SLASH:
                return right.multiply(0.3);
            case OVERHEAD:
                return new Vec3d(0.0, 0.35, 0.0);
            case THRUST:
                return forward.multiply(0.3);
            case NONE:
            default:
                return Vec3d.ZERO;
        }
    }

    private static boolean isLocalPlayer(LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        return entity != null && client.player != null && client.player.getUuid().equals(entity.getUuid());
    }
}