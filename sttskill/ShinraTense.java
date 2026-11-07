package id.stt.skill;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public final class ShinraTense implements Skill, Listener {

    private static final String SOUND = "stt:shinra_tensei";

    private final JavaPlugin plugin;
    private final Set<UUID> frozen = new HashSet<>();
    private final Set<UUID> levitating = new HashSet<>();
    private final Set<UUID> safe = new HashSet<>();
    private final Map<UUID, Boolean> flyBackup = new HashMap<>();
    /** Pemain yang lagi channel Shinra Tense (dari dipakai sampai ledakan). */
    private final Set<UUID> channel = new HashSet<>();

    public ShinraTense(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "shinra_tense";
    }

    @Override
    public String phrase() {
        return "sheen ra ten say";
    }

    @Override
    public String name() {
        return "Shinra Tense";
    }

    @Override
    public Map<String, Object> defaults() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("cost", 250.0);
        map.put("cooldown", 60.0);
        map.put("charge", 5.5);
        map.put("thunder", 17.85);
        map.put("explosion", 30.5);
        map.put("radius", 30.0);
        map.put("damage", 40.0);
        map.put("power", 20.0);
        map.put("slow-level", 5.0);
        return map;
    }

    /** True kalau player lagi channel Shinra Tense dan belum meledak. */
    public boolean channeling(Player player) {
        return channel.contains(player.getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (event.getTo() == null) {
            return;
        }
        if (frozen.contains(id)) {
            Location lock = event.getFrom().clone();
            lock.setYaw(event.getTo().getYaw());
            lock.setPitch(event.getTo().getPitch());
            event.setTo(lock);
        } else if (levitating.contains(id)) {
            Location lock = event.getTo().clone();
            lock.setX(event.getFrom().getX());
            lock.setZ(event.getFrom().getZ());
            event.setTo(lock);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && safe.contains(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        restore(event.getPlayer());
        channel.remove(event.getPlayer().getUniqueId());
        safe.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public void shutdown() {
        for (UUID id : new HashSet<>(flyBackup.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                restore(player);
            }
        }
        for (UUID id : new HashSet<>(levitating)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                restore(player);
            }
        }
        frozen.clear();
        levitating.clear();
        safe.clear();
        channel.clear();
    }

    private void restore(Player player) {
        UUID id = player.getUniqueId();
        frozen.remove(id);
        if (levitating.remove(id)) {
            player.removePotionEffect(PotionEffectType.LEVITATION);
        }
        Boolean before = flyBackup.remove(id);
        if (before != null && !before) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
    }

    @Override
    public void cast(Player player, SkillContext ctx) {
        int chargeTicks = (int) Math.round(ctx.num("charge", 5.5) * 20);
        int thunderTicks = Math.max(chargeTicks + 1, (int) Math.round(ctx.num("thunder", 17.85) * 20));
        int explosionTicks = Math.max(thunderTicks + 1, (int) Math.round(ctx.num("explosion", 30.5) * 20));
        double radius = ctx.num("radius", 30);
        double damage = ctx.num("damage", 40);
        double power = ctx.num("power", 20);
        int slowLevel = (int) ctx.num("slow-level", 5);
        UUID id = player.getUniqueId();
        Location base = player.getLocation().clone();

        safe.add(id);
        channel.add(id);
        frozen.add(id);
        player.setFallDistance(0);
        base.getWorld().playSound(base, SOUND, SoundCategory.MASTER, 4f, 1f);

        new BukkitRunnable() {
            private int phase;
            private int ticks;
            private int fall;

            @Override
            public void run() {
                ticks++;
                if (!player.isOnline() || player.isDead() || ticks > explosionTicks + 400) {
                    finish();
                    return;
                }
                player.setFallDistance(0);
                Location loc = player.getLocation();
                World world = loc.getWorld();

                if (phase == 0) {
                    if (ticks < chargeTicks) {
                        return;
                    }
                    phase = 1;
                    frozen.remove(id);
                    levitating.add(id);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, thunderTicks - chargeTicks, 0, false, false, false));
                    return;
                }

                if (phase == 1) {
                    world.spawnParticle(Particle.CLOUD, loc.clone().add(0, 0.5, 0), 8, 0.3, 0.4, 0.3, 0.02);
                    if (ticks < thunderTicks) {
                        return;
                    }
                    phase = 2;
                    levitating.remove(id);
                    player.removePotionEffect(PotionEffectType.LEVITATION);
                    flyBackup.put(id, player.getAllowFlight());
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    frozen.add(id);
                    for (int i = 0; i < 3; i++) {
                        world.strikeLightningEffect(loc);
                    }
                    int effectTicks = explosionTicks - ticks + 40;
                    for (Entity entity : world.getNearbyEntities(base, radius, radius, radius)) {
                        if (!(entity instanceof LivingEntity target) || entity instanceof ArmorStand || entity.equals(player) || Summons.ownedBy(entity, player)) {
                            continue;
                        }
                        if (target.getLocation().distance(base) > radius) {
                            continue;
                        }
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, effectTicks, Math.max(0, slowLevel - 1), false, true, true));
                        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, effectTicks, 0, false, true, true));
                        if (target instanceof Player) {
                            world.strikeLightningEffect(target.getLocation());
                        }
                    }
                    return;
                }

                if (phase == 2) {
                    if (ticks % 5 == 0) {
                        for (int i = 0; i < 48; i++) {
                            double angle = Math.PI * 2 * i / 48;
                            world.spawnParticle(Particle.FLAME, base.getX() + Math.cos(angle) * radius,
                                    base.getY() + 0.2, base.getZ() + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
                        }
                        world.spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 1, 0), 15, 0.5, 0.8, 0.5, 0.3);
                    }
                    if (ticks >= explosionTicks) {
                        Blast.explode(player, base, radius, damage);
                        Blast.destroy(plugin, player, base, radius, (float) power);
                        for (int i = 0; i < 6; i++) {
                            double angle = Math.PI * 2 * i / 6;
                            world.spawnParticle(Particle.EXPLOSION_HUGE,
                                    base.getX() + Math.cos(angle) * radius * 0.6, base.getY() + 0.5,
                                    base.getZ() + Math.sin(angle) * radius * 0.6, 1);
                        }
                        frozen.remove(id);
                        restore(player);
                        channel.remove(id);
                        phase = 3;
                    }
                    return;
                }

                fall++;
                if (fall > 2 && player.isOnGround()) {
                    finish();
                }
            }

            private void finish() {
                cancel();
                channel.remove(id);
                restore(player);
                new BukkitRunnable() {
                    private int delayTicks = 0;

                    @Override
                    public void run() {
                        delayTicks++;
                        if (!player.isOnline() || player.isOnGround() || delayTicks >= 100) {
                            if (player.isOnline()) {
                                player.setFallDistance(0);
                            }
                            safe.remove(id);
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 1L, 2L);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
