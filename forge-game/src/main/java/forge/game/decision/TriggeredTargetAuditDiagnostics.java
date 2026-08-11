package forge.game.decision;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.game.zone.Zone;
import forge.util.DeterminismAuditRandom;
import forge.util.MyRandom;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Opt-in, audit-only diagnostics for FRL-02K-C2 triggered target ownership.
 *
 * <p>The recorder observes the existing triggered-ability lifecycle. It never
 * generates candidates, chooses a target, changes a TargetChoices instance, or
 * changes game state. The output is intentionally limited to typed public
 * projections and request-local runtime/entity correlation keys.</p>
 */
public final class TriggeredTargetAuditDiagnostics {
    public static final String OUTPUT_PATH_PROPERTY = "forge.triggeredTarget.auditFile";

    private static final String HEADER = "token,sequence,profile,event,source_name,source_visibility,"
            + "decider_seat,activating_player_seat,targeting_player_seat,selector_path,target_count,target_values,"
            + "target_order,target_visibility,target_zone,target_owner_seats,target_controller_seats,target_kinds,"
            + "target_origin,action_continuation,state_neutral,rng_delta,result";
    private static final String OUTPUT_PATH = outputPath();
    private static final boolean ENABLED = !OUTPUT_PATH.isBlank();
    private static final AtomicLong NEXT_TOKEN = new AtomicLong(1L);
    private static final List<String> EVENTS = ENABLED ? new ArrayList<>() : null;
    private static final Map<SpellAbility, Scope> SCOPES = new IdentityHashMap<>();

    static {
        if (ENABLED) {
            Runtime.getRuntime().addShutdownHook(new Thread(TriggeredTargetAuditDiagnostics::writeCsv,
                    "forge-triggered-target-audit"));
        }
    }

    private TriggeredTargetAuditDiagnostics() {
    }

    public static void register(final WrappedAbility wrapper) {
        if (!ENABLED || wrapper == null) {
            return;
        }
        try {
            final Profile profile = profile(wrapper);
            if (profile == null) {
                return;
            }
            final SpellAbility ability = wrapper.getWrappedAbility();
            final Card source = wrapper.getHostCard();
            final Player decider = wrapper.getDecider();
            final Game game = source == null ? null : source.getGame();
            final Scope scope = new Scope(NEXT_TOKEN.getAndIncrement(), profile, game, source, decider);
            bind(scope, wrapper);
            bind(scope, ability);
            emit(scope, "TRIGGER_CONSTRUCTED", ability, "TriggerHandler.runSingleTriggerInternal", "NONE", null);
        } catch (final RuntimeException ignored) {
            // Audit failures must never alter trigger construction.
        }
    }

    public static void recordQueued(final SpellAbility ability) {
        record(ability, "TRIGGER_QUEUED", "MagicStack.addSimultaneousStackEntry", "NONE", null);
    }

    public static void recordTargetPreparation(final SpellAbility ability, final String selectorPath,
            final boolean result) {
        record(ability, "TARGET_PREPARATION", selectorPath, Boolean.toString(result), null);
    }

    public static void recordTargetStored(final SpellAbility ability, final String selectorPath) {
        record(ability, "TARGET_STORED", selectorPath, "NONE", null);
    }

    public static void recordStoredBeforeConfirm(final WrappedAbility wrapper, final SpellAbility ability,
            final TargetChoices storedTargets) {
        if (!ENABLED || wrapper == null || ability == null) {
            return;
        }
        final Scope scope = scope(wrapper);
        if (scope == null) {
            return;
        }
        try {
            emit(scope, "TARGET_A_BEFORE_CONFIRM", ability, "PlayerControllerAi.confirmTrigger", "NONE",
                    storedTargets);
        } catch (final RuntimeException ignored) {
            // Audit failures must never alter confirmation or target restoration.
        }
    }

