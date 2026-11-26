package com.mars4two.ikang.mixin;

import com.mars4two.ikang.core.util.PlayerHotbarSyncHook;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.21.2 {
import net.minecraft.world.entity.player.Player;
//?}
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
            at = @At("TAIL")
    )
    private void afkraft_companion$onHurtAndBreak(
            int amount,
            ServerLevel level,
            ServerPlayer player,
            Consumer<?> onBreak,
            CallbackInfo ci
    ) {
        if (player != null && player.getInventory() instanceof PlayerHotbarSyncHook hook) {
            hook.afkraft_companion$syncEquipmentAndHotbar();
        }
    }

    //? if >=1.21.2 {
    @Inject(method = "hurtWithoutBreaking", at = @At("TAIL"))
    private void afkraft_companion$onHurtWithoutBreaking(int amount, Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer
                && serverPlayer.getInventory() instanceof PlayerHotbarSyncHook hook) {
            hook.afkraft_companion$syncEquipmentAndHotbar();
        }
    }
    //?}
}
