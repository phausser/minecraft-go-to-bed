# GoToBed — Spezifikation

> Version: 1.0.0 · Stand: 2026-09-17 · Status: v1 spezifiziert, noch nicht implementiert
> Paper-Plugin: `/go-to-bed` setzt einem Spieler ab einer Uhrzeit ein Elternpaar vor die Nase.
>
> **Installieren:** Wenn der Nutzer „installieren“, „nochmal installieren“ oder ähnlich sagt → Abschnitt 8 befolgen. Nicht nach dem Server fragen, Host steht in der SSH-Config.

## 1. Ziel

Ein Admin kann einem Spieler per Befehl eine Uhrzeit und einen Satz geben. Ab dieser Uhrzeit stehen **Papa und Mama** (Minecraft-Spielerfiguren) dauerhaft vor dem Spieler, blockieren den Weg und sagen dem Opfer alle 10 Sekunden den Satz. Die Eltern verschwinden erst, wenn der Spieler sich ausloggt **und mindestens 10 Minuten offline bleibt**.

## 2. Kontext

| Punkt | Wert |
|---|---|
| Server | Paper **26.1.2** (Build 72), Minecraft **26.1.2** |
| Java | **25** |
| Host | `pfefferminz` (`192.168.1.170`, User `pat`) |
| Minecraft-Ordner | `/home/pat/minecraft-server` |
| Plugin-Name | `GoToBed` |
| Sprache der Spielertexte | Deutsch |
| Zielgruppe | Operatoren / Admins (`gotobed.admin`) |
| Sichtbarkeit der Eltern | **alle** Spieler (echte Entities) |
| Chat-Satz | **nur das Opfer** |
| Kollision | **an** — Eltern stehen im Weg |
| Uhrzeit | heute `HH:mm` Europe/Berlin; schon vorbei → **sofort** |

Der Server ist **nicht** 1.21.0. Bestehende Plugins (SimpleRTP, SimpleHome, …) nutzen `api-version: "26.1"`. GoToBed tut dasselbe.

## 3. Ablauf

1. Admin (OP oder Permission) schreibt `/go-to-bed <HH:mm> <spieler> <satz>`.
2. Plugin merkt sich Auftrag: Ziel-UUID, Satz, geplante Uhrzeit, Status `scheduled`.
3. Liegt `HH:mm` **heute noch in der Zukunft** → warten. Liegt sie **jetzt oder in der Vergangenheit** → Status `active`, Eltern spawnen (Spieler online) bzw. beim nächsten Join.
4. Solange `active` und Spieler online:
   - Papa und Mama stehen vor dem Spieler, gucken ihn an, haben Kollision.
   - Alle **10 Sekunden** bekommt **nur das Opfer** den Satz im Chat, im Wechsel `<Papa>` / `<Mama>`. Erste Zeile **sofort** beim Spawn.
5. Logout: Entities despawnen, `logoutAt` speichern, Status bleibt `active`.
6. Rejoin **vor** 10 Minuten Offlinezeit → Eltern sofort wieder da, Timer verworfen.
7. Rejoin **nach** ≥ 10 Minuten Offlinezeit, oder 10 Minuten um ohne Rejoin → Auftrag tot, nichts spawnt mehr.
8. `/go-to-bed cancel <spieler>` beendet den Auftrag jederzeit (geplant oder aktiv).

Im Bett schlafen, Sterben, Weltwechsel, `/kill`, Gamemode-Wechsel beenden den Auftrag **nicht**.

## 4. Regeln

### 4.1 Befehle und Rechte

| Punkt | Regel |
|---|---|
| Hauptbefehl | `/go-to-bed <HH:mm> <spieler> <satz>` |
| Sofort | `/go-to-bed now <spieler> <satz>` |
| Abbruch | `/go-to-bed cancel <spieler>` |
| Status | `/go-to-bed status [spieler]` — ohne Argument: alle aktiven/geplanten |
| Aliase | `/gotobed` |
| Permission | `gotobed.admin` |
| Default | `op` |
| Console | erlaubt |
| Tab-Complete | Subcommands `now` / `cancel` / `status`, Online-Spieler, Uhrzeit-Hinweis `HH:mm` |

Kein Befehl für das Opfer, sich selbst zu befreien.

