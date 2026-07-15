package net.Gabou.identity2.mixin;

import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.sugar.Local;
import net.Gabou.identity2.Identity2;
import net.Gabou.identity2.IdentitySettings;
import net.Gabou.identity2.api.IdentityApi;
import net.Gabou.identity2.checkonly.EntityMethodChecks;
import net.Gabou.identity2.identity.IdentityProgression;
import net.Gabou.identity2.identity.IdentityTraitTags;
import net.Gabou.identity2.identity.IdentityVariantNbtHelper;
import net.Gabou.identity2.identity.IdentityVanillaVariantHelper;
import net.Gabou.identity2.identity.SilverfishBurrowManager;
import net.Gabou.identity2.util.EntityAccessor;
import net.Gabou.identity2.util.EnderDragonEntityAccessor;
import net.Gabou.identity2.util.NbtComponentAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Mixin(Entity.class)
public class EntityMixin implements EntityAccessor {
    @Unique
    private static final String CUSTOM_DATA_TAG_KEY = "identity2_custom_data";

    @Unique
    private static final Set<ResourceLocation> identity2$ravagerRiderIds = Set.of(
            ResourceLocation.fromNamespaceAndPath("minecraft", "pillager"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "vindicator"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "evoker"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "illusioner"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "witch")
    );

    @Nullable
    private CompoundTag persistentData;
    @Shadow
    private int id;

    @Shadow
    public void setId(int id) {
        this.id = id;
    }

    @Shadow
    public int getId() {
        return id;
    }


    @Shadow
    public Vec3 getDeltaMovement() {
        return null;
    }

    @Shadow
    public int getAirSupply() {
        return 0;
    }

    @Shadow
    public void setAirSupply(int air) {
    }

    @Shadow
    public Vec3 position() {
        return null;
    }

    @Shadow
    public final void setPos(Vec3 v) {
    }

    @Shadow
    public void setDeltaMovement(Vec3 v) {
    }

