
package de.maxinv;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LanguageManager {

    private final JavaPlugin plugin;
    private YamlConfiguration lang;

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(String languageCode) {
        // Language files are expected at plugins/MaxInvPlugin/lang/lang_<code>.yml
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) langDir.mkdirs();

        String filename = "lang/lang_" + languageCode + ".yml";
        File target = new File(plugin.getDataFolder(), filename);

        // If not present on disk, try to copy default from resources
        if (!target.exists()) {
            if (plugin.getResource(filename) != null) {
                plugin.saveResource(filename, false);
            } else {
                // Fallback to English bundled resource
                plugin.saveResource("lang/lang_en.yml", false);
                target = new File(plugin.getDataFolder(), "lang/lang_en.yml");
            }
        }

        this.lang = YamlConfiguration.loadConfiguration(target);

        // Fallback chain: if a key is missing, look into bundled default (same language), then English
        // Load bundled resource (same language) for defaults
        InputStream inSame = plugin.getResource(filename);
        if (inSame != null) {
            YamlConfiguration def = YamlConfiguration.loadConfiguration(new InputStreamReader(inSame, StandardCharsets.UTF_8));
            this.lang.setDefaults(def);
        }
        // And English as ultimate fallback
        InputStream inEn = plugin.getResource("lang/lang_en.yml");
        if (inEn != null) {
            YamlConfiguration defEn = YamlConfiguration.loadConfiguration(new InputStreamReader(inEn, StandardCharsets.UTF_8));
            this.lang.addDefaults(defEn);
        }
        this.lang.options().copyDefaults(true);
    }

    public String get(String path) {
        if (lang == null) return path;
        return lang.getString(path, path);
    }

    public String format(String path, Map<String, String> placeholders) {
        String txt = get(path);
        if (placeholders == null) return txt;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            txt = txt.replace("%" + e.getKey() + "%", e.getValue());
        }
        return txt;
    }
}
