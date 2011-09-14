package forge.game.limited;

import forge.AllZone;
import forge.BoosterGenerator;
import forge.Card;
import forge.CardList;
import forge.Constant;
import forge.FileUtil;
import forge.SetUtils;
import forge.card.CardBlock;
import forge.card.CardSet;
import forge.deck.Deck;
import forge.deck.DeckManager;
import forge.gui.GuiUtils;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.ItemPool;
import forge.item.ItemPoolView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

/**
 * 
 * TODO Write javadoc for this type.
 *
 */
public class BoosterDraft_1 implements BoosterDraft {
    private final BoosterDraftAI draftAI = new BoosterDraftAI();
    private static final int nPlayers = 8;
    //private static int boosterPackSize = 14; // 10 com + 3 unc + 1 rare/myth
    private static int stopCount = 42; //boosterPackSize * 3;//should total of 42 - because you don't draft lands

    private int currentCount = 0;
    private List<List<CardPrinted>> pack; //size 8
    //private BoosterGenerator packs[] = {new BoosterGenerator(), new BoosterGenerator(), new BoosterGenerator()};
    private ArrayList<BoosterGenerator> packs = new ArrayList<BoosterGenerator>();
    private int packNum = 0;

    //helps the computer choose which booster packs to pick from
    //the first row says "pick from boosters 1-7, skip 0" since the players picks from 0
    //the second row says "pick from 0 and 2-7 boosters, skip 1" - player chooses from 1
    private final int[][] computerChoose = {
            {1, 2, 3, 4, 5, 6, 7},
            {0, 2, 3, 4, 5, 6, 7},
            {0, 1, 3, 4, 5, 6, 7},
            {0, 1, 2, 4, 5, 6, 7},
            {0, 1, 2, 3, 5, 6, 7},
            {0, 1, 2, 3, 4, 6, 7},
            {0, 1, 2, 3, 4, 5, 7},
            {0, 1, 2, 3, 4, 5, 6}
    };

    /**
     * 
     * TODO Write javadoc for Constructor.
     */
    public BoosterDraft_1() {
        pack = get8BoosterPack();
    }

