# Exact Flow: How a killer can use the killed player's skin

## 1) Death event enters morph pipeline
- src/main/java/me/ichun/mods/morph/common/core/EventHandlerServer.java:73
- src/main/java/me/ichun/mods/morph/common/core/EventHandlerServer.java:84

When a living entity dies, if killer is a real ServerPlayerEntity, Morph calls:
- MorphHandler.INSTANCE.handleMurderEvent(killer, victim)

## 2) Mode handler decides what to do on kill
- src/main/java/me/ichun/mods/morph/common/morph/MorphHandler.java:179
- src/main/java/me/ichun/mods/morph/common/morph/MorphHandler.java:181

Delegates to current mode (ClassicMode by default in this codebase):
- src/main/java/me/ichun/mods/morph/common/mode/ClassicMode.java:24
- src/main/java/me/ichun/mods/morph/common/mode/ClassicMode.java:28
- src/main/java/me/ichun/mods/morph/common/mode/ClassicMode.java:31

Important config behavior:
- src/main/java/me/ichun/mods/morph/common/config/ConfigServer.java:97
- src/main/java/me/ichun/mods/morph/common/config/ConfigServer.java:101

DEFAULT mode is forced back to CLASSIC in this build.

## 3) Player victim becomes a player morph variant keyed by UUID
- src/main/java/me/ichun/mods/morph/common/morph/MorphHandler.java:263
- src/main/java/me/ichun/mods/morph/common/morph/MorphHandler.java:303
- src/main/java/me/ichun/mods/morph/common/morph/MorphHandler.java:306

If victim is a player, Morph creates a player variant and stores:
- ariant.thisVariant.playerUUID = victim.getGameProfile().getId()

This UUID is the core of skin identity.

## 4) Edge case: killed player already morphed
- src/main/java/me/ichun/mods/morph/common/morph/MorphHandler.java:268
- src/main/java/me/ichun/mods/morph/common/morph/MorphHandler.java:276

If victim is a player currently morphed, code switches to victim's active morph entity before variant creation.
That means killer may acquire current morph form instead of victim's base player skin.

## 5) Variant is saved to killer's morph list
- src/main/java/me/ichun/mods/morph/common/morph/MorphHandler.java:364
- src/main/java/me/ichun/mods/morph/common/morph/MorphHandler.java:380
- src/main/java/me/ichun/mods/morph/common/morph/MorphHandler.java:382

cquireMorph stores variant and syncs it to client.

## 6) When killer morphs into that variant, player UUID drives skin resolution
- src/api/java/me/ichun/mods/morph/api/morph/MorphVariant.java:372
- src/api/java/me/ichun/mods/morph/api/morph/MorphVariant.java:380
- src/api/java/me/ichun/mods/morph/api/morph/MorphVariant.java:382

For EntityType.PLAYER, Morph creates a player-like entity using stored playerUUID.

## 7) Skin is not copied manually; it is fetched from GameProfile/UUID
- src/api/java/me/ichun/mods/morph/api/morph/MorphVariant.java:447
- src/api/java/me/ichun/mods/morph/api/morph/MorphVariant.java:449
- src/api/java/me/ichun/mods/morph/api/morph/MorphVariant.java:461
- src/api/java/me/ichun/mods/morph/api/morph/MorphVariant.java:463

On client, Morph:
1. Resolves GameProfile for stored UUID.
2. Temporarily injects NetworkPlayerInfo into playerInfoMap.
3. Creates RemoteClientPlayerEntity to render correct skin.
4. Removes temporary spoof entry.

Renderer has an extra safety path for missing player info:
- src/main/java/me/ichun/mods/morph/client/render/MorphRenderHandler.java:141
- src/main/java/me/ichun/mods/morph/client/render/MorphRenderHandler.java:144
- src/main/java/me/ichun/mods/morph/client/render/MorphRenderHandler.java:158

## 8) Data model fields that make this possible
- src/api/java/me/ichun/mods/morph/api/morph/MorphVariant.java:643
- src/api/java/me/ichun/mods/morph/api/morph/MorphVariant.java:657
- src/api/java/me/ichun/mods/morph/api/morph/MorphVariant.java:672

MorphVariant.Variant.playerUUID is serialized/deserialized and used as canonical player-skin identity.
