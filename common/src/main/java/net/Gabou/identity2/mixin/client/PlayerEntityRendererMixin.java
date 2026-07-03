package net.Gabou.identity2.mixin.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.util.EntityAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
public class PlayerEntityRendererMixin {
    private static final Map<String, ResourceLocation> identity2$resolvedSkinCache = new ConcurrentHashMap<>();
    private static final Set<String> identity2$requestedSkinKeys = ConcurrentHashMap.newKeySet();

    @Inject(
        method = "getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void identity2$overridePlayerSkin(AbstractClientPlayer avatarEntity, CallbackInfoReturnable<ResourceLocation> cir) {
        net.minecraft.world.entity.Entity entity = avatarEntity;
        CompoundTag nbt = ((EntityAccessor) entity).getCustomData();
        String selectedType = net.Gabou.identity2.util.NbtCompat.getStringOr(nbt, IdentityProgression.SELECTED_IDENTITY_TYPE_KEY, "");
        if (selectedType.isBlank()) {
            selectedType = net.Gabou.identity2.util.NbtCompat.getStringOr(nbt, "model_override", "");
        }
        if (!IdentityProgression.PLAYER_IDENTITY_ID.toString().equals(selectedType)) {
            return;
        }

        CompoundTag variant = IdentityProgression.parseVariantNbt(
            net.Gabou.identity2.util.NbtCompat.getStringOr(nbt, IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, "")
        );
        String uuidRaw = net.Gabou.identity2.util.NbtCompat.getStringOr(variant, IdentityProgression.PLAYER_SKIN_UUID_VARIANT_KEY, "").trim();
        String nameRaw = net.Gabou.identity2.util.NbtCompat.getStringOr(variant, IdentityProgression.PLAYER_SKIN_NAME_VARIANT_KEY, "").trim();
        String textureValue = net.Gabou.identity2.util.NbtCompat.getStringOr(variant, IdentityProgression.PLAYER_SKIN_TEXTURE_VALUE_VARIANT_KEY, "").trim();
        String textureSignature = net.Gabou.identity2.util.NbtCompat.getStringOr(variant, IdentityProgression.PLAYER_SKIN_TEXTURE_SIGNATURE_VARIANT_KEY, "").trim();

        UUID uuid = entity.getUUID();
        if (!uuidRaw.isEmpty()) {
            try {
                uuid = UUID.fromString(uuidRaw);
            } catch (Exception ignored) {
            }
        }
        String name = nameRaw.isEmpty() ? "Player" : nameRaw;
        ResourceLocation skinLocation = identity2$resolvePlayerSkin(uuid, name, textureValue, textureSignature);
        if (skinLocation != null) {
            cir.setReturnValue(skinLocation);
        }
    }

    private static ResourceLocation identity2$resolvePlayerSkin(UUID uuid, String name, String textureValue, String textureSignature) {
        Minecraft minecraft = Minecraft.getInstance();
        String safeName = (name == null || name.isBlank()) ? "Player" : name;
        String cacheKey = identity2$skinCacheKey(uuid, safeName);
        ResourceLocation cached = identity2$resolvedSkinCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        GameProfile profile = null;
        ClientPacketListener connection = minecraft.getConnection();
        if (connection != null) {
            PlayerInfo playerInfo = connection.getPlayerInfo(uuid);
            if (playerInfo == null && name != null && !name.isBlank()) {
                playerInfo = connection.getPlayerInfo(name);
            }
            if (playerInfo != null) {
                ResourceLocation fromInfo = identity2$playerInfoSkinLocation(playerInfo);
                if (fromInfo != null) {
                    identity2$resolvedSkinCache.put(cacheKey, fromInfo);
                    return fromInfo;
                }
                Object fromInfoProfile = identity2$invoke(playerInfo, "getProfile");
                if (fromInfoProfile instanceof GameProfile gameProfile) {
                    profile = gameProfile;
                }
            }
        }

        if (profile == null) {
            profile = new GameProfile(uuid, safeName);
        }
        if (identity2$profileHasNoTextures(profile)) {
            profile = identity2$populateProfileTextures(minecraft, profile, uuid);
        }
        identity2$applyProfileTextureProperties(profile, textureValue, textureSignature);

        identity2$ensureSkinRequested(minecraft, profile, cacheKey);
        ResourceLocation fromSkinManager = identity2$skinManagerLocation(minecraft, profile);
        if (fromSkinManager != null) {
            if (!identity2$isDefaultSkin(uuid, fromSkinManager)) {
                identity2$resolvedSkinCache.put(cacheKey, fromSkinManager);
            }
            return fromSkinManager;
        }

        ResourceLocation fromDefaultSkin = identity2$defaultSkinLocation(uuid, profile);
        if (fromDefaultSkin != null) {
            return fromDefaultSkin;
        }

        return DefaultPlayerSkin.getDefaultTexture();
    }

