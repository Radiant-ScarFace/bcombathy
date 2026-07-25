package com.bcombat.network.packet;

import com.bcombat.combat.controller.StaminaSyncSnapshot;
import net.minecraft.network.PacketByteBuf;

/**
 * Server-to-client packet wrapping a {@link StaminaSyncSnapshot}. Sent on
 * its own lower-frequency, throttled schedule (see {@code
 * ServerCombatNetworking#STAMINA_SYNC_INTERVAL_TICKS}) separately from
 * {@link CombatSyncS2CPacket}, since stamina changes far more often (every
 * regeneration tick) than the combat state machine does.
 */
public record StaminaSyncS2CPacket(StaminaSyncSnapshot snapshot) {

    public void write(PacketByteBuf buf) {
        buf.writeUuid(snapshot.playerId());
        buf.writeDouble(snapshot.currentStamina());
        buf.writeDouble(snapshot.maxStamina());
        buf.writeBoolean(snapshot.exhausted());
    }

    public static StaminaSyncS2CPacket read(PacketByteBuf buf) {
        return new StaminaSyncS2CPacket(new StaminaSyncSnapshot(
                buf.readUuid(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readBoolean()
        ));
    }
}