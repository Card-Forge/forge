package forge.web.deck;

import com.google.common.collect.MapMaker;
import com.google.common.primitives.Ints;
import forge.deck.CardPool;
import forge.deck.CommanderBracketCalculator;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.deck.DeckSection;
import forge.deck.DeckStatistics;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.Localizer;
import forge.web.protocol.ToBrowser.Bracket;
import forge.web.protocol.ToBrowser.BracketReason;
import forge.web.protocol.ToBrowser.BracketReasonKind;
import forge.web.protocol.ToBrowser.DeckStats;
import forge.web.protocol.ToBrowser.TypeCount;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** What the browser shows about a deck wherever it is listed: its colours, size, verdict, bracket and statistics. The slow ones are cached per deck. */
public final class DeckFacts {
    /** Cached because working out a deck's formats reads every printing of its cards. The keys are deck objects, so changed() must drop an edited deck. */
    private static final Map<Deck, Facts> FACTS = new MapMaker().weakKeys().makeMap();
    private static final Map<Deck, Map<String, Optional<String>>> VERDICTS = new MapMaker().weakKeys().makeMap();
    /** A Commander deck's bracket, cached the same way. It is worked out only for decks listed in Commander. */
    private static final Map<Deck, Integer> BRACKETS = new MapMaker().weakKeys().makeMap();

    private DeckFacts() {
    }

    /** The sanctioned formats among those a deck is legal in, in the sanctioned list's order. */
    private static List<String> legalIn(final Set<GameFormat> formats) {
        final List<String> out = new ArrayList<>();
        for (final GameFormat f : FModel.getFormats().getSanctionedList()) {
            if (formats.contains(f)) {
                out.add(f.getName());
            }
        }
        return out;
    }

    static void changed(final Deck deck) {
        FACTS.remove(deck);
        VERDICTS.remove(deck);
        BRACKETS.remove(deck);
    }

    /** A deck's suggested minimum Commander bracket, 1 to 4. */
    public static int bracketOf(final Deck deck) {
        return BRACKETS.computeIfAbsent(deck, CommanderBracketCalculator::getBracket);
    }

    /** The bracket and every kind of card that raised it, for the panel next to the list. */
    public static Bracket bracket(final Deck deck) {
        final CommanderBracketCalculator.Result r = CommanderBracketCalculator.calculate(deck);
        final Localizer text = Localizer.getInstance();
        final List<BracketReason> reasons = new ArrayList<>();
        final List<String> clear = new ArrayList<>();
        final int changers = r.getGamechangers().size();
        reason(reasons, clear, BracketReasonKind.gameChangers, text.getMessage("lblCommanderBracketGameChangers"),
                text.getMessage("lblWebBracketBriefGameChangers", changers), r.getGamechangers(), r.getGamechangersBracket());
        reason(reasons, clear, BracketReasonKind.massLandDenial, text.getMessage("lblCommanderBracketMassLandDenial"),
                text.getMessage("lblCommanderBracketMassLandDenial"), r.getMassLandDenial(), r.getMassLandDenialBracket());
        final int turns = r.getExtraTurns().size();
        reason(reasons, clear, BracketReasonKind.extraTurns, text.getMessage("lblCommanderBracketExtraTurns"),
                text.getMessage("lblWebBracketBriefExtraTurns", turns), r.getExtraTurns(), r.getExtraTurnsBracket());
        reason(reasons, clear, BracketReasonKind.chainedExtraTurns, text.getMessage("lblCommanderBracketChainedExtraTurns"),
                text.getMessage("lblCommanderBracketChainedExtraTurns"), r.getChainedExtraTurns(), r.getChainedExtraTurnsBracket());
        final List<String> late = r.getLateGameCombos().stream().map(Object::toString).toList();
        final List<String> early = r.getEarlyGameCombos().stream().map(Object::toString).toList();
        final String combos = text.getMessage("lblCommanderBracketTwoCardCombos");
        if (late.isEmpty() && early.isEmpty()) {
            clear.add(combos);
        }
        if (!late.isEmpty()) {
            reasons.add(new BracketReason(BracketReasonKind.lateCombos, combos + " · " + text.getMessage("lblCommanderBracketLateGame"),
                    text.getMessage("lblWebBracketBriefLateCombo"), late, r.getLateGameCombosBracket()));
        }
        if (!early.isEmpty()) {
            reasons.add(new BracketReason(BracketReasonKind.earlyCombos, combos + " · " + text.getMessage("lblCommanderBracketEarlyGame"),
                    text.getMessage("lblWebBracketBriefEarlyCombo"), early, r.getEarlyGameCombosBracket()));
        }
        reasons.sort(Comparator.comparingInt(BracketReason::raises).reversed());
        return new Bracket(r.getBracket(), reasons, clear);
    }

