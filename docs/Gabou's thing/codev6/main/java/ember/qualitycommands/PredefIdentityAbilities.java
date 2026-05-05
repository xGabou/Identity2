package ember.qualitycommands;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import ember.qualitycommands.util.EntityAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import ember.qualitycommands.util.ShulkerEntityAccessor;
public class PredefIdentityAbilities {
    abstract static class IdentityAbility{
        public void execute(Entity arg){};
        public void tick(Entity arg,int cooldown){};
        public void passivetick(Entity arg,boolean used){

        };
        public boolean overrideAttack(Entity arg){
            return false;
        }
    }
    public static Map<ResourceLocation,IdentityAbility> predef=Map.of(
        ResourceLocation.of("ghast"),new PredefIdentityAbilities.IdentityAbility() {
            @Override
            public void execute(Entity arg){
                return;
            }
            @Override
            public void tick(Entity arg,int cooldown){
                //QualityCommands.LOGGER.info("TICKING GHAST ABILITY");
                HitResult target=arg.raycast(1000,0,false);
                World world = arg.getEntityWorld();

					if (cooldown == 10 && !arg.isSilent()) {
						world.syncWorldEvent(null, WorldEvents.GHAST_WARNS, arg.getBlockPos(), 0);
					}
                    if(cooldown>10){
                        ((GhastEntity)(((EntityAccessor)arg).getCurrentIdentity())).setShooting(true);
                    }

					if (cooldown == 20) {
						double e = 4.0;
						Vec3d vec3d = arg.getRotationVec(1.0F);
						double f = target.getPos().x - (arg.getX() + vec3d.x * 4.0);
						double g = target.getPos().y - (0.5 + arg.getBodyY(0.5));
						double h = target.getPos().z - (arg.getZ() + vec3d.z * 4.0);
						Vec3d vec3d2 = new Vec3d(f, g, h);
						if (!arg.isSilent()) {
							world.syncWorldEvent(null, WorldEvents.GHAST_SHOOTS, arg.getBlockPos(), 0);
						}

						FireballEntity fireballEntity = new FireballEntity(world, (LivingEntity)arg, vec3d2.normalize(), 1);
						fireballEntity.setPosition(arg.getX() + vec3d.x * 4.0, arg.getBodyY(0.5) + 0.5, fireballEntity.getZ() + vec3d.z * 4.0);
						world.spawnEntity(fireballEntity);
                        ((GhastEntity)(((EntityAccessor)arg).getCurrentIdentity())).setShooting(false);
					}
                return; 
            }
        },
        ResourceLocation.of("enderman"),new PredefIdentityAbilities.IdentityAbility() {
            @Override
            public void execute(Entity player){
                World world=player.getEntityWorld();

                double maxDistance = IdentitySettings.endermanAbilityTeleportDistance;
                HitResult hit = player.raycast(maxDistance, 0, true);
                Vec3d targetPos = hit.getPos();

                // Point de base converti en BlockPos
                BlockPos blockPos = BlockPos.ofFloored(targetPos);

                // Monte tant que le bloc est solide pour éviter de téléporter dans un bloc
                while (!isSafeTeleportSpot(world, blockPos) && world.isInHeightLimit((int)targetPos.y)) {
                    blockPos = blockPos.up();
                }

                Vec3d safePos = Vec3d.ofCenter(blockPos);

                // Son départ
                world.playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                        SoundCategory.PLAYERS,
                        1.0F,
                        1.0F
                );

                // Téléportation
                player.requestTeleport(safePos.x, safePos.y, safePos.z);

                // Son arrivée
                world.playSound(
                        null,
                        safePos.x,
                        safePos.y,
                        safePos.z,
                        SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                        SoundCategory.PLAYERS,
                        1.0F,
                        1.0F
                );
            
            }
            @Override
            public void tick(Entity arg,int cooldown){
                
            }
            

            private boolean isSafeTeleportSpot(World world, BlockPos pos) {
                BlockState blockAtFeet = world.getBlockState(pos);
                BlockState blockAtHead = world.getBlockState(pos.up());
                return blockAtFeet.isAir() && blockAtHead.isAir();
            }
        },
        ResourceLocation.of("shulker"),new PredefIdentityAbilities.IdentityAbility() {
            @Override
            public void execute(Entity player){
                ShulkerEntity shulker=(ShulkerEntity)((EntityAccessor)player).getCurrentIdentity();
                if(((ShulkerEntityAccessor)shulker).runGetPeekAmount()!=100){
                    ((ShulkerEntityAccessor)shulker).setPeekAmount(100);
                }
            }
            @Override
            public void passivetick(Entity player,boolean usedLastTick){
                ShulkerEntity shulker=(ShulkerEntity)((EntityAccessor)player).getCurrentIdentity();
                if(usedLastTick==false){
                    if(((ShulkerEntityAccessor)shulker).runGetPeekAmount()!=0){
                        ((ShulkerEntityAccessor)shulker).setPeekAmount(0);
                    }
                }
                if (!shulker.getEntityWorld().isClient() && !shulker.hasVehicle() && !canStay(shulker.getBlockPos(), shulker.getAttachedFace(),shulker)) {
                    BlockPos pos=shulker.getBlockPos();
                    ((ShulkerEntityAccessor)shulker).runTryAttachOrTeleport();
                    if((pos==shulker.getBlockPos())==false){
                        player.requestTeleport(shulker.getEntityPos().getX(),shulker.getEntityPos().getY(),shulker.getEntityPos().getZ());
                        
                    }
                }
            }
            @Override
            public boolean overrideAttack(Entity arg){
                ShulkerEntity shulker=(ShulkerEntity)((EntityAccessor)arg).getCurrentIdentity();
                if(((ShulkerEntityAccessor)shulker).runGetPeekAmount()!=0){
                    double d = 128;
                    double e = MathHelper.square(d);
                    Vec3d vec3d = arg.getCameraPosVec(1);
                    Vec3d vec3d2 = arg.getRotationVec(1);
                    Vec3d vec3d3 = vec3d.add(vec3d2.x * d, vec3d2.y * d, vec3d2.z * d);
                    Box box = arg.getBoundingBox().stretch(vec3d2.multiply(d)).expand(1.0, 1.0, 1.0);
                    HitResult target=ProjectileUtil.raycast(arg, vec3d, vec3d3, box, EntityPredicates.CAN_HIT, e);
                    if(target==null){
                        QualityCommands.LOGGER.info("not entity");return false;}
                    if(target.getType()==HitResult.Type.ENTITY){
                        shulker.getEntityWorld()
                                .spawnEntity(
                                    new ShulkerBulletEntity(shulker.getEntityWorld(), shulker, ((EntityHitResult)target).getEntity(), shulker.getAttachedFace().getAxis())
                                );
                            shulker.playSound(
                                SoundEvents.ENTITY_SHULKER_SHOOT, 2.0F, /*((shulker.random.nextFloat() - shulker.random.nextFloat()) * 0.2F)*/ + 1.0F
                            );
                    }
                }
                return true;
            }
            boolean canStay(BlockPos pos, Direction direction,ShulkerEntity entity) {
                if (isInvalidPosition(pos,entity)) {
                    return false;
                } else {
                    Direction direction2 = direction.getOpposite();
                    if (!entity.getEntityWorld().isDirectionSolid(pos.offset(direction), entity, direction2)) {
                        return false;
                    } else {
                        Box box = ShulkerEntity.calculateBoundingBox(entity.getScale(), direction2, 1.0F, pos.toBottomCenterPos()).contract(1.0E-6);
                        return entity.getEntityWorld().isSpaceEmpty(entity, box);
                    }
                }
            }

            private boolean isInvalidPosition(BlockPos pos,ShulkerEntity entity) {
                BlockState blockState = entity.getEntityWorld().getBlockState(pos);
                if (blockState.isAir()) {
                    return false;
                } else {
                    boolean bl = blockState.isOf(Blocks.MOVING_PISTON) && pos.equals(entity.getBlockPos());
                    return !bl;
                }
            }
            
        }
    );
}


