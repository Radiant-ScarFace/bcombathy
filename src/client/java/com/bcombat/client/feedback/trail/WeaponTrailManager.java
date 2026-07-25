package com.bcombat.client.feedback.trail;

import com.bcombat.client.feedback.config.FeedbackConstants;
import com.bcombat.combat.controller.CombatController;
import com.bcombat.combat.controller.CombatControllerManager;
import com.bcombat.combat.state.CombatState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Samples and ages weapon-trail positions for every combatant the client
 * can currently see in {@code CombatState.ATTACKING}, and hands the live
 * samples off to the registered {@link WeaponTrailRenderer} each tick.
 * <p>
 * Reuses {@code CombatController#getCombatState()} (client-side
 * controller, kept in sync by the existing networking layer for players,
 * predictively accurate for the local player) as the sole source of
 * truth for "is this combatant swinging right now" - this class records
 * positions and manages fade lifetime only; it never decides combat
 * timing itself.
 */
public final class WeaponTrailManager {

    private static final Map<UUID, Deque<TrailSample>> ACTIVE_TRAILS = new ConcurrentHashMap<>();
    private static WeaponTrailRenderer renderer = new DefaultWeaponTrailRenderer();

    private WeaponTrailManager() {
        // Static holder, no instances.
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(WeaponTrailManager::onClientTick);
    }

    /**
     * Swaps the active trail renderer. Extension point for a future
     * proper ribbon/mesh trail - see {@link WeaponTrailRenderer}'s class
     * docs.
     */
    public static void setRenderer(WeaponTrailRenderer newRenderer) {
        if (newRenderer != null) {
            renderer = newRenderer;
        }
    }

    public static WeaponTrailRenderer getRenderer() {
        return renderer;
    }

    private static void onClientTick(MinecraftClient client) {
        if (!FeedbackConstants.FEEDBACK_ENABLED || !FeedbackConstants.WEAPON_TRAILS_ENABLED) {
            ACTIVE_TRAILS.clear();
            return;
        }
        if (client.player == null || client.world == null) {
            ACTIVE_TRAILS.clear();
            return;
        }

        for (LivingEntity combatant : nearbyTrackedCombatants(client)) {
            CombatController controller = CombatControllerManager.getIfPresent(combatant);
            boolean swinging = controller != null && controller.getCombatState() == CombatState.ATTACKING;

            Deque<TrailSample> trail = ACTIVE_TRAILS.computeIfAbsent(combatant.getUuid(), id -> new ArrayDeque<>());

            if (swinging) {
                Vec3d tip = WeaponTrailRenderer.approximateWeaponTipPosition(combatant);
                trail.addLast(new TrailSample(tip, FeedbackConstants.WEAPON_TRAIL_FADE_TICKS));
                while (trail.size() > Math.max(2, FeedbackConstants.WEAPON_TRAIL_SAMPLE_COUNT)) {
                    trail.removeFirst();
                }
            }

            trail.removeIf(sample -> sample.age() <= 0);
            trail.forEach(TrailSample::tick);

            if (!trail.isEmpty()) {
                List<TrailSample> snapshot = new ArrayList<>(trail);
                renderer.render(combatant, snapshot);
            }

            if (trail.isEmpty() && !swinging) {
                ACTIVE_TRAILS.remove(combatant.getUuid());
            }
        }
    }

    private static List<LivingEntity> nearbyTrackedCombatants(MinecraftClient client) {
        List<LivingEntity> result = new ArrayList<>();
        result.add(client.player);
        double range = FeedbackConstants.DEBUG_VISUALIZATION_RANGE_BLOCKS;
        Box searchBox = client.player.getBoundingBox().expand(range);
        for (Entity entity : client.world.getOtherEntities(client.player, searchBox,
                e -> e instanceof LivingEntity)) {
            result.add((LivingEntity) entity);
        }
        return result;
    }

    /** One recorded trail position plus its remaining fade lifetime in ticks. */
    public static final class TrailSample {
        private final Vec3d position;
        private int age;

        TrailSample(Vec3d position, int age) {
            this.position = position;
            this.age = age;
        }

        public Vec3d position() {
            return position;
        }

        public int age() {
            return age;
        }

        void tick() {
            age--;
        }
    }
}