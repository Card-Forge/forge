package forge;


import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;


/**
 * <p>ReadDraftBoosterPack class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class ReadDraftBoosterPack implements NewConstants {

    /** Constant <code>comment="//"</code> */
    final private static String comment = "//";

    private CardList commonCreatureList = new CardList();
    private CardList commonNonCreatureList = new CardList();

    private CardList commonList = new CardList();
    private CardList uncommonList = new CardList();
    private CardList rareList = new CardList();

    /**
     * <p>Constructor for ReadDraftBoosterPack.</p>
     */
    public ReadDraftBoosterPack() {
        setup();
    }

    //returns "common", "uncommon", or "rare"
    /**
     * <p>getRarity.</p>
     *
     * @param cardName a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public String getRarity(String cardName) {
        if (commonList.containsName(cardName)) return "Common";
        if (uncommonList.containsName(cardName)) return "Uncommon";
        if (rareList.containsName(cardName)) return "Rare";

        ArrayList<String> land = new ArrayList<String>();
        land.add("Forest");
        land.add("Plains");
        land.add("Swamp");
        land.add("Mountain");
        land.add("Island");
        land.add("Terramorphic Expanse");
        land.add("Snow-Covered Forest");
        land.add("Snow-Covered Plains");
        land.add("Snow-Covered Swamp");
        land.add("Snow-Covered Mountain");
        land.add("Snow-Covered Island");
        if (land.contains(cardName)) return "Land";

        return "error";
    }

    /**
     * <p>getBoosterPack5.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardList getBoosterPack5() {
        CardList list = new CardList();
        for (int i = 0; i < 5; i++)
            list.addAll(getBoosterPack());

        for (int i = 0; i < 20; i++) {
            list.add(AllZone.getCardFactory().getCard("Forest", AllZone.getHumanPlayer()));
            list.add(AllZone.getCardFactory().getCard("Island", AllZone.getHumanPlayer()));
            list.add(AllZone.getCardFactory().getCard("Plains", AllZone.getHumanPlayer()));
            list.add(AllZone.getCardFactory().getCard("Mountain", AllZone.getHumanPlayer()));
            list.add(AllZone.getCardFactory().getCard("Swamp", AllZone.getHumanPlayer()));
            list.add(AllZone.getCardFactory().getCard("Snow-Covered Forest", AllZone.getHumanPlayer()));
            list.add(AllZone.getCardFactory().getCard("Snow-Covered Island", AllZone.getHumanPlayer()));
            list.add(AllZone.getCardFactory().getCard("Snow-Covered Plains", AllZone.getHumanPlayer()));
            list.add(AllZone.getCardFactory().getCard("Snow-Covered Mountain", AllZone.getHumanPlayer()));
            list.add(AllZone.getCardFactory().getCard("Snow-Covered Swamp", AllZone.getHumanPlayer()));
        }

        for (int i = 0; i < 4; i++)
            list.add(AllZone.getCardFactory().getCard("Terramorphic Expanse", AllZone.getHumanPlayer()));

        return list;
    }//getBoosterPack5()

    /**
     * <p>getBoosterPack.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardList getBoosterPack() {
        CardList pack = new CardList();

        pack.add(getRandomCard(rareList));

        for (int i = 0; i < 3; i++)
            pack.add(getRandomCard(uncommonList));

        //11 commons, 7 creature 4 noncreature
        CardList variety;
        for (int i = 0; i < 7; i++) {
            variety = getVariety(commonCreatureList);
            pack.add(getRandomCard(variety));
        }

        for (int i = 0; i < 4; i++) {
            variety = getVariety(commonNonCreatureList);
            pack.add(getRandomCard(variety));
        }

        if (pack.size() != 15)
            throw new RuntimeException("ReadDraftBoosterPack : getBoosterPack() error, pack is not 15 cards - "
                    + pack.size());

        return pack;
    }

    /**
     * <p>getShopCards.</p>
     *
     * @param numberWins a int.
     * @return a {@link forge.CardList} object.
     */
    public CardList getShopCards(int numberWins) {
        CardList list = new CardList();

        int numberRares = 1 + numberWins / 15;
        if (numberRares > 10)
            numberRares = 10;

        for (int i = 0; i < numberRares; i++)
            list.add(getRandomCard(rareList));

        int numberUncommons = 3 + numberWins / 10;
        if (numberUncommons > 20)
            numberUncommons = 20;

        for (int i = 0; i < numberUncommons; i++)
            list.add(getRandomCard(uncommonList));

        int numberCommons = 5 + numberWins / 5;
        if (numberCommons > 35)
            numberCommons = 35;

        for (int i = 0; i < numberCommons; i++)
            list.add(getRandomCard(commonList));

        return list;
    }

    //return CardList of 5 or 6 cards, one for each color and maybe an artifact
    /**
     * <p>getVariety.</p>
     *
     * @param in a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    private CardList getVariety(CardList in) {
        CardList out = new CardList();

        String color[] = Constant.Color.Colors;
        Card check;
        in.shuffle();

        for (int i = 0; i < color.length; i++) {
            check = findColor(in, color[i]);
            if (check != null) out.add(check);
        }

        return out;
    }//getVariety()

    /**
     * <p>findColor.</p>
     *
     * @param in a {@link forge.CardList} object.
     * @param color a {@link java.lang.String} object.
     * @return a {@link forge.Card} object.
     */
    private Card findColor(CardList in, String color) {
        for (int i = 0; i < in.size(); i++)
            if (CardUtil.getColors(in.get(i)).contains(color)) return in.get(i);

        return null;
    }


    /**
     * <p>getRandomCard.</p>
     *
     * @param list a {@link forge.CardList} object.
     * @return a {@link forge.Card} object.
     */
    private Card getRandomCard(CardList list) {
        for (int i = 0; i < 10; i++)
            list.shuffle();

        int index = MyRandom.random.nextInt(list.size());

        Card c = AllZone.getCardFactory().copyCard(list.get(index));
        c.setRarity("rare");
        return c;
    }//getRandomCard()

    /**
     * <p>setup.</p>
     */
    private void setup() {
        commonList = readFile(ForgeProps.getFile(DRAFT.COMMON));
        uncommonList = readFile(ForgeProps.getFile(DRAFT.UNCOMMON));
        rareList = readFile(ForgeProps.getFile(DRAFT.RARE));

        System.out.println("commonList size:" + commonList.size());
        System.out.println("ucommonList size:" + uncommonList.size());
        System.out.println("rareList size:" + rareList.size());

        commonCreatureList = commonList.getType("Creature");
        commonNonCreatureList = commonList.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return !c.isCreature();
            }
        });

