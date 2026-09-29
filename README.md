# GoToBed

Schickt Minecraft-Spielern Papa und Mama vorbei: Die beiden folgen, stehen im Weg und erinnern den Zielspieler alle zehn Sekunden ans Schlafengehen. Nach zehn Minuten offline oder per Abbruch verschwinden sie.

Für **Paper 26.2 und Java 25**. Berechtigung: `gotobed.admin` (standardmäßig OP).

## Befehle

```text
/go-to-bed <HH:mm|30m|2h> <spieler> <satz>
/go-to-bed now <spieler> <satz>
/go-to-bed cancel <spieler>
/go-to-bed status [spieler]
```

Uhrzeiten gelten für heute, standardmäßig in `Europe/Berlin`; vergangene Zeiten starten sofort. Relative Zeiten zählen ab jetzt: ganze Minuten (`30m`) oder Stunden (`2h`), maximal 365 Tage. Alias: `/gotobed`. In der Serverkonsole ohne Slash.

## Build & Installation

```sh
./gradlew build
```

Server stoppen, `build/libs/GoToBed-<version>.jar` nach `plugins/` kopieren, alte Plugin-Version entfernen und den Server starten. Kein `/reload`.

Zeitzone, Abstände, Intervalle und Texte lassen sich in `plugins/GoToBed/config.yml` anpassen. Änderungen gelten nach einem Neustart.

## Releases

Version in `build.gradle.kts` setzen und einen passenden Tag wie `v0.5.0` pushen. Unter **Actions → Release → Run workflow** diesen Tag angeben. Der Workflow startet ausschließlich manuell, führt Build und Tests aus und veröffentlicht `GoToBed-<version>.jar` als Download im GitHub Release.

## Lizenz

[MIT](LICENSE)
