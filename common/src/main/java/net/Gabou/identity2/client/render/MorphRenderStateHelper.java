package net.Gabou.identity2.client.render;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import net.Gabou.identity2.Identity2Client;
import net.Gabou.identity2.PredefIdentityAbilities;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;

public final class MorphRenderStateHelper {
    private MorphRenderStateHelper() {
    }

    public static void applySharedState(Entity source, Entity identity, EntityRenderState renderState, float tickProgress) {
        if (renderState instanceof LivingEntityRenderState livingState) {
            applyBabyRenderState(source, identity, livingState);
        }

        applyAllayHoldingState(identity, renderState);
        applySyncedAbilityState(source, renderState);
        applyFlightState(source, identity, renderState, tickProgress);
        applyAttackState(source, renderState);
        applyMovementState(source, identity, renderState);
    }

    public static void resetAndApplyModelPartOverrides(Entity entity) {
        EntityModel<?> model = Identity2Client.getModel(entity);
        if (model == null) {
            return;
        }

        resetModelParts(model.root());

        CompoundTag nbt = ((NbtComponentAccessor) (Object) (((EntityAccessor) entity).getCustomData())).getNbt();
        for (String key : nbt.keySet()) {
            if (!key.startsWith("hidden_parts.")) {
                continue;
            }
            ModelPart part = model.root().createPartLookup().apply(key.substring(13));
            if (part != null) {
                part.skipDraw = nbt.getBooleanOr(key, false);
            }
        }
    }

    private static void applyAttackState(Entity source, EntityRenderState renderState) {
        if (!(source instanceof LivingEntity livingSource)) {
            return;
        }
        boolean attacking = livingSource.attackAnim > 0.0F || livingSource.swinging;
        if (!attacking) {
            return;
        }
        setBooleanFieldIfPresent(renderState, "isAngry", true);
        setBooleanFieldIfPresent(renderState, "attacking", true);
        setBooleanFieldIfPresent(renderState, "isAggressive", true);
        setFloatFieldMax(renderState, "rollAmount", livingSource.attackAnim);
    }

    private static void applyMovementState(Entity source, Entity identity, EntityRenderState renderState) {
        Vec3 motion = source.getDeltaMovement();
        if (!source.onGround() && motion.y > 0.05D) {
            startAnimationStates(renderState, source.tickCount, Set.of("jump", "leap", "hop"));
        }
        if (identity != null && (identity.getType() == EntityType.STRIDER || identity.getType() == EntityType.TURTLE)) {
            return;
        }
        if (motion.horizontalDistanceSqr() > 0.01D) {
            startAnimationStates(renderState, source.tickCount, Set.of("walk", "run", "move"));
        }
    }

