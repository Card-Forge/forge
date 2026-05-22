package forge.ai.llm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.eventbus.Subscribe;

import forge.ai.AiController;
import forge.ai.AiProps;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.card.CardView;
import forge.game.spellability.SpellAbility;
import forge.game.event.GameEvent;
import forge.game.event.GameEventLandPlayed;
import forge.game.event.GameEventPlayerLivesChanged;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.event.GameEventTurnBegan;
import forge.game.event.GameEventZone;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import org.tinylog.Logger;

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
    private final AiController aiController;

    private final List<Observation> observations = new CopyOnWriteArrayList<>();
    private final AtomicBoolean inFlight = new AtomicBoolean(false);
    private final AtomicBoolean rerunRequested = new AtomicBoolean(false);

    /** The AI's own deck (card names), used by the sidecar for format detection. */
    private final List<String> deckCards;

    /** Last message written to the log, to suppress identical consecutive guesses. */
    private volatile String lastPostedMessage = null;

    /** AI-player turn count. This is intentionally separate from the global game turn. */
    private volatile int aiTurnNumber = 0;

    public DeckRecognitionObserver(final Player aiPlayer, final Game game,
                                   final DeckRecognitionClient client,
                                   final AiController aiController) {
        this.aiPlayer = aiPlayer;
        this.game = game;
        this.client = client;
        this.aiController = aiController;
        this.deckCards = extractDeckCards(aiPlayer);
        this.aiTurnNumber = Math.max(0, aiPlayer.getTurn());
        identifyOwnArchetypeUpFront();
    }

    /**
     * Heuristically identify the AI's own archetype <em>before the game
     * begins</em>, using the sidecar's deterministic decklist matcher (no LLM
     * call). The result is posted to the game log and primes the sidecar's
     * per-game cache so the first {@code /recognize} call reuses it.
     */
    private void identifyOwnArchetypeUpFront() {
        if (deckCards.isEmpty()) {
            return;
        }
        final OwnArchetypeRequest request = new OwnArchetypeRequest(
                String.valueOf(game.getId()),
                game.getRules().getGameType().name(),
                deckCards);
        client.identifyOwnArchetypeAsync(request).whenComplete((result, err) -> {
            try {
                if (err != null || result == null || result.isEmpty()) {
                    Logger.debug("DeckRecognition: pre-game own-archetype lookup failed");
                    return;
                }
                final OwnArchetypeResult own = result.get();
                Logger.info("DeckRecognition: own archetype (heuristic) = '"
                        + own.ownArchetype() + "' guide=" + own.guideSource());
                final String label = own.isKnown() ? own.ownArchetype() : "Unknown";
                final String msg = "AI is piloting: " + label
                        + (own.guideSource() == null || own.guideSource().isEmpty()
                                ? "" : " (" + own.guideSource() + ")");
                if (!msg.equals(lastPostedMessage)) {
                    lastPostedMessage = msg;
                    game.getGameLog().add(GameLogEntryType.INFORMATION, msg);
                }
            } catch (final RuntimeException ex) {
                Logger.debug("DeckRecognition: pre-game lookup post failed: " + ex.getMessage());
            }
        });
    }

    /** Read the AI's own decklist (main deck card names) once, up front. */
    static List<String> extractDeckCards(final Player aiPlayer) {
        final List<String> names = new ArrayList<>();
        try {
            final RegisteredPlayer rp = aiPlayer.getRegisteredPlayer();
            if (rp != null && rp.getDeck() != null) {
                for (final Map.Entry<PaperCard, Integer> e : rp.getDeck().getMain()) {
                    if (e.getKey() != null) {
                        for (int i = 0; i < Math.max(1, e.getValue()); i++) {
                            names.add(e.getKey().getName());
                        }
                    }
                }
            }
        } catch (final RuntimeException ex) {
            Logger.debug("DeckRecognition: could not read AI deck: " + ex.getMessage());
        }
        return names;
    }

    /** @return full AI main-deck card names, preserving duplicate copies. */
    List<String> deckCards() {
        return List.copyOf(deckCards);
    }

    /** Single handler — Guava delivers every {@link GameEvent} subtype here. */
    @Subscribe
    public void onEvent(final GameEvent ev) {
        try {
            if (ev instanceof GameEventSpellAbilityCast cast) {
                handleSpellCast(cast);
            } else if (ev instanceof GameEventLandPlayed land) {
                handleLandPlayed(land);
            } else if (ev instanceof GameEventTurnBegan turnBegan) {
                if (turnBegan.turnOwner() != null && turnBegan.turnOwner().equals(aiPlayer.getView())) {
                    aiTurnNumber = Math.max(aiTurnNumber + 1, aiPlayer.getTurn() + 1);
                }
                // A turn boundary is itself information (e.g. a turn the
                // opponent passed without acting), so always re-evaluate.
                requestRecognition();
            } else if (ev instanceof GameEventPlayerLivesChanged
                    || ev instanceof GameEventZone) {
                // Life changes and zone churn (cards entering / leaving a zone)
                // can flip the role assessment without spell/land traffic.
                requestRecognition();
            }
        } catch (final RuntimeException ex) {
            // Never let observation bookkeeping disrupt the game.
            Logger.debug("DeckRecognition: error handling event: " + ex.getMessage());
        }
    }

    private void handleSpellCast(final GameEventSpellAbilityCast cast) {
        if (cast.sa() == null || !cast.sa().isSpell()) {
            return;
        }
        final PlayerView caster = cast.si() == null ? null : cast.si().getActivatingPlayer();
        if (caster == null || caster.equals(aiPlayer.getView())) {
            return; // ignore the AI's own plays
        }
        final Card host = resolveCard(cast.sa().getHostCard());
        if (host != null) {
            observations.add(toObservation(host, "spell"));
            requestRecognition();
        }
    }

    private void handleLandPlayed(final GameEventLandPlayed land) {
        if (land.player() == null || land.player().equals(aiPlayer.getView())) {
            return;
        }
        final Card landCard = resolveCard(land.land());
        if (landCard != null) {
            observations.add(toObservation(landCard, "land"));
            requestRecognition();
        }
    }

    /** Resolve a {@link CardView} carried by a game event back to its model card. */
    private Card resolveCard(final CardView view) {
        return view == null ? null : game.findByView(view);
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

    /** Collect card names in a player's zone. Returns an empty list on failure. */
    private List<String> zoneNames(final Player p, final ZoneType zone) {
        try {
            final List<String> names = new ArrayList<>();
            for (final Card c : p.getCardsIn(zone)) {
                if (c != null && c.getName() != null) {
                    names.add(c.getName());
                }
            }
            return names;
        } catch (final RuntimeException ex) {
            return List.of();
        }
    }

    /**
     * Detailed view of a player's battlefield: name, P/T, types, tapped state.
     * Returns an empty list on failure or when the player has no permanents.
     */
    private List<BoardCard> boardDetails(final Player p) {
        if (p == null) {
            return List.of();
        }
        try {
            final List<BoardCard> out = new ArrayList<>();
            for (final Card c : p.getCardsIn(ZoneType.Battlefield)) {
                if (c == null || c.getName() == null) {
                    continue;
                }
                final List<String> types = new ArrayList<>();
                try {
                    if (c.isCreature())      { types.add("Creature"); }
                    if (c.isArtifact())      { types.add("Artifact"); }
                    if (c.isEnchantment())   { types.add("Enchantment"); }
                    if (c.isLand())          { types.add("Land"); }
                    if (c.isPlaneswalker())  { types.add("Planeswalker"); }
                    if (c.isBattle())        { types.add("Battle"); }
                } catch (final RuntimeException ignored) {
                    // type query failed — skip types but still send the card
                }
                final boolean isCreature = !types.isEmpty() && types.contains("Creature");
                final boolean isPlaneswalker = !types.isEmpty() && types.contains("Planeswalker");
                final Integer power = isCreature ? safeInt(() -> c.getNetPower()) : null;
                final Integer tough;
                if (isCreature) {
                    tough = safeInt(() -> c.getNetToughness());
                } else if (isPlaneswalker) {
                    // Stash loyalty in `toughness` so Python sees it as the
                    // permanent's "stat" without expanding the wire format.
                    tough = safeInt(() -> c.getCurrentLoyalty());
                } else {
                    tough = null;
                }
                final boolean tapped;
                try { tapped = c.isTapped(); } catch (final RuntimeException ex) {
                    out.add(new BoardCard(c.getName(), power, tough, types, isCreature, false));
                    continue;
                }
                out.add(new BoardCard(c.getName(), power, tough, types, isCreature, tapped));
            }
            return out;
        } catch (final RuntimeException ex) {
            return List.of();
        }
    }

    /** Suppress exceptions from a P/T getter and return null on failure. */
    private static Integer safeInt(final java.util.function.IntSupplier f) {
        try {
            return f.getAsInt();
        } catch (final RuntimeException ex) {
            return null;
        }
    }

    /** Number of cards in a player's zone; -1 if unknown. */
    private int zoneSize(final Player p, final ZoneType zone) {
        if (p == null) {
            return 0;
        }
        try {
            return p.getCardsIn(zone).size();
        } catch (final RuntimeException ex) {
            return 0;
        }
    }

    /** Color letters seen across an opponent's lands (whether tapped or not).
     *  This tracks which colors the opponent has access to, used for color-screw
     *  detection on the sidecar side. */
    private List<String> opponentLandColors(final Player opp) {
        final Set<String> colors = new LinkedHashSet<>();
        if (opp == null) {
            return List.of();
        }
        try {
            for (final Card land : opp.getCardsIn(ZoneType.Battlefield)) {
                if (land == null || !land.isLand()) {
                    continue;
                }
                final var cs = land.getRules() != null ? land.getRules().getColorIdentity() : null;
                if (cs == null) {
                    colors.add("C");
                    continue;
                }
                if (cs.hasWhite()) { colors.add("W"); }
                if (cs.hasBlue())  { colors.add("U"); }
                if (cs.hasBlack()) { colors.add("B"); }
                if (cs.hasRed())   { colors.add("R"); }
                if (cs.hasGreen()) { colors.add("G"); }
                if (cs.isColorless()) { colors.add("C"); }
            }
        } catch (final RuntimeException ex) {
            // fall through
        }
        return new ArrayList<>(colors);
    }

    /**
     * The opponent's open mana picture, built from every untapped permanent
     * that can produce mana (lands, rocks, dorks). Untapped sources are mana
     * they can tap right now (open interaction); tapped sources are mana
     * already committed this turn.
     *
     * <p>For each untapped source we record the set of colors it can produce
     * (W/U/B/R/G/C) so the sidecar can reason about which color combinations
     * are reachable — e.g. an untapped Hallowed Fountain contributes
     * {@code [W, U]} and a Mana Confluence {@code [W, U, B, R, G]}. We do not
     * enumerate the combinations here; the per-source options let the strategist
     * work them out alongside its read on the opponent's hand.</p>
     */
    private ManaPicture opponentManaPicture(final Player opp) {
        final List<List<String>> untappedSources = new ArrayList<>();
        int spent = 0;
        if (opp == null) {
            return new ManaPicture(0, 0, untappedSources);
        }
        try {
            for (final Card c : opp.getCardsIn(ZoneType.Battlefield)) {
                if (c == null || c.getManaAbilities().isEmpty()) {
                    continue;
                }
                if (c.isTapped()) {
                    spent++;
                    continue;
                }
                final List<String> colors = producibleColors(c);
                if (!colors.isEmpty()) {
                    untappedSources.add(colors);
                }
            }
        } catch (final RuntimeException ex) {
            // fall through with whatever we collected
        }
        return new ManaPicture(untappedSources.size(), spent, untappedSources);
    }

    /** Colors (W/U/B/R/G/C) a permanent's mana abilities can produce, reusing
     *  Forge's own production logic so "any color" / combo / reflected mana are
     *  all handled. Returns an empty list when none can be determined. */
    private static List<String> producibleColors(final Card c) {
        final Set<String> names = new LinkedHashSet<>();
        for (final SpellAbility ab : c.getManaAbilities()) {
            if (ab.getApi() == ApiType.ManaReflected) {
                names.addAll(CardUtil.getReflectableManaColors(ab));
            } else {
                CardUtil.canProduce(6, ab, names);
            }
        }
        final List<String> letters = new ArrayList<>(names.size());
        for (final String name : names) {
            letters.add(MagicColor.toShortString(name));
        }
        return letters;
    }

    /** Snapshot of the opponent's mana: count of untapped sources, count of
     *  tapped (spent) sources, and per-untapped-source producible colors. */
    private record ManaPicture(int available, int spent, List<List<String>> untappedSources) { }

    /**
     * Coarse decision context derived from the current phase. This reactive
     * observer isn't tied to a specific AI choice, so it reports {@code
     * "combat"} during combat steps (where reading the opponent's open mana
     * matters most) and {@code "priority"} otherwise. The sidecar uses this to
     * decide when to spend the extra strategist LLM call.
     */
    private String currentDecisionType() {
        try {
            final String phase = game.getPhaseHandler().getPhase().name();
            return phase.startsWith("COMBAT") ? "combat" : "priority";
        } catch (final RuntimeException ex) {
            return "";
        }
    }

    /** First opposing player relative to the AI, or {@code null}. */
    private Player firstOpponent() {
        try {
            for (final Player p : game.getPlayers()) {
                if (p != null && !p.equals(aiPlayer)) {
                    return p;
                }
            }
        } catch (final RuntimeException ex) {
            // ignore
        }
        return null;
    }

    /** Life totals keyed "ai"/"human" so the sidecar can read them generically. */
    private Map<String, Integer> buildLifeTotals(final Player opponent) {
        final Map<String, Integer> out = new LinkedHashMap<>();
        try {
            out.put("ai", aiPlayer.getLife());
            if (opponent != null) {
                out.put("human", opponent.getLife());
            }
        } catch (final RuntimeException ex) {
            return Map.of();
        }
        return out;
    }

    /** Current phase name string, or empty when unavailable. */
    private String currentPhaseName() {
        try {
            return game.getPhaseHandler().getPhase().name();
        } catch (final RuntimeException ex) {
            return "";
        }
    }

    /** Color letters (W/U/B/R/G/C) producible by the AI's untapped lands. */
    private List<String> availableManaColors() {
        final Set<String> colors = new LinkedHashSet<>();
        try {
            for (final Card land : aiPlayer.getCardsIn(ZoneType.Battlefield)) {
                if (land == null || !land.isLand() || land.isTapped()) {
                    continue;
                }
                final var cs = land.getRules() != null ? land.getRules().getColorIdentity() : null;
                if (cs == null) {
                    colors.add("C");
                    continue;
                }
                if (cs.hasWhite()) { colors.add("W"); }
                if (cs.hasBlue())  { colors.add("U"); }
                if (cs.hasBlack()) { colors.add("B"); }
                if (cs.hasRed())   { colors.add("R"); }
                if (cs.hasGreen()) { colors.add("G"); }
                if (cs.isColorless()) { colors.add("C"); }
            }
        } catch (final RuntimeException ex) {
            // fall through with whatever we have
        }
        return new ArrayList<>(colors);
    }

    /** Build the personality map from AI profile properties, or empty map. */
    private Map<String, Object> buildPersonality() {
        try {
            final Map<String, Object> p = new java.util.LinkedHashMap<>();
            p.put("play_aggro", aiController.getBoolProperty(AiProps.PLAY_AGGRO));
            return p;
        } catch (final Exception ex) {
            return Map.of();
        }
    }

    /** Send one request; on completion, post the guess and honor any rerun. */
    private void fireCall() {
        final List<String> hand = zoneNames(aiPlayer, ZoneType.Hand);
        final List<String> ownBoard = zoneNames(aiPlayer, ZoneType.Battlefield);
        final List<String> ownGy = zoneNames(aiPlayer, ZoneType.Graveyard);
        final Player opponent = firstOpponent();
        final List<String> oppBoard = opponent == null ? List.of() : zoneNames(opponent, ZoneType.Battlefield);
        final List<String> oppGy = opponent == null ? List.of() : zoneNames(opponent, ZoneType.Graveyard);
        final Map<String, Integer> lifeTotals = buildLifeTotals(opponent);
        final String phase = currentPhaseName();
        final List<String> availableMana = availableManaColors();
        final int aiHandSize = zoneSize(aiPlayer, ZoneType.Hand);
        final int oppHandSize = zoneSize(opponent, ZoneType.Hand);
        final int aiLibSize = zoneSize(aiPlayer, ZoneType.Library);
        final int oppLibSize = zoneSize(opponent, ZoneType.Library);
        final List<BoardCard> ownBoardDetails = boardDetails(aiPlayer);
        final List<BoardCard> oppBoardDetails = boardDetails(opponent);
        final List<String> oppManaColors = opponentLandColors(opponent);
        final ManaPicture oppMana = opponentManaPicture(opponent);
        final String decisionType = currentDecisionType();

        final RecognitionRequest request = new RecognitionRequest(
                RecognitionRequest.CLIENT,
                String.valueOf(game.getId()),
                game.getRules().getGameType().name(),
                aiPlayer.getId(),
                currentAiTurnNumber(),
                new ArrayList<>(observations),
                deckCards,
                hand,
                ownBoard,
                oppBoard,
                ownGy,
                oppGy,
                lifeTotals,
                phase,
                availableMana,
                buildPersonality(),
                aiHandSize,
                oppHandSize,
                aiLibSize,
                oppLibSize,
                ownBoardDetails,
                oppBoardDetails,
                oppManaColors,
                oppMana.available(),
                oppMana.spent(),
                decisionType,
                oppMana.untappedSources(),
                List.of(),
                0,
                aiController.getIntProperty(AiProps.SIDECAR_INFLUENCE_WEIGHT));

        Logger.info("DeckRecognition: POST /recognize game=" + game.getId()
                + " aiTurn=" + currentAiTurnNumber()
                + " gameTurn=" + game.getPhaseHandler().getTurn()
                + " observations=" + observations.size()
                + " cards=" + observations.stream().map(Observation::card).toList());

        final var future = client.recognizeAsync(request);
        // Expose to the influence object so decision sites can wait on it.
        aiController.getSidecarInfluence().setLatestCall(future);
        future.whenComplete((result, err) -> {
            if (err != null) {
                Logger.info("DeckRecognition: /recognize failed: " + err.getMessage());
            } else if (result == null || result.isEmpty()) {
                Logger.info("DeckRecognition: /recognize returned no result (transport or schema error)");
            } else {
                final RecognitionResult recResult = result.get();
                Logger.info("DeckRecognition: opponent guess='" + recResult.archetype()
                        + "' confidence=" + recResult.confidence());
                postGuess(recResult);
                // Store sidecar influence data for AI decision-making
                aiController.onSidecarResult(recResult);
            }
            inFlight.set(false);
            if (rerunRequested.compareAndSet(true, false)) {
                requestRecognition();
            }
        });
    }

    private int currentAiTurnNumber() {
        return Math.max(aiTurnNumber, aiPlayer.getTurn());
    }

    private void postGuess(final RecognitionResult result) {
        try {
            final String message = result.toLogMessage();
            if (message.equals(lastPostedMessage)) {
                return; // unchanged since the last guess — don't spam the log
            }
            lastPostedMessage = message;
            game.getGameLog().add(GameLogEntryType.INFORMATION, message);
            // Also log piloting advice with action scores
            final String pilotingMsg = result.toPilotingLogMessage();
            if (pilotingMsg != null && !pilotingMsg.isEmpty()) {
                game.getGameLog().add(GameLogEntryType.INFORMATION, pilotingMsg);
            }
            final String roleMsg = result.toRoleLogMessage();
            if (roleMsg != null && !roleMsg.isEmpty()) {
                game.getGameLog().add(GameLogEntryType.INFORMATION, roleMsg);
            }
            final String handMsg = result.toHandValuesLogMessage();
            if (handMsg != null && !handMsg.isEmpty()) {
                game.getGameLog().add(GameLogEntryType.INFORMATION, handMsg);
            }
            final String oppHandMsg = result.toOpponentHandLogMessage();
            if (oppHandMsg != null && !oppHandMsg.isEmpty()) {
                game.getGameLog().add(GameLogEntryType.INFORMATION, oppHandMsg);
            }
        } catch (final RuntimeException ex) {
            Logger.debug("DeckRecognition: failed to write guess to log: " + ex.getMessage());
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
        // Combine the card's intrinsic color with its color identity. The intrinsic
        // color is empty for most lands (Hallowed Fountain is a colorless card),
        // but the color identity captures the colors a land taps for / a card
        // represents in deck-building (Hallowed Fountain -> W,U).
        var cs = card.getColor();
        try {
            final var identity = card.getRules() != null ? card.getRules().getColorIdentity() : null;
            if (identity != null) {
                cs = forge.card.ColorSet.fromMask(cs.getColor() | identity.getColor());
            }
        } catch (final RuntimeException ignored) {
            // Fall back to intrinsic color only.
        }
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
