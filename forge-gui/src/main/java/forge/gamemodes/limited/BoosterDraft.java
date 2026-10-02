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

import forge.StaticData;
import forge.card.CardEdition;
import forge.card.DraftOptions;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckBase;
import forge.deck.DeckSection;
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
import forge.util.*;
import forge.util.storage.IStorage;
import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Booster Draft Format.
 */
public class BoosterDraft implements IBoosterDraft {

    private int nextId = 0;
    public static final int N_PLAYERS = 8;
    public static final String FILE_EXT = ".draft";

    int podSize;
    private final List<LimitedPlayer> players = new ArrayList<>();
    private LimitedPlayer localPlayer;
    private boolean readyForComputerPick = false;
    // Skips are used up after passPacks routes the skipped pack, so a skipping seat is never a Dredger target
    private final Map<LimitedPlayer, DraftPack> pendingSkips = new LinkedHashMap<>();

    private IDraftLog draftLog = null;

    private boolean shouldShowDraftLog = false;
    private boolean forNetwork = false;
    private String productName;

    private DraftOptions.DoublePick doublePickDuringDraft;
    protected int nextBoosterGroup = 0;
    private int currentBoosterSize = 0;
    private int currentBoosterPick = 0;

    private final Map<String, Float> draftPicks = new TreeMap<>();
    static final List<CustomLimited> customs = new ArrayList<>();
    protected LimitedPoolType draftFormat;

    protected final List<IUnOpenedProduct> product = new ArrayList<>();
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

    /**
     * Create a draft for network play. Product is generated but boosters are NOT
     * initialized — the caller must configure pod size and human seats, then call
     * {@link #initializeBoosters()} manually.
     *
     * @param draftType the draft pool type
     * @return a partially-initialized draft, or null if product generation fails
     */
    public static BoosterDraft createDraftForNetwork(final LimitedPoolType draftType) {
        final BoosterDraft draft = new BoosterDraft(draftType);
        draft.forNetwork = true;
        if (!draft.generateProduct()) {
            return null;
        }
        return draft;
    }

    protected boolean generateProduct() {
        switch (this.draftFormat) {
            case Full: // Draft from all cards in Forge
                final IUnOpenedProduct s = new UnOpenedProduct(SealedTemplate.genericDraftBooster);

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
                this.productName = block.getName();

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

                this.shouldShowDraftLog = block.getName().contains("Conspiracy");

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

                    this.productName = block.getName() + " (" + p + ")";
                    final String[] pp = p.toString().split("/");
                    for (int i = 0; i < nPacks; i++) {
                        this.product.add(block.getBooster(pp[i]));
                    }
                } else {
                    // Only one set is chosen. If that set lets you draft 2 cards to start adjust draft settings now
                    String setCode = sets.get(0);
                    this.productName = block.getName() + " (" + setCode + ")";
                    CardEdition edition = FModel.getMagicDb().getEditions().get(setCode);
                    // If this is metaset, edtion will be null
                    if (edition != null) {
                        if (podSize != edition.getDraftOptions().getRecommendedPodSize()) {
                            // Auto choosing recommended pod size. In the future we may want to allow user to choose
                            setPodSize(edition.getDraftOptions().getRecommendedPodSize());
                        }
                        doublePickDuringDraft = edition.getDraftOptions().getDoublePick();
                    }

                    final IUnOpenedProduct product1 = block.getBooster(setCode);
                    // lets associate the booster here so we can reference it later
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

                    this.productName = customDraft.getName();
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
                this.productName = theme.getLabel();
                // Filter all sets by theme restrictions
                final Predicate<CardEdition> themeFilter = theme.getEditionFilter();
                final CardEdition.Collection allEditions = StaticData.instance().getEditions();
                final Iterable<CardEdition> chaosDraftEditions = IterableUtil.filter(
                        allEditions.getOrderedEditions(),
                        themeFilter);
                // Add chaos "boosters" as special suppliers
                final IUnOpenedProduct ChaosDraftSupplier;
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

            case Import:
                /*
                 * Import a cube from CubeCobra.
                 * Default settings are 3 boosters with a size of 15 cards.
                 */

                String lastCubeId = FModel.getPreferences().getPref(ForgePreferences.FPref.LAST_IMPORTED_CUBE_ID);
                String inputCubeId = SOptionPane.showInputDialog(
                        Localizer.getInstance().getMessage("lblEnterCubeCobraURL") + ":",
                        Localizer.getInstance().getMessage("lblImportCube"),
                        null,
                        lastCubeId);

                if (inputCubeId == null) {
                    return false;
                }

                try {
                    CubeImporter importer = new CubeImporter(inputCubeId);
                    CustomLimited importedDraft = importer.importCube();
                    if (importedDraft == null) {
                        SOptionPane.showErrorDialog(Localizer.getInstance().getMessage("lblFailedToImportCube") + ": " + inputCubeId);
                        return false;
                    }
                    this.productName = importedDraft.getName();
                    this.setupCustomDraft(importedDraft);
                } catch (Exception e) {
                    SOptionPane.showErrorDialog(Localizer.getInstance().getMessage("lblErrorImportingCube") + ": " + e.getMessage());
                    return false;
                }
                break;

            default:
                throw new NoSuchElementException("Draft for mode " + this.draftFormat + " has not been set up!");
        }

        return true;
    }

