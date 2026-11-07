package com.mars4two.ikang.mixin;

//? if <1.21.5 {
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
 * mutates {@code Inventory.selected} directly instead of calling {@code Inventory.setSelectedSlot}.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class HotbarSelectionPacketMixin {

    //? if <1.21.5 {
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
