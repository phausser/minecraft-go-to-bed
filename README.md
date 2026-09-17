# GoToBed

Paper-Plugin: `/go-to-bed` setzt einem Spieler ab einer Uhrzeit Papa und Mama vor die Nase. Die beiden Mannequins folgen, blockieren, und sagen nur dem Opfer alle 10 s den Satz. Weg sind sie erst nach Logout plus 10 Minuten oder `/go-to-bed cancel`.

Zielserver: Paper **26.1.2**, Java **25**. Permission `gotobed.admin` (default op). Details: [SPEC.md](SPEC.md).

## Befehle

```
/go-to-bed <spieler> <HH:mm> <satz>
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

## Installieren

Auf **pfefferminz** (`/home/pat/minecraft-server`), wie [SPEC.md §8](SPEC.md):

1. JAR nach `plugins/` kopieren, alte `GoToBed-*.jar` entfernen
2. Paper per SIGTERM stoppen und neu starten (kein `/reload`)
3. Log: `Enabling GoToBed v…`, `GoToBed aktiv.`, kein Error
