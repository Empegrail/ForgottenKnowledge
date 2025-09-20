package empegrail.forgotten_knowledge;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/**
 * A simple functional interface for spell effects.
 *
 * Implementations should return true when the effect actually performed something (so the caller can
 * play sounds/animations / return SUCCESS); return false to indicate nothing happened.
 */
public interface SpellEffect {
    boolean cast(World world, PlayerEntity user, Hand hand, ItemStack stack, HitResult hit);
}


