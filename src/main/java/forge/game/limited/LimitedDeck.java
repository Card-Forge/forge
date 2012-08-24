package forge.game.limited;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import forge.Constant;
import forge.card.CardColor;
import forge.card.CardManaCost;
import forge.card.CardRules;
import forge.card.DeckHints;
import forge.card.mana.ManaCostShard;
import forge.deck.Deck;
import forge.deck.generate.GenerateDeckUtil;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.util.MyRandom;
import forge.util.closures.Predicate;

/**
 * Limited format deck.
 * 
 */
public class LimitedDeck {

    private int numSpellsNeeded = 22;
    private int landsNeeded = 18;
    private CardColor colors;
    private DeckColors deckColors;
    private List<CardPrinted> availableList;
    private List<CardPrinted> aiPlayables;
    private List<CardPrinted> deckList = new ArrayList<CardPrinted>();
    private List<String> setsWithBasicLands = new ArrayList<String>();
    private static ReadDraftRankings draftRankings = new ReadDraftRankings();

    /**
     * 
     * Constructor.
     * 
     * @param dList
     *            Cards to build the deck from.
     * @param pClrs
     *            Chosen colors.
     */
    public LimitedDeck(List<CardPrinted> dList, DeckColors pClrs) {
        this.availableList = dList;
        this.deckColors = pClrs;
        this.colors = pClrs.getCardColors();
        removeUnplayables();
        findBasicLandSets();
    }

    /**
     * Constructor.
     * 
     * @param list
     *            Cards to build the deck from.
     */
    public LimitedDeck(List<CardPrinted> list) {
        this.availableList = list;
        this.deckColors = new DeckColors();
        removeUnplayables();
        findBasicLandSets();
    }

    /**
     * <p>
     * buildDeck.
     * </p>
     * 
     * @return the new Deck.
     */
    public Deck buildDeck() {
        // 0. Add any planeswalkers
        Predicate<CardRules> hasColor = Predicate.or(new GenerateDeckUtil.ContainsAllColorsFrom(colors),
                GenerateDeckUtil.COLORLESS_CARDS);
        List<CardPrinted> colorList = hasColor.select(aiPlayables, CardPrinted.FN_GET_RULES);
        List<CardPrinted> walkers = CardRules.Predicates.Presets.IS_PLANESWALKER.select(colorList,
                CardPrinted.FN_GET_RULES);
        deckList.addAll(walkers);
        aiPlayables.removeAll(walkers);
        if (walkers.size() > 0) {
            System.out.println("Planeswalker: " + walkers.get(0).getName());
        }

        // 1. Add creatures, trying to follow mana curve
        List<CardPrinted> creatures = CardRules.Predicates.Presets.IS_CREATURE.select(aiPlayables,
                CardPrinted.FN_GET_RULES);
        List<CardPrinted> onColorCreatures = hasColor.select(creatures, CardPrinted.FN_GET_RULES);
        addManaCurveCreatures(rankCards(onColorCreatures), 15);

        // 2.Try to fill up to 22 with on-color non-creature cards
        List<CardPrinted> nonCreatures = CardRules.Predicates.Presets.IS_NON_CREATURE_SPELL.select(aiPlayables,
                CardPrinted.FN_GET_RULES);
        List<CardPrinted> onColorNonCreatures = hasColor.select(nonCreatures, CardPrinted.FN_GET_RULES);
        addNonCreatures(rankCards(onColorNonCreatures), numSpellsNeeded - deckList.size());

        // 3.If we couldn't get up to 22, try to fill up to 22 with on-color
        // creature cards
        creatures = CardRules.Predicates.Presets.IS_CREATURE.select(aiPlayables, CardPrinted.FN_GET_RULES);
        onColorCreatures = hasColor.select(creatures, CardPrinted.FN_GET_RULES);
        addCreatures(rankCards(onColorCreatures), numSpellsNeeded - deckList.size());

        // 4. If there are still on-color cards and the average cmc is low add a
        // 23rd card.
        List<CardPrinted> nonLands = hasColor.select(aiPlayables, CardPrinted.FN_GET_RULES);
        if (deckList.size() == numSpellsNeeded && getAverageCMC(deckList) < 3 && !nonLands.isEmpty()) {
            List<CardRankingBean> list = rankCards(nonLands);
            CardPrinted c = list.get(0).getCardPrinted();
            deckList.add(c);
            getAiPlayables().remove(c);
            landsNeeded--;
        }

        // 5. If there are still less than 22 non-land cards add off-color
        // cards.
        addRandomCards(numSpellsNeeded - deckList.size());

        // 6. If it's not a mono color deck, add non-basic lands that were
        // drafted.
        // Commenting out because it adds worthless lands (e.g., a R/B land
        // to a U/W deck).
        // addNonBasicLands();

        // 7. Fill up with basic lands.
        final CCnt[] clrCnts = calculateLandNeeds();
        if (landsNeeded > 0) {
            addLands(clrCnts);
        }

        fixDeckSize(clrCnts);

        if (deckList.size() == 40) {
            Deck result = new Deck(generateName());
            result.getMain().add(deckList);
            result.getSideboard().add(aiPlayables);
            result.getSideboard().add(availableList);
            if (Constant.Runtime.DEV_MODE[0]) {
                debugFinalDeck();
            }
            return result;
        } else {
            throw new RuntimeException("BoosterDraftAI : buildDeck() error, decksize not 40");
        }
    }

