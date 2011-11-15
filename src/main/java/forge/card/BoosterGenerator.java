package forge.card;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import net.slightlymagic.braids.util.lambda.Lambda1;
import net.slightlymagic.maxmtg.Closure1;
import net.slightlymagic.maxmtg.Predicate;
import forge.MyRandom;
import forge.deck.Deck;
import forge.item.CardDb;
import forge.item.CardPrinted;

/**
 * <p>
 * BoosterGenerator class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class BoosterGenerator {

    // Function to open a booster as it is.
    /** The Constant IDENTITY_PICK. */
    public static final Lambda1<List<CardPrinted>, BoosterGenerator> IDENTITY_PICK = new Lambda1<List<CardPrinted>, BoosterGenerator>() {
        @Override
        public List<CardPrinted> apply(final BoosterGenerator arg1) {
            return arg1.getBoosterPack();
        }
    };

    // Closure which will hold both the booster and the way we want to pick from
    // it - holds default options
    /**
     * Gets the simple picker.
     * 
     * @param source
     *            the source
     * @return the simple picker
     */
    public static Closure1<List<CardPrinted>, BoosterGenerator> getSimplePicker(final BoosterGenerator source) {
        return new Closure1<List<CardPrinted>, BoosterGenerator>(BoosterGenerator.IDENTITY_PICK, source);
    }

    // These lists are to hold cards grouped by rarity in advance.
    private final List<CardPrinted> basicLands = new ArrayList<CardPrinted>();
    private final List<CardPrinted> allButLands = new ArrayList<CardPrinted>();
    private final List<CardPrinted> commons = new ArrayList<CardPrinted>();
    private final List<CardPrinted> uncommons = new ArrayList<CardPrinted>();
    private final List<CardPrinted> rares = new ArrayList<CardPrinted>();
    private final List<CardPrinted> mythics = new ArrayList<CardPrinted>();
    private final List<CardPrinted> specials = new ArrayList<CardPrinted>();
    private final List<CardPrinted> doubleFacedCommons = new ArrayList<CardPrinted>();
    private final List<CardPrinted> doubleFacedUncommons = new ArrayList<CardPrinted>();
    private final List<CardPrinted> doubleFacedRares = new ArrayList<CardPrinted>();
    private final List<CardPrinted> doubleFacedMythics = new ArrayList<CardPrinted>();

    // private List<CardPrinted> commonCreatures;
    // private List<CardPrinted> commonNonCreatures;

    private static final List<CardPrinted> EMPTY_LIST = Collections.unmodifiableList(new ArrayList<CardPrinted>(0));

    // Modern boosters contain 10 commons, 3 uncommmons, 1 rare/mythic
    // They also contain 1 land and 1 token/rules, but we don't pick them now.
    private int numCommons = 10;
    private int numUncommons = 3;
    private int numRareSlots = 1;
    private int numDoubleFaced = 0;
    private int numSpecials = 0;

    /**
     * <p>
     * Constructor for BoosterGenerator.
     * </p>
     * 
     * @param cards
     *            the cards
     */
    public BoosterGenerator(final Iterable<CardPrinted> cards) {
        for (final CardPrinted c : cards) {
            this.addToRarity(c);
        }
    }

    /**
     * Instantiates a new booster generator.
     * 
     * @param dPool
     *            the d pool
     */
    public BoosterGenerator(final Deck dPool) {
        for (final Entry<CardPrinted, Integer> e : dPool.getMain()) {
            this.addToRarity(e.getKey());
        }
    }

    /**
     * <p>
     * Constructor for BoosterGenerator.
     * </p>
     * 
     * @param cardSet
     *            the card set
     */
    public BoosterGenerator(final CardSet cardSet) {
        if (!cardSet.canGenerateBooster()) {
            throw new InvalidParameterException("BoosterGenerator: Set " + cardSet + " cannot generate boosters!");
        }
        final CardSet.BoosterData bs = cardSet.getBoosterData();

        this.numCommons = bs.getCommon();
        this.numUncommons = bs.getUncommon();
        this.numRareSlots = bs.getRare();
        this.numSpecials = bs.getSpecial();
        this.numDoubleFaced = bs.getDoubleFaced();

        final Predicate<CardPrinted> filter = CardPrinted.Predicates.printedInSets(cardSet.getCode());
        final List<CardPrinted> cardsInThisSet = filter.select(CardDb.instance().getAllCards());

        for (final CardPrinted c : cardsInThisSet) {
            this.addToRarity(c);
            // System.out.println(c);
        }
        // System.out.println("done");
    }

    private List<CardPrinted> pickRandomCards(final List<CardPrinted> source, final int count) {
        return this.pickRandomCards(source, count, false);
    }

    private List<CardPrinted> pickRandomCards(final List<CardPrinted> source, final int count, final boolean singleton) {
        int listSize = source == null ? 0 : source.size();
        if ((count <= 0) || (listSize == 0)) {
            return BoosterGenerator.EMPTY_LIST;
        }
        final List<CardPrinted> result = new ArrayList<CardPrinted>(count);

        int index = Integer.MAX_VALUE;
        for (int iCard = 0; iCard < count; iCard++) {
            if (index >= listSize) {
                Collections.shuffle(source, MyRandom.getRandom());
                index = 0;
            }
            result.add(source.get(index));

            if (!singleton) {
                index++;
            } else {
                source.remove(index);
                listSize--;
            }
        }
        return result;
    }

    private List<CardPrinted> pickRandomRaresOrMythics(final List<CardPrinted> rares, final List<CardPrinted> mythics,
            final int count) {
        final int raresSize = rares == null ? 0 : rares.size();
        final int mythicsSize = mythics == null ? 0 : mythics.size();
        if ((count <= 0) || (raresSize == 0)) {
            return BoosterGenerator.EMPTY_LIST;
        }

        final List<CardPrinted> result = new ArrayList<CardPrinted>(count);

        int indexRares = Integer.MAX_VALUE;
        int indexMythics = Integer.MAX_VALUE;
        for (int iCard = 0; iCard < count; iCard++) {
            final int rollD8 = MyRandom.getRandom().nextInt(8);
            final boolean takeMythic = (mythicsSize > 0) && (rollD8 < 1);
            if (takeMythic) {
                if (indexRares >= raresSize) {
                    Collections.shuffle(mythics, MyRandom.getRandom());
                    indexMythics = 0;
                }
                result.add(mythics.get(indexMythics));
                indexMythics++;
            } else {
                if (indexRares >= raresSize) {
                    Collections.shuffle(rares, MyRandom.getRandom());
                    indexRares = 0;
                }
                result.add(rares.get(indexRares));
                indexRares++;
            }
        }
        return result;
    }

    /**
     * Gets the booster pack.
     * 
     * @return the booster pack
     */
    public final List<CardPrinted> getBoosterPack() {
        return this.getBoosterPack(this.numCommons, this.numUncommons, this.numRareSlots, 0, 0, this.numSpecials,
                this.numDoubleFaced, 0, 0);
    }

    /**
     * Gets the singleton booster pack.
     * 
     * @param nAnyCard
     *            the n any card
     * @return the singleton booster pack
     */
    public final List<CardPrinted> getSingletonBoosterPack(final int nAnyCard) {
        final List<CardPrinted> temp = new ArrayList<CardPrinted>();

        temp.addAll(this.pickRandomCards(this.allButLands, nAnyCard, true));

        return temp;
    }

    /**
     * So many parameters are needed for custom limited cardpools,.
     * 
     * @param nCom
     *            the n com
     * @param nUnc
     *            the n unc
     * @param nRareSlots
     *            the n rare slots
     * @param nRares
     *            the n rares
     * @param nMythics
     *            the n mythics
     * @param nSpecs
     *            the n specs
     * @param nDoubls
     *            the n doubls
     * @param nAnyCard
     *            the n any card
     * @param nLands
     *            the n lands
     * @return the booster pack
     */
    public final List<CardPrinted> getBoosterPack(final int nCom, final int nUnc, final int nRareSlots,
            final int nRares, final int nMythics, final int nSpecs, final int nDoubls, final int nAnyCard,
            final int nLands) {

        final List<CardPrinted> temp = new ArrayList<CardPrinted>();

        temp.addAll(this.pickRandomCards(this.commons, nCom));
        /*
         * if( nComCreat > 0 || nComNonCr > 0) { if
         * (commonNonCreatures.isEmpty()) {
         * CardRules.Predicates.Presets.isCreature.split(commons,
         * CardPrinted.fnGetRules, commonCreatures, commonNonCreatures); }
         * temp.addAll(pickRandomCards(commonCreatures, nComCreat));
         * temp.addAll(pickRandomCards(commonNonCreatures, nComNonCr)); }
         */

        temp.addAll(this.pickRandomCards(this.uncommons, nUnc));

        if (nRareSlots > 0) {
            temp.addAll(this.pickRandomRaresOrMythics(this.rares, this.mythics, nRareSlots));
        }
        if ((nRares > 0) || (nMythics > 0)) {
            temp.addAll(this.pickRandomCards(this.rares, nRares));
            temp.addAll(this.pickRandomCards(this.mythics, nMythics));
        }
        if (nDoubls > 0) {
            final int dblFacedRarity = MyRandom.getRandom().nextInt(14);
            List<CardPrinted> listToUse;
            if (dblFacedRarity < 9) { // Common
                listToUse = this.doubleFacedCommons;
            } else if (dblFacedRarity < 13) { // Uncommon
                listToUse = this.doubleFacedUncommons;
            } else { // Rare or Mythic
                if (MyRandom.getRandom().nextInt(8) == 0) {
                    listToUse = this.doubleFacedMythics;
                } else {
                    listToUse = this.doubleFacedRares;
                }
            }
            temp.addAll(this.pickRandomCards(listToUse, nDoubls));
        }

        temp.addAll(this.pickRandomCards(this.specials, nSpecs));

        temp.addAll(this.pickRandomCards(this.allButLands, nAnyCard));

        temp.addAll(this.pickRandomCards(this.basicLands, nLands));

        return temp;
    }

    private void addToRarity(final CardPrinted c) {
        if (c.getCard().isAltState()) {
            return;
        }
        if (c.getCard().isDoubleFaced() && (this.numDoubleFaced > 0)) {
            switch (c.getRarity()) {
            case Common:
                this.doubleFacedCommons.add(c);
                break;
            case Uncommon:
                this.doubleFacedUncommons.add(c);
                break;
            case Rare:
                this.doubleFacedRares.add(c);
                break;
            case MythicRare:
                this.doubleFacedMythics.add(c);
                break;
            default:
                break;
            }
        } else {
            switch (c.getRarity()) {
            case Common:
                this.commons.add(c);
                break;
            case Uncommon:
                this.uncommons.add(c);
                break;
            case Rare:
                this.rares.add(c);
                break;
            case MythicRare:
                this.mythics.add(c);
                break;
            case Special:
                this.specials.add(c);
                break;
            default:
                break;
            }
        }

        if (c.getCard().getType().isBasicLand()) {
            this.basicLands.add(c);
        } else {
            this.allButLands.add(c);
        }
    }

}