    /**
     * <p>Constructor for BoosterDraft_1.</p>
     *
     * @param draftType a {@link java.lang.String} object.
     */
    public BoosterDraft_1(String draftType) {
        draftAI.bd = this;
        draftFormat[0] = draftType;

        if (draftType.equals("Full")) {    // Draft from all cards in Forge
            BoosterGenerator bpFull = new BoosterGenerator(CardDb.instance().getAllUniqueCards());
            for (int i = 0; i < 3; i++) {
                packs.add(bpFull);
            }

            LandSetCode[0] = AllZone.getCardFactory().getCard("Plains", AllZone.getHumanPlayer()).getMostRecentSet();
        } else if (draftType.equals("Block")) {    // Draft from cards by block or set
            List<CardBlock> blocks = SetUtils.getBlocks();

            Object o = GuiUtils.getChoice("Choose Block", blocks.toArray());
            CardBlock block = (CardBlock) o;

            CardSet[] cardSets = block.getSets();  
            String[] sets = new String[cardSets.length];
            for (int k = cardSets.length - 1; k >= 0 ; --k) { sets[k] = cardSets[k].getCode();} 

            int nPacks = block.getCntBoostersDraft();

            ArrayList<String> setCombos = new ArrayList<String>();
            if (sets.length >= 2) {
                setCombos.add(String.format("%s/%s/%s", sets[0], sets[0], sets[0]));
                setCombos.add(String.format("%s/%s/%s", sets[1], sets[0], sets[0]));
                setCombos.add(String.format("%s/%s/%s", sets[1], sets[1], sets[0]));
                setCombos.add(String.format("%s/%s/%s", sets[1], sets[1], sets[1]));
            }
            if (sets.length >= 3) {
                setCombos.add(String.format("%s/%s/%s", sets[2], sets[1], sets[0]));
                setCombos.add(String.format("%s/%s/%s", sets[2], sets[2], sets[0]));
                setCombos.add(String.format("%s/%s/%s", sets[2], sets[2], sets[1]));
                setCombos.add(String.format("%s/%s/%s", sets[2], sets[2], sets[2]));
            }

            int sumCards = 0;
            if (sets.length > 1) {
                Object p = GuiUtils.getChoice("Choose Set Combination", setCombos.toArray());
                String[] pp = p.toString().split("/");
                for (int i = 0; i < nPacks; i++) {
                    BoosterGenerator bpMulti = new BoosterGenerator(pp[i]);
                    packs.add(bpMulti);
                    sumCards += bpMulti.getBoosterPackSize();
                }
            } else {
                BoosterGenerator bpOne = new BoosterGenerator(sets[0]);
                for (int i = 0; i < nPacks; i++) {
                    packs.add(bpOne);
                    sumCards += bpOne.getBoosterPackSize();
                }
            }
            stopCount = sumCards;
            LandSetCode[0] = block.getLandSet().getCode();
            
        } else if (draftType.equals("Custom")) {    // Draft from user-defined cardpools
            String[] dList;
            ArrayList<CustomDraft> customs = new ArrayList<CustomDraft>();
            ArrayList<String> customList = new ArrayList<String>();

            // get list of custom draft files
            File dFolder = new File("res/draft/");
            if (!dFolder.exists()) {
                throw new RuntimeException("BoosterDraft : folder not found -- folder is " + dFolder.getAbsolutePath());
            }

            if (!dFolder.isDirectory()) {
                throw new RuntimeException("BoosterDraft : not a folder -- " + dFolder.getAbsolutePath());
            }

            dList = dFolder.list();

            for (int i = 0; i < dList.length; i++) {
                if (dList[i].endsWith(".draft")) {
                    ArrayList<String> dfData = FileUtil.readFile("res/draft/" + dList[i]);

                    CustomDraft cd = new CustomDraft();

                    for (int j = 0; j < dfData.size(); j++) {

                        String dfd = dfData.get(j);

                        if (dfd.startsWith("Name:")) {
                            cd.Name = dfd.substring(5);
                        }
                        if (dfd.startsWith("Type:")) {
                            cd.Type = dfd.substring(5);
                        }
                        if (dfd.startsWith("DeckFile:")) {
                            cd.DeckFile = dfd.substring(9);
                        }
                        if (dfd.startsWith("IgnoreRarity:")) {
                            cd.IgnoreRarity = dfd.substring(13).equals("True");
                        }
                        if (dfd.startsWith("LandSetCode:")) {
                            cd.LandSetCode = dfd.substring(12);
                        }

                        if (dfd.startsWith("NumCards:")) {
                            cd.NumCards = Integer.parseInt(dfd.substring(9));
                        }
                        if (dfd.startsWith("NumSpecials:")) {
                            cd.NumSpecials = Integer.parseInt(dfd.substring(12));
                        }
                        if (dfd.startsWith("NumMythics:")) {
                            cd.NumMythics = Integer.parseInt(dfd.substring(11));
                        }
                        if (dfd.startsWith("NumRares:")) {
                            cd.NumRares = Integer.parseInt(dfd.substring(9));
                        }
                        if (dfd.startsWith("NumUncommons:")) {
                            cd.NumUncommons = Integer.parseInt(dfd.substring(13));
                        }
                        if (dfd.startsWith("NumCommons:")) {
                            cd.NumCommons = Integer.parseInt(dfd.substring(11));
                        }
                        if (dfd.startsWith("NumPacks:")) {
                            cd.NumPacks = Integer.parseInt(dfd.substring(9));
                        }

                    }

                    customs.add(cd);
                    customList.add(cd.Name);
                }


            }
            CustomDraft chosenDraft = null;

            // present list to user
            if (customs.size() < 1) {
                JOptionPane.showMessageDialog(null, "No custom draft files found.", "", JOptionPane.INFORMATION_MESSAGE);
            } else {
                Object p = GuiUtils.getChoice("Choose Custom Draft", customList.toArray());

                for (int i = 0; i < customs.size(); i++) {
                    CustomDraft cd = customs.get(i);

                    if (cd.Name.equals(p.toString())) {
                        chosenDraft = cd;
                    }
                }

                if (chosenDraft.IgnoreRarity) {
                    chosenDraft.NumCommons = chosenDraft.NumCards;
                }

                BoosterGenerator bpCustom = new BoosterGenerator(chosenDraft.DeckFile, chosenDraft.NumCommons,
                        chosenDraft.NumUncommons, chosenDraft.NumRares, chosenDraft.NumMythics, chosenDraft.NumSpecials,
                        chosenDraft.IgnoreRarity);
                int n = 0;
                for (int i = 0; i < chosenDraft.NumPacks; i++) {
                    packs.add(bpCustom);
                    n += chosenDraft.NumCards; //bpCustom.getBoosterPackSize();
                }
                stopCount = n;

                LandSetCode[0] = chosenDraft.LandSetCode;
            }

        }

        pack = get8BoosterPack();
    }

