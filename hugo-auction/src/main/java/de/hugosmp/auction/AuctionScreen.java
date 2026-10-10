package de.hugosmp.auction;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Das Auktionsmenü im Liquid-Glass-Look: Item scannen, Dauer wählen, starten – danach Live-Rangliste. */
public class AuctionScreen extends Screen {

    private static final int[] PRESETS = {30, 60, 120, 300, 600};
    private static final int PANEL_W = 280;
    private static final int PANEL_H = 232;

    /** Bleibt über Öffnen/Schließen erhalten. */
    private static int selectedSeconds = 60;

    private final List<GlassButton> presetButtons = new ArrayList<>();
    private TextFieldWidget customField;
    private AuctionManager.State shownState;
    private String hint = "";

    public AuctionScreen() {
        super(Text.literal("Auktion"));
    }

    private int left() { return (width - PANEL_W) / 2; }

    private int top() { return (height - PANEL_H) / 2; }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ----------------------------------------------------------------- Aufbau

    @Override
    protected void init() {
        AuctionManager mgr = AuctionManager.INSTANCE;
        shownState = mgr.state();
        presetButtons.clear();
        customField = null;

        int l = left();
        int t = top();

        if (shownState == AuctionManager.State.IDLE) {
            addDrawableChild(new GlassButton(l + 84, t + 46, 184, 20, Text.literal("Item aus Hand scannen"),
                    b -> scanHeldItem()));

            int bx = l + 12;
            for (int seconds : PRESETS) {
                GlassButton btn = new GlassButton(bx, t + 108, 48, 20,
                        Text.literal(AuctionManager.formatDuration(seconds)), b -> {
                    selectedSeconds = seconds;
                    clearAndInit();
                });
                presetButtons.add(btn);
                addDrawableChild(btn);
                bx += 52;
            }

            // Eigene Dauer: Glas-Pille wird in renderBackground gezeichnet, das Feld selbst ist rahmenlos.
            customField = new TextFieldWidget(textRenderer, l + 158, t + 140, 48, 12, Text.literal("Sekunden"));
            customField.setDrawsBackground(false);
            customField.setTextPredicate(s -> s.matches("\\d{0,5}"));
            customField.setText(String.valueOf(selectedSeconds));
            customField.setChangedListener(s -> {
                if (!s.isEmpty()) {
                    selectedSeconds = Math.max(5, Math.min(86_400, Integer.parseInt(s)));
                    refreshPresetSelection();
                }
            });
            addDrawableChild(customField);
            refreshPresetSelection();

            GlassButton start = new GlassButton(l + 12, t + 164, 256, 24, Text.literal("Auktion starten"), b -> {
                if (AuctionManager.INSTANCE.start(selectedSeconds)) {
                    hint = "";
                    clearAndInit();
                }
            }, GlassButton.Style.ACCENT);
            start.active = !mgr.pendingItem().isEmpty();
            addDrawableChild(start);

            addDrawableChild(new GlassButton(l + 12, t + 196, 256, 20, Text.literal("Schließen"), b -> close()));
        } else if (shownState == AuctionManager.State.RUNNING) {
            addDrawableChild(new GlassButton(l + 12, t + 196, 124, 20, Text.literal("Auktion abbrechen"), b -> {
                AuctionManager.INSTANCE.cancel();
                clearAndInit();
            }, GlassButton.Style.DANGER));
            addDrawableChild(new GlassButton(l + 144, t + 196, 124, 20, Text.literal("Menü schließen"), b -> close()));
        } else {
            addDrawableChild(new GlassButton(l + 12, t + 196, 124, 20, Text.literal("Neue Auktion"), b -> {
                AuctionManager.INSTANCE.reset();
                clearAndInit();
            }, GlassButton.Style.ACCENT));
            addDrawableChild(new GlassButton(l + 144, t + 196, 124, 20, Text.literal("Schließen"), b -> close()));
        }
    }

    private void refreshPresetSelection() {
        for (int i = 0; i < presetButtons.size(); i++) {
            presetButtons.get(i).setSelected(PRESETS[i] == selectedSeconds);
        }
    }

    private void scanHeldItem() {
        if (client == null || client.player == null) {
            return;
        }
        ItemStack held = client.player.getMainHandStack();
        if (held.isEmpty()) {
            hint = "Du hast nichts in der Hand!";
        } else {
            AuctionManager.INSTANCE.setPendingItem(held);
            hint = "";
        }
        clearAndInit();
    }

    @Override
    public void tick() {
        if (AuctionManager.INSTANCE.state() != shownState) {
            clearAndInit();
        }
    }

    // --------------------------------------------------------------- Zeichnen

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Das Vanilla-Hintergrund-Blur sorgt dafür, dass die Welt dahinter weich durchscheint.
        super.renderBackground(ctx, mouseX, mouseY, delta);
        int l = left();
        int t = top();

