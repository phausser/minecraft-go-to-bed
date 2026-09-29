# GoToBed — TODO

> Stand: 2026-09-29 · Relativzeiten in 0.5.0 implementiert; `GoToBed-0.5.0` auf pfefferminz installiert; Start auf Paper 26.2-129 geprüft; Papa/Mama im Spiel vom Nutzer bestätigt; übrige Ingame-Prüfungen offen
> Zyklus: **Planen → Implementieren → Review → Self-Check → Abschluss**
>
> Legende: `[ ]` offen · `[~]` in Arbeit · `[x]` erledigt · `[-]` verworfen

---

## Iteration 0 — Spezifikation

**Ziel:** Verhalten und Schnitt festschreiben, bevor Code entsteht.

### Tasks
- [x] Anforderungen aus dem Gespräch ableiten (`/go-to-bed SPIELER UHRZEIT SATZ`, Elternpaar, 10 s Satz, 10 min Offline)
- [x] Server-Version klären (Paper 26.1.2-72, Java 25, nicht 1.21.0)
- [x] Produktentscheidungen: heute `HH:mm` sonst sofort; alle sehen die Eltern; Chat nur Opfer; Kollision an
- [x] Entity-Wahl: Paper-Mannequin statt Citizens/Packets
- [x] `SPEC.md` anlegen
- [x] `TODO.md` anlegen

### Akzeptanzkriterien
- [x] `SPEC.md` beschreibt Ablauf, Regeln, Config, Texte, Technik, Installation, Out-of-Scope
- [x] `TODO.md` listet Iterationen mit konkreten Tasks und Akzeptanzkriterien

### Status: **Abgeschlossen**

---

## Iteration 1 — Gradle-Projekt & Command-Hülle

**Ziel:** Lauffähiges Paper-Plugin, das lädt und `/go-to-bed` registriert (noch ohne Entities).

### Tasks
- [x] Gradle (Kotlin DSL), Java 25, Paper-API `26.1.2.build.72-stable`
- [x] `settings.gradle.kts`, `build.gradle.kts`, `.gitignore`, Wrapper analog SimpleRTP
- [x] `plugin.yml`: Name `GoToBed`, Main `dev.pat.gotobed.GoToBedPlugin`, `api-version: "26.1"`
- [x] Command `go-to-bed` / Alias `gotobed`, Permission `gotobed.admin` default op
- [x] Default-`config.yml` laut SPEC §5 inklusive `messages.*`
- [x] `GoToBedPlugin` lädt Config, registriert Command
- [x] `GoToBedCommand`: Usage, Permission, `now` / `cancel` / `status` / Hauptform parsen
- [x] Unbekannter Spieler, ungültige Zeit, leerer Satz → deutsche Ablehnung
- [x] Erfolgreiches Setzen → Bestätigung, intern nur loggen (noch kein Spawn)

### Akzeptanzkriterien
- [x] `./gradlew build` ist grün und legt `GoToBed-0.1.0.jar` unter `build/libs/` an
- [ ] Plugin erscheint nach Copy + Serverstart in `/plugins` (Log: Loading/Enabling GoToBed v0.1.0)
- [ ] Nicht-OP kann `/go-to-bed` nicht ausführen
- [ ] OP bekommt Usage bzw. Ablehnung/Bestätigung laut SPEC §6
- [ ] Kein Error im Paper-Log beim Enable

### Status: **Lokal abgeschlossen** — Command seit 0.3.0 enthalten; Plugin später auf pfefferminz installiert

---

## Iteration 2 — Zeitplan, Store, Offline-Timer

**Ziel:** Aufträge persistieren, zur Uhrzeit „aktivieren“, 10-Minuten-Offline-Logik ohne Entities (Chat-Platzhalter reicht).

