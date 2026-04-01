package net.Gabou.identity2.client.transition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

public final class MorphTransitionHelper {
    private static final float PREVIOUS_STAGE_END = 0.35F;
    private static final float NEXT_STAGE_START = 0.65F;
    private static final Map<Integer, CachedIdentity> CACHED_PREVIOUS_IDENTITIES = new ConcurrentHashMap<>();

    private MorphTransitionHelper() {
    }

    public static float getTransitionProgress(Entity host, float partialTick) {
        CompoundTag nbt = ((EntityAccessor) host).getCustomData();
        double duration = nbt.contains(IdentityProgression.TRANSITION_DURATION_TICKS_KEY) ? nbt.getDouble(IdentityProgression.TRANSITION_DURATION_TICKS_KEY): 0.0D;
        if (duration <= 0.0D || host.level() == null) {
            return 1.0F;
        }

        double start = nbt.contains(IdentityProgression.TRANSITION_START_TICK_KEY) ? nbt.getDouble(IdentityProgression.TRANSITION_START_TICK_KEY) : 0.0D;
        double now = host.level().getGameTime() + partialTick;
        return Mth.clamp((float) ((now - start) / duration), 0.0F, 1.0F);
    }

    public static boolean isTransitionActive(Entity host, float partialTick) {
        CompoundTag nbt = ((EntityAccessor) host).getCustomData();
        if ((nbt.contains(IdentityProgression.TRANSITION_DURATION_TICKS_KEY) ? nbt.getDouble(IdentityProgression.TRANSITION_DURATION_TICKS_KEY):0.0D) <= 0.0D) {
            return false;
        }
        return getTransitionProgress(host, partialTick) < 1.0F;
    }

    @Nullable
    public static Entity resolveRenderIdentity(Entity host, @Nullable Entity currentIdentity, float partialTick) {
        if (!isTransitionActive(host, partialTick)) {
            clearCachedPreviousIdentity(host.getId());
            return currentIdentity;
        }

        if (host.isPassenger() || host.getPose() == Pose.SWIMMING) {
            clearCachedPreviousIdentity(host.getId());
            return currentIdentity;
        }

        CompoundTag nbt = ((EntityAccessor) host).getCustomData();
        String previousType = nbt.contains(IdentityProgression.PREVIOUS_IDENTITY_TYPE_KEY) ? nbt.getString(IdentityProgression.PREVIOUS_IDENTITY_TYPE_KEY): "";
        String previousVariant =nbt.contains(IdentityProgression.PREVIOUS_IDENTITY_VARIANT_KEY) ? nbt.getString(IdentityProgression.PREVIOUS_IDENTITY_VARIANT_KEY): "";
        float progress = getTransitionProgress(host, partialTick);

        if (progress >= NEXT_STAGE_START) {
            return currentIdentity;
        }

        boolean usePrevious = progress <= PREVIOUS_STAGE_END || (((host.tickCount + host.getId()) & 1) == 0);
        if (!usePrevious) {
            return currentIdentity;
        }

        return resolvePreviousIdentity(host, currentIdentity, previousType, previousVariant);
    }

    public static void clearCachedPreviousIdentity(int hostEntityId) {
        CACHED_PREVIOUS_IDENTITIES.remove(hostEntityId);
    }

    @Nullable
    private static Entity resolvePreviousIdentity(
        Entity host,
        @Nullable Entity fallbackIdentity,
        String previousType,
        String previousVariant
    ) {
        if (previousType == null || previousType.isBlank()) {
            return fallbackIdentity;
        }
        if (IdentityProgression.BASE_PLAYER_TRANSITION_SENTINEL.equals(previousType)) {
            return null;
        }
        if (host.level() == null) {
            return fallbackIdentity;
        }

        String dimensionId = String.valueOf(host.level().dimension());
        String cacheKey = previousType + "|" + previousVariant + "|" + dimensionId;
        CachedIdentity cached = CACHED_PREVIOUS_IDENTITIES.get(host.getId());
        if (cached != null && cacheKey.equals(cached.cacheKey()) && cached.entity() != null && cached.entity().level() == host.level()) {
            return cached.entity();
        }

        Entity rebuilt = buildIdentity(host, previousType, previousVariant);
        if (rebuilt == null) {
            CACHED_PREVIOUS_IDENTITIES.remove(host.getId());
            return fallbackIdentity;
        }
        CACHED_PREVIOUS_IDENTITIES.put(host.getId(), new CachedIdentity(cacheKey, rebuilt));
        return rebuilt;
    }

    @Nullable
    private static Entity buildIdentity(Entity host, String typeId, String variantRaw) {
        ResourceLocation identifier;
        try {
            identifier = new ResourceLocation(typeId);
        } catch (Exception ignored) {
            return null;
        }
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(identifier)) {
            return null;
        }

        CompoundTag nbt = IdentityProgression.parseVariantNbt(variantRaw);
        nbt.putString("id", identifier.toString());

        try {
            Entity entity = EntityType.loadEntityRecursive(nbt, host.level(), loaded -> {
                loaded.moveTo(host.getX(), host.getY(), host.getZ(), loaded.getYRot(), loaded.getXRot());
                return loaded;
            });
            if (entity == null) {
                return null;
            }
            entity.setId(-Math.abs(host.getId()) - 1000);
            ((EntityAccessor) entity).setIdentityOf(host);
            return entity;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private record CachedIdentity(String cacheKey, Entity entity) {
    }
}

