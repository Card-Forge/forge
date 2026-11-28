package forge.ai;

import java.util.*;
import java.util.function.Predicate;

import forge.ai.ability.TapAi;
import forge.ai.simulation.OpeningHand;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.cost.Cost;
import forge.game.cost.CostPayLife;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Expressions;

/**
 * Plans the land drops of the next turns so that the spells in hand (and the commander) can be cast as early as the
 * colours allow, which matters most for decks with several colours.
 * <p>
 * The old rule of thumb scored every land by how many <em>new</em> colours it adds, whatever the hand needs. This
 * planner looks at what the hand needs instead. For every land that can be played now it tries the lands that could
 * follow it on the next two turns, and for each such sequence it schedules the spells turn by turn: on every turn the
 * best spells that can <em>jointly</em> be paid with the mana sources of that turn are cast (a source that enters
 * tapped only counts from the next turn on, and a dual land can pay only one colour at a time, which is checked by
 * matching the coloured costs to the sources). Each sequence is judged with the lands it has played by then (an Island
 * first lets a Drowned Catacomb enter untapped) and with the life it has paid by then; a shockland enters untapped in
 * it only when {@link TapAi} would pay for that on that turn. A spell cast sooner is worth
 * more than the same spell cast later, and a small bonus for the colours the library still needs breaks ties between
 * lands that do the same for the hand.
 * <p>
 * Only the cards the AI can see are used (its hand, its battlefield, its commander, and the colours of its own
 * library as deck statistics), nothing about the opponent. The planner never plays a land by itself: it only answers
 * which of the lands the caller already allowed is best, or says that the difference is too small to matter, in which
 * case the caller keeps its own rule.
 */
public final class LandPlanner {
    /** -Dforge.landplanner.debug=true prints every decision to stderr. */
    private static final boolean DEBUG = Boolean.getBoolean("forge.landplanner.debug");

    /** How many turns are planned, this one included. */
    static final int HORIZON = 3;
    /** What a spell is worth when it can be cast on turn 0 (this turn), 1 and 2. */
    private static final double[] DECAY = { 1.0, 0.7, 0.5 };
    /** The best land must beat the next one by this much (about a tenth of a cheap spell) to be chosen by the planner. */
    static final double MIN_MARGIN = 10;
    /** Value of the first/second source of a colour that the library still asks for, at most (times the demand 0..1). */
    private static final double LIBRARY_COLOR_BONUS = 30;
    private static final int MAX_SPELLS = 12;
    /** What a life point is worth in the score: a tenth of a cheap spell, so that it breaks a tie between equal plans. */
    static final double LIFE_COST = 12;

    private static final String[] TYPE_LETTERS = { "W", "U", "B", "R", "G", "C" };
    private static final byte[] TYPE_BITS = { ManaAtom.WHITE, ManaAtom.BLUE, ManaAtom.BLACK, ManaAtom.RED,
            ManaAtom.GREEN, ManaAtom.COLORLESS };

    private LandPlanner() {
    }

    /** One unit of mana the AI will be able to make: the types it may be (one of them) and from which turn it is there. */
    static final class Src {
        final byte mask;
        final int from;
        /** Colours available only on the turn this land enters (Gleaming Bastion without a basic land). */
        final byte entryOnly;
        /** Colours that a land played later in the plan unlocks (an Island for a Gloomlake Verge), and from which turn. */
        final byte later;
        final int laterFrom;
        /** The permanent that makes the mana, null for a made-up source. */
        final Card host;
        /** The types of the mask that this source makes only by paying life or taking damage (Horizon Canopy). */
        final byte pain;
        /** The activation that makes this mana (0: its own): Ancient Tomb's two {C} come from one, and hurt once. */
        final int activation;
        /** The life that the activation costs, whatever number of its mana is spent. */
        final int life;

        Src(final byte mask, final int from) {
            this(mask, from, (byte) 0, (byte) 0, 0, 1);
        }

        Src(final byte mask, final int from, final byte pain) {
            this(mask, from, (byte) 0, pain, 0, 1);
        }

        Src(final byte mask, final int from, final byte pain, final int activation, final int life) {
            this(mask, from, (byte) 0, pain, activation, life);
        }

        Src(final byte mask, final int from, final byte entryOnly, final byte pain,
            final int activation, final int life) {
            this(mask, from, entryOnly, pain, activation, life, null, (byte) 0, 0);
        }

        private Src(final byte mask, final int from, final byte entryOnly, final byte pain, final int activation,
                    final int life, final Card host, final byte later, final int laterFrom) {
            this.host = host;
            this.later = later;
            this.laterFrom = laterFrom;
            this.mask = mask;
            this.from = from;
            this.entryOnly = entryOnly;
            this.pain = pain;
            this.activation = activation;
            this.life = life;
        }

        /** The types it can make on the turns to come, not just on the turn it comes in. */
        byte lasting() {
            return (byte) (mask | later);
        }

        byte allColors() {
            return (byte) (mask | entryOnly | later);
        }

        Src forTurn(final int turn) {
            final byte available = (byte) (mask | (turn == from ? entryOnly : 0) | (turn >= laterFrom ? later : 0));
            return new Src(available, from, (byte) (pain & available), activation, life);
        }
    }

