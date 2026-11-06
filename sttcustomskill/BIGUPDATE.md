# STT - Project Update & Blueprint System (v3.0.0, revisi)

DOWNGRADE VERSION DARI 1.21.11 KE 1.20.1 JADI CEK DETAIL ADA API/SYTAX YG BEDA/BERUBAH CEK DENGAN DETAIL, DIPERHATIIN SEPERTI PROPERSOLA

Dokumen ini adalah spesifikasi FINAL arsitektur baru **STT (Speech-To-Text Skill Engine)**. Semua sistem lama (v2.3.6) yang berbasis relic item DIHAPUS dan digantikan sistem di dokumen ini. Fokus: **Roleplay Race System**, **Dynamic Voice Engine**, dan **proyek modular** (engine, skill, custom skill, resource pack dipisah).

> **Instruksi untuk AI builder:** Ikuti dokumen ini sebagai satu-satunya sumber kebenaran. Hapus semua kode relic, `/stt give relic`, flat 250 mana, `Pack.java`, dan auto-deploy pack (`AssetsDeployer`). Jangan menebak nama class/method API plugin lain (Simple Voice Chat, MythicLib): baca API versi yang terpasang. Jangan ada method kosong (stub) di hasil akhir. Kalau ada hal yang belum ditentukan user (misal phrase mantra), **tanya, jangan dikarang**.

> **Ruang lingkup build awal: HANYA 1 RAS, yaitu Witch.** Ras lain (Summoner, dll) belum ada karena user masih mencari ide ras dan skill yang cocok. **Keberadaan ras dan skill ditambah lewat KODE/JAR** (user tidak perlu mengedit config untuk menambahnya), sedangkan **angka tuning** (cost mana, cooldown, damage, dll) diedit user di `config.yml` plugin skill masing-masing (lihat Bagian 7).

---

## 1. Ringkasan Perubahan Konsep

| Fitur | Sistem Lama (v2.3.6) - DIHAPUS | Sistem Baru (v3.0.0) |
|---|---|---|
| **Pemicu akses skill** | Relic item di inventory | Player memilih ras (permanen), **semua skill ras langsung terbuka**. Tanpa level, tanpa quest |
| **Menu** | - | `/stt menu`: pilih ras (dengan konfirmasi) dan lihat daftar skill |
| **Max mana** | Flat 250 | **Flat 300** untuk semua player (`settings.max-mana`) |
| **Ganti ras** | - | Player **tidak bisa**. Pilih 1x, permanen. Admin bisa reset (`/stt resetrace`) untuk kasus character kill |
| **Struktur proyek** | 1 plugin berisi semuanya | **Dipecah**: `sttengine`, `sttskill`, `sttcustomskill` (3 plugin JAR) + `sttresourcepack` |
| **Resource pack** | Kirim URL GitHub dari `Pack.java` | **Dihapus dari STT.** Aset HUD dan voice ada di `sttresourcepack` |
| **Pack skill custom** | Pasang manual ke MythicMobs | **Tetap dipasang manual** oleh user. Tidak ada auto-deploy dari JAR |
| **Admin RP** | `/stt give relic` | `/stt addskill`, `/stt removeskill`, `/stt resetrace` |
| **Vosk grammar** | Satu grammar untuk semua | **Per player**: hanya skill milik player. Auto-refresh tanpa restart |

---

## 2. Arsitektur Modular

Tujuan pemisahan: tiap update tidak merombak semua file. Menambah skill = kirim source `sttengine` + `sttskill` / `sttcustomskill` saja.

