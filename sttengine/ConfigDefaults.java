package id.stt.engine.util;

import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Dipakai plugin skill: default angka ada di kode. Key yang belum ada di config.yml ditambahkan,
 * nilai yang sudah diedit user TIDAK ditimpa.
 */
public final class ConfigDefaults {

    private ConfigDefaults() {
    }

    public static void apply(JavaPlugin plugin, List<String> header, Map<String, Object> defaults) {
        FileConfiguration config = plugin.getConfig();
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            config.addDefault(entry.getKey(), entry.getValue());
        }
        config.options().copyDefaults(true);
        config.options().setHeader(header);
        plugin.saveConfig();
    }
}
