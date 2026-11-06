# LOGUPDATE - sttcustomskill (v3.0.0)

Spesifikasi: BIGUPDATE.md. `depend: [sttengine]`, `softdepend: [MythicMobs, MythicLib]`.

## Skill terdaftar (kode)
| Field | Nilai |
|---|---|
| `id` | `meteor_awakened` |
| `name` | Awakened Meteor |
| `phrase` | `meteor storm` (user menulis "meteor strom", dianggap typo) |
| `type` | `MYTHIC_MOBS` |
| `mythic-skill-name` | `Meteor` (huruf besar-kecil harus sama dengan `MythicMobs/Skills/*.yml`) |
| cost | 120 (dari user) |
| cooldown | 30 (default sementara dari contoh BIGUPDATE, user bilang akan diubah sendiri) |

`plugins/sttcustomskill/config.yml` hanya `skills.<id>.cost` dan `skills.<id>.cooldown`. Tidak ada damage/duration/modifiers.
Skill custom tidak masuk list ras. Diberikan admin: `/stt removeskill <player> meteor` lalu `/stt addskill <player> meteor_awakened`.

## Pasang pack `meteor_only` (varian MMOCORE)
Pasang manual, jangan pasang dua varian sekaligus (ID `Meteor` sama, saling menimpa).
- `MythicMobs/Skills/meteor_skills_mmocore.yml` (PASANG). `meteor_skills_mmoitems.yml` JANGAN dipasang. Hasil diff: kedua file identik 100%.
- `MythicMobs/Mobs/meteor_mobs.yml` (mob `Meteor` dan `Ground_Crack`, dipakai skill).
- `MythicMobs/Items/meteor_items.yml`, `ItemsAdder/contents/mage/...` (namespace `mage`, sound `mage_sounds:samus.*`).
- `MythicLib/skill/meteor.yml` tidak dipakai STT (boleh dipasang atau tidak).
- Server wajib punya MythicLib saja (tanpa MMOItems / MMOCore): skill pakai kondisi `mmoCanTarget`.

## Yang harus kamu edit sendiri di file pack (builder tidak mengedit pack dan tidak mengarang angka)
- Di `Meteor_HIT_D`: ganti `<modifier.duration>` di `ignite{ticks="20 * <modifier.duration>"}` dan `<modifier.damage>` di `damage{a="<modifier.damage>"}` dengan angka tetap.
- Di `Meteor_BURN`: ganti `<modifier.duration>` di `ignite`.
- Selama `<modifier.*>` masih ada, STT tetap jalan tapi damage dan burn kemungkinan 0.
- Saran: buang baris `Aura{auraName=Arcane_Missile_Aura;...}` di awal skill `Meteor` (sisa aura tanpa definisi).
- Cek bentrok: model ID PAPER 2258-2274 dan COAL 2069 (`icon_meteor`) tidak boleh dipakai pack lain.

## Test wajib sebelum rilis (belum dijalankan, butuh server)
1. Cast `meteor storm` lewat voice: damage dan burn benar-benar masuk (bukan 0).
2. `mmoCanTarget` bekerja lewat jalur cast ini: target terlarang / zona no-PvP tidak kena.
3. Caster tidak ikut kena skill area (`@EntitiesNearOrigin{r=6}` pada `Meteor_HIT`).
4. Kalau MythicMobs dimatikan: skill ini nonaktif, mana dan cooldown tidak berubah.

## Catatan
- `meteor storm`: kata `meteor` dan `storm` harus ada di kosakata `vosk-model-small-en-us-0.15`. Engine akan warning di console kalau tidak ada.
- BELUM DIKOMPILASI (lihat LOGUPDATE sttengine).
