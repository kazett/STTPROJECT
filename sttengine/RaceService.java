package id.stt.engine.race;

import id.stt.engine.SttEngine;
import id.stt.engine.data.PlayerData;
import id.stt.engine.data.PlayerStore;
import id.stt.engine.skill.SkillDef;
import id.stt.engine.skill.SkillRegistry;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

/** Pilih ras, sinkron skill ras, dan perubahan skill oleh admin. Jalan di main thread. */
public final class RaceService {

    private final SttEngine engine;
    private final PlayerStore store;
    private final SkillRegistry registry;

    public RaceService(SttEngine engine, PlayerStore store, SkillRegistry registry) {
        this.engine = engine;
        this.store = store;
        this.registry = registry;
    }

    public static Component raceName(RaceDef race) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(race.displayName());
    }

    /** Pilih ras (permanen). Return pesan error, atau null kalau berhasil. */
    public String chooseRace(Player player, String raceId) {
        PlayerData data = store.cached(player.getUniqueId());
        RaceDef race = Races.get(raceId);
        if (data == null || race == null) {
            return "Ras tidak ditemukan.";
        }
        if (data.race() != null) {
            return "Kamu sudah punya ras. Ras bersifat permanen.";
        }
        data.setRace(race.id());
        data.unlocked().addAll(race.skills());
        store.saveAsync(data);
        engine.refreshGrammar(player.getUniqueId());
        player.sendMessage(Component.text("Kamu sekarang ").color(NamedTextColor.GREEN)
                .append(raceName(race))
                .append(Component.text(". Semua skill ras terbuka, lihat di /stt menu.").color(NamedTextColor.GREEN)));
        return null;
    }

    /** Dipanggil saat join: skill list ras (kecuali removed-skills) otomatis masuk unlocked-skills. */
    public void syncOnJoin(PlayerData data) {
        RaceDef race = Races.get(data.race());
        if (race == null) {
            return;
        }
        boolean changed = false;
        for (String id : race.skills()) {
            if (!data.removed().contains(id) && data.unlocked().add(id)) {
                changed = true;
            }
        }
        if (changed) {
            store.saveAsync(data);
        }
    }

    /** Return pesan error, atau null kalau berhasil. */
    public String addSkill(PlayerData data, String skillId) {
        if (registry.get(skillId) == null) {
            return "Skill tidak terdaftar: " + skillId;
        }
        if (data.unlocked().contains(skillId)) {
            return data.name() + " sudah punya skill " + skillId + ".";
        }
        data.removed().remove(skillId);
        data.unlocked().add(skillId);
        store.saveAsync(data);
        engine.refreshGrammar(data.uuid());
        return null;
    }

    /** Return pesan error, atau null kalau berhasil. */
    public String removeSkill(PlayerData data, String skillId) {
        if (registry.get(skillId) == null) {
            return "Skill tidak terdaftar: " + skillId;
        }
        if (!data.unlocked().contains(skillId)) {
            return data.name() + " tidak punya skill " + skillId + ".";
        }
        data.unlocked().remove(skillId);
        RaceDef race = Races.get(data.race());
        if (race != null && race.skills().contains(skillId)) {
            data.removed().add(skillId);
        }
        store.saveAsync(data);
        engine.refreshGrammar(data.uuid());
        return null;
    }

    /** Character kill: ras kosong, semua skill (termasuk custom) dan removed-skills dihapus. */
    public void resetRace(PlayerData data) {
        data.setRace(null);
        data.unlocked().clear();
        data.removed().clear();
        store.saveAsync(data);
        engine.refreshGrammar(data.uuid());
    }

    /** Warning phrase: sama persis atau jadi awalan dari phrase skill lain milik player yang sama. */
    public List<String> phraseWarnings(PlayerData data, SkillDef added) {
        List<String> warnings = new ArrayList<>();
        for (String id : data.unlocked()) {
            SkillDef other = registry.get(id);
            if (other == null || other.id().equals(added.id())) {
                continue;
            }
            if (other.phrase().equals(added.phrase()) || other.phrase().startsWith(added.phrase() + " ")) {
                warnings.add("Phrase '" + added.phrase() + "' bentrok dengan skill " + other.id()
                        + " ('" + other.phrase() + "').");
            }
        }
        return warnings;
    }
}
