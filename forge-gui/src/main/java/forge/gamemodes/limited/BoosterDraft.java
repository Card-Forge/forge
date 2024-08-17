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
package forge.gamemodes.limited;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import forge.StaticData;
import forge.card.CardEdition;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckBase;
import forge.gui.util.SGuiChoose;
import forge.gui.util.SOptionPane;
import forge.item.PaperCard;
import forge.item.SealedTemplate;
import forge.item.generation.ChaosBoosterSupplier;
import forge.item.generation.IUnOpenedProduct;
import forge.item.generation.UnOpenedProduct;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.CardBlock;
import forge.model.FModel;
import forge.util.FileUtil;
import forge.util.ItemPool;
import forge.util.Localizer;
import forge.util.TextUtil;
import forge.util.storage.IStorage;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.util.*;

/**
 * Booster Draft Format.
 */
public class BoosterDraft implements IBoosterDraft {

    private int nextId = 0;
    private static final int N_PLAYERS = 8;
    public static final String FILE_EXT = ".draft";
    private final List<LimitedPlayer> players = new ArrayList<>();
    private final LimitedPlayer localPlayer;

    private IDraftLog draftLog = null;

    private String doublePickDuringDraft = ""; // "FirstPick" or "Always"
    protected int nextBoosterGroup = 0;
    private int currentBoosterSize = 0;
    private int currentBoosterPick = 0;
    private int packsInDraft;

    private final Map<String, Float> draftPicks = new TreeMap<>();
    static final List<CustomLimited> customs = new ArrayList<>();
    protected LimitedPoolType draftFormat;

    protected final List<Supplier<List<PaperCard>>> product = new ArrayList<>();
    public static void initializeCustomDrafts() {
        loadCustomDrafts();
    }
    public static BoosterDraft createDraft(final LimitedPoolType draftType) {
        final BoosterDraft draft = new BoosterDraft(draftType);
        if (!draft.generateProduct()) {
            return null;
        }
        draft.initializeBoosters();
        return draft;
    }

    protected boolean generateProduct() {
        switch (this.draftFormat) {
            case Full: // Draft from all cards in Forge
                final Supplier<List<PaperCard>> s = new UnOpenedProduct(SealedTemplate.genericDraftBooster);

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

                final CardBlock block = SGuiChoose.oneOrNone(Localizer.getInstance().getMessage("lblChooseBlock"), blocks);
                if (block == null) {
                    return false;
                }

                final List<CardEdition> cardSets = block.getSets();
                final Stack<String> sets = new Stack<>();
                for (int k = cardSets.size() - 1; k >= 0; k--) {
                    sets.add(cardSets.get(k).getCode());
                }

                for (final String setCode : block.getMetaSetNames()) {
                    if (block.getMetaSet(setCode).isDraftable()) {
                        sets.push(setCode); // to the beginning
                    }
                }

                if (sets.isEmpty()) {
                    SOptionPane.showErrorDialog(Localizer.getInstance().getMessage("lblBlockNotContainSetCombinations", block.toString()));
                    return false;
                }

                final int nPacks = block.getCntBoostersDraft();

                if (sets.size() > 1) {
                    Object p;
                    if (nPacks == 3 && sets.size() < 4) {
                        p = SGuiChoose.oneOrNone(Localizer.getInstance().getMessage("lblChooseSetCombination"), getSetCombos(sets));
                    } else {
                        p = choosePackByPack(sets, nPacks);
                    }

                    if (p == null) {
                        return false;
                    }

                    final String[] pp = p.toString().split("/");
                    for (int i = 0; i < nPacks; i++) {
                        this.product.add(block.getBooster(pp[i]));
                    }
                } else {
                    // Only one set is chosen. If that set lets you draft 2 cards to start adjust draft settings now
                    String setCode = sets.get(0);
                    CardEdition edition = FModel.getMagicDb().getEditions().get(setCode);
                    // If this is metaset, edtion will be null
                    if (edition != null) {
                        doublePickDuringDraft = edition.getDoublePickDuringDraft();
                    }

                    final IUnOpenedProduct product1 = block.getBooster(setCode);

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
                    SOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblNotFoundCustomDraftFiles"));
                } else {
                    myDrafts.sort(Comparator.comparing(DeckBase::getName));

                    final CustomLimited customDraft = SGuiChoose.oneOrNone(Localizer.getInstance().getMessage("lblChooseCustomDraft"), myDrafts);
                    if (customDraft == null) {
                        return false;
                    }

                    this.setupCustomDraft(customDraft);
                }
                break;

            case Chaos:
                /**
                 * A chaos draft consists of boosters from many different sets.
                 * Default settings are boosters from all sets with a booster size of 15 cards.
                 * Alternatively, the sets can be restricted to a format like Modern or to a theme.
                 * Examples for themes: sets that take place on a certain plane, core sets, masters sets,
                 * or sets that share a mechanic.
                 */
                // Get chaos draft themes
                final List<ThemedChaosDraft> themes = new ArrayList<>();
                final IStorage<ThemedChaosDraft> themeStorage = FModel.getThemedChaosDrafts();
                for (final ThemedChaosDraft theme : themeStorage) {
                    themes.add(theme);
                }
                Collections.sort(themes); // sort for user interface
                // Ask user to select theme
                final String dialogQuestion = Localizer.getInstance().getMessage("lblChooseChaosTheme");
                final ThemedChaosDraft theme = SGuiChoose.oneOrNone(dialogQuestion, themes);
                if (theme == null) {
                    return false; // abort if no theme is selected
                }
                // Filter all sets by theme restrictions
                final Predicate<CardEdition> themeFilter = theme.getEditionFilter();
                final CardEdition.Collection allEditions = StaticData.instance().getEditions();
                final Iterable<CardEdition> chaosDraftEditions = Iterables.filter(
                        allEditions.getOrderedEditions(),
                        themeFilter);
                // Add chaos "boosters" as special suppliers
                final Supplier<List<PaperCard>> ChaosDraftSupplier;
                try {
                    ChaosDraftSupplier = new ChaosBoosterSupplier(chaosDraftEditions);
                } catch(IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                    return false;
                }
                for (int i = 0; i < 3; i++) {
                    this.product.add(ChaosDraftSupplier);
                }
                break;

            default:
                throw new NoSuchElementException("Draft for mode " + this.draftFormat + " has not been set up!");
        }

        return true;
    }

