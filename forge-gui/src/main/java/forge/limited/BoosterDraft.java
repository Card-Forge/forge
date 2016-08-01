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
package forge.limited;

import com.google.common.base.Supplier;
import forge.card.CardEdition;
import forge.card.IUnOpenedProduct;
import forge.card.UnOpenedProduct;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.item.SealedProduct;
import forge.model.CardBlock;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;
import forge.util.FileUtil;
import forge.util.ItemPool;
import forge.util.gui.SGuiChoose;
import forge.util.gui.SOptionPane;
import forge.util.storage.IStorage;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.util.*;

/**
 * Booster Draft Format.
 */
public class BoosterDraft implements IBoosterDraft {
    private final BoosterDraftAI draftAI = new BoosterDraftAI();
    private static final int N_PLAYERS = 8;
    public static final String FILE_EXT = ".draft";

    protected int nextBoosterGroup = 0;
    private int currentBoosterSize = 0;
    private int currentBoosterPick = 0;
    private int[] draftingBooster;

    private List<List<PaperCard>> pack; // size 8

    /** The draft picks. */
    private final Map<String, Float> draftPicks = new TreeMap<>();
    protected LimitedPoolType draftFormat;

    protected final List<Supplier<List<PaperCard>>> product = new ArrayList<>();

    public static BoosterDraft createDraft(final LimitedPoolType draftType) {
        final BoosterDraft draft = new BoosterDraft(draftType);
        if (!draft.generateProduct()) { return null; }
        return draft;
    }

    protected boolean generateProduct() {
        switch (this.draftFormat) {
        case Full: // Draft from all cards in Forge
            final Supplier<List<PaperCard>> s = new UnOpenedProduct(SealedProduct.Template.genericBooster);

            for (int i = 0; i < 3; i++) {
                this.product.add(s);
            }
            IBoosterDraft.LAND_SET_CODE[0] = CardEdition.Predicates.getRandomSetWithAllBasicLands(FModel.getMagicDb().getEditions());
            IBoosterDraft.CUSTOM_RANKINGS_FILE[0] = null;
            break;

        case Block: // Draft from cards by block or set
        case FantasyBlock:
            final List<CardBlock> blocks = new ArrayList<>();
            final IStorage<CardBlock> storage = this.draftFormat == LimitedPoolType.Block
                    ? FModel.getBlocks()
                    : FModel.getFantasyBlocks();

            for (final CardBlock b : storage) {
                if (b.getCntBoostersDraft() > 0) {
                    blocks.add(b);
                }
            }

            final CardBlock block = SGuiChoose.oneOrNone("Choose Block", blocks);
            if (block == null) { return false; }

            final List<CardEdition> cardSets = block.getSets();
            if (cardSets.isEmpty()) {
                SOptionPane.showErrorDialog(block.toString() + " does not contain any set combinations.");
                return false;
            }

            final Stack<String> sets = new Stack<>();
            for (int k = cardSets.size() - 1; k >= 0; k--) {
                sets.add(cardSets.get(k).getCode());
            }

            for (final String setCode : block.getMetaSetNames()) {
                if (block.getMetaSet(setCode).isDraftable()) {
                    sets.push(setCode); // to the beginning
                }
            }

            final int nPacks = block.getCntBoostersDraft();

            if (sets.size() > 1) {
                final Object p = SGuiChoose.oneOrNone("Choose Set Combination", getSetCombos(sets));
                if (p == null) { return false; }

                final String[] pp = p.toString().split("/");
                for (int i = 0; i < nPacks; i++) {
                    this.product.add(block.getBooster(pp[i]));
                }
            }
            else {
                final IUnOpenedProduct product1 = block.getBooster(sets.get(0));

                for (int i = 0; i < nPacks; i++) {
                    this.product.add(product1);
                }
            }

            IBoosterDraft.LAND_SET_CODE[0] = block.getLandSet();
            IBoosterDraft.CUSTOM_RANKINGS_FILE[0] = null;
            break;

        case Custom:
            final List<CustomLimited> myDrafts = loadCustomDrafts();

            if (myDrafts.isEmpty()) {
                SOptionPane.showMessageDialog("No custom draft files found.");
            }
            else {
                final CustomLimited customDraft = SGuiChoose.oneOrNone("Choose Custom Draft", myDrafts);
                if (customDraft == null) { return false; }

                this.setupCustomDraft(customDraft);
            }
            break;

        default:
            throw new NoSuchElementException("Draft for mode " + this.draftFormat + " has not been set up!");
        }

        this.pack = this.get8BoosterPack();
        return true;
    }

    public static BoosterDraft createDraft(final LimitedPoolType draftType, final CardBlock block, final String[] boosters) {
        final BoosterDraft draft = new BoosterDraft(draftType);

        for (String booster : boosters) {
            draft.product.add(block.getBooster(booster));
        }

        IBoosterDraft.LAND_SET_CODE[0] = block.getLandSet();
        IBoosterDraft.CUSTOM_RANKINGS_FILE[0] = null;

        draft.pack = draft.get8BoosterPack();
        return draft;
    }

