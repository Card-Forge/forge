package forge.card;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.CollectionSuppliers;
import forge.util.MyRandom;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Test Class (only for test purposes) to compare previous method implementations
 * with the new refactored ones. This class has been updated to the latest version
 * available of methods' implementation before the change is going to be submitted.
 * This is also a useful way to keep a backlog of changes whenever major
 * API changes like this MR are going to happen.
 */
public class LegacyCardDb {
    public CardEdition.Collection editions;
    public ListMultimap<String, PaperCard> allCardsByName = Multimaps.newListMultimap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER), CollectionSuppliers.arrayLists());

    public enum LegacySetPreference {
        Latest(false),
        LatestCoreExp(true),
        Earliest(false),
        EarliestCoreExp(true),
        Random(false);

        final boolean filterSets;
        LegacySetPreference(boolean filterIrregularSets) {
            filterSets = filterIrregularSets;
        }

        public boolean accept(CardEdition ed) {
            if (ed == null)  return false;
            return !filterSets || ed.getType() == CardEdition.Type.CORE || ed.getType() == CardEdition.Type.EXPANSION || ed.getType() == CardEdition.Type.REPRINT;
        }
    }


    public final static String foilSuffix = "+";
    public final static char NameSetSeparator = '|';

    public static class LegacyCardRequest {
        // TODO Move Request to its own class
        public String cardName;
        public String edition;
        public int artIndex;
        public boolean isFoil;
        
        private LegacyCardRequest(String name, String edition, int artIndex, boolean isFoil) {
            cardName = name;
            this.edition = edition;
            this.artIndex = artIndex;
            this.isFoil = isFoil;
        }

        public static LegacyCardRequest fromString(String name) {
            boolean isFoil = name.endsWith(foilSuffix);
            if (isFoil) {
                name = name.substring(0, name.length() - foilSuffix.length());
            }

            String[] nameParts = TextUtil.split(name, NameSetSeparator);

            int setPos = nameParts.length >= 2 && !StringUtils.isNumeric(nameParts[1]) ? 1 : -1;
            int artPos = nameParts.length >= 2 && StringUtils.isNumeric(nameParts[1]) ? 1 : nameParts.length >= 3 && StringUtils.isNumeric(nameParts[2]) ? 2 : -1;

            String cardName = nameParts[0];
            if (cardName.endsWith(foilSuffix)) {
                cardName = cardName.substring(0, cardName.length() - foilSuffix.length());
                isFoil = true;
            }

            int artIndex = artPos > 0 ? Integer.parseInt(nameParts[artPos]) : 0;
            String setName = setPos > 0 ? nameParts[setPos] : null;
            if ("???".equals(setName)) {
                setName = null;
            }

            return new LegacyCardDb.LegacyCardRequest(cardName, setName, artIndex, isFoil);
        }
    }

    public LegacyCardDb(Collection<PaperCard> cards0, CardEdition.Collection editions){
        this.editions = editions;
        for (PaperCard card : cards0){
            allCardsByName.put(card.getName(), card);
        }
    }

    public String getName(final String cardName) {
        return cardName;
    }

    private ListMultimap<String, PaperCard> getAllCardsByName() {
        return allCardsByName;
    }

    public List<PaperCard> getAllCards(String cardName) {
        return getAllCardsByName().get(getName(cardName));
    }

    public PaperCard getCard(String cardName) {
        LegacyCardDb.LegacyCardRequest request = LegacyCardDb.LegacyCardRequest.fromString(cardName);
        return tryGetCard(request);
    }

    public PaperCard getCard(final String cardName, String setCode) {
        LegacyCardDb.LegacyCardRequest request = LegacyCardDb.LegacyCardRequest.fromString(cardName);
        if (setCode != null) {
            request.edition = setCode;
        }
        return tryGetCard(request);
    }

    public PaperCard getCard(final String cardName, String setCode, int artIndex) {
        LegacyCardDb.LegacyCardRequest request = LegacyCardDb.LegacyCardRequest.fromString(cardName);
        if (setCode != null) {
            request.edition = setCode;
        }
        if (artIndex > 0) {
            request.artIndex = artIndex;
        }
        return tryGetCard(request);
    }

    private PaperCard tryGetCard(LegacyCardDb.LegacyCardRequest request) {
        Collection<PaperCard> cards = getAllCards(request.cardName);
        if (cards == null) { return null; }

        PaperCard result = null;

        String reqEdition = request.edition;
        if (reqEdition != null && !editions.contains(reqEdition)) {
            CardEdition edition = editions.get(reqEdition);
            if (edition != null) {
                reqEdition = edition.getCode();
            }
        }

        if (request.artIndex <= 0) { // this stands for 'random art'
            Collection<PaperCard> candidates;
            if (reqEdition == null) {
                candidates = new ArrayList<>(cards);
            }
            else {
                candidates = new ArrayList<>();
                for (PaperCard pc : cards) {
                    if (pc.getEdition().equalsIgnoreCase(reqEdition)) {
                        candidates.add(pc);
                    }
                }
            }
            if (candidates.isEmpty()) {
                return null;
            }
            result = Aggregates.random(candidates);

            //if card image doesn't exist for chosen candidate, try another one if possible
            while (candidates.size() > 1 && !result.hasImage()) {
                candidates.remove(result);
                result = Aggregates.random(candidates);
            }
        }
        else {
            for (PaperCard pc : cards) {
                if (pc.getEdition().equalsIgnoreCase(reqEdition) && request.artIndex == pc.getArtIndex()) {
                    result = pc;
                    break;
                }
            }
        }
        if (result == null) { return null; }

        return request.isFoil ? getFoiled(result) : result;
    }

    public PaperCard getFoiled(PaperCard card0) {
        return card0.getFoiled();
    }
    
    public PaperCard getCardFromEdition(final String cardName, LegacySetPreference fromSet) {
        return getCardFromEdition(cardName, null, fromSet);
    }
    
    public PaperCard getCardFromEdition(final String cardName, final Date printedBefore, final LegacySetPreference fromSet) {
        return getCardFromEdition(cardName, printedBefore, fromSet, -1);
    }
    
    public PaperCard getCardFromEdition(final String cardName, final Date printedBefore, final LegacySetPreference fromSets, int artIndex) {
        final CardDb.CardRequest cr = CardDb.CardRequest.fromString(cardName);
        LegacySetPreference fromSet = fromSets;
        List<PaperCard> cards = getAllCards(cr.cardName);
        if (printedBefore != null){
            cards = Lists.newArrayList(Iterables.filter(cards, new Predicate<PaperCard>() {
                @Override public boolean apply(PaperCard c) {
                    CardEdition ed = editions.get(c.getEdition());
                    return ed.getDate().before(printedBefore); }
            }));
        }

        if (cards.size() == 0)  // Don't bother continuing! No cards has been found!
            return null;
        boolean cardsListReadOnly = true;

//        Removed from testing - completely Non-sense
//        //overrides
//        if (StaticData.instance().getPrefferedArtOption().equals("Earliest"))
//            fromSet = LegacySetPreference.EarliestCoreExp;

        if (StringUtils.isNotBlank(cr.edition)) {
            cards = Lists.newArrayList(Iterables.filter(cards, new Predicate<PaperCard>() {
                @Override public boolean apply(PaperCard input) { return input.getEdition().equalsIgnoreCase(cr.edition); }
            }));
        }
        if (artIndex == -1 && cr.artIndex > 0) {
            artIndex = cr.artIndex;
        }

        int sz = cards.size();
        if (fromSet == LegacySetPreference.Earliest || fromSet == LegacySetPreference.EarliestCoreExp) {
            PaperCard firstWithoutImage = null;
            for (int i = sz - 1 ; i >= 0 ; i--) {
                PaperCard pc = cards.get(i);
                CardEdition ed = editions.get(pc.getEdition());
                if (!fromSet.accept(ed)) {
                    continue;
                }

                if ((artIndex <= 0 || pc.getArtIndex() == artIndex) && (printedBefore == null || ed.getDate().before(printedBefore))) {
                    if (pc.hasImage()) {
                        return pc;
                    }
                    else if (firstWithoutImage == null) {
                        firstWithoutImage = pc; //ensure first without image returns if none have image
                    }
                }
            }
            return firstWithoutImage;
        }
        else if (fromSet == LegacySetPreference.LatestCoreExp || fromSet == LegacySetPreference.Latest || fromSet == null || fromSet == LegacySetPreference.Random) {
            PaperCard firstWithoutImage = null;
            for (int i = 0; i < sz; i++) {
                PaperCard pc = cards.get(i);
                CardEdition ed = editions.get(pc.getEdition());
                if (fromSet != null && !fromSet.accept(ed)) {
                    continue;
                }

                if ((artIndex < 0 || pc.getArtIndex() == artIndex) && (printedBefore == null || ed.getDate().before(printedBefore))) {
                    if (fromSet == LegacySetPreference.LatestCoreExp || fromSet == LegacySetPreference.Latest) {
                        if (pc.hasImage()) {
                            return pc;
                        }
                        else if (firstWithoutImage == null) {
                            firstWithoutImage = pc; //ensure first without image returns if none have image
                        }
                    }
                    else {
                        while (sz > i) {
                            int randomIndex = i + MyRandom.getRandom().nextInt(sz - i);
                            pc = cards.get(randomIndex);
                            if (pc.hasImage()) {
                                return pc;
                            }
                            else {
                                if (firstWithoutImage == null) {
                                    firstWithoutImage = pc; //ensure first without image returns if none have image
                                }
                                if (cardsListReadOnly) { //ensure we don't modify a cached collection
                                    cards = new ArrayList<>(cards);
                                    cardsListReadOnly = false;
                                }
                                cards.remove(randomIndex); //remove card from collection and try another random card
                                sz--;
                            }
                        }
                    }
                }
            }
            return firstWithoutImage;
        }
        return null;
    }

    public int getPrintCount(String cardName, String edition) {
        int cnt = 0;
        if (edition == null || cardName == null)
            return cnt;
        for (PaperCard pc : getAllCards(cardName)) {
            if (pc.getEdition().equals(edition)) {
                cnt++;
            }
        }
        return cnt;
    }

    public int getMaxPrintCount(String cardName) {
        int max = -1;
        if (cardName == null)
            return max;
        for (PaperCard pc : getAllCards(cardName)) {
            if (max < pc.getArtIndex()) {
                max = pc.getArtIndex();
            }
        }
        return max;
    }

    public int getArtCount(String cardName, String setName) {
        int cnt = 0;
        if (cardName == null || setName == null)
            return cnt;

        Collection<PaperCard> cards = getAllCards(cardName);
        if (null == cards) {
            return 0;
        }

        for (PaperCard pc : cards) {
            if (pc.getEdition().equalsIgnoreCase(setName)) {
                cnt++;
            }
        }

        return cnt;
    }
}