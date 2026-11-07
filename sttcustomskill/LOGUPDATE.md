# LOGUPDATE - sttcustomskill (v3.0.3)

Spesifikasi: BIGUPDATE.md. `depend: [sttengine]`, `softdepend: [MythicMobs, MythicLib]`.

## Skill terdaftar (kode)
| Field | Nilai |
|---|---|
| `id` | `meteor_awakened` |
| `name` | Awakened Meteor |
| `phrase` | `meteor storm` (user menulis "meteor strom", dianggap typo) |
| `type` | `MYTHIC_MOBS` |
| `mythic-skill-name` | `Meteor_Storm` (huruf besar-kecil harus sama dengan `MythicMobs/Skills/meteor_storm_skills.yml`) |
| cost | 120 (dari user) |
| cooldown | 30 (default sementara dari contoh BIGUPDATE, user bilang akan diubah sendiri) |

`plugins/sttcustomskill/config.yml` hanya `skills.<id>.cost` dan `skills.<id>.cooldown`. Tidak ada damage/duration/modifiers.
Skill custom tidak masuk list ras. Diberikan admin: `/stt removeskill <player> meteor` lalu `/stt addskill <player> meteor_awakened`.

## Pasang pack `meteor_storm` (pengganti `meteor_only`)
Pasang manual. Hapus dulu file pack lama `meteor_only` (skill `Meteor`, mob `Meteor` dan `Ground_Crack`, item, namespace `mage`) supaya tidak numpuk.
- `MythicMobs/Skills/meteor_storm_skills.yml`, `MythicMobs/Mobs/meteor_storm_mobs.yml`, `MythicMobs/Items/meteor_storm_items.yml`.
- `ItemsAdder/contents/archmage/...` (namespace `archmage`, sound `archmage_sounds:samus.*`, model PAPER 2726-2731).
- `ModelEngine/blueprints/meteor_storm_magic_circle.bbmodel` dan `meteor_storm_meteor.bbmodel` (butuh ModelEngine).
- `MythicLib/skill/meteor_storm.yml` tidak dipakai STT (boleh dipasang atau tidak).
- Skill yang dipanggil STT: `Meteor_Storm`. Skill ini stun caster 4 detik (`stun{d=80}`) dan menembak 7 gelombang meteor acak.

## Yang harus kamu edit sendiri di file pack (builder tidak mengedit pack dan tidak mengarang angka)
- Di `Meteor_Storm_Meteor_HIT_D` (`meteor_storm_skills.yml`): ganti `<modifier.damage>` di `damage{a="<modifier.damage>"}` dengan angka tetap.
- Selama `<modifier.damage>` masih ada, STT tetap jalan tapi damage kemungkinan 0.
- Cek bentrok: model ID PAPER 2726-2731 tidak boleh dipakai pack lain.

## Test wajib sebelum rilis (belum dijalankan, butuh server)
1. Cast `meteor storm` lewat voice: damage dan burn benar-benar masuk (bukan 0).
2. `mmoCanTarget` bekerja lewat jalur cast ini: target terlarang / zona no-PvP tidak kena.
3. Caster tidak ikut kena skill area (`@EntitiesNearOrigin{r=6}` pada `Meteor_Storm_Meteor_HIT`).
4. Kalau MythicMobs dimatikan: skill ini nonaktif, mana dan cooldown tidak berubah.

## Catatan
- `meteor storm`: kata `meteor` dan `storm` harus ada di kosakata `vosk-model-small-en-us-0.15`. Engine akan warning di console kalau tidak ada.
- BELUM DIKOMPILASI (lihat LOGUPDATE sttengine).

## v3.0.2
- `mythic-skill-name` `Meteor` -> `Meteor_Storm`: skill custom sekarang memanggil pack `meteor_storm`. Id, nama, phrase, cost, cooldown tidak diubah.
- Pack lama `meteor_only` tidak lagi dipanggil. BELUM DITES di server.

## v3.0.4
- ID skill custom `meteor_awakened` diganti `meteor_storm`, nama tampil `Awakened Meteor` -> `Meteor Storm` (phrase `meteor storm` dan skill MythicMobs `Meteor_Storm` tidak berubah).
- Player yang sudah diberi `meteor_awakened` perlu `/stt addskill <player> meteor_storm` ulang. ID lama di file player diabaikan (tidak dihapus). Config `skills.meteor_awakened.*` di `plugins/sttcustomskill/config.yml` diganti key baru `skills.meteor_storm.*` (angka cost/cooldown di key lama tidak terbawa, edit ulang kalau perlu).
