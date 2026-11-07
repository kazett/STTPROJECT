package id.stt.skill;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class Summons implements Listener {

    private record Summon(UUID owner, long expireAt, double range) {
    }

    private static final String META_KEY = "stt_summon_mob";
    private final JavaPlugin plugin;
    private final Map<UUID, Summon> summons = new HashMap<>();
    private BukkitTask task;

    public Summons(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        for (UUID id : new ArrayList<>(summons.keySet())) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
        summons.clear();

        // Cleanup tambahan jika ada mob tertinggal di world saat reload/stop
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.hasMetadata(META_KEY)) {
                    entity.remove();
                }
            }
        }
    }

    public <T extends Mob> void spawn(Player owner, Class<T> type, Consumer<? super T> setup, double duration, double range) {
        Location base = owner.getLocation();
        double yaw = Math.toRadians(base.getYaw());
        Location at = base.clone().add(-Math.sin(yaw) * 2.5, 0, Math.cos(yaw) * 2.5);
        if (!at.getBlock().isPassable() || !at.clone().add(0, 1, 0).getBlock().isPassable()) {
            at = base;
        }
        // 1.20.1: parameter terakhir bertipe org.bukkit.util.Consumer, bukan java.util.function.Consumer
        T mob = at.getWorld().spawn(at, type, Warden.class.isAssignableFrom(type), (org.bukkit.util.Consumer<T>) created -> setup.accept(created));
        mob.setPersistent(false);
        mob.setRemoveWhenFarAway(false);
        mob.setMetadata(META_KEY, new FixedMetadataValue(plugin, owner.getUniqueId().toString()));
        long expireAt = System.currentTimeMillis() + (long) (duration * 1000);
        summons.put(mob.getUniqueId(), new Summon(owner.getUniqueId(), expireAt, range));
    }

    static void zombie(Mob mob) {
        if (mob instanceof Zombie zombie) {
            zombie.setBaby(false);
        }
        diamondArmor(mob);
        mob.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
    }

    static void skeleton(Mob mob) {
        diamondArmor(mob);
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addEnchantment(Enchantment.ARROW_DAMAGE, 3);
        mob.getEquipment().setItemInMainHand(bow);
    }

    private static void diamondArmor(Mob mob) {
        EntityEquipment equipment = mob.getEquipment();
        equipment.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        equipment.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        equipment.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        equipment.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
    }

    static boolean ownedBy(Entity entity, Player owner) {
        String ownerId = owner.getUniqueId().toString();
        for (MetadataValue value : entity.getMetadata(META_KEY)) {
            if (ownerId.equals(value.asString())) {
                return true;
            }
        }
        return false;
    }

    private static boolean valid(Player player, UUID owner) {
        GameMode mode = player.getGameMode();
        return !player.getUniqueId().equals(owner) && !player.isDead()
                && (mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (UUID id : new ArrayList<>(summons.keySet())) {
            Summon summon = summons.get(id);
            if (summon == null) {
                continue;
            }
            if (!(Bukkit.getEntity(id) instanceof Mob mob) || !mob.isValid()) {
                summons.remove(id);
                continue;
            }
            if (now >= summon.expireAt()) {
                summons.remove(id);
                mob.remove();
                continue;
            }
            retarget(mob, summon);
        }
    }

    private void retarget(Mob mob, Summon summon) {
        Player nearest = null;
        double best = summon.range() * summon.range();
        for (Player player : mob.getWorld().getPlayers()) {
            if (!valid(player, summon.owner())) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(mob.getLocation());
            if (distance <= best) {
                best = distance;
                nearest = player;
            }
        }
        if (mob instanceof Warden warden) {
            keepDigCooldown(warden);
            LivingEntity angry = warden.getEntityAngryAt();
            for (int i = 0; i < 5 && angry != null && !(angry instanceof Player p && valid(p, summon.owner())); i++) {
                warden.clearAnger(angry);
                angry = warden.getEntityAngryAt();
            }
            if (nearest != null) {
                warden.setAnger(nearest, 150);
            }
            return;
        }
        if (mob.getTarget() != nearest) {
            mob.setTarget(nearest);
        }
    }

    private static void keepDigCooldown(Warden warden) {
        try {
            Object handle = warden.getClass().getMethod("getHandle").invoke(warden);
            Class<?> ai = Class.forName("net.minecraft.world.entity.monster.warden.WardenAi");
            for (Method method : ai.getMethods()) {
                if (method.getName().equals("setDigCooldown") && method.getParameterCount() == 1) {
                    method.invoke(null, handle);
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        Summon summon = summons.get(event.getEntity().getUniqueId());
        if (summon == null || event.getTarget() == null) {
            return;
        }
        if (!(event.getTarget() instanceof Player player) || !valid(player, summon.owner())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            damager = shooter;
        }
        Summon summon = summons.get(damager.getUniqueId());
        if (summon == null) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim) || victim.getUniqueId().equals(summon.owner())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (summons.remove(event.getEntity().getUniqueId()) != null) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }
}