    public static BoosterDraft createDraft(final LimitedPoolType draftType, final CardBlock block, final String[] boosters) {
        final BoosterDraft draft = new BoosterDraft(draftType);

        for (String booster : boosters) {
            try {
                draft.product.add(block.getBooster(booster));
            } catch (Exception ex) {
                System.err.println("Booster Draft Error: "+ex.getMessage());
            }
        }

        IBoosterDraft.LAND_SET_CODE[0] = block.getLandSet();
        IBoosterDraft.CUSTOM_RANKINGS_FILE[0] = null;

        draft.initializeBoosters();
        return draft;
    }

    protected BoosterDraft() {
        this(LimitedPoolType.Full);
    }

    protected BoosterDraft(final LimitedPoolType draftType) {
        this(draftType, N_PLAYERS);
    }

    protected BoosterDraft(final LimitedPoolType draftType, int numPlayers) {
        this.draftFormat = draftType;

        localPlayer = new LimitedPlayer(0, this);
        players.add(localPlayer);
        for (int i = 1; i < numPlayers; i++) {
            players.add(new LimitedPlayerAI(i, this));
        }
    }

    public DraftPack addBooster(CardEdition edition) {
        final IUnOpenedProduct product = new UnOpenedProduct(FModel.getMagicDb().getBoosters().get(edition.getCode()));
        return new DraftPack(product.get(), nextId++);
    }

    @Override
    public boolean isPileDraft() {
        return false;
    }

    @Override
    public void setLogEntry(IDraftLog draftingProcess) {
        draftLog = draftingProcess;
    }

    @Override
    public IDraftLog getDraftLog() {
        return draftLog;
    }

    @Override
    public int getRound() {
        return nextBoosterGroup;
    }

    @Override
    public LimitedPlayer getNeighbor(LimitedPlayer player, boolean left) {
        return players.get((player.order + (left ? 1 : -1) + N_PLAYERS) % N_PLAYERS);
    }

