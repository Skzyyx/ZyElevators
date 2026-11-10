package dev.skzyy.zyelevators.elevator;

import dev.skzyy.zyelevators.config.Settings;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/** Finds elevators and their destinations. Must run on the region thread that owns the blocks. */
public final class ElevatorService {

    private final ElevatorStore store;

    public ElevatorService(ElevatorStore store) {
        this.store = store;
    }

    public ElevatorStore store() {
        return store;
    }

    /** A configured elevator block that was placed from an elevator item. */
    public boolean isElevator(Block block, Settings settings) {
        return block != null && settings.isElevatorBlock(block.getType()) && store.isMarked(block);
    }

    /**
     * The nearest usable elevator above or below {@code from} in the same column,
     * or {@code null} if there is none within {@code max-distance}.
     */
    public Block findDestination(Block from, boolean up, Settings settings) {
        int[] levels = store.columnLevels(from);
        World world = from.getWorld();
        int fromY = from.getY();

        if (up) {
            for (int y : levels) {
                if (y <= fromY) continue;
                if (y - fromY > settings.maxDistance()) break;
                Block candidate = usable(world.getBlockAt(from.getX(), y, from.getZ()), from.getType(), settings);
                if (candidate != null) return candidate;
            }
        } else {
            for (int i = levels.length - 1; i >= 0; i--) {
                int y = levels[i];
                if (y >= fromY) continue;
                if (fromY - y > settings.maxDistance()) break;
                Block candidate = usable(world.getBlockAt(from.getX(), y, from.getZ()), from.getType(), settings);
                if (candidate != null) return candidate;
            }
        }
        return null;
    }

    private Block usable(Block candidate, Material fromType, Settings settings) {
        Material type = candidate.getType();
        if (!settings.isElevatorBlock(type)) {
            // The block was replaced by something else (WorldEdit, another plugin...): forget it.
            store.unmark(candidate);
            return null;
        }
        if (settings.sameBlockOnly() && type != fromType) return null;
        if (settings.safeDestination()
                && !(isFree(candidate.getRelative(BlockFace.UP)) && isFree(candidate.getRelative(0, 2, 0)))) {
            return null;
        }
        return candidate;
    }

    private static boolean isFree(Block block) {
        Material type = block.getType();
        return !type.isSolid() && type != Material.LAVA && type != Material.WATER;
    }
}