Zweites `/go-to-bed` auf denselben Spieler **überschreibt** den vorherigen Auftrag (alte Entities weg, neuer Satz / neue Zeit).

### 4.2 Spieler-Argument

- Online-Name oder Offline-Spieler, der dem Server bekannt ist (`OfflinePlayer` mit gültiger UUID, schon einmal gesehen).
- Unbekannter Name → ablehnen.
- Satz darf Leerzeichen enthalten (greedy Rest der Zeile). Leerer Satz → ablehnen.
- Satz-Länge hart begrenzen (z. B. 256 Zeichen), danach ablehnen.

### 4.3 Uhrzeit

- Format: `H:mm` oder `HH:mm` (24h). Beispiele: `9:00`, `22:00`.
- Zeitzone: **Europe/Berlin** (Server-Lokalzeit).
- Bedeutung: **heute** um diese Uhrzeit.
- Ist der Zeitpunkt `<= jetzt` → Auftrag **sofort** aktiv (nicht auf morgen schieben).
- Keine Sekunden, keine Relativzeiten (`30m`) in v1.
- Ungültiges Format → ablehnen, Hinweis auf `HH:mm`.

`/go-to-bed now` überspringt die Uhrzeit und aktiviert sofort.

### 4.4 Eltern-Paar

Zwei Paper-**Mannequins** (`org.bukkit.entity.Mannequin`), keine Citizens/Packet-NPCs.

| | Papa | Mama |
|---|---|---|
| Anzeigename | `Papa` | `Mama` |
| Modell | Steve (wide) | Alex (slim) |
| Aussehen | blond, weißer Bart | dunkelblond, kein Bart |
| Position | vor dem Spieler, leicht links | vor dem Spieler, leicht rechts |

Eigenschaften beider:

- Unverwundbar, keine KI, nicht leinenbar, nicht despawnbar durch Distanz.
- `removeWhenFarAway = false`, Gravity/Physik aus (sie fallen nicht, wenn der Spieler fliegt).
- **Kollision an** — der Spieler muss um sie herum.
- Custom-Name sichtbar. Description leer bzw. nicht „NPC“, wenn die API das hergibt.
- Plugin-PDC-Tag (`gotobed:role` = `papa` / `mama`, plus Ziel-UUID), damit Reste nach Crash gefunden und gelöscht werden.

Skins: zwei eigene 64×64-Minecraft-Skins im Plugin (nicht „irgendein Account, der ungefähr so aussieht“). Auf die Mannequins per `ResolvableProfile` (signierte Texture-Properties, z. B. via MineSkin einmalig erzeugt und in Resources abgelegt).

### 4.5 Position und Folgen

- Distanz: **2,0 Blöcke** vor dem Spieler.
- Seitlicher Abstand: **0,7 Blöcke** (Papa links, Mama rechts relativ zur Blickrichtung).
- Nur **Yaw** (horizontale Blickrichtung), nicht Pitch. Y = Spieler-Y (Füße). Blick nach oben setzt die Eltern nicht in den Himmel.
- Beide gucken den Spieler an (`lookAt` / Rotation).
- Follow-Intervall: alle **2 Ticks** teleportieren.
- Folgen durch Overworld, Nether, End, Flug, Elytra, Pferd, Boot, Minecart, Spectator.
- Zielpunkt in einem Vollblock: trotzdem dorthin (im Weg bleiben hat Vorrang vor „sauber stehen“).
- Chunk-Unload / Entity verschwunden → beim nächsten Follow-Tick neu spawnen.
- Plugin-Disable und Cancel: beide Entities entfernen.

Sichtbarkeit: echte Server-Entities, **alle** Spieler in Reichweite sehen sie.

### 4.6 Sprechen

- Intervall: **10 Sekunden**.
- Erste Zeile **sofort** wenn die Eltern spawnen (Join, Uhrzeit erreicht, `now`).
- Nur das **Opfer** bekommt die Nachricht (`player.sendMessage`). Andere hören nichts.
- Wechsel Papa / Mama. Start mit Papa.
- Format: `<Papa> {satz}` bzw. `<Mama> {satz}` (Adventure-Komponente, Namen farblich unterscheidbar).
- Dazu kurze Sprech-Animation (Armschwung am sprechenden Mannequin), sofern die API das am Mannequin kann; sonst weglassen, Chat bleibt Pflicht.

### 4.7 Beenden (10 Minuten Offline)

