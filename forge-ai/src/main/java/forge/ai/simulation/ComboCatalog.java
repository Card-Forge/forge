package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;

/**
 * The known combos of Vintage, Legacy and Modern as data: every combo is a list of slots, each slot is one card or a
 * choice between cards ("Voltaic Key / Manifold Key"). A combo is assembled when every slot is filled by a card.
 * <ul>
 *   <li>For the AI's own deck ({@link ComboPlan.Kind#CATALOG}): the combos the deck can make are read once, and the
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
        /** Normalized names, one set per slot. */
        final List<Set<String>> slots;

        Entry(final int number, final Type type, final String name, final String... slotSpecs) {
            this.number = number;
            this.name = name;
            this.type = type;
            final List<Set<String>> s = new ArrayList<>();
            for (final String spec : slotSpecs) {
                final Set<String> alternatives = new LinkedHashSet<>();
                for (final String n : spec.split(" / ")) {
                    alternatives.add(AiCardLists.normalize(n));
                }
                s.add(Collections.unmodifiableSet(alternatives));
            }
            this.slots = Collections.unmodifiableList(s);
        }

        /** The names of every card that fills a slot (not normalized: for tests and messages). */
        public List<String> slotSpecs() {
            final List<String> result = new ArrayList<>();
            for (final Set<String> slot : slots) {
                result.add(String.join(" / ", slot));
            }
            return result;
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

    private static final List<Entry> ENTRIES;

    static {
        final List<Entry> e = new ArrayList<>();
        // ---- combos that win the game
        e.add(new Entry(1, Type.WIN, "Channel + Fireball", "Channel", "Fireball"));
        e.add(new Entry(2, Type.WIN, "Time Vault + Voltaic Key", "Time Vault", "Voltaic Key / Manifold Key"));
        e.add(new Entry(3, Type.WIN, "Flash + Protean Hulk", "Flash", "Protean Hulk"));
        e.add(new Entry(4, Type.WIN, "Melira + Murderous Redcap + Viscera Seer", "Melira, Sylvok Outcast", "Murderous Redcap",
                "Viscera Seer"));
        e.add(new Entry(5, Type.WIN, "Tinker + Blightsteel Colossus", "Tinker", "Blightsteel Colossus"));
        e.add(new Entry(6, Type.WIN, "Thassa's Oracle + Demonic Consultation", "Thassa's Oracle", "Demonic Consultation"));
        e.add(new Entry(7, Type.WIN, "Thassa's Oracle + Tainted Pact", "Thassa's Oracle", "Tainted Pact"));
        e.add(new Entry(7, Type.WIN, "Doomsday + Thassa's Oracle", "Doomsday", "Thassa's Oracle"));
        e.add(new Entry(8, Type.WIN, "Demonic Consultation + Laboratory Maniac", "Demonic Consultation", "Laboratory Maniac"));
        e.add(new Entry(9, Type.WIN, "Painter's Servant + Grindstone", "Painter's Servant", "Grindstone"));
        e.add(new Entry(10, Type.WIN, "Tolarian Academy + Mind Over Matter + Stroke of Genius", "Tolarian Academy",
                "Mind Over Matter", "Stroke of Genius"));
        e.add(new Entry(11, Type.WIN, "Hermit Druid + Dread Return", "Hermit Druid", "Dread Return"));
        e.add(new Entry(12, Type.WIN, "Ad Nauseam + Angel's Grace + Tendrils / Grapeshot", "Ad Nauseam", "Angel's Grace",
                "Tendrils of Agony / Grapeshot"));
        e.add(new Entry(13, Type.WIN, "High Tide + Palinchron + Stroke of Genius", "High Tide", "Palinchron", "Stroke of Genius"));
        e.add(new Entry(14, Type.WIN, "Tendrils + Lion's Eye Diamond + Infernal Tutor", "Tendrils of Agony", "Lion's Eye Diamond",
                "Infernal Tutor"));
        e.add(new Entry(14, Type.WIN, "Yawgmoth's Will + Black Lotus + Dark Ritual + Tendrils", "Yawgmoth's Will", "Black Lotus",
                "Dark Ritual", "Tendrils of Agony"));
        e.add(new Entry(15, Type.WIN, "Underworld Breach + Lion's Eye Diamond + Brain Freeze", "Underworld Breach",
                "Lion's Eye Diamond", "Brain Freeze"));
        e.add(new Entry(16, Type.WIN, "Splinter Twin + Deceiver Exarch / Pestermite", "Splinter Twin",
                "Deceiver Exarch / Pestermite"));
        e.add(new Entry(17, Type.WIN, "Kiki-Jiki + Restoration Angel / Bounding Krasis", "Kiki-Jiki, Mirror Breaker",
                "Restoration Angel / Bounding Krasis"));
        e.add(new Entry(18, Type.WIN, "Saheeli Rai + Felidar Guardian", "Saheeli Rai", "Felidar Guardian"));
        e.add(new Entry(19, Type.WIN, "Jeskai Ascendancy + Retraction Helix", "Jeskai Ascendancy", "Retraction Helix"));
        e.add(new Entry(20, Type.WIN, "Heliod, Sun-Crowned + Walking Ballista", "Heliod, Sun-Crowned", "Walking Ballista"));
        e.add(new Entry(21, Type.WIN, "Exquisite Blood + Sanguine Bond", "Exquisite Blood", "Sanguine Bond"));
        e.add(new Entry(22, Type.WIN, "Mikaeus, the Unhallowed + Triskelion", "Mikaeus, the Unhallowed", "Triskelion"));
        e.add(new Entry(23, Type.WIN, "Spike Feeder + Archangel of Thune", "Spike Feeder", "Archangel of Thune"));
        e.add(new Entry(24, Type.WIN, "Witherbloom Apprentice + Chain of Smog", "Witherbloom Apprentice", "Chain of Smog"));
        e.add(new Entry(25, Type.WIN, "Bloodletter of Aclazotz + Unstoppable Slasher", "Bloodletter of Aclazotz",
                "Unstoppable Slasher"));
        e.add(new Entry(26, Type.WIN, "Devoted Druid + Vizier of Remedies", "Devoted Druid", "Vizier of Remedies"));
        e.add(new Entry(27, Type.WIN, "Basalt Monolith + Rings of Brighthearth", "Basalt Monolith", "Rings of Brighthearth"));
        e.add(new Entry(28, Type.WIN, "Grim Monolith + Power Artifact", "Grim Monolith", "Power Artifact"));
        e.add(new Entry(29, Type.WIN, "Metalworker + Staff of Domination", "Metalworker", "Staff of Domination"));
        e.add(new Entry(30, Type.WIN, "Freed from the Real + Bloom Tender", "Freed from the Real", "Bloom Tender"));
        e.add(new Entry(31, Type.WIN, "Pili-Pala + Grand Architect", "Pili-Pala", "Grand Architect"));
        e.add(new Entry(32, Type.WIN, "Auriok Salvagers + Lion's Eye Diamond", "Auriok Salvagers", "Lion's Eye Diamond"));
        e.add(new Entry(33, Type.WIN, "Krark-Clan Ironworks + Scrap Trawler + Myr Retriever", "Krark-Clan Ironworks",
                "Scrap Trawler", "Myr Retriever"));
        e.add(new Entry(34, Type.WIN, "Worldgorger Dragon + Animate Dead", "Worldgorger Dragon",
                "Animate Dead / Necromancy / Dance of the Dead"));
        e.add(new Entry(35, Type.WIN, "Sliver Queen + Mana Echoes", "Sliver Queen", "Mana Echoes"));
        e.add(new Entry(35, Type.WIN, "Urza, Lord High Artificer + Thopter Foundry + Sword of the Meek",
                "Urza, Lord High Artificer", "Thopter Foundry", "Sword of the Meek"));
        e.add(new Entry(36, Type.WIN, "Hive Mind + Pact of Negation", "Hive Mind", "Pact of Negation"));
        e.add(new Entry(37, Type.WIN, "Peer into the Abyss + Underworld Dreams", "Peer into the Abyss", "Underworld Dreams"));
        e.add(new Entry(38, Type.WIN, "Scapeshift + Valakut", "Scapeshift", "Valakut, the Molten Pinnacle"));
        e.add(new Entry(39, Type.WIN, "Dark Depths + Thespian's Stage / Vampire Hexmage", "Dark Depths",
                "Thespian's Stage / Vampire Hexmage"));
        e.add(new Entry(40, Type.WIN, "Illusions of Grandeur + Donate", "Illusions of Grandeur", "Donate"));
        e.add(new Entry(41, Type.WIN, "Leyline of the Void + Helm of Obedience", "Leyline of the Void", "Helm of Obedience"));
        e.add(new Entry(42, Type.WIN, "Near-Death Experience + Worship", "Near-Death Experience", "Worship"));
        e.add(new Entry(42, Type.WIN, "Aetherworks Marvel + Woodweaver's Puzzleknot", "Aetherworks Marvel",
                "Woodweaver's Puzzleknot"));
        e.add(new Entry(43, Type.WIN, "Second Sunrise + Conjurer's Bauble", "Second Sunrise", "Conjurer's Bauble"));
        // ---- combo locks
        e.add(new Entry(44, Type.LOCK, "Counterbalance + Sensei's Divining Top", "Counterbalance", "Sensei's Divining Top"));
        e.add(new Entry(45, Type.LOCK, "Mindslaver + Academy Ruins", "Mindslaver", "Academy Ruins"));
        e.add(new Entry(46, Type.LOCK, "Isochron Scepter + Orim's Chant", "Isochron Scepter", "Orim's Chant"));
        e.add(new Entry(47, Type.LOCK, "Teferi, Time Raveler + Knowledge Pool", "Teferi, Time Raveler", "Knowledge Pool"));
        e.add(new Entry(48, Type.LOCK, "Maralen of the Mornsong + Opposition Agent", "Maralen of the Mornsong",
                "Opposition Agent"));
        e.add(new Entry(49, Type.LOCK, "Karn, the Great Creator + Mycosynth Lattice", "Karn, the Great Creator",
                "Mycosynth Lattice"));
        e.add(new Entry(50, Type.LOCK, "Drana and Linvala + Urborg + Kormus Bell", "Drana and Linvala",
                "Urborg, Tomb of Yawgmoth / Kormus Bell"));
        e.add(new Entry(51, Type.LOCK, "Solemnity + Decree of Silence", "Solemnity", "Decree of Silence"));
        e.add(new Entry(52, Type.LOCK, "Narset, Parter of Veils + Geier Reach Sanitarium", "Narset, Parter of Veils",
                "Geier Reach Sanitarium"));
        e.add(new Entry(53, Type.LOCK, "Unburial Rites + Iona, Shield of Emeria", "Unburial Rites", "Iona, Shield of Emeria"));
        ENTRIES = Collections.unmodifiableList(e);
    }

    /** What an assembled combo in the hand or on the battlefield is worth to the AI, in evaluator points. */
    public static final int ASSEMBLED_WIN = 160;
    static final int ASSEMBLED_LOCK = 100;
    /** All slots but one filled: the missing card is what the AI is looking for. */
    static final int ONE_MISSING = 30;
    /** A slot filled by a permanent is worth more than one filled by a card in hand: it is already working. */
    static final int ON_BATTLEFIELD = 10;
    /** What an assembled combo of an opponent in view is worth to the opponent. */
    static final int VISIBLE_WIN = 140;
    public static final int VISIBLE_LOCK = 90;
    /** A card that fills a slot of a combo is aimed at with this share of the rank of a key card. */
    static final int RANK_PIECE_PERCENT = 40;
    /** The same when the other slots of the combo are already in view: that is the card that completes it. */
    static final int RANK_COMPLETING_PERCENT = 85;

    private ComboCatalog() {
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    /** Whether the card fills a slot of any combo. */
    public static boolean isPiece(final Card c) {
        if (c == null) {
            return false;
        }
        for (final Entry e : ENTRIES) {
            if (e.fillsSomeSlot(c)) {
                return true;
            }
        }
        return false;
    }

    /** The combos a list of cards can make: every slot is filled by one of the cards. */
    public static List<Entry> assembledBy(final Iterable<Card> cards) {
        final List<Entry> result = new ArrayList<>();
        for (final Entry e : ENTRIES) {
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
        for (final Entry e : ENTRIES) {
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
        for (final Entry e : ENTRIES) {
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
            cards.addAll(opp.getCardsIn(ZoneType.Battlefield));
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
        for (final Entry e : ENTRIES) {
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
