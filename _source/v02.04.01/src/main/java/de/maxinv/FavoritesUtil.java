package de.maxinv;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FavoritesUtil {

    private static File favFile(JavaPlugin plugin, UUID uuid) {
        return new File(plugin.getDataFolder(), "data/" + uuid + "_fav.yml");
    }

    public static Set<Material> getFavorites(JavaPlugin plugin, UUID uuid) {
        Set<Material> out = new HashSet<>();
        File f = favFile(plugin, uuid);
        if (!f.exists()) return out;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        for (String s : y.getStringList("favorites")) {
            try {
                out.add(Material.valueOf(s));
            } catch (IllegalArgumentException ignored) {}
        }
        return out;
    }

    public static void addFavorite(JavaPlugin plugin, UUID uuid, Material mat) {
        Set<Material> fav = getFavorites(plugin, uuid);
        fav.add(mat);
        save(plugin, uuid, fav);
    }

    public static void removeFavorite(JavaPlugin plugin, UUID uuid, Material mat) {
        Set<Material> fav = getFavorites(plugin, uuid);
        fav.remove(mat);
        save(plugin, uuid, fav);
    }

    private static void save(JavaPlugin plugin, UUID uuid, Set<Material> fav) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("favorites", fav.stream().map(Enum::name).toList());
        try {
            y.save(favFile(plugin, uuid));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}