package empegrail.forgotten_knowledge;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForgottenKnowledge implements ModInitializer {
    public static final String MOD_ID = "forgotten_knowledge";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // register items and anything else that depends on registries being ready
        ModItems.initialize();
        empegrail.forgotten_knowledge.spell.ModSpellRegistry.register(); // register recipes
    }
}


