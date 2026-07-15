package net.Gabou.identity2.packets;

import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.ModPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

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
    boolean unlockAllVariantsOnFirstUnlock
) implements CustomPacketPayload {
    public static final Type<IdentityClientConfigS2CPacketPayload> ID =
        new Type<>(ModPackets.CLIENT_CONFIG_SYNC_PACKET_ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, IdentityClientConfigS2CPacketPayload> CODEC =
        new StreamCodec<>() {
            @Override
            public IdentityClientConfigS2CPacketPayload decode(RegistryFriendlyByteBuf buffer) {
                return new IdentityClientConfigS2CPacketPayload(
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readFloat(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean()
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, IdentityClientConfigS2CPacketPayload payload) {
                buffer.writeBoolean(payload.enableClientSwapMenu());
                buffer.writeBoolean(payload.showPlayerNametag());
                buffer.writeBoolean(payload.renderOwnNametag());
                buffer.writeBoolean(payload.useIdentitySounds());
                buffer.writeBoolean(payload.enableMorphAbilities());
                buffer.writeBoolean(payload.enableFlight());
                buffer.writeFloat(payload.flySpeed());
                buffer.writeBoolean(payload.overrideCreativeFlySpeed());
                buffer.writeBoolean(payload.enableMorphTransitionParticles());
                buffer.writeVarInt(payload.morphTransitionTicks());
                buffer.writeBoolean(payload.enableMorphAcquisitionTendrils());
                buffer.writeVarInt(payload.morphAcquisitionAnimationTicks());
                buffer.writeBoolean(payload.identitiesEquipItems());
                buffer.writeBoolean(payload.identitiesEquipArmor());
                buffer.writeBoolean(payload.unlockAllVariantsOnFirstUnlock());
            }
        };

    public static IdentityClientConfigS2CPacketPayload fromSettings() {
        return new IdentityClientConfigS2CPacketPayload(
            IdentitySettings.enableClientSwapMenu,
            IdentitySettings.showPlayerNametag,
            IdentitySettings.renderOwnNametag,
            IdentitySettings.useIdentitySounds,
            IdentitySettings.enableMorphAbilities,
            IdentitySettings.enableFlight,
            IdentitySettings.flySpeed,
            IdentitySettings.overrideCreativeFlySpeed,
            IdentitySettings.enableMorphTransitionParticles,
            IdentitySettings.morphTransitionTicks,
            IdentitySettings.enableMorphAcquisitionTendrils,
            IdentitySettings.morphAcquisitionAnimationTicks,
            IdentitySettings.identitiesEquipItems,
            IdentitySettings.identitiesEquipArmor,
            IdentitySettings.unlockAllVariantsOnFirstUnlock
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
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
