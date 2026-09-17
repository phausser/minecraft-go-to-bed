package dev.pat.gotobed;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class GoToBedCommand {

    private final GoToBedPlugin plugin;
    private final GoToBedConfig config;
    private final GoToBedService service;

    GoToBedCommand(GoToBedPlugin plugin, GoToBedConfig config, GoToBedService service) {
        this.plugin = plugin;
        this.config = config;
        this.service = service;
    }

    void register() {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(build(), "Setzt einem Spieler ein Elternpaar vor die Nase.", List.of("gotobed"));
        });
    }

    private LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("go-to-bed")
                .requires(source -> source.getSender().hasPermission("gotobed.admin"))
                .executes(ctx -> usage(ctx.getSource().getSender()))
                .then(Commands.literal("now")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(this::suggestPlayers)
                                .then(Commands.argument("sentence", StringArgumentType.greedyString())
                                        .executes(this::now))))
                .then(Commands.literal("cancel")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(this::suggestPlayers)
                                .executes(this::cancel)))
                .then(Commands.literal("status")
                        .executes(this::statusAll)
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(this::suggestPlayers)
                                .executes(this::statusOne)))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(this::suggestPlayers)
                        .then(Commands.argument("time", StringArgumentType.word())
                                .suggests(this::suggestTimes)
                                .then(Commands.argument("sentence", StringArgumentType.greedyString())
                                        .executes(this::schedule))))
                .build();
    }

    private int usage(CommandSender sender) {
        sender.sendMessage(config.usage());
        return Command.SINGLE_SUCCESS;
    }

    private int schedule(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        OfflinePlayer target = resolvePlayer(ctx, sender);
        if (target == null) {
            return Command.SINGLE_SUCCESS;
        }
        String sentence = readSentence(ctx, sender);
        if (sentence == null) {
            return Command.SINGLE_SUCCESS;
        }
        var parsed = TimeParser.parse(StringArgumentType.getString(ctx, "time"), config.timezone(), Instant.now());
        if (parsed.isEmpty()) {
            sender.sendMessage(config.invalidTime());
            return Command.SINGLE_SUCCESS;
        }
        apply(sender, target, sentence, parsed.get());
        return Command.SINGLE_SUCCESS;
    }

    private int now(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        OfflinePlayer target = resolvePlayer(ctx, sender);
        if (target == null) {
            return Command.SINGLE_SUCCESS;
        }
        String sentence = readSentence(ctx, sender);
        if (sentence == null) {
            return Command.SINGLE_SUCCESS;
        }
        Instant now = Instant.now();
        var parsed = new TimeParser.Result(
                now.atZone(config.timezone()).toLocalTime().withSecond(0).withNano(0), now, true);
        apply(sender, target, sentence, parsed);
        return Command.SINGLE_SUCCESS;
    }

    private int cancel(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        OfflinePlayer target = resolvePlayer(ctx, sender);
        if (target == null) {
            return Command.SINGLE_SUCCESS;
        }
        String display = PlayerLookup.displayName(target, StringArgumentType.getString(ctx, "player"));
        if (service.cancel(target.getUniqueId()).isEmpty()) {
            sender.sendMessage(config.cancelNone(display));
            return Command.SINGLE_SUCCESS;
        }
        sender.sendMessage(config.cancelled(display));
        return Command.SINGLE_SUCCESS;
    }

    private int statusAll(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        var all = service.all();
        if (all.isEmpty()) {
            sender.sendMessage(config.statusNone());
            return Command.SINGLE_SUCCESS;
        }
        Instant now = Instant.now();
        for (Assignment assignment : all) {
            sender.sendMessage(statusLine(assignment, now));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int statusOne(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        OfflinePlayer target = resolvePlayer(ctx, sender);
        if (target == null) {
            return Command.SINGLE_SUCCESS;
        }
        String display = PlayerLookup.displayName(target, StringArgumentType.getString(ctx, "player"));
        var assignment = service.get(target.getUniqueId());
        if (assignment.isEmpty()) {
            sender.sendMessage(config.cancelNone(display));
            return Command.SINGLE_SUCCESS;
        }
        sender.sendMessage(statusLine(assignment.get(), Instant.now()));
        return Command.SINGLE_SUCCESS;
    }

    private void apply(CommandSender sender, OfflinePlayer target, String sentence, TimeParser.Result time) {
        String display = PlayerLookup.displayName(target, target.getUniqueId().toString());
        var previous = service.assign(target, display, sentence, time);
        previous.ifPresent(ignored -> sender.sendMessage(config.replaced(display)));
        if (time.immediate()) {
            sender.sendMessage(config.activated(display));
        } else {
            sender.sendMessage(config.scheduled(display, time.display(), sentence));
        }
    }

    private String statusLine(Assignment assignment, Instant now) {
        if (!service.isDue(assignment, now)) {
            return config.statusScheduled(
                    assignment.name(),
                    assignment.scheduledAt().atZone(config.timezone()).format(TimeParser.DISPLAY),
                    assignment.sentence());
        }
        if (assignment.logoutAt() != null) {
            return config.statusOffline(
                    assignment.name(), AssignmentRules.offlineMinutes(assignment, now), assignment.sentence());
        }
        return config.statusActive(assignment.name(), assignment.sentence());
    }

    private OfflinePlayer resolvePlayer(CommandContext<CommandSourceStack> ctx, CommandSender sender) {
        String name = StringArgumentType.getString(ctx, "player");
        OfflinePlayer target = PlayerLookup.findKnown(name);
        if (target == null) {
            sender.sendMessage(config.unknownPlayer(name));
            return null;
        }
        return target;
    }

    private String readSentence(CommandContext<CommandSourceStack> ctx, CommandSender sender) {
        String sentence = StringArgumentType.getString(ctx, "sentence").trim();
        if (sentence.isEmpty()) {
            sender.sendMessage(config.emptySentence());
            return null;
        }
        int max = config.maxSentenceLength();
        if (sentence.length() > max) {
            sender.sendMessage(config.sentenceTooLong(max));
            return null;
        }
        return sentence;
    }

    private CompletableFuture<Suggestions> suggestPlayers(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(remaining)) {
                builder.suggest(player.getName());
            }
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestTimes(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        int nextHour = ZonedDateTime.now(config.timezone()).getHour();
        for (int hour : new int[] {nextHour, 21, 22, 23}) {
            String suggestion = String.format("%02d:00", Math.floorMod(hour, 24));
            if (suggestion.startsWith(remaining)) {
                builder.suggest(suggestion);
            }
        }
        return builder.buildFuture();
    }
}
