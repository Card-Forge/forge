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
import forge.card.spellability.Ability_Mana;
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
    public String LandSetCode[] = { "" };

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

            this.LandSetCode[0] = CardDb.instance().getCard("Plains").getSet();
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

            this.LandSetCode[0] = block.getLandSet().getCode();

        } else if (sealedType.equals("Custom")) {
            String dList[];
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
                final Deck dPool = dio.getDeck(draft.DeckFile);
                if (dPool == null) {
                    throw new RuntimeException("BoosterGenerator : deck not found - " + draft.DeckFile);
                }

                final BoosterGenerator bpCustom = new BoosterGenerator(dPool);
                final Lambda1<List<CardPrinted>, BoosterGenerator> fnPick = new Lambda1<List<CardPrinted>, BoosterGenerator>() {
                    @Override
                    public List<CardPrinted> apply(final BoosterGenerator pack) {
                        if (draft.IgnoreRarity) {
                            return pack.getBoosterPack(0, 0, 0, 0, 0, 0, 0, draft.NumCards, 0);
                        }
                        return pack.getBoosterPack(draft.NumCommons, draft.NumUncommons, 0, draft.NumRares,
                                draft.NumMythics, draft.NumSpecials, draft.NumDoubleFaced, 0, 0);
                    }
                };

                final Closure1<List<CardPrinted>, BoosterGenerator> picker = new Closure1<List<CardPrinted>, BoosterGenerator>(
                        fnPick, bpCustom);

                for (int i = 0; i < draft.NumPacks; i++) {
                    this.packs.add(picker);
                }

                this.LandSetCode[0] = draft.LandSetCode;
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

        final CardList AIPlayables = aiCardpool.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return !(c.getSVar("RemAIDeck").equals("True"));
            }
        });

        CardList creatures = AIPlayables.getType("Creature");
        CardListUtil.sortByEvaluateCreature(creatures);

        final CardList colorChooserList = new CardList(); // choose colors based
                                                          // on top 33% of
                                                          // creatures
        for (int i = 0; i < (creatures.size() * .33); i++) {
            colorChooserList.add(creatures.get(i));
        }

        final int colorCounts[] = { 0, 0, 0, 0, 0 };
        final String colors[] = Constant.Color.ONLY_COLORS;
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
        dcAI.Color1 = colors[0];
        dcAI.Color2 = colors[1];
        dcAI.Splash = colors[2];
        dcAI.Mana1 = dcAI.ColorToMana(colors[0]);
        dcAI.Mana2 = dcAI.ColorToMana(colors[1]);
        dcAI.ManaS = dcAI.ColorToMana(colors[2]);

        creatures = AIPlayables.getType("Creature").getOnly2Colors(dcAI.Color1, dcAI.Color2);
        creatures.addAll(AIPlayables.getType("Artifact").getType("Creature"));

        CardListUtil.sortByEvaluateCreature(creatures);
        int i = 0;
        while ((nCreatures > 0) && (i < creatures.size())) {
            final Card c = creatures.get(i);

            deck.add(c);
            aiCardpool.remove(c);
            AIPlayables.remove(c);
            cardsNeeded--;
            nCreatures--;
            i++;
        }

        final CardList splashCreatures = AIPlayables.getType("Creature").getColor(dcAI.Splash);
        while ((nCreatures > 1) && (splashCreatures.size() > 1)) {
            final Card c = splashCreatures.get(MyRandom.getRandom().nextInt(splashCreatures.size() - 1));

            deck.add(c);
            aiCardpool.remove(c);
            AIPlayables.remove(c);
            splashCreatures.remove(c);
            cardsNeeded--;
            nCreatures--;
        }

        final CardList walkers = AIPlayables.getType("Planeswalker").getOnly2Colors(dcAI.Color1, dcAI.Color2);
        if (walkers.size() > 0) {
            deck.add(walkers.get(0));
            AIPlayables.remove(walkers.get(0));
            aiCardpool.remove(walkers.get(0));
            cardsNeeded--;
        }

        final CardList spells = AIPlayables.getType("Instant").getOnly2Colors(dcAI.Color1, dcAI.Color2);
        spells.addAll(AIPlayables.getType("Sorcery").getOnly2Colors(dcAI.Color1, dcAI.Color2));
        spells.addAll(AIPlayables.getType("Enchantment").getOnly2Colors(dcAI.Color1, dcAI.Color2));

        while ((cardsNeeded > 0) && (spells.size() > 1)) {
            final Card c = spells.get(MyRandom.getRandom().nextInt(spells.size() - 1));
            deck.add(c);
            spells.remove(c);
            AIPlayables.remove(c);
            aiCardpool.remove(c);
            cardsNeeded--;
        }

        final CardList splashSpells = AIPlayables.getType("Instant").getColor(dcAI.Splash);
        splashSpells.addAll(AIPlayables.getType("Sorcery").getColor(dcAI.Splash));

        while ((cardsNeeded > 0) && (splashSpells.size() > 1)) {
            final Card c = splashSpells.get(MyRandom.getRandom().nextInt(splashSpells.size() - 1));
            deck.add(c);
            splashSpells.remove(c);
            AIPlayables.remove(c);
            aiCardpool.remove(c);
            cardsNeeded--;
        }

        final CardList lands = AIPlayables.getType("Land");
        if (lands.size() > 0) {
            final DeckColors AIdc = dcAI; // just for the filter

            lands.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    final ArrayList<Ability_Mana> maList = c.getManaAbility();
                    for (int j = 0; j < maList.size(); j++) {
                        if (maList.get(j).canProduce(AIdc.Mana1) || maList.get(j).canProduce(AIdc.Mana2)) {
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
                    AIPlayables.remove(c);
                    landsNeeded--;

                }
            }

            if (landsNeeded > 0) // attempt to optimize basic land counts
                                 // according to color representation
            {
                final CCnt ClrCnts[] = { new CCnt("Plains", 0), new CCnt("Island", 0), new CCnt("Swamp", 0),
                        new CCnt("Mountain", 0), new CCnt("Forest", 0) };

                // count each card color using mana costs
                // TODO: count hybrid mana differently?
                for (i = 0; i < deck.size(); i++) {
                    final String mc = deck.get(i).getManaCost();

                    // count each mana symbol in the mana cost
                    for (int j = 0; j < mc.length(); j++) {
                        final char c = mc.charAt(j);

                        if (c == 'W') {
                            ClrCnts[0].setCount(ClrCnts[0].getCount() + 1);
                        } else if (c == 'U') {
                            ClrCnts[1].setCount(ClrCnts[1].getCount() + 1);
                        } else if (c == 'B') {
                            ClrCnts[2].setCount(ClrCnts[2].getCount() + 1);
                        } else if (c == 'R') {
                            ClrCnts[3].setCount(ClrCnts[3].getCount() + 1);
                        } else if (c == 'G') {
                            ClrCnts[4].setCount(ClrCnts[4].getCount() + 1);
                        }
                    }
                }

                // total of all ClrCnts
                int totalColor = 0;
                for (i = 0; i < 5; i++) {
                    totalColor += ClrCnts[i].getCount();
                }

                for (i = 0; i < 5; i++) {
                    if (ClrCnts[i].getCount() > 0) { // calculate number of
                                                     // lands for
                        // each color
                        final float p = (float) ClrCnts[i].getCount() / (float) totalColor;
                        final int nLand = (int) (landsNeeded * p) + 1;
                        // tmpDeck += "nLand-" + ClrCnts[i].Color + ":" + nLand
                        // + "\n";
                        if (Constant.Runtime.DEV_MODE[0]) {
                            System.out.println("Basics[" + ClrCnts[i].getColor() + "]:" + nLand);
                        }

                        for (int j = 0; j <= nLand; j++) {
                            final Card c = AllZone.getCardFactory().getCard(ClrCnts[i].getColor(),
                                    AllZone.getComputerPlayer());
                            c.setCurSetCode(this.LandSetCode[0]);
                            deck.add(c);
                            landsNeeded--;
                        }
                    }
                }
                int n = 0;
                while (landsNeeded > 0) {
                    if (ClrCnts[n].getCount() > 0) {
                        final Card c = AllZone.getCardFactory().getCard(ClrCnts[n].getColor(),
                                AllZone.getComputerPlayer());
                        c.setCurSetCode(this.LandSetCode[0]);
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
     * The Class DeckColors.
     */
    class DeckColors {

        /** The Color1. */
        public String Color1 = "none";

        /** The Color2. */
        public String Color2 = "none";

        /** The Splash. */
        public String Splash = "none";

        /** The Mana1. */
        public String Mana1 = "";

        /** The Mana2. */
        public String Mana2 = "";

        /** The Mana s. */
        public String ManaS = "";

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
            this.Color1 = c1;
            this.Color2 = c2;
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
        public String ColorToMana(final String color) {
            final String Mana[] = { "W", "U", "B", "R", "G" };
            final String Clrs[] = { "white", "blue", "black", "red", "green" };

            for (int i = 0; i < Constant.Color.ONLY_COLORS.length; i++) {
                if (Clrs[i].equals(color)) {
                    return Mana[i];
                }
            }

            return "";
        }

    }

}
