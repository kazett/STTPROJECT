package id.stt.engine.data;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

/** Baca/tulis file player. Tulis atomik: <UUID>.yml.tmp lalu rename menimpa file asli. */
public final class PlayerStore {

    private final Logger log;
    private final File dir;
    private final DoubleSupplier maxMana;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Map<String, UUID> names = new ConcurrentHashMap<>();
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public PlayerStore(Logger log, File dataFolder, DoubleSupplier maxMana) {
        this.log = log;
        this.dir = new File(dataFolder, "players");
        this.maxMana = maxMana;
        dir.mkdirs();
        indexNames();
    }

    private void indexNames() {
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            try {
                UUID id = UUID.fromString(file.getName().substring(0, file.getName().length() - 4));
                String name = YamlConfiguration.loadConfiguration(file).getString("name", "");
                if (!name.isEmpty()) {
                    names.put(name.toLowerCase(Locale.ROOT), id);
                }
            } catch (IllegalArgumentException ignored) {
                // bukan file player (nama bukan UUID)
            }
        }
    }

    private File fileOf(UUID id) {
        return new File(dir, id + ".yml");
    }

    /** Data player online dari cache, atau null. */
    public PlayerData cached(UUID id) {
        return cache.get(id);
    }

    /** Baca dari disk, null kalau player belum pernah punya file. */
    public PlayerData loadFromDisk(UUID id) {
        File file = fileOf(id);
        if (!file.exists()) {
            return null;
        }
        return PlayerData.fromYaml(id, YamlConfiguration.loadConfiguration(file), maxMana.getAsDouble());
    }

    /** Saat join: ambil dari disk atau buat baru, lalu masukkan ke cache. */
    public PlayerData onJoin(UUID id, String name) {
        PlayerData data = loadFromDisk(id);
        if (data == null) {
            data = new PlayerData(id, name, maxMana.getAsDouble());
        }
        data.setName(name);
        cache.put(id, data);
        names.put(name.toLowerCase(Locale.ROOT), id);
        return data;
    }

    /** Cache kalau online, kalau tidak baca dari disk (buat admin command pada player offline). */
    public PlayerData getOrLoad(UUID id) {
        PlayerData data = cache.get(id);
        return data != null ? data : loadFromDisk(id);
    }

    public UUID findByName(String name) {
        return names.get(name.toLowerCase(Locale.ROOT));
    }

    public java.util.Collection<String> knownNames() {
        java.util.List<String> list = new java.util.ArrayList<>();
        for (Map.Entry<String, UUID> e : names.entrySet()) {
            PlayerData data = getOrLoad(e.getValue());
            list.add(data != null && !data.name().isEmpty() ? data.name() : e.getKey());
        }
        return list;
    }

    public void unload(UUID id) {
        cache.remove(id);
    }

    public Iterable<PlayerData> online() {
        return cache.values();
    }

    /** Snapshot sekarang (main thread), tulis di thread I/O. */
    public void saveAsync(PlayerData data) {
        String yaml = data.toYamlString();
        UUID id = data.uuid();
        try {
            io.submit(() -> write(id, yaml));
        } catch (java.util.concurrent.RejectedExecutionException e) {
            write(id, yaml);
        }
    }

    private void write(UUID id, String yaml) {
        File target = fileOf(id);
        Path tmp = new File(dir, id + ".yml.tmp").toPath();
        try {
            Files.writeString(tmp, yaml, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.warning("Gagal menyimpan data player " + id + ": " + e.getMessage());
        }
    }

    public void saveAllOnline() {
        for (PlayerData data : cache.values()) {
            saveAsync(data);
        }
    }

    /** Dipanggil onDisable: simpan semua, tunggu antrean tulis selesai. */
    public void shutdown() {
        saveAllOnline();
        io.shutdown();
        try {
            if (!io.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warning("Antrean simpan data player belum selesai setelah 10 detik.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
