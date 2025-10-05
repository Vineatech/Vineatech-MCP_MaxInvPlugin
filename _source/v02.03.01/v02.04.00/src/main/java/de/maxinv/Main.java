package de.maxinv;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

public class Main extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private LanguageManager lang;

    private final Map<UUID, Integer> openPage = new HashMap<>();
    private final Map<UUID, Map<Material, Long>> renderedOnPage = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = getConfig();
        lang = new LanguageManager(this);
        lang.load(config.getString("language", "de"));

        File dataDir = new File(getDataFolder(), "data");
        if (!dataDir.exists()) dataDir.mkdirs();

        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("bag").setExecutor(this);

        getLogger().info("MaxInvPlugin v2.4 aktiviert (Seitenfunktion)");
    }

    private void runOnEntityThread(Player player, Runnable task) {
        try {
            Method getScheduler = player.getClass().getMethod("getScheduler");
            Object scheduler = getScheduler.invoke(player);
            for (Method m : scheduler.getClass().getMethods()) {
                if (m.getName().equals("run") && m.getParameterCount() >= 2) {
                    m.invoke(scheduler, this, (java.util.function.Consumer<Object>) (ignored) -> task.run());
                    return;
                }
            }
        } catch (Throwable ignored) {}
        Bukkit.getScheduler().runTask(this, task);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.get("msg.only_players"));
            return true;
        }

        int page = 1;
        if (args.length > 0) {
            try {
                page = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException ignored) {}
        }

        final int finalPage = page;
        runOnEntityThread(player, () -> openBag(player, finalPage));
        return true;
    }

    private void openBag(Player player, int page) {
        UUID uuid = player.getUniqueId();
        Map<Material, Long> totals = StorageUtil.load(new File(getDataFolder(), "data/" + uuid + ".yml"));

        List<ItemStack> stacks = new ArrayList<>();
        for (Material mat : totals.keySet().stream().sorted(Comparator.comparing(Enum::name)).toList()) {
            long total = totals.getOrDefault(mat, 0L);
            while (total > 0) {
                int count = (int) Math.min(64, total);
                stacks.add(new ItemStack(mat, count));
                total -= count;
            }
        }

        int maxPages = Math.max(1, (int) Math.ceil(stacks.size() / 45.0));
        int currentPage = Math.min(Math.max(1, page), maxPages);

        String title = lang.get("gui.page_title")
                .replace("%page%", String.valueOf(currentPage))
                .replace("%pages%", String.valueOf(maxPages));

        Inventory inv = Bukkit.createInventory(null, 54, Component.text(title));
        int start = (currentPage - 1) * 45;
        int end = Math.min(stacks.size(), start + 45);

        Map<Material, Long> rendered = new HashMap<>();
        for (int i = start; i < end; i++) {
            ItemStack stack = stacks.get(i);
            inv.setItem(i - start, stack);
            Material mat = stack.getType();
            rendered.put(mat, rendered.getOrDefault(mat, 0L) + stack.getAmount());
        }

        // Pfeile & Seitenanzeige
        inv.setItem(45, createArrow(lang.get("gui.prev")));
        inv.setItem(49, createPaper(lang.get("gui.page_label")
                .replace("%page%", String.valueOf(currentPage))
                .replace("%pages%", String.valueOf(maxPages))));
        inv.setItem(53, createArrow(lang.get("gui.next")));

        openPage.put(uuid, currentPage);
        renderedOnPage.put(uuid, rendered);
        player.openInventory(inv);

        if (config.getBoolean("show-open-hint", true)) {
            player.sendMessage(lang.get("msg.open_hint"));
        }
    }

    private ItemStack createArrow(String name) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        meta.displayName(Component.text(name));
        arrow.setItemMeta(meta);
        return arrow;
    }

    private ItemStack createPaper(String name) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        meta.displayName(Component.text(name));
        paper.setItemMeta(meta);
        return paper;
    }

    private boolean isOurGUI(Component title) {
        return PlainTextComponentSerializer.plainText().serialize(title)
                .contains(lang.get("gui.base"));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isOurGUI(event.getView().title())) return;

        int slot = event.getRawSlot();
        if (slot == 45 || slot == 49 || slot == 53) event.setCancelled(true);

        if (slot == 45 || slot == 53) {
            int current = openPage.getOrDefault(player.getUniqueId(), 1);
            int direction = slot == 45 ? -1 : 1;
            int next = Math.max(1, current + direction);
            runOnEntityThread(player, () -> openBag(player, next));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isOurGUI(event.getView().title())) return;

        UUID uuid = player.getUniqueId();
        Map<Material, Long> before = StorageUtil.load(new File(getDataFolder(), "data/" + uuid + ".yml"));
        Map<Material, Long> rendered = renderedOnPage.getOrDefault(uuid, new HashMap<>());

        Map<Material, Long> remainder = new HashMap<>(before);
        for (Map.Entry<Material, Long> e : rendered.entrySet()) {
            remainder.put(e.getKey(), remainder.get(e.getKey()) - e.getValue());
        }

        Map<Material, Long> newStored = new HashMap<>(remainder);
        for (int i = 0; i < 45; i++) {
            ItemStack item = event.getInventory().getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            Material mat = item.getType();
            int amount = item.getAmount();
            newStored.put(mat, newStored.getOrDefault(mat, 0L) + amount);
        }

        StorageUtil.save(new File(getDataFolder(), "data/" + uuid + ".yml"), newStored);
        renderedOnPage.remove(uuid);

        if (config.getBoolean("show-save-message", true)) {
            player.sendMessage(lang.get("msg.saved"));
        }
    }

    @EventHandler
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        Player p = event.getPlayer();
        if (!config.getBoolean("auto-store-if-full", true)) return;
        if (p.getInventory().firstEmpty() != -1) return;

        ItemStack item = event.getItem().getItemStack();
        if (item == null || item.getType() == Material.AIR) return;

        Material mat = item.getType();
        int amount = item.getAmount();
        File file = new File(getDataFolder(), "data/" + p.getUniqueId() + ".yml");
        Map<Material, Long> data = StorageUtil.load(file);
        data.put(mat, data.getOrDefault(mat, 0L) + amount);
        StorageUtil.save(file, data);

        event.getItem().remove();
        if (config.getBoolean("auto-store-message", true)) {
            p.sendMessage(lang.get("msg.auto_store")
                .replace("%amount%", String.valueOf(amount))
                .replace("%material%", mat.name()));
        }
    }
}
