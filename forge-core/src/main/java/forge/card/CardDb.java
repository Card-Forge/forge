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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import forge.card.CardEdition.CardInSet;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.CollectionSuppliers;
import forge.util.Lang;
import forge.util.MyRandom;
import forge.util.TextUtil;
import java.util.Arrays;

public final class CardDb implements ICardDatabase {
    public final static String foilSuffix = "+";
    private final static int foilSuffixLength = foilSuffix.length();

    // need this to obtain cardReference by name+set+artindex
    private final Multimap<String, PaperCard> allCardsByName = Multimaps.newListMultimap(new TreeMap<String,Collection<PaperCard>>(String.CASE_INSENSITIVE_ORDER),  CollectionSuppliers.<PaperCard>arrayLists());
    private final Map<String, PaperCard> uniqueCardsByName = new TreeMap<String, PaperCard>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, CardRules> rulesByName;

    private final List<PaperCard> allCards = new ArrayList<PaperCard>();
    private final List<PaperCard> roAllCards = Collections.unmodifiableList(allCards);
    private final Collection<PaperCard> roUniqueCards = Collections.unmodifiableCollection(uniqueCardsByName.values());
    private final CardEdition.Collection editions;

    public CardDb(Map<String, CardRules> rules, CardEdition.Collection editions0, boolean logMissingCards) {
        this.rulesByName = rules;
        this.editions = editions0;
        List<String> missingCards = new ArrayList<String>();
        for(CardEdition e : editions.getOrderedEditions()) {
            boolean worthLogging = logMissingCards && ( e.getType() == CardEdition.Type.CORE || e.getType() == CardEdition.Type.EXPANSION || e.getType() == CardEdition.Type.REPRINT );
            if(worthLogging)
                System.out.print(e.getName() + " (" + e.getCards().length + " cards)");
            String lastCardName = null;
            int artIdx = 0;
            for(CardEdition.CardInSet cis : e.getCards()) {
                if ( cis.name.equals(lastCardName) )
                    artIdx++;
                else {
                    artIdx = 0;
                    lastCardName = cis.name;
                }
                CardRules cr = rulesByName.get(lastCardName);
                if( cr != null )
                    addCard(new PaperCard(cr, e.getCode(), cis.rarity, artIdx));
                else if (worthLogging)
                    missingCards.add(cis.name);
            }
            if(worthLogging) {
                if(missingCards.isEmpty())
                    System.out.println(" ... 100% ");
                else {
                    int missing = (e.getCards().length - missingCards.size()) * 10000 / e.getCards().length;
                    System.out.printf(" ... %.2f%% (%s missing: %s )%n", missing * 0.01f, Lang.nounWithAmount(missingCards.size(), "card"), StringUtils.join(missingCards, " | ") );
                }
                missingCards.clear();
            }
        }

        for(CardRules cr : rulesByName.values()) {
            if( !allCardsByName.containsKey(cr.getName()) )
            {
                System.err.println("The card " + cr.getName() + " was not assigned to any set. Adding it to UNKNOWN set... to fix see res/cardeditions/ folder. ");
                addCard(new PaperCard(cr, CardEdition.UNKNOWN.getCode(), CardRarity.Special, 0));
            }
        }

        reIndex();
    }

    private void addCard(PaperCard paperCard) {
        allCardsByName.put(paperCard.getName(), paperCard);
    }

    private void reIndex() {
        uniqueCardsByName.clear();
        allCards.clear();
        for(Entry<String, Collection<PaperCard>> kv : allCardsByName.asMap().entrySet()) {
            uniqueCardsByName.put(kv.getKey(), Iterables.getFirst(kv.getValue(), null));
            allCards.addAll(kv.getValue());
        }
    }

    /**
     * Splits cardname into name, set and art index whenever deck line reads as name|set|artindex.
     */
    private static List<String> splitCardName(final String name) {
        String cardName = name; // .trim() ?
        String[] cardNameElems = TextUtil.split(cardName, '|');

        final String actualCardName = cardNameElems[0];
        String setName = null;
        String artIndex = "-1";
        if (cardNameElems.length > 1) {
            if (StringUtils.isNotBlank(cardNameElems[1]) && !"???".equals(cardNameElems[1])) {
                setName = cardNameElems[1];
            }
            if (cardNameElems.length > 2) {
                artIndex = cardNameElems[2];
            }
        }

        return Arrays.asList(actualCardName, setName, artIndex);
    }

    private boolean isFoil(final String cardName) {
        return cardName.toLowerCase().endsWith(CardDb.foilSuffix);
    }

    /**
     * Removes the foil suffix.
     *
     * @param cardName the card name
     * @return the string
     */
    public String removeFoilSuffix(final String cardName) {
        return cardName.toLowerCase().endsWith(CardDb.foilSuffix) ? cardName.substring(0, cardName.length() - CardDb.foilSuffixLength) : cardName;
    }

