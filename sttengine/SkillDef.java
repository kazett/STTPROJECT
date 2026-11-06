package id.stt.engine.skill;

import java.util.Locale;
import java.util.function.DoubleSupplier;
import org.bukkit.plugin.java.JavaPlugin;

/** Definisi satu skill yang didaftarkan plugin skill ke sttengine. cost dan cooldown dibaca live dari config plugin pemilik. */
public final class SkillDef {

    private final JavaPlugin owner;
    private final String id;
    private final String name;
    private final String phrase;
    private final SkillType type;
    private final String mythicSkillName;
    private final NativeSkill nativeSkill;
    private final DoubleSupplier cost;
    private final DoubleSupplier cooldown;

    private SkillDef(JavaPlugin owner, String id, String name, String phrase, SkillType type, String mythicSkillName,
                     NativeSkill nativeSkill, DoubleSupplier cost, DoubleSupplier cooldown) {
        if (id == null || !id.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("ID skill harus huruf kecil/angka/underscore: " + id);
        }
        if (phrase == null || phrase.isBlank()) {
            throw new IllegalArgumentException("Phrase skill kosong: " + id);
        }
        this.owner = owner;
        this.id = id;
        this.name = name;
        this.phrase = phrase.trim().toLowerCase(Locale.ROOT);
        this.type = type;
        this.mythicSkillName = mythicSkillName;
        this.nativeSkill = nativeSkill;
        this.cost = cost;
        this.cooldown = cooldown;
    }

    public static SkillDef nativeSkill(JavaPlugin owner, String id, String name, String phrase,
                                       DoubleSupplier cost, DoubleSupplier cooldown, NativeSkill impl) {
        return new SkillDef(owner, id, name, phrase, SkillType.NATIVE, null, impl, cost, cooldown);
    }

    public static SkillDef mythic(JavaPlugin owner, String id, String name, String phrase,
                                  DoubleSupplier cost, DoubleSupplier cooldown, String mythicSkillName) {
        if (mythicSkillName == null || mythicSkillName.isBlank()) {
            throw new IllegalArgumentException("mythic-skill-name kosong: " + id);
        }
        return new SkillDef(owner, id, name, phrase, SkillType.MYTHIC_MOBS, mythicSkillName, null, cost, cooldown);
    }

    public JavaPlugin owner() {
        return owner;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String phrase() {
        return phrase;
    }

    public SkillType type() {
        return type;
    }

    public String mythicSkillName() {
        return mythicSkillName;
    }

    public NativeSkill nativeSkill() {
        return nativeSkill;
    }

    public double cost() {
        return cost.getAsDouble();
    }

    public double cooldown() {
        return cooldown.getAsDouble();
    }
}
