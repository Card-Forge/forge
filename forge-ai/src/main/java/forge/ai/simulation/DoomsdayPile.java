package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import forge.ai.ComputerUtilCost;
import forge.card.MagicColor;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Doomsday: five cards are kept, everything else is exiled, and half the life is lost. The deck wins by emptying its
 * own library with the cards of that pile (draws, free cantrips) and then casting Thassa's Oracle (or Laboratory
 * Maniac) with the library at or below the devotion: the Oracle is held in hand and the pile is five cards that draw.
 * This decides when Doomsday may be cast, which five cards it keeps, and how far a Doomsday deck has come.
 */
public final class DoomsdayPile {
    /** The pile is five cards: a library of at most this many cards after Doomsday has been cast. */
    static final int PILE = 5;
    /** The life Doomsday takes is half, rounded up: with less than this it is not cast. */
    static final int MIN_LIFE = 4;

    private DoomsdayPile() {
    }

    public static boolean isDoomsday(final Card c) {
        return c != null && AiCardLists.has(AiCardLists.Role.DOOMSDAY, c.getName());
    }

    private static boolean isWinner(final Card c) {
        return AiCardLists.has(AiCardLists.Role.LIBRARY_OUT_PAYOFF, c.getName());
    }

    /** Whether a card that wins with a short library is in hand (or already in play). */
    static boolean winnerAvailable(final Player ai) {
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (isWinner(c)) {
                return true;
            }
        }
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (isWinner(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Doomsday is cast when a pile exists that draws through to the winner this turn (the winner may still be in the
     * library: it goes into the pile), or with the winner in hand as a set-up, and always with life to lose and a
     * library worth cutting down.
     */
    public static boolean ready(final Player ai) {
        if (ai.getLife() < MIN_LIFE || ai.getCardsIn(ZoneType.Library).size() <= 2 * PILE) {
            return false;
        }
        return winnerAvailable(ai) || winsThisTurn(ai);
    }

    /**
     * With a Doomsday in hand and a pile that wins this turn, the mana and the drawing wait for it: a creature cast first
     * (Street Wraith for its five mana) spends what Doomsday needs, and cycling the Wraith draws a card the pile should
     * have held. Mana cards (Dark Ritual, Lotus Petal), the winner and Doomsday itself are not held back.
     */
    public static boolean mustWaitForDoomsday(final Player ai, final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        if (ai == null || host == null || host.isLand() || isDoomsday(host) || isWinner(host)) {
            return false;
        }
        final boolean castPermanent = sa.isSpell() && host.isPermanent();
        final boolean cycling = sa.isAbility() && host.isInZone(ZoneType.Hand)
                && AiCardLists.has(AiCardLists.Role.DOOMSDAY_DRAW, host.getName());
        if (!castPermanent && !cycling) {
            return false;
        }
        if (AiCardLists.has(AiCardLists.Role.DOOMSDAY_MANA, host.getName())) {
            return false;
        }
        boolean doomsdayInHand = false;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            doomsdayInHand |= c != host && isDoomsday(c);
        }
        return doomsdayInHand && ready(ai) && winsThisTurn(ai);
    }

    /**
     * Drawing {@code numCards} out of a library that is already the cut pile, with the card that wins on a short library
     * in hand, in play or in the pile itself: the draw-through of Doomsday, which the "do not deck yourself" rule of the
     * draw heuristics would refuse. Never the draw that takes the last card of a library with no winner at hand.
     */
    public static boolean drawsThroughPile(final Player ai, final int numCards) {
        if (ai == null || numCards <= 0) {
            return false;
        }
        final int library = ai.getCardsIn(ZoneType.Library).size();
        if (library > PILE || numCards > library) {
            return false;
        }
        if (winnerAvailable(ai)) {
            return true;
        }
        for (final Card c : ai.getCardsIn(ZoneType.Library)) {
            if (isWinner(c)) {
                return numCards < library; // the winner must stay in the library to be drawn later
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------ the pile planner

    /** What a card does for the draw-through: the mana it costs and gives, the cards it draws, the life it takes. */
    private static final class Spec {
        final String name;
        final int cost;
        final int draw;
        final int gain;
        final int life;
        final boolean winner;
        final boolean protect;

        Spec(final String name, final int cost, final int draw, final int gain, final int life, final boolean winner,
             final boolean protect) {
            this.name = name;
            this.cost = cost;
            this.draw = draw;
            this.gain = gain;
            this.life = life;
            this.winner = winner;
            this.protect = protect;
        }

        boolean useful() {
            return winner || draw > 0 || gain > 0;
        }
    }

    private static final java.util.Set<String> PROTECTION = new java.util.HashSet<>(Arrays.asList(
            "Force of Will", "Force of Negation", "Flusterstorm", "Pact of Negation", "Mental Misstep", "Misdirection",
            "Daze", "Spell Pierce", "Spell Snare", "Mana Drain", "Counterspell", "Fierce Guardianship"));

    private static Spec spec(final Card c) {
        final String n = c.getName();
        final boolean protect = PROTECTION.contains(n);
        if (isWinner(c)) {
            return new Spec(n, 0, 0, 0, 0, true, false);
        }
        if (AiCardLists.has(AiCardLists.Role.DOOMSDAY_DRAW, n)) {
            switch (n) {
                case "Gitaxian Probe":
                case "Street Wraith":
                    return new Spec(n, 0, 1, 0, 2, false, false); // free for two life
                case "Gush":
                    return new Spec(n, 2, 2, 0, 0, false, false); // the two Islands it returns pay for it
                default:
                    return new Spec(n, c.getCMC(), draws(c), 0, 0, false, false);
            }
        }
        if (AiCardLists.has(AiCardLists.Role.DOOMSDAY_MANA, n)) {
            switch (n) {
                case "Black Lotus":
                    return new Spec(n, 0, 0, 3, 0, false, false);
                case "Dark Ritual":
                    return new Spec(n, 1, 0, 3, 0, false, false);
                case "Mox Opal":
                    return new Spec(n, 0, 0, 0, 0, false, false); // no metalcraft in a pile deck
                default:
                    return new Spec(n, 0, 0, 1, 0, false, false);
            }
        }
        return new Spec(n, 0, 0, 0, 0, false, protect);
    }

    /** Untapped permanents that make mana now. */
    private static List<Card> sources(final Player ai) {
        final List<Card> list = new ArrayList<>();
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isTapped() || c.isCreature() || c.getManaAbilities().isEmpty()) {
                continue;
            }
            list.add(c);
        }
        return list;
    }

    private static boolean makes(final Card c, final String color) {
        for (final SpellAbility ma : c.getManaAbilities()) {
            try {
                if (ma.canProduce(color)) {
                    return true;
                }
            } catch (final RuntimeException e) {
                // a mana ability that cannot say: not counted
            }
        }
        return false;
    }

    /** What the player has to work with when the pile is drawn through: mana now, blue for the winner, the hand. */
    private static final class Start {
        int mana;
        boolean blueOk;
        boolean castable = true; // Doomsday itself can be paid (only asked before it is cast)
        int life;
        final List<Spec> hand = new ArrayList<>();
    }

    private static Start start(final Player ai) {
        final Start st = new Start();
        final List<Card> src = sources(ai);
        boolean doomsdayInHand = false;
        final List<Card> handCards = new ArrayList<>();
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (isDoomsday(c)) {
                doomsdayInHand = true;
            } else {
                handCards.add(c);
            }
        }
        int blueOnly = 0;
        int dual = 0;
        int blackOnly = 0;
        for (final Card c : src) {
            final boolean u = makes(c, "U");
            final boolean b = makes(c, "B");
            if (u && b) {
                dual++;
            } else if (u) {
                blueOnly++;
            } else if (b) {
                blackOnly++;
            }
        }
        // two blue sources are kept for the Oracle (the blue-only ones first), the rest must make the black for Doomsday
        final int fromBlueOnly = Math.min(2, blueOnly);
        final int needDual = 2 - fromBlueOnly;
        st.blueOk = needDual <= dual;
        // mana already floating (a Dark Ritual cast a moment ago) counts like a source: it is there for Doomsday, and a
        // planner that only sees the untapped lands calls the line lost the moment the Ritual is paid for
        final int floating = ai.getManaPool().totalMana();
        final int floatingBlack = ai.getManaPool().getAmountOfColor(MagicColor.BLACK);
        int mana = src.size() + floating;
        int handMana = 0;
        int anyColor = 0;
        boolean ritual = false;
        for (final Card c : handCards) {
            final Spec s = spec(c);
            if (doomsdayInHand && s.gain > 0) {
                handMana += s.gain - s.cost; // the mana cards are played to pay for Doomsday
                if (s.cost == 0) {
                    anyColor += s.gain;
                } else {
                    ritual = true;
                }
            } else {
                st.hand.add(s);
            }
        }
        if (doomsdayInHand) {
            int black = blackOnly + Math.max(0, dual - needDual) + anyColor + floatingBlack;
            if (ritual && black >= 1) {
                black += 2;
            }
            st.castable = black >= 3;
            mana += handMana - 3;
        }
        st.mana = mana;
        final int life = ai.getLife();
        st.life = life - (doomsdayInHand || spellOnStack(ai) ? (life + 1) / 2 : 0);
        return st;
    }

    private static boolean spellOnStack(final Player ai) {
        if (ai.getGame().getStack().isEmpty()) {
            return false; // peekAbility fails on an empty stack
        }
        final SpellAbility top = ai.getGame().getStack().peekAbility();
        return top != null && isDoomsday(top.getHostCard());
    }

    /** The draw-through: score of the pile, top first, or -1 when the winner is not cast with the library at two or less. */
    private static int simulate(final Start st, final List<Spec> pile) {
        final List<Spec> hand = new ArrayList<>(st.hand);
        int mana = st.mana;
        int life = st.life;
        int drawn = 0;
        boolean progress = true;
        while (progress) {
            progress = false;
            final int library = pile.size() - drawn;
            if (library <= 2 && mana >= 3 && st.blueOk) {
                for (final Spec s : hand) {
                    if (s.winner) {
                        int guard = 0;
                        for (final Spec h : hand) {
                            if (h.protect) {
                                guard++;
                            }
                        }
                        return 1000 + 2 * (mana - 3) + 3 * guard + life;
                    }
                }
            }
            for (final Spec s : new ArrayList<>(hand)) { // mana first
                if (s.gain > 0 && mana >= s.cost) {
                    mana += s.gain - s.cost;
                    hand.remove(s);
                    progress = true;
                    break;
                }
            }
            if (progress) {
                continue;
            }
            Spec best = null;
            for (final Spec s : hand) { // then the cards that draw the most for the least
                if (s.draw > 0 && mana >= s.cost && life > s.life && drawn + s.draw <= pile.size()
                        && (best == null || s.draw > best.draw || s.draw == best.draw && s.cost < best.cost)) {
                    best = s;
                }
            }
            if (best != null) {
                mana -= best.cost;
                life -= best.life;
                hand.remove(best);
                for (int i = 0; i < best.draw; i++) {
                    hand.add(pile.get(drawn++));
                }
                progress = true;
            }
        }
        return -1;
    }

    private static int search(final Start st, final List<Spec> cands, final int size, final List<Spec> chosen,
                              final boolean[] used, final List<Spec> bestPile, final int[] bestScore) {
        if (chosen.size() == size) {
            final int score = simulate(st, chosen);
            if (score > bestScore[0]) {
                bestScore[0] = score;
                bestPile.clear();
                bestPile.addAll(chosen);
            }
            return score;
        }
        final java.util.Set<String> tried = new java.util.HashSet<>();
        for (int i = 0; i < cands.size(); i++) {
            if (used[i] || !tried.add(cands.get(i).name)) {
                continue;
            }
            used[i] = true;
            chosen.add(cands.get(i));
            search(st, cands, size, chosen, used, bestPile, bestScore);
            chosen.remove(chosen.size() - 1);
            used[i] = false;
        }
        return 0;
    }

    /** The best ordered pile (top first) out of the candidates, or null when none of them wins this turn. */
    private static List<Spec> bestPile(final Start st, final List<Spec> cands, final int size) {
        if (cands.size() < size || !st.castable) {
            return null;
        }
        final List<Spec> best = new ArrayList<>();
        final int[] score = {-1};
        search(st, cands, size, new ArrayList<>(), new boolean[cands.size()], best, score);
        return score[0] >= 0 ? best : null;
    }

    /** The candidates of a pile: what draws and what makes mana, the winner, a protection spell, a few fillers. */
    private static List<Spec> candidates(final Player ai, final Iterable<Card> options) {
        final Map<String, Spec> seen = new LinkedHashMap<>();
        final Map<String, Integer> count = new HashMap<>();
        for (final Card c : options) {
            count.merge(c.getName(), 1, Integer::sum);
            seen.putIfAbsent(c.getName(), spec(c));
        }
        final List<Spec> useful = new ArrayList<>();
        final List<Spec> blanks = new ArrayList<>();
        for (final Spec s : seen.values()) {
            final int n = Math.min(count.get(s.name), s.useful() || s.protect ? 2 : 1);
            for (int i = 0; i < (s.winner ? 1 : n); i++) {
                if (s.useful()) {
                    useful.add(s);
                } else if (s.protect) {
                    blanks.add(0, s);
                } else if (blanks.size() < 3) {
                    blanks.add(s);
                }
            }
        }
        while (useful.size() > 12) {
            useful.remove(useful.size() - 1);
        }
        final List<Spec> all = new ArrayList<>(useful);
        for (int i = 0; i < blanks.size() && i < 3; i++) {
            all.add(blanks.get(i));
        }
        return all;
    }

    private static final Map<String, Boolean> WINS = java.util.Collections.synchronizedMap(new LinkedHashMap<>());

    /** Whether a pile exists, out of the library and graveyard, that wins this turn after Doomsday. */
    static boolean winsThisTurn(final Player ai) {
        final Start st = start(ai);
        final CardCollection pool = new CardCollection(ai.getCardsIn(ZoneType.Library));
        pool.addAll(ai.getCardsIn(ZoneType.Graveyard));
        final StringBuilder key = new StringBuilder().append(st.mana).append('/').append(st.life).append('/')
                .append(st.blueOk).append('/').append(st.castable);
        for (final Spec s : st.hand) {
            key.append('|').append(s.name);
        }
        key.append('#');
        final List<String> names = new ArrayList<>();
        for (final Card c : pool) {
            names.add(c.getName());
        }
        java.util.Collections.sort(names);
        key.append(names.hashCode());
        final Boolean known = WINS.get(key.toString());
        if (known != null) {
            return known;
        }
        final boolean wins = bestPile(st, candidates(ai, pool), PILE) != null;
        if (WINS.size() > 64) {
            WINS.clear();
        }
        WINS.put(key.toString(), wins);
        return wins;
    }

    private static boolean playable(final Player ai, final SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        if (sa.usesTargeting()) {
            // a play chosen without the heuristics is not aimed by them: Ancestral Recall draws for the AI itself
            sa.resetTargets();
            if (!sa.canTarget(ai)) {
                return false;
            }
            sa.getTargets().add(ai);
        }
        final boolean ok = sa.canPlay() && ComputerUtilCost.canPayCost(sa, ai, false);
        if (!ok && sa.usesTargeting()) {
            sa.resetTargets();
        }
        return ok;
    }

    /**
     * The next play of the draw-through once Doomsday has cut the library: the cheapest draw that takes the most cards
     * (a free cycling or Probe first), mana (Black Lotus, a Ritual) when no draw is affordable, and the winner once the
     * library is at two or less. Null when the pile does not win, or this is not the time. No rollouts are needed: the
     * line is the one the planner simulated.
     */
    public static SpellAbility sureDraw(final Player ai, final List<SpellAbility> candidates) {
        if (ai == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        final int library = ai.getCardsIn(ZoneType.Library).size();
        if (!ph.isPlayerTurn(ai) || !ai.getGame().getStack().isEmpty() || !(ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2))
                || library < 1 || library > PILE) {
            return null;
        }
        final List<Spec> pile = new ArrayList<>();
        boolean winnerInPile = false;
        for (final Card c : ai.getCardsIn(ZoneType.Library)) {
            pile.add(spec(c));
            winnerInPile |= isWinner(c);
        }
        if (!winnerAvailable(ai) && !winnerInPile) {
            return null;
        }
        final Start st = start(ai);
        if (simulate(st, pile) < 0) {
            return null;
        }
        SpellAbility bestDraw = null;
        Spec bestSpec = null;
        SpellAbility mana = null;
        SpellAbility winner = null;
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host == null || !host.isInZone(ZoneType.Hand) || !ai.equals(host.getController())) {
                continue;
            }
            final Spec s = spec(host);
            if (s.winner) {
                if (library <= 2 && sa.isSpell() && st.blueOk && winner == null && playable(ai, sa)) {
                    winner = sa;
                }
            } else if (s.draw > 0 && s.draw <= library && (host.isCreature() ? sa.isCycling() : sa.isSpell() || sa.isCycling())) {
                if ((bestSpec == null || s.draw > bestSpec.draw || s.draw == bestSpec.draw && s.cost < bestSpec.cost)
                        && playable(ai, sa)) {
                    bestDraw = sa;
                    bestSpec = s;
                }
            } else if (s.gain > 0 && sa.isSpell() && mana == null && playable(ai, sa)) {
                mana = sa;
            }
        }
        if (winner != null) {
            return winner;
        }
        return bestDraw != null ? bestDraw : mana;
    }

    /** The picks of the Doomsday being resolved, in pile order (top first): a plan is made on the first pick. */
    private static final Map<Player, List<String>> PLANS = java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Player, Integer> SERVED = java.util.Collections.synchronizedMap(new WeakHashMap<>());

    private static Card planned(final Player ai, final CardCollectionView options) {
        List<String> plan = PLANS.get(ai);
        int served = SERVED.getOrDefault(ai, 0);
        boolean valid = plan != null && served > 0 && served < plan.size();
        if (valid) {
            for (int i = served; i < plan.size(); i++) {
                if (!hasName(options, plan.get(i))) {
                    valid = false;
                }
            }
        }
        if (!valid) {
            final Start st = start(ai);
            final List<Spec> best = bestPile(st, candidates(ai, options), PILE);
            if (best == null) {
                PLANS.remove(ai);
                SERVED.remove(ai);
                return null;
            }
            plan = new ArrayList<>();
            for (final Spec s : best) {
                plan.add(s.name);
            }
            PLANS.put(ai, plan);
            served = 0;
        }
        for (final Card c : options) {
            if (c.getName().equals(plan.get(served))) {
                if (served + 1 >= plan.size()) {
                    PLANS.remove(ai);
                    SERVED.remove(ai);
                } else {
                    SERVED.put(ai, served + 1);
                }
                return c;
            }
        }
        return null;
    }

    private static boolean hasName(final CardCollectionView options, final String name) {
        for (final Card c : options) {
            if (c.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The order the five cards of the pile are put on top of the library in, bottom first (the last card is put on top),
     * or null when the caller should keep its own order.
     */
    public static CardCollectionView order(final Player ai, final CardCollectionView cards) {
        if (ai == null || cards == null || cards.size() < 2 || cards.size() > PILE + 1) {
            return null;
        }
        final Start st = start(ai);
        final List<Spec> specs = new ArrayList<>();
        for (final Card c : cards) {
            specs.add(spec(c));
        }
        List<Spec> best = null;
        final int[] score = {-1};
        final List<Spec> found = new ArrayList<>();
        if (st.castable) {
            search(st, specs, cards.size(), new ArrayList<>(), new boolean[specs.size()], found, score);
            if (score[0] >= 0) {
                best = found;
            }
        }
        final List<Card> top = new ArrayList<>(); // top first
        if (best != null) {
            final List<Card> rest = new ArrayList<>(cards);
            for (final Spec s : best) {
                for (final Card c : rest) {
                    if (c.getName().equals(s.name)) {
                        top.add(c);
                        rest.remove(c);
                        break;
                    }
                }
            }
        } else { // no win in sight: the winner at the bottom, the cards that draw on top
            final List<Card> rest = new ArrayList<>(cards);
            rest.sort((a, b) -> {
                final int wa = isWinner(a) ? 1 : 0;
                final int wb = isWinner(b) ? 1 : 0;
                return wa != wb ? wa - wb : score(b) - score(a);
            });
            top.addAll(rest);
        }
        final CardCollection bottomFirst = new CardCollection();
        for (int i = top.size() - 1; i >= 0; i--) {
            bottomFirst.add(top.get(i));
        }
        return bottomFirst;
    }

    private static int draws(final Card c) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
                if (a.getApi() == ApiType.Draw) {
                    try {
                        return Integer.parseInt(a.getParamOrDefault("NumCards", "1").trim());
                    } catch (final NumberFormatException e) {
                        return 1;
                    }
                }
            }
        }
        return 1; // cycling (Street Wraith), Probe's reveal and draw
    }

    /** What a card is worth in the pile: the cards it draws first, then mana, the cheaper the better. 0 for the rest. */
    static int score(final Card c) {
        if (AiCardLists.has(AiCardLists.Role.DOOMSDAY_DRAW, c.getName())) {
            return 100 + 20 * draws(c) - 3 * c.getCMC();
        }
        if (AiCardLists.has(AiCardLists.Role.DOOMSDAY_MANA, c.getName())) {
            return 50 - 3 * c.getCMC();
        }
        return 0;
    }

    /**
     * The card Doomsday should keep next, or null for no opinion. The winner goes into the pile when none is in hand,
     * the rest of it is the cards that draw the most for the least.
     */
    public static Card pick(final Player ai, final CardCollectionView options) {
        if (ai == null || options == null || options.isEmpty()) {
            return null;
        }
        try {
            final Card planned = planned(ai, options);
            if (planned != null) {
                return planned;
            }
        } catch (final RuntimeException e) {
            // a pile that cannot be planned falls back to the scores below
        }
        if (!winnerAvailable(ai)) {
            for (final Card c : options) {
                if (isWinner(c)) {
                    return c;
                }
            }
        }
        Card best = null;
        int bestScore = 0;
        for (final Card c : options) {
            final int s = score(c);
            if (s > bestScore || s == bestScore && best != null && s > 0 && c.getName().compareTo(best.getName()) < 0) {
                best = c;
                bestScore = s;
            }
        }
        return best;
    }

    /**
     * Progress of a Doomsday deck in evaluator points. A cut library with the winner at hand is almost the game, and
     * every card drawn out of the pile brings it closer (without that, drawing through the pile is worth nothing to the
     * rollouts: the cycled card is replaced by the card it draws and the score does not move).
     */
    public static int term(final Player ai) {
        final int library = ai.getCardsIn(ZoneType.Library).size();
        if (library > 2 * PILE) {
            return 0;
        }
        if (winnerAvailable(ai)) {
            return library <= PILE ? 200 + 15 * (PILE - library) : 60;
        }
        if (library <= PILE) { // the winner may be one of the cards of the pile, drawn through
            final CardCollection top = new CardCollection(ai.getCardsIn(ZoneType.Library));
            boolean inPile = false;
            for (final Card c : top) {
                inPile |= isWinner(c);
            }
            if (inPile) {
                final Start st = start(ai);
                final List<Spec> pile = new ArrayList<>();
                for (final Card c : top) {
                    pile.add(spec(c));
                }
                return 80 + 15 * (PILE - library) + (simulate(st, pile) >= 0 ? 100 : 0);
            }
        }
        return -100; // a cut library and nothing to win with
    }
}
