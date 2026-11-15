package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;

/**
 * The known combos of Vintage, Legacy and Modern as data, read from {@code ai-combos.txt} (add a line there to teach the AI a new combo): every combo is a list of slots, each slot is one card or a
 * choice between cards ("Voltaic Key / Manifold Key"). A combo is assembled when every slot is filled by a card.
 * <ul>
 *   <li>For the AI's own deck ({@link ComboPlan.Kind #CATALOG}): the combos the deck can make are read once, and the
 *       rollouts are rewarded for having every slot in the hand or on the battlefield ({@link #term}).</li>
 *   <li>For the opponents ({@link AntiCombo}): a combo whose slots are all in view (battlefield, graveyard, stack) is a
 *       threat, and the cards that fill its slots are what a removal spell is aimed at ({@link #visibleThreat},
 *       {@link #isPiece}). Nothing in a hidden zone is looked at.</li>
 * </ul>
 * The steps of a combo (the loops) are found by the rollouts themselves; a loop that never ends cannot be simulated, so
 * the catalog only tells the AI which cards belong together, that they are worth keeping together, and that the
 * opponent's pair is worth breaking. {@code LOCK} combos do not win by themselves: they stop the opponent from playing.
 */
public final class ComboCatalog {
    /** Whether the combo wins the game or locks the opponent out of it. */
    public enum Type { WIN, LOCK }

    /** One combo: its name and the slots that must all be filled. */
    public static final class Entry {
        public final int number;
        public final String name;
        public final Type type;
        /** A two card combo of the Commander lists (see {@link AiCardLists}), not a line of ai-combos.txt. */
        public final boolean pair;
        /** Normalized names, one set per slot. */
        final List<Set<String>> slots;
        /** The slots as written in the file (card names with their own spelling). */
        private final List<String> rawSlots;

        Entry(final int number, final Type type, final String name, final String... slotSpecs) {
            this(number, type, false, name, slotSpecs);
        }

        Entry(final int number, final Type type, final boolean pair, final String name, final String... slotSpecs) {
            this.pair = pair;
            this.number = number;
            this.name = name;
            this.type = type;
            final List<Set<String>> s = new ArrayList<>();
            this.rawSlots = Collections.unmodifiableList(new ArrayList<>(java.util.Arrays.asList(slotSpecs)));
            for (final String spec : slotSpecs) {
                final Set<String> alternatives = new LinkedHashSet<>();
                for (final String n : spec.split(" / ")) {
                    alternatives.add(AiCardLists.normalize(n));
                }
                s.add(Collections.unmodifiableSet(alternatives));
            }
            this.slots = Collections.unmodifiableList(s);
        }

        /** The slots as written in the file, a slot being "Card A" or "Card A / Card B". */
        public List<String> slotSpecs() {
            return rawSlots;
        }

        public int slotCount() {
            return slots.size();
        }

        boolean fillsSomeSlot(final Card c) {
            final String n = AiCardLists.normalize(c.getName());
            for (final Set<String> slot : slots) {
                if (slot.contains(n)) {
                    return true;
                }
            }
            return false;
        }

        /** The cards that fill the slots (a card fills one slot only; a slot needs one card), in slot order. */
        List<Card> fillers(final Iterable<Card> cards) {
            final List<Card> used = new ArrayList<>();
            for (int i = 0; i < slots.size(); i++) {
                for (final Card c : cards) {
                    if (!used.contains(c) && slots.get(i).contains(AiCardLists.normalize(c.getName()))) {
                        used.add(c);
                        break;
                    }
                }
            }
            return used;
        }

        /** How many slots the cards fill. */
        int filled(final Iterable<Card> cards) {
            return fillers(cards).size();
        }

        boolean assembledBy(final Iterable<Card> cards) {
            return filled(cards) == slots.size();
        }

        @Override
        public String toString() {
            return "#" + number + " " + name;
        }
    }

    private static Supplier<List<String>> file;
    private static List<Entry> entries;
    private static List<String> problems = Collections.emptyList();

