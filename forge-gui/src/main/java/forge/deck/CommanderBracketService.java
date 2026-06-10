package forge.deck;

import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.JsonUtil;
import forge.util.Localizer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class CommanderBracketService {
    private static final Localizer localizer = Localizer.getInstance();
    private static final CommanderBracketApiClient API_CLIENT = new CommanderBracketApiClient(
            CommanderBracketService::cacheRemoteResult, CommanderBracketService::fireUpdateListeners);
    private static final List<WeakReference<Consumer<BracketUpdate>>> UPDATE_LISTENERS = new CopyOnWriteArrayList<>();

    private CommanderBracketService() {
    }

    public static int getBracket(final DeckProxy deck) {
        if (deck != null && deck.isGeneratedDeck()) {
            return 1;
        }
        return getResult(deck == null ? null : deck.getDeck(), deck, CommanderBracketApiClient.Priority.LOW, false).getBracket();
    }

    public static Object getBracketDisplay(final DeckProxy deck) {
        if (deck != null && deck.isGeneratedDeck()) {
            return "";
        }
        return getResult(deck == null ? null : deck.getDeck(), deck, CommanderBracketApiClient.Priority.LOW, true).getBracketDisplay();
    }

    public static int getBestAvailableBracket(final Deck deck) {
        final DeckContext context = DeckContext.create(deck);
        final RemoteResult cached = context.canUseApi() ? getCachedRemoteResult(deck, context.deckHash) : null;
        return cached == null ? context.getLocalResult().getBracket() : cached.bracket;
    }

    public static String getExplanation(final Deck deck) {
        return getResult(deck, null, CommanderBracketApiClient.Priority.HIGH, false).toExplanation();
    }

    public static String getExplanation(final Deck deck, final DeckProxy deckProxy) {
        return getResult(deck, deckProxy, CommanderBracketApiClient.Priority.HIGH, false).toExplanation();
    }

    public static boolean isPending(final Deck deck) {
        final DeckContext context = DeckContext.create(deck);
        return context.canUseApi() && API_CLIENT.isPending(context.deckHash);
    }

    public static void addUpdateListener(final Consumer<BracketUpdate> listener) {
        UPDATE_LISTENERS.add(new WeakReference<>(listener));
    }

    private static void fireUpdateListeners(final BracketUpdate update) {
        for (final WeakReference<Consumer<BracketUpdate>> reference : UPDATE_LISTENERS) {
            final Consumer<BracketUpdate> listener = reference.get();
            if (listener == null) {
                UPDATE_LISTENERS.remove(reference);
            }
            else {
                listener.accept(update);
            }
        }
    }

    private static Result getResult(final Deck deck, final DeckProxy deckProxy, final CommanderBracketApiClient.Priority priority,
                                    final boolean columnDisplay) {
        final DeckContext context = DeckContext.create(deck);
        if (!context.canUseApi()) {
            return new Result(context, null, false, !isApiEnabled());
        }

        final boolean apiEnabled = isApiEnabled();
        final RemoteResult cached = !apiEnabled || !columnDisplay
                ? getCachedRemoteResult(deck, context.deckHash)
                : API_CLIENT.getCachedResult(context.deckHash);
        if (!apiEnabled) {
            return new Result(context, cached, false, true);
        }
        if (cached != null) {
            if (priority == CommanderBracketApiClient.Priority.HIGH && !cached.hasDetails()) {
                final boolean pending = API_CLIENT.enqueue(deck, deckProxy, context.decklist, context.deckHash, priority);
                return new Result(context, cached, pending, false);
            }
            return new Result(context, cached, false, false);
        }

        if (columnDisplay && deck.getCommanderBracket() != null) {
            if (!context.deckHash.equals(deck.getDeckHash())) {
                API_CLIENT.enqueue(deck, deckProxy, context.decklist, context.deckHash, CommanderBracketApiClient.Priority.LOW);
            }
            return new Result(context, RemoteResult.fromCachedBracket(context.deckHash, deck.getCommanderBracket()),
                    API_CLIENT.isActive(context.deckHash), false);
        }

        final boolean pending = API_CLIENT.enqueue(deck, deckProxy, context.decklist, context.deckHash, priority);
        return new Result(context, null, columnDisplay ? API_CLIENT.isActive(context.deckHash) : pending,
                false);
    }

    private static boolean isApiEnabled() {
        return FModel.getPreferences().getPrefBoolean(FPref.UI_USE_COMMANDER_BRACKET_API);
    }

    private static String toCommanderBracketDecklist(final Deck deck) {
        final StringBuilder sb = new StringBuilder();
        final List<PaperCard> commanders = deck.getCommanders();
        if (!commanders.isEmpty()) {
            sb.append("// Commander\n");
            for (final PaperCard commander : commanders) {
                sb.append("1 ").append(commander.getName()).append("\n");
            }
            sb.append("\n");
        }

        final CardPool main = deck.getMain();
        if (main == null || main.isEmpty()) {
            return "";
        }
        for (final Entry<PaperCard, Integer> entry : main) {
            sb.append(entry.getValue()).append(" ").append(entry.getKey().getName()).append("\n");
        }
        return sb.toString().trim();
    }

    private static String hashDecklist(final String decklist) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(decklist.getBytes(StandardCharsets.UTF_8));
            final StringBuilder sb = new StringBuilder();
            for (final byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
        catch (final NoSuchAlgorithmException e) {
            return Integer.toHexString(decklist.hashCode());
        }
    }

    private static RemoteResult getCachedRemoteResult(final Deck deck, final String deckHash) {
        final RemoteResult sessionCached = API_CLIENT.getCachedResult(deckHash);
        if (sessionCached != null) {
            return sessionCached;
        }
        if (deckHash.equals(deck.getDeckHash()) && deck.getCommanderBracket() != null) {
            return RemoteResult.fromCachedBracket(deckHash, deck.getCommanderBracket());
        }
        return null;
    }

    private static void cacheRemoteResult(final Deck deck, final DeckProxy deckProxy, final RemoteResult result) {
        deck.setCommanderBracket(result.deckHash, result.bracket);
        if (deckProxy != null) {
            deckProxy.saveDeckMetadata();
        }
    }

    private static final class DeckContext {
        private final Deck deck;
        private final String decklist;
        private final String deckHash;
        private CommanderBracketCalculator.Result localResult;

        private DeckContext(final Deck deck, final CommanderBracketCalculator.Result localResult,
                            final String decklist, final String deckHash) {
            this.deck = deck;
            this.localResult = localResult;
            this.decklist = decklist;
            this.deckHash = deckHash;
        }

        private static DeckContext create(final Deck deck) {
            if (deck == null) {
                return new DeckContext(null, CommanderBracketCalculator.calculate((Deck)null), "", "");
            }
            final String decklist = toCommanderBracketDecklist(deck);
            return new DeckContext(deck, null, decklist, decklist.isEmpty() ? "" : hashDecklist(decklist));
        }

        private boolean canUseApi() {
            return !deckHash.isEmpty();
        }

        private CommanderBracketCalculator.Result getLocalResult() {
            if (localResult == null) {
                localResult = CommanderBracketCalculator.calculate(deck);
            }
            return localResult;
        }
    }

    private static final class Result {
        private final DeckContext context;
        private final RemoteResult remoteResult;
        private final boolean remotePending;
        private final boolean apiDisabled;

        private Result(final DeckContext context, final RemoteResult remoteResult,
                       final boolean remotePending, final boolean apiDisabled) {
            this.context = context;
            this.remoteResult = remoteResult;
            this.remotePending = remotePending;
            this.apiDisabled = apiDisabled;
        }

        private int getBracket() {
            return remoteResult == null ? context.getLocalResult().getBracket() : remoteResult.bracket;
        }

        private Object getBracketDisplay() {
            return remotePending ? "..." : getBracket();
        }

        private String toExplanation() {
            final StringBuilder sb = new StringBuilder();
            if (apiDisabled) {
                if (remoteResult != null) {
                    if (remoteResult.hasDetails()) {
                        remoteResult.appendExplanation(sb);
                    }
                    else {
                        remoteResult.appendEstimate(sb);
                    }
                    sb.append("\n\n");
                }
                sb.append(localizer.getMessage("lblCommanderBracketEnableApi")).append("\n\n");
            }
            else if (remoteResult != null) {
                remoteResult.appendExplanation(sb);
                if (remotePending) {
                    sb.append("\n").append(localizer.getMessage("lblCommanderBracketRefreshingDetails"));
                }
                sb.append("\n\n");
            }
            else if (remotePending) {
                sb.append(localizer.getMessage("lblCommanderBracketAnalysisQueued")).append("\n\n");
            }
            sb.append(context.getLocalResult().toExplanation());
            return sb.toString();
        }
    }

    public static final class BracketUpdate {
        private final String deckHash;

        BracketUpdate(final String deckHash) {
            this.deckHash = deckHash;
        }

        public String getDeckHash() {
            return deckHash;
        }
    }

    static final class RemoteResult {
        private final String deckHash;
        private final int bracket;
        private final String bracketName;
        private final String bracketDescription;
        private final String bracketReason;
        private final String bracketNarrative;
        private final String confidence;
        private final String confidenceReason;
        private final String estimatedWinTurn;
        private final int totalGameChangers;
        private final int fastManaCount;
        private final int tutorCount;
        private final int comboCount;
        private final int cardsFound;
        private final int totalCards;
        private final String attributionLabel;

        private RemoteResult(final String deckHash, final int bracket) {
            this(deckHash, bracket, "", "", "", "", "", "", "", 0, 0, 0, 0, 0, 0,
                    localizer.getMessage("lblCommanderBracketAttribution"));
        }

        private RemoteResult(final String deckHash, final int bracket, final String bracketName,
                             final String bracketDescription, final String bracketReason, final String bracketNarrative,
                             final String confidence, final String confidenceReason, final String estimatedWinTurn,
                             final int totalGameChangers, final int fastManaCount, final int tutorCount,
                             final int comboCount, final int cardsFound, final int totalCards,
                             final String attributionLabel) {
            this.deckHash = deckHash;
            this.bracket = bracket;
            this.bracketName = bracketName;
            this.bracketDescription = bracketDescription;
            this.bracketReason = bracketReason;
            this.bracketNarrative = bracketNarrative;
            this.confidence = confidence;
            this.confidenceReason = confidenceReason;
            this.estimatedWinTurn = estimatedWinTurn;
            this.totalGameChangers = totalGameChangers;
            this.fastManaCount = fastManaCount;
            this.tutorCount = tutorCount;
            this.comboCount = comboCount;
            this.cardsFound = cardsFound;
            this.totalCards = totalCards;
            this.attributionLabel = attributionLabel;
        }

        static RemoteResult fromResponse(final String deckHash, final String response) throws IOException {
            final Object parsed = JsonUtil.parse(response);
            if (!(parsed instanceof Map<?, ?> root)) {
                throw new IOException("Unexpected CommanderBracket response.");
            }

            final Map<?, ?> bracketAnalysis = asMap(root.get("bracket_analysis"));
            final Map<?, ?> deckStats = asMap(root.get("deck_stats"));
            final int bracket = normalizeBracket(coerceBracket(root, bracketAnalysis));
            final String winTurn = firstString(root, bracketAnalysis, "estimated_win_turn");

            return new RemoteResult(
                    deckHash,
                    bracket,
                    sanitizeBracketLabel(firstString(root, bracketAnalysis, "bracket_name"), bracket),
                    sanitizeBracketLabel(firstString(root, bracketAnalysis, "bracket_description"), bracket),
                    firstString(root, bracketAnalysis, "bracket_reason"),
                    firstString(root, bracketAnalysis, "bracket_narrative"),
                    firstString(root, bracketAnalysis, "bracket_confidence"),
                    sanitizeBracketLabel(firstString(root, bracketAnalysis, "confidence_reason"), bracket),
                    winTurn,
                    intValue(bracketAnalysis.get("total_game_changers")),
                    intValue(bracketAnalysis.get("fast_mana_count")),
                    intValue(bracketAnalysis.get("tutor_count")),
                    countCombos(root),
                    intValue(deckStats.get("cards_found")),
                    intValue(deckStats.get("total_cards")),
                    localizer.getMessage("lblCommanderBracketAttribution"));
        }

        private static RemoteResult fromCachedBracket(final String deckHash, final int bracket) {
            return new RemoteResult(deckHash, bracket);
        }

        private void appendExplanation(final StringBuilder sb) {
            appendEstimate(sb);
            if (StringUtils.isNotBlank(bracketName)) {
                sb.append(" - ").append(bracketName);
            }
            sb.append("\n");
            appendLine(sb, localizer.getMessage("lblCommanderBracketDescriptionLabel"), bracketDescription);
            appendLine(sb, localizer.getMessage("lblCommanderBracketReasonLabel"), bracketReason);
            appendLine(sb, localizer.getMessage("lblCommanderBracketNarrativeLabel"), bracketNarrative);
            appendLine(sb, localizer.getMessage("lblCommanderBracketEstimatedWinTurnLabel"), estimatedWinTurn);
            appendLine(sb, localizer.getMessage("lblCommanderBracketConfidenceLabel"), confidence);
            appendLine(sb, localizer.getMessage("lblCommanderBracketConfidenceReasonLabel"), confidenceReason);
            if (cardsFound > 0 || totalCards > 0) {
                sb.append(localizer.getMessage("lblCommanderBracketCardsFound", cardsFound, totalCards)).append("\n");
            }
            if (hasSignalDetails()) {
                sb.append(localizer.getMessage("lblCommanderBracketSignals",
                        totalGameChangers, fastManaCount, tutorCount, comboCount)).append("\n\n");
            }
            sb.append(attributionLabel);
        }

        private void appendEstimate(final StringBuilder sb) {
            sb.append(localizer.getMessage("lblCommanderBracketAppEstimate", bracket));
        }

        private boolean hasSignalDetails() {
            return totalGameChangers > 0 || fastManaCount > 0 || tutorCount > 0 || comboCount > 0;
        }

        private boolean hasDetails() {
            return StringUtils.isNotBlank(bracketName)
                    || StringUtils.isNotBlank(bracketDescription)
                    || StringUtils.isNotBlank(bracketReason)
                    || StringUtils.isNotBlank(bracketNarrative)
                    || StringUtils.isNotBlank(confidence)
                    || StringUtils.isNotBlank(confidenceReason)
                    || StringUtils.isNotBlank(estimatedWinTurn)
                    || cardsFound > 0
                    || totalCards > 0
                    || hasSignalDetails();
        }

        private static void appendLine(final StringBuilder sb, final String label, final String value) {
            if (StringUtils.isNotBlank(value)) {
                sb.append(label).append(": ").append(value).append("\n");
            }
        }
    }

    private static Map<?, ?> asMap(final Object value) {
        return value instanceof Map<?, ?> map ? map : Collections.emptyMap();
    }

    private static String firstString(final Map<?, ?> root, final Map<?, ?> nested, final String key) {
        final String fromRoot = stringValue(root.get(key));
        return StringUtils.isNotBlank(fromRoot) ? fromRoot : stringValue(nested.get(key));
    }

    private static int firstInt(final Map<?, ?> values, final String... keys) {
        for (final String key : keys) {
            final int value = intValue(values.get(key));
            if (value > 0) {
                return value;
            }
        }
        return 0;
    }

    private static int coerceBracket(final Map<?, ?> root, final Map<?, ?> bracketAnalysis) {
        final int nestedEstimate = firstInt(bracketAnalysis, "final_bracket", "bracket", "overall_bracket", "estimated_bracket");
        if (nestedEstimate > 0) {
            return nestedEstimate;
        }
        final int rootEstimate = firstInt(root, "final_bracket", "bracket", "overall_bracket", "estimated_bracket");
        if (rootEstimate > 0) {
            return rootEstimate;
        }
        final int rootDeckBracket = intValue(root.get("deck_bracket"));
        if (rootDeckBracket > 0) {
            return rootDeckBracket;
        }
        return intValue(bracketAnalysis.get("deck_bracket"));
    }

    private static int normalizeBracket(final int bracket) {
        return Math.max(1, Math.min(5, bracket));
    }

    private static String sanitizeBracketLabel(final String value, final int bracket) {
        final int labeledBracket = findLabeledBracket(value);
        return labeledBracket > 0 && labeledBracket != bracket ? "" : value;
    }

    private static int findLabeledBracket(final String value) {
        if (StringUtils.isBlank(value)) {
            return 0;
        }
        final String lowerValue = value.toLowerCase(Locale.ROOT);
        for (int i = 1; i <= 5; i++) {
            if (lowerValue.contains("bracket " + i)) {
                return i;
            }
        }
        return 0;
    }

    private static int countCombos(final Map<?, ?> root) {
        final Object combos = asMap(asMap(root.get("ipom_analysis")).get("combos")).get("detected_combos");
        return combos instanceof List<?> list ? list.size() : 0;
    }

    private static String stringValue(final Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Number number) {
            return number.toString();
        }
        return String.valueOf(value);
    }

    private static int intValue(final Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            }
            catch (final NumberFormatException ignored) {
            }
        }
        return 0;
    }

}
