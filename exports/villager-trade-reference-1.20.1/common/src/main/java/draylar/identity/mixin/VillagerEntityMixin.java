package draylar.identity.mixin;

import draylar.identity.api.PlayerIdentity;
import draylar.identity.impl.PlayerDataProvider;
import draylar.identity.mixin.accessor.VillagerEntityAccessor;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.poi.PointOfInterestTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {

    @Shadow protected abstract void sayNo();
    @Inject(
            method = "interactMob",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        LivingEntity identity = PlayerIdentity.getIdentity(player);

        if(identity != null && identity.getType() == EntityType.ZOMBIE_VILLAGER) {
            this.sayNo();
            cir.setReturnValue(ActionResult.SUCCESS);
        }

        // If this Villager is a player's Identity, enforce workstation validity before opening trades
        if (!player.getWorld().isClient) {
            VillagerEntity villager = (VillagerEntity) (Object) this;
            ServerPlayerEntity owner = null;

            for (ServerPlayerEntity sp : villager.getServer().getPlayerManager().getPlayerList()) {
                if (PlayerIdentity.getIdentity(sp) == villager) {
                    owner = sp;
                    break;
                }
            }

            if (owner != null) {
                PlayerDataProvider data = (PlayerDataProvider) owner;
                String activeKey = data.getActiveVillagerKey();
                if (activeKey != null && data.getVillagerIdentities().containsKey(activeKey)) {
                    NbtCompound tag = data.getVillagerIdentities().get(activeKey);
                    String dim = tag.getString("WorkstationDim");
                    long posLong = tag.contains("WorkstationPos") ? tag.getLong("WorkstationPos") : Long.MIN_VALUE;

                    boolean invalid = (dim == null || dim.isEmpty() || posLong == Long.MIN_VALUE);
                    if (!invalid) {
                        ServerWorld world = owner.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(dim)));
                        if (world == null) {
                            invalid = true;
                        } else {
                            BlockPos pos = BlockPos.fromLong(posLong);
                            if (world.isAir(pos) || PointOfInterestTypes.getTypeForState(world.getBlockState(pos)).isEmpty()) {
                                invalid = true;
                            }
                        }
                    }

                    if (invalid) {
                        this.sayNo();
                        if (player instanceof ServerPlayerEntity sp) {
                            sp.sendMessage(Text.translatable("identity.profession.invalid_workstation"), true);
                        }
                        cir.setReturnValue(ActionResult.SUCCESS);
                    }
                }
            }
        }
    }

    @Inject(method = "afterUsing", at = @At("TAIL"))
    private void onTrade(TradeOffer offer, CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;
        ServerPlayerEntity owner = null;

        for (ServerPlayerEntity player : villager.getServer().getPlayerManager().getPlayerList()) {
            if (PlayerIdentity.getIdentity(player) == villager) {
                owner = player;
                break;
            }
        }

        if (owner != null) {
            PlayerDataProvider data = (PlayerDataProvider) owner;
            String activeKey = data.getActiveVillagerKey();
            if (activeKey != null && data.getVillagerIdentities().containsKey(activeKey)) {
                NbtCompound existing = data.getVillagerIdentities().get(activeKey);
                NbtCompound updated = new NbtCompound();
                villager.writeNbt(updated);
                updated.putString("ProfessionId", Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().getProfession()).toString());
                if (existing.contains("WorkstationDim")) {
                    updated.putString("WorkstationDim", existing.getString("WorkstationDim"));
                }
                if (existing.contains("WorkstationPos")) {
                    updated.putLong("WorkstationPos", existing.getLong("WorkstationPos"));
                }
                updated.putString("IdentityName", activeKey);
                data.setVillagerIdentity(activeKey, updated);
                PlayerIdentity.sync(owner);

                // Ensure leveling occurs when XP threshold is met for the identity villager
                VillagerEntityAccessor accessor = (VillagerEntityAccessor) villager;
                if (((VillagerEntityAccessor) villager).callGetNextLevelExperience()) {
                    accessor.callLevelUp();
                }
            }
        }
    }
}