    //    @ModifyConstant(constant=@Constant(doubleValue=3.0E7),method="absSnapTo(DDD)V")
//    private static double TDIOA(double x){
//        return Identity2.maxWorldSize;
//    }
//    @ModifyConstant(constant=@Constant(doubleValue=-3.0E7),method="absSnapTo(DDD)V")
//    private static double TDIOB(double x){
//        return -Identity2.maxWorldSize;
//    }
    // Use the full descriptor to avoid ambiguous remapping of the bare method name.
    @Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At("TAIL"))
    private void moveOnEntityLandOverride(MoverType moverType, Vec3 movementInput, CallbackInfo ci) {
        CompoundTag nbt = this.getCustomData();
        double multiplier = net.Gabou.identity2.util.NbtCompat.getDoubleOr(nbt, "land_speed_multiplier_override", Double.NaN);
        if (this.currentIdentity != null && !Double.isNaN(multiplier) && multiplier != 0.0D) {
            Entity entity = (Entity) (Object) this;
            if (entity.onGround()) {
                entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, multiplier, 1.0));
            }
        }
    }


    @Inject(method = "tick", at = @At("HEAD"))
    private void identityFixCanFlyCheck(CallbackInfo info) {
        //this.identity2$applyShulkerOpenVisualState();
        //this.identity2$applyMorphPassiveTraits();
        if (this.identityOf != null) {
            if (!this.entityCanFlyTickEvaluated) {
                this.entityCanFlyTickEvaluated = true;
                this.entityCanFlyEvaluated = false;
            }
            // Re-evaluate flight periodically: the first evaluation can run before
            // entity type tags are synced/bound, and the result would otherwise be
            // latched forever (flying morphs then never receive their flight grant).
            long now = ((Entity) (Object) this).level() == null ? 0L : ((Entity) (Object) this).level().getGameTime();
            if (this.entityCanFlyLastEvalTick == Long.MIN_VALUE
                    || now - this.entityCanFlyLastEvalTick >= ENTITY_FLY_REEVAL_TICKS) {
                this.entityCanFlyLastEvalTick = now;
                this.entityCanFlyEvaluated = false;
            }
            if (!this.entityCanFlyEvaluated) {
                this.canFly();
            }
            this.identityOf.noPhysics = this.noPhysics;
        }
    }

    @Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
    protected void disableNoClipSuffocate(CallbackInfoReturnable info) {
        if (this.noPhysics) {
            info.setReturnValue(false);
            return;
        }
        if ((Entity) (Object) this instanceof Player player) {
            Entity identity = ((EntityAccessor) player).getCurrentIdentity();
            if (identity != null && !identity2$hasMorphSuffocatingCollision(player)) {
                info.setReturnValue(false);
                return;
            }
            if (
                    identity != null
                            && player.isInWater()
                            && Boolean.TRUE.equals(IdentityTraitTags.resolveCanBreatheUnderwater(identity.getType()))
            ) {
                // Aquatic morphs at water/solid boundaries (e.g. under ice) can trigger false in-wall checks.
                info.setReturnValue(false);
                return;
            }
            if (
                    (!((Entity) (Object) this).level().isClientSide() && IdentityProgression.isMorphDamageGraceActive(player))
                            || (identity != null && identity.getType() == EntityType.ENDER_DRAGON)
            ) {
                info.setReturnValue(false);
                return;
            }
            if (identity != null && ((EntityAccessor) identity).canFly()) {
                info.setReturnValue(false);
            }
        }
    }

    @Unique
    private static boolean identity2$hasMorphSuffocatingCollision(Entity entity) {
        if (entity == null || entity.level() == null) {
            return false;
        }

        AABB box = entity.getBoundingBox().deflate(1.0E-3D);
        if (box.getSize() <= 1.0E-6D) {
            return false;
        }

        Level level = entity.level();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int minX = Mth.floor(box.minX);
        int maxX = Mth.floor(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.floor(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.floor(box.maxZ);
        VoxelShape entityShape = Shapes.create(box);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir() || !state.isSuffocating(level, cursor)) {
                        continue;
                    }

                    VoxelShape blockShape = state.getCollisionShape(level, cursor);
                    if (blockShape.isEmpty()) {
                        continue;
                    }

                    if (Shapes.joinIsNotEmpty(blockShape.move(x, y, z), entityShape, BooleanOp.AND)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Unique
    private static float identity2$scaleHealth(float value, float fromMax, float toMax) {
        if (Float.isNaN(value)) {
            return value;
        }
        if (fromMax <= 0.0F || toMax <= 0.0F || fromMax == toMax) {
            return value;
        }
        return value * (toMax / fromMax);
    }

    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void identity2$ignoreCobwebSlowdownForSpiderMorphs(BlockState state, Vec3 multiplier, CallbackInfo ci) {
        if (!state.is(Blocks.COBWEB)) {
            return;
        }

        EntityType<?> hostType = ((Entity) (Object) this).getType();
        if (hostType == EntityType.SPIDER || hostType == EntityType.CAVE_SPIDER) {
            ci.cancel();
            return;
        }

        if (this.currentIdentity == null) {
            return;
        }
        EntityType<?> identityType = this.currentIdentity.getType();
        if (identityType == EntityType.SPIDER || identityType == EntityType.CAVE_SPIDER) {
            ci.cancel();
        }
    }
    @Inject(method = "canChangeDimensions(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/Level;)Z", at = @At("HEAD"), cancellable = true)
    private void identity2$preventAttachedIdentityDimensionChange(Level from, Level to, CallbackInfoReturnable<Boolean> cir) {
        if (this.identityOf != null) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;", at = @At("HEAD"), cancellable = true)
    private void identity2$preventAttachedIdentityDimensionTravel(DimensionTransition transition, CallbackInfoReturnable<Entity> cir) {
        if (this.identityOf != null) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void identityFix(CallbackInfo info) {
        if (this.currentIdentity != null) {
            if ((Entity) (Object) this instanceof ServerPlayer serverPlayer && serverPlayer.isDeadOrDying()) {
                this.currentIdentity.discard();
                this.currentIdentity = null;
                return;
            }

            boolean hostIsPlayer = ((Entity) (Object) this) instanceof Player;

//            if(((Entity) (Object) this) instanceof Player player)
//            {
//                this.currentIdentity.setInvulnerable(player.isInvulnerable());
//            }
            this.currentIdentity.setPos(this.position());
            this.currentIdentity.setDeltaMovement(this.getDeltaMovement());
            this.currentIdentity.setAirSupply(this.getAirSupply());
            this.currentIdentity.setSwimming(((Entity) (Object) this).isSwimming());
            ((EntityAccessor) this.currentIdentity).setTouchingWater(((Entity) (Object) this).isInWater());
            identity2$syncIdentityEquipmentFromHost((Entity) (Object) this, this.currentIdentity);
            identity2$applySyncedMorphState(this.currentIdentity);
            if (
                    (this.currentIdentity instanceof LivingEntity livingIdentity) &&
                            ((Entity) (Object) this instanceof LivingEntity livingEntity)
            ) {
                if (hostIsPlayer && livingEntity instanceof ServerPlayer serverPlayer) {
                    IdentityProgression.refreshScaledHealth(serverPlayer);
                }
                livingIdentity.setHealth(identity2$scaleHealth(livingEntity.getHealth(), livingEntity.getMaxHealth(), livingIdentity.getMaxHealth()));
            }
            if (!this.currentIdentity.level().isClientSide()) {
                if (this.currentIdentity instanceof Mob mobIdentity) {
                    mobIdentity.setNoAi(true);
                }
                IdentityApi.runMorphTickHandlers((Entity) (Object) this, this.currentIdentity);
                if (this.currentIdentity.level() instanceof ServerLevel identityServerLevel) {
                    identityServerLevel.tickNonPassenger(this.currentIdentity);
                } else {
                    this.currentIdentity.tick();
                }
                if (
                        hostIsPlayer
                                && (Entity) (Object) this instanceof ServerPlayer serverPlayer
                                && serverPlayer.level() instanceof ServerLevel serverLevel
                ) {
                    identity2$tickEnderDragonMorphSimulation(serverPlayer, serverLevel, this.currentIdentity);
                }
            }


            if (hostIsPlayer) {
                // For players, keep vanilla movement/gravity authoritative.
                // Some morph AIs (especially flying mobs) can otherwise inject
                // non-player motion and feel like speed/gravity glitches.
                this.currentIdentity.setPos(this.position());
                this.currentIdentity.setDeltaMovement(this.getDeltaMovement());
                this.setAirSupply(this.currentIdentity.getAirSupply());
                Entity hostEntity = (Entity) (Object) this;
                if (
                    !hostEntity.onGround()
                        && !hostEntity.isInWater()
                        && this.currentIdentity != null
                        && IdentityTraitTags.hasSlowFalling(this.currentIdentity.getType())
                ) {
                    Vec3 motion = hostEntity.getDeltaMovement();
                    if (identity2$shouldSlowFallingFastFall(hostEntity)) {
                        if (motion.y > -0.45D) {
                            hostEntity.setDeltaMovement(motion.x, -0.45D, motion.z);
                        }
                    } else if (motion.y < -0.08D) {
                        hostEntity.setDeltaMovement(motion.x, -0.08D, motion.z);
                        hostEntity.resetFallDistance();
                    }
                }
            } else {
                this.setPos(this.currentIdentity.position());
                this.setDeltaMovement(this.currentIdentity.getDeltaMovement());
                this.setAirSupply(this.currentIdentity.getAirSupply());
            }

            if (hostIsPlayer && (Entity) (Object) this instanceof Player playerHost) {
                identity2$maintainIdentityFlight(playerHost);
                identity2$applyMorphSpecificPlayerTraits(playerHost);
            }

            identity2$applyWardenEffects((Entity) (Object) this);

            if (
                    (this.currentIdentity instanceof LivingEntity livingIdentity) &&
                            ((Entity) (Object) this instanceof LivingEntity livingEntity)
            ) {
                // Do not mirror transient identity damage back into players (prevents login hurt ticks/sounds).
                if (!hostIsPlayer) {
                    livingEntity.setHealth(livingIdentity.getHealth());
                }

            }


        }
    }

    @Unique
    private void identity2$tickEnderDragonMorphSimulation(ServerPlayer host, ServerLevel level, Entity identity) {
        if (!(identity instanceof EnderDragon)) {
            return;
        }

        List<AABB> proxyBoxes = identity2$getEnderDragonProxyBoxes(host);
        identity2$destroyEnderDragonMorphBlocks(level, identity, proxyBoxes);
        identity2$bridgeEnderDragonMorphProjectileDamage(host, level, identity, proxyBoxes);
    }

    @Unique
    private static List<AABB> identity2$getEnderDragonProxyBoxes(Entity host) {
        Vec3 origin = host.position();
        float yaw = host.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));

        List<AABB> boxes = Lists.newArrayList();
        boxes.add(host.getBoundingBox().inflate(2.0D, 1.0D, 2.0D));
        boxes.add(identity2$boxAt(origin.add(forward.scale(1.5D)).add(0.0D, 2.5D, 0.0D), 8.0D, 5.0D, 8.0D));
        boxes.add(identity2$boxAt(origin.add(forward.scale(6.0D)).add(0.0D, 2.7D, 0.0D), 5.0D, 4.0D, 5.0D));
        boxes.add(identity2$boxAt(origin.add(forward.scale(9.0D)).add(0.0D, 2.8D, 0.0D), 4.0D, 4.0D, 4.0D));
        return boxes;
    }

    @Unique
    private static AABB identity2$boxAt(Vec3 center, double width, double height, double depth) {
        double halfWidth = width * 0.5D;
        double halfDepth = depth * 0.5D;
        double halfHeight = height * 0.5D;
        return new AABB(
                center.x - halfWidth,
                center.y - halfHeight,
                center.z - halfDepth,
                center.x + halfWidth,
                center.y + halfHeight,
                center.z + halfDepth
        );
    }

    @Unique
    private static void identity2$destroyEnderDragonMorphBlocks(ServerLevel level, Entity identity, List<AABB> proxyBoxes) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }

        boolean destroyedAny = false;
        boolean blocked = false;

        for (AABB box : proxyBoxes) {
            int minX = Mth.floor(box.minX);
            int minY = Mth.floor(box.minY);
            int minZ = Mth.floor(box.minZ);
            int maxX = Mth.floor(box.maxX);
            int maxY = Mth.floor(box.maxY);
            int maxZ = Mth.floor(box.maxZ);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        if (state.isAir() || state.is(BlockTags.DRAGON_TRANSPARENT)) {
                            continue;
                        }
                        if (state.is(BlockTags.DRAGON_IMMUNE) || state.getDestroySpeed(level, pos) < 0.0F) {
                            blocked = true;
                            continue;
                        }
                        destroyedAny = level.removeBlock(pos, false) || destroyedAny;
                    }
                }
            }
        }

        if (destroyedAny) {
            AABB box = proxyBoxes.get(identity.tickCount % proxyBoxes.size());
            BlockPos eventPos = BlockPos.containing(
                    Mth.lerp(level.random.nextDouble(), box.minX, box.maxX),
                    Mth.lerp(level.random.nextDouble(), box.minY, box.maxY),
                    Mth.lerp(level.random.nextDouble(), box.minZ, box.maxZ)
            );
            level.levelEvent(2008, eventPos, 0);
        }

        if (blocked) {
            identity.resetFallDistance();
        }
    }

    @Unique
    private static void identity2$bridgeEnderDragonMorphProjectileDamage(ServerPlayer host, ServerLevel level, Entity identity, List<AABB> proxyBoxes) {
        AABB searchBox = identity2$combineBoxes(proxyBoxes).inflate(1.0D);
        List<Projectile> projectiles = level.getEntitiesOfClass(
                Projectile.class,
                searchBox,
                projectile -> !projectile.isRemoved() && projectile.getOwner() != host && projectile.getOwner() != identity
        );

        for (Projectile projectile : projectiles) {
            AABB projectileBox = projectile.getBoundingBox().inflate(0.4D);
            if (!identity2$intersectsAny(projectileBox, proxyBoxes)) {
                continue;
            }

            DamageSource source = identity2$projectileDamageSource(host, projectile);
            float damage = identity2$projectileDamageAmount(projectile);
            if (host.hurt(source, damage)) {
                projectile.discard();
            }
        }
    }

    @Unique
    private static AABB identity2$combineBoxes(List<AABB> boxes) {
        AABB combined = boxes.get(0);
        for (int i = 1; i < boxes.size(); i++) {
            combined = combined.minmax(boxes.get(i));
        }
        return combined;
    }

    @Unique
    private static boolean identity2$intersectsAny(AABB testBox, List<AABB> boxes) {
        for (AABB box : boxes) {
            if (testBox.intersects(box)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static DamageSource identity2$projectileDamageSource(ServerPlayer host, Projectile projectile) {
        Entity owner = projectile.getOwner();
        if (projectile instanceof AbstractArrow arrow) {
            return host.damageSources().arrow(arrow, owner != null ? owner : projectile);
        }
        if (projectile instanceof FireworkRocketEntity firework) {
            return host.damageSources().fireworks(firework, owner);
        }
        if (projectile instanceof Fireball fireball) {
            return host.damageSources().fireball(fireball, owner);
        }
        if (owner instanceof LivingEntity livingOwner) {
            return host.damageSources().mobProjectile(projectile, livingOwner);
        }
        return host.damageSources().thrown(projectile, owner);
    }

    @Unique
    private static float identity2$projectileDamageAmount(Projectile projectile) {
        if (projectile instanceof AbstractArrow) {
            return 4.0F;
        }
        if (projectile instanceof Fireball) {
            return 6.0F;
        }
        if (projectile instanceof FireworkRocketEntity) {
            return 5.0F;
        }
        return 2.0F;
    }

    @Unique
    private static void identity2$syncIdentityEquipmentFromHost(Entity host, Entity identity) {
        if (!(host instanceof LivingEntity livingHost) || !(identity instanceof LivingEntity livingIdentity)) {
            return;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = identity2$visibleMorphEquipment(livingHost, slot);
            if (!ItemStack.matches(livingIdentity.getItemBySlot(slot), stack)) {
                livingIdentity.setItemSlot(slot, stack);
            }
        }
    }

    @Unique
    private static ItemStack identity2$visibleMorphEquipment(LivingEntity host, EquipmentSlot slot) {
        if (slot.getType() == EquipmentSlot.Type.HAND && !IdentitySettings.identitiesEquipItems) {
            return ItemStack.EMPTY;
        }
        if (slot.getType() != EquipmentSlot.Type.HAND && !IdentitySettings.identitiesEquipArmor) {
            return ItemStack.EMPTY;
        }
        return host.getItemBySlot(slot).copy();
    }

    @Unique
    private void identity2$applySyncedMorphState(Entity identity) {
        if (!(identity instanceof Armadillo armadillo)) {
            return;
        }
        boolean shellActive = net.Gabou.identity2.util.NbtCompat.getBooleanOr(
                this.getCustomData(),
                net.Gabou.identity2.PredefIdentityAbilities.ARMADILLO_SHELL_STATE_KEY,
                false
        );
        if (shellActive) {
            armadillo.rollUp();
        } else {
            armadillo.rollOut();
        }
    }

    @Unique
    private static boolean identity2$isRavager(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity.getType() == EntityType.RAVAGER) {
            return true;
        }
        if (entity instanceof EntityAccessor accessor) {
            Entity currentIdentity = accessor.getCurrentIdentity();
            return currentIdentity != null && currentIdentity.getType() == EntityType.RAVAGER;
        }
        return false;
    }

    @Unique
    private static boolean identity2$canRideRavager(Player player) {
        return player != null;
    }

    @Unique
    private void identity2$applyMorphSpecificPlayerTraits(Player player) {
        Entity activeIdentity = this.currentIdentity;
        if (activeIdentity == null || player.level().isClientSide()) {
            return;
        }

        if (activeIdentity.getType() == EntityType.DOLPHIN && player.isInWater()) {
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 0, true, false, true));
        }

        if (activeIdentity instanceof EnderDragon dragonIdentity && player.level() instanceof ServerLevel serverLevel) {
            ((EnderDragonEntityAccessor) dragonIdentity).runTickWithEndCrystals();
            if (identity2$invokeTwoArgs(dragonIdentity, "checkWalls", serverLevel, dragonIdentity.getBoundingBox()) == null) {
                identity2$invokeOneArg(dragonIdentity, "checkWalls", dragonIdentity.getBoundingBox());
            }
            if (dragonIdentity.getHealth() > player.getHealth()) {
                player.setHealth(Math.min(player.getMaxHealth(), dragonIdentity.getHealth()));
            }
        }

        if (activeIdentity.fireImmune()) {
            activeIdentity.clearFire();
        }
        if (IdentityTraitTags.burnsInDaylight(activeIdentity.getType())
                && player.level().isDay()
                && !player.isInWaterRainOrBubble()
                && player.level().canSeeSky(BlockPos.containing(player.getX(), player.getEyeY(), player.getZ()))) {
            activeIdentity.igniteForSeconds(8.0F);
            player.igniteForSeconds(8.0F);
        }
        identity2$syncFireStateFromIdentity(player, activeIdentity);

        if (activeIdentity instanceof AbstractPiglin piglinIdentity) {
            identity2$tickMorphZombification(player, piglinIdentity, EntityType.ZOMBIFIED_PIGLIN);
        } else if (activeIdentity instanceof Hoglin hoglinIdentity) {
            identity2$tickMorphZombification(player, hoglinIdentity, EntityType.ZOGLIN);
        } else {
            identity2$clearMorphZombificationTicks();
        }
        // Strider floating runs after travel in LivingEntityMixin so both the
        // client-authoritative player and server use vanilla's lava behavior.
    }

    @Unique
    private void identity2$tickMorphZombification(Player player, Entity identity, EntityType<?> convertedType) {
        if (!(player instanceof ServerPlayer serverPlayer) || identity == null || convertedType == null) {
            return;
        }
        CompoundTag nbt = ((NbtComponentAccessor) this.getCustomData()).getNbt();
        boolean shouldConvert = !player.level().dimensionType().piglinSafe();
        Object immune = identity2$invokeNoArg(identity, "isImmuneToZombification");
        if (immune instanceof Boolean immuneToZombification && immuneToZombification) {
            shouldConvert = false;
        }
        if (!shouldConvert) {
            nbt.putInt(IDENTITY2_ZOMBIFICATION_TICKS_KEY, 0);
            return;
        }

        int conversionTicks = Math.max(0, net.Gabou.identity2.util.NbtCompat.getIntOr(nbt, IDENTITY2_ZOMBIFICATION_TICKS_KEY, 0)) + 1;
        nbt.putInt(IDENTITY2_ZOMBIFICATION_TICKS_KEY, conversionTicks);
        if (conversionTicks < 300) {
            return;
        }

        String convertedId = EntityType.getKey(convertedType).toString();
        nbt.putInt(IDENTITY2_ZOMBIFICATION_TICKS_KEY, 0);
        nbt.putString(IdentityProgression.SELECTED_IDENTITY_TYPE_KEY, convertedId);
        nbt.putString("model_override", convertedId);
        nbt.putString(IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, "");
        IdentityProgression.restoreMorphFromSavedDataAndSync(serverPlayer);
    }

    @Unique
    private void identity2$clearMorphZombificationTicks() {
        CompoundTag nbt = ((NbtComponentAccessor) this.getCustomData()).getNbt();
        if (net.Gabou.identity2.util.NbtCompat.getIntOr(nbt, IDENTITY2_ZOMBIFICATION_TICKS_KEY, 0) != 0) {
            nbt.putInt(IDENTITY2_ZOMBIFICATION_TICKS_KEY, 0);
        }
    }

    @Unique
    private static void identity2$syncFireStateFromIdentity(Player player, Entity identity) {
        if (player == null || identity == null) {
            return;
        }
        int remainingFireTicks = Math.max(0, identity.getRemainingFireTicks());
        player.setRemainingFireTicks(remainingFireTicks);
        player.setSharedFlagOnFire(identity.isOnFire() && remainingFireTicks > 0);
        if (remainingFireTicks <= 0) {
            player.clearFire();
        }
    }

    @Redirect(method = "move",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(DDD)V"))
    private void moveOnEntityLandWallOverride(Entity entity, double x, double y, double z, @Local(ordinal = 0) boolean bl, @Local(ordinal = 1) boolean bl2, @Local(ordinal = 2) Vec3 vec3d4) {
        CompoundTag nbt = ((NbtComponentAccessor) this.getCustomData()).getNbt();
        double multiplier = net.Gabou.identity2.util.NbtCompat.getDoubleOr(nbt, "horizontal_collision_speed_multiplier_override", Double.NaN);
        if (this.currentIdentity != null &&!Double.isNaN(multiplier) && multiplier != 0.0D) {
            Vec3 baseDelta = entity.getDeltaMovement();
            boolean collidedX = x == 0.0D && baseDelta.x != 0.0D;
            boolean collidedZ = z == 0.0D && baseDelta.z != 0.0D;
            entity.setDeltaMovement(collidedX ? baseDelta.x * multiplier : baseDelta.x, baseDelta.y, collidedZ ? baseDelta.z * multiplier : baseDelta.z);
            return;
        }
        entity.setDeltaMovement(x, y, z);
    }


    public int abilityCooldown = 0;
    public int secondaryAbilityCooldown = 0;

    public int getAbilityCooldown() {
        return this.abilityCooldown;
    }

    public void setAbilityCooldown(int cooldown) {
        this.abilityCooldown = cooldown;
    }

    public int getSecondaryAbilityCooldown() {
        return this.secondaryAbilityCooldown;
    }

    public void setSecondaryAbilityCooldown(int cooldown) {
        this.secondaryAbilityCooldown = cooldown;
    }


    @Inject(method = "baseTick", at = @At("HEAD"), cancellable = true)
    private void identity2$baseTick(CallbackInfo info) {
        if (this.abilityCooldown > 0) {
            this.abilityCooldown -= 1;
        }
        if (this.secondaryAbilityCooldown > 0) {
            this.secondaryAbilityCooldown -= 1;
        }
        if ((Entity) (Object) this instanceof ServerPlayer serverPlayer) {
            SilverfishBurrowManager.serverTick(serverPlayer);
            IdentityProgression.tickDailyRandomMorph(serverPlayer);
            identity2$tickMorphFear(serverPlayer);
        }
        identity2$tickGiantDaylightBurn();
    }

    @Unique
    private void identity2$tickMorphFear(ServerPlayer player) {
        if (player == null
                || player.level().isClientSide()
                || (player.tickCount & 7) != 0) {
            return;
        }

        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity == null) {
            return;
        }

        EntityType<?> identityType = identity.getType();
        if (!identity2$isFearIdentity(identityType)) {
            return;
        }

        AABB search = player.getBoundingBox().inflate(12.0D);

        for (Mob mob : player.level().getEntitiesOfClass(
                Mob.class,
                search,
                mob -> mob.isAlive()
                        && identity2$shouldMobFearIdentity(
                        mob.getType(),
                        identityType
                )
        )) {
            Vec3 away = mob.position().subtract(player.position());

            if (away.horizontalDistanceSqr() < 1.0E-4D) {
                away = Vec3.directionFromRotation(0.0F, mob.getYRot());
            }

            Vec3 horizontalAway = new Vec3(
                    away.x,
                    0.0D,
                    away.z
            );

            if (horizontalAway.lengthSqr() < 1.0E-6D) {
                continue;
            }

            Vec3 target = mob.position()
                    .add(horizontalAway.normalize().scale(8.0D));

            mob.setTarget(null);

            if (mob instanceof Creeper creeper) {
                CreeperAccessor accessor = (CreeperAccessor) creeper;

                creeper.setSwellDir(-1);
                accessor.identity2$setOldSwell(0);
                accessor.identity2$setSwell(0);
            }

            mob.getNavigation().moveTo(
                    target.x,
                    mob.getY(),
                    target.z,
                    1.35D
            );
        }
    }

    @Unique
    private static boolean identity2$isFearIdentity(EntityType<?> identityType) {
        return identity2$isVillagerFearIdentity(identityType)
                || identityType == EntityType.WOLF
                || identityType == EntityType.CAT
                || identityType == EntityType.OCELOT;
    }

    @Unique
    private static boolean identity2$shouldMobFearIdentity(EntityType<?> mobType, EntityType<?> identityType) {
        if (IdentitySettings.villagersRunFromIdentities
                && (mobType == EntityType.VILLAGER || mobType == EntityType.WANDERING_TRADER)
                && identity2$isVillagerFearIdentity(identityType)) {
            return true;
        }
        if (identityType == EntityType.WOLF
                && (mobType == EntityType.SKELETON || mobType == EntityType.STRAY || mobType == EntityType.WITHER_SKELETON)) {
            return true;
        }
        return (identityType == EntityType.CAT || identityType == EntityType.OCELOT) && mobType == EntityType.CREEPER;
    }

    @Unique
    private static boolean identity2$isVillagerFearIdentity(EntityType<?> type) {
        return type == EntityType.ZOMBIE
                || type == EntityType.HUSK
                || type == EntityType.DROWNED
                || type == EntityType.ZOMBIFIED_PIGLIN
                || type == EntityType.ZOMBIE_VILLAGER
                || type == EntityType.GIANT;
    }

    @Unique
    private void identity2$tickGiantDaylightBurn() {
        Entity self = (Entity) (Object) this;
        if (self.getType() != EntityType.GIANT || self.level().isClientSide() || !self.isAlive() || self.isInWaterRainOrBubble()) {
            return;
        }
        if (self.level().isDay() && self.level().canSeeSky(BlockPos.containing(self.getX(), self.getEyeY(), self.getZ()))) {
            self.igniteForSeconds(8.0F);
        }
    }


    @Shadow
    public boolean noPhysics = false;
    @Shadow
    public boolean horizontalCollision;

    public boolean entityCanFly = false;
    public boolean entityCanFlyEvaluated = false;
    public boolean entityCanFlyTickEvaluated = false;
    private boolean identity2$grantedMayfly = false;
    private float identity2$storedFlyingSpeed = Float.NaN;
    private boolean identity2$overrodeFlyingSpeed = false;
    private long entityCanFlyLastEvalTick = Long.MIN_VALUE;
    private static final long ENTITY_FLY_REEVAL_TICKS = 20L;
    private static final String IDENTITY2_ZOMBIFICATION_TICKS_KEY = "identity2.zombification_ticks";
    private static final String FALL_METHOD_NAME = identity2$resolveFallMethodName();


    public boolean canFly() {
        if (!this.entityCanFlyEvaluated) {
            Boolean taggedFlight = IdentityTraitTags.resolveFlight(((Entity) (Object) this).getType());
            if (taggedFlight != null) {
                this.entityCanFly = taggedFlight;
            } else {
                try {
                    this.entityCanFly = net.Gabou.identity2.util.MFCheck.isMethodEmpty(((Object) this).getClass(), FALL_METHOD_NAME);
                } catch (Exception ignored) {
                }
                if (!this.shouldTickBlockCollision()) {
                    this.entityCanFly = true;
                }
                if (this.noPhysics) {
                    this.entityCanFly = true;
                }
            }
            this.entityCanFlyEvaluated = true;
            if (this.identityOf instanceof Player player) {
                ((EntityMixin) (Object) player).applyIdentityFlightGrant(player, this.entityCanFly);
            }
        }
        return this.entityCanFly;
    }

    private static String identity2$resolveFallMethodName() {
        try {
            return EntityMethodChecks.class
                    .getDeclaredMethod("checkFallDamage", double.class, boolean.class, BlockState.class, BlockPos.class)
                    .getName();
        } catch (NoSuchMethodException ignored) {
            return "checkFallDamage";
        }
    }

    private void applyIdentityFlightGrant(Player player, boolean identityCanFly) {
        if (player.isSpectator()) {
            identity2$restoreIdentityFlyingSpeed(player);
            this.identity2$grantedMayfly = false;
            return;
        }

        boolean canGrantFlight = identityCanFly
                && IdentitySettings.enableFlight
                && (!(player instanceof ServerPlayer serverPlayer) || IdentityProgression.canGrantIdentityFlight(serverPlayer));

        if (player.getAbilities().instabuild) {
            if (canGrantFlight && IdentitySettings.overrideCreativeFlySpeed && identity2$isStrictFlyingIdentity(this.currentIdentity)) {
                identity2$applyConfiguredFlyingSpeed(player);
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.onUpdateAbilities();
                }
            } else {
                identity2$restoreIdentityFlyingSpeed(player);
            }
            this.identity2$grantedMayfly = false;
            return;
        }

        if (canGrantFlight) {
            boolean abilitiesChanged = false;
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                abilitiesChanged = true;
            }
            if (identity2$applyConfiguredFlyingSpeed(player)) {
                abilitiesChanged = true;
            }
            identity2$landFlyingIdentityIfRequested(player);
            if (abilitiesChanged && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.onUpdateAbilities();
            }
            this.identity2$grantedMayfly = true;
            return;
        }

        if (this.identity2$grantedMayfly) {
            player.getAbilities().mayfly = false;
            if (player.getAbilities().flying) {
                player.getAbilities().flying = false;
            }
            identity2$restoreIdentityFlyingSpeed(player);
            this.identity2$grantedMayfly = false;
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.onUpdateAbilities();
            }
        } else {
            identity2$restoreIdentityFlyingSpeed(player);
        }
    }

    @Unique
    private boolean identity2$applyConfiguredFlyingSpeed(Player player) {
        if (player == null) {
            return false;
        }
        if (Float.isNaN(this.identity2$storedFlyingSpeed)) {
            float currentSpeed = ((AbilitiesAccessor) player.getAbilities()).identity2$getFlyingSpeed();
            this.identity2$storedFlyingSpeed = Math.abs(currentSpeed - IdentitySettings.flySpeed) < 1.0E-5F ? 0.05F : currentSpeed;
        }
        float configuredFlyingSpeed = Math.max(0.0F, IdentitySettings.flySpeed);
        if (((AbilitiesAccessor) player.getAbilities()).identity2$getFlyingSpeed() == configuredFlyingSpeed) {
            this.identity2$overrodeFlyingSpeed = true;
            return false;
        }
        ((AbilitiesAccessor) player.getAbilities()).identity2$setFlyingSpeed(configuredFlyingSpeed);
        this.identity2$overrodeFlyingSpeed = true;
        return true;
    }

    @Unique
    private void identity2$restoreIdentityFlyingSpeed(Player player) {
        if (player == null || !this.identity2$overrodeFlyingSpeed) {
            return;
        }
        if (!Float.isNaN(this.identity2$storedFlyingSpeed)) {
            ((AbilitiesAccessor) player.getAbilities()).identity2$setFlyingSpeed(this.identity2$storedFlyingSpeed);
        } else {
            ((AbilitiesAccessor) player.getAbilities()).identity2$setFlyingSpeed(0.05F);
        }
        this.identity2$storedFlyingSpeed = Float.NaN;
        this.identity2$overrodeFlyingSpeed = false;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.onUpdateAbilities();
        }
    }

    @Unique
    private void identity2$landFlyingIdentityIfRequested(Player player) {
        if (player == null || !player.isShiftKeyDown() || !player.getAbilities().flying) {
            return;
        }
        if (!player.onGround() && !identity2$isCloseToLandingSurface(player)) {
            return;
        }
        player.getAbilities().flying = false;
        player.setDeltaMovement(player.getDeltaMovement().x, Math.min(0.0D, player.getDeltaMovement().y), player.getDeltaMovement().z);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.onUpdateAbilities();
        }
    }

    /**
     * Returns true when a slow falling morph should accelerate downward.
     *
     * @param entity the entity using the morph
     * @return true if crouching and currently moving downward
     */
    @Unique
    private static boolean identity2$shouldSlowFallingFastFall(Entity entity) {
        return entity.isShiftKeyDown() && entity.getDeltaMovement().y < 0.0D;
    }

    @Unique
    private static boolean identity2$isCloseToLandingSurface(Player player) {
        if (player == null || player.level() == null) {
            return false;
        }
        for (double distance = 0.05D; distance <= 1.25D; distance += 0.2D) {
            if (!player.level().noCollision(player, player.getBoundingBox().move(0.0D, -distance, 0.0D))) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static boolean identity2$isStrictFlyingIdentity(@Nullable Entity identity) {
        return identity != null && Boolean.TRUE.equals(IdentityTraitTags.resolveFlight(identity.getType()));
    }

    @Unique
    private void identity2$maintainIdentityFlight(Player player) {
        Entity activeIdentity = this.currentIdentity;
        if (activeIdentity == null) {
            return;
        }
        this.applyIdentityFlightGrant(player, ((EntityAccessor) activeIdentity).canFly());
    }

    @Inject(method = "isControlledByLocalInstance", at = @At("HEAD"), cancellable = true)
    private void isControlledByPlayerOverride(CallbackInfoReturnable info) {
        if (this.identityOf != null) {
            info.setReturnValue(this.identityOf.isControlledByLocalInstance());
        }
    }

    @Inject(method = "getBbWidth", at = @At("HEAD"), cancellable = true)
    private void getWidthOverride(CallbackInfoReturnable info) {
        CompoundTag nbt = this.getCustomData();
        double override = net.Gabou.identity2.util.NbtCompat.getDoubleOr(nbt, "width_override", 0.0D);
        if (override > 0.0D) {
            info.setReturnValue((float) override);
        }
    }

    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void getDimensionsModification(CallbackInfoReturnable info) {
        EntityDimensions dimensions = (EntityDimensions) info.getReturnValue();
        float oldWidth = dimensions.width();
        float oldHeight = dimensions.height();
        float widthOverride = oldWidth;
        float heightOverride = oldHeight;

        CompoundTag nbt = this.getCustomData();
        double widthValue = net.Gabou.identity2.util.NbtCompat.getDoubleOr(nbt, "width_override", 0.0D);
        if (widthValue > 0.0D) {
            widthOverride = (float) widthValue;
        }
        double heightValue = net.Gabou.identity2.util.NbtCompat.getDoubleOr(nbt, "height_override", 0.0D);
        if (heightValue > 0.0D) {
            heightOverride = (float) heightValue;
        }

        float widthScale = oldWidth > 0.0F ? widthOverride / oldWidth : 1.0F;
        float heightScale = oldHeight > 0.0F ? heightOverride / oldHeight : 1.0F;
        info.setReturnValue(dimensions.scale(widthScale, heightScale));
    }

    @Inject(method = "getBbHeight", at = @At("HEAD"), cancellable = true)
    private void getHeightOverride(CallbackInfoReturnable info) {
        CompoundTag nbt = this.getCustomData();
        double override = net.Gabou.identity2.util.NbtCompat.getDoubleOr(nbt, "height_override", 0.0D);
        if (override > 0.0D) {
            info.setReturnValue((float) override);
        }
    }

    @Shadow
    private AABB bb;

    @Inject(method = "setBoundingBox", at = @At("TAIL"))
    private void getBoundingBoxModification(CallbackInfo info) {
        AABB box = this.bb;
        double old_width = box.maxX - box.minX;
        double old_height = box.maxY - box.minY;
        double center_x = (box.maxX + box.minX) / 2;
        double center_z = (box.maxZ + box.minZ) / 2;
        double center_y = box.minY;
        double new_width = old_width;
        double new_height = old_height;
        boolean hasOverride = false;

        CompoundTag nbt = this.getCustomData();
        double widthOverride = net.Gabou.identity2.util.NbtCompat.getDoubleOr(nbt, "width_override", 0.0D);
        if (widthOverride > 0.0D) {
            new_width = widthOverride;
            hasOverride = true;
        }
        double heightOverride = net.Gabou.identity2.util.NbtCompat.getDoubleOr(nbt, "height_override", 0.0D);
        if (heightOverride > 0.0D) {
            new_height = heightOverride;
            hasOverride = true;
        }
        if (!hasOverride) {
            return;
        }
        box = box.setMaxX(center_x + new_width / 2);
        box = box.setMinX(center_x - new_width / 2);
        box = box.setMaxZ(center_z + new_width / 2);
        box = box.setMinZ(center_z - new_width / 2);
        box = box.setMaxY(center_y + new_height);
        this.bb = box;
        //info.setReturnValue(box);
    }

    @Override
    public CompoundTag getCustomData() {
        if (this.persistentData == null) {
            this.persistentData = new CompoundTag();
        }
        return this.persistentData;
    }

    @Nullable
    public Entity currentIdentity = null;
    @Nullable
    public Entity identityOf = null;

    @Nullable
    public Entity getCurrentIdentity() {
        return this.currentIdentity;
    }

    @Nullable
    public Entity getIdentityOwner() {
        return this.identityOf;
    }

    public void setCurrentIdentity(Entity e) {
        this.currentIdentity = e;
    }

    public void setIdentityOf(Entity e) {
        this.identityOf = e;
    }

    public void setCurrentIdentity(String id, CompoundTag data) {
        if (data != null && !data.isEmpty()) {
            this.setCurrentIdentity(id + data);
            return;
        }
        this.setCurrentIdentity(id);
    }

    public void fixAttributes(Entity entity, Entity identity) {
    }

    public void setCurrentIdentity(String id) {
        this.noPhysics = false;
        this.entityCanFlyEvaluated = false;
        this.entityCanFlyTickEvaluated = false;
        this.entityCanFlyLastEvalTick = Long.MIN_VALUE;
        this.identity2$clearTransientMovementOverrides();
        if (this.currentIdentity != null) {
            this.currentIdentity.discard();
            this.currentIdentity = null;
        }
        if ((Entity) (Object) this instanceof ServerPlayer serverPlayer) {
            SilverfishBurrowManager.stop(serverPlayer, true);
        }
        ResourceLocation forcedIdentity = null;
        if ((Entity) (Object) this instanceof Player player) {
            IdentityProgression.updateHostileIdentityGrace(player instanceof ServerPlayer serverPlayer ? serverPlayer : null, null);
            forcedIdentity = IdentityProgression.getForcedIdentity();
            if (forcedIdentity != null) {
                id = forcedIdentity.toString();
            }
        }
        CompoundTag nbtCompound = null;
        if (id.contains("{")) {
            try {
                nbtCompound = net.minecraft.commands.arguments.CompoundTagArgument.compoundTag().parse(new com.mojang.brigadier.StringReader(id.substring(id.indexOf('{'))));
                id = id.substring(0, id.indexOf('{'));
            } catch (Exception e) {
                id = id.substring(0, id.indexOf('{'));
            }
        }
        if (nbtCompound == null) {
            nbtCompound = new CompoundTag().copy();
        }
        if (nbtCompound.isEmpty() && forcedIdentity == null) {
            String variantRaw = net.Gabou.identity2.util.NbtCompat.getStringOr(this.getCustomData(), IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, "");
            if (!variantRaw.isBlank()) {
                try {
                    nbtCompound = net.minecraft.commands.arguments.CompoundTagArgument.compoundTag()
                            .parse(new com.mojang.brigadier.StringReader(variantRaw));
                } catch (Exception ignored) {
                    nbtCompound = new CompoundTag().copy();
                }
            }
        }
        if (id.length() == 0) {
            this.currentIdentity = null;
            this.entityCanFly = false;
            ((Entity) (Object) this).refreshDimensions();
            this.setStandingEyeHeight(((Entity) (Object) this).getEyeHeight());
            if ((Entity) (Object) this instanceof Player player) {
                this.applyIdentityFlightGrant(player, false);
            }
            return;
        }
        ResourceLocation identityId;
        try {
            identityId = ResourceLocation.parse(id);
        } catch (Exception e) {
            this.deactivateIdentityAfterFailure(null, "invalid id");
            return;
        }
        if (IdentityProgression.PLAYER_IDENTITY_ID.equals(identityId)) {
            this.currentIdentity = null;
            this.entityCanFly = false;
            ((Entity) (Object) this).refreshDimensions();
            this.setStandingEyeHeight(((Entity) (Object) this).getEyeHeight());
            if ((Entity) (Object) this instanceof Player player) {
                this.applyIdentityFlightGrant(player, false);
            }
            return;
        }
        if (IdentityProgression.isIdentityTemporarilyDisabled(identityId)) {
            this.deactivateIdentityAfterFailure(identityId, IdentityProgression.getDisabledIdentityReason(identityId));
            return;
        }
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(identityId)) {
            this.deactivateIdentityAfterFailure(identityId, "entity type missing");
            return;
        }

        nbtCompound.putString("id", identityId.toString());
        Vec3 pos = new Vec3(0, 0, 0);
        try {
            Level serverWorld = ((Entity) (Object) this).level();
            Entity entity = EntityType.loadEntityRecursive(nbtCompound, serverWorld, entityx -> {
                entityx.moveTo(pos.x, pos.y, pos.z, entityx.getYRot(), entityx.getXRot());
                return entityx;
            });
            if (entity == null) {
                throw new IllegalStateException("loadEntityWithPassengers returned null");
            }
            entity.setId(-this.getId());
            this.identity2$applyIdentityVariantState(entity, nbtCompound);
            ((EntityAccessor) entity).fixAttributes((Entity) (Object) this, entity);
            this.currentIdentity = entity;
            ((EntityAccessor) this.currentIdentity).setIdentityOf((Entity) (Object) this);
            this.identity2$resetNewIdentityAnimationState(this.currentIdentity);
        } catch (Throwable throwable) {
            String reason = throwable.getClass().getSimpleName();
            IdentityProgression.disableIdentity(identityId, reason);
            Identity2.LOGGER.error("Failed to load identity {}. It has been disabled for this runtime.", identityId, throwable);
            this.deactivateIdentityAfterFailure(identityId, reason);
            return;
        }

        if (this.currentIdentity != null) {
            ((EntityAccessor) this.currentIdentity).setIdentityOf((Entity) (Object) this);
            ((Entity) (Object) this).refreshDimensions();
            this.setStandingEyeHeight(this.currentIdentity.getEyeHeight());
            if ((Entity) (Object) this instanceof Player player) {
                Entity playerIdentity = ((EntityAccessor) player).getCurrentIdentity();
                if (player instanceof ServerPlayer serverPlayer) {
                    IdentityProgression.updateHostileIdentityGrace(serverPlayer, this.currentIdentity);
                }
                this.applyIdentityFlightGrant(player, playerIdentity != null && ((EntityAccessor) playerIdentity).canFly());
            }
        }

    }

    @Unique
    private void identity2$resetNewIdentityAnimationState(Entity identity) {
        if (identity == null) {
            return;
        }
        if (identity instanceof LivingEntity livingIdentity) {
            livingIdentity.swinging = false;
            livingIdentity.swingTime = 0;
            livingIdentity.attackAnim = 0.0F;
            livingIdentity.oAttackAnim = 0.0F;
            livingIdentity.yBodyRot = ((Entity) (Object) this).getYRot();
            livingIdentity.yBodyRotO = ((Entity) (Object) this).getYRot();
            livingIdentity.yHeadRot = ((Entity) (Object) this).getYRot();
            livingIdentity.yHeadRotO = ((Entity) (Object) this).getYRot();
        }
        if (identity instanceof IronGolem ironGolem) {
            ((IronGolemAccessor) ironGolem).identity2$setAttackAnimationTick(0);
        } else if (identity instanceof Hoglin hoglin) {
            ((HoglinAccessor) hoglin).identity2$setAttackAnimationRemainingTicks(0);
        } else if (identity instanceof Zoglin zoglin) {
            ((ZoglinAccessor) zoglin).identity2$setAttackAnimationRemainingTicks(0);
        } else if (identity instanceof Ravager ravager) {
            ((RavagerAccessor) ravager).identity2$setAttackTick(0);
        } else if (identity instanceof EnderMan enderMan) {
            enderMan.getEntityData().set(EnderManAccessor.identity2$getDataCreepy(), false);
            enderMan.getEntityData().set(EnderManAccessor.identity2$getDataStaredAt(), false);
        } else if (identity instanceof Warden warden) {
            warden.attackAnimationState.stop();
            warden.sonicBoomAnimationState.stop();
        }
    }

    private void identity2$applyIdentityVariantState(Entity identityEntity, CompoundTag variantNbt) {
        if (identityEntity == null || variantNbt == null || variantNbt.isEmpty()) {
            return;
        }
        IdentityVariantNbtHelper.applyVariantData(identityEntity, variantNbt);
        IdentityVanillaVariantHelper.applyVariantData(identityEntity, variantNbt);
        IdentityApi.applyVariantData(identityEntity, variantNbt);
    }

    private static Object identity2$invokeOneArg(Object target, String methodName, Object arg) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Method method : identity2$getAllMethods(target.getClass())) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> paramType = method.getParameterTypes()[0];
            if (arg != null && !identity2$isAssignable(paramType, arg.getClass())) {
                continue;
            }
            try {
                if (!method.canAccess(target)) {
                    method.setAccessible(true);
                }
                Object result = method.invoke(target, arg);
                return result == null ? target : result;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object identity2$invokeTwoArgs(Object target, String methodName, Object firstArg, Object secondArg) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Method method : identity2$getAllMethods(target.getClass())) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (firstArg != null && !identity2$isAssignable(params[0], firstArg.getClass())) {
                continue;
            }
            if (secondArg != null && !identity2$isAssignable(params[1], secondArg.getClass())) {
                continue;
            }
            try {
                if (!method.canAccess(target)) {
                    method.setAccessible(true);
                }
                Object result = method.invoke(target, firstArg, secondArg);
                return result == null ? target : result;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object identity2$invokeIntArg(Object target, String methodName, int value) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Method method : identity2$getAllMethods(target.getClass())) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> paramType = method.getParameterTypes()[0];
            if (!(paramType == int.class || paramType == Integer.class)) {
                continue;
            }
            try {
                if (!method.canAccess(target)) {
                    method.setAccessible(true);
                }
                Object result = method.invoke(target, value);
                return result == null ? target : result;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static List<Method> identity2$getAllMethods(Class<?> type) {
        List<Method> methods = Lists.newArrayList();
        Set<String> signatures = new java.util.LinkedHashSet<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                String signature = method.getName() + "#" + method.getParameterCount();
                for (Class<?> parameterType : method.getParameterTypes()) {
                    signature += ":" + parameterType.getName();
                }
                if (signatures.add(signature)) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    private static boolean identity2$isAssignable(Class<?> paramType, Class<?> argType) {
        if (paramType.isAssignableFrom(argType)) {
            return true;
        }
        if (paramType == int.class && argType == Integer.class) {
            return true;
        }
        if (paramType == boolean.class && argType == Boolean.class) {
            return true;
        }
        if (paramType == byte.class && argType == Byte.class) {
            return true;
        }
        if (paramType == short.class && argType == Short.class) {
            return true;
        }
        if (paramType == long.class && argType == Long.class) {
            return true;
        }
        if (paramType == float.class && argType == Float.class) {
            return true;
        }
        return paramType == double.class && argType == Double.class;
    }

    private void deactivateIdentityAfterFailure(@Nullable ResourceLocation identityId, String reason) {
        this.currentIdentity = null;
        this.entityCanFly = false;
        this.entityCanFlyEvaluated = false;
        this.entityCanFlyTickEvaluated = false;
        this.entityCanFlyLastEvalTick = Long.MIN_VALUE;
        this.identity2$clearTransientMovementOverrides();
        CompoundTag nbt = this.getCustomData();
        nbt.putString("model_override", "");
        nbt.putString(IdentityProgression.SELECTED_IDENTITY_TYPE_KEY, "");
        nbt.putString(IdentityProgression.SELECTED_IDENTITY_VARIANT_KEY, "");
        nbt.putString(IdentityProgression.PREVIOUS_IDENTITY_TYPE_KEY, "");
        nbt.putString(IdentityProgression.PREVIOUS_IDENTITY_VARIANT_KEY, "");
        nbt.putDouble("width_override", 0.0);
        nbt.putDouble("height_override", 0.0);
        nbt.putDouble(IdentityProgression.MORPH_DAMAGE_GRACE_END_TICK_KEY, 0.0D);
        nbt.putDouble(IdentityProgression.TRANSITION_START_TICK_KEY, 0.0D);
        nbt.putDouble(IdentityProgression.TRANSITION_DURATION_TICKS_KEY, 0.0D);

        if ((Entity) (Object) this instanceof Player player) {
            if (player instanceof ServerPlayer serverPlayer) {
                IdentityProgression.updateHostileIdentityGrace(serverPlayer, null);
            }
            this.applyIdentityFlightGrant(player, false);
            ((Entity) (Object) this).refreshDimensions();
            this.setStandingEyeHeight(((Entity) (Object) this).getEyeHeight());
            if (player instanceof ServerPlayer serverPlayer && identityId != null) {
                serverPlayer.displayClientMessage(
                        Component.literal(
                                "Identity disabled after load failure: " + identityId + (reason == null || reason.isBlank() ? "" : " (" + reason + ")")
                        ),
                        false
                );
            }
        }
    }
    @Unique
    private void identity2$clearTransientMovementOverrides() {
        CompoundTag nbt = this.getCustomData();
        nbt.putDouble("land_speed_multiplier_override", 0.0D);
        nbt.putDouble("horizontal_collision_speed_multiplier_override", 0.0D);
        this.identity2$storedFlyingSpeed = Float.NaN;
        this.identity2$overrodeFlyingSpeed = false;
    }

    @Unique
    private void identity2$applyWardenEffects(Entity host) {
        if (!(host instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (this.currentIdentity == null || this.currentIdentity.getType() != EntityType.WARDEN) {
            return;
        }

        if (IdentitySettings.wardenIsBlinded) {
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, true));
        }

        if (!IdentitySettings.wardenBlindsNearby || !(serverPlayer.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        AABB nearby = serverPlayer.getBoundingBox().inflate(24.0D);
        for (ServerPlayer target : serverLevel.getEntitiesOfClass(
                ServerPlayer.class,
                nearby,
                target -> target != serverPlayer && !target.isSpectator()
        )) {
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, true));
        }
    }

    @Shadow
    protected boolean wasTouchingWater;
    @Shadow
    @Nullable
    private Entity vehicle;

    public void setVehicle(Entity vehicle) {
        this.vehicle = vehicle;
    }

    public void setTouchingWater(boolean isTouchingWater) {
        this.wasTouchingWater = isTouchingWater;
    }

    @Shadow
    public double xo;
    @Shadow
    public double yo;
    @Shadow
    public double zo;
    @Shadow
    public double xOld;
    @Shadow
    public double yOld;
    @Shadow
    public double zOld;

    @Unique
    private void identity2$setOldPos(Vec3 pos) {
        this.xo = this.xOld = pos.x;
        this.yo = this.yOld = pos.y;
        this.zo = this.zOld = pos.z;
    }

    @Override
    public void setLastPosition(Vec3 pos) {
        this.identity2$setOldPos(pos);
    }

    @Shadow
    public void processFlappingMovement() {
    }

    public void runAddAirTravelEffects() {
        this.processFlappingMovement();
    }

    @Shadow
    EntityDimensions dimensions;
    @Shadow
    float eyeHeight;

    public EntityDimensions getEntityDimensions() {
        return this.dimensions;
    }

    public void setEntityDimensions(EntityDimensions dimensions) {
        this.dimensions = dimensions;
        ((Entity) (Object) this).refreshDimensions();
    }

    @Shadow
    public float getEyeHeight() {
        return this.eyeHeight;
    }

    @Override
    public float getStandingEyeHeight() {
        return this.getEyeHeight();
    }

    public void setStandingEyeHeight(float standingEyeHeight) {
        this.eyeHeight = standingEyeHeight;
    }


    //Tons of Redirects - Begin!
    @Shadow
    public Entity.MovementEmission getMovementEmission() {
        return null;
    }

    @Override
    public Entity.MovementEmission getMoveEffect() {
        return this.getMovementEmission();
    }

    @Inject(method = "getMovementEmission()Lnet/minecraft/world/entity/Entity$MovementEmission;", at = @At("HEAD"), cancellable = true)
    private void getMoveEffectIdentity(CallbackInfoReturnable info) {
        if (this.currentIdentity != null) {
            info.setReturnValue(((EntityAccessor) this.currentIdentity).getMoveEffect());
        }
    }


    @Inject(method = "handleDamageEvent(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"), cancellable = true)
    private void onDamagedActual(DamageSource source, CallbackInfo info) {
        if (this.currentIdentity != null && !(((Entity) (Object) this) instanceof Player) && !this.currentIdentity.isRemoved()) {
            this.currentIdentity.handleDamageEvent(source);
        }
    }

    @Inject(method = "isRemoved()Z", at = @At("HEAD"), cancellable = true)
    private void isRemovedActual(CallbackInfoReturnable info) {
        if (this.identityOf != null) {
            info.setReturnValue(false);
        }
    }


    @Shadow
    public boolean isFlapping() {
        return false;
    }

    @Override
    public boolean isFlappingWings() {
        return this.isFlapping();
    }

    @Inject(method = "isFlapping()Z", at = @At("HEAD"), cancellable = true)
    private void isFlappingWingsIdentity(CallbackInfoReturnable info) {
        if (this.currentIdentity != null) {
            info.setReturnValue(((EntityAccessor) this.currentIdentity).isFlappingWings());
        }
    }

    @Override
    public boolean shouldTickBlockCollision() {
        if (((Entity) (Object) this) instanceof Player) {
            return true;
        }
        if (this.currentIdentity != null) {
            return ((EntityAccessor) this.currentIdentity).shouldTickBlockCollision();
        }
        return true;
    }


    @Override
    public double getIdentityGravity() {
        return 0.08D;
    }


    @Inject(method = "getSoundSource()Lnet/minecraft/sounds/SoundSource;", at = @At("HEAD"), cancellable = true)
    private void getSoundCategoryIdentity(CallbackInfoReturnable info) {
        if (this.currentIdentity != null) {
            info.setReturnValue(this.currentIdentity.getSoundSource());
        }
    }

    @Inject(method = "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V", at = @At("HEAD"), cancellable = true)
    private void identity2$cancelOwnedIdentityAmbientSound(SoundEvent sound, float volume, float pitch, CallbackInfo info) {
        Entity self = (Entity) (Object) this;

        if (this.identityOf == null || sound == null) {
            return;
        }

        if (!(self instanceof Mob mob)) {
            return;
        }

        SoundEvent ambientSound = ((MobAccessor) mob).identity2$invokeGetAmbientSound();

        if (ambientSound != null && ambientSound.equals(sound)) {
            info.cancel();
        }
    }

    @Inject(method = "lerpTo(DDDFFI)V", at = @At("HEAD"))
    private void identity2$forwardLerpTo(
            double x,
            double y,
            double z,
            float yRot,
            float xRot,
            int interpolationSteps,
            CallbackInfo info
    ) {
        if (this.currentIdentity != null) {
            this.currentIdentity.lerpTo(x, y, z, yRot, xRot, interpolationSteps);
        }
    }

    @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
    private void identity2$canBeCollidedWith(CallbackInfoReturnable<Boolean> cir) {

        if (this.currentIdentity != null) {
            cir.setReturnValue(this.currentIdentity.canBeCollidedWith());
            return;
        }

        if ((Object) this instanceof Entity self) {
            try {
                if (((EntityAccessor) self).getIdentityOwner() != null) {
                    cir.setReturnValue(false);
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Inject(method = "isColliding(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z", at = @At("HEAD"), cancellable = true)
    private void collidesWithStateAtPosIdentity(BlockPos pos, BlockState state, CallbackInfoReturnable info) {
        if (((Entity) (Object) this) instanceof Player) {
            return;
        }
        if (this.currentIdentity != null) {
            info.setReturnValue(this.currentIdentity.isColliding(pos, state));
        }
    }

    @Inject(method = "canSpawnSprintParticle()Z", at = @At("HEAD"), cancellable = true)
    private void shouldSpawnSprintingParticlesIdentity(CallbackInfoReturnable info) {
        if (this.currentIdentity != null) {
            info.setReturnValue(this.currentIdentity.canSpawnSprintParticle());
        }
    }

    @Inject(method = "canBeHitByProjectile()Z", at = @At("HEAD"), cancellable = true)
    private void canBeHitByProjectileIdentity(CallbackInfoReturnable info) {
        if ((Entity) (Object) this instanceof Player) {
            return;
        }
        if (this.currentIdentity != null) {
            info.setReturnValue(this.currentIdentity.canBeHitByProjectile());
        }
    }

    @Inject(method = "isPickable()Z", at = @At("HEAD"), cancellable = true)
    private void canHitIdentity(CallbackInfoReturnable info) {
        if (this.identityOf != null) {
            info.setReturnValue(false);
            return;
        }
        if (this.currentIdentity != null) {
            info.setReturnValue(this.currentIdentity.isPickable());
        }
    }

    @Inject(method = "isPushable()Z", at = @At("HEAD"), cancellable = true)
    private void isPushableIdentity(CallbackInfoReturnable info) {

        if (this.currentIdentity != null) {
            info.setReturnValue(this.currentIdentity.isPushable());
        }
    }

    @Inject(method = "isPushedByFluid()Z", at = @At("HEAD"), cancellable = true)
    private void isPushedByFluidsIdentity(CallbackInfoReturnable info) {

        if (this.currentIdentity != null) {
            info.setReturnValue(this.currentIdentity.isPushedByFluid());
        }
    }

    @Inject(method = "isInWall()Z", at = @At("HEAD"), cancellable = true)
    private void isInsideWallIdentity(CallbackInfoReturnable info) {
        if (((Entity) (Object) this) instanceof Player) {
            return;
        }
        if (this.currentIdentity != null) {
            info.setReturnValue(this.currentIdentity.isInWall());
        }
    }


    @Inject(method = "interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), cancellable = true)
    private void interactIdentity(Player player, InteractionHand hand, CallbackInfoReturnable info) {
        Entity self = (Entity) (Object) this;
        if (hand == InteractionHand.MAIN_HAND
                && identity2$isRavager(self)
                && identity2$canRideRavager(player)
                && !player.isPassenger()) {
            if (player.level().isClientSide()) {
                info.setReturnValue(InteractionResult.SUCCESS);
                return;
            }
            if (player.startRiding(self)) {
                info.setReturnValue(InteractionResult.CONSUME);
                return;
            }
        }
        if (this.currentIdentity != null) {
            InteractionResult actionResult = this.currentIdentity.interact(player, hand);
            if (actionResult != InteractionResult.PASS) {
                info.setReturnValue(actionResult);
            }
        }
    }

    @Inject(method = "canAddPassenger", at = @At("HEAD"), cancellable = true, require = 0)
    private void identity2$canAddIllagerMorphPassenger(Entity passenger, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!identity2$isRavager(self) || !(passenger instanceof Player player) || !identity2$canRideRavager(player)) {
            return;
        }
        cir.setReturnValue(self.getPassengers().isEmpty());
    }

    @Inject(
            method = "positionRider(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V",
            at = @At("TAIL"),
            require = 0
    )
    private void identity2$adjustMorphedPassengerPosition(Entity passenger, Entity.MoveFunction moveFunction, CallbackInfo ci) {
        if (!(passenger instanceof Player player)) {
            return;
        }
        double offset = identity2$getMorphPassengerYOffset(player, (Entity) (Object) this);
        if (offset <= 0.0D) {
            return;
        }
        passenger.setPos(passenger.getX(), passenger.getY() + offset, passenger.getZ());
    }

    /**
     * Returns the extra Y offset required when a morphed player rides another entity.
     *
     * @param player the morphed player
     * @param vehicle the vehicle being ridden
     * @return the extra vertical offset
     */
    @Unique
    private static double identity2$getMorphPassengerYOffset(Player player, Entity vehicle) {
        Entity identity = ((EntityAccessor) player).getCurrentIdentity();
        if (identity == null) {
            return 0.0D;
        }
        // Player dimensions already match the morph here, so compare against the
        // vanilla standing player height to recover how much a small morph lost.
        double heightDifference = Player.STANDING_DIMENSIONS.height() - identity.getBbHeight();
        if (heightDifference <= 0.0D) {
            return 0.0D;
        }
        return Math.max(0.0D, heightDifference * (vehicle instanceof Boat ? 0.5D : 0.35D));
    }

    @Inject(method = "canCollideWith(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void collidesWithIdentity(Entity other, CallbackInfoReturnable info) {
        if (((EntityAccessor) other).getIdentityOwner() != null) {
            info.setReturnValue(false);
            return;
        }
        if (this.currentIdentity != null) {
            info.setReturnValue(this.currentIdentity.canCollideWith(other));
        }
    }

    @Inject(method = "getMaxAirSupply()I", at = @At("HEAD"), cancellable = true)
    private void getMaxAirIdentity(CallbackInfoReturnable info) {
        if (this.currentIdentity != null) {
            info.setReturnValue(this.currentIdentity.getType() == EntityType.DOLPHIN
                    ? 2400
                    : this.currentIdentity.getMaxAirSupply());
        }
    }

    @Inject(method = "getPercentFrozen()F", at = @At("HEAD"), cancellable = true)
    private void getFreezingScaleIdentity(CallbackInfoReturnable info) {
        if (this.currentIdentity != null) {
            info.setReturnValue(this.currentIdentity.getPercentFrozen());
        }
    }

    @Inject(method = "isAttackable()Z", at = @At("HEAD"), cancellable = true)
    private void isAttackableIdentity(CallbackInfoReturnable info) {
        if ((Entity) (Object) this instanceof Player) {
            return;
        }
        if (this.currentIdentity != null) {
            info.setReturnValue(this.currentIdentity.isAttackable());
        }
    }

    @Inject(method = "isInvulnerable()Z", at = @At("HEAD"), cancellable = true)
    private void isInvulnerableIdentity(CallbackInfoReturnable info) {
        if ((Entity) (Object) this instanceof Player) {
            return;
        }
        if (this.currentIdentity != null) {
            info.setReturnValue(this.currentIdentity.isInvulnerable());
        }
    }

    @Inject(method = "isCustomNameVisible()Z", at = @At("HEAD"), cancellable = true)
    private void isCustomNameVisibleIdentity(CallbackInfoReturnable info) {
        if (this.currentIdentity != null) {
            info.setReturnValue(this.currentIdentity.isCustomNameVisible());
        }
    }

/*@Inject(method = "getEyeHeight(Lnet/minecraft/entity/Entity;)F", at=@At("HEAD"),cancellable=true)
private void getEyeHeightIdentity(EntityPose pose, CallbackInfoReturnable info){
    if(this.currentIdentity!=null){
        info.setReturnValue(this.currentIdentity.getEyeHeight(pose));
    }
}*/

    public boolean saving = false;

    @Inject(method = "saveWithoutId", at = @At("HEAD"), cancellable = true, require = 0)
    public void writeDataLabelSaving(CompoundTag compoundTag, CallbackInfoReturnable<CompoundTag> cir) {
        this.saving = true;
    }

    @Inject(method = "saveWithoutId", at = @At("TAIL"), cancellable = true, require = 0)
    public void writeDataLabelDoneSaving(CompoundTag compoundTag, CallbackInfoReturnable<CompoundTag> cir) {
        this.saving = false;
    }

    @Inject(method = "saveWithoutId", at = @At("TAIL"), require = 0)
    private void identity2$saveCustomData(CompoundTag compoundTag, CallbackInfoReturnable<CompoundTag> cir) {
        identity2$writeCustomData(compoundTag);
    }

    @Inject(method = "load", at = @At("TAIL"), require = 0)
    private void identity2$loadCustomData(CompoundTag compoundTag, CallbackInfo info) {
        identity2$readCustomData(compoundTag);
    }

    private void identity2$writeCustomData(CompoundTag compoundTag) {
        if (compoundTag == null) {
            return;
        }
        CompoundTag customData = this.persistentData;
        if (customData == null || customData.isEmpty()) {
            compoundTag.remove(CUSTOM_DATA_TAG_KEY);
            return;
        }
        compoundTag.put(CUSTOM_DATA_TAG_KEY, customData.copy());
    }

    private void identity2$readCustomData(CompoundTag compoundTag) {
        if (compoundTag != null && compoundTag.contains(CUSTOM_DATA_TAG_KEY, Tag.TAG_COMPOUND)) {
            this.persistentData = compoundTag.getCompound(CUSTOM_DATA_TAG_KEY).copy();
            return;
        }
        this.persistentData = new CompoundTag();
    }


    @Inject(method = "getEyeHeight()F", at = @At("HEAD"), cancellable = true)
    private void getStandingEyeHeightIdentity(CallbackInfoReturnable info) {
        if (this.currentIdentity != null) {
            info.setReturnValue(this.currentIdentity.getEyeHeight());
        }
    }

    // Pose changes (crouch/uncrouch) call refreshDimensions, which resets the cached
    // eyeHeight field from the player's (scaled) dimensions. getEyeY() reads that field
    // directly, so projectile spawn points and bucket raytraces would revert to the
    // player's own head height until the next morph. Restore the identity eye height.
    @Inject(method = "refreshDimensions", at = @At("TAIL"), require = 0)
    private void identity2$restoreMorphEyeHeightAfterRefresh(CallbackInfo info) {
        if (this.currentIdentity != null) {
            this.eyeHeight = this.currentIdentity.getEyeHeight();
        }
    }

    @Inject(method = "getEyeHeight(Lnet/minecraft/world/entity/Pose;)F", at = @At("HEAD"), cancellable = true)
    private void identity2$getPoseEyeHeightIdentity(Pose pose, CallbackInfoReturnable<Float> info) {
        if (this.currentIdentity != null) {
            if (pose == Pose.CROUCHING || pose == Pose.STANDING) {
                info.setReturnValue(this.currentIdentity.getEyeHeight());
                return;
            }
            info.setReturnValue(this.currentIdentity.getEyeHeight(pose));
        }
    }

    @Inject(method = "getPistonPushReaction()Lnet/minecraft/world/level/material/PushReaction;", at = @At("HEAD"), cancellable = true)
    private void getPistonBehaviorIdentity(CallbackInfoReturnable info) {
        if (this.currentIdentity != null) {
            info.setReturnValue(this.currentIdentity.getPistonPushReaction());
        }
    }

    @Inject(method = "canSprint()Z", at = @At("HEAD"), cancellable = true)
    private void canSprintAsVehicleIdentity(CallbackInfoReturnable info) {
        if (this.currentIdentity != null) {
            info.setReturnValue(this.currentIdentity.canSprint());
        }
    }

    @Inject(method = "maxUpStep()F", at = @At("HEAD"), cancellable = true)
    private void getStepHeightIdentity(CallbackInfoReturnable info) {
        if (this.currentIdentity != null) {
            info.setReturnValue(this.currentIdentity.maxUpStep());
        }
    }

    @Inject(method = "resetFallDistance()V", at = @At("HEAD"))
    private void onLandingIdentity(CallbackInfo info) {
        if (this.currentIdentity != null) {
            this.currentIdentity.resetFallDistance();
        }
    }

    @Inject(method = "setCustomNameVisible(Z)V", at = @At("HEAD"))
    private void setCustomNameVisibleIdentity(boolean visible, CallbackInfo info) {
        if (this.currentIdentity != null) {
            this.currentIdentity.setCustomNameVisible(visible);
        }
    }

    @Inject(
            method = "isInvulnerableTo(Lnet/minecraft/world/damagesource/DamageSource;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void isInvulnerableToIdentity(DamageSource source, CallbackInfoReturnable<Boolean> info) {
        if (this.identityOf instanceof Player owner
                && SilverfishBurrowManager.isHidden(owner)
                && source != null
                && source.is(DamageTypes.IN_WALL)) {
            info.setReturnValue(true);
            return;
        }
        if ((Object) this instanceof Player player && source != null) {
            if (SilverfishBurrowManager.isHidden(player) && source.is(DamageTypes.IN_WALL)) {
                info.setReturnValue(true);
                return;
            }
            if (player.getAbilities().instabuild || player.isSpectator()) {
                if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                    return;
                }
                info.setReturnValue(true);
                return;
            }

            if (
                    this.currentIdentity != null
                            && source.is(DamageTypes.IN_WALL)
                            && player.isInWater()
                            && Boolean.TRUE.equals(IdentityTraitTags.resolveCanBreatheUnderwater(this.currentIdentity.getType()))
            ) {
                info.setReturnValue(true);
                return;
            }

            Entity activeIdentity = ((EntityAccessor) player).getCurrentIdentity();

            if (identity2$isOwnIdentityDamage(player, activeIdentity, source)) {
                info.setReturnValue(true);
                return;
            }

            if (
                    activeIdentity != null
                            && activeIdentity.getType() == EntityType.ENDER_DRAGON
                            && (source.is(DamageTypes.DRAGON_BREATH) || identity2$isOwnDragonBreathCloud(player, source))
            ) {
                info.setReturnValue(true);
                return;
            }

            if (
                    activeIdentity != null
                            && activeIdentity.getType() != EntityType.ENDER_DRAGON
                            && identity2$isMorphFireImmune(activeIdentity)
                            && (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.LAVA))
            ) {
                info.setReturnValue(true);
                return;
            }

            if (
                    activeIdentity != null
                            && source.is(DamageTypes.IN_WALL)
                            && identity2$shouldIgnoreMorphSuffocation(player, activeIdentity)
            ) {
                info.setReturnValue(true);
                return;
            }

            if (
                    activeIdentity != null
                            && identity2$isFallDamage(source)
                            && (
                            activeIdentity.getType() == EntityType.CHICKEN
                                    || activeIdentity.getType() == EntityType.CAT
                                    || IdentityTraitTags.hasSlowFalling(activeIdentity.getType())
                    )
            ) {
                info.setReturnValue(true);
                return;
            }

            boolean dragonIdentity = activeIdentity != null && activeIdentity.getType() == EntityType.ENDER_DRAGON;
            if ((dragonIdentity || IdentityProgression.isMorphDamageGraceActive(player)) && identity2$isWallCollisionDamage(source)) {
                info.setReturnValue(true);
                return;
            }

            return;
        }

        if (this.currentIdentity != null) {
            if (this.currentIdentity instanceof LivingEntity livingIdentity) {
                info.setReturnValue(livingIdentity.isInvulnerableTo(source));
            }
        }
    }

    @Unique
    private static boolean identity2$isOwnIdentityDamage(Player player, Entity activeIdentity, DamageSource source) {
        if (player == null || activeIdentity == null || source == null) {
            return false;
        }
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        return identity2$isOwnedIdentityDamageEntity(player, activeIdentity, attacker)
                || identity2$isOwnedIdentityDamageEntity(player, activeIdentity, direct);
    }

    @Unique
    private static boolean identity2$isOwnedIdentityDamageEntity(Player player, Entity activeIdentity, Entity damageEntity) {
        if (damageEntity == null) {
            return false;
        }
        if (damageEntity == activeIdentity) {
            return true;
        }
        try {
            return ((EntityAccessor) damageEntity).getIdentityOwner() == player;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Unique
    private static boolean identity2$isMorphFireImmune(Entity activeIdentity) {
        return activeIdentity != null && activeIdentity.fireImmune();
    }

    @Unique
    private static boolean identity2$isOwnDragonBreathCloud(Player player, DamageSource source) {
        if (player == null || source == null) {
            return false;
        }
        Entity direct = source.getDirectEntity();
        if (!(direct instanceof AreaEffectCloud cloud)) {
            return false;
        }
        return cloud.getOwner() == player || source.getEntity() == player;
    }

    @Unique
    private static boolean identity2$shouldIgnoreMorphSuffocation(Player player, Entity activeIdentity) {
        float idHeight = activeIdentity.getBbHeight();
        if (idHeight >= 1.2f) {
            return false;
        }

        if (player.isCrouching() || player.isSwimming()) {
            return false;
        }

        AABB box = player.getBoundingBox();

        AABB feet = new AABB(
                box.minX, box.minY, box.minZ,
                box.maxX, box.minY + 0.35, box.maxZ
        );

        double headStart = box.maxY - 0.35;
        AABB head = new AABB(
                box.minX, headStart, box.minZ,
                box.maxX, box.maxY, box.maxZ
        );

        boolean feetCollide = !player.level().noCollision(player, feet);
        boolean headCollide = !player.level().noCollision(player, head);

        return headCollide && !feetCollide;
    }
    @Unique
    private static boolean identity2$isFallDamage(DamageSource source) {
        if (source == null) {
            return false;
        }
        if (source.is(DamageTypes.FALL)) {
            return true;
        }
        if (source.is(DamageTypeTags.IS_FALL)) {
            return true;
        }
        String msgId = identity2$getDamageMessageId(source);
        if (msgId == null || msgId.isBlank()) {
            return false;
        }
        String normalized = msgId.trim().toLowerCase(Locale.ROOT).replace("-", "_");
        return normalized.equals("fall");
    }

    @Unique
    private static boolean identity2$isWallCollisionDamage(DamageSource source) {
        String msgId = identity2$getDamageMessageId(source);
        if (msgId == null || msgId.isBlank()) {
            return false;
        }
        String normalized = msgId.trim().toLowerCase(Locale.ROOT).replace("-", "_");
        return normalized.equals("inwall")
                || normalized.equals("in_wall")
                || normalized.equals("flyintowall")
                || normalized.equals("fly_into_wall")
                || normalized.equals("cramming");
    }

    @Unique
    private static String identity2$getDamageMessageId(DamageSource source) {
        if (source == null) {
            return "";
        }
        Object direct = identity2$invokeNoArg(source, "getMsgId");
        if (direct instanceof String text && !text.isBlank()) {
            return text;
        }
        Object type = identity2$invokeNoArg(source, "type");
        Object fromType = identity2$invokeNoArg(type, "msgId");
        if (fromType instanceof String text && !text.isBlank()) {
            return text;
        }
        Object holder = identity2$invokeNoArg(source, "typeHolder");
        Object value = identity2$invokeNoArg(holder, "value");
        Object fromHolder = identity2$invokeNoArg(value, "msgId");
        if (fromHolder instanceof String text && !text.isBlank()) {
            return text;
        }
        return "";
    }

    @Unique
    private static Object identity2$invokeNoArg(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isBlank()) {
            return null;
        }
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != 0) {
                    continue;
                }
                try {
                    if (!method.canAccess(target)) {
                        method.setAccessible(true);
                    }
                    return method.invoke(target);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }
//Tons of Redirects - End
}






