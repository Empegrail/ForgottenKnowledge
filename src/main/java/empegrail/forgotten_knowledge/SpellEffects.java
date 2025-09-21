package empegrail.forgotten_knowledge;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.List;


/**
 * Small collection of example SpellEffect implementations.
 */
public final class SpellEffects {
    private SpellEffects() {}

    // Helper: Create a player attack DamageSource that works across mappings/versions
    private static DamageSource createPlayerAttackSource(ServerWorld world, PlayerEntity player) {
        try {
            // Standard case: playerAttack(PlayerEntity)
            return world.getDamageSources().playerAttack(player);
        } catch (Exception ignored) {
            // Fallback if mappings differ
            return world.getDamageSources().generic();
        }
    }


    // Create a Lighting Strike Effect.
    public static final SpellEffect LIGHTNING_STRIKE = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        BlockPos targetPos;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hit;
            targetPos = bhr.getBlockPos();
        } else {
            targetPos = user.getBlockPos().offset(user.getHorizontalFacing(), 10);
        }

        LightningEntity bolt = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
        bolt.refreshPositionAfterTeleport(targetPos.toCenterPos());
        world.spawnEntity(bolt);

        return true;
    };

    // Create an Explosion Effect.
    public static final SpellEffect EXPLOSION = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        BlockPos targetPos;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hit;
            targetPos = bhr.getBlockPos();
        } else {
            targetPos = user.getBlockPos().offset(user.getHorizontalFacing(), 10);
        }

        world.createExplosion(
                user,
                targetPos.getX() + 0.5,
                targetPos.getY() + 0.5,
                targetPos.getZ() + 0.5,
                3.0f,
                false,
                World.ExplosionSourceType.TNT
        );

        return true;
    };

    // Create an Effect that makes any object vanish, blocks etc. but not entities.
    public static final SpellEffect VANISH_OBJECT = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        BlockPos targetPos;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hit;
            targetPos = bhr.getBlockPos();
        } else {
            targetPos = user.getBlockPos().offset(user.getHorizontalFacing(), 10);
        }

        BlockState state = world.getBlockState(targetPos);

        if (state.isAir() || state.getBlock().getHardness() < 0) {
            return false;
        }

        world.setBlockState(targetPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);

        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    ParticleTypes.PORTAL,
                    targetPos.getX() + 0.5,
                    targetPos.getY() + 0.5,
                    targetPos.getZ() + 0.5,
                    20,
                    0.5, 0.5, 0.5,
                    0.1
            );
        }

        return true;
    };

    // Sword Slash Effect - cuts only one target directly in front of the player.
    public static final SpellEffect SWORD_SLASH = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        double range = 20.0;
        float damage = 9.0f;

        Vec3d lookVec = user.getRotationVec(1.0f);
        Vec3d start = user.getEyePos();
        Vec3d end = start.add(lookVec.multiply(range));

        Box hitBox = new Box(start, end).expand(1.0);
        List<Entity> targets = world.getOtherEntities(user, hitBox);

        // Find the closest valid target in front of the player
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity target : targets) {
            if (target instanceof LivingEntity living && target != user) {
                Vec3d toTarget = living.getPos().subtract(start).normalize();
                if (lookVec.dotProduct(toTarget) > 0.3) { // make sure it's in front
                    double dist = living.squaredDistanceTo(start);
                    if (dist < closestDist) {
                        closest = living;
                        closestDist = dist;
                    }
                }
            }
        }

        if (closest != null && world instanceof ServerWorld serverWorld) {
            DamageSource src = createPlayerAttackSource(serverWorld, user);
            closest.damage(serverWorld, src, damage);

            // Spawn a single sweep particle at the target's position
            serverWorld.spawnParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    closest.getX(),
                    closest.getBodyY(0.5), // center around torso
                    closest.getZ(),
                    1, 0, 0, 0, 0.0
            );

            // Play slash sound
            world.playSound(null, user.getBlockPos(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                    SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        return true;
    };


}




