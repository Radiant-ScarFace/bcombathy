package com.bcombat.client.network;

import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.block.GuardDirection;
import com.bcombat.combat.controller.CombatController;
import com.bcombat.combat.controller.CombatControllerManager;
import com.bcombat.combat.controller.CombatSyncSnapshot;
import com.bcombat.combat.controller.StaminaSyncSnapshot;
import com.bcombat.network.CombatNetworking;
import com.bcombat.network.packet.AttackDirectionC2SPacket;
import com.bcombat.network.packet.CombatActionC2SPacket;
import com.bcombat.network.packet.CombatActionType;
import com.bcombat.network.packet.CombatSyncS2CPacket;
import com.bcombat.network.packet.GuardDirectionC2SPacket;
import com.bcombat.network.packet.StaminaSyncS2CPacket;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;

/**
 * The client half of the combat framework's networking layer, mirroring
 * {@code com.bcombat.network.ServerCombatNetworking}.
 * <p>
 * Two responsibilities:
 * <ul>
 *     <li>Static {@code send*} helpers that {@code CombatInputHandler}
 *     calls alongside every local predictive {@link CombatController}
 *     call, so the server's authoritative controller for this player
 *     receives the exact same request this tick.</li>
 *     <li>S2C receivers that apply an incoming {@link CombatSyncSnapshot}/
 *     {@link StaminaSyncSnapshot} to the matching client-side {@link
 *     CombatController} — the local player's predictive copy (corrected
 *     back in line with the server) or a remote player's purely
 *     network-driven mirror — resolved by UUID via {@link
 *     MinecraftClient#world}.</li>
 * </ul>
 */
public final class ClientCombatNetworking {

    private ClientCombatNetworking() {
        // Static holder, no instances.
    }

    /**
     * Registers every S2C receiver. Must be called exactly once, from
     * {@code BannerlordCombatClient#onInitializeClient()}.
     */
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(CombatNetworking.S2C_COMBAT_SYNC,
                (client, handler, buf, sender) -> {
                    CombatSyncS2CPacket packet = CombatSyncS2CPacket.read(buf);
                    client.execute(() -> applyCombatSync(client, packet.snapshot()));
                });

        ClientPlayNetworking.registerGlobalReceiver(CombatNetworking.S2C_STAMINA_SYNC,
                (client, handler, buf, sender) -> {
                    StaminaSyncS2CPacket packet = StaminaSyncS2CPacket.read(buf);
                    client.execute(() -> applyStaminaSync(client, packet.snapshot()));
                });
    }

    private static void applyCombatSync(MinecraftClient client, CombatSyncSnapshot snapshot) {
        LivingEntity entity = resolveEntity(client, snapshot.playerId());
        if (entity == null) {
            return;
        }
        CombatControllerManager.get(entity).applySnapshot(snapshot);
    }

    private static void applyStaminaSync(MinecraftClient client, StaminaSyncSnapshot snapshot) {
        LivingEntity entity = resolveEntity(client, snapshot.playerId());
        if (entity == null) {
            return;
        }
        CombatControllerManager.get(entity).applyStaminaSnapshot(snapshot);
    }

    private static LivingEntity resolveEntity(MinecraftClient client, java.util.UUID id) {
        if (client.world == null) {
            return null;
        }
        if (client.player != null && client.player.getUuid().equals(id)) {
            return client.player;
        }
        for (PlayerEntity p : client.world.getPlayers()) {
            if (p.getUuid().equals(id)) {
                return p;
            }
        }
        // Falls through to every other loaded entity so an AI-controlled
        // combatant's synced snapshot resolves to its client-side mirror
        // too, now that ServerCombatNetworking broadcasts for AI subjects
        // as well as players - see that class's docs. Checked last since
        // it's the most expensive lookup and the overwhelming majority of
        // incoming snapshots describe a player.
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof LivingEntity living && living.getUuid().equals(id)) {
                return living;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // C2S senders - called by CombatInputHandler alongside every local
    // predictive CombatController call.
    // ------------------------------------------------------------------

    public static void sendAction(CombatActionType action) {
        if (!ClientPlayNetworking.canSend(CombatNetworking.C2S_COMBAT_ACTION)) {
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        new CombatActionC2SPacket(action).write(buf);
        ClientPlayNetworking.send(CombatNetworking.C2S_COMBAT_ACTION, buf);
    }

    public static void sendAttackDirection(AttackDirection direction) {
        if (!ClientPlayNetworking.canSend(CombatNetworking.C2S_ATTACK_DIRECTION)) {
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        new AttackDirectionC2SPacket(direction).write(buf);
        ClientPlayNetworking.send(CombatNetworking.C2S_ATTACK_DIRECTION, buf);
    }

    public static void sendGuardDirection(GuardDirection direction) {
        if (!ClientPlayNetworking.canSend(CombatNetworking.C2S_GUARD_DIRECTION)) {
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        new GuardDirectionC2SPacket(direction).write(buf);
        ClientPlayNetworking.send(CombatNetworking.C2S_GUARD_DIRECTION, buf);
    }
}
