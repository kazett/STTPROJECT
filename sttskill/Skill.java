package id.stt.skill;

import java.util.Map;
import org.bukkit.entity.Player;

public interface Skill {

    /** ID skill, huruf kecil dan underscore. */
    String id();

    /** Nama tampil. */
    String name();

    /** Kata yang harus diucapkan (huruf kecil, bahasa Inggris, boleh beberapa kata). */
    String phrase();

    /** Semua angka tuning default (key tanpa prefix), termasuk cost dan cooldown. Dipakai untuk mengisi config.yml. */
    Map<String, Object> defaults();

    void cast(Player player, SkillContext ctx);

    /** Return false kalau skill tidak perlu dipakai sekarang (mana dan cooldown tidak kepotong). */
    default boolean canCast(Player player) {
        return true;
    }

    /** Dipanggil saat plugin dimatikan, buat ngebalikin state yang diubah skill. */
    default void shutdown() {
    }
}
