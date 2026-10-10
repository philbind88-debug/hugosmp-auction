package de.hugosmp.auction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.Optional;

/** Kleines Glas-Overlay oben in der Mitte: Item, Restzeit, aktueller Spitzenreiter. */
public final class AuctionHud {

    private AuctionHud() {}

    public static void render(DrawContext ctx) {
        AuctionManager mgr = AuctionManager.INSTANCE;
        boolean running = mgr.state() == AuctionManager.State.RUNNING;
        if (!running && !mgr.showResultInHud()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) {
            return;
        }
        TextRenderer tr = mc.textRenderer;

        String title = running
                ? "Auktion  " + AuctionManager.formatClock(mgr.remainingMillis())
                : "Auktion beendet";
        Optional<Map.Entry<String, Long>> leader = mgr.leader();
        String line2 = leader
                .map(e -> (running ? "Führend: " : "Gewinner: ") + e.getKey() + " – " + AuctionManager.formatMoney(e.getValue()))
                .orElse("Noch keine Gebote");

        int w = Math.max(150, Math.max(tr.getWidth(title), tr.getWidth(line2)) + 48);
        int h = 32;
        int x = (ctx.getScaledWindowWidth() - w) / 2;
        int y = 6;

        GlassRender.panel(ctx, x, y, w, h, 10);

        // Item auf kleiner Glas-Kachel
        GlassRender.glass(ctx, x + 5, y + 5, 22, 22, 7, 0x30FFFFFF, 0x10FFFFFF, 0.7f, 0x24);
        ctx.drawItem(mgr.auctionItem(), x + 8, y + 8);

        int color = running && mgr.remainingMillis() <= 10_000 ? GlassRender.RED : GlassRender.GOLD;
        ctx.drawTextWithShadow(tr, Text.literal(title), x + 34, y + 7, color);
        ctx.drawTextWithShadow(tr, Text.literal(line2), x + 34, y + 18, GlassRender.WHITE);
    }
}