| Bagian | Bentuk | Isi |
|---|---|---|
| **sttengine** | Plugin JAR | Otak utama: data player, ras (definisi ras ada di kode), mana (config, max, regen, burnout), command, Vosk per player, integrasi Simple Voice Chat, dan **yang memanggil/mengeksekusi skill**. Menyediakan API registrasi skill |
| **sttskill** | Plugin JAR | Skill bawaan STT (NATIVE): kode Java, definisi skill di JAR (id, nama, phrase), dan **`config.yml` angka tuning** (cost mana, cooldown, damage, duration, dll). Build awal: skill Witch |
| **sttcustomskill** | Plugin JAR | Definisi skill custom di JAR (id, nama, phrase, nama skill MythicMobs) dan **`config.yml` berisi cost mana + cooldown saja**. Tidak ada damage/duration di sini |
| **sttresourcepack** | Resource pack (bukan plugin) | HUD mana (`.png`), font metadata, file voice/audio (`.ogg`) |

### 2.1 Aturan Dependency (`plugin.yml`)

* `sttengine`: `softdepend: [voicechat, ItemsAdder, MythicMobs, MythicLib]`. Nama plugin Simple Voice Chat di `plugin.yml` **wajib dicek** dari JAR yang terpasang (umumnya `voicechat`), jangan ditebak.
* `sttskill`: `depend: [sttengine]`.
* `sttcustomskill`: `depend: [sttengine]`, `softdepend: [MythicMobs, MythicLib]`.

### 2.2 API Registrasi Skill (di `sttengine`)

* `sttskill` dan `sttcustomskill` **mendaftarkan skill-nya ke `sttengine`** saat `onEnable`. Engine tidak boleh hardcode daftar skill.
* Tiap skill yang terdaftar punya: `id`, `name`, `phrase`, `cost`, `cooldown`, `type` (`NATIVE` atau `MYTHIC_MOBS`), plus data cast sesuai type.
* Pemanggilan (cek unlock, cek mana, cek cooldown, eksekusi) **selalu lewat `sttengine`**, bukan di plugin skill.
* Setiap kali ada plugin skill yang mendaftarkan skill baru, engine **refresh grammar Vosk semua player online** yang terdampak.
* Jika `sttcustomskill` tidak terpasang, ID skill custom di file player **diabaikan, JANGAN dihapus dari file**.

---

## 3. Resource Pack & ItemsAdder

* `Pack.java` dan semua pemanggilnya dihapus. STT **tidak mengirim resource pack sendiri**.
* Aset HUD mana dan voice ada di `sttresourcepack` dan **dipasang manual** (atau lewat ItemsAdder `/iazip` jika user memakainya). ItemsAdder hanya untuk memudahkan pemasangan otomatis, bukan syarat.
* **Jika ItemsAdder tidak aktif:** `sttengine` **tetap jalan**, cukup log warning. Resource pack dipakai manual.
* Tidak ada auto-deploy file dari JAR ke folder `plugins/` (fitur `AssetsDeployer` dihapus). Konsekuensinya masalah "aset tidak ke-update karena skip file yang sudah ada" otomatis hilang.
* Edit di `sttresourcepack` hanya menyentuh file HUD dan file voice.

---

## 4. Pack Skill Custom (MythicMobs) & Dependency MythicLib

Pack skill custom **dipasang manual** oleh user. `sttcustomskill` hanya berisi definisi (di JAR) yang menunjuk ke skill di pack itu, plus `config.yml` untuk cost mana dan cooldown.

### 4.1 Checklist Wajib AI Builder Saat Membaca Pack

Pack yang dikirim user bisa berubah-ubah dan **nama file, nama skill, dan isi folder BERBEDA di tiap pack**. Jangan menyamakan satu pack dengan pack lain dan jangan menebak nama. Untuk TIAP pack:

1. **Inventaris yang benar-benar ada.** Daftar semua folder dan file di pack. Folder yang tidak ada (misal `ModelEngine/`) berarti pack itu tidak memakainya.
2. **Cocokkan ID lintas file** (jika file-nya ada di pack itu):
   * ID skill di `MythicMobs/Skills/*.yml` harus cocok dengan `mythic-skill-name` di definisi skill. Jika pack punya `MythicLib/skill/*.yml`, cek juga ID-nya cocok (**nama key penghubung ikuti isi pack itu sendiri**, jangan ditebak), walau file itu tidak dipakai STT.
   * Nama model di `MythicMobs/Mobs/*.yml` harus ada di `ModelEngine/blueprints/`.
   * Nama sound di `sounds.json` dan di skill harus cocok dengan file `.ogg`.
