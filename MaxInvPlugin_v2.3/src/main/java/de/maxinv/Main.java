
package de.maxinv;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

public class Main extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private final Map<UUID, Map<Material, Long>> openSessionRendered = new HashMap<>();
    private LanguageManager lang;
    private String TITLE_TEXT;
    private Component TITLE;

    @Override
    public void onEnable() {
        // Config
        saveDefaultConfig();
        reloadCustomConfig();

        // Language
        lang = new LanguageManager(this);
        String languageCode = this.config.getString("language", "de");
        lang.load(languageCode);

        TITLE_TEXT = lang.get("gui.title");
        TITLE = Component.text(TITLE_TEXT);

        // Ensure data dir exists
        File dataDir = new File(getDataFolder(), "data");
        if (!dataDir.exists()) dataDir.mkdirs();

        // Events
        Bukkit.getPluginManager().registerEvents(this, this);

        getLogger().info("MaxInvPlugin v2.3 aktiviert: /bag, Language=" + languageCode);
    }

    private void reloadCustomConfig() {
        File cfg = new File(getDataFolder(), "config.yml");
        if (!cfg.exists()) {
            saveResource("config.yml", false);
        }
        config = new YamlConfiguration();
        try {
            config.load(cfg);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    // -------- Folia-friendly scheduling (fallback to Bukkit main) --------
    private void runOnEntityThread(Player player, Runnable task) {
        try {
            Method getScheduler = player.getClass().getMethod("getScheduler");
            Object entityScheduler = getScheduler.invoke(player);
            Method runMethod = null;
            for (Method m : entityScheduler.getClass().getMethods()) {
                if (!m.getName().equals("run")) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length >= 2 && Plugin.class.isAssignableFrom(p[0]) && java.util.function.Consumer.class.isAssignableFrom(p[1])) {
                    runMethod = m; break;
                }
            }
            if (runMethod != null) {
                java.util.function.Consumer<Object> c = (ignored) -> task.run();
                runMethod.invoke(entityScheduler, this, c);
                return;
            }
        } catch (Throwable ignored) {}
        Bukkit.getScheduler().runTask(this, task);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("bag")) return false;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.get("msg.only_players"));
            return true;
        }
        runOnEntityThread(player, () -> openBag(player));
        return true;
    }

    private void openBag(Player player) {
        UUID uuid = player.getUniqueId();
        Map<Material, Long> totals = loadTotals(uuid);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        Map<Material, Long> rendered = new HashMap<>();
        int slot = 0;

        List<Material> mats = new ArrayList<>(totals.keySet());
        mats.sort(Comparator.comparing(Enum::name));

        for (Material mat : mats) {
            long count = totals.getOrDefault(mat, 0L);
            if (count <= 0) continue;
            while (count > 0 && slot < 54) {
                if (slot == 53) break; // reserve last slot for summary
                int put = (int) Math.min(64, count);
                ItemStack stack = new ItemStack(mat, put);
                inv.setItem(slot++, stack);
                rendered.put(mat, rendered.getOrDefault(mat, 0L) + put);
                count -= put;
            }
            if (slot >= 53) break;
        }

        openSessionRendered.put(uuid, rendered);

        // Build remainder summary
        Map<Material, Long> remainder = calcRemainder(totals, rendered);
        if (!remainder.isEmpty()) {
            ItemStack info = new ItemStack(Material.PAPER, 1);
            ItemMeta meta = info.getItemMeta();
            meta.displayName(Component.text(lang.get("gui.rest_summary")));
            List<Component> lore = new ArrayList<>();
            int lines = 0;
            for (Map.Entry<Material, Long> e : remainder.entrySet()) {
                lore.add(Component.text(e.getKey().name() + ": " + e.getValue()));
                if (++lines >= 10) { lore.add(Component.text("...")); break; }
            }
            meta.lore(lore);
            info.setItemMeta(meta);
            inv.setItem(53, info);
        }

        player.openInventory(inv);
        if (config.getBoolean("show-open-hint", true)) {
            player.sendMessage(lang.get("msg.open_hint"));
        }
    }

    private Map<Material, Long> calcRemainder(Map<Material, Long> totals, Map<Material, Long> rendered) {
        Map<Material, Long> remainder = new HashMap<>(totals);
        for (Map.Entry<Material, Long> e : rendered.entrySet()) {
            Material m = e.getKey();
            long r = remainder.getOrDefault(m, 0L) - e.getValue();
            if (r <= 0) remainder.remove(m);
            else remainder.put(m, r);
        }
        remainder.entrySet().removeIf(en -> en.getValue() <= 0);
        return remainder;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String titlePlain = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!TITLE_TEXT.equals(titlePlain)) return;

        UUID uuid = player.getUniqueId();
        Map<Material, Long> totalsBefore = loadTotals(uuid);
        Map<Material, Long> rendered = openSessionRendered.getOrDefault(uuid, Collections.emptyMap());

        Map<Material, Long> remainder = calcRemainder(totalsBefore, rendered);

        Map<Material, Long> guiNow = new HashMap<>();
        ItemStack[] contents = event.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (i == 53) continue; // skip summary item
            ItemStack is = contents[i];
            if (is == null || is.getType() == Material.AIR) continue;
            guiNow.put(is.getType(), guiNow.getOrDefault(is.getType(), 0L) + is.getAmount());
        }

        Map<Material, Long> newTotals = new HashMap<>(remainder);
        for (Map.Entry<Material, Long> e : guiNow.entrySet()) {
            Material m = e.getKey();
            newTotals.put(m, safeAdd(newTotals.getOrDefault(m, 0L), e.getValue()));
        }

        saveTotals(uuid, newTotals);
        openSessionRendered.remove(uuid);
        if (config.getBoolean("show-save-message", true)) {
            player.sendMessage(lang.get("msg.saved"));
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!config.getBoolean("auto-store-if-full", true)) return;
        if (player.getInventory().firstEmpty() != -1) return;

        ItemStack item = event.getItem().getItemStack();
        if (item == null || item.getType() == Material.AIR) return;

        UUID uuid = player.getUniqueId();
        Map<Material, Long> totals = loadTotals(uuid);
        Material mat = item.getType();
        long add = item.getAmount();
        totals.put(mat, safeAdd(totals.getOrDefault(mat, 0L), add));
        saveTotals(uuid, totals);

        event.setCancelled(true);
        event.getItem().remove();

        if (config.getBoolean("auto-store-message", true)) {
            Map<String,String> ph = new HashMap<>();
            ph.put("amount", String.valueOf(add));
            ph.put("material", mat.name());
            player.sendMessage(lang.format("msg.auto_store", ph));
        }
    }

    // === Persistence ===

    private File getDataFile(UUID uuid) {
        return new File(getDataFolder(), "data" + File.separator + uuid + ".yml");
    }

    private Map<Material, Long> loadTotals(UUID uuid) {
        Map<Material, Long> map = new HashMap<>();
        File f = getDataFile(uuid);
        if (!f.exists()) return map;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
        if (!yml.contains("counts")) return map;
        if (yml.getConfigurationSection("counts") == null) return map;
        for (String key : yml.getConfigurationSection("counts").getKeys(false)) {
            try {
                Material mat = Material.valueOf(key);
                long val = yml.getLong("counts." + key, 0L);
                if (val > 0) map.put(mat, val);
            } catch (IllegalArgumentException ex) { }
        }
        return map;
    }

    private void saveTotals(UUID uuid, Map<Material, Long> totals) {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<Material, Long> e : totals.entrySet()) {
            if (e.getValue() <= 0) continue;
            yml.set("counts." + e.getKey().name(), e.getValue());
        }
        try {
            yml.save(getDataFile(uuid));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private long safeAdd(long a, long b) {
        long r = a + b;
        if (b > 0 && r < a) return Long.MAX_VALUE;
        if (b < 0 && r > a) return 0L;
        return Math.max(0L, r);
    }
}
