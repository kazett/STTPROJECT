package id.stt.engine.hud;

import id.stt.engine.SttEngine;
import id.stt.engine.data.PlayerData;
import id.stt.engine.data.PlayerStore;
import id.stt.engine.mana.Mana;
import id.stt.engine.skill.SkillDef;
import id.stt.engine.skill.SkillService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * HUD actionbar: bar mana + satu kartu per skill yang dimiliki player (hijau = siap, merah = cooldown).
 * Daftar kartu dibaca langsung dari skill player saat ini, jadi jumlahnya ikut ras dan skill tambahan dari admin.
 * Gambarnya dari font stt:hud_* di sttresourcepack, susunan di HudLayout.
 */
public final class Hud {

    /** 2 tick supaya timer cooldown (per 0,1 detik) terlihat jalan. Paket baru hanya dikirim kalau tampilan berubah. */
    private static final long TICK = 2L;
    /** Actionbar hilang sekitar 3 detik setelah paket terakhir, jadi kirim ulang walau tampilan sama. */
    private static final long KEEPALIVE_MS = 1500L;

    private final SttEngine plugin;
    private final PlayerStore store;
    private final Mana mana;
    private final SkillService skills;
    private final Map<UUID, String> lastView = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastSent = new ConcurrentHashMap<>();
    private BukkitTask task;

    public Hud(SttEngine plugin, PlayerStore store, Mana mana, SkillService skills) {
        this.plugin = plugin;
        this.store = store;
        this.mana = mana;
        this.skills = skills;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, TICK, TICK);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        lastView.clear();
        lastSent.clear();
    }

    public void forget(UUID id) {
        lastView.remove(id);
        lastSent.remove(id);
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            show(player);
        }
    }

    public void show(Player player) {
        UUID id = player.getUniqueId();
        PlayerData data = store.cached(id);
        if (data == null) {
            return;
        }
        List<HudLayout.Card> cards = new ArrayList<>();
        for (SkillDef def : skills.unlockedDefs(data)) {
            cards.add(new HudLayout.Card(def.name(), skills.cooldownLeft(id, def), def.cost(), def.cooldown(),
                    mana.has(player, def.cost())));
        }
        List<HudLayout.Run> runs = HudLayout.build(mana.get(id), mana.max(), cards);

        StringBuilder view = new StringBuilder();
        for (HudLayout.Run run : runs) {
            view.append(run.font()).append(run.color()).append(run.text()).append('|');
        }
        String now = view.toString();
        long time = System.currentTimeMillis();
        Long sent = lastSent.get(id);
        if (now.equals(lastView.get(id)) && sent != null && time - sent < KEEPALIVE_MS) {
            return;
        }
        lastView.put(id, now);
        lastSent.put(id, time);

        Component out = Component.empty();
        for (HudLayout.Run run : runs) {
            TextColor color = run.color() < 0 ? NamedTextColor.WHITE : TextColor.color(run.color());
            out = out.append(Component.text(run.text()).font(Key.key("stt", run.font())).color(color));
        }
        player.sendActionBar(out);
    }
}
