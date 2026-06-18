package com.aermini.bwagain.listeners;

import com.aermini.bwagain.AerBedwarsAgain;
import com.aermini.bwagain.menu.GameEndMenu;
import io.github.bedwarsrel.BedwarsRel;
import io.github.bedwarsrel.events.BedwarsGameOverEvent;
import io.github.bedwarsrel.game.Game;
import io.github.bedwarsrel.game.GameState;
import io.github.bedwarsrel.game.Team;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class BedwarsListener implements Listener {
    private final AerBedwarsAgain plugin;
    private final GameEndMenu menu;

    private final Set<UUID> openedMenuPlayers = new HashSet<>();
    private final Set<UUID> pendingMenuPlayers = new HashSet<>();
    private final Set<UUID> forceOpenPlayers = new HashSet<>();

    public BedwarsListener(AerBedwarsAgain plugin) {
        this.plugin = plugin;
        this.menu = new GameEndMenu(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Game game = BedwarsRel.getInstance().getGameManager().getGameOfPlayer(player);
        if (game == null) return;
        if (game.getState() != GameState.RUNNING) return;

        Team team = game.getPlayerTeam(player);
        if (team == null) return;
        if (!team.isDead(game)) return;

        giveAgainItem(player, team);
        openMenuDelayed(player, getDelayTicks());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameEnd(BedwarsGameOverEvent event) {
        Game game = event.getGame();
        long delayTicks = getDelayTicks();

        for (Player player : game.getPlayers()) {
            if (player == null || !player.isOnline()) continue;
            if (game.isSpectator(player)) continue;

            Team team = game.getPlayerTeam(player);
            if (team != null && team.isDead(game)) giveAgainItem(player, team);
            openMenuDelayed(player, delayTicks);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!forceOpenPlayers.contains(playerId)) return;
        if (!event.isCancelled()) {
            forceOpenPlayers.remove(playerId);
            return;
        }

        if (event.getInventory() == null || event.getInventory().getTitle() == null) {
            forceOpenPlayers.remove(playerId);
            return;
        }
        String menuTitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("menu.title", "游戏结束, 请选择"));
        if (!event.getInventory().getTitle().equals(menuTitle)) {
            forceOpenPlayers.remove(playerId);
            return;
        }

        event.setCancelled(false);
        forceOpenPlayers.remove(playerId);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getInventory() == null || event.getInventory().getTitle() == null) return;

        String menuTitle = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("menu.title", "游戏结束, 请选择"));
        if (!event.getInventory().getTitle().equals(menuTitle)) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        menu.handleClick(player, event.getSlot());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClickAgainItem(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getClickedInventory() == null ||
                event.getClickedInventory().getType() != InventoryType.PLAYER) return;
        if (event.getSlot() != 7) return;
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        if (!plugin.getConfig().contains("inventory.again")) return;

        String expectedName = plugin.getConfig().getString("inventory.again.name", "再来一局");
        String expectedNameTranslated = ChatColor.translateAlternateColorCodes('&', expectedName);
        if (!clickedItem.getItemMeta().getDisplayName().equals(expectedNameTranslated)) return;

        event.setCancelled(true);
        String againServer = plugin.getConfig().getString("againServer", "bw44");
        com.aermini.bwagain.utils.BungeeCordUtils.sendPlayAgain(player, againServer);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        pendingMenuPlayers.remove(playerId);
        openedMenuPlayers.remove(playerId);
        forceOpenPlayers.remove(playerId);
    }

    private void giveAgainItem(Player player, Team team) {
        int itemId = plugin.getConfig().getInt("inventory.again.item", 345);
        int data = plugin.getConfig().getInt("inventory.again.data", 0);
        String displayName = plugin.getConfig().getString("inventory.again.name", "再来一局");
        List<String> lore = plugin.getConfig().getStringList("inventory.again.lore");

        Material material = Material.getMaterial(itemId);
        if (material == null) {
            material = Material.getMaterial("COMPASS");
            if (material == null) material = Material.COMPASS;
        }

        ItemStack item = new ItemStack(material, 1, (short) data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        player.getInventory().setItem(7, item);
        player.updateInventory();
    }

    private void openMenuDelayed(final Player player, long delay) {
        final UUID playerId = player.getUniqueId();
        if (openedMenuPlayers.contains(playerId)) return;
        if (pendingMenuPlayers.contains(playerId)) return;
        pendingMenuPlayers.add(playerId);

        new BukkitRunnable() {
            @Override
            public void run() {
                pendingMenuPlayers.remove(playerId);
                if (!player.isOnline()) return;

                openedMenuPlayers.add(playerId);
                try {
                    forceOpenPlayers.add(playerId);
                    Inventory inventory = menu.createInventory(player);
                    player.openInventory(inventory);
                } catch (Exception e) {
                    forceOpenPlayers.remove(playerId);
                    e.printStackTrace();
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        openedMenuPlayers.remove(playerId);
                        forceOpenPlayers.remove(playerId);
                    }
                }.runTaskLater(plugin, 300L);
            }
        }.runTaskLater(plugin, delay);
    }

    private long getDelayTicks() {
        int waitMs = plugin.getConfig().getInt("waitSecond", 3000);
        return (long) (waitMs / 1000.0 * 20);
    }
}