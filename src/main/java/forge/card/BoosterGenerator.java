package forge.card;

import forge.MyRandom;
import forge.deck.Deck;
import forge.item.CardDb;
import forge.item.CardPrinted;

import java.security.InvalidParameterException;
import java.util.ArrayList;
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
    private final List<CardPrinted> basicLands = new ArrayList<CardPrinted>();
    private final List<CardPrinted> allButLands = new ArrayList<CardPrinted>();
    private final List<CardPrinted> commons = new ArrayList<CardPrinted>();
    private final List<CardPrinted> uncommons = new ArrayList<CardPrinted>();
    private final List<CardPrinted> rares = new ArrayList<CardPrinted>();
    private final List<CardPrinted> mythics = new ArrayList<CardPrinted>();
    private final List<CardPrinted> specials = new ArrayList<CardPrinted>();

    //private List<CardPrinted> commonCreatures;
    //private List<CardPrinted> commonNonCreatures;

    private static final List<CardPrinted> emptyList = Collections.unmodifiableList( new ArrayList<CardPrinted>(0) ); 

    // Modern boosters contain 10 commons, 3 uncommmons, 1 rare/mythic
    // They also contain 1 land and 1 token/rules, but we don't pick them now.
    private int numCommons = 10;
    private int numUncommons = 3;
    private int numRareSlots = 1;
    private int numSpecials = 0;

    /**
     * <p>Constructor for BoosterGenerator.</p>
     */
    public BoosterGenerator(Iterable<CardPrinted> cards) {
        for (CardPrinted c : cards) {
            addToRarity(c);
        }
    }

    public BoosterGenerator(Deck dPool)
    {
        for (Entry<CardPrinted, Integer> e : dPool.getMain()) { addToRarity(e.getKey()); }
    }

    /**
     * <p>Constructor for BoosterGenerator.</p>
     *
     * @param setCode a {@link java.lang.String} object.
     */
    public BoosterGenerator(final CardSet cardSet) {
        if (!cardSet.canGenerateBooster()) {
            throw new InvalidParameterException("BoosterGenerator: Set " + cardSet + " cannot generate boosters!");
        }
        CardSet.BoosterData bs = cardSet.getBoosterData();
        
        numCommons = bs.getCommon();
        numUncommons = bs.getUncommon();
        numRareSlots = bs.getRare();
        numSpecials = bs.getSpecial();

        Predicate<CardPrinted> filter = CardPrinted.Predicates.printedInSets(cardSet.getCode());
        List<CardPrinted> cardsInThisSet = filter.select(CardDb.instance().getAllCards());

        for (CardPrinted c : cardsInThisSet) {
            addToRarity(c);
            //System.out.println(c);
        }
        //System.out.println("done");
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
            int rollD8 = MyRandom.random.nextInt(8);
            boolean takeMythic = mythicsSize > 0 && rollD8 < 1;
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
        return getBoosterPack(numCommons, numUncommons, numRareSlots, 0, 0, numSpecials, 0, 0);
    }
    /**
     * So many parameters needed for custom limited cardpools, 
     */
    public final List<CardPrinted> getBoosterPack(final int nCom, final int nUnc, final int nRareSlots,
            final int nRares, final int nMythics, final int nSpecs, final int nAnyCard, final int nLands) {
        
        List<CardPrinted> temp = new ArrayList<CardPrinted>();

        temp.addAll(pickRandomCards(commons, nCom));
        /*
        if( nComCreat > 0 || nComNonCr > 0) {
            if (commonNonCreatures.isEmpty()) { 
                CardRules.Predicates.Presets.isCreature.split(commons, CardPrinted.fnGetRules, commonCreatures, commonNonCreatures);
            }
            temp.addAll(pickRandomCards(commonCreatures, nComCreat));
            temp.addAll(pickRandomCards(commonNonCreatures, nComNonCr));
        }
        */
        
        temp.addAll(pickRandomCards(uncommons, nUnc));
        
        if (nRareSlots > 0) {
            temp.addAll(pickRandomRaresOrMythics(rares, mythics, nRareSlots));
        }
        if (nRares > 0 || nMythics > 0) {
            temp.addAll(pickRandomCards(rares, nRares));
            temp.addAll(pickRandomCards(mythics, nMythics));
        }
        
        temp.addAll(pickRandomCards(specials, nSpecs));
        
        temp.addAll(pickRandomCards(allButLands, nAnyCard));
        
        temp.addAll(pickRandomCards(basicLands, nLands));

        return temp;
    }

    private void addToRarity(final CardPrinted c) {
        switch(c.getRarity()) {
            case Common: commons.add(c); break;
            case Uncommon: uncommons.add(c); break;
            case Rare: rares.add(c); break;
            case MythicRare: mythics.add(c); break;
            case Special: specials.add(c); break;
        }

        if (c.getCard().getType().isBasicLand()) {
            basicLands.add(c);
        } else {
            allButLands.add(c);
        }
    }

}
