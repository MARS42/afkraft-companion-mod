package com.mars4two.ikang.mixin;

import com.mars4two.ikang.core.util.ServerPlayerAttackHook;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void afkraft_companion$onBeforeAttack(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide() && (Object) this instanceof ServerPlayerAttackHook hook) {
            hook.afkraft_companion$onBeforeAttack(target);
        }
    }
}