    @Override
    public PaperCard tryGetCard(final String cardName0) {
        return tryGetCard(cardName0, true);
    }

    @Override
    public PaperCard tryGetCard(final String cardName0, boolean fromLastSet) {
        if (null == cardName0) {
            return null;  // obviously
        }

        final boolean isFoil = this.isFoil(cardName0);
        final String cardName = isFoil ? this.removeFoilSuffix(cardName0) : cardName0;

        final List<String> splitName = CardDb.splitCardName(cardName);
        final int artIndex = Integer.parseInt(splitName.get(2));
        final int effectiveArtIndex = artIndex == 0 ? 1 : artIndex - 1;

        final PaperCard res = splitName.get(1) == null
                ? ( fromLastSet ? this.uniqueCardsByName.get(splitName.get(0)) : tryGetCard(splitName.get(0), Aggregates.random(this.allCardsByName.get(splitName.get(0))).getEdition(), -1))
                : tryGetCard(splitName.get(0), splitName.get(1), effectiveArtIndex);

        if (fromLastSet && null != res && CardEdition.UNKNOWN.getCode() != res.getEdition()) {
            final PaperCard res_randart = tryGetCard(res.getName(), res.getEdition(), effectiveArtIndex);
            return null != res_randart && isFoil ? getFoiled(res_randart) : res_randart;
        }
        
        return null != res && isFoil ? getFoiled(res) : res;
    }

    @Override
    public PaperCard tryGetCardPrintedByDate(final String name0, final boolean fromLatestSet, final Date printedBefore) {
        final boolean isFoil = this.isFoil(name0);
        final String cardName = isFoil ? this.removeFoilSuffix(name0) : name0;

        final List<String> splitName = CardDb.splitCardName(cardName);
        final int artIndex = Integer.parseInt(splitName.get(2));
        final int effectiveArtIndex = artIndex == 0 ? 1 : artIndex - 1;

        PaperCard res = null;
        if (null != splitName.get(1)) // set explicitly requested, should return card from it and disregard the date
            res = tryGetCard(splitName.get(0), splitName.get(1), effectiveArtIndex);
        else {
            Collection<PaperCard> cards = this.allCardsByName.get(splitName.get(0)); // cards are sorted by datetime desc
            int idxRightSet = 0;
            for (PaperCard card : cards) {
                if (editions.get(card.getEdition()).getDate().after(printedBefore))
                    idxRightSet++;
                else
                    break;
            }
            res = fromLatestSet ? Iterables.get(cards, idxRightSet, null) : Aggregates.random(Iterables.skip(cards, idxRightSet));
        }

        return null != res && isFoil ? getFoiled(res) : res;
    }

    @Override
    public PaperCard tryGetCard(final String cardName, String setName) {
        return tryGetCard(cardName, setName, -1);
    }

    @Override
    public PaperCard tryGetCard(final String cardName0, String setName, int index) {
        final boolean isFoil = this.isFoil(cardName0);
        final String cardName = isFoil ? this.removeFoilSuffix(cardName0) : cardName0;

        Collection<PaperCard> cards = allCardsByName.get(cardName);
        if ( null == cards ) return null;

        CardEdition edition = editions.get(setName);
        if ( null == edition )
            return tryGetCard(cardName, true); // set not found, try to get the same card from just any set.
        String effectiveSet = edition.getCode();

        PaperCard result = null;
        if ( index < 0 ) { // this stands for 'random art'
            PaperCard[] candidates = new PaperCard[9]; // 9 cards with same name per set is a maximum of what has been printed (Arnchenemy)
            int cnt = 0;
            for( PaperCard pc : cards ) {
                if( pc.getEdition().equalsIgnoreCase(effectiveSet) )
                    candidates[cnt++] = pc;
            }

            if (cnt == 0 ) return null;
            result = cnt == 1  ? candidates[0] : candidates[MyRandom.getRandom().nextInt(cnt)];
        } else
            for( PaperCard pc : cards ) {
                if( pc.getEdition().equalsIgnoreCase(effectiveSet) && index == pc.getArtIndex() ) {
                    result = pc;
                    break;
                }
            }
        if ( result == null ) return null;
        return isFoil ? getFoiled(result) : result;
    }

    public PaperCard getFoiled(PaperCard card0) {
        // Here - I am still unsure if there should be a cache Card->Card from unfoiled to foiled, to avoid creation of N instances of single plains
        return new PaperCard(card0.getRules(), card0.getEdition(), card0.getRarity(), card0.getArtIndex(), true);
    }

