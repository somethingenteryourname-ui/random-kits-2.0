package com.customabilities;

import org.bukkit.block.Block;

/** Turns a block position into a simple string so it can be stored in sets and files. */
public final class BlockKey {

    private BlockKey() {}

    public static String of(Block block) {
        return block.getWorld().getName() + ";" + block.getX() + ";" + block.getY() + ";" + block.getZ();
    }
}
