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
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String key : y.getKeys(false)) {
            try {
                Material m = Material.valueOf(key);
                long val = y.getLong(key, 0L);
                if (val > 0) map.put(m, val);
            } catch (IllegalArgumentException ignored) {}
        }
        return map;
    }

    public static void save(File file, Map<Material, Long> totals) {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<Material, Long> e : totals.entrySet()) {
            if (e.getValue() <= 0) continue;
            y.set(e.getKey().name(), e.getValue());
        }
        try {
            y.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
