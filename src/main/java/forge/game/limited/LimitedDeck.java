package forge.game.limited;

import java.util.HashMap;
import java.util.Map;

import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardListUtil;
import forge.Constant;
import forge.card.CardColor;
import forge.card.CardManaCost;
import forge.card.DeckWants;
import forge.card.mana.ManaCostShard;
import forge.deck.Deck;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.util.MyRandom;

/**
 * Limited format deck.
 * 
 */
public class LimitedDeck extends Deck {

    private static final long serialVersionUID = -8818599781874958332L;
    private int numSpellsNeeded = 22;
    private int landsNeeded = 18;
    private DeckColors colors;
    private CardList availableList;
    private CardList aiPlayables;
    private CardList deckList = new CardList();

    /**
     * 
     * Constructor.
     * 
     * @param dList
     *            Cards to build the deck from.
     * @param pClrs
     *            Chosen colors.
     */
    public LimitedDeck(CardList dList, DeckColors pClrs) {
        super("");
        this.availableList = dList;
        this.colors = pClrs;
        removeUnplayables();
    }

    /**
     * Constructor.
     * 
     * @param list
     *            Cards to build the deck from.
     */
    public LimitedDeck(CardList list) {
        super("");
        this.availableList = list;
        removeUnplayables();
    }

    /**
     * <p>
     * buildDeck.
     * </p>
     */
    protected void buildDeck() {
        // 0. Add any planeswalkers
        CardList walkers = getAiPlayables().getOnly2Colors(getColors().getColor1(), getColors().getColor2()).getType("Planeswalker");
        deckList.addAll(walkers);
        getAiPlayables().removeAll(walkers);
        if (walkers.size() > 0) {
            System.out.println("Planeswalker: " + walkers.get(0).toString());
        }

        // 0.5. Add combo cards (should this be done later? or perhaps within
        // each method?)
        addComboCards();

        // 1. Add creatures, trying to follow mana curve
        addManaCurveCreatures(15 - deckList.getType("Creature").size());

        // 2.Try to fill up to 22 with on-color non-creature cards
        addNonCreatures(numSpellsNeeded - deckList.size());

        // 3.Try to fill up to 22 with on-color creatures cards (if more than 15
        // are present)
        addBestCreatures(numSpellsNeeded - deckList.size());

        CardList nonLands = getAiPlayables().getNotType("Land").getOnly2Colors(getColors().getColor1(), getColors().getColor2());

        // 4. If there are still on-color cards and the average cmc is low add a
        // 23rd card.
        if (deckList.size() == numSpellsNeeded && CardListUtil.getAverageCMC(deckList) < 3 && !nonLands.isEmpty()) {
            Card c = nonLands.get(0);
            deckList.add(c);
            getAiPlayables().remove(0);
            landsNeeded--;
        }

        // 5. If there are still less than 22 non-land cards add off-color
        // cards.
        addRandomCards(numSpellsNeeded - deckList.size());

        // 6. If it's not a mono color deck, add non-basic lands that were
        // drafted.
        addNonBasicLands();

        // 7. Fill up with basic lands.
        final CCnt[] clrCnts = calculateLandNeeds();
        if (landsNeeded > 0) {
            addLands(clrCnts);
        }

        while (deckList.size() > 40) {
            final Card c = deckList.get(MyRandom.getRandom().nextInt(deckList.size() - 1));
            deckList.remove(c);
            getAiPlayables().add(c);
        }

        while (deckList.size() < 40) {
            if (getAiPlayables().size() > 1) {
                final Card c = getAiPlayables().get(MyRandom.getRandom().nextInt(getAiPlayables().size() - 1));
                deckList.add(c);
                getAiPlayables().remove(c);
            } else if (getAiPlayables().size() == 1) {
                final Card c = getAiPlayables().get(0);
                deckList.add(c);
                getAiPlayables().remove(c);
            } else {
                // if no playable cards remain fill up with basic lands
                for (int i = 0; i < 5; i++) {
                    if (clrCnts[i].getCount() > 0) {
                        final CardPrinted cp = CardDb.instance().getCard(clrCnts[i].getColor(), IBoosterDraft.LAND_SET_CODE[0]);
                        deckList.add(cp.toForgeCard());
                        break;
                    }
                }
            }
        }
        if (deckList.size() == 40) {
            this.getMain().add(deckList);
            this.getSideboard().add(getAiPlayables());
            this.getSideboard().add(availableList);

            int i = 0;
            System.out.println("DECK");
            for (Card c : deckList) {
                i++;
                System.out.println(i + ". " + c.toString() + ": " + c.getManaCost().toString());
            }
            i = 0;
            System.out.println("NOT PLAYABLE");
            for (Card c : availableList) {
                i++;
                System.out.println(i + ". " + c.toString() + ": " + c.getManaCost().toString());
            }
            i = 0;
            System.out.println("NOT PICKED");
            for (Card c : getAiPlayables()) {
                i++;
                System.out.println(i + ". " + c.toString() + ": " + c.getManaCost().toString());
            }

        } else {
            throw new RuntimeException("BoosterDraftAI : buildDeck() error, decksize not 40");
        }
    }