    /**
     * Says where the combos are: the supplier returns the lines of {@code ai-combos.txt} (forge-ai cannot see the resource
     * folder, so the front end hands it over, as for {@link AiCardLists}). Forgets what was read before.
     */
    public static synchronized void configure(final Supplier<List<String>> lines) {
        file = lines;
        entries = null;
        pairEntries = null;
    }

    /** Forgets what was read, so the next question reads the file again (after it was edited, in tests). */
    public static synchronized void reload() {
        entries = null;
        pairEntries = null;
    }

    /** The lines of the file that could not be read as a combo, with the reason (for the data test). */
    public static synchronized List<String> problems() {
        entries();
        return problems;
    }

    /**
     * Reads {@code win | Name | slot | slot ...} (or {@code lock | ...}); a slot is a card or {@code Card A / Card B}.
     * Blank lines and what follows a {@code #} are ignored. A line that is not a combo is reported and skipped.
     */
    static List<Entry> parse(final List<String> lines, final List<String> problemList) {
        final List<Entry> result = new ArrayList<>();
        int number = 0;
        for (final String raw : lines) {
            final int hash = raw.indexOf('#');
            final String line = (hash < 0 ? raw : raw.substring(0, hash)).trim();
            if (line.isEmpty()) {
                continue;
            }
            final String[] parts = line.split("\\s*\\|\\s*");
            final String kind = parts[0].trim().toLowerCase(java.util.Locale.ROOT);
            if (parts.length < 4 || !(kind.equals("win") || kind.equals("lock"))) {
                problemList.add("'" + line + "': expected  win|lock | Name | slot | slot ...  with at least two slots");
                continue;
            }
            final String[] slots = new String[parts.length - 2];
            boolean empty = false;
            for (int i = 2; i < parts.length; i++) {
                slots[i - 2] = parts[i].trim();
                empty |= slots[i - 2].isEmpty();
            }
            if (empty) {
                problemList.add("'" + line + "': an empty slot");
                continue;
            }
            result.add(new Entry(++number, kind.equals("win") ? Type.WIN : Type.LOCK, parts[1].trim(), slots));
        }
        return Collections.unmodifiableList(result);
    }

    private static synchronized List<Entry> load() {
        if (entries == null) {
            final List<String> found = new ArrayList<>();
            List<String> lines = Collections.emptyList();
            if (file == null) {
                found.add("no combo file was configured (ComboCatalog.configure)");
            } else {
                try {
                    final List<String> read = file.get();
                    lines = read == null ? Collections.<String>emptyList() : read;
                } catch (final RuntimeException ex) {
                    found.add("the combo file could not be read: " + ex);
                }
            }
            entries = parse(lines, found);
            problems = Collections.unmodifiableList(found);
            for (final String problem : found) {
                System.err.println("ComboCatalog: " + problem);
            }
        }
        return entries;
    }

    /** What an assembled combo in the hand or on the battlefield is worth to the AI, in evaluator points. */
    public static final int ASSEMBLED_WIN = 160;
    static final int ASSEMBLED_LOCK = 100;
    /** All slots but one filled: the missing card is what the AI is looking for. */
    static final int ONE_MISSING = 30;
    /** A slot filled by a permanent is worth more than one filled by a card in hand: it is already working. */
    static final int ON_BATTLEFIELD = 10;
    /** What an assembled combo of an opponent in view is worth to the opponent. */
    public static final int VISIBLE_WIN = 140;
    public static final int VISIBLE_LOCK = 90;
    /** A card that fills a slot of a combo is aimed at with this share of the rank of a key card. */
    static final int RANK_PIECE_PERCENT = 40;
    /** The same when the other slots of the combo are already in view: that is the card that completes it. */
    static final int RANK_COMPLETING_PERCENT = 85;

    private ComboCatalog() {
    }

    public static List<Entry> entries() {
        return load();
    }

