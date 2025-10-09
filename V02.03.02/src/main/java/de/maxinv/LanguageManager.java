package de.maxinv;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LanguageManager {

    private final JavaPlugin plugin;
    private YamlConfiguration lang;

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(String code) {
        File dir = new File(plugin.getDataFolder(), "lang");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, "lang_" + code + ".yml");
        if (!file.exists()) {
            plugin.saveResource("lang/lang_" + code + ".yml", false);
        }

        lang = YamlConfiguration.loadConfiguration(file);

        try (InputStreamReader reader = new InputStreamReader(plugin.getResource("lang/lang_en.yml"), StandardCharsets.UTF_8)) {
            YamlConfiguration def = YamlConfiguration.loadConfiguration(reader);
            lang.addDefaults(def);
            lang.options().copyDefaults(true);
        } catch (Exception ignored) {}
    }

    public String get(String key) {
        return lang.getString(key, key);
    }
}
