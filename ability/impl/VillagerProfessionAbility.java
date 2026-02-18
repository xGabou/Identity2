package draylar.identity.ability.impl;

import draylar.identity.ability.IdentityAbility;
import draylar.identity.impl.PlayerDataProvider;
import draylar.identity.network.impl.VillagerProfessionPackets;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import net.minecraft.village.VillagerProfession;

import java.util.Optional;
import java.util.function.Predicate;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class VillagerProfessionAbility extends IdentityAbility<VillagerEntity> {

    @Override
    public void onUse(PlayerEntity player, VillagerEntity identity, World world) {
        if (world.isClient) {
            return;
        }

        if (!player.isSneaking()) {
            ((ServerPlayerEntity) player).sendMessage(Text.translatable("identity.profession.must_sneak"), true);
            return;
        }

        HitResult result = player.raycast(5.0, 0.0F, false);
        if (result.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult blockResult = (BlockHitResult) result;
        Optional<RegistryEntry<PointOfInterestType>> poi = PointOfInterestTypes.getTypeForState(world.getBlockState(blockResult.getBlockPos()));
        if (poi.isPresent()) {
            RegistryEntry<PointOfInterestType> targetPoi = poi.get();

            Identifier poiId = Registries.POINT_OF_INTEREST_TYPE.getId(targetPoi.value());
            Identifier worldId = player.getWorld().getRegistryKey().getValue();
            Map<String, net.minecraft.nbt.NbtCompound> villagerMap = ((PlayerDataProvider) player).getVillagerIdentities();
            String existingName = null;
            String existingProfession = null;
            long workstationPos = blockResult.getBlockPos().asLong();

            for (Map.Entry<String, net.minecraft.nbt.NbtCompound> entry : villagerMap.entrySet()) {
                net.minecraft.nbt.NbtCompound saved = entry.getValue();
                if (matchesWorkstation(saved, worldId, workstationPos)) {
                    existingName = entry.getKey();
                    existingProfession = saved.getString("ProfessionId");
                    break;
                }
            }

            for (VillagerProfession profession : Registries.VILLAGER_PROFESSION) {
                boolean matches = false;

                // 1) Simple ID match: many mappings name POI types after the profession (e.g., minecraft:librarian)
                Identifier profIdDirect = Registries.VILLAGER_PROFESSION.getId(profession);
                if (poiId != null && poiId.equals(profIdDirect)) {
                    matches = true;
                }

                try {
                    Method held = VillagerProfession.class.getMethod("heldWorkstation");
                    Object value = held.invoke(profession);
                    if (!matches && value instanceof RegistryEntry<?> entry) {
                        matches = entry.equals(targetPoi);
                    }
                } catch (NoSuchMethodException e) {
                    try {
                        Method acquirable = VillagerProfession.class.getMethod("acquirableJobSite");
                        Object predicate = acquirable.invoke(profession);
                        if (!matches && predicate instanceof Predicate<?> raw) {
                            @SuppressWarnings("unchecked")
                            Predicate<RegistryEntry<PointOfInterestType>> p = (Predicate<RegistryEntry<PointOfInterestType>>) raw;
                            matches = p.test(targetPoi);
                        }
                    } catch (NoSuchMethodException ignored) {
                        try {
                            Method acquirable = VillagerProfession.class.getMethod("acquirableWorkstation");
                            Object predicate = acquirable.invoke(profession);
                            if (!matches && predicate instanceof Predicate<?> raw) {
                                @SuppressWarnings("unchecked")
                                Predicate<RegistryEntry<PointOfInterestType>> p = (Predicate<RegistryEntry<PointOfInterestType>>) raw;
                                matches = p.test(targetPoi);
                            }
                        } catch (NoSuchMethodException ignoredToo) {
                            // no-op; API mismatch we can't resolve here
                        } catch (IllegalAccessException | InvocationTargetException reflectError) {
                            // ignore and continue
                        }
                    } catch (IllegalAccessException | InvocationTargetException reflectError) {
                        // ignore and continue
                    }
                } catch (IllegalAccessException | InvocationTargetException reflectError) {
                    // ignore and continue
                }

                if (matches) {
                    Identifier profId = Registries.VILLAGER_PROFESSION.getId(profession);
                    VillagerProfessionPackets.openScreen((ServerPlayerEntity) player, profId, blockResult.getBlockPos(), worldId, existingName, existingProfession);
                    break;
                }
            }
        } else {
            ((ServerPlayerEntity) player).sendMessage(Text.translatable("identity.profession.invalid_workstation"), true);
        }
    }

    @Override
    public Item getIcon() {
        return Items.EMERALD;
    }

    private boolean matchesWorkstation(net.minecraft.nbt.NbtCompound tag, Identifier worldId, long workstationPos) {
        if (tag == null) {
            return false;
        }
        String dim = tag.getString("WorkstationDim");
        long storedPos = tag.contains("WorkstationPos") ? tag.getLong("WorkstationPos") : Long.MIN_VALUE;
        return !dim.isEmpty() && storedPos != Long.MIN_VALUE && worldId.toString().equals(dim) && storedPos == workstationPos;
    }
}
