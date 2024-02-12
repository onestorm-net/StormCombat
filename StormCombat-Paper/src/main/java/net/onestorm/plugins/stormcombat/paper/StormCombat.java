package net.onestorm.plugins.stormcombat.paper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class StormCombat extends JavaPlugin implements Listener {

    private static final long COMBAT_TAG_TIME = 15L;
    private final Map<EnderCrystal, Player> crystalMap = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledFuture<?>> taskMap = new ConcurrentHashMap<>();
    private final Set<UUID> tagged = new HashSet<>();

    private ScheduledExecutorService executor;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void onDisable() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damagerEntity = event.getDamager();
        Entity defenderEntity = event.getEntity();

        if (damagerEntity.getType() == EntityType.ENDER_PEARL || defenderEntity.getType() == EntityType.ENDER_PEARL) {
            return;
        }

        Player damager = null;
        if (damagerEntity instanceof Player player) {
            damager = player;
        } else if (damagerEntity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            damager = player;
        } else if (damagerEntity instanceof AreaEffectCloud cloud && cloud.getSource() instanceof Player player) {
            damager = player;
        } else if (damagerEntity instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) {
            damager = player;
        }

        Player defender = null;
        if (defenderEntity instanceof Player player && !player.hasMetadata("NPC")) { // player but not a npc
            defender = player;
        } else if (defenderEntity instanceof EnderCrystal defenderCrystal) {
            if (damager != null) {
                crystalMap.put(defenderCrystal, damager);
            } else if (damagerEntity instanceof EnderCrystal damagerCrystal) {
                Player player = crystalMap.remove(damagerCrystal);
                if (player == null) {
                    return;
                }
                crystalMap.put(defenderCrystal, player);
            }
        }

        if (damagerEntity instanceof EnderCrystal damagerCrystal) {
            Player player = crystalMap.remove(damagerCrystal);

            if (player == null) {
                return;
            }

            damager = player;
        }

        if (damager == null || defender == null || damager == defender) {
            return;
        }

        getServer().broadcast(Component.text("[Debug] " + damager.getType() + " -> " + defender.getType()));

        combatTag(damager);
        combatTag(defender);
    }

    private void combatTag(Player player) {
        UUID uuid = player.getUniqueId();
        if (tagged.add(uuid)) {
            player.sendMessage(Component.text("You are now combat tagged", NamedTextColor.RED));
        } else {
            player.sendMessage(Component.text("[Debug] already tagged", NamedTextColor.RED));
        }

        ScheduledFuture<?> future = executor.schedule(() -> {
            getServer().getScheduler().runTask(this, () -> {
                if (tagged.remove(uuid)) {
                    player.sendMessage(Component.text("You are no longer in combat", NamedTextColor.GREEN));
                }
            });
        }, COMBAT_TAG_TIME, TimeUnit.SECONDS);

        // remove old
        ScheduledFuture<?> oldFuture = taskMap.remove(uuid);
        if (oldFuture != null) {
            oldFuture.cancel(true);
        }
        // put new
        taskMap.put(uuid, future);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        ScheduledFuture<?> oldFuture = taskMap.remove(uuid);
        if (oldFuture != null) {
            oldFuture.cancel(true);
        }
        if (tagged.remove(uuid)) {
            player.sendMessage(Component.text("You are no longer in combat", NamedTextColor.GREEN));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        ScheduledFuture<?> oldFuture = taskMap.remove(uuid);
        if (oldFuture != null) {
            getLogger().info("cancel start");
            long start = System.currentTimeMillis();
            oldFuture.cancel(true);
            long duration = System.currentTimeMillis() - start;
            getLogger().info("cancel stop: " + duration + "ms");
        }
        if (tagged.remove(uuid)) {
            getServer().broadcast(Component.text(player.getName() + " left while combat tagged!", NamedTextColor.RED));
            getLogger().info(player.getName() + " left while combat tagged!");
        }
    }

}