    public static BoosterDraft createDraft(final LimitedPoolType draftType, final CardBlock block, final String[] boosters) {
        return createDraft(draftType, block, boosters, null);
    }

    public static BoosterDraft createDraft(final LimitedPoolType draftType, final CardBlock block, final String[] boosters, Integer numPlayers) {
        final BoosterDraft draft = new BoosterDraft(draftType);

        String setCode = boosters[0];
        CardEdition edition = FModel.getMagicDb().getEditions().get(setCode);
        // If this is metaset, edtion will be null
        if (edition != null) {
            // Auto choosing recommended pod size. If we've chosen the podsize it should be passed in via numPlayers
            int newPodSize = Objects.requireNonNullElseGet(numPlayers, () -> edition.getDraftOptions().getRecommendedPodSize());
            if (newPodSize != draft.getPodSize()) {
                draft.setPodSize(edition.getDraftOptions().getRecommendedPodSize());
            }
            draft.doublePickDuringDraft = edition.getDraftOptions().getDoublePick();
        }

        for (String booster : boosters) {
            try {
                draft.product.add(block.getBooster(booster));
            } catch (Exception ex) {
                System.err.println("Booster Draft Error: "+ex.getMessage());
            }
        }

        draft.shouldShowDraftLog = block.getName().contains("Conspiracy"); //TODO: Make this an actual property on the block and/or edition data.

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
        this.podSize = numPlayers;

        localPlayer = new LimitedPlayer(0, this);
        players.add(localPlayer);
        for (int i = 1; i < this.podSize; i++) {
            players.add(new LimitedPlayerAI(i, this));
        }
    }

    public DraftPack addBooster(CardEdition edition) {
        final IUnOpenedProduct product = new UnOpenedProduct(FModel.getMagicDb().getBoosters().get(edition.getCode()));
        return new DraftPack(product.get(), nextId++);
    }

    public void setPodSize(int size) {
        if (size < 2 || size > N_PLAYERS) {
            throw new IllegalArgumentException("BoosterDraft : invalid pod size " + size);
        }
        this.podSize = size;

        // Resize players list if it was already generated
        while (this.players.size() < this.podSize) {
            this.players.add(new LimitedPlayerAI(this.players.size(), this));
        }
        while (this.players.size() > this.podSize) {
            this.players.remove(this.players.size() - 1);
        }
    }

    /**
     * Authoritatively set which seats are human-controlled. Seats in {@code humanSeats}
     * become {@link LimitedPlayer} instances; all other seats become {@link LimitedPlayerAI}.
     * The constructor seeds seat 0 as a local human by default for single-player use —
     * network drafts must call this to reassign according to the shuffled seat layout,
     * which may place the host at a different seat.
     *
     * <p>Must be called before {@link #initializeBoosters()} so pack state is allocated
     * against the final seat configuration. Only network drafts call this; single-player
     * code leaves seat 0 as the default human.
     *
     * @param humanSeats seat indices (0-based) that should be human-controlled
     */
    public void setHumanSeats(Set<Integer> humanSeats) {
        for (int seat = 0; seat < players.size(); seat++) {
            boolean shouldBeHuman = humanSeats.contains(seat);
            LimitedPlayer current = players.get(seat);
            if (shouldBeHuman && current instanceof LimitedPlayerAI) {
                players.set(seat, new LimitedPlayer(seat, this));
            } else if (!shouldBeHuman && !(current instanceof LimitedPlayerAI)) {
                players.set(seat, new LimitedPlayerAI(seat, this));
            }
        }
        // Keep localPlayer consistent with whatever occupies seat 0 now.
        this.localPlayer = players.get(0);
    }