    /** How a planned land comes in: when its mana is there, and the life paid to have it untapped. */
    static final class Entry {
        final int delay;
        final int life;

        Entry(final int delay, final int life) {
            this.delay = delay;
            this.life = life;
        }
    }

    /** A spell the hand wants to cast. */
    static final class Spell {
        final String name;
        /** The coloured parts of the cost, each as the set of mana types that can pay it. */
        final byte[] coloured;
        final int generic;
        final double weight;
        /** The card to cast, null for a made-up spell. */
        final Card card;

        Spell(final String name, final byte[] coloured, final int generic, final double weight) {
            this(name, coloured, generic, weight, null);
        }

        Spell(final String name, final byte[] coloured, final int generic, final double weight, final Card card) {
            this.card = card;
            this.name = name;
            this.coloured = coloured;
            this.generic = generic;
            this.weight = weight;
        }
    }

    /**
     * @param ai           the AI player
     * @param playable     the lands it may play now (the caller has already filtered them), at least two
     * @param entersTapped says whether a land will enter the battlefield tapped
     * @return the land the plan prefers, or null when the lands are too close to call (or nothing can be planned)
     */
    public static Card choose(final Player ai, final CardCollection playable, final Predicate<Card> entersTapped) {
        if (playable == null || playable.size() < 2) {
            return null;
        }
        try {
            final List<Spell> spells = spellsWanted(ai);
            final List<Src> base = battlefieldSources(ai);
            final double[] demand = libraryDemand(ai);
            final List<Card> handLands = new ArrayList<>(CardLists.filter(ai.getCardsIn(ZoneType.Hand), Card::isLand));

            Card best = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            double second = Double.NEGATIVE_INFINITY;
            final Set<String> seen = new HashSet<>();
            for (final Card land : playable) {
                if (!seen.add(land.getName() + "|" + entersTapped.test(land))) {
                    continue;
                }
                final List<Card> rest = new ArrayList<>(handLands);
                rest.remove(land);
                final Entry entry = entry(ai, land, entersTapped.test(land), base, spells, 0, List.of(), 0);
                final List<Src> srcs = unlockedBy(base, ai, List.of(land), 0);
                srcs.addAll(sourcesForLand(land, ai, Collections.singletonList(land), entry.delay, entry.delay == 0));
                final double score = bestFuture(ai, srcs, rest, 1, spells, demand, entersTapped,
                        List.of(land), entry.life) - LIFE_COST * entry.life;
                if (DEBUG) {
                    System.err.println("[landplanner] " + land + (entersTapped.test(land) ? " (tapped)" : "") + " = " + score);
                }
                if (score > bestScore) {
                    second = bestScore;
                    bestScore = score;
                    best = land;
                } else if (score > second) {
                    second = score;
                }
            }
            if (best == null || second == Double.NEGATIVE_INFINITY || bestScore - second < MIN_MARGIN) {
                return null;
            }
            if (DEBUG) {
                System.err.println("[landplanner] => " + best + " by " + (bestScore - second));
            }
            return best;
        } catch (final RuntimeException e) {
            return null; // a refinement only: the caller's rule decides
        }
    }

    /**
     * The best score over the lands that could be played on the turns after this one.
     *
     * @param played   the lands this sequence has played so far
     * @param lifePaid the life this sequence has paid so far to have lands enter untapped
     */
    private static double bestFuture(final Player ai, final List<Src> srcs, final List<Card> rest, final int turn,
                                     final List<Spell> spells, final double[] demand, final Predicate<Card> entersTapped,
                                     final List<Card> played, final int lifePaid) {
        if (turn >= HORIZON || rest.isEmpty()) {
            return score(srcs, spells, demand);
        }
        double best = Double.NEGATIVE_INFINITY;
        final Set<String> seen = new HashSet<>();
        for (final Card land : rest) {
            final boolean tapped = entersTappedAfter(land, ai, played, entersTapped);
            if (!seen.add(land.getName() + "|" + tapped)) {
                continue;
            }
            final List<Card> remaining = new ArrayList<>(rest);
            remaining.remove(land);
            final List<Card> then = new ArrayList<>(played);
            then.add(land);
            final Entry entry = entry(ai, land, tapped, srcs, spells, turn, played, lifePaid);
            final List<Src> next = unlockedBy(srcs, ai, then, turn);
            next.addAll(sourcesForLand(land, ai, then, turn + entry.delay, entry.delay == 0));
            best = Math.max(best, bestFuture(ai, next, remaining, turn + 1, spells, demand, entersTapped,
                    then, lifePaid + entry.life) - LIFE_COST * entry.life);
        }
        return best;
    }

