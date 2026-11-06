package id.stt.skill;

import id.stt.engine.SttEngine;
import id.stt.engine.skill.NativeSkill;
import id.stt.engine.skill.SkillDef;
import id.stt.engine.util.ConfigDefaults;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/** Skill bawaan STT (NATIVE). Build awal: skill ras Witch. Definisi di kode, angka tuning di config.yml. */
public final class SttSkill extends JavaPlugin {

    @Override
    public void onEnable() {
        List<Skill> skills = List.of(
                new Heal(),
                new SwapStep(this),
                new BlackFire(this),
                new MeteorCrash(this));

        Map<String, Object> defaults = new LinkedHashMap<>();
        for (Skill skill : skills) {
            for (Map.Entry<String, Object> e : skill.defaults().entrySet()) {
                defaults.put("skills." + skill.id() + "." + e.getKey(), e.getValue());
            }
        }
        ConfigDefaults.apply(this, List.of(
                "sttskill - angka tuning skill bawaan STT (NATIVE).",
                "Edit lalu jalankan /stt reload. cost = mana, cooldown = detik.",
                "Skill baru dari update JAR ditambahkan otomatis, nilai yang sudah kamu edit tidak ditimpa."), defaults);

        Bukkit.getPluginManager().registerEvents(new BlastGuard(), this);
        for (Skill skill : skills) {
            if (skill instanceof Listener listener) {
                Bukkit.getPluginManager().registerEvents(listener, this);
            }
            String key = "skills." + skill.id() + ".";
            double defaultCost = (double) skill.defaults().get("cost");
            double defaultCooldown = (double) skill.defaults().get("cooldown");
            SkillContext ctx = new SkillContext(this, skill.id());
            SttEngine.get().getRegistry().register(SkillDef.nativeSkill(this, skill.id(), skill.name(), skill.phrase(),
                    () -> getConfig().getDouble(key + "cost", defaultCost),
                    () -> getConfig().getDouble(key + "cooldown", defaultCooldown),
                    new NativeSkill() {
                        @Override
                        public boolean canCast(Player player) {
                            return skill.canCast(player);
                        }

                        @Override
                        public void cast(Player player) {
                            skill.cast(player, ctx);
                        }

                        @Override
                        public void shutdown() {
                            skill.shutdown();
                        }
                    }));
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
