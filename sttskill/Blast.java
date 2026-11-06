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
            if (!(entity instanceof LivingEntity target) || entity instanceof ArmorStand || entity.equals(caster)) {
                continue;
            }
            if (target.getLocation().distance(center) > radius) {
                continue;
            }
            if (damage > 0) {
                target.damage(damage, caster);
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

    static void dig(JavaPlugin plugin, Player caster, Location center, float power, double depth) {
        World world = center.getWorld();
        double step = power * 0.5;
        int layers = (int) Math.ceil(depth / step);
        blow(caster, center, power);
        for (int k = 1; k <= layers; k++) {
            double y = center.getY() - step * k;
            Bukkit.getScheduler().runTaskLater(plugin, () -> blow(caster, new Location(world, center.getX(), y, center.getZ()), power), k);
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
