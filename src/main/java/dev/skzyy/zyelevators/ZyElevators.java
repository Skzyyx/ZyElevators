package dev.skzyy.zyelevators;

import dev.skzyy.zyelevators.command.ElevatorsCommand;
import dev.skzyy.zyelevators.config.Messages;
import dev.skzyy.zyelevators.config.Settings;
import dev.skzyy.zyelevators.elevator.ElevatorItems;
import dev.skzyy.zyelevators.elevator.ElevatorService;
import dev.skzyy.zyelevators.elevator.ElevatorStore;
import dev.skzyy.zyelevators.elevator.RecipeManager;
import dev.skzyy.zyelevators.listener.BlockListener;
import dev.skzyy.zyelevators.listener.MovementListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZyElevators extends JavaPlugin {

    // Swapped as a whole on reload, read from every region thread.
    private volatile Settings settings;
    private volatile Messages messages;

    private RecipeManager recipes;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Messages.saveDefaults(this);
        loadFiles();

        ElevatorItems items = new ElevatorItems(this);
        ElevatorService elevators = new ElevatorService(new ElevatorStore(this));
        recipes = new RecipeManager(this, items);
        recipes.register();

        PluginManager plugins = getServer().getPluginManager();
        plugins.registerEvents(new MovementListener(this, elevators), this);
        plugins.registerEvents(new BlockListener(this, elevators, items), this);
        plugins.registerEvents(recipes, this);

        PluginCommand command = getCommand("zyelevators");
        if (command != null) {
            ElevatorsCommand executor = new ElevatorsCommand(this, items);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
        if (recipes == null) return;
        try {
            recipes.unregister();
        } catch (RuntimeException e) {
            // Some servers don't allow recipe changes while shutting down; they are rebuilt on start anyway.
            getLogger().fine("Could not remove recipes on shutdown: " + e.getMessage());
        }
    }

    /** Reloads config.yml and the language file, then rebuilds the recipes. */
    public void reload() {
        loadFiles();
        // Recipes are global state: on Folia they must change on the global region thread.
        Bukkit.getGlobalRegionScheduler().execute(this, recipes::register);
    }

    private void loadFiles() {
        reloadConfig();
        settings = Settings.load(getConfig(), getLogger());
        messages = Messages.load(this, settings.language());
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }
}
