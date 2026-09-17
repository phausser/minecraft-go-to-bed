package dev.pat.gotobed;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

final class GoToBedService implements Listener {

    private final GoToBedPlugin plugin;
    private final GoToBedConfig config;
    private final AssignmentStore store;
    private final ConcurrentHashMap<UUID, Assignment> assignments = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, BukkitTask> activationTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, BukkitTask> speakTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, BukkitTask> clearTasks = new ConcurrentHashMap<>();

    GoToBedService(GoToBedPlugin plugin, GoToBedConfig config, AssignmentStore store) {
        this.plugin = plugin;
        this.config = config;
        this.store = store;
    }

    void restore() {
        Instant now = Instant.now();
        Duration timeout = offlineTimeout();
        List<Assignment> restored = AssignmentRules.restore(store.load(), now, timeout);
        assignments.clear();
        for (Assignment assignment : restored) {
            assignments.put(assignment.uuid(), assignment);
            arm(assignment);
        }
        persist();
        plugin.getLogger().info("GoToBed: " + assignments.size() + " Auftrag/Aufträge geladen.");
    }

    Optional<Assignment> assign(OfflinePlayer target, String displayName, String sentence, TimeParser.Result time) {
        cancelTasks(target.getUniqueId());
        Assignment next = Assignment.create(
                target.getUniqueId(),
                displayName,
                sentence,
                time.scheduledAt(),
                time.immediate()
        );
        Assignment previous = assignments.put(next.uuid(), next);
        plugin.getLogger().info(
                "Auftrag für " + displayName + " (" + next.uuid() + "): "
                        + (time.immediate() ? "sofort" : "um " + time.display())
                        + " — " + sentence
        );
        arm(next);
        persist();
        return Optional.ofNullable(previous);
    }

    Optional<Assignment> cancel(UUID uuid) {
        Assignment removed = assignments.remove(uuid);
        cancelTasks(uuid);
        if (removed != null) {
            persist();
        }
        return Optional.ofNullable(removed);
    }

    Optional<Assignment> get(UUID uuid) {
        return Optional.ofNullable(assignments.get(uuid));
    }

    Collection<Assignment> all() {
        return List.copyOf(assignments.values());
    }

    boolean isDue(Assignment assignment, Instant now) {
        return AssignmentRules.isActive(assignment, now);
    }

    void shutdown() {
        for (UUID uuid : List.copyOf(assignments.keySet())) {
            cancelTasks(uuid);
        }
        persist();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Assignment assignment = assignments.get(player.getUniqueId());
        if (assignment == null) {
            return;
        }
        Instant now = Instant.now();
        if (AssignmentRules.shouldClearOffline(assignment, now, offlineTimeout())) {
            clear(assignment.uuid(), "10 Minuten offline (Join)");
            return;
        }
        Assignment resumed = AssignmentRules.onJoinResume(assignment).withName(player.getName());
        if (AssignmentRules.shouldActivate(resumed, now)) {
            resumed = resumed.activate();
        }
        assignments.put(resumed.uuid(), resumed);
        cancel(clearTasks, resumed.uuid());
        persist();
        if (resumed.status() == AssignmentStatus.ACTIVE) {
            startSpeak(player, resumed);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Assignment assignment = assignments.get(uuid);
        if (assignment == null) {
            return;
        }
        cancel(speakTasks, uuid);
        Assignment updated = AssignmentRules.onQuit(assignment, Instant.now());
        assignments.put(uuid, updated);
        persist();
        if (updated.status() == AssignmentStatus.ACTIVE && updated.logoutAt() != null) {
            scheduleOfflineClear(updated);
        }
    }

    private void arm(Assignment assignment) {
        if (assignment.status() == AssignmentStatus.SCHEDULED) {
            if (AssignmentRules.shouldActivate(assignment, Instant.now())) {
                activateDue(assignment.uuid());
            } else {
                scheduleActivation(assignment);
            }
            return;
        }
        Player player = Bukkit.getPlayer(assignment.uuid());
        if (player != null && player.isOnline()) {
            startSpeak(player, assignment);
            return;
        }
        if (assignment.logoutAt() != null) {
            scheduleOfflineClear(assignment);
        }
    }

    private void scheduleActivation(Assignment assignment) {
        Instant now = Instant.now();
        long delayMs = Math.max(0L, assignment.scheduledAt().toEpochMilli() - now.toEpochMilli());
        long ticks = Math.max(1L, delayMs / 50L);
        BukkitTask task = plugin.getServer().getScheduler()
                .runTaskLater(plugin, () -> activateDue(assignment.uuid()), ticks);
        activationTasks.put(assignment.uuid(), task);
    }

    private void activateDue(UUID uuid) {
        Assignment assignment = assignments.get(uuid);
        if (assignment == null || assignment.status() == AssignmentStatus.ACTIVE) {
            return;
        }
        Assignment active = assignment.activate();
        assignments.put(uuid, active);
        cancel(activationTasks, uuid);
        persist();
        plugin.getLogger().info("Auftrag aktiv: " + active.name());
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            startSpeak(player, active);
        }
    }

    private void scheduleOfflineClear(Assignment assignment) {
        Instant logoutAt = assignment.logoutAt();
        if (logoutAt == null) {
            return;
        }
        Instant deadline = logoutAt.plus(offlineTimeout());
        long delayMs = Math.max(0L, deadline.toEpochMilli() - Instant.now().toEpochMilli());
        long ticks = Math.max(1L, delayMs / 50L);
        BukkitTask task = plugin.getServer().getScheduler()
                .runTaskLater(plugin, () -> {
                    Assignment current = assignments.get(assignment.uuid());
                    if (current != null && AssignmentRules.shouldClearOffline(current, Instant.now(), offlineTimeout())) {
                        clear(current.uuid(), "10 Minuten offline");
                    }
                }, ticks);
        clearTasks.put(assignment.uuid(), task);
    }

    private void startSpeak(Player player, Assignment assignment) {
        cancel(speakTasks, assignment.uuid());
        speakOnce(player);
        long period = config.speakIntervalSeconds() * 20L;
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Player online = Bukkit.getPlayer(assignment.uuid());
            if (online == null || !online.isOnline()) {
                cancel(speakTasks, assignment.uuid());
                return;
            }
            speakOnce(online);
        }, period, period);
        speakTasks.put(assignment.uuid(), task);
    }

    private void speakOnce(Player player) {
        Assignment assignment = assignments.get(player.getUniqueId());
        if (assignment == null || assignment.status() != AssignmentStatus.ACTIVE) {
            cancel(speakTasks, player.getUniqueId());
            return;
        }
        String line = assignment.nextSpeaker() == Speaker.PAPA
                ? config.chatPapa(assignment.sentence())
                : config.chatMama(assignment.sentence());
        player.sendMessage(line);
        assignments.put(assignment.uuid(), assignment.spoken());
    }

    private void clear(UUID uuid, String reason) {
        Assignment removed = assignments.remove(uuid);
        cancelTasks(uuid);
        persist();
        if (removed != null) {
            plugin.getLogger().info("Auftrag beendet (" + reason + "): " + removed.name());
        }
    }

    private void persist() {
        store.save(assignments.values());
    }

    private Duration offlineTimeout() {
        return Duration.ofMinutes(config.offlineClearMinutes());
    }

    private void cancelTasks(UUID uuid) {
        cancel(activationTasks, uuid);
        cancel(speakTasks, uuid);
        cancel(clearTasks, uuid);
    }

    private static void cancel(ConcurrentHashMap<UUID, BukkitTask> tasks, UUID uuid) {
        BukkitTask task = tasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }
}
