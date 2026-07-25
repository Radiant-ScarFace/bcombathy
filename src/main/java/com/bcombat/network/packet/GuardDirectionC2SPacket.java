package com.bcombat.network.packet;

import com.bcombat.combat.block.GuardDirection;
import net.minecraft.network.PacketByteBuf;

/**
 * Client-to-server packet proposing a new locked {@link GuardDirection},
 * mirroring {@code CombatController#updateGuardDirection}. Sent by
 * {@code CombatInputHandler} only when {@code GuardDirectionTracker#resolve}
 * actually changes, not every tick.
 */
public record GuardDirectionC2SPacket(GuardDirection direction) {

    public void write(PacketByteBuf buf) {
        buf.writeEnumConstant(direction);
    }

    public static GuardDirectionC2SPacket read(PacketByteBuf buf) {
        return new GuardDirectionC2SPacket(buf.readEnumConstant(GuardDirection.class));
    }
}