package forge.game.decision;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.trigger.WrappedAbility;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/** Optional lifecycle diagnostics for the explicitly supported confirmation profiles. */
public final class ConfirmationDiagnostics {
    public static final String OUTPUT_PATH_PROPERTY = DiagnosticOutputPaths.CONFIRMATION_FILE_PROPERTY;

    private static final String HEADER = "event_type,process_id,game_id,turn,phase,decider,status,reason,request_id,"
            + "legal_candidates,forced,selected,native_callback,source_name,mode,execute,profile";
    private static final String OUTPUT_PATH = outputPath();
    private static final boolean ENABLED = !OUTPUT_PATH.isBlank();
    private static final long PROCESS_ID = ProcessHandle.current().pid();
    private static final List<String> EVENTS = ENABLED ? new ArrayList<>() : null;

    static {
        if (ENABLED) {
            Runtime.getRuntime().addShutdownHook(new Thread(ConfirmationDiagnostics::writeCsv,
                    "forge-confirmation-diagnostics"));
        }
    }

    private ConfirmationDiagnostics() {
    }

    /** Captures one raw optional-trigger callback without exporting engine objects or hidden identities. */
    public static Capture capture(final Game game, final Player decider,
            final ConfirmationDecisionProvider.Generation generation, final WrappedAbility wrapper) {
        if (!ENABLED || game == null || decider == null || generation == null) {
            return Capture.disabled();
        }
        final DecisionRequest request = generation.getRequest();
        final String candidates = request == null ? "" : String.join(";",
                request.getCandidates().stream().map(LegalCandidate::getSemanticKey).toList());
        final String requestId = request == null ? "" : Long.toString(request.getRequestId());
        final String forced = request == null ? "" : Boolean.toString(request.isForced());
        final String profile = profileName(generation);
        final PublicSource source = safePublicSource(wrapper, decider);
        synchronized (EVENTS) {
            EVENTS.add(row("CALLBACK", game, decider, generation.getStatus().name(), generation.getReason(), requestId,
                    candidates, forced, "", "", source.name, source.mode, source.execute, profile));
        }
        return new Capture(game, decider, generation.getStatus(), requestId, source, profile, true);
    }

    private static String outputPath() {
        try {
            return DiagnosticOutputPaths.resolve().confirmationMetricsFile().map(Path::toString).orElse("");
        } catch (final RuntimeException ex) {
            return "";
        }
    }

    private static String row(final String eventType, final Game game, final Player decider, final String status,
            final String reason, final String requestId, final String candidates, final String forced,
            final String selected,
            final String nativeCallback, final String sourceName, final String mode, final String execute,
            final String profile) {
        return String.join(",", eventType, Long.toString(PROCESS_ID), Long.toString(game.getId()),
                Integer.toString(game.getPhaseHandler().getTurn()), csv(String.valueOf(game.getPhaseHandler().getPhase())),
                Integer.toString(decider.getId()), status, reason, requestId, csv(candidates), forced, csv(selected),
                nativeCallback, csv(sourceName), csv(mode), csv(execute), csv(profile));
    }

    private static String profileName(final ConfirmationDecisionProvider.Generation generation) {
        try {
            if (generation == null || generation.getRequest() == null
                    || generation.getRequest().getConfirmationContext() == null
                    || generation.getRequest().getConfirmationContext().getProfile() == null) {
                return "";
            }
            return generation.getRequest().getConfirmationContext().getProfile().name();
        } catch (final RuntimeException ex) {
            return "";
        }
    }

    private static PublicSource safePublicSource(final WrappedAbility wrapper, final Player decider) {
        try {
            return publicSource(wrapper, decider);
        } catch (final RuntimeException ex) {
            return PublicSource.EMPTY;
        }
    }

    private static PublicSource publicSource(final WrappedAbility wrapper, final Player decider) {
        final Card source = wrapper == null ? null : wrapper.getHostCard();
        if (source == null || source.isFaceDown() || !source.getView().canBeShownTo(decider.getView())) {
            return PublicSource.EMPTY;
        }
        return new PublicSource(source.getName(), wrapper.getTrigger() == null
                ? "" : wrapper.getTrigger().getMode().name(), wrapper.getParamOrDefault("Execute", ""));
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
            // Diagnostics must never alter the Forge callback or game-loop path.
        }
    }

    public static final class Capture {
        private final Game game;
        private final Player decider;
        private final ConfirmationDecisionProvider.Status status;
        private final String requestId;
        private final PublicSource source;
        private final String profile;
        private final boolean enabled;
        private boolean resultRecorded;

        private Capture(final Game game, final Player decider,
                final ConfirmationDecisionProvider.Status status, final String requestId, final PublicSource source,
                final String profile, final boolean enabled) {
            this.game = game;
            this.decider = decider;
            this.status = status;
            this.requestId = requestId;
            this.source = source;
            this.profile = profile;
            this.enabled = enabled;
        }

        private static Capture disabled() {
            return new Capture(null, null, null, "", PublicSource.EMPTY, "", false);
        }

        /** Records the one mapped result for an admitted request. */
        public void recordResult(final LegalCandidate selectedCandidate, final boolean nativeCallback) {
            if (!enabled || resultRecorded || status != ConfirmationDecisionProvider.Status.ADMITTED) {
                return;
            }
            resultRecorded = true;
            synchronized (EVENTS) {
                EVENTS.add(row("RESULT", game, decider, status.name(), status.name(), requestId, "", "",
                        selectedCandidate == null ? "" : selectedCandidate.getSemanticKey(),
                        Boolean.toString(nativeCallback), source.name, source.mode, source.execute, profile));
            }
        }
    }

    private static final class PublicSource {
        private static final PublicSource EMPTY = new PublicSource("", "", "");
        private final String name;
        private final String mode;
        private final String execute;

        private PublicSource(final String name, final String mode, final String execute) {
            this.name = name;
            this.mode = mode;
            this.execute = execute;
        }
    }
}
