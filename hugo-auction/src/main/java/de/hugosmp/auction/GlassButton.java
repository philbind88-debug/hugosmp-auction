package de.hugosmp.auction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Button im Liquid-Glass-Stil: durchscheinend, runde Ecken, Glanz, Hover-Aufhellung. */
public class GlassButton extends ButtonWidget {

    public enum Style { NORMAL, ACCENT, DANGER }

    private final Style style;
    private boolean selected;

    public GlassButton(int x, int y, int width, int height, Text message, PressAction onPress, Style style) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.style = style;
    }

    public GlassButton(int x, int y, int width, int height, Text message, PressAction onPress) {
        this(x, y, width, height, message, onPress, Style.NORMAL);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        int radius = Math.min(h / 2, 8);

        boolean hover = active && isHovered();
        Style s = selected ? Style.ACCENT : style;

        int top;
        int bottom;
        float edge;
        int spec;
        if (!active) {
            top = 0x16FFFFFF;
            bottom = 0x0AFFFFFF;
            edge = 0.35f;
            spec = 0x14;
        } else {
            switch (s) {
                case ACCENT -> {
                    top = hover ? 0xB02F95FF : 0x980A84FF;
                    bottom = hover ? 0x8A1C74E6 : 0x700A63D6;
                }
                case DANGER -> {
                    top = hover ? 0xA8FF5A50 : 0x8CFF453A;
                    bottom = hover ? 0x80E03C34 : 0x66C92E28;
                }
                default -> {
                    top = hover ? 0x5CFFFFFF : 0x3CFFFFFF;
                    bottom = hover ? 0x34FFFFFF : 0x1AFFFFFF;
                }
            }
            edge = hover ? 1.0f : 0.8f;
            spec = hover ? 0x58 : 0x40;
        }

        GlassRender.glass(ctx, x, y, w, h, radius, top, bottom, edge, spec);

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int color = active ? GlassRender.WHITE : GlassRender.TEXT_DIM;
        ctx.drawCenteredTextWithShadow(tr, getMessage(), x + w / 2, y + (h - 8) / 2, color);
    }
}
