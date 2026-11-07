package id.stt.customskill;

import id.stt.engine.SttEngine;
import id.stt.engine.skill.SkillDef;
import id.stt.engine.util.ConfigDefaults;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Skill custom (MYTHIC_MOBS). Definisi (id, nama, phrase, nama skill MythicMobs) ada di kode.
 * config.yml hanya berisi cost mana dan cooldown. Damage/duration ada di file skill pack MythicMobs.
 */
public final class SttCustomSkill extends JavaPlugin {

    /** id, nama, phrase, mythic-skill-name, cost default, cooldown default (detik). */
    private record Custom(String id, String name, String phrase, String mythicSkillName, double cost, double cooldown) {
    }

    private static final List<Custom> SKILLS = List.of(
            new Custom("meteor_awakened", "Awakened Meteor", "meteor storm", "Meteor_Storm", 120.0, 30.0));

    @Override
    public void onEnable() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        for (Custom c : SKILLS) {
            defaults.put("skills." + c.id() + ".cost", c.cost());
            defaults.put("skills." + c.id() + ".cooldown", c.cooldown());
        }
        ConfigDefaults.apply(this, List.of(
                "sttcustomskill - hanya cost mana dan cooldown (detik) skill custom.",
                "Damage dan duration TIDAK di sini: isi langsung di file skill pack MythicMobs.",
                "Edit lalu jalankan /stt reload."), defaults);

        for (Custom c : SKILLS) {
            String key = "skills." + c.id() + ".";
            SttEngine.get().getRegistry().register(SkillDef.mythic(this, c.id(), c.name(), c.phrase(),
                    () -> getConfig().getDouble(key + "cost", c.cost()),
                    () -> getConfig().getDouble(key + "cooldown", c.cooldown()),
                    c.mythicSkillName()));
        }
    }

    @Override
    public void onDisable() {
        SttEngine engine = SttEngine.get();
        if (engine != null) {
            engine.getRegistry().unregisterOwner(this);
        }
    }
}
