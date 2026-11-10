package dev.skzyy.zyelevators.elevator;

import dev.skzyy.zyelevators.ZyElevators;
import dev.skzyy.zyelevators.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/** One recipe per elevator block: 8 of the block around the center item. */
public final class RecipeManager implements Listener {

    private final ZyElevators plugin;
    private final ElevatorItems items;
    private final List<NamespacedKey> keys = new CopyOnWriteArrayList<>();

    public RecipeManager(ZyElevators plugin, ElevatorItems items) {
        this.plugin = plugin;
        this.items = items;
    }

    /** Must run on the global region thread on Folia (or during onEnable). */
    public void register() {
        unregister();
        Settings settings = plugin.settings();
        if (!settings.craftingEnabled()) return;

        for (Material block : settings.blocks()) {
            NamespacedKey key = new NamespacedKey(plugin, "elevator_" + block.getKey().getKey().toLowerCase(Locale.ROOT));
            ShapedRecipe recipe = new ShapedRecipe(key, items.create(block, settings.craftingAmount()));
            recipe.shape("BBB", "BCB", "BBB");
            recipe.setIngredient('B', block);
            recipe.setIngredient('C', settings.craftingCenter());
            recipe.setGroup("zyelevators");
            if (Bukkit.addRecipe(recipe)) {
                keys.add(key);
            }
        }
    }

    public void unregister() {
        for (NamespacedKey key : keys) {
            Bukkit.removeRecipe(key);
        }
        keys.clear();
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe instanceof Keyed keyed && keys.contains(keyed.getKey())
                && !event.getView().getPlayer().hasPermission("zyelevators.craft")) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!keys.isEmpty() && event.getPlayer().hasPermission("zyelevators.craft")) {
            event.getPlayer().discoverRecipes(List.copyOf(keys));
        }
    }
}
