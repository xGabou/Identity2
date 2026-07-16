package net.Gabou.identity2.packets;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record IdentityUnlockSyncEntry(ResourceLocation identityId, boolean replaceTokens, List<String> variantIds) {
}
