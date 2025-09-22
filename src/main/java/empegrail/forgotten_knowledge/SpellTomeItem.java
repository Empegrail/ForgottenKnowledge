package empegrail.forgotten_knowledge;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class SpellTomeItem extends Item {
    private final SpellEffect effect;
    private final double range;
    private final int baseXpCost;

    public SpellTomeItem(Settings settings, SpellEffect effect, int baseXpCost) {
        this(settings, effect, baseXpCost, 20.0);
    }

    public SpellTomeItem(Settings settings, SpellEffect effect, int baseXpCost, double range) {
        super(settings);
        this.effect = effect;
        this.range = range;
        this.baseXpCost = baseXpCost;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        int level = stack.getOrDefault(ModDataComponents.SPELL_LEVEL, 1);
        int xpCost = baseXpCost * level;

        if (!world.isClient && effect != null) {
            if (user.experienceLevel < xpCost) {
                user.sendMessage(Text.literal("Not enough XP! Need " + xpCost + " levels"), true);
                return ActionResult.FAIL;
            }

            HitResult hit = user.raycast((float) this.range, 0.0F, false);
            boolean performed = effect.cast(world, user, hand, stack, hit);

            if (performed) {
                user.addExperienceLevels(-xpCost);
                user.swingHand(hand, true);

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
