package com.aermini.bwagain.menu;

import com.aermini.bwagain.AerBedwarsAgain;
import com.aermini.bwagain.utils.BungeeCordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameEndMenu {

    private final AerBedwarsAgain plugin;
    private final String title;
    private final int size;

    private final MenuItem viewItem;
    private final MenuItem againItem;
    private final MenuItem hubItem;
    private final MenuItem closeItem;

    private final Set<Integer> airSlots;
    private final MenuItem fillItem;

    private final String againServer;
    private final String hubServer;

    public static class MenuItem {
        private final int slot;
        private final Material material;
        private final int data;
        private final String name;
        private final List<String> lore;

        public MenuItem(int slot, int materialId, int data, String name, List<String> lore) {
            this.slot = slot;
            this.material = Material.getMaterial(materialId);
            this.data = data;
            this.name = ChatColor.translateAlternateColorCodes('&', name);
            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                this.lore = coloredLore;
            } else {
                this.lore = lore;
            }
        }

        public ItemStack toItemStack() {
            if (material == null) return null;
            ItemStack item = new ItemStack(material, 1, (short) data);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return item;
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }
    }

    public GameEndMenu(AerBedwarsAgain plugin) {
        this.plugin = plugin;

        this.title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("menu.title", "游戏结束, 请选择"));
        this.size = plugin.getConfig().getInt("menu.slot", 27);
        this.againServer = plugin.getConfig().getString("againServer", "bw44");
        this.hubServer = plugin.getConfig().getString("hubServer", "bwlobby");

        this.airSlots = new HashSet<>();
        List<Integer> airList = plugin.getConfig().getIntegerList("menu.air");
        if (airList != null) {
            for (Integer slot : airList) {
                if (slot != null) airSlots.add(slot);
            }
        }

        int fillMaterialId = plugin.getConfig().getInt("menu.fill.item", 160);
        int fillData = plugin.getConfig().getInt("menu.fill.data", 15);
        String fillName = plugin.getConfig().getString("menu.fill.name", "&f");
        List<String> fillLore = plugin.getConfig().getStringList("menu.fill.lore");
        this.fillItem = new MenuItem(-1, fillMaterialId, fillData, fillName, fillLore);

        int closeSlot = getSlot("menu.close.slot", 8);
        int closeMaterialId = plugin.getConfig().getInt("menu.close.item", 160);
        int closeData = plugin.getConfig().getInt("menu.close.data", 14);
        String closeName = plugin.getConfig().getString("menu.close.name", "&c&l关闭");
        List<String> closeLore = plugin.getConfig().getStringList("menu.close.lore");
        this.closeItem = new MenuItem(closeSlot, closeMaterialId, closeData, closeName, closeLore);

        int viewSlot = getSlot("menu.view.slot", 10);
        int viewMaterialId = plugin.getConfig().getInt("menu.view.item", 381);
        int viewData = plugin.getConfig().getInt("menu.view.data", 0);
        String viewName = plugin.getConfig().getString("menu.view.name", "继续旁观");
        List<String> viewLore = plugin.getConfig().getStringList("menu.view.lore");
        this.viewItem = new MenuItem(viewSlot, viewMaterialId, viewData, viewName, viewLore);

        int againSlot = getSlot("menu.again.slot", 13);
        int againMaterialId = plugin.getConfig().getInt("menu.again.item", 342);
        int againData = plugin.getConfig().getInt("menu.again.data", 0);
        String againName = plugin.getConfig().getString("menu.again.name", "再来一局");
        List<String> againLore = plugin.getConfig().getStringList("menu.again.lore");
        this.againItem = new MenuItem(againSlot, againMaterialId, againData, againName, againLore);

        int hubSlot = getSlot("menu.hub.slot", 15);
        int hubMaterialId = plugin.getConfig().getInt("menu.hub.item", 341);
        int hubData = plugin.getConfig().getInt("menu.hub.data", 0);
        String hubName = plugin.getConfig().getString("menu.hub.name", "离开游戏");
        List<String> hubLore = plugin.getConfig().getStringList("menu.hub.lore");
        this.hubItem = new MenuItem(hubSlot, hubMaterialId, hubData, hubName, hubLore);
    }

    public void open(Player player) {
        try {
            Inventory inventory = createInventory(player);
            player.openInventory(inventory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Inventory createInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, size, title);

        Set<Integer> occupiedSlots = new HashSet<>();
        occupiedSlots.add(viewItem.slot);
        occupiedSlots.add(againItem.slot);
        occupiedSlots.add(hubItem.slot);
        occupiedSlots.add(closeItem.slot);
        occupiedSlots.addAll(airSlots);

        inventory.setItem(viewItem.slot, viewItem.toItemStack());
        inventory.setItem(againItem.slot, againItem.toItemStack());
        inventory.setItem(hubItem.slot, hubItem.toItemStack());
        inventory.setItem(closeItem.slot, closeItem.toItemStack());

        for (int i = 0; i < size; i++) {
            if (!occupiedSlots.contains(i)) inventory.setItem(i, fillItem.toItemStack());
        }

        return inventory;
    }

    public boolean handleClick(Player player, int slot) {
        if (slot == closeItem.slot) {
            player.closeInventory();
            return true;
        } else if (slot == viewItem.slot) {
            player.closeInventory();
            return true;
        } else if (slot == againItem.slot) {
            player.closeInventory();
            BungeeCordUtils.sendPlayAgain(player, againServer);
            return true;
        } else if (slot == hubItem.slot) {
            player.closeInventory();
            BungeeCordUtils.sendToHub(player, hubServer);
            return true;
        }

        return true;
    }

    private int getSlot(String path, int defaultVal) {
        List<Integer> list = plugin.getConfig().getIntegerList(path);
        if (list != null && !list.isEmpty()) return list.get(0);
        return defaultVal;
    }
}