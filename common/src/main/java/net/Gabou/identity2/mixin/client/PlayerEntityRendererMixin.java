package net.Gabou.identity2.mixin.client;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(
        method = "getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void identity2$overridePlayerSkin(AbstractClientPlayer avatarEntity, CallbackInfoReturnable<ResourceLocation> cir) {
        net.minecraft.world.entity.Entity entity = (net.minecraft.world.entity.Entity) avatarEntity;
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

        UUID uuid = entity.getUUID();
        if (!uuidRaw.isEmpty()) {
            try {
                uuid = UUID.fromString(uuidRaw);
            } catch (Exception ignored) {
            }
        }
        String name = nameRaw.isEmpty() ? "Player" : nameRaw;
        PlayerSkin skin = identity2$resolvePlayerSkin(uuid, name);
        if (skin != null) {
            cir.setReturnValue(skin.texture());
        }
    }

    private static PlayerSkin identity2$resolvePlayerSkin(UUID uuid, String name) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection != null) {
            PlayerInfo playerInfo = connection.getPlayerInfo(uuid);
            if (playerInfo == null && name != null && !name.isBlank()) {
                playerInfo = connection.getPlayerInfo(name);
            }
            if (playerInfo != null) {
                PlayerSkin playerSkin = playerInfo.getSkin();
                if (playerSkin != null) {
                    return playerSkin;
                }
            }
        }
        return minecraft.getSkinManager().getInsecureSkin(new GameProfile(uuid, (name == null || name.isBlank()) ? "Player" : name));
    }
}


