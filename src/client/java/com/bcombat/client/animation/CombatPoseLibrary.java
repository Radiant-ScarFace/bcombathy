package com.bcombat.client.animation;

import com.bcombat.combat.animation.AnimationState;
import com.bcombat.combat.weapon.WeaponCategory;

/**
 * Pure, stateless pose table: turns an {@link AnimationState} plus a
 * 0-1 progress value and the attacker's weapon grip into a {@link
 * CombatPose}. This is the "technical reference adaptation" of Epic
 * Fight's keyframe animation clips — instead of authored keyframes
 * sampled from an external animation file, every pose here is computed
 * procedurally from a handful of anticipation/follow-through curves, so
 * the framework needs no animation assets, model format, or GeckoLib
 * dependency to give every directional attack, block, and defensive
 * reaction a distinct, readable silhouette.
 * <p>
 * Nothing in this class touches a renderer, an entity, or any mutable
 * state — {@link CombatAnimationApplier} is the only caller, and it is
 * solely responsible for blending two poses together and writing the
 * result onto an actual model.
 */
public final class CombatPoseLibrary {

    private CombatPoseLibrary() {
        // Utility class, no instances.
    }

    private static float rad(double degrees) {
        return (float) Math.toRadians(degrees);
    }

    /**
     * @param state    the animation state to pose for.
     * @param progress 0-1 progress through that state (wind-up
     *                 anticipation, release follow-through, recovery
     *                 settle, or ignored for static poses) — see {@code
     *                 CombatController}'s {@code get*Progress()} family.
     * @param grip     the attacker's weapon grip — see {@link
     *                 WeaponGrip}.
     * @param mounted  true if the combatant is currently mounted, which
     *                 keeps the off-hand slightly forward/down (reins)
     *                 rather than a free-hand guard position, except
     *                 where a dedicated couch pose overrides it entirely.
     */
    public static CombatPose pose(AnimationState state, float progress, WeaponGrip grip, boolean mounted) {
        CombatPose p = CombatPose.neutral();
        float t = com.bcombat.combat.util.MathUtil.clamp(progress, 0.0f, 1.0f);

        switch (state) {
            case COMBAT_IDLE, COMBAT_WALK, COMBAT_RUN, COMBAT_SPRINT, COMBAT_JUMP -> combatIdle(p, grip, mounted);

            case ENTER_COMBAT -> lerpFromNeutral(p, grip, mounted, t);
            case EXIT_COMBAT -> lerpFromNeutral(p, grip, mounted, 1.0f - t);

            case WIND_UP_LEFT -> windUp(p, grip, -1, t);
            case WIND_UP_RIGHT -> windUp(p, grip, 1, t);
            case WIND_UP_OVERHEAD -> windUpOverhead(p, grip, t);
            case WIND_UP_THRUST -> windUpThrust(p, grip, t);

            case RELEASE_LEFT -> release(p, grip, -1, t);
            case RELEASE_RIGHT -> release(p, grip, 1, t);
            case RELEASE_OVERHEAD -> releaseOverhead(p, grip, t);
            case RELEASE_THRUST -> releaseThrust(p, grip, t);

            case RECOVERY -> {
                // Settles from a generic follow-through back to the
                // ready idle stance — direction-agnostic since by the
                // time RECOVERY is reached the swing's distinct shape
                // has already read clearly during RELEASE_*.
                CombatPose idle = CombatPose.neutral();
                combatIdle(idle, grip, mounted);
                CombatPose released = CombatPose.neutral();
                releaseThrust(released, grip, 1.0f);
                CombatPose.lerp(released, idle, t, p);
            }

            case ENTER_BLOCK -> {
                CombatPose idle = CombatPose.neutral();
                combatIdle(idle, grip, mounted);
                CombatPose guard = CombatPose.neutral();
                guardIdle(guard, grip);
                CombatPose.lerp(idle, guard, t, p);
            }
            case EXIT_BLOCK -> {
                CombatPose idle = CombatPose.neutral();
                combatIdle(idle, grip, mounted);
                CombatPose guard = CombatPose.neutral();
                guardIdle(guard, grip);
                CombatPose.lerp(guard, idle, t, p);
            }

            case BLOCK_IDLE -> guardIdle(p, grip);
            case GUARD_LEFT -> guardDirectional(p, grip, -1, 0);
            case GUARD_RIGHT -> guardDirectional(p, grip, 1, 0);
            case GUARD_UP -> guardDirectional(p, grip, 0, 1);
            case GUARD_THRUST -> guardDirectional(p, grip, 0, 0);

            case PERFECT_BLOCK -> {
                guardIdle(p, grip);
                p.bodyPitch -= rad(6);
                p.headPitch -= rad(4);
            }
            case PARRY -> {
                guardIdle(p, grip);
                p.rightArmYaw += rad(20);
                p.bodyYaw += rad(10);
            }
            case CHAMBER_PREPARE -> windUpThrust(p, grip, 0.4f);
            case CHAMBER_SUCCESS -> {
                windUpThrust(p, grip, 0.15f);
                p.bodyPitch -= rad(4);
            }

            case COUCH_PREPARE -> couch(p, grip, 0.35f);
            case COUCH_ACTIVE -> couch(p, grip, 1.0f);
            case COUCH_IMPACT -> couch(p, grip, 1.0f + 0.25f * (1.0f - t));
            case COUCH_RECOVERY -> {
                CombatPose couched = CombatPose.neutral();
                couch(couched, grip, 1.0f);
                CombatPose idle = CombatPose.neutral();
                combatIdle(idle, grip, true);
                CombatPose.lerp(couched, idle, t, p);
            }

            case HIT_REACT -> {
                combatIdle(p, grip, mounted);
                p.headPitch += rad(10);
                p.bodyPitch += rad(6);
            }

            // Non-combat locomotion states are never sent through this
            // library by CombatAnimationApplier (it only overrides the
            // model while CombatState.isCombatActive()), but resolve to
            // a fully neutral pose here regardless so the switch stays
            // exhaustive and safe to call from anywhere.
            default -> {
            }
        }

        return p;
    }

