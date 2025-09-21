package empegrail.forgotten_knowledge;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.explosion.EntityExplosionBehavior;

/**
 * Small collection of example SpellEffect implementations.
 */
public final class SpellEffects {
    private SpellEffects() {}

    // Create a Lighting Strike Effect.
    public static final SpellEffect LIGHTNING_STRIKE = (world, user, hand, stack, hit) -> {
        if (world.isClient) return false;

        // If we hit a block, summon lightning at the hit block center,
        // otherwise spawn 10 blocks in front of the player
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

        // If we hit a block, explode at the hit block center,
        // otherwise explode 10 blocks in front of the player
        BlockPos targetPos;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hit;
            targetPos = bhr.getBlockPos();
        } else {
            targetPos = user.getBlockPos().offset(user.getHorizontalFacing(), 10);
        }

        // Create explosion (like TNT). Params:
        // (entityCausingExplosion, x, y, z, power, createFire, destructionType)
        world.createExplosion(
                user,                                   // entity that caused it (player)
                targetPos.getX() + 0.5,                 // center X
                targetPos.getY() + 0.5,                 // center Y
                targetPos.getZ() + 0.5,                 // center Z
                3.0f,                                   // explosion power (TNT is 4.0F)
                false,                                  // whether to set fire
                World.ExplosionSourceType.TNT           // block destruction behavior
        );

        return true;
    };

}