    private static void identity2$applyProfileTextureProperties(GameProfile profile, String textureValue, String textureSignature) {
        if (profile == null || textureValue == null || textureValue.isBlank()) {
            return;
        }
        try {
            profile.getProperties().removeAll("textures");
            if (textureSignature != null && !textureSignature.isBlank()) {
                profile.getProperties().put("textures", new Property("textures", textureValue, textureSignature));
            } else {
                profile.getProperties().put("textures", new Property("textures", textureValue));
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean identity2$profileHasNoTextures(GameProfile profile) {
        if (profile == null) {
            return true;
        }
        try {
            Collection<Property> existing = profile.getProperties().get("textures");
            return existing == null || existing.isEmpty();
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static GameProfile identity2$populateProfileTextures(Minecraft minecraft, GameProfile profile, UUID uuid) {
        if (minecraft == null || profile == null || !identity2$profileHasNoTextures(profile)) {
            return profile;
        }
        try {
            Object sessionService = identity2$invoke(minecraft, "getMinecraftSessionService");
            if (sessionService == null) {
                return profile;
            }

            Object filled = identity2$invoke(sessionService, "fillProfileProperties", profile, Boolean.TRUE);
            if (filled instanceof GameProfile filledProfile && !identity2$profileHasNoTextures(filledProfile)) {
                return filledProfile;
            }

            Object fetched = identity2$invoke(sessionService, "fetchProfile", uuid, Boolean.TRUE);
            Object fetchedProfile = identity2$invoke(fetched, "profile");
            if (fetchedProfile == null) {
                fetchedProfile = identity2$invoke(fetched, "getProfile");
            }
            if (fetchedProfile instanceof GameProfile gameProfile && !identity2$profileHasNoTextures(gameProfile)) {
                return gameProfile;
            }
        } catch (Throwable ignored) {
        }
        return profile;
    }

    private static void identity2$ensureSkinRequested(Minecraft minecraft, GameProfile profile, String cacheKey) {
        if (minecraft == null || profile == null || cacheKey == null || cacheKey.isBlank()) {
            return;
        }
        if (!identity2$requestedSkinKeys.add(cacheKey)) {
            return;
        }

        Object skinManager = minecraft.getSkinManager();
        Class<?> callbackType = identity2$findClass(
            "net.minecraft.client.resources.SkinManager$SkinTextureCallback",
            "net.minecraft.client.resources.SkinManager$SkinAvailableCallback"
        );
        if (skinManager == null || callbackType == null) {
            return;
        }

        InvocationHandler handler = (proxy, method, args) -> {
            if (args != null && args.length >= 2 && args[1] instanceof ResourceLocation textureLocation) {
                Object type = args[0];
                if (type != null && "SKIN".equalsIgnoreCase(type.toString())) {
                    identity2$resolvedSkinCache.put(cacheKey, textureLocation);
                }
            }
            return null;
        };

        Object callback = Proxy.newProxyInstance(
            callbackType.getClassLoader(),
            new Class<?>[] { callbackType },
            handler
        );

        Object direct = identity2$invoke(skinManager, "registerSkins", profile, callback, Boolean.FALSE);
        if (direct == null) {
            identity2$invoke(skinManager, "registerTextures", profile, callback, Boolean.FALSE);
        }
    }

    private static String identity2$skinCacheKey(UUID uuid, String name) {
        String safeName = name == null ? "" : name.trim().toLowerCase(java.util.Locale.ROOT);
        return (uuid == null ? "null" : uuid.toString()) + "|" + safeName;
    }

    private static boolean identity2$isDefaultSkin(UUID uuid, ResourceLocation candidate) {
        if (candidate == null) {
            return true;
        }
        try {
            ResourceLocation defaultSkin = DefaultPlayerSkin.get(uuid).texture();
            return defaultSkin != null && defaultSkin.equals(candidate);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    private static ResourceLocation identity2$playerInfoSkinLocation(PlayerInfo playerInfo) {
        Object direct = identity2$invoke(playerInfo, "getSkinLocation");
        if (direct instanceof ResourceLocation resourceLocation) {
            return resourceLocation;
        }

        Object playerSkin = identity2$invoke(playerInfo, "getSkin");
        return identity2$extractTextureLocation(playerSkin);
    }

    @Nullable
    private static ResourceLocation identity2$skinManagerLocation(Minecraft minecraft, GameProfile profile) {
        Object direct = identity2$invoke(minecraft.getSkinManager(), "getInsecureSkinLocation", profile);
        if (direct instanceof ResourceLocation resourceLocation) {
            return resourceLocation;
        }

        Object playerSkin = identity2$invoke(minecraft.getSkinManager(), "getInsecureSkin", profile);
        return identity2$extractTextureLocation(playerSkin);
    }

    @Nullable
    private static ResourceLocation identity2$defaultSkinLocation(UUID uuid, GameProfile profile) {
        Object direct = identity2$invokeStatic(DefaultPlayerSkin.class, "getDefaultSkin", uuid);
        if (direct instanceof ResourceLocation resourceLocation) {
            return resourceLocation;
        }

        ResourceLocation fromDefaultSkinObj = identity2$extractTextureLocation(direct);
        if (fromDefaultSkinObj != null) {
            return fromDefaultSkinObj;
        }

        Object fromUuid = identity2$invokeStatic(DefaultPlayerSkin.class, "get", uuid);
        ResourceLocation fromUuidTexture = identity2$extractTextureLocation(fromUuid);
        if (fromUuidTexture != null) {
            return fromUuidTexture;
        }

        Object fromProfile = identity2$invokeStatic(DefaultPlayerSkin.class, "get", profile);
        return identity2$extractTextureLocation(fromProfile);
    }

    @Nullable
    private static ResourceLocation identity2$extractTextureLocation(@Nullable Object skinObject) {
        if (skinObject == null) {
            return null;
        }
        if (skinObject instanceof ResourceLocation resourceLocation) {
            return resourceLocation;
        }

        Object texture = identity2$invoke(skinObject, "texture");
        if (texture instanceof ResourceLocation resourceLocation) {
            return resourceLocation;
        }

        Object getTexture = identity2$invoke(skinObject, "getTexture");
        if (getTexture instanceof ResourceLocation resourceLocation) {
            return resourceLocation;
        }
        return null;
    }

    @Nullable
    private static Object identity2$invoke(@Nullable Object target, String methodName, Object... args) {
        if (target == null) {
            return null;
        }
        try {
            Method method = identity2$findMethod(target.getClass(), methodName, args);
            if (method == null) {
                return null;
            }
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Object identity2$invokeStatic(Class<?> owner, String methodName, Object... args) {
        try {
            Method method = identity2$findMethod(owner, methodName, args);
            if (method == null) {
                return null;
            }
            method.setAccessible(true);
            return method.invoke(null, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Method identity2$findMethod(Class<?> owner, String methodName, Object... args) {
        Method[] methods = owner.getMethods();
        for (Method method : methods) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != args.length) {
                continue;
            }
            boolean compatible = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                Object arg = args[i];
                if (arg == null) {
                    continue;
                }
                if (!identity2$isAssignable(parameterTypes[i], arg.getClass())) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                return method;
            }
        }
        return null;
    }

    private static boolean identity2$isAssignable(Class<?> parameterType, Class<?> argumentType) {
        if (parameterType.isAssignableFrom(argumentType)) {
            return true;
        }
        if (parameterType == boolean.class && argumentType == Boolean.class) {
            return true;
        }
        if (parameterType == byte.class && argumentType == Byte.class) {
            return true;
        }
        if (parameterType == short.class && argumentType == Short.class) {
            return true;
        }
        if (parameterType == int.class && argumentType == Integer.class) {
            return true;
        }
        if (parameterType == long.class && argumentType == Long.class) {
            return true;
        }
        if (parameterType == float.class && argumentType == Float.class) {
            return true;
        }
        if (parameterType == double.class && argumentType == Double.class) {
            return true;
        }
        return parameterType == char.class && argumentType == Character.class;
    }

    @Nullable
    private static Class<?> identity2$findClass(String... classNames) {
        if (classNames == null) {
            return null;
        }
        for (String className : classNames) {
            if (className == null || className.isBlank()) {
                continue;
            }
            try {
                return Class.forName(className);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
