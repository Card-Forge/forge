package forge.deck;

import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.Localizer;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class CommanderBracketService {
    private static final Localizer localizer = Localizer.getInstance();
    private static final CommanderBracketApiClient API_CLIENT = new CommanderBracketApiClient(CommanderBracketService::cacheRemoteResult,
            CommanderBracketService::fireUpdateListeners, CommanderBracketService::isApiEnabled);
    // Deck managers are transient; weak listeners keep this static service from retaining closed screens.
    private static final List<WeakReference<Consumer<DeckProxy>>> UPDATE_LISTENERS = new CopyOnWriteArrayList<>();

    private CommanderBracketService() {
    }

    public static int getBracket(final DeckProxy deck) {
        // Background columns must not materialize generated decks or submit Forge's bundled precons to the API.
        if (deck != null && deck.isGeneratedDeck()) {
            return 1;
        }
        if (deck != null && deck.isPreconstructedDeck()) {
            return CommanderBracketCalculator.getBracket(deck.getDeck());
        }
        return getResult(deck == null ? null : deck.getDeck(), deck, CommanderBracketApiClient.Priority.LOW, false).getBracket();
    }

    public static Object getBracketDisplay(final DeckProxy deck) {
        if (deck != null && deck.isGeneratedDeck()) {
            return "";
        }
        if (deck != null && deck.isPreconstructedDeck()) {
            return CommanderBracketCalculator.getBracket(deck.getDeck());
        }
        return getResult(deck == null ? null : deck.getDeck(), deck, CommanderBracketApiClient.Priority.LOW, true).getBracketDisplay();
    }

    public static int getBestAvailableBracket(final Deck deck) {
        final DeckContext context = DeckContext.create(deck);
        // Match tooltips may use existing results, but must never initiate network traffic.
        final CommanderBracketResult cached = context.canUseApi() ? getCachedRemoteResult(deck, context.deckHash) : null;
        return cached == null ? context.getLocalResult().getBracket() : cached.bracket();
    }

    public static String getExplanation(final Deck deck, final DeckProxy deckProxy) {
        return getResult(deck, deckProxy, CommanderBracketApiClient.Priority.HIGH, false).toExplanation();
    }

    public static boolean isPending(final Deck deck) {
        final DeckContext context = DeckContext.create(deck);
        return isApiEnabled() && context.canUseApi() && API_CLIENT.isPending(context.deckHash);
    }

    public static void addUpdateListener(final Consumer<DeckProxy> listener) {
        UPDATE_LISTENERS.add(new WeakReference<>(listener));
    }

    private static void fireUpdateListeners(final DeckProxy deckProxy) {
        for (final WeakReference<Consumer<DeckProxy>> reference : UPDATE_LISTENERS) {
            final Consumer<DeckProxy> listener = reference.get();
            if (listener == null) {
                UPDATE_LISTENERS.remove(reference);
            }
            else {
                listener.accept(deckProxy);
            }
        }
    }

    private static Result getResult(final Deck deck, final DeckProxy deckProxy, final CommanderBracketApiClient.Priority priority, final boolean columnDisplay) {
        final DeckContext context = DeckContext.create(deck);
        if (!context.canUseApi()) {
            return new Result(context, null, false, !isApiEnabled());
        }

        // Prefer this session's full result, then a saved result with a matching hash. A stale saved bracket remains useful
        // in low-priority columns while its replacement is queued; high-priority detail views wait for current analysis.
        final boolean apiEnabled = isApiEnabled();
        final CommanderBracketResult cached = getCachedRemoteResult(deck, context.deckHash);
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

        if (priority == CommanderBracketApiClient.Priority.LOW && deck.getCommanderBracket() != null) {
            if (!context.deckHash.equals(deck.getDeckHash())) {
                API_CLIENT.enqueue(deck, deckProxy, context.decklist, context.deckHash, CommanderBracketApiClient.Priority.LOW);
            }
            return new Result(context, CommanderBracketResult.fromCachedBracket(context.deckHash, deck.getCommanderBracket()), API_CLIENT.isActive(context.deckHash), false);
        }

        final boolean pending = API_CLIENT.enqueue(deck, deckProxy, context.decklist, context.deckHash, priority);
        // Columns retain a useful estimate while queued and show "..." only while their request is actively running.
        return new Result(context, null, columnDisplay ? API_CLIENT.isActive(context.deckHash) : pending, false);
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

    private static String toCanonicalCommanderBracketDecklist(final Deck deck) {
        // Card order is meaningful in the deck file, but not to the analysis; sorting makes the cache key order-independent.
        final List<String> lines = new ArrayList<>();
        for (final PaperCard commander : deck.getCommanders()) {
            lines.add("C 1 " + commander.getName());
        }
        final CardPool main = deck.getMain();
        if (main != null) {
            for (final Entry<PaperCard, Integer> entry : main) {
                lines.add("M " + entry.getValue() + " " + entry.getKey().getName());
            }
        }
        lines.sort(String.CASE_INSENSITIVE_ORDER);
        return String.join("\n", lines);
    }

    private static String hashDecklist(final String decklist) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(decklist.getBytes(StandardCharsets.UTF_8)));
        }
        catch (final NoSuchAlgorithmException e) {
            return Integer.toHexString(decklist.hashCode());
        }
    }

    private static CommanderBracketResult getCachedRemoteResult(final Deck deck, final String deckHash) {
        final CommanderBracketResult sessionCached = API_CLIENT.getCachedResult(deckHash);
        if (sessionCached != null) {
            return sessionCached;
        }
        if (deckHash.equals(deck.getDeckHash()) && deck.getCommanderBracket() != null) {
            return CommanderBracketResult.fromCachedBracket(deckHash, deck.getCommanderBracket());
        }
        return null;
    }

    private static void cacheRemoteResult(final Deck deck, final DeckProxy deckProxy, final CommanderBracketResult result) {
        if (deckProxy != null && !deckProxy.canSaveDeckMetadata()) {
            return;
        }
        deck.setCommanderBracket(result.deckHash(), result.bracket());
        if (deckProxy != null) {
            deckProxy.saveDeckMetadata();
        }
    }

    private static final class DeckContext {
        private final Deck deck;
        private final String decklist;
        private final String deckHash;
        private CommanderBracketCalculator.Result localResult;

        private DeckContext(final Deck deck, final CommanderBracketCalculator.Result localResult, final String decklist, final String deckHash) {
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
            return new DeckContext(deck, null, decklist, decklist.isEmpty() ? "" : hashDecklist(toCanonicalCommanderBracketDecklist(deck)));
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

    private record Result(DeckContext context, CommanderBracketResult remoteResult, boolean remotePending, boolean apiDisabled) {
        private int getBracket() {
            return remoteResult == null ? context.getLocalResult().getBracket() : remoteResult.bracket();
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

}
