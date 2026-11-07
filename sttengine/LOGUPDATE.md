# LOGUPDATE - sttengine (v3.0.3)

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

## v3.0.3
- `HudLayout`: gambar bar (latar dan frame) sekarang 3 glyph berurutan (`putBar`), bukan 1 glyph 180 px. Sebabnya batas glyph 256 px client 1.20.1 (lihat LOGUPDATE sttresourcepack). `HudMetrics`: `BAR_ADV`, `BG_ADV` diganti `PIECE_W`, `PIECE_ADV`. Pack HUD harus versi 3.0.3.

## v3.0.4 - fix HUD bar bolong
- Gejala (screenshot): frame bar cuma ada potongan kiri, latar bar muncul terpisah di kanan, angka `19 / 300` geser ke kiri bar. Penyebab: glyph potongan ke-2 dan ke-3 (``-``) tidak ke-render di client dan lebarnya 0, jadi semua yang digambar sesudahnya ikut geser sekitar 122 gui px (2 x 61).
- `HudLayout`: bar (latar ``, frame ``) kembali 1 glyph per lapisan, tapi ukuran tekstur dikecilkan jadi 256 px lebar (batas client). `putBar` sekarang 1 glyph. `HudMetrics`: `PIECE_W` / `PIECE_ADV` diganti `BAR_ADV` (181).
- Pack HUD harus versi 3.0.4 (lihat LOGUPDATE sttresourcepack). Hapus pack lama di client.
- `/stt addskill <player> <id>` sudah bisa untuk SEMUA skill yang terdaftar di registry (tidak harus ada di list ras). Tab-complete juga menampilkan semua id terdaftar. Tidak ada perubahan kode di sini.
- BELUM DITES di client / server asli.
