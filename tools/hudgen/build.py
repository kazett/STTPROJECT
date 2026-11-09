"""Bangun resource pack HUD STT: tekstur, font JSON, dan HudMetrics.java (lebar glyph yang dihitung persis seperti client)."""
import json, os, sys
from PIL import Image, ImageDraw, ImageFont
import art
from art import S, BAR_W, BAR_H, CARD_W, CARD_H, GAP, CELL_H, FILL_UNIT, ROW_SHIFT

OUT = sys.argv[1]            # .../sttresourcepack
ASSET = os.path.join(OUT, "assets/stt")
TEX = os.path.join(ASSET, "textures/font")
FONT = os.path.join(ASSET, "font")
LORA = "/usr/share/fonts/truetype/google-fonts/Lora-Variable.ttf"
os.makedirs(TEX, exist_ok=True); os.makedirs(FONT, exist_ok=True)

def adv(actual_w, s=S):                # sama dengan BitmapProvider 1.20.1, scale = 1/s
    return int(0.5 + actual_w / s) + 1

def actual_width(img):
    a = img.getchannel("A")
    for x in range(img.width - 1, -1, -1):
        if a.crop((x, 0, x + 1, img.height)).getbbox():
            return x + 1
    return 0

# ---- glyph spasi: +/-1,2,4,...,1024 -------------------------------------------------
POS0, NEG0 = 0xE300, 0xE320
def space_provider():
    adv_map = {}
    for k in range(11):
        adv_map[chr(POS0 + k)] = 1 << k
        adv_map[chr(NEG0 + k)] = -(1 << k)
    return {"type": "space", "advances": adv_map}

def write_font(name, providers):
    with open(os.path.join(FONT, name + ".json"), "w") as f:
        json.dump({"providers": providers + [space_provider()]}, f, indent=2)

def bitmap(file, chars, cell_h_gui, rows):
    return {"type": "bitmap", "file": "stt:font/" + file, "ascent": cell_h_gui, "height": cell_h_gui, "chars": rows}

# ---- bar --------------------------------------------------------------------------
# Satu glyph per lapisan (latar, frame), lebar <= 256 px. Potongan 3 glyph (v3.0.3) tidak ke-render di client:
# glyph ke-2 dan ke-3 hilang dan lebarnya 0, jadi seluruh susunan di sebelah kanannya ikut geser.
BAR_PX_W, BAR_PX_H = 256, 81           # 180 x 57 gui px -> sekitar 1.42 px per gui px
UP_CELL_H = CELL_H + ROW_SHIFT

def bar_adv(actual_w, cell_h, px_h):
    return int(0.5 + actual_w * cell_h / px_h) + 1

def bar_set(cell_h, suffix):
    px_h = round(cell_h * BAR_PX_H / CELL_H)
    assert px_h * CELL_H == cell_h * BAR_PX_H and px_h <= 256

    def fit(img):
        return art.mark_last_col(img.resize((BAR_PX_W, px_h), Image.LANCZOS))

    def tile_of(low):
        full = art.pad_bottom(art.bar_fill_tile(low), cell_h)
        return art.mark_last_col(full.resize((max(1, round(full.width * BAR_PX_W / (BAR_W * S))), px_h), Image.LANCZOS))

    bg = fit(art.pad_bottom(art.bar_bg(), cell_h))
    frame = fit(art.pad_bottom(art.bar_frame(), cell_h))
    tile, tile_low = tile_of(False), tile_of(True)
    for name, img in (("bar_bg", bg), ("bar_fill", tile), ("bar_frame", frame), ("bar_fill_low", tile_low)):
        img.save(os.path.join(TEX, name + suffix + ".png"))
    bar = bar_adv(actual_width(bg), cell_h, px_h)
    fill = bar_adv(actual_width(tile), cell_h, px_h)
    assert bar == bar_adv(actual_width(frame), cell_h, px_h) and fill == bar_adv(actual_width(tile_low), cell_h, px_h)
    write_font("hud_bar" + suffix, [
        bitmap("bar_bg" + suffix + ".png", None, cell_h, ["\ue000"]),
        bitmap("bar_fill" + suffix + ".png", None, cell_h, ["\ue001"]),
        bitmap("bar_frame" + suffix + ".png", None, cell_h, ["\ue002"]),
        bitmap("bar_fill_low" + suffix + ".png", None, cell_h, ["\ue003"]),
    ])
    return bar, fill

bar_main, fill_main = bar_set(CELL_H, "")
bar_up, fill_up = bar_set(UP_CELL_H, "_u")
assert (bar_main, fill_main) == (bar_up, fill_up)
metrics = {"BAR_ADV": bar_main, "FILL_ADV": fill_main}

