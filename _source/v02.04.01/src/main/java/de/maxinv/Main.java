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
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private LanguageManager lang;

    // GUI state
    private final Map<UUID, Integer> openPage = new HashMap<>();
    private final Map<UUID, Map<Material, Long>> renderedOnPage = new HashMap<>();
    private final Map<UUID, String> searchQuery = new HashMap<>();
    private final Set<UUID> favoritesMode = new HashSet<>(); // toggle favorites-view

    // Layout
    private static final int PAGE_SIZE = 45; // 0..44 content, 45..53 controls
    private static final int SLOT_PREV  = 45; // ◀
    private static final int SLOT_FAV   = 46; // ⭐ toggle
    private static final int SLOT_CLR   = 47; // ❌ clear search
    private static final int SLOT_TOTAL = 48; // 📦 total items
    private static final int SLOT_PAGE  = 49; // Page label
    private static final int SLOT_SORT  = 50; // 🔀 sort toggle
    private static final int SLOT_NEXT  = 53; // ▶

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

        getLogger().info("MaxInvPlugin v2.4.1 enabled (search, favorites, totals, sort, paging).");
    }

    // Folia-friendly: try entity scheduler, fallback to Bukkit main thread
    private void runOnEntityThread(Player player, Runnable task) {
        try {
            Method getScheduler = player.getClass().getMethod("getScheduler");
            Object scheduler = getScheduler.invoke(player);
            for (Method m : scheduler.getClass().getMethods()) {
                if (m.getName().equals("run") && m.getParameterCount() >= 2) {
                    try {
                        m.invoke(scheduler, this, (java.util.function.Consumer<Object>) (ignored) -> task.run());
                    } catch (Throwable t) {
                        m.invoke(scheduler, this, task);
                    }
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

        // /bag [page] | /bag search <q> | /bag clearsearch | /bag fav [add|remove] <MATERIAL> | /bag fav
        if (args.length >= 1) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("search") && args.length >= 2) {
                String q = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
                searchQuery.put(player.getUniqueId(), q);
                favoritesMode.remove(player.getUniqueId());
                final int openFirst = 1;
                runOnEntityThread(player, () -> openBag(player, openFirst));
                return true;
            } else if (sub.equals("clearsearch")) {
                searchQuery.remove(player.getUniqueId());
                runOnEntityThread(player, () -> openBag(player, 1));
                return true;
            } else if (sub.equals("fav")) {
                if (args.length == 1) {
                    if (favoritesMode.contains(player.getUniqueId())) favoritesMode.remove(player.getUniqueId());
                    else favoritesMode.add(player.getUniqueId());
                    runOnEntityThread(player, () -> openBag(player, 1));
                    return true;
                }
                if (args.length >= 3) {
                    String op = args[1].toLowerCase(Locale.ROOT);
                    String matName = args[2].toUpperCase(Locale.ROOT);
                    try {
                        Material mat = Material.valueOf(matName);
                        if (op.equals("add")) {
                            FavoritesUtil.addFavorite(this, player.getUniqueId(), mat);
                            player.sendMessage(lang.get("msg.fav_added").replace("%material%", mat.name()));
                        } else if (op.equals("remove")) {
                            FavoritesUtil.removeFavorite(this, player.getUniqueId(), mat);
                            player.sendMessage(lang.get("msg.fav_removed").replace("%material%", mat.name()));
                        }
                        return true;
                    } catch (IllegalArgumentException ex) {
                        player.sendMessage(lang.get("msg.invalid_material").replace("%material%", matName));
                        return true;
                    }
                }
            } else {
                try {
                    int page = Math.max(1, Integer.parseInt(args[0]));
                    final int pg = page;
                    runOnEntityThread(player, () -> openBag(player, pg));
                    return true;
                } catch (NumberFormatException ignored) {}
            }
        }

        runOnEntityThread(player, () -> openBag(player, 1));
        return true;
    }

    private boolean isOurGUI(Component title) {
        String base = lang.get("gui.base");
        String plain = PlainTextComponentSerializer.plainText().serialize(title);
        return plain != null && plain.contains(base);
    }

    private Comparator<Material> comparatorFor(Map<Material, Long> totals) {
        String mode = config.getString("sort-mode", "alphabetical").toLowerCase(Locale.ROOT);
        if (mode.equals("amount-desc")) {
            return (a, b) -> Long.compare(totals.getOrDefault(b, 0L), totals.getOrDefault(a, 0L));
        }
        return Comparator.comparing(Enum::name);
    }

    private List<ItemStack> expandStacks(Map<Material, Long> totals, Comparator<Material> order) {
        List<Material> mats = totals.keySet().stream().sorted(order).collect(Collectors.toList());
        List<ItemStack> out = new ArrayList<>();
        for (Material m : mats) {
            long left = totals.getOrDefault(m, 0L);
            while (left > 0) {
                int put = (int) Math.min(64, left);
                out.add(new ItemStack(m, put));
                left -= put;
            }
        }
        return out;
    }

    private Map<Material, Long> applyFilters(UUID uuid, Map<Material, Long> totals) {
        Map<Material, Long> src = new HashMap<>(totals);
        // favorites filter
        if (favoritesMode.contains(uuid)) {
            Set<Material> fav = FavoritesUtil.getFavorites(this, uuid);
            src.keySet().retainAll(fav);
        }
        // search filter
        String q = searchQuery.get(uuid);
        if (q != null && !q.isEmpty()) {
            String ql = q.toLowerCase(Locale.ROOT);
            src.keySet().removeIf(m -> !m.name().toLowerCase(Locale.ROOT).contains(ql));
        }
        return src;
    }

    private long totalCount(Map<Material, Long> totals) {
        long sum = 0L;
        for (long v : totals.values()) sum += v;
        return sum;
    }

    private ItemStack named(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        im.displayName(Component.text(name));
        it.setItemMeta(im);
        return it;
    }

    private ItemStack namedWithLore(Material mat, String name, List<Component> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        im.displayName(Component.text(name));
        im.lore(lore);
        it.setItemMeta(im);
        return it;
    }

    private void openBag(Player player, int page) {
        UUID uuid = player.getUniqueId();
        File file = new File(getDataFolder(), "data/" + uuid + ".yml");
        Map<Material, Long> totals = StorageUtil.load(file);

        totals = applyFilters(uuid, totals);
        Comparator<Material> comp = comparatorFor(totals);
        List<ItemStack> stacks = expandStacks(totals, comp);

        int pages = Math.max(1, (int) Math.ceil(stacks.size() / (double) PAGE_SIZE));
        int current = Math.min(Math.max(1, page), pages);

        String title = lang.get("gui.page_title")
                .replace("%page%", String.valueOf(current))
                .replace("%pages%", String.valueOf(pages));

        Inventory inv = Bukkit.createInventory(null, 54, Component.text(title));

        // render page
        Map<Material, Long> rendered = new HashMap<>();
        int start = (current - 1) * PAGE_SIZE;
        int end = Math.min(stacks.size(), start + PAGE_SIZE);
        int slot = 0;
        for (int i = start; i < end; i++) {
            ItemStack s = stacks.get(i);
            inv.setItem(slot++, s);
            rendered.put(s.getType(), rendered.getOrDefault(s.getType(), 0L) + s.getAmount());
        }

        // controls (non-draggable)
        inv.setItem(SLOT_PREV, named(Material.ARROW, lang.get("gui.prev")));
        inv.setItem(SLOT_NEXT, named(Material.ARROW, lang.get("gui.next")));
        inv.setItem(SLOT_PAGE, named(Material.PAPER, lang.get("gui.page_label")
                .replace("%page%", String.valueOf(current)).replace("%pages%", String.valueOf(pages))));

        long total = totalCount(totals);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(lang.get("gui.total_l1")));
        lore.add(Component.text(String.valueOf(total)));
        inv.setItem(SLOT_TOTAL, namedWithLore(Material.CHEST, lang.get("gui.total_title"), lore));

        if (searchQuery.containsKey(uuid)) {
            inv.setItem(SLOT_CLR, named(Material.BARRIER, lang.get("gui.clear_search")));
        } else {
            inv.setItem(SLOT_CLR, named(Material.GLASS_PANE, lang.get("gui.no_search")));
        }
        if (favoritesMode.contains(uuid)) {
            inv.setItem(SLOT_FAV, named(Material.GOLD_NUGGET, lang.get("gui.fav_on")));
        } else {
            inv.setItem(SLOT_FAV, named(Material.IRON_NUGGET, lang.get("gui.fav_off")));
        }

        String mode = config.getString("sort-mode", "alphabetical");
        String label = mode.equalsIgnoreCase("amount-desc") ? lang.get("gui.sort_amount") : lang.get("gui.sort_alpha");
        inv.setItem(SLOT_SORT, named(Material.COMPASS, label));

        openPage.put(uuid, current);
        renderedOnPage.put(uuid, rendered);
        player.openInventory(inv);

        if (config.getBoolean("show-open-hint", true)) {
            String hint = lang.get("msg.open_hint");
            if (searchQuery.containsKey(uuid)) {
                hint += " | " + lang.get("msg.hint_searching").replace("%query%", searchQuery.get(uuid));
            }
            if (favoritesMode.contains(uuid)) {
                hint += " | " + lang.get("msg.hint_favorites");
            }
            player.sendMessage(hint);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isOurGUI(event.getView().title())) return;

        // Only allow interaction with top inventory content slots (0..44) – block all control slots 45..53
        int raw = event.getRawSlot();
        if (raw >= 45 && raw <= 53) {
            event.setCancelled(true);
        }
        if (raw < 0 || raw >= event.getInventory().getSize()) {
            event.setCancelled(true);
            return;
        }

        UUID uuid = player.getUniqueId();
        int current = openPage.getOrDefault(uuid, 1);

        // navigation & controls
        if (raw == SLOT_PREV || raw == SLOT_NEXT) {
            int dir = (raw == SLOT_NEXT) ? 1 : -1;
            int next = Math.max(1, current + dir);
            event.setCancelled(true);
            runOnEntityThread(player, () -> openBag(player, next));
            return;
        }

        if (raw == SLOT_CLR) {
            event.setCancelled(true);
            if (searchQuery.containsKey(uuid)) {
                searchQuery.remove(uuid);
                runOnEntityThread(player, () -> openBag(player, 1));
            }
            return;
        }

        if (raw == SLOT_FAV) {
            event.setCancelled(true);
            if (favoritesMode.contains(uuid)) favoritesMode.remove(uuid);
            else favoritesMode.add(uuid);
            runOnEntityThread(player, () -> openBag(player, 1));
            return;
        }

        if (raw == SLOT_SORT) {
            event.setCancelled(true);
            String mode = config.getString("sort-mode", "alphabetical");
            if ("amount-desc".equalsIgnoreCase(mode)) config.set("sort-mode", "alphabetical");
            else config.set("sort-mode", "amount-desc");
            saveConfig();
            runOnEntityThread(player, () -> openBag(player, current));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isOurGUI(event.getView().title())) return;

        UUID uuid = player.getUniqueId();
        File file = new File(getDataFolder(), "data/" + uuid + ".yml");
        Map<Material, Long> before = StorageUtil.load(file);
        Map<Material, Long> rendered = renderedOnPage.getOrDefault(uuid, Collections.emptyMap());

        // remove rendered from before -> remainder from other pages
        Map<Material, Long> remainder = new HashMap<>(before);
        for (Map.Entry<Material, Long> e : rendered.entrySet()) {
            Material m = e.getKey();
            long val = remainder.getOrDefault(m, 0L) - e.getValue();
            if (val <= 0) remainder.remove(m);
            else remainder.put(m, val);
        }

        // read visible content area (0..44)
        Map<Material, Long> now = new HashMap<>();
        for (int i = 0; i < PAGE_SIZE; i++) {
            ItemStack it = event.getInventory().getItem(i);
            if (it == null || it.getType() == Material.AIR) continue;
            now.put(it.getType(), now.getOrDefault(it.getType(), 0L) + it.getAmount());
        }

        Map<Material, Long> total = new HashMap<>(remainder);
        for (Map.Entry<Material, Long> e : now.entrySet()) {
            total.put(e.getKey(), total.getOrDefault(e.getKey(), 0L) + e.getValue());
        }

        StorageUtil.save(file, total);
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

        ItemStack stack = event.getItem().getItemStack();
        if (stack == null || stack.getType() == Material.AIR) return;

        File file = new File(getDataFolder(), "data/" + p.getUniqueId() + ".yml");
        Map<Material, Long> totals = StorageUtil.load(file);
        Material mat = stack.getType();
        long add = stack.getAmount();
        totals.put(mat, safeAdd(totals.getOrDefault(mat, 0L), add));
        StorageUtil.save(file, totals);

        event.getItem().remove();
        if (config.getBoolean("auto-store-message", true)) {
            p.sendMessage(lang.get("msg.auto_store")
                    .replace("%amount%", String.valueOf(add))
                    .replace("%material%", mat.name()));
        }
    }

    private long safeAdd(long a, long b) {
        long r = a + b;
        if (b > 0 && r < a) return Long.MAX_VALUE;
        if (b < 0 && r > a) return 0L;
        return Math.max(0L, r);
    }
}