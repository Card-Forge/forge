/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.card;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;

import forge.card.CardEdition.CardInSet;
import forge.card.CardEdition.Type;
import forge.deck.generation.IDeckGenPool;
import forge.item.PaperCard;
import forge.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.Map.Entry;

public final class CardDb implements ICardDatabase, IDeckGenPool {
    public final static String foilSuffix = "+";
    public final static char NameSetSeparator = '|';
    
    // need this to obtain cardReference by name+set+artindex
    private final ListMultimap<String, PaperCard> allCardsByName = Multimaps.newListMultimap(new TreeMap<String,Collection<PaperCard>>(String.CASE_INSENSITIVE_ORDER),  CollectionSuppliers.<PaperCard>arrayLists());
    private final Map<String, PaperCard> uniqueCardsByName = new TreeMap<String, PaperCard>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, CardRules> rulesByName;
    private static Map<String, String> artPrefs = new HashMap<String, String>();

    private final List<PaperCard> allCards = new ArrayList<PaperCard>();
    private final List<PaperCard> roAllCards = Collections.unmodifiableList(allCards);
    private final CardEdition.Collection editions;

    public enum SetPreference {
        Latest(false),
        LatestCoreExp(true),
        Earliest(false),
        EarliestCoreExp(true),
        Random(false);
        
        final boolean filterSets;
        private SetPreference(boolean filterIrregularSets) {
            filterSets = filterIrregularSets;
        }
        
        public boolean accept(CardEdition ed) {
            if (ed == null)  return false;
            return !filterSets || ed.getType() == Type.CORE || ed.getType() == Type.EXPANSION || ed.getType() == Type.REPRINT; 
        }
    }
    
    // NO GETTERS/SETTERS HERE!
    private static class CardRequest {
        public String cardName;
        public String edition;
        public int artIndex;
        public boolean isFoil;
        
        private CardRequest(String name, String edition, int artIndex, boolean isFoil) {
            cardName = name;
            this.edition = edition;
            this.artIndex = artIndex;
            this.isFoil = isFoil;
        }
        
        public static CardRequest fromString(String name) {
            boolean isFoil = name.endsWith(foilSuffix);
            if (isFoil) {
                name = name.substring(0, name.length() - foilSuffix.length());
            }

            String preferredArt = artPrefs.get(name);
            if (preferredArt != null) { //account for preferred art if needed
                name += NameSetSeparator + preferredArt;
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

            return new CardRequest(cardName, setName, artIndex, isFoil);
        }
    }

    public CardDb(Map<String, CardRules> rules, CardEdition.Collection editions0) {
        this.rulesByName = rules;
        this.editions = editions0;
    }

    public void initialize(boolean logMissingPerEdition, boolean logMissingSummary) {
        Set<String> allMissingCards = new LinkedHashSet<String>();
        List<String> missingCards = new ArrayList<String>();
        Map<String, Integer> artIds = new HashMap<String, Integer>();

        for (CardEdition e : editions.getOrderedEditions()) {
            boolean isCoreExpSet = e.getType() == CardEdition.Type.CORE || e.getType() == CardEdition.Type.EXPANSION || e.getType() == CardEdition.Type.REPRINT; 
            if (logMissingPerEdition && isCoreExpSet) {
                System.out.print(e.getName() + " (" + e.getCards().length + " cards)");
            }
            for (CardEdition.CardInSet cis : e.getCards()) {
                int artIdx = 1;
                if (artIds.containsKey(cis.name)) {
                    artIdx = artIds.get(cis.name) + 1;
                }
                artIds.put(cis.name, artIdx);
                CardRules cr = rulesByName.get(cis.name);
                if (cr != null) {
                    addCard(new PaperCard(cr, e.getCode(), cis.rarity, artIdx));
                }
                else {
                    missingCards.add(cis.name);
                }
            }
            if (isCoreExpSet && logMissingPerEdition) {
                if (missingCards.isEmpty()) {
                    System.out.println(" ... 100% ");
                }
                else {
                    int missing = (e.getCards().length - missingCards.size()) * 10000 / e.getCards().length;
                    System.out.printf(" ... %.2f%% (%s missing: %s)%n", missing * 0.01f, Lang.nounWithAmount(missingCards.size(), "card"), StringUtils.join(missingCards, " | "));
                }
            }
            if (isCoreExpSet && logMissingSummary) {
                allMissingCards.addAll(missingCards);
            }
            missingCards.clear();
            artIds.clear();
        }
        
        if (logMissingSummary) {
            System.out.printf("Totally %d cards not implemented: %s\n", allMissingCards.size(), StringUtils.join(allMissingCards, " | "));
        }

        for (CardRules cr : rulesByName.values()) {
            if (!allCardsByName.containsKey(cr.getName())) {
                System.err.println("The card " + cr.getName() + " was not assigned to any set. Adding it to UNKNOWN set... to fix see res/cardeditions/ folder. ");
                addCard(new PaperCard(cr, CardEdition.UNKNOWN.getCode(), CardRarity.Special, 1));
            }
        }

        reIndex();
    }

