package forge.game.limited;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import net.slightlymagic.braids.util.lambda.Lambda1;
import net.slightlymagic.maxmtg.Closure1;
import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardListUtil;
import forge.Constant;
import forge.FileUtil;
import forge.MyRandom;
import forge.SetUtils;
import forge.card.BoosterGenerator;
import forge.card.CardBlock;
import forge.card.CardSet;
import forge.card.spellability.AbilityMana;
import forge.deck.Deck;
import forge.deck.DeckManager;
import forge.game.GameType;
import forge.gui.GuiUtils;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.ItemPool;

/**
 * <p>
 * SealedDeck class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 * @since 1.0.15
 */
public class SealedDeck {
    private final List<Closure1<List<CardPrinted>, BoosterGenerator>> packs = new ArrayList<Closure1<List<CardPrinted>, BoosterGenerator>>();

    /** The Land set code. */
    private String[] landSetCode = { "" };

    /**
     * <p>
     * Constructor for SealedDeck.
     * </p>
     * 
     * @param sealedType
     *            a {@link java.lang.String} object.
     */
    public SealedDeck(final String sealedType) {

        if (sealedType.equals("Full")) {
            final BoosterGenerator bpFull = new BoosterGenerator(CardDb.instance().getAllUniqueCards());
            final Closure1<List<CardPrinted>, BoosterGenerator> picker = BoosterGenerator.getSimplePicker(bpFull);
            for (int i = 0; i < 6; i++) {
                this.packs.add(picker);
            }

            this.getLandSetCode()[0] = CardDb.instance().getCard("Plains").getSet();
        } else if (sealedType.equals("Block")) {

            final Object o = GuiUtils.getChoice("Choose Block", SetUtils.getBlocks().toArray());
            final CardBlock block = (CardBlock) o;

            final CardSet[] cardSets = block.getSets();
            final String[] sets = new String[cardSets.length];
            for (int k = cardSets.length - 1; k >= 0; --k) {
                sets[k] = cardSets[k].getCode();
            }

            final int nPacks = block.getCntBoostersSealed();

            final List<String> setCombos = new ArrayList<String>();
            if (sets.length >= 2) {
                setCombos.add(String.format("%s/%s/%s/%s/%s/%s", sets[0], sets[0], sets[0], sets[0], sets[0], sets[0]));
                setCombos.add(String.format("%s/%s/%s/%s/%s/%s", sets[1], sets[1], sets[0], sets[0], sets[0], sets[0]));
                setCombos.add(String.format("%s/%s/%s/%s/%s/%s", sets[1], sets[1], sets[1], sets[0], sets[0], sets[0]));
            }
            if (sets.length >= 3) {
                setCombos.add(String.format("%s/%s/%s/%s/%s/%s", sets[2], sets[2], sets[2], sets[0], sets[0], sets[0]));
                setCombos.add(String.format("%s/%s/%s/%s/%s/%s", sets[2], sets[2], sets[1], sets[1], sets[0], sets[0]));
            }

            if (sets.length > 1) {
                final Object p = GuiUtils.getChoice("Choose Set Combination", setCombos.toArray());

                final String[] pp = p.toString().split("/");
                for (int i = 0; i < nPacks; i++) {
                    final BoosterGenerator bpMulti = new BoosterGenerator(SetUtils.getSetByCode(pp[i]));
                    this.packs.add(BoosterGenerator.getSimplePicker(bpMulti));
                }
            } else {
                final BoosterGenerator bpOne = new BoosterGenerator(SetUtils.getSetByCode(sets[0]));
                final Closure1<List<CardPrinted>, BoosterGenerator> picker = BoosterGenerator.getSimplePicker(bpOne);
                for (int i = 0; i < nPacks; i++) {
                    this.packs.add(picker);
                }
            }

            this.getLandSetCode()[0] = block.getLandSet().getCode();

        } else if (sealedType.equals("Custom")) {
            String[] dList;
            final ArrayList<CustomLimited> customs = new ArrayList<CustomLimited>();

            // get list of custom draft files
            final File dFolder = new File("res/sealed/");
            if (!dFolder.exists()) {
                throw new RuntimeException("GenerateSealed : folder not found -- folder is "
                        + dFolder.getAbsolutePath());
            }

            if (!dFolder.isDirectory()) {
                throw new RuntimeException("GenerateSealed : not a folder -- " + dFolder.getAbsolutePath());
            }

            dList = dFolder.list();

            for (final String element : dList) {
                if (element.endsWith(".sealed")) {
                    final ArrayList<String> dfData = FileUtil.readFile("res/sealed/" + element);
                    final CustomLimited cs = CustomLimited.parse(dfData);
                    customs.add(cs);
                }
            }

            // present list to user
            if (customs.size() < 1) {
                JOptionPane.showMessageDialog(null, "No custom sealed files found.", "",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                final CustomLimited draft = (CustomLimited) GuiUtils.getChoice("Choose Custom Sealed Pool",
                        customs.toArray());

                final DeckManager dio = AllZone.getDeckManager();
                final Deck dPool = dio.getDeck(draft.getDeckFile());
                if (dPool == null) {
                    throw new RuntimeException("BoosterGenerator : deck not found - " + draft.getDeckFile());
                }

                final BoosterGenerator bpCustom = new BoosterGenerator(dPool);
                final Lambda1<List<CardPrinted>, BoosterGenerator> fnPick = new Lambda1<List<CardPrinted>, BoosterGenerator>() {
                    @Override
                    public List<CardPrinted> apply(final BoosterGenerator pack) {
                        if (draft.getIgnoreRarity()) {
                            return pack.getBoosterPack(0, 0, 0, 0, 0, 0, 0, draft.getNumCards(), 0);
                        }
                        return pack.getBoosterPack(draft.getNumCommons(), draft.getNumUncommons(), 0, draft.getNumRares(),
                                draft.getNumMythics(), draft.getNumSpecials(), draft.getNumDoubleFaced(), 0, 0);
                    }
                };

                final Closure1<List<CardPrinted>, BoosterGenerator> picker = new Closure1<List<CardPrinted>, BoosterGenerator>(
                        fnPick, bpCustom);

                for (int i = 0; i < draft.getNumPacks(); i++) {
                    this.packs.add(picker);
                }

                this.getLandSetCode()[0] = draft.getLandSetCode();
            }
        }
    }

    /**
     * <p>
     * getCardpool.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public ItemPool<CardPrinted> getCardpool() {
        final ItemPool<CardPrinted> pool = new ItemPool<CardPrinted>(CardPrinted.class);

        for (int i = 0; i < this.packs.size(); i++) {
            pool.addAllCards(this.packs.get(i).apply());
        }

        return pool;
    }

    /**
     * <p>
     * buildAIDeck.
     * </p>
     * 
     * @param aiCardpool
     *            a {@link forge.CardList} object.
     * @return a {@link forge.deck.Deck} object.
     */
    public Deck buildAIDeck(final CardList aiCardpool) {
        final CardList deck = new CardList();

        int cardsNeeded = 22;
        int landsNeeded = 18;
        int nCreatures = 15;

        final CardList aiPlayables = aiCardpool.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return !(c.getSVar("RemAIDeck").equals("True"));
            }
        });

        CardList creatures = aiPlayables.getType("Creature");
        CardListUtil.sortByEvaluateCreature(creatures);

        final CardList colorChooserList = new CardList(); // choose colors based
                                                          // on top 33% of
                                                          // creatures
        for (int i = 0; i < (creatures.size() * .33); i++) {
            colorChooserList.add(creatures.get(i));
        }

        final int[] colorCounts = { 0, 0, 0, 0, 0 };
        final String[] colors = Constant.Color.ONLY_COLORS;
        for (int i = 0; i < colors.length; i++) {
            colorCounts[i] = colorChooserList.getColor(colors[i]).size();
        }

        for (int i = 0; i < 4; i++) {
            if (colorCounts[i + 1] < colorCounts[i]) {
                final int t = colorCounts[i];
                colorCounts[i] = colorCounts[i + 1];
                colorCounts[i + 1] = t;

                final String s = colors[i];
                colors[i] = colors[i + 1];
                colors[i + 1] = s;
            }
        }

        final DeckColors dcAI = new DeckColors();
        dcAI.color1 = colors[0];
        dcAI.color2 = colors[1];
        dcAI.splash = colors[2];
        dcAI.mana1 = dcAI.colorToMana(colors[0]);
        dcAI.mana2 = dcAI.colorToMana(colors[1]);
        //dcAI.manaS = dcAI.colorToMana(colors[2]);

        creatures = aiPlayables.getType("Creature").getOnly2Colors(dcAI.color1, dcAI.color2);
        creatures.addAll(aiPlayables.getType("Artifact").getType("Creature"));

        CardListUtil.sortByEvaluateCreature(creatures);
        int i = 0;
        while ((nCreatures > 0) && (i < creatures.size())) {
            final Card c = creatures.get(i);

            deck.add(c);
            aiCardpool.remove(c);
            aiPlayables.remove(c);
            cardsNeeded--;
            nCreatures--;
            i++;
        }

        final CardList splashCreatures = aiPlayables.getType("Creature").getColor(dcAI.splash);
        while ((nCreatures > 1) && (splashCreatures.size() > 1)) {
            final Card c = splashCreatures.get(MyRandom.getRandom().nextInt(splashCreatures.size() - 1));

            deck.add(c);
            aiCardpool.remove(c);
            aiPlayables.remove(c);
            splashCreatures.remove(c);
            cardsNeeded--;
            nCreatures--;
        }

        final CardList walkers = aiPlayables.getType("Planeswalker").getOnly2Colors(dcAI.color1, dcAI.color2);
        if (walkers.size() > 0) {
            deck.add(walkers.get(0));
            aiPlayables.remove(walkers.get(0));
            aiCardpool.remove(walkers.get(0));
            cardsNeeded--;
        }

        final CardList spells = aiPlayables.getType("Instant").getOnly2Colors(dcAI.color1, dcAI.color2);
        spells.addAll(aiPlayables.getType("Sorcery").getOnly2Colors(dcAI.color1, dcAI.color2));
        spells.addAll(aiPlayables.getType("Enchantment").getOnly2Colors(dcAI.color1, dcAI.color2));

        while ((cardsNeeded > 0) && (spells.size() > 1)) {
            final Card c = spells.get(MyRandom.getRandom().nextInt(spells.size() - 1));
            deck.add(c);
            spells.remove(c);
            aiPlayables.remove(c);
            aiCardpool.remove(c);
            cardsNeeded--;
        }

        final CardList splashSpells = aiPlayables.getType("Instant").getColor(dcAI.splash);
        splashSpells.addAll(aiPlayables.getType("Sorcery").getColor(dcAI.splash));

        while ((cardsNeeded > 0) && (splashSpells.size() > 1)) {
            final Card c = splashSpells.get(MyRandom.getRandom().nextInt(splashSpells.size() - 1));
            deck.add(c);
            splashSpells.remove(c);
            aiPlayables.remove(c);
            aiCardpool.remove(c);
            cardsNeeded--;
        }

        final CardList lands = aiPlayables.getType("Land");
        if (lands.size() > 0) {
            final DeckColors aiDC = dcAI; // just for the filter

            lands.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    final ArrayList<AbilityMana> maList = c.getManaAbility();
                    for (int j = 0; j < maList.size(); j++) {
                        if (maList.get(j).canProduce(aiDC.mana1) || maList.get(j).canProduce(aiDC.mana2)) {
                            return true;
                        }
                    }

                    return false;
                }
            });

            if (lands.size() > 0) {
                for (i = 0; i < lands.size(); i++) {
                    final Card c = lands.get(i);

                    deck.add(c);
                    aiCardpool.remove(c);
                    aiPlayables.remove(c);
                    landsNeeded--;

                }
            }

            if (landsNeeded > 0) {
                // attempt to optimize basic land counts
                // according to color representation
                final CCnt[] clrCnts = { new CCnt("Plains", 0), new CCnt("Island", 0), new CCnt("Swamp", 0),
                        new CCnt("Mountain", 0), new CCnt("Forest", 0) };

                // count each card color using mana costs
                // TODO: count hybrid mana differently?
                for (i = 0; i < deck.size(); i++) {
                    final String mc = deck.get(i).getManaCost();

                    // count each mana symbol in the mana cost
                    for (int j = 0; j < mc.length(); j++) {
                        final char c = mc.charAt(j);

                        if (c == 'W') {
                            clrCnts[0].setCount(clrCnts[0].getCount() + 1);
                        } else if (c == 'U') {
                            clrCnts[1].setCount(clrCnts[1].getCount() + 1);
                        } else if (c == 'B') {
                            clrCnts[2].setCount(clrCnts[2].getCount() + 1);
                        } else if (c == 'R') {
                            clrCnts[3].setCount(clrCnts[3].getCount() + 1);
                        } else if (c == 'G') {
                            clrCnts[4].setCount(clrCnts[4].getCount() + 1);
                        }
                    }
                }

                // total of all ClrCnts
                int totalColor = 0;
                for (i = 0; i < 5; i++) {
                    totalColor += clrCnts[i].getCount();
                }

                for (i = 0; i < 5; i++) {
                    if (clrCnts[i].getCount() > 0) { // calculate number of
                                                     // lands for
                        // each color
                        final float p = (float) clrCnts[i].getCount() / (float) totalColor;
                        final int nLand = (int) (landsNeeded * p) + 1;
                        // tmpDeck += "nLand-" + ClrCnts[i].Color + ":" + nLand
                        // + "\n";
                        if (Constant.Runtime.DEV_MODE[0]) {
                            System.out.println("Basics[" + clrCnts[i].getColor() + "]:" + nLand);
                        }

                        for (int j = 0; j <= nLand; j++) {
                            final Card c = AllZone.getCardFactory().getCard(clrCnts[i].getColor(),
                                    AllZone.getComputerPlayer());
                            c.setCurSetCode(this.getLandSetCode()[0]);
                            deck.add(c);
                            landsNeeded--;
                        }
                    }
                }
                int n = 0;
                while (landsNeeded > 0) {
                    if (clrCnts[n].getCount() > 0) {
                        final Card c = AllZone.getCardFactory().getCard(clrCnts[n].getColor(),
                                AllZone.getComputerPlayer());
                        c.setCurSetCode(this.getLandSetCode()[0]);
                        deck.add(c);
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
        }

        final Deck aiDeck = new Deck(GameType.Sealed);

        for (i = 0; i < deck.size(); i++) {
            aiDeck.addMain(deck.get(i).getName() + "|" + deck.get(i).getCurSetCode());
        }

        for (i = 0; i < aiCardpool.size(); i++) {
            aiDeck.addSideboard(aiCardpool.get(i).getName() + "|" + aiCardpool.get(i).getCurSetCode());
        }

        return aiDeck;
    }

    /**
     * @return the landSetCode
     */
    public String[] getLandSetCode() {
        return this.landSetCode;
    }

    /**
     * @param landSetCode
     *            the landSetCode to set
     */
    public void setLandSetCode(final String[] landSetCode) {
        this.landSetCode = landSetCode; // TODO: Add 0 to parameter's name.
    }

    /**
     * The Class DeckColors.
     */
    class DeckColors {

        /** The Color1. */
        private String color1 = "none";

        /** The Color2. */
        private String color2 = "none";

        /** The Splash. */
        private String splash = "none";

        /** The Mana1. */
        private String mana1 = "";

        /** The Mana2. */
        private String mana2 = "";

        /** The Mana s. */
        //private String manaS = "";

        /**
         * Instantiates a new deck colors.
         * 
         * @param c1
         *            the c1
         * @param c2
         *            the c2
         * @param sp
         *            the sp
         */
        public DeckColors(final String c1, final String c2, final String sp) {
            this.color1 = c1;
            this.color2 = c2;
            // Splash = sp;
        }

        /**
         * Instantiates a new deck colors.
         */
        public DeckColors() {

        }

        /**
         * Color to mana.
         * 
         * @param color
         *            the color
         * @return the string
         */
        public String colorToMana(final String color) {
            final String[] mana = { "W", "U", "B", "R", "G" };
            final String[] clrs = { "white", "blue", "black", "red", "green" };

            for (int i = 0; i < Constant.Color.ONLY_COLORS.length; i++) {
                if (clrs[i].equals(color)) {
                    return mana[i];
                }
            }

            return "";
        }

    }

}