    /**
     * <p>nextChoice.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public final ItemPoolView<CardPrinted> nextChoice() {
        if (pack.get(getCurrentBoosterIndex()).size() == 0) {
            pack = get8BoosterPack();
        }

        computerChoose();
        return ItemPool.createFrom(pack.get(getCurrentBoosterIndex()), CardPrinted.class);
    }

    /**
     * <p>get8BoosterPack.</p>
     *
     * @return an array of {@link forge.CardList} objects.
     */
    public final List<List<CardPrinted>> get8BoosterPack() {
        List<List<CardPrinted>> list = new ArrayList<List<CardPrinted>>();

        if (packNum < packs.size()) {
            for (int i = 0; i < 8; i++) {
                list.add(packs.get(packNum).getBoosterPack());
            }
        }

        packNum++;

        return list;
    } //get8BoosterPack()

    //size 7, all the computers decks

    /**
     * <p>getDecks.</p>
     *
     * @return an array of {@link forge.deck.Deck} objects.
     */
    public final Deck[] getDecks() {
        return draftAI.getDecks();
    }

    private void computerChoose() {
        int[] row = computerChoose[getCurrentBoosterIndex()];

        for (int i = 0; i < row.length; i++) {
            CardList forAi = new CardList();
            List<CardPrinted> booster = pack.get(row[i]);
            for (CardPrinted cr : booster) {
                forAi.add(cr.toForgeCard());
            }
            // TODO Please write this drafting code to work without heavy cards
            Card aiPick = draftAI.choose(forAi, i);
            String pickedName = aiPick.getName();

            for (int pick = booster.size() - 1; pick >= 0; pick--) {
                CardPrinted cp = booster.get(pick);
                if (cp.getName().equalsIgnoreCase(pickedName)) {
                    booster.remove(pick);
                    break;
                }
            }
        }
    } //computerChoose()

    private int getCurrentBoosterIndex() {
        return currentCount % nPlayers;
    }

    /**
     * <p>hasNextChoice.</p>
     *
     * @return a boolean.
     */
    public final boolean hasNextChoice() {
        return currentCount < stopCount;
    }

    /** {@inheritDoc} */
    public final void setChoice(final CardPrinted c) {
        List<CardPrinted> thisBooster = pack.get(getCurrentBoosterIndex());

        if (!thisBooster.contains(c)) {
            throw new RuntimeException("BoosterDraft : setChoice() error - card not found - " + c + " - booster pack = " + thisBooster);
        }

        if (Constant.Runtime.UpldDrft[0]) {
            for (int i = 0; i < thisBooster.size(); i++) {
                CardPrinted cc = thisBooster.get(i);
                String cnBk = cc.getName() + "|" + cc.getSet();

                float pickValue = 0;
                if (cc.equals(c)) {
                    pickValue = thisBooster.size() * (1f - ((float) currentCount) / stopCount) * 2f;
                } else {
                    pickValue = 0;
                }

                if (!draftPicks.containsKey(cnBk)) {
                    draftPicks.put(cnBk, pickValue);
                } else {
                    float curValue = draftPicks.get(cnBk);
                    float newValue = (curValue + pickValue) / 2;
                    draftPicks.put(cnBk, newValue);
                }
            }
        }

        thisBooster.remove(c);
        currentCount++;
    } //setChoice()
}