    /** Returns the total number of booster rounds in this draft. */
    public int getNumRounds() {
        return product.size();
    }

    /** Human-readable name of the chosen block / theme / cube (null for Full). */
    public String getProductName() {
        return productName;
    }

    public int getPodSize() {
        return this.podSize;
    }

    public void setDoublePick(DraftOptions.DoublePick doublePick) {
        this.doublePickDuringDraft = doublePick;
    }

    /** The pick rule as the set declared it, before resolving against a pod size. */
    public DraftOptions.DoublePick getDoublePick() {
        return this.doublePickDuringDraft;
    }

    /**
     * True when the pick rule gives this player another card from the pack they just
     * picked from. Call after {@link LimitedPlayer#draftCard} has counted the pick.
     * Keyed on the player's own count so the network host, where seats do not pick in
     * lockstep, gets the same answer as offline play.
     */
    public boolean keepsPackAfterPick(LimitedPlayer player) {
        DraftOptions.DoublePick rule = this.doublePickDuringDraft == null
                ? DraftOptions.DoublePick.NEVER
                : this.doublePickDuringDraft.resolve(this.podSize);
        return switch (rule) {
            case FIRST_PICK -> player.draftedThisRound == 1;
            case ALWAYS -> player.draftedThisRound % 2 == 1;
            default -> false;
        };
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
    public boolean shouldShowDraftLog() {
        return this.shouldShowDraftLog; //Hacky implementation for now.
    }

    @Override
    public int getRound() {
        return nextBoosterGroup;
    }

    @Override
    public LimitedPlayer getNeighbor(LimitedPlayer player, boolean left) {
        return players.get((player.order + (left ? 1 : -1) + this.podSize) % this.podSize);
    }

    private void setupCustomDraft(final CustomLimited draft) {
        final ItemPool<PaperCard> dPool = draft.getCardPool();
        if (dPool == null) {
            throw new RuntimeException("BoosterGenerator : deck not found");
        }

        final SealedTemplate tpl = draft.getSealedProductTemplate();

        int players = draft.getNumPlayers();
        final int minPlayers = 2;
        final int maxPlayers = 8;

        // Check if numPlayers is set in draft file, if not default to 8.
        if (players == 0) {
            players = N_PLAYERS;
        } else {
            players = Math.max(players, minPlayers);
            players = Math.min(players, maxPlayers);
        }

        setPodSize(players);

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
            ConcurrentLinkedQueue<CustomLimited> queue = new ConcurrentLinkedQueue<>();

            // get list of custom draft files
            final File dFolder = new File(ForgeConstants.DRAFT_DIR);
            if (!dFolder.exists()) {
                throw new RuntimeException("BoosterDraft : folder not found -- folder is " + dFolder.getAbsolutePath());
            }

            if (!dFolder.isDirectory()) {
                throw new RuntimeException("BoosterDraft : not a folder -- " + dFolder.getAbsolutePath());
            }

            dList = dFolder.list();

            if (dList != null) {
                List<CompletableFuture<?>> futures = new ArrayList<>();
                for (final String element : dList) {
                    if (element.endsWith(FILE_EXT)) {
                        futures.add(CompletableFuture.supplyAsync(()-> {
                            final List<String> dfData = FileUtil.readFile(ForgeConstants.DRAFT_DIR + element);
                            queue.add(CustomLimited.parse(dfData, FModel.getDecks().getCubes()));
                            return null;
                        }).exceptionally(ex -> {
                            ex.printStackTrace();
                            return null;
                        }));
                    }
                }
                CompletableFuture<?>[] futuresArray = futures.toArray(new CompletableFuture<?>[0]);
                CompletableFuture.allOf(futuresArray).join();
                futures.clear();
            }
            // stream().toList() causes crash on Android 8-13, use Collectors.toList()
            customs.addAll(queue.stream().collect(Collectors.toList()));
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

        if(readyForComputerPick)
            this.computerChoose();
        readyForComputerPick = false;

        final CardPool result = new CardPool();

        DraftPack nextChoice = localPlayer.nextChoice();
        if (nextChoice != null && !nextChoice.isEmpty())
            result.addAllFlat(nextChoice);

        if (result.isEmpty()) {
            // Can't set a card, since none are available. Just pass "empty" packs.
            this.passPacks();
            readyForComputerPick = true;
            // Recur until we find a cardpool or finish
            return nextChoice();
        }
        else {
            localPlayer.debugPrint(String.valueOf(nextChoice));
        }

        return result;
    }

    public void initializeBoosters() {

        for (Supplier<List<PaperCard>> boosterRound : this.product) {
            for (int i = 0; i < this.podSize; i++) {
                DraftPack pack = new DraftPack(boosterRound.get(), nextId++);
                this.players.get(i).receiveUnopenedPack(pack);
            }
        }
        startRound();
    }

    public boolean startRound() {
        this.nextBoosterGroup++;
        this.currentBoosterPick = 0;
        this.readyForComputerPick = true;
        LimitedPlayer firstPlayer = this.players.get(0);
        if (firstPlayer.unopenedPacks.isEmpty()) {
            return false;
        }

        for (LimitedPlayer pl : this.players) {
            pl.newPack();
        }
        if (this.getDraftLog() != null) {
            this.addLog("Round " + this.nextBoosterGroup + " is starting...", null);
        }
        this.currentBoosterSize = firstPlayer.packQueue.peek().size();
        return true;
    }

    @Override
    public Deck[] getComputerDecks() {
        Deck[] decks = new Deck[this.podSize - 1];
        for (int i = 1; i < this.podSize; i++) {
            decks[i - 1] = ((LimitedPlayerAI) this.players.get(i)).buildDeck(IBoosterDraft.LAND_SET_CODE[0] != null ? IBoosterDraft.LAND_SET_CODE[0].getCode() : null);
        }
        return decks;
    }

    @Override
    public LimitedPlayer[] getOpposingPlayers() {
        return this.players.subList(1, players.size()).toArray(new LimitedPlayer[this.podSize - 1]);
    }

    @Override
    public LimitedPlayer getHumanPlayer() {
        return this.localPlayer;
    }

    @Override
    public List<LimitedPlayer> getAllPlayers() {
        return Collections.unmodifiableList(this.players);
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

        Map<DraftPack, LimitedPlayer> toPass = new HashMap<>();
        for (int i = 0; i < this.podSize; i++) {
            LimitedPlayer pl = this.players.get(i);
            DraftPack passingPack = pl.passPack();

            if (passingPack == null)
                continue;

            if (passingPack.isEmpty()) {
                debugPrint("Pack #" + passingPack.getId() + " is empty. Discarding.");
                continue;
            }

            routeLastCard(pl, passingPack);
            LimitedPlayer passToPlayer = passingPack.getDestination();
            passingPack.setDestination(null);

            if (passToPlayer == null) {
                passToPlayer = keepsPackAfterPick(pl)
                        ? pl
                        : this.players.get((i + adjust + this.podSize) % this.podSize);
            }

            assert(!toPass.containsKey(passingPack));

            toPass.put(passingPack, passToPlayer);
        }
        toPass.forEach((pack, player) -> player.receiveOpenedPack(pack));
        pendingSkips.forEach(LimitedPlayer::consumeSkip);
        pendingSkips.clear();

        if(ForgePreferences.DEV_MODE) {
            int[] packCounts = players.stream().mapToInt((p) -> p.packQueue.size()).toArray();
            int[] unopenedPackCounts = players.stream().mapToInt((p) -> p.unopenedPacks.size()).toArray();
            debugPrint("Packs passed. Remaining Opened: " + Arrays.toString(packCounts) + "; Unopened: " + Arrays.toString(unopenedPackCounts));
        }
    }

    protected void computerChoose() {
        // Loop through players 1-7 to draft their current pack
        for (int i = 1; i < this.podSize; i++) {
            LimitedPlayer pl = this.players.get(i);
            if (pl.shouldSkipThisPick()) {
                pl.debugPrint("Skipped (shouldSkipThisPick)");
                DraftPack head = pl.nextChoice();
                if (head != null && !head.isEmpty()) {
                    pendingSkips.put(pl, head);
                }
                continue;
            }

            // Computer player has an empty pack or is passing the pack
            Boolean passPack;
            do {
                // THe player holding onto the pack to draft an extra card... Do it now.
                passPack = ((LimitedPlayerAI) pl).draftNext();
            } while (Boolean.FALSE.equals(passPack));
        }
    }

    public int getCurrentBoosterIndex() {
        return localPlayer.currentPack;
    }

    @Override
    public boolean isRoundOver() {
        return players.stream().allMatch((p) -> p.packQueue.isEmpty());
    }

    @Override
    public boolean hasNextChoice() {
        return !this.isRoundOver() || !this.localPlayer.unopenedPacks.isEmpty();
    }

    // Return false is the pack will be passed
    @Override
    public boolean setChoice(final PaperCard c, DeckSection section, DraftAction variant) {
        final DraftPack thisBooster = this.localPlayer.nextChoice();

        if (!localPlayer.isPackHidden() && !thisBooster.contains(c)) {
            System.out.println("BoosterDraft : setChoice() error - card not found - " + c
                    + " - booster pack = " + thisBooster);
            return false;
        }

        recordDraftPick(thisBooster, c);

        boolean passPack = Boolean.TRUE.equals(this.localPlayer.draftCard(c, section, variant));
        if (passPack) {
            // Computer players do their extra drafts in computerChoose.
            // Human players get handed the same pack twice by nextChoice.
            // A flag here disables the AI choosing again when nextChoice is called.
            // This could be done a lot better but that'd basically involve reorganizing all the draft logic.
            this.passPacks();
            readyForComputerPick = true;
        }
        this.currentBoosterPick++;

        // Return whether or not we passed, but that the UI always needs to refresh
        // But returning might be useful for testing or other things?
        return passPack;
    }

    @Override
    public void skipChoice() {
        this.localPlayer.debugPrint("Skipped pick.");
        pendingSkips.put(localPlayer, localPlayer.nextChoice());
        this.passPacks();
        readyForComputerPick = true;
    }

    public void postDraftActions() {
        // Uses are counted once, before any trade, so a traded Deal Broker adds nothing
        Map<LimitedPlayer, Integer> uses = new LinkedHashMap<>();
        for (LimitedPlayer pl : this.players) {
            int n = pl.brokerUses();
            if (n > 0) {
                uses.put(pl, n);
            }
        }
        List<LimitedPlayer> brokers = new ArrayList<>(uses.keySet());
        Collections.shuffle(brokers);
        runBrokers(brokers, uses, 0);
    }

    private void runBrokers(List<LimitedPlayer> brokers, Map<LimitedPlayer, Integer> uses, int index) {
        if (index >= brokers.size()) {
            return;
        }
        LimitedPlayer broker = brokers.get(index);
        broker.activateBrokers(this.players, uses.get(broker), () -> runBrokers(brokers, uses, index + 1));
    }

    /** Sets the pack's Canal Dredger recipient, asking the passer when several seats qualify. */
    public void routeLastCard(LimitedPlayer passer, DraftPack pack) {
        if (pack.getDestination() != null || pack.size() != 1) {
            return;
        }
        List<LimitedPlayer> eligible = players.stream()
                .filter(p -> p.hasFaceUp(LimitedPlayer.Effect.LAST_CARD) && !p.shouldSkipThisPick())
                .collect(Collectors.toList());
        if (eligible.size() == 1) {
            pack.setDestination(eligible.get(0));
        } else if (eligible.size() > 1) {
            passer.chooseDredgerSeat(eligible, pack, pack::setDestination);
        }
    }
    @Override
    public void addPrivateLog(LimitedPlayer seat, String message, PaperCard card) {
        if (draftLog == null) {
            return;
        }
        if (forNetwork) {
            draftLog.addPrivateLogEntry(seat.order, message, card);
        } else if (seat == localPlayer) {
            draftLog.addLogEntry(message, card);
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

    @Override
    public void addLog(String message, PaperCard card) {
        if (this.getDraftLog() != null) {
            this.getDraftLog().addLogEntry(message, card);
        }
        System.out.println("[DRAFT] " + message);
    }

    public void debugPrint(String text) {
        if(!ForgePreferences.DEV_MODE)
            return;
        System.out.println("[DRAFT] - " + text);
    }
}