    /**
     * The cards of the pool (what a tutor can find) that complete a catalogued combo: the combo has every slot but one
     * filled by cards the AI has in play or in hand, and the card fills the empty slot. The best come first: a combo
     * that wins before one that locks, the cheaper card before the dearer, each card once. A combo of which nothing is
     * there yet is not served: a lone piece wins nothing, and the normal choice of the tutor knows better.
     * Empty when no combo is one card short.
     */
    public static List<Card> missingPieces(final Player ai, final Iterable<Card> pool) {
        final List<Card> out = new ArrayList<>();
        if (ai == null || pool == null) {
            return out;
        }
        final Set<String> have = new java.util.HashSet<>();
        for (final ZoneType z : new ZoneType[] { ZoneType.Battlefield, ZoneType.Hand }) {
            for (final Card c : ai.getCardsIn(z)) {
                have.add(AiCardLists.normalize(c.getName()));
            }
        }
        final List<Card> candidates = new ArrayList<>();
        final java.util.Map<Card, Integer> rank = new java.util.HashMap<>();
        for (final Entry e : entries()) {
            Set<String> empty = null;
            int open = 0;
            for (final Set<String> slot : e.slots) {
                boolean filled = false;
                for (final String n : slot) {
                    filled |= have.contains(n);
                }
                if (!filled) {
                    open++;
                    empty = slot;
                }
            }
            if (open != 1 || e.slots.size() < 2) {
                continue;
            }
            for (final Card c : pool) {
                if (empty.contains(AiCardLists.normalize(c.getName())) && !c.isLand()) {
                    final int r = (e.type == Type.WIN ? 200 : 100) - c.getCMC();
                    if (!rank.containsKey(c) || rank.get(c) < r) {
                        rank.put(c, r);
                    }
                    if (!candidates.contains(c)) {
                        candidates.add(c);
                    }
                }
            }
        }
        candidates.sort((x, y) -> Integer.compare(rank.get(y), rank.get(x)));
        final Set<String> names = new java.util.HashSet<>();
        for (final Card c : candidates) {
            if (names.add(c.getName())) {
                out.add(c);
            }
        }
        return out;
    }

    /** The first number of the two card combos of the Commander lists: above every line of ai-combos.txt. */
    public static final int PAIR_NUMBER_BASE = 1000;

    private static List<Entry> pairEntries;

    /**
     * The two card combos of {@code commander-bracket-combos.txt} and {@code ai-commander-combos.txt} as entries (win
     * combos of two slots, numbered from {@link #PAIR_NUMBER_BASE}). They are not part of {@link #entries()}: the
     * rollouts' terms and the opponent's threat list stay as they were, the combos are put together by
     * {@link ComboAssembly} and tested from here.
     */
    public static synchronized List<Entry> commanderEntries() {
        if (pairEntries == null) {
            final List<Entry> list = new ArrayList<>();
            int number = PAIR_NUMBER_BASE;
            for (final AiCardLists.Pair p : AiCardLists.allPairs()) {
                if (p.firstKey.equals(p.secondKey)) {
                    continue;
                }
                list.add(new Entry(number++, Type.WIN, true, p.toString(), p.first, p.second));
            }
            pairEntries = Collections.unmodifiableList(list);
        }
        return pairEntries;
    }

    /** The lines of ai-combos.txt and then the two card combos of the Commander lists. */
    public static List<Entry> assemblyEntries() {
        final List<Entry> all = new ArrayList<>(load());
        all.addAll(commanderEntries());
        return all;
    }