    private void addCard(PaperCard paperCard) {
        allCardsByName.put(paperCard.getName(), paperCard);

        if (paperCard.getRules().getSplitType() == CardSplitType.None) { return; }

        //allow looking up card by the name of other faces
        allCardsByName.put(paperCard.getRules().getOtherPart().getName(), paperCard);
        if (paperCard.getRules().getSplitType() == CardSplitType.Split) {
            //also include main part for split cards
            allCardsByName.put(paperCard.getRules().getMainPart().getName(), paperCard);
        }
    }

    private void reIndex() {
        uniqueCardsByName.clear();
        allCards.clear();
        for (Entry<String, Collection<PaperCard>> kv : allCardsByName.asMap().entrySet()) {
            uniqueCardsByName.put(kv.getKey(), getFirstWithImage(kv.getValue()));
            allCards.addAll(kv.getValue());
        }
    }

    private static PaperCard getFirstWithImage(final Collection<PaperCard> cards) {
        //NOTE: this is written this way to avoid checking final card in list
        final Iterator<PaperCard> iterator = cards.iterator();
        PaperCard pc = iterator.next();
        while (iterator.hasNext()) {
            if (pc.hasImage()) {
                return pc;
            }
            pc = iterator.next();
        }
        return pc;
    }

    public boolean setPreferredArt(String cardName, String preferredArt) {
        CardRequest request = CardRequest.fromString(cardName + NameSetSeparator + preferredArt);
        PaperCard pc = tryGetCard(request);
        if (pc != null) {
            artPrefs.put(cardName, preferredArt);
            uniqueCardsByName.put(cardName, pc);
            return true;
        }
        return false;
    }

    @Override
    public PaperCard getCard(String cardName) {
        CardRequest request = CardRequest.fromString(cardName);
        return tryGetCard(request);
    }

    @Override
    public PaperCard getCard(final String cardName, String setCode) {
        CardRequest request = CardRequest.fromString(cardName);
        if (setCode != null) {
            request.edition = setCode;
        }
        return tryGetCard(request);
    }

    @Override
    public PaperCard getCard(final String cardName, String setCode, int artIndex) {
        CardRequest request = CardRequest.fromString(cardName);
        if (setCode != null) {
            request.edition = setCode;
        }
        if (artIndex > 0) {
            request.artIndex = artIndex;
        }
        return tryGetCard(request);
    }
    
    public int getCardCollectorNumber(String cardName, String reqEdition) {
        CardEdition edition = editions.get(reqEdition);
        if (edition == null)
            return -1;
        for (CardInSet card : edition.getCards()) {
            if (card.name.equalsIgnoreCase(cardName)) {
                return card.collectorNumber;
            }
        }
        return -1;
    }

