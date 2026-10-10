# Hugo Auktion (Fabric-Mod, Minecraft 1.21.11)

Client-Mod für Streamer auf Hugo SMP.

## Benutzung
1. Taste **J** drücken (änderbar unter Steuerung → „Hugo Auktion“) oder `/auktion` tippen.
2. Item in die Hand nehmen und **„Item aus Hand scannen“** klicken.
3. Dauer wählen (30 Sek. bis 10 Min. oder eigene Sekundenzahl) und **„Auktion starten“**.
4. Oben mittig läuft ein Timer mit dem aktuell Führenden. Im Menü gibt es die Live-Rangliste.
5. Am Ende wird der Gewinner angezeigt (höchste **Gesamtsumme** pro Spieler).
   `/auktion stop` bricht eine laufende Auktion ab.

## Welche Nachrichten zählen?
Nur Systemnachrichten des Servers (nicht der Spieler-Chat) und nur, wenn die **gesamte**
Nachricht exakt so aussieht:

    [HugoSMP] Du hast $30.000.000[BEISPIEL] von [SPIELERNAME] erhalten.

Davor und dahinter darf kein Zeichen stehen. Das Muster steht in
`src/main/java/de/hugosmp/auction/PaymentParser.java` und lässt sich dort anpassen.

## Bauen
Voraussetzung: JDK 21. Projekt in IntelliJ öffnen (Gradle-Import) oder in der Konsole:

    gradle build

Die fertige Mod liegt danach in `build/libs/hugo-auction-1.0.0.jar` (die Datei ohne `-sources`).
Zum Starten braucht man Fabric Loader und Fabric API für 1.21.11.

Falls Gradle meckert, Versionen in `gradle.properties` auf die Empfehlungen von
https://fabricmc.net/develop/ setzen.
