package id.stt.engine.command;

import id.stt.engine.SttEngine;
import id.stt.engine.data.PlayerData;
import id.stt.engine.data.PlayerStore;
import id.stt.engine.gui.SttMenu;
import id.stt.engine.mana.Mana;
import id.stt.engine.race.RaceService;
import id.stt.engine.skill.SkillDef;
import id.stt.engine.skill.SkillRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/** /stt menu (player). /stt addskill, removeskill, resetrace, reload, mana (admin: stt.admin). */
public final class SttCommand implements CommandExecutor, TabCompleter {

    private final SttEngine engine;
    private final PlayerStore store;
    private final SkillRegistry registry;
    private final RaceService races;
    private final Mana mana;
    private final SttMenu menu;

    public SttCommand(SttEngine engine, PlayerStore store, SkillRegistry registry, RaceService races, Mana mana, SttMenu menu) {
        this.engine = engine;
        this.store = store;
        this.registry = registry;
        this.races = races;
        this.mana = mana;
        this.menu = menu;
    }

    private static boolean isAdmin(CommandSender sender) {
        return sender.hasPermission("stt.admin") || sender.isOp();
    }

    private static void say(CommandSender sender, String text, NamedTextColor color) {
        sender.sendMessage(Component.text(text).color(color));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "menu" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "menu" -> {
                if (!(sender instanceof Player player)) {
                    say(sender, "Hanya player yang bisa membuka menu.", NamedTextColor.RED);
                    return true;
                }
                menu.open(player);
            }
            case "addskill" -> addSkill(sender, args);
            case "removeskill" -> removeSkill(sender, args);
            case "resetrace" -> resetRace(sender, args);
            case "reload" -> reload(sender);
            case "mana" -> {
                if (!isAdmin(sender)) {
                    say(sender, "Kamu tidak memiliki izin.", NamedTextColor.RED);
                } else if (sender instanceof Player self) {
                    mana.refill(self);
                    say(self, "[STT] Mana kamu di-reset penuh.", NamedTextColor.GREEN);
                } else {
                    say(sender, "Hanya player yang bisa reset mana.", NamedTextColor.RED);
                }
            }
            default -> say(sender, "Pakai: /stt menu" + (isAdmin(sender)
                    ? " | /stt addskill <player> <skill_id> | /stt removeskill <player> <skill_id> | /stt resetrace <player> | /stt reload | /stt mana"
                    : ""), NamedTextColor.RED);
        }
        return true;
    }

    private void reload(CommandSender sender) {
        if (!isAdmin(sender)) {
            say(sender, "Kamu tidak memiliki izin.", NamedTextColor.RED);
            return;
        }
        engine.reloadAll();
        say(sender, "[STT] Config sttengine dan plugin skill di-reload.", NamedTextColor.GREEN);
    }

    /** Cari UUID player (online atau pernah join). Kirim pesan error dan return null kalau tidak ada. */
    private UUID resolve(CommandSender sender, String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }
        UUID id = store.findByName(name);
        if (id == null) {
            say(sender, "Player belum pernah join: " + name, NamedTextColor.RED);
        }
        return id;
    }

    private void addSkill(CommandSender sender, String[] args) {
        if (!isAdmin(sender)) {
            say(sender, "Kamu tidak memiliki izin.", NamedTextColor.RED);
            return;
        }
        if (args.length != 3) {
            say(sender, "Pakai: /stt addskill <player> <skill_id>", NamedTextColor.RED);
            return;
        }
        UUID id = resolve(sender, args[1]);
        if (id == null) {
            return;
        }
        PlayerData data = store.getOrLoad(id);
        if (data == null) {
            say(sender, "Data player tidak ditemukan: " + args[1], NamedTextColor.RED);
            return;
        }
        String error = races.addSkill(data, args[2]);
        if (error != null) {
            say(sender, error, NamedTextColor.RED);
            return;
        }
        say(sender, "[STT] Skill " + args[2] + " ditambahkan ke " + data.name() + ".", NamedTextColor.GREEN);
        SkillDef def = registry.get(args[2]);
        for (String warning : races.phraseWarnings(data, def)) {
            say(sender, "[STT] Peringatan: " + warning, NamedTextColor.YELLOW);
        }
    }

    private void removeSkill(CommandSender sender, String[] args) {
        if (!isAdmin(sender)) {
            say(sender, "Kamu tidak memiliki izin.", NamedTextColor.RED);
            return;
        }
        if (args.length != 3) {
            say(sender, "Pakai: /stt removeskill <player> <skill_id>", NamedTextColor.RED);
            return;
        }
        UUID id = resolve(sender, args[1]);
        if (id == null) {
            return;
        }
        PlayerData data = store.getOrLoad(id);
        if (data == null) {
            say(sender, "Data player tidak ditemukan: " + args[1], NamedTextColor.RED);
            return;
        }
        String error = races.removeSkill(data, args[2]);
        if (error != null) {
            say(sender, error, NamedTextColor.RED);
            return;
        }
        say(sender, "[STT] Skill " + args[2] + " dicabut dari " + data.name() + ".", NamedTextColor.GREEN);
    }

    private void resetRace(CommandSender sender, String[] args) {
        if (!isAdmin(sender)) {
            say(sender, "Kamu tidak memiliki izin.", NamedTextColor.RED);
            return;
        }
        if (args.length != 2) {
            say(sender, "Pakai: /stt resetrace <player>", NamedTextColor.RED);
            return;
        }
        UUID id = resolve(sender, args[1]);
        if (id == null) {
            return;
        }
        PlayerData data = store.getOrLoad(id);
        if (data == null) {
            say(sender, "Data player tidak ditemukan: " + args[1], NamedTextColor.RED);
            return;
        }
        races.resetRace(data);
        say(sender, "[STT] Ras dan semua skill " + data.name() + " direset. Dia bisa memilih ras lagi lewat /stt menu.", NamedTextColor.GREEN);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            options.add("menu");
            if (isAdmin(sender)) {
                options.addAll(List.of("addskill", "removeskill", "resetrace", "reload", "mana"));
            }
        } else if (isAdmin(sender)) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (args.length == 2 && List.of("addskill", "removeskill", "resetrace").contains(sub)) {
                options.addAll(store.knownNames());
            } else if (args.length == 3 && sub.equals("addskill")) {
                options.addAll(registry.ids());
            } else if (args.length == 3 && sub.equals("removeskill")) {
                UUID id = store.findByName(args[1]);
                PlayerData data = id == null ? null : store.getOrLoad(id);
                if (data != null) {
                    for (String skill : data.unlocked()) {
                        if (registry.get(skill) != null) {
                            options.add(skill);
                        }
                    }
                }
            }
        }
        options.removeIf(o -> !o.toLowerCase(Locale.ROOT).startsWith(last));
        return options;
    }
}