    // ------------------------------------------------------------------
    // Static stances
    // ------------------------------------------------------------------

    private static void combatIdle(CombatPose p, WeaponGrip grip, boolean mounted) {
        p.rightArmPitch = rad(-55);
        p.rightArmYaw = rad(-8);
        p.rightArmRoll = rad(-6);
        p.bodyPitch = rad(4);

        if (grip == WeaponGrip.TWO_HANDED) {
            p.leftArmPitch = rad(-50);
            p.leftArmYaw = rad(10);
            p.leftArmRoll = rad(6);
        } else if (mounted) {
            p.leftArmPitch = rad(-70);
            p.leftArmYaw = rad(4);
        } else {
            p.leftArmPitch = rad(-25);
            p.leftArmYaw = rad(6);
        }
    }

    private static void guardIdle(CombatPose p, WeaponGrip grip) {
        p.rightArmPitch = rad(-90);
        p.rightArmYaw = rad(-4);
        p.bodyPitch = rad(2);
        if (grip == WeaponGrip.TWO_HANDED) {
            p.leftArmPitch = rad(-85);
            p.leftArmYaw = rad(6);
        } else {
            p.leftArmPitch = rad(-60);
            p.leftArmYaw = rad(-4);
        }
    }

    /** @param dirX -1 left guard, 1 right guard, 0 centered. @param dirUp 1 for the up/overhead guard. */
    private static void guardDirectional(CombatPose p, WeaponGrip grip, int dirX, int dirUp) {
        guardIdle(p, grip);
        p.rightArmYaw += rad(dirX * 30);
        p.bodyYaw += rad(dirX * 8);
        p.rightArmPitch += rad(-dirUp * 25);
        p.headPitch -= rad(dirUp * 6);
    }

    private static void lerpFromNeutral(CombatPose p, WeaponGrip grip, boolean mounted, float t) {
        CombatPose idle = CombatPose.neutral();
        combatIdle(idle, grip, mounted);
        CombatPose.lerp(CombatPose.neutral(), idle, t, p);
    }

    // ------------------------------------------------------------------
    // Directional slashes / thrust — wind-up (anticipation) and
    // release (follow-through) share the same "arc" per direction so
    // release visually continues exactly where wind-up left off.
    // ------------------------------------------------------------------

