package forge.card;


import forge.Constant;
import forge.MyRandom;
import forge.error.ErrorViewer;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.ItemPool;
import forge.item.ItemPoolView;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.slightlymagic.maxmtg.Predicate;


/**
 * <p>ReadDraftBoosterPack class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class ReadDraftBoosterPack implements NewConstants {

    /** Constant <code>comment="//"</code>. */
    private static final String comment = "//";

    private List<CardPrinted> commonCreatureList = new ArrayList<CardPrinted>();
    private List<CardPrinted> commonNonCreatureList = new ArrayList<CardPrinted>();

    private List<CardPrinted> commonList = new ArrayList<CardPrinted>();
    private List<CardPrinted> uncommonList = new ArrayList<CardPrinted>();
    private List<CardPrinted> rareList = new ArrayList<CardPrinted>();

    /**
     * <p>Constructor for ReadDraftBoosterPack.</p>
     */
    public ReadDraftBoosterPack() {
        setup();
    }

    /**
     * <p>getBoosterPack.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public final ItemPoolView<CardPrinted> getBoosterPack() {
        ItemPool<CardPrinted> pack = new ItemPool<CardPrinted>(CardPrinted.class);

        pack.add(getRandomCard(rareList));

        for (int i = 0; i < 3; i++)
            pack.add(getRandomCard(uncommonList));

        //11 commons, 7 creature 4 noncreature
        List<CardPrinted> variety;
        for (int i = 0; i < 7; i++) {
            variety = getVariety(commonCreatureList);
            pack.add(getRandomCard(variety));
        }

        for (int i = 0; i < 4; i++) {
            variety = getVariety(commonNonCreatureList);
            pack.add(getRandomCard(variety));
        }

        if (pack.countAll() != 15)
            throw new RuntimeException("ReadDraftBoosterPack : getBoosterPack() error, pack is not 15 cards - "
                    + pack.countAll());

        return pack;
    }

    //return CardList of 5 or 6 cards, one for each color and maybe an artifact
    private List<CardPrinted> getVariety(final List<CardPrinted> in) {
        List<CardPrinted> out = new ArrayList<CardPrinted>();
        Collections.shuffle(in, MyRandom.random);

        for (int i = 0; i < Constant.Color.Colors.length; i++) {
            CardPrinted check = findColor(in, i);
            if (check != null) { out.add(check); }
        }

        return out;
    } //getVariety()

    private static CardPrinted findColor(final List<CardPrinted> in, final int color) {
        Predicate<CardRules> filter = null;
        switch (color) {
            case 0: filter = CardRules.Predicates.Presets.isWhite; break;
            case 1: filter = CardRules.Predicates.Presets.isBlue; break;
            case 2: filter = CardRules.Predicates.Presets.isBlack; break;
            case 3: filter = CardRules.Predicates.Presets.isRed; break;
            case 4: filter = CardRules.Predicates.Presets.isGreen; break;
            case 5: filter = CardRules.Predicates.Presets.isColorless; break;
            default: break;
        }
        if (null == filter) { return null; }
        return filter.first(in, CardPrinted.fnGetRules);
    }



    private static CardPrinted getRandomCard(final List<CardPrinted> list) {
        for (int i = 0; i < 10; i++) {
            Collections.shuffle(list, MyRandom.random);
        }
        int index = MyRandom.random.nextInt(list.size());
        return list.get(index);
    } //getRandomCard()


    /**
     * <p>setup.</p>
     */
    private void setup() {
        CardDb db = CardDb.instance();
        commonList = db.getCards(readFile(ForgeProps.getFile(DRAFT.COMMON)));
        uncommonList = db.getCards(readFile(ForgeProps.getFile(DRAFT.UNCOMMON)));
        rareList = db.getCards(readFile(ForgeProps.getFile(DRAFT.RARE)));

        System.out.println("commonList size:" + commonList.size());
        System.out.println("ucommonList size:" + uncommonList.size());
        System.out.println("rareList size:" + rareList.size());

        CardRules.Predicates.Presets.isCreature.split(commonList, CardPrinted.fnGetRules,
                commonCreatureList, commonNonCreatureList);

    } //setup()

    private List<String> readFile(final File file) {
        List<String> cardList = new ArrayList<String>();

        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader(file));
            String line = in.readLine();

            //stop reading if end of file or blank line is read
            while (line != null && (line.trim().length() != 0)) {
                if (!line.startsWith(comment)) {
                    cardList.add(line.trim());
                }

                line = in.readLine();
            }

        } catch (Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("ReadDraftBoosterPack : readFile error, " + ex);
        }

        return cardList;
    } //readFile()
}
