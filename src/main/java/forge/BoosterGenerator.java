package forge;

import forge.card.CardDb;
import forge.card.CardPoolView;
import forge.card.CardPrinted;
import forge.deck.Deck;
import forge.deck.DeckManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import net.slightlymagic.maxmtg.Predicate;

/**
 * <p>BoosterGenerator class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class BoosterGenerator {
    private List<CardPrinted> commons = new ArrayList<CardPrinted>();
    private List<CardPrinted> uncommons = new ArrayList<CardPrinted>();
    private List<CardPrinted> rares = new ArrayList<CardPrinted>();
    private List<CardPrinted> mythics = new ArrayList<CardPrinted>();
    private List<CardPrinted> specials = new ArrayList<CardPrinted>();

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

        for (CardPrinted c : CardDb.instance().getAllUniqueCards()) {
            addToRarity(c);
        }

        shuffleAll();
    }

    /**
     * 
     * TODO Write javadoc for Constructor.
     * @param deckFile a String
     * @param nCommons an int
     * @param nUncommons an int
     * @param nRares an int
     * @param nMythics an int
     * @param nSpecials an int
     * @param ignoreRarity a boolean
     */
    public BoosterGenerator(final String deckFile, final int nCommons, final int nUncommons, final int nRares,
            final int nMythics, final int nSpecials, final boolean ignoreRarity)
    {
        numCommons = nCommons;
        numUncommons = nUncommons;
        numRares = nRares;
        numMythics = nMythics;
        numSpecials = nSpecials;

        //DeckManager dio = new DeckManager(ForgeProps.getFile(NewConstants.NEW_DECKS));
        DeckManager dio = AllZone.getDeckManager();
        Deck dPool = dio.getDeck(deckFile);
        if (dPool == null) {
            throw new RuntimeException("BoosterGenerator : deck not found - " + deckFile);
        }

        CardPoolView tList = dPool.getMain();
        for (Entry<CardPrinted, Integer> e : tList) {
            if (ignoreRarity) { commons.add(e.getKey()); }
            else { addToRarity(e.getKey()); }
        }

        shuffleAll();
    }

    /**
     * <p>Constructor for BoosterGenerator.</p>
     *
     * @param setCode a {@link java.lang.String} object.
     */
    public BoosterGenerator(final String setCode) {
        numCommons = 0;
        numUncommons = 0;
        numRares = 0;
        numMythics = 0;
        numSpecials = 0;

        List<String> setsList = Arrays.asList(new String[]{setCode});
        Predicate<CardPrinted> filter = CardPrinted.Predicates.printedInSets(setsList, true);
        List<CardPrinted> cardsInThisSet = filter.select(CardDb.instance().getAllCards());

        for (CardPrinted c : cardsInThisSet) {
            addToRarity(c);
        }

        shuffleAll();

        ArrayList<String> bpData = FileUtil.readFile("res/boosterdata/" + setCode + ".pack");

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
     * <p>shuffleAll.</p>
     */
    private void shuffleAll() {

        if (!commons.isEmpty()) { Collections.shuffle(commons, MyRandom.random); }
        if (!uncommons.isEmpty()) { Collections.shuffle(uncommons, MyRandom.random); }
        if (!rares.isEmpty()) { Collections.shuffle(rares, MyRandom.random); }
        if (!mythics.isEmpty()) { Collections.shuffle(mythics, MyRandom.random); }
        if (!specials.isEmpty()) { Collections.shuffle(specials, MyRandom.random); }

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
    public final List<CardPrinted> getBoosterPack() {
        List<CardPrinted> temp = new ArrayList<CardPrinted>();

        int i = 0;

        if (commons.size() > numCommons) {
            for (i = 0; i < numCommons; i++) {
                if (iCommons >= commons.size()) {
                    iCommons = 0;
                }

                temp.add(commons.get(iCommons++));
            }
        }

        if (uncommons.size() > numUncommons) {
            for (i = 0; i < numUncommons; i++) {
                if (iUncommons >= uncommons.size()) {
                    iUncommons = 0;
                }

                temp.add(uncommons.get(iUncommons++));
            }
        }

        for (i = 0; i < numRares; i++) {
            if (numMythics > 0) {
                if (mythics.size() > numMythics) {
                    if (MyRandom.random.nextInt(8) <= 1) {
                        if (iMythics >= mythics.size()) {
                            iMythics = 0;
                        }

                        temp.add(mythics.get(iMythics++));
                    } else {
                        if (iRares >= rares.size()) {
                            iRares = 0;
                        }

                        temp.add(rares.get(iRares++));
                    }
                }
            } else {
                if (rares.size() > numRares) {
                    if (iRares >= rares.size()) {
                        iRares = 0;
                    }

                    temp.add(rares.get(iRares++));
                }
            }
        }

        if (specials.size() > numSpecials) {
            for (i = 0; i < numSpecials; i++) {
                if (iSpecials >= specials.size()) {
                    iSpecials = 0;
                }

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
    public final int getBoosterPackSize() {
        return numCommons + numUncommons + numRares + numSpecials;
    }

    private void addToRarity(final CardPrinted c) {
        switch(c.getRarity()) {
            case Common: commons.add(c); break;
            case Uncommon: uncommons.add(c); break;
            case Rare: rares.add(c); break;
            case MythicRare: mythics.add(c); break;
            case Special: specials.add(c); break;
            default: return;
        }
    }

}
