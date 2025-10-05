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
    private final Map<UUID, Map<Material, Long>> openSessionRendered = new HashMap<>();
    private final String TITLE_KEY = "gui.title";
    private String TITLE_TEXT;
    private Component TITLE_COMPONENT;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        lang = new LanguageManager(this);
        lang.load(config.getString("language", "en"));

        TITLE_TEXT = lang.get(TITLE_KEY);
        TITLE_COMPONENT = Component.text(TITLE_TEXT);

        File dataDir = new File(getDataFolder(), "data");
        if (!dataDir.exists()) dataDir.mkdirs();

        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("bag").setExecutor(this);

        getLogger().info("MaxInvPlugin v2.3.1 enabled.");
    }

    private void runOnEntityThread(Player player, Runnable task) {
        try {
            Method getScheduler = player.getClass().getMethod("getScheduler");
            Object scheduler = getScheduler.invoke(player);
            for (Method m : scheduler.getClass().getMethods()) {
                if (m.getName().equals("run") && m.getParameterCount() >= 2) {
                    m.invoke(scheduler, this, (Runnable) task);
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
        runOnEntityThread(player, () -> openBag(player));
        return true;
    }

    private void openBag(Player player) {
        UUID uuid = player.getUniqueId();
        Map<Material, Long> totals = StorageUtil.load(new File(getDataFolder(), "data/" + uuid + ".yml"));

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_COMPONENT);
        Map<Material, Long> rendered = new HashMap<>();
        int slot = 0;

        List<Material> sorted = new ArrayList<>(totals.keySet());
        sorted.sort(Comparator.comparing(Enum::name));

        for (Material mat : sorted) {
            long count = totals.getOrDefault(mat, 0L);
            while (count > 0 && slot < 53) {
                int amount = (int) Math.min(64, count);
                inv.setItem(slot++, new ItemStack(mat, amount));
                rendered.put(mat, rendered.getOrDefault(mat, 0L) + amount);
                count -= amount;
            }
        }

        openSessionRendered.put(uuid, rendered);

        Map<Material, Long> rest = calcRemainder(totals, rendered);
        if (!rest.isEmpty()) {
            ItemStack summary = new ItemStack(Material.PAPER);
            ItemMeta meta = summary.getItemMeta();
            meta.displayName(Component.text(lang.get("gui.rest_summary")));
            List<Component> lore = new ArrayList<>();
            for (Map.Entry<Material, Long> e : rest.entrySet()) {
                lore.add(Component.text(e.getKey().name() + ": " + e.getValue()));
            }
            meta.lore(lore);
            summary.setItemMeta(meta);
            inv.setItem(53, summary);
        }

        player.openInventory(inv);

        if (config.getBoolean("show-open-hint", true)) {
            player.sendMessage(lang.get("msg.open_hint"));
        }
    }

    private Map<Material, Long> calcRemainder(Map<Material, Long> total, Map<Material, Long> shown) {
        Map<Material, Long> rest = new HashMap<>(total);
        for (Map.Entry<Material, Long> e : shown.entrySet()) {
            rest.put(e.getKey(), Math.max(0L, rest.get(e.getKey()) - e.getValue()));
        }
        rest.values().removeIf(v -> v <= 0);
        return rest;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player p)) return;
        if (!PlainTextComponentSerializer.plainText().serialize(event.getView().title()).equals(TITLE_TEXT)) return;

        UUID uuid = p.getUniqueId();
        Map<Material, Long> before = StorageUtil.load(new File(getDataFolder(), "data/" + uuid + ".yml"));
        Map<Material, Long> rendered = openSessionRendered.getOrDefault(uuid, new HashMap<>());
        Map<Material, Long> remainder = calcRemainder(before, rendered);

        Map<Material, Long> now = new HashMap<>();
        for (ItemStack is : event.getInventory().getContents()) {
            if (is == null || is.getType() == Material.AIR) continue;
            if (is.getType() == Material.PAPER) continue; // summary
            now.put(is.getType(), now.getOrDefault(is.getType(), 0L) + is.getAmount());
        }

        Map<Material, Long> total = new HashMap<>(remainder);
        for (Map.Entry<Material, Long> e : now.entrySet()) {
            total.put(e.getKey(), total.getOrDefault(e.getKey(), 0L) + e.getValue());
        }

        StorageUtil.save(new File(getDataFolder(), "data/" + uuid + ".yml"), total);
        openSessionRendered.remove(uuid);

        if (config.getBoolean("show-save-message", true)) {
            p.sendMessage(lang.get("msg.saved"));
        }
    }

    @EventHandler
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        Player p = event.getPlayer();
        if (!config.getBoolean("auto-store-if-full", true)) return;
        if (p.getInventory().firstEmpty() != -1) return;

        ItemStack stack = event.getItem().getItemStack();
        Material mat = stack.getType();
        int amount = stack.getAmount();

        File file = new File(getDataFolder(), "data/" + p.getUniqueId() + ".yml");
        Map<Material, Long> inv = StorageUtil.load(file);
        inv.put(mat, inv.getOrDefault(mat, 0L) + amount);
        StorageUtil.save(file, inv);

        event.getItem().remove();
        if (config.getBoolean("auto-store-message", true)) {
            p.sendMessage(lang.get("msg.auto_store")
                .replace("%amount%", String.valueOf(amount))
                .replace("%material%", mat.name()));
        }
    }
}
