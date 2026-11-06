package id.stt.engine.data;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;

/** Data satu player (plugins/sttengine/players/<UUID>.yml). Dipakai dari main thread saja. */
public final class PlayerData {

    private final UUID uuid;
    private String name;
    private String race;
    private double currentMana;
    private final Set<String> unlocked = new LinkedHashSet<>();
    private final Set<String> removed = new LinkedHashSet<>();

    public PlayerData(UUID uuid, String name, double currentMana) {
        this.uuid = uuid;
        this.name = name;
        this.currentMana = currentMana;
    }

    public static PlayerData fromYaml(UUID uuid, YamlConfiguration yaml, double maxMana) {
        PlayerData data = new PlayerData(uuid, yaml.getString("name", ""), yaml.getDouble("current-mana", maxMana));
        String race = yaml.getString("race");
        data.race = race == null || race.isBlank() ? null : race;
        data.unlocked.addAll(yaml.getStringList("unlocked-skills"));
        data.removed.addAll(yaml.getStringList("removed-skills"));
        return data;
    }

    /** Snapshot YAML. Dipanggil di main thread, hasil string ditulis di thread lain. */
    public String toYamlString() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("uuid", uuid.toString());
        yaml.set("name", name);
        yaml.set("race", race);
        yaml.set("current-mana", currentMana);
        yaml.set("unlocked-skills", new java.util.ArrayList<>(unlocked));
        yaml.set("removed-skills", new java.util.ArrayList<>(removed));
        return yaml.saveToString();
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String race() {
        return race;
    }

    public void setRace(String race) {
        this.race = race;
    }

    public double currentMana() {
        return currentMana;
    }

    public void setCurrentMana(double currentMana) {
        this.currentMana = currentMana;
    }

    public Set<String> unlocked() {
        return unlocked;
    }

    public Set<String> removed() {
        return removed;
    }
}
