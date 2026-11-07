package id.stt.skill;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class SonicDash implements Skill {

    @Override
    public String id() {
        return "sonic_dash";
    }

    @Override
    public String phrase() {
        return "sonic dash";
    }

    @Override
    public String name() {
        return "Sonic Dash";
    }

    @Override
    public Map<String, Object> defaults() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("cost", 12.0);
        map.put("cooldown", 8.0);
        map.put("power", 3.5);
        return map;
    }

    @Override
    public void cast(Player player, SkillContext ctx) {
        double power = ctx.num("power", 3.5);
        Location loc = player.getLocation();
        double yaw = Math.toRadians(loc.getYaw());
        Vector push = new Vector(-Math.sin(yaw), 0, Math.cos(yaw)).multiply(power);
        push.setY(0.2);
        player.setVelocity(push);
        player.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.4f);
        player.getWorld().spawnParticle(Particle.SONIC_BOOM, loc.clone().add(0, 1, 0), 1);

        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (!player.isOnline() || ++ticks > 8) {
                    cancel();
                    return;
                }
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.5, 0), 6, 0.3, 0.3, 0.3, 0.02);
            }
        }.runTaskTimer(ctx.plugin(), 1L, 1L);
    }
}
