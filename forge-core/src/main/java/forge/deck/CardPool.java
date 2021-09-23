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

import com.google.common.base.Predicate;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.util.CollectionSuppliers;
import forge.util.ItemPool;
import forge.util.ItemPoolSorter;
import forge.util.MyRandom;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CardPool extends ItemPool<PaperCard> {
    private static final long serialVersionUID = -5379091255613968393L;

    public CardPool() {
        super(PaperCard.class);
    }

    public CardPool(final Iterable<Entry<PaperCard, Integer>> cards) {
        this();
        this.addAll(cards);
    }

    public void add(final String cardRequest, final int amount) {
        CardDb.CardRequest request = CardDb.CardRequest.fromString(cardRequest);
        this.add(request.cardName, request.edition, request.artIndex, amount);
    }

    public void add(final String cardName, final String setCode) {
        this.add(cardName, setCode, IPaperCard.DEFAULT_ART_INDEX, 1);
    }

    public void add(final String cardName, final String setCode, final int amount) {
        this.add(cardName, setCode, IPaperCard.DEFAULT_ART_INDEX, amount);
    }

    public void add(final String cardName, final String setCode, final int amount, boolean addAny) {
        this.add(cardName, setCode, IPaperCard.NO_ART_INDEX, amount, addAny);
    }

    // NOTE: ART indices are "1" -based
    public void add(String cardName, String setCode, int artIndex, final int amount) {
        this.add(cardName, setCode, artIndex, amount, false);
    }
    public void add(String cardName, String setCode, int artIndex, final int amount, boolean addAny) {
        Map<String, CardDb> dbs = StaticData.instance().getAvailableDatabases();
        PaperCard paperCard = null;
        String selectedDbName = "";
        artIndex = Math.max(artIndex, IPaperCard.DEFAULT_ART_INDEX);
        int loadAttempt = 0;
        while (paperCard == null && loadAttempt < 2) {
            for (Map.Entry<String, CardDb> entry: dbs.entrySet()){
                String dbName = entry.getKey();
                CardDb db = entry.getValue();
                paperCard = db.getCard(cardName, setCode, artIndex);
                if (paperCard != null) {
                    selectedDbName = dbName;
                    break;
                }
            }
            loadAttempt += 1;
            if (paperCard == null && loadAttempt < 2) {
                /* Attempt to load the card first, and then try again all the three available DBs
                 as we simply don't know which db the card has been added to (in case). */
                StaticData.instance().attemptToLoadCard(cardName, setCode);
                artIndex = IPaperCard.DEFAULT_ART_INDEX;  // Reset Any artIndex passed in, at this point
            }
        }
        if (addAny && paperCard == null) {
            paperCard = StaticData.instance().getCommonCards().getCard(cardName);
            selectedDbName = "Common";
        }
        if (paperCard == null){
            // after all still null
            System.err.println("An unsupported card was requested: \"" + cardName + "\" from \"" + setCode + "\". \n");
            paperCard = StaticData.instance().getCommonCards().createUnsupportedCard(cardName);
            selectedDbName = "Common";
        }
        CardDb cardDb = dbs.getOrDefault(selectedDbName, StaticData.instance().getCommonCards());
        // Determine Art Index
        setCode = paperCard.getEdition();
        cardName = paperCard.getName();
        int artCount = cardDb.getArtCount(cardName, setCode);
        boolean artIndexExplicitlySet = (artIndex > IPaperCard.DEFAULT_ART_INDEX) ||
                (CardDb.CardRequest.fromString(cardName).artIndex > IPaperCard.NO_ART_INDEX);

        if ((artIndexExplicitlySet || artCount == 1) && !addAny) {
            // either a specific art index is specified, or there is only one art, so just add the card
            this.add(paperCard, amount);
        } else {
            // random art index specified, make sure we get different groups of cards with different art
            int[] artGroups = MyRandom.splitIntoRandomGroups(amount, artCount);
            for (int i = 1; i <= artGroups.length; i++) {
                int cnt = artGroups[i - 1];
                if (cnt <= 0)
                    continue;
                PaperCard randomCard = cardDb.getCard(cardName, setCode, i);
                this.add(randomCard, cnt);
            }
        }
    }


    /**
     * Add all from a List of CardPrinted.
     *
     * @param list CardPrinteds to add
     */
    public void add(final Iterable<PaperCard> list) {
        for (PaperCard cp : list) {
            this.add(cp);
        }
    }

    /**
     * returns n-th card from this DeckSection. LINEAR time. No fixed order between changes
     *
     * @param n
     * @return
     */
    public PaperCard get(int n) {
        for (Entry<PaperCard, Integer> e : this) {
            n -= e.getValue();
            if (n <= 0) return e.getKey();
        }
        return null;
    }

    public int countByName(String cardName, boolean isCommonCard) {
        PaperCard pc = isCommonCard
                ? StaticData.instance().getCommonCards().getCard(cardName)
                : StaticData.instance().getVariantCards().getCard(cardName);

        return this.count(pc);
    }

    /**
     * Get the Map of frequencies (i.e. counts) for all the CardEdition found
     * among cards in the Pool.
     *
     * @param includeBasicLands determines whether or not basic lands should be counted in or
     *                          not when gathering statistics
     * @return Map<CardEdition, Integer>
     * An HashMap structure mapping each CardEdition in Pool to its corresponding frequency count
     */
    public Map<CardEdition, Integer> getCardEditionStatistics(boolean includeBasicLands) {
        Map<CardEdition, Integer> editionStatistics = new HashMap<>();
        for(Entry<PaperCard, Integer> cp : this.items.entrySet()) {
            PaperCard card = cp.getKey();
            // Check whether or not including basic land in stats count
            if (card.getRules().getType().isBasicLand() && !includeBasicLands)
                continue;
            int count = cp.getValue();
            CardEdition edition = StaticData.instance().getCardEdition(card.getEdition());
            if (edition == null)
                continue;
            int currentCount = editionStatistics.getOrDefault(edition, 0);
            currentCount += count;
            editionStatistics.put(edition, currentCount);
        }
        return editionStatistics;
    }

    /**
     * Returns the map of card frequency indexed by frequency value, rather than single card edition.
     * Therefore, all editions with the same card count frequency will be grouped together.
     *
     * Note: This method returns the reverse map generated by <code>getCardEditionStatistics</code>
     *
     * @param includeBasicLands Decide to include or not basic lands in gathered statistics
     *
     * @return a ListMultimap structure matching each unique frequency value to its corresponding list
     * of CardEditions
     *
     * @see CardPool#getCardEditionStatistics(boolean)
     */
    public ListMultimap<Integer, CardEdition> getCardEditionsGroupedByNumberOfCards(boolean includeBasicLands){
        Map<CardEdition, Integer> editionsFrequencyMap = this.getCardEditionStatistics(includeBasicLands);
        ListMultimap<Integer, CardEdition> reverseMap = Multimaps.newListMultimap(new HashMap<>(), CollectionSuppliers.arrayLists());
        for (Map.Entry<CardEdition, Integer> entry : editionsFrequencyMap.entrySet())
            reverseMap.put(entry.getValue(), entry.getKey());
        return reverseMap;
    }

    /**
     * Gather Statistics per Edition Type from cards included in the CardPool.
     *
     * @param includeBasicLands  Determine whether or not basic lands should be included in gathered statistics
     *
     * @return an HashMap structure mapping each <code>CardEdition.Type</code> found among
     * cards in the Pool, and their corresponding (card) count.
     *
     * @see CardPool#getCardEditionStatistics(boolean)
     */
    public Map<CardEdition.Type, Integer> getCardEditionTypeStatistics(boolean includeBasicLands){
        Map<CardEdition.Type, Integer> editionTypeStats = new HashMap<>();
        Map<CardEdition, Integer> editionStatistics = this.getCardEditionStatistics(includeBasicLands);
        for(Entry<CardEdition, Integer> entry : editionStatistics.entrySet()) {
            CardEdition edition = entry.getKey();
            int count = entry.getValue();
            CardEdition.Type key = edition.getType();
            int currentCount = editionTypeStats.getOrDefault(key, 0);
            currentCount += count;
            editionTypeStats.put(key, currentCount);
        }
        return editionTypeStats;
    }

    /**
     * Returns the <code>CardEdition.Type</code> that is the most frequent among cards' editions
     * in the pool. In case of more than one candidate, Expansion Type will be preferred (if available).
     *
     * @return The most frequent CardEdition.Type in the pool, or null if the Pool is empty
     */
    public CardEdition.Type getTheMostFrequentEditionType(){
        Map<CardEdition.Type, Integer> editionTypeStats = this.getCardEditionTypeStatistics(false);
        Integer mostFrequentType = 0;
        List<CardEdition.Type> mostFrequentEditionTypes = new ArrayList<>();
        for (Map.Entry<CardEdition.Type, Integer> entry : editionTypeStats.entrySet()){
            if (entry.getValue() > mostFrequentType) {
                mostFrequentType = entry.getValue();
                mostFrequentEditionTypes.add(entry.getKey());
            }
        }
        if (mostFrequentEditionTypes.isEmpty())
            return null;
        CardEdition.Type mostFrequentEditionType = mostFrequentEditionTypes.get(0);
        for (int i=1; i < mostFrequentEditionTypes.size(); i++){
            CardEdition.Type frequentType = mostFrequentEditionTypes.get(i);
            if (frequentType == CardEdition.Type.EXPANSION)
                return frequentType;
        }
        return mostFrequentEditionType;
    }

    /**
     * Determines whether (the majority of the) cards in the Pool are modern framed
     * (that is, cards are from Modern Card Edition).
     *
     * @return True if the majority of cards in Pool are from Modern Edition, false otherwise.
     * If the count of Modern and PreModern cards is tied, the return value is determined
     * by the preferred Card Art Preference settings, namely True if Latest Art, False otherwise.
     */
    public boolean isModern(){
        int modernEditionsCount = 0;
        int preModernEditionsCount = 0;
        Map<CardEdition, Integer> editionStats = this.getCardEditionStatistics(false);
        for (Map.Entry<CardEdition, Integer> entry: editionStats.entrySet()){
            CardEdition edition = entry.getKey();
            if (edition.isModern())
                modernEditionsCount += entry.getValue();
            else
                preModernEditionsCount += entry.getValue();
        }
        if (modernEditionsCount == preModernEditionsCount)
            return StaticData.instance().cardArtPreferenceIsLatest();
        return modernEditionsCount > preModernEditionsCount;
    }

    /**
     * Determines the Pivot Edition for cards in the Pool.
     * <p>
     * The Pivot Edition refers to the <code>CardEdition</code> for cards in the pool that sets the
     * <i>reference boundary</i> for cards in the pool.
     * Therefore, the <i>Pivot Edition</i> will be selected considering the per-edition distribution of
     * cards in the Pool.
     * If the majority of the cards in the pool corresponds to a single edition, this edition will be the Pivot.
     * The majority exists if the highest card frequency accounts for at least a third of the whole Pool
     * (i.e. 1 over 3 cards - not including basic lands).
     * <p>
     * However, there are cases in which cards in a Pool are gathered from several editions, so that there is
     * no clear winner for a single edition of reference.
     * In these cases, the Pivot will be selected as the "Median Edition", that is the edition whose frequency
     * is the closest to the average.
     * <p>
     * In cases where multiple candidates could be selected (most likely to occur when the average frequency
     * is considered) pivot candidates will be first sorted in ascending (earliest edition first) or
     * descending (latest edition first) order depending on whether or not the selected Card Art Preference policy
     * and the majority of cards in the Pool are compliant. This is to give preference more likely to
     * the best candidate for alternative card art print search.
     *
     * @param isLatestCardArtPreference Determines whether the Card Art Preference to consider should
     *                                  prefer or not Latest Card Art Editions first.
     * @return CardEdition instance representing the Pivot Edition
     *
     * @see #isModern()
     */
    public CardEdition getPivotCardEdition(boolean isLatestCardArtPreference) {
        ListMultimap<Integer, CardEdition> editionsStatistics = this.getCardEditionsGroupedByNumberOfCards(false);
        List<Integer> frequencyValues = new ArrayList<>(editionsStatistics.keySet());
        // Sort in descending order
        frequencyValues.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer f1, Integer f2) {
                return (f1.compareTo(f2)) * -1;
            }
        });
        float weightedMean = 0;
        int sumWeights = 0;
        for (Integer freq : frequencyValues) {
            int editionsCount = editionsStatistics.get(freq).size();
            int weightedFrequency = freq * editionsCount;
            sumWeights += editionsCount;
            weightedMean += weightedFrequency;
        }
        int totalNoCards = (int)weightedMean;
        weightedMean /= sumWeights;

        int topFrequency = frequencyValues.get(0);
        float ratio = ((float) topFrequency) / totalNoCards;
        // determine the Pivot Frequency
        int pivotFrequency;
        if (ratio >= 0.33)  // 1 over 3 cards are from the most frequent edition(s)
            pivotFrequency = topFrequency;
        else
            pivotFrequency = getMedianFrequency(frequencyValues, weightedMean);

        // Now Get editions corresponding to pivot frequency
        List<CardEdition> pivotCandidates = new ArrayList<>(editionsStatistics.get(pivotFrequency));
        // Now Sort candidates chronologically
        pivotCandidates.sort(new Comparator<CardEdition>() {
            @Override
            public int compare(CardEdition ed1, CardEdition ed2) {
                return ed1.compareTo(ed2);
            }
        });
        boolean searchPolicyAndPoolAreCompliant = isLatestCardArtPreference == this.isModern();
        if (!searchPolicyAndPoolAreCompliant)
            Collections.reverse(pivotCandidates);  // reverse to have latest-first.
        return pivotCandidates.get(0);
    }

    /* Utility (static) method to return the median value given a target mean.  */
    private static int getMedianFrequency(List<Integer> frequencyValues, float meanFrequency) {
        int medianFrequency = frequencyValues.get(0);
        float refDelta = Math.abs(meanFrequency - medianFrequency);
        for (int i = 1; i < frequencyValues.size(); i++){
            int currentFrequency = frequencyValues.get(i);
            float delta = Math.abs(meanFrequency - currentFrequency);
            if (delta < refDelta) {
                medianFrequency = currentFrequency;
                refDelta = delta;
            }
        }
        return medianFrequency;
    }

    @Override
    public String toString() {
        if (this.isEmpty()) {
            return "[]";
        }

        boolean isFirst = true;
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (Entry<PaperCard, Integer> e : this) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(", ");
            }
            sb.append(e.getValue()).append(" x ").append(e.getKey().getName());
        }
        return sb.append(']').toString();
    }

    private final static Pattern p = Pattern.compile("((\\d+)\\s+)?(.*?)");

    public static CardPool fromCardList(final Iterable<String> lines) {
        CardPool pool = new CardPool();
        if (lines == null) {
            return pool;
        }
        List<Pair<String, Integer>> cardRequests = processCardList(lines);
        for (Pair<String, Integer> pair : cardRequests) {
            String cardRequest = pair.getLeft();
            int count = pair.getRight();
            pool.add(cardRequest, count);
        }
        return pool;
    }

    public static List<Pair<String, Integer>> processCardList(final Iterable<String> lines){
        List<Pair<String, Integer>> cardRequests = new ArrayList<>();
        if (lines == null)
            return cardRequests;  // empty list

        for (String line : lines) {
            if (line.startsWith(";") || line.startsWith("#")) {
                continue;
            } // that is a comment or not-yet-supported card

            final Matcher m = p.matcher(line.trim());
            boolean matches = m.matches();
            if (!matches)
                continue;
            final String sCnt = m.group(2);
            final String cardRequest = m.group(3);
            if (StringUtils.isBlank(cardRequest))
                continue;
            final int count = sCnt == null ? 1 : Integer.parseInt(sCnt);
            cardRequests.add(Pair.of(cardRequest, count));
        }
        return cardRequests;
    }

    public String toCardList(String separator) {
        List<Entry<PaperCard, Integer>> main2sort = Lists.newArrayList(this);
        Collections.sort(main2sort, ItemPoolSorter.BY_NAME_THEN_SET);
        final CardDb commonDb = StaticData.instance().getCommonCards();
        StringBuilder sb = new StringBuilder();

        boolean isFirst = true;

        for (final Entry<PaperCard, Integer> e : main2sort) {
            if (!isFirst)
                sb.append(separator);
            else
                isFirst = false;

            CardDb db = !e.getKey().getRules().isVariant() ? commonDb : StaticData.instance().getVariantCards();
            sb.append(e.getValue()).append(" ");
            db.appendCardToStringBuilder(e.getKey(), sb);

        }
        return sb.toString();
    }

    /**
     * Applies a predicate to this CardPool's cards.
     *
     * @param predicate the Predicate to apply to this CardPool
     * @return a new CardPool made from this CardPool with only the cards that agree with the provided Predicate
     */
    public CardPool getFilteredPool(Predicate<PaperCard> predicate) {
        CardPool filteredPool = new CardPool();
        Iterator<PaperCard> cardsInPool = this.items.keySet().iterator();
        while (cardsInPool.hasNext()){
            PaperCard c = cardsInPool.next();
            if (predicate.apply(c))
                filteredPool.add(c, this.items.get(c));
        }
        return filteredPool;
    }
}
