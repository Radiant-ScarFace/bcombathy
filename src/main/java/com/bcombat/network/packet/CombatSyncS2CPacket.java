package com.bcombat.network.packet;

import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.block.GuardDirection;
import com.bcombat.combat.controller.CombatSyncSnapshot;
import com.bcombat.combat.movement.MovementMode;
import com.bcombat.combat.state.CombatState;
import net.minecraft.network.PacketByteBuf;

/**
 * Server-to-client packet wrapping a {@link CombatSyncSnapshot}. Sent to
 * every player tracking the described player (plus the described player
 * themselves, for local-prediction reconciliation) whenever {@code
 * ServerCombatNetworking}'s per-tick broadcast detects the authoritative
 * snapshot actually changed.
 */
public record CombatSyncS2CPacket(CombatSyncSnapshot snapshot) {

    public void write(PacketByteBuf buf) {
        buf.writeUuid(snapshot.playerId());
        buf.writeEnumConstant(snapshot.state());
        buf.writeEnumConstant(snapshot.attackDirection());
        buf.writeEnumConstant(snapshot.guardDirection());
        buf.writeEnumConstant(snapshot.movementMode());
        buf.writeVarInt(snapshot.transitionTicksRemaining());
    }

    public static CombatSyncS2CPacket read(PacketByteBuf buf) {
        return new CombatSyncS2CPacket(new CombatSyncSnapshot(
                buf.readUuid(),
                buf.readEnumConstant(CombatState.class),
                buf.readEnumConstant(AttackDirection.class),
                buf.readEnumConstant(GuardDirection.class),
                buf.readEnumConstant(MovementMode.class),
                buf.readVarInt()
        ));
    }
}