    private void setupCustomDraft(final CustomLimited draft) {
        final ItemPool<PaperCard> dPool = draft.getCardPool();
        if (dPool == null) {
            throw new RuntimeException("BoosterGenerator : deck not found");
        }

        final SealedTemplate tpl = draft.getSealedProductTemplate();

        final UnOpenedProduct toAdd = new UnOpenedProduct(tpl, dPool);
        toAdd.setLimitedPool(draft.isSingleton());
        for (int i = 0; i < draft.getNumPacks(); i++) {
            this.product.add(toAdd);
        }

        IBoosterDraft.LAND_SET_CODE[0] = FModel.getMagicDb().getEditions().get(draft.getLandSetCode());
        IBoosterDraft.CUSTOM_RANKINGS_FILE[0] = draft.getCustomRankingsFileName();
    }

    /**
     * Looks for draft files, reads them, returns a list.
     */
    private static List<CustomLimited> loadCustomDrafts() {
        if (customs.isEmpty()) {
            String[] dList;

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
        }
        return customs;
    }

    @Override
    public CardPool nextChoice() {
        // Primary draft loop - Computer Chooses from their packs, you choose form your packs
        if (this.isRoundOver()) {
            // If this round is over, try to start the next round
            if (!startRound()) {
                return null;
            }
        }

        this.computerChoose();

        final CardPool result = new CardPool();

        List<PaperCard> nextChoice = localPlayer.nextChoice();
        if (nextChoice != null && !nextChoice.isEmpty())
            result.addAllFlat(nextChoice);

        if (result.isEmpty()) {
            // Can't set a card, since none are available. Just pass "empty" packs.
            this.passPacks();
            // Recur until we find a cardpool or finish
            return nextChoice();
        }

        return result;
    }

    public void initializeBoosters() {

        for (Supplier<List<PaperCard>> boosterRound : this.product) {
            for (int i = 0; i < N_PLAYERS; i++) {
                DraftPack pack = new DraftPack(boosterRound.get(), nextId++);
                this.players.get(i).receiveUnopenedPack(pack);
            }
        }
        startRound();
    }

    public boolean startRound() {
        this.nextBoosterGroup++;
        this.currentBoosterPick = 0;
        packsInDraft = this.players.size();
        LimitedPlayer firstPlayer = this.players.get(0);
        if (firstPlayer.unopenedPacks.isEmpty()) {
            return false;
        }

        for (LimitedPlayer pl : this.players) {
            pl.newPack();
        }
        if (this.getDraftLog() != null) {
            this.getDraftLog().addLogEntry("Round " + this.nextBoosterGroup + " is starting...");
        }
        this.currentBoosterSize = firstPlayer.packQueue.peek().size();
        return true;
    }

    @Override
    public Deck[] getDecks() {
        Deck[] decks = new Deck[7];
        for (int i = 1; i < N_PLAYERS; i++) {
            decks[i - 1] = ((LimitedPlayerAI) this.players.get(i)).buildDeck(IBoosterDraft.LAND_SET_CODE[0] != null ? IBoosterDraft.LAND_SET_CODE[0].getCode() : null);
        }
        return decks;
    }

    @Override
    public LimitedPlayer[] getOpposingPlayers() {
        return this.players.toArray(new LimitedPlayer[7]);
    }

    @Override
    public LimitedPlayer getHumanPlayer() {
        return this.localPlayer;
    }

    @Override
    public LimitedPlayer getPlayer(int i) {
        if (i == 0) {
            return this.localPlayer;
        }

        return this.players.get(i - 1);
    }

