# GoToBed — TODO

> Stand: 2026-09-17 · Iteration 4 lokal fertig (`GoToBed-0.4.1`)
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

### Status: **Lokal abgeschlossen** — Command in 0.3.0 enthalten, noch nicht auf pfefferminz

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
- [ ] Papa ist als blonder Mann mit weißem Bart erkennbar (Code + Skin da, noch nicht ingame)
- [ ] Mama ist als dunkelblonde Frau erkennbar
- [ ] Skins laden ohne Error; kein Fallback auf Steve/Alex-Default im Normalbetrieb
- [x] PNGs und Texture-JSON liegen in der JAR

### Status: **Lokal abgeschlossen** (`GoToBed-0.4.1.jar`) — MineSkin-signiert, eigene Profil-UUIDs, Ingame offen

---

## Iteration 5 — Feinschliff, Build, Ingame-Check

**Ziel:** v1 gegen SPEC prüfen, JAR auf pfefferminz, ingame bestätigt.

### Tasks
- [ ] Alle SPEC-§5-Keys aus `config.yml` lesen, Defaults identisch
- [ ] Texte zentral unter `messages.*`
- [ ] README mit Build + Installation (Verweis auf SPEC §8)
- [ ] Version 1.0.0, `./gradlew build`
- [ ] Installieren laut SPEC §8 (alte JARs weg, SIGTERM, `setsid ./start-paper.sh`)
- [ ] Log: Enabling GoToBed, kein Error
- [ ] Manuelle Ingame-Checkliste unten abhaken

### Akzeptanzkriterien
- [ ] SPEC-§10-Kriterien, die ohne Ingame prüfbar sind, abgehakt; Rest dort als Ingame vermerkt
- [ ] `./gradlew build` erzeugt `GoToBed-1.0.0.jar`
- [ ] Plugin startet auf Paper 26.1.2 ohne Error

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

### Status: **Offen**

---

## Backlog (nicht v1)

- [ ] Relativzeiten (`30m`, `2h`)
- [ ] Nur-Opfer-Sichtbarkeit
- [ ] Nearby- oder Server-Broadcast des Satzes
- [ ] Option ohne Kollision
- [ ] Bett als Ausweg
- [ ] Elter-Namen/Skins über Config tauschen
- [ ] Ingame-Reload der Config