    /** Whether the card fills a slot of any combo. */
    public static boolean isPiece(final Card c) {
        if (c == null) {
            return false;
        }
        for (final Entry e : load()) {
            if (e.fillsSomeSlot(c)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the card name fills a slot of any combo (for logs that only have names). */
    public static boolean isPieceName(final String name) {
        if (name == null) {
            return false;
        }
        final String n = AiCardLists.normalize(name);
        for (final Entry e : load()) {
            for (final Set<String> slot : e.slots) {
                if (slot.contains(n)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Whether this card is a piece of a catalogued winning combo. */
    public static boolean isWinComboPiece(final Card c) {
        if (c == null) {
            return false;
        }
        for (final Entry e : load()) {
            if (e.type == Type.WIN && e.fillsSomeSlot(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a temporary mana source in the Command zone may pay for this spell. The source and the spell must fill
     * different slots of the same WIN entry, and every other slot must already be represented by a card the AI owns
     * in its hand, on the battlefield, or in the Command zone. This keeps Channel's life-to-mana ability for its
     * catalogued payoff instead of spending life to cast unrelated cards.
     */
    public static boolean canUseCommandManaSourceFor(final Player ai, final Card source, final SpellAbility spell) {
        if (ai == null || source == null || spell == null || spell.getHostCard() == null) {
            return false;
        }
        final Card payoff = spell.getHostCard();
        if (source == payoff) {
            return false;
        }
        // Do not spend Channel's life to cast a second working legendary payoff such as Ulamog, the Ceaseless Hunger.
        if (payoff.isPermanent() && !payoff.ignoreLegendRule() && !payoff.hasSVar("AILegendaryException")
                && ComboGuard.duplicatesWorkingLegend(ai, payoff)) {
            return false;
        }

        final String sourceName = AiCardLists.normalize(source.getName());
        final String payoffName = AiCardLists.normalize(payoff.getName());
        final List<Card> available = own(ai, false);
        available.addAll(ai.getCardsIn(ZoneType.Command));
        if (!available.contains(source)) {
            available.add(source);
        }
        if (!available.contains(payoff)) {
            available.add(payoff);
        }

        for (final Entry e : load()) {
            if (e.type != Type.WIN) {
                continue;
            }
            for (int sourceSlot = 0; sourceSlot < e.slots.size(); sourceSlot++) {
                if (!e.slots.get(sourceSlot).contains(sourceName)) {
                    continue;
                }
                for (int payoffSlot = 0; payoffSlot < e.slots.size(); payoffSlot++) {
                    if (payoffSlot == sourceSlot || !e.slots.get(payoffSlot).contains(payoffName)) {
                        continue;
                    }

                    final List<Card> used = new ArrayList<>();
                    used.add(source);
                    used.add(payoff);
                    boolean complete = true;
                    for (int slot = 0; slot < e.slots.size(); slot++) {
                        if (slot == sourceSlot || slot == payoffSlot) {
                            continue;
                        }
                        Card filler = null;
                        for (final Card candidate : available) {
                            if (!used.contains(candidate)
                                    && e.slots.get(slot).contains(AiCardLists.normalize(candidate.getName()))) {
                                filler = candidate;
                                break;
                            }
                        }
                        if (filler == null) {
                            complete = false;
                            break;
                        }
                        used.add(filler);
                    }
                    if (complete) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** The combos a list of cards can make: every slot is filled by one of the cards. */
    public static List<Entry> assembledBy(final Iterable<Card> cards) {
        final List<Entry> result = new ArrayList<>();
        for (final Entry e : load()) {
            if (e.assembledBy(cards)) {
                result.add(e);
            }
        }
        return result;
    }

    /** The combos of a list of card names (a decklist). */
    public static List<Entry> inNames(final Iterable<String> names) {
        final Set<String> keys = AiCardLists.deckKeys(names);
        final List<Entry> result = new ArrayList<>();
        for (final Entry e : load()) {
            boolean all = true;
            for (final Set<String> slot : e.slots) {
                boolean found = false;
                for (final String n : slot) {
                    found |= keys.contains(n);
                }
                all &= found;
            }
            if (all) {
                result.add(e);
            }
        }
        return result;
    }

    /** The names of the pieces of the combos, for the plan of a deck. */
    static Set<String> pieceNames(final List<Entry> entries) {
        final Set<String> names = new LinkedHashSet<>();
        for (final Entry e : entries) {
            for (final Set<String> slot : e.slots) {
                names.addAll(slot);
            }
        }
        return names;
    }

    private static List<Card> own(final Player ai, final boolean battlefieldOnly) {
        final List<Card> cards = new ArrayList<>();
        cards.addAll(ai.getCardsIn(ZoneType.Battlefield));
        if (!battlefieldOnly) {
            cards.addAll(ai.getCardsIn(ZoneType.Hand));
        }
        return cards;
    }

    /**
     * The term for the AI's own combos: the points of the combos the hand and the battlefield assemble, a little for a
     * combo missing one card and for the slots that are already in play. The best combo only (they share their cards),
     * plus the slots in play of the others.
     */
    public static int term(final Player ai) {
        if (ai == null) {
            return 0;
        }
        final List<Card> cards = own(ai, false);
        final List<Card> board = own(ai, true);
        int best = 0;
        for (final Entry e : load()) {
            final List<Card> fillers = e.fillers(cards);
            final int filled = fillers.size();
            if (filled == 0) {
                continue;
            }
            // casting a spell of the combo (Doomsday, Tinker, Fireball) takes it out of the hand: it must never cost
            // points, so a combo with a spell in the hand only counts what is already on the battlefield
            boolean permanents = true;
            for (final Card c : fillers) {
                permanents &= c.isPermanent();
            }
            final int onBoard = e.filled(board);
            int points = ON_BATTLEFIELD * onBoard / 2;
            if (permanents && filled == e.slots.size()) {
                points = (e.type == Type.WIN ? ASSEMBLED_WIN : ASSEMBLED_LOCK) + ON_BATTLEFIELD * onBoard;
            } else if (permanents && filled == e.slots.size() - 1 && e.slots.size() >= 2) {
                points = ONE_MISSING + ON_BATTLEFIELD * onBoard / 2;
            }
            best = Math.max(best, points);
        }
        return best;
    }

    /** The cards of the opponents the AI can see: battlefield, graveyard (what has to be brought back) and stack. */
    private static List<Card> visible(final Player ai, final boolean withGraveyard) {
        final List<Card> cards = new ArrayList<>();
        for (final Player opp : ai.getOpponents()) {
            for (final Card c : opp.getCardsIn(ZoneType.Battlefield)) {
                if (!DonateCombo.isGiftedTarget(ai, c)) {
                    cards.add(c);
                }
            }
            if (withGraveyard) {
                cards.addAll(opp.getCardsIn(ZoneType.Graveyard));
            }
        }
        for (final SpellAbilityStackInstance si : ai.getGame().getStack()) {
            final Card host = si.getSourceCard();
            if (host != null && si.getActivatingPlayer() != null && si.getActivatingPlayer().isOpponentOf(ai)) {
                cards.add(host);
            }
        }
        return cards;
    }

    /** The combos the opponents have all the pieces of in view. */
    public static List<Entry> visibleCombos(final Player ai) {
        return ai == null ? Collections.<Entry>emptyList() : assembledBy(visible(ai, true));
    }

    /** The threat of the combos in view: the worst one counts, the others add a quarter each. */
    public static int visibleThreat(final Player ai) {
        int best = 0;
        int rest = 0;
        for (final Entry e : visibleCombos(ai)) {
            final int points = e.type == Type.WIN ? VISIBLE_WIN : VISIBLE_LOCK;
            if (points > best) {
                rest += best / 4;
                best = points;
            } else {
                rest += points / 4;
            }
        }
        return best + rest;
    }

    /**
     * The rank of an opponent's card as a target: a card that fills a slot of a combo is worth {@code RANK_PIECE_PERCENT}
     * of a key card, and {@code RANK_COMPLETING_PERCENT} when every other slot is in view already. 0 for any other card.
     */
    public static int rankPercent(final Player ai, final Card c) {
        if (ai == null || c == null || c.getController() == null || !c.getController().isOpponentOf(ai)) {
            return 0;
        }
        int percent = 0;
        final List<Card> seen = visible(ai, true);
        for (final Entry e : load()) {
            if (!e.fillsSomeSlot(c)) {
                continue;
            }
            percent = Math.max(percent, RANK_PIECE_PERCENT);
            final List<Card> others = new ArrayList<>(seen);
            others.remove(c);
            if (e.slots.size() >= 2 && e.filled(others) == e.slots.size() - 1 && e.filled(seen) >= e.slots.size() - 1) {
                percent = Math.max(percent, RANK_COMPLETING_PERCENT);
            }
        }
        return percent;
    }
}