    public void passPacks() {
        // Alternate direction of pack passing
        int adjust = this.nextBoosterGroup % 2 == 1 ? 1 : -1;
        if ("FirstPick".equals(this.doublePickDuringDraft) && currentBoosterPick == 1) {
            adjust = 0;
        } else if (currentBoosterPick % 2 == 1 && "Always".equals(this.doublePickDuringDraft)) {
            // This may not work with Conspiracy cards that mess with the draft
            // But it probably doesn't matter since Conspiracy doesn't have double pick?
            adjust = 0;
        }

        // Do any players have a Canal Dredger?
        List<LimitedPlayer> dredgers = new ArrayList<>();
        for (LimitedPlayer pl : this.players) {
            if (pl.hasCanalDredger()) {
                dredgers.add(pl);
            }
        }

        for (int i = 0; i < N_PLAYERS; i++) {
            LimitedPlayer pl = this.players.get(i);
            DraftPack passingPack = pl.passPack();

            if (passingPack == null)
                continue;

            LimitedPlayer passToPlayer = null;
            if (passingPack.isEmpty()) {
                packsInDraft--;
                continue;
            }

            if (passingPack.size() == 1) {
                if (dredgers.size() == 1) {
                    passToPlayer = dredgers.get(0);
                } else if (dredgers.size() > 1) {
                    // Multiple dredgers, so we need to choose one to pass to
                    if (dredgers.contains(pl)) {
                        // If the current player has a Canal Dredger, they should pass to themselves
                        passToPlayer = pl;
                    } else if (pl instanceof LimitedPlayerAI) {
                        // Maybe the AI could have more knowledge about the other players.
                        // Like don't pass to players that have revealed certain cards or colors
                        // But random is probably fine for now
                        Collections.shuffle(dredgers);
                        passToPlayer = dredgers.get(0);
                    } else {
                        // Human player, so we need to ask them
                        passToPlayer = SGuiChoose.one("Which player with Canal Dredger should we pass the last card to?", dredgers);
                    }
                }
            }

            if (passToPlayer == null) {
                passToPlayer = this.players.get((i + adjust + N_PLAYERS) % N_PLAYERS);
            }

            passToPlayer.receiveOpenedPack(passingPack);
        }
    }

    protected void computerChoose() {
        // Loop through players 1-7 to draft their current pack
        for (int i = 1; i < N_PLAYERS; i++) {
            LimitedPlayer pl = this.players.get(i);
            if (pl.shouldSkipThisPick()) {
                continue;
            }

            // Computer player has an empty pack or is passing the pack
            Boolean passPack;
            do {
                // THe player holding onto the pack to draft an extra card... Do it now.
                passPack = pl.draftCard(pl.chooseCard());
            } while (passPack != null && !passPack);
        }
    }

    public int getCurrentBoosterIndex() {
        return localPlayer.currentPack;
    }

    @Override
    public boolean isRoundOver() {
        // Really should check if all packs are empty, but this is a good enough approximation
        return packsInDraft == 0;
    }