3. **Varian file dengan ID skill yang sama.** Jangan memasang dua varian sekaligus (ID duplikat saling menimpa). **Keputusan user: pakai varian `MMOCORE`** (satu mantra = satu skill langsung jalan). Varian `MMOITEMS` dipakai untuk item, bukan voice. Varian MMOCORE **tidak butuh** plugin MMOCore/MMOItems, tapi tetap butuh MythicLib.
4. **Bentrok antar pack.** Cek `font/default.json`, `model_id` yang sama pada material yang sama, namespace/nama sound yang sama. Kalau bentrok, laporkan ke user, jangan timpa diam-diam.
5. **Daftarkan di definisi skill `sttcustomskill` (kode/JAR)** hanya skill yang benar-benar ada di pack. Jika `phrase` belum ditentukan user, **tanya**.

### 4.2 Cara Cast Skill MythicMobs

**Keputusan user (berlaku untuk SEMUA pack skill custom, bukan hanya meteor): damage dan duration di-hardcode langsung di file skill MythicMobs pack.** Tidak lewat modifier MythicLib dan tidak lewat config STT. (Alasan: di pack meteor, `MythicLib/skill/meteor.yml` hanya berisi daftar nama modifier tanpa nilai default, jadi tanpa nilai damage dan burn jadi 0.)

1. **User yang mengganti sendiri** `<modifier.damage>` dan `<modifier.duration>` di file skill pack dengan angka tetap (contoh pack meteor: `damage{a=...}` dan `ignite{ticks=...}` di `Meteor_HIT_D` dan `Meteor_BURN`). Builder **TIDAK mengedit file pack dan TIDAK mengarang angka**. Jika masih ada `<modifier.*>` di file pack yang terpasang, STT tetap jalan tapi damage/burn kemungkinan 0, jadi ingatkan user saat test.
2. Definisi skill STT **tidak punya `modifiers`**. File `MythicLib/skill/*.yml` pack tidak dipakai STT (boleh tetap terpasang).
3. **Server tetap wajib memasang MythicLib saja** (tanpa MMOItems dan MMOCore), karena skill pack memakai kondisi `mmoCanTarget`. Tetap cek per pack apakah skill memakai `mmo*`.
4. **Cast lewat Java API MythicMobs memakai nama skill MythicMobs** (`mythic-skill-name`, contoh `Meteor`) dengan caster = player, BUKAN `dispatchCommand("mm ...")`. Baca API MythicMobs versi yang terpasang (javadoc atau jar), jangan menebak nama method.
5. **Fallback aman:** jika MythicMobs atau MythicLib tidak aktif saat startup, skill bertipe `MYTHIC_MOBS` dinonaktifkan dengan log warning jelas. Skill `NATIVE` tetap jalan, plugin tidak crash.
6. **Cek dulu, baru potong mana.** Sebelum memotong mana dan memulai cooldown, pastikan skill benar-benar bisa dicast (MythicMobs aktif dan nama skill ditemukan). Jika gagal: pesan singkat ke player, log warning, **mana dan cooldown tidak berubah**.
7. **Test wajib sebelum rilis:** cast satu skill pack lewat voice dan pastikan (a) damage dan burn benar-benar masuk (bukan 0), (b) `mmoCanTarget` bekerja lewat jalur cast ini (target terlarang atau zona no-PvP tidak kena), (c) caster tidak ikut kena skill area.

### 4.3 Catatan Pack `meteor_only` (hasil pengecekan)

