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

        File file = new File(plugin.getDataFolder(), "lang/lang_" + code + ".yml");
        if (!file.exists()) {
            if (plugin.getResource("lang/lang_" + code + ".yml") != null) {
                plugin.saveResource("lang/lang_" + code + ".yml", false);
            } else {
                plugin.saveResource("lang/lang_en.yml", false);
                file = new File(plugin.getDataFolder(), "lang/lang_en.yml");
            }
        }

        lang = YamlConfiguration.loadConfiguration(file);

        try {
            InputStreamReader en = new InputStreamReader(plugin.getResource("lang/lang_en.yml"), StandardCharsets.UTF_8);
            YamlConfiguration yEn = YamlConfiguration.loadConfiguration(en);
            lang.addDefaults(yEn);
            lang.options().copyDefaults(true);
        } catch (Exception ignored) {}
    }

    public String get(String key) {
        return lang.getString(key, key);
    }
}