    @Override
    public boolean hasNextChoice() {
        return !this.isRoundOver() || !this.localPlayer.unopenedPacks.isEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public boolean setChoice(final PaperCard c) {
        final DraftPack thisBooster = this.localPlayer.nextChoice();

        if (!thisBooster.contains(c)) {
            throw new RuntimeException("BoosterDraft : setChoice() error - card not found - " + c
                    + " - booster pack = " + thisBooster);
        }

        recordDraftPick(thisBooster, c);

        boolean passPack = this.localPlayer.draftCard(c);
        if (passPack) {
            // Leovolds Operative and Cogwork Librarian get to draft an extra card.. How do we do that?
            this.passPacks();
        }
        this.currentBoosterPick++;

        // Return whether or not we passed, but that the UI always needs to refresh
        // But returning might be useful for testing or other things?
        return passPack;
    }

    public void postDraftActions() {
        List<LimitedPlayer> brokers = new ArrayList<>();
        for (LimitedPlayer pl : this.players) {
            if (pl.hasBrokers()) {
                brokers.add(pl);
            }
        }

        Collections.shuffle(brokers);
        for(LimitedPlayer pl : brokers) {
            pl.activateBrokers(this.players);
        }

    }


    private static String choosePackByPack(final List<String> setz, int packs) {
        StringBuilder sb = new StringBuilder();

        for (int i = 1; i <= packs; i++) {
            String choice = SGuiChoose.oneOrNone(Localizer.getInstance().getMessage("lblChooseSetForNPack", String.valueOf(i), String.valueOf(packs)), setz);
            if (choice == null) {
                return null;
            }
            sb.append(choice);

            if (i != packs) {
                sb.append("/");
            }
        }
        return sb.toString();
    }

    private static List<String> getSetCombos(final List<String> setz) {
        final String[] sets = setz.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
        final List<String> setCombos = new ArrayList<>();
        if (sets.length >= 2) {
            setCombos.add(TextUtil.concatNoSpace(sets[0], "/", sets[0], "/", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[0], "/", sets[0], "/", sets[1]));
            setCombos.add(TextUtil.concatNoSpace(sets[0], "/", sets[1], "/", sets[1]));
            if (sets.length >= 3) {
                setCombos.add(TextUtil.concatNoSpace(sets[0], "/", sets[1], "/", sets[2]));
                setCombos.add(TextUtil.concatNoSpace(sets[0], "/", sets[2], "/", sets[2]));
            }
            setCombos.add(TextUtil.concatNoSpace(sets[1], "/", sets[0], "/", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[1], "/", sets[1], "/", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[1], "/", sets[1], "/", sets[1]));
            if (sets.length >= 3) {
                setCombos.add(TextUtil.concatNoSpace(sets[1], "/", sets[1], "/", sets[2]));
                setCombos.add(TextUtil.concatNoSpace(sets[1], "/", sets[2], "/", sets[2]));
            }
        }
        if (sets.length >= 3) {
            setCombos.add(TextUtil.concatNoSpace(sets[2], "/", sets[1], "/", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[2], "/", sets[2], "/", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[2], "/", sets[2], "/", sets[1]));
            setCombos.add(TextUtil.concatNoSpace(sets[2], "/", sets[2], "/", sets[2]));
        } // Beyond 3, skimp on the choice configurations, or the list will be enormous!
        if (sets.length >= 4) {
            setCombos.add(TextUtil.concatNoSpace(sets[3], "/", sets[1], "/", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[3], "/", sets[2], "/", sets[1]));
        }
        if (sets.length >= 5) {
            setCombos.add(TextUtil.concatNoSpace(sets[4], "/", sets[1], "/", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[4], "/", sets[3], "/", sets[2]));
            setCombos.add(TextUtil.concatNoSpace(sets[4], "/", sets[2], "/", sets[0]));
        }
        if (sets.length >= 6) {
            setCombos.add(TextUtil.concatNoSpace(sets[5], "/", sets[1], "/", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[5], "/", sets[3], "/", sets[2]));
            setCombos.add(TextUtil.concatNoSpace(sets[5], "/", sets[4], "/", sets[3]));
            setCombos.add(TextUtil.concatNoSpace(sets[5], "/", sets[2], "/", sets[0]));
        }
        if (sets.length >= 7) {
            setCombos.add(TextUtil.concatNoSpace(sets[6], "/", sets[1], "/", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[6], "/", sets[3], "/", sets[2]));
            setCombos.add(TextUtil.concatNoSpace(sets[6], "/", sets[5], "/", sets[4]));
            setCombos.add(TextUtil.concatNoSpace(sets[6], "/", sets[3], "/", sets[0]));
        }
        if (sets.length >= 8) {
            setCombos.add(TextUtil.concatNoSpace(sets[7], "/", sets[1], "/", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[7], "/", sets[3], "/", sets[2]));
            setCombos.add(TextUtil.concatNoSpace(sets[7], "/", sets[5], "/", sets[4]));
            setCombos.add(TextUtil.concatNoSpace(sets[7], "/", sets[6], "/", sets[5]));
            setCombos.add(TextUtil.concatNoSpace(sets[7], "/", sets[3], "/", sets[0]));
        }
        if (sets.length >= 9) {
            setCombos.add(TextUtil.concatNoSpace(sets[8], "/", sets[1], "/", sets[0]));
            setCombos.add(TextUtil.concatNoSpace(sets[8], "/", sets[3], "/", sets[2]));
            setCombos.add(TextUtil.concatNoSpace(sets[8], "/", sets[5], "/", sets[4]));
            setCombos.add(TextUtil.concatNoSpace(sets[8], "/", sets[7], "/", sets[6]));
            setCombos.add(TextUtil.concatNoSpace(sets[8], "/", sets[4], "/", sets[0]));
        }
        return setCombos;
    }

    private void recordDraftPick(final List<PaperCard> thisBooster, PaperCard c) {
        if (!ForgePreferences.UPLOAD_DRAFT) {
            return;
        }

        for (int i = 0; i < thisBooster.size(); i++) {
            final PaperCard cc = thisBooster.get(i);
            final String cnBk = cc.getName() + "|" + cc.getEdition();

            float pickValue;
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
}
