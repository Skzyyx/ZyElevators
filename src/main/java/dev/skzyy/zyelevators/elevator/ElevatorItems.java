package dev.skzyy.zyelevators.elevator;

import dev.skzyy.zyelevators.ZyElevators;
import dev.skzyy.zyelevators.config.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** Creates and recognizes elevator items. */
public final class ElevatorItems {

    private final ZyElevators plugin;
    private final NamespacedKey key;

    public ElevatorItems(ZyElevators plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "elevator");
    }

    public ItemStack create(Material block, int amount) {
        ItemStack item = new ItemStack(block, amount);
        Messages messages = plugin.messages();
        TagResolver blockName = blockName(block);
        item.editMeta(meta -> {
            meta.displayName(plain(messages.get("item.name", blockName)));
            meta.lore(messages.list("item.lore", blockName).stream().map(ElevatorItems::plain).toList());
            if (plugin.settings().glint()) {
                meta.setEnchantmentGlintOverride(true);
            }
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        });
        return item;
    }

    public boolean isElevator(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    /** {@code <block>} placeholder shown in each player's own game language. */
    public static TagResolver blockName(Material block) {
        return Placeholder.component("block", Component.translatable(block.translationKey()));
    }

    private static Component plain(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