    /**
     * Generate a descriptive name.
     * 
     * @return name
     */
    private String generateName() {
        return deckColors.toString();
    }

    /**
     * Print out listing of all cards for debugging.
     */
    private void debugFinalDeck() {
        int i = 0;
        System.out.println("DECK");
        for (CardPrinted c : deckList) {
            i++;
            System.out.println(i + ". " + c.toString() + ": " + c.getCard().getManaCost().toString());
        }
        i = 0;
        System.out.println("NOT PLAYABLE");
        for (CardPrinted c : availableList) {
            i++;
            System.out.println(i + ". " + c.toString() + ": " + c.getCard().getManaCost().toString());
        }
        i = 0;
        System.out.println("NOT PICKED");
        for (CardPrinted c : getAiPlayables()) {
            i++;
            System.out.println(i + ". " + c.toString() + ": " + c.getCard().getManaCost().toString());
        }
    }

    /**
     * If the deck does not have 40 cards, fix it. This method should not be
     * called if the stuff above it is working correctly.
     * 
     * @param clrCnts
     *            color counts needed
     */
    private void fixDeckSize(final CCnt[] clrCnts) {
        while (deckList.size() > 40) {
            System.out.println("WARNING: Fixing deck size, currently " + deckList.size() + " cards.");
            final CardPrinted c = deckList.get(MyRandom.getRandom().nextInt(deckList.size() - 1));
            deckList.remove(c);
            getAiPlayables().add(c);
            System.out.println(" - Removed " + c.getName() + " randomly.");
        }

        while (deckList.size() < 40) {
            System.out.println("WARNING: Fixing deck size, currently " + deckList.size() + " cards.");
            if (getAiPlayables().size() > 1) {
                final CardPrinted c = getAiPlayables().get(MyRandom.getRandom().nextInt(getAiPlayables().size() - 1));
                deckList.add(c);
                getAiPlayables().remove(c);
                System.out.println(" - Added " + c.getName() + " randomly.");
            } else if (getAiPlayables().size() == 1) {
                final CardPrinted c = getAiPlayables().get(0);
                deckList.add(c);
                getAiPlayables().remove(c);
                System.out.println(" - Added " + c.getName() + " randomly.");
            } else {
                // if no playable cards remain fill up with basic lands
                for (int i = 0; i < 5; i++) {
                    if (clrCnts[i].getCount() > 0) {
                        final CardPrinted cp = getBasicLand(clrCnts[i].getColor());
                        deckList.add(cp);
                        System.out.println(" - Added " + cp.getName() + " as last resort.");
                        break;
                    }
                }
            }
        }
    }

    /**
     * Remove AI unplayable cards.
     */
    protected void removeUnplayables() {
        setAiPlayables(CardRules.Predicates.IS_KEPT_IN_AI_DECKS.select(availableList, CardPrinted.FN_GET_RULES));
        availableList.removeAll(getAiPlayables());
    }

    /**
     * Find the sets that have basic lands for the available cards.
     */
    private void findBasicLandSets() {
        Set<String> sets = new HashSet<String>();
        for (CardPrinted cp : aiPlayables) {
            if (CardDb.instance().isCardSupported("Plains", cp.getEdition())) {
                sets.add(cp.getEdition());
            }
        }
        setsWithBasicLands.addAll(sets);
        if (setsWithBasicLands.isEmpty()) {
            setsWithBasicLands.add("M13");
        }
    }

