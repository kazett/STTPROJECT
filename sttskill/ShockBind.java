package id.stt.skill;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class ShockBind implements Skill, Listener {

    private final JavaPlugin plugin;
    private final Set<UUID> stunned = new HashSet<>();

    public ShockBind(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "shock_bind";
    }

    @Override
    public String phrase() {
        return "shock bind";
    }

    @Override
    public String name() {
        return "Shock Bind";
    }

    @Override
    public Map<String, Object> defaults() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("cost", 15.0);
        map.put("cooldown", 8.0);
        map.put("radius", 4.0);
        map.put("duration", 3.0);
        return map;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!stunned.contains(event.getPlayer().getUniqueId())) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        to.setX(from.getX());
        to.setZ(from.getZ());
        event.setTo(to);
    }

    @Override
    public void cast(Player caster, SkillContext ctx) {
        double radius = ctx.num("radius", 4);
        int ticks = (int) (ctx.num("duration", 3) * 20);
        Location center = caster.getLocation();
        center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1f, 1.6f);
        for (int i = 0; i < 40; i++) {
            double angle = Math.PI * 2 * i / 40;
            center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                    center.getX() + Math.cos(angle) * radius, center.getY() + 0.2, center.getZ() + Math.sin(angle) * radius,
                    2, 0.1, 0.1, 0.1, 0.1);
        }
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity target) || entity instanceof ArmorStand || entity.equals(caster) || Summons.ownedBy(entity, caster)) {
                continue;
            }
            if (target.getLocation().distance(center) > radius) {
                continue;
            }
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, ticks, 6, false, false, true));
            target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, ticks, 128, false, false, false));
            target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.2);
            if (target instanceof Player victim) {
                UUID id = victim.getUniqueId();
                stunned.add(id);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> stunned.remove(id), ticks);
            }
        }
    }
}
