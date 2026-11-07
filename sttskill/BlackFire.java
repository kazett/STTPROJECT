package id.stt.skill;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class BlackFire implements Skill {

    private static final double CONE_COS = 0.85;

    private final JavaPlugin plugin;

    public BlackFire(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "black_fire";
    }

    @Override
    public String name() {
        return "Black Fire";
    }

    @Override
    public String phrase() {
        return "black fire";
    }

    @Override
    public Map<String, Object> defaults() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("cost", 45.0);
        map.put("cooldown", 10.0);
        map.put("range", 15.0);
        map.put("duration", 5.0);
        map.put("damage-per-sec", 4.0);
        // true = black fire juga bisa kuncian mobs (buat tes sendirian), false = cuma player lain
        map.put("hit-mobs", false);
        return map;
    }

    @Override
    public void cast(Player player, SkillContext ctx) {
        double range = ctx.num("range", 15);
        double durationSec = ctx.num("duration", 5);
        double damagePerHit = ctx.num("damage-per-sec", 4.0) / 2.0;
        boolean hitMobs = ctx.bool("hit-mobs", false); // true = bisa kena mobs buat tes sendirian
        int totalTicks = (int) (durationSec * 20);

        // Cari target lewat RayTrace / Cone
        LivingEntity target = findTarget(player, range, hitMobs);
        if (target == null) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1f);
            return;
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.5f, 0.6f);

        // Task efek api hitam yang selalu nempel / ngikutin target
        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                ticks += 10;
                if (!target.isValid() || target.isDead() || ticks > totalTicks) {
                    cancel();
                    return;
                }

                // Efek damage & potion efek Wither
                target.damage(damagePerHit, player);
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 1, false, false, false));

                // Partikel Api Hitam (Spawning tepat di posisi target terkini)
                Location loc = target.getLocation().add(0, target.getHeight() / 2.0, 0);
                target.getWorld().spawnParticle(Particle.SQUID_INK, loc, 15, 0.3, 0.5, 0.3, 0.02);
                target.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 10, 0.2, 0.4, 0.2, 0.01);
                target.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 8, 0.2, 0.4, 0.2, 0.01);
            }
        }.runTaskTimer(plugin, 1L, 10L);
    }

    private LivingEntity findTarget(Player player, double range, boolean hitMobs) {
        RayTraceResult ray = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                range,
                0.8,
                entity -> isValidTarget(entity, player, hitMobs)
        );
        if (ray != null && ray.getHitEntity() instanceof LivingEntity living) {
            return living;
        }
        // Fallback: Cari target terdekat dalam cone depan jika raytrace tidak presisi
        LivingEntity nearest = null;
        double bestDist = range * range;
        Location eye = player.getEyeLocation();
        Vector facing = eye.getDirection();
        for (Entity e : player.getNearbyEntities(range, range, range)) {
            if (isValidTarget(e, player, hitMobs)) {
                Vector toTarget = e.getLocation().add(0, e.getHeight() / 2.0, 0).toVector().subtract(eye.toVector());
                if (toTarget.lengthSquared() == 0 || toTarget.normalize().dot(facing) < CONE_COS) {
                    continue;
                }
                double dist = player.getLocation().distanceSquared(e.getLocation());
                if (dist < bestDist) {
                    bestDist = dist;
                    nearest = (LivingEntity) e;
                }
            }
        }
        return nearest;
    }

    private boolean isValidTarget(Entity entity, Player caster, boolean hitMobs) {
        if (!(entity instanceof LivingEntity) || entity instanceof ArmorStand || entity.equals(caster) || Summons.ownedBy(entity, caster)) {
            return false;
        }
        if (!hitMobs && !(entity instanceof Player)) {
            return false; // Cuma player lain kalau hit-mobs = false
        }
        return caster.hasLineOfSight(entity);
    }
}
