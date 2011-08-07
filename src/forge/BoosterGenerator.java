package forge;

import forge.deck.Deck;
import forge.deck.DeckManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * <p>BoosterGenerator class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class BoosterGenerator {
    private CardList commons = new CardList();
    private CardList uncommons = new CardList();
    private CardList rares = new CardList();
    private CardList mythics = new CardList();
    private CardList specials = new CardList();

    private int iCommons = 0;
    private int iUncommons = 0;
    private int iRares = 0;
    private int iMythics = 0;
    private int iSpecials = 0;

    private int numCommons = 0;
    private int numUncommons = 0;
    private int numRares = 0;
    private int numMythics = 0;
    private int numSpecials = 0;

    //private Random r  = new Random();

    /**
     * <p>Constructor for BoosterGenerator.</p>
     */
    public BoosterGenerator() {
        numCommons = 11;
        numUncommons = 3;
        numRares = 1;
        numMythics = 0;
        numSpecials = 0;

        for (Card c : AllZone.getCardFactory()) {
            SetInfo si = SetInfoUtil.getSetInfo_Code(c.getSets(), SetInfoUtil.getMostRecentSet(c.getSets()));

            addToRarity(c, si);
        }

        shuffleAll();
        
/*        //reduce cardpool to approximate the size of a small set (175) for better drafting in full mode
        tList.clear();
        for (int i=0; i<100; i++)			// 8 x 11 x 3 commons = 264 cards with each card showing up about once per round
        	tList.add(commons.get(i));
        commons.clear();
        commons.addAll(tList);
        
        tList.clear();
        for (int i=0; i<50; i++)			// 8 x 3 x 3 uncommons = 72 cards with some cards showing up twice
        	tList.add(uncommons.get(i));
        uncommons.clear();
        uncommons.addAll(tList);
        
        tList.clear();
        for (int i=0; i<25; i++)			// 8 x 1 x 3 rares = 24 cards with no cards
        	tList.add(rares.get(i));
        rares.clear();
        rares.addAll(tList);
        
        // don't worry about reducing the mythics
*/    }

    /**
     * <p>Constructor for BoosterGenerator.</p>
     *
     * @param DeckFile a {@link java.lang.String} object.
     * @param nCommons a int.
     * @param nUncommons a int.
     * @param nRares a int.
     * @param nMythics a int.
     * @param nSpecials a int.
     * @param ignoreRarity a boolean.
     */
    public BoosterGenerator(String DeckFile, int nCommons, int nUncommons, int nRares, int nMythics, int nSpecials, boolean ignoreRarity) {
        numCommons = nCommons;
        numUncommons = nUncommons;
        numRares = nRares;
        numMythics = nMythics;
        numSpecials = nSpecials;

        //DeckManager dio = new DeckManager(ForgeProps.getFile(NewConstants.NEW_DECKS));
        DeckManager dio = AllZone.getDeckManager();
        Deck dPool = dio.getDeck(DeckFile);
        if (dPool == null)
            throw new RuntimeException("BoosterGenerator : deck not found - " + DeckFile);

        CardList cList = new CardList();
        List<String> tList = dPool.getMain();

        for (int i = 0; i < tList.size(); i++) {
            String cardName = tList.get(i);
            String setCode = "";
            if (cardName.contains("|")) {
                String s[] = cardName.split("\\|", 2);
                cardName = s[0];
                setCode = s[1];
            }

            Card c = AllZone.getCardFactory().getCard(cardName, AllZone.getHumanPlayer());

            if (!setCode.equals(""))
                c.setCurSetCode(setCode);
            else if ((c.getSets().size() > 0)) // && card.getCurSetCode().equals(""))
                c.setRandomSetCode();

            cList.add(c);
        }


        for (int i = 0; i < cList.size(); i++) {
            Card c = cList.get(i);
            SetInfo si = null;
            if (c.getCurSetCode().equals(""))
                si = SetInfoUtil.getSetInfo_Code(c.getSets(), SetInfoUtil.getMostRecentSet(c.getSets()));
            else
                si = SetInfoUtil.getSetInfo_Code(c.getSets(), c.getCurSetCode());

            if (ignoreRarity)
                commons.add(c);
            else
                addToRarity(c, si);
        }

        shuffleAll();
    }

    /**
     * <p>Constructor for BoosterGenerator.</p>
     *
     * @param SetCode a {@link java.lang.String} object.
     */
    public BoosterGenerator(final String SetCode) {
        numCommons = 0;
        numUncommons = 0;
        numRares = 0;
        numMythics = 0;
        numSpecials = 0;

        for (Card c : AllZone.getCardFactory()) {
            SetInfo si = SetInfoUtil.getSetInfo_Code(c.getSets(), SetCode);

            if (si != null) {
                c.setCurSetCode(SetCode);

                Random r = new Random();
                int n = si.PicCount;
                if (n > 1)
                    c.setRandomPicture(r.nextInt(n - 1) + 1);

                addToRarity(c, si);
            }
        }

        shuffleAll();

        ArrayList<String> bpData = FileUtil.readFile("res/boosterdata/" + SetCode + ".pack");

        for (String line : bpData) {
            if (line.startsWith("Commons:")) {
                numCommons = Integer.parseInt(line.substring(8));
            } else if (line.startsWith("Uncommons:")) {
                numUncommons = Integer.parseInt(line.substring(10));
            } else if (line.startsWith("Rares:")) {
                numRares = Integer.parseInt(line.substring(6));
            } else if (line.startsWith("Mythics:")) {
                numMythics = Integer.parseInt(line.substring(8));
            } else if (line.startsWith("Specials:")) {
                numSpecials = Integer.parseInt(line.substring(9));
            }

        }

        if (Constant.Runtime.DevMode[0]) {
            System.out.println("numCommons: " + numCommons);
            System.out.println("numUncommons: " + numUncommons);
            System.out.println("numRares: " + numRares);
            System.out.println("numMythics: " + numMythics);
            System.out.println("numSpecials: " + numSpecials);
        }

    }

    /**
     * <p>addToRarity.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param si a {@link forge.SetInfo} object.
     */
    private void addToRarity(Card c, SetInfo si) {
        if (si != null) {
            if (si.Rarity.equals("Common"))
                commons.add(c);
            else if (si.Rarity.equals("Uncommon"))
                uncommons.add(c);
            else if (si.Rarity.equals("Rare"))
                rares.add(c);
            else if (si.Rarity.equals("Mythic"))
                mythics.add(c);
            else if (si.Rarity.equals("Special"))
                specials.add(c);
        }
    }

    /**
     * <p>shuffleAll.</p>
     */
    private void shuffleAll() {

        if (commons.size() > 0)
            commons.shuffle();

        if (uncommons.size() > 0)
            uncommons.shuffle();

        if (rares.size() > 0)
            rares.shuffle();

        if (mythics.size() > 0)
            mythics.shuffle();

        if (specials.size() > 0)
            specials.shuffle();

        if (Constant.Runtime.DevMode[0]) {
            System.out.println("commons.size: " + commons.size());
            System.out.println("uncommons.size: " + uncommons.size());
            System.out.println("rares.size: " + rares.size());
            System.out.println("mythics.size: " + mythics.size());
            System.out.println("specials.size: " + specials.size());
        }
    }

    /**
     * <p>getBoosterPack.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardList getBoosterPack() {
        CardList temp = new CardList();

        int i = 0;

        if (commons.size() > numCommons) {
            for (i = 0; i < numCommons; i++) {
                if (iCommons >= commons.size())
                    iCommons = 0;

                temp.add(commons.get(iCommons++));
            }
        }

        if (uncommons.size() > numUncommons) {
            for (i = 0; i < numUncommons; i++) {
                if (iUncommons >= uncommons.size())
                    iUncommons = 0;

                temp.add(uncommons.get(iUncommons++));
            }
        }

        for (i = 0; i < numRares; i++) {
            if (numMythics > 0) {
                if (mythics.size() > numMythics) {
                    if (MyRandom.random.nextInt(8) <= 1) {
                        if (iMythics >= mythics.size())
                            iMythics = 0;

                        temp.add(mythics.get(iMythics++));
                    } else {
                        if (iRares >= rares.size())
                            iRares = 0;

                        temp.add(rares.get(iRares++));
                    }
                }
            } else {
                if (rares.size() > numRares) {
                    if (iRares >= rares.size())
                        iRares = 0;

                    temp.add(rares.get(iRares++));
                }
            }
        }

        if (specials.size() > numSpecials) {
            for (i = 0; i < numSpecials; i++) {
                if (iSpecials >= specials.size())
                    iSpecials = 0;

                temp.add(specials.get(iSpecials++));
            }
        }

        return temp;
    }

    /**
     * <p>getBoosterPackSize.</p>
     *
     * @return a int.
     */
    public int getBoosterPackSize() {
        return numCommons + numUncommons + numRares + numSpecials;
    }
}
