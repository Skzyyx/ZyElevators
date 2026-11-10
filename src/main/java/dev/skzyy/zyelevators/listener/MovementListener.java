package dev.skzyy.zyelevators.listener;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import dev.skzyy.zyelevators.ZyElevators;
import dev.skzyy.zyelevators.config.Settings;
import dev.skzyy.zyelevators.elevator.ElevatorService;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Jump to go up, sneak to go down. */
public final class MovementListener implements Listener {

    private static final int ARROW_TICKS = 20;

    private final ZyElevators plugin;
    private final ElevatorService elevators;
    // Folia fires these events on each player's region thread, so the map must be concurrent.
    private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();

    public MovementListener(ZyElevators plugin, ElevatorService elevators) {
        this.plugin = plugin;
        this.elevators = elevators;
    }

    @EventHandler(ignoreCancelled = true)
    public void onJump(PlayerJumpEvent event) {
        if (plugin.settings().jumpToGoUp()) {
            use(event.getPlayer(), true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking() && plugin.settings().sneakToGoDown()) {
            use(event.getPlayer(), false);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastUse.remove(event.getPlayer().getUniqueId());
    }

    private void use(Player player, boolean up) {
        Settings settings = plugin.settings();
        if (settings.isDisabledWorld(player.getWorld().getName())) return;

        Block from = standingOn(player, settings);
        if (from == null || !player.hasPermission("zyelevators.use")) return;

        long now = System.currentTimeMillis();
        if (!player.hasPermission("zyelevators.bypass.cooldown")) {
            Long last = lastUse.get(player.getUniqueId());
            long left = last == null ? 0 : settings.cooldownMillis() - (now - last);
            if (left > 0) {
                plugin.messages().actionBar(player, "cooldown",
                        Placeholder.unparsed("seconds", String.format(Locale.ROOT, "%.1f", left / 1000.0)));
                return;
            }
        }

        Block destination = elevators.findDestination(from, up, settings);
        if (destination == null) {
            plugin.messages().actionBar(player, "no-destination");
            return;
        }

        Location current = player.getLocation();
        Location target = destination.getLocation().add(0.5, 1.0, 0.5);
        target.setYaw(current.getYaw());
        target.setPitch(current.getPitch());
        Vector facing = current.getDirection().setY(0);

        lastUse.put(player.getUniqueId(), now);
        player.teleportAsync(target).thenAccept(success -> {
            if (success) effects(destination.getLocation(), facing, up, settings);
        });
    }

    private Block standingOn(Player player, Settings settings) {
        Location location = player.getLocation();
        Block below = location.clone().subtract(0, 0.1, 0).getBlock();
        if (elevators.isElevator(below, settings)) return below;
        below = location.getBlock().getRelative(BlockFace.DOWN);
        return elevators.isElevator(below, settings) ? below : null;
    }

    private void effects(Location block, Vector facing, boolean up, Settings settings) {
        World world = block.getWorld();
        if (world == null) return;
        double x = block.getX() + 0.5;
        double y = block.getY() + 1.2;
        double z = block.getZ() + 0.5;

        if (settings.soundEnabled()) {
            Sound sound = Sound.sound(settings.soundKey(), Sound.Source.PLAYER, settings.soundVolume(), settings.soundPitch());
            Bukkit.getRegionScheduler().execute(plugin, block, () -> world.playSound(sound, x, y, z));
        }
        if (!settings.particlesEnabled()) return;

        // Arrow drawn across the player's view, so it always faces them.
        Vector dir = facing.lengthSquared() == 0 ? new Vector(0, 0, 1) : facing.clone().normalize();
        Vector right = dir.crossProduct(new Vector(0, 1, 0)).normalize();
        Color color = up ? settings.upColor() : settings.downColor();
        Particle.DustOptions dust = new Particle.DustOptions(color, 0.5F);

        int[] ticks = {0};
        Bukkit.getRegionScheduler().runAtFixedRate(plugin, block, task -> {
            if (ticks[0] > ARROW_TICKS) {
                task.cancel();
                return;
            }
            for (double dy = 0.2; dy <= 1.1; dy += 0.05) {
                world.spawnParticle(Particle.DUST, x, y + dy, z, 1, 0, 0, 0, 0, dust, true);
            }
            for (double d = 0; d <= 0.25; d += 0.02) {
                double dy = up ? 1.1 - d : 0.2 + d;
                world.spawnParticle(Particle.DUST, x + right.getX() * d, y + dy, z + right.getZ() * d, 1, 0, 0, 0, 0, dust, true);
                world.spawnParticle(Particle.DUST, x - right.getX() * d, y + dy, z - right.getZ() * d, 1, 0, 0, 0, 0, dust, true);
            }
            ticks[0] += 2;
        }, 1L, 2L);
    }
}