* **Isi:** ItemsAdder (namespace `mage`), MythicLib 1 file, MythicMobs Items/Mobs/Skills. **Tidak ada ModelEngine.** Semua ID lintas file sudah cocok (skill, mob, item, model ID 2258-2266 dan 2274, sound, namespace).
* **Dua varian skill identik 100%** (`meteor_skills_mmocore.yml` dan `meteor_skills_mmoitems.yml`). Pasang **satu saja** (`mmocore`), karena dua-duanya berisi ID `Meteor` yang sama. Catatan: untuk pack ini varian MMOITEMS TIDAK berbeda dari MMOCORE, jangan berasumsi sama untuk pack lain.
* **Sisa aura:** baris pertama skill `Meteor` memakai `Aura{auraName=Arcane_Missile_Aura;duration=99999;maxStacks=5}` tanpa definisi aura di pack. Kemungkinan sisa dari pack asli. **Disarankan dibuang oleh user** saat mengedit file pack.
* **Cek bentrok:** model ID PAPER 2258-2274 (dan COAL 2069 untuk `icon_meteor`) tidak boleh bentrok dengan pack lain di server.
* ID MythicMobs `Meteor` tidak bentrok dengan skill NATIVE STT `meteor` karena beda sistem, tapi ID di definisi STT harus beda (`meteor` vs `meteor_awakened`).

---

## 5. Sistem Mana (di `sttengine`)

* **Max mana flat 300 untuk semua player dari awal** (`settings.max-mana`). Tidak ada level, quest, atau kenaikan bertahap.
* **Regen mana dan burnout DIPERTAHANKAN seperti sistem lama (v2.3.6)** untuk sementara (`mana-regen-delay`, `burnout-delay`). Builder membaca perilakunya dari source lama. User mungkin mengubahnya nanti.
* Semua konfigurasi mana ada di `sttengine`. `sttskill` dan `sttcustomskill` hanya menyimpan `cost` per skill, mana tetap diambil dari engine.
* Skill custom/admin tidak mengubah max mana.
* Hapus kode flat 250 dan kalkulasi mana berbasis jumlah skill.
* Saat plugin start, beri **warning di console** jika ada skill dengan `cost` lebih besar dari `max-mana`.
* HUD mana dirender memakai aset dari `sttresourcepack`.

### Data Player (`plugins/sttengine/players/<UUID>.yml`)

```yaml
uuid: "12345678-abcd-1234-abcd-123456789abc"
name: "Player1"
race: "witch"        # PERMANEN. Hanya boleh diset jika masih null. Direset admin lewat /stt resetrace
current-mana: 125.0
unlocked-skills:     # skill ras (otomatis saat pilih ras) + skill custom dari /stt addskill
  - heal
  - step_swap
  - black_fire
  - meteor_awakened  # contoh skill custom hasil upgrade, diberikan admin
removed-skills:      # skill ras yang dicabut admin (jangan di-grant ulang saat join)
  - meteor
```

Penyimpanan harus aman dari crash: tulis ke `<UUID>.yml.tmp` lalu rename menimpa file asli (atomik). Simpan langsung (async) setiap kali `race` atau `unlocked-skills` berubah, jangan hanya menunggu autosave.

---

## 6. Aturan Ras & Unlock Skill

* **Semua ID skill memakai underscore** (`step_swap`, `meteor_awakened`), bukan strip.
* Player baru **belum punya ras dan belum punya skill** (disebut "human"). Voice belum memproses apa-apa.
* **Build awal hanya ada ras Witch** dengan skill: `heal`, `step_swap`, `black_fire`, `meteor`. Semuanya skill **NATIVE bawaan STT** (kode ada di source lama, dipindahkan ke `sttskill`). Selain skill Witch ada banyak skill lain di source lama yang belum 100% selesai: jangan dimasukkan ke ras apa pun sampai user minta.
* Pilih ras lewat `/stt menu`. **Wajib ada langkah konfirmasi** ("Yakin pilih Witch? Ini permanen") sebelum tersimpan. Setelah dipilih, ras terkunci. Tidak ada command ganti ras untuk player.
* Sebelum menyimpan: cek `race == null`. Jika sudah terisi, tolak.
* **Saat ras dipilih, berurutan:**
  1. Set `race`.
  2. Tambahkan semua skill di list ras ke `unlocked-skills`.
  3. Simpan file player (atomik).
  4. Panggil `VoiceManager.refreshGrammar(player)`.
  5. Kirim pesan ke player.
