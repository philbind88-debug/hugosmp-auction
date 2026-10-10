package de.hugosmp.auction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Zentraler Zustand der laufenden Auktion (läuft komplett auf dem Client-Thread). */
public final class AuctionManager {

    public static final AuctionManager INSTANCE = new AuctionManager();

    public enum State { IDLE, RUNNING, FINISHED }

    /** Wie lange das Ergebnis nach Ende noch im HUD stehen bleibt. */
    public static final long RESULT_HUD_MS = 30_000L;

    private State state = State.IDLE;
    private ItemStack pendingItem = ItemStack.EMPTY;
    private ItemStack auctionItem = ItemStack.EMPTY;
    private long endAt;
    private long finishedAt;
    private final Map<String, Long> totals = new LinkedHashMap<>();

    private AuctionManager() {}

    // ---------------------------------------------------------------- Zugriff

    public State state() { return state; }

    public ItemStack pendingItem() { return pendingItem; }

    public void setPendingItem(ItemStack stack) {
        pendingItem = stack.copy();
    }

    public ItemStack auctionItem() { return auctionItem; }

    public long remainingMillis() {
        return state == State.RUNNING ? Math.max(0, endAt - System.currentTimeMillis()) : 0;
    }

    public boolean showResultInHud() {
        return state == State.FINISHED && System.currentTimeMillis() - finishedAt < RESULT_HUD_MS;
    }

    /** Rangliste: höchste Gesamtsumme zuerst. */
    public List<Map.Entry<String, Long>> ranking() {
        List<Map.Entry<String, Long>> list = new ArrayList<>(totals.entrySet());
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return list;
    }

    public Optional<Map.Entry<String, Long>> leader() {
        List<Map.Entry<String, Long>> r = ranking();
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    // ------------------------------------------------------------- Steuerung

    public boolean start(int seconds) {
        if (state == State.RUNNING || pendingItem.isEmpty()) {
            return false;
        }
        auctionItem = pendingItem.copy();
        totals.clear();
        endAt = System.currentTimeMillis() + seconds * 1000L;
        state = State.RUNNING;
        say(Text.literal("Auktion gestartet: ").formatted(Formatting.GOLD)
                .append(auctionItem.getName()).append(Text.literal(" (" + formatDuration(seconds) + ")")));
        return true;
    }

    public void cancel() {
        if (state == State.RUNNING) {
            state = State.IDLE;
            totals.clear();
            say(Text.literal("Auktion abgebrochen.").formatted(Formatting.RED));
        }
    }

    public void reset() {
        if (state != State.RUNNING) {
            state = State.IDLE;
            totals.clear();
        }
    }

    /** Wird jeden Client-Tick aufgerufen. */
    public void tick() {
        if (state == State.RUNNING && System.currentTimeMillis() >= endAt) {
            finish();
        }
    }

    private void finish() {
        state = State.FINISHED;
        finishedAt = System.currentTimeMillis();
        Optional<Map.Entry<String, Long>> winner = leader();
        if (winner.isPresent()) {
            say(Text.literal("Auktion beendet! Gewinner: ").formatted(Formatting.GOLD)
                    .append(Text.literal(winner.get().getKey()).formatted(Formatting.GREEN, Formatting.BOLD))
                    .append(Text.literal(" mit " + formatMoney(winner.get().getValue())).formatted(Formatting.YELLOW)));
        } else {
            say(Text.literal("Auktion beendet – es gab keine Zahlungen.").formatted(Formatting.GRAY));
        }
        playSound(net.minecraft.sound.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f);
    }

    // --------------------------------------------------------------- Zahlung

    /**
     * Wird für jede Systemnachricht des Servers aufgerufen. Nur die exakte
     * Zahlungs-Nachricht (siehe {@link PaymentParser}) zählt.
     */
    public void onServerMessage(String plainText) {
        if (state != State.RUNNING) {
            return;
        }
        PaymentParser.parse(plainText).ifPresent(this::onPayment);
    }

    private void onPayment(PaymentParser.Payment p) {
        if (System.currentTimeMillis() >= endAt) {
            return;
        }
        long total = totals.merge(p.player(), p.amount(), Long::sum);
        say(Text.literal("+ " + formatMoney(p.amount()) + " von ").formatted(Formatting.GREEN)
                .append(Text.literal(p.player()).formatted(Formatting.WHITE))
                .append(Text.literal(" (gesamt " + formatMoney(total) + ")").formatted(Formatting.GRAY)));
        playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f);
    }

    // ---------------------------------------------------------------- Helfer

    private static void say(Text text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("[Auktion] ").formatted(Formatting.GOLD).append(text), false);
        }
    }

    private static void playSound(net.minecraft.sound.SoundEvent event, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
           if (mc.player != null) { mc.player.playSound(event, 1.0f, pitch); } }

    public static String formatMoney(long amount) {
        return "$" + NumberFormat.getIntegerInstance(Locale.GERMANY).format(amount);
    }

    public static String formatClock(long millis) {
        long totalSec = (millis + 999) / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
    }

    public static String formatDuration(int seconds) {
        if (seconds < 60) return seconds + " Sek.";
        if (seconds % 60 == 0) return (seconds / 60) + " Min.";
        return (seconds / 60) + " Min. " + (seconds % 60) + " Sek.";
    }
}
