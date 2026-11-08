package com.mars4two.ikang.mixin;

//? if >=26.3 {
import com.mars4two.ikang.core.util.ServerPlayerAttackHook;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?} elif <1.21.5 {
/*import com.mars4two.ikang.core.util.PlayerHotbarSyncHook;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Captures hotbar slot changes on Minecraft 1.21.1–1.21.4 where {@code ServerGamePacketListenerImpl}
 * mutates {@code Inventory.selected} directly instead of calling {@code Inventory.setSelectedSlot},
 * and air/block punch swings on Minecraft 26.3+ where {@code handlePunch} replaces {@code ServerPlayer.swing(InteractionHand)}.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class HotbarSelectionPacketMixin {

    //? if >=26.3 {
    @Shadow
    public ServerPlayer player;

    @Inject(
            method = "handlePunch",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;resetAttackStrengthTicker()V"
            )
    )
    private void onBeforePunchResetTicker(ServerboundPunchPacket packet, CallbackInfo ci) {
        if (this.player instanceof ServerPlayerAttackHook hook) {
            hook.afkraft_companion$onBeforeSwing(InteractionHand.MAIN_HAND);
        }
    }
    //?} elif <1.21.5 {
    /*@Shadow
    public ServerPlayer player;

    @Inject(method = "handleSetCarriedItem", at = @At("TAIL"))
    private void onAfterSetCarriedItem(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
        if (this.player != null
                && this.player.getInventory().selected == packet.getSlot()
                && this.player.getInventory() instanceof PlayerHotbarSyncHook hook) {
            hook.afkraft_companion$syncHotbarSelection();
        }
    }
    *///?}
}
