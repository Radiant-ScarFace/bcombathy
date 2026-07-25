package com.bcombat.client.mixin;

import com.bcombat.client.feedback.camera.CameraShakeManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies {@link CameraShakeManager}'s current yaw/pitch offset to the
 * render camera immediately after vanilla positions and rotates it for
 * the frame. Purely a rendering nudge: this runs after {@code
 * Camera#update} has already computed the camera's "real" rotation from
 * the focused entity's look direction, and only adds a small transient
 * offset on top for this frame - the player entity's actual rotation
 * (and therefore aim, attack direction detection, and everything combat-
 * related) is completely untouched.
 * <p>
 * Skipped entirely for anything other than the client's own primary
 * render camera, and a no-op whenever {@link CameraShakeManager} has no
 * active trauma, so this costs nothing when the feature is idle or
 * disabled.
 */
@Mixin(Camera.class)
public abstract class CameraShakeMixin {

    @Shadow
    public abstract float getYaw();

    @Shadow
    public abstract float getPitch();

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void bcombat$applyCameraShake(BlockView area, Entity focusedEntity, boolean thirdPerson,
                                          boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (!CameraShakeManager.isActive()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getCameraEntity() != focusedEntity) {
            return;
        }
        float yawOffset = CameraShakeManager.getYawOffsetDegrees(tickDelta);
        float pitchOffset = CameraShakeManager.getPitchOffsetDegrees(tickDelta);
        if (yawOffset == 0.0f && pitchOffset == 0.0f) {
            return;
        }
        setRotation(getYaw() + yawOffset, getPitch() + pitchOffset);
    }
}