/*        CardList AllCards = AllZone.getCardFactory().getAllCards();

for (int i=0; i<AllCards.size(); i++)
{
    Card aCard = AllCards.get(i);
    String rr = aCard.getSVar("Rarity");

    if (rr.equals("Common"))
    {
        commonList.add(aCard);
        if (aCard.isCreature())
            commonCreatureList.add(aCard);
        else
            commonNonCreatureList.add(aCard);
    }
    else if (rr.equals("Uncommon"))
    {
        uncommonList.add(aCard);
    }
    else if (rr.equals("Rare"))
    {
        rareList.add(aCard);
    }
    else if (rr.equals("Mythic"))
    {
        rareList.add(aCard);
    }

}*/
    }//setup()


    /**
     * <p>readFile.</p>
     *
     * @param file a {@link java.io.File} object.
     * @return a {@link forge.CardList} object.
     */
    private CardList readFile(File file) {
        CardList cardList = new CardList();

        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader(file));
            String line = in.readLine();

            //stop reading if end of file or blank line is read
            while (line != null && (line.trim().length() != 0)) {
                Card c;
                if (!line.startsWith(comment)) {
                    c = AllZone.getCardFactory().getCard(line.trim(), AllZone.getHumanPlayer());
                    cardList.add(c);
                }

                line = in.readLine();
            }//if

        } catch (Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("ReadDraftBoosterPack : readFile error, " + ex);
        }

        return cardList;
    }//readFile()
}


