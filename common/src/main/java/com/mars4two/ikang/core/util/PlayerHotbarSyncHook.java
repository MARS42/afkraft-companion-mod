package com.mars4two.ikang.core.util;

public interface PlayerHotbarSyncHook {
    void afkraft_companion$syncHotbarSelection();
    void afkraft_companion$syncEquipmentAndHotbar();
    void afkraft_companion$syncSlotChange(int slot);
}
