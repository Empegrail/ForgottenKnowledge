package empegrail.forgotten_knowledge;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

public class ModItems {
    // Declare the field but don’t assign yet
    public static Item KNOWLEDGE_SHARD;

    private static Item registerItem(String name, Item item) {
        return Registry.register(
                Registries.ITEM,
                Identifier.of(ForgottenKnowledge.MOD_ID, name),
                item
        );
    }

    public static void registerModItems() {
        ForgottenKnowledge.LOGGER.info("Registering items for " + ForgottenKnowledge.MOD_ID);

        // Initialize here (after Minecraft’s registries exist)
        KNOWLEDGE_SHARD = registerItem("knowledge_shard",
                new Item(new Item.Settings()));

        // Add to a creative tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(KNOWLEDGE_SHARD);
        });
    }
}
