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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.card.CardEdition.Type;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.MyRandom;
import forge.util.maps.CollectionSuppliers;
import forge.util.maps.MapOfLists;
import forge.util.maps.TreeMapOfLists;

public final class CardDb implements ICardDatabase {
    private static volatile CardDb commonCards = null; // 'volatile' keyword makes this working
    private static volatile CardDb variantCards = null; // 'volatile' keyword makes this working
    public final static String foilSuffix = " foil";
    private final static int foilSuffixLength = foilSuffix.length(); 

    public static ICardDatabase instance() {
        if (CardDb.commonCards == null) {
            throw new NullPointerException("CardDb has not yet been initialized, run setup() first");
        }
        return CardDb.commonCards;
    }
    
    public static ICardDatabase variants() {
        if (CardDb.variantCards == null) {
            throw new NullPointerException("CardDb has not yet been initialized, run setup() first");
        }
        return CardDb.variantCards;
    }

    public static void setup(final Iterable<CardRules> rules, Iterable<CardEdition> editions) {
        if (CardDb.commonCards != null) {
            throw new RuntimeException("CardDb has already been initialized, don't do it twice please");
        }
        synchronized (CardDb.class) {
            if (CardDb.commonCards == null) { // It's broken under 1.4 and below, on 1.5+ works again!
                CardSorter cs = new CardSorter(rules);
                commonCards = new CardDb(cs.regularCards, editions, false);
                variantCards = new CardDb(cs.variantsCards, editions, false);
            }
        }
    }

    // need this to obtain cardReference by name+set+artindex
    private final MapOfLists<String, PaperCard> allCardsByName = new TreeMapOfLists<String, PaperCard>(String.CASE_INSENSITIVE_ORDER, CollectionSuppliers.<PaperCard>arrayLists());
    private final Map<String, PaperCard> uniqueCardsByName = new TreeMap<String, PaperCard>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, CardRules> rulesByName;
    
    private final List<PaperCard> allCards = new ArrayList<PaperCard>();
    private final List<PaperCard> roAllCards = Collections.unmodifiableList(allCards); 
    private final Collection<PaperCard> roUniqueCards = Collections.unmodifiableCollection(uniqueCardsByName.values());
    
    // Lambda to get rules for selects from list of printed cards
    /** The Constant fnGetCardPrintedByForgeCard. */
    public static final Function<Card, PaperCard> FN_GET_CARD_PRINTED_BY_FORGE_CARD = new Function<Card, PaperCard>() {
        @Override
        public PaperCard apply(final Card from) {
            return CardDb.instance().getCard(from.getName());
        }
    };

    private CardDb(Map<String, CardRules> rules, Iterable<CardEdition> editions, boolean logMissingCards) {
        this.rulesByName = rules;
        List<String> missingCards = new ArrayList<String>();
        for(CardEdition e : editions) {
            boolean worthLogging = logMissingCards && ( e.getType() == Type.CORE || e.getType() == Type.EXPANSION || e.getType() == Type.REPRINT );
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
                    System.out.printf(" ... %.2f%% (%s missing: %s)%n", missing * 0.01f, Lang.nounWithAmount(missingCards.size(), "card"), StringUtils.join(missingCards, " | ") );
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
        allCardsByName.add(paperCard.name, paperCard);
    }

    private void reIndex() {
        uniqueCardsByName.clear();
        allCards.clear();
        for(Entry<String, Collection<PaperCard>> kv : allCardsByName.entrySet()) {
            uniqueCardsByName.put(kv.getKey(), Iterables.get(kv.getValue(), 0));
            allCards.addAll(kv.getValue());
        }
    }

    /**
     * Splits cardname into Name and set whenever deck line reads as name|set.
     */
    private static ImmutablePair<String, String> splitCardName(final String name) {
        String cardName = name; // .trim() ?
        final int pipePos = cardName.indexOf('|');

        if (pipePos >= 0) {
            final String setName = cardName.substring(pipePos + 1).trim();
            cardName = cardName.substring(0, pipePos);
            // only if set is not blank try to load it
            if (StringUtils.isNotBlank(setName) && !"???".equals(setName)) {
                return new ImmutablePair<String, String>(cardName, setName);
            }
        }
        return new ImmutablePair<String, String>(cardName, null);
    }

    private boolean isFoil(final String cardName) {
        return cardName.toLowerCase().endsWith(CardDb.foilSuffix) && (cardName.length() > CardDb.foilSuffixLength);
    }

    /**
     * Removes the foil suffix.
     *
     * @param cardName the card name
     * @return the string
     */
    public String removeFoilSuffix(final String cardName) {
        return cardName.substring(0, cardName.length() - CardDb.foilSuffixLength);
    }

    
    /**
     * Checks if is card supported.
     */
    
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
        final ImmutablePair<String, String> nameWithSet = CardDb.splitCardName(cardName);
        
        final PaperCard res = nameWithSet.right == null 
                ? ( fromLastSet ? this.uniqueCardsByName.get(nameWithSet.left) : Aggregates.random(this.allCardsByName.get(nameWithSet.left)) ) 
                : tryGetCard(nameWithSet.left, nameWithSet.right);
        return null != res && isFoil ? PaperCard.makeFoiled(res) : res;
    }

    @Override
    public PaperCard tryGetCard(final String cardName, String setName) {
        return tryGetCard(cardName, setName, -1);
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
    public PaperCard tryGetCard(final String cardName, String setName, int index) {
        Collection<PaperCard> cards = allCardsByName.get(cardName);
        if ( null == cards ) return null;

        if ( index < 0 ) { // this stands for 'random art'
            PaperCard[] candidates = new PaperCard[9]; // 9 cards with same name per set is a maximum of what has been printed (Arnchenemy)
            int cnt = 0;
            for( PaperCard pc : cards ) {
                if( pc.getEdition().equals(setName) )
                    candidates[cnt++] = pc;
            }

            if (cnt == 0 ) return null;
            if (cnt == 1 ) return candidates[0];
            return candidates[MyRandom.getRandom().nextInt(cnt)];
        } else 
            for( PaperCard pc : cards ) {
                if( pc.getEdition().equals(setName) && index == pc.getArtIndex() )
                    return pc;
            }
        return null;
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

    // Fetch from Forge's Card instance. Well, there should be no errors, but
    // we'll still check
    public static PaperCard getCard(final Card forgeCard) {
        final String name = forgeCard.getName();
        final String set = forgeCard.getCurSetCode();
        
        if (StringUtils.isNotBlank(set)) {
            PaperCard cp = variants().tryGetCard(name, set);
            
            return cp == null ? instance().getCard(name, set) : cp;
        }
        PaperCard cp = variants().tryGetCard(name, true);
        return cp == null ? instance().getCard(name) : cp;
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

    private static class CardSorter{
        // Here are refs, get them by name
        public final Map<String, CardRules> regularCards = new TreeMap<String, CardRules>(String.CASE_INSENSITIVE_ORDER);
        public final Map<String, CardRules> variantsCards = new TreeMap<String, CardRules>(String.CASE_INSENSITIVE_ORDER);


        CardSorter(final Iterable<CardRules> parser) {
            for (CardRules card : parser) {
                if (null == card) continue;
                
                final String cardName = card.getName();
                if ( card.isVariant() )
                    variantsCards.put(cardName, card);
                else
                    regularCards.put(cardName, card);
            }
        }
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
            for(PaperCard c : cc) if (sets.contains(c.getEdition())) return true;
            return false;
        }
    }
    
}
