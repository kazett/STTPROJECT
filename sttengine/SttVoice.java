package id.stt.engine.voice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import id.stt.engine.skill.SkillService;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Jembatan Simple Voice Chat: Opus 48 kHz -> PCM -> 16 kHz -> VoiceManager (Vosk per player). */
public final class SttVoice implements VoicechatPlugin {

    private final JavaPlugin plugin;
    private final VoiceManager voice;
    private final SkillService skills;
    private volatile boolean stopped;
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private VoicechatApi api;

    public SttVoice(JavaPlugin plugin, VoiceManager voice, SkillService skills) {
        this.plugin = plugin;
        this.voice = voice;
        this.skills = skills;
    }

    @Override
    public String getPluginId() {
        return "stt";
    }

    @Override
    public void initialize(VoicechatApi api) {
        this.api = api;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMic);
        registration.registerEvent(PlayerDisconnectedEvent.class, this::onDisconnect);
    }

    private void onMic(MicrophonePacketEvent event) {
        if (stopped) {
            return;
        }
        VoicechatConnection connection = event.getSenderConnection();
        if (connection == null || connection.getPlayer() == null) {
            return;
        }
        UUID id = connection.getPlayer().getUuid();
        // Player human (belum punya skill): audio tidak diproses
        if (!voice.has(id)) {
            return;
        }
        byte[] data = event.getPacket().getOpusEncodedData();
        try {
            Session session = sessions.computeIfAbsent(id, key -> new Session(id));
            session.submit(data == null ? new byte[0] : data.clone());
        } catch (RejectedExecutionException ignored) {
            // sedang shutdown
        }
    }

    private void onDisconnect(PlayerDisconnectedEvent event) {
        Session session = sessions.remove(event.getPlayerUuid());
        if (session != null) {
            session.close();
        }
    }

    public void shutdown() {
        stopped = true;
        List<CompletableFuture<Void>> done = new ArrayList<>();
        sessions.values().forEach(session -> done.add(session.close()));
        sessions.clear();
        try {
            CompletableFuture.allOf(done.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // lanjut matikan pool
        }
        pool.shutdownNow();
    }

    private static byte[] toPcm16k(short[] pcm48k) {
        int length = pcm48k.length / 3;
        ByteBuffer buffer = ByteBuffer.allocate(length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < length; i++) {
            int sum = pcm48k[i * 3] + pcm48k[i * 3 + 1] + pcm48k[i * 3 + 2];
            buffer.putShort((short) (sum / 3));
        }
        return buffer.array();
    }

    private final class Session {

        private final UUID id;
        private OpusDecoder decoder;
        private CompletableFuture<Void> tail;
        private boolean closed;

        private Session(UUID id) {
            this.id = id;
            this.tail = CompletableFuture.runAsync(this::init, pool);
        }

        private void init() {
            try {
                decoder = api.createDecoder();
            } catch (Exception e) {
                plugin.getLogger().warning("Gagal bikin decoder suara: " + e.getMessage());
            }
        }

        private synchronized void submit(byte[] data) {
            if (closed) {
                return;
            }
            tail = tail.thenRunAsync(() -> process(data), pool);
        }

        private synchronized CompletableFuture<Void> close() {
            if (!closed) {
                closed = true;
                tail = tail.thenRunAsync(() -> {
                    if (decoder != null) {
                        try {
                            decoder.close();
                        } catch (Exception ignored) {
                            // sudah tertutup
                        }
                    }
                }, pool);
            }
            return tail;
        }

        private void process(byte[] data) {
            try {
                if (decoder == null) {
                    return;
                }
                if (data.length == 0) {
                    decoder.resetState();
                    handle(voice.finish(id));
                    return;
                }
                byte[] pcm = toPcm16k(decoder.decode(data));
                handle(voice.process(id, pcm));
            } catch (Exception e) {
                plugin.getLogger().warning("Gagal proses suara: " + e.getMessage());
            }
        }

        private void handle(String text) {
            if (text == null) {
                return;
            }
            // Eksekusi di main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(id);
                if (player != null && player.isOnline() && !player.isDead()) {
                    skills.castByVoice(player, text);
                }
            });
        }
    }
}
