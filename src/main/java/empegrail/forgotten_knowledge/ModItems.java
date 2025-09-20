package empegrail.forgotten_knowledge;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

/**
 * Central item registration for ForgottenKnowledge.
 *
 * NOTE: Call ModItems.initialize() from your mod initializer (ForgottenKnowledge.onInitialize).
 */
public final class ModItems {
    private ModItems() {}

    // --- Declared items (initialized in initialize()) ---
    public static Item SPELL_TOME;
    public static Item LIGHTNING_TOME;
    // add more Item fields here as you create them

    // --- Generic register helper that uses Items.register(factory) so the registry key is set
    private static Item register(String name, Function<Item.Settings, Item> factory, Item.Settings settings) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ForgottenKnowledge.MOD_ID, name));
        // Items.register will set the registry reference for the item settings before calling the factory
        return Items.register(key, factory, settings);
    }

    // small helper to create SpellTomeItem instances
    private static Item registerTome(String name, SpellEffect effect, int xpCost) {
        Item.Settings settings = new Item.Settings().maxCount(1); // don't set group here; add to groups with ItemGroupEvents if needed
        return register(name, s -> new SpellTomeItem(s, effect, xpCost), settings);
    }

    /**
     * Call this once from your mod initializer (ForgottenKnowledge.onInitialize).
     */
    public static void initialize() {
        ForgottenKnowledge.LOGGER.info("Registering items for " + ForgottenKnowledge.MOD_ID);

        // plain placeholder tome
        SPELL_TOME = register("spell_tome", Item::new, new Item.Settings());

        // example spell tome bound to a SpellEffect
        LIGHTNING_TOME = registerTome("lightning_tome", SpellEffects.LIGHTNING_STRIKE, 2);

        // register additional tomes similarly
    }
}
