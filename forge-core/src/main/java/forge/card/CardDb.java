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
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;

import forge.card.CardEdition.CardInSet;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.CollectionSuppliers;
import forge.util.Lang;
import forge.util.MyRandom;
import forge.util.TextUtil;

public final class CardDb implements ICardDatabase {
    public final static String foilSuffix = "+";
    // need this to obtain cardReference by name+set+artindex
    private final ListMultimap<String, PaperCard> allCardsByName = Multimaps.newListMultimap(new TreeMap<String,Collection<PaperCard>>(String.CASE_INSENSITIVE_ORDER),  CollectionSuppliers.<PaperCard>arrayLists());
    private final Map<String, PaperCard> uniqueCardsByName = new TreeMap<String, PaperCard>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, CardRules> rulesByName;

    private final List<PaperCard> allCards = new ArrayList<PaperCard>();
    private final List<PaperCard> roAllCards = Collections.unmodifiableList(allCards);
    private final Collection<PaperCard> roUniqueCards = Collections.unmodifiableCollection(uniqueCardsByName.values());
    private final CardEdition.Collection editions;

    public enum SetPreference {
        Latest,
        Earliest,
        Random
    }
    
    // NO GETTERS/SETTERS HERE!
    private static class CardRequest {
        public String cardName;
        public String edition;
        public int artIndex;
        public boolean isFoil;
        
        public CardRequest(String name, String edition, int artIndex, boolean isFoil) {
            cardName = name;
            this.edition = edition;
            this.artIndex = artIndex;
            this.isFoil = isFoil;
        }
        
        public static CardRequest fromString(String name) { 
            boolean isFoil = name.endsWith(foilSuffix);
            if( isFoil )
                name = name.substring(0, name.length() - foilSuffix.length());

            String[] nameParts = TextUtil.split(name, '|');

            int setPos = nameParts.length >= 2 && !StringUtils.isNumeric(nameParts[1]) ? 1 : -1;
            int artPos = nameParts.length >= 2 && StringUtils.isNumeric(nameParts[1]) ? 1 : nameParts.length >= 3 && StringUtils.isNumeric(nameParts[2]) ? 2 : -1;

            int artIndex = artPos > 0 ? Integer.parseInt(nameParts[artPos]) : -1;
            String setName = setPos > 0 ? nameParts[setPos] : null;
            if( "???".equals(setName) )
                setName = null;

            return new CardRequest(nameParts[0], setName, artIndex, isFoil);
        }
    }
    
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

    @Override
    public PaperCard getCard(final String cardName) {
        CardRequest request = CardRequest.fromString(cardName);
        return tryGetCard(request);
    }

    @Override
    public PaperCard getCard(final String cardName, String setName) {
        CardRequest request = CardRequest.fromString(cardName);
        request.edition = setName;
        return tryGetCard(request);
    }

    @Override
    public PaperCard getCard(final String cardName, String setName, int artIndex) {
        CardRequest request = CardRequest.fromString(cardName);
        request.edition = setName;
        request.artIndex = artIndex;
        return tryGetCard(request);
    }    
    
    
    private PaperCard tryGetCard(CardRequest request) {
        Collection<PaperCard> cards = allCardsByName.get(request.cardName);
        if ( null == cards ) return null;

        PaperCard result = null;
        
        if ( request.artIndex < 0 ) { // this stands for 'random art'
            List<PaperCard> candidates = new ArrayList<PaperCard>(9); // 9 cards with same name per set is a maximum of what has been printed (Arnchenemy)
            for( PaperCard pc : cards ) {
                if( pc.getEdition().equalsIgnoreCase(request.edition) || request.edition == null )
                    candidates.add(pc);
            }

            if (candidates.isEmpty())
                return null;
            result = Aggregates.random(candidates);
        } else {
            for( PaperCard pc : cards ) {
                if( pc.getEdition().equalsIgnoreCase(request.edition) && request.artIndex == pc.getArtIndex() ) {
                    result = pc;
                    break;
                }
            }
        }
        if ( result == null ) return null;
        
        return request.isFoil ? getFoiled(result) : result;
    }
    
    @Override
    public PaperCard getCardFromEdition(final String cardName, SetPreference fromSet) {
        return getCardFromEdition(cardName, null, fromSet);
    }
    
    @Override
    public PaperCard getCardFromEdition(final String cardName, final Date printedBefore, final SetPreference fromSet) {
        List<PaperCard> cards = this.allCardsByName.get(cardName);

        int sz = cards.size();
        if( fromSet == SetPreference.Earliest ) {
            for(int i = 0 ; i < sz ; i++)
                if( printedBefore == null || editions.get(cards.get(i).getEdition()).getDate().after(printedBefore) )
                    return cards.get(i);
            return null;
        } else if( fromSet == SetPreference.Latest || fromSet == null || fromSet == SetPreference.Random  ) {
            for(int i = sz - 1 ; i >= 0 ; i--)
                if( printedBefore == null || editions.get(cards.get(i).getEdition()).getDate().after(printedBefore) ) {
                    if( fromSet == SetPreference.Latest )
                        return cards.get(i);
                    return cards.get(MyRandom.getRandom().nextInt(i+1));
                }
            return null;
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
