package empegrail.forgotten_knowledge.mixin;

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
 * Mixin that replaces anvil result when two enchanted books match a recipe in ModSpellRegistry.
 *
 * Behavior:
 *  - Only checks enchanted books (Items.ENCHANTED_BOOK).
 *  - Reads stored enchantments (STORED_ENCHANTMENTS component) from each book.
 *  - For each recipe: checks both orders (left/right and swapped).
 *  - If a recipe matches (type + required level), writes the recipe result item to the result slot
 *    and sets the anvil's XP cost to the configured value.
 */
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {

    // Shadow the anvil's levelCost property so we can set the XP cost in the GUI
    @Shadow @Final private Property levelCost;

    @Inject(method = "updateResult", at = @At("RETURN"))
    private void onUpdateResult(CallbackInfo ci) {
        AnvilScreenHandler self = (AnvilScreenHandler) (Object) this;

        // Gather input stacks from the anvil slots
        ItemStack left = self.getSlot(0).getStack();
        ItemStack right = self.getSlot(1).getStack();

        // Only operate on enchanted books
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
            //   (left input matches recipe.left && right input matches recipe.right)
            // OR
            //   (left input matches recipe.right && right input matches recipe.left)
            if ((hasEnchantment(leftComp, leftKey, recipe.leftLevel) && hasEnchantment(rightComp, rightKey, recipe.rightLevel))
                    || (hasEnchantment(leftComp, rightKey, recipe.rightLevel) && hasEnchantment(rightComp, leftKey, recipe.leftLevel))) {

                // Put the recipe result into the anvil output slot (slot index 2)
                self.getSlot(2).setStack(recipe.result.getDefaultStack());

                // Set displayed XP cost
                this.levelCost.set(recipe.xpCost);

                // Stop after first match
                return;
            }
        }
    }

    /**
     * Check whether an ItemEnchantmentsComponent contains the given enchantment registry key
     * at *at least* the required level.
     *
     * The component stores entries as RegistryEntry<Enchantment> + integer level (Object2IntMap).
     */
    @Unique
    private static boolean hasEnchantment(ItemEnchantmentsComponent comp, RegistryKey<Enchantment> key, int requiredLevel) {
        if (comp == null) return false;

        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> e : comp.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> regEntry = e.getKey();
            int level = e.getIntValue(); // stored integer level

            Optional<RegistryKey<Enchantment>> k = regEntry.getKey();
            if (k.isPresent() && k.get().equals(key) && level >= requiredLevel) {
                return true;
            }
        }

        return false;
    }
}





















