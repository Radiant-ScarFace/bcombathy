package com.bcombat.combat.ai.group;

import com.bcombat.combat.ai.AICombatController;
import com.bcombat.combat.controller.CombatController;
import com.bcombat.combat.controller.CombatControllerManager;
import com.bcombat.combat.state.CombatState;
import com.bcombat.combat.util.CombatConstants;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A named group of {@link AICombatController} instances that share
 * combat awareness — the "Group Combat Framework" half of this phase.
 * Owned and driven exclusively by {@link SquadManager}; every method
 * here only ever reads existing state already exposed by {@link
 * CombatController}/{@link MobEntity} (positions, health, combat state,
 * mount state) and computes derived tactical information (a shared
 * focus target, a regroup point, per-member flank/surround slots) that
 * {@link AICombatController} then acts on through the exact same {@code
 * request*}/navigation calls the solo framework already uses. This
 * class never touches damage, collision, or a member's {@code
 * CombatController} state directly — it only ever hands back
 * information for {@link AICombatController} to act on, the same
 * "report state, never mutate someone else's" split every other
 * sub-controller in this framework already follows.
 * <p>
 * Not thread-safe beyond what {@link CopyOnWriteArrayList} itself
 * guarantees for membership changes; {@link #update()} and every query
 * method are only ever called from the single server tick thread, same
 * as every other combat-framework class.
 */
public final class CombatSquad {

    private final String id;
    private final List<AICombatController> members = new CopyOnWriteArrayList<>();

    private LivingEntity focusTarget;
    private long lastTargetSwitchTime = Long.MIN_VALUE;
    private Vec3d regroupPoint;
    private double rotationOffsetDegrees;

    CombatSquad(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /** @return a live snapshot-safe view of this squad's current members. */
    public List<AICombatController> members() {
        return members;
    }

    public int size() {
        return members.size();
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    void addMember(AICombatController member) {
        if (!members.contains(member)) {
            members.add(member);
        }
    }

    void removeMember(AICombatController member) {
        members.remove(member);
    }

    /** @return this squad's currently shared focus target, or {@code null} if the squad has no live shared threat right now. */
    public LivingEntity getFocusTarget() {
        return (focusTarget != null && focusTarget.isAlive()) ? focusTarget : null;
    }

    /**
     * @return the position healthy squad members are currently
     * clustered around, for wounded/regrouping members to fall back
     * toward, or {@code null} if no member currently counts as healthy
     * (see {@link CombatConstants#SQUAD_REGROUP_HEALTHY_RATIO}).
     */
    public Vec3d getRegroupPoint() {
        return regroupPoint;
    }

    /**
     * Advances this squad's shared awareness by one tick. Must be
     * called by {@link SquadManager#tickAll()}, before any member's own
     * {@link AICombatController#tick()} runs this same tick, so every
     * member acts on this tick's freshly computed focus target/regroup
     * point rather than last tick's.
     */
    void update() {
        purgeDead();
        if (members.isEmpty()) {
            return;
        }
        long now = worldTime();
        updateThreatsAndFocus(now);
        updateRegroupPoint();
        // A slow continuous rotation so a squad's flank/surround slots
        // sweep around the target over time rather than every member
        // freezing at a fixed bearing - part of what keeps surrounding
        // behavior from reading as static/robotic.
        rotationOffsetDegrees = (rotationOffsetDegrees + 1.5) % 360.0;
    }

    private void purgeDead() {
        members.removeIf(member -> !member.getEntity().isAlive() || member.getEntity().isRemoved());
    }

    private long worldTime() {
        MobEntity any = members.get(0).getEntity();
        return any.getWorld().getTime();
    }

    // ------------------------------------------------------------------
    // Shared combat awareness, threat prioritization & group target
    // selection/switching
    // ------------------------------------------------------------------

    private void updateThreatsAndFocus(long now) {
        Map<UUID, ThreatInfo> candidates = new HashMap<>();

        for (AICombatController member : members) {
            MobEntity self = member.getEntity();
            LivingEntity seen = self.getTarget();
            if (seen == null || !seen.isAlive() || isMember(seen)) {
                // Ally awareness: a squad-mate is never itself a valid
                // threat candidate, however vanilla's own targeting
                // arrived at it.
                continue;
            }
            double distance = self.distanceTo(seen);
            if (distance > CombatConstants.SQUAD_AWARENESS_RADIUS) {
                continue;
            }

            ThreatInfo info = candidates.computeIfAbsent(seen.getUuid(), key -> new ThreatInfo(seen));
            double proximity = CombatConstants.SQUAD_THREAT_WEIGHT_PROXIMITY / Math.max(1.0, distance);
            double lowHealth = CombatConstants.SQUAD_THREAT_WEIGHT_LOW_HEALTH * (1.0 - healthRatio(seen));
            double activeThreat = isThreateningAnyMember(seen) ? CombatConstants.SQUAD_THREAT_WEIGHT_ACTIVE_THREAT : 0.0;
            info.addContribution(1.0 + proximity + lowHealth + activeThreat);
        }

        if (candidates.isEmpty()) {
            if (focusTarget != null && !focusTarget.isAlive()) {
                focusTarget = null;
            }
            return;
        }

        ThreatInfo best = candidates.values().stream()
                .max(Comparator.comparingDouble(ThreatInfo::score))
                .orElse(null);

        if (focusTarget == null || !focusTarget.isAlive()) {
            adoptFocus(best, now);
            return;
        }

        if (best.target() == focusTarget) {
            return;
        }

        ThreatInfo currentInfo = candidates.get(focusTarget.getUuid());
        double currentScore = currentInfo != null ? currentInfo.score() : 0.0;
        boolean cooldownElapsed = (now - lastTargetSwitchTime) >= CombatConstants.SQUAD_TARGET_SWITCH_COOLDOWN_TICKS;
        boolean beatsMargin = best.score() > currentScore * (1.0 + CombatConstants.SQUAD_TARGET_SWITCH_MARGIN);

        // Target switching based on combat conditions: only actually
        // re-focuses once the new candidate is clearly better AND the
        // squad hasn't just switched, so a squad doesn't thrash between
        // two similarly-threatening targets tick to tick.
        if (cooldownElapsed && beatsMargin) {
            adoptFocus(best, now);
        }
    }

    private void adoptFocus(ThreatInfo info, long now) {
        if (info == null) {
            return;
        }
        focusTarget = info.target();
        lastTargetSwitchTime = now;
    }

    private boolean isThreateningAnyMember(LivingEntity candidate) {
        CombatController opponentController = CombatControllerManager.getIfPresent(candidate);
        if (opponentController == null) {
            return false;
        }
        CombatState state = opponentController.getCombatState();
        if (state != CombatState.PREPARING_ATTACK && state != CombatState.ATTACKING) {
            return false;
        }
        for (AICombatController member : members) {
            if (member.getEntity().distanceTo(candidate) <= CombatConstants.SQUAD_AWARENESS_RADIUS) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Retreat & regroup
    // ------------------------------------------------------------------

    private void updateRegroupPoint() {
        List<AICombatController> healthy = new ArrayList<>();
        for (AICombatController member : members) {
            if (healthRatio(member.getEntity()) >= CombatConstants.SQUAD_REGROUP_HEALTHY_RATIO) {
                healthy.add(member);
            }
        }
        if (healthy.isEmpty()) {
            regroupPoint = null;
            return;
        }
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        for (AICombatController member : healthy) {
            Vec3d pos = member.getEntity().getPos();
            x += pos.x;
            y += pos.y;
            z += pos.z;
        }
        int count = healthy.size();
        regroupPoint = new Vec3d(x / count, y / count, z / count);
    }

    // ------------------------------------------------------------------
    // Ally awareness / friendly-fire avoidance / spacing / flanking
    // ------------------------------------------------------------------

    /** @return true if {@code candidate} is one of this squad's own members' entities (never {@code requester} itself). */
    public boolean isAlly(AICombatController requester, LivingEntity candidate) {
        if (candidate == null) {
            return false;
        }
        for (AICombatController member : members) {
            if (member != requester && member.getEntity() == candidate) {
                return true;
            }
        }
        return false;
    }

    private boolean isMember(LivingEntity candidate) {
        for (AICombatController member : members) {
            if (member.getEntity() == candidate) {
                return true;
            }
        }
        return false;
    }

    /** @return every other member's entity within {@code radius} blocks of {@code requester}. */
    public List<LivingEntity> nearbyAllies(AICombatController requester, double radius) {
        List<LivingEntity> result = new ArrayList<>();
        MobEntity self = requester.getEntity();
        for (AICombatController member : members) {
            if (member == requester) {
                continue;
            }
            MobEntity other = member.getEntity();
            if (other.isAlive() && self.distanceTo(other) <= radius) {
                result.add(other);
            }
        }
        return result;
    }

    /**
     * @return true if an ally currently stands roughly on the line
     * between {@code requester} and {@code target}, closer than the
     * target itself — the condition under which {@code
     * AICombatController} withholds that tick's attack initiation
     * rather than risk striking a squad-mate. Purely advisory: it never
     * touches {@code CollisionDetector}/{@code DamageService} — it only
     * ever informs whether the AI *chooses* to swing this tick.
     */
    public boolean isFriendlyFireRisk(AICombatController requester, LivingEntity target) {
        MobEntity self = requester.getEntity();
        Vec3d toTarget = target.getPos().subtract(self.getPos());
        double targetDistance = toTarget.length();
        if (targetDistance < 1.0E-4) {
            return false;
        }
        Vec3d toTargetDir = toTarget.multiply(1.0 / targetDistance);

        for (LivingEntity ally : nearbyAllies(requester, targetDistance)) {
            Vec3d toAlly = ally.getPos().subtract(self.getPos());
            double allyDistance = toAlly.length();
            if (allyDistance < 1.0E-4 || allyDistance >= targetDistance) {
                continue;
            }
            double cos = toAlly.multiply(1.0 / allyDistance).dotProduct(toTargetDir);
            if (cos >= CombatConstants.SQUAD_FRIENDLY_FIRE_CONE_COS) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return {@code requester}'s desired flanking/surrounding position
     * around {@code target} at the given radius, already nudged away
     * from any ally currently occupying that spot (maintaining combat
     * spacing). Each member is assigned a distinct angular slot around
     * the target based on its position in {@link #members}, biased by
     * its own {@link com.bcombat.combat.ai.CombatRole#flankAngleBias()}
     * and slowly rotated over time by this squad's {@link
     * #rotationOffsetDegrees}, which is what produces surrounding
     * behavior rather than every member converging on the same spot.
     */
    public Vec3d flankPosition(AICombatController requester, LivingEntity target, double radius) {
        int index = Math.max(0, members.indexOf(requester));
        int count = Math.max(1, members.size());
        double slotAngle = (360.0 / count) * index;
        double angleDegrees = slotAngle * requester.getRole().flankAngleBias() + rotationOffsetDegrees;
        double angleRadians = Math.toRadians(angleDegrees);

        double dx = Math.cos(angleRadians) * radius;
        double dz = Math.sin(angleRadians) * radius;
        Vec3d desired = target.getPos().add(dx, 0.0, dz);

        for (LivingEntity ally : nearbyAllies(requester, CombatConstants.SQUAD_AWARENESS_RADIUS)) {
            double distanceToAlly = desired.distanceTo(ally.getPos());
            if (distanceToAlly > 1.0E-4 && distanceToAlly < CombatConstants.SQUAD_MIN_ALLY_SPACING) {
                Vec3d push = desired.subtract(ally.getPos())
                        .multiply(1.0 / distanceToAlly)
                        .multiply(CombatConstants.SQUAD_MIN_ALLY_SPACING - distanceToAlly);
                desired = desired.add(push);
            }
        }
        return desired;
    }

    /**
     * @return {@code desired} nudged away from any nearby mounted ally
     * that is currently charging, so an unmounted member doesn't hold a
     * flank position directly in a squad-mate's charge lane. A no-op for
     * a mounted requester — mounted vs. infantry positioning is
     * otherwise {@link AICombatController}'s own concern.
     */
    public Vec3d avoidMountedChargeLanes(AICombatController requester, Vec3d desired) {
        if (requester.getCombatController().isMounted()) {
            return desired;
        }
        MobEntity self = requester.getEntity();
        for (AICombatController member : members) {
            if (member == requester) {
                continue;
            }
            CombatController allyController = member.getCombatController();
            if (!allyController.isMounted() || allyController.getMountSpeed() < CombatConstants.MOUNTED_CHARGE_SPEED_THRESHOLD) {
                continue;
            }
            MobEntity rider = member.getEntity();
            double distance = self.distanceTo(rider);
            if (distance < CombatConstants.SQUAD_MOUNTED_CHARGE_DANGER_RADIUS) {
                Vec3d push = self.getPos().subtract(rider.getPos());
                if (push.lengthSquared() < 1.0E-4) {
                    push = new Vec3d(1.0, 0.0, 0.0);
                } else {
                    push = push.multiply(1.0 / push.length());
                }
                desired = desired.add(push.multiply(CombatConstants.SQUAD_MOUNTED_CHARGE_DANGER_RADIUS - distance));
            }
        }
        return desired;
    }

    private static double healthRatio(LivingEntity entity) {
        float max = entity.getMaxHealth();
        if (max <= 0.0F) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, entity.getHealth() / max));
    }
}