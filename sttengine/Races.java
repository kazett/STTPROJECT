package id.stt.engine.race;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;

/** Daftar ras. Ras baru ditambahkan lewat kode di sini. Build awal: Witch saja. */
public final class Races {

    private static final Map<String, RaceDef> ALL = new LinkedHashMap<>();

    static {
        add(new RaceDef("witch", "&dRace Witch", Material.CAULDRON,
                List.of("heal", "step_swap", "black_fire", "meteor")));
    }

    private Races() {
    }

    private static void add(RaceDef race) {
        ALL.put(race.id(), race);
    }

    public static RaceDef get(String id) {
        return id == null ? null : ALL.get(id);
    }

    public static List<RaceDef> all() {
        return new ArrayList<>(ALL.values());
    }
}
