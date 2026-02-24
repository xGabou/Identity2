package net.Gabou.identity2.mixin.client;

import com.mojang.authlib.GameProfile;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.Gabou.identity2.ModEffects;
import java.util.Set;
import net.Gabou.identity2.ModBlocks;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerSkin;
import java.util.Map;
import net.Gabou.identity2.util.MinecraftClientAccessor;
import net.Gabou.identity2.util.LimbAnimatorAccessor;
@Mixin(AvatarRenderer.class)
public class PlayerEntityRendererMixin implements net.Gabou.identity2.util.PlayerEntityRendererAccessor{
    @Shadow
    private void renderHand(PoseStack matrices, SubmitNodeCollector queue, int light, ResourceLocation skinTexture,ModelPart arm, boolean sleeveVisible) {}
    public void callRenderArm(PoseStack matrices, SubmitNodeCollector queue, int light, ResourceLocation skinTexture,ModelPart arm, boolean sleeveVisible) {
    this.renderHand(matrices, queue, light, skinTexture,arm, sleeveVisible);
    }
    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void identity2$overridePlayerSkin(Avatar avatarEntity, AvatarRenderState renderState, float tickProgress, CallbackInfo info) {
        net.minecraft.world.entity.Entity entity = (net.minecraft.world.entity.Entity) avatarEntity;
        CompoundTag nbt = ((NbtComponentAccessor) (Object) ((EntityAccessor) entity).getCustomData()).getNbt();
        String selectedType = nbt.getStringOr(IdentityProgression.SELECTED_IDENTITY_TYPE_KEY, "");
        if (selectedType.isBlank()) {
            selectedType = nbt.getStringOr("model_override", "");
        }
        if (!IdentityProgression.PLAYER_IDENTITY_ID.toString().equals(selectedType)) {
            return;
        }

        CompoundTag variant = IdentityProgression.parseVariantNbt(nbt.getStringOr(IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, ""));
        String uuidRaw = variant.getStringOr(IdentityProgression.PLAYER_SKIN_UUID_VARIANT_KEY, "").trim();
        String nameRaw = variant.getStringOr(IdentityProgression.PLAYER_SKIN_NAME_VARIANT_KEY, "").trim();

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
            renderState.skin = skin;
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
            if (playerInfo == null) {
                GameProfile profile = new GameProfile(uuid, (name == null || name.isBlank()) ? "Player" : name);
                Map<UUID, PlayerInfo> seenPlayers = connection.getSeenPlayers();
                playerInfo = seenPlayers.computeIfAbsent(profile.id(), ignored -> new PlayerInfo(profile, false));
            }
            if (playerInfo != null) {
                PlayerSkin playerSkin = playerInfo.getSkin();
                if (playerSkin != null) {
                    return playerSkin;
                }
            }
        }
        return minecraft.getSkinManager().createLookup(new GameProfile(uuid, (name == null || name.isBlank()) ? "Player" : name), false).get();
    }
    /*@Inject(method = "updateRenderState", at = @At("TAIL"))
	private void getAndUpdateRenderStateModifier(AbstractClientPlayerEntity entity,PlayerEntityRenderState playerEntityRenderState, float tickProgress,CallbackInfo info) {
		PlayerEntityRenderState entityRenderState=(PlayerEntityRenderState)playerEntityRenderState;
        if(((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getString("model_override").isPresent()){
            if(((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getString("model_override").get().length()!=0){
                if(Registries.ENTITY_TYPE.containsId(ResourceLocation.of(((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getString("model_override").get()))){
                    EntityType<?> newType=Registries.ENTITY_TYPE.get(ResourceLocation.of(((NbtComponentAccessor)(Object)((EntityAccessor)entity).getCustomData()).getNbt().getString("model_override").get()));
                    playerEntityRenderState.entityType=newType;
                }
                
            }
        }
	}*/
}

