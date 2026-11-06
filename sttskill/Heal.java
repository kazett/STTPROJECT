package id.stt.skill;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class Heal implements Skill {

    @Override
    public String id() {
        return "heal";
    }

    @Override
    public String name() {
        return "Heal";
    }

    @Override
    public String phrase() {
        return "heal";
    }

    @Override
    public Map<String, Object> defaults() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("cost", 20.0);
        map.put("cooldown", 8.0);
        // jumlah darah yang dipulihkan (8 = 4 hati)
        map.put("amount", 8.0);
        return map;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canCast(Player player) {
        return player.getHealth() < player.getMaxHealth() - 0.01;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void cast(Player player, SkillContext ctx) {
        double amount = ctx.num("amount", 8);
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + amount));
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1.8, 0), 8, 0.4, 0.3, 0.4);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
    }
}
