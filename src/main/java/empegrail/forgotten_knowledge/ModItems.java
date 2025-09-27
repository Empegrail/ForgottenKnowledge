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
    public static Item EXPLOSION_TOME;
    public static Item VANISHMENT_TOME;
    public static Item CUTTING_TOME;
    public static Item BIND_TOME;
    public static Item FROST_TOME;
    public static Item FEATHER_TOME;
    public static Item HOLY_TOME;
    public static Item VERMIN_TOME;
    public static Item IGNITE_TOME;
    public static Item FIREBOLT_TOME;
    public static Item FIREWAVE_TOME;
    public static Item ICE_TOME;
    public static Item RETRIBUTION_TOME;
    public static Item NECROTIC_TOME;
    public static Item DEFENSE_TOME;
    public static Item HASTE_TOME;
    // add more Item fields here as you create them

    // --- Generic register helper that uses Items.register(factory) so the registry key is set
    private static Item register(String name, Function<Item.Settings, Item> factory, Item.Settings settings) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ForgottenKnowledge.MOD_ID, name));
        // Items.register will set the registry reference for the item settings before calling the factory
        return Items.register(key, factory, settings);
    }

    // small helper to create SpellTomeItem instances
    private static Item registerTome(String name, SpellEffect effect, int baseXpCost) {
        Item.Settings settings = new Item.Settings().maxCount(1);
        return register(name, s -> new SpellTomeItem(s, effect, baseXpCost), settings);
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
        EXPLOSION_TOME = registerTome("explosion_tome", SpellEffects.EXPLOSION, 2);
        VANISHMENT_TOME = registerTome("vanishment_tome", SpellEffects.VANISH_OBJECT, 2);
        CUTTING_TOME = registerTome("cutting_tome", SpellEffects.SWORD_SLASH, 2);
        BIND_TOME = registerTome("bind_tome", SpellEffects.BIND, 2);
        FROST_TOME = registerTome("frost_tome", SpellEffects.FROST, 2);
        FEATHER_TOME = registerTome("feather_tome", SpellEffects.FEATHER, 2);
        HOLY_TOME = registerTome("holy_tome", SpellEffects.HOLY_NOVA, 2);
        VERMIN_TOME = registerTome("vermin_tome", SpellEffects.VERMIN_BANE, 2);
        IGNITE_TOME = registerTome("ignite_tome", SpellEffects.IGNITE, 2);
        FIREBOLT_TOME = registerTome("firebolt_tome", SpellEffects.FIRE_BOLT, 2);
        FIREWAVE_TOME = registerTome("firewave_tome", SpellEffects.FIRE_WAVE, 2);
        ICE_TOME = registerTome("ice_tome", SpellEffects.ICE_SPEAR, 2);
        RETRIBUTION_TOME = registerTome("retribution_tome", SpellEffects.RETRIBUTION, 2);
        NECROTIC_TOME = registerTome("necrotic_tome", SpellEffects.NECROTIC, 2);
        DEFENSE_TOME = registerTome("defense_tome", SpellEffects.DEFENSE, 2);
        HASTE_TOME = registerTome("haste_tome", SpellEffects.HASTE, 2);

        // register additional tomes similarly
    }
}
