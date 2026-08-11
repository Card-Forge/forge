package forge.game.decision;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.WrappedAbility;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Opt-in, audit-only records for boolean controller callbacks.
 *
 * <p>The recorder is disabled unless {@link #OUTPUT_PATH_PROPERTY} is set. It never
 * evaluates an AI decision, reads a prompt, exposes a raw engine object, or changes
 * game state. Hidden source cards are represented only by a visibility marker.</p>
 */
public final class BooleanCallbackAuditDiagnostics {
    public static final String OUTPUT_PATH_PROPERTY = "forge.booleanCallback.metricsFile";

    private static final String HEADER = "family,immediate_caller,owner_hint,source_name,visibility,ability_api,mode,"
            + "choice_kind,mandatory,optional_param,cost_bearing,decider_player,affected_player,active_player,"
            + "triggering_player,candidate_shape,action_continuation,provenance,native_result,card_state,trigger_type,"
            + "normalized_trigger_params,execute,live_wrapped_effect,intrinsic,spawning_ability,triggering_object_keys,"
            + "source_controller";
    private static final Set<String> DESCRIPTIVE_TRIGGER_PARAMS = Set.of("Description", "TriggerDescription",
            "SpellDescription", "StackDescription", "ChangeTypeDesc", "ValidTgtsDesc", "Execute");
    private static final String OUTPUT_PATH = outputPath();
    private static final boolean ENABLED = !OUTPUT_PATH.isBlank();
    private static final List<String> EVENTS = ENABLED ? new ArrayList<>() : null;

    static {
        if (ENABLED) {
            Runtime.getRuntime().addShutdownHook(new Thread(BooleanCallbackAuditDiagnostics::writeCsv,
                    "forge-boolean-callback-audit"));
        }
    }

    private BooleanCallbackAuditDiagnostics() {
    }

    public static void recordSpellAbility(final String family, final String callbackMethod, final String ownerHint,
            final Player decider, final SpellAbility ability, final String mode, final String choiceKind,
            final String mandatory, final String costBearing, final String candidateShape,
            final boolean result, final GameEntity affected) {
        if (!ENABLED) {
            return;
        }
        try {
            final Player triggeringPlayer = playerValue(ability == null ? null
                    : ability.getTriggeringObject(AbilityKey.Activator));
            record(family, callbackMethod, ownerHint, decider, ability == null ? null : ability.getHostCard(), ability,
                    mode, choiceKind, mandatory, optionalParam(ability), costBearing, affected, triggeringPlayer,
                    candidateShape, result, "NOT_APPLICABLE");
        } catch (final RuntimeException ignored) {
            // Audit failures must never alter the Forge callback or game-loop path.
        }
    }

    public static void recordCard(final String family, final String callbackMethod, final String ownerHint,
            final Player decider, final Card source, final SpellAbility ability, final String mode,
            final String choiceKind, final String mandatory, final String costBearing,
            final String candidateShape, final boolean result, final GameEntity affected) {
        if (!ENABLED) {
            return;
        }
        try {
            final Player triggeringPlayer = playerValue(ability == null ? null
                    : ability.getTriggeringObject(AbilityKey.Activator));
            record(family, callbackMethod, ownerHint, decider, source, ability, mode, choiceKind, mandatory,
                    optionalParam(ability), costBearing, affected, triggeringPlayer, candidateShape, result,
                    "NOT_APPLICABLE");
        } catch (final RuntimeException ignored) {
            // Audit failures must never alter the Forge callback or game-loop path.
        }
    }

    public static void recordTrigger(final Player decider, final WrappedAbility wrapper, final boolean result) {
        if (!ENABLED) {
            return;
        }
        try {
            final SpellAbility ability = wrapper == null ? null : wrapper.getWrappedAbility();
            final Player triggeringPlayer = wrapper == null ? null : triggeringPlayer(wrapper.getWrappedAbility());
            final Trigger trigger = wrapper == null ? null : wrapper.getTrigger();
            final Player affectedPlayer = wrapper == null ? null : affectedPlayer(wrapper.getWrappedAbility());
            final String provenance = wrapper == null ? "UNKNOWN" : wrapper.isIntrinsic()
                    ? "INTRINSIC" : "DERIVED_OR_COPIED";
            record("confirmTrigger", "confirmTrigger", "TRIGGER_PROCEDURAL_OR_OPTIONAL", decider,
                    wrapper == null ? null : wrapper.getHostCard(), ability,
                    wrapper == null || wrapper.getTrigger() == null ? "" : wrapper.getTrigger().getMode().name(),
                    "BOOLEAN_TRIGGER_CALLBACK", wrapper != null && wrapper.isMandatory() ? "YES" : "NO",
                    trigger == null ? optionalParam(ability) : Boolean.toString(trigger.getMapParams()
                            .containsKey("OptionalDecider")
                            || ability != null && ability.hasParam("Optional")),
                    costBearing(ability), affectedPlayer, triggeringPlayer,
                    "TRUE=PROCEED;FALSE=SUPPRESS_OR_DECLINE", result, provenance,
                    triggerDetails(wrapper, ability, decider));
        } catch (final RuntimeException ignored) {
            // Audit failures must never alter the Forge callback or game-loop path.
        }
    }

    private static void record(final String family, final String callbackMethod, final String ownerHint,
            final Player decider, final Card source, final SpellAbility ability, final String mode,
            final String choiceKind, final String mandatory, final String optionalParam,
            final String costBearing, final GameEntity affected, final Player triggeringPlayer,
            final String candidateShape, final boolean result, final String provenance) {
        record(family, callbackMethod, ownerHint, decider, source, ability, mode, choiceKind, mandatory,
                optionalParam, costBearing, affected, triggeringPlayer, candidateShape, result, provenance,
                TriggerDetails.NOT_APPLICABLE);
    }

    private static void record(final String family, final String callbackMethod, final String ownerHint,
            final Player decider, final Card source, final SpellAbility ability, final String mode,
            final String choiceKind, final String mandatory, final String optionalParam,
            final String costBearing, final GameEntity affected, final Player triggeringPlayer,
            final String candidateShape, final boolean result, final String provenance,
            final TriggerDetails triggerDetails) {
        final Game game = decider == null ? source == null ? null : source.getGame() : decider.getGame();
        final PublicSource publicSource = publicSource(source, decider);
        final String activePlayer = game == null || game.getPhaseHandler().getPlayerTurn() == null ? ""
                : Integer.toString(game.getPhaseHandler().getPlayerTurn().getId());
        final String row = String.join(",", csv(family), csv(immediateCaller(callbackMethod)), csv(ownerHint),
                csv(publicSource.name), csv(publicSource.visibility), csv(api(ability)), csv(mode), csv(choiceKind),
                csv(mandatory), csv(optionalParam), csv(costBearing), csv(playerValue(decider)),
                csv(playerValue(affected)), csv(activePlayer), csv(playerValue(triggeringPlayer)),
                csv(candidateShape), csv(Boolean.toString(PriorityActionDiagnostics.hasActiveActionContinuation())),
                csv(provenance), csv(Boolean.toString(result)), csv(triggerDetails.cardState),
                csv(triggerDetails.triggerType), csv(triggerDetails.normalizedParams), csv(triggerDetails.execute),
                csv(triggerDetails.liveEffect), csv(triggerDetails.intrinsic), csv(triggerDetails.spawningAbility),
                csv(triggerDetails.triggeringObjectKeys), csv(triggerDetails.sourceController));
        synchronized (EVENTS) {
            EVENTS.add(row);
        }
    }

    private static TriggerDetails triggerDetails(final WrappedAbility wrapper, final SpellAbility ability,
            final Player decider) {
        if (wrapper == null || wrapper.getTrigger() == null) {
            return TriggerDetails.NOT_APPLICABLE;
        }
        final PublicSource source = publicSource(wrapper.getHostCard(), decider);
        if ("HIDDEN".equals(source.visibility)) {
            return TriggerDetails.HIDDEN;
        }
        final Trigger trigger = wrapper.getTrigger();
        final String normalizedParams = trigger.getMapParams().entrySet().stream()
                .filter(entry -> !DESCRIPTIVE_TRIGGER_PARAMS.contains(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(";"));
        final String cardState = wrapper.getHostCard() == null || wrapper.getHostCard().getCurrentStateName() == null
                ? "UNKNOWN" : wrapper.getHostCard().getCurrentStateName().name();
        final String triggerType = trigger.getMode() == null ? "UNKNOWN" : trigger.getMode().name();
        final String execute = trigger.getParamOrDefault("Execute", "");
        final String liveEffect = ability == null || ability.getApi() == null ? "NONE"
                : "LIVE_API=" + ability.getApi().name();
        final String spawningAbility = trigger.getSpawningAbility() == null ? "ABSENT" : "PRESENT";
        final String triggeringObjectKeys = wrapper.getTriggeringObjects().keySet().stream()
                .map(Enum::name).sorted(Comparator.naturalOrder()).collect(Collectors.joining(";"));
        final String sourceController = wrapper.getHostCard() == null || wrapper.getHostCard().getController() == null
                ? "UNKNOWN" : Integer.toString(wrapper.getHostCard().getController().getId());
        return new TriggerDetails(cardState, triggerType, normalizedParams.isEmpty() ? "NONE" : normalizedParams,
                execute.isEmpty() ? "NONE" : execute, liveEffect, Boolean.toString(wrapper.isIntrinsic()),
                spawningAbility, triggeringObjectKeys.isEmpty() ? "NONE" : triggeringObjectKeys, sourceController);
    }

    private static Player triggeringPlayer(final SpellAbility ability) {
        if (ability == null) {
            return null;
        }
        for (final AbilityKey key : List.of(AbilityKey.Activator, AbilityKey.Player, AbilityKey.AttackingPlayer)) {
            final Object value = ability.getTriggeringObject(key);
            if (value instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    private static Player affectedPlayer(final SpellAbility ability) {
        if (ability == null) {
            return null;
        }
        for (final AbilityKey key : List.of(AbilityKey.Affected, AbilityKey.DefendingPlayer,
                AbilityKey.DamageTarget, AbilityKey.Target)) {
            final Object value = ability.getTriggeringObject(key);
            if (value instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    private static String outputPath() {
        final String configured = System.getProperty(OUTPUT_PATH_PROPERTY);
        return configured == null ? "" : configured;
    }

    private static String immediateCaller(final String callbackMethod) {
        final String diagnosticsClass = BooleanCallbackAuditDiagnostics.class.getName();
        for (final StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            final String className = frame.getClassName();
            final String methodName = frame.getMethodName();
            if (className.equals(Thread.class.getName()) || className.equals(diagnosticsClass)
                    || (className.equals("forge.ai.PlayerControllerAi") && methodName.equals(callbackMethod))
                    || (className.equals("forge.game.player.PlayerController")
                    && methodName.equals(callbackMethod))) {
                continue;
            }
            return className + "#" + methodName;
        }
        return "UNKNOWN";
    }

    private static String api(final SpellAbility ability) {
        return ability == null || ability.getApi() == null ? "" : ability.getApi().name();
    }

    private static String optionalParam(final SpellAbility ability) {
        return ability == null ? "" : Boolean.toString(ability.hasParam("Optional"));
    }

    private static String costBearing(final SpellAbility ability) {
        if (ability == null || ability.getPayCosts() == null) {
            return "UNKNOWN";
        }
        return ability.getPayCosts().isFree() ? "NO" : "YES";
    }

    private static Player playerValue(final Object value) {
        return value instanceof Player ? (Player) value : null;
    }

    private static String playerValue(final Player player) {
        return player == null ? "" : Integer.toString(player.getId());
    }

    private static String playerValue(final GameEntity entity) {
        return entity instanceof Player ? Integer.toString(((Player) entity).getId())
                : entity == null ? "" : entity.getClass().getSimpleName();
    }

    private static PublicSource publicSource(final Card source, final Player viewer) {
        if (source == null) {
            return PublicSource.EMPTY;
        }
        if (viewer == null || source.isFaceDown() || !source.getView().canBeShownTo(viewer.getView())) {
            return new PublicSource("<HIDDEN>", "HIDDEN");
        }
        return new PublicSource(source.getName(), "PUBLIC");
    }

    private static String csv(final String value) {
        final String text = value == null ? "" : value;
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
            // Audit failures must never alter the Forge callback or game-loop path.
        }
    }

    private static final class PublicSource {
        private static final PublicSource EMPTY = new PublicSource("", "NONE");
        private final String name;
        private final String visibility;

        private PublicSource(final String name, final String visibility) {
            this.name = name;
            this.visibility = visibility;
        }
    }

    private static final class TriggerDetails {
        private static final TriggerDetails NOT_APPLICABLE = new TriggerDetails("NOT_APPLICABLE", "NOT_APPLICABLE",
                "NOT_APPLICABLE", "NOT_APPLICABLE", "NOT_APPLICABLE", "NOT_APPLICABLE", "NOT_APPLICABLE",
                "NOT_APPLICABLE", "NOT_APPLICABLE");
        private static final TriggerDetails HIDDEN = new TriggerDetails("HIDDEN", "HIDDEN", "HIDDEN", "HIDDEN",
                "HIDDEN", "HIDDEN", "UNKNOWN", "HIDDEN", "HIDDEN");

        private final String cardState;
        private final String triggerType;
        private final String normalizedParams;
        private final String execute;
        private final String liveEffect;
        private final String intrinsic;
        private final String spawningAbility;
        private final String triggeringObjectKeys;
        private final String sourceController;

        private TriggerDetails(final String cardState, final String triggerType, final String normalizedParams,
                final String execute, final String liveEffect, final String intrinsic, final String spawningAbility,
                final String triggeringObjectKeys, final String sourceController) {
            this.cardState = cardState;
            this.triggerType = triggerType;
            this.normalizedParams = normalizedParams;
            this.execute = execute;
            this.liveEffect = liveEffect;
            this.intrinsic = intrinsic;
            this.spawningAbility = spawningAbility;
            this.triggeringObjectKeys = triggeringObjectKeys;
            this.sourceController = sourceController;
        }
    }
}
