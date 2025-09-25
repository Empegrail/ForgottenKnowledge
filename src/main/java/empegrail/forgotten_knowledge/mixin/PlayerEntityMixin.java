package empegrail.forgotten_knowledge.mixin;

import empegrail.forgotten_knowledge.SpellEffects;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Check if the damage comes from a living entity and retribution is active
        if (source.getAttacker() instanceof net.minecraft.entity.LivingEntity attacker) {
            SpellEffects.RetributionManager.onPlayerAttacked(player, attacker, amount);
        }
    }
}