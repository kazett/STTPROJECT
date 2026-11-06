package id.stt.engine.skill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

/** Daftar skill yang terdaftar. Engine tidak hardcode skill, semuanya didaftarkan plugin skill saat onEnable. */
public final class SkillRegistry {

    private final Logger log;
    private final Map<String, SkillDef> skills = new LinkedHashMap<>();
    private Runnable onChange = () -> {
    };
    private java.util.function.Consumer<SkillDef> onRegister = def -> {
    };

    public SkillRegistry(Logger log) {
        this.log = log;
    }

    /** Dipanggil engine: refresh grammar saat daftar skill berubah. */
    public synchronized void setListeners(java.util.function.Consumer<SkillDef> onRegister, Runnable onChange) {
        this.onRegister = onRegister;
        this.onChange = onChange;
    }

    public synchronized boolean register(SkillDef def) {
        if (skills.containsKey(def.id())) {
            log.warning("Skill '" + def.id() + "' sudah terdaftar, pendaftaran dari " + def.owner().getName() + " diabaikan.");
            return false;
        }
        for (SkillDef other : skills.values()) {
            if (other.phrase().equals(def.phrase())) {
                log.warning("Phrase '" + def.phrase() + "' dipakai dua skill: " + other.id() + " dan " + def.id());
            }
        }
        skills.put(def.id(), def);
        onRegister.accept(def);
        onChange.run();
        return true;
    }

    /** Dipanggil saat plugin skill dimatikan. */
    public synchronized void unregisterOwner(JavaPlugin owner) {
        boolean changed = false;
        for (SkillDef def : new ArrayList<>(skills.values())) {
            if (def.owner().equals(owner)) {
                skills.remove(def.id());
                if (def.nativeSkill() != null) {
                    try {
                        def.nativeSkill().shutdown();
                    } catch (RuntimeException e) {
                        log.warning("Gagal shutdown skill " + def.id() + ": " + e.getMessage());
                    }
                }
                changed = true;
            }
        }
        if (changed) {
            onChange.run();
        }
    }

    public synchronized SkillDef get(String id) {
        return skills.get(id);
    }

    public synchronized Collection<SkillDef> all() {
        return new ArrayList<>(skills.values());
    }

    public synchronized List<String> ids() {
        return new ArrayList<>(skills.keySet());
    }

    public synchronized List<JavaPlugin> owners() {
        List<JavaPlugin> owners = new ArrayList<>();
        for (SkillDef def : skills.values()) {
            if (!owners.contains(def.owner())) {
                owners.add(def.owner());
            }
        }
        return owners;
    }
}
