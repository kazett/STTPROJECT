package id.stt.skill;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public final class FireballSkill implements Skill, Listener {

    private final JavaPlugin plugin;
    private final NamespacedKey key;

    public FireballSkill(JavaPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "stt_fireball");
    }

    @Override
    public String id() {
        return "fireball";
    }

    @Override
    public String phrase() {
        return "fire ball";
    }

    @Override
    public String name() {
        return "Fireball";
    }

    @Override
    public Map<String, Object> defaults() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("cost", 20.0);
        map.put("cooldown", 8.0);
        map.put("speed", 1.5);
        map.put("radius", 4.0);
        map.put("damage", 10.0);
        return map;
    }

    @Override
    public void cast(Player player, SkillContext ctx) {
        double speed = ctx.num("speed", 1.5);
        Vector dir = player.getEyeLocation().getDirection().normalize();
        Fireball ball = player.launchProjectile(Fireball.class, dir.clone().multiply(speed));
        ball.setDirection(dir);
        ball.setVelocity(dir.clone().multiply(speed));
        ball.setIsIncendiary(false);
        ball.setYield(0f);
        ball.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (ball.isValid()) {
                ball.remove();
            }
        }, 60L);
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Fireball ball)) {
            return;
        }
        if (!ball.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
            return;
        }
        event.setCancelled(true);
        Location at = ball.getLocation();
        ball.remove();
        if (ball.getShooter() instanceof Player caster) {
            double radius = plugin.getConfig().getDouble("skills.fireball.radius", 4);
            double damage = plugin.getConfig().getDouble("skills.fireball.damage", 10);
            Blast.explode(caster, at, radius, damage);
        }
    }
}
