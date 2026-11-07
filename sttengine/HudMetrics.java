package id.stt.engine.hud;

/** DIHASILKAN oleh gen/build.py bersama tekstur sttresourcepack. Jangan diedit tangan: ukuran harus sama dengan pack. */
final class HudMetrics {

    private HudMetrics() {
    }

    static final int BAR_W = 180;
    static final int CARD_W = 56;
    static final int FILL_X0 = 26;
    static final int FILL_UNIT = 2;
    static final int FILL_MAX = 69;
    static final int BAR_ADV = 181;
    static final int FILL_ADV = 3;
    static final int CARD_ADV = 57;
    static final int GEM_ADV = 5;
    static final int CLOCK_ADV = 6;
    /** Lebar bersih (advance - 1, tiap glyph diikuti geser -1) glyph ASCII 33..126 pada font stt:hud_name. Spasi = 3. */
    static final int[] NAME = {2, 3, 6, 4, 6, 5, 2, 3, 3, 4, 3, 2, 4, 2, 5, 4, 3, 3, 4, 4, 4, 4, 3, 4, 4, 2, 2, 4, 3, 4, 3, 6, 5, 5, 5, 6, 5, 4, 6, 6, 3, 4, 5, 4, 7, 6, 6, 5, 6, 5, 4, 5, 6, 6, 7, 5, 5, 4, 3, 5, 3, 6, 5, 3, 4, 4, 4, 4, 4, 4, 5, 5, 3, 2, 5, 3, 6, 5, 4, 4, 4, 3, 4, 3, 5, 4, 6, 4, 4, 4, 3, 2, 3, 3};
    static final int NAME_SPACE = 3;
    /** Lebar bersih glyph "0123456789.s" pada font stt:hud_timer. */
    static final int[] TIMER = {5, 3, 4, 5, 5, 4, 4, 4, 5, 5, 2, 4};
    /** Lebar bersih glyph "0123456789.s" pada font stt:hud_cost. */
    static final int[] COST = {3, 2, 3, 3, 3, 3, 3, 3, 3, 3, 1, 3};
    /** Lebar bersih glyph "0123456789/" pada font stt:hud_num. Spasi = 4. */
    static final int[] NUM = {5, 3, 4, 5, 5, 5, 5, 4, 5, 5, 6};
    static final int NUM_SPACE = 4;
}