### Tasks
- [x] `TimeParser`: `H:mm` / `HH:mm`, Europe/Berlin, Vergangenheit → sofort
- [x] `AssignmentStore` in `plugins/GoToBed/data.yml` (Felder laut SPEC §4.8)
- [x] `GoToBedService`: create/replace, cancel, status, load on enable
- [x] Scheduler bis `scheduledAt`; `now` setzt sofort `active`
- [x] Join/Quit: `logoutAt`, Rejoin vor 10 min hält `active`, danach Auftrag löschen
- [x] Enable: Store laden, abgelaufene Schedules aktivieren, abgelaufene Offline-Timer löschen
- [x] Zweites `/go-to-bed` auf dieselbe UUID ersetzt den Auftrag
- [x] Status-Command listet geplant / aktiv / offline-seit
- [x] JUnit: Zeit-Parsing, sofort-vs-warten, Offline-10-min, Replace

### Akzeptanzkriterien
- [ ] `/go-to-bed 22:00 Spieler Satz` speichert den Auftrag über Neustart (Code + `data.yml` da, noch nicht ingame)
- [x] Vergangenheit oder `now` setzt Status `active` ohne zu warten (Unit-Test)
- [x] Zukunft wartet bis `HH:mm` (heute) (Unit-Test)
- [x] Quit + 10 min → Auftrag weg (auch wenn der Spieler offline bleibt) (Unit-Test)
- [x] Quit + Rejoin nach 2 min → Auftrag noch `active` (Unit-Test)
- [x] `cancel` löscht geplant und aktiv
- [x] Unit-Tests für Parser und Timer sind grün (15 Tests)

### Status: **Lokal abgeschlossen** (`GoToBed-0.2.0.jar`) — Persistenz über Server-Neustart noch nicht ingame geprüft

---

## Iteration 3 — Mannequins, Follow, Kollision, Chat

**Ziel:** Papa und Mama stehen wirklich vor dem Spieler, blockieren, sprechen nur das Opfer an.

### Tasks
- [x] `ParentPair`: zwei `Mannequin` spawnen, PDC-Tag, Namen Papa/Mama
- [x] Flags: invulnerable, keine KI, keine Gravity, `removeWhenFarAway=false`, **collidable**
- [x] Follow-Task alle `follow-interval-ticks`: 2 Blöcke vor Yaw, Papa links / Mama rechts, `lookAt` Spieler
- [x] Dimension-/Vehicle-/Flug-Wechsel mitgehen
- [x] Verschwundene Entity beim nächsten Tick neu spawnen
- [x] Speak-Task: sofort, dann alle 10 s, nur Opfer, Wechsel Papa/Mama, Format SPEC §6
- [x] Armschwung am Sprecher, falls Mannequin das hergibt; sonst weglassen
- [x] Enable: getaggte Rest-Mannequins in geladenen Welten entfernen, dann Paare aus Store spawnen
- [x] Disable / cancel / clear: beide Entities entfernen
- [x] Default-Mannequin-Skin vorerst akzeptabel (eigene Skins kommen in Iteration 4)

### Akzeptanzkriterien
- [ ] Ab `active` stehen zwei Mannequins vor dem Spieler und folgen ihm (Code da, noch nicht ingame)
- [ ] Andere Spieler sehen sie
- [ ] Opfer läuft gegen sie (Kollision)
- [ ] Nur das Opfer sieht `<Papa>` / `<Mama>` im 10-s-Takt, erste Zeile sofort
- [ ] Weltwechsel: Eltern sind in der neuen Welt wieder vor ihm
- [ ] Cancel despawnt beide; keine verwaisten Mannequins nach Disable

### Status: **Lokal abgeschlossen** (`GoToBed-0.3.0.jar`) — Follow-Position unit-getestet, Ingame offen

---

## Iteration 4 — Skins Papa / Mama

**Ziel:** Die Figuren sehen aus wie beschrieben, nicht wie Default-Mannequins.

### Tasks
- [x] 64×64-Skin Papa: blond, weißer Bart, Steve-wide, Minecraft-Stil
- [x] 64×64-Skin Mama: dunkelblond, Alex-slim, Minecraft-Stil
- [x] Signierte Texture-Properties erzeugen (MineSkin o. ä.) und unter `src/main/resources/skins/` ablegen
- [x] `ResolvableProfile` auf beide Mannequins setzen (wide vs slim)
- [x] Custom-Name sichtbar, keine störende Default-Description „NPC“ falls abschaltbar

