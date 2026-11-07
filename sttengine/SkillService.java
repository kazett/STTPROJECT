package id.stt.engine.skill;

import id.stt.engine.data.PlayerData;
import id.stt.engine.data.PlayerStore;
import id.stt.engine.mana.Mana;
import id.stt.engine.mythic.MythicHook;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Semua pemanggilan skill lewat sini: cek unlock, cek bisa dicast, cooldown, mana, eksekusi. Main thread saja. */
public final class SkillService {

    private final Logger log;
    private final SkillRegistry registry;
    private final PlayerStore store;
    private final Mana mana;
    private final MythicHook mythic;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public SkillService(Logger log, SkillRegistry registry, PlayerStore store, Mana mana, MythicHook mythic) {
        this.log = log;
        this.registry = registry;
        this.store = store;
        this.mana = mana;
        this.mythic = mythic;
    }

    /** Skill milik player yang ada di registry. ID tak dikenal diabaikan (tidak dihapus dari file). */
    public List<SkillDef> unlockedDefs(PlayerData data) {
        List<SkillDef> defs = new ArrayList<>();
        for (String id : data.unlocked()) {
            SkillDef def = registry.get(id);
            if (def != null) {
                defs.add(def);
            }
        }
        return defs;
    }

    /** Phrase skill milik player saja, buat grammar Vosk. */
    public List<String> getUnlockedPhrases(UUID id) {
        PlayerData data = store.cached(id);
        if (data == null) {
            return List.of();
        }
        Set<String> phrases = new LinkedHashSet<>();
        for (SkillDef def : unlockedDefs(data)) {
            if (def.type() == SkillType.MYTHIC_MOBS && !mythic.ready()) {
                continue;
            }
            phrases.add(def.phrase());
        }
        return new ArrayList<>(phrases);
    }

    /** Hasil Vosk -> cari skill milik player yang phrase-nya ada di teks (yang terpanjang menang), lalu cast. */
    public void castByVoice(Player player, String text) {
        PlayerData data = store.cached(player.getUniqueId());
        if (data == null) {
            return;
        }
        String padded = " " + text + " ";
        SkillDef best = null;
        for (SkillDef def : unlockedDefs(data)) {
            if (padded.contains(" " + def.phrase() + " ")
                    && (best == null || def.phrase().length() > best.phrase().length())) {
                best = def;
            }
        }
        if (best != null) {
            cast(player, best);
        }
    }

    public void cast(Player player, SkillDef def) {
        if (!player.isOnline() || player.isDead()) {
            return;
        }
        PlayerData data = store.cached(player.getUniqueId());
        // Vosk sudah mengenali phrase, tetap cek skill itu memang milik player
        if (data == null || !data.unlocked().contains(def.id())) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
            return;
        }
        if (!available(player, def)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
            return;
        }
        long now = System.currentTimeMillis();
        Map<String, Long> mine = cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        Long readyAt = mine.get(def.id());
        if (readyAt != null && now < readyAt) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
            return;
        }
        double cost = def.cost();
        if (!mana.has(player, cost)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
            return;
        }
        boolean ok;
        if (def.type() == SkillType.MYTHIC_MOBS) {
            ok = mythic.cast(player, def.mythicSkillName());
            if (!ok) {
                log.warning("Cast skill '" + def.id() + "' (MythicMobs: " + def.mythicSkillName() + ") gagal. Mana dan cooldown tidak berubah.");
                player.sendMessage(net.kyori.adventure.text.Component.text("Skill gagal dijalankan.")
                        .color(net.kyori.adventure.text.format.NamedTextColor.RED));
                return;
            }
        } else {
            def.nativeSkill().cast(player);
        }
        mana.use(player, cost);
        mine.put(def.id(), now + (long) (def.cooldown() * 1000));
    }

    /** Cek dulu sebelum mana dipotong dan cooldown mulai. */
    private boolean available(Player player, SkillDef def) {
        if (def.type() == SkillType.MYTHIC_MOBS) {
            if (!mythic.canCast(def.mythicSkillName())) {
                log.warning("Skill '" + def.id() + "': MythicMobs tidak aktif atau skill '" + def.mythicSkillName() + "' tidak ditemukan.");
                return false;
            }
            return true;
        }
        return def.nativeSkill().canCast(player);
    }

    /** Sisa cooldown skill dalam detik, 0 kalau sudah siap. Dipakai HUD. */
    public double cooldownLeft(UUID id, SkillDef def) {
        Map<String, Long> mine = cooldowns.get(id);
        Long readyAt = mine == null ? null : mine.get(def.id());
        if (readyAt == null) {
            return 0;
        }
        return Math.max(0, (readyAt - System.currentTimeMillis()) / 1000.0);
    }

    public void forget(UUID id) {
        cooldowns.remove(id);
    }
}