# ---- kartu ------------------------------------------------------------------------
CARD_CELLS = [art.card(v) for v in ("ready", "cool", "low")]

def card_set(cell_h, suffix):
    ph = cell_h * S
    atlas = Image.new("RGBA", (CARD_CELLS[0].width, ph * len(CARD_CELLS)), (0, 0, 0, 0))
    for i, im in enumerate(CARD_CELLS):
        atlas.paste(art.mark_last_col(art.pad_bottom(im, cell_h)), (0, i * ph))
    atlas.save(os.path.join(TEX, "card" + suffix + ".png"))
    write_font("hud_card" + suffix, [{"type": "bitmap", "file": "stt:font/card" + suffix + ".png", "ascent": cell_h, "height": cell_h, "chars": ["\ue010", "\ue011", "\ue012"]}])
    return adv(actual_width(CARD_CELLS[0]))

metrics["CARD_ADV"] = card_set(CARD_H, "")
assert card_set(CARD_H + ROW_SHIFT, "_u") == metrics["CARD_ADV"]

# ---- atlas teks -------------------------------------------------------------------
def load_font(px, weight):
    f = ImageFont.truetype(LORA, px)
    f.set_variation_by_name(weight)
    return f

def text_atlas(file, chars, cell_w, cell_h_gui, baseline_gui, px, weight, icons=None, cols=16, xscale=1.0, s=S):
    """Sel bawah = Y 0, tinggi cell_h_gui. baseline_gui = jarak baseline dari bawah sel. s = px tekstur per gui px."""
    font = load_font(px, weight)
    ch = round(cell_h_gui * s)
    assert ch <= 256
    rows = (len(chars) + cols - 1) // cols
    img = Image.new("RGBA", (cols * cell_w, rows * ch), (0, 0, 0, 0))
    widths = {}
    grid = []
    for r in range(rows):
        grid.append("")
    for i, c in enumerate(chars):
        r, col = divmod(i, cols)
        cell = Image.new("RGBA", (cell_w, ch), (0, 0, 0, 0))
        if c == "\0":
            grid[r] += "\0"; continue
        if icons and c in icons:
            icons[c](cell)
        else:
            wide = Image.new("RGBA", (int(cell_w / xscale), ch), (0, 0, 0, 0))
            ImageDraw.Draw(wide).text((0, ch - baseline_gui * s), c, font=font, fill=(255, 255, 255, 255), anchor="ls")
            cell = wide if xscale == 1.0 else wide.resize((cell_w, ch), Image.LANCZOS)
        if cell.getbbox() is None:
            grid[r] += "\0"; continue
        widths[c] = adv(actual_width(cell), s)
        img.paste(cell, (col * cell_w, r * ch))
        grid[r] += c
    while len(grid[-1]) < cols:
        grid[-1] += "\0"
    for r in range(rows):
        grid[r] = grid[r].ljust(cols, "\0")
    img.save(os.path.join(TEX, file))
    return {"type": "bitmap", "file": "stt:font/" + file, "ascent": cell_h_gui, "height": cell_h_gui, "chars": grid}, widths

def gem(cell, shift=0):
    d = ImageDraw.Draw(cell, "RGBA")
    cy = cell.height - (5.2 + shift) * S
    pts = [(1, cy), (6.5, cy - 8), (12, cy), (6.5, cy + 8)]
    d.polygon(pts, fill=(60, 130, 255, 255), outline=(10, 20, 70, 255))
    d.polygon([(1, cy), (6.5, cy - 8), (6.5, cy)], fill=(150, 220, 255, 255))
    d.polygon([(6.5, cy - 8), (12, cy), (6.5, cy)], fill=(90, 170, 255, 255))
    d.line(pts + [pts[0]], fill=(10, 20, 70, 255), width=1)

def clock(cell, shift=0):
    d = ImageDraw.Draw(cell, "RGBA")
    cy = cell.height - (5.2 + shift) * S
    cx = 7
    d.ellipse([cx - 6, cy - 6, cx + 6, cy + 6], outline=(255, 255, 255, 255), width=2)
    d.line([(cx, cy), (cx, cy - 4)], fill=(255, 255, 255, 255), width=2)
    d.line([(cx, cy), (cx + 3, cy + 1)], fill=(255, 255, 255, 255), width=2)

