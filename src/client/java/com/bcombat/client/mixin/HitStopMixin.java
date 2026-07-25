package com.bcombat.client.mixin;

import com.bcombat.client.feedback.hitstop.HitStopManager;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Implements the "hit stop" / freeze-frame effect by substituting a held
 * tick-delta for the real one at the very start of the frame render,
 * whenever {@link HitStopManager} reports an active freeze window. Every
 * downstream consumer of this frame's tick delta (world render
 * interpolation, entity render interpolation, the camera) reads the same
 * substituted value, so the visible scene briefly stops advancing
 * between game ticks - a pure rendering trick, not a pause of the game
 * loop: input, networking, and every tick-driven system (including
 * {@code CombatController}) continue completely unaffected underneath.
 * <p>
 * {@link HitStopManager#applyToTickDelta} itself decides whether to
 * substitute anything, so this mixin has zero effect - and zero
 * overhead beyond one static call - when no freeze is active.
 */
@Mixin(GameRenderer.class)
public abstract class HitStopMixin {

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private float bcombat$freezeTickDelta(float tickDelta) {
        return HitStopManager.applyToTickDelta(tickDelta);
    }
}