package forge.game.limited;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardListUtil;
import forge.Constant;
import forge.card.CardColor;
import forge.card.CardManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.Deck;
import forge.util.MyRandom;

/**
 * Deck built from a Booster Draft.
 * 
 */
public class BoosterDeck extends Deck {

    private static final long serialVersionUID = -7818685851099321964L;

    private int cardsNeeded = 22;
    private int landsNeeded = 18;
    private DeckColors colors;
    private CardList draftedList;

    private CardList aiPlayables;

    private CardList deckList = new CardList();

    public BoosterDeck(CardList dList, DeckColors pClrs) {
        super("");
        this.draftedList = dList;
        this.colors = pClrs;
        buildDeck();
    }

    /**
     * <p>
     * buildDeck.
     * </p>
     * 
     * @param draftedList
     *            a {@link forge.CardList} object.
     * @param colors
     *            a {@link forge.game.limited.DeckColors} object.
     * @return a {@link forge.deck.Deck} object.
     */
    private void buildDeck() {

        aiPlayables = draftedList.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return !(c.getSVar("RemAIDeck").equals("True") || c.getSVar("RemRandomDeck").equals("True"));
            }
        });
        for (int i = 0; i < aiPlayables.size(); i++) {
            draftedList.remove(aiPlayables.get(i));
        }


        // 1. Add best 15 on-color creatures
        addBestCreatures(15);

        // 2.Try to fill up to 22 with on-color non-creature cards
        addNonCreatures(cardsNeeded - deckList.size());

        // 3.Try to fill up to 22 with on-color creatures cards (if more than 15
        // are present)
        addBestCreatures(cardsNeeded - deckList.size());

        CardList nonLands = aiPlayables.getNotType("Land").getOnly2Colors(colors.getColor1(), colors.getColor2());

        // 4. If there are still on-color cards and the average cmc is low add a
        // 23rd card.
        if (cardsNeeded == 0 && CardListUtil.getAverageCMC(deckList) < 3 && !nonLands.isEmpty()) {
            Card c = nonLands.get(0);
            deckList.add(c);
            aiPlayables.remove(0);
            landsNeeded--;
        }

        // 5. If there are still less than 22 non-land cards add off-color
        // cards.
        addRandomCards(cardsNeeded - deckList.size());

        // 6. If it's not a mono color deck, add non-basic lands that were drafted.
        addNonBasicLands();

        final CCnt[] clrCnts = calculateLandNeeds();

        if (landsNeeded > 0) {
            addLands(clrCnts);
        }

        while (deckList.size() > 40) {
            final Card c = deckList.get(MyRandom.getRandom().nextInt(deckList.size() - 1));
            deckList.remove(c);
            aiPlayables.add(c);
        }

        while (deckList.size() < 40) {
            if (aiPlayables.size() > 1) {
                final Card c = aiPlayables.get(MyRandom.getRandom().nextInt(aiPlayables.size() - 1));
                deckList.add(c);
                aiPlayables.remove(c);
            } else if (aiPlayables.size() == 1) {
                final Card c = aiPlayables.get(0);
                deckList.add(c);
                aiPlayables.remove(c);
            } else {
                // if no playable cards remain fill up with basic lands
                for (int i = 0; i < 5; i++) {
                    if (clrCnts[i].getCount() > 0) {
                        final Card c = AllZone.getCardFactory().getCard(clrCnts[i].getColor(),
                                AllZone.getComputerPlayer());
                        c.setCurSetCode(IBoosterDraft.LAND_SET_CODE[0]);
                        deckList.add(c);
                        break;
                    }
                }
            }
        }
        if (deckList.size() == 40) {
            this.getMain().add(deckList);
            this.getSideboard().add(aiPlayables);
            this.getSideboard().add(draftedList);
        } else {
            throw new RuntimeException("BoosterDraftAI : buildDeck() error, decksize not 40");
        }
    }

    /**
     * Add lands to fulfill the given color counts.
     * @param clrCnts
     */
    private void addLands(final CCnt[] clrCnts) {
        
        // total of all ClrCnts
        int totalColor = 0;
        for (int i = 0; i < 5; i++) {
            totalColor += clrCnts[i].getCount();
            // tmpDeck += ClrCnts[i].Color + ":" + ClrCnts[i].Count + "\n";
        }

        // tmpDeck += "totalColor:" + totalColor + "\n";

        for (int i = 0; i < 5; i++) {
            if (clrCnts[i].getCount() > 0) { // calculate number of lands
                                             // for
                // each color
                final float p = (float) clrCnts[i].getCount() / (float) totalColor;
                final int nLand = (int) (landsNeeded * p) + 1;
                // tmpDeck += "nLand-" + ClrCnts[i].Color + ":" + nLand +
                // "\n";
                if (Constant.Runtime.DEV_MODE[0]) {
                    System.out.println("Basics[" + clrCnts[i].getColor() + "]:" + nLand);
                }

                // just to prevent a null exception by the deck size fixing
                // code
                // CardCounts.put(ClrCnts[i].Color, nLand);

                for (int j = 0; j <= nLand; j++) {
                    final Card c = AllZone.getCardFactory().getCard(clrCnts[i].getColor(),
                            AllZone.getComputerPlayer());
                    c.setCurSetCode(IBoosterDraft.LAND_SET_CODE[0]);
                    deckList.add(c);
                    landsNeeded--;
                }
            }
        }
        int n = 0;
        while (landsNeeded > 0) {
            if (clrCnts[n].getCount() > 0) {
                final Card c = AllZone.getCardFactory().getCard(clrCnts[n].getColor(), AllZone.getComputerPlayer());
                c.setCurSetCode(IBoosterDraft.LAND_SET_CODE[0]);
                deckList.add(c);
                landsNeeded--;

                if (Constant.Runtime.DEV_MODE[0]) {
                    System.out.println("AddBasics: " + c.getName());
                }
            }
            if (++n > 4) {
                n = 0;
            }
        }
    }

    /**
     * attempt to optimize basic land counts according to color representation
     * @return CCnt
     */
    private CCnt[] calculateLandNeeds() {
        final CCnt[] clrCnts = { new CCnt("Plains", 0), new CCnt("Island", 0), new CCnt("Swamp", 0),
                new CCnt("Mountain", 0), new CCnt("Forest", 0) };

        // count each card color using mana costs
        // TODO: count hybrid mana differently?
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
        CardList lands = aiPlayables.getType("Land");
        while (!colors.getColor1().equals(colors.getColor2()) && landsNeeded > 0 && lands.size() > 0) {
            final Card c = lands.get(0);

            deckList.add(c);
            landsNeeded--;
            aiPlayables.remove(c);

            lands = aiPlayables.getType("Land");

            if (Constant.Runtime.DEV_MODE[0]) {
                System.out.println("Land:" + c.getName());
            }
        }
    }

    /**
     * Add random cards to the deck.
     * @param nCards
     */
    private void addRandomCards(int nCards) {
        CardList z = aiPlayables.getNotType("Land");
        int ii = 0;
        while ((nCards > 0) && (z.size() > 1)) {

            final Card c = z.get(MyRandom.getRandom().nextInt(z.size() - 1));

            deckList.add(c);
            cardsNeeded--;
            nCards--;
            aiPlayables.remove(c);
            z.remove(c);

            if (Constant.Runtime.DEV_MODE[0]) {
                System.out.println("NonLands[" + ii++ + "]:" + c.getName() + "(" + c.getManaCost() + ")");
            }
        }
    }

    /**
     * Add non creatures to the deck.
     * @param nCards
     */
    private void addNonCreatures(int nCards) {
        CardList others = aiPlayables.getNotType("Creature").getNotType("Land")
                .getOnly2Colors(colors.getColor1(), colors.getColor2());

        int ii = 0;
        while (nCards > 0 && others.size() > 0) {
            int index = 0;
            if (others.size() > 1) {
                index = MyRandom.getRandom().nextInt(others.size() - 1);
            }
            final Card c = others.get(index);

            deckList.add(c);
            cardsNeeded--;
            nCards--;
            aiPlayables.remove(c);
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
        CardList creatures = aiPlayables.getType("Creature").getOnly2Colors(colors.getColor1(), colors.getColor2());
        creatures.sort(new CreatureComparator());

        int i = 0;
        while (nCreatures > 0 && creatures.size() > 0) {
            final Card c = creatures.get(0);

            deckList.add(c);
            cardsNeeded--;
            nCreatures--;
            aiPlayables.remove(c);
            creatures.remove(c);

            if (Constant.Runtime.DEV_MODE[0]) {
                System.out.println("Creature[" + i + "]:" + c.getName() + " (" + c.getManaCost() + ")");
            }

            i++;
        }
    }

}
