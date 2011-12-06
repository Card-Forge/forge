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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import net.slightlymagic.braids.util.lambda.Lambda1;
import net.slightlymagic.maxmtg.Closure1;

import org.apache.commons.lang3.ArrayUtils;

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

/**
 * 
 * TODO Write javadoc for this type.
 * 
 */
public final class BoosterDraft implements IBoosterDraft {
    private final BoosterDraftAI draftAI = new BoosterDraftAI();
    private static final int N_PLAYERS = 8;

    private int nextBoosterGroup = 0;
    private int currentBoosterSize = 0;
    private int currentBoosterPick = 0;
    private List<List<CardPrinted>> pack; // size 8

    /** The draft picks. */
    private final Map<String, Float> draftPicks = new TreeMap<String, Float>();
    private final CardPoolLimitation draftFormat;

    private final ArrayList<Closure1<List<CardPrinted>, BoosterGenerator>> packs = new ArrayList<Closure1<List<CardPrinted>, BoosterGenerator>>();

    /**
     * <p>
     * Constructor for BoosterDraft_1.
     * </p>
     * 
     * @param draftType
     *            a {@link java.lang.String} object.
     */
    public BoosterDraft(final CardPoolLimitation draftType) {
        this.draftAI.setBd(this);
        this.draftFormat = draftType;

        switch (draftType) {
        case Full: // Draft from all cards in Forge
            final BoosterGenerator bpFull = new BoosterGenerator(CardDb.instance().getAllUniqueCards());
            final Closure1<List<CardPrinted>, BoosterGenerator> picker = BoosterGenerator.getSimplePicker(bpFull);
            for (int i = 0; i < 3; i++) {
                this.packs.add(picker);
            }

            IBoosterDraft.LAND_SET_CODE[0] = CardDb.instance().getCard("Plains").getSet();
            break;

        case Block: // Draft from cards by block or set
            final List<CardBlock> blocks = SetUtils.getBlocks();

            final Object o = GuiUtils.getChoice("Choose Block", blocks.toArray());
            final CardBlock block = (CardBlock) o;

            final CardSet[] cardSets = block.getSets();
            final String[] sets = new String[cardSets.length];
            for (int k = cardSets.length - 1; k >= 0; --k) {
                sets[k] = cardSets[k].getCode();
            }

            final int nPacks = block.getCntBoostersDraft();

            final ArrayList<String> setCombos = new ArrayList<String>();
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
                final Object p = GuiUtils.getChoice("Choose Set Combination", setCombos.toArray());
                final String[] pp = p.toString().split("/");
                for (int i = 0; i < nPacks; i++) {
                    final BoosterGenerator bpMulti = new BoosterGenerator(SetUtils.getSetByCode(pp[i]));
                    this.packs.add(BoosterGenerator.getSimplePicker(bpMulti));
                }

            } else {
                final BoosterGenerator bpOne = new BoosterGenerator(SetUtils.getSetByCode(sets[0]));
                final Closure1<List<CardPrinted>, BoosterGenerator> pick1 = BoosterGenerator.getSimplePicker(bpOne);
                for (int i = 0; i < nPacks; i++) {
                    this.packs.add(pick1);
                }
            }

            IBoosterDraft.LAND_SET_CODE[0] = block.getLandSet().getCode();
            break;

        case Custom:
            final List<CustomLimited> myDrafts = this.loadCustomDrafts("res/draft/", ".draft");

            if (myDrafts.size() < 1) {
                JOptionPane
                        .showMessageDialog(null, "No custom draft files found.", "", JOptionPane.INFORMATION_MESSAGE);
            } else {
                final CustomLimited draft = (CustomLimited) GuiUtils.getChoice("Choose Custom Draft",
                        myDrafts.toArray());
                this.setupCustomDraft(draft);
            }
            break;
        default:
            throw new NoSuchElementException("Draft for mode " + draftType + " has not been set up!");
        }

