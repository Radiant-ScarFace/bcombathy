package com.bcombat.network;

import com.bcombat.BannerlordCombat;
import net.minecraft.util.Identifier;

/**
 * Shared networking constants: the channel {@link Identifier}s every
 * packet in {@code com.bcombat.network.packet} travels on. Kept in one
 * place so client and server registration code (see {@link
 * ServerCombatNetworking} and {@code com.bcombat.client.network.ClientCombatNetworking})
 * never risk drifting apart on the literal channel name.
 * <p>
 * Five channels total: three client-to-server (the discrete action enum,
 * attack direction proposals, guard direction proposals) and two
 * server-to-client (the combat state snapshot, and the separately-throttled
 * stamina snapshot) - see each packet record's class docs for why the
 * split is exactly this shape.
 */
public final class CombatNetworking {

    public static final Identifier C2S_COMBAT_ACTION = id("c2s_combat_action");
    public static final Identifier C2S_ATTACK_DIRECTION = id("c2s_attack_direction");
    public static final Identifier C2S_GUARD_DIRECTION = id("c2s_guard_direction");

    public static final Identifier S2C_COMBAT_SYNC = id("s2c_combat_sync");
    public static final Identifier S2C_STAMINA_SYNC = id("s2c_stamina_sync");

    private CombatNetworking() {
        // Constants holder, no instances.
    }

    private static Identifier id(String path) {
        return BannerlordCombat.id(path);
    }
}