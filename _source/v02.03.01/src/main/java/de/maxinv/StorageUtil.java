package de.maxinv;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StorageUtil {

    public static Map<Material, Long> load(File file) {
        Map<Material, Long> map = new HashMap<>();
        if (!file.exists()) return map;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String key : yml.getKeys(false)) {
            try {
                Material mat = Material.valueOf(key);
                long amount = yml.getLong(key);
                if (amount > 0) map.put(mat, amount);
            } catch (IllegalArgumentException ignored) {}
        }
        return map;
    }

    public static void save(File file, Map<Material, Long> map) {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<Material, Long> e : map.entrySet()) {
            if (e.getValue() > 0)
                yml.set(e.getKey().name(), e.getValue());
        }
        try {
            yml.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
