package empegrail.forgotten_knowledge;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public final class ModItems {
    private ModItems() {
    }

    // Example item
    public static final Item SPELL_TOME = register("spell_tome", Item::new, new Item.Settings());

    private static Item register(String name, Function<Item.Settings, Item> factory, Item.Settings settings) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ForgottenKnowledge.MOD_ID, name));
        return Items.register(key, factory, settings);
    }

    public static void initialize() {
        ForgottenKnowledge.LOGGER.info("Registering items for " + ForgottenKnowledge.MOD_ID);
    }
}

