package id.stt.engine.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Susunan HUD (bar mana + kartu skill) sebagai daftar potongan teks berfont stt:hud_*. Tanpa Bukkit, jadi bisa dites sendiri.
 * Semua posisi dihitung dalam gui px; geser kursor memakai glyph spasi (+/-2^k) yang ada di tiap font.
 * Urutan gambar: kartu, nama, timer, ikon + angka cost, angka mana, lalu bar (bg, isi, frame). Yang belakangan menimpa yang di depan.
 * Sampai 5 skill: satu baris kartu di bawah bar. 6 sampai 8 skill: dua baris, kartu dibagi rata (baris atas dapat sisa ganjil).
 * Di mode dua baris angka mana, bar, dan baris atas memakai font berakhiran _u (glyph sama, posisi lebih tinggi), baris bawah tetap di posisi satu baris.
 */
final class HudLayout {

    /** Satu potongan teks. color -1 = putih. */
    record Run(String font, String text, int color) {
    }

    /** Data satu kartu skill. cooldown = lama cooldown skill (detik), remaining = sisa cooldown (detik, 0 = siap). */
    record Card(String name, double remaining, double cost, double cooldown, boolean enough) {
    }

    /**
     * Kartu per halaman (dua baris). Satu baris maksimal 5 kartu = 288 gui px, masih muat di layar tersempit yang dipakai Minecraft (320 gui px).
     * Kalau skill lebih dari 8, kartu dibagi per halaman yang berganti otomatis (lihat Hud), jadi lebar HUD tidak pernah melebihi 288.
     */
    static final int MAX_CARDS = 8;
    static final int SINGLE_ROW_CARDS = 5;
    private static final double LOW_RATIO = 0.2;

    static int pageCount(int cards) {
        return Math.max(1, (cards + MAX_CARDS - 1) / MAX_CARDS);
    }
    private static final int CARD_GAP = 2;
    private static final int NAME_MAX = 50;
    private static final int COLOR_NAME = 0xE9EEF8;
    private static final int COLOR_TIMER = 0xFF4545;
    private static final int COLOR_NUM = 0xDCE8FF;
    private static final int COLOR_LOW = 0xFF6B6B;
    private static final int WHITE = -1;
    private static final char POS0 = '\uE300';
    private static final char NEG0 = '\uE320';
    private static final char BG = '\uE000';
    private static final char FILL = '\uE001';
    private static final char FILL_LOW = '\uE003';
    private static final char FRAME = '\uE002';
    private static final char READY = '\uE010';
    private static final char COOL = '\uE011';
    private static final char NO_MANA = '\uE012';
    private static final char GEM = '\uE020';
    private static final char CLOCK = '\uE021';

    private final List<Run> runs = new ArrayList<>();
    private StringBuilder current;
    private String font;
    private int color;
    private int x;

    private HudLayout() {
    }

    static List<Run> build(double mana, double max, List<Card> all) {
        return build(mana, max, all, 0);
    }

