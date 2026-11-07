package id.stt.skill;

import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Damage skill yang tidak dikurangi armor. Damage biasa (LivingEntity#damage) dipotong armor dan enchant,
 * jadi Black Fire (2 per tick-hit) dan ledakan meteor hampir tidak terasa ke player berarmor.
 * Kalau ignore-armor false di config.yml sttskill, kembali ke damage biasa.
 */
final class TrueDamage {

    static volatile boolean ignoreArmor = true;

    private TrueDamage() {
    }

    static void apply(Player caster, LivingEntity target, double amount) {
        if (amount <= 0 || target.isDead()) {
            return;
        }
        if (!ignoreArmor) {
            target.damage(amount, caster);
            return;
        }
        if (target.isInvulnerable()) {
            return;
        }
        if (target instanceof Player victim && (victim.getGameMode() == GameMode.CREATIVE || victim.getGameMode() == GameMode.SPECTATOR)) {
            return;
        }
        double hp = target.getHealth() + target.getAbsorptionAmount();
        if (amount >= hp) {
            // mematikan: lewat damage biasa supaya kill credit, death message, dan totem tetap jalan
            target.damage(1_000_000, caster);
            return;
        }
        // damage kecil dulu untuk animasi luka, knockback, dan atribusi pelaku, sisanya dipotong langsung dari health
        target.damage(0.01, caster);
        if (target.isDead()) {
            return;
        }
        double absorption = target.getAbsorptionAmount();
        double left = amount;
        if (absorption > 0) {
            double taken = Math.min(absorption, left);
            target.setAbsorptionAmount(absorption - taken);
            left -= taken;
        }
        if (left > 0) {
            target.setHealth(Math.max(0.1, target.getHealth() - left));
        }
    }
}