    /** @param side -1 left slash, 1 right slash. */
    private static void windUp(CombatPose p, WeaponGrip grip, int side, float t) {
        p.rightArmPitch = rad(-70) + rad(-15) * t;
        p.rightArmYaw = rad(-side * 45) * t;
        p.rightArmRoll = rad(-side * 20) * t;
        p.bodyYaw = rad(-side * 15) * t;
        p.headYaw = rad(-side * 8) * t;
        twoHandedFollow(p, grip);
    }

    private static void release(CombatPose p, WeaponGrip grip, int side, float t) {
        p.rightArmPitch = rad(-85) + rad(30) * t;
        p.rightArmYaw = rad(-side * 45) + rad(side * 90) * t;
        p.rightArmRoll = rad(-side * 20) + rad(side * 40) * t;
        p.bodyYaw = rad(-side * 15) + rad(side * 30) * t;
        p.headYaw = rad(-side * 8) + rad(side * 16) * t;
        twoHandedFollow(p, grip);
    }

    private static void windUpOverhead(CombatPose p, WeaponGrip grip, float t) {
        p.rightArmPitch = rad(-90) + rad(-70) * t;
        p.bodyPitch = rad(-10) * t;
        p.headPitch = rad(-6) * t;
        twoHandedFollow(p, grip);
    }

    private static void releaseOverhead(CombatPose p, WeaponGrip grip, float t) {
        p.rightArmPitch = rad(-160) + rad(150) * t;
        p.bodyPitch = rad(-10) + rad(20) * t;
        p.headPitch = rad(-6) + rad(10) * t;
        twoHandedFollow(p, grip);
    }

    private static void windUpThrust(CombatPose p, WeaponGrip grip, float t) {
        p.rightArmPitch = rad(-60) + rad(30) * t;
        p.rightArmYaw = rad(-6);
        p.bodyPitch = rad(-6) * t;
        twoHandedFollow(p, grip);
    }

    private static void releaseThrust(CombatPose p, WeaponGrip grip, float t) {
        p.rightArmPitch = rad(-30) + rad(-55) * t;
        p.rightArmYaw = rad(-6);
        p.bodyPitch = rad(-6) + rad(10) * t;
        twoHandedFollow(p, grip);
    }

    private static void twoHandedFollow(CombatPose p, WeaponGrip grip) {
        if (grip == WeaponGrip.TWO_HANDED) {
            // Off-hand tracks the main hand closely, as if gripping the
            // same haft, rather than swinging independently.
            p.leftArmPitch = p.rightArmPitch + rad(5);
            p.leftArmYaw = -p.rightArmYaw * 0.6f;
            p.leftArmRoll = p.rightArmRoll * 0.6f;
        } else {
            p.leftArmPitch = rad(-25);
            p.leftArmYaw = rad(6);
        }
    }

    // ------------------------------------------------------------------
    // Couch lance — braced under the arm, off-hand on the reins.
    // @param intensity 0-1+ how far into the couched brace (>1.0 covers
    // the momentary impact recoil overshoot).
    // ------------------------------------------------------------------

    private static void couch(CombatPose p, WeaponGrip grip, float intensity) {
        p.rightArmPitch = rad(-80) * intensity;
        p.rightArmYaw = rad(-25) * intensity;
        p.rightArmRoll = rad(-30) * intensity;
        p.bodyPitch = rad(8) * intensity;
        p.bodyYaw = rad(-6) * intensity;
        p.leftArmPitch = rad(-70) * Math.min(1.0f, intensity);
        p.leftArmYaw = rad(8) * Math.min(1.0f, intensity);
    }

    /** Coarse grip classification a weapon presents to the pose table — see {@link #of(WeaponCategory)}. */
    public enum WeaponGrip {
        ONE_HANDED,
        TWO_HANDED;

        /** @return the grip pose to use for a given weapon's category. */
        public static WeaponGrip of(WeaponCategory category) {
            return switch (category) {
                case TWO_HANDED_SWORD, POLEARM, SPEAR -> TWO_HANDED;
                default -> ONE_HANDED;
            };
        }
    }
}
