package forge.screens.home;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.ai.AIOption;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.deck.DeckSection;
import forge.deck.DeckType;
import forge.deck.DeckgenUtil;
import forge.deck.RandomDeckGenerator;
import forge.deckchooser.FDeckChooser;
import forge.game.GameType;
import forge.game.card.CardView;
import forge.gamemodes.match.GameLobby;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.event.UpdateLobbyPlayerEvent;
import forge.gui.CardDetailPanel;
import forge.gui.GuiBase;
import forge.gui.UiCommand;
import forge.gui.interfaces.ILobbyView;
import forge.gui.util.SOptionPane;
import forge.interfaces.IPlayerChangeListener;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.toolbox.FCheckBox;
import forge.toolbox.FLabel;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.toolbox.FPanel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FScrollPanel;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.FTextField;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.NameGenerator;
import net.miginfocom.swing.MigLayout;

/**
 * Lobby view. View of a number of players at the deck selection stage.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VLobby implements ILobbyView {

    static final int MAX_PLAYERS = 8;
    final Localizer localizer = Localizer.getInstance();
    private static final ForgePreferences prefs = FModel.getPreferences();

    // General variables
    private final GameLobby lobby;
    private IPlayerChangeListener playerChangeListener = null;
    private final LblHeader lblTitle = new LblHeader(localizer.getMessage("lblHeaderConstructedMode"));
    private int activePlayersNum = 0;
    private int playerWithFocus = 0; // index of the player that currently has focus

    private final StartButton btnStart  = new StartButton();
    private final JPanel pnlStart = new JPanel(new MigLayout("insets 0, gap 0, wrap 2"));
    private final JPanel constructedFrame = new JPanel(new MigLayout("insets 0, gap 0, wrap 2")); // Main content frame

    // Variants frame and variables
    private final FPanel variantsPanel = new FPanel(new MigLayout("insets 10, gapx 10"));
    private final VariantCheckBox vntVanguard = new VariantCheckBox(GameType.Vanguard);
    private final VariantCheckBox vntMomirBasic = new VariantCheckBox(GameType.MomirBasic);
    private final VariantCheckBox vntMoJhoSto = new VariantCheckBox(GameType.MoJhoSto);
    private final VariantCheckBox vntCommander = new VariantCheckBox(GameType.Commander);
    private final VariantCheckBox vntOathbreaker = new VariantCheckBox(GameType.Oathbreaker);
    private final VariantCheckBox vntTinyLeaders = new VariantCheckBox(GameType.TinyLeaders);
    private final VariantCheckBox vntBrawl = new VariantCheckBox(GameType.Brawl);
    private final VariantCheckBox vntPlanechase = new VariantCheckBox(GameType.Planechase);
    private final VariantCheckBox vntArchenemy = new VariantCheckBox(GameType.Archenemy);
    private final VariantCheckBox vntArchenemyRumble = new VariantCheckBox(GameType.ArchenemyRumble);
    private final ImmutableList<VariantCheckBox> vntBoxesLocal  =
            ImmutableList.of(vntVanguard, vntMomirBasic, vntMoJhoSto, vntCommander, vntOathbreaker, vntBrawl, vntTinyLeaders, vntPlanechase, vntArchenemy, vntArchenemyRumble);
    private final ImmutableList<VariantCheckBox> vntBoxesNetwork =
            ImmutableList.of(vntVanguard, vntMomirBasic, vntMoJhoSto, vntCommander, vntOathbreaker, vntBrawl, vntTinyLeaders /*, vntPlanechase, vntArchenemy, vntArchenemyRumble */);

    // Player frame elements
    private final JPanel playersFrame = new JPanel(new MigLayout("insets 0, gap 0 5, wrap, hidemode 3"));
    private final FScrollPanel playersScroll = new FScrollPanel(new MigLayout("insets 0, gap 0, wrap, hidemode 3"), true);
    private final List<PlayerPanel> playerPanels = new ArrayList<>(MAX_PLAYERS);

    private final FLabel addPlayerBtn = new FLabel.ButtonBuilder().fontSize(14).text(localizer.getMessage("lblAddAPlayer")).build();

    // Deck frame elements
    private final JPanel decksFrame = new JPanel(new MigLayout("insets 0, gap 0, wrap, hidemode 3"));
    private final List<FDeckChooser> deckChoosers = Lists.newArrayListWithCapacity(MAX_PLAYERS);
    private final FCheckBox cbSingletons = new FCheckBox(localizer.getMessage("cbSingletons"));
    private final FCheckBox cbArtifacts = new FCheckBox(localizer.getMessage("cbRemoveArtifacts"));
    private final Deck[] decks = new Deck[MAX_PLAYERS];

    // Variants
    private final List<FDeckChooser> commanderDeckChoosers = Lists.newArrayListWithCapacity(MAX_PLAYERS);
    private final List<FDeckChooser> oathbreakerDeckChoosers = Lists.newArrayListWithCapacity(MAX_PLAYERS);
    private final List<FDeckChooser> tinyLeadersDeckChoosers = Lists.newArrayListWithCapacity(MAX_PLAYERS);
    private final List<FDeckChooser> brawlDeckChoosers = Lists.newArrayListWithCapacity(MAX_PLAYERS);

    private final List<FList<Object>> schemeDeckLists = new ArrayList<>();
    private final List<FPanel> schemeDeckPanels = new ArrayList<>(MAX_PLAYERS);

    private final List<FList<Object>> planarDeckLists = new ArrayList<>();
    private final List<FPanel> planarDeckPanels = new ArrayList<>(MAX_PLAYERS);

    private final List<FList<Object>> vgdAvatarLists = new ArrayList<>();
    private final List<FPanel> vgdPanels = new ArrayList<>(MAX_PLAYERS);
    private final List<CardDetailPanel> vgdAvatarDetails = new ArrayList<>();
    private final List<PaperCard> vgdAllAvatars = new ArrayList<>();
    private final List<PaperCard> vgdAllAiAvatars = new ArrayList<>();
    private final List<PaperCard> nonRandomHumanAvatars = new ArrayList<>();
    private final List<PaperCard> nonRandomAiAvatars = new ArrayList<>();
    private final Vector<Object> humanListData = new Vector<>();
    private final Vector<Object> aiListData = new Vector<>();

    public boolean isForCommander() {
        return isForCommander;
    }

    public void setForCommander(boolean forCommander) {
        isForCommander = forCommander;
    }

    private boolean isForCommander = false;

    // CTR
    public VLobby(final GameLobby lobby) {
        this.lobby = lobby;

        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        ////////////////////////////////////////////////////////
        //////////////////// Variants Panel ////////////////////
        ImmutableList<VariantCheckBox> vntBoxes = null;
        if (lobby.isAllowNetworking()) {
            vntBoxes = vntBoxesNetwork;
        } else {
            vntBoxes = vntBoxesLocal;
        }

        variantsPanel.setOpaque(false);
        variantsPanel.add(newLabel(localizer.getMessage("lblVariants")));
        for (final VariantCheckBox vcb : vntBoxes) {
            variantsPanel.add(vcb);
        }

        constructedFrame.add(new FScrollPane(variantsPanel, false, true,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                "w 100%, h 45px!, gapbottom 10px, spanx 2, wrap");

        playersFrame.setOpaque(false);
        playersFrame.add(playersScroll, "w 100%, h 100%-35px");

        if (lobby.hasControl()) {
            addPlayerBtn.setFocusable(true);
            addPlayerBtn.setCommand(new Runnable() {
                @Override
                public final void run() {
                    lobby.addSlot();
                }
            });
            playersFrame.add(addPlayerBtn, "height 30px!, growx, pushx");
        }

        constructedFrame.add(playersFrame, "gapright 10px, w 50%-5px, growy, pushy");

        ////////////////////////////////////////////////////////
        ////////////////////// Deck Panel //////////////////////

        populateVanguardLists();
        for (int i = 0; i < MAX_PLAYERS; i++) {
            buildDeckPanels(i);
        }
        constructedFrame.add(decksFrame, "w 50%-5px, growy, pushy");
        constructedFrame.setOpaque(false);
        decksFrame.setOpaque(false);

        // Start Button
        if (lobby.hasControl()) {
            pnlStart.setOpaque(false);
            pnlStart.add(btnStart, "align center");
            // Start button event handling
            btnStart.addActionListener(new ActionListener() {
                @Override
                public final void actionPerformed(final ActionEvent arg0) {
                    Runnable startGame = lobby.startGame();
                    if (startGame != null) {
                        startGame.run();
                    }
                }
            });
        }
    }

    public void updateDeckPanel() {
        for (int iPlayer = 0; iPlayer < activePlayersNum; iPlayer++) {
            final FDeckChooser fdc = getDeckChooser(iPlayer);
            fdc.restoreSavedState();
            final FDeckChooser fdcom = getCommanderDeckChooser(iPlayer);
            fdcom.restoreSavedState();
            final FDeckChooser fdob = getOathbreakerDeckChooser(iPlayer);
            fdob.restoreSavedState();
            final FDeckChooser fdtl = getTinyLeaderDeckChooser(iPlayer);
            fdtl.restoreSavedState();
            final FDeckChooser fdbr = getBrawlDeckChooser(iPlayer);
            fdbr.restoreSavedState();
        }
    }

    public void focusOnAvatar() {
        getPlayerPanelWithFocus().focusOnAvatar();
    }

    @Override
    public void update(final int slot, final LobbySlotType type){
        final FDeckChooser deckChooser = getDeckChooser(slot);
        deckChooser.setIsAi(type==LobbySlotType.AI);
        DeckType selectedDeckType = deckChooser.getSelectedDeckType();
        switch (selectedDeckType){
            case STANDARD_CARDGEN_DECK:
            case PIONEER_CARDGEN_DECK:
            case HISTORIC_CARDGEN_DECK:
            case MODERN_CARDGEN_DECK:
            case LEGACY_CARDGEN_DECK:
            case VINTAGE_CARDGEN_DECK:
            case COLOR_DECK:
            case STANDARD_COLOR_DECK:
            case MODERN_COLOR_DECK:
                deckChooser.refreshDeckListForAI();
                break;
            default:
                break;
        }
        updateCommanderStyleDeckChooser(getCommanderDeckChooser(slot), type);
        updateCommanderStyleDeckChooser(getOathbreakerDeckChooser(slot), type);
        updateCommanderStyleDeckChooser(getTinyLeaderDeckChooser(slot), type);
        updateCommanderStyleDeckChooser(getBrawlDeckChooser(slot), type);
    }

    private void updateCommanderStyleDeckChooser(final FDeckChooser deckChooser, final LobbySlotType type) {
        deckChooser.setIsAi(type==LobbySlotType.AI);
        DeckType selectedDeckType = deckChooser.getSelectedDeckType();
        switch (selectedDeckType){
        case RANDOM_CARDGEN_COMMANDER_DECK:
        case RANDOM_COMMANDER_DECK:
            deckChooser.refreshDeckListForAI();
            break;
        default:
            break;
        }
    }

    @Override
    public void update(final boolean fullUpdate) {
        activePlayersNum = lobby.getNumberOfSlots();
        addPlayerBtn.setEnabled(activePlayersNum < MAX_PLAYERS);

        final boolean allowNetworking = lobby.isAllowNetworking();

        GuiBase.setNetworkplay(allowNetworking);

        ImmutableList<VariantCheckBox> vntBoxes = null;
        if (allowNetworking) {
            vntBoxes = vntBoxesNetwork;
        } else {
            vntBoxes = vntBoxesLocal;
        }
        for (final VariantCheckBox vcb : vntBoxes) {
            vcb.setSelected(hasVariant(vcb.variant));
            vcb.setEnabled(lobby.hasControl());
        }

        for (int i = 0; i < MAX_PLAYERS; i++) {
            final boolean hasPanel = i < playerPanels.size();
            if (i < activePlayersNum) {
                // visible panels
                final LobbySlot slot = lobby.getSlot(i);
                final FDeckChooser deckChooser = getDeckChooser(i);
                final FDeckChooser commanderDeckChooser = getCommanderDeckChooser(i);
                final FDeckChooser oathbreakerDeckChooser = getOathbreakerDeckChooser(i);
                final FDeckChooser tinyLeaderDeckChooser = getTinyLeaderDeckChooser(i);
                final FDeckChooser brawlDeckChooser = getBrawlDeckChooser(i);
                final PlayerPanel panel;
                final boolean isNewPanel;
                if (hasPanel) {
                    panel = playerPanels.get(i);
                    isNewPanel = !panel.isVisible();
                } else {
                    panel = new PlayerPanel(this, allowNetworking, i, slot, lobby.mayEdit(i), lobby.hasControl());
                    playerPanels.add(panel);
                    String constraints = "pushx, growx, wrap, hidemode 3";
                    if (i == 0) {
                        constraints += ", gaptop 5px";
                    }
                    playersScroll.add(panel, constraints);
                    deckChooser.restoreSavedState();
                    commanderDeckChooser.restoreSavedState();
                    oathbreakerDeckChooser.restoreSavedState();
                    tinyLeaderDeckChooser.restoreSavedState();
                    brawlDeckChooser.restoreSavedState();
                    if (i == 0) {
                        slot.setIsDevMode(prefs.getPrefBoolean(FPref.DEV_MODE_ENABLED));
                        changePlayerFocus(0);
                    }
                    isNewPanel = true;
                }

                final LobbySlotType type = slot.getType();
                panel.setType(type);
                panel.setPlayerName(slot.getName());
                panel.setAvatarIndex(slot.getAvatarIndex());
                panel.setTeam(slot.getTeam());
                panel.setIsReady(slot.isReady());
                panel.setIsDevMode(slot.isDevMode());
                panel.setIsArchenemy(slot.isArchenemy());
                panel.setUseAiSimulation(slot.getAiOptions().contains(AIOption.USE_SIMULATION));
                panel.setMayEdit(lobby.mayEdit(i));
                panel.setMayControl(lobby.mayControl(i));
                panel.setMayRemove(lobby.mayRemove(i));
                panel.update();

                final boolean isSlotAI = slot.getType() == LobbySlotType.AI;

                deckChooser.setIsAi(isSlotAI);
                commanderDeckChooser.setIsAi(isSlotAI);
                oathbreakerDeckChooser.setIsAi(isSlotAI);
                tinyLeaderDeckChooser.setIsAi(isSlotAI);
                brawlDeckChooser.setIsAi(isSlotAI);
                if (fullUpdate && (type == LobbySlotType.LOCAL || isSlotAI)) {
                    selectDeck(i);
                }
                if (isNewPanel) {
                    panel.setVisible(true);
                }
            } else if (hasPanel) {
                playerPanels.get(i).setVisible(false);
            }
        }

        if (playerWithFocus >= activePlayersNum) {
            changePlayerFocus(activePlayersNum - 1);
        } else {
            populateDeckPanel(getCurrentGameMode());
        }
        refreshPanels(true, true);
    }

    public void setPlayerChangeListener(final IPlayerChangeListener listener) {
        this.playerChangeListener = listener;
    }

    void setReady(final int index, final boolean ready) {
        if (ready && decks[index] == null && !vntMomirBasic.isSelected() && !vntMoJhoSto.isSelected()) {
            SOptionPane.showErrorDialog("Select a deck before readying!");
            update(false);
            return;
        }

        firePlayerChangeListener(index);
        changePlayerFocus(index);
    }
    void setDevMode(final int index) {
        // clear ready for everyone
        for (int i = 0; i < activePlayersNum; i++) {
            final PlayerPanel panel = playerPanels.get(i);
            panel.setIsReady(false);
            firePlayerChangeListener(i);
        }
        changePlayerFocus(index);
    }
    void firePlayerChangeListener(final int index) {
        if (playerChangeListener != null) {
            playerChangeListener.update(index, getSlot(index));
        }
    }
    private void fireDeckChangeListener(final int index, final Deck deck) {
        decks[index] = deck;
        if (playerChangeListener != null) {
            playerChangeListener.update(index, UpdateLobbyPlayerEvent.deckUpdate(deck));
        }
    }
    private void fireDeckSectionChangeListener(final int index, final DeckSection section, final CardPool cards) {
        final Deck deck = decks[index];
        final Deck copy = deck == null ? new Deck() : new Deck(decks[index]);
        copy.putSection(section, cards);
        decks[index] = copy;
        if (playerChangeListener != null) {
            playerChangeListener.update(index, UpdateLobbyPlayerEvent.deckUpdate(section, cards));
        }
    }

    void removePlayer(final int index) {
        lobby.removeSlot(index);
    }
    boolean hasVariant(final GameType variant) {
        return lobby.hasVariant(variant);
    }

    private UpdateLobbyPlayerEvent getSlot(final int index) {
        final PlayerPanel panel = playerPanels.get(index);
        return UpdateLobbyPlayerEvent.create(panel.getType(), panel.getPlayerName(), panel.getAvatarIndex(), -1/*TODO panel.getSleeveIndex()*/, panel.getTeam(), panel.isArchenemy(), panel.isReady(), panel.isDevMode(), panel.getAiOptions());
    }

    /** Builds the actual deck panel layouts for each player.
     * These are added to a list which can be referenced to populate the deck panel appropriately. */
    @SuppressWarnings("serial")
    private void buildDeckPanels(final int playerIndex) {
        // Main deck
        final FDeckChooser mainChooser = new FDeckChooser(null, isPlayerAI(playerIndex), GameType.Constructed, false);
        mainChooser.getLstDecks().setSelectCommand(new UiCommand() {
            @Override public final void run() {
                selectMainDeck(playerIndex);
            }
        });
        mainChooser.initialize();
        deckChoosers.add(mainChooser);

        // Scheme deck list
        buildDeckPanel(localizer.getMessage("lblSchemeDeck"), playerIndex, schemeDeckLists, schemeDeckPanels, new ListSelectionListener() {
            @Override public final void valueChanged(final ListSelectionEvent e) {
                selectSchemeDeck(playerIndex);
            }
        });

        final FDeckChooser commanderChooser = new FDeckChooser(null, isPlayerAI(playerIndex), GameType.Commander, true);
        commanderChooser.getLstDecks().setSelectCommand(new UiCommand() {
            @Override public final void run() {
                selectCommanderDeck(playerIndex);
            }
        });
        commanderChooser.initialize();
        commanderDeckChoosers.add(commanderChooser);

        final FDeckChooser oathbreakerChooser = new FDeckChooser(null, isPlayerAI(playerIndex), GameType.Oathbreaker, true);
        oathbreakerChooser.getLstDecks().setSelectCommand(new UiCommand() {
            @Override public final void run() {
                selectOathbreakerDeck(playerIndex);
            }
        });
        oathbreakerChooser.initialize();
        oathbreakerDeckChoosers.add(oathbreakerChooser);

        final FDeckChooser tinyLeaderChooser = new FDeckChooser(null, isPlayerAI(playerIndex), GameType.TinyLeaders, true);
        tinyLeaderChooser.getLstDecks().setSelectCommand(new UiCommand() {
            @Override public final void run() {
                selectTinyLeadersDeck(playerIndex);
            }
        });
        tinyLeaderChooser.initialize();
        tinyLeadersDeckChoosers.add(tinyLeaderChooser);

        final FDeckChooser brawlChooser = new FDeckChooser(null, isPlayerAI(playerIndex), GameType.Brawl, true);
        brawlChooser.getLstDecks().setSelectCommand(new UiCommand() {
            @Override public final void run() {
                selectBrawlDeck(playerIndex);
            }
        });
        brawlChooser.initialize();
        brawlDeckChoosers.add(brawlChooser);

        // Planar deck list
        buildDeckPanel(localizer.getMessage("lblPlanarDeck"), playerIndex, planarDeckLists, planarDeckPanels, new ListSelectionListener() {
            @Override public final void valueChanged(final ListSelectionEvent e) {
                selectPlanarDeck(playerIndex);
            }
        });

        // Vanguard avatar list
        buildDeckPanel(localizer.getMessage("lblVanguardAvatar"), playerIndex, vgdAvatarLists, vgdPanels, new ListSelectionListener() {
            @Override public final void valueChanged(final ListSelectionEvent e) {
                selectVanguardAvatar(playerIndex);
            }
        });
        Iterables.getLast(vgdAvatarLists).setListData(isPlayerAI(playerIndex) ? aiListData : humanListData);
        Iterables.getLast(vgdAvatarLists).setSelectedIndex(0);
        final CardDetailPanel vgdDetail = new CardDetailPanel();
        vgdAvatarDetails.add(vgdDetail);
        Iterables.getLast(vgdPanels).add(vgdDetail, "h 200px, pushx, growx, hidemode 3");
    }

    private void buildDeckPanel(final String formatName, final int playerIndex,
            final List<FList<Object>> deckLists, final List<FPanel> deckPanels,
            final ListSelectionListener selectionListener) {
        final FPanel deckPanel = new FPanel();
        deckPanel.setBorderToggle(false);
        deckPanel.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        deckPanel.add(new FLabel.Builder().text("Select " + formatName)
                .fontStyle(Font.BOLD).fontSize(14).fontAlign(SwingConstants.CENTER)
                .build(), "gaptop 10px, gapbottom 5px, growx, pushx");
        final FList<Object> deckList = new FList<>();
        deckList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deckList.addListSelectionListener(selectionListener);

        final FScrollPane scrollPane = new FScrollPane(deckList, true,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        deckPanel.add(scrollPane, "grow, push");

        deckLists.add(deckList);
        deckPanels.add(deckPanel);
    }

    private void selectDeck(final int playerIndex) {
        // Full deck selection
        selectMainDeck(playerIndex);
        selectCommanderDeck(playerIndex);
        selectOathbreakerDeck(playerIndex);
        selectTinyLeadersDeck(playerIndex);
        selectBrawlDeck(playerIndex);

        // Deck section selection
        selectSchemeDeck(playerIndex);
        selectPlanarDeck(playerIndex);
        selectVanguardAvatar(playerIndex);
    }

    private void selectMainDeck(final int playerIndex) {
        if (hasVariant(GameType.Commander) || hasVariant(GameType.Oathbreaker) || hasVariant(GameType.TinyLeaders) || hasVariant(GameType.Brawl)) {
            // These game types use specific deck panel
            return;
        }
        final FDeckChooser mainChooser = getDeckChooser(playerIndex);
        final DeckType type = mainChooser.getSelectedDeckType();
        final Deck deck = mainChooser.getDeck();
        // something went wrong, clear selection to prevent error loop
        if (deck == null) {
            mainChooser.getLstDecks().setSelectedIndex(0);
        }
        final Collection<DeckProxy> selectedDecks = mainChooser.getLstDecks().getSelectedItems();
        if (playerIndex < activePlayersNum && lobby.mayEdit(playerIndex)) {
            final String text = type.toString() + ": " + Lang.joinHomogenous(selectedDecks, DeckProxy.FN_GET_NAME);
            playerPanels.get(playerIndex).setDeckSelectorButtonText(text);
            fireDeckChangeListener(playerIndex, deck);
        }
        mainChooser.saveState();
    }

    private void selectCommanderDeck(final int playerIndex) {
        if (!hasVariant(GameType.Commander) && !hasVariant(GameType.Oathbreaker) && !hasVariant(GameType.TinyLeaders) && !hasVariant(GameType.Brawl)) {
            // Only these game types use this specific deck panel
            return;
        }
        final FDeckChooser mainChooser = getCommanderDeckChooser(playerIndex);
        final DeckType type = mainChooser.getSelectedDeckType();
        final Deck deck = mainChooser.getDeck();
        final Collection<DeckProxy> selectedDecks = mainChooser.getLstDecks().getSelectedItems();
        if (playerIndex < activePlayersNum && lobby.mayEdit(playerIndex)) {
            final String text = type.toString() + ": " + Lang.joinHomogenous(selectedDecks, DeckProxy.FN_GET_NAME);
            playerPanels.get(playerIndex).setCommanderDeckSelectorButtonText(text);
            fireDeckChangeListener(playerIndex, deck);
        }
        mainChooser.saveState();
    }

    private void selectOathbreakerDeck(final int playerIndex) {
        if (!hasVariant(GameType.Oathbreaker)) {
            // Only these game types use this specific deck panel
            return;
        }
        final FDeckChooser mainChooser = getOathbreakerDeckChooser(playerIndex);
        final DeckType type = mainChooser.getSelectedDeckType();
        final Deck deck = mainChooser.getDeck();
        final Collection<DeckProxy> selectedDecks = mainChooser.getLstDecks().getSelectedItems();
        if (playerIndex < activePlayersNum && lobby.mayEdit(playerIndex)) {
            final String text = type.toString() + ": " + Lang.joinHomogenous(selectedDecks, DeckProxy.FN_GET_NAME);
            playerPanels.get(playerIndex).setCommanderDeckSelectorButtonText(text);
            fireDeckChangeListener(playerIndex, deck);
        }
        mainChooser.saveState();
    }

    private void selectTinyLeadersDeck(final int playerIndex) {
        if (!hasVariant(GameType.TinyLeaders)) {
            // Only these game types use this specific deck panel
            return;
        }
        final FDeckChooser mainChooser = getTinyLeaderDeckChooser(playerIndex);
        final DeckType type = mainChooser.getSelectedDeckType();
        final Deck deck = mainChooser.getDeck();
        final Collection<DeckProxy> selectedDecks = mainChooser.getLstDecks().getSelectedItems();
        if (playerIndex < activePlayersNum && lobby.mayEdit(playerIndex)) {
            final String text = type.toString() + ": " + Lang.joinHomogenous(selectedDecks, DeckProxy.FN_GET_NAME);
            playerPanels.get(playerIndex).setCommanderDeckSelectorButtonText(text);
            fireDeckChangeListener(playerIndex, deck);
        }
        mainChooser.saveState();
    }

    private void selectBrawlDeck(final int playerIndex) {
        if (!hasVariant(GameType.Brawl)) {
            // Only these game types use this specific deck panel
            return;
        }
        final FDeckChooser mainChooser = getBrawlDeckChooser(playerIndex);
        final DeckType type = mainChooser.getSelectedDeckType();
        final Deck deck = mainChooser.getDeck();
        final Collection<DeckProxy> selectedDecks = mainChooser.getLstDecks().getSelectedItems();
        if (playerIndex < activePlayersNum && lobby.mayEdit(playerIndex)) {
            final String text = type.toString() + ": " + Lang.joinHomogenous(selectedDecks, DeckProxy.FN_GET_NAME);
            playerPanels.get(playerIndex).setCommanderDeckSelectorButtonText(text);
            fireDeckChangeListener(playerIndex, deck);
        }
        mainChooser.saveState();
    }

    private void selectSchemeDeck(final int playerIndex) {
        if (playerIndex >= activePlayersNum || !(hasVariant(GameType.Archenemy) || hasVariant(GameType.ArchenemyRumble))) {
            return;
        }

        final Object selected = getSchemeDeckLists().get(playerIndex).getSelectedValue();
        final Deck deck = decks[playerIndex];
        CardPool schemePool = null;
        if (selected instanceof String) {
            String sel = (String) selected;
            if (sel.contains("Use deck's scheme section")) {
                if (deck.has(DeckSection.Schemes)) {
                    schemePool = deck.get(DeckSection.Schemes);
                } else {
                    sel = "Random";
                }
            }
            if (sel.equals("Random")) {
                final Deck randomDeck = RandomDeckGenerator.getRandomUserDeck(lobby, playerPanels.get(playerIndex).isAi());
                schemePool = randomDeck.get(DeckSection.Schemes);
            }
        } else if (selected instanceof Deck) {
            schemePool = ((Deck) selected).get(DeckSection.Schemes);
        }
        if (schemePool == null) { //Can be null if player deselects the list selection or chose Generate
            schemePool = DeckgenUtil.generateSchemePool();
        }
        fireDeckSectionChangeListener(playerIndex, DeckSection.Schemes, schemePool);
        getDeckChooser(playerIndex).saveState();
    }

    private void selectPlanarDeck(final int playerIndex) {
        if (playerIndex >= activePlayersNum || !hasVariant(GameType.Planechase)) {
            return;
        }

        final Object selected = getPlanarDeckLists().get(playerIndex).getSelectedValue();
        final Deck deck = decks[playerIndex];
        CardPool planePool = null;
        if (selected instanceof String) {
            String sel = (String) selected;
            if (sel.contains("Use deck's planes section")) {
                if (deck.has(DeckSection.Planes)) {
                    planePool = deck.get(DeckSection.Planes);
                } else {
                    sel = "Random";
                }
            }
            if (sel.equals("Random")) {
                final Deck randomDeck = RandomDeckGenerator.getRandomUserDeck(lobby, playerPanels.get(playerIndex).isAi());
                planePool = randomDeck.get(DeckSection.Planes);
            }
        } else if (selected instanceof Deck) {
            planePool = ((Deck) selected).get(DeckSection.Planes);
        }
        if (planePool == null) { //Can be null if player deselects the list selection or chose Generate
            planePool = DeckgenUtil.generatePlanarPool();
        }
        fireDeckSectionChangeListener(playerIndex, DeckSection.Planes, planePool);
        getDeckChooser(playerIndex).saveState();
    }

    private void selectVanguardAvatar(final int playerIndex) {
        if (playerIndex >= activePlayersNum || !hasVariant(GameType.Vanguard)) {
            return;
        }

        final Object selected = vgdAvatarLists.get(playerIndex).getSelectedValue();
        final PlayerPanel pp = playerPanels.get(playerIndex);
        final CardDetailPanel cdp = vgdAvatarDetails.get(playerIndex);

        PaperCard vanguardAvatar = null;
        final Deck deck = decks[playerIndex];
        if (selected instanceof PaperCard) {
            pp.setVanguardButtonText(((PaperCard) selected).getName());
            cdp.setCard(CardView.getCardForUi((PaperCard) selected));
            cdp.setVisible(true);
            refreshPanels(false, true);

            vanguardAvatar = (PaperCard)selected;
        } else {
            final String sel = (String) selected;
            pp.setVanguardButtonText(sel);
            cdp.setVisible(false);

            if (sel == null) {
                return;
            }
            if (sel.contains("Use deck's default avatar") && deck != null && deck.has(DeckSection.Avatar)) {
                vanguardAvatar = deck.get(DeckSection.Avatar).get(0);
            } else { //Only other string is "Random"
                if (playerPanels.get(playerIndex).isAi()) { //AI
                    vanguardAvatar = Aggregates.random(getNonRandomAiAvatars());
                } else { //Human
                    vanguardAvatar = Aggregates.random(getNonRandomHumanAvatars());
                }
            }
        }

        final CardPool avatarOnce = new CardPool();
        avatarOnce.add(vanguardAvatar);
        fireDeckSectionChangeListener(playerIndex, DeckSection.Avatar, avatarOnce);
        getDeckChooser(playerIndex).saveState();
    }

    /** Populates the deck panel with the focused player's deck choices. */
    private void populateDeckPanel(final GameType forGameType) {
        decksFrame.removeAll();

        if (!lobby.mayEdit(playerWithFocus)) {
            return;
        }

        switch (forGameType) {
        case Constructed:
            decksFrame.add(deckChoosers.get(playerWithFocus), "grow, push");
            if (deckChoosers.get(playerWithFocus).getSelectedDeckType().toString().contains(localizer.getMessage("lblRandom"))) {
                final String strCheckboxConstraints = "h 30px!, gap 0 20px 0 0";
                decksFrame.add(cbSingletons, strCheckboxConstraints);
                decksFrame.add(cbArtifacts, strCheckboxConstraints);
            }
            break;
        case Archenemy:
        case ArchenemyRumble:
            if (isPlayerArchenemy(playerWithFocus)) {
                decksFrame.add(schemeDeckPanels.get(playerWithFocus), "grow, push");
            } else {
                populateDeckPanel(GameType.Constructed);
            }
            break;
        case Commander:
            decksFrame.add(commanderDeckChoosers.get(playerWithFocus), "grow, push");
            break;
        case Oathbreaker:
            decksFrame.add(oathbreakerDeckChoosers.get(playerWithFocus), "grow, push");
            break;
        case TinyLeaders:
            decksFrame.add(tinyLeadersDeckChoosers.get(playerWithFocus), "grow, push");
            break;
        case Brawl:
            decksFrame.add(brawlDeckChoosers.get(playerWithFocus), "grow, push");
            break;
        case Planechase:
            decksFrame.add(planarDeckPanels.get(playerWithFocus), "grow, push");
            break;
        case Vanguard:
            updateVanguardList(playerWithFocus);
            decksFrame.add(vgdPanels.get(playerWithFocus), "grow, push");
            break;
        default:
            break;
        }
        refreshPanels(false, true);
    }

    /** @return {@link javax.swing.JButton} */
    JButton getBtnStart() {
        return this.btnStart;
    }

    public LblHeader getLblTitle() { return lblTitle; }
    public JPanel getConstructedFrame() { return constructedFrame; }
    public JPanel getPanelStart() { return pnlStart; }
    public List<FDeckChooser> getDeckChoosers() { return Collections.unmodifiableList(deckChoosers); }

    /** Gets the random deck checkbox for Singletons. */
    FCheckBox getCbSingletons() { return cbSingletons; }

    /** Gets the random deck checkbox for Artifacts. */
    FCheckBox getCbArtifacts() { return cbArtifacts; }

    public final List<PlayerPanel> getPlayerPanels() {
        return playerPanels;
    }
    private PlayerPanel getPlayerPanelWithFocus() {
        return getPlayerPanels().get(playerWithFocus);
    }
    boolean hasFocus(final int iPlayer) {
        return iPlayer == playerWithFocus;
    }

    public final FDeckChooser getDeckChooser(final int playernum) {
        return deckChoosers.get(playernum);
    }

    public final FDeckChooser getCommanderDeckChooser(final int playernum) {
        return commanderDeckChoosers.get(playernum);
    }

    public final FDeckChooser getOathbreakerDeckChooser(final int playernum) {
        return oathbreakerDeckChoosers.get(playernum);
    }

    public final FDeckChooser getTinyLeaderDeckChooser(final int playernum) {
        return tinyLeadersDeckChoosers.get(playernum);
    }

    public final FDeckChooser getBrawlDeckChooser(final int playernum) {
        return brawlDeckChoosers.get(playernum);
    }

    GameType getCurrentGameMode() {
        return lobby.getGameType();
    }
    void setCurrentGameMode(final GameType mode) {
        lobby.setGameType(mode);
        update(true);
    }

    private boolean isPlayerAI(final int playernum) {
        if(playernum < activePlayersNum){
            return playerPanels.get(playernum).isAi();
        }
        return true;
    }

    public int getNumPlayers() {
        return activePlayersNum;
    }

    /** Revalidates the player and deck sections. Necessary after adding or hiding any panels. */
    private void refreshPanels(final boolean refreshPlayerFrame, final boolean refreshDeckFrame) {
        if (refreshPlayerFrame) {
            playersScroll.validate();
            playersScroll.repaint();
        }
        if (refreshDeckFrame) {
            decksFrame.validate();
            decksFrame.repaint();
        }
    }

    public void changePlayerFocus(final int newFocusOwner) {
        changePlayerFocus(newFocusOwner, lobby.getGameType());
    }

    void changePlayerFocus(final int newFocusOwner, final GameType gType) {
        final PlayerPanel oldFocus = getPlayerPanelWithFocus();
        if (oldFocus != null) {
            oldFocus.setFocused(false);
        }
        playerWithFocus = newFocusOwner;
        final PlayerPanel newFocus = getPlayerPanelWithFocus();
        newFocus.setFocused(true);

        playersScroll.getViewport().scrollRectToVisible(newFocus.getBounds());
        populateDeckPanel(gType);

        refreshPanels(true, true);
    }

    /** Saves avatar prefs for players one and two. */
    void updateAvatarPrefs() {
        final int pOneIndex = playerPanels.get(0).getAvatarIndex();
        final int pTwoIndex = playerPanels.get(1).getAvatarIndex();

        prefs.setPref(FPref.UI_AVATARS, pOneIndex + "," + pTwoIndex);
        prefs.save();
    }

    /** Saves sleeve prefs for players one and two. */
    void updateSleevePrefs() {
        final int pOneIndex = playerPanels.get(0).getSleeveIndex();
        final int pTwoIndex = playerPanels.get(1).getSleeveIndex();

        prefs.setPref(FPref.UI_SLEEVES, pOneIndex + "," + pTwoIndex);
        prefs.save();
    }

    /** Adds a pre-styled FLabel component with the specified title. */
    FLabel newLabel(final String title) {
        return new FLabel.Builder().text(title).fontSize(14).fontStyle(Font.ITALIC).build();
    }

    List<Integer> getUsedAvatars() {
        final List<Integer> usedAvatars = Lists.newArrayListWithCapacity(MAX_PLAYERS);
        for (final PlayerPanel pp : playerPanels) {
            usedAvatars.add(pp.getAvatarIndex());
        }
        return usedAvatars;
    }

    List<Integer> getUsedSleeves() {
        final List<Integer> usedSleeves = Lists.newArrayListWithCapacity(MAX_PLAYERS);
        for (final PlayerPanel pp : playerPanels) {
            usedSleeves.add(pp.getSleeveIndex());
        }
        return usedSleeves;
    }


    private static final ImmutableList<String> genderOptions = ImmutableList.of("Male",    "Female",  "Any"),
                                               typeOptions   = ImmutableList.of("Fantasy", "Generic", "Any");
    final String getNewName() {
        final String title = localizer.getMessage("lblGetNewRandomName");
        final String message = localizer.getMessage("lbltypeofName");
        final SkinImage icon = FOptionPane.QUESTION_ICON;

        final int genderIndex = FOptionPane.showOptionDialog(message, title, icon, genderOptions, 2);
        if (genderIndex < 0) {
            return null;
        }
        final int typeIndex = FOptionPane.showOptionDialog(message, title, icon, typeOptions, 2);
        if (typeIndex < 0) {
            return null;
        }

        final String gender = genderOptions.get(genderIndex);
        final String type = typeOptions.get(typeIndex);

        String confirmMsg, newName;
        final List<String> usedNames = getPlayerNames();
        do {
            newName = NameGenerator.getRandomName(gender, type, usedNames);
            confirmMsg = localizer.getMessage("lblconfirmName").replace("%s","\"" +newName + "\"");
        } while (!FOptionPane.showConfirmDialog(confirmMsg, title, localizer.getMessage("lblUseThisName"), localizer.getMessage("lblTryAgain"), true));

        return newName;
    }

    List<String> getPlayerNames() {
        final List<String> names = new ArrayList<>();
        for (final PlayerPanel pp : playerPanels) {
            names.add(pp.getPlayerName());
        }
        return names;
    }

    /////////////////////////////////////////////
    //========== Various listeners in build order

    @SuppressWarnings("serial") private class VariantCheckBox extends FCheckBox {
        private final GameType variant;
        private VariantCheckBox(final GameType variantType) {
            super(variantType.toString());
            this.variant = variantType;

            setToolTipText(variantType.getDescription());
            addItemListener(new ItemListener() {
                @Override public final void itemStateChanged(final ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        lobby.applyVariant(variantType);
                    } else {
                        lobby.removeVariant(variantType);
                    }
                }
            });
        }
    }

    final ActionListener nameListener = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
            final FTextField nField = (FTextField)e.getSource();
            nField.transferFocus();
        }
    };

    /////////////////////////////////////
    //========== METHODS FOR VARIANTS

    /** Gets the list of planar deck lists. */
    public List<FList<Object>> getPlanarDeckLists() {
        return planarDeckLists;
    }

    /** Gets the list of scheme deck lists. */
    public List<FList<Object>> getSchemeDeckLists() {
        return schemeDeckLists;
    }

    public boolean isPlayerArchenemy(final int playernum) {
        return playerPanels.get(playernum).isArchenemy();
    }

    /** Gets the list of Vanguard avatar lists. */
    public List<FList<Object>> getVanguardLists() {
        return vgdAvatarLists;
    }

    /** Return all the Vanguard avatars. */
    public Iterable<PaperCard> getAllAvatars() {
        if (vgdAllAvatars.isEmpty()) {
            for (final PaperCard c : FModel.getMagicDb().getVariantCards().getAllCards()) {
                if (c.getRules().getType().isVanguard()) {
                    vgdAllAvatars.add(c);
                }
            }
        }
        return vgdAllAvatars;
    }

    /** Return the Vanguard avatars not flagged RemoveDeck:All. */
    public List<PaperCard> getAllAiAvatars() {
        return vgdAllAiAvatars;
    }

    /** Return the Vanguard avatars not flagged RemoveDeck:Random. */
    public List<PaperCard> getNonRandomHumanAvatars() {
        return nonRandomHumanAvatars;
    }

    /** Return the Vanguard avatars not flagged RemoveDeck:All or RemoveDeck:Random. */
    public List<PaperCard> getNonRandomAiAvatars() {
        return nonRandomAiAvatars;
    }

    /** Populate vanguard lists. */
    private void populateVanguardLists() {
        humanListData.add("Use deck's default avatar (random if unavailable)");
        humanListData.add("Random");
        aiListData.add("Use deck's default avatar (random if unavailable)");
        aiListData.add("Random");
        for (final PaperCard cp : getAllAvatars()) {
            humanListData.add(cp);
            if (!cp.getRules().getAiHints().getRemRandomDecks()) {
                nonRandomHumanAvatars.add(cp);
            }
            if (!cp.getRules().getAiHints().getRemAIDecks()) {
                aiListData.add(cp);
                vgdAllAiAvatars.add(cp);
                if (!cp.getRules().getAiHints().getRemRandomDecks()) {
                    nonRandomAiAvatars.add(cp);
                }
            }
        }
    }

    /** update vanguard list. */
    public void updateVanguardList(final int playerIndex) {
        final FList<Object> vgdList = getVanguardLists().get(playerIndex);
        final Object lastSelection = vgdList.getSelectedValue();
        vgdList.setListData(isPlayerAI(playerIndex) ? aiListData : humanListData);
        if (null != lastSelection) {
            vgdList.setSelectedValue(lastSelection, true);
        }

        if (-1 == vgdList.getSelectedIndex()) {
            vgdList.setSelectedIndex(0);
        }
    }
}