Auftrag endet **nur** wenn:

1. Admin `/go-to-bed cancel <spieler>` nutzt, oder
2. Spieler ausloggt **und** danach durchgehend **≥ 10 Minuten** offline ist.

Regeln zum Timer:

- `logoutAt` beim Quit setzen.
- Join bevor 10 Minuten um sind → Timer verwerfen, Eltern sofort wieder spawnen, `logoutAt` löschen.
- Join danach, oder 10 Minuten um während der Spieler offline ist → Status `cleared`, Daten löschen, keine Entities.
- Server-Neustart: gespeicherten `logoutAt` weiterverwenden. Beim Enable prüfen, ob 10 Minuten schon um sind.

Nicht beendend: Schlafen im Bett, Tod, Respawn, Weltwechsel, Gamemode, Keep-Inventory, Plugin-Reload (Reload ist ohnehin ununterstützt — siehe §8).

### 4.8 Persistenz

Datei `plugins/GoToBed/data.yml`, überlebt Neustarts.

Pro Auftrag mindestens:

| Feld | Bedeutung |
|---|---|
| `uuid` | Ziel-Spieler |
| `name` | letzter bekannter Name (Anzeige) |
| `sentence` | Satz |
| `scheduledAt` | geplante Instant (ISO-8601) |
| `status` | `scheduled` / `active` |
| `logoutAt` | Instant oder leer |
| `nextSpeaker` | `papa` / `mama` |

Beim Enable:

1. Daten laden.
2. Alle Mannequins mit PDC-Tag in geladenen Welten entfernen (Crash-Reste).
3. `scheduled` in der Zukunft: Scheduler neu setzen.
4. `scheduled` in der Vergangenheit: auf `active` heben.
5. `active` und Spieler online: Eltern spawnen.
6. `active` und Spieler offline: wenn `logoutAt + 10 min` vorbei → löschen, sonst Timer weiterlaufen lassen.

Entity-UUIDs nicht als einzige Quelle nutzen; nach Restart immer neu spawnen.

### 4.9 Ablehnungen

Kurze deutsche Nachricht, wenn:

- Sender keine Permission hat (Paper-Standard reicht)
- Spieler unbekannt
- Uhrzeit ungültig
- Satz leer oder zu lang
- `cancel`/`status` ohne passenden Auftrag
- Falsche Syntax (Usage zeigen)

## 5. Konfiguration

`plugins/GoToBed/config.yml`, ohne Rebuild änderbar (Neustart; kein `/reload`).

| Schlüssel | Default | Bedeutung |
|---|---|---|
| `timezone` | `Europe/Berlin` | Auswertung von `HH:mm` |
| `follow-interval-ticks` | `2` | Teleport-Takt der Eltern |
| `speak-interval-seconds` | `10` | Chat-Takt |
| `offline-clear-minutes` | `10` | Offlinezeit bis Ende |
| `stand-distance` | `2.0` | Blöcke vor dem Spieler |
| `lateral-offset` | `0.7` | seitlicher Abstand Papa/Mama |
| `max-sentence-length` | `256` | Hartes Limit |
| `messages.*` | siehe §6 | Spieler-/Admin-Texte |

Plugin-Defaults müssen mit dieser Tabelle übereinstimmen.

## 6. Texte (Deutsch)

Spieler- und Admin-Texte auf Deutsch, über `messages.*` überschreibbar.

| Situation | Beispiel |
|---|---|
| Geplant | `{player} muss um {time} ins Bett. Satz: {sentence}` |
| Sofort aktiv | `Elternpaar ist jetzt bei {player}.` |
| Überschrieben | `Vorheriger Auftrag für {player} wurde ersetzt.` |
| Cancel | `Auftrag für {player} beendet.` |
| Cancel ohne Auftrag | `{player} hat keinen GoToBed-Auftrag.` |
| Status geplant | `{player}: geplant {time} — {sentence}` |
| Status aktiv | `{player}: aktiv — {sentence}` |
| Status aktiv, offline | `{player}: aktiv, offline seit {minutes} min — {sentence}` |
| Status leer | `Keine Aufträge.` |
| Unbekannter Spieler | `Spieler nicht gefunden: {player}` |
| Ungültige Zeit | `Uhrzeit muss HH:mm sein, z. B. 22:00.` |
| Leerer Satz | `Satz fehlt.` |
| Satz zu lang | `Satz ist zu lang (max. {max} Zeichen).` |
| Usage | `/go-to-bed <HH:mm> <spieler> <satz>` |
| Chat Papa | `<Papa> {sentence}` |
| Chat Mama | `<Mama> {sentence}` |
| Keine Permission | Standard-Paper-Meldung reicht |