    public static void recordTemporaryTargetEvaluation(final WrappedAbility wrapper, final SpellAbility ability,
            final boolean result) {
        if (!ENABLED || wrapper == null || ability == null) {
            return;
        }
        final Scope scope = scope(wrapper);
        if (scope == null) {
            return;
        }
        try {
            emit(scope, "TARGET_B_EVALUATION", ability,
                    "PlayerControllerAi.confirmTrigger->AiController.brains.doTrigger", Boolean.toString(result), null);
        } catch (final RuntimeException ignored) {
            // Audit failures must never alter the AI callback path.
        }
    }

    public static void recordStackBeforePush(final SpellAbility ability) {
        record(ability, "STACK_BEFORE_PUSH", "MagicStack.add", "NONE", null);
    }

    public static void recordStackAfterPush(final SpellAbility ability) {
        record(ability, "STACK_AFTER_PUSH", "MagicStack.push", "NONE", null);
    }

    public static void recordResolveEnter(final WrappedAbility wrapper) {
        record(wrapper, "RESOLVE_ENTER", "WrappedAbility.resolve", "NONE", null);
    }

    public static void recordConfirmationEnter(final WrappedAbility wrapper) {
        record(wrapper, "CONFIRM_TRIGGER_ENTER", "WrappedAbility.resolve", "NONE", null);
    }

    public static void recordConfirmationResult(final WrappedAbility wrapper, final boolean result) {
        record(wrapper, "CONFIRM_TRIGGER_RESULT", "WrappedAbility.resolve", Boolean.toString(result), null);
    }

    public static void recordEffect(final SpellAbility ability, final boolean entering) {
        record(ability, entering ? "EFFECT_ENTER" : "EFFECT_EXIT", "ChangeZoneEffect.resolve", "NONE", null);
    }

    public static void recordResolveExit(final WrappedAbility wrapper) {
        if (!ENABLED || wrapper == null) {
            return;
        }
        final Scope scope = scope(wrapper);
        if (scope == null) {
            return;
        }
        try {
            emit(scope, "RESOLVE_EXIT", wrapper.getWrappedAbility(), "WrappedAbility.resolve", "NONE", null);
        } catch (final RuntimeException ignored) {
            // Audit failures must never alter resolution.
        } finally {
            synchronized (SCOPES) {
                SCOPES.entrySet().removeIf(entry -> entry.getValue() == scope);
            }
        }
    }

    private static void record(final SpellAbility ability, final String event, final String selectorPath,
            final String result, final TargetChoices explicitTargets) {
        if (!ENABLED || ability == null) {
            return;
        }
        final Scope scope = scope(ability);
        if (scope == null) {
            return;
        }
        try {
            emit(scope, event, ability, selectorPath, result, explicitTargets);
        } catch (final RuntimeException ignored) {
            // Audit failures must never alter the game-loop path.
        }
    }

    private static void record(final WrappedAbility wrapper, final String event, final String selectorPath,
            final String result, final TargetChoices explicitTargets) {
        if (!ENABLED || wrapper == null) {
            return;
        }
        final Scope scope = scope(wrapper);
        if (scope == null) {
            return;
        }
        try {
            emit(scope, event, wrapper.getWrappedAbility(), selectorPath, result, explicitTargets);
        } catch (final RuntimeException ignored) {
            // Audit failures must never alter the game-loop path.
        }
    }

    private static void bind(final Scope scope, final SpellAbility ability) {
        SpellAbility current = ability;
        while (current != null) {
            synchronized (SCOPES) {
                SCOPES.put(current, scope);
            }
            current = current.getSubAbility();
        }
    }

    private static Scope scope(final SpellAbility ability) {
        synchronized (SCOPES) {
            return SCOPES.get(ability);
        }
    }

