package forge.game.decision;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.ApiType;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Opt-in, audit-only lifecycle diagnostics for the FRL-02K-C1 ChangesZone
 * trigger projection. The recorder never supplies a decision and never emits
 * a raw engine object or raw CardLKI value.
 */
public final class ChangesZoneAuditDiagnostics {
    public static final String OUTPUT_PATH_PROPERTY = "forge.changesZone.auditFile";

    private static final String HEADER = "token,sequence,profile,event,source_name,source_visibility,decider_seat,"
            + "viewer_seat,trigger_mode,origin,destination,valid_card,trigger_zones,execute,live_api,"
            + "optional_trigger,optional_effect,rule_decision_model,target_ownership_verdict,defined_context,"
            + "triggering_object_keys,source_controller,"
            + "decision_context_type,decision_context_visibility,decision_context_name,decision_context_zone,"
            + "public_context_key,card_owner_seat,card_controller_seat,card_is_creature,lki_present,lki_origin_zone,"
            + "lki_visibility,hidden_at_decision,previously_hidden,raw_card_exported,raw_lki_exported,target_count,"
            + "target_values,target_order,target_origin,trigger_result,confirm_action_result,clone_state_before,"
            + "clone_state_after,clone_state_changed,action_continuation,state_neutral,rng_delta";
    private static final String OUTPUT_PATH = outputPath();
    private static final boolean ENABLED = !OUTPUT_PATH.isBlank();
    private static final AtomicLong NEXT_TOKEN = new AtomicLong(1L);
    private static final List<String> EVENTS = ENABLED ? new ArrayList<>() : null;
    private static final ThreadLocal<Deque<Scope>> SCOPES = ThreadLocal.withInitial(ArrayDeque::new);

    static {
        if (ENABLED) {
            Runtime.getRuntime().addShutdownHook(new Thread(ChangesZoneAuditDiagnostics::writeCsv,
                    "forge-changes-zone-audit"));
        }
    }

    private ChangesZoneAuditDiagnostics() {
    }

    public static Scope begin(final WrappedAbility wrapper) {
        if (!ENABLED || wrapper == null) {
            return null;
        }
        try {
            final Profile profile = profile(wrapper);
            if (profile == null) {
                return null;
            }
            final Scope scope = new Scope(NEXT_TOKEN.getAndIncrement(), profile, facts(wrapper));
            SCOPES.get().push(scope);
            try {
                emit(scope, "TRIGGER_ENTER", null, TargetDetails.NONE, "NONE", "NONE", "NONE", "NONE", "NONE");
            } catch (final RuntimeException ignored) {
                SCOPES.get().remove(scope);
                throw ignored;
            }
            return scope;
        } catch (final RuntimeException ignored) {
            return null;
        }
    }

    public static void recordStoredTargetBeforeConfirm(final WrappedAbility wrapper, final SpellAbility ability,
            final TargetChoices storedTargets) {
        if (!ENABLED || wrapper == null || ability == null || !ability.usesTargeting()) {
            return;
        }
        final Scope scope = current();
        if (scope == null || scope.profile != Profile.BLOOD_OPERATIVE) {
            return;
        }
        try {
            emit(scope, "STORED_TARGET_BEFORE_CONFIRM", null,
                    targetDetails(storedTargets, "STORED_TARGET_BEFORE_CONFIRM", scope.facts.decider), "NONE", "NONE",
                    "NONE", "NONE", "NONE");
        } catch (final RuntimeException ignored) {
            // Audit failures must never alter the AI callback or game-loop path.
        }
    }

    public static void end(final Scope scope) {
        if (!ENABLED || scope == null) {
            return;
        }
        try {
            try {
                emit(scope, "TRIGGER_EXIT", null, TargetDetails.NONE, "NONE", "NONE", "NONE", "NONE", "NONE");
            } catch (final RuntimeException ignored) {
                // Audit failures must never alter the callback or game-loop path.
            }
        } finally {
            final Deque<Scope> stack = SCOPES.get();
            stack.remove(scope);
            if (stack.isEmpty()) {
                SCOPES.remove();
            }
        }
    }

    public static void recordTriggerResult(final Scope scope, final boolean result) {
        if (!ENABLED || scope == null) {
            return;
        }
        emit(scope, "CONFIRM_TRIGGER_RESULT", null, TargetDetails.NONE, Boolean.toString(result), "NONE",
                "NONE", "NONE", "NONE");
    }

