package forge;

import forge.deck.Deck;
import forge.deck.DeckManager;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.ItemPoolView;

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

    private int numCommons = 0;
    private int numUncommons = 0;
    // these two used when numbers of rares/myths is specified explicitly
    private int numRares = 0;
    private int numMythics = 0;
    // this is used to specify number of slots for a rare - generator will decide by itself which to take
    private int numRareSlots = 0;
    
    private int numSpecials = 0;

    //private Random r  = new Random();

    /**
     * <p>Constructor for BoosterGenerator.</p>
     */
    public BoosterGenerator(Iterable<CardPrinted> cards) {
        numCommons = 11;
        numUncommons = 3;
        numRareSlots = 1;
        numSpecials = 0;

        for (CardPrinted c : cards) {
            addToRarity(c);
        }
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
        numSpecials = nSpecials;
        numMythics = nMythics;

        //DeckManager dio = new DeckManager(ForgeProps.getFile(NewConstants.NEW_DECKS));
        DeckManager dio = AllZone.getDeckManager();
        Deck dPool = dio.getDeck(deckFile);
        if (dPool == null) {
            throw new RuntimeException("BoosterGenerator : deck not found - " + deckFile);
        }

        ItemPoolView<CardPrinted> tList = dPool.getMain();
        for (Entry<CardPrinted, Integer> e : tList) {
            if (ignoreRarity) { commons.add(e.getKey()); }
            else { addToRarity(e.getKey()); }
        }

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
        numSpecials = 0;

        List<String> setsList = Arrays.asList(new String[]{setCode});
        Predicate<CardPrinted> filter = CardPrinted.Predicates.printedInSets(setsList, true);
        List<CardPrinted> cardsInThisSet = filter.select(CardDb.instance().getAllCards());

        for (CardPrinted c : cardsInThisSet) {
            addToRarity(c);
        }

        ArrayList<String> bpData = FileUtil.readFile("res/boosterdata/" + setCode + ".pack");

        for (String line : bpData) {
            if (line.startsWith("Commons:")) {
                numCommons = Integer.parseInt(line.substring(8));
            } else if (line.startsWith("Uncommons:")) {
                numUncommons = Integer.parseInt(line.substring(10));
            } else if (line.startsWith("Rares:")) {
                numRareSlots = Integer.parseInt(line.substring(6));
            } else if (line.startsWith("Specials:")) {
                numSpecials = Integer.parseInt(line.substring(9));
            }

        }

        if (Constant.Runtime.DevMode[0]) {
            System.out.println("numCommons: " + numCommons);
            System.out.println("numUncommons: " + numUncommons);
            System.out.println("numRares: " + numRares);
            System.out.println("numSpecials: " + numSpecials);
        }

    }

    private List<CardPrinted> pickRandomCards(List<CardPrinted> source, int count)
    {
        List<CardPrinted> result = new ArrayList<CardPrinted>(count);
        if (count <= 0 || source == null || source.isEmpty()) { return result; } 
        
        int listSize = source.size();
        int index = Integer.MAX_VALUE;
        for (int iCard = 0; iCard < count; iCard++) {
            if (index >= listSize) {
                Collections.shuffle(source, MyRandom.random);
                index = 0;
            }
            result.add(source.get(index));
            index++;
        }
        return result;
    }

    private List<CardPrinted> pickRandomRaresOrMythics(List<CardPrinted> rares, List<CardPrinted> mythics, int count)
    {
        List<CardPrinted> result = new ArrayList<CardPrinted>(count);
        int raresSize = rares == null ? 0 : rares.size();
        int mythicsSize = mythics == null ? 0 : mythics.size();
        if (count <= 0 || raresSize == 0) { return result; } 
        
        int indexRares = Integer.MAX_VALUE;
        int indexMythics = Integer.MAX_VALUE;
        for (int iCard = 0; iCard < count; iCard++) {
            boolean takeMythic = mythicsSize > 0 && MyRandom.random.nextInt(8) <= 1;
            if (takeMythic) {
                if (indexRares >= raresSize) {
                    Collections.shuffle(mythics, MyRandom.random);
                    indexMythics = 0;
                }
                result.add(mythics.get(indexMythics));
                indexMythics++;
            }
            else 
            {
                if (indexRares >= raresSize) {
                    Collections.shuffle(rares, MyRandom.random);
                    indexRares = 0;
                }
                result.add(rares.get(indexRares));
                indexRares++;
            }
        }
        return result;
    }    

    /**
     * <p>getBoosterPack.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public final List<CardPrinted> getBoosterPack() {
        List<CardPrinted> temp = new ArrayList<CardPrinted>();

        temp.addAll(pickRandomCards(commons, numCommons));
        temp.addAll(pickRandomCards(uncommons, numUncommons));
        // You can specify number of rare-slots or number of rares and mythics explicitly... or both, but they'll sum up
        if (numRareSlots > 0) {
            temp.addAll(pickRandomRaresOrMythics(rares, mythics, numRareSlots));
        }
        if (numRares > 0 || numMythics > 0) {
            temp.addAll(pickRandomCards(rares, numRares));
            temp.addAll(pickRandomCards(mythics, numMythics));
        }
        
        temp.addAll(pickRandomCards(specials, numSpecials));

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
