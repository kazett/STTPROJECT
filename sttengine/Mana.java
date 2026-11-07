package id.stt.engine.mana;

import id.stt.engine.SttEngine;
import id.stt.engine.data.PlayerData;
import id.stt.engine.data.PlayerStore;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/** Mana (di engine). Max dari settings.max-mana. Regen dan burnout dipertahankan dari sistem lama v2.3.6. */
public final class Mana {

    private static final double REGEN_AMOUNT = 3;
    private static final double REGEN_SECONDS = 5;
    private static final long TICK = 10L;
    /** Mana di bawah ini dianggap habis. */
    private static final double EMPTY = 0.5;
    /** Batas debuff, angka mentah (sama dengan default sistem lama). */
    private static final double LOW = 20;
    private static final double CRITICAL = 10;
    /** Debuff diperpanjang tiap tick, durasinya sengaja pendek supaya hilang cepat saat mana pulih. */
    private static final int DEBUFF_TICKS = 30;
    private static final long AUTOSAVE_TICKS = 6000L;

    private final SttEngine plugin;
    private final PlayerStore store;
    private final Map<UUID, Long> regenBlockedUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> tiers = new ConcurrentHashMap<>();
    /** Tampilan HUD (diisi engine dengan Hud). Mana tidak tahu cara menggambarnya. */
    private java.util.function.Consumer<Player> display = p -> {
    };
    private int beat;
    private BukkitTask task;
    private BukkitTask autosave;

    public Mana(SttEngine plugin, PlayerStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    public void setDisplay(java.util.function.Consumer<Player> display) {
        this.display = display;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, TICK, TICK);
        autosave = Bukkit.getScheduler().runTaskTimer(plugin, store::saveAllOnline, AUTOSAVE_TICKS, AUTOSAVE_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        if (autosave != null) {
            autosave.cancel();
        }
        regenBlockedUntil.clear();
        tiers.clear();
    }

    public double max() {
        return Math.max(1, plugin.getConfig().getDouble("settings.max-mana", 300));
    }

    public double get(UUID id) {
        PlayerData data = store.cached(id);
        return data == null ? max() : data.currentMana();
    }

    private void set(UUID id, double value) {
        PlayerData data = store.cached(id);
        if (data != null) {
            data.setCurrentMana(Math.max(0, Math.min(max(), value)));
        }
    }

    public boolean has(Player player, double amount) {
        return get(player.getUniqueId()) >= amount;
    }

    public boolean use(Player player, double amount) {
        UUID id = player.getUniqueId();
        double now = get(id);
        if (now < amount) {
            return false;
        }
        double left = now - amount;
        set(id, left);
        boolean burnout = left <= EMPTY;
        double delay = plugin.getConfig().getDouble(burnout ? "settings.burnout-delay" : "settings.mana-regen-delay", burnout ? 8 : 3);
        regenBlockedUntil.put(id, System.currentTimeMillis() + (long) (delay * 1000));
        show(player);
        return true;
    }

    public void refill(Player player) {
        set(player.getUniqueId(), max());
        regenBlockedUntil.remove(player.getUniqueId());
        show(player);
    }

    public void forget(UUID id) {
        regenBlockedUntil.remove(id);
        tiers.remove(id);
    }

    private void tick() {
        double step = REGEN_AMOUNT / REGEN_SECONDS * (TICK / 20.0);
        long now = System.currentTimeMillis();
        beat++;
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            if (store.cached(id) == null) {
                continue;
            }
            Long blocked = regenBlockedUntil.get(id);
            if (blocked == null || now >= blocked) {
                set(id, get(id) + step);
            }
            applyDebuff(player, get(id));
        }
    }

    /** 0 = aman, 1 = menipis, 2 = kritis, 3 = habis. */
    private static int tierOf(double mana) {
        if (mana <= EMPTY) {
            return 3;
        }
        if (mana <= CRITICAL) {
            return 2;
        }
        if (mana <= LOW) {
            return 1;
        }
        return 0;
    }

    private void applyDebuff(Player player, double mana) {
        UUID id = player.getUniqueId();
        int tier = tierOf(mana);
        int before = tiers.getOrDefault(id, 0);
        tiers.put(id, tier);
        GameMode mode = player.getGameMode();
        if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR || player.isDead()) {
            return;
        }
        if (tier > before) {
            warn(player, tier);
        }
        if (tier == 0) {
            return;
        }
        if (tier >= 2 && beat % 3 == 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 1f);
        }
        switch (tier) {
            case 1 -> {
                effect(player, PotionEffectType.SLOW, 0);
                effect(player, PotionEffectType.WEAKNESS, 0);
            }
            case 2 -> {
                effect(player, PotionEffectType.SLOW, 1);
                effect(player, PotionEffectType.WEAKNESS, 1);
                effect(player, PotionEffectType.SLOW_DIGGING, 0);
            }
            default -> {
                effect(player, PotionEffectType.SLOW, 2);
                effect(player, PotionEffectType.WEAKNESS, 1);
                effect(player, PotionEffectType.SLOW_DIGGING, 1);
                effect(player, PotionEffectType.DARKNESS, 0);
            }
        }
    }

    /** Jangan menimpa efek lain yang lebih lama. */
    private static void effect(Player player, PotionEffectType type, int amplifier) {
        PotionEffect current = player.getPotionEffect(type);
        if (current != null && current.getDuration() > DEBUFF_TICKS) {
            return;
        }
        player.addPotionEffect(new PotionEffect(type, DEBUFF_TICKS, amplifier, true, false, true));
    }

    private void warn(Player player, int tier) {
        String title = switch (tier) {
            case 1 -> "Mana menipis";
            case 2 -> "Mana kritis!";
            default -> "MANA HABIS";
        };
        String subtitle = switch (tier) {
            case 1 -> "Tubuhmu mulai melemah";
            case 2 -> "Gerakanmu melambat";
            default -> "Kamu kelelahan, tunggu mana pulih";
        };
        NamedTextColor color = switch (tier) {
            case 1 -> NamedTextColor.YELLOW;
            case 2 -> NamedTextColor.GOLD;
            default -> NamedTextColor.RED;
        };
        player.showTitle(Title.title(
                Component.text(title).color(color).decorate(TextDecoration.BOLD),
                Component.text(subtitle).color(NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1200), Duration.ofMillis(400))));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, tier == 3 ? 0.5f : 0.8f);
    }

    public void show(Player player) {
        display.accept(player);
    }
}