    private PaperCard tryGetCard(CardRequest request) {
        Collection<PaperCard> cards = allCardsByName.get(request.cardName);
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
                candidates = new ArrayList<PaperCard>(cards);
            }
            else {
                candidates = new ArrayList<PaperCard>();
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

    @Override
    public PaperCard getCardFromEdition(final String cardName, SetPreference fromSet) {
        return getCardFromEdition(cardName, null, fromSet);
    }

    @Override
    public PaperCard getCardFromEdition(final String cardName, final Date printedBefore, final SetPreference fromSet) {
        return getCardFromEdition(cardName, printedBefore, fromSet, -1);
    }

    @Override
    public PaperCard getCardFromEdition(final String cardName, final Date printedBefore, final SetPreference fromSet, int artIndex) {
        final CardRequest cr = CardRequest.fromString(cardName);
        List<PaperCard> cards = this.allCardsByName.get(cr.cardName);
        boolean cardsListReadOnly = true;

        if (StringUtils.isNotBlank(cr.edition)) {
            cards = Lists.newArrayList(Iterables.filter(cards, new Predicate<PaperCard>() {
                @Override public boolean apply(PaperCard input) { return input.getEdition().equalsIgnoreCase(cr.edition); }
            }));
        }
        if (artIndex == -1 && cr.artIndex > 0) {
            artIndex = cr.artIndex;
        }

        int sz = cards.size();
        if (fromSet == SetPreference.Earliest || fromSet == SetPreference.EarliestCoreExp) {
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
        else if (fromSet == SetPreference.LatestCoreExp || fromSet == SetPreference.Latest || fromSet == null || fromSet == SetPreference.Random) {
            PaperCard firstWithoutImage = null;
            for (int i = 0; i < sz; i++) {
                PaperCard pc = cards.get(i);
                CardEdition ed = editions.get(pc.getEdition());
                if (fromSet != null && !fromSet.accept(ed)) {
                    continue;
                }

                if ((artIndex < 0 || pc.getArtIndex() == artIndex) && (printedBefore == null || ed.getDate().before(printedBefore))) {
                    if (fromSet == SetPreference.LatestCoreExp || fromSet == SetPreference.Latest) {
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
                                    cards = new ArrayList<PaperCard>(cards);
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

    public PaperCard getFoiled(PaperCard card0) {
        // Here - I am still unsure if there should be a cache Card->Card from unfoiled to foiled, to avoid creation of N instances of single plains
        return new PaperCard(card0.getRules(), card0.getEdition(), card0.getRarity(), card0.getArtIndex(), true);
    }

    @Override
    public int getPrintCount(String cardName, String edition) {
        int cnt = 0;
        for (PaperCard pc : allCardsByName.get(cardName)) {
            if (pc.getEdition().equals(edition)) {
                cnt++;
            }
        }
        return cnt;
    }

    @Override
    public int getMaxPrintCount(String cardName) {
        int max = -1;
        for (PaperCard pc : allCardsByName.get(cardName)) {
            if (max < pc.getArtIndex()) {
                max = pc.getArtIndex();
            }
        }
        return max;
    }

    @Override
    public int getArtCount(String cardName, String setName) {
        int cnt = 0;

        Collection<PaperCard> cards = allCardsByName.get(cardName);
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

    // returns a list of all cards from their respective latest (or preferred) editions
    @Override
    public Collection<PaperCard> getUniqueCards() {
        return uniqueCardsByName.values();
    }

    @Override
    public List<PaperCard> getAllCards() {
        return roAllCards;
    }

    @Override
    public List<PaperCard> getAllCards(String cardName) {
        return allCardsByName.get(cardName);
    }

    /**  Returns a modifiable list of cards matching the given predicate */
    @Override
    public List<PaperCard> getAllCards(Predicate<PaperCard> predicate) {
        return Lists.newArrayList(Iterables.filter(this.roAllCards, predicate));
    }

    @Override
    public boolean contains(String name) {
        return allCardsByName.containsKey(name);
    }

    @Override
    public Iterator<PaperCard> iterator() {
        return this.roAllCards.iterator();
    }

    public Predicate<? super PaperCard> wasPrintedInSets(List<String> setCodes) {
        return new PredicateExistsInSets(setCodes);
    }

    private class PredicateExistsInSets implements Predicate<PaperCard> {
        private final List<String> sets;

        public PredicateExistsInSets(final List<String> wantSets) {
            this.sets = wantSets; // maybe should make a copy here?
        }

        @Override
        public boolean apply(final PaperCard subject) {
            Collection<PaperCard> cc = allCardsByName.get(subject.getName());
            for (PaperCard c : cc) {
                if (sets.contains(c.getEdition())) {
                    return true;
                }
            }
            return false;
        }
    }
    
    public StringBuilder appendCardToStringBuilder(PaperCard card, StringBuilder sb) {
        final boolean hasBadSetInfo = "???".equals(card.getEdition()) || StringUtils.isBlank(card.getEdition());
        sb.append(card.getName());
        if (card.isFoil()) {
            sb.append(CardDb.foilSuffix);
        }

        if (!hasBadSetInfo) {
            int artCount = getArtCount(card.getName(), card.getEdition());
            sb.append(CardDb.NameSetSeparator).append(card.getEdition());
            if (artCount > 1) {
                sb.append(CardDb.NameSetSeparator).append(card.getArtIndex()); // indexes start at 1 to match image file name conventions
            }
        }

        return sb;
    }

    public PaperCard createUnsuportedCard(String cardName) {
        CardRequest request = CardRequest.fromString(cardName);
        CardEdition cE = CardEdition.UNKNOWN;
        CardRarity cR = CardRarity.Unknown;

        // May iterate over editions and find out if there is any card named 'cardName' but not implemented with Forge script.
        if (StringUtils.isBlank(request.edition)) {
            for (CardEdition e : editions) {
                for (CardInSet cs : e.getCards()) {
                    if (cs.name.equals(request.cardName)) {
                        cE = e;
                        cR = cs.rarity;
                        break;
                    }
                }
                if (cE != CardEdition.UNKNOWN) {
                    break;
                }
            }
        }
        else {
            cE = editions.get(request.edition);
            if (cE != null) {
                for (CardInSet cs : cE.getCards()) {
                    if (cs.name.equals(request.cardName)) {
                        cR = cs.rarity;
                        break;
                    }
                }
            }
            else {
                cE = CardEdition.UNKNOWN;
            }
        }
            

        // Write to log that attempt,
        if (cR == CardRarity.Unknown) {
            System.err.println(String.format("An unknown card found when loading Forge decks: \"%s\" Forge does not know of such a card's existence. Have you mistyped the card name?", cardName));
        }
        else {
            System.err.println(String.format("An unsupported card was requested: \"%s\" from \"%s\" set. We're sorry, but you cannot use this card yet.", request.cardName, cE.getName()));
        }
        
        return new PaperCard(CardRules.getUnsupportedCardNamed(request.cardName), cE.getCode(), cR, 1);
    }

    private final Editor editor = new Editor();
    public Editor getEditor() { return editor; }
    public class Editor {
        private boolean immediateReindex = true;
        public CardRules putCard(CardRules rules) { return putCard(rules, null); /* will use data from editions folder */ }
        public CardRules putCard(CardRules rules, List<Pair<String, CardRarity>> whenItWasPrinted){ // works similarly to Map<K,V>, returning prev. value
            String cardName = rules.getName();

            CardRules result = rulesByName.get(cardName);
            if (result != null && result.getName().equals(cardName)){ // change properties only
                result.reinitializeFromRules(rules);
                return result;
            }

            result = rulesByName.put(cardName, rules);
            
            // 1. generate all paper cards from edition data we have (either explicit, or found in res/editions, or add to unknown edition)
            List<PaperCard> paperCards = new ArrayList<PaperCard>();
            if (null == whenItWasPrinted || whenItWasPrinted.isEmpty()) {
                for (CardEdition e : editions.getOrderedEditions()) {
                    int artIdx = 1;
                    for (CardInSet cis : e.getCards()) {
                        if (!cis.name.equals(cardName)) {
                            continue;
                        }
                        paperCards.add(new PaperCard(rules, e.getCode(), cis.rarity, artIdx++));
                    }
                }
            }
            else {
                String lastEdition = null;
                int artIdx = 0;
                for (Pair<String, CardRarity> tuple : whenItWasPrinted){
                    if (!tuple.getKey().equals(lastEdition)) {
                        artIdx = 1;
                        lastEdition = tuple.getKey();
                    }
                    CardEdition ed = editions.get(lastEdition);
                    if (null == ed) {
                        continue;
                    }
                    paperCards.add(new PaperCard(rules, lastEdition, tuple.getValue(), artIdx++));
                }
            }
            if (paperCards.isEmpty()) {
                paperCards.add(new PaperCard(rules, CardEdition.UNKNOWN.getCode(), CardRarity.Special, 1));
            }
            // 2. add them to db
            for (PaperCard paperCard : paperCards) {
                addCard(paperCard);
            }
            // 3. reindex can be temporary disabled and run after the whole batch of rules is added to db.
            if (immediateReindex) {
                reIndex();
            }
            return result;
        }

        public boolean isImmediateReindex() {
            return immediateReindex;
        }
        public void setImmediateReindex(boolean immediateReindex) {
            this.immediateReindex = immediateReindex;
        }
    }
}
