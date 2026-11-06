package id.stt.engine.voice;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import id.stt.engine.skill.SkillService;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Satu-satunya titik yang menyentuh kelas Simple Voice Chat dari SttEngine.
 * Dipisah supaya SttEngine tetap bisa dimuat kalau Simple Voice Chat tidak terpasang.
 */
public final class VoiceLink {

    private VoiceLink() {
    }

    /** Return SttVoice (sebagai Object) kalau berhasil, null kalau layanan SVC tidak ada. */
    public static Object start(JavaPlugin plugin, VoiceManager manager, SkillService skills) {
        BukkitVoicechatService service = plugin.getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service == null) {
            return null;
        }
        SttVoice voice = new SttVoice(plugin, manager, skills);
        service.registerPlugin(voice);
        return voice;
    }

    public static void stop(Object voice) {
        if (voice instanceof SttVoice v) {
            v.shutdown();
        }
    }
}
