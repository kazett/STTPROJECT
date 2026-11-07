# LOGUPDATE - sttresourcepack (v3.0.0)

Resource pack (bukan plugin): HUD mana dan file voice/audio. Dipasang manual, atau lewat ItemsAdder `/iazip` kalau dipakai. STT tidak mengirim pack ini sendiri.

## Isi (dari `stt-mana-pack.zip` v2.3.7)
- (diganti di revisi HUD di bawah) `mana.json`, `mana_bar.png`, `mana_digits.png`, font `stt:mana`.
- `assets/stt/sounds.json`, `assets/stt/sounds/shinra_tensei.ogg` (audio Shinra Tense, belum dipakai build ini, dibiarkan).

## Perubahan
- `pack.mcmeta`: `pack_format` 75 (+ `supported_formats` 34-75) diganti `15` (format pack Minecraft 1.20.1). Tanpa ini client 1.20.1 menandai pack tidak kompatibel.
- File HUD dan audio tidak diubah.

## Revisi HUD (bar mana baru + kartu skill)
- Dihapus: `font/mana.json`, `textures/font/mana_bar.png`, `mana_digits.png` (font `stt:mana`).
- Baru, font di `assets/stt/font/`: `hud_bar` (latar, isi, frame bar), `hud_card` (kartu hijau `\uE010` / merah `\uE011`), `hud_name` (nama skill, ASCII), `hud_timer` (angka cooldown merah), `hud_cost` (ikon permata/jam + angka cost), `hud_num` (angka di dalam bar, `300 / 300`). Tekstur di `textures/font/`.
- Semua font punya glyph spasi geser `\uE300`-`\uE30A` (+1..+1024) dan `\uE320`-`\uE32A` (-1..-1024). Engine memakainya untuk menumpuk bar, kartu, dan teks dalam SATU actionbar (bar di atas, kartu di bawahnya).
- Kartu tidak ditetapkan per ras: jumlah dan isinya dibuat engine dari skill player saat itu (lihat LOGUPDATE_sttengine.md). Pack hanya berisi gambar kartu dan huruf.
- Teks pakai font Lora (SIL OFL). Gambar dibuat oleh `tools/hudgen/build.py` (butuh Python, Pillow, numpy dan Lora-Variable.ttf). Script yang sama menulis ulang `sttengine/HudMetrics.java` supaya lebar glyph di kode selalu sama dengan pack. Ubah ukuran / font di script, jalankan ulang: `python3 tools/hudgen/build.py sttresourcepack sttengine/HudMetrics.java`.
- `sttresourcepack.zip` (di dalam folder ini) dibuat ulang dari isi folder.

## Perlu dites (HUD)
- Pasang pack baru di client 1.20.1 (hapus versi lama). Cek bar dan kartu tampil di tengah bawah, tidak menabrak hati/armor.
- Diuji hanya lewat simulasi susunan glyph di sandbox (`tools/hudgen/preview.py`), belum di client Minecraft asli. Kalau ada geseran beberapa pixel, atur konstanta di `build.py` / `HudLayout.java`.
- Bayangan teks bawaan client tetap muncul di bawah gambar (efek drop shadow).
