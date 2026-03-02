package net.Gabou.identity2.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public interface NetworkPayload {
    ResourceLocation id();

    void write(FriendlyByteBuf buffer);
}