* **Sinkron saat join:** pastikan `unlocked-skills` memuat semua skill list ras player, **kecuali** yang ada di `removed-skills`. Skill baru yang ditambahkan ke list ras otomatis diterima player lama. Lalu `refreshGrammar`. ID di `unlocked-skills` yang tidak ada di registry diabaikan, jangan error dan jangan dihapus.
* **Skill dicabut dari list ras di kode (update JAR):** player lama **tetap memilikinya**. Admin mencabut manual lewat `/stt removeskill`.
* **Skill custom TIDAK masuk list ras mana pun.** Hanya diberikan admin per player lewat `/stt addskill` (hasil roleplay).
* **Contoh upgrade:** Player1 (Witch) upgrade meteor lewat roleplay. Admin: `/stt removeskill Player1 meteor` lalu `/stt addskill Player1 meteor_awakened`. Player2 (Witch juga) tidak berubah, tetap `meteor` default.

---

## 7. Definisi Ras & Skill (Kode/JAR) dan Config Tuning

**Keputusan user:**

* **Keberadaan** ras dan skill (ID, nama, phrase, type, `mythic-skill-name`, isi list ras) didefinisikan di **kode/JAR**. Update JAR otomatis membawa ras atau skill baru, user tidak perlu mengedit config untuk menambahnya.
* **Angka tuning** diedit user di `config.yml` plugin skill masing-masing: `sttskill/config.yml` (cost mana, cooldown, damage, duration, dll skill native) dan `sttcustomskill/config.yml` (**hanya** cost mana dan cooldown skill custom, tanpa damage/duration).
* Nilai default semua angka ada di kode. Config berisi nilai yang bisa diedit. **Skill atau key baru dari update JAR harus ditambahkan otomatis ke config tanpa menimpa nilai yang sudah diedit user.** Skill yang belum ada di config memakai default dari kode. (Tanpa aturan ini, config lama di server akan menyembunyikan skill baru.)

### 7.1 `sttengine/config.yml` (setting umum)

```yaml
settings:
  max-mana: 300
  mana-regen-delay: 3     # dipertahankan dari sistem lama
  burnout-delay: 8        # dipertahankan dari sistem lama
  itemsadder-integration: true
```

### 7.2 Definisi ras (kode `sttengine`)

| Ras | Nama tampil | Skill (otomatis terbuka saat dipilih) |
|---|---|---|
| `witch` | `&dRace Witch` | `heal`, `step_swap`, `black_fire`, `meteor` |

Ras baru ditambahkan builder lewat kode. Ras yang terdaftar otomatis muncul di `/stt menu`.

### 7.3 Skill NATIVE (`sttskill`)

**Di kode:** `id`, `name`, `phrase`, `type: NATIVE`. Jika `phrase` belum ada di source lama, **tanya user**.

**Di `sttskill/config.yml`:** angka tuning per skill.

```yaml
# Nilai contoh. Ambil nilai asli dari source lama.
# Skill baru dari update JAR ditambahkan otomatis, nilai yang sudah diedit tidak ditimpa.
skills:
  heal:
    cost: 20
    cooldown: 10
    # nilai khusus skill (damage, duration, dll) ikut yang ada di source lama
  # step_swap, black_fire, meteor: format sama
```

### 7.4 Skill custom (`sttcustomskill`)

