package com.bcombat.network;

import com.bcombat.combat.controller.CombatController;
import com.bcombat.combat.controller.CombatControllerManager;
import com.bcombat.combat.controller.CombatSyncSnapshot;
import com.bcombat.combat.controller.StaminaSyncSnapshot;
import com.bcombat.network.packet.AttackDirectionC2SPacket;
import com.bcombat.network.packet.CombatActionC2SPacket;
import com.bcombat.network.packet.CombatSyncS2CPacket;
import com.bcombat.network.packet.GuardDirectionC2SPacket;
import com.bcombat.network.packet.StaminaSyncS2CPacket;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The server half of the combat framework's networking layer. This is
 * the missing piece that actually makes the client's predictive {@code
 * CombatController} calls reach the server's authoritative one: without
 * this class registered, {@code CombatInputHandler}'s {@code
 * request*()}/{@code update*()} calls only ever mutate the local
 * client-side mirror, and the authoritative server-side {@link
 * CombatController} for that same player never leaves {@code
 * CombatState.NORMAL} — meaning collision detection (gated to
 * authoritative instances only) never runs and no damage is ever dealt.
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Registers the three C2S channels ({@link CombatActionC2SPacket},
 *     {@link AttackDirectionC2SPacket}, {@link GuardDirectionC2SPacket})
 *     and applies each one to the sending player's authoritative {@link
 *     CombatController} the instant it's received.</li>
 *     <li>Every server tick, broadcasts a {@link CombatSyncS2CPacket} to
 *     every player tracking a changed combatant (plus the combatant
 *     themselves, for local-prediction reconciliation) whenever that
 *     combatant's {@link CombatSyncSnapshot} actually changed since the
 *     last broadcast.</li>
 *     <li>Every {@link #STAMINA_SYNC_INTERVAL_TICKS} ticks, sends each
 *     player their own {@link StaminaSyncS2CPacket} (stamina is only
 *     ever displayed for the local player's own HUD, so this is never
 *     broadcast to observers).</li>
 * </ul>
 */
public final class ServerCombatNetworking {

    /** How often (in server ticks) the throttled stamina sync fires. */
    public static final int STAMINA_SYNC_INTERVAL_TICKS = 4;

    private static final Map<UUID, CombatSyncSnapshot> LAST_BROADCAST = new HashMap<>();
    private static int tickCounter = 0;

    private ServerCombatNetworking() {
        // Static registrar, no instances.
    }

    /**
     * Drops the cached last-broadcast snapshot for a player, e.g. on
     * disconnect/respawn, so a reconnecting or respawned player's first
     * snapshot is always treated as changed rather than compared against
     * a stale entry from a previous session/instance.
     */
    public static void forget(UUID playerId) {
        LAST_BROADCAST.remove(playerId);
    }

    /** Clears every cached last-broadcast snapshot, e.g. on server shutdown. */
    public static void clear() {
        LAST_BROADCAST.clear();
    }

    /**
     * Registers every C2S receiver and the per-tick broadcast loop. Must
     * be called exactly once, from {@code BannerlordCombat#onInitialize()}.
     */
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(CombatNetworking.C2S_COMBAT_ACTION,
                (server, player, handler, buf, sender) -> {
                    CombatActionC2SPacket packet = CombatActionC2SPacket.read(buf);
                    server.execute(() -> applyAction(player, packet));
                });

        ServerPlayNetworking.registerGlobalReceiver(CombatNetworking.C2S_ATTACK_DIRECTION,
                (server, player, handler, buf, sender) -> {
                    AttackDirectionC2SPacket packet = AttackDirectionC2SPacket.read(buf);
                    server.execute(() -> CombatControllerManager.get(player).updateAttackDirection(packet.direction()));
                });

        ServerPlayNetworking.registerGlobalReceiver(CombatNetworking.C2S_GUARD_DIRECTION,
                (server, player, handler, buf, sender) -> {
                    GuardDirectionC2SPacket packet = GuardDirectionC2SPacket.read(buf);
                    server.execute(() -> CombatControllerManager.get(player).updateGuardDirection(packet.direction()));
                });

        ServerTickEvents.END_SERVER_TICK.register(ServerCombatNetworking::onEndServerTick);
    }

    private static void applyAction(ServerPlayerEntity player, CombatActionC2SPacket packet) {
        CombatController controller = CombatControllerManager.get(player);
        switch (packet.action()) {
            case ENTER_COMBAT -> controller.requestEnterCombat();
            case EXIT_COMBAT -> controller.requestExitCombat();
            case PREPARE_ATTACK -> controller.requestPrepareAttack();
            case CANCEL_PREPARE_ATTACK -> controller.cancelPrepareAttack();
            case RELEASE_ATTACK -> controller.releaseAttack();
            case BUFFER_NEXT_ATTACK -> controller.bufferNextAttack();
            case ENTER_BLOCK -> controller.requestEnterBlock();
            case EXIT_BLOCK -> controller.requestExitBlock();
        }
    }

    private static void onEndServerTick(MinecraftServer server) {
        tickCounter++;

        for (CombatController controller : CombatControllerManager.serverControllers()) {
            // Widened to every authoritative combatant - real players
            // AND AI-controlled mobs driven by AICombatController - so
            // AI combat state (wind-up, release, blocking, etc.) is
            // mirrored to nearby clients exactly the way a player's
            // already is, letting the client-side render pipeline
            // (CombatAnimationApplier/CombatPoseCache) animate AI
            // combatants too. This reuses the exact same
            // CombatSyncSnapshot/CombatSyncS2CPacket machinery below -
            // no new packet type, no networking redesign.
            LivingEntity subject = controller.getEntity();

            CombatSyncSnapshot snapshot = controller.captureSnapshot();
            CombatSyncSnapshot previous = LAST_BROADCAST.get(subject.getUuid());
            if (!snapshot.equals(previous)) {
                LAST_BROADCAST.put(subject.getUuid(), snapshot);
                broadcastCombatSync(subject, snapshot);
            }

            if (controller.isPlayer() && tickCounter % STAMINA_SYNC_INTERVAL_TICKS == 0) {
                ServerPlayerEntity player = (ServerPlayerEntity) controller.getPlayer();
                if (player != null) {
                    sendStaminaSync(player, controller.captureStaminaSnapshot());
                }
            }
        }
    }

    /**
     * Broadcasts {@code snapshot} to every client that needs it: the
     * subject themselves for local-prediction reconciliation (only
     * meaningful - and only possible - when the subject is a real,
     * connected {@link ServerPlayerEntity}), plus every player currently
     * tracking the subject entity via vanilla's own {@link
     * PlayerLookup#tracking}, which works identically for a player
     * subject or an AI-controlled mob subject.
     */
    private static void broadcastCombatSync(LivingEntity subject, CombatSyncSnapshot snapshot) {
        if (subject instanceof ServerPlayerEntity selfPlayer) {
            PacketByteBuf buf = PacketByteBufs.create();
            new CombatSyncS2CPacket(snapshot).write(buf);
            ServerPlayNetworking.send(selfPlayer, CombatNetworking.S2C_COMBAT_SYNC, buf);
        }

        for (ServerPlayerEntity observer : PlayerLookup.tracking(subject)) {
            if (observer == subject) {
                // Already sent above - avoid double-delivering the same
                // tick's snapshot to a player observing themselves.
                continue;
            }
            PacketByteBuf observerBuf = PacketByteBufs.create();
            new CombatSyncS2CPacket(snapshot).write(observerBuf);
            ServerPlayNetworking.send(observer, CombatNetworking.S2C_COMBAT_SYNC, observerBuf);
        }
    }

    private static void sendStaminaSync(ServerPlayerEntity player, StaminaSyncSnapshot snapshot) {
        PacketByteBuf buf = PacketByteBufs.create();
        new StaminaSyncS2CPacket(snapshot).write(buf);
        ServerPlayNetworking.send(player, CombatNetworking.S2C_STAMINA_SYNC, buf);
    }
}