    /**
     * Whether the land will enter tapped once the lands in {@code played} are on the battlefield too. The caller's
     * answer is about the battlefield as it is now. It is changed only for a land whose own condition counts
     * permanents (check lands, fast lands, slow lands) and gives another answer with those lands there. A land that
     * something else makes enter tapped now stays tapped.
     */
    static boolean entersTappedAfter(final Card land, final Player ai, final List<Card> played,
                                     final Predicate<Card> entersTapped) {
        final boolean now = entersTapped.test(land);
        for (final ReplacementEffect re : land.getReplacementEffects()) {
            final SpellAbility sa = re.ensureAbility();
            if (sa == null || sa.getApi() != ApiType.Tap || !sa.hasParam("ConditionPresent")
                    || sa.hasParam("ConditionPresent2") || sa.hasParam("ConditionDefined")
                    || sa.hasParam("ConditionZone")) {
                continue;
            }
            sa.setActivatingPlayer(ai);
            final boolean ownNow = presentConditionMet(land, sa, ai, Collections.emptyList());
            final boolean ownThen = presentConditionMet(land, sa, ai, played);
            if (ownNow == ownThen) {
                return now;
            }
            return now && !ownNow || ownThen; // tapped now though its own condition says no: that is something else
        }
        return now;
    }

    /** The ConditionPresent of the ability, counted on the battlefield with the planned lands added to it. */
    private static boolean presentConditionMet(final Card land, final SpellAbility sa, final Player ai,
                                               final List<Card> planned) {
        final List<Card> there = new ArrayList<>();
        for (final Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
            if (c != land) {
                there.add(c);
            }
        }
        there.addAll(planned);
        final String[] valid = sa.getParam("ConditionPresent").split(",");
        int count = 0;
        for (final Card c : there) {
            if (c.isValid(valid, ai, land, sa)) {
                count++;
            }
        }
        final String compare = sa.getParamOrDefault("ConditionCompare", "GE1");
        return Expressions.compare(count, compare, AbilityUtils.calculateAmount(land, compare.substring(2), sa));
    }

    // ----------------------------------------------------------------------------------------------- the schedule

    /** Spells cast as early as the sources allow, each worth less the later it is cast; plus the library colour bonus. */
    static double score(final List<Src> srcs, final List<Spell> spells, final double[] demand) {
        final boolean[] done = new boolean[spells.size()];
        double total = 0;
        for (int t = 0; t < HORIZON; t++) {
            final List<Src> avail = availableOn(srcs, t);
            final List<byte[]> coloured = new ArrayList<>();
            int generic = 0;
            for (final Spell sp : castOn(avail, spells, done)) {
                coloured.add(sp.coloured);
                generic += sp.generic;
                total += sp.weight * DECAY[t];
            }
            total -= painCost(avail, coloured, generic);
        }
        // the colours the library will ask for: a first source of a colour is worth more than a second
        final int[] sources = new int[5];
        for (final Src s : srcs) {
            for (int c = 0; c < 5; c++) {
                if ((s.lasting() & TYPE_BITS[c]) != 0) {
                    sources[c]++;
                }
            }
        }
        for (int c = 0; c < 5; c++) {
            final double share = sources[c] == 0 ? 0 : sources[c] == 1 ? 0.6 : 1.0;
            total += LIBRARY_COLOR_BONUS * demand[c] * share;
        }
        return total;
    }

    /** The sources that make mana on the turn, each with the types it can make then. */
    private static List<Src> availableOn(final List<Src> srcs, final int turn) {
        final List<Src> avail = new ArrayList<>();
        for (final Src s : srcs) {
            if (s.from <= turn) {
                final Src active = s.forTurn(turn);
                if (active.mask != 0) {
                    avail.add(active);
                }
            }
        }
        return avail;
    }

    /** The spells not {@code done} yet that the sources of a turn can pay together, best first; marks them done. */
    private static List<Spell> castOn(final List<Src> avail, final List<Spell> spells, final boolean[] done) {
        final List<Spell> cast = new ArrayList<>();
        final List<byte[]> coloured = new ArrayList<>();
        int generic = 0;
        for (int i = 0; i < spells.size(); i++) { // spells are sorted by weight, best first
            if (done[i]) {
                continue;
            }
            final Spell sp = spells.get(i);
            coloured.add(sp.coloured);
            if (canPay(coloured, generic + sp.generic, avail)) {
                generic += sp.generic;
                done[i] = true;
                cast.add(sp);
            } else {
                coloured.remove(coloured.size() - 1);
            }
        }
        return cast;
    }

    /** What the schedule has done on the turns before one: the cards it cast, and the life that their mana cost. */
    static final class Past {
        final Set<Card> cast = new HashSet<>();
        int painLife;
    }

    static Past before(final List<Src> srcs, final List<Spell> spells, final int turn) {
        final boolean[] done = new boolean[spells.size()];
        final Past past = new Past();
        for (int t = 0; t < turn; t++) {
            final List<Src> avail = availableOn(srcs, t);
            final List<byte[]> coloured = new ArrayList<>();
            int generic = 0;
            for (final Spell sp : castOn(avail, spells, done)) {
                coloured.add(sp.coloured);
                generic += sp.generic;
                if (sp.card != null) {
                    past.cast.add(sp.card);
                }
            }
            past.painLife += (int) Math.round(painCost(avail, coloured, generic) / LIFE_COST);
        }
        return past;
    }

    /** Up to this many painful activations are tried in every combination; more are chosen cheapest first. */
    private static final int EXACT_ACTIVATIONS = 8;