    /**
     * Add lands to fulfill the given color counts.
     * 
     * @param clrCnts
     */
    private void addLands(final CCnt[] clrCnts) {

        // total of all ClrCnts
        int totalColor = 0;
        for (int i = 0; i < 5; i++) {
            totalColor += clrCnts[i].getCount();
        }

        // do not update landsNeeded until after the loop, because the
        // calculation involves landsNeeded
        int landsAdded = 0;
        for (int i = 0; i < 5; i++) {
            if (clrCnts[i].getCount() > 0) {
                // calculate number of lands for each color
                final float p = (float) clrCnts[i].getCount() / (float) totalColor;
                final int nLand = (int) (landsNeeded * p); // desired truncation
                                                           // to int
                if (Constant.Runtime.DEV_MODE[0]) {
                    System.out.println("Basics[" + clrCnts[i].getColor() + "]: " + clrCnts[i].getCount() + "/"
                            + totalColor + " = " + p + " = " + nLand);
                }

                for (int j = 0; j < nLand; j++) {
                    deckList.add(getBasicLand(clrCnts[i].getColor()));
                    landsAdded++;
                }
            }
        }

        // Add extra lands to get up to the right number.
        // Start with the smallest CCnt to "even out" a little.
        landsNeeded -= landsAdded;
        Arrays.sort(clrCnts);
        int n = 0;
        while (landsNeeded > 0) {
            if (clrCnts[n].getCount() > 0) {
                final CardPrinted cp = getBasicLand(clrCnts[n].getColor());
                deckList.add(cp);
                landsNeeded--;

                if (Constant.Runtime.DEV_MODE[0]) {
                    System.out.println("AddBasics: " + cp.getName());
                }
            }
            if (++n > 4) {
                n = 0;
            }
        }
    }

    /**
     * Get basic land.
     * 
     * @param basicLand
     * @return card
     */
    private CardPrinted getBasicLand(String basicLand) {
        String set;
        if (setsWithBasicLands.size() > 1) {
            set = setsWithBasicLands.get(MyRandom.getRandom().nextInt(setsWithBasicLands.size() - 1));
        } else {
            set = setsWithBasicLands.get(0);
        }
        return CardDb.instance().getCard(basicLand, set);
    }

    /**
     * attempt to optimize basic land counts according to color representation.
     * 
     * @return CCnt
     */
    private CCnt[] calculateLandNeeds() {
        final CCnt[] clrCnts = { new CCnt("Plains", 0), new CCnt("Island", 0), new CCnt("Swamp", 0),
                new CCnt("Mountain", 0), new CCnt("Forest", 0) };

        // count each card color using mana costs
        for (int i = 0; i < deckList.size(); i++) {
            final CardManaCost mc = deckList.get(i).getCard().getManaCost();

            // count each mana symbol in the mana cost
            for (ManaCostShard shard : mc.getShards()) {
                byte mask = shard.getColorMask();

                if ((mask & CardColor.WHITE) > 0) {
                    clrCnts[0].setCount(clrCnts[0].getCount() + 1);
                }
                if ((mask & CardColor.BLUE) > 0) {
                    clrCnts[1].setCount(clrCnts[1].getCount() + 1);
                }
                if ((mask & CardColor.BLACK) > 0) {
                    clrCnts[2].setCount(clrCnts[2].getCount() + 1);
                }
                if ((mask & CardColor.RED) > 0) {
                    clrCnts[3].setCount(clrCnts[3].getCount() + 1);
                }
                if ((mask & CardColor.GREEN) > 0) {
                    clrCnts[4].setCount(clrCnts[4].getCount() + 1);
                }
            }
        }
        return clrCnts;
    }

    /**
     * Add non-basic lands to the deck.
     */
    // private void addNonBasicLands() {
    // CardList lands = getAiPlayables().getType("Land");
    // while (!getColors().getColor1().equals(getColors().getColor2()) &&
    // landsNeeded > 0 && lands.size() > 0) {
    // final Card c = lands.get(0);
    //
    // deckList.add(c);
    // landsNeeded--;
    // getAiPlayables().remove(c);
    //
    // lands = getAiPlayables().getType("Land");
    //
    // if (Constant.Runtime.DEV_MODE[0]) {
    // System.out.println("Land:" + c.getName());
    // }
    // }
    // }

