package id.stt.engine.mythic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Cast skill MythicMobs lewat Java API (bukan dispatchCommand), caster = player.
 * Dipanggil lewat reflection supaya build tidak butuh JAR MythicMobs Premium.
 * Nama dicek dari MythicMobs 5.8.2:
 *   io.lumine.mythic.bukkit.MythicBukkit#inst() (static), #getAPIHelper(), #getSkillManager()
 *   io.lumine.mythic.bukkit.BukkitAPIHelper#castSkill(Entity, String) : boolean
 *   io.lumine.mythic.api.skills.SkillManager#getSkill(String) : Optional
 */
public final class MythicHook {

    private final Logger log;
    private boolean ready;
    private Object inst;
    private Method getApiHelper;
    private Method getSkillManager;
    private Method castSkill;
    private Method getSkill;

    public MythicHook(Logger log) {
        this.log = log;
        init();
    }

    private void init() {
        Plugin mythic = Bukkit.getPluginManager().getPlugin("MythicMobs");
        boolean mythicOn = mythic != null && mythic.isEnabled();
        boolean libOn = Bukkit.getPluginManager().isPluginEnabled("MythicLib");
        if (!mythicOn || !libOn) {
            log.warning("Skill bertipe MYTHIC_MOBS dinonaktifkan: " + (!mythicOn ? "MythicMobs" : "MythicLib")
                    + " tidak aktif. Skill NATIVE tetap jalan.");
            return;
        }
        try {
            ClassLoader loader = mythic.getClass().getClassLoader();
            Class<?> bukkit = Class.forName("io.lumine.mythic.bukkit.MythicBukkit", true, loader);
            Class<?> helper = Class.forName("io.lumine.mythic.bukkit.BukkitAPIHelper", true, loader);
            Class<?> manager = Class.forName("io.lumine.mythic.api.skills.SkillManager", true, loader);
            inst = bukkit.getMethod("inst").invoke(null);
            getApiHelper = bukkit.getMethod("getAPIHelper");
            getSkillManager = bukkit.getMethod("getSkillManager");
            castSkill = helper.getMethod("castSkill", org.bukkit.entity.Entity.class, String.class);
            getSkill = manager.getMethod("getSkill", String.class);
            ready = inst != null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            log.warning("API MythicMobs tidak cocok dengan yang diharapkan (5.8.2), skill MYTHIC_MOBS dinonaktifkan: " + e);
        }
    }

    public boolean ready() {
        return ready;
    }

    /** True kalau MythicMobs aktif dan skill dengan nama itu ditemukan. */
    public boolean canCast(String skillName) {
        if (!ready) {
            return false;
        }
        try {
            Object skills = getSkillManager.invoke(inst);
            return ((Optional<?>) getSkill.invoke(skills, skillName)).isPresent();
        } catch (ReflectiveOperationException | RuntimeException e) {
            log.warning("Gagal cek skill MythicMobs '" + skillName + "': " + e);
            return false;
        }
    }

    /** Cast dengan caster = player. Return true kalau MythicMobs menjalankannya. */
    public boolean cast(Player player, String skillName) {
        if (!ready) {
            return false;
        }
        try {
            Object api = getApiHelper.invoke(inst);
            return (boolean) castSkill.invoke(api, player, skillName);
        } catch (InvocationTargetException e) {
            log.warning("MythicMobs error saat cast '" + skillName + "': " + e.getCause());
            return false;
        } catch (ReflectiveOperationException | RuntimeException e) {
            log.warning("Gagal cast skill MythicMobs '" + skillName + "': " + e);
            return false;
        }
    }
}