    private static void applySyncedAbilityState(Entity source, EntityRenderState renderState) {
        boolean beamActive = PredefIdentityAbilities.isSyncedAnimationActive(source, PredefIdentityAbilities.ANIM_BEAM_TICKS_KEY);
        boolean chargeActive = PredefIdentityAbilities.isSyncedAnimationActive(source, PredefIdentityAbilities.ANIM_CHARGE_TICKS_KEY);
        boolean jumpActive = PredefIdentityAbilities.isSyncedAnimationActive(source, PredefIdentityAbilities.ANIM_JUMP_TICKS_KEY);
        boolean rollActive = PredefIdentityAbilities.isSyncedAnimationActive(source, PredefIdentityAbilities.ANIM_ROLL_TICKS_KEY);
        boolean angryActive = PredefIdentityAbilities.isSyncedAnimationActive(source, PredefIdentityAbilities.ANIM_ANGRY_TICKS_KEY);
        boolean attackActive = PredefIdentityAbilities.isSyncedAnimationActive(source, PredefIdentityAbilities.ANIM_ATTACK_TICKS_KEY);
        boolean puffActive = PredefIdentityAbilities.isSyncedAnimationActive(source, PredefIdentityAbilities.PUFFER_PUFF_TICKS_KEY);

        if (beamActive) {
            startAnimationStates(renderState, (int) PredefIdentityAbilities.getSyncedAnimationStartTick(source, PredefIdentityAbilities.ANIM_BEAM_TICKS_KEY), Set.of("beam", "sonic", "laser"));
        }
        if (chargeActive) {
            startAnimationStates(renderState, (int) PredefIdentityAbilities.getSyncedAnimationStartTick(source, PredefIdentityAbilities.ANIM_CHARGE_TICKS_KEY), Set.of("charge", "ram", "bite", "attack"));
            setBooleanFieldIfPresent(renderState, "isAggressive", true);
            setFloatFieldMax(renderState, "attackTicksRemaining", PredefIdentityAbilities.getSyncedTicksRemaining(source, PredefIdentityAbilities.ANIM_CHARGE_TICKS_KEY));
            setIntFieldMax(renderState, "attackAnimationRemainingTicks", PredefIdentityAbilities.getSyncedTicksRemaining(source, PredefIdentityAbilities.ANIM_CHARGE_TICKS_KEY));
        }
        if (jumpActive) {
            startAnimationStates(renderState, (int) PredefIdentityAbilities.getSyncedAnimationStartTick(source, PredefIdentityAbilities.ANIM_JUMP_TICKS_KEY), Set.of("jump", "leap", "hop"));
        }
        if (rollActive) {
            startAnimationStates(renderState, (int) PredefIdentityAbilities.getSyncedAnimationStartTick(source, PredefIdentityAbilities.ANIM_ROLL_TICKS_KEY), Set.of("roll"));
            setFloatFieldMax(renderState, "rollAmount", 1.0F);
        }
        if (angryActive) {
            setBooleanFieldIfPresent(renderState, "isAngry", true);
            setBooleanFieldIfPresent(renderState, "charging", true);
            setBooleanFieldIfPresent(renderState, "isCharging", true);
        }
        if (attackActive) {
            startAnimationStates(renderState, (int) PredefIdentityAbilities.getSyncedAnimationStartTick(source, PredefIdentityAbilities.ANIM_ATTACK_TICKS_KEY), Set.of("attack", "bite"));
            setBooleanFieldIfPresent(renderState, "attacking", true);
            setBooleanFieldIfPresent(renderState, "isAggressive", true);
            setFloatFieldMax(renderState, "attackTicksRemaining", PredefIdentityAbilities.getSyncedTicksRemaining(source, PredefIdentityAbilities.ANIM_ATTACK_TICKS_KEY));
            setIntFieldMax(renderState, "attackAnimationRemainingTicks", PredefIdentityAbilities.getSyncedTicksRemaining(source, PredefIdentityAbilities.ANIM_ATTACK_TICKS_KEY));
        }
        if (puffActive) {
            setBooleanFieldIfPresent(renderState, "inflated", true);
            setBooleanFieldIfPresent(renderState, "isInflated", true);
            setBooleanFieldIfPresent(renderState, "isPuffing", true);
            setFloatFieldMax(renderState, "puffState", 2.0F);
        }
    }

    private static void applyFlightState(Entity source, Entity identity, EntityRenderState renderState, float tickProgress) {
        if (identity instanceof EnderDragon) {
            return;
        }
        Vec3 motion = source.getDeltaMovement();
        if (!source.onGround() && motion.y < -0.02D) {
            setFloatFieldMax(renderState, "flapSpeed", 1.0F);
            setFloatField(renderState, "flap", source.tickCount + tickProgress);
        }
        if (!source.onGround()) {
            startAnimationStates(renderState, source.tickCount, Set.of("fly", "flap", "glide"));
        }
    }

    private static void applyAllayHoldingState(Entity identity, EntityRenderState renderState) {
        if (!(identity instanceof LivingEntity livingIdentity) || livingIdentity.getMainHandItem().isEmpty()) {
            return;
        }
        setFloatFieldMax(renderState, "holdingAnimationProgress", 1.0F);
        setBooleanFieldIfPresent(renderState, "holdingItem", true);
        setBooleanFieldIfPresent(renderState, "isHoldingItem", true);
    }

    private static void applyBabyRenderState(Entity source, Entity identity, LivingEntityRenderState renderState) {
        CompoundTag variant = getSelectedVariant(source);
        if (variant == null) {
            return;
        }
        Boolean baby = resolveBabyVariant(variant);
        if (baby == null) {
            return;
        }
        renderState.isBaby = baby;
        Object scale = invokeNoArg(identity, "getAgeScale");
        if (scale instanceof Number number) {
            renderState.ageScale = number.floatValue();
        } else if (!baby) {
            renderState.ageScale = 1.0F;
        }
    }

