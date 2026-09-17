package dev.pat.gotobed;

import java.time.DateTimeException;
import java.time.ZoneId;
import org.bukkit.configuration.file.FileConfiguration;

final class GoToBedConfig {

    private final GoToBedPlugin plugin;

    GoToBedConfig(GoToBedPlugin plugin) {
        this.plugin = plugin;
    }

    ZoneId timezone() {
        String raw = cfg().getString("timezone", "Europe/Berlin");
        try {
            return ZoneId.of(raw == null ? "Europe/Berlin" : raw);
        } catch (DateTimeException e) {
            plugin.getLogger().warning("Ungültige timezone=" + raw + ", nutze Europe/Berlin");
            return ZoneId.of("Europe/Berlin");
        }
    }

    int maxSentenceLength() {
        return Math.max(1, cfg().getInt("max-sentence-length", 256));
    }

    int followIntervalTicks() {
        return Math.max(1, cfg().getInt("follow-interval-ticks", 2));
    }

    int speakIntervalSeconds() {
        return Math.max(1, cfg().getInt("speak-interval-seconds", 10));
    }

    int offlineClearMinutes() {
        return Math.max(1, cfg().getInt("offline-clear-minutes", 10));
    }

    double standDistance() {
        return Math.max(0.5, cfg().getDouble("stand-distance", 2.0));
    }

    double lateralOffset() {
        return Math.max(0.0, cfg().getDouble("lateral-offset", 0.7));
    }

    String scheduled(String player, String time, String sentence) {
        return msg("scheduled", "{player} muss um {time} ins Bett. Satz: {sentence}")
                .replace("{player}", player)
                .replace("{time}", time)
                .replace("{sentence}", sentence);
    }

    String activated(String player) {
        return msg("activated", "Elternpaar ist jetzt bei {player}.").replace("{player}", player);
    }

    String replaced(String player) {
        return msg("replaced", "Vorheriger Auftrag für {player} wurde ersetzt.").replace("{player}", player);
    }

    String cancelled(String player) {
        return msg("cancelled", "Auftrag für {player} beendet.").replace("{player}", player);
    }

    String cancelNone(String player) {
        return msg("cancel-none", "{player} hat keinen GoToBed-Auftrag.").replace("{player}", player);
    }

    String statusScheduled(String player, String time, String sentence) {
        return msg("status-scheduled", "{player}: geplant {time} — {sentence}")
                .replace("{player}", player)
                .replace("{time}", time)
                .replace("{sentence}", sentence);
    }

    String statusActive(String player, String sentence) {
        return msg("status-active", "{player}: aktiv — {sentence}")
                .replace("{player}", player)
                .replace("{sentence}", sentence);
    }

    String statusOffline(String player, long minutes, String sentence) {
        return msg("status-offline", "{player}: aktiv, offline seit {minutes} min — {sentence}")
                .replace("{player}", player)
                .replace("{minutes}", Long.toString(minutes))
                .replace("{sentence}", sentence);
    }

    String statusNone() {
        return msg("status-none", "Keine Aufträge.");
    }

    String unknownPlayer(String player) {
        return msg("unknown-player", "Spieler nicht gefunden: {player}").replace("{player}", player);
    }

    String invalidTime() {
        return msg("invalid-time", "Uhrzeit muss HH:mm sein, z. B. 22:00.");
    }

    String emptySentence() {
        return msg("empty-sentence", "Satz fehlt.");
    }

    String sentenceTooLong(int max) {
        return msg("sentence-too-long", "Satz ist zu lang (max. {max} Zeichen).")
                .replace("{max}", Integer.toString(max));
    }

    String usage() {
        return msg("usage", "/go-to-bed <HH:mm> <spieler> <satz>");
    }

    String chatPapa(String sentence) {
        return msg("chat-papa", "<Papa> {sentence}").replace("{sentence}", sentence);
    }

    String chatMama(String sentence) {
        return msg("chat-mama", "<Mama> {sentence}").replace("{sentence}", sentence);
    }

    private String msg(String key, String fallback) {
        String value = cfg().getString("messages." + key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private FileConfiguration cfg() {
        return plugin.getConfig();
    }
}
