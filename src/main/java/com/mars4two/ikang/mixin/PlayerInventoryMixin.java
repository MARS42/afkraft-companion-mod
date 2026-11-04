package com.mars4two.ikang.mixin;

import com.mars4two.ikang.core.snapshots.ingest.SnapshotIngestManager;
import com.mars4two.ikang.core.snapshots.model.RawItemStack;
import com.mars4two.ikang.core.util.InventoryView;
import com.mars4two.ikang.core.util.ItemStackMapper;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(net.minecraft.world.entity.player.Inventory.class)
public abstract class PlayerInventoryMixin {

    @Final
    @Shadow
    private NonNullList<ItemStack> items;

    @Final
    @Shadow
    private EntityEquipment equipment;

    @Final
    @Shadow
    public Player player;

    @Shadow
    public abstract int getSelectedSlot();

    @Unique
    private static final SnapshotIngestManager ingestManager = SnapshotIngestManager.getInstance();

    @Unique
    private final InventoryView afkraft_companion$inventoryView = new InventoryView() {
        @Override
        public UUID playerId() {
            return player.getGameProfile().id();
        }

        @Override
        public List<RawItemStack> hotbarItems() {
            List<RawItemStack> rawItems = new ArrayList<>(Inventory.SELECTION_SIZE);
            for (int i = 0; i < Inventory.SELECTION_SIZE; i++) {
                rawItems.add(ItemStackMapper.from(i, items.get(i)));
            }
            return rawItems;
        }

        @Override
        public List<RawItemStack> inventoryItems() {
            int size = Math.max(0, items.size() - Inventory.SELECTION_SIZE);
            List<RawItemStack> rawItems = new ArrayList<>(size);
            for (int i = Inventory.SELECTION_SIZE; i < items.size(); i++) {
                rawItems.add(ItemStackMapper.from(i, items.get(i)));
            }
            return rawItems;
        }

        @Override
        public List<RawItemStack> playerEquipment() {
            return List.of(
                    ItemStackMapper.from(0, equipment.get(EquipmentSlot.CHEST)),
                    ItemStackMapper.from(1, equipment.get(EquipmentSlot.HEAD)),
                    ItemStackMapper.from(2, equipment.get(EquipmentSlot.LEGS)),
                    ItemStackMapper.from(3, equipment.get(EquipmentSlot.FEET))
            );
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
            return getSelectedSlot();
        }
    };

    @Unique
    private boolean afkraft_companion$isServerSide() {
        return player != null && player.level() != null && !player.level().isClientSide();
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

    @Unique
    private void ingestSlotedItems(int slot) {
        if (!afkraft_companion$isServerSide()) {
            return;
        }
        if (Inventory.isHotbarSlot(slot)) {
            ingestManager.ingestPlayerHotbarState(afkraft_companion$inventoryView);
            return;
        }
        ingestManager.ingestPlayerInventoryState(afkraft_companion$inventoryView);
    }
}