    public static void recordAiTargetEvaluation(final WrappedAbility wrapper, final SpellAbility ability,
            final TargetChoices originalTargets) {
        if (!ENABLED) {
            return;
        }
        final Scope scope = current();
        if (scope == null || wrapper == null || ability == null || !ability.usesTargeting()) {
            return;
        }
        try {
            final TargetDetails targets = targetDetails(ability.getTargets(), "AI_CONFIRM_TRIGGER", scope.facts.decider);
            final int originalCount = originalTargets == null ? 0 : originalTargets.getTargetCards().size();
            emit(scope, "AI_TARGET_EVALUATION", null, targets, "NONE", "NONE", "NONE", "NONE",
                    Boolean.toString(targets.count > originalCount));
        } catch (final RuntimeException ignored) {
            // Audit failures must never alter the AI callback or game-loop path.
        }
    }

    public static void recordChangeZoneEffect(final SpellAbility ability, final boolean entering) {
        if (!ENABLED || ability == null) {
            return;
        }
        final Scope scope = current();
        if (scope == null || scope.profile != Profile.BLOOD_OPERATIVE || ability.getApi() != ApiType.ChangeZone) {
            return;
        }
        try {
            emit(scope, entering ? "CHANGE_ZONE_EFFECT_ENTER" : "CHANGE_ZONE_EFFECT_EXIT", null,
                    targetDetails(ability.getTargets(), "EFFECT_RESOLVE", scope.facts.decider), "NONE", "NONE", "NONE", "NONE",
                    "NONE");
        } catch (final RuntimeException ignored) {
            // Audit failures must never alter the effect or game-loop path.
        }
    }

    public static void recordCloneEffect(final SpellAbility ability, final boolean entering) {
        if (!ENABLED || ability == null) {
            return;
        }
        final Scope scope = current();
        if (scope == null || scope.profile != Profile.LAZAV || ability.getApi() != ApiType.Clone) {
            return;
        }
        try {
            emit(scope, entering ? "CLONE_EFFECT_ENTER" : "CLONE_EFFECT_EXIT", null, TargetDetails.NONE,
                    "NONE", "NONE", "NONE", "NONE", "NONE");
        } catch (final RuntimeException ignored) {
            // Audit failures must never alter the effect or game-loop path.
        }
    }

    public static void recordCloneConfirmAction(final SpellAbility ability, final Card cardToCopy,
            final boolean entered, final boolean result) {
        if (!ENABLED || ability == null) {
            return;
        }
        final Scope scope = current();
        if (scope == null || scope.profile != Profile.LAZAV || ability.getApi() != ApiType.Clone) {
            return;
        }
        try {
            emit(scope, entered ? "CLONE_CONFIRM_ACTION_ENTER" : "CLONE_CONFIRM_ACTION_RESULT", cardToCopy,
                    TargetDetails.NONE, "NONE", entered ? "NONE" : Boolean.toString(result), "NONE", "NONE",
                    "NONE");
        } catch (final RuntimeException ignored) {
            // Audit failures must never alter the effect or game-loop path.
        }
    }

    public static void recordCloneStateChanged(final SpellAbility ability, final Card target,
            final int before, final int after) {
        if (!ENABLED || ability == null || target == null) {
            return;
        }
        final Scope scope = current();
        if (scope == null || scope.profile != Profile.LAZAV || ability.getApi() != ApiType.Clone) {
            return;
        }
        try {
            emit(scope, "CLONE_STATE_CHANGED", target, TargetDetails.NONE, "NONE", "NONE", Integer.toString(before),
                    Integer.toString(after), Boolean.toString(after != before));
        } catch (final RuntimeException ignored) {
            // Audit failures must never alter the effect or game-loop path.
        }
    }

