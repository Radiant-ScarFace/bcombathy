package com.bcombat.network.packet;

/**
 * The discrete, no-extra-payload combat actions a client can request of
 * the server. Mirrors the no-argument request methods on {@code
 * CombatController} one-for-one ({@code requestEnterCombat()} ->
 * {@link #ENTER_COMBAT}, etc.) - see {@link CombatActionC2SPacket} for
 * how a value here is translated back into the matching controller call
 * on receipt.
 * <p>
 * Direction proposals ({@code AttackDirection}/{@code GuardDirection})
 * are deliberately NOT part of this enum, since they carry their own
 * payload - see {@link AttackDirectionC2SPacket}/{@link GuardDirectionC2SPacket}.
 */
public enum CombatActionType {
    ENTER_COMBAT,
    EXIT_COMBAT,
    PREPARE_ATTACK,
    CANCEL_PREPARE_ATTACK,
    RELEASE_ATTACK,
    BUFFER_NEXT_ATTACK,
    ENTER_BLOCK,
    EXIT_BLOCK
}