    /**
     * Remove AI unplayable cards.
     */
    protected void removeUnplayables() {
        setAiPlayables(availableList.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                boolean unPlayable = c.getSVar("RemAIDeck").equals("True");
                unPlayable |= c.getSVar("RemRandomDeck").equals("True") && c.getSVar("DeckWants").equals("");
                return !unPlayable;
            }
        }));
        availableList.removeAll(getAiPlayables());
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

        int landsAdded = 0;
        for (int i = 0; i < 5; i++) {
            if (clrCnts[i].getCount() > 0) {
                // calculate number of lands for each color
                final float p = (float) clrCnts[i].getCount() / (float) totalColor;
                final int nLand = (int) (landsNeeded * p) + 1;
                if (Constant.Runtime.DEV_MODE[0]) {
                    System.out.println("Basics[" + clrCnts[i].getColor() + "]: " + clrCnts[i].getCount() + "/"
                            + totalColor + " = " + p + " = " + nLand);
                }

                for (int j = 0; j <= nLand; j++) {
                    final CardPrinted cp = CardDb.instance().getCard(clrCnts[i].getColor(), IBoosterDraft.LAND_SET_CODE[0]);
                    deckList.add(cp.toForgeCard());
                    landsAdded++;
                }
            }
        }

        landsNeeded = landsNeeded - landsAdded;
        int n = 0;
        while (landsNeeded > 0) {
            if (clrCnts[n].getCount() > 0) {
                CardPrinted cp = CardDb.instance().getCard(clrCnts[n].getColor(), IBoosterDraft.LAND_SET_CODE[0]);
                deckList.add(cp.toForgeCard());
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
     * attempt to optimize basic land counts according to color representation.
     * 
     * @return CCnt
     */
    private CCnt[] calculateLandNeeds() {
        final CCnt[] clrCnts = { new CCnt("Plains", 0), new CCnt("Island", 0), new CCnt("Swamp", 0),
                new CCnt("Mountain", 0), new CCnt("Forest", 0) };

        // count each card color using mana costs
        for (int i = 0; i < deckList.size(); i++) {
            final CardManaCost mc = deckList.get(i).getManaCost();

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
    private void addNonBasicLands() {
        CardList lands = getAiPlayables().getType("Land");
        while (!getColors().getColor1().equals(getColors().getColor2()) && landsNeeded > 0 && lands.size() > 0) {
            final Card c = lands.get(0);

            deckList.add(c);
            landsNeeded--;
            getAiPlayables().remove(c);

            lands = getAiPlayables().getType("Land");

            if (Constant.Runtime.DEV_MODE[0]) {
                System.out.println("Land:" + c.getName());
            }
        }
    }

    /**
     * Add random cards to the deck.
     * 
     * @param nCards
     */
    private void addRandomCards(int nCards) {
        CardList z = getAiPlayables().getNotType("Land");
        int ii = 0;
        while ((nCards > 0) && (z.size() > 1)) {

            final Card c = z.get(MyRandom.getRandom().nextInt(z.size() - 1));

            deckList.add(c);
            nCards--;
            getAiPlayables().remove(c);
            z.remove(c);

            if (Constant.Runtime.DEV_MODE[0]) {
                System.out.println("NonLands[" + ii++ + "]:" + c.getName() + "(" + c.getManaCost() + ")");
            }
        }
    }

    /**
     * Add non creatures to the deck.
     * 
     * @param nCards
     */
    private void addNonCreatures(int nCards) {
        CardList others = getAiPlayables().getNotType("Creature").getNotType("Land")
                .getOnly2Colors(getColors().getColor1(), getColors().getColor2());

        int ii = 0;
        while (nCards > 0 && others.size() > 0) {
            int index = 0;
            if (others.size() > 1) {
                index = MyRandom.getRandom().nextInt(others.size() - 1);
            }
            final Card c = others.get(index);

            deckList.add(c);
            nCards--;
            getAiPlayables().remove(c);
            others.remove(c);

            if (Constant.Runtime.DEV_MODE[0]) {
                System.out.println("Others[" + ii++ + "]:" + c.getName() + " (" + c.getManaCost() + ")");
            }
        }
    }

    /**
     * Add the best creatures to the deck.
     * 
     * @param nCreatures
     */
    private void addBestCreatures(int nCreatures) {
        CardList creatures = getAiPlayables().getType("Creature").getOnly2Colors(getColors().getColor1(), getColors().getColor2());
        creatures.sort(new CreatureComparator());

        int i = 0;
        while (nCreatures > 0 && creatures.size() > 0) {
            final Card c = creatures.get(0);

            deckList.add(c);
            nCreatures--;
            getAiPlayables().remove(c);
            creatures.remove(c);

            if (Constant.Runtime.DEV_MODE[0]) {
                System.out.println("Creature[" + i + "]:" + c.getName() + " (" + c.getManaCost() + ")");
            }

            i++;
        }
    }

    /**
     * Add creatures to the deck, trying to follow some mana curve. Trying to
     * have generous limits at each cost, but perhaps still too strict. But
     * we're trying to prevent the AI from adding everything at a single cost.
     * 
     * @param nCreatures
     */
    private void addManaCurveCreatures(int nCreatures) {
        CardList creatures = getAiPlayables().getType("Creature").getOnly2Colors(getColors().getColor1(), getColors().getColor2());
        creatures.sort(new CreatureComparator());

        Map<Integer, Integer> creatureCosts = new HashMap<Integer, Integer>();
        for (int i = 1; i < 7; i++) {
            creatureCosts.put(i, 0);
        }
        CardList currentCreatures = deckList.getType("Creature");
        for (Card creature : currentCreatures) {
            int cmc = creature.getCMC();
            if (cmc < 1) {
                cmc = 1;
            } else if (cmc > 6) {
                cmc = 6;
            }
            creatureCosts.put(cmc, creatureCosts.get(cmc) + 1);
        }
        int i = 0;
        for (Card c : creatures) {
            int cmc = c.getCMC();
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
                nCreatures--;
                getAiPlayables().remove(c);
                creatureCosts.put(cmc, creatureCosts.get(cmc) + 1);

                if (Constant.Runtime.DEV_MODE[0]) {
                    System.out.println("Creature[" + i + "]:" + c.getName() + " (" + c.getManaCost() + ")");
                }
                i++;
            } else {
                if (Constant.Runtime.DEV_MODE[0]) {
                    System.out.println(c.getName() + " not added because CMC " + c.getCMC() + " has " + currentAtCmc
                            + " already.");
                }
            }
            if (nCreatures <= 0) {
                break;
            }

        }
    }

    /**
     * Add cards that need other cards to be in the deck.
     */
    private void addComboCards() {
        CardList onColorPlayables = getAiPlayables().getOnly2Colors(getColors().getColor1(), getColors().getColor2());
        for (Card c : onColorPlayables) {
            if (!c.getSVar("DeckWants").equals("")) {
                DeckWants wants = c.getDeckWants();
                CardList cards = wants.filter(onColorPlayables);
                if (cards.size() >= wants.getMinCardsNeeded()) {
                    if (Constant.Runtime.DEV_MODE[0]) {
                        System.out.println("Adding " + c.getName() + " with up to " + cards.size()
                                + " combo cards (e.g., " + cards.get(0).getName() + ").");
                    }
                    deckList.add(c);
                    getAiPlayables().remove(c);
                    if (cards.size() <= 4) {
                        deckList.addAll(cards);
                        getAiPlayables().removeAll(cards);
                    } else {
                        cards.shuffle();
                        for (int i = 0; i < 4; i++) {
                            Card theCard = cards.get(i);
                            deckList.add(theCard);
                            getAiPlayables().remove(theCard);
                        }
                    }
                } else {
                    // Could not find combo cards, so don't put this card in the deck.
                    getAiPlayables().remove(c);
                }
            }
        }
    }

    /**
     * @return the colors
     */
    public DeckColors getColors() {
        return colors;
    }

    /**
     * @param colors the colors to set
     */
    public void setColors(DeckColors colors) {
        this.colors = colors;
    }

    /**
     * @return the aiPlayables
     */
    public CardList getAiPlayables() {
        return aiPlayables;
    }

    /**
     * @param aiPlayables the aiPlayables to set
     */
    public void setAiPlayables(CardList aiPlayables) {
        this.aiPlayables = aiPlayables;
    }

}
