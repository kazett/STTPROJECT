package id.stt.engine.race;

import java.util.List;
import org.bukkit.Material;

/** Definisi ras (di kode). Skill di list ini otomatis terbuka saat ras dipilih. */
public record RaceDef(String id, String displayName, Material icon, List<String> skills) {
}
