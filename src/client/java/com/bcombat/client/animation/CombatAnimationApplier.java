package com.bcombat.client.animation;

import com.bcombat.combat.controller.CombatController;
import com.bcombat.combat.controller.CombatControllerManager;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;

/**
 * Connects the combat framework's procedural pose system to Minecraft's
 * rendering pipeline. {@code CombatPoseRenderMixin} is the single call
 * site: once per rendered frame, for every {@link LivingEntity} rendered
 * through a {@link BipedEntityModel}, it calls {@link #apply} immediately
 * after vanilla's own {@code EntityModel#setAngles} has already written
 * this frame's base pose onto the model.
 * <p>
 * Every combatant this runs for - the local player, every remote
 * player, and every AI-controlled combatant synchronized via {@code
 * ServerCombatNetworking}/{@code ClientCombatNetworking} - is driven
 * through the exact same {@link CombatController} → {@link
 * CombatPoseCache} → {@link CombatPoseLibrary} pipeline, so any mob
 * rendered with a {@link BipedEntityModel} (every humanoid mob renderer:
 * zombies, skeletons, piglins, illagers, ...) picks up the same
 * directional wind-up/release/block/parry/chamber/couch poses a player
 * does, entirely for free - no per-mob-type wiring needed.
 */
public final class CombatAnimationApplier {

    private CombatAnimationApplier() {
        // Static entry point, no instances.
    }

    /**
     * Overrides {@code model}'s head/body/arm bone rotations with this
     * frame's combat pose for {@code entity}, if any is active. A no-op
     * — leaving vanilla's own pose completely untouched - whenever:
     * <ul>
     *     <li>{@code model} isn't a {@link BipedEntityModel} (this
     *     framework's pose table only ever drives biped bones);</li>
     *     <li>{@code entity} has no tracked {@link CombatController} at
     *     all (a plain vanilla mob never gets one just to be rendered -
     *     see {@link CombatControllerManager#getIfPresent});</li>
     *     <li>or that controller's {@code CombatState} isn't currently
     *     {@link com.bcombat.combat.state.CombatState#isCombatActive()}
     *     - the single gate {@link CombatPoseLibrary}'s own class docs
     *     already document as this applier's responsibility.</li>
     * </ul>
     */
    public static void apply(LivingEntity entity, EntityModel<?> model, float tickDelta) {
        if (!(model instanceof BipedEntityModel<?> biped)) {
            return;
        }

        CombatController controller = CombatControllerManager.getIfPresent(entity);
        if (controller == null || !controller.getCombatState().isCombatActive()) {
            return;
        }

        CombatPose pose = CombatPoseCache.sample(entity, tickDelta);
        if (pose == null) {
            return;
        }

        applyToModel(biped, pose);
    }

    /**
     * Adds each pose field on top of whatever vanilla's own {@code
     * setAngles} already computed for this bone this frame - additive,
     * never a replacement, per {@link CombatPose}'s own documented
     * "relative to vanilla's own rest pose" contract.
     */
    private static void applyToModel(BipedEntityModel<?> model, CombatPose pose) {
        model.head.pitch += pose.headPitch;
        model.head.yaw += pose.headYaw;

        model.body.pitch += pose.bodyPitch;
        model.body.yaw += pose.bodyYaw;

        model.rightArm.pitch += pose.rightArmPitch;
        model.rightArm.yaw += pose.rightArmYaw;
        model.rightArm.roll += pose.rightArmRoll;

        model.leftArm.pitch += pose.leftArmPitch;
        model.leftArm.yaw += pose.leftArmYaw;
        model.leftArm.roll += pose.leftArmRoll;
    }
}