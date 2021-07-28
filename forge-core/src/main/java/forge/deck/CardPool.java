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
import com.google.common.collect.Lists;
import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.item.IPaperCard;
import forge.item.PaperCard;
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

        PaperCard paperCard = StaticData.instance().getCommonCards().getCard(cardRequest);
        final boolean isCommonCard = paperCard != null;

        if (!isCommonCard) {
            paperCard = StaticData.instance().getCustomCards().getCard(cardRequest);

            if (paperCard == null) // not a custom card
                paperCard = StaticData.instance().getVariantCards().getCard(cardRequest);

            if (paperCard == null) {  // not even a variant then
                StaticData.instance().attemptToLoadCard(cardRequest);
                paperCard = StaticData.instance().getVariantCards().getCard(cardRequest);
            }
        }

        if (paperCard == null){
            System.err.print("An unsupported card was requested: \"" + cardRequest + "\". ");
            paperCard = StaticData.instance().getCommonCards().createUnsupportedCard(cardRequest);
        }
        this.add(paperCard, amount);

        // @leriomaggio: The following is a useless replication of the logic already in CardDb for CardRequest parsing!
        // This should be removed
        /*
        if (cardRequest.contains("|")) {
            // an encoded cardRequest with set and possibly art index was passed, split it and pass in full
            String[] splitCardName = StringUtils.split(cardRequest, "|");
            if (splitCardName.length == 2) {
                // set specified
                this.add(splitCardName[0], splitCardName[1], amount);
            } else if (splitCardName.length == 3) {
                // set and art specified
                this.add(splitCardName[0], splitCardName[1], Integer.parseInt(splitCardName[2]), amount);
            }
        } else {
            this.add(cardRequest, null, IPaperCard.DEFAULT_ART_INDEX, amount);
        } */
    }

    public void add(final String cardName, final String setCode) {
        this.add(cardName, setCode, IPaperCard.DEFAULT_ART_INDEX, 1);
    }

    public void add(final String cardName, final String setCode, final int amount) {
        this.add(cardName, setCode, IPaperCard.DEFAULT_ART_INDEX, amount);
    }

    // NOTE: ART indices are "1" -based
    public void add(String cardName, String setCode, final int artIndex, final int amount) {

        PaperCard paperCard = StaticData.instance().getCommonCards().getCard(cardName, setCode, artIndex);
        boolean isCommonCard = paperCard != null;
        boolean isCustomCard = !(isCommonCard);

        if (!isCommonCard) {
            paperCard = StaticData.instance().getCustomCards().getCard(cardName, setCode);
            isCustomCard = paperCard != null;

            if (!isCustomCard)
                paperCard = StaticData.instance().getVariantCards().getCard(cardName, setCode);

            if (paperCard == null) {
                // Attempt to load the card first
                StaticData.instance().attemptToLoadCard(cardName);
                // Now try again all the three available DBs
                // we simply don't know which db the card has been added to (in case)
                // so we will try all of them again in this second round of attempt.
                CardDb[] dbs = new CardDb[] {StaticData.instance().getCommonCards(),
                                             StaticData.instance().getCustomCards(),
                                             StaticData.instance().getVariantCards()};
                for (int i = 0; i < 3; i++) {
                    CardDb db = dbs[i];
                    paperCard = db.getCard(cardName, setCode);
                    if (paperCard != null){
                        if (i == 0)
                            isCommonCard = true;
                        else if (i == 1)
                            isCustomCard = true;
                        break;
                    }
                }
            }
        }

        // TODO: @leriomaggio from here on, this could be optimised!
        int artCount = IPaperCard.DEFAULT_ART_INDEX;
        if (paperCard != null) {
            setCode = paperCard.getEdition();
            cardName = paperCard.getName();
            CardDb cardDb = isCustomCard ? StaticData.instance().getCustomCards() : StaticData.instance().getCommonCards();
            artCount = cardDb.getArtCount(cardName, setCode);
        } else {
            System.err.println("An unsupported card was requested: \"" + cardName + "\" from \"" + setCode + "\". \n");
            paperCard = StaticData.instance().getCommonCards().createUnsupportedCard(cardName);
            // the unsupported card will be temporarily added to common cards (for no use, really)
            // this would happen only if custom cards has been found but option is disabled!
            isCustomCard = false;
        }

        boolean artIndexExplicitlySet = artIndex > IPaperCard.NO_ART_INDEX || Character.isDigit(cardName.charAt(cardName.length() - 1)) && cardName.charAt(cardName.length() - 2) == CardDb.NameSetSeparator;

        if (artIndexExplicitlySet || artCount <= 1) {
            // either a specific art index is specified, or there is only one art, so just add the card
            this.add(paperCard, amount);
        } else {
            // random art index specified, make sure we get different groups of cards with different art
            int[] artGroups = MyRandom.splitIntoRandomGroups(amount, artCount);
            CardDb cardDb = isCustomCard ? StaticData.instance().getCustomCards() : StaticData.instance().getCommonCards();
            for (int i = 1; i <= artGroups.length; i++) {
                int cnt = artGroups[i - 1];
                if (cnt <= 0) {
                    continue;
                }
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
     * An HashMap mapping each CardEdition to its corresponding frequency count
     */
    public Map<CardEdition, Integer> getCardEditionFrequencyMap(boolean includeBasicLands) {
        Map<CardEdition, Integer> editionStatistics = new HashMap<>();
        for(Entry<PaperCard, Integer> cp : this.items.entrySet()) {
            PaperCard card = cp.getKey();
            // Check whether or not including basic land in stats count
            if (card.getRules().getType().isBasicLand() && !includeBasicLands)
                continue;
            int count = cp.getValue();
            CardEdition edition = StaticData.instance().getCardEdition(card.getEdition());
            int currentCount = editionStatistics.getOrDefault(edition, 0);
            currentCount += count;
            editionStatistics.put(edition, currentCount);
        }
        return editionStatistics;
    }

    /** Determines the Pivot Edition for cards in the Pool, according to default
     * Card Art Preference settings (@see forge.card.CardDb.CardArtPreference).
     *
     * For a more thorough explanation of Pivot Edition, please
     * @see CardPool#getPivotEdition(boolean)
     *
     * @return CardEdition representing the reference edition for the card pool.
     */
    public CardEdition getPivotEdition(){
        boolean isLatestCardArtPreference = StaticData.instance().cardArtPreferenceIsLatest();
        return getPivotEdition(isLatestCardArtPreference);
    }

    /**
     * Determines the Pivot Edition for cards in the Pool.
     * <p>
     * The Pivot Edition refers to <b>the Reference Edition</b> considered as the
     * <i>most representative</i> for cards in the pool.
     * The Pivot Edition simply corresponds to the edition having the majority of cards
     * in the pool.
     * <p>
     * Whenever this Edition cannot be immediately determined
     * (i.e. <i>the <code>max</code> is not unique</i>), the Pivot Edition will be chosen
     * according to the specified set-preference criterion.
     * <p>
     * In other words:
     * <ul>
     *     <li><code>isLatestCardArtPreference=true</code>:</li> Pivot Edition will be
     *     the earliest among the most recent editions (lower bound).
     *     <li><code>isLatestCardArtPreference=false</code>:</li> Pivot Edition will be
     *     the latest among the earliest editions (upper bound).
     * </ul>
     * <p>
     * Note: Cards in the Pool may have been generated according to the specified CardArtPreference
     * but we might be interested in "forcing" a specific selection criterion.
     *
     * @param isLatestCardArtPreference if true, precedence will be given to most recent editions
     *                                  (Latest Card Art)
     * @return CardEdition instance representing the Pivot Edition
     */
    public CardEdition getPivotEdition(boolean isLatestCardArtPreference) {
        CardEdition pivotEdition = null;
        int maxCardOccurrence = 0;
        Map<CardEdition, Integer> editionsStatistics = this.getCardEditionFrequencyMap(false);

        for(Entry<CardEdition, Integer> entry : editionsStatistics.entrySet()) {
            Integer cardCount = entry.getValue();
            CardEdition ed = entry.getKey();
            if (cardCount < maxCardOccurrence)
                continue;

            if (pivotEdition == null || cardCount > maxCardOccurrence){
                maxCardOccurrence = cardCount;
                pivotEdition = ed;
            }
            else {  //  i.e. cardCount == maxCardOccurrence
                if (isLatestCardArtPreference){
                    // update only if older
                    if (ed.getDate().compareTo(pivotEdition.getDate()) < 0)
                        pivotEdition = ed;
                } else {
                    // update only if newer
                    if (ed.getDate().compareTo(pivotEdition.getDate()) > 0)
                        pivotEdition = ed;
                }
            }
        }
        return pivotEdition;
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
        for (PaperCard pc : this.items.keySet()) {
            if (predicate.apply(pc)) filteredPool.add(pc);
        }
        return filteredPool;
    }
}
