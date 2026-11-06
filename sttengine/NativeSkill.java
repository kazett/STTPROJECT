package id.stt.engine.skill;

import org.bukkit.entity.Player;

/** Kode eksekusi skill NATIVE. Dipanggil engine setelah cek unlock, cooldown, dan mana. */
public interface NativeSkill {

    /** Return false kalau skill tidak perlu dipakai sekarang (mana dan cooldown tidak kepotong). */
    default boolean canCast(Player player) {
        return true;
    }

    void cast(Player player);

    /** Dipanggil saat plugin pemilik skill dimatikan. */
    default void shutdown() {
    }
}