    /**
     * Add random cards to the deck.
     * 
     * @param nCards
     */
    private void addRandomCards(int nCards) {
        List<CardPrinted> others = CardRules.Predicates.Presets.IS_NON_LAND.select(aiPlayables,
                CardPrinted.FN_GET_RULES);
        List<CardRankingBean> ranked = rankCards(others);
        for (CardRankingBean bean : ranked) {
            if (nCards > 0) {
                deckList.add(bean.getCardPrinted());
                aiPlayables.remove(bean.getCardPrinted());
                nCards--;
                if (Constant.Runtime.DEV_MODE[0]) {
                    System.out.println("Random[" + nCards + "]:" + bean.getCardPrinted().getName() + "("
                            + bean.getCardPrinted().getCard().getManaCost() + ")");
                }
            } else {
                break;
            }
        }
    }

    /**
     * Add highest ranked non-creatures to the deck.
     * 
     * @param nonCreatures
     *            cards to choose from
     * @param num
     */
    private void addNonCreatures(List<CardRankingBean> nonCreatures, int num) {
        for (CardRankingBean bean : nonCreatures) {
            if (num > 0) {
                CardPrinted cardToAdd = bean.getCardPrinted();
                deckList.add(cardToAdd);
                num--;
                getAiPlayables().remove(cardToAdd);
                if (Constant.Runtime.DEV_MODE[0]) {
                    System.out.println("Others[" + num + "]:" + cardToAdd.getName() + " ("
                            + cardToAdd.getCard().getManaCost() + ")");
                }
                num = addComboCards(cardToAdd, num);
            } else {
                break;
            }
        }
    }

    /**
     * Add cards that work well with the given card.
     * 
     * @param cardToAdd
     *            card being checked
     * @param num
     *            number of cards
     * @return number left after adding
     */
    private int addComboCards(CardPrinted cardToAdd, int num) {
        boolean foundOne = false;
        if (cardToAdd.getCard().getDeckHints() != null
                && cardToAdd.getCard().getDeckHints().getType() != DeckHints.Type.NONE) {
            DeckHints hints = cardToAdd.getCard().getDeckHints();
            List<CardPrinted> comboCards = hints.filter(getAiPlayables());
            if (Constant.Runtime.DEV_MODE[0]) {
                System.out.println("Found " + comboCards.size() + " cards for " + cardToAdd.getName());
            }
            List<CardRankingBean> rankedComboCards = rankCards(comboCards);
            for (CardRankingBean comboBean : rankedComboCards) {
                if (num > 0) {
                    // TODO: This is not exactly right, because the
                    // rankedComboCards could include creatures and
                    // non-creatures. This code could add too many of one or the
                    // other.
                    CardPrinted combo = comboBean.getCardPrinted();
                    deckList.add(combo);
                    num--;
                    getAiPlayables().remove(combo);
                    foundOne = true;
                } else {
                    break;
                }
            }
            foundOne |= !hints.filter(deckList).isEmpty();
        }
        // remove cards with RemRandomDeck that do not have combo partners
        if (!foundOne && cardToAdd.getCard().getRemRandomDecks()) {
            deckList.remove(cardToAdd);
            availableList.add(cardToAdd);
            num++;
        }
        
        return num;
    }

    /**
     * Add creatures to the deck.
     * 
     * @param creatures
     *            cards to choose from
     * @param num
     */
    private void addCreatures(List<CardRankingBean> creatures, int num) {
        for (CardRankingBean bean : creatures) {
            if (num > 0) {
                CardPrinted c = bean.getCardPrinted();
                deckList.add(c);
                num--;
                getAiPlayables().remove(c);
                if (Constant.Runtime.DEV_MODE[0]) {
                    System.out.println("Creature[" + num + "]:" + c.getName() + " (" + c.getCard().getManaCost() + ")");
                }
                num = addComboCards(c, num);
            } else {
                break;
            }
        }
    }

