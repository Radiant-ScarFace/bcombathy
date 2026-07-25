package com.bcombat.server;

import com.bcombat.BannerlordCombat;
import com.bcombat.combat.ai.AICombatManager;
import com.bcombat.combat.controller.CombatController;
import com.bcombat.combat.controller.CombatControllerManager;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Owns every server lifecycle hook the combat framework needs in order
 * to create and destroy {@link CombatController}/{@code
 * AICombatController} instances at exactly the right moments, so no
 * stale entry is ever left ticking a disconnected player or a mob that
 * no longer exists, and no live combatant is ever missing one.
 * <p>
 * Registers against four independent Fabric API events, each owning one
 * slice of the controller lifecycle:
 * <ul>
 *     <li>{@link ServerPlayConnectionEvents#JOIN} — creates a fresh,
 *     authoritative {@link CombatController} for a newly connected
 *     player the instant they're ready, rather than waiting for the
 *     first incidental lookup.</li>
 *     <li>{@link ServerPlayConnectionEvents#DISCONNECT} — removes that
 *     player's server-side controller so the tick loop stops driving a
 *     combatant nobody is controlling anymore.</li>
 *     <li>{@link ServerPlayerEvents#AFTER_RESPAWN} — a respawn swaps in a
 *     brand new {@code ServerPlayerEntity} instance for the same UUID;
 *     the old instance's controller is discarded and a fresh one is
 *     created for the new instance, so combat state never carries over
 *     from a death (matching vanilla's own "death resets you" semantics)
 *     and no controller is ever left pointing at a defunct entity
 *     object.</li>
 *     <li>{@link ServerLivingEntityEvents#AFTER_DEATH} — for an
 *     AI-controlled {@link MobEntity} (never a player — players are
 *     handled by the respawn hook above), tears down both the AI
 *     decision layer and the underlying {@link CombatController} the
 *     instant it dies, since a dead mob's entity object will never be
 *     ticked again.</li>
 *     <li>{@link ServerLifecycleEvents#SERVER_STOPPED} — clears both
 *     registries wholesale on shutdown, so nothing survives into the
 *     next server session.</li>
 * </ul>
 * Every other way an AI-controlled mob's entity object can stop existing
 * without dying (chunk unload, world change, forced removal, natural
 * despawn) is deliberately NOT handled by a dedicated unload event here.
 * Instead, {@link ServerCombatTickHandler} prunes any AI controller whose
 * entity is no longer alive/present as part of its normal per-tick pass
 * — see that class's docs — which covers exactly the same cases with one
 * fewer Fabric API surface this class depends on.
 * <p>
 * Client-side registries are untouched here — {@link
 * CombatControllerManager#clearClient()} is driven by the client's own
 * disconnect handling, entirely independent of this (server-only) class.
 */
public final class ServerCombatLifecycleHandler {

    private ServerCombatLifecycleHandler() {
        // Static registrar, no instances.
    }

    /**
     * Registers every lifecycle hook this class owns. Must be called
     * exactly once, from {@code BannerlordCombat#onInitialize()}.
     */
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register(ServerCombatLifecycleHandler::onPlayerJoin);
        ServerPlayConnectionEvents.DISCONNECT.register(ServerCombatLifecycleHandler::onPlayerDisconnect);
        ServerPlayerEvents.AFTER_RESPAWN.register(ServerCombatLifecycleHandler::onPlayerRespawn);
        ServerLivingEntityEvents.AFTER_DEATH.register(ServerCombatLifecycleHandler::onLivingEntityDeath);
        ServerLifecycleEvents.SERVER_STOPPED.register(ServerCombatLifecycleHandler::onServerStopped);
    }

    private static void onPlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        // Eagerly creates the controller rather than waiting for the
        // first incidental CombatControllerManager.get() call, so the
        // tick loop (which only iterates already-tracked controllers)
        // picks this player up starting the very next server tick.
        CombatControllerManager.get(handler.player);
    }

    private static void onPlayerDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        CombatControllerManager.removeServer(handler.player.getUuid());
    }

    private static void onPlayerRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        // The old ServerPlayerEntity instance is defunct the instant
        // respawn completes - discard whatever controller it had (its
        // combat state is meaningless once dead) and create a brand new
        // one for the instance that actually keeps playing.
        CombatControllerManager.removeServer(oldPlayer.getUuid());
        CombatControllerManager.get(newPlayer);
    }

    private static void onLivingEntityDeath(LivingEntity entity, DamageSource damageSource) {
        // Players are intentionally excluded here: their controller
        // lifecycle is fully owned by the join/disconnect/respawn hooks
        // above, since a dead ServerPlayerEntity instance either respawns
        // (handled by onPlayerRespawn) or disconnects (handled by
        // onPlayerDisconnect) - never simply "dies and keeps existing"
        // the way a mob's entity object can linger briefly post-death.
        if (entity instanceof PlayerEntity) {
            return;
        }
        if (entity instanceof MobEntity mob) {
            AICombatManager.disable(mob);
        }
        CombatControllerManager.remove(entity);
    }

    private static void onServerStopped(MinecraftServer server) {
        AICombatManager.clear();
        CombatControllerManager.clearServer();
        BannerlordCombat.LOGGER.info("Combat framework registries cleared on server stop.");
    }
}
