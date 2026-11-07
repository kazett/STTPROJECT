# LOGUPDATE - sttskill (v3.0.3)

Spesifikasi: BIGUPDATE.md. Butuh `sttengine` (`depend`).

## Isi
Skill NATIVE ras Witch, dipindah dari source v2.3.7: `heal`, `step_swap`, `black_fire`, `meteor`. Skill lain di source lama (thunder, sonic dash, fireball, shock bind, shinra tense, gravity pull, summon x4) TIDAK dimasukkan sesuai spesifikasi.

| ID | Nama | Phrase (dari source lama) | cost | cooldown |
|---|---|---|---|---|
| `heal` | Heal | `heal` | 20 | 8 |
| `step_swap` | Step Swap | `swap step` | 25 | 8 |
| `black_fire` | Black Fire | `black fire` | 45 | 10 |
| `meteor` | Meteor | `meteor crash` | 40 | 8 |

Angka tuning lain (amount, range, duration, damage-per-sec, hit-mobs, height, up-speed, down-speed, radius, damage, power, depth) sama dengan default config v2.3.7.

## Perubahan dari source lama
- ID `swap-step` -> `step_swap`, `black-fire` -> `black_fire` (semua ID pakai underscore). Phrase tetap dari source lama.
- Config pindah ke `skills.<id>.<key>` di `plugins/sttskill/config.yml`. Default ada di kode. Key / skill baru dari update JAR ditambahkan otomatis (`ConfigDefaults`), nilai yang sudah diedit tidak ditimpa. Cost dan cooldown dibaca live oleh engine, `/stt reload` cukup.
- Interface `Skill` (lokal): `defaultCost()` diganti `defaults()` (map semua angka), ditambah `name()`. `SkillContext` baca `skills.<id>.<key>`.
- `SwapStep`: pengecekan `ShinraTense.channeling` dihapus (Shinra Tense tidak ada di build ini).
- `BlackFire`, `Blast`: pengecekan `Summons.ownedBy` dihapus (summon tidak ada di build ini).
- `Blast.destroy` dihapus (hanya dipakai Shinra Tense). `Blast.explode` dan `Blast.dig` dipertahankan.
- Registrasi ke engine lewat `SkillDef.nativeSkill` di `onEnable`, dicabut di `onDisable`.

## Port 1.21.11 -> 1.20.1
- `Particle.EXPLOSION_EMITTER` -> `EXPLOSION_HUGE` (Blast).
- `Particle.SMOKE` -> `SMOKE_NORMAL` (BlackFire).
- `plugin.yml` `api-version: '1.20'`. Nama partikel / sound / efek lain dicek tetap valid di 1.20.1.

## Catatan
- `meteor` memakai `createExplosion(..., breakBlocks=true)` seperti v2.3.7: meteor menghancurkan block (lubang sedalam `depth`).
- BELUM DIKOMPILASI (lihat LOGUPDATE sttengine).

## v3.0.2
- `meteor` (Meteor Crash): default `power` 5.0 -> 3.0 supaya ledakan lebih kecil. `depth` (10), `radius` (6), `damage` (16) tidak diubah.
- Default di kode hanya berlaku untuk key yang belum ada. Kalau `plugins/sttskill/config.yml` sudah punya `skills.meteor.power`, edit manual angkanya (atau hapus barisnya lalu `/stt reload` / restart).
- BELUM DITES di server.

## v3.0.4 - Meteor Crash
- Tinggi naik: `height` default 20 -> 4 block, `up-speed` 1.0 -> 0.7.
- Tidak menggali dalam lagi: `depth` dihapus. Sekarang `Blast.crater`: 1 ledakan di tengah (setinggi pinggang, `power`) + cincin ledakan kecil (`power` x 0.7) sejauh `spread` block (default 4, key baru `skills.meteor.spread`) di sekelilingnya. Hasilnya kawah lebar dan dangkal.
- `Blast.dig` diganti `Blast.crater`.
- Migrasi satu kali (`config-version: 2` di `plugins/sttskill/config.yml`): `skills.meteor.height` dan `up-speed` ditimpa ke nilai baru, key `depth` dihapus. Setelah itu angka bebas kamu edit lagi.
- Ingin kawah lebih lebar: naikkan `spread` atau `power`. Ingin lebih dangkal: turunkan `power`.
- BELUM DITES di server.

## v3.0.4 - skill lama dikembalikan (dari source v2.3.7)
Terdaftar di registry tapi TIDAK masuk list ras mana pun (Witch tetap 4 skill). Diberi admin lewat `/stt addskill <player> <id>`.

| ID | Phrase (sama dgn lama) | cost | cooldown |
|---|---|---|---|
| `thunder` | `thunder` | 10 | 8 |
| `sonic_dash` | `sonic dash` | 12 | 8 |
| `fireball` | `fire ball` | 20 | 8 |
| `shock_bind` | `shock bind` | 15 | 8 |
| `gravity_pull` | `gravity pull` | 30 | 8 |
| `shinra_tense` | `sheen ra ten say` | 250 | 60 |
| `summon_zombie` | `summon zombie` | 30 | 8 |
| `summon_skeleton` | `summon skeleton` | 30 | 8 |
| `summon_golem` | `summon golem` | 50 | 8 |
| `summon_warden` | `summon warden` | 100 | 8 |

- Cooldown lama = 8 detik untuk semua skill (default global). Pengecualian `shinra_tense` = 60 (sengaja, supaya tidak bisa dicast lagi di tengah timeline 30 detik). Ubah di `plugins/sttskill/config.yml`.
- ID lama pakai strip -> underscore (`sonic-dash` -> `sonic_dash`, dst). Config pindah ke `skills.<id>.<key>`; semua angka lama (radius, damage, power, charge, dll) dipertahankan sebagai default.
- `Summons` (mesin summon) ikut dipindah, start di `onEnable`, stop di `onDisable`. `Blast.destroy` (Shinra) dan cek `Summons.ownedBy` di `Blast`/`BlackFire` dikembalikan. `SwapStep` dibatalkan lagi kalau player/target lagi channel Shinra Tense.
- Port 1.20.1: `SLOWNESS`->`SLOW`, `JUMP_BOOST`->`JUMP`, `Particle.SMOKE`->`SMOKE_NORMAL`, `EXPLOSION_EMITTER`->`EXPLOSION_HUGE`, `Enchantment.POWER`->`ARROW_DAMAGE`, `Sound.ENTITY_WIND_CHARGE_WIND_BURST` (tidak ada di 1.20.1) -> `ENTITY_PHANTOM_SWOOP`.
- Skill `teleport` yang disebut di project.md v2.3.7 TIDAK ada source-nya di zip, jadi tidak ikut.
- Sound `stt:shinra_tensei` sudah ada di resource pack.
- BELUM DIKOMPILASI / DITES. Perlu dicek di 1.20.1: `World.spawn(..., randomizeData, setup)` (summon warden) dan refleksi `WardenAi.setDigCooldown` (kalau gagal, diabaikan diam-diam).
