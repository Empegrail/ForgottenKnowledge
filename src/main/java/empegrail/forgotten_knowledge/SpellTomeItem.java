package empegrail.forgotten_knowledge;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class SpellTomeItem extends Item {
    private final SpellEffect effect;
    private final double range;
    private final int xpCost;

    public SpellTomeItem(Settings settings, SpellEffect effect, int xpCost) {
        this(settings, effect, xpCost, 20.0); // default range 20 blocks
    }

    public SpellTomeItem(Settings settings, SpellEffect effect, int xpCost, double range) {
        super(settings);
        this.effect = effect;
        this.range = range;
        this.xpCost = xpCost;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        // Always show the enchantment glint for spell tomes
        return true;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        // Only perform world-changing actions on the server
        if (!world.isClient && effect != null) {
            // Check XP levels before casting
            if (user.experienceLevel < xpCost) {
                user.sendMessage(Text.literal("Not enough XP!"), true);
                return ActionResult.FAIL;
            }





            // Raycast where the player is looking
            HitResult hit = user.raycast((float) this.range, 0.0F, false);

            // Delegate the actual behaviour to the SpellEffect
            boolean performed = effect.cast(world, user, hand, stack, hit);

            if (performed) {
                // Drain XP levels
                user.addExperienceLevels(-xpCost);

                // Swing animation for the player
                user.swingHand(hand, true);

                // Play a little casting sound (optional)
                world.playSound(
                        null,
                        user.getBlockPos(),
                        SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL,
                        SoundCategory.PLAYERS,
                        1.0F,
                        1.0F
                );

                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.PASS;
    }
}
