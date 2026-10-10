package de.hugosmp.auction;

import net.minecraft.client.gui.DrawContext;

/**
 * Zeichenhelfer für den "Liquid Glass"-Look: runde, halbtransparente Flächen mit
 * Farbverlauf, hellem Glanz im oberen Bereich und feiner Lichtkante.
 * Alles nur mit Rechtecken pro Pixelzeile gezeichnet, daher ohne Shader/Texturen.
 */
public final class GlassRender {

    // Farben (ARGB)
    public static final int WHITE = 0xFFFFFFFF;
    public static final int TEXT_SOFT = 0xFFD6DCE8;
    public static final int TEXT_DIM = 0xFF8E97A8;
    public static final int ACCENT_TEXT = 0xFFA9CDFF;
    public static final int GOLD = 0xFFFFD60A;
    public static final int RED = 0xFFFF6B63;

    private GlassRender() {}

    // ------------------------------------------------------------ Grundlagen

    public static int lerp(int a, int b, float t) {
        int aa = (a >>> 24) & 0xFF, ar = (a >>> 16) & 0xFF, ag = (a >>> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >>> 16) & 0xFF, bg = (b >>> 8) & 0xFF, bb = b & 0xFF;
        int ra = Math.round(aa + (ba - aa) * t);
        int rr = Math.round(ar + (br - ar) * t);
        int rg = Math.round(ag + (bg - ag) * t);
        int rb = Math.round(ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    /** Wie weit Zeile i (0 = oben) wegen der Rundung von links/rechts eingerückt ist. */
    static int inset(int i, int h, int r) {
        int d;
        if (i < r) {
            d = r - i;
        } else if (i >= h - r) {
            d = i - (h - 1 - r);
        } else {
            return 0;
        }
        double dy = d - 0.5;
        double dx = Math.sqrt(Math.max(0, (double) r * r - dy * dy));
        return (int) Math.round(r - dx);
    }

    private static int clampRadius(int w, int h, int r) {
        return Math.max(0, Math.min(r, Math.min(w, h) / 2));
    }

    // --------------------------------------------------------- Einzelteile

    /** Gefülltes Rundrechteck mit vertikalem Farbverlauf. */
    public static void fill(DrawContext ctx, int x, int y, int w, int h, int r, int top, int bottom) {
        r = clampRadius(w, h, r);
        for (int i = 0; i < h; i++) {
            int in = inset(i, h, r);
            float t = h <= 1 ? 0f : (float) i / (h - 1);
            ctx.fill(x + in, y + i, x + w - in, y + i + 1, lerp(top, bottom, t));
        }
    }

    /** 1px-Kante mit vertikalem Farbverlauf (oben meist heller als unten). */
    public static void outline(DrawContext ctx, int x, int y, int w, int h, int r, int top, int bottom) {
        r = clampRadius(w, h, r);
        for (int i = 0; i < h; i++) {
            int in = inset(i, h, r);
            float t = h <= 1 ? 0f : (float) i / (h - 1);
            int color = lerp(top, bottom, t);
            if (i == 0 || i == h - 1) {
                ctx.fill(x + in, y + i, x + w - in, y + i + 1, color);
                continue;
            }
            int span;
            if (i < h / 2) {
                span = Math.max(1, inset(i - 1, h, r) - in + 1);
            } else {
                span = Math.max(1, inset(i + 1, h, r) - in + 1);
            }
            ctx.fill(x + in, y + i, x + in + span, y + i + 1, color);
            ctx.fill(x + w - in - span, y + i, x + w - in, y + i + 1, color);
        }
    }

    /** Heller Glanz im oberen Bereich, wie die Spiegelung auf Glas. */
    public static void specular(DrawContext ctx, int x, int y, int w, int h, int r, int topAlpha) {
        r = clampRadius(w, h, r);
        int band = Math.max(3, (int) (h * 0.46f));
        int pad = 2;
        for (int i = pad; i < band; i++) {
            int in = inset(i, h, r) + pad;
            float t = (float) (i - pad) / Math.max(1, band - pad - 1);
            int alpha = Math.round(topAlpha * (1f - t) * (1f - t * 0.5f));
            if (alpha <= 0 || x + in >= x + w - in) {
                continue;
            }
            ctx.fill(x + in, y + i, x + w - in, y + i + 1, (alpha << 24) | 0xFFFFFF);
        }
        // zarter Lichtsaum am unteren Rand (Brechung)
        if (h > 14) {
            int i = h - 3;
            int in = inset(i, h, r) + pad;
            ctx.fill(x + in, y + i, x + w - in, y + i + 1, 0x16FFFFFF);
        }
    }

    // ------------------------------------------------------------ Komplett

    /**
     * Komplettes Glaselement: getönte Fläche, Glanz und Lichtkante.
     *
     * @param tintTop    Farbe oben (ARGB, meist niedrige Alpha)
     * @param tintBottom Farbe unten
     * @param edgeBoost  0..1, wie hell die Kante ist (Hover = höher)
     */
    public static void glass(DrawContext ctx, int x, int y, int w, int h, int r,
                             int tintTop, int tintBottom, float edgeBoost, int specularAlpha) {
        fill(ctx, x, y, w, h, r, tintTop, tintBottom);
        specular(ctx, x, y, w, h, r, specularAlpha);
        int edgeTop = ((int) (0xB0 * edgeBoost) << 24) | 0xFFFFFF;
        int edgeBottom = ((int) (0x48 * edgeBoost) << 24) | 0xFFFFFF;
        outline(ctx, x, y, w, h, r, edgeTop, edgeBottom);
    }

    /** Standard-Glasfläche für Panels. */
    public static void panel(DrawContext ctx, int x, int y, int w, int h, int r) {
        // weicher Schlagschatten
        fill(ctx, x - 2, y + 2, w + 4, h + 4, r + 2, 0x16000000, 0x2A000000);
        glass(ctx, x, y, w, h, r, 0x7036466A, 0x70162038, 0.9f, 0x36);
    }
}
