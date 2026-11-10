package dev.skzyy.zyelevators.config;

import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Immutable snapshot of config.yml. A reload builds a new instance and swaps it,
 * so region threads never see a half-loaded config.
 */
public record Settings(
        String language,
        Set<Material> blocks,
        boolean sameBlockOnly,
        int maxDistance,
        long cooldownMillis,
        boolean jumpToGoUp,
        boolean sneakToGoDown,
        boolean safeDestination,
        boolean explosionProof,
        Set<String> disabledWorlds,
        boolean glint,
        boolean craftingEnabled,
        Material craftingCenter,
        int craftingAmount,
        boolean soundEnabled,
        Key soundKey,
        float soundVolume,
        float soundPitch,
        boolean particlesEnabled,
        Color upColor,
        Color downColor
) {

    private static final Key DEFAULT_SOUND = Key.key("entity.enderman.teleport");

    public static Settings load(FileConfiguration config, Logger logger) {
        ConfigurationSection elevator = section(config, "elevator");
        ConfigurationSection crafting = section(config, "crafting");
        ConfigurationSection sound = section(config, "effects.sound");
        ConfigurationSection particles = section(config, "effects.particles");

        Set<Material> blocks = EnumSet.noneOf(Material.class);
        for (String name : elevator.getStringList("blocks")) {
            Material material = Material.matchMaterial(name);
            if (material == null || !material.isBlock() || !material.isSolid()) {
                logger.warning("Ignoring elevator block '" + name + "': it must be a solid block.");
                continue;
            }
            blocks.add(material);
        }
        if (blocks.isEmpty()) {
            logger.warning("No valid elevator blocks configured, using WHITE_WOOL.");
            blocks.add(Material.WHITE_WOOL);
        }

        Material center = Material.matchMaterial(crafting.getString("center", "ENDER_PEARL"));
        if (center == null || !center.isItem() || center.isAir()) {
            logger.warning("Invalid crafting center item, using ENDER_PEARL.");
            center = Material.ENDER_PEARL;
        }

        Set<String> disabledWorlds = new HashSet<>();
        for (String world : elevator.getStringList("disabled-worlds")) {
            disabledWorlds.add(world.toLowerCase(Locale.ROOT));
        }

        return new Settings(
                config.getString("language", "en"),
                Collections.unmodifiableSet(blocks),
                elevator.getBoolean("same-block-only", true),
                clamp(elevator.getInt("max-distance", 64), 2, 4064),
                Math.max(0L, Math.round(elevator.getDouble("cooldown", 1.0) * 1000.0)),
                elevator.getBoolean("jump-to-go-up", true),
                elevator.getBoolean("sneak-to-go-down", true),
                elevator.getBoolean("safe-destination", true),
                elevator.getBoolean("explosion-proof", true),
                Collections.unmodifiableSet(disabledWorlds),
                config.getBoolean("item.glint", true),
                crafting.getBoolean("enabled", true),
                center,
                clamp(crafting.getInt("amount", 1), 1, 64),
                sound.getBoolean("enabled", true),
                parseKey(sound.getString("key"), logger),
                (float) sound.getDouble("volume", 0.5),
                (float) sound.getDouble("pitch", 1.2),
                particles.getBoolean("enabled", true),
                parseColor(particles.getString("up-color"), Color.LIME, logger),
                parseColor(particles.getString("down-color"), Color.RED, logger)
        );
    }

    public boolean isElevatorBlock(Material material) {
        return blocks.contains(material);
    }

    public boolean isDisabledWorld(String worldName) {
        return disabledWorlds.contains(worldName.toLowerCase(Locale.ROOT));
    }

    private static ConfigurationSection section(FileConfiguration config, String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        return section != null ? section : config.createSection(path);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Key parseKey(String raw, Logger logger) {
        if (raw == null || raw.isBlank()) return DEFAULT_SOUND;
        try {
            return Key.key(raw.toLowerCase(Locale.ROOT));
        } catch (InvalidKeyException e) {
            logger.warning("Invalid sound key '" + raw + "', using " + DEFAULT_SOUND.asString() + ".");
            return DEFAULT_SOUND;
        }
    }

    private static Color parseColor(String raw, Color fallback, Logger logger) {
        if (raw == null || raw.isBlank()) return fallback;
        String hex = raw.startsWith("#") ? raw.substring(1) : raw;
        try {
            return Color.fromRGB(Integer.parseInt(hex, 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            logger.warning("Invalid color '" + raw + "', it must look like #55FF55.");
            return fallback;
        }
    }
}