    private static void emit(final Scope scope, final String event, final SpellAbility ability,
            final String selectorPath, final String result, final TargetChoices explicitTargets) {
        final String beforeState = safeState(scope.game);
        final long beforeRng = rngCount();
        final TargetDetails targets = explicitTargets == null
                ? targetDetails(ability == null ? null : ability.getTargets(), scope.decider)
                : targetDetails(explicitTargets, scope.decider);
        final String afterState = safeState(scope.game);
        final long afterRng = rngCount();
        final String stateNeutral = Boolean.toString(beforeState != null && beforeState.equals(afterState));
        final String rngDelta = beforeRng < 0L || afterRng < 0L ? "UNKNOWN" : Long.toString(afterRng - beforeRng);
        final List<String> columns = List.of(
                Long.toString(scope.token), Integer.toString(scope.sequence()), scope.profile.name(), event,
                sourceName(scope.source, scope.decider), visibility(scope.source, scope.decider),
                playerId(scope.decider), playerId(ability == null ? null : ability.getActivatingPlayer()),
                playerId(ability == null ? null : ability.getTargetingPlayer()), selectorPath,
                Integer.toString(targets.count), targets.values, targets.order, targets.visibility, targets.zones,
                targets.owners, targets.controllers, targets.kinds, targets.origin,
                Boolean.toString(PriorityActionDiagnostics.hasActiveActionContinuation()), stateNeutral, rngDelta,
                result);
        final String row = columns.stream().map(TriggeredTargetAuditDiagnostics::csv).collect(Collectors.joining(","));
        synchronized (EVENTS) {
            EVENTS.add(row);
        }
    }

    private static TargetDetails targetDetails(final TargetChoices targets, final Player viewer) {
        if (targets == null || targets.isEmpty()) {
            return TargetDetails.NONE;
        }
        final List<String> values = new ArrayList<>();
        final List<String> order = new ArrayList<>();
        final List<String> visibility = new ArrayList<>();
        final List<String> zones = new ArrayList<>();
        final List<String> owners = new ArrayList<>();
        final List<String> controllers = new ArrayList<>();
        final List<String> kinds = new ArrayList<>();
        for (final Object target : targets) {
            if (target instanceof Card card) {
                final CardProjection projection = cardProjection(card, viewer);
                values.add(projection.value);
                order.add(projection.order);
                visibility.add(projection.visibility);
                zones.add(projection.zone);
                owners.add(projection.owner);
                controllers.add(projection.controller);
                kinds.add("TARGET_CARD");
            } else if (target instanceof Player player) {
                values.add(player.getName());
                order.add("TARGET_PLAYER|" + player.getId());
                visibility.add("PUBLIC");
                zones.add("NONE");
                owners.add(playerId(player));
                controllers.add(playerId(player));
                kinds.add("TARGET_PLAYER");
            } else if (target instanceof SpellAbility stackAbility) {
                final Card source = stackAbility.getHostCard();
                final CardProjection projection = cardProjection(source, viewer);
                values.add(projection.value);
                order.add("TARGET_STACK_OBJECT|" + projection.order);
                visibility.add(projection.visibility);
                zones.add("STACK");
                owners.add(projection.owner);
                controllers.add(projection.controller);
                kinds.add("TARGET_STACK_OBJECT");
            } else {
                values.add("UNSUPPORTED");
                order.add("UNSUPPORTED");
                visibility.add("UNKNOWN");
                zones.add("UNKNOWN");
                owners.add("UNKNOWN");
                controllers.add("UNKNOWN");
                kinds.add("UNSUPPORTED");
            }
        }
        return new TargetDetails(targets.size(), String.join(";", values), String.join(";", order),
                String.join(";", visibility), String.join(";", zones), String.join(";", owners),
                String.join(";", controllers), String.join(";", kinds), "TARGET_CHOICES");
    }

    private static CardProjection cardProjection(final Card card, final Player viewer) {
        if (card == null) {
            return new CardProjection("NONE", "NONE", "NONE", "NONE", "NONE", "NONE");
        }
        final String cardVisibility = visibility(card, viewer);
        if (!"PUBLIC".equals(cardVisibility)) {
            return new CardProjection("<HIDDEN>", "HIDDEN", "HIDDEN", "HIDDEN", "HIDDEN", "HIDDEN");
        }
        final String zone = zone(card.getZone());
        final String order = "TARGET_CARD|" + zoneOrdinal(card.getZone()) + "|" + card.getId() + "|"
                + card.getGameTimestamp();
        return new CardProjection(card.getName(), order, cardVisibility, zone, playerId(card.getOwner()),
                playerId(card.getController()));
    }

