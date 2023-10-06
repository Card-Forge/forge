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
package forge.deck;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.CardType;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.Map.Entry;

/**
 * <p>
 * Deck class.
 * </p>
 * 
 * The set of MTG legal cards that become player's library when the game starts.
 * Any other data is not part of a deck and should be stored elsewhere. Current
 * fields allowed for deck metadata are Name, Title, Description and Deck Type.
 */
@SuppressWarnings("serial")
public class Deck extends DeckBase implements Iterable<Entry<DeckSection, CardPool>> {
    private final Map<DeckSection, CardPool> parts = new EnumMap<>(DeckSection.class);
    private final Set<String> tags = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    // Supports deferring loading a deck until we actually need its contents. This works in conjunction with
    // the lazy card load feature to ensure we don't need to load all cards on start up.
    private Map<String, List<String>> deferredSections = null;
    private Map<String, List<String>> loadedSections = null;
    private String lastCardArtPreferenceUsed = "";
    private Boolean lastCardArtOptimisationOptionUsed = null;
    private boolean includeCardsFromUnspecifiedSet = false;
    private transient UnplayableAICards unplayableAI = null;

    public Deck() {
        this("");
    }

    /**
     * Instantiates a new deck.
     *
     * @param name0 the name0
     */
    public Deck(final String name0) {
        super(name0);
        getOrCreate(DeckSection.Main);
    }

    /**
     * Create a new deck from a cardpool
     */
    public Deck(final String name0, CardPool main) {
        super(name0);
        getOrCreate(DeckSection.Main).add(main.toFlatList());
    }


    /**
     * Copy constructor.
     * 
     * @param other
     *            the {@link Deck} to copy.
     */
    public Deck(final Deck other) {
        this(other, other.getName());
    }

    /**
     * Copy constructor with a different name for the new deck.
     * 
     * @param other
     *            the {@link Deck} to copy.
     * @param newName
     *            the name of the new deck.
     */
    public Deck(final Deck other, final String newName) {
        super(newName);
        other.cloneFieldsTo(this);
        for (final Entry<DeckSection, CardPool> sections : other.parts.entrySet()) {
            parts.put(sections.getKey(), new CardPool(sections.getValue()));
        }
        tags.addAll(other.getTags());
    }

    @Override
    public String getItemType() {
        return "Deck";
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getName();
    }

    public CardPool getMain() {
        loadDeferredSections();
        return parts.get(DeckSection.Main);
    }

    public List<PaperCard> getCommanders() {
        List<PaperCard> result = Lists.newArrayList();
        final CardPool cp = get(DeckSection.Commander);
        if (cp == null) {
            return result;
        }
        for (final Entry<PaperCard, Integer> c : cp) {
            result.add(c.getKey());
        }
        if (result.size() > 1) { //sort by type so signature spell comes after oathbreaker
            Collections.sort(result, new Comparator<PaperCard>() {
                @Override
                public int compare(final PaperCard c1, final PaperCard c2) {
                    return Boolean.compare(c1.getRules().canBeSignatureSpell(), c2.getRules().canBeSignatureSpell());
                }
            });
        }
        return result;
    }

    //at least for now, Oathbreaker will only support one oathbreaker and one signature spell
    public PaperCard getOathbreaker() {
        final CardPool cp = get(DeckSection.Commander);
        if (cp == null) {
            return null;
        }
        for (final Entry<PaperCard, Integer> c : cp) {
            PaperCard card = c.getKey();
            if (card.getRules().canBeOathbreaker()) {
                return card;
            }
        }
        return null;
    }
    public PaperCard getSignatureSpell() {
        final CardPool cp = get(DeckSection.Commander);
        if (cp == null) {
            return null;
        }
        for (final Entry<PaperCard, Integer> c : cp) {
            PaperCard card = c.getKey();
            if (card.getRules().canBeSignatureSpell()) {
                return card;
            }
        }
        return null;
    }

