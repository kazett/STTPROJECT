package id.stt.skill;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class MeteorCrash implements Skill, Listener {

    private final JavaPlugin plugin;
    private final Set<UUID> active = new HashSet<>();

    public MeteorCrash(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "meteor";
    }

    @Override
    public String phrase() {
        return "meteor crash";
    }

    @Override
    public String name() {
        return "Meteor";
    }

    @Override
    public Map<String, Object> defaults() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("cost", 40.0);
        map.put("cooldown", 8.0);
        map.put("height", 20.0);
        map.put("up-speed", 1.0);
        map.put("down-speed", 2.5);
        map.put("radius", 6.0);
        map.put("damage", 16.0);
        // kekuatan ledakan asli (TNT = 4)
        map.put("power", 5.0);
        // kedalaman lubang ledakan (block)
        map.put("depth", 10.0);
        return map;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && active.contains(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        active.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public void cast(Player player, SkillContext ctx) {
        double height = ctx.num("height", 20);
        double up = ctx.num("up-speed", 1.0);
        double down = ctx.num("down-speed", 2.5);
        double radius = ctx.num("radius", 6);
        double damage = ctx.num("damage", 16);
        double power = ctx.num("power", 5);
        double depth = ctx.num("depth", 10);
        UUID id = player.getUniqueId();
        double startY = player.getLocation().getY();

        active.add(id);
        player.setFallDistance(0);
        player.setVelocity(new Vector(0, up, 0));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.7f);

        new BukkitRunnable() {
            private boolean rising = true;
            private double lastY = startY;
            private int stuck;
            private int ticks;
            private int slamTicks;

            @Override
            public void run() {
                ticks++;
                if (!player.isOnline() || player.isDead() || ticks > 120) {
                    finish();
                    return;
                }
                player.setFallDistance(0);
                Location loc = player.getLocation();
                double y = loc.getY();
                if (ticks > 3) {
                    stuck = (rising ? y - lastY : lastY - y) < 0.05 ? stuck + 1 : 0;
                }
                lastY = y;
                player.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 0.5, 0), 8, 0.3, 0.4, 0.3, 0.02);

                if (rising) {
                    if (y >= startY + height || stuck >= 2) {
                        rising = false;
                        stuck = 0;
                        player.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);
                    } else {
                        player.setVelocity(new Vector(0, up, 0));
                        return;
                    }
                }

                slamTicks++;
                if (slamTicks > 2 && (player.isOnGround() || stuck >= 15)) {
                    Blast.explode(player, player.getLocation(), radius, damage);
                    Blast.dig(plugin, player, player.getLocation(), (float) power, depth);
                    finish();
                    return;
                }
                player.setVelocity(new Vector(0, -down, 0));
            }

            private void finish() {
                cancel();
                if (player.isOnline()) {
                    player.setFallDistance(0);
                }
                // Hapus perlindungan jatuh setelah mendarat sempurna (max 100 ticks)
                new BukkitRunnable() {
                    private int delayTicks = 0;
                    @Override
                    public void run() {
                        delayTicks++;
                        if (!player.isOnline() || player.isOnGround() || delayTicks >= 100) {
                            if (player.isOnline()) player.setFallDistance(0);
                            active.remove(id);
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 1L, 2L);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