    private static void reason(final List<BracketReason> reasons, final List<String> clear, final BracketReasonKind kind, final String title,
            final String brief, final List<String> cards, final int raises) {
        if (cards.isEmpty()) {
            clear.add(title);
        } else {
            reasons.add(new BracketReason(kind, title, brief, cards, raises));
        }
    }

    static Facts factsOf(final Deck deck, final DeckProxy proxy) {
        return FACTS.computeIfAbsent(deck, d -> new Facts(colors(d), played(d), count(d.get(DeckSection.Sideboard)),
                legalIn(proxy != null ? proxy.getFormats() : FModel.getFormats().getAllFormatsOfDeck(d)),
                proxy == null ? null : proxy.getFormatsString(), d.getAverageCMC()));
    }

    /** The same verdict as problem, cached for each deck until it is saved. */
    public static String problemAt(final Deck deck, final GameType format, final GameFormat pool) {
        final String table = format.name() + "|" + (pool == null ? "" : pool.getName()) + "|"
                + Legality.enforced();
        return VERDICTS.computeIfAbsent(deck, d -> new ConcurrentHashMap<>())
                .computeIfAbsent(table, t -> Optional.ofNullable(problem(deck, format, pool))).orElse(null);
    }

    /** The deck's colour identity as WUBRG letters, or "C" when it has none. */
    public static String colors(final Deck deck) {
        return CardCatalog.letters(DeckProxy.getColorIdentity(deck));
    }

    /** What the panel shows about a deck next to its card list. The types use the editor's own headings, so the two agree. */
    public static DeckStats stats(final Deck deck) {
        final CardPool main = deck.get(DeckSection.Main);
        final DeckStatistics shared = DeckStatistics.fromDeck(deck);
        final int[] curve = new int[DeckStatistics.CURVE_BUCKETS];
        for (int i = 0; i < curve.length; i++) {
            curve[i] = shared.permanentCurve[i] + shared.spellCurve[i];
        }
        final Map<String, Integer> types = new LinkedHashMap<>();
        if (main != null) {
            main.forEach(e -> types.merge(CardCatalog.heading(e.getKey()), e.getValue(), Integer::sum));
        }
        return new DeckStats(played(deck), count(main), count(deck.get(DeckSection.Sideboard)), shared.landCount,
                Math.round(shared.averageCmc * 100) / 100f, Ints.asList(curve), Ints.asList(shared.spellCurve),
                types.entrySet().stream().map(e -> new TypeCount(e.getKey(), e.getValue())).toList(), Ints.asList(shared.pips));
    }

    static List<String> aiSkipNames(final Deck deck) {
        final List<? extends PaperCard> skipped = deck.getUnplayableAICards().unplayable.get(DeckSection.Main);
        return skipped == null ? null : skipped.stream().map(PaperCard::getName).distinct().toList();
    }

    static Integer aiSkips(final Deck deck) {
        final int skipped = deck.getUnplayableAICards().inMainDeck;
        return skipped == 0 ? null : skipped;
    }

    /** Why this deck cannot be played here, or null when it can. A chosen card pool is checked even with deck legality checks off. */
    public static String problem(final Deck deck, final GameType format, final GameFormat pool) {
        if (deck == null) {
            return Localizer.getInstance().getMessage("lblWebDeckCatalogNoDeck");
        }
        final String outOfPool = pool == null ? null : pool.getDeckConformanceProblem(deck);
        if (outOfPool != null) {
            // GameFormat lists the cards on separate lines
            return Localizer.getInstance().getMessage("lblNotLegalInFormat", pool.getName(), outOfPool.replaceFirst("\n+", " ").replace("\n", ", "));
        }
        return Legality.enforced() ? format.getDeckFormat().getDeckConformanceProblem(deck) : null;
    }

    /** The cards a deck plays with: its main deck and its commanders. A Commander deck's 100 cards include both. */
    public static int played(final Deck deck) {
        return count(deck.get(DeckSection.Main)) + count(deck.get(DeckSection.Commander));
    }

    private static int count(final CardPool pool) {
        return pool == null ? 0 : pool.countAll();
    }

    /** What a deck's summary says at any table. */
    record Facts(String colors, int played, int sideboard, List<String> legalIn, String formats, int averageMana) {
    }
}
