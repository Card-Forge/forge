package forge;

import forge.card.CardPool;
import forge.card.CardPoolView;
import forge.card.CardPrinted;
import forge.deck.Deck;
import forge.gui.GuiUtils;

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
    //private static int boosterPackSize = 15;
    private static int stopCount = 45; //boosterPackSize * 3;//should total of 45

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
    BoosterDraft_1() {
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
            BoosterGenerator bpFull = new BoosterGenerator();
            for (int i = 0; i < 3; i++) {
                packs.add(bpFull);
            }

            LandSetCode[0] = AllZone.getCardFactory().getCard("Plains", AllZone.getHumanPlayer()).getMostRecentSet();
        } else if (draftType.equals("Block")) {    // Draft from cards by block or set
            ArrayList<String> bNames = SetInfoUtil.getBlockNameList();
            ArrayList<String> rbNames = new ArrayList<String>();
            for (int i = bNames.size() - 1; i >= 0; i--) {
                rbNames.add(bNames.get(i));
            }

            Object o = GuiUtils.getChoice("Choose Block", rbNames.toArray());

            ArrayList<String> blockSets = SetInfoUtil.getSetsBlockName(o.toString());
            int nPacks = SetInfoUtil.getDraftPackCount(o.toString());

            ArrayList<String> setCombos = new ArrayList<String>();

            //if (blockSets.get(1).equals("") && blockSets.get(2).equals("")) { // Block only has one set
            if (blockSets.size() == 1) {
                BoosterGenerator bpOne = new BoosterGenerator(blockSets.get(0));
                int n = 0;
                for (int i = 0; i < nPacks; i++) {
                    packs.add(bpOne);
                    n += bpOne.getBoosterPackSize();
                }
                stopCount = n;
            } else {
                //if (!blockSets.get(1).equals("") && blockSets.get(2).equals("")) { // Block only has two sets
                if (blockSets.size() == 2) {
                    setCombos.add(String.format("%s/%s/%s", blockSets.get(0), blockSets.get(0), blockSets.get(0)));
                    setCombos.add(String.format("%s/%s/%s", blockSets.get(1), blockSets.get(0), blockSets.get(0)));
                    setCombos.add(String.format("%s/%s/%s", blockSets.get(1), blockSets.get(1), blockSets.get(0)));
                    setCombos.add(String.format("%s/%s/%s", blockSets.get(1), blockSets.get(1), blockSets.get(1)));
                }
                //else if (!blockSets.get(1).equals("") && !blockSets.get(2).equals("")) { // Block has three sets
                else if (blockSets.size() == 3) {
                    setCombos.add(String.format("%s/%s/%s", blockSets.get(0), blockSets.get(0), blockSets.get(0)));
                    setCombos.add(String.format("%s/%s/%s", blockSets.get(1), blockSets.get(0), blockSets.get(0)));
                    setCombos.add(String.format("%s/%s/%s", blockSets.get(2), blockSets.get(1), blockSets.get(0)));
                    setCombos.add(String.format("%s/%s/%s", blockSets.get(1), blockSets.get(1), blockSets.get(0)));
                    setCombos.add(String.format("%s/%s/%s", blockSets.get(1), blockSets.get(1), blockSets.get(1)));
                    setCombos.add(String.format("%s/%s/%s", blockSets.get(2), blockSets.get(2), blockSets.get(0)));
                    setCombos.add(String.format("%s/%s/%s", blockSets.get(2), blockSets.get(2), blockSets.get(1)));
                    setCombos.add(String.format("%s/%s/%s", blockSets.get(2), blockSets.get(2), blockSets.get(2)));
                }

                Object p = GuiUtils.getChoice("Choose Set Combination", setCombos.toArray());

                String[] pp = p.toString().split("/");
                int n = 0;
                for (int i = 0; i < nPacks; i++) {
                    BoosterGenerator bpMulti = new BoosterGenerator(pp[i]);
                    packs.add(bpMulti);
                    n += bpMulti.getBoosterPackSize();
                }
                stopCount = n;
            }

            LandSetCode[0] = SetInfoUtil.getLandCode(o.toString());
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
    public final CardPoolView nextChoice() {
        if (pack.get(getCurrentBoosterIndex()).size() == 0) {
            pack = get8BoosterPack();
        }

        computerChoose();
        return new CardPool(pack.get(getCurrentBoosterIndex()));
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
