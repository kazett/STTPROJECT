package id.stt.skill;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class SwapStep implements Skill {

    private final JavaPlugin plugin;

    public SwapStep(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "step_swap";
    }

    @Override
    public String name() {
        return "Step Swap";
    }

    @Override
    public String phrase() {
        return "swap step";
    }

    @Override
    public Map<String, Object> defaults() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("cost", 25.0);
        map.put("cooldown", 8.0);
        // jarak maksimal player target di depan (block), target harus kelihatan (tidak terhalang block)
        map.put("range", 8.0);
        return map;
    }

    @Override
    public boolean canCast(Player player) {
        return findTarget(player) != null;
    }

    @Override
    public void cast(Player player, SkillContext ctx) {
        Player target = findTarget(player);
        if (target == null) {
            return;
        }
        Location from = player.getLocation();
        Location to = target.getLocation();
        Location playerDestination = to.clone();
        playerDestination.setYaw(from.getYaw());
        playerDestination.setPitch(from.getPitch());
        Location targetDestination = from.clone();
        targetDestination.setYaw(to.getYaw());
        targetDestination.setPitch(to.getPitch());
        from.getWorld().spawnParticle(Particle.PORTAL, from.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.5);
        from.getWorld().playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        to.getWorld().spawnParticle(Particle.PORTAL, to.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.5);
        to.getWorld().playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        player.teleport(playerDestination);
        target.teleport(targetDestination);
        player.setFallDistance(0);
        target.setFallDistance(0);
    }

    private Player findTarget(Player player) {
        double range = plugin.getConfig().getDouble("skills." + id() + ".range", 8);
        Location eye = player.getEyeLocation();
        RayTraceResult ray = player.getWorld().rayTraceEntities(eye, eye.getDirection(), range, 0.8, entity -> isValid(entity, player));
        if (ray == null || !(ray.getHitEntity() instanceof Player target) || ray.getHitPosition() == null) {
            return null;
        }
        // rayTraceEntities tidak peduli block, jadi pastikan tidak ada block antara mata caster dan titik yang kena crosshair
        Vector delta = ray.getHitPosition().clone().subtract(eye.toVector());
        double distance = delta.length();
        if (distance > 0.2) {
            RayTraceResult wall = player.getWorld().rayTraceBlocks(eye, delta.normalize(), distance - 0.1, FluidCollisionMode.NEVER, true);
            if (wall != null) {
                return null;
            }
        }
        return target;
    }

    private boolean isValid(Entity entity, Player caster) {
        return entity instanceof Player other && !other.equals(caster) && other.getGameMode() != GameMode.SPECTATOR && caster.hasLineOfSight(other);
    }
}
