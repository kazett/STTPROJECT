# LOGUPDATE - sttresourcepack (v3.0.0)

Resource pack (bukan plugin): HUD mana dan file voice/audio. Dipasang manual, atau lewat ItemsAdder `/iazip` kalau dipakai. STT tidak mengirim pack ini sendiri.

## Isi (dari `stt-mana-pack.zip` v2.3.7)
- `assets/stt/font/mana.json`, `assets/stt/textures/font/mana_bar.png`, `mana_digits.png` (HUD mana, font `stt:mana`).
- `assets/stt/sounds.json`, `assets/stt/sounds/shinra_tensei.ogg` (audio Shinra Tense, belum dipakai build ini, dibiarkan).

## Perubahan
- `pack.mcmeta`: `pack_format` 75 (+ `supported_formats` 34-75) diganti `15` (format pack Minecraft 1.20.1). Tanpa ini client 1.20.1 menandai pack tidak kompatibel.
- File HUD dan audio tidak diubah.
- HUD memakai 3 digit angka, cukup untuk max-mana 300.

## Perlu dites
- Bar mana tampil di actionbar (50 level, glyph `\uE000`-`\uE032`, digit `\uE100`-`\uE109`) di client 1.20.1.
