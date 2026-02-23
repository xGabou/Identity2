package draylar.identity.network.impl;

import draylar.identity.network.NetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class Payload {
    public record IdentitySyncPayload(UUID uuid, String id, NbtCompound entityNbt) implements CustomPayload {
        public static final CustomPayload.Id<IdentitySyncPayload> ID =
                new CustomPayload.Id<>(Identifier.of("identity", "identity_sync"));

        public static final PacketCodec<RegistryByteBuf, UUID> UUID_CODEC =
                PacketCodec.of(
                        (value,buf) -> {
                            buf.writeLong(value.getMostSignificantBits());
                            buf.writeLong(value.getLeastSignificantBits());
                        },
                        buf -> new UUID(buf.readLong(), buf.readLong())
                );

        public static final PacketCodec<RegistryByteBuf, IdentitySyncPayload> CODEC =
                PacketCodec.tuple(
                        UUID_CODEC, IdentitySyncPayload::uuid,
                        PacketCodecs.STRING, IdentitySyncPayload::id,
                        PacketCodecs.NBT_COMPOUND, IdentitySyncPayload::entityNbt,
                        IdentitySyncPayload::new
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    public record OpenProfessionScreenPayload(Identifier professionId,
                                              BlockPos pos,
                                              Identifier worldId,
                                              Optional<String> existingName,
                                              Optional<String> existingProfessionId) implements CustomPayload {
        public static final CustomPayload.Id<OpenProfessionScreenPayload> ID =
                new CustomPayload.Id<>(NetworkHandler.OPEN_PROFESSION_SCREEN);

        private static final PacketCodec<RegistryByteBuf, Identifier> IDENTIFIER_CODEC =
                PacketCodec.of((id, buf) -> buf.writeIdentifier(id), buf -> buf.readIdentifier());

        private static final PacketCodec<RegistryByteBuf, BlockPos> BLOCKPOS_CODEC =
                PacketCodec.of((target, buf) -> buf.writeBlockPos(target), buf -> buf.readBlockPos());

        public static final PacketCodec<RegistryByteBuf, OpenProfessionScreenPayload> CODEC =
                PacketCodec.tuple(
                        IDENTIFIER_CODEC, OpenProfessionScreenPayload::professionId,
                        BLOCKPOS_CODEC, OpenProfessionScreenPayload::pos,
                        IDENTIFIER_CODEC, OpenProfessionScreenPayload::worldId,
                        PacketCodecs.optional(PacketCodecs.STRING), OpenProfessionScreenPayload::existingName,
                        PacketCodecs.optional(PacketCodecs.STRING), OpenProfessionScreenPayload::existingProfessionId,
                        OpenProfessionScreenPayload::new
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }


    public record SetProfessionPayload(Identifier professionId,
                                       String name,
                                       boolean reset,
                                       BlockPos pos,
                                       Identifier worldId,
                                       Optional<String> originalName) implements CustomPayload {

        public static final CustomPayload.Id<SetProfessionPayload> ID =
                new CustomPayload.Id<>(NetworkHandler.SET_PROFESSION);

        private static final PacketCodec<RegistryByteBuf, Identifier> IDENTIFIER_CODEC =
                PacketCodec.of((id, buf) -> buf.writeIdentifier(id),
                        buf -> buf.readIdentifier());

        private static final PacketCodec<RegistryByteBuf, BlockPos> BLOCKPOS_CODEC =
                PacketCodec.of((target, buf) -> buf.writeBlockPos(target),
                        buf -> buf.readBlockPos());

        public static final PacketCodec<RegistryByteBuf, SetProfessionPayload> CODEC =
                PacketCodec.tuple(
                        IDENTIFIER_CODEC, SetProfessionPayload::professionId,
                        PacketCodecs.STRING, SetProfessionPayload::name,
                        PacketCodecs.BOOL, SetProfessionPayload::reset,
                        BLOCKPOS_CODEC, SetProfessionPayload::pos,
                        IDENTIFIER_CODEC, SetProfessionPayload::worldId,
                        PacketCodecs.optional(PacketCodecs.STRING), SetProfessionPayload::originalName,
                        SetProfessionPayload::new
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    public record TradeRequestPayload(UUID target) implements CustomPayload {
        public static final CustomPayload.Id<TradeRequestPayload> ID =
                new CustomPayload.Id<>(NetworkHandler.START_TRADE); // same identifier

        // Codec: UUID is 2 longs
        public static final PacketCodec<RegistryByteBuf, TradeRequestPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodec.of(
                                (uuid, buf) -> {
                                    buf.writeLong(uuid.getMostSignificantBits());
                                    buf.writeLong(uuid.getLeastSignificantBits());
                                },
                                buf -> new UUID(buf.readLong(), buf.readLong())
                        ),
                        TradeRequestPayload::target,
                        TradeRequestPayload::new
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record VillagerIdentitiesSyncPayload(NbtCompound data) implements CustomPayload {
        public static final CustomPayload.Id<VillagerIdentitiesSyncPayload> ID =
                new CustomPayload.Id<>(NetworkHandler.VILLAGER_IDENTITIES_SYNC);

        public static final PacketCodec<RegistryByteBuf, VillagerIdentitiesSyncPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.NBT_COMPOUND, VillagerIdentitiesSyncPayload::data,
                        VillagerIdentitiesSyncPayload::new
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }


    public record UseAbilityPayload() implements CustomPayload {
        // Packet ID
        public static final CustomPayload.Id<UseAbilityPayload> ID =
                new CustomPayload.Id<>(Identifier.of("identity", "use_ability"));

        // Codec: always encodes/decodes the same singleton instance
        public static final PacketCodec<RegistryByteBuf, UseAbilityPayload> CODEC =
                PacketCodec.unit(new UseAbilityPayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    public record UnlockSyncPayload(NbtCompound data) implements CustomPayload {
        public static final CustomPayload.Id<UnlockSyncPayload> ID =
                new CustomPayload.Id<>(NetworkHandler.UNLOCK_SYNC);

        public static final PacketCodec<RegistryByteBuf, UnlockSyncPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.NBT_COMPOUND, UnlockSyncPayload::data,
                        UnlockSyncPayload::new
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record FavoriteUpdatePayload(Identifier entityTypeId,
                                        int variant,
                                        boolean favorite) implements CustomPayload {

        public static final CustomPayload.Id<FavoriteUpdatePayload> ID =
                new CustomPayload.Id<>(NetworkHandler.FAVORITE_UPDATE);

        // Mojang official mappings may not have IDENTIFIER, so we roll our own if needed
        private static final PacketCodec<RegistryByteBuf, Identifier> IDENTIFIER_CODEC =
                PacketCodec.of((id, buf) -> buf.writeIdentifier(id),
                        buf -> buf.readIdentifier());

        public static final PacketCodec<RegistryByteBuf, FavoriteUpdatePayload> CODEC =
                PacketCodec.tuple(
                        IDENTIFIER_CODEC, FavoriteUpdatePayload::entityTypeId,
                        PacketCodecs.VAR_INT, FavoriteUpdatePayload::variant,
                        PacketCodecs.BOOL, FavoriteUpdatePayload::favorite,
                        FavoriteUpdatePayload::new
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    public record FavoriteSyncPayload(NbtCompound data) implements CustomPayload {
        public static final CustomPayload.Id<FavoriteSyncPayload> ID =
                new CustomPayload.Id<>(NetworkHandler.FAVORITE_SYNC);

        public static final PacketCodec<RegistryByteBuf, FavoriteSyncPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.NBT_COMPOUND, FavoriteSyncPayload::data,
                        FavoriteSyncPayload::new
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    public record AbilitySyncPayload(int cooldown) implements CustomPayload {
        public static final CustomPayload.Id<AbilitySyncPayload> ID =
                new CustomPayload.Id<>(NetworkHandler.ABILITY_SYNC);

        public static final PacketCodec<RegistryByteBuf, AbilitySyncPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.VAR_INT, AbilitySyncPayload::cooldown,
                        AbilitySyncPayload::new
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    public record ConfigSyncPayload(boolean enableClientSwapMenu,
                                    boolean showPlayerNametag) implements CustomPayload {

        public static final CustomPayload.Id<ConfigSyncPayload> ID =
                new CustomPayload.Id<>(NetworkHandler.CONFIG_SYNC);

        public static final PacketCodec<RegistryByteBuf, ConfigSyncPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.BOOL, ConfigSyncPayload::enableClientSwapMenu,
                        PacketCodecs.BOOL, ConfigSyncPayload::showPlayerNametag,
                        ConfigSyncPayload::new
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
    public record SwapRequestPayload(boolean validType,
                                     @Nullable Identifier entityTypeId,
                                     int variant) implements CustomPayload {

        public static final CustomPayload.Id<SwapRequestPayload> ID =
                new CustomPayload.Id<>(NetworkHandler.IDENTITY_REQUEST);

        private static final PacketCodec<RegistryByteBuf, Identifier> IDENTIFIER_CODEC =
                PacketCodec.of((id, buf) -> buf.writeIdentifier(id),
                        buf -> buf.readIdentifier());

        public static final PacketCodec<RegistryByteBuf, SwapRequestPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.BOOL, SwapRequestPayload::validType,
                        PacketCodecs.optional(IDENTIFIER_CODEC),
                        p -> Optional.ofNullable(p.entityTypeId()),
                        PacketCodecs.VAR_INT, SwapRequestPayload::variant,
                        (validType, optId, variant) ->
                                new SwapRequestPayload(validType, optId.orElse(null), variant)
                );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }









}