**Di kode:** `id`, `name`, `phrase`, `type: MYTHIC_MOBS`, `mythic-skill-name`. **Tidak ada `modifiers`, damage, atau duration** (semua ada di file skill pack, Bagian 4.2).

| Field kode | Contoh (meteor) |
|---|---|
| `id` | `meteor_awakened` |
| `name` | Awakened Meteor |
| `phrase` | `meteor awakened` (contoh, **tanya user**) |
| `type` | `MYTHIC_MOBS` |
| `mythic-skill-name` | `Meteor` (nama skill di `MythicMobs/Skills/*.yml`, huruf besar-kecil harus sama) |

**Di `sttcustomskill/config.yml`:** hanya cost mana dan cooldown. Mana (max, regen) tetap dari `sttengine`.

```yaml
skills:
  meteor_awakened:
    cost: 150        # contoh, bukan nilai final
    cooldown: 30
```

### 7.5 Alur Menambah Ras atau Skill

1. User mengirim source `sttengine` + `sttskill` / `sttcustomskill` ke builder.
2. Builder menambah ras atau skill di kode (beserta default angkanya), lalu build JAR baru.
3. User mengganti JAR di server. Entri config untuk skill baru muncul otomatis. User hanya mengedit angka kalau mau mengubah dari default.
4. Player lama yang rasnya mendapat skill baru menerimanya otomatis saat join (Bagian 6).

---

## 8. Voice: Simple Voice Chat + Vosk per Player

### A. Sumber Audio

* Audio mic player lewat **Simple Voice Chat** (plugin server + client mod). Builder wajib membaca API SVC versi terpasang, jangan menebak nama class/method.
* Audio dari SVC berupa Opus 48 kHz. Vosk di sini memakai 16 kHz. **Wajib decode Opus ke PCM lalu downsample ke 16 kHz** sebelum masuk Vosk.
* Jika SVC tidak ada, fitur voice nonaktif dengan log warning, plugin tidak crash.

### B. Aturan Vosk Per Player

* `Model` Vosk di-load **1x** dan dipakai bersama semua player.
* Player yang punya minimal 1 skill punya 1 `Recognizer` sendiri. Grammar **hanya berisi phrase skill milik player** ditambah `"[unk]"`.
* Player human (belum pilih ras, belum punya skill): **tidak ada Recognizer**, audio tidak diproses.
* `refreshGrammar(player)` dipanggil saat pilih ras, `/stt addskill`, `/stt removeskill`, `/stt resetrace`, join, dan saat plugin skill mendaftarkan skill baru. Recognizer baru dibuat async lalu ditukar, hanya player itu yang terpengaruh.
* Saat player quit, recognizer di-close. Saat plugin disable, semua recognizer, executor, dan model ditutup.
* Grammar dinamis hanya jalan di model Vosk kecil (`vosk-model-small-*`). Model besar dengan rescoring tidak mendukung grammar.
* **Tiap phrase harus ada di kosakata model Vosk yang dipakai.** Kata di luar kosakata tidak akan dikenali. Cek saat skill didaftarkan dan beri warning.
* Setelah Vosk mengenali phrase, STT **tetap cek** skill itu ada di `unlocked-skills` player sebelum dieksekusi.

### C. Kerangka `VoiceManager.java`

Recognizer Vosk adalah native object. Menutup recognizer saat thread audio masih memakainya bisa crash JVM, jadi swap dilindungi lock per player. Kerangka ini sudah memperbaiki: hasil Vosk di-parse dari JSON, task refresh yang antre tidak membuat recognizer baru setelah player quit, dan ada `shutdown()`.

