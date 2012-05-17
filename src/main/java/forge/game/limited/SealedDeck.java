/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.limited;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardListUtil;
import forge.Constant;
import forge.Singletons;
import forge.card.BoosterGenerator;
import forge.card.CardBlock;
import forge.card.CardColor;
import forge.card.CardEdition;
import forge.card.CardManaCost;
import forge.card.UnOpenedProduct;
import forge.card.mana.ManaCostShard;
import forge.card.spellability.AbilityMana;
import forge.deck.Deck;
import forge.gui.GuiUtils;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.ItemPool;
import forge.util.FileUtil;
import forge.util.MyRandom;
import forge.util.closures.Lambda1;

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
    private final ArrayList<UnOpenedProduct> product = new ArrayList<UnOpenedProduct>();

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
            for (int i = 0; i < 6; i++) {
                this.product.add(new UnOpenedProduct(BoosterGenerator.IDENTITY_PICK, bpFull));
            }

            this.getLandSetCode()[0] = CardDb.instance().getCard("Plains").getEdition();
        } else if (sealedType.equals("Block")) {

            List<CardBlock> blocks = new ArrayList<CardBlock>();
            for (CardBlock b : Singletons.getModel().getBlocks()) {
                blocks.add(b);
            }
            final CardBlock block = GuiUtils.chooseOne("Choose Block", blocks);

            final CardEdition[] cardSets = block.getSets();
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
                final Object p = GuiUtils.chooseOne("Choose Set Combination", setCombos.toArray());

                final String[] pp = p.toString().split("/");
                for (int i = 0; i < nPacks; i++) {
                    this.product.add(new UnOpenedProduct(Singletons.getModel().getBoosters().get(pp[i])));
                }
            } else {
                final UnOpenedProduct product1 = new UnOpenedProduct(Singletons.getModel().getBoosters().get(sets[0]));
                for (int i = 0; i < nPacks; i++) {
                    this.product.add(product1);
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
                    final List<String> dfData = FileUtil.readFile("res/sealed/" + element);
                    final CustomLimited cs = CustomLimited.parse(dfData, Singletons.getModel().getDecks().getCubes());
                    customs.add(cs);
                }
            }

            // present list to user
            if (customs.size() < 1) {
                JOptionPane.showMessageDialog(null, "No custom sealed files found.", "",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                final CustomLimited draft = (CustomLimited) GuiUtils.chooseOne("Choose Custom Sealed Pool",
                        customs.toArray());

                final BoosterGenerator bpCustom = new BoosterGenerator(draft.getCardPool());
                final Lambda1<List<CardPrinted>, BoosterGenerator> fnPick = new Lambda1<List<CardPrinted>, BoosterGenerator>() {
                    @Override
                    public List<CardPrinted> apply(final BoosterGenerator pack) {
                        if (draft.getIgnoreRarity()) {
                            if (!draft.getSingleton()) {
                                return pack.getBoosterPack(0, 0, 0, 0, 0, 0, 0, draft.getNumCards(), 0);
                            } else {
                                return pack.getSingletonBoosterPack(draft.getNumCards());
                            }
                        }
                        return pack.getBoosterPack(draft.getNumbersByRarity(), 0, 0, 0);
                    }
                };

                for (int i = 0; i < draft.getNumPacks(); i++) {
                    this.product.add(new UnOpenedProduct(fnPick, bpCustom));
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

        for (int i = 0; i < this.product.size(); i++) {
            pool.addAllFlat(this.product.get(i).open());
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
                return !(c.getSVar("RemAIDeck").equals("True") || c.getSVar("RemRandomDeck").equals("True"));
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
        // dcAI.manaS = dcAI.colorToMana(colors[2]);

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
                    final CardManaCost mc = deck.get(i).getManaCost();

                    // count each mana symbol in the mana cost
                    for (ManaCostShard shard : mc.getShards()) {
                        byte mask = shard.getColorMask();
                        
                        if ((mask & CardColor.WHITE) > 0 ) 
                            clrCnts[0].setCount(clrCnts[0].getCount() + 1);
                        if ((mask & CardColor.BLUE) > 0 ) 
                            clrCnts[1].setCount(clrCnts[1].getCount() + 1);
                        if ((mask & CardColor.BLACK) > 0 ) 
                            clrCnts[2].setCount(clrCnts[2].getCount() + 1);
                        if ((mask & CardColor.RED) > 0 ) 
                            clrCnts[3].setCount(clrCnts[3].getCount() + 1);
                        if ((mask & CardColor.GREEN) > 0 )
                            clrCnts[4].setCount(clrCnts[4].getCount() + 1);
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

        final Deck aiDeck = new Deck();

        for (i = 0; i < deck.size(); i++) {
            if (deck.get(i).getName().equals("Plains") || deck.get(i).getName().equals("Island")
                    || deck.get(i).getName().equals("Swamp") || deck.get(i).getName().equals("Mountain")
                    || deck.get(i).getName().equals("Forest")) {
                //System.out.println("Heyo!");
            }
            aiDeck.getMain().add(deck.get(i));
        }

        aiDeck.getSideboard().add(aiCardpool);
        return aiDeck;
    }

    /**
     * Gets the land set code.
     * 
     * @return the landSetCode
     */
    public String[] getLandSetCode() {
        return this.landSetCode;
    }

    /**
     * Sets the land set code.
     * 
     * @param landSetCode0
     *            the landSetCode to set
     */
    public void setLandSetCode(final String[] landSetCode0) {
        this.landSetCode = landSetCode0;
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

        /**
         * The Mana s.
         * 
         * @param c1
         *            the c1
         * @param c2
         *            the c2
         * @param sp
         *            the sp
         */
        // private String manaS = "";

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