### Akzeptanzkriterien
- [x] Papa erscheint als eigene Figur (Nutzerbestätigung 2026-09-29; blond/weißer Bart zusätzlich an den Texturen geprüft)
- [x] Mama erscheint als eigene Figur (Nutzerbestätigung 2026-09-29; dunkelblond zusätzlich an der Textur geprüft)
- [ ] Skins laden ohne Error; kein Fallback auf Steve/Alex-Default im Normalbetrieb
- [x] PNGs und Texture-JSON liegen in der JAR

### Status: **Lokal abgeschlossen** (`GoToBed-0.4.1.jar`) — MineSkin-signiert, eigene Profil-UUIDs; Papa/Mama ingame am 2026-09-29 bestätigt, Logprüfung offen

---

## Iteration 5 — Feinschliff, Build, Ingame-Check

**Ziel:** v1 gegen SPEC prüfen, JAR auf pfefferminz, ingame bestätigt.

### Tasks
- [x] Alle SPEC-§5-Keys aus `config.yml` lesen, Defaults identisch (Codeprüfung 2026-09-28)
- [x] Texte zentral unter `messages.*` (Codeprüfung 2026-09-28; Permission-Ablehnung durch Paper)
- [x] README mit Build + Installation (Verweis auf SPEC §8)
- [ ] Version 1.0.0, `./gradlew build`
- [x] 0.4.3 installiert laut SPEC §8 (2026-09-29; alte JAR und Plugin-Daten gesichert, SIGTERM, `setsid ./start-paper.sh`)
- [x] Log: Enabling GoToBed v0.4.3, GoToBed aktiv, Done, kein Error (2026-09-29)
- [ ] Manuelle Ingame-Checkliste unten abhaken

### Akzeptanzkriterien
- [ ] SPEC-§10-Kriterien, die ohne Ingame prüfbar sind, abgehakt; Rest dort als Ingame vermerkt
- [ ] `./gradlew build` erzeugt `GoToBed-1.0.0.jar`
- [x] Plugin 0.4.3 startet auf dem aktuellen Paper 26.2-129 ohne Error (2026-09-29)

### Ingame-Checkliste
- [ ] OP: `/go-to-bed 22:00 <online> Testsatz` (oder `now`) spawnt Eltern
- [ ] Nicht-OP: Befehl abgelehnt
- [ ] Eltern stehen vor dem Opfer, andere sehen sie, Opfer bleibt an ihnen hängen
- [ ] Chat nur beim Opfer, alle 10 s, Papa/Mama im Wechsel
- [ ] Vergangenheit-Uhrzeit startet sofort
- [ ] Zukunft-Uhrzeit wartet
- [ ] Logout 10 s + Rejoin → Eltern wieder da
- [ ] Logout 10 min → Eltern weg
- [ ] `/go-to-bed cancel` despawnt
- [ ] Server-Neustart mit aktivem Auftrag stellt Eltern wieder her
- [ ] Zweites `/go-to-bed` ersetzt Satz/Zeit

### Status: **In Arbeit** — Build 0.4.3 und 26 Tests einschließlich Formatprüfung erfolgreich; v1-Freigabe und Ingame-Prüfung offen

### Untersuchung „zweimal Mama“ (2026-09-28)

