package dev.skzyy.zyelevators.command;

import dev.skzyy.zyelevators.ZyElevators;
import dev.skzyy.zyelevators.config.Messages;
import dev.skzyy.zyelevators.elevator.ElevatorItems;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** /zyelevators give | reload | help */
public final class ElevatorsCommand implements TabExecutor {

    private static final String ADMIN = "zyelevators.admin";
    private static final int MAX_AMOUNT = 64 * 36;

    private final ZyElevators plugin;
    private final ElevatorItems items;

    public ElevatorsCommand(ZyElevators plugin, ElevatorItems items) {
        this.plugin = plugin;
        this.items = items;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.messages();
        if (!sender.hasPermission(ADMIN)) {
            messages.send(sender, "no-permission");
            return true;
        }
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "give" -> give(sender, args);
            case "reload" -> {
                plugin.reload();
                plugin.messages().send(sender, "reloaded");
            }
            default -> messages.sendLines(sender, "help",
                    Placeholder.unparsed("version", plugin.getPluginMeta().getVersion()));
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        Messages messages = plugin.messages();
        if (args.length < 3) {
            messages.sendLines(sender, "help", Placeholder.unparsed("version", plugin.getPluginMeta().getVersion()));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messages.send(sender, "player-not-found", Placeholder.unparsed("player", args[1]));
            return;
        }

        Material block = Material.matchMaterial(args[2]);
        if (block == null || !plugin.settings().isElevatorBlock(block)) {
            messages.send(sender, "invalid-block",
                    Placeholder.unparsed("input", args[2]),
                    Placeholder.unparsed("blocks", String.join(", ", blockNames())));
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                amount = -1;
            }
        }
        if (amount < 1 || amount > MAX_AMOUNT) {
            messages.send(sender, "invalid-amount", Placeholder.unparsed("max", String.valueOf(MAX_AMOUNT)));
            return;
        }

        ItemStack template = items.create(block, 1);
        int total = amount;
        // Folia: the player's inventory belongs to the player's region thread.
        target.getScheduler().run(plugin, task -> {
            int left = total;
            while (left > 0) {
                ItemStack stack = template.clone();
                stack.setAmount(Math.min(left, template.getMaxStackSize()));
                left -= stack.getAmount();
                target.getInventory().addItem(stack).values()
                        .forEach(rest -> target.getWorld().dropItemNaturally(target.getLocation(), rest));
            }
        }, null);

        messages.send(sender, "given",
                Placeholder.unparsed("amount", String.valueOf(amount)),
                ElevatorItems.blockName(block),
                Placeholder.unparsed("player", target.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(ADMIN)) return List.of();
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(List.of("give", "reload", "help"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            Bukkit.getOnlinePlayers().forEach(player -> options.add(player.getName()));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            options.addAll(blockNames());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            options.addAll(List.of("1", "16", "64"));
        }
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }

    private List<String> blockNames() {
        return plugin.settings().blocks().stream()
                .map(material -> material.getKey().getKey())
                .sorted()
                .collect(Collectors.toList());
    }
}
