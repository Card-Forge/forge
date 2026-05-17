package forge.ai.llm;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.esotericsoftware.minlog.Log;
import com.google.common.eventbus.Subscribe;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.card.Card;
import forge.game.event.GameEvent;
import forge.game.event.GameEventLandPlayed;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.event.GameEventTurnBegan;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.item.PaperCard;

/**
 * Subscribes to the game's event bus and records the opponent's public plays
 * (spells cast, lands played). It re-runs deck recognition <em>every time the
 * opponent takes an action</em> and at <em>every turn boundary</em> (so turns
 * the opponent passes without acting are also reflected), then writes the
 * guess to the game log.
 *
 * <p>Because a local LLM call is slow, calls use latest-wins coalescing: if a
 * recognition is already in flight when a new trigger arrives, a single rerun
 * is queued and fired (against the newest game state) once the current call
 * finishes. Intermediate states may be skipped, but the final state is always
 * evaluated.</p>
 *
 * <p>This class only <em>reads</em> events and <em>writes</em> to the game log;
 * it never influences the heuristic AI's decisions.</p>
 */
public final class DeckRecognitionObserver {

    /** Don't bother guessing before this turn while nothing has been seen. */
    private static final int MIN_TURN_TO_GUESS = 2;

    private final Player aiPlayer;
    private final Game game;
    private final DeckRecognitionClient client;

    private final List<Observation> observations = new CopyOnWriteArrayList<>();
    private final AtomicBoolean inFlight = new AtomicBoolean(false);
    private final AtomicBoolean rerunRequested = new AtomicBoolean(false);

    /** The AI's own deck (card names), used by the sidecar for format detection. */
    private final List<String> deckCards;

    /** Last message written to the log, to suppress identical consecutive guesses. */
    private volatile String lastPostedMessage = null;

    public DeckRecognitionObserver(final Player aiPlayer, final Game game,
                                   final DeckRecognitionClient client) {
        this.aiPlayer = aiPlayer;
        this.game = game;
        this.client = client;
        this.deckCards = extractDeckCards(aiPlayer);
    }

    /** Read the AI's own decklist (main deck card names) once, up front. */
    private static List<String> extractDeckCards(final Player aiPlayer) {
        final Set<String> names = new LinkedHashSet<>();
        try {
            final RegisteredPlayer rp = aiPlayer.getRegisteredPlayer();
            if (rp != null && rp.getDeck() != null) {
                for (final Map.Entry<PaperCard, Integer> e : rp.getDeck().getMain()) {
                    if (e.getKey() != null) {
                        names.add(e.getKey().getName());
                    }
                }
            }
        } catch (final RuntimeException ex) {
            Log.debug("DeckRecognition: could not read AI deck: " + ex.getMessage());
        }
        return new ArrayList<>(names);
    }

    /** Single handler — Guava delivers every {@link GameEvent} subtype here. */
    @Subscribe
    public void onEvent(final GameEvent ev) {
        try {
            if (ev instanceof GameEventSpellAbilityCast cast) {
                handleSpellCast(cast);
            } else if (ev instanceof GameEventLandPlayed land) {
                handleLandPlayed(land);
            } else if (ev instanceof GameEventTurnBegan) {
                // A turn boundary is itself information (e.g. a turn the
                // opponent passed without acting), so always re-evaluate.
                requestRecognition();
            }
        } catch (final RuntimeException ex) {
            // Never let observation bookkeeping disrupt the game.
            Log.debug("DeckRecognition: error handling event: " + ex.getMessage());
        }
    }

    private void handleSpellCast(final GameEventSpellAbilityCast cast) {
        if (cast.sa() == null || !cast.sa().isSpell()) {
            return;
        }
        final Player caster = cast.sa().getActivatingPlayer();
        if (caster == null || caster.equals(aiPlayer)) {
            return; // ignore the AI's own plays
        }
        final Card host = cast.sa().getHostCard();
        if (host != null) {
            observations.add(toObservation(host, "spell"));
            requestRecognition();
        }
    }

    private void handleLandPlayed(final GameEventLandPlayed land) {
        if (land.player() == null || land.player().equals(aiPlayer)) {
            return;
        }
        if (land.land() != null) {
            observations.add(toObservation(land.land(), "land"));
            requestRecognition();
        }
    }

    /**
     * Kick off a recognition call, or queue a rerun if one is already running.
     */
    private void requestRecognition() {
        if (observations.isEmpty() && game.getPhaseHandler().getTurn() < MIN_TURN_TO_GUESS) {
            return; // too early and nothing observed yet
        }
        if (inFlight.compareAndSet(false, true)) {
            fireCall();
        } else {
            rerunRequested.set(true);
        }
    }

    /** Send one request; on completion, post the guess and honor any rerun. */
    private void fireCall() {
        final RecognitionRequest request = new RecognitionRequest(
                RecognitionRequest.CLIENT,
                String.valueOf(game.getId()),
                game.getRules().getGameType().name(),
                aiPlayer.getId(),
                game.getPhaseHandler().getTurn(),
                new ArrayList<>(observations),
                deckCards);

        client.recognizeAsync(request).whenComplete((result, err) -> {
            if (err == null && result != null && result.isPresent()) {
                postGuess(result.get());
            }
            inFlight.set(false);
            if (rerunRequested.compareAndSet(true, false)) {
                requestRecognition();
            }
        });
    }

    private void postGuess(final RecognitionResult result) {
        try {
            final String message = result.toLogMessage();
            if (message.equals(lastPostedMessage)) {
                return; // unchanged since the last guess — don't spam the log
            }
            lastPostedMessage = message;
            game.getGameLog().add(GameLogEntryType.INFORMATION, message);
        } catch (final RuntimeException ex) {
            Log.debug("DeckRecognition: failed to write guess to log: " + ex.getMessage());
        }
    }

    private Observation toObservation(final Card card, final String event) {
        return new Observation(
                game.getPhaseHandler().getTurn(),
                event,
                card.getName(),
                card.getCMC(),
                colorsOf(card),
                typesOf(card));
    }

    private static List<String> colorsOf(final Card card) {
        final List<String> colors = new ArrayList<>(5);
        final var cs = card.getColor();
        if (cs.hasWhite()) { colors.add("W"); }
        if (cs.hasBlue())  { colors.add("U"); }
        if (cs.hasBlack()) { colors.add("B"); }
        if (cs.hasRed())   { colors.add("R"); }
        if (cs.hasGreen()) { colors.add("G"); }
        return colors;
    }

    private static List<String> typesOf(final Card card) {
        final List<String> types = new ArrayList<>(4);
        if (card.isLand())         { types.add("Land"); }
        if (card.isCreature())     { types.add("Creature"); }
        if (card.isArtifact())     { types.add("Artifact"); }
        if (card.isEnchantment())  { types.add("Enchantment"); }
        if (card.isPlaneswalker()) { types.add("Planeswalker"); }
        if (card.isBattle())       { types.add("Battle"); }
        if (card.isInstant())      { types.add("Instant"); }
        if (card.isSorcery())      { types.add("Sorcery"); }
        return types;
    }
}
