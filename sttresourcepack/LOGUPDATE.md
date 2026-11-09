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

## v3.0.3 - fix bar HUD jadi kotak
- Penyebab: client 1.20.1 hanya bisa menaruh glyph bitmap dengan ukuran sel maksimal 256 x 256 px di atlas font. `bar_bg.png` dan `bar_frame.png` selamanya 540 px per glyph, jadi diam-diam diganti glyph "kotak" tanpa error di log. Simulasi `preview.py` lama tidak mengecek batas ini.
- `hud_bar.json`: `bar_bg.png` dan `bar_frame.png` sekarang dipotong 3 kolom (tiap sel 180 x 171 px): latar `\uE000\uE003\uE004`, frame `\uE002\uE005\uE006`. Tekstur PNG tidak diubah. Tiap potongan lebar 60 gui px, advance 61.
- `sttengine`: `HudLayout.putBar` menaruh 3 potongan berurutan (geser -1 antar potongan), `HudMetrics` `BAR_ADV` dan `BG_ADV` diganti `PIECE_W` (60) dan `PIECE_ADV` (61). Lebar bar total tetap 180.
- `tools/hudgen/build.py` dan `preview.py` disesuaikan (preview sekarang gagal kalau sel glyph > 256 px).
- Font lain sudah di bawah batas (kartu 168 x 96, nama 27 x 96, angka bar 33 x 171). BELUM DITES di client Minecraft asli. Pasang pack baru dan hapus versi lama.

## v3.0.4 - fix bar bolong
- `font/hud_bar.json`: `bar_bg.png` dan `bar_frame.png` kembali 1 glyph (``, ``), tidak dipotong 3 lagi. Glyph ``-`` dihapus.
- `bar_bg.png`, `bar_frame.png`: 256 x 81 px (sebelumnya 540 x 171). `bar_fill.png`: 3 x 81 px. Gambar gui tetap 180 x 57.
- `tools/hudgen/build.py` disesuaikan (bar = 1 glyph, `BAR_ADV`) dan menulis ulang `sttengine/HudMetrics.java`. File pack lain tidak diubah.
- `sttresourcepack.zip` dibuat ulang. Pasang pengganti pack lama, lalu relog / reload resource pack di client.
- Diuji hanya lewat simulasi susunan glyph (`preview.py`), belum di client Minecraft asli.

## v3.0.6
- `barnum.png`: sel glyph angka jadi 36 x 210 px (36 x 70 gui, sebelumnya 33 x 171), digit di bagian atas sel. `font/hud_num.json` ascent/height 57 -> 70.
- `tools/hudgen/build.py`: `LABEL_PAD = 13`, `NUM_BASELINE_FROM_TOP`. Menulis ulang `HudMetrics.java` (hanya `NUM` yang berubah).
- File pack lain tidak diubah. Pasang `sttresourcepack.zip` baru.

## v3.0.7
- `bar_frame.png`, `card.png`, `bar_bg.png`, `bar_fill.png` dibuat ulang: lubang transparan di berlian bar dan permata kartu ditutup (lihat LOGUPDATE sttengine v3.0.7). Ukuran dan metrik tidak berubah.
- `tools/hudgen/art.py`: gambar hiasan lewat layer + `alpha_composite`.

## v3.0.8
- Font baru `hud_bar_u`, `hud_card_u`, `hud_name_u`, `hud_timer_u`, `hud_cost_u`, `hud_num_u` dengan tekstur `*_u.png`: glyph dan kode karakter sama dengan font aslinya, tapi sel diberi ruang bening di bawah supaya naik 38 gui px (ascent tidak boleh lebih besar dari height, jadi naik lewat tinggi sel). Dipakai engine untuk mode dua baris.
- `hud_bar` dan `hud_bar_u`: glyph baru `\uE003` (isi bar merah, `bar_fill_low.png` dan `bar_fill_low_u.png`).
- `hud_card` dan `hud_card_u`: glyph baru `\uE012` (kartu amber, mana kurang). `card.png` jadi 3 sel, dua sel pertama identik piksel dengan v3.0.7.
- `barnum_u.png` memakai 7/3 px per gui px (bukan 3) karena sel 108 gui px x 3 = 324 melebihi batas 256 px. Lebar digitnya sama persis dengan versi biasa, jadi `HudMetrics.java` tidak berubah (build.py menolak kalau lebarnya beda).
- Semua tekstur v3.0.7 identik piksel (cost, name, timer, bar_bg, bar_fill, bar_frame, barnum).
- `tools/hudgen/build.py` dan `art.py` disesuaikan (`ROW_SHIFT = 38`). Jalankan ulang: `python3 tools/hudgen/build.py sttresourcepack sttengine/HudMetrics.java`.
- `sttresourcepack.zip` dibuat ulang. Hapus pack lama di client dan relog. BELUM DITES di client Minecraft asli.