Opfer bekommt **keine** Extra-Begrüßung außer dem Satz-Takt. Admins, die den Befehl setzen, bekommen die Bestätigung.

## 7. Technik

### 7.1 Stack

| Teil | Wahl |
|---|---|
| Build | Gradle (Kotlin DSL), analog SimpleRTP |
| API | Paper-API `26.1.2.build.72-stable` |
| Sprache | Java 25 |
| Artefakt | eine JAR `GoToBed-<version>.jar` nach `build/libs/` |
| Entities | Paper `Mannequin` + `ResolvableProfile` |
| Commands | Paper Brigadier (`LifecycleEvents.COMMANDS` bzw. Plugin-Command mit greedy String) |
| Persistenz | YAML über Bukkit-`YamlConfiguration` |
| Tests | JUnit für Zeit-Parsing, Offline-Timer, Überschreiben; Follow/Skins manuell ingame |

Keine Abhängigkeit von Citizens, ProtocolLib, PacketEvents, FancyNPCs.

### 7.2 Projektstruktur

```
minecraft-go-to-bed/
  SPEC.md
  TODO.md
  build.gradle.kts
  settings.gradle.kts
  src/main/resources/plugin.yml
  src/main/resources/config.yml
  src/main/resources/skins/papa.png
  src/main/resources/skins/mama.png
  src/main/resources/skins/papa.texture.json   # value + signature
  src/main/resources/skins/mama.texture.json
  src/main/java/dev/pat/gotobed/GoToBedPlugin.java
  src/main/java/dev/pat/gotobed/GoToBedCommand.java
  src/main/java/dev/pat/gotobed/GoToBedService.java
  src/main/java/dev/pat/gotobed/AssignmentStore.java
  src/main/java/dev/pat/gotobed/ParentPair.java
  src/main/java/dev/pat/gotobed/TimeParser.java
```

`plugin.yml`:

- `name: GoToBed`
- `main: dev.pat.gotobed.GoToBedPlugin`
- `api-version: "26.1"`
- Command `go-to-bed`, Alias `gotobed`
- Permission `gotobed.admin` mit `default: op`

Package: `dev.pat.gotobed` (wie SimpleRTP unter `dev.pat.*`).

### 7.3 Interner Ablauf

1. `GoToBedCommand` parst Subcommand / Spieler / Zeit / Satz, prüft Permission.
2. `GoToBedService` legt oder ersetzt den Auftrag, schreibt `AssignmentStore`.
3. Scheduler (Bukkit-Task) feuert zur `scheduledAt` bzw. sofort.
4. `ParentPair` spawnt zwei Mannequins, setzt Skin/Name/Flags, startet Follow-Task (2 Ticks) und Speak-Task (10 s).
5. Quit: Pair despawnen, `logoutAt` setzen, Delayed-Task 10 min (oder Prüfung beim nächsten Enable/Join).
6. Join: wenn Auftrag `active` und Offlinezeit < 10 min → Pair neu; sonst aufräumen.
7. Enable: Store laden, Reste löschen, Scheduler/Paare restaurieren.

Main-Thread: Spawn, Teleport, Chat, Despawn. Datei-I/O kurz halten (YAML klein); nicht auf dem Command-Thread blockierend ins Netz (Skins liegen lokal in der JAR).

## 8. Installation

Zielserver ist **immer** `pfefferminz` (SSH-Config `~/.ssh/config`, Host `192.168.1.170`, User `pat`). Minecraft-Ordner: `/home/pat/minecraft-server`. Öffentlich: `binaerraum.ddns.net:25565`. RCON ist aus.

Login-Shell auf dem Host ist **fish**. Remote-Befehle deshalb in `bash` ausführen (`ssh pfefferminz 'bash -lc "..."'` oder `ssh pfefferminz 'bash -s' <<'EOF'`). Kein `set -e` direkt über fish.

### 8.1 Bauen

Im Repo `/Users/pat/Code/Minecraft/minecraft-go-to-bed`:

