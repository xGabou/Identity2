package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record IdentityAbilityPacketPayload(int entityid) implements NetworkPayload {
    public static final ResourceLocation ID = ModPackets.IDENTITY_ABILITY_PACKET_ID;

    public static IdentityAbilityPacketPayload decode(FriendlyByteBuf buffer) {
        return new IdentityAbilityPacketPayload(buffer.readVarInt());
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityid);
    }
}