    protected BoosterDraft() {
        this.draftFormat = LimitedPoolType.Full;
    }
    protected BoosterDraft(final LimitedPoolType draftType) {
        this.draftAI.setBd(this);
        this.draftFormat = draftType;
    }

    private void setupCustomDraft(final CustomLimited draft) {
        final ItemPool<PaperCard> dPool = draft.getCardPool();
        if (dPool == null) {
            throw new RuntimeException("BoosterGenerator : deck not found");
        }

        final SealedProduct.Template tpl = draft.getSealedProductTemplate();

        final UnOpenedProduct toAdd = new UnOpenedProduct(tpl, dPool);
        toAdd.setLimitedPool(draft.isSingleton());
        for (int i = 0; i < draft.getNumPacks(); i++) {
            this.product.add(toAdd);
        }

        IBoosterDraft.LAND_SET_CODE[0] = FModel.getMagicDb().getEditions().get(draft.getLandSetCode());
        IBoosterDraft.CUSTOM_RANKINGS_FILE[0] = draft.getCustomRankingsFileName();
    }

    /** Looks for draft files, reads them, returns a list. */
    private static List<CustomLimited> loadCustomDrafts() {
        String[] dList;
        final List<CustomLimited> customs = new ArrayList<>();

        // get list of custom draft files
        final File dFolder = new File(ForgeConstants.DRAFT_DIR);
        if (!dFolder.exists()) {
            throw new RuntimeException("BoosterDraft : folder not found -- folder is " + dFolder.getAbsolutePath());
        }

        if (!dFolder.isDirectory()) {
            throw new RuntimeException("BoosterDraft : not a folder -- " + dFolder.getAbsolutePath());
        }

        dList = dFolder.list();

        for (final String element : dList) {
            if (element.endsWith(FILE_EXT)) {
                final List<String> dfData = FileUtil.readFile(ForgeConstants.DRAFT_DIR + element);
                customs.add(CustomLimited.parse(dfData, FModel.getDecks().getCubes()));
            }
        }
        return customs;
    }

    /**
     * <p>
     * nextChoice.
     * </p>
     *
     * @return a {@link forge.deck.CardPool} object.
     */
    @Override
    public CardPool nextChoice() {
        if (this.isRoundOver()) {
            // If all packs are depleted crack 8 new packs
            this.pack = this.get8BoosterPack();
            if (this.pack == null) {
                return null;
            }
        }

        this.computerChoose();

        final CardPool result = new CardPool();
        result.addAllFlat(this.pack.get(this.getCurrentBoosterIndex()));

        if (result.isEmpty()) {
            // Can't set a card, since none are available. Just pass "empty" packs.
            this.passPacks();
            // Recur until we find a cardpool or finish
            return nextChoice();
        }

        return result;
    }

    /**
     * <p>
     * get8BoosterPack.
     * </p>
     *
     * @return an array of {@link forge.deck.CardPool} objects.
     */
    public List<List<PaperCard>> get8BoosterPack() {
        if (this.nextBoosterGroup >= this.product.size()) {
            return null;
        }

        final List<List<PaperCard>> list = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            list.add(this.product.get(this.nextBoosterGroup).get());
        }

