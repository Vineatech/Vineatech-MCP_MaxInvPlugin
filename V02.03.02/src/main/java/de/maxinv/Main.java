package de.maxinv;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Map;

public class Main extends JavaPlugin implements Listener {

    private LanguageManager lang;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("materials.yml", false);
        lang = new LanguageManager(this);
        lang.load(getConfig().getString("language", "de"));
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("MaxInvPlugin v2.3.2 (AutoStore Materials) enabled!");
    }

    @EventHandler
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        Player p = event.getPlayer();
        ItemStack stack = event.getItem().getItemStack();
        if (stack == null || stack.getType() == Material.AIR) return;

        File matFile = new File(getDataFolder(), "materials.yml");
        YamlConfiguration mats = YamlConfiguration.loadConfiguration(matFile);
        List<String> autoList = mats.getStringList("auto-store");

        Material type = stack.getType();

        // Wenn Material in AutoStore-Liste enthalten ist → sofort einlagern
        if (autoList.contains(type.name())) {
            File file = new File(getDataFolder(), "data/" + p.getUniqueId() + ".yml");
            Map<Material, Long> totals = StorageUtil.load(file);
            long add = stack.getAmount();
            totals.put(type, totals.getOrDefault(type, 0L) + add);
            StorageUtil.save(file, totals);
            event.getItem().remove();

            p.sendMessage(lang.get("msg.auto_store")
                    .replace("%amount%", String.valueOf(add))
                    .replace("%material%", type.name()));
            return;
        }

        // Normales AutoStore-Verhalten (Inventar voll)
        if (!getConfig().getBoolean("auto-store-if-full", true)) return;
        if (p.getInventory().firstEmpty() != -1) return;

        File file = new File(getDataFolder(), "data/" + p.getUniqueId() + ".yml");
        Map<Material, Long> totals = StorageUtil.load(file);
        totals.put(type, totals.getOrDefault(type, 0L) + stack.getAmount());
        StorageUtil.save(file, totals);
        event.getItem().remove();

        if (getConfig().getBoolean("auto-store-message", true)) {
            p.sendMessage(lang.get("msg.auto_store")
                    .replace("%amount%", String.valueOf(stack.getAmount()))
                    .replace("%material%", type.name()));
        }
    }
}