    private static CompoundTag getSelectedVariant(Entity source) {
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

    private static Boolean resolveBabyVariant(CompoundTag variant) {
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

    private static void resetModelParts(ModelPart root) {
        if (root == null) {
            return;
        }
        for (ModelPart part : collectModelParts(root)) {
            part.skipDraw = false;
            part.xScale = 1.0F;
            part.yScale = 1.0F;
            part.zScale = 1.0F;
        }
    }

    private static Set<ModelPart> collectModelParts(ModelPart root) {
        Set<ModelPart> parts = new LinkedHashSet<>();
        collectModelParts(root, parts);
        return parts;
    }

    @SuppressWarnings("unchecked")
    private static void collectModelParts(ModelPart part, Set<ModelPart> parts) {
        if (part == null || !parts.add(part)) {
            return;
        }
        for (Class<?> current = part.getClass(); current != null; current = current.getSuperclass()) {
            try {
                Field children = current.getDeclaredField("children");
                if (!children.canAccess(part)) {
                    children.setAccessible(true);
                }
                Object value = children.get(part);
                if (value instanceof java.util.Map<?, ?> map) {
                    for (Object child : map.values()) {
                        if (child instanceof ModelPart modelPart) {
                            collectModelParts(modelPart, parts);
                        }
                    }
                }
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    private static void startAnimationStates(Object renderState, int tickCount, Set<String> names) {
        if (renderState == null || names == null || names.isEmpty()) {
            return;
        }
        for (Class<?> current = renderState.getClass(); current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                String normalizedName = field.getName().toLowerCase(Locale.ROOT);
                if (names.stream().noneMatch(normalizedName::contains)) {
                    continue;
                }
                try {
                    if (!field.canAccess(renderState)) {
                        field.setAccessible(true);
                    }
                    invokeOneArg(field.get(renderState), "startIfStopped", tickCount);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static void setBooleanFieldIfPresent(Object target, String fieldNameFragment, boolean value) {
        Field field = findField(target, fieldNameFragment, boolean.class, Boolean.class);
        if (field == null) {
            return;
        }
        try {
            if (!field.canAccess(target)) {
                field.setAccessible(true);
            }
            field.setBoolean(target, value);
        } catch (Throwable ignored) {
        }
    }

    private static void setFloatFieldMax(Object target, String fieldNameFragment, float minValue) {
        Field field = findField(target, fieldNameFragment, float.class, Float.class);
        if (field == null) {
            return;
        }
        try {
            if (!field.canAccess(target)) {
                field.setAccessible(true);
            }
            float current = field.getFloat(target);
            if (current < minValue) {
                field.setFloat(target, minValue);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void setFloatField(Object target, String fieldNameFragment, float value) {
        Field field = findField(target, fieldNameFragment, float.class, Float.class);
        if (field == null) {
            return;
        }
        try {
            if (!field.canAccess(target)) {
                field.setAccessible(true);
            }
            field.setFloat(target, value);
        } catch (Throwable ignored) {
        }
    }

    private static void setIntFieldMax(Object target, String fieldNameFragment, int minValue) {
        Field field = findField(target, fieldNameFragment, int.class, Integer.class);
        if (field == null) {
            return;
        }
        try {
            if (!field.canAccess(target)) {
                field.setAccessible(true);
            }
            int current = field.getInt(target);
            if (current < minValue) {
                field.setInt(target, minValue);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Field findField(Object target, String fieldNameFragment, Class<?>... acceptedTypes) {
        if (target == null || fieldNameFragment == null || fieldNameFragment.isBlank()) {
            return null;
        }
        String fragment = fieldNameFragment.toLowerCase(Locale.ROOT);
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                String fieldName = field.getName().toLowerCase(Locale.ROOT);
                if (!fieldName.contains(fragment)) {
                    continue;
                }
                for (Class<?> acceptedType : acceptedTypes) {
                    if (acceptedType == field.getType()) {
                        return field;
                    }
                }
            }
        }
        return null;
    }

    private static Object invokeNoArg(Object target, String methodName) {
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

    private static Object invokeOneArg(Object target, String methodName, Object arg) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
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
