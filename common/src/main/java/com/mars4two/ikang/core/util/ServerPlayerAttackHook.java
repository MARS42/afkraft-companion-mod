package com.mars4two.ikang.core.util;

import net.minecraft.world.entity.Entity;

public interface ServerPlayerAttackHook {
    void afkraft_companion$onBeforeAttack(Entity target);
}
