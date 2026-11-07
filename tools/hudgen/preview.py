"""Simulasi render client Minecraft 1.20.1 untuk teks actionbar berfont bitmap + space (cukup untuk cek susunan)."""
import json, sys, os
from PIL import Image, ImageDraw
S = 3
def load_fonts(pack):
    fonts = {}
    base = os.path.join(pack, "assets/stt")
    for fn in os.listdir(os.path.join(base, "font")):
        data = json.load(open(os.path.join(base, "font", fn)))
        glyphs = {}
        for p in data["providers"]:
            if p["type"] == "space":
                for ch, a in p["advances"].items():
                    glyphs[ch] = ("space", a)
            else:
                img = Image.open(os.path.join(base, "textures", p["file"].split(":")[1])).convert("RGBA")
                rows = p["chars"]; cw = img.width // len(rows[0]); chh = img.height // len(rows)
                assert p["ascent"] <= p["height"]
                scale = p["height"] / chh
                for r, row in enumerate(rows):
                    for c, ch in enumerate(row):
                        if ch == "\0": continue
                        cell = img.crop((c * cw, r * chh, (c + 1) * cw, (r + 1) * chh))
                        a = cell.getchannel("A"); aw = 0
                        for x in range(cw - 1, -1, -1):
                            if a.crop((x, 0, x + 1, chh)).getbbox(): aw = x + 1; break
                        glyphs[ch] = ("img", int(0.5 + aw * scale) + 1, cell, scale, p["ascent"], p["height"])
        fonts[os.path.splitext(fn)[0]] = glyphs
    return fonts

def render(runs, fonts, canvas, cx, baseline, gui=3):
    # total lebar
    total = 0
    for r in runs:
        for ch in r["text"]:
            g = fonts[r["font"]][ch]; total += g[1] if g[0] == "img" else g[1]
    x0 = cx - total / 2
    for shadow in (True, False):
        x = x0
        for r in runs:
            col = r["color"]
            rgb = (255, 255, 255) if col == -1 else ((col >> 16) & 255, (col >> 8) & 255, col & 255)
            for ch in r["text"]:
                g = fonts[r["font"]][ch]
                if g[0] == "space":
                    x += g[1]; continue
                _, adv, cell, scale, asc, hgt = g
                w = round(cell.width * scale * gui); h = round(cell.height * scale * gui)
                im = cell.resize((w, h), Image.NEAREST)
                tint = Image.new("RGBA", im.size, rgb + (255,))
                px = im.copy()
                # modulasi warna
                r_, g_, b_, a_ = px.split()
                mul = lambda c, m: c.point(lambda v: v * m // 255)
                px = Image.merge("RGBA", (mul(r_, rgb[0]), mul(g_, rgb[1]), mul(b_, rgb[2]), a_))
                if shadow:
                    px = Image.merge("RGBA", (px.getchannel("R").point(lambda v: v // 4), px.getchannel("G").point(lambda v: v // 4), px.getchannel("B").point(lambda v: v // 4), px.getchannel("A")))
                off = gui if shadow else 0
                top = baseline - asc * gui
                canvas.alpha_composite(px, (int(round(x * gui + off)), int(top + off)))
                x += adv
    return total

if __name__ == "__main__":
    pack, runsfile, out = sys.argv[1:4]
    runs = json.load(open(runsfile))
    fonts = load_fonts(pack)
    W, H = 640 * 3, 150 * 3
    bgc = Image.new("RGBA", (W, H), (60, 90, 55, 255))
    d = ImageDraw.Draw(bgc)
    # hotbar + hati/lapar palsu
    gx = W // 2
    d.rectangle([gx - 91 * 3, H - 22 * 3, gx + 91 * 3, H], fill=(40, 40, 40, 255), outline=(120, 120, 120, 255))
    d.rectangle([gx - 91 * 3, H - 49 * 3, gx - 11 * 3, H - 39 * 3], fill=(150, 30, 30, 255))
    d.rectangle([gx + 11 * 3, H - 49 * 3, gx + 91 * 3, H - 39 * 3], fill=(150, 100, 30, 255))
    baseline = H - 65 * 3 + 14 * 3   # baseline actionbar kira2 (h-65) + tambahan supaya bar muat di crop
    baseline = 110 * 3
    total = render(runs, fonts, bgc, gx / 3, baseline)
    bgc.save(out)
    print("width", total)
