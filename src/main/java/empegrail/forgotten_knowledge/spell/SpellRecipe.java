package empegrail.forgotten_knowledge.spell;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;

/**
 * Simple value container for a single anvil -> spell recipe.
 *
 * leftEnchantment/leftLevel  = required enchantment and required minimum level on the LEFT book
 * rightEnchantment/rightLevel = same for the RIGHT book
 * result                      = Item to place in anvil result slot (your tome Item)
 * xpCost                      = anvil level cost to show for the operation
 */
public class SpellRecipe {
    public final RegistryKey<Enchantment> leftEnchantment;
    public final int leftLevel;
    public final RegistryKey<Enchantment> rightEnchantment;
    public final int rightLevel;
    public final Item result;
    public final int xpCost;

    public SpellRecipe(RegistryKey<Enchantment> leftEnchantment, int leftLevel,
                       RegistryKey<Enchantment> rightEnchantment, int rightLevel,
                       Item result, int xpCost) {
        this.leftEnchantment = leftEnchantment;
        this.leftLevel = leftLevel;
        this.rightEnchantment = rightEnchantment;
        this.rightLevel = rightLevel;
        this.result = result;
        this.xpCost = xpCost;
    }
}




