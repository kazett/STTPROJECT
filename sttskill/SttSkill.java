package id.stt.skill;

import id.stt.engine.SttEngine;
import id.stt.engine.skill.NativeSkill;
import id.stt.engine.skill.SkillDef;
import id.stt.engine.util.ConfigDefaults;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Zombie;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/** Skill bawaan STT (NATIVE): skill ras Witch + skill lama (thunder, shinra, summon, dll). Definisi di kode, angka tuning di config.yml. */
public final class SttSkill extends JavaPlugin {

    private Summons summons;

    @Override
    public void onEnable() {
        summons = new Summons(this);
        ShinraTense shinra = new ShinraTense(this);
        // Witch (ras): heal, step_swap, black_fire, meteor. Sisanya tidak masuk ras mana pun, diberi admin lewat /stt addskill.
        List<Skill> skills = List.of(
                new Heal(),
                new SwapStep(this, shinra),
                new BlackFire(this),
                new MeteorCrash(this),
                new Thunder(),
                new SonicDash(),
                new FireballSkill(this),
                new ShockBind(this),
                shinra,
                new GravityPull(),
                new SummonSkill(summons, "summon_zombie", "Summon Zombie", "summon zombie", 30, Zombie.class, Summons::zombie),
                new SummonSkill(summons, "summon_skeleton", "Summon Skeleton", "summon skeleton", 30, Skeleton.class, Summons::skeleton),
                new SummonSkill(summons, "summon_golem", "Summon Golem", "summon golem", 50, IronGolem.class, mob -> {
                }),
                new SummonSkill(summons, "summon_warden", "Summon Warden", "summon warden", 100, Warden.class, mob -> {
                }));
        summons.start();

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

        // v3.0.4: nilai meteor lama (tinggi 20, lubang dalam) tersimpan di config.yml dan tidak ikut berubah oleh default, jadi diganti sekali.
        if (getConfig().getInt("config-version", 0) < 2) {
            getConfig().set("skills.meteor.height", 4.0);
            getConfig().set("skills.meteor.up-speed", 0.7);
            getConfig().set("skills.meteor.depth", null);
            getConfig().set("config-version", 2);
            saveConfig();
        }

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
        if (summons != null) {
            summons.stop();
        }
        SttEngine engine = SttEngine.get();
        if (engine != null) {
            engine.getRegistry().unregisterOwner(this);
        }
    }
}
