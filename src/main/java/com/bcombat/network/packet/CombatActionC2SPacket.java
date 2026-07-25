package com.bcombat.network.packet;

import net.minecraft.network.PacketByteBuf;

/**
 * Client-to-server packet carrying a single {@link CombatActionType}.
 * Sent by {@code CombatInputHandler} the same tick it drives its local
 * predictive {@code CombatController}, and applied on receipt to the
 * sender's authoritative controller by {@code
 * com.bcombat.network.ServerCombatNetworking}.
 *
 * @param action which discrete action was requested.
 */
public record CombatActionC2SPacket(CombatActionType action) {

    public void write(PacketByteBuf buf) {
        buf.writeEnumConstant(action);
    }

    public static CombatActionC2SPacket read(PacketByteBuf buf) {
        return new CombatActionC2SPacket(buf.readEnumConstant(CombatActionType.class));
    }
}