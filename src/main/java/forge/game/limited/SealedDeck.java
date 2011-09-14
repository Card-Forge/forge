package forge.game.limited;

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

import javax.swing.*;

import net.slightlymagic.braids.util.lambda.Lambda1;
import net.slightlymagic.maxmtg.Closure1;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>SealedDeck class.</p>
 *
 * @author Forge
 * @version $Id$
 * @since 1.0.15
 */
public class SealedDeck {
    private List<Closure1<List<CardPrinted>, BoosterGenerator>> packs = new ArrayList<Closure1<List<CardPrinted>, BoosterGenerator>>();
    public String LandSetCode[] = {""};

    /**
     * <p>Constructor for SealedDeck.</p>
     *
     * @param sealedType a {@link java.lang.String} object.
     */
    public SealedDeck(String sealedType) {

        if (sealedType.equals("Full")) {
            BoosterGenerator bpFull = new BoosterGenerator(CardDb.instance().getAllUniqueCards());
            Closure1<List<CardPrinted>, BoosterGenerator> picker = BoosterGenerator.getSimplePicker(bpFull);
            for (int i = 0; i < 6; i++)
                packs.add(picker);

            LandSetCode[0] = CardDb.instance().getCard("Plains").getSet();
        } else if (sealedType.equals("Block")) {

            Object o = GuiUtils.getChoice("Choose Block", SetUtils.getBlocks().toArray());
            CardBlock block = (CardBlock) o;

            CardSet[] cardSets = block.getSets();  
            String[] sets = new String[cardSets.length];
            for (int k = cardSets.length - 1; k >= 0 ; --k) { sets[k] = cardSets[k].getCode();} 

            int nPacks = block.getCntBoostersSealed();

            List<String> setCombos = new ArrayList<String>();
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
                Object p = GuiUtils.getChoice("Choose Set Combination", setCombos.toArray());

                String[] pp = p.toString().split("/");
                for (int i = 0; i < nPacks; i++) {
                    BoosterGenerator bpMulti = new BoosterGenerator(pp[i]);
                    packs.add(BoosterGenerator.getSimplePicker(bpMulti));
                }
            } else {
                BoosterGenerator bpOne = new BoosterGenerator(sets[0]);
                Closure1<List<CardPrinted>, BoosterGenerator> picker = BoosterGenerator.getSimplePicker(bpOne);
                for (int i = 0; i < nPacks; i++) { packs.add(picker); }
            }

            LandSetCode[0] = block.getLandSet().getCode();

        } else if (sealedType.equals("Custom")) {
            String dList[];
            ArrayList<CustomLimited> customs = new ArrayList<CustomLimited>();

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
                    CustomLimited cs = CustomLimited.parse(dfData);
                    customs.add(cs);
                }
            }

            // present list to user
            if (customs.size() < 1)
                JOptionPane.showMessageDialog(null, "No custom sealed files found.", "", JOptionPane.INFORMATION_MESSAGE);

            else {
                final CustomLimited draft = (CustomLimited) GuiUtils.getChoice("Choose Custom Sealed Pool", customs.toArray());

                DeckManager dio = AllZone.getDeckManager();
                Deck dPool = dio.getDeck(draft.DeckFile);
                if (dPool == null) {
                    throw new RuntimeException("BoosterGenerator : deck not found - " + draft.DeckFile);
                }

                BoosterGenerator bpCustom = new BoosterGenerator(dPool);
                Lambda1<List<CardPrinted>, BoosterGenerator> fnPick = new Lambda1<List<CardPrinted>, BoosterGenerator>() {
                    @Override public List<CardPrinted> apply(BoosterGenerator pack) {
                        if ( draft.IgnoreRarity ) {
                            return pack.getBoosterPack(0, 0, 0, 0, 0, 0, draft.NumCards);
                        }
                        return pack.getBoosterPack(draft.NumCommons, draft.NumUncommons, 0, draft.NumRares, draft.NumMythics, draft.NumSpecials, 0);
                    }
                };

                Closure1<List<CardPrinted>, BoosterGenerator> picker = new Closure1<List<CardPrinted>, BoosterGenerator>(fnPick, bpCustom);

                for (int i = 0; i < draft.NumPacks; i++) {
                    packs.add(picker);
                }

                LandSetCode[0] = draft.LandSetCode;
            }
        }
    }

    /**
     * <p>getCardpool.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public ItemPool<CardPrinted> getCardpool() {
        ItemPool<CardPrinted> pool = new ItemPool<CardPrinted>(CardPrinted.class);

        for (int i = 0; i < packs.size(); i++)
            pool.addAllCards(packs.get(i).apply());

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

        Deck aiDeck = new Deck(GameType.Sealed);

        for (i = 0; i < deck.size(); i++)
            aiDeck.addMain(deck.get(i).getName() + "|" + deck.get(i).getCurSetCode());

        for (i = 0; i < aiCardpool.size(); i++)
            aiDeck.addSideboard(aiCardpool.get(i).getName() + "|" + aiCardpool.get(i).getCurSetCode());

        return aiDeck;
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