    private static void emit(final Scope scope, final String event, final Card decisionCard,
            final TargetDetails targets, final String triggerResult, final String confirmActionResult,
            final String cloneStateBefore, final String cloneStateAfter, final String cloneStateChanged) {
        final Game game = scope.facts.game;
        final String beforeState = safeState(game);
        final long beforeRng = rngCount();
        final Context context = context(decisionCard == null ? scope.facts.currentCard : decisionCard,
                scope.facts.decider);
        final String afterState = safeState(game);
        final long afterRng = rngCount();
        final String stateNeutral = Boolean.toString(beforeState != null && beforeState.equals(afterState));
        final String rngDelta = beforeRng < 0L || afterRng < 0L ? "UNKNOWN" : Long.toString(afterRng - beforeRng);
        final List<String> columns = List.of(
                Long.toString(scope.token), Integer.toString(scope.sequence++), scope.profile.name(), event,
                scope.facts.sourceName, scope.facts.sourceVisibility, playerId(scope.facts.decider),
                playerId(scope.facts.decider), scope.facts.triggerMode, scope.facts.origin, scope.facts.destination,
                scope.facts.validCard, scope.facts.triggerZones, scope.facts.execute, scope.facts.liveApi,
                Boolean.toString(scope.facts.optionalTrigger), Boolean.toString(scope.facts.optionalEffect),
                ruleDecisionModel(scope.profile), targetOwnershipVerdict(scope.profile), scope.facts.definedContext,
                scope.facts.triggeringObjectKeys, scope.facts.sourceController,
                context.type, context.visibility, context.name, context.zone, context.publicKey, context.owner,
                context.controller, context.creature, Boolean.toString(scope.facts.lkiPresent()), scope.facts.lkiOrigin(),
                scope.facts.lkiVisibility, context.hiddenAtDecision, Boolean.toString(scope.facts.previouslyHidden()), "false", "false",
                Integer.toString(targets.count), targets.values, targets.order, targets.origin, triggerResult,
                confirmActionResult, cloneStateBefore, cloneStateAfter, cloneStateChanged,
                Boolean.toString(PriorityActionDiagnostics.hasActiveActionContinuation()), stateNeutral, rngDelta);
        final String row = columns.stream().map(ChangesZoneAuditDiagnostics::csv).collect(Collectors.joining(","));
        synchronized (EVENTS) {
            EVENTS.add(row);
        }
    }

    private static Scope current() {
        final Deque<Scope> stack = SCOPES.get();
        return stack.isEmpty() ? null : stack.peek();
    }

    private static Facts facts(final WrappedAbility wrapper) {
        final SpellAbility ability = wrapper.getWrappedAbility();
        final Trigger trigger = wrapper.getTrigger();
        final Card source = wrapper.getHostCard();
        final Player decider = wrapper.getDecider();
        final Card currentCard = card(wrapper.getTriggeringObject(AbilityKey.Card));
        final Card lki = card(wrapper.getTriggeringObject(AbilityKey.CardLKI));
        final String triggerMode = trigger == null || trigger.getMode() == null ? "NONE" : trigger.getMode().name();
        return new Facts(ability == null ? null : ability.getActivatingPlayer() == null
                ? source == null ? null : source.getGame() : ability.getActivatingPlayer().getGame(), decider,
                currentCard, sourceName(source, decider), visibility(source, decider), triggerMode,
                triggerParam(trigger, "Origin"), triggerParam(trigger, "Destination"),
                triggerParam(trigger, "ValidCard"), triggerParam(trigger, "TriggerZones"),
                triggerParam(trigger, "Execute"), ability == null || ability.getApi() == null ? "NONE"
                        : ability.getApi().name(), trigger != null && (trigger.getMapParams().containsKey("OptionalDecider")
                        || ability != null && ability.hasParam("Optional")), ability != null && ability.hasParam("Optional"),
                ability == null ? "NONE" : ability.hasParam("Defined") ? ability.getParam("Defined") : "NONE",
                objectKeys(wrapper), playerId(source == null ? null : source.getController()), lki,
                visibility(lki, decider));
    }

    private static Profile profile(final WrappedAbility wrapper) {
        final Card source = wrapper.getHostCard();
        final Trigger trigger = wrapper.getTrigger();
        if (source == null || trigger == null || trigger.getMode() == null) {
            return null;
        }
        final String name = source.getName();
        final String execute = triggerParam(trigger, "Execute");
        final String destination = triggerParam(trigger, "Destination");
        if ("Blood Operative".equals(name) && TriggerType.ChangesZone.equals(trigger.getMode())
                && "TrigChangeZone".equals(execute) && "Battlefield".equals(destination)) {
            return Profile.BLOOD_OPERATIVE;
        }
        if ("Lazav, Dimir Mastermind".equals(name) && TriggerType.ChangesZone.equals(trigger.getMode())
                && "LazavCopy".equals(execute) && "Graveyard".equals(destination)) {
            return Profile.LAZAV;
        }
        return null;
    }

