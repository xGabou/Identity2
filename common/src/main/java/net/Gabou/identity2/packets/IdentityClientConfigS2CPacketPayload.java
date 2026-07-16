package net.Gabou.identity2.packets;

import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.ModPackets;
import net.Gabou.identity2.util.NetworkPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Client-visible settings copied from the authoritative server configuration. */
public record IdentityClientConfigS2CPacketPayload(
        boolean enableClientSwapMenu,
        boolean showPlayerNametag,
        boolean renderOwnNametag,
        boolean useIdentitySounds,
        boolean enableMorphAbilities,
        boolean enableFlight,
        float flySpeed,
        boolean overrideCreativeFlySpeed,
        boolean enableMorphTransitionParticles,
        int morphTransitionTicks,
        boolean enableMorphAcquisitionTendrils,
        int morphAcquisitionAnimationTicks,
        boolean identitiesEquipItems,
        boolean identitiesEquipArmor,
        boolean unlockAllVariantsOnFirstUnlock,
        boolean enableModdedMorphWorldInteractions,
        boolean enableModdedMorphInventoryInteractions
) implements NetworkPayload {
    public static final ResourceLocation ID = ModPackets.CLIENT_CONFIG_SYNC_PACKET_ID;

    public static IdentityClientConfigS2CPacketPayload decode(FriendlyByteBuf buffer) {
        return new IdentityClientConfigS2CPacketPayload(
                buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readFloat(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    public static IdentityClientConfigS2CPacketPayload fromSettings() {
        return new IdentityClientConfigS2CPacketPayload(
                IdentitySettings.enableClientSwapMenu, IdentitySettings.showPlayerNametag,
                IdentitySettings.renderOwnNametag, IdentitySettings.useIdentitySounds,
                IdentitySettings.enableMorphAbilities, IdentitySettings.enableFlight, IdentitySettings.flySpeed,
                IdentitySettings.overrideCreativeFlySpeed, IdentitySettings.enableMorphTransitionParticles,
                IdentitySettings.morphTransitionTicks, IdentitySettings.enableMorphAcquisitionTendrils,
                IdentitySettings.morphAcquisitionAnimationTicks, IdentitySettings.identitiesEquipItems,
                IdentitySettings.identitiesEquipArmor, IdentitySettings.unlockAllVariantsOnFirstUnlock,
                IdentitySettings.enableModdedMorphWorldInteractions,
                IdentitySettings.enableModdedMorphInventoryInteractions
        );
    }

    public void applyToClientSettings() {
        IdentitySettings.enableClientSwapMenu = enableClientSwapMenu;
        IdentitySettings.showPlayerNametag = showPlayerNametag;
        IdentitySettings.renderOwnNametag = renderOwnNametag;
        IdentitySettings.useIdentitySounds = useIdentitySounds;
        IdentitySettings.enableMorphAbilities = enableMorphAbilities;
        IdentitySettings.enableFlight = enableFlight;
        IdentitySettings.flySpeed = flySpeed;
        IdentitySettings.overrideCreativeFlySpeed = overrideCreativeFlySpeed;
        IdentitySettings.enableMorphTransitionParticles = enableMorphTransitionParticles;
        IdentitySettings.morphTransitionTicks = morphTransitionTicks;
        IdentitySettings.enableMorphAcquisitionTendrils = enableMorphAcquisitionTendrils;
        IdentitySettings.morphAcquisitionAnimationTicks = morphAcquisitionAnimationTicks;
        IdentitySettings.identitiesEquipItems = identitiesEquipItems;
        IdentitySettings.identitiesEquipArmor = identitiesEquipArmor;
        IdentitySettings.unlockAllVariantsOnFirstUnlock = unlockAllVariantsOnFirstUnlock;
        IdentitySettings.enableModdedMorphWorldInteractions = enableModdedMorphWorldInteractions;
        IdentitySettings.enableModdedMorphInventoryInteractions = enableModdedMorphInventoryInteractions;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(enableClientSwapMenu);
        buffer.writeBoolean(showPlayerNametag);
        buffer.writeBoolean(renderOwnNametag);
        buffer.writeBoolean(useIdentitySounds);
        buffer.writeBoolean(enableMorphAbilities);
        buffer.writeBoolean(enableFlight);
        buffer.writeFloat(flySpeed);
        buffer.writeBoolean(overrideCreativeFlySpeed);
        buffer.writeBoolean(enableMorphTransitionParticles);
        buffer.writeVarInt(morphTransitionTicks);
        buffer.writeBoolean(enableMorphAcquisitionTendrils);
        buffer.writeVarInt(morphAcquisitionAnimationTicks);
        buffer.writeBoolean(identitiesEquipItems);
        buffer.writeBoolean(identitiesEquipArmor);
        buffer.writeBoolean(unlockAllVariantsOnFirstUnlock);
        buffer.writeBoolean(enableModdedMorphWorldInteractions);
        buffer.writeBoolean(enableModdedMorphInventoryInteractions);
    }
}
