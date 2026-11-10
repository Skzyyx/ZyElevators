package dev.skzyy.zyelevators.listener;

import dev.skzyy.zyelevators.ZyElevators;
import dev.skzyy.zyelevators.config.Settings;
import dev.skzyy.zyelevators.elevator.ElevatorItems;
import dev.skzyy.zyelevators.elevator.ElevatorService;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.HopperInventorySearchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Iterator;
import java.util.List;

/** Placing, breaking and protecting elevator blocks. */
public final class BlockListener implements Listener {

    private final ZyElevators plugin;
    private final ElevatorService elevators;
    private final ElevatorItems items;

    public BlockListener(ZyElevators plugin, ElevatorService elevators, ElevatorItems items) {
        this.plugin = plugin;
        this.elevators = elevators;
        this.items = items;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlacePermission(BlockPlaceEvent event) {
        if (items.isElevator(event.getItemInHand()) && !event.getPlayer().hasPermission("zyelevators.place")) {
            event.setCancelled(true);
            plugin.messages().send(event.getPlayer(), "cannot-place");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (items.isElevator(event.getItemInHand()) && plugin.settings().isElevatorBlock(block.getType())) {
            elevators.store().mark(block);
            faceUp(block);
        } else if (elevators.store().isMarked(block)) {
            // A normal block took the place of an elevator that was removed without an event.
            elevators.store().unmark(block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Settings settings = plugin.settings();
        if (!elevators.isElevator(block, settings)) return;

        Material type = block.getType();
        elevators.store().unmark(block);
        if (event.isDropItems() && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            event.setDropItems(false);
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), items.create(type, 1));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private void handleExplosion(List<Block> blocks) {
        Settings settings = plugin.settings();
        Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (!elevators.isElevator(block, settings)) continue;
            iterator.remove();
            if (settings.explosionProof()) continue;

            // Break it ourselves so it drops the elevator item instead of a plain block.
            Material type = block.getType();
            elevators.store().unmark(block);
            block.setType(Material.AIR);
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), items.create(type, 1));
        }
    }

    // Elevators are remembered by position, so they must not be moved.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (containsElevator(event.getBlocks())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (containsElevator(event.getBlocks())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (elevators.isElevator(event.getBlock(), plugin.settings())) event.setCancelled(true);
    }

    // Endermen, withers, falling blocks...
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (elevators.isElevator(event.getBlock(), plugin.settings())) event.setCancelled(true);
    }

    // Container elevators (shulker boxes, barrels...) can't be opened.
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || !plugin.settings().isElevatorBlock(block.getType())) return;
        if (block.getState(false) instanceof InventoryHolder && elevators.isElevator(block, plugin.settings())) {
            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    // ...and hoppers can't pull from or push into them.
    @EventHandler
    public void onHopperSearch(HopperInventorySearchEvent event) {
        Block block = event.getSearchBlock();
        if (block != null && elevators.isElevator(block, plugin.settings())) {
            event.setInventory(null);
        }
    }

    private boolean containsElevator(List<Block> blocks) {
        Settings settings = plugin.settings();
        for (Block block : blocks) {
            if (elevators.isElevator(block, settings)) return true;
        }
        return false;
    }

    private static void faceUp(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Directional directional && directional.getFaces().contains(BlockFace.UP)) {
            directional.setFacing(BlockFace.UP);
            block.setBlockData(directional, false);
        }
    }
}
