package forge.game.limited;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.Constant;
import forge.FileUtil;
import forge.HttpUtil;
import forge.SetUtils;
import forge.card.BoosterGenerator;
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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.ArrayUtils;

import net.slightlymagic.braids.util.lambda.Lambda1;
import net.slightlymagic.maxmtg.Closure1;

/**
 * 
 * TODO Write javadoc for this type.
 *
 */
public final class BoosterDraft_1 implements BoosterDraft {
    private final BoosterDraftAI draftAI = new BoosterDraftAI();
    private static final int nPlayers = 8;

    private int nextBoosterGroup = 0;
    private int currentBoosterSize = 0;
    private int currentBoosterPick = 0;
    private List<List<CardPrinted>> pack; //size 8
    
    public Map<String,Float> draftPicks = new TreeMap<String,Float>();
    private CardPoolLimitation draftFormat;

    private ArrayList<Closure1<List<CardPrinted>, BoosterGenerator>> packs = new ArrayList<Closure1<List<CardPrinted>, BoosterGenerator>>();

    /**
     * <p>Constructor for BoosterDraft_1.</p>
     *
     * @param draftType a {@link java.lang.String} object.
     */
    public BoosterDraft_1(CardPoolLimitation draftType) {
        draftAI.bd = this;
        draftFormat = draftType;

        switch (draftType) {
            case Full:    // Draft from all cards in Forge
                BoosterGenerator bpFull = new BoosterGenerator(CardDb.instance().getAllUniqueCards());
                Closure1<List<CardPrinted>, BoosterGenerator> picker = BoosterGenerator.getSimplePicker(bpFull);
                for (int i = 0; i < 3; i++) {
                    packs.add(picker);
                }
    
                LandSetCode[0] = CardDb.instance().getCard("Plains").getSet();
                break;
    
            case Block:   // Draft from cards by block or set
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
    
    
                if (sets.length > 1) {
                    Object p = GuiUtils.getChoice("Choose Set Combination", setCombos.toArray());
                    String[] pp = p.toString().split("/");
                    for (int i = 0; i < nPacks; i++) {
                        BoosterGenerator bpMulti = new BoosterGenerator(SetUtils.getSetByCode(pp[i]));
                        packs.add(BoosterGenerator.getSimplePicker(bpMulti));
                    }
    
                } else {
                    BoosterGenerator bpOne = new BoosterGenerator(SetUtils.getSetByCode(sets[0]));
                    Closure1<List<CardPrinted>, BoosterGenerator> pick1 = BoosterGenerator.getSimplePicker(bpOne);
                    for (int i = 0; i < nPacks; i++) { packs.add(pick1); }
                }
    
                LandSetCode[0] = block.getLandSet().getCode();
                break;
                
           case Custom:
                List<CustomLimited> myDrafts = loadCustomDrafts("res/draft/", ".draft");
                
                if (myDrafts.size() < 1) {
                    JOptionPane.showMessageDialog(null, "No custom draft files found.", "", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    CustomLimited draft = (CustomLimited) GuiUtils.getChoice("Choose Custom Draft", myDrafts.toArray());
                    setupCustomDraft(draft);
                }
                break;
            default: 
                throw new NoSuchElementException("Draft for mode " + draftType + " has not been set up!");
        }

        pack = get8BoosterPack();
    }

    private void setupCustomDraft(final CustomLimited draft)
    {
        DeckManager dio = AllZone.getDeckManager();
        Deck dPool = dio.getDeck(draft.DeckFile);
        if (dPool == null) {
            throw new RuntimeException("BoosterGenerator : deck not found - " + draft.DeckFile);
        }

        BoosterGenerator bpCustom = new BoosterGenerator(dPool);
        Lambda1<List<CardPrinted>, BoosterGenerator> fnPick = new Lambda1<List<CardPrinted>, BoosterGenerator>() {
            @Override public List<CardPrinted> apply(BoosterGenerator pack) {
                if ( draft.IgnoreRarity ) {
                    if (!draft.Singleton) {
	                    return pack.getBoosterPack(0, 0, 0, 0, 0, 0, 0, draft.NumCards, 0);
                    } else {
                        return pack.getSingletonBoosterPack(draft.NumCards);
                    }
                }
                return pack.getBoosterPack(draft.NumCommons, draft.NumUncommons, 0, draft.NumRares, draft.NumMythics, draft.NumSpecials, 0, 0, 0);
            }
        };

        Closure1<List<CardPrinted>, BoosterGenerator> picker = new Closure1<List<CardPrinted>, BoosterGenerator>(fnPick, bpCustom);
        for (int i = 0; i < draft.NumPacks; i++) { packs.add(picker); }

        LandSetCode[0] = draft.LandSetCode;
    }
    
    /** Looks for res/draft/*.draft files, reads them, returns a list */
    private List<CustomLimited> loadCustomDrafts(String lookupFolder, String fileExtension)
    {
        String[] dList;
        ArrayList<CustomLimited> customs = new ArrayList<CustomLimited>();

        // get list of custom draft files
        File dFolder = new File(lookupFolder);
        if (!dFolder.exists()) {
            throw new RuntimeException("BoosterDraft : folder not found -- folder is " + dFolder.getAbsolutePath());
        }

        if (!dFolder.isDirectory()) {
            throw new RuntimeException("BoosterDraft : not a folder -- " + dFolder.getAbsolutePath());
        }

        dList = dFolder.list();

        for (int i = 0; i < dList.length; i++) {
            if (dList[i].endsWith(fileExtension)) {
                List<String> dfData = FileUtil.readFile(lookupFolder + dList[i]);
                customs.add(CustomLimited.parse(dfData));
            }
        }
        return customs;
    }
    
    
    /**
     * <p>nextChoice.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public ItemPoolView<CardPrinted> nextChoice() {
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
    public List<List<CardPrinted>> get8BoosterPack() {
        if (nextBoosterGroup >= packs.size()) { return null; }

        List<List<CardPrinted>> list = new ArrayList<List<CardPrinted>>();
        for (int i = 0; i < 8; i++) { list.add(packs.get(nextBoosterGroup).apply()); }
        
        nextBoosterGroup++;
        currentBoosterSize = list.get(0).size();
        currentBoosterPick = 0;
        return list;
    }

    //size 7, all the computers decks

    /**
     * <p>getDecks.</p>
     *
     * @return an array of {@link forge.deck.Deck} objects.
     */
    public Deck[] getDecks() {
        return draftAI.getDecks();
    }

    private void computerChoose() {

        int iHumansBooster = getCurrentBoosterIndex();
        int iPlayer = 0;
        for (int i = 0; i < pack.size(); i++) {
            if (iHumansBooster == i) { continue; } // don't touch player's booster

            CardList forAi = new CardList();
            List<CardPrinted> booster = pack.get(i);
            for (CardPrinted cr : booster) {
                forAi.add(cr.toForgeCard());
            }
            // TODO: Please write this drafting code to work without heavy card objects
            Card aiPick = draftAI.choose(forAi, iPlayer++);
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
        return currentBoosterPick % nPlayers;
    }

    /**
     * <p>hasNextChoice.</p>
     *
     * @return a boolean.
     */
    public boolean hasNextChoice() {
        boolean isLastGroup = nextBoosterGroup >= packs.size();
        boolean isBoosterDepleted = currentBoosterPick >= currentBoosterSize; 
        boolean noMoreCards = isLastGroup && isBoosterDepleted; 
        return !noMoreCards;
    }

    /** {@inheritDoc} */
    public void setChoice(final CardPrinted c) {
        List<CardPrinted> thisBooster = pack.get(getCurrentBoosterIndex());

        if (!thisBooster.contains(c)) {
            throw new RuntimeException("BoosterDraft : setChoice() error - card not found - " + c + " - booster pack = " + thisBooster);
        }

        if (Constant.Runtime.UPLOAD_DRAFT[0]) {
            for (int i = 0; i < thisBooster.size(); i++) {
                CardPrinted cc = thisBooster.get(i);
                String cnBk = cc.getName() + "|" + cc.getSet();

                float pickValue = 0;
                if (cc.equals(c)) {
                    pickValue = thisBooster.size() * (1f - ((float) currentBoosterPick / currentBoosterSize) * 2f);
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
        currentBoosterPick++;
    } //setChoice()

    /** This will upload drafting picks to cardforge HQ. */
    public void finishedDrafting() {
        if (Constant.Runtime.UPLOAD_DRAFT[0]) {
            if (draftPicks.size() > 1) {
                ArrayList<String> outDraftData = new ArrayList<String>();

                String[] keys = draftPicks.keySet().toArray(ArrayUtils.EMPTY_STRING_ARRAY);

                for (int i = 0; i < keys.length; i++) {
                    outDraftData.add(keys[i] + "|" + draftPicks.get(keys[i]));
                }

                FileUtil.writeFile("res/draft/tmpDraftData.txt", outDraftData);

                HttpUtil poster = new HttpUtil();
                poster.upload("http://cardforge.org/draftAI/submitDraftData.php?fmt="
                + draftFormat, "res/draft/tmpDraftData.txt");
            }
        }
    }
}
