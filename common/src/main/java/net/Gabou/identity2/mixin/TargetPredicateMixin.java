package net.Gabou.identity2.mixin;

import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

@Mixin(TargetingConditions.class)
public class TargetPredicateMixin {
    @Unique
    private static final ThreadLocal<Boolean> identity2$replaceTargetWithIdentity = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(
            method = "test(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void identity2$skipHostileVsHostileMorph(ServerLevel world, @Nullable LivingEntity tester, LivingEntity target, CallbackInfoReturnable<Boolean> info) {
        identity2$replaceTargetWithIdentity.set(Boolean.FALSE);

        if (!IdentitySettings.hostilesIgnoreHostileIdentityPlayer) {
            return;
        }
        if (!(tester instanceof Monster)) {
            return;
        }
        if (!(target instanceof Player player)) {
            return;
        }

        Entity currentIdentity = ((EntityAccessor) target).getCurrentIdentity();
        if (!(currentIdentity instanceof LivingEntity identityLiving)) {
            return;
        }
        if (!identity2$isHostileMob(identityLiving.getType())) {
            return;
        }

        if (IdentitySettings.hostilesForgetNewHostileIdentityPlayer
                && target instanceof ServerPlayer serverPlayer
                && IdentityProgression.isHostileIdentityGraceActive(serverPlayer)) {
            return;
        }

        info.setReturnValue(false);
    }

    @Inject(
            method = "test(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD")
    )
    private void identity2$prepareTargetReplacement(ServerLevel world, @Nullable LivingEntity tester, LivingEntity target, CallbackInfoReturnable<Boolean> info) {
        if (!(target instanceof Player)) {
            return;
        }

        Entity currentIdentity = ((EntityAccessor) target).getCurrentIdentity();
        if (!(currentIdentity instanceof LivingEntity identityLiving)) {
            return;
        }

        identity2$replaceTargetWithIdentity.set(identity2$shouldReplaceTarget(tester, target, identityLiving));
    }

    @Inject(
            method = "test(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("RETURN")
    )
    private void identity2$clearTargetReplacement(ServerLevel world, @Nullable LivingEntity tester, LivingEntity target, CallbackInfoReturnable<Boolean> info) {
        identity2$replaceTargetWithIdentity.remove();
    }

    @ModifyVariable(
            method = "test(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"),
            index = 2
    )
    private LivingEntity injected(LivingEntity target) {
        if (!Boolean.TRUE.equals(identity2$replaceTargetWithIdentity.get())) {
            return target;
        }
        if (((EntityAccessor) target).getCurrentIdentity() instanceof LivingEntity identity) {
            identity.setPosRaw(
                    ((Entity) target).position().x,
                    ((Entity) target).position().y,
                    ((Entity) target).position().z
            );
            ((EntityAccessor) identity).setIdentityOf(target);
            return identity;
        }
        return target;
    }

    @Unique
    private static boolean identity2$shouldReplaceTarget(@Nullable LivingEntity tester, LivingEntity target, LivingEntity identityLiving) {
        EntityType<?> identityType = identityLiving.getType();
        if (identityType == null) {
            return false;
        }

        if (tester == null) {
            return true;
        }

        EntityType<?> testerType = tester.getType();
        if (testerType == EntityType.WOLF) {
            boolean tamed = Boolean.TRUE.equals(identity2$invokeNoArg(tester, "isTame"));
            if (tamed) {
                return IdentitySettings.ownedWolvesAttackIdentityPrey && identity2$isWolfPrey(identityType);
            }
            return IdentitySettings.wolvesAttackIdentityPrey && identity2$isWolfPrey(identityType);
        }

        if (testerType == EntityType.FOX) {
            return IdentitySettings.foxesAttackIdentityPrey && identity2$isFoxPrey(identityType);
        }

        if (testerType == EntityType.VILLAGER || testerType == EntityType.WANDERING_TRADER) {
            return IdentitySettings.villagersRunFromIdentities && identity2$isVillagerFearIdentity(identityType);
        }

        return true;
    }

    @Unique
    private static boolean identity2$isHostileMob(EntityType<?> type) {
        return type != null && type.getCategory() == MobCategory.MONSTER;
    }

    @Unique
    private static boolean identity2$isWolfPrey(EntityType<?> type) {
        return type == EntityType.SHEEP
                || type == EntityType.FOX
                || type == EntityType.SKELETON;
    }

    @Unique
    private static boolean identity2$isFoxPrey(EntityType<?> type) {
        return type == EntityType.CHICKEN
                || type == EntityType.COD
                || type == EntityType.SALMON
                || type == EntityType.PUFFERFISH
                || type == EntityType.TROPICAL_FISH;
    }

    @Unique
    private static boolean identity2$isVillagerFearIdentity(EntityType<?> type) {
        return type == EntityType.ZOMBIE
                || type == EntityType.HUSK
                || type == EntityType.DROWNED
                || type == EntityType.ZOMBIFIED_PIGLIN
                || type == EntityType.ZOMBIE_VILLAGER;
    }

    @Unique
    private static Object identity2$invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Method method : identity2$getAllMethods(target.getClass())) {
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
        return null;
    }

    @Unique
    private static List<Method> identity2$getAllMethods(Class<?> type) {
        List<Method> methods = new java.util.ArrayList<>();
        java.util.Set<String> signatures = new java.util.LinkedHashSet<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                String signature = method.getName() + "#" + method.getParameterCount();
                Class<?>[] params = method.getParameterTypes();
                for (Class<?> param : params) {
                    signature += ":" + param.getName();
                }
                if (signatures.add(signature)) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }
}
