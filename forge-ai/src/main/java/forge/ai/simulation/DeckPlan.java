package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * What the AI's own deck is trying to do, worked out once from the whole list before the first decision and then
 * fixed for the game (the same deck always gives the same plan).
 * <p>
 * The rollouts are deck independent; the plan only tilts what they value: an aggro or burn deck counts damage to the
 * opponent more and its own life less, a control deck the opposite, a combo deck is given long playouts, a ramp deck
 * values mana development, and a lands deck (Exploration, Life from the Loam, Crucible of Worlds, Wasteland, creature
 * lands) values every land it gets onto the table and plays the long game. The tilt is small (a few evaluator points
 * per life point) so that the rollouts can still disagree with it when the position says otherwise.
 */
public final class DeckPlan {
    public enum Archetype { AGGRO, BURN, CONTROL, COMBO, RAMP, LANDS, MIDRANGE }

    public final Archetype archetype;
    /** Extra evaluator points for each point of life the opponent has lost (from 20). */
    public final int opponentLifeExtra;
    /** Extra evaluator points for each point of life the AI has lost (from 20). */
    public final int ownLifeExtra;
    /** Scales how much a dangerous crack-back counts, in percent. */
    public final int riskPercent;
    /** Mana development value per land and mana source beyond the first four, evaluator points. */
    public final int manaDevelopmentExtra;
    /** Long playouts always, not only when the hand looks like a combo. */
    public final boolean longPlayouts;
    /**
     * The combos the deck is built around (none for most decks), read once together with the rest of the plan. Never
     * null; check {@link ComboPlan#isScanComplete()} to know whether the deck has been read.
     */
    public final ComboPlan combos;
    /**
     * What a Commander deck is about (strategies of the commander, known combos); {@link CommanderPlan#EMPTY} in a
     * game without commanders. Never null.
     */
    public final CommanderPlan commander;

    private DeckPlan(final Archetype archetype, final int oppLife, final int ownLife, final int risk, final int mana,
                     final boolean longPlayouts, final ComboPlan combos) {
        this(archetype, oppLife, ownLife, risk, mana, longPlayouts, combos, CommanderPlan.EMPTY);
    }

    private DeckPlan(final Archetype archetype, final int oppLife, final int ownLife, final int risk, final int mana,
                     final boolean longPlayouts, final ComboPlan combos, final CommanderPlan commander) {
        this.archetype = archetype;
        this.opponentLifeExtra = oppLife;
        this.ownLifeExtra = ownLife;
        this.riskPercent = risk;
        this.manaDevelopmentExtra = mana;
        this.longPlayouts = longPlayouts;
        this.combos = combos == null ? ComboPlan.EMPTY : combos;
        this.commander = commander == null ? CommanderPlan.EMPTY : commander;
    }

    public static final DeckPlan NONE = new DeckPlan(Archetype.MIDRANGE, 0, 0, 100, 0, false, ComboPlan.EMPTY);

    /**
     * This plan, keeping its archetype, together with the result of reading the deck for combos. The result is kept
     * even when no combo was found, so the plan knows that the deck has been read ({@link ComboPlan#isScanComplete()})
     * and the scan is never repeated.
     */
    DeckPlan withScan(final ComboPlan found) {
        if (found == null) {
            return this;
        }
        return new DeckPlan(archetype, opponentLifeExtra, ownLifeExtra, riskPercent, manaDevelopmentExtra,
                longPlayouts || !found.isEmpty(), found, commander);
    }

    /** This plan together with the Commander plan of the deck (a no-op for a game without commanders). */
    DeckPlan withCommander(final CommanderPlan cmd) {
        if (cmd == null || !cmd.isActive()) {
            return this;
        }
        return new DeckPlan(archetype, opponentLifeExtra, ownLifeExtra, riskPercent, manaDevelopmentExtra,
                longPlayouts || cmd.hasCombos(), combos, cmd);
    }