- Auf pfefferminz liegt `GoToBed-0.4.2.jar`; der Skin-Code enthält bereits getrennte Profil-UUIDs und Texturen.
- Beide signierten Texture-Payloads verweisen auf unterschiedliche Bilder. Papa nutzt classic/wide, Mama slim.
- Die Bilder wurden von `textures.minecraft.net` abgerufen und visuell geprüft: Papa blond mit weißem Bart, Mama dunkelblond ohne Bart. Die heruntergeladenen Bilder stimmen pixelgenau mit den jeweiligen PNGs im Projekt überein.
- Regressionstests prüfen unterschiedliche Textur-Adressen und Payload-Profil-IDs, die Modellzuordnung, decodierbare Texture-/Signaturdaten, URL-Konsistenz, 64×64-PNGs und Papas Bart. Sie prüfen weder die Signatur kryptografisch noch die Darstellung im Minecraft-Client.
- Kein reproduzierter Darstellungsfehler, daher keine Änderung am Laufzeitcode und kein Server-Neustart im Rahmen dieser Untersuchung.
- Ergebnis vom 2026-09-29: Nutzer bestätigt „es erscheinen papa und mama. das funktioniert“. Die doppelte Frauenfigur ist im aktuellen Test nicht mehr aufgetreten. Die dabei eingesetzte Plugin-Version wurde nicht angegeben; eine Installation von 0.4.3 ist dadurch nicht bestätigt.

### Timerkorrektur 0.4.3 (2026-09-29)

- Wartezeiten werden auf volle Ticks aufgerundet, einschließlich Resten unter einer Millisekunde.
- Geplante Starts prüfen beim Callback erneut die Uhrzeit. Bei zu frühem Callback wird der Timer neu gesetzt.
- Offline-Timer planen eine erneute Prüfung, wenn die Frist noch nicht abgelaufen ist. Bisher konnte ein knapp zu früher Callback den Auftrag ohne weiteren Timer zurücklassen.
- Beim Ersetzen eines Timers wird der vorherige Task abgebrochen; eine Aktivierung beim Join entfernt den noch geplanten Start-Task.
- Vier zusätzliche Tests prüfen Tick-Grenzen, überfällige Fristen und die Restwartezeit nach einer frühen Offline-Prüfung. Die Tests simulieren keinen laufenden Paper-Scheduler.
- 0.4.3 am 2026-09-29 installiert und Start geprüft: Paper 26.2-129, Java 25; keine Fehler im Startlog. JAR-Prüfsumme vor Installation bestätigt. Sicherung der alten JAR und Plugin-Daten: `/home/pat/minecraft-server/backups/gotobed-20260929/`.
- Ingame-Prüfung der Timerkorrektur bleibt offen; die Figurenprüfung ist bestanden.

---

## Backlog (nicht v1)

- [x] Relativzeiten (`30m`, `2h`) in 0.5.0 implementiert und installiert (Ingame-Test offen)
- [ ] Nur-Opfer-Sichtbarkeit
- [ ] Nearby- oder Server-Broadcast des Satzes
- [ ] Option ohne Kollision
- [ ] Bett als Ausweg
- [ ] Elter-Namen/Skins über Config tauschen
- [ ] Ingame-Reload der Config

## Erweiterung 0.5.0 — Relativzeiten

- Positive ganze Minuten oder Stunden, maximal 365 Tage; Groß-/Kleinschreibung egal.
- Zieltermin als Instant gespeichert; Dauer läuft über Mitternacht, Zeitumstellung und Neustart weiter.
- Tab-Vorschläge und Hilfetexte ergänzt; alte unveränderte Standardtexte werden automatisch aktualisiert, angepasste Texte bleiben erhalten.
- Bestätigung relativer Termine und Status geplanter Aufträge mit Datum und Uhrzeit.
- Tests für Minuten/Stunden, Sekundenreste, Mitternacht, beide Zeitumstellungen, Grenzen, ungültige Eingaben und Befehlszerlegung.
- [x] 0.5.0 installiert; SHA-256 geprüft, geordneter Neustart, GoToBed aktiv und Done ohne Fehler bestätigt (2026-09-29).
- Sicherung: `/home/pat/minecraft-server/backups/gotobed-0.5.0-20260929-110231/`.
- [ ] `/go-to-bed 1m <spieler> Testsatz` im Spiel prüfen.
- [ ] Älteren abweichenden Servertext `messages.usage` korrigieren: aktuell noch Spieler vor Uhrzeit; gültig ist `/go-to-bed <HH:mm|30m|2h> <spieler> <satz>`. Der abweichende Text wurde durch die Migration bewusst erhalten.
- Build 0.5.0 erfolgreich: 31 Tests und Formatprüfung bestanden.
