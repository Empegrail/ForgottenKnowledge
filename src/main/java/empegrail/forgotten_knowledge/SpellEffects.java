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

import java.util.List;

/**
 * Small collection of example SpellEffect implementations.
 * Now includes level scaling for spell effects.
 */
public final class SpellEffects {
    private SpellEffects() {}

    // Helper: Get spell level from item stack
    private static int getSpellLevel(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.SPELL_LEVEL, 1);
    }

    // Helper: Calculate scaling factor based on spell level
    private static float getScaleFactor(int level) {
        return 1.0f + (level - 1) * 0.5f; // Level 1: 1.0x, Level 2: 1.5x, Level 3: 2.0x, etc.
    }

    // Helper: Create a player attack DamageSource that works across mappings/versions
    private static DamageSource createPlayerAttackSource(ServerWorld world, PlayerEntity player) {
        try {
            return world.getDamageSources().playerAttack(player);
        } catch (Exception ignored) {
            return world.getDamageSources().generic();
        }
    }

    // Create a Lighting Strike Effect with level scaling
    public static final SpellEffect LIGHTNING_STRIKE = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        int level = getSpellLevel(stack);
        float scaleFactor = getScaleFactor(level);

        BlockPos targetPos;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hit;
            targetPos = bhr.getBlockPos();
        } else {
            targetPos = user.getBlockPos().offset(user.getHorizontalFacing(), 10);
        }

        // Spawn multiple lightning bolts based on level
        for (int i = 0; i < level; i++) {
            LightningEntity bolt = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
            Vec3d offset = new Vec3d(
                    world.random.nextDouble() * 5.0 * scaleFactor - 2.5 * scaleFactor,
                    0,
                    world.random.nextDouble() * 5.0 * scaleFactor - 2.5 * scaleFactor
            );
            bolt.refreshPositionAfterTeleport(targetPos.toCenterPos().add(offset));
            world.spawnEntity(bolt);
        }

        return true;
    };

    // Create an Explosion Effect with level scaling
    public static final SpellEffect EXPLOSION = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        int level = getSpellLevel(stack);
        float scaleFactor = getScaleFactor(level);

        BlockPos targetPos;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hit;
            targetPos = bhr.getBlockPos();
        } else {
            targetPos = user.getBlockPos().offset(user.getHorizontalFacing(), 10);
        }

        float power = 3.0f * scaleFactor;

        world.createExplosion(
                user,
                targetPos.getX() + 0.5,
                targetPos.getY() + 0.5,
                targetPos.getZ() + 0.5,
                power,
                false,
                World.ExplosionSourceType.TNT
        );

        return true;
    };

    // Create an Effect that makes objects vanish with level scaling
    public static final SpellEffect VANISH_OBJECT = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        int level = getSpellLevel(stack);
        float scaleFactor = getScaleFactor(level);

        BlockPos targetPos;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hit;
            targetPos = bhr.getBlockPos();
        } else {
            targetPos = user.getBlockPos().offset(user.getHorizontalFacing(), 10);
        }

        boolean vanishedAny = false;
        int radius = (int) Math.ceil(scaleFactor) - 1;

        // Vanish blocks in an area based on level
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos currentPos = targetPos.add(x, y, z);
                    BlockState state = world.getBlockState(currentPos);

                    if (state.isAir() || state.getBlock().getHardness() < 0) {
                        continue;
                    }

                    world.setBlockState(currentPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                    vanishedAny = true;
                }
            }
        }

        if (vanishedAny && world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    ParticleTypes.PORTAL,
                    targetPos.getX() + 0.5,
                    targetPos.getY() + 0.5,
                    targetPos.getZ() + 0.5,
                    (int)(20 * scaleFactor),
                    scaleFactor, scaleFactor, scaleFactor,
                    0.1
            );
        }

        return vanishedAny;
    };

    // Sword Slash Effect with level scaling
    public static final SpellEffect SWORD_SLASH = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        int level = getSpellLevel(stack);
        float scaleFactor = getScaleFactor(level);

        double range = 20.0 * scaleFactor;
        float damage = 9.0f * scaleFactor;

        Vec3d lookVec = user.getRotationVec(1.0f);
        Vec3d start = user.getEyePos();
        Vec3d end = start.add(lookVec.multiply(range));

        Box hitBox = new Box(start, end).expand(scaleFactor);
        List<Entity> targets = world.getOtherEntities(user, hitBox);

        // Find the closest valid target in front of the player
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity target : targets) {
            if (target instanceof LivingEntity living && target != user) {
                Vec3d toTarget = living.getPos().subtract(start).normalize();
                if (lookVec.dotProduct(toTarget) > 0.3) {
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

            // Spawn particles based on level
            serverWorld.spawnParticles(
                    ParticleTypes.SWEEP_ATTACK,
                    closest.getX(),
                    closest.getBodyY(0.5),
                    closest.getZ(),
                    level, // More particles for higher levels
                    0.5 * scaleFactor, 0.5 * scaleFactor, 0.5 * scaleFactor,
                    0.0
            );

            // Play slash sound
            world.playSound(null, user.getBlockPos(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                    SoundCategory.PLAYERS, 1.0f * scaleFactor, 1.0f);
        }

        return true;
    };
}