    static DeckPlan of(final Archetype a) {
        switch (a) {
            case AGGRO:
                return new DeckPlan(a, 4, 1, 70, 0, false, ComboPlan.EMPTY);
            case BURN:
                return new DeckPlan(a, 6, 0, 60, 0, false, ComboPlan.EMPTY);
            case CONTROL:
                return new DeckPlan(a, 0, 3, 130, 0, false, ComboPlan.EMPTY);
            case COMBO:
                return new DeckPlan(a, 1, 1, 100, 0, true, ComboPlan.EMPTY);
            case RAMP:
                return new DeckPlan(a, 0, 1, 100, 8, false, ComboPlan.EMPTY);
            case LANDS:
                // a grinding deck: its own life matters a little more than the opponent's, and every land counts
                return new DeckPlan(a, 1, 2, 100, 6, false, ComboPlan.EMPTY);
            default:
                return NONE;
        }
    }

    /**
     * Plans already worked out, by setting and deck list. A plan is only names and numbers (never a card or a game), so
     * the copies of a game that Monte Carlo plays out, each with a controller of its own, share the plan of the real
     * game instead of reading the same sixty cards again.
     */
    private static final Map<String, DeckPlan> KNOWN = Collections.synchronizedMap(
            new LinkedHashMap<String, DeckPlan>(16, 0.75f, true) {
                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(final Map.Entry<String, DeckPlan> eldest) {
                    return size() > 48;
                }
            });

    /** The deck as a sorted list of card names: the same list, wherever its cards are, gives the same key. */
    private static String deckKey(final Player player) {
        final List<String> names = new ArrayList<>();
        for (final Card c : player.getAllCards()) {
            if (!c.isToken() && !c.isImmutable()) {
                names.add(c.getName());
            }
        }
        Collections.sort(names);
        // the same list is a different deck in a Commander game, where the commander is part of it
        final boolean cmd = !player.getCommanders().isEmpty();
        return (cmd ? "cmd#" : "") + String.join("|", names);
    }

