package id.stt.engine.gui;

import id.stt.engine.SttEngine;
import id.stt.engine.data.PlayerData;
import id.stt.engine.data.PlayerStore;
import id.stt.engine.mana.Mana;
import id.stt.engine.race.RaceDef;
import id.stt.engine.race.RaceService;
import id.stt.engine.race.Races;
import id.stt.engine.skill.SkillDef;
import id.stt.engine.skill.SkillRegistry;
import id.stt.engine.skill.SkillService;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** /stt menu: pilih ras (dengan konfirmasi, hanya kalau race == null) dan lihat skill milik sendiri. */
public final class SttMenu implements Listener {

    private enum Kind { RACE, CONFIRM, SKILLS }

    private static final class Holder implements InventoryHolder {
        final Kind kind;
        final String raceId;
        Inventory inventory;

        Holder(Kind kind, String raceId) {
            this.kind = kind;
            this.raceId = raceId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final int CONFIRM_YES = 11;
    private static final int CONFIRM_NO = 15;

    private final SttEngine engine;
    private final PlayerStore store;
    private final Mana mana;
    private final SkillRegistry registry;
    private final SkillService skills;
    private final RaceService races;

    public SttMenu(SttEngine engine, PlayerStore store, Mana mana, SkillRegistry registry, SkillService skills, RaceService races) {
        this.engine = engine;
        this.store = store;
        this.mana = mana;
        this.registry = registry;
        this.skills = skills;
        this.races = races;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, engine);
    }

    public void open(Player player) {
        PlayerData data = store.cached(player.getUniqueId());
        if (data == null) {
            player.sendMessage(Component.text("Data kamu belum dimuat, coba lagi sebentar.").color(NamedTextColor.RED));
            return;
        }
        if (data.race() == null) {
            openRaces(player);
        } else {
            openSkills(player, data);
        }
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
    }

    private static Inventory create(Holder holder, int size, Component title) {
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.inventory = inv;
        return inv;
    }

    private void openRaces(Player player) {
        Holder holder = new Holder(Kind.RACE, null);
        Inventory inv = create(holder, 27, Component.text("Pilih Ras").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));
        int slot = 10;
        for (RaceDef race : Races.all()) {
            if (slot > 16) {
                break;
            }
            List<Component> lore = new ArrayList<>();
            lore.add(line("Skill yang terbuka:", NamedTextColor.GRAY));
            for (String id : race.skills()) {
                SkillDef def = registry.get(id);
                lore.add(line(" - " + (def != null ? def.name() : id), NamedTextColor.YELLOW));
            }
            lore.add(Component.empty());
            lore.add(line("Klik untuk memilih (PERMANEN)", NamedTextColor.RED));
            inv.setItem(slot++, item(race.icon(), RaceService.raceName(race).decorate(TextDecoration.BOLD), lore));
        }
        player.openInventory(inv);
    }

    private void openConfirm(Player player, RaceDef race) {
        Holder holder = new Holder(Kind.CONFIRM, race.id());
        Component question = Component.text("Yakin pilih ").color(NamedTextColor.DARK_PURPLE)
                .append(RaceService.raceName(race)).append(Component.text("? Ini permanen").color(NamedTextColor.DARK_PURPLE));
        Inventory inv = create(holder, 27, question);
        inv.setItem(CONFIRM_YES, item(Material.LIME_WOOL, line("Ya, pilih ras ini", NamedTextColor.GREEN).decorate(TextDecoration.BOLD),
                List.of(line("Tidak bisa diganti sendiri setelah ini.", NamedTextColor.RED))));
        inv.setItem(13, item(race.icon(), RaceService.raceName(race).decorate(TextDecoration.BOLD), List.of()));
        inv.setItem(CONFIRM_NO, item(Material.RED_WOOL, line("Batal", NamedTextColor.RED).decorate(TextDecoration.BOLD), List.of()));
        player.openInventory(inv);
    }

    private void openSkills(Player player, PlayerData data) {
        Holder holder = new Holder(Kind.SKILLS, null);
        Inventory inv = create(holder, 36, Component.text("Skill Kamu").color(NamedTextColor.DARK_PURPLE).decorate(TextDecoration.BOLD));
        int slot = 0;
        for (SkillDef def : skills.unlockedDefs(data)) {
            if (slot >= 27) {
                break;
            }
            List<Component> lore = new ArrayList<>();
            lore.add(line("Mantra Voice: ", NamedTextColor.GRAY)
                    .append(Component.text("\"" + def.phrase() + "\"").color(NamedTextColor.YELLOW).decorate(TextDecoration.ITALIC)));
            lore.add(line("Biaya Mana: ", NamedTextColor.GRAY)
                    .append(Component.text((int) def.cost() + " MP").color(NamedTextColor.AQUA)));
            lore.add(line("Cooldown: ", NamedTextColor.GRAY)
                    .append(Component.text(def.cooldown() + "s").color(NamedTextColor.GREEN)));
            inv.setItem(slot++, item(Material.ENCHANTED_BOOK,
                    Component.text("✦ " + def.name()).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD), lore));
        }
        Component glassName = Component.text(" ");
        for (int i = 27; i < 36; i++) {
            inv.setItem(i, item(Material.BLACK_STAINED_GLASS_PANE, glassName, List.of()));
        }
        RaceDef race = Races.get(data.race());
        List<Component> info = new ArrayList<>();
        info.add(line("Ras: ", NamedTextColor.GRAY)
                .append(race != null ? RaceService.raceName(race) : Component.text(data.race())));
        info.add(line("Mana: ", NamedTextColor.GRAY)
                .append(Component.text((int) mana.get(player.getUniqueId()) + " / " + (int) mana.max() + " MP").color(NamedTextColor.AQUA)));
        inv.setItem(35, item(Material.BOOK, Component.text("ℹ Status Player").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD), info));
        player.openInventory(inv);
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text).color(color).decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack item(Material material, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        int slot = event.getSlot();
        switch (holder.kind) {
            case RACE -> {
                List<RaceDef> all = Races.all();
                int index = slot - 10;
                if (index >= 0 && index < all.size() && slot <= 16) {
                    openConfirm(player, all.get(index));
                }
            }
            case CONFIRM -> {
                if (slot == CONFIRM_YES) {
                    String error = races.chooseRace(player, holder.raceId);
                    player.closeInventory();
                    if (error != null) {
                        player.sendMessage(Component.text(error).color(NamedTextColor.RED));
                    } else {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                    }
                } else if (slot == CONFIRM_NO) {
                    openRaces(player);
                }
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }
}
