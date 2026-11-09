"""Gambar tekstur HUD STT (bar mana + kartu skill). Semua ukuran dalam 'gui px', 1 gui px = S px tekstur."""
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageChops

S = 3          # px tekstur per gui px
SS = 4         # supersample
K = S * SS

def canvas(wg, hg):
    return Image.new("RGBA", (int(wg * K), int(hg * K)), (0, 0, 0, 0))

def finish(img, wg, hg):
    return img.resize((int(wg * S), int(hg * S)), Image.LANCZOS)

def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)

def grad(w, h, stops, axis="v"):
    """stops: [(pos, '#rrggbb' atau rgba)] -> RGBA image ukuran w x h px."""
    n = h if axis == "v" else w
    pos = [p for p, _ in stops]
    cols = np.array([hexc(c) if isinstance(c, str) else c for _, c in stops], dtype=float)
    t = np.linspace(0, 1, n)
    ch = [np.interp(t, pos, cols[:, i]) for i in range(4)]
    line = np.stack(ch, axis=1)
    arr = np.repeat(line[:, None, :], w, axis=1) if axis == "v" else np.repeat(line[None, :, :], h, axis=0)
    return Image.fromarray(arr.astype(np.uint8), "RGBA")

def mask_poly(w, h, pts, wg_scale=K):
    m = Image.new("L", (w, h), 0)
    ImageDraw.Draw(m).polygon([(x * wg_scale, y * wg_scale) for x, y in pts], fill=255)
    return m

def mask_rrect(w, h, box, r):
    m = Image.new("L", (w, h), 0)
    x0, y0, x1, y1 = box
    ImageDraw.Draw(m).rounded_rectangle([x0 * K, y0 * K, x1 * K, y1 * K], radius=r * K, fill=255)
    return m

def erode(m, gui):
    size = int(gui * K) * 2 + 1
    return m.filter(ImageFilter.MinFilter(size))

def blur(img, gui):
    return img.filter(ImageFilter.GaussianBlur(gui * K))

def paste_fill(base, mask, fill_img):
    layer = Image.new("RGBA", base.size, (0, 0, 0, 0))
    layer.paste(fill_img, (0, 0), mask)
    base.alpha_composite(layer)

def solid(w, h, c):
    return Image.new("RGBA", (w, h), hexc(c) if isinstance(c, str) else c)

def tint_glow(mask, color, radius, strength):
    g = blur(mask, radius)
    g = g.point(lambda v: int(min(255, v * strength)))
    layer = Image.new("RGBA", mask.size, hexc(color))
    layer.putalpha(g)
    return layer

SILVER = [(0, "#F6FAFF"), (0.3, "#BCC9E2"), (0.62, "#7C8BAE"), (1, "#445076")]
CARD_SILVER = [(0, "#E4EEFF"), (0.35, "#9FB6E6"), (0.7, "#5F74B4"), (1, "#34457E")]
OUTLINE = "#0E1738"

def shape_with_outline(base, mask, stops, outline_gui=0.45):
    w, h = base.size
    inner = erode(mask, outline_gui)
    rim = ImageChops.subtract(mask, inner)
    paste_fill(base, rim, solid(w, h, OUTLINE))
    paste_fill(base, inner, grad(w, h, stops))

def diamond(cx, cy, rx, ry):
    return [(cx - rx, cy), (cx, cy - ry), (cx + rx, cy), (cx, cy + ry)]

