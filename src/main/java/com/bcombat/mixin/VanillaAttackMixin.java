package com.bcombat.mixin;

import com.bcombat.combat.controller.CombatController;
import com.bcombat.combat.controller.CombatControllerManager;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Disables vanilla's own {@link PlayerEntity#attack(Entity)} - vanilla's
 * entire melee swing/damage/knockback/sweep pipeline - for the exact
 * ticks a player is inside Combat Mode ({@code CombatState != NORMAL}),
 * so the combat framework's own {@code CombatController}-driven
 * wind-up/release/collision/damage pipeline is the only thing that can
 * ever deal melee damage while Combat Mode is engaged.
 * <p>
 * {@code PlayerEntity#attack(Entity)} is the single entry point vanilla
 * calls on <em>both</em> logical sides for a melee swing - client-side
 * from {@code ClientPlayerInteractionManager#attackEntity} the instant
 * the attack key is pressed, and server-side from {@code
 * ServerPlayNetworkHandler#onPlayerInteractEntity} once the matching
 * {@code PlayerInteractEntityC2SPacket} arrives - so this mixin targets
 * {@link PlayerEntity} directly (not a client- or server-only subclass)
 * and lives in the common source set, applied via the common ({@code
 * environment}-unrestricted) mixin config so it is woven into both the
 * client and dedicated-server jars identically. Leaving either side
 * un-mixed would let that side's own copy of vanilla combat continue to
 * run unopposed - client-side prediction would still show a vanilla
 * swing, or the server would still apply vanilla damage independently
 * of {@code CombatController}'s authoritative outcome.
 * <p>
 * Reads combat state off whichever {@link CombatController} already
 * exists for this exact {@link PlayerEntity} instance via {@link
 * CombatControllerManager#getIfPresent} - the client-side predictive/
 * mirror instance on the client, the authoritative instance on the
 * server - rather than {@code get()}, so this never accidentally
 * instantiates a controller for an entity the rest of the framework
 * hasn't started tracking yet (spectators, entities on a side this mod
 * isn't otherwise driving, etc.). No controller yet tracked simply means
 * "not in Combat Mode", so vanilla attack proceeds completely normally -
 * this is exactly how the framework already behaves everywhere else
 * before a player ever presses the Combat Mode key.
 */
@Mixin(PlayerEntity.class)
public abstract class VanillaAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void bcombat$disableVanillaAttackInCombatMode(Entity target, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        CombatController controller = CombatControllerManager.getIfPresent(self);
        if (controller != null && controller.getCombatState().isCombatActive()) {
            // Combat Mode owns melee damage now - the framework's own
            // collision/damage pipeline (CollisionController ->
            // resolveCollisionOutcome -> AttackHitEvent -> DamageService)
            // is what decides whether this swing lands, not vanilla's.
            ci.cancel();
        }
    }
}
