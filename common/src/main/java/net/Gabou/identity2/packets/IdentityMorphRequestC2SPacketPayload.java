package net.Gabou.identity2.packets;

import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record IdentityMorphRequestC2SPacketPayload(String identityId, String variantId) implements NetworkPayload {
    public static final ResourceLocation ID = ModPackets.IDENTITY_MORPH_REQUEST_PACKET_ID;

    public static IdentityMorphRequestC2SPacketPayload decode(FriendlyByteBuf buffer) {
        return new IdentityMorphRequestC2SPacketPayload(buffer.readUtf(256), buffer.readUtf(64));
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(identityId == null ? "" : identityId, 256);
        buffer.writeUtf(variantId == null ? "" : variantId, 64);
    }
}