```
./gradlew build
```

JAR: `build/libs/GoToBed-<version>.jar`. Java-Toolchain 25, Paper-API `26.1.2.build.72-stable`.

### 8.2 JAR kopieren

```
scp build/libs/GoToBed-*.jar pfefferminz:/home/pat/minecraft-server/plugins/
```

Alte `GoToBed-*.jar` in `plugins/` vorher löschen, wenn die Versionsnummer wechselt (sonst lädt Paper beide).

### 8.3 Paper neu starten

Kein `/reload`. RCON ist aus; Konsole hängt an einem TTY. Graceful Stop per **SIGTERM an den Java-Prozess** (PID per `ps` holen, **nicht** `pgrep -f paper-26` im selben Skript — das matcht den Wrapper und killt sich selbst).

```
# PID:
ssh pfefferminz "ps -eo pid,cmd | grep '[j]ava' | grep paper-26"

# Stop (Beispiel-PID ersetzen):
ssh pfefferminz 'kill -TERM <JAVA_PID>'
```

Warten, bis der Prozess weg ist (meist < 15 s; Worlds speichern). Dann starten:

```
ssh pfefferminz 'bash -lc "cd /home/pat/minecraft-server && setsid ./start-paper.sh >> /home/pat/minecraft-server/logs/console.out 2>&1 < /dev/null & echo started:\$!"'
```

`start-paper.sh` startet `paper-26.1.*.jar nogui` mit 6G/8G Heap. `setsid` löst den Prozess von der SSH-Session. Log bleibt `logs/latest.log`.

### 8.4 Prüfen

In `logs/latest.log` muss stehen:

- `[GoToBed] Loading server plugin GoToBed v…`
- `[GoToBed] Enabling GoToBed v…`
- `[GoToBed] GoToBed aktiv.`
- `Done (…s)!`
- kein `ERROR` / Stacktrace von GoToBed

Config nach dem ersten Enable: `/home/pat/minecraft-server/plugins/GoToBed/config.yml` (Defaults aus der JAR, nicht überschreiben wenn der Nutzer sie schon geändert hat).

## 9. Nicht in v1

- Relativzeiten (`30m`, `2h`)
- Nur-Opfer-Sichtbarkeit (Packet-NPCs)
- Satz an Nearby oder den ganzen Server
- Durchlaufen ohne Kollision
- Bett als Ausweg
- Konfigurierbare Elter-Namen über Command
- Mehrere gleichzeitige Aufträge pro Spieler (es gibt genau einen)
- Folia / Multi-Region-Scheduler
- Ingame-Skin-Editor
- `/reload` unterstützen

## 10. Akzeptanzkriterien (v1)

- [ ] `/go-to-bed <HH:mm> <spieler> <satz>` ist nur mit `gotobed.admin` (default op) nutzbar
- [ ] Uhrzeit `HH:mm` gilt für heute, Europe/Berlin; liegt sie in der Vergangenheit, starten die Eltern sofort
- [ ] `/go-to-bed now <spieler> <satz>` startet ohne Wartezeit
- [ ] Ab der Aktivierung stehen zwei Mannequins (Papa blond + weißer Bart, Mama dunkelblond) vor dem Spieler
- [ ] Alle Spieler in Sichtweite sehen die Eltern
- [ ] Die Eltern haben Kollision und folgen dem Spieler (Yaw, alle 2 Ticks, alle Welten)
- [ ] Nur das Opfer sieht alle 10 s den Satz im Chat, im Wechsel Papa/Mama; erste Zeile sofort
- [ ] Logout < 10 min und Rejoin → Eltern sofort wieder da
- [ ] Logout ≥ 10 min offline → Auftrag weg, keine Eltern mehr
- [ ] Schlafen, Tod, Weltwechsel beenden den Auftrag nicht
- [ ] `/go-to-bed cancel <spieler>` beendet geplanten und aktiven Auftrag
- [ ] Auftrag überlebt Server-Neustart (`data.yml`)
- [ ] Zweites `/go-to-bed` auf denselben Spieler ersetzt den alten Auftrag
- [ ] Follow-Takt, Sprech-Intervall, Offline-Minuten und Texte stehen in `config.yml`
- [ ] `./gradlew build` erzeugt eine installierbare JAR
- [ ] Plugin startet auf Paper 26.1.2 ohne Error im Log