    private static TargetDetails targetDetails(final TargetChoices targets, final String origin,
            final Player viewer) {
        if (targets == null) {
            return new TargetDetails(0, "NONE", "NONE", origin);
        }
        final List<String> values = new ArrayList<>();
        final List<String> order = new ArrayList<>();
        for (final Object target : targets) {
            if (target instanceof Card card) {
                final Context context = context(card, viewer);
                values.add(context.visibility.equals("PUBLIC") ? context.name : "<HIDDEN>");
                order.add(context.publicKey);
            } else {
                values.add("NON_CARD");
                order.add("NON_CARD");
            }
        }
        return new TargetDetails(targets.size(), values.isEmpty() ? "NONE" : String.join(";", values),
                order.isEmpty() ? "NONE" : String.join(";", order), origin);
    }

    private static Context context(final Card card, final Player viewer) {
        if (card == null) {
            return Context.NONE;
        }
        final String cardVisibility = visibility(card, viewer);
        final boolean publicCard = "PUBLIC".equals(cardVisibility);
        final String name = publicCard ? card.getName() : "<HIDDEN>";
        final String zone = zone(card.getZone());
        final String owner = publicCard ? playerId(card.getOwner()) : "HIDDEN";
        final String controller = publicCard ? playerId(card.getController()) : "HIDDEN";
        final String creature = publicCard ? Boolean.toString(card.isCreature()) : "UNKNOWN";
        final String publicKey = publicCard ? String.join("|", name, zone, owner, controller, creature) : "HIDDEN";
        return new Context(card.isLKI() ? "CardLKI" : "Card", cardVisibility, name, zone, publicKey, owner,
                controller, creature, Boolean.toString("HIDDEN".equals(cardVisibility)));
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

    private static String triggerParam(final Trigger trigger, final String key) {
        if (trigger == null || !trigger.hasParam(key)) {
            return "NONE";
        }
        final String value = trigger.getParam(key);
        return value == null || value.isBlank() ? "NONE" : value;
    }

    private static String ruleDecisionModel(final Profile profile) {
        return profile == Profile.LAZAV
                ? "SAME_RULE_DECISION_DUPLICATED_BY_ENGINE_SURFACES"
                : "SINGLE_RULE_MAY_TRIGGER_ONLY";
    }

    private static String targetOwnershipVerdict(final Profile profile) {
        return profile == Profile.BLOOD_OPERATIVE
                ? "BLOOD_OPERATIVE_TARGET_OWNERSHIP_UNPROVEN" : "NOT_APPLICABLE";
    }

    private static String objectKeys(final WrappedAbility wrapper) {
        final Set<AbilityKey> keys = wrapper.getTriggeringObjects().keySet();
        final String result = keys.stream().map(Enum::name).sorted(Comparator.naturalOrder()).collect(Collectors.joining(";"));
        return result.isBlank() ? "NONE" : result;
    }

    private static String playerId(final Player player) {
        return player == null ? "NONE" : Integer.toString(player.getId());
    }

    private static Card card(final Object value) {
        return value instanceof Card ? (Card) value : null;
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
        BLOOD_OPERATIVE,
        LAZAV
    }

    public static final class Scope {
        private final long token;
        private final Profile profile;
        private final Facts facts;
        private int sequence;

        private Scope(final long token, final Profile profile, final Facts facts) {
            this.token = token;
            this.profile = profile;
            this.facts = facts;
        }
    }

    private record Facts(Game game, Player decider, Card currentCard, String sourceName, String sourceVisibility,
            String triggerMode, String origin, String destination, String validCard, String triggerZones,
            String execute, String liveApi, boolean optionalTrigger, boolean optionalEffect, String definedContext,
            String triggeringObjectKeys, String sourceController, Card lki, String lkiVisibility) {
        private boolean lkiPresent() {
            return lki != null;
        }

        private String lkiOrigin() {
            return lki == null ? "NONE" : zone(lki.getLastKnownZone());
        }

        private boolean previouslyHidden() {
            return "HIDDEN".equals(lkiVisibility);
        }
    }

    private record Context(String type, String visibility, String name, String zone, String publicKey, String owner,
            String controller, String creature, String hiddenAtDecision) {
        private static final Context NONE = new Context("NONE", "NONE", "NONE", "NONE", "NONE", "NONE", "NONE",
                "NONE", "UNKNOWN");
    }

    private record TargetDetails(int count, String values, String order, String origin) {
        private static final TargetDetails NONE = new TargetDetails(0, "NONE", "NONE", "NONE");
    }
}
