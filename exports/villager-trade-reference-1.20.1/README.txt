Villager trade reference export (from this 1.20.1 branch)

Included files:
- common/src/main/java/draylar/identity/network/impl/VillagerTradePackets.java
- common/src/main/java/draylar/identity/network/impl/Payload.java
- common/src/main/java/draylar/identity/network/ServerNetworking.java
- common/src/main/java/draylar/identity/network/NetworkHandler.java
- common/src/main/java/draylar/identity/mixin/VillagerEntityMixin.java
- common/src/main/java/draylar/identity/mixin/accessor/VillagerEntityAccessor.java
- common/src/main/java/draylar/identity/command/IdentityCommand.java
- common/src/main/java/draylar/identity/api/platform/IdentityConfig.java
- fabric/src/main/java/draylar/identity/fabric/config/IdentityFabricConfig.java
- neoforge/src/main/java/draylar/identity/neoforge/config/IdentityNeoForgeConfig.java
- common/src/main/java/draylar/identity/impl/PlayerDataProvider.java
- common/src/main/java/draylar/identity/mixin/player/PlayerEntityDataMixin.java
- common/src/main/java/draylar/identity/api/PlayerIdentity.java
- common/src/main/java/draylar/identity/network/impl/VillagerIdentitiesPackets.java

Notes:
- Self-trade is explicitly wired via /identity_villager trade myself in IdentityCommand.java.
- The C2S trade packet handler is in VillagerTradePackets.java (TradeRequestPayload).
- In this branch, no direct caller of VillagerTradePackets.sendTradeRequest(UUID) was found.
