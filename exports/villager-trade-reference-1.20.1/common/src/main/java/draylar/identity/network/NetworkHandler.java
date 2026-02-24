package draylar.identity.network;

import draylar.identity.Identity;
import net.minecraft.util.ResourceLocation;

public interface NetworkHandler {
    ResourceLocation CAN_OPEN_MENU = Identity.id("can_open_menu");
    ResourceLocation IDENTITY_REQUEST = Identity.id("request");
    ResourceLocation FAVORITE_UPDATE = Identity.id("favorite");
    ResourceLocation USE_ABILITY = Identity.id("use_ability");
    ResourceLocation IDENTITY_SYNC = Identity.id("identity_sync");
    ResourceLocation FAVORITE_SYNC = Identity.id("favorite_sync");
    ResourceLocation ABILITY_SYNC = Identity.id("ability_sync");
    ResourceLocation CONFIG_SYNC = Identity.id("config_sync");
    ResourceLocation UNLOCK_SYNC = Identity.id("unlock_sync");
    ResourceLocation OPEN_PROFESSION_SCREEN = Identity.id("open_profession_screen");
    ResourceLocation SET_PROFESSION = Identity.id("set_profession");
    ResourceLocation START_TRADE = Identity.id("start_trade");
    ResourceLocation VILLAGER_IDENTITIES_SYNC = Identity.id("villager_identities_sync");
}
