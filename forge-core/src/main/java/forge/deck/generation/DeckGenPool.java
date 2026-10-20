package forge.deck.generation;

import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.util.IterableUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class DeckGenPool implements IDeckGenPool {
    private final Map<String, PaperCard> cards = new HashMap<>();

    public DeckGenPool() {
    }
    public DeckGenPool(Iterable<PaperCard> cc) {
        addAll(cc);
    }

    /**
     * Keeps ONE print per card name: the one the Card Art Preference ranks first (instead of whichever print
     * happens to be iterated last, which used to give promos and special frames in generated decks).
     */
    public void add(PaperCard c) {
        PaperCard current = cards.get(c.getName());
        if (current == null || isPreferred(c, current))
            cards.put(c.getName(), c);
    }

    /** Only used for its edition ranking (Core/Expansion first, promos and compilations last), which does not depend on the border. */
    private static final CardDb.CardArtPreference RANKER = CardDb.CardArtPreference.ORIGINAL_ART_BLACK_BORDER_ALL_EDITIONS;

    private static boolean isPreferred(PaperCard candidate, PaperCard current) {
        StaticData data = StaticData.instance();
        if (data == null || candidate.getEdition().equals(current.getEdition()) && candidate.getCollectorNumber().equals(current.getCollectorNumber()))
            return false;
        CardEdition candidateEdition = data.getCardEdition(candidate.getEdition());
        CardEdition currentEdition = data.getCardEdition(current.getEdition());
        if (candidateEdition == null || currentEdition == null)
            return candidateEdition != null;
        int cmp = data.getCardArtPreference().compare(candidateEdition, currentEdition);
        if (cmp != 0)
            return cmp < 0;
        // Same release date (e.g. a set and its promo set): original releases before promos and compilations.
        int rankCmp = Integer.compare(RANKER.getEditionRank(candidateEdition), RANKER.getEditionRank(currentEdition));
        if (rankCmp != 0)
            return rankCmp < 0;
        CardDb db = data.getCommonCards();
        boolean candidateTraditional = db.isTraditionalFramePrint(candidate);
        boolean currentTraditional = db.isTraditionalFramePrint(current);
        if (candidateTraditional != currentTraditional)
            return candidateTraditional;
        int numberCmp = candidate.getCollectorNumber().compareToIgnoreCase(current.getCollectorNumber());
        if (numberCmp != 0)
            return numberCmp < 0;
        return candidate.getEdition().compareToIgnoreCase(current.getEdition()) < 0; // deterministic last resort
    }
    public void addAll(Iterable<PaperCard> cc) {
        for (PaperCard c : cc) {
            add(c);
        }
    }

    public int size() {
        return cards.size();
    }

    @Override
    public PaperCard getCard(String name) {
        return cards.get(name);
    }

    @Override
    public PaperCard getCard(String name, String edition) {
        Predicate<PaperCard> filter = PaperCardPredicates.printedInSet(edition).and(PaperCardPredicates.name(name));
        return cards.values().stream()
                .filter(filter)
                .findFirst().orElseGet(() -> getCard(name));
    }

    @Override
    public PaperCard getCard(String name, String edition, int artIndex) {
        return getCard(name, edition);
    }

    public boolean contains(PaperCard card) {
        return contains(card.getName());
    }

    @Override
    public boolean contains(String name) {
        return cards.containsKey(name);
    }

    @Override
    public Iterable<PaperCard> getAllCards() {
        return cards.values();
    }

    @Override
    public Iterable<PaperCard> getAllCards(Predicate<PaperCard> filter) {
        return IterableUtil.filter(getAllCards(), filter);
    }
}
