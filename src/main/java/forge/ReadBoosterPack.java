package forge;


//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;

import forge.card.CardRules;
import forge.card.CardDb;
import forge.card.CardPool;
import forge.card.CardPoolView;
import forge.card.CardPrinted;
import forge.properties.NewConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.slightlymagic.maxmtg.Predicate;


/**
 * <p>ReadBoosterPack class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class ReadBoosterPack implements NewConstants {

    
    //private List<CardPrinted> mythics;
    private List<CardPrinted> rares;
    private List<CardPrinted> uncommons;
    private List<CardPrinted> commons;
    
    private List<CardPrinted> commonCreatures;
    private List<CardPrinted> commonNonCreatures;
    /**
     * <p>Constructor for ReadBoosterPack.</p>
     */
    public ReadBoosterPack() {
        //mythics = CardPrinted.Predicates.Presets.isMythicRare.select(CardDb.instance().getAllUniqueCards()); 
        rares = CardPrinted.Predicates.Presets.isRare.select(CardDb.instance().getAllUniqueCards());
        commons = CardPrinted.Predicates.Presets.isCommon.select(CardDb.instance().getAllUniqueCards());
        uncommons = CardPrinted.Predicates.Presets.isUncommon.select(CardDb.instance().getAllUniqueCards());

        commonCreatures = new ArrayList<CardPrinted>();
        commonNonCreatures = new ArrayList<CardPrinted>();
        CardRules.Predicates.Presets.isCreature.split(commons, CardPrinted.fnGetRules, commonCreatures, commonNonCreatures);
    }

    /**
     * <p>getBoosterPack5.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardPoolView<CardPrinted> getBoosterPack5() {
        CardPool<CardPrinted> list = new CardPool<CardPrinted>();
        for (int i = 0; i < 5; i++) { list.addAll(getBoosterPack()); }

        addBasicLands(list, 20);
        addBasicSnowLands(list, 20);

        for (int i = 0; i < 4; i++)
            list.add(CardDb.instance().getCard("Terramorphic Expanse", "M10"));

        return list;
    }//getBoosterPack5()

    public static final void addBasicLands(final CardPool<CardPrinted> pool, final int count) {
        for (int i = 0; i < count; i++) {
            pool.add(CardDb.instance().getCard("Forest", "M10"));
            pool.add(CardDb.instance().getCard("Island", "M10"));
            pool.add(CardDb.instance().getCard("Plains", "M10"));
            pool.add(CardDb.instance().getCard("Mountain", "M10"));
            pool.add(CardDb.instance().getCard("Swamp", "M10"));
        }
    }
    public static final void addBasicSnowLands(final CardPool<CardPrinted> pool, final int count) {
        for (int i = 0; i < count; i++) {
            pool.add(CardDb.instance().getCard("Snow-Covered Forest", "ICE"));
            pool.add(CardDb.instance().getCard("Snow-Covered Island", "ICE"));
            pool.add(CardDb.instance().getCard("Snow-Covered Plains", "ICE"));
            pool.add(CardDb.instance().getCard("Snow-Covered Mountain", "ICE"));
            pool.add(CardDb.instance().getCard("Snow-Covered Swamp", "ICE"));
        }
    }
    
    /**
     * <p>getBoosterPack.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardPoolView<CardPrinted> getBoosterPack() {
        CardPool<CardPrinted> pack = new CardPool<CardPrinted>();

        pack.add(getRandomCard(rares));

        for (int i = 0; i < 3; i++)
            pack.add(getRandomCard(uncommons));

        //11 commons, 7 creature 4 noncreature
        List<CardPrinted> variety;
        for (int i = 0; i < 7; i++) {
            variety = getVariety(commonCreatures);
            pack.add(getRandomCard(variety));
        }

        for (int i = 0; i < 4; i++) {
            variety = getVariety(commonNonCreatures);
            pack.add(getRandomCard(variety));
        }

        if (pack.countAll() != 15)
            throw new RuntimeException("ReadBoosterPack : getBoosterPack() error, pack is not 15 card - "
                    + pack.countAll());

        return pack;
    }

    /**
     * <p>getShopCards.</p>
     *
     * @param numberWins a int.
     * @param questLevel a int.
     * @return a {@link forge.CardList} object.
     */
    public CardPoolView<CardPrinted> getShopCards(int totalPacks) {
        CardPool<CardPrinted> list = new CardPool<CardPrinted>();

        // Number of Packs granted


        for (int i = 0; i < totalPacks; i++) {
            // TODO: Balance CardPool Availability
            // Each "Pack" yields 1 Rare, 3 Uncommon, 7 Commons
            list.add(getRandomCard(rares));
            for (int j = 0; j < 7; j++) {
                if (j < 3)
                    list.add(getRandomCard(uncommons));

                list.add(getRandomCard(commons));
            }
        }

        addBasicLands(list, 10);
        addBasicSnowLands(list, 5);

        return list;
    }

    //return CardList of 5 or 6 cards, one for each color and maybe an artifact
    private List<CardPrinted> getVariety(List<CardPrinted> in) {
        List<CardPrinted> out = new ArrayList<CardPrinted>();
        Collections.shuffle(in, MyRandom.random);

        for (int i = 0; i < Constant.Color.Colors.length; i++) {
            CardPrinted check = findCardOfColor(in, i);
            if (check != null) { out.add(check); }
        }

        return out;
    }//getVariety()

    private CardPrinted findCardOfColor(final List<CardPrinted> in, final int color) {
        Predicate<CardRules> filter = CardRules.Predicates.Presets.colors.get(color);
        if (null == filter) { return null; }
        return filter.first(in, CardPrinted.fnGetRules);
    }

    private CardPrinted getRandomCard(final List<CardPrinted> list) {
        Collections.shuffle(list, MyRandom.random);
        int index = MyRandom.random.nextInt(list.size());
        Collections.shuffle(list, MyRandom.random);
        return list.get(index);
    }//getRandomCard()

}

