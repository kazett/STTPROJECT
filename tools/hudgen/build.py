"""Bangun resource pack HUD STT: tekstur, font JSON, dan HudMetrics.java (lebar glyph yang dihitung persis seperti client)."""
import json, os, sys
from PIL import Image, ImageDraw, ImageFont
import art
from art import S, BAR_W, BAR_H, CARD_W, CARD_H, GAP, CELL_H, FILL_UNIT

OUT = sys.argv[1]            # .../sttresourcepack
ASSET = os.path.join(OUT, "assets/stt")
TEX = os.path.join(ASSET, "textures/font")
FONT = os.path.join(ASSET, "font")
LORA = "/usr/share/fonts/truetype/google-fonts/Lora-Variable.ttf"
os.makedirs(TEX, exist_ok=True); os.makedirs(FONT, exist_ok=True)

def adv(actual_w):                     # sama dengan BitmapProvider 1.20.1, scale = 1/S
    return int(0.5 + actual_w / S) + 1

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
bg = art.mark_last_col(art.pad_bottom(art.bar_bg(), CELL_H))
frame = art.mark_last_col(art.pad_bottom(art.bar_frame(), CELL_H))
tile = art.mark_last_col(art.pad_bottom(art.bar_fill_tile(), CELL_H))
bg.save(os.path.join(TEX, "bar_bg.png")); frame.save(os.path.join(TEX, "bar_frame.png")); tile.save(os.path.join(TEX, "bar_fill.png"))
metrics = {"BAR_ADV": adv(actual_width(frame)), "BG_ADV": adv(actual_width(bg)), "FILL_ADV": adv(actual_width(tile))}
write_font("hud_bar", [
    bitmap("bar_bg.png", None, CELL_H, ["\ue000"]),
    bitmap("bar_fill.png", None, CELL_H, ["\ue001"]),
    bitmap("bar_frame.png", None, CELL_H, ["\ue002"]),
])

# ---- kartu ------------------------------------------------------------------------
ready, cool = art.card("ready"), art.card("cool")
atlas = Image.new("RGBA", (ready.width, ready.height * 2), (0, 0, 0, 0))
atlas.paste(ready, (0, 0)); atlas.paste(cool, (0, ready.height))
art.mark_last_col(atlas.crop((0, 0, ready.width, ready.height)))
for i, im in enumerate((ready, cool)):
    art.mark_last_col(im); atlas.paste(im, (0, i * ready.height))
atlas.save(os.path.join(TEX, "card.png"))
metrics["CARD_ADV"] = adv(actual_width(ready))
write_font("hud_card", [{"type": "bitmap", "file": "stt:font/card.png", "ascent": CARD_H, "height": CARD_H, "chars": ["\ue010", "\ue011"]}])

# ---- atlas teks -------------------------------------------------------------------
def load_font(px, weight):
    f = ImageFont.truetype(LORA, px)
    f.set_variation_by_name(weight)
    return f

def text_atlas(file, chars, cell_w, cell_h_gui, baseline_gui, px, weight, icons=None, cols=16, xscale=1.0):
    """Sel bawah = Y 0, tinggi cell_h_gui. baseline_gui = jarak baseline dari bawah sel."""
    font = load_font(px, weight)
    ch = cell_h_gui * S
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
            ImageDraw.Draw(wide).text((0, ch - baseline_gui * S), c, font=font, fill=(255, 255, 255, 255), anchor="ls")
            cell = wide if xscale == 1.0 else wide.resize((cell_w, ch), Image.LANCZOS)
        if cell.getbbox() is None:
            grid[r] += "\0"; continue
        widths[c] = adv(actual_width(cell))
        img.paste(cell, (col * cell_w, r * ch))
        grid[r] += c
    while len(grid[-1]) < cols:
        grid[-1] += "\0"
    for r in range(rows):
        grid[r] = grid[r].ljust(cols, "\0")
    img.save(os.path.join(TEX, file))
    return {"type": "bitmap", "file": "stt:font/" + file, "ascent": cell_h_gui, "height": cell_h_gui, "chars": grid}, widths

def lay(y_from_top, cap_gui):                # baseline gui dari bawah sel kartu, untuk teks rata tengah y_from_top
    return CELL_H_CARD - y_from_top - cap_gui / 2

CELL_H_CARD = CARD_H
ASCII = [chr(c) for c in range(33, 127)]
name_prov, name_w = text_atlas("name.png", ASCII, 27, CARD_H, CARD_H - 10.8 - 2.5, 21, "SemiBold", cols=16, xscale=0.9)
timer_chars = list("0123456789.s")
timer_prov, timer_w = text_atlas("timer.png", timer_chars, 30, 20, 8.3, 26, "Bold", cols=12)

def gem(cell):
    d = ImageDraw.Draw(cell, "RGBA")
    cy = cell.height - 5.2 * S
    pts = [(1, cy), (6.5, cy - 8), (12, cy), (6.5, cy + 8)]
    d.polygon(pts, fill=(60, 130, 255, 255), outline=(10, 20, 70, 255))
    d.polygon([(1, cy), (6.5, cy - 8), (6.5, cy)], fill=(150, 220, 255, 255))
    d.polygon([(6.5, cy - 8), (12, cy), (6.5, cy)], fill=(90, 170, 255, 255))
    d.line(pts + [pts[0]], fill=(10, 20, 70, 255), width=1)

def clock(cell):
    d = ImageDraw.Draw(cell, "RGBA")
    cy = cell.height - 5.2 * S
    cx = 7
    d.ellipse([cx - 6, cy - 6, cx + 6, cy + 6], outline=(255, 255, 255, 255), width=2)
    d.line([(cx, cy), (cx, cy - 4)], fill=(255, 255, 255, 255), width=2)
    d.line([(cx, cy), (cx + 3, cy + 1)], fill=(255, 255, 255, 255), width=2)

cost_chars = list("0123456789.s") + ["\ue020", "\ue021"]
cost_prov, cost_w = text_atlas("cost.png", cost_chars, 21, 9, 3.6, 17, "SemiBold", icons={"\ue020": gem, "\ue021": clock}, cols=14)
num_chars = list("0123456789/")
num_prov, num_w = text_atlas("barnum.png", num_chars, 33, CELL_H, CELL_H - 11 - 3.2, 27, "SemiBold", cols=11)

write_font("hud_name", [name_prov, {"type": "space", "advances": {" ": 3}}])
write_font("hud_timer", [timer_prov])
write_font("hud_cost", [cost_prov])
write_font("hud_num", [num_prov, {"type": "space", "advances": {" ": 4}}])

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
    static final int BG_ADV = {metrics["BG_ADV"]};
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
