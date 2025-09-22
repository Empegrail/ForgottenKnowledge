package empegrail.forgotten_knowledge.mixin;

import empegrail.forgotten_knowledge.ModDataComponents;
import empegrail.forgotten_knowledge.SpellTomeItem;
import empegrail.forgotten_knowledge.spell.ModSpellRegistry;
import empegrail.forgotten_knowledge.spell.SpellRecipe;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Mixin that handles both spell recipe creation and spell tome level upgrading.
 */
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {

    @Shadow @Final private Property levelCost;

    @Inject(method = "updateResult", at = @At("RETURN"))
    private void onUpdateResult(CallbackInfo ci) {
        AnvilScreenHandler self = (AnvilScreenHandler) (Object) this;

        // Gather input stacks from the anvil slots
        ItemStack left = self.getSlot(0).getStack();
        ItemStack right = self.getSlot(1).getStack();

        // First, check if we're combining two spell tomes of the same type
        if (left.getItem() instanceof SpellTomeItem && right.getItem() instanceof SpellTomeItem) {
            if (left.getItem() == right.getItem()) {
                // Both are the same type of spell tome - try to upgrade
                handleSpellTomeUpgrade(self, left, right);
                return;
            }
        }

        // If not upgrading spell tomes, check for enchanted book recipes
        if (!left.isOf(Items.ENCHANTED_BOOK) || !right.isOf(Items.ENCHANTED_BOOK)) {
            return;
        }

        // For enchanted books the enchantments are stored in STORED_ENCHANTMENTS
        ItemEnchantmentsComponent leftComp = left.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        ItemEnchantmentsComponent rightComp = right.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);

        // Iterate recipes (stop at first match)
        for (SpellRecipe recipe : ModSpellRegistry.getRecipes()) {
            RegistryKey<Enchantment> leftKey = recipe.leftEnchantment;
            RegistryKey<Enchantment> rightKey = recipe.rightEnchantment;

            // Check both orders:
            if ((hasEnchantment(leftComp, leftKey, recipe.leftLevel) && hasEnchantment(rightComp, rightKey, recipe.rightLevel))
                    || (hasEnchantment(leftComp, rightKey, recipe.rightLevel) && hasEnchantment(rightComp, leftKey, recipe.leftLevel))) {

                // Put the recipe result into the anvil output slot (slot index 2)
                ItemStack resultStack = recipe.result.getDefaultStack();
                // Set initial level to 1 for newly created spell tomes
                resultStack.set(ModDataComponents.SPELL_LEVEL, 1);
                self.getSlot(2).setStack(resultStack);

                // Set displayed XP cost
                this.levelCost.set(recipe.xpCost);

                // Stop after first match
                return;
            }
        }
    }

    /**
     * Handle upgrading spell tomes by combining two of the same type
     */
    @Unique
    private void handleSpellTomeUpgrade(AnvilScreenHandler self, ItemStack left, ItemStack right) {
        int leftLevel = left.getOrDefault(ModDataComponents.SPELL_LEVEL, 1);
        int rightLevel = right.getOrDefault(ModDataComponents.SPELL_LEVEL, 1);

        // Only allow upgrading if levels are the same (to prevent skipping levels)
        if (leftLevel == rightLevel) {
            ItemStack resultStack = left.copy();
            int newLevel = leftLevel + 1;
            resultStack.set(ModDataComponents.SPELL_LEVEL, newLevel);

            // Calculate XP cost for upgrading (scales with new level)
            int upgradeCost = 5 * newLevel; // Base cost of 5 per level

            self.getSlot(2).setStack(resultStack);
            this.levelCost.set(upgradeCost);
        }
    }

    /**
     * Check whether an ItemEnchantmentsComponent contains the given enchantment registry key
     * at *at least* the required level.
     */
    @Unique
    private static boolean hasEnchantment(ItemEnchantmentsComponent comp, RegistryKey<Enchantment> key, int requiredLevel) {
        if (comp == null) return false;

        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> e : comp.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> regEntry = e.getKey();
            int level = e.getIntValue();

            Optional<RegistryKey<Enchantment>> k = regEntry.getKey();
            if (k.isPresent() && k.get().equals(key) && level >= requiredLevel) {
                return true;
            }
        }

        return false;
    }
}





