    @Override
    public int getPrintCount(String cardName, String edition) {
        int cnt = 0;
        for( PaperCard pc : allCardsByName.get(cardName) ) {
            if( pc.getEdition().equals(edition) )
                cnt++;
        }
        return cnt;
    }

    @Override
    public int getMaxPrintCount(String cardName) {
        int max = -1;
        for( PaperCard pc : allCardsByName.get(cardName) ) {
            if ( max < pc.getArtIndex() )
                max = pc.getArtIndex();
        }
        return max + 1;
    }

    @Override
    public int getArtCount(String cardName, String setName) {
        int cnt = 0;

        Collection<PaperCard> cards = allCardsByName.get(cardName);
        if ( null == cards ) {
            return 0;
        } 

        for ( PaperCard pc : cards ) {
            if ( pc.getEdition().equalsIgnoreCase(setName) ) {
                cnt++;
            }
        }

        return cnt;
    }

    // Single fetch
    @Override
    public PaperCard getCard(final String name) {
        return this.getCard(name, false);
    }

    @Override
    public PaperCard getCard(final String name0, final boolean fromLatestSet) {
        // Sometimes they read from decks things like "CardName|Set" - but we
        // can handle it
        final PaperCard result = tryGetCard(name0, fromLatestSet);
        if (null == result) {
            throw new NoSuchElementException(String.format("Card '%s' not found in our database.", name0));
        }
        return result;
    }

    @Override
    public PaperCard getCardPrintedByDate(final String name0, final boolean fromLatestSet, Date printedBefore ) {
        // Sometimes they read from decks things like "CardName|Set" - but we
        // can handle it
        final PaperCard result = tryGetCard(name0, fromLatestSet);
        if (null == result) {
            throw new NoSuchElementException(String.format("Card '%s' released before %s not found in our database.", name0, printedBefore.toString()));
        }
        return result;
    }

    // Advanced fetch by name+set
    @Override
    public PaperCard getCard(final String name, final String set) {
        return this.getCard(name, set, -1);
    }

    @Override
    public PaperCard getCard(final String name, final String set, final int artIndex) {
        final PaperCard result = tryGetCard(name, set, artIndex);
        if (null == result) {
            final String message = String.format("Asked for '%s' from '%s' #%d: db didn't find that copy.", name, set, artIndex);
            throw new NoSuchElementException(message);
        }
        return result;
    }

    // returns a list of all cards from their respective latest editions
    @Override
    public Collection<PaperCard> getUniqueCards() {
        return roUniqueCards;
    }

    @Override
    public List<PaperCard> getAllCards() {
        return roAllCards;
    }

    /**  Returns a modifiable list of cards matching the given predicate */
    @Override
    public List<PaperCard> getAllCards(Predicate<PaperCard> predicate) {
        return Lists.newArrayList(Iterables.filter(this.roAllCards, predicate));
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
            for(PaperCard c : cc)
                if (sets.contains(c.getEdition()))
                    return true;
            return false;
        }
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
                for(CardEdition e : editions.getOrderedEditions()) {
                    int artIdx = 0;
                    for(CardInSet cis : e.getCards()) {
                        if( !cis.name.equals(cardName) )
                            continue;
                        paperCards.add(new PaperCard(rules, e.getCode(), cis.rarity, artIdx++));
                    }
                }
            } else {
                String lastEdition = null;
                int artIdx = 0;
                for(Pair<String, CardRarity> tuple : whenItWasPrinted){
                    if(!tuple.getKey().equals(lastEdition)) {
                        artIdx = 0;
                        lastEdition = tuple.getKey();
                    }
                    CardEdition ed = editions.get(lastEdition);
                    if(null == ed)
                        continue;
                    paperCards.add(new PaperCard(rules, lastEdition, tuple.getValue(), artIdx++));
                }
            }
            if(paperCards.isEmpty())
                paperCards.add(new PaperCard(rules, CardEdition.UNKNOWN.getCode(), CardRarity.Special, 0));

            // 2. add them to db
            for (PaperCard paperCard : paperCards)
                addCard(paperCard);
            // 3. reindex can be temporary disabled and run after the whole batch of rules is added to db.
            if(immediateReindex)
                reIndex();

            return result;
        }
        public void removeCard(String name) {
            allCardsByName.removeAll(name);
            uniqueCardsByName.remove(name);
            rulesByName.remove(name);
            Iterator<PaperCard> it = allCards.iterator();
            while(it.hasNext()) {
                PaperCard pc = it.next();
                if( pc.getName().equalsIgnoreCase(name))
                    it.remove();
            }
        }
        public void rebuildIndex() { reIndex(); }

        public boolean isImmediateReindex() {
            return immediateReindex;
        }
        public void setImmediateReindex(boolean immediateReindex) {
            this.immediateReindex = immediateReindex;
        }
    }
}
