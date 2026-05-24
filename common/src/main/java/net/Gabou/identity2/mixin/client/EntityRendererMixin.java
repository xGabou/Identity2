package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.Identity2Client;
import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.identity.MorphEntityTraits;
import net.Gabou.identity2.client.render.MorphRenderContext;
import net.Gabou.identity2.client.render.MorphRenderStateHelper;
import net.Gabou.identity2.client.transition.MorphTransitionHelper;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.LimbAnimatorAccessor;
import net.Gabou.identity2.util.LivingEntityAccessor;
import net.Gabou.identity2.util.MinecraftClientAccessor;
import net.Gabou.identity2.util.NbtCompat;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AllayRenderState;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.client.renderer.entity.state.BeeRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.FoxRenderState;
import net.minecraft.client.renderer.entity.state.GoatRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;
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
        MorphRenderContext.Context context = MorphRenderContext.current();
        Entity modelOverrideSource = entity;

        if (context != null && context.matches(entity)) {
            modelOverrideSource = context.source();
            identity2$patchMorphRenderState(context.source(), entity, renderState, tickProgress);
        }

        Entity renderIdentity = MorphTransitionHelper.resolveRenderIdentity(entity, ((EntityAccessor) entity).getCurrentIdentity(), tickProgress);
        if (renderIdentity != null) {
            EntityRenderer renderer = ((MinecraftClientAccessor) Minecraft.getInstance()).getEntityRenderManager().getRenderer(renderIdentity);
            if (renderer != null && renderer == (Object) this) {
                identity2$syncIdentityForRender(entity, renderIdentity, tickProgress);
                EntityRenderState replacement = renderer.createRenderState();
                renderer.extractRenderState(renderIdentity, replacement, tickProgress);
                identity2$patchMorphRenderState(entity, renderIdentity, replacement, tickProgress);
                renderState = replacement;
            }
        }

        MorphRenderStateHelper.resetAndApplyModelPartOverrides(entity, modelOverrideSource);
        cir.setReturnValue((S) renderState);
    }

    private static void identity2$syncIdentityForRender(Entity source, Entity identity, float tickProgress) {
        if (identity instanceof EnderDragon) {
            identity2$syncDragonMultipartPosition((EnderDragon) identity, source.position());
            identity.setYRot(source.getYRot() + 180.0F);
        } else {
            identity.setPosRaw(source.position().x, source.position().y, source.position().z);
            identity.setYRot(source.getYRot());
        }
        ((EntityAccessor) identity).setLastPosition(source.oldPosition());

        if (identity instanceof LivingEntity livingIdentity && source instanceof LivingEntity livingSource) {
            identity2$applyBabyVariantState(source, identity);
            identity2$syncLivingHealthForRender(livingSource, livingIdentity);
            boolean sourceJumping = ((LivingEntityAccessor) livingSource).identity2$isJumping();
            if (((LivingEntityAccessor) livingIdentity).identity2$isJumping() != sourceJumping) {
                livingIdentity.setJumping(sourceJumping);
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
        identity.setShiftKeyDown(!(identity instanceof Parrot) && source.isShiftKeyDown());
        identity.setSprinting(source.isSprinting());
        identity.setSwimming(source.isSwimming());
        if (identity instanceof Parrot) {
            identity.setPose(Pose.STANDING);
        }

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

        identity.setSharedFlagOnFire(source.isOnFire() && !MorphEntityTraits.isFireImmune(identity));
        identity2$syncEntityAnimationState(source, identity, tickProgress);
    }

    private static void identity2$syncDragonMultipartPosition(EnderDragon dragon, Vec3 targetPos) {
        if (dragon == null || targetPos == null) {
            return;
        }
        Vec3 previous = dragon.position();
        Vec3 delta = targetPos.subtract(previous);
        dragon.setPosRaw(targetPos.x, targetPos.y, targetPos.z);
        if (delta.lengthSqr() <= 1.0E-8D) {
            return;
        }
        for (Entity part : dragon.getSubEntities()) {
            if (part == null) {
                continue;
            }
            Vec3 shifted = part.position().add(delta);
            part.setPosRaw(shifted.x, shifted.y, shifted.z);
        }
    }

    private static void identity2$syncLivingHealthForRender(LivingEntity source, LivingEntity identity) {
        float sourceMaxHealth = source.getMaxHealth();
        float identityMaxHealth = identity.getMaxHealth();
        if (sourceMaxHealth <= 0.0F || identityMaxHealth <= 0.0F) {
            return;
        }
        float scaledHealth = source.getHealth() * (identityMaxHealth / sourceMaxHealth);
        identity.setHealth(Math.max(0.0F, Math.min(identityMaxHealth, scaledHealth)));
    }

    private static void identity2$patchMorphRenderState(Entity source, Entity identity, EntityRenderState renderState, float tickProgress) {
        MorphRenderStateHelper.applySharedState(source, identity, renderState, tickProgress);

        if (renderState instanceof LivingEntityRenderState livingState) {
            identity2$applyBabyRenderState(source, identity, livingState);
        }

        if (renderState instanceof AllayRenderState allayState && identity instanceof LivingEntity livingIdentity) {
            if (!livingIdentity.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
                allayState.holdingAnimationProgress = Math.max(allayState.holdingAnimationProgress, 1.0F);
            }
        }

        if (renderState instanceof BeeRenderState beeState && source instanceof LivingEntity livingSource) {
            if (livingSource.attackAnim > 0.0F || livingSource.swinging) {
                beeState.isAngry = true;
                beeState.rollAmount = Math.max(beeState.rollAmount, livingSource.attackAnim);
            }
        }

        if (renderState instanceof ChickenRenderState chickenState) {
            Vec3 motion = source.getDeltaMovement();
            if (!source.onGround() && motion.y < -0.02D) {
                chickenState.flapSpeed = Math.max(chickenState.flapSpeed, 1.0F);
                chickenState.flap = source.tickCount + tickProgress;
            }
        }

        if (renderState instanceof ParrotRenderState parrotState) {
            parrotState.pose = source.onGround() ? ParrotModel.Pose.STANDING : ParrotModel.Pose.FLYING;
        }
        if (renderState instanceof LivingEntityRenderState livingState && identity instanceof Parrot) {
            livingState.pose = Pose.STANDING;
        }
        if (renderState instanceof FoxRenderState foxState && identity.getType() == EntityType.FOX) {
            boolean jumpActive = PredefIdentityAbilities.isSyncedAnimationActive(source, PredefIdentityAbilities.ANIM_JUMP_TICKS_KEY);
            foxState.isPouncing = jumpActive;
            foxState.isFaceplanted = false;
            foxState.isCrouching = false;
            foxState.crouchAmount = jumpActive ? 1.0F : 0.0F;
        }
        if (renderState instanceof GoatRenderState goatState && identity.getType() == EntityType.GOAT) {
            goatState.rammingXHeadRot = PredefIdentityAbilities.isSyncedAnimationActive(source, PredefIdentityAbilities.ANIM_CHARGE_TICKS_KEY)
                ? (30.0F * ((float) Math.PI / 180.0F))
                : 0.0F;
            if (renderState instanceof LivingEntityRenderState livingState) {
                livingState.bodyRot = source.getYRot();
                livingState.yRot = source.getYRot();
            }
        }

    }

    private static void identity2$applyBabyVariantState(Entity source, Entity identity) {
        CompoundTag variant = identity2$getSelectedVariant(source);
        if (variant == null) {
            return;
        }
        Boolean baby = identity2$resolveBabyVariant(variant);
        if (baby == null) {
            return;
        }
        identity2$invokeOneArg(identity, "setBaby", baby);
        identity2$invokeOneArg(identity, "setAge", baby ? -24000 : 0);
    }

    private static void identity2$applyBabyRenderState(Entity source, Entity identity, LivingEntityRenderState renderState) {
        CompoundTag variant = identity2$getSelectedVariant(source);
        if (variant == null) {
            return;
        }
        Boolean baby = identity2$resolveBabyVariant(variant);
        if (baby == null) {
            return;
        }
        renderState.isBaby = baby;
        Object scale = identity2$invokeNoArg(identity, "getAgeScale");
        if (scale instanceof Number number) {
            renderState.ageScale = number.floatValue();
        } else if (!baby) {
            renderState.ageScale = 1.0F;
        }
    }

    private static CompoundTag identity2$getSelectedVariant(Entity source) {
        if (!(source instanceof EntityAccessor accessor)) {
            return null;
        }
        CompoundTag nbt = ((NbtComponentAccessor) (Object) accessor.getCustomData()).getNbt();
        String raw = NbtCompat.getStringOr(nbt, IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, "");
        if (raw.isBlank()) {
            return null;
        }
        return IdentityProgression.parseVariantNbt(raw);
    }

    private static Boolean identity2$resolveBabyVariant(CompoundTag variant) {
        if (variant == null || variant.isEmpty()) {
            return null;
        }
        if (variant.contains("IsBaby", Tag.TAG_BYTE)) {
            return variant.getBoolean("IsBaby");
        }
        if (variant.contains("Baby", Tag.TAG_BYTE)) {
            return variant.getBoolean("Baby");
        }
        if (variant.contains("Age", Tag.TAG_ANY_NUMERIC)) {
            return variant.getInt("Age") < 0;
        }
        return null;
    }

    private static void identity2$forceBatFlightAnimation(Bat bat, int tickCount) {
        bat.setResting(false);
        Object flyAnimationState = identity2$getFieldValue(bat, "flyAnimationState");
        Object restAnimationState = identity2$getFieldValue(bat, "restAnimationState");
        identity2$invokeOneArg(flyAnimationState, "startIfStopped", tickCount);
        identity2$invokeNoArg(restAnimationState, "stop");
    }

    private static void identity2$syncEntityAnimationState(Entity source, Entity identity, float tickProgress) {
        if (source == null || identity == null) {
            return;
        }

        if (identity instanceof IronGolem) {
            identity2$setIntFieldExact(identity, "attackAnimationTick", Math.max(0, PredefIdentityAbilities.getSyncedTicksRemaining(source, PredefIdentityAbilities.ANIM_ATTACK_TICKS_KEY)));
        }

        int attackTicks = Math.max(
            PredefIdentityAbilities.getSyncedTicksRemaining(source, PredefIdentityAbilities.ANIM_ATTACK_TICKS_KEY),
            PredefIdentityAbilities.getSyncedTicksRemaining(source, PredefIdentityAbilities.ANIM_CHARGE_TICKS_KEY)
        );
        if (identity instanceof Ravager) {
            identity2$setIntFieldExact(identity, "attackTick", attackTicks);
        }

        if (identity instanceof Hoglin) {
            identity2$setIntFieldExact(identity, "attackAnimationRemainingTicks", attackTicks);
        }

        if (identity instanceof Warden) {
            int beamStart = (int) PredefIdentityAbilities.getSyncedAnimationStartTick(source, PredefIdentityAbilities.ANIM_BEAM_TICKS_KEY);
            int attackStart = (int) PredefIdentityAbilities.getSyncedAnimationStartTick(source, PredefIdentityAbilities.ANIM_ATTACK_TICKS_KEY);
            identity2$syncAnimationStateField(identity, "sonicBoomAnimationState", PredefIdentityAbilities.isSyncedAnimationActive(source, PredefIdentityAbilities.ANIM_BEAM_TICKS_KEY), beamStart);
            identity2$syncAnimationStateField(identity, "attackAnimationState", PredefIdentityAbilities.isSyncedAnimationActive(source, PredefIdentityAbilities.ANIM_ATTACK_TICKS_KEY), attackStart);
        }

        if (identity instanceof Pufferfish) {
            int puffState = PredefIdentityAbilities.isSyncedAnimationActive(source, PredefIdentityAbilities.PUFFER_PUFF_TICKS_KEY) ? 2 : 0;
            identity2$invokeOneArg(identity, "setPuffState", puffState);
        }

        if (identity instanceof Parrot) {
            identity2$syncParrotMotion(identity, source, tickProgress);
        }

        if (identity instanceof Squid) {
            identity2$syncSquidMotion(identity, source);
        }

    }

    private static void identity2$syncAnimationStateField(Object target, String fieldName, boolean active, int startTick) {
        Object state = identity2$getFieldValue(target, fieldName);
        if (state == null) {
            return;
        }
        if (active) {
            identity2$invokeOneArg(state, "startIfStopped", startTick);
        } else {
            identity2$invokeNoArg(state, "stop");
        }
    }

    private static void identity2$syncParrotMotion(Entity identity, Entity source, float tickProgress) {
        Object previousFlap = identity2$getFieldValue(identity, "flap");
        Object previousFlapSpeed = identity2$getFieldValue(identity, "flapSpeed");
        Vec3 motion = source.getDeltaMovement();
        float motionSpeed = (float) motion.horizontalDistance();
        float flapSpeed = source.onGround() ? Math.min(1.0F, motionSpeed * 4.0F) : Math.min(1.0F, 0.6F + motionSpeed * 6.0F);
        float time = source.tickCount + tickProgress;
        identity2$setFloatFieldExact(identity, "oFlap", previousFlap instanceof Number number ? number.floatValue() : time - 1.0F);
        identity2$setFloatFieldExact(identity, "oFlapSpeed", previousFlapSpeed instanceof Number number ? number.floatValue() : flapSpeed);
        identity2$setFloatFieldExact(identity, "flapSpeed", Math.max(0.15F, flapSpeed));
        identity2$setFloatFieldExact(identity, "flap", time);
    }

    private static void identity2$syncSquidMotion(Entity identity, Entity source) {
        Vec3 motion = source.getDeltaMovement();
        float currentMovement = identity2$getFloatFieldExact(identity, "tentacleMovement", 0.0F);
        float currentAngle = identity2$getFloatFieldExact(identity, "tentacleAngle", 0.0F);
        float nextMovement = currentMovement + 0.04F;
        if (nextMovement > Math.PI * 2.0D) {
            nextMovement -= (float) (Math.PI * 2.0D);
        }

        float nextAngle;
        if (source.isInWater()) {
            if (nextMovement < (float) Math.PI) {
                float phase = nextMovement / (float) Math.PI;
                nextAngle = Mth.sin(phase * phase * (float) Math.PI) * (float) Math.PI * 0.25F;
            } else {
                nextAngle = 0.0F;
            }
        } else {
            nextAngle = Mth.abs(Mth.sin(nextMovement)) * (float) Math.PI * 0.25F;
        }

        float xBodyRot = identity2$getFloatFieldExact(identity, "xBodyRot", 0.0F);
        float zBodyRot = identity2$getFloatFieldExact(identity, "zBodyRot", 0.0F);
        double horizontalSpeed = motion.horizontalDistance();
        float targetPitch = source.isInWater()
            ? -((float) Mth.atan2(horizontalSpeed, motion.y)) * (180.0F / (float) Math.PI)
            : -90.0F;

        identity2$setFloatFieldExact(identity, "oldTentacleMovement", currentMovement);
        identity2$setFloatFieldExact(identity, "tentacleMovement", nextMovement);
        identity2$setFloatFieldExact(identity, "oldTentacleAngle", currentAngle);
        identity2$setFloatFieldExact(identity, "tentacleAngle", nextAngle);
        identity2$setFloatFieldExact(identity, "xBodyRotO", xBodyRot);
        identity2$setFloatFieldExact(identity, "xBodyRot", xBodyRot + (targetPitch - xBodyRot) * 0.1F);
        identity2$setFloatFieldExact(identity, "zBodyRotO", zBodyRot);
        identity2$setFloatFieldExact(identity, "zBodyRot", source.isInWater() ? zBodyRot + (float) Math.PI * 0.06F : zBodyRot);
    }

    private static float identity2$getFloatFieldExact(Object target, String fieldName, float fallback) {
        Object value = identity2$getFieldValue(target, fieldName);
        return value instanceof Number number ? number.floatValue() : fallback;
    }

    private static void identity2$setFloatFieldExact(Object target, String fieldName, float value) {
        if (target == null || fieldName == null || fieldName.isBlank()) {
            return;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(fieldName);
                if (field.getType() != float.class && field.getType() != Float.class) {
                    continue;
                }
                if (!field.canAccess(target)) {
                    field.setAccessible(true);
                }
                field.setFloat(target, value);
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    private static void identity2$setIntFieldExact(Object target, String fieldName, int value) {
        if (target == null || fieldName == null || fieldName.isBlank()) {
            return;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(fieldName);
                if (field.getType() != int.class && field.getType() != Integer.class) {
                    continue;
                }
                if (!field.canAccess(target)) {
                    field.setAccessible(true);
                }
                field.setInt(target, value);
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    private static void identity2$setEnumFieldIfPresent(Object target, String fieldName, String enumConstant) {
        if (target == null || fieldName == null || fieldName.isBlank() || enumConstant == null || enumConstant.isBlank()) {
            return;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(fieldName);
                if (!field.getType().isEnum()) {
                    continue;
                }
                if (!field.canAccess(target)) {
                    field.setAccessible(true);
                }
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object value = Enum.valueOf((Class<? extends Enum>) field.getType(), enumConstant);
                field.set(target, value);
                return;
            } catch (Throwable ignored) {
            }
        }
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

}
