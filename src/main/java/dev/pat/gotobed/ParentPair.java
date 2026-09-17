package dev.pat.gotobed;

import io.papermc.paper.entity.LookAnchor;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.Nullable;

final class ParentPair {

    private final GoToBedPlugin plugin;
    private final GoToBedConfig config;
    private final PdcKeys keys;
    private final UUID targetUuid;
    private @Nullable Mannequin papa;
    private @Nullable Mannequin mama;
    private @Nullable BukkitTask followTask;

    ParentPair(GoToBedPlugin plugin, GoToBedConfig config, UUID targetUuid) {
        this.plugin = plugin;
        this.config = config;
        this.keys = PdcKeys.of(plugin);
        this.targetUuid = targetUuid;
    }

    void start(Player player) {
        if (followTask != null) {
            followTask.cancel();
            followTask = null;
        }
        despawnEntities();
        papa = spawn(player, Speaker.PAPA);
        mama = spawn(player, Speaker.MAMA);
        followTask =
                plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 0L, config.followIntervalTicks());
    }

    void despawn() {
        if (followTask != null) {
            followTask.cancel();
            followTask = null;
        }
        despawnEntities();
    }

    void swing(Speaker speaker) {
        Mannequin mannequin = speaker == Speaker.PAPA ? papa : mama;
        if (mannequin != null && mannequin.isValid()) {
            mannequin.swingMainHand();
        }
    }

    static void removeTagged(GoToBedPlugin plugin) {
        PdcKeys keys = PdcKeys.of(plugin);
        for (World world : Bukkit.getWorlds()) {
            for (Mannequin mannequin : world.getEntitiesByClass(Mannequin.class)) {
                if (mannequin.getPersistentDataContainer().has(keys.target, PersistentDataType.STRING)) {
                    mannequin.remove();
                }
            }
        }
    }

    private void tick() {
        Player player = Bukkit.getPlayer(targetUuid);
        if (player == null || !player.isOnline() || !player.isValid()) {
            despawnEntities();
            return;
        }
        papa = ensure(papa, player, Speaker.PAPA);
        mama = ensure(mama, player, Speaker.MAMA);
        move(papa, player, Speaker.PAPA);
        move(mama, player, Speaker.MAMA);
    }

    private Mannequin ensure(@Nullable Mannequin current, Player player, Speaker speaker) {
        if (current != null && current.isValid() && current.getWorld().equals(player.getWorld())) {
            return current;
        }
        if (current != null && current.isValid()) {
            current.remove();
        }
        return spawn(player, speaker);
    }

    private Mannequin spawn(Player player, Speaker speaker) {
        Location location = station(player, speaker);
        return player.getWorld().spawn(location, Mannequin.class, mannequin -> configure(mannequin, speaker));
    }

    private void configure(Mannequin mannequin, Speaker speaker) {
        mannequin.customName(Component.text(speaker == Speaker.PAPA ? "Papa" : "Mama"));
        mannequin.setCustomNameVisible(true);
        mannequin.setDescription(null);
        mannequin.setAI(false);
        mannequin.setGravity(false);
        mannequin.setCollidable(true);
        mannequin.setInvulnerable(true);
        mannequin.setRemoveWhenFarAway(false);
        mannequin.setPersistent(false);
        mannequin.setSilent(true);
        mannequin.setCanPickupItems(false);
        mannequin.setImmovable(true);
        mannequin.setProfile(plugin.skins().profile(speaker));
        var pdc = mannequin.getPersistentDataContainer();
        pdc.set(keys.role, PersistentDataType.STRING, speaker.yaml());
        pdc.set(keys.target, PersistentDataType.STRING, targetUuid.toString());
    }

    private void move(Mannequin mannequin, Player player, Speaker speaker) {
        Location location = station(player, speaker);
        mannequin.teleport(location);
        Location eyes = player.getEyeLocation();
        mannequin.lookAt(eyes.getX(), eyes.getY(), eyes.getZ(), LookAnchor.EYES);
    }

    private Location station(Player player, Speaker speaker) {
        Location feet = player.getLocation();
        ParentStations.Offset offset = speaker == Speaker.PAPA
                ? ParentStations.papa(feet.getYaw(), config.standDistance(), config.lateralOffset())
                : ParentStations.mama(feet.getYaw(), config.standDistance(), config.lateralOffset());
        Location location =
                new Location(feet.getWorld(), feet.getX() + offset.x(), feet.getY(), feet.getZ() + offset.z());
        location.setYaw(feet.getYaw());
        location.setPitch(0f);
        return location;
    }

    private void despawnEntities() {
        if (papa != null) {
            if (papa.isValid()) {
                papa.remove();
            }
            papa = null;
        }
        if (mama != null) {
            if (mama.isValid()) {
                mama.remove();
            }
            mama = null;
        }
    }

    private record PdcKeys(NamespacedKey role, NamespacedKey target) {
        static PdcKeys of(GoToBedPlugin plugin) {
            return new PdcKeys(new NamespacedKey(plugin, "role"), new NamespacedKey(plugin, "target"));
        }
    }
}
