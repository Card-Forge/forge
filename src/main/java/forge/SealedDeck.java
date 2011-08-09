package forge;

import forge.card.spellability.Ability_Mana;
import forge.deck.Deck;
import forge.gui.GuiUtils;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;

/**
 * <p>SealedDeck class.</p>
 *
 * @author Forge
 * @version $Id$
 * @since 1.0.15
 */
public class SealedDeck {
    private ArrayList<BoosterGenerator> packs = new ArrayList<BoosterGenerator>();
    public String LandSetCode[] = {""};

    /**
     * <p>Constructor for SealedDeck.</p>
     *
     * @param sealedType a {@link java.lang.String} object.
     */
    public SealedDeck(String sealedType) {

        if (sealedType.equals("Full")) {
            BoosterGenerator bpFull = new BoosterGenerator();
            for (int i = 0; i < 6; i++)
                packs.add(bpFull);

            LandSetCode[0] = AllZone.getCardFactory().getCard("Plains", AllZone.getHumanPlayer()).getMostRecentSet();
        } else if (sealedType.equals("Block")) {
            ArrayList<String> bNames = SetInfoUtil.getBlockNameList();
            ArrayList<String> rbNames = new ArrayList<String>();
            for (int i = bNames.size() - 1; i >= 0; i--)
                rbNames.add(bNames.get(i));

            Object o = GuiUtils.getChoice("Choose Block", rbNames.toArray());

            ArrayList<String> blockSets = SetInfoUtil.getSets_BlockName(o.toString());
            int nPacks = SetInfoUtil.getSealedPackCount(o.toString());

            ArrayList<String> setCombos = new ArrayList<String>();

            //if (blockSets.get(1).equals("") && blockSets.get(2).equals("")) { // Block only has one set
            if (blockSets.size() == 1) {
                BoosterGenerator bpOne = new BoosterGenerator(blockSets.get(0));

                for (int i = 0; i < nPacks; i++)
                    packs.add(bpOne);
            } else {
                //if (!blockSets.get(1).equals("") && blockSets.get(2).equals("")) { // Block only has two sets
                if (blockSets.size() == 2) {
                    setCombos.add(String.format("%s/%s/%s/%s/%s/%s", blockSets.get(0), blockSets.get(0), blockSets.get(0), blockSets.get(0), blockSets.get(0), blockSets.get(0)));
                    setCombos.add(String.format("%s/%s/%s/%s/%s/%s", blockSets.get(1), blockSets.get(1), blockSets.get(0), blockSets.get(0), blockSets.get(0), blockSets.get(0)));
                }
                //else if (!blockSets.get(1).equals("") && !blockSets.get(2).equals("")) { // Block has three sets
                else if (blockSets.size() == 3) {
                    setCombos.add(String.format("%s/%s/%s/%s/%s/%s", blockSets.get(0), blockSets.get(0), blockSets.get(0), blockSets.get(0), blockSets.get(0), blockSets.get(0)));
                    setCombos.add(String.format("%s/%s/%s/%s/%s/%s", blockSets.get(1), blockSets.get(1), blockSets.get(0), blockSets.get(0), blockSets.get(0), blockSets.get(0)));
                    setCombos.add(String.format("%s/%s/%s/%s/%s/%s", blockSets.get(2), blockSets.get(2), blockSets.get(1), blockSets.get(1), blockSets.get(0), blockSets.get(0)));
                }

                Object p = GuiUtils.getChoice("Choose Set Combination", setCombos.toArray());

                String pp[] = p.toString().split("/");
                for (int i = 0; i < nPacks; i++) {
                    BoosterGenerator bpMulti = new BoosterGenerator(pp[i]);
                    packs.add(bpMulti);
                }
            }

            LandSetCode[0] = blockSets.get(0);

        } else if (sealedType.equals("Custom")) {
            String dList[];
            ArrayList<CustomSealed> customs = new ArrayList<CustomSealed>();
            ArrayList<String> customList = new ArrayList<String>();

            // get list of custom draft files
            File dFolder = new File("res/sealed/");
            if (!dFolder.exists())
                throw new RuntimeException("GenerateSealed : folder not found -- folder is " + dFolder.getAbsolutePath());

            if (!dFolder.isDirectory())
                throw new RuntimeException("GenerateSealed : not a folder -- " + dFolder.getAbsolutePath());

            dList = dFolder.list();

            for (int i = 0; i < dList.length; i++) {
                if (dList[i].endsWith(".sealed")) {
                    ArrayList<String> dfData = FileUtil.readFile("res/sealed/" + dList[i]);

                    CustomSealed cs = new CustomSealed();

                    for (int j = 0; j < dfData.size(); j++) {

                        String dfd = dfData.get(j);

                        if (dfd.startsWith("Name:"))
                            cs.Name = dfd.substring(5);
                        if (dfd.startsWith("Type:"))
                            cs.Type = dfd.substring(5);
                        if (dfd.startsWith("DeckFile:"))
                            cs.DeckFile = dfd.substring(9);
                        if (dfd.startsWith("IgnoreRarity:"))
                            cs.IgnoreRarity = dfd.substring(13).equals("True");
                        if (dfd.startsWith("LandSetCode:"))
                            cs.LandSetCode = dfd.substring(12);

                        if (dfd.startsWith("NumCards:"))
                            cs.NumCards = Integer.parseInt(dfd.substring(9));
                        if (dfd.startsWith("NumSpecials:"))
                            cs.NumSpecials = Integer.parseInt(dfd.substring(12));
                        if (dfd.startsWith("NumMythics:"))
                            cs.NumMythics = Integer.parseInt(dfd.substring(11));
                        if (dfd.startsWith("NumRares:"))
                            cs.NumRares = Integer.parseInt(dfd.substring(9));
                        if (dfd.startsWith("NumUncommons:"))
                            cs.NumUncommons = Integer.parseInt(dfd.substring(13));
                        if (dfd.startsWith("NumCommons:"))
                            cs.NumCommons = Integer.parseInt(dfd.substring(11));
                        if (dfd.startsWith("NumPacks:"))
                            cs.NumPacks = Integer.parseInt(dfd.substring(9));

                    }

                    customs.add(cs);
                    customList.add(cs.Name);
                }


            }
            CustomSealed chosenSealed = null;

            // present list to user
            if (customs.size() < 1)
                JOptionPane.showMessageDialog(null, "No custom sealed files found.", "", JOptionPane.INFORMATION_MESSAGE);

            else {
                Object p = GuiUtils.getChoice("Choose Custom Sealed Pool", customList.toArray());

                for (int i = 0; i < customs.size(); i++) {
                    CustomSealed cs = customs.get(i);

                    if (cs.Name.equals(p.toString()))
                        chosenSealed = cs;
                }

                if (chosenSealed.IgnoreRarity)
                    chosenSealed.NumCommons = chosenSealed.NumCards;

                BoosterGenerator bpCustom = new BoosterGenerator(chosenSealed.DeckFile, chosenSealed.NumCommons, chosenSealed.NumUncommons, chosenSealed.NumRares, chosenSealed.NumMythics, chosenSealed.NumSpecials, chosenSealed.IgnoreRarity);

                for (int i = 0; i < chosenSealed.NumPacks; i++) {
                    packs.add(bpCustom);
                }

                LandSetCode[0] = chosenSealed.LandSetCode;
            }
        }
    }

