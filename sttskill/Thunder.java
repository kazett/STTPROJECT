package id.stt.skill;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public final class Thunder implements Skill {

    @Override
    public String id() {
        return "thunder";
    }

    @Override
    public String phrase() {
        return "thunder";
    }

    @Override
    public String name() {
        return "Thunder";
    }

    @Override
    public Map<String, Object> defaults() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("cost", 10.0);
        map.put("cooldown", 8.0);
        map.put("radius", 7.0);
        map.put("damage", 8.0);
        map.put("hit-mobs", false);
        return map;
    }

    @Override
    public void cast(Player caster, SkillContext ctx) {
        double radius = ctx.num("radius", 7);
        double damage = ctx.num("damage", 8);
        boolean hitMobs = ctx.bool("hit-mobs", false);
        Location center = caster.getLocation();
        for (Player target : caster.getWorld().getNearbyPlayers(center, radius)) {
            if (target.equals(caster)) {
                continue;
            }
            strike(caster, target, damage);
        }
        if (hitMobs) {
            for (Entity mob : caster.getWorld().getNearbyEntities(center, radius, radius, radius, e -> e instanceof Mob && !Summons.ownedBy(e, caster))) {
                strike(caster, (LivingEntity) mob, damage);
            }
        }
    }

    private static void strike(Player caster, LivingEntity target, double damage) {
        target.getWorld().strikeLightningEffect(target.getLocation());
        if (damage > 0) {
            target.damage(damage, caster);
        }
    }
}
