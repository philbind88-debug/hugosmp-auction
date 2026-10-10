package de.hugosmp.auction;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Erkennt die Zahlungs-Nachricht des Servers. Bewusst streng:
 * die komplette Nachricht muss exakt dem Muster entsprechen (^...$),
 * davor und dahinter darf kein einziges Zeichen stehen.
 *
 * Erwartetes Format:
 *   [HugoSMP] Du hast $30.000.000[BEISPIEL] von [SPIELERNAME] erhalten.
 *
 * Der Block direkt hinter dem Betrag ("[BEISPIEL]") und die eckigen Klammern
 * um den Spielernamen sind optional, damit die Mod mit der echten Nachricht
 * funktioniert, egal ob dort Klammern stehen oder nicht.
 */
public final class PaymentParser {

    public record Payment(String player, long amount) {}

    private static final Pattern PATTERN = Pattern.compile(
            "^\\[HugoSMP\\] Du hast \\$([0-9][0-9.,]*)(?:\\[[^\\]]{1,32}\\])? von \\[?([A-Za-z0-9_]{1,16})\\]? erhalten\\.$");

    private PaymentParser() {}

    public static Optional<Payment> parse(String message) {
        if (message == null || message.length() > 200) {
            return Optional.empty();
        }
        Matcher m = PATTERN.matcher(message);
        if (!m.matches()) {
            return Optional.empty();
        }
        String digits = m.group(1).replaceAll("[.,]", "");
        if (digits.isEmpty() || digits.length() > 15) {
            return Optional.empty();
        }
        try {
            long amount = Long.parseLong(digits);
            if (amount <= 0) {
                return Optional.empty();
            }
            return Optional.of(new Payment(m.group(2), amount));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
