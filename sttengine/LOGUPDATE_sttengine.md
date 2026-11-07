# LOGUPDATE - sttengine (v3.0.0)

Spesifikasi: BIGUPDATE.md (v3.0.0 revisi). Downgrade target: Paper 1.20.1, Java 21.

## Dihapus dari v2.3.6
- `Relic.java`, `/stt give relic`, `/stt give allrelic`, config `relic.required`.
- `Pack.java` dan semua pemanggilnya, `AssetsDeployer` (tidak ada auto-deploy / kirim resource pack dari JAR).
- Flat 250 mana (`Mana.MAX`). Max mana sekarang `settings.max-mana` (300).
- `Mana.textMode`, `setTextMode`, `renderText`: dulu hanya dipanggil `Pack.java`, jadi ikut dihapus. HUD selalu pakai font `stt:mana` dari sttresourcepack.
- `Skills.java`, `SttGui.java` (Spellbook lama), semua skill selain Witch tidak ada di engine (skill ada di plugin skill).
- File `mana.yml` lama tidak dipakai lagi. Mana ada di `players/<UUID>.yml`. Mana lama TIDAK dimigrasi.

## Struktur baru (package `id.stt.engine.*`)
- `SttEngine`: main plugin, `SttEngine.get().getRegistry()` dipakai plugin skill buat daftar skill.
- `skill/`: `SkillDef`, `SkillType` (NATIVE, MYTHIC_MOBS), `NativeSkill`, `SkillRegistry`, `SkillService` (satu-satunya jalur cast).
- `race/`: `RaceDef`, `Races` (ras di KODE, build awal cuma `witch`), `RaceService`.
- `data/`: `PlayerData`, `PlayerStore`.
- `mana/Mana`, `mythic/MythicHook`, `voice/` (`VoiceManager`, `SttVoice`, `VoiceLink`, `VocabCheck`), `gui/SttMenu`, `command/SttCommand`, `util/ConfigDefaults`.

## Perilaku
- Data player `plugins/sttengine/players/<UUID>.yml`: uuid, name, race, current-mana, unlocked-skills, removed-skills. Simpan atomik (`.yml.tmp` lalu rename), async lewat satu thread I/O, langsung disimpan tiap race / skill berubah. Autosave tiap 5 menit (mana), simpan saat quit dan disable.
- Ras Witch (`&dRace Witch`): `heal`, `step_swap`, `black_fire`, `meteor`. Pilih lewat `/stt menu` + konfirmasi, permanen (`race == null` dicek sebelum simpan). Urutan: set race, unlock skill ras, simpan, `refreshGrammar`, pesan.
- Sinkron saat join: skill ras (kecuali `removed-skills`) otomatis masuk `unlocked-skills`. ID tak dikenal di file diabaikan, tidak dihapus.
- Cast: cek unlock, cek bisa dicast (MythicMobs aktif dan skill ditemukan / `canCast` native), cooldown, mana cukup, eksekusi, BARU potong mana dan mulai cooldown. Kalau gagal, mana dan cooldown tidak berubah.
- Cast MYTHIC_MOBS lewat Java API MythicMobs (`BukkitAPIHelper.castSkill(Entity, String)`), caster = player, bukan `dispatchCommand`. Dipanggil lewat reflection (JAR MythicMobs Premium tidak ada di Maven publik). Nama class/method dicek dari JAR MythicMobs 5.8.2 yang dikirim user.
- MythicMobs atau MythicLib tidak aktif: skill MYTHIC_MOBS nonaktif + warning, NATIVE tetap jalan.
- Regen mana dan burnout dipertahankan dari v2.3.6 (3 mana per 5 detik, `mana-regen-delay`, `burnout-delay`). Ambang debuff low=20 dan critical=10 sekarang konstanta di kode (dulu config `mana.low` / `mana.critical`, default sama).
- Key config pindah ke `settings.*`: `max-mana`, `mana-regen-delay`, `burnout-delay`, `itemsadder-integration`.
- Warning console: skill dengan cost > max-mana (saat skill terdaftar dan saat `/stt reload`), phrase dengan kata di luar kosakata Vosk (baca `model/graph/words.txt`).
- ItemsAdder tidak aktif: warning, engine tetap jalan.

## Voice
- Model Vosk dimuat 1x. Tiap player punya 1 `Recognizer` dengan grammar = phrase skill miliknya + `[unk]`. Human (tanpa skill) tidak punya recognizer, audio tidak diproses.
- `refreshGrammar` dipanggil saat join, pilih ras, addskill, removeskill, resetrace, dan saat plugin skill mendaftar / dimatikan.
- Opus 48 kHz didecode lalu downsample 16 kHz (rata-rata 3 sampel, sama seperti v2.3.6).
- Setelah Vosk mengenali teks, skill dicari di skill milik player, phrase terpanjang menang, cocok per kata. Tetap dicek `unlocked-skills` sebelum eksekusi.
- Penambahan di luar kerangka BIGUPDATE: `VoiceManager.has()` dan `finish()` (akhir ucapan, ganti `getFinalResult` lama), serta `VoiceLink` (isolasi kelas Simple Voice Chat supaya engine tetap bisa load tanpa SVC).
- Kerangka `VoiceManager` dari BIGUPDATE dipakai apa adanya, hanya nama player diambil sebelum task async dan `Logger` dipakai sebagai ganti `Stt`.

