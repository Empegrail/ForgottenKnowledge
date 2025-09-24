package empegrail.forgotten_knowledge.spell;

import empegrail.forgotten_knowledge.ModItems;
import net.minecraft.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central place to register spell recipes.
 *
 * Add recipes inside register() — this is the only file you need to touch
 * to add new anvil-based spell recipes.
 */
public final class ModSpellRegistry {

    // Public so the mixin can read it; we expose an unmodifiable view below.
    public static final List<SpellRecipe> RECIPES = new ArrayList<>();

    private ModSpellRegistry() {}

    /**
     * Populate the recipe list. Call once from your mod initializer.
     */
    public static void register() {
        RECIPES.clear();

        // Example: Channeling + Channeling -> Lightning Tome (xp cost 5).
        // (Channeling is single-level, so we use level 1)
        RECIPES.add(new SpellRecipe(
                Enchantments.CHANNELING, 1,
                Enchantments.CHANNELING, 1,
                ModItems.LIGHTNING_TOME,
                5
        ));

        // Example: Blast Protection IV + Blast Protection IV -> Explosion Tome (xp cost 5)
        // This requires the books be level 4.
        RECIPES.add(new SpellRecipe(
                Enchantments.BLAST_PROTECTION, 4,
                Enchantments.BLAST_PROTECTION, 4,
                ModItems.EXPLOSION_TOME,
                5
        ));

        // Example: Curse Of Vanishing + Curse Of Vanishing -> Vanishment Tome (xp cost 5).
        // (Curse Of Vanishing is single-level, so we use level 1)
        RECIPES.add(new SpellRecipe(
                Enchantments.VANISHING_CURSE, 1,
                Enchantments.VANISHING_CURSE, 1,
                ModItems.VANISHMENT_TOME,
                5
        ));

        // This requires the books be level 4.
        RECIPES.add(new SpellRecipe(
                Enchantments.SHARPNESS, 5,
                Enchantments.SHARPNESS, 5,
                ModItems.CUTTING_TOME,
                5
        ));

        // This requires the books be level 4.
        RECIPES.add(new SpellRecipe(
                Enchantments.BINDING_CURSE, 1,
                Enchantments.BINDING_CURSE, 1,
                ModItems.BIND_TOME,
                5
        ));

        // This requires the books be level 2.
        RECIPES.add(new SpellRecipe(
                Enchantments.FROST_WALKER, 2,
                Enchantments.FROST_WALKER, 2,
                ModItems.FROST_TOME,
                5
        ));

        // This requires the books be level 2.
        RECIPES.add(new SpellRecipe(
                Enchantments.FEATHER_FALLING, 4,
                Enchantments.FEATHER_FALLING, 4,
                ModItems.FEATHER_TOME,
                5
        ));

        // Add more recipes below in the same format.
    }

    public static List<SpellRecipe> getRecipes() {
        return Collections.unmodifiableList(RECIPES);
    }
}

