# GoToBed

Paper-Plugin: `/go-to-bed` setzt einem Spieler ab einer Uhrzeit Papa und Mama vor die Nase. Die beiden Mannequins folgen, blockieren, und sagen nur dem Opfer alle 10 s den Satz. Weg sind sie erst nach Logout plus 10 Minuten oder `/go-to-bed cancel`.

Server: Paper **26.2** (Build 129), Java **25**. Build-API weiterhin **26.1.2**. Permission `gotobed.admin` (default op). Details: [SPEC.md](SPEC.md).

Stand 2026-09-29: **0.5.0** mit relativen Zeiten ist auf pfefferminz installiert; Start auf Paper 26.2 ohne Fehler bestätigt. 31 Tests und Formatprüfung bestanden. Papa und Mama erscheinen laut Ingame-Test korrekt. Die Offline- und übrigen Verhaltenstests sind noch nicht bestätigt; Details in [TODO.md](TODO.md).

## Befehle

```
/go-to-bed <HH:mm|30m|2h> <spieler> <satz>
/go-to-bed now <spieler> <satz>
/go-to-bed cancel <spieler>
/go-to-bed status [spieler]
```

`HH:mm` gilt für heute (Europe/Berlin). Liegt die Zeit in der Vergangenheit, starten die Eltern sofort. Relative Zeiten (`30m`, `2h`, auch `M`/`H`) zählen ab jetzt; erlaubt sind positive ganze Minuten oder Stunden bis 365 Tage. Für 90 Minuten: `90m`, nicht `1h30m`. Sie funktionieren auch über Mitternacht und Zeitumstellungen hinweg. In der **Konsole ohne Slash**.

Beispiel: `/go-to-bed 30m Z_o_o_m Ab ins Bett!`. Bestätigung und Status nennen den Zielzeitpunkt; der gespeicherte Termin bleibt bei einem Neustart erhalten. Alte unveränderte Standard-Hilfetexte werden beim Start aktualisiert; eigene Texte in `messages.usage` und `messages.invalid-time` bitte bei Bedarf ergänzen.

## Bauen

```
./gradlew build
```

JAR: `build/libs/GoToBed-<version>.jar`

Der Build führt auch Tests und die Formatprüfung aus. Die Skin-Tests prüfen unter anderem, dass Papa und Mama unterschiedliche Texturen mit den passenden Modellen verwenden. Die tatsächliche Darstellung wurde am 2026-09-29 vom Nutzer im Spiel bestätigt.

## Figuren prüfen

Mit `/go-to-bed now <spieler> Testsatz` starten und die Skins laden lassen:

- **Papa:** blond, weißer Bart, blaues Oberteil, braune Hose, breite Arme.
- **Mama:** dunkelblond, kein Bart, rotes Oberteil, blaue Hose, schmale Arme.

Danach mit `/go-to-bed cancel <spieler>` beenden. Falls beide gleich aussehen, die Namen über den Figuren und das sichtbare Aussehen festhalten. Untersuchungsstand und offene Ingame-Prüfungen stehen in [TODO.md](TODO.md).

## Installieren

Auf **pfefferminz** (`/home/pat/minecraft-server`), wie [SPEC.md §8](SPEC.md):

1. JAR nach `plugins/` kopieren, alte `GoToBed-*.jar` entfernen
2. Paper per SIGTERM stoppen und neu starten (kein `/reload`)
3. Log: `Enabling GoToBed v…`, `GoToBed aktiv.`, kein Error
