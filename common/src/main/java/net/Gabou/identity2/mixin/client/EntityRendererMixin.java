package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.Identity2Client;
import net.Gabou.identity2.client.transition.MorphTransitionHelper;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.LimbAnimatorAccessor;
import net.Gabou.identity2.util.MinecraftClientAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Shulker;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Inject(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @At("RETURN"), cancellable = true)
    private void identity2$createRenderState(T entity, float tickProgress, CallbackInfoReturnable<S> cir) {
        EntityRenderState renderState = cir.getReturnValue();

        Entity renderIdentity = MorphTransitionHelper.resolveRenderIdentity(entity, ((EntityAccessor) entity).getCurrentIdentity(), tickProgress);
        if (renderIdentity != null) {
            EntityRenderer renderer = ((MinecraftClientAccessor) Minecraft.getInstance()).getEntityRenderManager().getRenderer(renderIdentity);
            if (renderer != null) {
                identity2$syncIdentityForRender(entity, renderIdentity);
                EntityRenderState replacement = renderer.createRenderState();
                renderer.extractRenderState(renderIdentity, replacement, tickProgress);
                renderState = replacement;
            }
        }

        identity2$applyModelPartOverrides(entity);
        cir.setReturnValue((S) renderState);
    }

    private static void identity2$syncIdentityForRender(Entity source, Entity identity) {
        identity.setPosRaw(source.position().x, source.position().y, source.position().z);
        if (identity instanceof EnderDragon) {
            identity.setYRot(source.getYRot() + 180.0F);
        } else {
            identity.setYRot(source.getYRot());
        }
        ((EntityAccessor) identity).setLastPosition(source.oldPosition());

        if (identity instanceof LivingEntity livingIdentity && source instanceof LivingEntity livingSource) {
            if (livingIdentity.isJumping() != livingSource.isJumping()) {
                livingIdentity.setJumping(livingSource.isJumping());
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
                identity2$forceBatFlightAnimation(batIdentity, source.tickCount);
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

    private static void identity2$forceBatFlightAnimation(Bat bat, int tickCount) {
        bat.setResting(false);
        Object flyAnimationState = identity2$getFieldValue(bat, "flyAnimationState");
        Object restAnimationState = identity2$getFieldValue(bat, "restAnimationState");
        identity2$invokeOneArg(flyAnimationState, "startIfStopped", tickCount);
        identity2$invokeNoArg(restAnimationState, "stop");
    }

    private static Object identity2$getFieldValue(Object target, String fieldName) {
        if (target == null || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(fieldName);
                if (!field.canAccess(target)) {
                    field.setAccessible(true);
                }
                return field.get(target);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object identity2$invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != 0) {
                    continue;
                }
                try {
                    if (!method.canAccess(target)) {
                        method.setAccessible(true);
                    }
                    return method.invoke(target);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static Object identity2$invokeOneArg(Object target, String methodName, Object arg) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> param = method.getParameterTypes()[0];
                if (arg != null && !(param.isAssignableFrom(arg.getClass()) || (param == int.class && arg instanceof Integer))) {
                    continue;
                }
                try {
                    if (!method.canAccess(target)) {
                        method.setAccessible(true);
                    }
                    return method.invoke(target, arg);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static void identity2$applyModelPartOverrides(Entity entity) {
        CompoundTag nbt = ((NbtComponentAccessor) (Object) (((EntityAccessor) entity).getCustomData())).getNbt();
        boolean hasHiddenPartOverrides = false;
        for (String key : nbt.keySet()) {
            if (key.startsWith("hidden_parts.")) {
                hasHiddenPartOverrides = true;
                break;
            }
        }

        boolean shouldHideHead = false;
        if (Minecraft.getInstance().player != null) {
            Entity playerIdentity = ((EntityAccessor) Minecraft.getInstance().player).getCurrentIdentity();
            shouldHideHead = playerIdentity instanceof Shulker;
        }

        if (!hasHiddenPartOverrides && !shouldHideHead) {
            return;
        }

        EntityModel model = Identity2Client.getModel(entity);
        if (model == null) {
            return;
        }

        if (hasHiddenPartOverrides) {
            for (String key : nbt.keySet()) {
                if (key.startsWith("hidden_parts.")) {
                    ModelPart part = model.root().createPartLookup().apply(key.substring(13));
                    if (part != null) {
                        part.skipDraw = nbt.getBooleanOr(key, false);
                    }
                }
            }
        }

        if (shouldHideHead) {
            ModelPart head = model.root().createPartLookup().apply("head");
            if (head != null) {
                head.skipDraw = true;
                head.xScale = 0.0F;
            }
        }
    }
}