## Command
- Player: `/stt menu` (juga `/stt` polos).
- Admin `stt.admin`: `/stt addskill <player> <skill_id>`, `/stt removeskill <player> <skill_id>`, `/stt resetrace <player>`. Bisa untuk player offline (ubah file). Tab-complete sesuai spesifikasi. Warning phrase bentrok (sama persis / awalan) ke admin saat addskill.
- Dipertahankan dari v2.3.6 (tidak diminta dihapus): `/stt reload` (sekarang juga reload config plugin skill), `/stt mana` (isi penuh mana sendiri).

## Port 1.21.11 -> 1.20.1 (engine)
- `PotionEffectType.SLOWNESS` -> `SLOW`, `MINING_FATIGUE` -> `SLOW_DIGGING`.
- `plugin.yml` `api-version: '1.20'`. `pom.xml`: paper-api `1.20.1-R0.1-SNAPSHOT`, `maven.compiler.release` 21.
- Menu pakai `InventoryHolder` (bukan banding judul inventory).

## PERLU DIKONFIRMASI / DITES
- Folder data berubah dari `plugins/stt` ke `plugins/sttengine`: pindahkan folder `model` (Vosk `vosk-model-small-en-us-0.15`) ke `plugins/sttengine/model`.
- Nama plugin SVC `voicechat` diambil dari `depend: [voicechat]` di plugin lama yang jalan di server. Belum dicek dari JAR SVC (tidak ada di sandbox).
- `voicechat-api` 2.6.0 dipakai karena sama dengan versi lama. Cocokkan dengan versi SVC di server 1.20.1.
- BELUM DIKOMPILASI: sandbox tidak punya Paper API 1.20.1, SVC API, Vosk, dan akses internet. Hanya dicek sintaks. Build lewat GitHub Actions (`.github/workflows/build.yml`) atau Maven lokal.

## Fix build #1 (log GitHub Actions)
- `VoiceManager.refreshGrammar`: `catch (IOException)` diganti `catch (Exception)` karena konstruktor `Recognizer` Vosk 0.3.45 tidak melempar `IOException` (error compile `exception java.io.IOException is never thrown`). Kerangka di BIGUPDATE bagian 8C punya kesalahan yang sama. Import `IOException` yang jadi tidak terpakai dihapus.

## Revisi HUD: bar mana baru + kartu skill
- Baru `hud/Hud.java`, `hud/HudLayout.java`, `hud/HudMetrics.java` (HudMetrics dihasilkan `tools/hudgen/build.py`, jangan edit tangan).
- `Mana`: gambar HUD (font `stt:mana`, 50 level, digit) dihapus. `Mana.show()` sekarang memanggil `display` yang diisi engine (`mana.setDisplay(hud::show)`). Regen, burnout, debuff tidak diubah. `tick()` tidak lagi mengirim actionbar sendiri, `Hud` yang mengirim.
- `SkillService.cooldownLeft(UUID, SkillDef)` ditambah: sisa cooldown detik. Tidak ada perubahan perilaku cast.
- `SttEngine`: buat `Hud`, `hud.start()` / `stop()` / `forget(uuid)` saat quit.
- Isi HUD dibaca live dari `SkillService.unlockedDefs(data)`: tiap skill yang dimiliki player jadi satu kartu, urut sesuai `unlocked-skills`. Skill ras, skill dari `/stt addskill`, dan skill yang dihapus (`removeskill` / `resetrace`) langsung ikut berubah paling lama 2 tick. Tidak ada daftar skill di kode HUD.
- Kartu: nama skill, timer sisa cooldown (hanya saat cooldown, merah, per 0,1 detik), mana cost (ikon permata), lama cooldown (ikon jam). Hijau = siap, merah = cooldown. Angka mana cost jadi merah muda kalau mana player tidak cukup. Nama dibersihkan dari kode warna `&x` / `§x`, karakter non-ASCII jadi `?`, nama terlalu panjang dipotong `..`.
- Batas 8 kartu (`HudLayout.MAX_CARDS`), supaya muat di layar GUI scale 4. Skill ke-9 dan seterusnya tetap bisa dipakai (suara), tapi tidak digambar. Kalau mau lebih, perlu baris kedua (butuh font dan tekstur tambahan).
- Actionbar dikirim tiap 2 tick hanya kalau tampilan berubah, dan diulang tiap 1,5 detik supaya tidak hilang.
- Bar mana menampilkan `mana / max-mana` (max dari `settings.max-mana`), isi bar 69 segmen.
- BELUM DIKOMPILASI terhadap Paper API (sandbox tidak punya). `HudLayout` (tanpa Bukkit) sudah dikompilasi dan dites sendiri: 0, 1, 2, 9, 20 skill, nama aneh, mana 0 / 0,4 / 150 / 300. `Hud`, `Mana`, `SkillService`, `SttEngine` hanya dicek sintaks.

