package com.bcombat.client.mixin;

import com.bcombat.client.animation.CombatAnimationApplier;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The rendering half of the combat animation pipeline: hooks every
 * humanoid-model entity renderer - {@code PlayerEntityRenderer}, {@code
 * ZombieEntityRenderer}, {@code SkeletonEntityRenderer}, every other mob
 * renderer that extends {@link LivingEntityRenderer} with a biped model
 * - immediately after vanilla computes this frame's base model pose,
 * and lets {@link CombatAnimationApplier} layer this frame's combat
 * pose on top.
 * <p>
 * Injecting right after {@code EntityModel#setAngles} (rather than the
 * very start or end of {@code render}) is what satisfies "apply on top
 * of vanilla's base pose": every vanilla angle (walk cycle, item-use
 * pose, head look) has already been written onto the model's bones by
 * this point, and {@link CombatAnimationApplier} only ever adds to what
 * is already there rather than replacing it.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class CombatPoseRenderMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Inject(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Lnet/minecraft/entity/Entity;FFFFF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void bcombat$applyCombatPose(T entity, float yaw, float tickDelta, MatrixStack matrices,
                                          VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        @SuppressWarnings("unchecked")
        LivingEntityRenderer<T, M> self = (LivingEntityRenderer<T, M>) (Object) this;
        CombatAnimationApplier.apply(entity, self.getModel(), tickDelta);
    }
}