    /**
     * What the spells of a turn cost in life: the cheapest set of painful activations (Horizon Canopy, Ancient Tomb)
     * that the payment needs, times {@link #LIFE_COST}. An activation costs its life once, however many of the mana it
     * makes are spent (an Ancient Tomb hurts for 2 even for one {C}), and a turn that a painless land pays for costs
     * nothing, so the plan with the Forest first beats the plan with the Canopy first when only green is wanted.
     */
    static double painCost(final List<Src> avail, final List<byte[]> coloured, final int generic) {
        if (coloured.isEmpty() && generic == 0) {
            return 0;
        }
        // the painful sources by the activation they come from: key and the life it costs
        final Map<Integer, Integer> life = new LinkedHashMap<>();
        final int[] key = new int[avail.size()];
        for (int i = 0; i < avail.size(); i++) {
            final Src src = avail.get(i);
            if (src.pain == 0) {
                continue;
            }
            key[i] = src.activation != 0 ? src.activation : -(i + 1); // a source of its own: its own activation
            life.merge(key[i], src.life, Math::min);
        }
        if (life.isEmpty()) {
            return 0;
        }
        final List<Integer> activations = new ArrayList<>(life.keySet());
        if (activations.size() <= EXACT_ACTIVATIONS) {
            double best = Double.POSITIVE_INFINITY;
            for (int subset = 0; subset < 1 << activations.size(); subset++) {
                int cost = 0;
                final Set<Integer> allowed = new HashSet<>();
                for (int j = 0; j < activations.size(); j++) {
                    if ((subset & 1 << j) != 0) {
                        allowed.add(activations.get(j));
                        cost += life.get(activations.get(j));
                    }
                }
                if (cost < best && canPay(coloured, generic, withPain(avail, key, allowed))) {
                    best = cost;
                }
            }
            return best == Double.POSITIVE_INFINITY ? LIFE_COST * sum(life) : LIFE_COST * best;
        }
        // many painful sources: allow the cheapest ones until the payment works, then give back what is not needed
        activations.sort((x, y) -> Integer.compare(life.get(x), life.get(y)));
        final Set<Integer> allowed = new HashSet<>();
        for (final int activation : activations) {
            allowed.add(activation);
            if (canPay(coloured, generic, withPain(avail, key, allowed))) {
                break;
            }
        }
        final List<Integer> used = new ArrayList<>(allowed);
        used.sort((x, y) -> Integer.compare(life.get(y), life.get(x)));
        for (final int activation : used) {
            allowed.remove(activation);
            if (!canPay(coloured, generic, withPain(avail, key, allowed))) {
                allowed.add(activation);
            }
        }
        int cost = 0;
        for (final int activation : allowed) {
            cost += life.get(activation);
        }
        return LIFE_COST * cost;
    }

    private static int sum(final Map<Integer, Integer> life) {
        int total = 0;
        for (final int l : life.values()) {
            total += l;
        }
        return total;
    }

    /** The sources, where the ones of an activation that may not hurt make only their painless types (or nothing). */
    private static List<Src> withPain(final List<Src> avail, final int[] key, final Set<Integer> allowed) {
        final List<Src> trial = new ArrayList<>();
        for (int i = 0; i < avail.size(); i++) {
            final Src src = avail.get(i);
            if (src.pain == 0 || allowed.contains(key[i])) {
                trial.add(src);
            } else if ((src.mask & ~src.pain) != 0) {
                trial.add(new Src((byte) (src.mask & ~src.pain), src.from));
            }
        }
        return trial;
    }

    /**
     * Whether the sources can pay all the coloured parts and the generic mana together. The coloured parts are matched
     * to distinct sources (a dual land pays one of its colours), whatever is left pays the generic mana.
     */
    static boolean canPay(final List<byte[]> colouredCosts, final int generic, final List<Src> avail) {
        final List<Byte> reqs = new ArrayList<>();
        for (final byte[] cost : colouredCosts) {
            for (final byte b : cost) {
                reqs.add(b);
            }
        }
        if (reqs.size() + generic > avail.size()) {
            return false;
        }
        // the pickiest requirements first, so that the search stays small
        reqs.sort((a, b) -> Integer.compare(Integer.bitCount(a), Integer.bitCount(b)));
        final byte[] masks = new byte[avail.size()];
        for (int i = 0; i < masks.length; i++) {
            masks[i] = avail.get(i).mask;
        }
        return match(reqs, 0, masks, new boolean[masks.length], avail.size() - reqs.size() - generic);
    }

    /** {@code spare} is how many sources may stay unused: the sources minus the coloured parts minus the generic mana. */
    private static boolean match(final List<Byte> reqs, final int i, final byte[] masks, final boolean[] used, final int spare) {
        if (i == reqs.size()) {
            return spare >= 0;
        }
        final byte req = reqs.get(i);
        final Set<Byte> tried = new HashSet<>(); // sources with the same mask are interchangeable
        for (int s = 0; s < masks.length; s++) {
            if (used[s] || (masks[s] & req) == 0 || !tried.add(masks[s])) {
                continue;
            }
            used[s] = true;
            final boolean ok = match(reqs, i + 1, masks, used, spare);
            used[s] = false;
            if (ok) {
                return true;
            }
        }
        return false;
    }

