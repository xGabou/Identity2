package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record IdentityAbilityPacketPayload(int entityid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<IdentityAbilityPacketPayload> ID = new CustomPacketPayload.Type<>(ModPackets.IDENTITY_ABILITY_PACKET_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, IdentityAbilityPacketPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        IdentityAbilityPacketPayload::entityid,
        IdentityAbilityPacketPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