        this.pack = this.get8BoosterPack();
    }

    private void setupCustomDraft(final CustomLimited draft) {
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
                    if (!draft.getSingleton()) {
                        return pack.getBoosterPack(0, 0, 0, 0, 0, 0, 0, draft.getNumCards(), 0);
                    } else {
                        return pack.getSingletonBoosterPack(draft.getNumCards());
                    }
                }
                return pack.getBoosterPack(draft.getNumCommons(), draft.getNumUncommons(), 0, draft.getNumRares(),
                        draft.getNumMythics(), draft.getNumSpecials(), 0, 0, 0);
            }
        };

        final Closure1<List<CardPrinted>, BoosterGenerator> picker = new Closure1<List<CardPrinted>, BoosterGenerator>(
                fnPick, bpCustom);
        for (int i = 0; i < draft.getNumPacks(); i++) {
            this.packs.add(picker);
        }

        IBoosterDraft.LAND_SET_CODE[0] = draft.getLandSetCode();
    }

    /** Looks for res/draft/*.draft files, reads them, returns a list. */
    private List<CustomLimited> loadCustomDrafts(final String lookupFolder, final String fileExtension) {
        String[] dList;
        final ArrayList<CustomLimited> customs = new ArrayList<CustomLimited>();

        // get list of custom draft files
        final File dFolder = new File(lookupFolder);
        if (!dFolder.exists()) {
            throw new RuntimeException("BoosterDraft : folder not found -- folder is " + dFolder.getAbsolutePath());
        }

        if (!dFolder.isDirectory()) {
            throw new RuntimeException("BoosterDraft : not a folder -- " + dFolder.getAbsolutePath());
        }

        dList = dFolder.list();

        for (final String element : dList) {
            if (element.endsWith(fileExtension)) {
                final List<String> dfData = FileUtil.readFile(lookupFolder + element);
                customs.add(CustomLimited.parse(dfData));
            }
        }
        return customs;
    }

    /**
     * <p>
     * nextChoice.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    @Override
    public ItemPoolView<CardPrinted> nextChoice() {
        if (this.pack.get(this.getCurrentBoosterIndex()).size() == 0) {
            this.pack = this.get8BoosterPack();
        }

        this.computerChoose();
        return ItemPool.createFrom(this.pack.get(this.getCurrentBoosterIndex()), CardPrinted.class);
    }

    /**
     * <p>
     * get8BoosterPack.
     * </p>
     * 
     * @return an array of {@link forge.CardList} objects.
     */
    public List<List<CardPrinted>> get8BoosterPack() {
        if (this.nextBoosterGroup >= this.packs.size()) {
            return null;
        }

        final List<List<CardPrinted>> list = new ArrayList<List<CardPrinted>>();
        for (int i = 0; i < 8; i++) {
            list.add(this.packs.get(this.nextBoosterGroup).apply());
        }

        this.nextBoosterGroup++;
        this.currentBoosterSize = list.get(0).size();
        this.currentBoosterPick = 0;
        return list;
    }

    // size 7, all the computers decks

    /**
     * <p>
     * getDecks.
     * </p>
     * 
     * @return an array of {@link forge.deck.Deck} objects.
     */
    @Override
    public Deck[] getDecks() {
        return this.draftAI.getDecks();
    }

    private void computerChoose() {

        final int iHumansBooster = this.getCurrentBoosterIndex();
        int iPlayer = 0;
        for (int i = 1; i < this.pack.size(); i++) {

            final CardList forAi = new CardList();
            final List<CardPrinted> booster = this.pack.get((iHumansBooster+i) % this.pack.size());
            for (final CardPrinted cr : booster) {
                forAi.add(cr.toForgeCard());
            }
            // TODO: Please write this drafting code to work without heavy card
            // objects
            final Card aiPick = this.draftAI.choose(forAi, iPlayer++);
            final String pickedName = aiPick.getName();

            for (int pick = booster.size() - 1; pick >= 0; pick--) {
                final CardPrinted cp = booster.get(pick);
                if (cp.getName().equalsIgnoreCase(pickedName)) {
                    booster.remove(pick);
                    break;
                }
            }
        }
    } // computerChoose()

    private int getCurrentBoosterIndex() {
        return this.currentBoosterPick % BoosterDraft.N_PLAYERS;
    }

    /**
     * <p>
     * hasNextChoice.
     * </p>
     * 
     * @return a boolean.
     */
    @Override
    public boolean hasNextChoice() {
        final boolean isLastGroup = this.nextBoosterGroup >= this.packs.size();
        final boolean isBoosterDepleted = this.currentBoosterPick >= this.currentBoosterSize;
        final boolean noMoreCards = isLastGroup && isBoosterDepleted;
        return !noMoreCards;
    }

    /** {@inheritDoc} */
    @Override
    public void setChoice(final CardPrinted c) {
        final List<CardPrinted> thisBooster = this.pack.get(this.getCurrentBoosterIndex());

        if (!thisBooster.contains(c)) {
            throw new RuntimeException("BoosterDraft : setChoice() error - card not found - " + c
                    + " - booster pack = " + thisBooster);
        }

        if (Constant.Runtime.UPLOAD_DRAFT[0]) {
            for (int i = 0; i < thisBooster.size(); i++) {
                final CardPrinted cc = thisBooster.get(i);
                final String cnBk = cc.getName() + "|" + cc.getSet();

                float pickValue = 0;
                if (cc.equals(c)) {
                    pickValue = thisBooster.size()
                            * (1f - (((float) this.currentBoosterPick / this.currentBoosterSize) * 2f));
                } else {
                    pickValue = 0;
                }

                if (!this.draftPicks.containsKey(cnBk)) {
                    this.draftPicks.put(cnBk, pickValue);
                } else {
                    final float curValue = this.draftPicks.get(cnBk);
                    final float newValue = (curValue + pickValue) / 2;
                    this.draftPicks.put(cnBk, newValue);
                }
            }
        }

        thisBooster.remove(c);
        this.currentBoosterPick++;
    } // setChoice()

    /** This will upload drafting picks to cardforge HQ. */
    @Override
    public void finishedDrafting() {
        if (Constant.Runtime.UPLOAD_DRAFT[0]) {
            if (this.draftPicks.size() > 1) {
                final ArrayList<String> outDraftData = new ArrayList<String>();

                final String[] keys = this.draftPicks.keySet().toArray(ArrayUtils.EMPTY_STRING_ARRAY);

                for (final String key : keys) {
                    outDraftData.add(key + "|" + this.draftPicks.get(key));
                }

                FileUtil.writeFile("res/draft/tmpDraftData.txt", outDraftData);

                final HttpUtil poster = new HttpUtil();
                poster.upload("http://cardforge.org/draftAI/submitDraftData.php?fmt=" + this.draftFormat,
                        "res/draft/tmpDraftData.txt");
            }
        }
    }
}
