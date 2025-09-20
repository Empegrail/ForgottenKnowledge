package empegrail.forgotten_knowledge.mixin;

import empegrail.forgotten_knowledge.ModItems;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
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
 * This mixin hooks into the anvil update process.
 * It checks if two enchanted books with Channeling are combined,
 * and replaces the result with a custom Lightning Tome.
 */
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {

    // Represents the XP cost shown in the anvil.
    @Shadow @Final private Property levelCost;

    /**
     * Runs after vanilla finishes computing the anvil output.
     * If both inputs are enchanted books with Channeling, we override the result.
     */
    @Inject(method = "updateResult", at = @At("RETURN"))
    private void onUpdateResult(CallbackInfo ci) {
        AnvilScreenHandler self = (AnvilScreenHandler) (Object) this;

        ItemStack left = self.getSlot(0).getStack();
        ItemStack right = self.getSlot(1).getStack();

        // Only continue if both inputs are enchanted books.
        if (!left.isOf(Items.ENCHANTED_BOOK) || !right.isOf(Items.ENCHANTED_BOOK)) {
            return;
        }

        // Enchantments on books are stored in STORED_ENCHANTMENTS, not ENCHANTMENTS.
        ItemEnchantmentsComponent leftComp = left.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        ItemEnchantmentsComponent rightComp = right.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);

        // Check if both books contain the Channeling enchantment.
        boolean leftHas = hasEnchantment(leftComp, Enchantments.CHANNELING);
        boolean rightHas = hasEnchantment(rightComp, Enchantments.CHANNELING);

        // If both books have Channeling, output the custom Lightning Tome.
        if (leftHas && rightHas) {
            self.getSlot(2).setStack(ModItems.LIGHTNING_TOME.getDefaultStack());
            this.levelCost.set(5); // Set the XP cost for the operation.
        }
    }

    /**
     * Helper function: checks if an ItemEnchantmentsComponent
     * contains the specified enchantment.
     */
    @Unique
    private static boolean hasEnchantment(ItemEnchantmentsComponent comp, RegistryKey<Enchantment> key) {
        if (comp == null) return false;
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> e : comp.getEnchantmentEntries()) {
            Optional<RegistryKey<Enchantment>> k = e.getKey().getKey();
            if (k.isPresent() && k.get().equals(key)) {
                return true;
            }
        }
        return false;
    }
}


















