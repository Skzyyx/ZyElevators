package dev.skzyy.zyelevators.elevator;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;

/**
 * Remembers which blocks are elevators. Positions are stored in the chunk's
 * PersistentDataContainer, so any block type works (not only tile entities),
 * the data is saved with the world, and there is no file or database to manage.
 *
 * <p>Every method must run on the thread that owns the block's region (Folia),
 * which is always the case inside block and player events.
 */
public final class ElevatorStore {

    private static final int Y_OFFSET = 2048;

    private final NamespacedKey key;

    public ElevatorStore(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "elevators");
    }

    public boolean isMarked(Block block) {
        int packed = pack(block);
        for (int value : read(block.getChunk())) {
            if (value == packed) return true;
        }
        return false;
    }

    public void mark(Block block) {
        Chunk chunk = block.getChunk();
        int[] current = read(chunk);
        int packed = pack(block);
        for (int value : current) {
            if (value == packed) return;
        }
        int[] updated = Arrays.copyOf(current, current.length + 1);
        updated[current.length] = packed;
        chunk.getPersistentDataContainer().set(key, PersistentDataType.INTEGER_ARRAY, updated);
    }

    public void unmark(Block block) {
        Chunk chunk = block.getChunk();
        int[] current = read(chunk);
        int packed = pack(block);
        int[] updated = Arrays.stream(current).filter(value -> value != packed).toArray();
        if (updated.length == current.length) return;
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (updated.length == 0) {
            pdc.remove(key);
        } else {
            pdc.set(key, PersistentDataType.INTEGER_ARRAY, updated);
        }
    }

    /** Y levels of every elevator in the same column (same X and Z) as the block, sorted from bottom to top. */
    public int[] columnLevels(Block block) {
        int column = ((block.getZ() & 15) << 4) | (block.getX() & 15);
        return Arrays.stream(read(block.getChunk()))
                .filter(value -> (value & 0xFF) == column)
                .map(value -> (value >>> 8) - Y_OFFSET)
                .sorted()
                .toArray();
    }

    private int[] read(Chunk chunk) {
        int[] values = chunk.getPersistentDataContainer().get(key, PersistentDataType.INTEGER_ARRAY);
        return values != null ? values : new int[0];
    }

    /** 12 bits of Y (offset to stay positive), 4 bits of Z and 4 bits of X inside the chunk. */
    private static int pack(Block block) {
        return ((block.getY() + Y_OFFSET) << 8) | ((block.getZ() & 15) << 4) | (block.getX() & 15);
    }
}