    // ----------------------------------------------------------------------------------------------- what is wanted

    /** The spells in hand and the commander in the command zone, best first; at most {@link #MAX_SPELLS}. */
    static List<Spell> spellsWanted(final Player ai) {
        final List<Spell> out = new ArrayList<>();
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.isLand()) {
                continue;
            }
            addSpell(out, c, 0, 1.0);
        }
        for (final Card c : ai.getCardsIn(ZoneType.Command)) {
            if (c.isCommander() && !c.isLand()) {
                int tax = 0;
                try {
                    tax = 2 * ai.getCommanderCast(c);
                } catch (final RuntimeException e) {
                    tax = 0;
                }
                addSpell(out, c, tax, 1.5); // the card the deck is built around is worth getting online
            }
        }
        out.sort((a, b) -> Double.compare(b.weight, a.weight));
        while (out.size() > MAX_SPELLS) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    private static void addSpell(final List<Spell> out, final Card c, final int extraGeneric, final double factor) {
        final ManaCost cost = c.getManaCost();
        if (cost == null || cost.isNoCost()) {
            return;
        }
        final List<Byte> coloured = new ArrayList<>();
        int generic = Math.max(0, cost.getGenericCost()) + extraGeneric;
        for (final ManaCostShard shard : cost) {
            if (shard.isPhyrexian() || shard.isOfKind(ManaAtom.IS_X)) {
                continue; // paid with life, or X = 0 for planning
            }
            if (shard.isOr2Generic()) {
                generic += 2; // {2/W}: planned as the expensive way, which never over-promises
                continue;
            }
            final byte mask = (byte) (shard.getShard() & ManaAtom.ALL_MANA_TYPES);
            if (mask == 0) {
                generic++;
            } else {
                coloured.add(mask);
            }
        }
        final byte[] arr = new byte[coloured.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = coloured.get(i);
        }
        final int cmc = Math.min(8, Math.max(1, cost.getCMC()));
        out.add(new Spell(c.getName(), arr, generic, (60 + 30 * cmc) * factor, c));
    }

    /** How much of its spells the library has a pip of each colour for, 0..1 (WUBRG order). */
    static double[] libraryDemand(final Player ai) {
        final double[] demand = new double[5];
        int spells = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Library)) {
            if (c.isLand() || c.getManaCost() == null || c.getManaCost().isNoCost()) {
                continue;
            }
            spells++;
            final int[] pips = c.getManaCost().getColorShardCounts();
            for (int i = 0; i < 5 && i < pips.length; i++) {
                if (pips[i] > 0) {
                    demand[i]++;
                }
            }
        }
        if (spells == 0) {
            return demand;
        }
        for (int i = 0; i < 5; i++) {
            demand[i] = Math.min(1.0, 2.0 * demand[i] / spells); // a colour in half of the spells counts in full
        }
        return demand;
    }

    // ----------------------------------------------------------------------------------------------- the sources

    /** The mana the AI has on the battlefield: a source that is tapped (or a summoning sick creature) counts from next turn. */
    static List<Src> battlefieldSources(final Player ai) {
        final List<Src> out = new ArrayList<>();
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isPhasedOut() || c.getManaAbilities().isEmpty()) {
                continue;
            }
            final int from = c.isTapped() || c.isCreature() && c.isSick() ? 1 : 0;
            addSources(out, c, ai, from);
        }
        return out;
    }

    /**
     * How a land comes in on a turn of the plan. A land that enters tapped makes mana from the next turn, and so does a
     * land that is a creature (Dryad Arbor), which is summoning sick. A shockland comes in untapped for its life exactly
     * when {@link TapAi} would pay it then: the plan cannot count on a payment that its rule refuses, nor save the life
     * of one that it makes.
     *
     * @param srcs     the sources of the plan before this land
     * @param turn     the turn of the plan on which the land is played, 0 for this one
     * @param played   the lands the plan has played before
     * @param lifePaid the life the plan has paid before to have lands enter untapped; what its painful mana has cost
     *                 by this turn is worked out from {@code srcs} and counts too
     */
    static Entry entry(final Player ai, final Card land, final boolean tapped, final List<Src> srcs,
                       final List<Spell> spells, final int turn, final List<Card> played, final int lifePaid) {
        final int sick = land.isCreature() && !land.hasKeyword(Keyword.HASTE) ? 1 : 0;
        if (tapped) {
            final int life = lifeToEnterUntapped(land);
            if (life > 0) {
                final Past past = before(srcs, spells, turn);
                if (wouldPayToEnterUntapped(ai, land, life, turn, played, past.cast, lifePaid + past.painLife)) {
                    return new Entry(sick, life);
                }
            }
        }
        return new Entry(sick + (tapped ? 1 : 0), 0);
    }

    /**
     * Whether {@link TapAi} would pay {@code life} for the land on this turn of the plan. By then the lands in
     * {@code played} are in play, the cards in {@code cast} have left the hand, and from the next turn on every land
     * in play has untapped. For this turn it is the question the game itself will ask.
     */
    static boolean wouldPayToEnterUntapped(final Player ai, final Card land, final int life, final int turn,
                                           final List<Card> played, final Set<Card> cast, final int lifePaid) {
        final SpellAbility payment = lifePaymentAbility(land);
        if (payment == null) {
            return false;
        }
        payment.setActivatingPlayer(ai);
        final List<Card> untapped = new ArrayList<>();
        final List<Card> alsoThere = new ArrayList<>(played);
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isLand() && (turn > 0 || c.isUntapped())) {
                untapped.add(c);
            }
            if (turn > 0 && c.isTapped()) {
                alsoThere.add(c);
            }
        }
        untapped.addAll(played);
        final List<Card> hand = new ArrayList<>(ai.getCardsIn(ZoneType.Hand));
        hand.remove(land);
        hand.removeAll(played);
        hand.removeAll(cast);
        return TapAi.shouldPayForLandEntry(ai, land, payment, life, lifePaid, untapped, hand, alsoThere);
    }

    /**
     * Whether {@code life} can still be paid after the {@code lifePaid} that the planned sequence pays before it. The
     * payment is decided by TapAi with a reserve (10 life, or what the opponent hits for next turn plus 5).
     */
    static boolean canPayToEnterUntapped(final Player ai, final int life, final int lifePaid) {
        return ai.getLife() - lifePaid > life + 1 && TapAi.lifeCanBeSpared(ai, lifePaid + life);
    }

    /** The life a land asks to enter untapped ("you may pay 2 life"), 0 when it has no such choice. */
    private static int lifeToEnterUntapped(final Card land) {
        for (final ReplacementEffect re : land.getReplacementEffects()) {
            final SpellAbility sa = re.ensureAbility();
            if (sa == null || sa.getApi() != ApiType.Tap || !sa.hasParam("UnlessCost")) {
                continue;
            }
            final String unless = sa.getParam("UnlessCost");
            if (!unless.startsWith("PayLife<")) {
                continue;
            }
            final String amount = unless.substring("PayLife<".length()).split("[>/]")[0];
            try {
                return Integer.parseInt(amount.trim());
            } catch (final NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private static SpellAbility lifePaymentAbility(final Card land) {
        for (final ReplacementEffect re : land.getReplacementEffects()) {
            final SpellAbility sa = re.ensureAbility();
            if (sa != null && sa.getApi() == ApiType.Tap && sa.hasParam("UnlessCost")
                    && sa.getParam("UnlessCost").startsWith("PayLife<")) {
                return sa;
            }
        }
        return null;
    }

    /**
     * Adds the mana a permanent can make every turn: its mana abilities that cost nothing but tapping (an ability
     * that sacrifices something or needs mana of its own is not a source to plan with). A land that makes several
     * types at once (a bounce land) is one source for each type; a land that makes one of several is one source that
     * may be any of them. A land that only fetches (Evolving Wilds) makes the types of the lands it can find.
     */
    private static void addSources(final List<Src> out, final Card c, final Player ai, final int from) {
        addSources(out, c, ai, from, Collections.emptyList(), from == 0 && c.enteredThisTurn());
    }

    private static void addSources(final List<Src> out, final Card c, final Player ai, final int from,
                                   final List<Card> planned, final boolean entryTurnAvailable) {
        final int before = out.size();
        final CardCollection context = new CardCollection();
        context.addAll(planned);
        for (final Card candidate : ai.getCardsIn(ZoneType.Hand)) {
            if (OpeningHand.isFreePermanentEnabler(candidate)) {
                context.add(candidate);
            }
        }
        addSourcesOf(out, c, ai, from, context, entryTurnAvailable);
        final byte pain = painMask(c, ai);
        if (pain != 0) {
            final int life = painLife(c, ai);
            for (int i = before; i < out.size(); i++) {
                final Src s = out.get(i);
                out.set(i, new Src(s.mask, s.from, s.entryOnly, (byte) (pain & s.allColors()),
                        c.getId(), life)); // one activation for the card
            }
        }
        for (int i = before; i < out.size(); i++) {
            final Src s = out.get(i);
            out.set(i, new Src(s.mask, s.from, s.entryOnly, s.pain, s.activation, s.life, c, s.later, s.laterFrom));
        }
    }

    /**
     * The sources once the lands in {@code planned} are in play, the last of them from {@code turn} on. A land whose
     * ability counts what is in play may make more from then on than when its sources were first worked out.
     */
    static List<Src> unlockedBy(final List<Src> srcs, final Player ai, final List<Card> planned, final int turn) {
        final List<Src> out = new ArrayList<>(srcs);
        final Set<Card> judged = new HashSet<>();
        for (final Src first : srcs) {
            final Card host = first.host;
            if (host == null || !judged.add(host) || !hasPresenceCondition(host)) {
                continue;
            }
            final List<Integer> own = new ArrayList<>();
            for (int i = 0; i < srcs.size(); i++) {
                if (srcs.get(i).host == host) {
                    own.add(i);
                }
            }
            final List<Src> now = sourcesForLand(host, ai, planned, turn, false);
            if (now.size() != own.size()) {
                continue; // not the same mana to compare: leave it as it was
            }
            for (int j = 0; j < own.size(); j++) {
                final Src was = out.get(own.get(j));
                final Src is = now.get(j);
                final byte gained = (byte) (is.mask & ~was.lasting());
                if (gained == 0 || was.later != 0) {
                    continue;
                }
                final boolean hurts = (is.pain & gained) != 0;
                out.set(own.get(j), new Src(was.mask, was.from, was.entryOnly, (byte) (was.pain | is.pain & gained),
                        hurts ? is.activation : was.activation, hurts ? is.life : was.life, host, gained,
                        Math.max(turn, was.from)));
            }
        }
        return out;
    }

    private static boolean hasPresenceCondition(final Card c) {
        return c.getManaAbilities().stream().anyMatch(sa ->
                sa.getRestrictions() != null && sa.getRestrictions().getIsPresent() != null);
    }

    /** Sources for a land being added to a simulated land-drop sequence. */
    static List<Src> sourcesForLand(final Card land, final Player ai, final List<Card> planned,
                                    final int from, final boolean entryTurnAvailable) {
        final List<Src> out = new ArrayList<>();
        addSources(out, land, ai, from, planned, entryTurnAvailable);
        return out;
    }

    /** The life that the cheapest painful mana ability of the permanent costs: a paid life point or damage to you. */
    private static int painLife(final Card c, final Player ai) {
        int least = Integer.MAX_VALUE;
        for (final SpellAbility ma : ComputerUtilMana.getAIPlayableMana(c)) {
            if (!isPlannable(ma.getPayCosts()) || !isPainful(ma)) {
                continue;
            }
            int life = 0;
            for (final forge.game.cost.CostPart part : ma.getPayCosts().getCostParts()) {
                if (part instanceof CostPayLife) {
                    final Integer amount = ((CostPayLife) part).convertAmount();
                    life += amount == null ? 1 : amount;
                }
            }
            for (SpellAbility sub = ma.getSubAbility(); sub != null; sub = sub.getSubAbility()) {
                if (sub.getApi() == ApiType.DealDamage || sub.getApi() == ApiType.LoseLife) {
                    try {
                        life += Integer.parseInt(sub.getParamOrDefault(sub.getApi() == ApiType.DealDamage ? "NumDmg" : "LifeAmount", "1"));
                    } catch (final NumberFormatException e) {
                        life += 1;
                    }
                }
            }
            least = Math.min(least, Math.max(1, life));
        }
        return least == Integer.MAX_VALUE ? 1 : least;
    }

    /** The types that a permanent can make only by paying life or taking damage: Horizon Canopy's {G} and {W}. */
    private static byte painMask(final Card c, final Player ai) {
        byte pain = 0;
        byte free = 0;
        for (final SpellAbility ma : ComputerUtilMana.getAIPlayableMana(c)) {
            final Cost cost = ma.getPayCosts();
            if (!isPlannable(cost)) {
                continue;
            }
            ma.setActivatingPlayer(ai);
            if (isPainful(ma)) {
                pain |= maskOf(ma);
            } else {
                free |= maskOf(ma);
            }
        }
        return (byte) (pain & ~free);
    }

    /** A cost that is paid every turn without mana: tapping, and paying life (Horizon Canopy), which is the pain. */
    private static boolean isPlannable(final Cost cost) {
        if (cost == null || cost.hasManaCost()) {
            return false;
        }
        for (final forge.game.cost.CostPart part : cost.getCostParts()) {
            if (!part.isReusable() && !(part instanceof CostPayLife)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPainful(final SpellAbility ma) {
        for (final forge.game.cost.CostPart part : ma.getPayCosts().getCostParts()) {
            if (part instanceof CostPayLife) {
                return true;
            }
        }
        for (SpellAbility sub = ma.getSubAbility(); sub != null; sub = sub.getSubAbility()) {
            if (sub.getApi() == ApiType.DealDamage || sub.getApi() == ApiType.LoseLife) {
                return true; // the damage dealt to you by an Adarkar Wastes or a Karplusan Forest
            }
        }
        return false;
    }

    private static void addSourcesOf(final List<Src> out, final Card c, final Player ai, final int from,
                                     final CardCollectionView context, final boolean entryTurnAvailable) {
        final List<SpellAbility> usable = new ArrayList<>();
        final List<SpellAbility> later = new ArrayList<>();
        for (final SpellAbility ma : ComputerUtilMana.getAIPlayableMana(c)) {
            final Cost cost = ma.getPayCosts();
            if (!isPlannable(cost)) {
                continue;
            }
            ma.setActivatingPlayer(ai);
            final boolean normal = OpeningHand.manaAbilityRequirementsMet(c, ma, context, false);
            final boolean active = entryTurnAvailable
                    ? OpeningHand.manaAbilityRequirementsMet(c, ma, context, true) : normal;
            if (normal) {
                later.add(ma);
            }
            if (active) {
                usable.add(ma);
            }
        }
        if (usable.isEmpty()) {
            final byte fetched = fetchMask(c, ai);
            if (fetched != 0) {
                out.add(new Src(fetched, from));
            }
            return;
        }
        if (usable.size() == 1) {
            final SpellAbility ma = usable.get(0);
            ma.setActivatingPlayer(ai);
            final boolean persistent = later.contains(ma);
            final AbilityManaPart mp = ma.getManaPart();
            if (mp != null && !mp.isAnyMana() && !mp.isComboMana()) {
                final byte[] each = simultaneous(ma, mp);
                if (each != null) {
                    for (final byte b : each) {
                        out.add(new Src(persistent ? b : 0, from, persistent ? (byte) 0 : b,
                                (byte) 0, 0, 1));
                    }
                    return;
                }
            }
            if (addChosenColorSources(out, ma, from, persistent)) {
                return;
            }
        }
        byte mask = 0;
        byte activeMask = 0;
        int amount = 1;
        for (final SpellAbility ma : usable) {
            ma.setActivatingPlayer(ai);
            activeMask |= maskOf(ma);
            if (later.contains(ma)) {
                mask |= maskOf(ma);
            }
            try {
                amount = Math.max(amount, ma.amountOfManaGenerated(true));
            } catch (final RuntimeException e) {
                // keep one
            }
        }
        final byte entryOnly = (byte) (activeMask & ~mask);
        if (mask == 0 && entryOnly == 0) {
            return;
        }
        for (int i = 0; i < Math.min(amount, 4); i++) {
            out.add(new Src(mask, from, entryOnly, (byte) 0, 0, 1));
        }
    }

    /** For "choose a colour" abilities (Nyx Lotus): sources of the one colour that makes the most, false when this is not that. */
    private static boolean addChosenColorSources(final List<Src> out, final SpellAbility ma, final int from,
                                                 final boolean persistent) {
        for (SpellAbility part = ma; part != null; part = part.getSubAbility()) {
            final AbilityManaPart mp = part.getManaPart();
            if (mp == null || !mp.choosesColorOnResolution(part)) {
                continue;
            }
            final int[] amounts = mp.amountsIfChosen(part);
            int best = 0;
            for (int i = 1; i < amounts.length; i++) {
                if (amounts[i] > amounts[best]) {
                    best = i;
                }
            }
            for (int i = 0; i < Math.min(amounts[best], 4); i++) {
                out.add(new Src(persistent ? TYPE_BITS[best] : 0, from,
                        persistent ? (byte) 0 : TYPE_BITS[best], (byte) 0, 0, 1));
            }
            return true;
        }
        return false;
    }

    /** For "{W}{U}" style abilities: one mask per type made at the same time, null when this is not that. */
    private static byte[] simultaneous(final SpellAbility ma, final AbilityManaPart mp) {
        final String produced;
        try {
            produced = mp.mana(ma);
        } catch (final RuntimeException e) {
            return null;
        }
        if (produced == null) {
            return null;
        }
        final String[] tokens = produced.trim().split("\\s+");
        if (tokens.length < 2) {
            return null;
        }
        final byte[] masks = new byte[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            masks[i] = 0;
            for (int t = 0; t < TYPE_LETTERS.length; t++) {
                if (TYPE_LETTERS[t].equals(tokens[i])) {
                    masks[i] = TYPE_BITS[t];
                }
            }
            if (masks[i] == 0) {
                return null; // something we do not understand: plan it as one source instead
            }
        }
        return masks;
    }

    private static byte maskOf(final SpellAbility ma) {
        byte mask = 0;
        for (int t = 0; t < TYPE_LETTERS.length; t++) {
            if (ma.canProduce(TYPE_LETTERS[t])) {
                mask |= TYPE_BITS[t];
            }
        }
        return mask;
    }

    /** The types a land that fetches another land can make: those of the lands it can find in the library. */
    private static byte fetchMask(final Card land, final Player ai) {
        for (final SpellAbility sa : land.getSpellAbilities()) {
            if (sa.getApi() != ApiType.ChangeZone || !"Library".equals(sa.getParamOrDefault("Origin", ""))
                    || !"Battlefield".equals(sa.getParamOrDefault("Destination", ""))) {
                continue;
            }
            final CardCollectionView lib = ai.getCardsIn(ZoneType.Library);
            CardCollection lands = CardLists.filter(lib, Card::isLand);
            final String type = sa.getParamOrDefault("ChangeType", "");
            if (!type.isEmpty()) {
                try {
                    lands = CardLists.getValidCards(lands, type, ai, land, sa);
                } catch (final RuntimeException e) {
                    // keep every land of the library
                }
            }
            byte mask = 0;
            for (final Card l : lands) {
                // This fetch chooses one land; another possible target cannot satisfy its condition.
                final CardCollection alone = new CardCollection(l);
                mask |= OpeningHand.potentialProducedColors(l, ai, alone);
                // those are colours only: a Wastes makes mana too, which pays the generic part of a cost
                for (final SpellAbility ma : ComputerUtilMana.getAIPlayableMana(l)) {
                    ma.setActivatingPlayer(ai);
                    if (ma.canProduce("C") && OpeningHand.manaAbilityRequirementsMet(l, ma, alone, true)) {
                        mask |= ManaAtom.COLORLESS;
                    }
                }
            }
            return mask;
        }
        return 0;
    }
}
