package com.mars4two.ikang.mixin;

import com.mars4two.ikang.core.snapshots.ingest.SnapshotIngestManager;
import com.mars4two.ikang.core.snapshots.model.RawItemStack;
import com.mars4two.ikang.core.util.InventoryView;
import com.mars4two.ikang.core.util.PlayerHotbarSyncHook;
import com.mars4two.ikang.platform.mc.InventoryEquipmentAdapter;
import com.mars4two.ikang.platform.mc.WorldAndEntityAdapter;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

@Mixin(net.minecraft.world.entity.player.Inventory.class)
public abstract class PlayerInventoryMixin implements PlayerHotbarSyncHook {

    //? if >=26.2 {
    @Final
    @Shadow
    private NonNullList<ItemStack> items;
    //?} else {
    /*@Final
    @Shadow
    public NonNullList<ItemStack> items;
    *///?}

    @Final
    @Shadow
    public Player player;

    @Unique
    private static final SnapshotIngestManager ingestManager = SnapshotIngestManager.getInstance();

    @Unique
    private final InventoryView afkraft_companion$inventoryView = new InventoryView() {
        @Override
        public UUID playerId() {
            return WorldAndEntityAdapter.playerId(player);
        }

        @Override
        public List<RawItemStack> hotbarItems() {
            return InventoryEquipmentAdapter.extractHotbarItems(items);
        }

        @Override
        public List<RawItemStack> inventoryItems() {
            return InventoryEquipmentAdapter.extractMainInventoryItems(items);
        }

        @Override
        public List<RawItemStack> playerEquipment() {
            return InventoryEquipmentAdapter.extractArmorEquipment(player);
        }

        @Override
        public int armorValue() {
            if (afkraft_companion$isServerSide() && player instanceof LivingEntityInvoker invoker) {
                invoker.afkraft_companion$detectEquipmentUpdates();
            }
            return player != null ? player.getArmorValue() : 0;
        }

        @Override
        public int selectedSlot() {
            return InventoryEquipmentAdapter.getSelectedHotbarSlot((Inventory) (Object) PlayerInventoryMixin.this);
        }
    };

    @Unique
    private boolean afkraft_companion$isServerSide() {
        return player != null && player.level() != null && !player.level().isClientSide();
    }

    @Override
    public void afkraft_companion$syncHotbarSelection() {
        if (!afkraft_companion$isServerSide()) {
            return;
        }
        ingestManager.ingestPlayerHotbarState(afkraft_companion$inventoryView);
    }

    @Inject(method = "setItem", at = @At("TAIL"))
    private void onSetItem(int slot, ItemStack itemStack, CallbackInfo ci) {
        if (!afkraft_companion$isServerSide()) return;
        ingestSlotedItems(slot);
    }

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("TAIL"))
    private void onAdd(int slot, ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (!afkraft_companion$isServerSide()) return;
        ingestSlotedItems(slot);
    }

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("TAIL"))
    private void onAdd(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (!afkraft_companion$isServerSide()) return;
        ingestManager.ingestPlayerInventoryState(afkraft_companion$inventoryView);
    }

    @Inject(method = "removeItem(II)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void onRemove(int slot, int count, CallbackInfoReturnable<ItemStack> cir) {
        if (!afkraft_companion$isServerSide()) return;
        ingestSlotedItems(slot);
    }

    @Inject(method = "removeItem(Lnet/minecraft/world/item/ItemStack;)V", at = @At("TAIL"))
    private void onRemove(ItemStack itemStack, CallbackInfo ci) {
        if (!afkraft_companion$isServerSide()) return;
        ingestManager.ingestPlayerInventoryState(afkraft_companion$inventoryView);
    }

    @Inject(method = "dropAll", at = @At("TAIL"))
    private void onDropAll(CallbackInfo ci) {
        if (!afkraft_companion$isServerSide()) return;
        ingestManager.ingestPlayerInventoryState(afkraft_companion$inventoryView);
    }

    //? if >=26.2 {
    @Inject(method = "setSelectedSlot", at = @At("TAIL"))
    private void onSetSelectedSlot(int slot, CallbackInfo ci) {
        if (!afkraft_companion$isServerSide()) return;
        ingestSlotedItems(slot);
    }

    @Inject(method = "setSelectedItem", at = @At("TAIL"))
    private void onSetSelectedItem(ItemStack itemStack, CallbackInfoReturnable<ItemStack> cir) {
        if (!afkraft_companion$isServerSide()) return;
        ingestManager.ingestPlayerHotbarState(afkraft_companion$inventoryView);
    }
    //?} else {
    /*@Inject(method = "pickSlot", at = @At("TAIL"))
    private void onPickSlot(int slot, CallbackInfo ci) {
        afkraft_companion$syncHotbarSelection();
    }

    @Inject(method = "setPickedItem", at = @At("TAIL"))
    private void onSetPickedItem(ItemStack itemStack, CallbackInfo ci) {
        afkraft_companion$syncHotbarSelection();
    }
    *///?}

    @Unique
    private void ingestSlotedItems(int slot) {
        if (!afkraft_companion$isServerSide()) {
            return;
        }
        if (InventoryEquipmentAdapter.isHotbarSlot(slot)) {
            ingestManager.ingestPlayerHotbarState(afkraft_companion$inventoryView);
            return;
        }
        ingestManager.ingestPlayerInventoryState(afkraft_companion$inventoryView);
    }
}
