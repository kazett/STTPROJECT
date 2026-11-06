package id.stt.engine.voice;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import id.stt.engine.skill.SkillService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.vosk.Model;
import org.vosk.Recognizer;

/** Satu Recognizer Vosk per player (grammar = phrase skill milik player). Model dipakai bersama. */
public final class VoiceManager {

    private static final float SAMPLE_RATE = 16000f;

    private final Logger log;
    private final SkillService skillService;
    private final Model model;
    private final Map<UUID, PlayerRecognizer> recognizers = new ConcurrentHashMap<>();
    private final Set<UUID> active = ConcurrentHashMap.newKeySet();
    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    public VoiceManager(Logger log, SkillService skillService, Model model) {
        this.log = log;
        this.skillService = skillService;
        this.model = model;
    }

    private static final class PlayerRecognizer {
        final ReentrantLock lock = new ReentrantLock();
        Recognizer recognizer;
    }

    /** Panggil saat join, pilih ras, addskill/removeskill/resetrace, atau skill baru terdaftar. Main thread. */
    public void refreshGrammar(Player player) {
        UUID id = player.getUniqueId();
        String name = player.getName();
        List<String> phrases = skillService.getUnlockedPhrases(id);

        if (phrases.isEmpty()) {
            remove(id);
            return;
        }

        List<String> words = new ArrayList<>(phrases);
        words.add("[unk]");
        String grammar = new Gson().toJson(words);

        active.add(id);
        try {
            pool.submit(() -> {
                if (!active.contains(id)) {
                    return; // player sudah quit
                }
                try {
                    Recognizer fresh = new Recognizer(model, SAMPLE_RATE, grammar);
                    PlayerRecognizer pr = recognizers.computeIfAbsent(id, k -> new PlayerRecognizer());
                    Recognizer old;
                    pr.lock.lock();
                    try {
                        if (!active.contains(id)) { // quit saat recognizer dibuat
                            fresh.close();
                            recognizers.remove(id, pr);
                            return;
                        }
                        old = pr.recognizer;
                        pr.recognizer = fresh;
                    } finally {
                        pr.lock.unlock();
                    }
                    if (old != null) {
                        old.close();
                    }
                } catch (IOException e) {
                    log.warning("Gagal build grammar untuk " + name + ": " + e.getMessage());
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException ignored) {
            // plugin sedang dimatikan
        }
    }

    /** True kalau player punya recognizer (bukan human). Dipanggil thread audio. */
    public boolean has(UUID id) {
        PlayerRecognizer pr = recognizers.get(id);
        return pr != null && pr.recognizer != null;
    }

    /** Dipanggil thread audio. Return teks hasil (sudah di-parse), atau null. */
    public String process(UUID id, byte[] pcm) {
        PlayerRecognizer pr = recognizers.get(id);
        if (pr == null) {
            return null;
        }
        pr.lock.lock();
        try {
            if (pr.recognizer == null) {
                return null;
            }
            if (!pr.recognizer.acceptWaveForm(pcm, pcm.length)) {
                return null;
            }
            return parse(pr.recognizer.getResult());
        } finally {
            pr.lock.unlock();
        }
    }

    /** Akhir ucapan (paket kosong dari SVC): ambil hasil akhir. */
    public String finish(UUID id) {
        PlayerRecognizer pr = recognizers.get(id);
        if (pr == null) {
            return null;
        }
        pr.lock.lock();
        try {
            return pr.recognizer == null ? null : parse(pr.recognizer.getFinalResult());
        } finally {
            pr.lock.unlock();
        }
    }

    private static String parse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        var obj = JsonParser.parseString(json).getAsJsonObject();
        if (!obj.has("text")) {
            return null;
        }
        String text = obj.get("text").getAsString().trim();
        return text.isEmpty() || text.equals("[unk]") ? null : text;
    }

    public void remove(UUID id) {
        active.remove(id);
        PlayerRecognizer pr = recognizers.remove(id);
        if (pr == null) {
            return;
        }
        pr.lock.lock();
        try {
            if (pr.recognizer != null) {
                pr.recognizer.close();
            }
            pr.recognizer = null;
        } finally {
            pr.lock.unlock();
        }
    }

    /** Panggil di onDisable. */
    public void shutdown() {
        pool.shutdown();
        try {
            pool.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        for (UUID id : new ArrayList<>(recognizers.keySet())) {
            remove(id);
        }
        active.clear();
        model.close();
    }
}
