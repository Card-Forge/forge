package forge.card;

import forge.Constant;
import forge.FileUtil;
import forge.MyRandom;
import forge.deck.Deck;
import forge.item.CardDb;
import forge.item.CardPrinted;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import net.slightlymagic.braids.util.lambda.Lambda1;
import net.slightlymagic.maxmtg.Closure1;
import net.slightlymagic.maxmtg.Predicate;

/**
 * <p>BoosterGenerator class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class BoosterGenerator {

    // Function to open a booster as it is.
    public static final Lambda1<List<CardPrinted>, BoosterGenerator> IDENTITY_PICK = new Lambda1<List<CardPrinted>, BoosterGenerator>() {
        @Override public List<CardPrinted> apply(BoosterGenerator arg1) {
            return arg1.getBoosterPack();
        }
    };
    // Closure which will hold both the booster and the way we want to pick from it - holds default options 
    public static Closure1<List<CardPrinted>, BoosterGenerator> getSimplePicker(BoosterGenerator source) { 
        return new Closure1<List<CardPrinted>, BoosterGenerator>(IDENTITY_PICK, source);
    }
    
    // These lists are to hold cards grouped by rarity in advance.
    private final List<CardPrinted> allNoLands = new ArrayList<CardPrinted>();
    private final List<CardPrinted> commons = new ArrayList<CardPrinted>();
    private final List<CardPrinted> uncommons = new ArrayList<CardPrinted>();
    private final List<CardPrinted> rares = new ArrayList<CardPrinted>();
    private final List<CardPrinted> mythics = new ArrayList<CardPrinted>();
    private final List<CardPrinted> specials = new ArrayList<CardPrinted>();

    private List<CardPrinted> commonCreatures;
    private List<CardPrinted> commonNonCreatures;

    private static final List<CardPrinted> emptyList = Collections.unmodifiableList( new ArrayList<CardPrinted>(0) ); 

    // This set of cards 
    private int numCommons = 0;
    private int numUncommons = 0;
    private int numRareSlots = 0;
    private int numSpecials = 0;

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

    public BoosterGenerator(Deck dPool)
    {
        /*
        //DeckManager dio = new DeckManager(ForgeProps.getFile(NewConstants.NEW_DECKS));
        DeckManager dio = AllZone.getDeckManager();
        Deck dPool = dio.getDeck(deckFile);
        if (dPool == null) {
            throw new RuntimeException("BoosterGenerator : deck not found - " + deckFile);
        }*/

        for (Entry<CardPrinted, Integer> e : dPool.getMain()) { addToRarity(e.getKey()); }
    }

    /**
     * <p>Constructor for BoosterGenerator.</p>
     *
     * @param setCode a {@link java.lang.String} object.
     */
    public BoosterGenerator(final String setCode) {
        numCommons = 0;
        numUncommons = 0;
        numRareSlots = 0;
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
            System.out.println("numRares: " + numRareSlots);
            System.out.println("numSpecials: " + numSpecials);
        }

    }

    private List<CardPrinted> pickRandomCards(List<CardPrinted> source, int count)
    {
        int listSize = source == null ? 0 : source.size();
        if (count <= 0 || listSize == 0) { return emptyList; } 
        List<CardPrinted> result = new ArrayList<CardPrinted>(count);
        
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
        int raresSize = rares == null ? 0 : rares.size();
        int mythicsSize = mythics == null ? 0 : mythics.size();
        if (count <= 0 || raresSize == 0) { return emptyList; } 
        
        List<CardPrinted> result = new ArrayList<CardPrinted>(count);
        
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

    
    public final List<CardPrinted> getBoosterPack() {
        return getBoosterPack(numCommons, 0, 0, numUncommons, numRareSlots, 0, 0, numSpecials, 0);
    }
    /**
     * <p>getBoosterPack.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public final List<CardPrinted> getBoosterPack(final int nCom, final int nComCreat, final int nComNonCr, final int nUnc,
            final int nRareSlots, final int nRares, final int nMythics, final int nSpecs, final int nAnyCard) {
        
        List<CardPrinted> temp = new ArrayList<CardPrinted>();

        temp.addAll(pickRandomCards(commons, nCom));
        if( nComCreat > 0 || nComNonCr > 0) {
            if (commonNonCreatures.isEmpty()) { 
                CardRules.Predicates.Presets.isCreature.split(commons, CardPrinted.fnGetRules, commonCreatures, commonNonCreatures);
            }
            temp.addAll(pickRandomCards(commonCreatures, nComCreat));
            temp.addAll(pickRandomCards(commonNonCreatures, nComNonCr));
        }
        
        temp.addAll(pickRandomCards(uncommons, nUnc));
        
        if (nRareSlots > 0) {
            temp.addAll(pickRandomRaresOrMythics(rares, mythics, nRareSlots));
        }
        if (nRares > 0 || nMythics > 0) {
            temp.addAll(pickRandomCards(rares, nRares));
            temp.addAll(pickRandomCards(mythics, nMythics));
        }
        
        temp.addAll(pickRandomCards(specials, nSpecs));
        
        temp.addAll(pickRandomCards(allNoLands, nAnyCard));

        return temp;
    }

    /**
     * <p>getBoosterPackSize.</p>
     *
     * @return a int.
     */
    public final int getBoosterPackSize() {
        return numCommons + numUncommons + numRareSlots + numSpecials;
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
        if (!c.getCard().getType().isBasicLand()) {
            allNoLands.add(c);
        }
    }

}
