package id.stt.skill;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

/** Ledakan custom: efek + damage ke semua makhluk hidup di radius, tanpa api dan tanpa rusak block. */
final class Blast {

    private Blast() {
    }

    static void explode(Player caster, Location center, double radius, double damage) {
        World world = center.getWorld();
        world.spawnParticle(Particle.EXPLOSION_HUGE, center, 1);
        world.spawnParticle(Particle.FLAME, center, 60, radius / 3, 0.4, radius / 3, 0.05);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.9f);
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity target) || entity instanceof ArmorStand || entity.equals(caster) || Summons.ownedBy(entity, caster)) {
                continue;
            }
            if (target.getLocation().distance(center) > radius) {
                continue;
            }
            if (damage > 0) {
                TrueDamage.apply(caster, target, damage);
            }
            Vector push = target.getLocation().toVector().subtract(center.toVector());
            push.setY(0);
            if (push.lengthSquared() > 0.0001) {
                push.normalize().multiply(0.6);
            }
            push.setY(0.4);
            target.setVelocity(push);
        }
    }

    static void destroy(JavaPlugin plugin, Player caster, Location center, double radius, float power) {
        World world = center.getWorld();
        blow(caster, center, power);
        double reach = radius - power;
        double step = power * 1.6;
        int rings = (int) Math.round(reach / step);
        for (int k = 1; k <= rings; k++) {
            double ringRadius = reach * k / rings;
            int count = Math.max(6, (int) Math.ceil(Math.PI * 2 * ringRadius / step));
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (int i = 0; i < count; i++) {
                    double angle = Math.PI * 2 * i / count;
                    Location at = new Location(world, center.getX() + Math.cos(angle) * ringRadius, center.getY(), center.getZ() + Math.sin(angle) * ringRadius);
                    blow(caster, at, power);
                }
            }, k * 2L);
        }
    }

    /**
     * Kawah lebar dan dangkal: satu ledakan di tengah (setinggi pinggang supaya tidak menggali dalam),
     * lalu cincin ledakan kecil sejauh spread block di sekelilingnya. Tiap ledakan lewat createExplosion biasa.
     */
    static void crater(JavaPlugin plugin, Player caster, Location center, float power, double spread) {
        Location core = center.clone().add(0, 1, 0);
        blow(caster, core, power);
        int points = Math.max(6, (int) Math.round(spread * 2));
        float ringPower = Math.max(1f, power * 0.7f);
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            Location at = core.clone().add(Math.cos(angle) * spread, 0, Math.sin(angle) * spread);
            Bukkit.getScheduler().runTaskLater(plugin, () -> blow(caster, at, ringPower), 1L + i / 3);
        }
    }

    private static void blow(Player caster, Location at, float power) {
        boolean online = caster.isOnline();
        Vector velocity = caster.getVelocity();
        BlastGuard.run(() -> at.getWorld().createExplosion(at, power, true, true, online ? caster : null));
        if (online) {
            caster.setVelocity(velocity);
        }
    }
}