        this.nextBoosterGroup++;
        this.currentBoosterSize = list.get(0).size();
        this.currentBoosterPick = 0;
        draftingBooster = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
        return list;
    }

    public void addSingleBoosterPack(int player, boolean random) {
        // TODO Cogwork Librarian
    }

    // size 7, all the computers decks
    @Override
    public Deck[] getDecks() {
        return this.draftAI.getDecks();
    }

    public void passPacks() {
        // Alternate direction of pack passing
        int adjust = this.nextBoosterGroup % 2 == 1 ? 1 : -1;
        for(int i = 0; i < N_PLAYERS; i++) {
            draftingBooster[i] = (draftingBooster[i] + adjust + pack.size()) % pack.size();
        }
    }

    protected void computerChoose() {
        // Loop through players 1-7 to draft their current pack
        for (int i = 1; i < N_PLAYERS; i++) {
            final List<PaperCard> booster = this.pack.get(this.draftingBooster[i]);

            // Empty boosters can happen in a Conspiracy draft
            if (!booster.isEmpty()) {
                booster.remove(this.draftAI.choose(booster, i-1));
            }
        }
    } // computerChoose()

    /**
     *
     * Get the current booster index for the Human
     * @return int
     */
    public int getCurrentBoosterIndex() {
        return this.draftingBooster[0];
    }

    @Override
    public boolean isRoundOver() {
        for(List<PaperCard> singlePack : this.pack) {
            if (!singlePack.isEmpty()) {
                return false;
            }
        }

        return true;
    }


    @Override
    public boolean hasNextChoice() {
        return this.nextBoosterGroup < this.product.size() || !this.isRoundOver();
    }

    /** {@inheritDoc} */
    @Override
    public void setChoice(final PaperCard c) {
        final List<PaperCard> thisBooster = this.pack.get(this.getCurrentBoosterIndex());

        if (!thisBooster.contains(c)) {
            throw new RuntimeException("BoosterDraft : setChoice() error - card not found - " + c
                    + " - booster pack = " + thisBooster);
        }

        if (ForgePreferences.UPLOAD_DRAFT) {
            for (int i = 0; i < thisBooster.size(); i++) {
                final PaperCard cc = thisBooster.get(i);
                final String cnBk = cc.getName() + "|" + cc.getEdition();

                float pickValue;
                if (cc.equals(c)) {
                    pickValue = thisBooster.size()
                            * (1f - (((float) this.currentBoosterPick / this.currentBoosterSize) * 2f));
                }
                else {
                    pickValue = 0;
                }

                if (!this.draftPicks.containsKey(cnBk)) {
                    this.draftPicks.put(cnBk, pickValue);
                }
                else {
                    final float curValue = this.draftPicks.get(cnBk);
                    final float newValue = (curValue + pickValue) / 2;
                    this.draftPicks.put(cnBk, newValue);
                }
            }
        }

        thisBooster.remove(c);
        this.currentBoosterPick++;
        this.passPacks();
    } // setChoice()

    @Override
    public boolean isPileDraft() {
        return false;
    }

    private static List<String> getSetCombos(final List<String> setz) {
        final String[] sets = setz.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
        final List<String> setCombos = new ArrayList<>();
        if (sets.length >= 2) {
            setCombos.add(String.format("%s/%s/%s", sets[0], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s", sets[0], sets[0], sets[1]));
            setCombos.add(String.format("%s/%s/%s", sets[0], sets[1], sets[1]));
            if (sets.length >= 3) {
                setCombos.add(String.format("%s/%s/%s", sets[0], sets[1], sets[2]));
                setCombos.add(String.format("%s/%s/%s", sets[0], sets[2], sets[2]));
            }
            setCombos.add(String.format("%s/%s/%s", sets[1], sets[0], sets[0]));
            setCombos.add(String.format("%s/%s/%s", sets[1], sets[1], sets[0]));
            setCombos.add(String.format("%s/%s/%s", sets[1], sets[1], sets[1]));
            if (sets.length >= 3) {
                setCombos.add(String.format("%s/%s/%s", sets[1], sets[1], sets[2]));
                setCombos.add(String.format("%s/%s/%s", sets[1], sets[2], sets[2]));
            }
        }
        if (sets.length >= 3) {
            setCombos.add(String.format("%s/%s/%s", sets[2], sets[1], sets[0]));
            setCombos.add(String.format("%s/%s/%s", sets[2], sets[2], sets[0]));
            setCombos.add(String.format("%s/%s/%s", sets[2], sets[2], sets[1]));
            setCombos.add(String.format("%s/%s/%s", sets[2], sets[2], sets[2]));
        } // Beyond 3, skimp on the choice configurations, or the list will be enormous!
        if (sets.length >= 4) {
            setCombos.add(String.format("%s/%s/%s", sets[3], sets[1], sets[0]));
            setCombos.add(String.format("%s/%s/%s", sets[3], sets[2], sets[1]));
        }
        if (sets.length >= 5) {
            setCombos.add(String.format("%s/%s/%s", sets[4], sets[1], sets[0]));
            setCombos.add(String.format("%s/%s/%s", sets[4], sets[3], sets[2]));
            setCombos.add(String.format("%s/%s/%s", sets[4], sets[2], sets[0]));
        }
        if (sets.length >= 6) {
            setCombos.add(String.format("%s/%s/%s", sets[5], sets[1], sets[0]));
            setCombos.add(String.format("%s/%s/%s", sets[5], sets[3], sets[2]));
            setCombos.add(String.format("%s/%s/%s", sets[5], sets[4], sets[3]));
            setCombos.add(String.format("%s/%s/%s", sets[5], sets[2], sets[0]));
        }
        if (sets.length >= 7) {
            setCombos.add(String.format("%s/%s/%s", sets[6], sets[1], sets[0]));
            setCombos.add(String.format("%s/%s/%s", sets[6], sets[3], sets[2]));
            setCombos.add(String.format("%s/%s/%s", sets[6], sets[5], sets[4]));
            setCombos.add(String.format("%s/%s/%s", sets[6], sets[3], sets[0]));
        }
        if (sets.length >= 8) {
            setCombos.add(String.format("%s/%s/%s", sets[7], sets[1], sets[0]));
            setCombos.add(String.format("%s/%s/%s", sets[7], sets[3], sets[2]));
            setCombos.add(String.format("%s/%s/%s", sets[7], sets[5], sets[4]));
            setCombos.add(String.format("%s/%s/%s", sets[7], sets[6], sets[5]));
            setCombos.add(String.format("%s/%s/%s", sets[7], sets[3], sets[0]));
        }
        if (sets.length >= 9) {
            setCombos.add(String.format("%s/%s/%s", sets[8], sets[1], sets[0]));
            setCombos.add(String.format("%s/%s/%s", sets[8], sets[3], sets[2]));
            setCombos.add(String.format("%s/%s/%s", sets[8], sets[5], sets[4]));
            setCombos.add(String.format("%s/%s/%s", sets[8], sets[7], sets[6]));
            setCombos.add(String.format("%s/%s/%s", sets[8], sets[4], sets[0]));
        }
        return setCombos;
    }
}