    /** The plan for a profile setting: "off", "auto" or the name of an archetype. */
    public static DeckPlan forSetting(final String setting, final Player player) {
        final String s = setting == null ? "off" : setting.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty() || s.equals("off") || s.equals("false")) {
            return NONE;
        }
        final String key = s + "#" + deckKey(player);
        DeckPlan plan = KNOWN.get(key);
        if (plan == null) {
            plan = read(s, player);
            KNOWN.put(key, plan);
        }
        return plan;
    }

    private static DeckPlan read(final String s, final Player player) {
        if (!s.equals("auto") && !s.equals("true")) {
            for (final Archetype a : Archetype.values()) {
                if (a.name().toLowerCase(Locale.ROOT).equals(s)) {
                    // the archetype is forced, but the combos of the deck are still read: they are facts about the list
                    return of(a).withScan(ComboPlan.scan(player)).withCommander(CommanderPlan.scan(player));
                }
            }
        }
        // one read of the whole deck: the combos (at most three) first, because a deck built around one is a combo deck
        // whatever its cheap spells look like to the archetype counters below
        final ComboPlan combos = ComboPlan.scan(player);
        final CommanderPlan cmd = CommanderPlan.scan(player);
        Archetype arch = combos.isEmpty() ? classify(player) : Archetype.COMBO;
        if (combos.has(ComboPlan.Kind.DARK_DEPTHS) && classify(player) == Archetype.LANDS) {
            // Legacy Lands: Dark Depths with Thespian's Stage is its win condition, but the deck is still about its
            // lands (the long playouts of a combo plan stay: the plan keeps the combo)
            arch = Archetype.LANDS;
        }
        if (arch == Archetype.MIDRANGE && cmd.archetypeHint() != null) {
            // 99 singleton cards never look like aggro or control to the counters of classify: the commander says
            arch = cmd.archetypeHint();
        }
        return of(arch).withScan(combos).withCommander(cmd);
    }

    /** Reads the player's whole deck (every card the player owns, wherever it is now). */
    static Archetype classify(final Player player) {
        int spells = 0;
        int creatures = 0;
        int cmcSum = 0;
        int burn = 0;
        int comboPieces = 0;
        int manaSources = 0;
        int bigSpells = 0;
        int reactive = 0;
        int counters = 0;
        int lands = 0;
        int landMatters = 0;
        int utilityLands = 0;
        for (final Card c : player.getAllCards()) {
            if (c.isToken() || c.isImmutable()) {
                continue;
            }
            if (c.isLand()) {
                lands++;
                if (isUtilityLand(c)) {
                    utilityLands++;
                }
                continue;
            }
            if (isLandMatters(c)) {
                landMatters++;
            }
            spells++;
            cmcSum += c.getCMC();
            if (c.isCreature()) {
                creatures++;
            }
            if (c.getCMC() >= 5) {
                bigSpells++;
            }
            if (c.hasKeyword(Keyword.STORM)) {
                comboPieces += 2;
            }
            if (c.isInstant() || c.hasKeyword(Keyword.FLASH)) {
                reactive++;
            }
            boolean mana = false;
            for (final SpellAbility sa : c.getSpellAbilities()) {
                final ApiType api = sa.getApi();
                if (api == ApiType.Counter) {
                    counters++;
                } else if (api == ApiType.DealDamage && c.isSpell() && (c.isInstant() || c.isSorcery())) {
                    burn++;
                } else if (api == ApiType.Mana) {
                    mana = true;
                    if (c.getCMC() <= 1 && !c.isCreature()) {
                        comboPieces++; // a ritual
                    }
                }
            }
            if (mana && c.getCMC() <= 3) {
                manaSources++;
            }
        }
        if (spells < 12) {
            return Archetype.MIDRANGE;
        }
        final double avgCmc = (double) cmcSum / spells;
        if (comboPieces >= 6) {
            return Archetype.COMBO;
        }
        if (burn * 100 / spells >= 25 && creatures * 100 / spells <= 35) {
            return Archetype.BURN;
        }
        if (lands >= LANDS_MIN_LANDS && lands * 100 >= LANDS_MIN_SHARE * (lands + spells)
                && landMatters + utilityLands >= LANDS_MIN_PAYOFFS) {
            return Archetype.LANDS; // before RAMP: a lands deck with a Titan is still about its lands
        }
        if (counters >= 3 || reactive * 100 / spells >= 30 && creatures * 100 / spells <= 40) {
            return Archetype.CONTROL;
        }
        if (manaSources >= 6 && bigSpells >= 4) {
            return Archetype.RAMP;
        }
        if (avgCmc <= 2.4 && creatures * 100 / spells >= 45) {
            return Archetype.AGGRO;
        }
        return Archetype.MIDRANGE;
    }

    /** A lands deck plays this many lands or more (a normal 60 card deck plays 24 or fewer) ... */
    static final int LANDS_MIN_LANDS = 26;
    /**
     * ... and at least this share of the deck (percent), so that a 99 card Commander deck, which plays 36 lands or
     * more as a matter of course, is not taken for one ...
     */
    static final int LANDS_MIN_SHARE = 45;
    /** ... and this many cards that care about them: land-matters spells and lands that do more than make mana. */
    static final int LANDS_MIN_PAYOFFS = 10;

    /**
     * What a land-matters spell says: landfall and the cards that play an extra land, take lands back from the
     * graveyard (Crucible of Worlds, Life from the Loam), count or sacrifice lands, or destroy them.
     */
    private static final Pattern LAND_TEXT = Pattern.compile(
            "landfall|whenever a land|whenever a (forest|island|swamp|mountain|plains)|additional land"
                    + "|lands? cards? from your graveyard|play lands from|lands? you control"
                    + "|sacrifice (a|any number of) lands?|destroy target (nonbasic )?land");

    /** A spell that cares about lands: read from the card's text. */
    static boolean isLandMatters(final Card c) {
        final String text = c.getOracleText();
        return text != null && LAND_TEXT.matcher(text.toLowerCase(Locale.ROOT)).find();
    }

    /**
     * A land that does more than make mana: a creature land, a land that destroys or copies another land, or one that
     * untaps. Fetch lands and the like do not count, they are in every deck.
     */
    static boolean isUtilityLand(final Card c) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            final ApiType api = sa.getApi();
            if (sa.isActivatedAbility() && (api == ApiType.Animate || api == ApiType.Destroy || api == ApiType.Clone
                    || api == ApiType.Untap)) {
                return true;
            }
        }
        return false;
    }

    /** Archetype and combos, for statistics and the game log ("COMBO[MASS_REANIMATION(52: ...)]"). */
    /**
     * What to search the library for, for any kind of deck: the combo's missing piece first (see
     * {@link ComboPlan#tutorPicks}), then what the commander plan is built around, then for a ramp deck the mana it
     * is short of or the big spell it can now cast. Null for no opinion: the normal choice is made.
     */
    public forge.game.card.CardCollection tutorPicks(final Player ai, final Iterable<forge.game.card.Card> pool, final int n) {
        if (ai == null || pool == null || n <= 0) {
            return null;
        }
        final forge.game.card.CardCollection fromCombo = combos.tutorPicks(ai, pool, n);
        if (fromCombo != null) {
            return fromCombo;
        }
        if (n != 1) {
            return null;
        }
        final forge.game.card.CardCollectionView hand = ai.getCardsIn(forge.game.zone.ZoneType.Hand);
        final forge.game.card.CardCollectionView board = ai.getCardsIn(forge.game.zone.ZoneType.Battlefield);
        for (final forge.game.card.Card c : pool) {
            if (!c.isLand() && commander.isKeyCard(c) && !hand.contains(c) && !named(hand, board, c.getName())) {
                return new forge.game.card.CardCollection(c);
            }
        }
        final forge.game.card.Card ramp = archetype == Archetype.RAMP ? rampPick(pool, hand, board) : null;
        return ramp == null ? null : new forge.game.card.CardCollection(ramp);
    }

    /** The one card to search for, or null for no opinion. */
    public forge.game.card.Card tutorPick(final Player ai, final Iterable<forge.game.card.Card> pool) {
        final forge.game.card.CardCollection picks = tutorPicks(ai, pool, 1);
        return picks == null ? null : picks.getFirst();
    }

    private static boolean named(final forge.game.card.CardCollectionView hand, final forge.game.card.CardCollectionView board,
                                 final String name) {
        for (final forge.game.card.Card x : hand) {
            if (x.getName().equals(name)) {
                return true;
            }
        }
        for (final forge.game.card.Card x : board) {
            if (x.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A ramp deck short of mana (or stuck with a big spell in hand) fetches the cheapest mana source it can cast now;
     * with the mana it fetches the biggest spell it can cast within a turn or two, the one it was built for.
     */
    private static forge.game.card.Card rampPick(final Iterable<forge.game.card.Card> pool,
                                                 final forge.game.card.CardCollectionView hand,
                                                 final forge.game.card.CardCollectionView board) {
        int sources = 0;
        for (final forge.game.card.Card x : board) {
            if (x.isLand() || !x.getManaAbilities().isEmpty()) {
                sources++;
            }
        }
        int stuckAt = 0;
        for (final forge.game.card.Card x : hand) {
            if (!x.isLand()) {
                stuckAt = Math.max(stuckAt, x.getCMC());
            }
        }
        forge.game.card.Card best = null;
        int bestScore = 0;
        for (final forge.game.card.Card c : pool) {
            if (c.isLand()) {
                continue;
            }
            final boolean source = !c.getManaAbilities().isEmpty();
            int score = 0;
            if (source && sources < 7 && c.getCMC() <= sources + 1) {
                score = 100 - 10 * c.getCMC() + (stuckAt > sources + 1 ? 50 : 0);
            } else if (!source && sources >= 4 && c.getCMC() <= sources + 2) {
                score = 60 + 8 * c.getCMC() + (c.isCreature() ? forge.ai.ComputerUtilCard.evaluateCreature(c) / 20 : 0);
            }
            if (score > 0 && named(hand, board, c.getName())) {
                score -= 60; // one is already at hand
            }
            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        return best;
    }

    public String describe() {
        final String base = combos.isEmpty() ? archetype.name() : archetype.name() + combos;
        return commander.isActive() ? base + "/CMD" + commander : base;
    }

    @Override
    public String toString() {
        return archetype.name();
    }
}