ASCII = [chr(c) for c in range(33, 127)]
timer_chars = list("0123456789.s")
cost_chars = list("0123456789.s") + ["\ue020", "\ue021"]
num_chars = list("0123456789/")
# Angka mana di ATAS bar: sel glyph dibuat LABEL_PAD gui lebih tinggi dari sel bar (ascent = height = tinggi sel),
# jadi sisi atasnya menjorok di atas bar. Digit digambar di bagian atas sel itu, bar mulai LABEL_PAD gui di bawahnya.
LABEL_PAD = 13
NUM_CELL_H = CELL_H + LABEL_PAD
NUM_BASELINE_FROM_TOP = 10.5
NUM_UP_S = 7 / 3

def text_set(shift, suffix):
    name_prov, name_w = text_atlas("name" + suffix + ".png", ASCII, 27, CARD_H + shift, CARD_H - 10.8 - 2.5 + shift, 21, "SemiBold", cols=16, xscale=0.9)
    timer_prov, timer_w = text_atlas("timer" + suffix + ".png", timer_chars, 30, 20 + shift, 8.3 + shift, 26, "Bold", cols=12)
    cost_prov, cost_w = text_atlas("cost" + suffix + ".png", cost_chars, 21, 9 + shift, 3.6 + shift, 17, "SemiBold", icons={"\ue020": lambda c: gem(c, shift), "\ue021": lambda c: clock(c, shift)}, cols=14)
    if shift:
        num_prov, num_w = text_atlas("barnum" + suffix + ".png", num_chars, 28, NUM_CELL_H + shift, NUM_CELL_H + shift - NUM_BASELINE_FROM_TOP, 28, "SemiBold", cols=11, s=NUM_UP_S)
    else:
        num_prov, num_w = text_atlas("barnum.png", num_chars, 36, NUM_CELL_H, NUM_CELL_H - NUM_BASELINE_FROM_TOP, 36, "SemiBold", cols=11)
    write_font("hud_name" + suffix, [name_prov, {"type": "space", "advances": {" ": 3}}])
    write_font("hud_timer" + suffix, [timer_prov])
    write_font("hud_cost" + suffix, [cost_prov])
    write_font("hud_num" + suffix, [num_prov, {"type": "space", "advances": {" ": 4}}])
    return name_w, timer_w, cost_w, num_w

name_w, timer_w, cost_w, num_w = text_set(0, "")
name_wu, timer_wu, cost_wu, num_wu = text_set(ROW_SHIFT, "_u")
assert name_w == name_wu and timer_w == timer_wu and cost_w == cost_wu and num_w == num_wu

# ---- HudMetrics.java --------------------------------------------------------------
def table(widths, chars, default):
    return ", ".join(str(widths.get(c, default + 1) - 1) for c in chars)

space_name, space_num = 3, 4
java = f'''package id.stt.engine.hud;

/** DIHASILKAN oleh gen/build.py bersama tekstur sttresourcepack. Jangan diedit tangan: ukuran harus sama dengan pack. */
final class HudMetrics {{

    private HudMetrics() {{
    }}

    static final int BAR_W = {BAR_W};
    static final int CARD_W = {CARD_W};
    static final int FILL_X0 = {art.FILL_X0};
    static final int FILL_UNIT = {FILL_UNIT};
    static final int FILL_MAX = {art.FILL_MAX};
    static final int BAR_ADV = {metrics["BAR_ADV"]};
    static final int FILL_ADV = {metrics["FILL_ADV"]};
    static final int CARD_ADV = {metrics["CARD_ADV"]};
    static final int GEM_ADV = {cost_w["\ue020"]};
    static final int CLOCK_ADV = {cost_w["\ue021"]};
    /** Lebar bersih (advance - 1, tiap glyph diikuti geser -1) glyph ASCII 33..126 pada font stt:hud_name. Spasi = {space_name}. */
    static final int[] NAME = {{{table(name_w, [chr(c) for c in range(33, 127)], 6)}}};
    static final int NAME_SPACE = {space_name};
    /** Lebar bersih glyph "0123456789.s" pada font stt:hud_timer. */
    static final int[] TIMER = {{{table(timer_w, timer_chars, 8)}}};
    /** Lebar bersih glyph "0123456789.s" pada font stt:hud_cost. */
    static final int[] COST = {{{table(cost_w, timer_chars, 5)}}};
    /** Lebar bersih glyph "0123456789/" pada font stt:hud_num. Spasi = {space_num}. */
    static final int[] NUM = {{{table(num_w, num_chars, 8)}}};
    static final int NUM_SPACE = {space_num};
}}
'''
with open(sys.argv[2], "w") as f:
    f.write(java)
json.dump({"metrics": metrics, "name": name_w, "timer": timer_w, "cost": {k: v for k, v in cost_w.items()}, "num": num_w},
          open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "metrics.json"), "w"), indent=1)
print("ok", metrics)
