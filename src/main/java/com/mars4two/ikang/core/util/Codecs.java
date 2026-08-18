package com.mars4two.ikang.core.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;

import java.util.UUID;

public class Codecs {
    public static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public static final Codec<BlockPos> STRING_BLOCK_POS_CODEC = Codec.STRING.xmap(s -> {
        String[] split = s.split(",");
        return new BlockPos(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
    }, blockPos -> String.format("%d,%d,%d", blockPos.getX(), blockPos.getY(), blockPos.getZ()));

    private Codecs(){
        // Utility class, no instances allowed
    }
}