```java
public class VoiceManager {

    private static final float SAMPLE_RATE = 16000f;

    private final Stt plugin;
    private final SkillService skillService;
    private final Model model; // shared, load sekali
    private final Map<UUID, PlayerRecognizer> recognizers = new ConcurrentHashMap<>();
    private final Set<UUID> active = ConcurrentHashMap.newKeySet();
    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    public VoiceManager(Stt plugin, SkillService skillService, Model model) {
        this.plugin = plugin;
        this.skillService = skillService;
        this.model = model;
    }

    private static class PlayerRecognizer {
        final ReentrantLock lock = new ReentrantLock();
        Recognizer recognizer;
    }

    /** Panggil saat join, pilih ras, addskill/removeskill/resetrace, atau skill baru terdaftar. */
    public void refreshGrammar(Player player) {
        UUID id = player.getUniqueId();
        List<String> phrases = skillService.getUnlockedPhrases(id); // phrase skill milik player saja

        if (phrases.isEmpty()) { remove(id); return; }

        List<String> words = new ArrayList<>(phrases);
        words.add("[unk]");
        String grammar = new Gson().toJson(words);

        active.add(id);
        pool.submit(() -> {
            if (!active.contains(id)) return; // player sudah quit
            try {
                Recognizer fresh = new Recognizer(model, SAMPLE_RATE, grammar);
                PlayerRecognizer pr = recognizers.computeIfAbsent(id, k -> new PlayerRecognizer());
                Recognizer old;
                pr.lock.lock();
                try {
                    if (!active.contains(id)) {      // quit saat recognizer dibuat
                        fresh.close();
                        recognizers.remove(id, pr);
                        return;
                    }
                    old = pr.recognizer;
                    pr.recognizer = fresh;
                } finally { pr.lock.unlock(); }
                if (old != null) old.close();
            } catch (IOException e) {
                plugin.getLogger().warning("Gagal build grammar untuk " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    /** Dipanggil thread audio. Return teks hasil (sudah di-parse), atau null. */
    public String process(UUID id, byte[] pcm) {
        PlayerRecognizer pr = recognizers.get(id);
        if (pr == null) return null;
        pr.lock.lock();
        try {
            if (pr.recognizer == null) return null;
            if (!pr.recognizer.acceptWaveForm(pcm, pcm.length)) return null;
            String json = pr.recognizer.getResult();   // contoh: {"text":"heal"}
            String text = JsonParser.parseString(json).getAsJsonObject().get("text").getAsString().trim();
            return text.isEmpty() || text.equals("[unk]") ? null : text;
        } finally { pr.lock.unlock(); }
    }

    public void remove(UUID id) {
        active.remove(id);
        PlayerRecognizer pr = recognizers.remove(id);
        if (pr == null) return;
        pr.lock.lock();
        try { if (pr.recognizer != null) pr.recognizer.close(); pr.recognizer = null; }
        finally { pr.lock.unlock(); }
    }

    /** Panggil di onDisable. */
    public void shutdown() {
        pool.shutdown();
        try { pool.awaitTermination(2, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        for (UUID id : new ArrayList<>(recognizers.keySet())) remove(id);
        active.clear();
        model.close();
    }
}
```

---

## 9. Command (`sttengine`)

### A. Player

| Command | Fungsi |
|---|---|
| `/stt menu` | Pilih ras (dengan konfirmasi, hanya jika `race == null`) dan lihat daftar skill milik sendiri |

### B. Admin (permission `stt.admin`)

| Command | Fungsi |
|---|---|
| `/stt addskill <player> <skill_id>` | Tambah skill ke `unlocked-skills`. Jika ada di `removed-skills`, hapus dari sana. Simpan, lalu `refreshGrammar` |
| `/stt removeskill <player> <skill_id>` | Hapus skill dari `unlocked-skills`. **Jika skill itu bagian dari list ras player, catat di `removed-skills`** supaya sinkron join tidak memberikannya lagi. Simpan, lalu `refreshGrammar` |
| `/stt resetrace <player>` | Untuk kasus **character kill**. Kosongkan `race`, **hapus semua skill termasuk skill custom**, kosongkan `removed-skills`. Player jadi "human" lagi dan bisa memilih ras ulang lewat `/stt menu`. Kosongkan grammar voice |

