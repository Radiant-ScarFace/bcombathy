package com.bcombat.network.packet;

import com.bcombat.combat.attack.AttackDirection;
import net.minecraft.network.PacketByteBuf;

/**
 * Client-to-server packet proposing a new committed {@link
 * AttackDirection}, mirroring {@code CombatController#updateAttackDirection}.
 * Sent by {@code CombatInputHandler} only when {@code
 * AttackDirectionTracker#resolve} actually changes, not every tick, to
 * keep wind-up direction tracking cheap on the wire.
 */
public record AttackDirectionC2SPacket(AttackDirection direction) {

    public void write(PacketByteBuf buf) {
        buf.writeEnumConstant(direction);
    }

    public static AttackDirectionC2SPacket read(PacketByteBuf buf) {
        return new AttackDirectionC2SPacket(buf.readEnumConstant(AttackDirection.class));
    }
}