def crystal(base, cx, cy, rx, ry, light="#9FE0FF", mid="#3A78FF", dark="#1B3AA0"):
    w, h = base.size
    pts = diamond(cx, cy, rx, ry)
    m = mask_poly(w, h, pts)
    paste_fill(base, erode(m, 0.0) if False else m, grad(w, h, [(0, light), (0.5, mid), (1, dark)]))
    # ImageDraw pada gambar RGBA MENIMPA piksel (alpha ikut tertimpa, jadi muncul lubang). Gambar di layer terpisah lalu alpha_composite.
    layer = Image.new("RGBA", base.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    c = (cx * K, cy * K)
    lw = max(1, int(0.18 * K))
    for p in pts:
        d.line([c, (p[0] * K, p[1] * K)], fill=(220, 245, 255, 150), width=lw)
    d.polygon([(cx * K, (cy - ry) * K), (cx * K, cy * K), ((cx - rx) * K, cy * K)], fill=(255, 255, 255, 70))
    d.line([(p[0] * K, p[1] * K) for p in pts + [pts[0]]], fill=hexc("#0B1747", 230), width=lw)
    base.alpha_composite(layer)

# ---------------------------------------------------------------- BAR
BAR_W, BAR_H = 180, 22
CARD_W, CARD_H = 56, 32
DRAW_W = 52                              # kartu digambar 52 lalu direntang ke CARD_W
GAP = 3
CELL_H = BAR_H + GAP + CARD_H           # 57, bar di atas, kartu di bawah
ROW_SHIFT = 38
HOLE = (25.0, 7.0, 165.0, 15.0)         # lubang rel (tempat bar isi)
FILL_X0, FILL_UNIT, FILL_MAX = 26, 2, 69
FILL_Y0, FILL_Y1 = 6.8, 15.2

def bar_frame():
    wg, hg = BAR_W, BAR_H
    img = canvas(wg, hg)
    w, h = img.size
    hole = mask_rrect(w, h, HOLE, 3.0)
    rail = mask_rrect(w, h, (21, 4.2, 171, 17.8), 3.2)
    spike = mask_poly(w, h, [(158, 4.2), (172, 2.2), (180, 11), (172, 19.8), (158, 17.8)])
    star = mask_poly(w, h, [(0, 11), (8, 6.2), (13, 0.2), (16, 6.2), (32, 9.4), (21.5, 11), (32, 12.6),
                            (16, 15.8), (13, 21.8), (8, 15.8)])
    solid_shape = ImageChops.lighter(ImageChops.lighter(rail, spike), star)
    # glow biru di belakang, jangan menutupi lubang rel
    glow = tint_glow(solid_shape, "#3C78FF", 1.3, 2.2)
    keep = ImageChops.invert(hole)
    ga = ImageChops.multiply(glow.getchannel("A"), keep)
    glow.putalpha(ga)
    img.alpha_composite(glow)
    # lingkaran cahaya di belakang kristal
    ring_layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(ring_layer)
    for r, a in ((10.6, 150), (9.2, 90)):
        d.ellipse([(13 - r) * K, (11 - r) * K, (13 + r) * K, (11 + r) * K], outline=hexc("#5C8CFF", a), width=int(0.35 * K))
    img.alpha_composite(ring_layer)
    body = ImageChops.subtract(solid_shape, hole)
    shape_with_outline(img, body, SILVER)
    # garis dalam tipis di sekeliling lubang
    rim = ImageChops.subtract(hole, erode(hole, 0.35))
    paste_fill(img, rim, solid(w, h, "#091236"))
    # lubang harus benar2 bening
    a = img.getchannel("A")
    a = ImageChops.subtract(a, ImageChops.subtract(erode(hole, 0.35), Image.new("L", a.size, 0)))
    img.putalpha(a)
    # ornamen: kristal kiri + permata kanan
    ring = mask_poly(w, h, diamond(12.5, 11, 8.6, 9.4))
    paste_fill(img, ring, solid(w, h, "#0A1640"))
    crystal(img, 12.5, 11, 7.2, 8.2)
    crystal(img, 173, 11, 2.6, 3.2)
    # titik kecil bercahaya
    dots = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(dots)
    for x, y in ((6, 3), (4, 17), (24, 2.5), (168, 5)):
        d.ellipse([(x - .25) * K, (y - .25) * K, (x + .25) * K, (y + .25) * K], fill=(150, 200, 255, 220))
    img.alpha_composite(dots)
    return finish(img, wg, hg)

def bar_bg():
    wg, hg = BAR_W, BAR_H
    img = canvas(wg, hg)
    w, h = img.size
    m = mask_rrect(w, h, (HOLE[0] - 0.6, HOLE[1] - 0.4, HOLE[2] + 0.6, HOLE[3] + 0.4), 3.0)
    paste_fill(img, m, grad(w, h, [(0.25, "#040A22"), (0.5, "#0A1A55"), (0.7, "#0F2A7A")]))
    return finish(img, wg, hg)

def bar_fill_tile(low=False):
    """Satu ubin isi bar, lebar FILL_UNIT gui px, tinggi sama dengan cell glyph bar (diratakan atas)."""
    w, h = FILL_UNIT * S, BAR_H * S
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    y0, y1 = int(FILL_Y0 * S), int(FILL_Y1 * S)
    stops = [(0, "#8EC8FF"), (0.18, "#4F95FF"), (0.5, "#2C63F0"), (0.85, "#1B3FBE"), (1, "#14308F")]
    shine = (210, 235, 255, 255)
    if low:
        stops = [(0, "#FFA08E"), (0.18, "#FF5F4F"), (0.5, "#E8323A"), (0.85, "#B81C2C"), (1, "#841024")]
        shine = (255, 220, 212, 255)
    band = grad(w, y1 - y0, stops)
    img.paste(band, (0, y0))
    # garis kilau tipis
    px = img.load()
    ky = y0 + int((y1 - y0) * 0.28)
    for x in range(w):
        px[x, ky] = shine
    return img

# ---------------------------------------------------------------- CARD
def card(variant):
    wg, hg = DRAW_W, CARD_H
    img = canvas(wg, hg)
    w, h = img.size
    if variant == "ready":
        inner_top, inner_bot, rim_c = "#03231A", "#0B6A3A", "#2DFF86"
        glow_a, glow_b = "#25FF7A", "#3C78FF"
        floor = (40, 255, 130)
    elif variant == "low":
        inner_top, inner_bot, rim_c = "#2A1904", "#7A4A0A", "#FFB02E"
        glow_a, glow_b = "#FFB02E", "#FF6A1F"
        floor = (255, 176, 46)
    else:
        inner_top, inner_bot, rim_c = "#26040F", "#7A1022", "#FF3355"
        glow_a, glow_b = "#FF2A4A", "#9A5BFF"
        floor = (255, 50, 80)
    outer = mask_poly(w, h, [(3, 5), (49, 5), (51.5, 8), (51.5, 28), (48.5, 31.5), (3.5, 31.5), (0.5, 28), (0.5, 8)])
    hole = mask_poly(w, h, [(4.2, 7.4), (47.8, 7.4), (49.1, 9), (49.1, 27), (47.5, 29.2), (4.5, 29.2), (2.9, 27), (2.9, 9)])
    spikes = mask_poly(w, h, [(0.5, 8), (0.8, 2.2), (6, 6)])
    for pts in ([(51.5, 8), (51.2, 2.2), (46, 6)], [(0.5, 28), (0.2, 33), (6, 30)], [(51.5, 28), (51.8, 33), (46, 30)],
                [(20, 5.5), (26, -0.5), (32, 5.5)]):
        spikes = ImageChops.lighter(spikes, mask_poly(w, h, pts))
    shape = ImageChops.lighter(outer, spikes)
    # glow luar: warna atas-kiri vs bawah-kanan
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ga = tint_glow(shape, glow_a, 1.5, 2.0)
    gb = tint_glow(shape, glow_b, 1.5, 2.0)
    diag = grad(w, h, [(0, (255, 255, 255, 255)), (0.5, (255, 255, 255, 255)), (1, (0, 0, 0, 0))], "h").getchannel("A")
    mix = Image.composite(ga, gb, diag)
    ma = ImageChops.multiply(mix.getchannel("A"), ImageChops.invert(hole))
    mix.putalpha(ma)
    img.alpha_composite(mix)
    body = ImageChops.subtract(shape, hole)
    shape_with_outline(img, body, CARD_SILVER, 0.4)
    # isi kartu
    ihole = erode(hole, 0.15)
    paste_fill(img, ihole, grad(w, h, [(0.2, inner_top), (1, inner_bot)]))
    # cahaya tepi dalam
    edge = ImageChops.subtract(hole, erode(hole, 1.6))
    eg = blur(edge, 0.8).point(lambda v: int(v * 0.55))
    lay = Image.new("RGBA", (w, h), floor + (255,))
    lay.putalpha(ImageChops.multiply(eg, hole))
    img.alpha_composite(lay)
    # kilau bawah
    rad = Image.new("L", (w, h), 0)
    ImageDraw.Draw(rad).ellipse([8 * K, 22 * K, 44 * K, 40 * K], fill=120)
    rad = blur(rad, 3).point(lambda v: v)
    lay2 = Image.new("RGBA", (w, h), floor + (255,))
    lay2.putalpha(ImageChops.multiply(rad, hole))
    img.alpha_composite(lay2)
    # garis pemisah + berlian kecil, garis atas baris bawah
    deco = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(deco)
    lw = max(1, int(0.2 * K))
    d.line([(7 * K, 14.9 * K), (23.6 * K, 14.9 * K)], fill=floor + (110,), width=lw)
    d.line([(28.4 * K, 14.9 * K), (45 * K, 14.9 * K)], fill=floor + (110,), width=lw)
    d.polygon([(26 * K, 13.7 * K), (27.2 * K, 14.9 * K), (26 * K, 16.1 * K), (24.8 * K, 14.9 * K)], fill=floor + (230,))
    d.line([(5 * K, 23.2 * K), (47 * K, 23.2 * K)], fill=floor + (90,), width=lw)
    img.alpha_composite(deco)
    crystal(img, 26, 4.2, 3.4, 3.6)
    return img.resize((CARD_W * S, hg * S), Image.LANCZOS)

def pad_bottom(img, cell_h_gui):
    """Taruh img di atas cell setinggi cell_h_gui, sisanya bening (ascent glyph tidak boleh > height)."""
    out = Image.new("RGBA", (img.width, int(cell_h_gui * S)), (0, 0, 0, 0))
    out.paste(img, (0, 0))
    return out

def mark_last_col(img):
    """Pixel alpha=1 di kolom terakhir: bikin lebar glyph = lebar sel (advance pasti)."""
    px = img.load()
    x = img.width - 1
    if all(px[x, y][3] == 0 for y in range(img.height)):
        px[x, img.height - 1] = (0, 0, 0, 1)
    return img