* Ketiganya harus bisa dipakai pada **player offline** (ubah file player). Jika online, berlaku langsung dan grammar dibangun ulang saat itu. Jika offline, grammar dibangun saat join.
* Tolak dengan pesan jelas jika `skill_id` tidak ada di registry, player belum pernah join, skill sudah dimiliki (add) atau tidak dimiliki (remove).
* Tab-complete: `addskill` menampilkan semua skill terdaftar, `removeskill` hanya skill milik player itu.
* Jika phrase skill yang di-`addskill` sama persis atau menjadi awalan dari phrase skill lain milik player yang sama, tampilkan **warning ke admin** (skill tetap ditambahkan).

---

## 10. Roadmap Pengerjaan

1. **Phase 1: Pecah proyek & hapus sistem lama**
   * Pecah jadi `sttengine`, `sttskill`, `sttcustomskill`. Buat API registrasi skill di engine.
   * Hapus `Pack.java`, relic item, `/stt give relic`, flat 250 mana, `AssetsDeployer`.
2. **Phase 2: Data player, ras Witch, mana**
   * `PlayerData` dengan penyimpanan atomik, max mana 300, regen/burnout dari sistem lama, pilih ras permanen dan unlock skill ras.
3. **Phase 3: GUI `/stt menu`**
   * Pemilihan ras dengan konfirmasi dan daftar skill milik player.
4. **Phase 4: Voice**
   * Integrasi Simple Voice Chat (decode Opus, downsample 16 kHz) dan `VoiceManager` per player sesuai Bagian 8.
5. **Phase 5: Skill Witch & command admin**
   * Pindahkan `heal`, `step_swap`, `black_fire`, `meteor` ke `sttskill`.
   * `/stt addskill`, `/stt removeskill`, `/stt resetrace`.
6. **Phase 6: `sttcustomskill` & cast MythicMobs via MythicLib**
   * Definisi skill custom, cast sesuai Bagian 4.2, lalu test damage.
7. **Phase 7: `sttresourcepack`**
   * HUD mana dan file voice. Pasang manual atau lewat ItemsAdder.

---

## 11. Asumsi Builder yang Perlu Dikonfirmasi User

Hal-hal di bawah dipilih builder/penulis karena belum dinyatakan jelas. Jika salah, user koreksi:

1. `cooldown` per skill ikut diedit di `config.yml` bersama `cost` (bukan hanya `cost`). `phrase` tetap di kode, bukan config.
2. Definisi ras ada di kode `sttengine` (bukan config), sesuai keputusan Bagian 7.
3. `/stt resetrace` menghapus **semua** skill termasuk skill custom dan `removed-skills`.
4. Phrase mantra skill Witch diambil dari source lama. Jika tidak ada, tanya user.
5. Nilai damage dan duration skill custom diisi user langsung di file MythicMobs pack (lihat 4.2 butir 1). Builder tanya angkanya.
6. Nama plugin Simple Voice Chat di `plugin.yml` (umumnya `voicechat`) dicek dari JAR yang terpasang.
7. Cast skill custom lewat API MythicMobs (nama skill), bukan API MythicLib, karena tidak ada modifier yang perlu dikirim. Perlu dites bahwa `mmoCanTarget` tetap bekerja.



KIRIM FILE SORUCE CODE DALAM 4 ISI DARI MASING" PACK TETEP FLAT, DAN SETIAP SYSTEM DI CATEY DI LOGUPDATE.MD MASING" FILE DAN SEMUA FILE PACK ADA FILE BIGUPDATE.MD

1STT ENGINE
2STT SKILL
3STT SKILLCUSTUM
4REUARSPACK HUD






##SELALU CATET SETIAP PERUBAHAN SEKECIL APAUN TTP CATET DAN MASUKIN BIGUPDATE INI KE SOURCE CODE#