    private static Profile profile(final WrappedAbility wrapper) {
        final Card source = wrapper.getHostCard();
        final Trigger trigger = wrapper.getTrigger();
        if (source == null || trigger == null || trigger.getMode() == null) {
            return null;
        }
        if ("Blood Operative".equals(source.getName()) && TriggerType.ChangesZone.equals(trigger.getMode())
                && "TrigChangeZone".equals(triggerParam(trigger, "Execute"))
                && "Battlefield".equals(triggerParam(trigger, "Destination"))) {
            return Profile.BLOOD_OPERATIVE;
        }
        return null;
    }

    private static String triggerParam(final Trigger trigger, final String key) {
        if (trigger == null || !trigger.hasParam(key)) {
            return "NONE";
        }
        final String value = trigger.getParam(key);
        return value == null || value.isBlank() ? "NONE" : value;
    }

    private static String safeState(final Game game) {
        if (game == null) {
            return null;
        }
        try {
            return ForgeStateFingerprint.canonical(game);
        } catch (final RuntimeException ignored) {
            return null;
        }
    }

    private static long rngCount() {
        try {
            return MyRandom.getRandom() instanceof DeterminismAuditRandom random ? random.getDrawCount() : -1L;
        } catch (final RuntimeException ignored) {
            return -1L;
        }
    }

    private static String sourceName(final Card source, final Player viewer) {
        return source == null ? "NONE" : "PUBLIC".equals(visibility(source, viewer)) ? source.getName() : "<HIDDEN>";
    }

    private static String visibility(final Card card, final Player viewer) {
        if (card == null) {
            return "NONE";
        }
        try {
            return viewer != null && !card.isFaceDown() && card.getView().canBeShownTo(viewer.getView())
                    ? "PUBLIC" : "HIDDEN";
        } catch (final RuntimeException ignored) {
            return "HIDDEN";
        }
    }

    private static String zone(final Zone zone) {
        return zone == null || zone.getZoneType() == null ? "NONE" : zone.getZoneType().name();
    }

    private static String zoneOrdinal(final Zone zone) {
        return zone == null || zone.getZoneType() == null ? "-1" : Integer.toString(zone.getZoneType().ordinal());
    }

    private static String playerId(final Player player) {
        return player == null ? "NONE" : Integer.toString(player.getId());
    }

    private static String outputPath() {
        final String configured = System.getProperty(OUTPUT_PATH_PROPERTY);
        return configured == null ? "" : configured;
    }

    private static String csv(final String value) {
        final String text = value == null ? "NONE" : value;
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    private static void writeCsv() {
        if (!ENABLED) {
            return;
        }
        try {
            final Path output = Path.of(OUTPUT_PATH);
            if (output.getParent() != null) {
                Files.createDirectories(output.getParent());
            }
            final List<String> snapshot;
            synchronized (EVENTS) {
                snapshot = new ArrayList<>(EVENTS);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                writer.write(HEADER);
                writer.newLine();
                for (final String event : snapshot) {
                    writer.write(event);
                    writer.newLine();
                }
            }
        } catch (final IOException | RuntimeException ignored) {
            // Audit failures must never alter the game-loop path.
        }
    }

    public enum Profile {
        BLOOD_OPERATIVE
    }

    private record Scope(long token, Profile profile, Game game, Card source, Player decider, int[] sequenceBox) {
        private Scope(final long token, final Profile profile, final Game game, final Card source,
                final Player decider) {
            this(token, profile, game, source, decider, new int[]{0});
        }

        private int sequence() {
            return sequenceBox[0]++;
        }
    }

    private record CardProjection(String value, String order, String visibility, String zone, String owner,
            String controller) {
    }

    private record TargetDetails(int count, String values, String order, String visibility, String zones,
            String owners, String controllers, String kinds, String origin) {
        private static final TargetDetails NONE = new TargetDetails(0, "NONE", "NONE", "NONE", "NONE", "NONE",
                "NONE", "NONE", "NONE");
    }
}
