package draylar.identity.api;

import dev.architectury.networking.NetworkManager;
import draylar.identity.Identity;
import draylar.identity.api.variant.IdentityType;
import draylar.identity.impl.PlayerDataProvider;
import draylar.identity.network.NetworkHandler;
import draylar.identity.network.impl.Payload;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.lang.reflect.Method;
import java.util.Map;

public class PlayerIdentity {

    /**
     * Returns the identity associated with the {@link PlayerEntity} this component is attached to.
     *
     * <p>Note that this method may return null, which represents "no identity."
     *
     * @return the current {@link LivingEntity} identity associated with this component's player owner, or null if they have no identity equipped
     */
    public static LivingEntity getIdentity(PlayerEntity player) {
        return ((PlayerDataProvider) player).getIdentity();
    }

    public static IdentityType<?> getIdentityType(PlayerEntity player) {
        return ((PlayerDataProvider) player).getIdentityType();
    }

    public static Map<String, NbtCompound> getVillagerIdentities(PlayerEntity player) {
        return ((PlayerDataProvider) player).getVillagerIdentities();
    }

    public static void setVillagerIdentity(PlayerEntity player, String key, NbtCompound identity) {
        ((PlayerDataProvider) player).setVillagerIdentity(key, identity);
    }

    public static void removeVillagerIdentity(PlayerEntity player, String key) {
        ((PlayerDataProvider) player).removeVillagerIdentity(key);
    }

    /**
     * Sets the identity of the specified player.
     *
     * <p>Setting a identity refreshes the player's dimensions/hitbox, and toggles flight capabilities depending on the entity.
     * To clear this component's identity, pass null.
     *
     * @param entity {@link LivingEntity} new identity for this component, or null to clear
     */
    public static boolean updateIdentity(ServerPlayerEntity player, IdentityType<?> type, LivingEntity entity) {
        // Protect against broken dragons from DragonMounts with null breed
        if(entity == null) {
            ((PlayerDataProvider) player).setIdentityType(type);
            return ((PlayerDataProvider) player).updateIdentity(null);
        }
        if (entity.getClass().getName().equals("com.github.kay9.dragonmounts.dragon.TameableDragon")) {
            try {
                Method getBreed = entity.getClass().getMethod("getBreed");
                Object breed = getBreed.invoke(entity);
                if (breed == null) {
                    player.sendMessage(Text.literal("This dragon identity is broken (no breed). Identity not applied."), false);
                    return false;
                }
            } catch (Throwable t) {
                Identity.LOGGER.warn("[Identity] Failed to validate DragonMounts dragon breed", t);
                return false;
            }
        }

        // Proceed as usual
        ((PlayerDataProvider) player).setIdentityType(type);
        return ((PlayerDataProvider) player).updateIdentity(entity);
    }



    public static void sync(ServerPlayerEntity player) {
        sync(player, player);
    }

    public static void sync(ServerPlayerEntity changed, ServerPlayerEntity packetTarget) {
        NbtCompound entityTag = new NbtCompound();

        // serialize current identity data to tag if it exists
        LivingEntity identity = getIdentity(changed);
        if (identity != null) {
            identity.writeNbt(entityTag);
        }
        String typeId = identity == null
                ? "minecraft:empty"
                : Registries.ENTITY_TYPE.getId(identity.getType()).toString();

        Payload.IdentitySyncPayload payload =
                new Payload.IdentitySyncPayload(changed.getUuid(), typeId, entityTag);

        NetworkManager.sendToPlayer(packetTarget, payload);
    }

}
