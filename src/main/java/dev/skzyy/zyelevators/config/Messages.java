package dev.skzyy.zyelevators.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Messages from lang/&lt;language&gt;.yml, with the bundled English file as fallback for missing keys. */
public final class Messages {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final List<String> BUNDLED = List.of("en", "es");

    private final YamlConfiguration yaml;

    private Messages(YamlConfiguration yaml) {
        this.yaml = yaml;
    }

    public static void saveDefaults(JavaPlugin plugin) {
        for (String language : BUNDLED) {
            if (!new File(plugin.getDataFolder(), "lang/" + language + ".yml").exists()) {
                plugin.saveResource("lang/" + language + ".yml", false);
            }
        }
    }

    public static Messages load(JavaPlugin plugin, String language) {
        File file = new File(plugin.getDataFolder(), "lang/" + language + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("lang/" + language + ".yml not found, using English.");
            file = new File(plugin.getDataFolder(), "lang/en.yml");
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource("lang/en.yml")) {
            if (in != null) {
                yaml.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not read the bundled English messages: " + e.getMessage());
        }
        return new Messages(yaml);
    }

    public Component get(String key, TagResolver... resolvers) {
        return MINI.deserialize(raw(key), resolvers);
    }

    public List<Component> list(String key, TagResolver... resolvers) {
        return yaml.getStringList(key).stream()
                .map(line -> MINI.deserialize(line, resolvers))
                .toList();
    }

    /** Sends a chat message with the prefix. Empty messages are skipped. */
    public void send(CommandSender to, String key, TagResolver... resolvers) {
        String raw = raw(key);
        if (raw.isEmpty()) return;
        to.sendMessage(MINI.deserialize(yaml.getString("prefix", "") + raw, resolvers));
    }

    /** Sends an action bar message. Empty messages are skipped. */
    public void actionBar(Player to, String key, TagResolver... resolvers) {
        String raw = raw(key);
        if (raw.isEmpty()) return;
        to.sendActionBar(MINI.deserialize(raw, resolvers));
    }

    public void sendLines(CommandSender to, String key, TagResolver... resolvers) {
        list(key, resolvers).forEach(to::sendMessage);
    }

    private String raw(String key) {
        String value = yaml.getString(key);
        return value != null ? value : key;
    }
}