        GlassRender.panel(ctx, l, t, PANEL_W, PANEL_H, 12);

        // dezente Trennlinie unter dem Titel
        ctx.fill(l + 16, t + 27, l + PANEL_W - 16, t + 28, 0x30FFFFFF);

        if (customField != null) {
            // Glas-Pille hinter dem Textfeld
            GlassRender.glass(ctx, l + 152, t + 136, 60, 20, 8, 0x30000000, 0x18000000,
                    customField.isFocused() ? 1.0f : 0.6f, 0x18);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int l = left();
        int t = top();

        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Auktion").formatted(Formatting.BOLD), l + PANEL_W / 2, t + 10, GlassRender.WHITE);

        if (shownState == AuctionManager.State.IDLE) {
            renderSetup(ctx, l, t);
        } else {
            renderLive(ctx, l, t);
        }
    }

    private void renderSetup(DrawContext ctx, int l, int t) {
        ItemStack pending = AuctionManager.INSTANCE.pendingItem();

        drawItemFrame(ctx, l + 20, t + 38, pending);
        if (pending.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("Kein Item gescannt"), l + 84, t + 74, GlassRender.TEXT_DIM);
        } else {
            ctx.drawTextWithShadow(textRenderer, pending.getName(), l + 84, t + 74, GlassRender.WHITE);
            ctx.drawTextWithShadow(textRenderer, Text.literal("Anzahl: " + pending.getCount()), l + 84, t + 86,
                    GlassRender.TEXT_SOFT);
        }

        ctx.drawTextWithShadow(textRenderer, Text.literal("Dauer"), l + 14, t + 96, GlassRender.ACCENT_TEXT);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Eigene Zeit (Sek.):"), l + 14, t + 142, GlassRender.TEXT_SOFT);

        if (!hint.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(hint), l + PANEL_W / 2, t + 219, GlassRender.RED);
        } else if (pending.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Item in die Hand nehmen und „scannen“ klicken."),
                    l + PANEL_W / 2, t + 219, GlassRender.TEXT_DIM);
        }
    }

    private void renderLive(DrawContext ctx, int l, int t) {
        AuctionManager mgr = AuctionManager.INSTANCE;
        boolean running = shownState == AuctionManager.State.RUNNING;

        drawItemFrame(ctx, l + 20, t + 36, mgr.auctionItem());
        ctx.drawTextWithShadow(textRenderer, mgr.auctionItem().getName(), l + 84, t + 42, GlassRender.WHITE);

        String clock = running ? AuctionManager.formatClock(mgr.remainingMillis()) : "Beendet";
        int clockColor = running && mgr.remainingMillis() <= 10_000 ? GlassRender.RED : GlassRender.GOLD;
        ctx.drawTextWithShadow(textRenderer, Text.literal(clock).formatted(Formatting.BOLD), l + 84, t + 58, clockColor);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Rangliste"), l + 14, t + 94, GlassRender.ACCENT_TEXT);

        List<Map.Entry<String, Long>> ranking = mgr.ranking();
        if (ranking.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("Noch keine Zahlungen …"), l + 16, t + 112,
                    GlassRender.TEXT_DIM);
        }
        int y = t + 106;
        for (int i = 0; i < Math.min(6, ranking.size()); i++) {
            Map.Entry<String, Long> e = ranking.get(i);
            boolean first = i == 0;
            // Glas-Pille pro Zeile, Spitzenreiter leicht golden
            GlassRender.glass(ctx, l + 12, y, PANEL_W - 24, 13, 6,
                    first ? 0x50FFD60A : 0x24FFFFFF, first ? 0x2CFFD60A : 0x10FFFFFF,
                    first ? 0.9f : 0.5f, first ? 0x28 : 0x14);
            int color = first ? GlassRender.GOLD : GlassRender.WHITE;
            ctx.drawTextWithShadow(textRenderer, Text.literal((i + 1) + ". " + e.getKey()), l + 20, y + 3, color);
            String money = AuctionManager.formatMoney(e.getValue());
            ctx.drawTextWithShadow(textRenderer, Text.literal(money),
                    l + PANEL_W - 20 - textRenderer.getWidth(money), y + 3, color);
            y += 14;
        }
    }

    /** 48x48-Glasrahmen, Item wird auf 2x vergrößert. */
    private void drawItemFrame(DrawContext ctx, int x, int y, ItemStack stack) {
        GlassRender.glass(ctx, x, y, 48, 48, 10, 0x38FFFFFF, 0x14FFFFFF, 0.9f, 0x30);
        if (!stack.isEmpty()) {
            var m = ctx.getMatrices();
            m.pushMatrix();
            m.translate(x + 8, y + 8);
            m.scale(2.0f, 2.0f);
            ctx.drawItem(stack, 0, 0);
            m.popMatrix();
        }
    }
}
