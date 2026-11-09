package com.mars4two.ikang.core.util;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

public interface ServerPlayerAttackHook {
    void afkraft_companion$onBeforeAttack(Entity target);

    void afkraft_companion$onBeforeSwing(InteractionHand hand);
}