    /**
     * <p>getCardpool.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardList getCardpool() {
        CardList pool = new CardList();

        for (int i = 0; i < packs.size(); i++)
            pool.addAll(packs.get(i).getBoosterPack());

        return pool;
    }

    /**
     * <p>buildAIDeck.</p>
     *
     * @param aiCardpool a {@link forge.CardList} object.
     * @return a {@link forge.deck.Deck} object.
     */
    public Deck buildAIDeck(CardList aiCardpool) {
        CardList deck = new CardList();

        int cardsNeeded = 22;
        int landsNeeded = 18;
        int nCreatures = 15;

        CardList AIPlayables = aiCardpool.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return !(c.getSVar("RemAIDeck").equals("True"));
            }
        });

        CardList creatures = AIPlayables.getType("Creature");
        CardListUtil.sortByEvaluateCreature(creatures);

        CardList colorChooserList = new CardList(); // choose colors based on top 33% of creatures
        for (int i = 0; i < (creatures.size() * .33); i++)
            colorChooserList.add(creatures.get(i));

        int colorCounts[] = {0, 0, 0, 0, 0};
        String colors[] = Constant.Color.onlyColors;
        for (int i = 0; i < colors.length; i++)
            colorCounts[i] = colorChooserList.getColor(colors[i]).size();

        for (int i = 0; i < 4; i++) {
            if (colorCounts[i + 1] < colorCounts[i]) {
                int t = colorCounts[i];
                colorCounts[i] = colorCounts[i + 1];
                colorCounts[i + 1] = t;

                String s = colors[i];
                colors[i] = colors[i + 1];
                colors[i + 1] = s;
            }
        }

        DeckColors dcAI = new DeckColors();
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
        while (nCreatures > 0 && i < creatures.size()) {
            Card c = creatures.get(i);

            deck.add(c);
            aiCardpool.remove(c);
            AIPlayables.remove(c);
            cardsNeeded--;
            nCreatures--;
            i++;
        }

        CardList splashCreatures = AIPlayables.getType("Creature").getColor(dcAI.Splash);
        while (nCreatures > 1 && splashCreatures.size() > 1) {
            Card c = splashCreatures.get(MyRandom.random.nextInt(splashCreatures.size() - 1));

            deck.add(c);
            aiCardpool.remove(c);
            AIPlayables.remove(c);
            splashCreatures.remove(c);
            cardsNeeded--;
            nCreatures--;
        }

        CardList walkers = AIPlayables.getType("Planeswalker").getOnly2Colors(dcAI.Color1, dcAI.Color2);
        if (walkers.size() > 0) {
            deck.add(walkers.get(0));
            AIPlayables.remove(walkers.get(0));
            aiCardpool.remove(walkers.get(0));
            cardsNeeded--;
        }

        CardList spells = AIPlayables.getType("Instant").getOnly2Colors(dcAI.Color1, dcAI.Color2);
        spells.addAll(AIPlayables.getType("Sorcery").getOnly2Colors(dcAI.Color1, dcAI.Color2));
        spells.addAll(AIPlayables.getType("Enchantment").getOnly2Colors(dcAI.Color1, dcAI.Color2));

        while (cardsNeeded > 0 && spells.size() > 1) {
            Card c = spells.get(MyRandom.random.nextInt(spells.size() - 1));
            deck.add(c);
            spells.remove(c);
            AIPlayables.remove(c);
            aiCardpool.remove(c);
            cardsNeeded--;
        }

        CardList splashSpells = AIPlayables.getType("Instant").getColor(dcAI.Splash);
        splashSpells.addAll(AIPlayables.getType("Sorcery").getColor(dcAI.Splash));

        while (cardsNeeded > 0 && splashSpells.size() > 1) {
            Card c = splashSpells.get(MyRandom.random.nextInt(splashSpells.size() - 1));
            deck.add(c);
            splashSpells.remove(c);
            AIPlayables.remove(c);
            aiCardpool.remove(c);
            cardsNeeded--;
        }

        CardList lands = AIPlayables.getType("Land");
        if (lands.size() > 0) {
            final DeckColors AIdc = dcAI;    // just for the filter

            lands.filter(new CardListFilter() {
                public boolean addCard(Card c) {
                    ArrayList<Ability_Mana> maList = c.getManaAbility();
                    for (int j = 0; j < maList.size(); j++) {
                        if (maList.get(j).canProduce(AIdc.Mana1) || maList.get(j).canProduce(AIdc.Mana2))
                            return true;
                    }

                    return false;
                }
            });

            if (lands.size() > 0) {
                for (i = 0; i < lands.size(); i++) {
                    Card c = lands.get(i);

                    deck.add(c);
                    aiCardpool.remove(c);
                    AIPlayables.remove(c);
                    landsNeeded--;

                }
            }

            if (landsNeeded > 0)    // attempt to optimize basic land counts according to color representation
            {
                CCnt ClrCnts[] = {new CCnt("Plains", 0),
                        new CCnt("Island", 0),
                        new CCnt("Swamp", 0),
                        new CCnt("Mountain", 0),
                        new CCnt("Forest", 0)};

                // count each card color using mana costs
                // TODO: count hybrid mana differently?
                for (i = 0; i < deck.size(); i++) {
                    String mc = deck.get(i).getManaCost();

                    // count each mana symbol in the mana cost
                    for (int j = 0; j < mc.length(); j++) {
                        char c = mc.charAt(j);

                        if (c == 'W')
                            ClrCnts[0].Count++;
                        else if (c == 'U')
                            ClrCnts[1].Count++;
                        else if (c == 'B')
                            ClrCnts[2].Count++;
                        else if (c == 'R')
                            ClrCnts[3].Count++;
                        else if (c == 'G')
                            ClrCnts[4].Count++;
                    }
                }

                // total of all ClrCnts
                int totalColor = 0;
                for (i = 0; i < 5; i++) {
                    totalColor += ClrCnts[i].Count;
                }

                for (i = 0; i < 5; i++) {
                    if (ClrCnts[i].Count > 0) {    // calculate number of lands for each color
                        float p = (float) ClrCnts[i].Count / (float) totalColor;
                        int nLand = (int) ((float) landsNeeded * p) + 1;
                        //tmpDeck += "nLand-" + ClrCnts[i].Color + ":" + nLand + "\n";
                        if (Constant.Runtime.DevMode[0])
                            System.out.println("Basics[" + ClrCnts[i].Color + "]:" + nLand);

                        for (int j = 0; j <= nLand; j++) {
                            Card c = AllZone.getCardFactory().getCard(ClrCnts[i].Color, AllZone.getComputerPlayer());
                            c.setCurSetCode(this.LandSetCode[0]);
                            deck.add(c);
                            landsNeeded--;
                        }
                    }
                }
                int n = 0;
                while (landsNeeded > 0) {
                    if (ClrCnts[n].Count > 0) {
                        Card c = AllZone.getCardFactory().getCard(ClrCnts[n].Color, AllZone.getComputerPlayer());
                        c.setCurSetCode(this.LandSetCode[0]);
                        deck.add(c);
                        landsNeeded--;

                        if (Constant.Runtime.DevMode[0])
                            System.out.println("AddBasics: " + c.getName());
                    }
                    if (++n > 4)
                        n = 0;
                }
            }
        }

        Deck aiDeck = new Deck(Constant.GameType.Sealed);

        for (i = 0; i < deck.size(); i++)
            aiDeck.addMain(deck.get(i).getName() + "|" + deck.get(i).getCurSetCode());

        for (i = 0; i < aiCardpool.size(); i++)
            aiDeck.addSideboard(aiCardpool.get(i).getName() + "|" + aiCardpool.get(i).getCurSetCode());

        return aiDeck;
    }

    class CustomSealed {
        public String Name;
        public String Type;
        public String DeckFile;
        public Boolean IgnoreRarity;
        public int NumCards = 15;
        public int NumSpecials = 0;
        public int NumMythics = 1;
        public int NumRares = 1;
        public int NumUncommons = 3;
        public int NumCommons = 11;
        public int NumPacks = 3;
        public String LandSetCode = AllZone.getCardFactory().getCard("Plains", AllZone.getHumanPlayer()).getMostRecentSet();
    }

    class DeckColors {
        public String Color1 = "none";
        public String Color2 = "none";
        public String Splash = "none";
        public String Mana1 = "";
        public String Mana2 = "";
        public String ManaS = "";

        public DeckColors(String c1, String c2, String sp) {
            Color1 = c1;
            Color2 = c2;
            //Splash = sp;
        }

        public DeckColors() {

        }

        public String ColorToMana(String color) {
            String Mana[] = {"W", "U", "B", "R", "G"};
            String Clrs[] = {"white", "blue", "black", "red", "green"};

            for (int i = 0; i < Constant.Color.onlyColors.length; i++) {
                if (Clrs[i].equals(color))
                    return Mana[i];
            }

            return "";
        }


    }

}
