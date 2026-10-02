package com.mars4two.ikang.mixin;

import com.mars4two.ikang.core.snapshots.ingest.SnapshotIngestManager;import com.mars4two.ikang.core.snapshots.model.RawItemStack;import com.mars4two.ikang.core.util.PlayerInventoryWrapper;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(net.minecraft.world.entity.player.Inventory.class)
public class PlayerInventoryMixin implements PlayerInventoryWrapper {

    @Final
    @Shadow
    private NonNullList<ItemStack> items;

    @Final
    @Shadow
    private EntityEquipment equipment;

    @Final
    @Shadow
    public Player player;

    @Unique
    private static final SnapshotIngestManager ingestManager = SnapshotIngestManager.getInstance();

    @Override
    public UUID afkraft_companion$getPlayerId() {
        return player.getGameProfile().id();
    }

    @Override
    public NonNullList<RawItemStack> afkraft_companion$getInventoryItems() {
        NonNullList<RawItemStack> rawItems = NonNullList.withSize(items.size(), RawItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            rawItems.set(i, RawItemStack.from(i, items.get(i)));
        }
        return rawItems;
    }

    @Override
    public NonNullList<RawItemStack> afkraft_companion$getPlayerEquipment() {
        NonNullList<RawItemStack> equipmentItems = NonNullList.withSize(4, RawItemStack.EMPTY);
        equipmentItems.set(0, RawItemStack.from(0, equipment.get(EquipmentSlot.CHEST)));
        equipmentItems.set(1, RawItemStack.from(1, equipment.get(EquipmentSlot.HEAD)));
        equipmentItems.set(2, RawItemStack.from(2, equipment.get(EquipmentSlot.LEGS)));
        equipmentItems.set(3, RawItemStack.from(3, equipment.get(EquipmentSlot.FEET)));
        return equipmentItems;
    }

    @Inject(method = "setItem", at = @At("TAIL"))
    private void onSetItem(int slot, ItemStack itemStack, CallbackInfo ci) {
        ingestManager.ingestPlayerInventoryState(this);
    }

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("TAIL"))
    private void onAdd(int slot, ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        ingestManager.ingestPlayerInventoryState(this);
    }

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("TAIL"))
    private void onAdd(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        ingestManager.ingestPlayerInventoryState(this);
    }

    @Inject(method = "removeItem(II)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void onRemove(int slot, int count, CallbackInfoReturnable<ItemStack> cir) {
        ingestManager.ingestPlayerInventoryState(this);
    }

    @Inject(method = "removeItem(Lnet/minecraft/world/item/ItemStack;)V", at = @At("TAIL"))
    private void onRemove(ItemStack itemStack, CallbackInfo ci) {
        ingestManager.ingestPlayerInventoryState(this);
    }

    @Inject(method = "dropAll", at = @At("TAIL"))
    private void onDropAll(CallbackInfo ci) {
        ingestManager.ingestPlayerInventoryState(this);
    }
}
