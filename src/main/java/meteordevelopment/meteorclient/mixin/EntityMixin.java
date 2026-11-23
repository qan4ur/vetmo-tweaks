/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.LivingEntityMoveEvent;
import meteordevelopment.meteorclient.events.entity.player.JumpVelocityMultiplierEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.ICamera;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.ESP;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @ModifyArgs(method = "pushAwayFrom(Lnet/minecraft/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V"))
    private void onPushAwayFrom(Args args, Entity entity) {
        // FakePlayerEntity
        if (entity instanceof FakePlayerEntity player && player.doNotPush) {
            args.set(0, 0.0);
            args.set(2, 0.0);
        }
    }

    @ModifyReturnValue(method = "getJumpVelocityMultiplier", at = @At("RETURN"))
    private float onGetJumpVelocityMultiplier(float original) {
        if ((Object) this == mc.player) {
            JumpVelocityMultiplierEvent event = MeteorClient.EVENT_BUS.post(JumpVelocityMultiplierEvent.get());
            return (original * event.multiplier);
        }

        return original;
    }

    @Inject(method = "move", at = @At("HEAD"))
    private void onMove(MovementType type, Vec3d movement, CallbackInfo info) {
        if ((Object) this == mc.player) {
            MeteorClient.EVENT_BUS.post(PlayerMoveEvent.get(type, movement));
        }
        else if ((Object) this instanceof LivingEntity) {
            MeteorClient.EVENT_BUS.post(LivingEntityMoveEvent.get((LivingEntity) (Object) this, movement));
        }
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> info) {
        if (PostProcessShaders.rendering) {
            Color color = Modules.get().get(ESP.class).getColor((Entity) (Object) this);
            if (color != null) info.setReturnValue(color.getPacked());
        }
    }

    @ModifyReturnValue(method = "isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z", at = @At("RETURN"))
    private boolean isInvisibleToCanceller(boolean original) {
        if (!Utils.canUpdate()) return original;
        ESP esp = Modules.get().get(ESP.class);
        if (esp.isActive() && !esp.shouldSkip((Entity) (Object) this)) return false;
        return original;
    }

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void onIsInvisibleTo(PlayerEntity player, CallbackInfoReturnable<Boolean> info) {
        if (player == null) info.setReturnValue(false);
    }

    @ModifyReturnValue(method = "getPose", at = @At("RETURN"))
    private EntityPose modifyGetPose(EntityPose original) {
        if ((Object) this != mc.player) return original;

        if (original == EntityPose.CROUCHING && !mc.player.isSneaking() && ((PlayerEntityAccessor) mc.player).meteor$canChangeIntoPose(EntityPose.STANDING)) return EntityPose.STANDING;
        return original;
    }

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void updateChangeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if ((Object) this != mc.player) return;

        Freecam freecam = Modules.get().get(Freecam.class);

        if (freecam.isActive()) {
            freecam.changeLookDirection(cursorDeltaX * 0.15, cursorDeltaY * 0.15);
            ci.cancel();
        }
    }
}
