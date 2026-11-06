# LOGUPDATE - sttskill (v3.0.0)

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
