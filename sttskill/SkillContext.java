package id.stt.skill;

import org.bukkit.plugin.java.JavaPlugin;

public final class SkillContext {

    private final JavaPlugin plugin;
    private final String id;

    public SkillContext(JavaPlugin plugin, String id) {
        this.plugin = plugin;
        this.id = id;
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public double num(String key, double fallback) {
        return plugin.getConfig().getDouble("skills." + id + "." + key, fallback);
    }

    public boolean bool(String key, boolean fallback) {
        return plugin.getConfig().getBoolean("skills." + id + "." + key, fallback);
    }
}