    // may return nulls
    public CardPool get(DeckSection deckSection) {
        loadDeferredSections();
        return parts.get(deckSection);
    }

    public boolean has(DeckSection deckSection) {
        final CardPool cp = get(deckSection);
        return cp != null && !cp.isEmpty();
    }

    // will return new if it was absent
    public CardPool getOrCreate(DeckSection deckSection) {
        CardPool p = get(deckSection);
        if (p != null)
            return p;
        p = new CardPool();
        this.parts.put(deckSection, p);
        return p;
    }
    
    public void putSection(DeckSection section, CardPool pool) {
        this.parts.put(section, pool);
    }

    public void setDeferredSections(Map<String, List<String>> deferredSections) {
        this.deferredSections = deferredSections;
    }

    /* (non-Javadoc)
     * @see forge.deck.DeckBase#cloneFieldsTo(forge.deck.DeckBase)
     */
    @Override
    protected void cloneFieldsTo(final DeckBase clone) {
        super.cloneFieldsTo(clone);
        final Deck result = (Deck) clone;
        loadDeferredSections();
        for (Entry<DeckSection, CardPool> kv : parts.entrySet()) {
            CardPool cp = new CardPool();
            result.parts.put(kv.getKey(), cp);
            cp.addAll(kv.getValue());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.deck.DeckBase#newInstance(java.lang.String)
     */
    @Override
    protected DeckBase newInstance(final String name0) {
        return new Deck(name0);
    }

    private void loadDeferredSections() {
        if (deferredSections == null && loadedSections == null)
            return;

        if (loadedSections != null && !includeCardsFromUnspecifiedSet)
            return;  // deck loaded, and does not include ANY card with no specified edition: all good!

        String cardArtPreference = StaticData.instance().getCardArtPreferenceName();
        boolean smartCardArtSelection = StaticData.instance().isEnabledCardArtSmartSelection();

        if (lastCardArtOptimisationOptionUsed == null)  // first time here
            lastCardArtOptimisationOptionUsed = smartCardArtSelection;

        if (loadedSections != null && cardArtPreference.equals(lastCardArtPreferenceUsed) &&
                lastCardArtOptimisationOptionUsed == smartCardArtSelection)
            return;  // deck loaded already - card with no set have been found, but no change since last time: all good!

        Map<String, List<String>> referenceDeckLoadingMap;
        if (deferredSections != null) {
            this.validateDeferredSections();
            referenceDeckLoadingMap = new HashMap<>(this.deferredSections);
        } else
            referenceDeckLoadingMap = new HashMap<>(loadedSections);

        loadedSections = new HashMap<>();
        lastCardArtPreferenceUsed = cardArtPreference;
        lastCardArtOptimisationOptionUsed = smartCardArtSelection;
        Map<DeckSection, ArrayList<String>> cardsWithNoEdition = null;
        if (smartCardArtSelection)
             cardsWithNoEdition = new EnumMap<>(DeckSection.class);

        for (Entry<String, List<String>> s : referenceDeckLoadingMap.entrySet()) {
            // first thing, update loaded section
            loadedSections.put(s.getKey(), s.getValue());
            DeckSection sec = DeckSection.smartValueOf(s.getKey());
            if (sec == null)
                continue;
            final List<String> cardsInSection = s.getValue();
            ArrayList<String> cardNamesWithNoEdition = getAllCardNamesWithNoSpecifiedEdition(cardsInSection);
            if (cardNamesWithNoEdition.size() > 0) {
                includeCardsFromUnspecifiedSet = true;
                if (smartCardArtSelection)
                    cardsWithNoEdition.put(sec, cardNamesWithNoEdition);
            }

            CardPool pool = CardPool.fromCardList(cardsInSection);
            putSection(sec, pool);
        }
        deferredSections = null;  // set to null, just in case!
        if (includeCardsFromUnspecifiedSet && smartCardArtSelection)
            optimiseCardArtSelectionInDeckSections(cardsWithNoEdition);
    }

    private void validateDeferredSections() {
        /*
         Construct a temporary (DeckSection, CardPool) Maps, to be sanitised and finalised
         before copying into `this.parts`. This sanitisation is applied because of the
         validation schema introduced in DeckSections.
         */
        Map<String, List<String>> validatedSections = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Entry<String, List<String>> s : this.deferredSections.entrySet()) {
            final DeckSection deckSection = DeckSection.smartValueOf(s.getKey());
            if (deckSection == null) {
                validatedSections.put(s.getKey(), s.getValue());
                continue;
            }

            final List<String> cardsInSection = s.getValue();
            List<Pair<String, Integer>> originalCardRequests = CardPool.processCardList(cardsInSection);
            CardPool pool = CardPool.fromCardList(cardsInSection);
            if (pool.countDistinct() == 0)
                continue;  // pool empty, no card has been found!

            // Filter pool by applying DeckSection Validation schema for Card Types (to avoid inconsistencies)
            CardPool filteredPool = pool.getFilteredPoolWithCardsCount(new Predicate<PaperCard>() {
                @Override
                public boolean apply(PaperCard input) {
                    return deckSection.validate(input);
                }
            });
            // Add all the cards from ValidPool anyway!
            List<String> whiteList = validatedSections.getOrDefault(s.getKey(), null);
            if (whiteList == null)
                whiteList = new ArrayList<>();
            for (Entry<PaperCard, Integer> entry : filteredPool) {
                String poolRequest = getPoolRequest(entry, originalCardRequests);
                whiteList.add(poolRequest);
            }
            validatedSections.put(s.getKey(), whiteList);

            if (filteredPool.countDistinct() != pool.countDistinct()) {
                CardPool blackList = pool.getFilteredPoolWithCardsCount(new Predicate<PaperCard>() {
                    @Override
                    public boolean apply(PaperCard input) {
                        return !(deckSection.validate(input));
                    }
                });

                for (Entry<PaperCard, Integer> entry : blackList) {
                    DeckSection cardSection = DeckSection.matchingSection(entry.getKey());
                    String poolRequest = getPoolRequest(entry, originalCardRequests);
                    List<String> sectionCardList = validatedSections.getOrDefault(cardSection.name(), null);
                    if (sectionCardList == null)
                        sectionCardList = new ArrayList<>();
                    sectionCardList.add(poolRequest);
                    validatedSections.put(cardSection.name(), sectionCardList);
                } // end for blacklist
            } // end if
        } // end main for on deferredSections

        // Overwrite deferredSections
        this.deferredSections = validatedSections;
    }

    private String getPoolRequest(Entry<PaperCard, Integer> entry, List<Pair<String, Integer>> originalCardRequests) {
        PaperCard card = entry.getKey();
        int amount = entry.getValue();
        String poolCardRequest = CardDb.CardRequest.compose(
                card.isFoil() ? CardDb.CardRequest.compose(card.getName(), true) : card.getName(),
                card.getEdition(), card.getArtIndex());
        String originalRequestCandidate = null;
        for (Pair<String, Integer> originalRequest : originalCardRequests){
            String cardRequest = originalRequest.getLeft();
            if (!StringUtils.startsWithIgnoreCase(poolCardRequest, cardRequest))
                continue;
            originalRequestCandidate = cardRequest;
            int cardAmount = originalRequest.getRight();
            if (amount == cardAmount)
                return String.format("%d %s", cardAmount, cardRequest);
        }
        // This is just in case, it should never happen as we're
        if (originalRequestCandidate != null)
            return String.format("%d %s", amount, originalRequestCandidate);
        return String.format("%d %s", amount, poolCardRequest);
    }

    private ArrayList<String> getAllCardNamesWithNoSpecifiedEdition(List<String> cardsInSection) {
        ArrayList<String> cardNamesWithNoEdition = new ArrayList<>();
        List<Pair<String, Integer>> cardRequests = CardPool.processCardList(cardsInSection);
        for (Pair<String, Integer> pair : cardRequests) {
            String requestString = pair.getLeft();
            CardDb.CardRequest request = CardDb.CardRequest.fromString(requestString);
            if (request.edition == null)
                cardNamesWithNoEdition.add(request.cardName);
        }
        return cardNamesWithNoEdition;
    }

    private void optimiseCardArtSelectionInDeckSections(Map<DeckSection, ArrayList<String>> cardsWithNoEdition) {
        StaticData data = StaticData.instance();
        // Get current Card Art Preference Settings
        boolean isCardArtPreferenceLatestArt = data.cardArtPreferenceIsLatest();
        boolean cardArtPreferenceHasFilter = data.isCoreExpansionOnlyFilterSet();

        for (Entry<DeckSection, CardPool> part : parts.entrySet()) {
            DeckSection deckSection = part.getKey();
            if (deckSection != DeckSection.Main && deckSection != DeckSection.Sideboard && deckSection != DeckSection.Commander)
                continue;

            // == 0. First Off, check if there is anything at all to do for the current section
            ArrayList<String> cardNamesWithNoEditionInSection = cardsWithNoEdition.getOrDefault(deckSection, null);
            if (cardNamesWithNoEditionInSection == null || cardNamesWithNoEditionInSection.size() == 0)
                continue; // nothing to do here

            CardPool pool = part.getValue();
            // Set options for the alternative card print search
            boolean isExpansionTheMajorityInThePool = (pool.getTheMostFrequentEditionType() == CardEdition.Type.EXPANSION);
            boolean isPoolModernFramed = pool.isModern();

            // == Get the most representative (Pivot) Edition in the Pool
            // Note: Card Art Updates (if any) will be determined based on the Pivot Edition.
            CardEdition pivotEdition = pool.getPivotCardEdition(isCardArtPreferenceLatestArt);
            if (pivotEdition == null)
                continue;

            // == Inspect and Update the Pool
            Date releaseDatePivotEdition = pivotEdition.getDate();
            CardPool newPool = new CardPool();
            for (Entry<PaperCard, Integer> cp : pool) {
                PaperCard card = cp.getKey();
                int totalToAddToPool = cp.getValue();
                // A. Skip cards not requiring any update, because they add the edition specified!
                if (!cardNamesWithNoEditionInSection.contains(card.getName())) {
                    addCardToPool(newPool, card, totalToAddToPool, card.isFoil());
                    continue;
                }
                // B. Determine if current card requires update
                boolean cardArtNeedsOptimisation = this.isCardArtUpdateRequired(card, releaseDatePivotEdition);
                if (!cardArtNeedsOptimisation) {
                    addCardToPool(newPool, card, totalToAddToPool, card.isFoil());
                    continue;
                }
                PaperCard alternativeCardPrint = data.getAlternativeCardPrint(card, releaseDatePivotEdition,
                                                                              isCardArtPreferenceLatestArt,
                                                                              cardArtPreferenceHasFilter,
                                                                              isExpansionTheMajorityInThePool,
                                                                              isPoolModernFramed);
                if (alternativeCardPrint == null)  // no alternative found, add original card in Pool
                    addCardToPool(newPool, card, totalToAddToPool, card.isFoil());
                else
                    addCardToPool(newPool, alternativeCardPrint, totalToAddToPool, card.isFoil());
            }
            parts.put(deckSection, newPool);
        }
    }

    private void addCardToPool(CardPool pool, PaperCard card, int totalToAdd, boolean isFoil) {
        StaticData data = StaticData.instance();
        if (card.getArtIndex() != IPaperCard.NO_ART_INDEX && card.getArtIndex() != IPaperCard.DEFAULT_ART_INDEX)
            pool.add(isFoil ? card.getFoiled() : card, totalToAdd);  // art index requested, keep that way!
        else {
            int artCount = data.getCardArtCount(card);
            if (artCount > 1)
                addAlternativeCardPrintInPoolWithMultipleArt(card, pool, totalToAdd, artCount);
            else
                pool.add(isFoil ? card.getFoiled() : card, totalToAdd);
        }
    }

    private void addAlternativeCardPrintInPoolWithMultipleArt(PaperCard alternativeCardPrint, CardPool pool,
                                                              int totalNrToAdd, int nrOfAvailableArts) {
        StaticData data = StaticData.instance();

        // distribute available card art
        String cardName = alternativeCardPrint.getName();
        String setCode = alternativeCardPrint.getEdition();
        boolean isFoil = alternativeCardPrint.isFoil();
        int cardsPerArtIndex = totalNrToAdd / nrOfAvailableArts;
        int restOfCardsToAdd = cardsPerArtIndex > 0 ? totalNrToAdd % nrOfAvailableArts : 0;
        cardsPerArtIndex = Math.max(1, cardsPerArtIndex);  // make sure is never zero
        int cardsAdded = 0;
        PaperCard alternativeCardArt = null;
        for (int artIndex = 1; artIndex <= nrOfAvailableArts; artIndex++) {
            alternativeCardArt = data.getOrLoadCommonCard(cardName, setCode, artIndex, isFoil);
            cardsAdded += cardsPerArtIndex;
            pool.add(alternativeCardArt, cardsPerArtIndex);
            if (cardsAdded == totalNrToAdd)
                break;
        }
        if (restOfCardsToAdd > 0)
            pool.add(alternativeCardArt, restOfCardsToAdd);
    }

    private boolean isCardArtUpdateRequired(PaperCard card, Date referenceReleaseDate) {
        /* A Card Art update is required ONLY IF the current edition of the card is either
        newer (older) than pivot edition when LATEST ART (ORIGINAL ART) Card Art Preference
        is selected.
        This is because what we're trying to "FIX" is the card art selection that is
        "too new" wrt. PivotEdition (or, "too old" with ORIGINAL ART Preference, respectively).
        Example:
        - Case 1: [Latest Art]
        We don't want Lands automatically selected from AFR (too new) within a Deck of mostly Core21 (Pivot)
        - Case 2: [Original Art]
        We don't want an Atog from LEA (too old) in a Deck of Mirrodin (Pivot)

        NOTE: the control implemented in release date also consider the case when the input PaperCard
        is exactly from the Pivot Edition. In this case, NO update will be required!
        */

        if (card.getRules().isVariant())
            return false;  // skip variant cards
        if (StaticData.instance().getCommonCards().hasPreferredArt(card.getName())) {
            // if there is any preferred art, never update it!
            CardDb.CardRequest request = CardDb.CardRequest.fromString(card.getName());
            if (request.edition.equals(card.getEdition()) && request.artIndex == card.getArtIndex())
                return false;
        }
        boolean isLatestCardArtPreference = StaticData.instance().cardArtPreferenceIsLatest();
        CardEdition cardEdition = StaticData.instance().getCardEdition(card.getEdition());
        if (cardEdition == null)  return false;
        Date releaseDate = cardEdition.getDate();
        if (releaseDate == null)  return false;
        if (isLatestCardArtPreference)  // Latest Art
            return releaseDate.compareTo(referenceReleaseDate) > 0;
        // Original Art
        return releaseDate.compareTo(referenceReleaseDate) < 0;
    }

    public static final Function<Deck, String> FN_NAME_SELECTOR = new Function<Deck, String>() {
        @Override
        public String apply(Deck arg1) {
            return arg1.getName();
        }
    };

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Entry<DeckSection, CardPool>> iterator() {
        loadDeferredSections();
        return parts.entrySet().iterator();
    }

    /**
     * @return the associated tags, a writable set
     */
    public Set<String> getTags() {
        return tags;
    }

    public CardPool getAllCardsInASinglePool() {
        return getAllCardsInASinglePool(true);
    }
    public CardPool getAllCardsInASinglePool(final boolean includeCommander) {
        final CardPool allCards = new CardPool(); // will count cards in this pool to enforce restricted
        allCards.addAll(this.getMain());
        if (this.has(DeckSection.Sideboard)) {
            allCards.addAll(this.get(DeckSection.Sideboard));
        }
        if (includeCommander && this.has(DeckSection.Commander)) {
            allCards.addAll(this.get(DeckSection.Commander));
        }
        // do not include schemes / avatars and any non-regular cards
        return allCards;
    }

    public UnplayableAICards getUnplayableAICards() {
        if (unplayableAI == null) {
            unplayableAI = new UnplayableAICards(this);
        }
        return unplayableAI;
    }

    public static final class UnplayableAICards {
        public final Map<DeckSection, List<? extends PaperCard>> unplayable = new HashMap<>();
        public final int inMainDeck;

        private UnplayableAICards(Deck myDeck) {
            int mainDeck = 0;
            for (Entry<DeckSection, CardPool> ds : myDeck) {
                List<PaperCard> result = Lists.newArrayList();
                for (Entry<PaperCard, Integer> cp : ds.getValue()) {
                    if (cp.getKey().getRules().getAiHints().getRemAIDecks()) {
                        result.add(cp.getKey());
                    }
                }
                if (ds.getKey().equals(DeckSection.Main)) {
                  mainDeck = result.size();
                }
                if (!result.isEmpty()) {
                    unplayable.put(ds.getKey(), result);
                }
            }
            inMainDeck = mainDeck;
        }
    }

    @Override
    public boolean isEmpty() {
        loadDeferredSections();
        for (CardPool part : parts.values()) {
            if (!part.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getImageKey(boolean altState) {
        return null;
    }

    @Override
    public Deck getHumanDeck() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object o) {
        if (o instanceof Deck) {
            final DeckBase dbase = (DeckBase) o;
            boolean deckBaseEquals = super.equals(dbase);
            if (!deckBaseEquals)
                return false;
            // ok so far we made sure they do have the same name. Now onto comparing parts
            final Deck d = (Deck) o;
            for (DeckSection deckSection : this.parts.keySet()) {
                CardPool otherPool = d.get(deckSection);
                CardPool thisPool = this.parts.get(deckSection);
                if (!thisPool.equals(otherPool))  // this also accounts for null from d.get
                    return false;
            }
            // if we reached this far, it means all sections in this.parts are identical to d.parts
            // now let's consider the other way around, as in any section in d not in parts.
            for (DeckSection deckSection: d.parts.keySet()){
                CardPool otherPool = d.get(deckSection);
                if (!this.parts.containsKey(deckSection) && otherPool.countAll() > 0)
                    return false;
            }
            return true;
        }
        return false;
    }

    public static int getAverageCMC(Deck deck) {
        int totalCMC = 0;
        int totalCount = 0;
        for (final Entry<DeckSection, CardPool> deckEntry : deck) {
            switch (deckEntry.getKey()) {
            case Main:
            case Commander:
                for (final Entry<PaperCard, Integer> poolEntry : deckEntry.getValue()) {
                    CardRules rules = poolEntry.getKey().getRules();
                    CardType type = rules.getType();
                    if (!type.isLand() && (type.isArtifact() || type.isCreature() || type.isEnchantment() || type.isPlaneswalker() || type.isInstant() || type.isSorcery())) {
                        totalCMC += rules.getManaCost().getCMC();
                        totalCount++;
                    }
                }
                break;
            default:
                break; //ignore other sections
            }
        }
        return totalCount == 0 ? 0 : Math.round(totalCMC / totalCount);
    }
}