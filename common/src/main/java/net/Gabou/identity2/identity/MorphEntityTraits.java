package net.Gabou.identity2.identity;

import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class MorphEntityTraits {
    private MorphEntityTraits() {
    }

    public static boolean burnsInDaylight(@Nullable Entity identity) {
        if (identity == null) {
            return false;
        }
        if (IdentityTraitTags.burnsInDaylight(identity.getType())) {
            return true;
        }
        if (identity instanceof EnderDragon) {
            return true;
        }
        if (identity instanceof AbstractSkeleton && identity.getType() != EntityType.WITHER_SKELETON) {
            return true;
        }
        return identity instanceof Zombie && identity.getType() != EntityType.ZOMBIFIED_PIGLIN;
    }

    public static boolean shouldBurnInDaylight(@Nullable LivingEntity host, @Nullable Entity identity) {
        return tickSunBurnLikeVanilla(host, identity);
    }

    public static boolean tickSunBurnLikeVanilla(@Nullable LivingEntity host, @Nullable Entity identity) {
        if (host == null || identity == null || !burnsInDaylight(identity)) {
            return false;
        }
        if (!host.isAlive() || host.level() == null || host.level().isClientSide()) {
            return false;
        }
        Boolean sunSensitive = identity2$invokeBoolean(identity, "isSunSensitive");
        if (sunSensitive == null) {
            sunSensitive = Boolean.TRUE;
        }
        Boolean sunBurnTick = identity2$invokeSunBurnTick(identity);
        if (!Boolean.TRUE.equals(sunSensitive) || !Boolean.TRUE.equals(sunBurnTick)) {
            return false;
        }
        ItemStack head = host.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.isEmpty()) {
            if (head.isDamageableItem()) {
                head.setDamageValue(head.getDamageValue() + host.getRandom().nextInt(2));
                if (head.getDamageValue() >= head.getMaxDamage()) {
                    host.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                }
            }
            return false;
        }
        BlockPos exposurePos = BlockPos.containing(host.getX(), host.getEyeY(), host.getZ());
        if (host.isInWater() || host.level().isRainingAt(exposurePos)) {
            return false;
        }
        if (!host.level().canSeeSky(exposurePos)) {
            return false;
        }
        if (host instanceof Player player && player.isCreative()) {
            return false;
        }
        // Vanilla mobs only need the burn timer refreshed occasionally; if we keep
        // forcing a full 8-second refresh every sunlight proc on the morph host,
        // the normal fire tick cadence can look like it is double-applying damage.
        if (host.isOnFire() && host.getRemainingFireTicks() > 40) {
            return true;
        }
        host.igniteForSeconds(8.0F);
        return true;
    }

    public static boolean canBreatheUnderwater(@Nullable Entity identity) {
        if (!(identity instanceof LivingEntity livingIdentity)) {
            return false;
        }
        return livingIdentity.canBreatheUnderwater()
            || Boolean.TRUE.equals(IdentityTraitTags.resolveCanBreatheUnderwater(livingIdentity.getType()));
    }

    public static boolean isFireImmune(@Nullable Entity identity) {
        return identity != null && identity.fireImmune();
    }

    public static boolean hasSlowFalling(@Nullable Entity identity) {
        return identity != null && IdentityTraitTags.hasSlowFalling(identity.getType());
    }

    public static boolean isHostileIdentity(@Nullable Entity identity) {
        if (identity == null) {
            return false;
        }
        return identity instanceof Monster || identity.getType().getCategory() == MobCategory.MONSTER;
    }

    public static boolean hostilesIgnoreTargeting(@Nullable Entity identity) {
        return identity != null && IdentityTraitTags.hostileIgnoresTargeting(identity.getType());
    }

    public static boolean preventsInvalidMorphMounting(@Nullable Entity identity) {
        return identity != null && IdentityTraitTags.preventsInvalidMorphMounting(identity.getType());
    }

    public static boolean shouldBlockHostileTargeting(@Nullable LivingEntity tester, @Nullable LivingEntity identityLiving) {
        if (tester == null || identityLiving == null) {
            return false;
        }
        EntityType<?> testerType = tester.getType();
        EntityType<?> identityType = identityLiving.getType();
        if (testerType == null || identityType == null || testerType.getCategory() != MobCategory.MONSTER) {
            return false;
        }
        if (testerType == identityType || hostilesIgnoreTargeting(identityLiving)) {
            return true;
        }
        if (testerType == EntityType.WARDEN) {
            return identityType == EntityType.WARDEN;
        }
        if (testerType == EntityType.ZOGLIN) {
            return identityType == EntityType.ZOGLIN;
        }
        if (testerType == EntityType.RAVAGER) {
            return identityType != EntityType.VILLAGER
                && identityType != EntityType.WANDERING_TRADER
                && identityType != EntityType.IRON_GOLEM;
        }
        return true;
    }

    public static boolean canIdentityRide(@Nullable Entity identity, @Nullable Entity vehicle) {
        if (!(identity instanceof LivingEntity) || vehicle == null || vehicle.hasPassenger(identity)) {
            return false;
        }
        EntityType<?> riderType = identity.getType();
        EntityType<?> vehicleType = vehicle.getType();
        if (riderType == null || vehicleType == null) {
            return false;
        }
        if (vehicleType == EntityType.RAVAGER) {
            return riderType == EntityType.PILLAGER
                || riderType == EntityType.VINDICATOR
                || riderType == EntityType.EVOKER
                || riderType == EntityType.ILLUSIONER;
        }
        if (vehicleType == EntityType.SPIDER || vehicleType == EntityType.CAVE_SPIDER) {
            return riderType == EntityType.SKELETON
                || riderType == EntityType.STRAY
                || riderType == EntityType.WITHER_SKELETON
                || riderType == EntityType.BOGGED;
        }
        if (vehicleType == EntityType.CHICKEN && identity instanceof Zombie) {
            return Boolean.TRUE.equals(identity2$invokeBoolean(identity, "isBaby"));
        }
        if (vehicleType == EntityType.ZOMBIE_HORSE && identity instanceof Zombie) {
            return true;
        }
        return false;
    }

    public static boolean hasHighJumpAbility(@Nullable Entity identity) {
        return identity != null && IdentityTraitTags.hasHighJumpAbility(identity.getType());
    }

    public static boolean hasSecondaryHighJumpAbility(@Nullable Entity identity) {
        return identity != null && IdentityTraitTags.hasSecondaryHighJumpAbility(identity.getType());
    }

    public static boolean hasRamAttackAbility(@Nullable Entity identity) {
        return identity != null && IdentityTraitTags.hasRamAttackAbility(identity.getType());
    }

    public static boolean hasRollAbility(@Nullable Entity identity) {
        return identity != null && IdentityTraitTags.hasRollAbility(identity.getType());
    }

    public static boolean hasDefensivePuffAbility(@Nullable Entity identity) {
        return identity != null && IdentityTraitTags.hasDefensivePuffAbility(identity.getType());
    }

    public static boolean ignitesTargetsOnMelee(@Nullable Entity identity) {
        return identity != null && IdentityTraitTags.ignitesTargetsOnMelee(identity.getType());
    }

    public static double resolveNaturalMaxHealth(@Nullable ServerLevel level, @Nullable LivingEntity livingIdentity) {
        if (livingIdentity == null) {
            return 20.0D;
        }
        if (level == null) {
            return livingIdentity.getMaxHealth();
        }
        try {
            Entity probe = livingIdentity.getType().create(level, EntitySpawnReason.COMMAND);
            if (probe instanceof LivingEntity probeLiving) {
                return probeLiving.getMaxHealth();
            }
        } catch (Throwable ignored) {
        }
        return livingIdentity.getMaxHealth();
    }

    public static boolean shouldSkipFullServerMorphTick(@Nullable Entity host, @Nullable Entity identity) {
        if (host == null || identity == null) {
            return false;
        }
        if (!(host instanceof Player)) {
            return false;
        }
        if (identity instanceof EnderDragon) {
            return false;
        }
        if (identity instanceof AbstractPiglin) {
            return false;
        }
        return true;
    }

    private static boolean isSunBurnProtectedByHelmet(LivingEntity host) {
        ItemStack head = host.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) {
            return false;
        }
        if (head.isDamageableItem()) {
            head.hurtAndBreak(1, host, EquipmentSlot.HEAD);
        }
        return true;
    }

    @Nullable
    private static Boolean identity2$invokeBoolean(Entity identity, String methodName) {
        if (identity == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Class<?> current = identity.getClass(); current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != 0) {
                    continue;
                }
                try {
                    if (!method.canAccess(identity)) {
                        method.setAccessible(true);
                    }
                    Object value = method.invoke(identity);
                    if (value instanceof Boolean result) {
                        return result;
                    }
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    @Nullable
    private static Boolean identity2$invokeSunBurnTick(Entity identity) {
        return identity2$invokeBoolean(identity, "isSunBurnTick");
    }

}