    /** page = nomor halaman (0, 1, ...), dibulatkan ke jumlah halaman yang ada. */
    static List<Run> build(double mana, double max, List<Card> all, int page) {
        HudLayout out = new HudLayout();
        int pages = pageCount(all.size());
        int current = Math.floorMod(page, pages);
        List<Card> cards = all.subList(Math.min(all.size(), current * MAX_CARDS), Math.min(all.size(), (current + 1) * MAX_CARDS));
        int n = cards.size();
        boolean two = n > SINGLE_ROW_CARDS;
        int top = two ? (n + 1) / 2 : n;
        int width = Math.max(rowWidth(top), HudMetrics.BAR_W);
        int barX = (width - HudMetrics.BAR_W) / 2;

        out.row(cards.subList(0, top), width, two);
        out.row(cards.subList(top, n), width, false);
        // angka mana di ATAS bar, rata tengah terhadap bar (glyph angka lebih tinggi dari bar, lihat build.py LABEL_PAD)
        String label = (int) Math.floor(mana) + " / " + (int) Math.floor(max);
        int labelW = numWidth(label);
        out.moveTo(face("hud_num", two), COLOR_NUM, barX + (HudMetrics.BAR_W - labelW) / 2);
        out.putText(label, labelW);
        if (pages > 1) {
            // penanda halaman kartu ("2/3") di kanan atas bar, jauh dari angka mana
            String pageText = (current + 1) + "/" + pages;
            int pageW = numWidth(pageText);
            out.moveTo(face("hud_num", two), COLOR_NUM, barX + HudMetrics.BAR_W - pageW - 3);
            out.putText(pageText, pageW);
        }

        // bar: latar, isi, frame. Digambar paling akhir: kalau client salah menghitung lebar glyph bar, kartu dan teks tidak ikut geser.
        double ratio = max <= 0 ? 0 : Math.max(0, Math.min(1, mana / max));
        int tiles = (int) Math.round(ratio * HudMetrics.FILL_MAX);
        if (tiles == 0 && mana >= 0.5) {
            tiles = 1;
        }
        char fill = ratio <= LOW_RATIO ? FILL_LOW : FILL;
        out.putBar(BG, barX, two);
        int fillX = barX + HudMetrics.FILL_X0;
        for (int i = 0; i < tiles; i++) {
            out.moveTo(face("hud_bar", two), WHITE, fillX + i * HudMetrics.FILL_UNIT);
            out.put(String.valueOf(fill), HudMetrics.FILL_ADV);
        }
        out.putBar(FRAME, barX, two);

        // lebar total teks = lebar konten, supaya actionbar (rata tengah) pas di tengah layar
        out.moveTo(out.font == null ? "hud_bar" : out.font, out.color, width);
        out.flush();
        return out.runs;
    }

    private void moveTo(String f, int c, int target) {
        if (!f.equals(font) || c != color) {
            flush();
            font = f;
            color = c;
            current = new StringBuilder();
        }
        int delta = target - x;
        int bits = Math.abs(delta);
        for (int k = 10; k >= 0; k--) {
            if ((bits & (1 << k)) != 0) {
                current.append((char) ((delta > 0 ? POS0 : NEG0) + k));
            }
        }
        x = target;
    }

    private static String face(String base, boolean up) {
        return up ? base + "_u" : base;
    }

    private static int rowWidth(int cards) {
        return cards == 0 ? 0 : cards * (HudMetrics.CARD_W + CARD_GAP) - CARD_GAP;
    }

    private void row(List<Card> cards, int width, boolean up) {
        int n = cards.size();
        int step = HudMetrics.CARD_W + CARD_GAP;
        int cardsX = (width - rowWidth(n)) / 2;
        for (int i = 0; i < n; i++) {
            Card card = cards.get(i);
            moveTo(face("hud_card", up), WHITE, cardsX + i * step);
            put(String.valueOf(card.remaining() > 0 ? COOL : card.enough() ? READY : NO_MANA), HudMetrics.CARD_ADV);
        }
        for (int i = 0; i < n; i++) {
            String name = fit(clean(cards.get(i).name()));
            int w = nameWidth(name);
            moveTo(face("hud_name", up), COLOR_NAME, cardsX + i * step + (HudMetrics.CARD_W - w) / 2);
            putText(name, w);
        }
        for (int i = 0; i < n; i++) {
            Card card = cards.get(i);
            if (card.remaining() > 0) {
                String t = timerText(card.remaining());
                int w = digitsWidth(t, HudMetrics.TIMER);
                moveTo(face("hud_timer", up), COLOR_TIMER, cardsX + i * step + (HudMetrics.CARD_W - w) / 2);
                putText(t, w);
            }
        }
        for (int i = 0; i < n; i++) {
            Card card = cards.get(i);
            int cx = cardsX + i * step;
            String cost = trim(card.cost());
            String cd = trim(card.cooldown()) + "s";
            int costW = HudMetrics.GEM_ADV + 2 + digitsWidth(cost, HudMetrics.COST);
            int cdW = HudMetrics.CLOCK_ADV + 2 + digitsWidth(cd, HudMetrics.COST);
            int left = cx + 5;
            int right = Math.max(left + costW + 2, cx + HudMetrics.CARD_W - 5 - cdW);
            moveTo(face("hud_cost", up), WHITE, left);
            put(String.valueOf(GEM), HudMetrics.GEM_ADV);
            moveTo(face("hud_cost", up), card.enough() ? WHITE : COLOR_LOW, left + HudMetrics.GEM_ADV + 2);
            putText(cost, digitsWidth(cost, HudMetrics.COST));
            moveTo(face("hud_cost", up), WHITE, right);
            put(String.valueOf(CLOCK), HudMetrics.CLOCK_ADV);
            moveTo(face("hud_cost", up), WHITE, right + HudMetrics.CLOCK_ADV + 2);
            putText(cd, digitsWidth(cd, HudMetrics.COST));
        }
    }

