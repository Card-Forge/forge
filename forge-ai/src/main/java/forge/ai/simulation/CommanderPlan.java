package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * What a Commander deck is about, read once from the commander(s) and the 99 and then fixed for the game, like
 * {@link DeckPlan} (which holds one of these) and {@link ComboPlan}.
 * <p>
 * It is only active for a player that has a commander ({@code Player#getCommanders()}); everywhere else {@link #EMPTY} is
 * used and nothing changes. It knows three things:
 * <ul>
 *   <li><b>The strategies</b> of the commander ({@link Strategy}): Voltron, tokens, aristocrats, spellslinger,
 *       +1/+1 counters, landfall, graveyard, artifacts, tribal and stax. They come from what the commander says
 *       and from how many cards of the deck back it up, so a commander with a text for equipment in a deck without any
 *       equipment is not read as Voltron. At most two are kept, the best first.</li>
 *   <li><b>The known combos</b> of the deck (Thassa's Oracle with Demonic Consultation, Kiki-Jiki with a copy
 *       creature, ...). A combo is listed when the deck really holds all of its pieces, the commander counts as a piece
 *       of the deck, and it is read once.</li>
 *   <li><b>The plan of the game</b>: the commander on the table is worth more than its own stats (it is the card the
 *       deck is built around and it can come back), commander damage is progress towards a win, and commander damage
 *       taken is a clock.</li>
 * </ul>
 * Like every term of the strategic evaluator the numbers are small bonuses in evaluator points (a 2/2 creature is
 * worth about 150) and bounded, so the rollouts can still disagree with the plan when the position says otherwise.
 */
public final class CommanderPlan {
    public enum Strategy { VOLTRON, TOKENS, ARISTOCRATS, SPELLSLINGER, COUNTERS, LANDFALL, GRAVEYARD, ARTIFACTS,
        TRIBAL, STAX }

    /** The result for a game without commanders, or for a deck that could not be read. */
    public static final CommanderPlan EMPTY = new CommanderPlan(false, Collections.<String>emptyList(),
            Collections.<Strategy>emptyList(), Collections.<KnownCombo>emptyList(), 0, Collections.<String>emptySet());

    /** At most this many strategies are kept. */
    public static final int MAX_STRATEGIES = 2;
    /** A strategy needs this many points to count: a commander text hit is worth 4, every two backing cards 1. */
    static final int STRATEGY_MIN_POINTS = 6;
    private static final int TERM_MAX = 400;
    private static final int TERM_MIN = -300;

    /** The commander on the battlefield: the deck is built around it, and losing it costs the tax to cast it again. */
    static final int COMMANDER_IN_PLAY = 30;
    /** Per point of commander damage dealt to an opponent (21 kills), doubled for Voltron. */
    static final int COMMANDER_DAMAGE_DEALT = 4;
    /** Per point of commander damage taken from an opposing commander. */
    static final int COMMANDER_DAMAGE_TAKEN = 6;
    /** One more hit of the same commander kills when this much has been taken. */
    static final int COMMANDER_DANGER = 15;
    static final int COMBO_ASSEMBLED = 150;
    static final int COMBO_ONE_MISSING = 60;

    // ------------------------------------------------------------------------------------------ the known combos

    /** At most this many combos are kept for a deck: every one is looked at in every rollout. */
    static final int MAX_KNOWN_COMBOS = 40;

    /**
     * A two card combo of the format, from the lists read by {@link AiCardLists} (the Commander bracket combos and
     * the combos that only the AI knows). Both cards make it.
     */
    public static final class KnownCombo {
        public final String name;
        private final AiCardLists.Pair pair;

        KnownCombo(final AiCardLists.Pair pair) {
            this.pair = pair;
            this.name = pair.toString();
        }

        /** Whether the card is a piece of this combo. */
        public boolean isPiece(final Card c) {
            final String k = AiCardLists.normalize(c.getName());
            return k.equals(AiCardLists.normalize(pair.first)) || k.equals(AiCardLists.normalize(pair.second));
        }

        Set<String> pieces() {
            final Set<String> all = new LinkedHashSet<>();
            all.add(pair.first);
            all.add(pair.second);
            return all;
        }

        /** How many of the two cards are among these (already normalized) names. */
        int have(final Set<String> normalizedNames) {
            return (normalizedNames.contains(AiCardLists.normalize(pair.first)) ? 1 : 0)
                    + (normalizedNames.contains(AiCardLists.normalize(pair.second)) ? 1 : 0);
        }

        int needed() {
            return 2;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // ------------------------------------------------------------------------------------------------- the plan

    private final boolean active;
    private final List<String> commanders;
    private final List<Strategy> strategies;
    private final List<KnownCombo> combos;
    private final int cardsScanned;
    /** The creature types of the commanders, for the tribal strategy. */
    private final Set<String> tribe;

    private CommanderPlan(final boolean active, final List<String> commanders, final List<Strategy> strategies,
                          final List<KnownCombo> combos, final int cardsScanned, final Set<String> tribe) {
        this.active = active;
        this.commanders = Collections.unmodifiableList(commanders);
        this.strategies = Collections.unmodifiableList(strategies);
        this.combos = Collections.unmodifiableList(combos);
        this.cardsScanned = cardsScanned;
        this.tribe = Collections.unmodifiableSet(tribe);
    }

    /** Whether this player has a commander: otherwise nothing of this plan applies. */
    public boolean isActive() {
        return active;
    }

    public List<String> getCommanderNames() {
        return commanders;
    }

    /** The strategies of the commander, strongest first (at most {@link #MAX_STRATEGIES}). */
    public List<Strategy> getStrategies() {
        return strategies;
    }

    public boolean has(final Strategy s) {
        return strategies.contains(s);
    }

    /** The combos of the deck whose pieces are all there (the commander is part of the deck). */
    public List<KnownCombo> getCombos() {
        return combos;
    }

    public boolean hasCombos() {
        return !combos.isEmpty();
    }

    public int getCardsScanned() {
        return cardsScanned;
    }

    /**
     * What a plain archetype read of the deck would miss: a Voltron or tribal commander wants to attack, stax wants
     * to control, landfall wants mana. Null when the strategy says nothing about the archetype.
     */
    DeckPlan.Archetype archetypeHint() {
        for (final Strategy s : strategies) {
            switch (s) {
                case VOLTRON:
                case TRIBAL:
                    return DeckPlan.Archetype.AGGRO;
                case STAX:
                    return DeckPlan.Archetype.CONTROL;
                case LANDFALL:
                    return DeckPlan.Archetype.RAMP;
                default:
                    break;
            }
        }
        return null;
    }

    // --------------------------------------------------------------------------------------------- the scan

    /** Reads the commanders and the deck of the player once. */
    public static CommanderPlan scan(final Player player) {
        try {
            // a player with a commander is in a Commander style game, whichever variant flags the rules carry
            if (player == null || player.getCommanders().isEmpty()) {
                return EMPTY;
            }
            return scan(player.getCommanders(), ComboPlan.deckCards(player));
        } catch (final RuntimeException e) {
            return EMPTY; // a refinement only: never let it break a game
        }
    }

    /** Reads the commanders and the cards of a deck (the commanders included). */
    static CommanderPlan scan(final List<Card> cmds, final Iterable<Card> deck) {
        final Set<String> deckNames = new LinkedHashSet<>();
        final List<Card> spells = new ArrayList<>();
        int scanned = 0;
        for (final Card c : deck) {
            scanned++;
            deckNames.add(c.getName());
            if (!c.isLand() && !c.isToken() && !c.isImmutable() && !cmds.contains(c)) {
                spells.add(c);
            }
        }
        final List<String> cmdNames = new ArrayList<>();
        for (final Card c : cmds) {
            cmdNames.add(c.getName());
            deckNames.add(c.getName());
        }

        // the combos of the lists whose two cards are both in the deck (the commander is part of it)
        final List<KnownCombo> found = new ArrayList<>();
        for (final AiCardLists.Pair pair : AiCardLists.combosIn(AiCardLists.deckKeys(deckNames))) {
            if (found.size() >= MAX_KNOWN_COMBOS) {
                break;
            }
            found.add(new KnownCombo(pair));
        }
        final Set<String> cmdTypes = new LinkedHashSet<>();
        for (final Card cmd : cmds) {
            cmdTypes.addAll(cmd.getType().getCreatureTypes());
        }
        return new CommanderPlan(true, cmdNames, strategiesOf(cmds, spells, cmdTypes), found, scanned, cmdTypes);
    }

    private static final String[][] TEXT_HITS = new String[Strategy.values().length][];
    static {
        TEXT_HITS[Strategy.VOLTRON.ordinal()] = new String[] { "equipment", "equipped creature", "aura",
            "enchanted creature", "double strike", "attach", "commander damage" };
        TEXT_HITS[Strategy.TOKENS.ordinal()] = new String[] { "create", "token", "populate", "creatures you control get" };
        TEXT_HITS[Strategy.ARISTOCRATS.ordinal()] = new String[] { "sacrifice", "dies", "whenever a creature you control dies",
            "each opponent loses" };
        TEXT_HITS[Strategy.SPELLSLINGER.ordinal()] = new String[] { "instant or sorcery", "instant and sorcery", "whenever you cast",
            "copy target instant", "magecraft" };
        TEXT_HITS[Strategy.COUNTERS.ordinal()] = new String[] { "+1/+1 counter", "proliferate", "counter on" };
        TEXT_HITS[Strategy.LANDFALL.ordinal()] = new String[] { "landfall", "land enters", "play an additional land",
            "lands you control" };
        TEXT_HITS[Strategy.GRAVEYARD.ordinal()] = new String[] { "from your graveyard", "graveyard to the battlefield",
            "mill", "dredge", "flashback" };
        TEXT_HITS[Strategy.ARTIFACTS.ordinal()] = new String[] { "artifact", "treasure", "improvise", "affinity for artifacts" };
        TEXT_HITS[Strategy.STAX.ordinal()] = new String[] { "can't cast", "can't attack", "players can't", "each opponent sacrifices",
            "opponents can't", "doesn't untap", "don't untap" };
    }

    /** How many of the markers of the strategy the text of a card contains. */
    private static int textHits(final Strategy s, final String text) {
        int hits = 0;
        final String[] marks = TEXT_HITS[s.ordinal()];
        if (marks == null) {
            return 0;
        }
        for (final String m : marks) {
            if (text.contains(m)) {
                hits++;
            }
        }
        return hits;
    }

    private static List<Strategy> strategiesOf(final List<Card> cmds, final List<Card> spells,
                                               final Set<String> cmdTypes) {
        // what the commander says, once for each strategy
        final Map<Strategy, Integer> points = new EnumMap<>(Strategy.class);
        for (final Strategy s : Strategy.values()) {
            points.put(s, 0);
        }
        for (final Card cmd : cmds) {
            final String text = oracle(cmd);
            for (final Strategy s : Strategy.values()) {
                if (s != Strategy.TRIBAL && textHits(s, text) > 0) {
                    points.merge(s, 4, Integer::sum);
                }
            }
            // a commander that names its own type: "other Goblins you control", "whenever a Goblin enters"
            for (final String t : cmd.getType().getCreatureTypes()) {
                if (text.contains(t.toLowerCase(Locale.ROOT))) {
                    points.merge(Strategy.TRIBAL, 4, Integer::sum);
                    break;
                }
            }
        }

        // what the rest of the deck does for it
        int equipAura = 0;
        int tokenMakers = 0;
        int sacrifice = 0;
        int spellsCount = 0;
        int counters = 0;
        int landfall = 0;
        int graveyard = 0;
        int artifacts = 0;
        int tribe = 0;
        int stax = 0;
        for (final Card c : spells) {
            final String text = oracle(c);
            if (c.isEquipment() || c.isAura()) {
                equipAura++;
            }
            if (makesTokens(c, text)) {
                tokenMakers++;
            }
            if (text.contains("sacrifice a creature") || text.contains("sacrifice another creature")
                    || text.contains("creature you control dies")) {
                sacrifice++;
            }
            if (c.isInstant() || c.isSorcery()) {
                spellsCount++;
            }
            if (text.contains("+1/+1 counter")) {
                counters++;
            }
            if (text.contains("landfall") || text.contains("search your library for a basic land")
                    || text.contains("additional land")) {
                landfall++;
            }
            if (text.contains("from your graveyard") || text.contains("from a graveyard")) {
                graveyard++;
            }
            if (c.isArtifact()) {
                artifacts++;
            }
            if (c.isCreature() && sharesType(c, cmdTypes)) {
                tribe++;
            }
            if (textHits(Strategy.STAX, text) > 0) {
                stax++;
            }
        }
        add(points, Strategy.VOLTRON, equipAura);
        add(points, Strategy.TOKENS, tokenMakers);
        add(points, Strategy.ARISTOCRATS, sacrifice);
        add(points, Strategy.SPELLSLINGER, spellsCount);
        add(points, Strategy.COUNTERS, counters);
        add(points, Strategy.LANDFALL, landfall);
        add(points, Strategy.GRAVEYARD, graveyard);
        add(points, Strategy.ARTIFACTS, artifacts);
        add(points, Strategy.TRIBAL, tribe);
        add(points, Strategy.STAX, stax);

        final List<Strategy> ranked = new ArrayList<>();
        for (final Strategy s : Strategy.values()) {
            if (points.get(s) >= STRATEGY_MIN_POINTS) {
                ranked.add(s);
            }
        }
        // ties keep the order of the enum, so the same deck always gives the same plan
        ranked.sort((a, b) -> Integer.compare(points.get(b), points.get(a)));
        while (ranked.size() > MAX_STRATEGIES) {
            ranked.remove(ranked.size() - 1);
        }
        return ranked;
    }

    /** The backing cards count one point for every two of them, up to twelve. */
    private static void add(final Map<Strategy, Integer> points, final Strategy s, final int cards) {
        points.merge(s, Math.min(cards, 12) / 2, Integer::sum);
    }

    private static boolean makesTokens(final Card c, final String text) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Token) {
                return true;
            }
        }
        return text.contains("create") && text.contains("token");
    }

    private static boolean sharesType(final Card c, final Set<String> types) {
        if (types.isEmpty()) {
            return false;
        }
        if (c.getType().hasStringType("Changeling")) {
            return true;
        }
        for (final String t : c.getType().getCreatureTypes()) {
            if (types.contains(t)) {
                return true;
            }
        }
        return false;
    }

    private static String oracle(final Card c) {
        final String t = c.getOracleText();
        return t == null ? "" : t.toLowerCase(Locale.ROOT);
    }

    // --------------------------------------------------------------------------------------- the questions

    /** Whether the card is a commander of the deck or a piece of one of its combos. */
    public boolean isKeyCard(final Card c) {
        if (!active || c == null) {
            return false;
        }
        if (commanders.contains(c.getName())) {
            return true;
        }
        for (final KnownCombo k : combos) {
            if (k.isPiece(c)) {
                return true;
            }
        }
        return false;
    }

    /** Names of every piece of the combos of the deck (and of the commanders). */
    public Set<String> pieceNames() {
        final Set<String> all = new LinkedHashSet<>(commanders);
        for (final KnownCombo k : combos) {
            all.addAll(k.pieces());
        }
        return all;
    }

    /** The normalized names of the cards of the player that are in play, in hand or in the command zone. */
    private static Set<String> available(final Player ai) {
        final Set<String> names = new LinkedHashSet<>();
        for (final ZoneType z : new ZoneType[] { ZoneType.Hand, ZoneType.Battlefield, ZoneType.Command }) {
            for (final Card c : ai.getCardsIn(z)) {
                names.addAll(AiCardLists.deckKeys(Collections.singletonList(c.getName())));
            }
        }
        return names;
    }

    /**
     * The cards of the list without the pieces of a combo that is under way: a piece whose partners are already in
     * hand or on the table is what a cost that throws away cards must leave alone while something else will do.
     */
    public CardCollection withoutProtected(final Player ai, final CardCollection cards) {
        if (!active || combos.isEmpty() || ai == null) {
            return cards;
        }
        final Set<String> there = available(ai);
        final CardCollection safe = new CardCollection();
        for (final Card c : cards) {
            boolean keep = false;
            if (c.isInZone(ZoneType.Hand)) {
                for (final KnownCombo k : combos) {
                    if (k.isPiece(c) && k.have(there) >= 2) {
                        keep = true;
                        break;
                    }
                }
            }
            if (!keep) {
                safe.add(c);
            }
        }
        return safe;
    }

    // ------------------------------------------------------------------------------------------ the evaluation

    /**
     * Progress of the Commander plan in a position, in evaluator points: added to the score of a rollout so that the
     * commander on the table, commander damage dealt (and taken), the pieces of a combo coming together and the
     * things the strategy likes are worth something at the end of the turn.
     */
    public int term(final Player ai) {
        if (!active || ai == null) {
            return 0;
        }
        int sum = commanderTerm(ai);
        sum += comboTerm(ai);
        sum += strategyTerm(ai);
        return Math.max(TERM_MIN, Math.min(TERM_MAX, sum));
    }

    /** The card the game holds now for a commander (each zone change can hand the game a new object). */
    private static Card current(final Card c) {
        if (c.getGame() == null) {
            return c;
        }
        final Card state = c.getGame().getCardState(c);
        return state == null ? c : state;
    }

    private int commanderTerm(final Player ai) {
        final boolean voltron = has(Strategy.VOLTRON);
        int sum = 0;
        for (final Card cmd : ai.getCommanders()) {
            final Card now = current(cmd);
            if (now.isInZone(ZoneType.Battlefield)) {
                sum += voltron ? 2 * COMMANDER_IN_PLAY : COMMANDER_IN_PLAY;
                if (voltron) {
                    sum += 25 * Math.min(3, now.getEquippedBy().size() + now.getEnchantedBy().size());
                }
            }
            for (final Player opp : ai.getOpponents()) {
                final int dealt = Math.min(21, opp.getCommanderDamage(now));
                sum += dealt * (voltron ? 2 * COMMANDER_DAMAGE_DEALT : COMMANDER_DAMAGE_DEALT);
            }
        }
        // what the commanders of the opponents did to the AI: a clock, with a cliff before 21
        for (final Player opp : ai.getOpponents()) {
            for (final Card cmd : opp.getCommanders()) {
                final int taken = Math.min(21, ai.getCommanderDamage(current(cmd)));
                sum -= taken * COMMANDER_DAMAGE_TAKEN;
                if (taken >= COMMANDER_DANGER) {
                    sum -= 100;
                }
            }
        }
        return sum;
    }

    private int comboTerm(final Player ai) {
        if (combos.isEmpty()) {
            return 0;
        }
        final Set<String> there = available(ai);
        int sum = 0;
        for (final KnownCombo k : combos) {
            final int have = k.have(there);
            if (have >= k.needed()) {
                sum += COMBO_ASSEMBLED;
            } else if (have == k.needed() - 1 && have >= 1) {
                sum += COMBO_ONE_MISSING;
            }
        }
        return sum;
    }

    private int strategyTerm(final Player ai) {
        int sum = 0;
        for (final Strategy s : strategies) {
            switch (s) {
                case TOKENS:
                    sum += 6 * Math.min(8, count(ai, c -> c.isToken() && c.isCreature()));
                    break;
                case COUNTERS:
                    sum += 4 * Math.min(10, countersOnCreatures(ai));
                    break;
                case ARISTOCRATS:
                    sum += 5 * Math.min(8, count(ai, Card::isCreature));
                    break;
                case LANDFALL:
                    sum += 6 * Math.min(5, Math.max(0, count(ai, Card::isLand) - 5));
                    break;
                case ARTIFACTS:
                    sum += 4 * Math.min(8, count(ai, Card::isArtifact));
                    break;
                case TRIBAL:
                    sum += 5 * Math.min(8, count(ai, c -> c.isCreature() && sharesType(c, tribe())));
                    break;
                case GRAVEYARD:
                    sum += 4 * Math.min(8, graveyardCreatures(ai));
                    break;
                case SPELLSLINGER:
                    sum += 3 * Math.min(6, handSpells(ai));
                    break;
                default:
                    break; // voltron is in the commander term, stax is carried out by the playout itself
            }
        }
        return sum;
    }

    private Set<String> tribe() {
        return tribe;
    }

    private static int count(final Player ai, final java.util.function.Predicate<Card> pred) {
        int n = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (pred.test(c)) {
                n++;
            }
        }
        return n;
    }

    private static int countersOnCreatures(final Player ai) {
        int n = 0;
        for (final Card c : ai.getCreaturesInPlay()) {
            n += c.getCounters(CounterEnumType.P1P1);
        }
        return n;
    }

    private static int graveyardCreatures(final Player ai) {
        int n = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Graveyard)) {
            if (c.isCreature()) {
                n++;
            }
        }
        return n;
    }

    private static int handSpells(final Player ai) {
        int n = 0;
        final CardCollectionView hand = ai.getCardsIn(ZoneType.Hand);
        for (final Card c : hand) {
            if (c.isInstant() || c.isSorcery()) {
                n++;
            }
        }
        return n;
    }

    /** One line for statistics and the game log: the commanders, their strategies and the combos of the deck. */
    public String summary() {
        if (!active) {
            return "no commander";
        }
        return commanders + " " + strategies + ", " + (combos.isEmpty() ? "no known combo" : combos.size() + " combo(s): " + combos);
    }

    @Override
    public String toString() {
        return active ? strategies.toString() + (combos.isEmpty() ? "" : combos.toString()) : "[]";
    }
}