    /**
     * Add creatures to the deck, trying to follow some mana curve. Trying to
     * have generous limits at each cost, but perhaps still too strict. But
     * we're trying to prevent the AI from adding everything at a single cost.
     * 
     * @param creatures
     *            cards to choose from
     * @param num
     */
    private void addManaCurveCreatures(List<CardRankingBean> creatures, int num) {
        Map<Integer, Integer> creatureCosts = new HashMap<Integer, Integer>();
        for (int i = 1; i < 7; i++) {
            creatureCosts.put(i, 0);
        }
        List<CardPrinted> currentCreatures = CardRules.Predicates.Presets.IS_CREATURE.select(deckList,
                CardPrinted.FN_GET_RULES);
        for (CardPrinted creature : currentCreatures) {
            int cmc = creature.getCard().getManaCost().getCMC();
            if (cmc < 1) {
                cmc = 1;
            } else if (cmc > 6) {
                cmc = 6;
            }
            creatureCosts.put(cmc, creatureCosts.get(cmc) + 1);
        }

        for (CardRankingBean bean : creatures) {
            CardPrinted c = bean.getCardPrinted();
            int cmc = c.getCard().getManaCost().getCMC();
            if (cmc < 1) {
                cmc = 1;
            } else if (cmc > 6) {
                cmc = 6;
            }
            Integer currentAtCmc = creatureCosts.get(cmc);
            boolean willAddCreature = false;
            if (cmc <= 1 && currentAtCmc < 2) {
                willAddCreature = true;
            } else if (cmc == 2 && currentAtCmc < 4) {
                willAddCreature = true;
            } else if (cmc == 3 && currentAtCmc < 6) {
                willAddCreature = true;
            } else if (cmc == 4 && currentAtCmc < 7) {
                willAddCreature = true;
            } else if (cmc == 5 && currentAtCmc < 3) {
                willAddCreature = true;
            } else if (cmc >= 6 && currentAtCmc < 3) {
                willAddCreature = true;
            }

            if (willAddCreature) {
                deckList.add(c);
                num--;
                getAiPlayables().remove(c);
                creatureCosts.put(cmc, creatureCosts.get(cmc) + 1);
                if (Constant.Runtime.DEV_MODE[0]) {
                    System.out.println("Creature[" + num + "]:" + c.getName() + " (" + c.getCard().getManaCost() + ")");
                }
                num = addComboCards(c, num);
            } else {
                if (Constant.Runtime.DEV_MODE[0]) {
                    System.out.println(c.getName() + " not added because CMC " + c.getCard().getManaCost().getCMC()
                            + " has " + currentAtCmc + " already.");
                }
            }
            if (num <= 0) {
                break;
            }

        }
    }

    /**
     * Rank cards.
     * 
     * @param cards
     *            CardPrinteds to rank
     * @return List of beans with card rankings
     */
    protected List<CardRankingBean> rankCards(List<CardPrinted> cards) {
        List<CardRankingBean> ranked = new ArrayList<CardRankingBean>();
        for (CardPrinted card : cards) {
            Integer rkg = draftRankings.getRanking(card.getName(), card.getEdition());
            if (rkg != null) {
                ranked.add(new CardRankingBean(rkg, card));
            }
        }
        Collections.sort(ranked, new CardRankingComparator());
        return ranked;
    }

    /**
     * Calculate average CMC.
     * 
     * @param cards
     * @return the average
     */
    private double getAverageCMC(List<CardPrinted> cards) {
        double sum = 0.0;
        for (CardPrinted cardPrinted : cards) {
            sum += cardPrinted.getCard().getManaCost().getCMC();
        }
        return sum / cards.size();
    }

    /**
     * @return the colors
     */
    public CardColor getColors() {
        return colors;
    }

    /**
     * @param colors
     *            the colors to set
     */
    public void setColors(CardColor colors) {
        this.colors = colors;
    }

    /**
     * @return the aiPlayables
     */
    public List<CardPrinted> getAiPlayables() {
        return aiPlayables;
    }

    /**
     * @param aiPlayables
     *            the aiPlayables to set
     */
    public void setAiPlayables(List<CardPrinted> aiPlayables) {
        this.aiPlayables = aiPlayables;
    }

    /**
     * Bean to hold card ranking info.
     * 
     */
    protected class CardRankingBean {
        private Integer rank;
        private CardPrinted card;

        /**
         * Constructor.
         * 
         * @param rank
         *            the rank
         * @param card
         *            the CardPrinted
         */
        public CardRankingBean(Integer rank, CardPrinted card) {
            this.rank = rank;
            this.card = card;
        }

        /**
         * @return the rank
         */
        public Integer getRank() {
            return rank;
        }

        /**
         * @return the card
         */
        public CardPrinted getCardPrinted() {
            return card;
        }

    }

}
