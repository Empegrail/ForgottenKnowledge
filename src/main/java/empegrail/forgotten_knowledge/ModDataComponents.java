package empegrail.forgotten_knowledge;

import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModDataComponents {
    public static final ComponentType<Integer> SPELL_LEVEL = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(ForgottenKnowledge.MOD_ID, "spell_level"),
            ComponentType.<Integer>builder().codec(com.mojang.serialization.Codec.INT).build()
    );

    public static void registerDataComponents() {
        // This just ensures the class loads.
    }
}
