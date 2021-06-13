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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import forge.StaticData;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;

import forge.card.CardEdition.CardInSet;
import forge.card.CardEdition.Type;
import forge.deck.generation.IDeckGenPool;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.CollectionSuppliers;
import forge.util.Lang;
import forge.util.MyRandom;
import forge.util.TextUtil;

public final class CardDb implements ICardDatabase, IDeckGenPool {
    public final static String foilSuffix = "+";
    public final static char NameSetSeparator = '|';
    private final String exlcudedCardName = "Concentrate";
    private final String exlcudedCardSet = "DS0";

    // need this to obtain cardReference by name+set+artindex
    private final ListMultimap<String, PaperCard> allCardsByName = Multimaps.newListMultimap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER),  CollectionSuppliers.arrayLists());
    private final Map<String, PaperCard> uniqueCardsByName = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, CardRules> rulesByName;
    private final Map<String, ICardFace> facesByName = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
    private static Map<String, String> artPrefs = new HashMap<>();

    private final Map<String, String> alternateName = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, Integer> artIds = new HashMap<>();

    private final CardEdition.Collection editions;
    private List<String> filtered;

    public enum SetPreference {
        Latest(false),
        LatestCoreExp(true),
        Earliest(false),
        EarliestCoreExp(true),
        Random(false);

        final boolean filterSets;
        SetPreference(boolean filterIrregularSets) {
            filterSets = filterIrregularSets;
        }

        public boolean accept(CardEdition ed) {
            if (ed == null)  return false;
            return !filterSets || ed.getType() == Type.CORE || ed.getType() == Type.EXPANSION || ed.getType() == Type.REPRINT;
        }
    }

    // NO GETTERS/SETTERS HERE!
    public static class CardRequest {
        // TODO Move Request to its own class
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

    public CardDb(Map<String, CardRules> rules, CardEdition.Collection editions0, List<String> filteredCards) {
        this.filtered = filteredCards;
        this.rulesByName = rules;
        this.editions = editions0;

        // create faces list from rules
        for (final CardRules rule : rules.values() ) {
            if (filteredCards.contains(rule.getName()) && !exlcudedCardName.equalsIgnoreCase(rule.getName()))
                continue;
            final ICardFace main = rule.getMainPart();
            facesByName.put(main.getName(), main);
            if (main.getAltName() != null) {
                alternateName.put(main.getAltName(), main.getName());
            }
            final ICardFace other = rule.getOtherPart();
            if (other != null) {
                facesByName.put(other.getName(), other);
                if (other.getAltName() != null) {
                    alternateName.put(other.getAltName(), other.getName());
                }
            }
        }
    }

    private ListMultimap<String, PaperCard> getAllCardsByName() {
        return allCardsByName;
    }

    private void addSetCard(CardEdition e, CardInSet cis, CardRules cr) {
        int artIdx = 1;
        String key = e.getCode() + "/" + cis.name;
        if (artIds.containsKey(key)) {
            artIdx = artIds.get(key) + 1;
        }

        artIds.put(key, artIdx);
        addCard(new PaperCard(cr, e.getCode(), cis.rarity, artIdx));
    }

    public void loadCard(String cardName, CardRules cr) {
        rulesByName.put(cardName, cr);
        // This seems very unperformant. Does this get called often?
        System.out.println("Inside loading card");

        for (CardEdition e : editions) {
            for (CardInSet cis : e.getAllCardsInSet()) {
                if (cis.name.equalsIgnoreCase(cardName)) {
                    addSetCard(e, cis, cr);
                }
            }
        }

        reIndex();
    }

    public void initialize(boolean logMissingPerEdition, boolean logMissingSummary, boolean enableUnknownCards) {
        Set<String> allMissingCards = new LinkedHashSet<>();
        List<String> missingCards = new ArrayList<>();
        CardEdition upcomingSet = null;
        Date today = new Date();

        for (CardEdition e : editions.getOrderedEditions()) {
            boolean coreOrExpSet = e.getType() == CardEdition.Type.CORE || e.getType() == CardEdition.Type.EXPANSION;
            boolean isCoreExpSet = coreOrExpSet || e.getType() == CardEdition.Type.REPRINT;
            if (logMissingPerEdition && isCoreExpSet) {
                System.out.print(e.getName() + " (" + e.getAllCardsInSet().size() + " cards)");
            }
            if (coreOrExpSet && e.getDate().after(today) && upcomingSet == null) {
                upcomingSet = e;
            }

            for (CardEdition.CardInSet cis : e.getAllCardsInSet()) {
                CardRules cr = rulesByName.get(cis.name);
                if (cr != null) {
                    addSetCard(e, cis, cr);
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
                    int missing = (e.getAllCardsInSet().size() - missingCards.size()) * 10000 / e.getAllCardsInSet().size();
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

        if (upcomingSet != null) {
            System.err.println("Upcoming set " + upcomingSet + " dated in the future. All unaccounted cards will be added to this set with unknown rarity.");
        }

        for (CardRules cr : rulesByName.values()) {
            if (!contains(cr.getName())) {
                if (upcomingSet != null) {
                    addCard(new PaperCard(cr, upcomingSet.getCode(), CardRarity.Unknown, 1));
                } else if(enableUnknownCards) {
                    System.err.println("The card " + cr.getName() + " was not assigned to any set. Adding it to UNKNOWN set... to fix see res/editions/ folder. ");
                    addCard(new PaperCard(cr, CardEdition.UNKNOWN.getCode(), CardRarity.Special, 1));
                }
            }
        }

        reIndex();
    }

    public void addCard(PaperCard paperCard) {
        if (excludeCard(paperCard.getName(), paperCard.getEdition()))
            return;

        allCardsByName.put(paperCard.getName(), paperCard);

        if (paperCard.getRules().getSplitType() == CardSplitType.None) { return; }

        if (paperCard.getRules().getOtherPart() != null) {
            //allow looking up card by the name of other faces
            allCardsByName.put(paperCard.getRules().getOtherPart().getName(), paperCard);
        }
        if (paperCard.getRules().getSplitType() == CardSplitType.Split) {
            //also include main part for split cards
            allCardsByName.put(paperCard.getRules().getMainPart().getName(), paperCard);
        }
    }
    private boolean excludeCard(String cardName, String cardEdition) {
        if (filtered.isEmpty())
            return false;
        if (filtered.contains(cardName)) {
            if (exlcudedCardSet.equalsIgnoreCase(cardEdition) && exlcudedCardName.equalsIgnoreCase(cardName))
                return true;
            else if (!exlcudedCardName.equalsIgnoreCase(cardName))
                return true;
        }
        return false;
    }
    private void reIndex() {
        uniqueCardsByName.clear();
        for (Entry<String, Collection<PaperCard>> kv : getAllCardsByName().asMap().entrySet()) {
            PaperCard pc = getFirstWithImage(kv.getValue());
            uniqueCardsByName.put(kv.getKey(), pc);
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

    public CardRules getRules(String cardname) {
        CardRules result = rulesByName.get(cardname);
        if (result != null) {
            return result;
        } else {
            return CardRules.getUnsupportedCardNamed(cardname);
        }
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

    public String getCardCollectorNumber(String cardName, String reqEdition, int artIndex) {
        cardName = getName(cardName);
        CardEdition edition = editions.get(reqEdition);
        if (edition == null)
            return null;
        int numMatches = 0;
        for (CardInSet card : edition.getAllCardsInSet()) {
            if (card.name.equalsIgnoreCase(cardName)) {
                numMatches += 1;
                if (numMatches == artIndex) {
                    return card.collectorNumber;
                }
            }
        }
        return null;
    }

    private PaperCard tryGetCard(CardRequest request) {
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

    @Override
    public PaperCard getCardFromEdition(final String cardName, SetPreference fromSet) {
        return getCardFromEdition(cardName, null, fromSet);
    }

    @Override
    public PaperCard getCardFromEdition(final String cardName, final Date printedBefore, final SetPreference fromSet) {
        return getCardFromEdition(cardName, printedBefore, fromSet, -1);
    }

    @Override
    public PaperCard getCardFromEdition(final String cardName, final Date printedBefore, final SetPreference fromSets, int artIndex) {
        final CardRequest cr = CardRequest.fromString(cardName);
        SetPreference fromSet = fromSets;
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

        //overrides
        if (StaticData.instance().getPrefferedArtOption().equals("Earliest"))
            fromSet = SetPreference.EarliestCoreExp;

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

    public PaperCard getFoiled(PaperCard card0) {
        // Here - I am still unsure if there should be a cache Card->Card from unfoiled to foiled, to avoid creation of N instances of single plains
        return new PaperCard(card0.getRules(), card0.getEdition(), card0.getRarity(), card0.getArtIndex(), true);
    }

    @Override
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

    @Override
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

    @Override
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

    // returns a list of all cards from their respective latest (or preferred) editions
    @Override
    public Collection<PaperCard> getUniqueCards() {
        return uniqueCardsByName.values();
    }

    public Collection<PaperCard> getUniqueCardsNoAlt() {
        return Maps.filterEntries(this.uniqueCardsByName, new Predicate<Entry<String, PaperCard>>() {
            @Override
            public boolean apply(Entry<String, PaperCard> e) {
                if (e == null)
                    return false;
                return e.getKey().equals(e.getValue().getName());
            }
        }).values();
    }

    public PaperCard getUniqueByName(final String name) {
        return uniqueCardsByName.get(getName(name));
    }

    public Collection<ICardFace> getAllFaces() {
        return facesByName.values();
    }

    public ICardFace getFaceByName(final String name) {
        return facesByName.get(getName(name));
    }

    @Override
    public Collection<PaperCard> getAllCards() {
        return Collections.unmodifiableCollection(getAllCardsByName().values());
    }

    public Collection<PaperCard> getAllCardsNoAlt() {
        return Multimaps.filterEntries(getAllCardsByName(), new Predicate<Entry<String, PaperCard>>() {
            @Override
            public boolean apply(Entry<String, PaperCard> entry) {
                return entry.getKey().equals(entry.getValue().getName());
            }
        }).values();
    }

    public Collection<PaperCard> getAllNonPromoCards() {
        return Lists.newArrayList(Iterables.filter(getAllCards(), new Predicate<PaperCard>() {
            @Override
            public boolean apply(final PaperCard paperCard) {
                CardEdition edition = null;
                try {
                    edition = editions.getEditionByCodeOrThrow(paperCard.getEdition());
                } catch (Exception ex) {
                    return false;
                }
                return edition != null && edition.getType() != Type.PROMO;
            }
        }));
    }

    public Collection<PaperCard> getAllNonPromosNonReprintsNoAlt() {
        return Lists.newArrayList(Iterables.filter(getAllCardsNoAlt(), new Predicate<PaperCard>() {
            @Override
            public boolean apply(final PaperCard paperCard) {
                CardEdition edition = null;
                try {
                    edition = editions.getEditionByCodeOrThrow(paperCard.getEdition());
                    if (edition.getType() == Type.PROMO||edition.getType() == Type.REPRINT)
                        return false;
                } catch (Exception ex) {
                    return false;
                }
                return true;
            }
        }));
    }

    public String getName(final String cardName) {
        if (alternateName.containsKey(cardName)) {
            return alternateName.get(cardName);
        }
        return cardName;
    }

    @Override
    public List<PaperCard> getAllCards(String cardName) {
        return getAllCardsByName().get(getName(cardName));
    }

    public List<PaperCard> getAllCardsNoAlt(String cardName) {
        return Lists.newArrayList(Multimaps.filterEntries(getAllCardsByName(), new Predicate<Entry<String, PaperCard>>() {
            @Override
            public boolean apply(Entry<String, PaperCard> entry) {
                return entry.getKey().equals(entry.getValue().getName());
            }
        }).get(getName(cardName)));
    }

    /**  Returns a modifiable list of cards matching the given predicate */
    @Override
    public List<PaperCard> getAllCards(Predicate<PaperCard> predicate) {
        return Lists.newArrayList(Iterables.filter(getAllCards(), predicate));
    }

    /**  Returns a modifiable list of cards matching the given predicate */
    public List<PaperCard> getAllCardsNoAlt(Predicate<PaperCard> predicate) {
        return Lists.newArrayList(Iterables.filter(getAllCardsNoAlt(), predicate));
    }

    // Do I want a foiled version of these cards?
    @Override
    public List<PaperCard> getAllCardsFromEdition(CardEdition edition) {
        List<PaperCard> cards = Lists.newArrayList();

        for(CardInSet cis : edition.getAllCardsInSet()) {
            PaperCard card = this.getCard(cis.name, edition.getCode());
            if (card == null) {
                // Just in case the card is listed in the edition file but Forge doesn't support it
                continue;
            }

            cards.add(card);
        }
        return cards;
    }

    @Override
    public boolean contains(String name) {
        return getAllCardsByName().containsKey(getName(name));
    }

    @Override
    public Iterator<PaperCard> iterator() {
        return getAllCards().iterator();
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
            for (PaperCard c : getAllCards(subject.getName())) {
                if (sets.contains(c.getEdition())) {
                    return true;
                }
            }
            return false;
        }
    }
    // This Predicate validates if a card was printed at [rarity], on any of its printings
    public Predicate<? super PaperCard> wasPrintedAtRarity(CardRarity rarity) {
        return new PredicatePrintedAtRarity(rarity);
    }

    private class PredicatePrintedAtRarity implements Predicate<PaperCard> {
        private final Set<String> matchingCards;

        public PredicatePrintedAtRarity(CardRarity rarity) {
            this.matchingCards = new HashSet<>();
            for (PaperCard c : getAllCards()) {
                if (c.getRarity() == rarity) {
                    this.matchingCards.add(c.getName());
                }
            }
        }
        @Override
        public boolean apply(final PaperCard subject) {
            return matchingCards.contains(subject.getName());
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

    public PaperCard createUnsupportedCard(String cardName) {
        CardRequest request = CardRequest.fromString(cardName);
        CardEdition cardEdition = CardEdition.UNKNOWN;
        CardRarity cardRarity = CardRarity.Unknown;

        // May iterate over editions and find out if there is any card named 'cardName' but not implemented with Forge script.
        if (StringUtils.isBlank(request.edition)) {
            for (CardEdition edition : editions) {
                for (CardInSet cardInSet : edition.getAllCardsInSet()) {
                    if (cardInSet.name.equals(request.cardName)) {
                        cardEdition = edition;
                        cardRarity = cardInSet.rarity;
                        break;
                    }
                }
                if (cardEdition != CardEdition.UNKNOWN) {
                    break;
                }
            }
        } else {
            cardEdition = editions.get(request.edition);
            if (cardEdition != null) {
                for (CardInSet cardInSet : cardEdition.getAllCardsInSet()) {
                    if (cardInSet.name.equals(request.cardName)) {
                        cardRarity = cardInSet.rarity;
                        break;
                    }
                }
            }
            else {
                cardEdition = CardEdition.UNKNOWN;
            }
        }

        if (cardRarity == CardRarity.Unknown) {
            System.err.println("Forge does not know of such a card's existence. Have you mistyped the card name?");
        } else {
            System.err.println("We're sorry, but you cannot use this card yet.");
        }

        return new PaperCard(CardRules.getUnsupportedCardNamed(request.cardName), cardEdition.getCode(), cardRarity, 1);
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
            List<PaperCard> paperCards = new ArrayList<>();
            if (null == whenItWasPrinted || whenItWasPrinted.isEmpty()) {
                // TODO Not performant Each time we "putCard" we loop through ALL CARDS IN ALL editions
                for (CardEdition e : editions.getOrderedEditions()) {
                    int artIdx = 1;
                    for (CardInSet cis : e.getAllCardsInSet()) {
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
