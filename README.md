# GoToBed

Paper-Plugin: `/go-to-bed` setzt einem Spieler ab einer Uhrzeit Papa und Mama vor die Nase. Die beiden Mannequins folgen, blockieren, und sagen nur dem Opfer alle 10 s den Satz. Weg sind sie erst nach Logout plus 10 Minuten oder `/go-to-bed cancel`.

Zielserver: Paper **26.1.2**, Java **25**. Permission `gotobed.admin` (default op). Details: [SPEC.md](SPEC.md).

Stand 2026-09-29: **0.4.3** lokal gebaut, 26 Tests und Formatprüfung bestanden. Papa und Mama erscheinen laut Ingame-Test korrekt. Die Installation von 0.4.3 sowie die Offline- und übrigen Verhaltenstests sind noch nicht bestätigt; Details in [TODO.md](TODO.md).

## Befehle

```
/go-to-bed <HH:mm> <spieler> <satz>
/go-to-bed now <spieler> <satz>
/go-to-bed cancel <spieler>
/go-to-bed status [spieler]
```

`HH:mm` gilt für heute (Europe/Berlin). Liegt die Zeit in der Vergangenheit, starten die Eltern sofort. In der **Konsole ohne Slash**.

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
