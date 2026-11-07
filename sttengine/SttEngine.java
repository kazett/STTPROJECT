package id.stt.engine;

import id.stt.engine.command.SttCommand;
import id.stt.engine.data.PlayerData;
import id.stt.engine.data.PlayerStore;
import id.stt.engine.gui.SttMenu;
import id.stt.engine.hud.Hud;
import id.stt.engine.mana.Mana;
import id.stt.engine.mythic.MythicHook;
import id.stt.engine.race.RaceService;
import id.stt.engine.skill.SkillDef;
import id.stt.engine.skill.SkillRegistry;
import id.stt.engine.skill.SkillService;
import id.stt.engine.voice.VoiceLink;
import id.stt.engine.voice.VocabCheck;
import id.stt.engine.voice.VoiceManager;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.vosk.Model;

/** STT Engine: data player, ras, mana, voice per player, dan eksekusi skill. Plugin skill mendaftar lewat getRegistry(). */
public final class SttEngine extends JavaPlugin implements Listener {

    private static SttEngine instance;

    private PlayerStore store;
    private SkillRegistry registry;
    private Mana mana;
    private MythicHook mythic;
    private SkillService skillService;
    private Hud hud;
    private RaceService races;
    private VoiceManager voiceManager;
    private Object voice;
    private VocabCheck vocab;

    public static SttEngine get() {
        return instance;
    }

    public SkillRegistry getRegistry() {
        return registry;
    }

    public double getMaxMana() {
        return mana.max();
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        store = new PlayerStore(getLogger(), getDataFolder(), () -> getConfig().getDouble("settings.max-mana", 300));
        registry = new SkillRegistry(getLogger());
        mana = new Mana(this, store);
        mythic = new MythicHook(getLogger());
        skillService = new SkillService(getLogger(), registry, store, mana, mythic);
        hud = new Hud(this, store, mana, skillService);
        mana.setDisplay(hud::show);
        races = new RaceService(this, store, registry);

        checkItemsAdder();
        setupVoice();

        SttMenu menu = new SttMenu(this, store, mana, registry, skillService, races);
        menu.start();
        SttCommand command = new SttCommand(this, store, registry, races, mana, menu);
        getCommand("stt").setExecutor(command);
        getCommand("stt").setTabCompleter(command);

        registry.setListeners(this::checkSkill, this::refreshAllGrammar);
        Bukkit.getPluginManager().registerEvents(this, this);
        mana.start();
        hud.start();
        for (Player player : Bukkit.getOnlinePlayers()) {
            handleJoin(player);
        }
        // Setelah semua plugin skill selesai enable
        Bukkit.getScheduler().runTask(this, this::refreshAllGrammar);
    }

    @Override
    public void onDisable() {
        if (hud != null) {
            hud.stop();
        }
        if (mana != null) {
            mana.stop();
        }
        if (voice != null) {
            VoiceLink.stop(voice);
        }
        if (voiceManager != null) {
            voiceManager.shutdown();
        }
        if (store != null) {
            store.shutdown();
        }
        instance = null;
    }

    private void checkItemsAdder() {
        if (!getConfig().getBoolean("settings.itemsadder-integration", true)) {
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            getLogger().warning("ItemsAdder tidak aktif. STT tetap jalan, pasang sttresourcepack secara manual.");
        }
    }

    private void setupVoice() {
        File dir = new File(getDataFolder(), "model");
        if (!dir.isDirectory()) {
            getDataFolder().mkdirs();
            getLogger().severe("Folder model Vosk tidak ada: " + dir.getPath() + ". Fitur voice nonaktif.");
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("voicechat")) {
            getLogger().warning("Simple Voice Chat tidak ditemukan. Fitur voice nonaktif.");
            return;
        }
        try {
            vocab = new VocabCheck(dir);
            if (!vocab.available()) {
                getLogger().warning("graph/words.txt tidak ada di model Vosk, cek kosakata phrase dilewati.");
            }
            Model model = new Model(dir.getPath());
            VoiceManager manager = new VoiceManager(getLogger(), skillService, model);
            manager.setFastCast(getConfig().getBoolean("settings.fast-cast", true));
            Object link = VoiceLink.start(this, manager, skillService);
            if (link == null) {
                getLogger().warning("Layanan Simple Voice Chat tidak tersedia. Fitur voice nonaktif.");
                manager.shutdown();
                return;
            }
            voiceManager = manager;
            voice = link;
        } catch (IOException e) {
            getLogger().severe("Gagal load model Vosk: " + e.getMessage() + ". Fitur voice nonaktif.");
        } catch (LinkageError e) {
            getLogger().warning("API Simple Voice Chat tidak cocok: " + e + ". Fitur voice nonaktif.");
        }
    }

    /** Dipanggil saat skill baru terdaftar: warning cost dan kosakata phrase. */
    private void checkSkill(SkillDef def) {
        if (def.cost() > mana.max()) {
            getLogger().warning("Skill '" + def.id() + "' cost " + def.cost() + " lebih besar dari max-mana " + mana.max() + ", tidak akan pernah bisa dicast.");
        }
        if (vocab != null) {
            List<String> missing = vocab.missing(def.phrase());
            if (!missing.isEmpty()) {
                getLogger().warning("Phrase skill '" + def.id() + "' ('" + def.phrase() + "') punya kata di luar kosakata model Vosk: " + missing + ". Skill ini tidak akan terdeteksi.");
            }
        }
    }

    public void reloadAll() {
        reloadConfig();
        for (JavaPlugin owner : registry.owners()) {
            owner.reloadConfig();
        }
        for (SkillDef def : registry.all()) {
            if (def.cost() > mana.max()) {
                getLogger().warning("Skill '" + def.id() + "' cost " + def.cost() + " lebih besar dari max-mana " + mana.max() + ".");
            }
        }
    }

    public void refreshGrammar(UUID id) {
        Player player = Bukkit.getPlayer(id);
        if (player != null && voiceManager != null) {
            voiceManager.refreshGrammar(player);
        }
    }

    private void refreshAllGrammar() {
        if (voiceManager == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            voiceManager.refreshGrammar(player);
        }
    }

    private void handleJoin(Player player) {
        PlayerData data = store.onJoin(player.getUniqueId(), player.getName());
        races.syncOnJoin(data);
        if (voiceManager != null) {
            voiceManager.refreshGrammar(player);
        }
        mana.show(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        PlayerData data = store.cached(id);
        if (data != null) {
            store.saveAsync(data);
        }
        if (voiceManager != null) {
            voiceManager.remove(id);
        }
        store.unload(id);
        mana.forget(id);
        hud.forget(id);
        skillService.forget(id);
    }
}
