package dev.pat.gotobed;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

final class AssignmentStore {

    private final GoToBedPlugin plugin;
    private final File file;

    AssignmentStore(GoToBedPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    List<Assignment> load() {
        if (!file.exists()) {
            return List.of();
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("assignments");
        if (root == null) {
            return List.of();
        }
        List<Assignment> loaded = new ArrayList<>();
        for (String key : root.getKeys(false)) {
            Assignment assignment = readOne(root.getConfigurationSection(key), key);
            if (assignment != null) {
                loaded.add(assignment);
            }
        }
        return loaded;
    }

    void save(Collection<Assignment> assignments) {
        plugin.getDataFolder().mkdirs();
        YamlConfiguration yaml = new YamlConfiguration();
        for (Assignment assignment : assignments) {
            String path = "assignments." + assignment.uuid();
            yaml.set(path + ".name", assignment.name());
            yaml.set(path + ".sentence", assignment.sentence());
            yaml.set(path + ".scheduledAt", assignment.scheduledAt().toString());
            yaml.set(path + ".status", assignment.status().yaml());
            yaml.set(
                    path + ".logoutAt",
                    assignment.logoutAt() == null ? null : assignment.logoutAt().toString());
            yaml.set(path + ".nextSpeaker", assignment.nextSpeaker().yaml());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Konnte data.yml nicht speichern", e);
        }
    }

    private Assignment readOne(ConfigurationSection section, String key) {
        if (section == null) {
            plugin.getLogger().warning("Ungültiger Auftrag in data.yml: " + key);
            return null;
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(key);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Ungültige UUID in data.yml: " + key);
            return null;
        }
        String name = section.getString("name", key);
        String sentence = section.getString("sentence");
        if (sentence == null || sentence.isBlank()) {
            plugin.getLogger().warning("Auftrag ohne Satz in data.yml: " + key);
            return null;
        }
        Instant scheduledAt = parseInstant(section.getString("scheduledAt"));
        if (scheduledAt == null) {
            plugin.getLogger().warning("Auftrag ohne scheduledAt in data.yml: " + key);
            return null;
        }
        return new Assignment(
                uuid,
                name == null || name.isBlank() ? key : name,
                sentence,
                scheduledAt,
                AssignmentStatus.fromYaml(section.getString("status")),
                parseInstant(section.getString("logoutAt")),
                Speaker.fromYaml(section.getString("nextSpeaker")));
    }

    private static Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
