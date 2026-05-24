package net.Gabou.identity2.mixin.client;

import net.Gabou.identity2.Identity2Client;
import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.client.render.MorphRenderStateHelper;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.client.transition.MorphTransitionHelper;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.LimbAnimatorAccessor;
import net.Gabou.identity2.util.MinecraftClientAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AllayRenderState;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.client.renderer.entity.state.CreakingRenderState;
import net.minecraft.client.renderer.entity.state.BeeRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
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

        Entity renderIdentity = MorphTransitionHelper.resolveRenderIdentity(entity, ((EntityAccessor) entity).getCurrentIdentity(), tickProgress);
        if (renderIdentity != null) {
            EntityRenderer renderer = ((MinecraftClientAccessor) Minecraft.getInstance()).getEntityRenderManager().getRenderer(renderIdentity);
            if (renderer != null) {
                identity2$syncIdentityForRender(entity, renderIdentity);
                EntityRenderState replacement = renderer.createRenderState();
                renderer.extractRenderState(renderIdentity, replacement, tickProgress);
                identity2$patchMorphRenderState(entity, renderIdentity, replacement, tickProgress);
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
            identity2$applyBabyVariantState(source, identity);
            identity2$syncLivingHealthForRender(livingSource, livingIdentity);
            if (livingIdentity.isJumping() != livingSource.isJumping()) {
                livingIdentity.setJumping(livingSource.isJumping());
            }

            if (identity.getType() != EntityType.STRIDER && identity.getType() != EntityType.TURTLE) {
                LimbAnimatorAccessor target = (LimbAnimatorAccessor) livingIdentity.walkAnimation;
                LimbAnimatorAccessor origin = (LimbAnimatorAccessor) livingSource.walkAnimation;
                target.setPrevSpeed(origin.getPrevSpeed());
                target.setSpeed(origin.getSpeed());
                target.setPosition(origin.getPosition());
                target.setPositionScale(origin.getPositionScale());
            }

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

        if (source.isInWater()) {
            identity.clearFire();
            identity.setSharedFlagOnFire(false);
        } else {
            identity.setSharedFlagOnFire(identity.isOnFire());
        }
        identity2$syncEntityAnimationState(source, identity);
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

        if (renderState instanceof CreakingRenderState creakingState && identity instanceof Creaking && source instanceof LivingEntity livingSource) {
            if (livingSource.attackAnim > 0.0F || livingSource.swinging) {
                creakingState.attackAnimationState.startIfStopped(source.tickCount);
            } else {
                creakingState.attackAnimationState.stop();
            }
        }

        if (identity != null && identity.getType() == EntityType.ZOMBIE && source instanceof LivingEntity livingSource) {
            if (livingSource.attackAnim > 0.0F || livingSource.swinging) {
                identity2$setBooleanFieldIfPresent(renderState, "attacking", true);
                identity2$setBooleanFieldIfPresent(renderState, "aggressive", true);
                identity2$setBooleanFieldIfPresent(renderState, "angry", true);
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
        String raw = nbt.getStringOr(IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, "");
        if (raw.isBlank()) {
            return null;
        }
        return IdentityProgression.parseVariantNbt(raw);
    }

    private static Boolean identity2$resolveBabyVariant(CompoundTag variant) {
        if (variant == null || variant.isEmpty()) {
            return null;
        }
        if (variant.getBoolean("IsBaby").isPresent()) {
            return variant.getBooleanOr("IsBaby", false);
        }
        if (variant.getBoolean("Baby").isPresent()) {
            return variant.getBooleanOr("Baby", false);
        }
        if (variant.getInt("Age").isPresent()) {
            return variant.getInt("Age").get() < 0;
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

    private static void identity2$syncEntityAnimationState(Entity source, Entity identity) {
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

    private static void identity2$setBooleanFieldIfPresent(Object target, String fieldNameFragment, boolean value) {
        if (target == null || fieldNameFragment == null || fieldNameFragment.isBlank()) {
            return;
        }
        String fragment = fieldNameFragment.toLowerCase(java.util.Locale.ROOT);
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!(field.getType() == boolean.class || field.getType() == Boolean.class)) {
                    continue;
                }
                if (!field.getName().toLowerCase(java.util.Locale.ROOT).contains(fragment)) {
                    continue;
                }
                try {
                    if (!field.canAccess(target)) {
                        field.setAccessible(true);
                    }
                    field.setBoolean(target, value);
                    return;
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
