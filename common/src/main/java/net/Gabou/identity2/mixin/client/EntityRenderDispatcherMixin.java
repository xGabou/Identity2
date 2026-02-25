package net.Gabou.identity2.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.Gabou.identity2.client.transition.MorphTransitionHelper;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.LimbAnimatorAccessor;
import net.Gabou.identity2.util.LivingEntityAccessor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Shadow
    public abstract  <T extends Entity> EntityRenderer<? super T> getRenderer(T entity);

    @Shadow
    public  <E extends Entity> void render(E entity, double d, double e, double f, float g, float h, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
    }

    @Redirect(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            ),
            require = 0
    )
    private <E extends Entity> void identity2$renderWithMorphEntity(
            EntityRenderer<? super E> originalRenderer,
            E entity,
            float yaw,
            float tickDelta,
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            int light
    ) {
        Entity renderIdentity = MorphTransitionHelper.resolveRenderIdentity(
                entity,
                ((EntityAccessor) entity).getCurrentIdentity(),
                tickDelta
        );

        if (renderIdentity == null || renderIdentity == entity) {
            originalRenderer.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
            return;
        }

        EntityRenderer identityRenderer = this.getRenderer(renderIdentity);
        if (identityRenderer == null) {
            originalRenderer.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
            return;
        }

        identity2$syncIdentityForRender(entity, renderIdentity);

        //noinspection unchecked
        ((EntityRenderer) identityRenderer).render(renderIdentity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void identity2$syncIdentityForRender(Entity source, Entity identity) {
        identity.setPosRaw(source.position().x, source.position().y, source.position().z);
        if (identity instanceof EnderDragon) {
            identity.setYRot(source.getYRot() + 180.0F);
        } else {
            identity.setYRot(source.getYRot());
        }
        ((EntityAccessor) identity).setLastPosition(new Vec3(source.xOld, source.yOld, source.zOld));

        if (identity instanceof LivingEntity livingIdentity && source instanceof LivingEntity livingSource) {
            if (((LivingEntityAccessor) livingIdentity).identity2$isJumping() != ((LivingEntityAccessor)livingSource).identity2$isJumping()) {
                livingIdentity.setJumping(((LivingEntityAccessor)livingSource).identity2$isJumping());
            }

            LimbAnimatorAccessor target = (LimbAnimatorAccessor) livingIdentity.walkAnimation;
            LimbAnimatorAccessor origin = (LimbAnimatorAccessor) livingSource.walkAnimation;
            target.setPrevSpeed(origin.getPrevSpeed());
            target.setSpeed(origin.getSpeed());
            target.setPosition(origin.getPosition());
            target.setPositionScale(origin.getPositionScale());

            livingIdentity.swinging = livingSource.swinging;
            livingIdentity.swingTime = livingSource.swingTime;
            livingIdentity.oAttackAnim = livingSource.oAttackAnim;
            livingIdentity.attackAnim = livingSource.attackAnim;
            if (!(livingIdentity instanceof Shulker)) {
                livingIdentity.yBodyRot = livingSource.yBodyRot;
                livingIdentity.yBodyRotO = livingSource.yBodyRotO;
            }
            livingIdentity.yHeadRot = livingSource.yHeadRot;
            livingIdentity.yHeadRotO = livingSource.yHeadRotO;
            livingIdentity.swingingArm = livingSource.swingingArm;
            if (livingSource.isUsingItem()) {
                livingIdentity.startUsingItem(livingSource.getUsedItemHand());
            } else {
                livingIdentity.stopUsingItem();
            }

            if (livingIdentity instanceof Bat batIdentity) {
                batIdentity.setResting(false);
                batIdentity.flyAnimationState.startIfStopped(source.tickCount);
                batIdentity.restAnimationState.stop();
            }
        }

        identity.tickCount = source.tickCount;
        identity.setOnGround(source.onGround());
        identity.setDeltaMovement(source.getDeltaMovement());
        identity.setShiftKeyDown(source.isShiftKeyDown());
        identity.setSprinting(source.isSprinting());
        identity.setSwimming(source.isSwimming());
        identity.setPose(source.getPose());

        ((EntityAccessor) identity).setVehicle(source.getVehicle());
        ((EntityAccessor) identity).setTouchingWater(source.isInWater());

        if (identity instanceof Phantom) {
            identity.setXRot(-source.getXRot());
            identity.xRotO = -source.xRotO;
        } else if (!(identity instanceof Shulker)) {
            identity.setXRot(source.getXRot());
            identity.xRotO = source.xRotO;
        }

        if (source instanceof LivingEntity livingSource && identity instanceof LivingEntity livingIdentity) {
            livingIdentity.setItemSlot(EquipmentSlot.MAINHAND, livingSource.getItemBySlot(EquipmentSlot.MAINHAND));
            livingIdentity.setItemSlot(EquipmentSlot.OFFHAND, livingSource.getItemBySlot(EquipmentSlot.OFFHAND));
            livingIdentity.setItemSlot(EquipmentSlot.HEAD, livingSource.getItemBySlot(EquipmentSlot.HEAD));
            livingIdentity.setItemSlot(EquipmentSlot.CHEST, livingSource.getItemBySlot(EquipmentSlot.CHEST));
            livingIdentity.setItemSlot(EquipmentSlot.LEGS, livingSource.getItemBySlot(EquipmentSlot.LEGS));
            livingIdentity.setItemSlot(EquipmentSlot.FEET, livingSource.getItemBySlot(EquipmentSlot.FEET));
        }

        if (source instanceof LivingEntity livingSource && identity instanceof Mob mobIdentity) {
            mobIdentity.setAggressive(livingSource.isUsingItem());
        }

        identity.setSharedFlagOnFire(source.isOnFire());
    }
}
