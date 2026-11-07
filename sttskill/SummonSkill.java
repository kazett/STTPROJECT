package id.stt.skill;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public final class SummonSkill implements Skill {

    private final Summons summons;
    private final String id;
    private final String name;
    private final String phrase;
    private final double cost;
    private final Class<? extends Mob> type;
    private final Consumer<Mob> setup;

    public SummonSkill(Summons summons, String id, String name, String phrase, double cost, Class<? extends Mob> type, Consumer<Mob> setup) {
        this.summons = summons;
        this.id = id;
        this.name = name;
        this.phrase = phrase;
        this.cost = cost;
        this.type = type;
        this.setup = setup;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String phrase() {
        return phrase;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Map<String, Object> defaults() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("cost", cost);
        map.put("cooldown", 8.0);
        // lama summon hidup (detik) dan jarak mob mencari target (block)
        map.put("duration", 120.0);
        map.put("range", 30.0);
        return map;
    }

    @Override
    public void cast(Player player, SkillContext ctx) {
        summons.spawn(player, type, setup, ctx.num("duration", 120), ctx.num("range", 30));
    }
}