    private void putBar(char glyph, int x0, boolean up) {
        moveTo(face("hud_bar", up), WHITE, x0);
        put(String.valueOf(glyph), HudMetrics.BAR_ADV);
    }

    private void put(String text, int advance) {
        current.append(text);
        x += advance;
    }

    /** Teks biasa: tiap glyph diikuti geser -1 (menghapus jarak +1 otomatis client), lebar = jumlah lebar bersih. */
    private void putText(String text, int width) {
        for (char c : text.toCharArray()) {
            current.append(c);
            if (c != ' ') {
                current.append(NEG0);
            }
        }
        x += width;
    }

    private void flush() {
        if (current != null && current.length() > 0) {
            runs.add(new Run(font, current.toString(), color));
        }
        current = null;
    }

    /** Buang kode warna (&a, §a) dan karakter di luar ASCII yang ada di font. */
    static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replaceAll("[&\u00A7][0-9a-fk-orA-FK-OR]", "");
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            sb.append(c == ' ' || (c >= 33 && c <= 126) ? c : '?');
        }
        return sb.toString().trim();
    }

    static int nameWidth(String s) {
        int w = 0;
        for (char c : s.toCharArray()) {
            w += c == ' ' ? HudMetrics.NAME_SPACE : HudMetrics.NAME[c - 33];
        }
        return w;
    }

    /** Potong nama yang kelewat lebar, ganti ujungnya dengan "..". */
    static String fit(String s) {
        if (nameWidth(s) <= NAME_MAX) {
            return s;
        }
        String t = s;
        while (!t.isEmpty() && nameWidth(t + "..") > NAME_MAX) {
            t = t.substring(0, t.length() - 1);
        }
        return t.trim() + "..";
    }

    private static int numWidth(String s) {
        int w = 0;
        for (char c : s.toCharArray()) {
            w += c == ' ' ? HudMetrics.NUM_SPACE : HudMetrics.NUM[c == '/' ? 10 : c - '0'];
        }
        return w;
    }

    /** Lebar teks yang hanya berisi "0123456789.s". */
    private static int digitsWidth(String s, int[] table) {
        int w = 0;
        for (char c : s.toCharArray()) {
            w += table[c == '.' ? 10 : c == 's' ? 11 : c - '0'];
        }
        return w;
    }

    /** Sisa cooldown, dibulatkan ke atas per 0,1 detik supaya tidak pernah tampil 0.0 saat masih terkunci. */
    static String timerText(double seconds) {
        int tenths = (int) Math.ceil(Math.min(seconds, 999.9) * 10 - 1e-9);
        return (tenths / 10) + "." + (tenths % 10);
    }

    /** 50 -> "50", 12.5 -> "12.5". */
    static String trim(double v) {
        double r = Math.round(v * 10) / 10.0;
        return r == Math.rint(r) ? String.valueOf((long) r) : String.format(Locale.ROOT, "%.1f", r);
    }
}
