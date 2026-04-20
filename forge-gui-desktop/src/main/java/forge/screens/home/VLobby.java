package forge.screens.home;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.util.*;
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Singletons;
import forge.ai.AIOption;
import forge.deck.*;
import forge.deckchooser.FDeckChooser;
import forge.game.GameType;
import forge.game.card.CardView;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.LimitedPoolType;
import forge.gamemodes.match.GameLobby;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.*;
import forge.gamemodes.net.client.FGameClient;
import forge.gamemodes.net.event.DraftPickEvent;
import forge.gamemodes.net.event.UpdateLobbyPlayerEvent;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.gui.CardDetailPanel;
import forge.gui.FDraftOverlay;
import forge.gui.GuiChoose;
import forge.gui.SwingPrefBinders;
import forge.gui.framework.FScreen;
import forge.gui.interfaces.ILobbyView;
import forge.gui.util.SOptionPane;
import forge.interfaces.IPlayerChangeListener;
import forge.localinstance.skin.FSkinProp;
import forge.item.PaperCard;
import forge.itemmanager.ItemManagerConfig;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorLimited;
import forge.screens.deckeditor.controllers.CEditorNetworkDraft;
import forge.screens.deckeditor.controllers.NetworkDraftLog;
import forge.screens.deckeditor.views.VEditorLog;
import forge.screens.home.online.VSubmenuOnlineLobby;
import forge.toolbox.*;
import forge.toolbox.FSkin.SkinImage;
import forge.util.*;
import net.miginfocom.swing.MigLayout;

/**
 * Lobby view. View of a number of players at the deck selection stage.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VLobby implements ILobbyView {

    static final int MAX_PLAYERS = 8;
    private static final int EVENT_BTN_WIDTH = 200;
    private static final int EVENT_BTN_HEIGHT = 50;
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
    private final JComboBox<String> gamesInMatch = new JComboBox<String>(new String[] {"1","3","5"});
    private final SwingPrefBinders.ComboBox gamesInMatchBinder =
      new SwingPrefBinders.ComboBox(FPref.UI_MATCHES_PER_GAME, gamesInMatch);
    private final JPanel gamesInMatchFrame = new JPanel(new MigLayout("insets 0, gap 0, wrap 2"));
    private final JPanel constructedFrame = new JPanel(new MigLayout("insets 0, gap 0, wrap 2, hidemode 3")); // Main content frame

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
    // Cache deck choosers so switching settings doesn't re-generate random decks.
    private final Map<FPref, FDeckChooser> cachedDeckChoosers = new HashMap<>();

    private final FLabel addPlayerBtn = new FLabel.ButtonBuilder().fontSize(14).text(localizer.getMessage("lblAddAPlayer")).build();

    // Deck frame elements
    private final JPanel decksFrame = new JPanel(new MigLayout("insets 0, gap 0, wrap, hidemode 3"));
    private final FCheckBox cbSingletons = new FCheckBox(localizer.getMessage("cbSingletons"));
    private final FCheckBox cbArtifacts = new FCheckBox(localizer.getMessage("cbRemoveArtifacts"));
    private final Deck[] decks = new Deck[MAX_PLAYERS];

    // Variants
    private final List<FList<Object>> schemeDeckLists = new ArrayList<>();
    private final List<FPanel> schemeDeckPanels = new ArrayList<>(MAX_PLAYERS);

    private final List<FList<Object>> planarDeckLists = new ArrayList<>();
    private final List<FPanel> planarDeckPanels = new ArrayList<>(MAX_PLAYERS);

    private final List<FList<Object>> vgdAvatarLists = new ArrayList<>();
    private final List<FPanel> vgdPanels = new ArrayList<>(MAX_PLAYERS);
    private final List<CardDetailPanel> vgdAvatarDetails = new ArrayList<>();
    private final List<PaperCard> vgdAllAvatars = new ArrayList<>();
    private final List<PaperCard> nonRandomHumanAvatars = new ArrayList<>();
    private final List<PaperCard> nonRandomAiAvatars = new ArrayList<>();
    private final Vector<Object> humanListData = new Vector<>();
    private final Vector<Object> aiListData = new Vector<>();

    // Mode selector (network only)
    private enum LobbyMode { CONSTRUCTED, LIMITED }
    private LobbyMode currentMode = LobbyMode.CONSTRUCTED;
    private boolean suppressModeListener;
    private EventFormat configuredFormat;
    private final FComboBoxPanel<String> cboModePanel = new FComboBoxPanel<>(Localizer.getInstance().getMessage("lblNetworkLobbyMode"),
            ImmutableList.of(Localizer.getInstance().getMessage("lblNetworkModeConstructed"),
                    Localizer.getInstance().getMessage("lblNetworkModeLimited")));

    // Event config panel (top of right panel in Draft/Sealed mode)
    private final FPanel eventConfigPanel = new FPanel(new MigLayout("insets 5 10 15 10, gap 2, wrap"));
    private final FLabel lblEventFormat = new FLabel.Builder().text("\u2014").fontSize(14).fontStyle(Font.BOLD).fontAlign(javax.swing.SwingConstants.LEFT).build();
    private final FLabel lblEventProduct = new FLabel.Builder().text("\u2014").fontSize(14).fontStyle(Font.BOLD).fontAlign(javax.swing.SwingConstants.LEFT).build();
    private final FLabel lblEventPanelTitle = new FLabel.Builder().text(Localizer.getInstance().getMessage("lblNetworkEventDetailsTitle")).fontSize(15).fontStyle(Font.BOLD).build();
    private final FLabel lblEventStatus = new FLabel.Builder().fontSize(12).fontStyle(Font.ITALIC).build();
    private final FLabel lblEventFormatCaption = new FLabel.Builder().text(Localizer.getInstance().getMessage("lblNetworkFormatCaption")).fontSize(13).build();
    private final FLabel lblEventProductCaption = new FLabel.Builder().text(Localizer.getInstance().getMessage("lblNetworkProductCaption")).fontSize(13).build();
    private final FLabel lblEventPickTimerCaption = new FLabel.Builder().text(Localizer.getInstance().getMessage("lblNetworkPickTimerCaption")).fontSize(13).build();
    private final FLabel lblEventDateCaption = new FLabel.Builder().text(Localizer.getInstance().getMessage("lblNetworkDateCaption")).fontSize(13).build();
    private final FLabel lblEventDate = new FLabel.Builder().text("\u2014").fontSize(14).fontStyle(Font.BOLD).fontAlign(javax.swing.SwingConstants.LEFT).build();
    private final FLabel lblEventPickTimer = new FLabel.Builder().text("\u2014").fontSize(14).fontStyle(Font.BOLD).fontAlign(javax.swing.SwingConstants.LEFT).build();
    private final FButton btnNewEvent = new FButton(Localizer.getInstance().getMessage("lblNetworkNewEventButton"));
    private final FLabel btnDismissEvent = new FLabel.Builder().icon(FSkin.getIcon(FSkinProp.ICO_CLOSE)).iconInBackground(false).hoverable(true).tooltip(Localizer.getInstance().getMessage("lblNetworkDismissEventTooltip")).build();
    private final FCheckBox cbDeckConformance = new FCheckBox(Localizer.getInstance().getMessage("lblNetworkDeckFilter"));

    // Split panel for right side in Draft/Sealed mode
    private final FPanel eventRightPanel = new FPanel(new MigLayout("insets 0, gap 0, wrap, fill"));

    // Event dropdown (host selects completed events from local deck files)

    // Active event state
    private String activeEventId;
    private boolean activeConformance = true;
    private List<String> eventIdsByDropdownIndex = new ArrayList<>();

    // Action buttons for Draft/Sealed mode
    private final FButton btnStartEvent = new FButton(Localizer.getInstance().getMessage("lblNetworkStartDraft"));
    private final FButton btnStartMatch = new FButton(Localizer.getInstance().getMessage("lblNetworkStartMatch"));

    // Network draft state
    private CEditorNetworkDraft networkDraftEditor;
    private int mySeatIndex;
    private int lastPackNumber;

    // CTR
    public VLobby(final GameLobby lobby) {
        this.lobby = lobby;

        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        if (lobby.isAllowNetworking()) {
            cboModePanel.addActionListener(e -> onModeChanged());
            // Set a larger font on the combo box to match/exceed the variants label
            for (final Component c : cboModePanel.getComponents()) {
                c.setFont(FSkin.getBoldFont(14).getBaseFont());
            }
            constructedFrame.add(cboModePanel, "w 100%, h 28px!, gapbottom 10px, spanx 2, wrap");

            eventRightPanel.setOpaque(false);
            eventConfigPanel.setOpaque(true);
            eventConfigPanel.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2).stepColor(20).getColor());
            eventConfigPanel.setLayout(new MigLayout(
                    "insets 10 14 10 14, gap 14 8, wrap 2, hidemode 3",
                    "[110px!][grow,fill]"));

            // Muted caption color derived from CLR_TEXT so it degrades with the theme
            java.awt.Color captionColor = FSkin.getColor(FSkin.Colors.CLR_TEXT).stepColor(-80).getColor();
            lblEventFormatCaption.setForeground(captionColor);
            lblEventProductCaption.setForeground(captionColor);
            lblEventPickTimerCaption.setForeground(captionColor);
            lblEventDateCaption.setForeground(captionColor);
            lblEventStatus.setForeground(captionColor);

            // Row 1: title (+ X dismiss for host in right column)
            if (lobby.hasControl()) {
                btnDismissEvent.setCommand(() -> {
                    if (lobby instanceof ServerGameLobby serverLobby) {
                        configuredFormat = null;
                        serverLobby.clearCurrentEvent();
                    }
                    activeEventId = null;
                    broadcastEventSelection();
                    updateEventPanelState();
                    updateActionButtons();
                    updateDeckListFilter();
                });
                eventConfigPanel.add(lblEventPanelTitle, "growx, pushx");
                eventConfigPanel.add(btnDismissEvent, "w 24px!, h 24px!, align right, wrap");
            } else {
                eventConfigPanel.add(lblEventPanelTitle, "span 2, growx, wrap");
            }

            // Row 2: status subtitle (italic, muted)
            eventConfigPanel.add(lblEventStatus, "span 2, growx, wrap, gapbottom 4");

            // Rows 3-6: caption | value pairs
            eventConfigPanel.add(lblEventFormatCaption);
            eventConfigPanel.add(lblEventFormat, "wrap");
            eventConfigPanel.add(lblEventProductCaption);
            eventConfigPanel.add(lblEventProduct, "wrap");
            eventConfigPanel.add(lblEventPickTimerCaption);
            eventConfigPanel.add(lblEventPickTimer, "wrap");
            eventConfigPanel.add(lblEventDateCaption);
            eventConfigPanel.add(lblEventDate, "wrap, gapbottom 6");

            // Row 7: filter checkbox
            cbDeckConformance.setSelected(true);
            if (lobby.hasControl()) {
                cbDeckConformance.addActionListener(e -> onConformanceChanged());
            } else {
                cbDeckConformance.setEnabled(false);
            }
            eventConfigPanel.add(cbDeckConformance, "span 2, wrap");

            updateEventPanelState();
        }

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
            addPlayerBtn.setCommand(lobby::addSlot);
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
            btnStart.addActionListener(arg0 -> {
                Runnable startGame = lobby.startGame();
                if (startGame != null) {
                    startGame.run();
                }
            });
        }
        if (lobby.isAllowNetworking() && lobby.hasControl()) {
            btnStartEvent.setFont(FSkin.getRelativeFont(18));
            btnStartEvent.addActionListener(e -> startEvent());
            btnStartMatch.setFont(FSkin.getRelativeFont(18));
            btnStartMatch.addActionListener(arg0 -> {
                Runnable startGame = lobby.startGame();
                if (startGame != null) {
                    startGame.run();
                }
            });
            btnNewEvent.setFont(FSkin.getRelativeFont(18));
            btnNewEvent.addActionListener(e -> openEventConfigDialog());
        }
        String defaultGamesInMatch = FModel.getPreferences().getPref(FPref.UI_MATCHES_PER_GAME);
        if (defaultGamesInMatch == null || defaultGamesInMatch.isEmpty()) {
            defaultGamesInMatch = "3";
        }

        gamesInMatchFrame.add(newLabel(localizer.getMessage("lblGamesInMatch")), "w 150px!, h 30px!");
        gamesInMatchFrame.add(gamesInMatch, "w 50px!, h 30px!");
        gamesInMatchFrame.setOpaque(false);

        pnlStart.add(gamesInMatchFrame);
    }

    public void updateDeckPanel() {
        for (final PlayerPanel playerPanel : playerPanels) {
            playerPanel.getDeckChooser().restoreSavedState();
        }
    }

    public void focusOnAvatar() {
        getPlayerPanelWithFocus().focusOnAvatar();
    }

    private PlayerPanel getPlayerPanel(int slot) {
        return playerPanels.get(slot);
    }

    @Override
    public void update(final int slot, final LobbySlotType type) {
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
            case PAUPER_CARDGEN_DECK:
            case COLOR_DECK:
            case STANDARD_COLOR_DECK:
            case MODERN_COLOR_DECK:
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

        // Client: sync lobby mode from host's state. Items are [Constructed, Limited].
        // Suppress the mode-change listener — we'll do the UI refresh ourselves
        // below (updateRightPanelForMode) rather than letting onModeChanged
        // rebuild the panel and the outer update() rebuild it a second time.
        if (lobby.isAllowNetworking() && !lobby.hasControl() && lobby.getData() != null) {
            boolean hostIsLimited = lobby.getData().isLimitedMode();
            int desiredIndex = hostIsLimited ? 1 : 0;
            if (cboModePanel.getSelectedIndex() != desiredIndex) {
                suppressModeListener = true;
                try {
                    cboModePanel.setSelectedIndex(desiredIndex);
                    currentMode = hostIsLimited ? LobbyMode.LIMITED : LobbyMode.CONSTRUCTED;
                } finally {
                    suppressModeListener = false;
                }
            }
        }

        // Detect event-state transitions from the latest lobby data snapshot and
        // drive the same UI updates the dedicated callbacks used to perform.
        final GameLobby.GameLobbyData data = lobby.getData();
        if (data != null) {
            boolean eventPanelNeedsUpdate = false;
            NetworkEventView newView = data.getEventView();
            if (newView != lastEventView) {
                lastEventView = newView;
                eventPanelNeedsUpdate = true;
            }
            String newEventId = data.getActiveEventId();
            boolean newConformance = data.isActiveConformance();
            if (!java.util.Objects.equals(newEventId, activeEventId) || newConformance != activeConformance) {
                activeEventId = newEventId;
                activeConformance = newConformance;
                if (!lobby.hasControl()) {
                    cbDeckConformance.setSelected(newConformance);
                }
                eventPanelNeedsUpdate = true;
                if (!lobby.hasControl()) {
                    updateDeckListFilter();
                }
            }
            if (eventPanelNeedsUpdate) {
                updateEventPanelState();
            }
        }

        final boolean allowNetworking = lobby.isAllowNetworking();

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
                panel.setAiProfile(slot.getAiProfile());
                panel.update();

                final boolean isSlotAI = slot.getType() == LobbySlotType.AI;
                if (isNewPanel || fullUpdate) {
                    final FDeckChooser deckChooser = createDeckChooser(lobby.getGameType(), i, isSlotAI);
                    deckChooser.populate();
                    panel.setDeckChooser(deckChooser);
                    if (i == 0) {
                        // TODO: This seems like the wrong place to do this:
                        slot.setIsDevMode(prefs.getPrefBoolean(FPref.DEV_MODE_ENABLED));
                    }
                    if (lobby.mayEdit(i)) {
                        changePlayerFocus(i);
                    }
                } else {
                    panel.getDeckChooser().setIsAi(isSlotAI);
                }
                if (fullUpdate && (type == LobbySlotType.LOCAL || isSlotAI)) {
                    // Deck section selection
                    panel.getDeckChooser().getLstDecks().getSelectCommand().run();
                    selectSchemeDeck(i);
                    selectPlanarDeck(i);
                    selectVanguardAvatar(i);
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
            updateRightPanelForMode();
        }
        refreshPanels(true, true);
    }

    public void setPlayerChangeListener(final IPlayerChangeListener listener) {
        this.playerChangeListener = listener;
    }

    void setReady(final int index, final boolean ready) {
        // Limited mode: deck is produced by the draft/sealed flow (no pre-selection
        // required when starting a new event) or is selected from the filtered event
        // deck list when running a match from a past event. Skip the generic check.
        boolean deckRequired = currentMode != LobbyMode.LIMITED;
        if (ready && deckRequired && decks[index] == null && !vntMomirBasic.isSelected() && !vntMoJhoSto.isSelected()) {
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
            getPlayerPanel(i).setIsReady(false);
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
        final PlayerPanel panel = getPlayerPanel(index);
        return UpdateLobbyPlayerEvent.create(panel.getType(),
                panel.getPlayerName(),
                panel.getAvatarIndex(), -1 /*TODO panel.getSleeveIndex()*/,
                panel.getTeam(), panel.isArchenemy(),
                panel.isReady(),
                panel.isDevMode(),
                panel.getAiOptions(),
                panel.getAiProfile());
    }

    /** Builds the actual deck panel layouts for each player.
     * These are added to a list which can be referenced to populate the deck panel appropriately. */
    @SuppressWarnings("serial")
    private void buildDeckPanels(final int playerIndex) {
        // Scheme deck list
        buildDeckPanel(localizer.getMessage("lblSchemeDeck"), playerIndex, schemeDeckLists, schemeDeckPanels, e -> selectSchemeDeck(playerIndex));

        // Planar deck list
        buildDeckPanel(localizer.getMessage("lblPlanarDeck"), playerIndex, planarDeckLists, planarDeckPanels, e -> selectPlanarDeck(playerIndex));

        // Vanguard avatar list
        buildDeckPanel(localizer.getMessage("lblVanguardAvatar"), playerIndex, vgdAvatarLists, vgdPanels, e -> selectVanguardAvatar(playerIndex));
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

    private FDeckChooser getDeckChooser(final int iSlot) {
        return getPlayerPanel(iSlot).getDeckChooser();
    }

    private void selectMainDeck(final FDeckChooser mainChooser, final int playerIndex, final boolean isCommanderDeck) {
        final DeckType type = mainChooser.getSelectedDeckType();
        final Deck deck = mainChooser.getDeck();
        // something went wrong, clear selection to prevent error loop
        if (deck == null) {
            mainChooser.getLstDecks().setSelectedIndex(0);
        }
        final Collection<DeckProxy> selectedDecks = mainChooser.getLstDecks().getSelectedItems();
        if (playerIndex < activePlayersNum && lobby.mayEdit(playerIndex)) {
            final String text = type.toString() + ": " + Lang.joinHomogenous(selectedDecks, DeckProxy::getName);
            if (isCommanderDeck) {
                getPlayerPanel(playerIndex).setCommanderDeckSelectorButtonText(text);
            } else {
                getPlayerPanel(playerIndex).setDeckSelectorButtonText(text);
            }
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
                final Deck randomDeck = RandomDeckGenerator.getRandomUserDeck(lobby, isPlayerAI(playerIndex));
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
                final Deck randomDeck = RandomDeckGenerator.getRandomUserDeck(lobby, isPlayerAI(playerIndex));
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
        final PlayerPanel pp = getPlayerPanel(playerIndex);
        final CardDetailPanel cdp = vgdAvatarDetails.get(playerIndex);

        PaperCard vanguardAvatar = null;
        final Deck deck = decks[playerIndex];
        if (selected instanceof PaperCard) {
            pp.setVanguardButtonText(((PaperCard) selected).getDisplayName());
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
                if (isPlayerAI(playerIndex)) { //AI
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
            decksFrame.add(getDeckChooser(playerWithFocus), "grow, push");
            if (getDeckChooser(playerWithFocus).getSelectedDeckType().toString().contains(localizer.getMessage("lblRandom"))) {
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
        case Oathbreaker:
        case TinyLeaders:
        case Brawl:
            decksFrame.add(getDeckChooser(playerWithFocus), "grow, push");
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
    public List<FDeckChooser> getDeckChoosers() {
        List<FDeckChooser> choosers = Lists.newArrayList();
        for (final PlayerPanel playerPanel : playerPanels) {
            choosers.add(playerPanel.getDeckChooser());
        }
        return choosers;
    }

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

    void setCurrentGameMode(final GameType mode) {
        lobby.setGameType(mode);
        update(true);
    }

    private boolean isPlayerAI(final int playernum) {
        if (playernum < activePlayersNum) {
            return playerPanels.get(playernum).isAi();
        }
        return true;
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
        updateRightPanelForMode();

        refreshPanels(true, true);
    }

    private void onModeChanged() {
        // update() uses this flag to refresh the combo silently — the outer
        // update() path handles the panel rebuild, so skip the work here.
        if (suppressModeListener) return;

        // Client: mode is host-controlled. If a user click diverges from the synced
        // value, revert via setSelectedIndex (which re-fires this listener). Only
        // return early when we actually revert — otherwise fall through so the rest
        // of the UI (right panel, action buttons) updates with the new mode.
        // (setEnabled(false) breaks FComboBox rendering, so we intercept here.)
        if (lobby.isAllowNetworking() && !lobby.hasControl()) {
            boolean hostIsLimited = lobby.getData() != null && lobby.getData().isLimitedMode();
            int desiredIndex = hostIsLimited ? 1 : 0;
            if (cboModePanel.getSelectedIndex() != desiredIndex) {
                cboModePanel.setSelectedIndex(desiredIndex);
                return;
            }
        }

        final String selected = cboModePanel.getSelectedItem();
        if (localizer.getMessage("lblNetworkModeLimited").equals(selected)) {
            currentMode = LobbyMode.LIMITED;
        } else {
            currentMode = LobbyMode.CONSTRUCTED;
        }

        final boolean isLimited = (currentMode == LobbyMode.LIMITED);

        // Clear event when switching away from Limited, and broadcast the new mode.
        if (lobby.hasControl() && lobby instanceof ServerGameLobby serverLobby) {
            if (!isLimited) {
                configuredFormat = null;
                serverLobby.clearCurrentEvent();
            }
            serverLobby.setLimitedMode(isLimited);
        }
        updateEventPanelState();

        // Toggle variants panel visibility — it's inside an FScrollPane
        Container scrollPane = variantsPanel.getParent();
        while (scrollPane != null && !(scrollPane instanceof JScrollPane)) {
            scrollPane = scrollPane.getParent();
        }
        if (scrollPane != null) {
            scrollPane.setVisible(!isLimited);
        }

        // Update right panel content
        updateRightPanelForMode();

        // Update action buttons
        updateActionButtons();

        constructedFrame.revalidate();
        constructedFrame.repaint();
    }

    private void updateRightPanelForMode() {
        decksFrame.removeAll();
        if (currentMode == LobbyMode.CONSTRUCTED) {
            populateDeckPanel(lobby.getGameType());
        } else {
            eventRightPanel.removeAll();
            eventRightPanel.add(eventConfigPanel, "w 100%, growx, gapbottom 10px, wrap");

            if (playerWithFocus < playerPanels.size() && lobby.mayEdit(playerWithFocus)) {
                final FDeckChooser chooser = getDeckChooser(playerWithFocus);
                if (chooser != null) {
                    eventRightPanel.add(chooser, "w 100%, h 100%, grow, push");
                }
            }

            decksFrame.add(eventRightPanel, "w 100%, h 100%, growy, pushy");

            if (lobby.hasControl()) {
                scanAvailableEvents();
            }
            updateDeckListFilter();
        }
        decksFrame.revalidate();
        decksFrame.repaint();
    }

    private void updateActionButtons() {
        final boolean isLimited = (currentMode == LobbyMode.LIMITED);

        // Rebuild pnlStart layout
        pnlStart.removeAll();
        pnlStart.setOpaque(false);
        if (lobby.hasControl()) {
            if (isLimited) {
                pnlStart.setLayout(new MigLayout("insets 0, gap 0"));
                final String label = (configuredFormat == EventFormat.SEALED)
                        ? localizer.getMessage("lblNetworkGeneratePools")
                        : localizer.getMessage("lblNetworkStartDraft");
                btnStartEvent.setText(label);
                boolean isExistingEvent = activeEventId != null;
                btnStartEvent.setEnabled(configuredFormat != null && !isExistingEvent);
                btnStartMatch.setEnabled(isExistingEvent);
                final String eventBtn = "w " + EVENT_BTN_WIDTH + "px!, h " + EVENT_BTN_HEIGHT + "px!";
                pnlStart.add(btnNewEvent, "cell 0 0, " + eventBtn + ", gapright 20");
                pnlStart.add(btnStartEvent, "cell 1 0, " + eventBtn + ", gapright 20");
                pnlStart.add(btnStartMatch, "cell 2 0, " + eventBtn);
                pnlStart.add(gamesInMatchFrame, "cell 2 1, align center");
            } else {
                // Constructed mode: Start button centered with games-in-match below
                pnlStart.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));
                pnlStart.add(btnStart, "align center, spanx 2, wrap");
                pnlStart.add(gamesInMatchFrame, "spanx 2, align center");
            }
        }
        // Non-host: nothing to show here — match controls are host-only.
        pnlStart.revalidate();
        pnlStart.repaint();
    }

    private void startEvent() {
        if (!(lobby instanceof ServerGameLobby serverLobby)) return;
        NetworkEvent event = serverLobby.getCurrentEvent();
        if (event == null) {
            FOptionPane.showErrorDialog(localizer.getMessage("lblNetworkNoEventConfigured"));
            return;
        }

        // Require all non-OPEN participants to be ready before starting the draft/sealed
        for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
            LobbySlot slot = lobby.getSlot(i);
            if (slot == null || slot.getType() == LobbySlotType.OPEN) continue;
            if (!slot.isReady()) {
                SOptionPane.showMessageDialog(localizer.getMessage("lblPlayerIsNotReady", slot.getName()));
                return;
            }
        }

        if (event.getFormat() == EventFormat.SEALED) {
            serverLobby.startSealedEvent();
        } else if (event.getFormat() == EventFormat.BOOSTER_DRAFT) {
            ServerGameLobby.DraftStartResult result = serverLobby.startDraftEvent();
            if (result == null) {
                FOptionPane.showErrorDialog(localizer.getMessage("lblNetworkFailedDraft"));
                return;
            }
            mySeatIndex = result.hostSeatIndex();
            FDraftOverlay.SINGLETON_INSTANCE.initDraft(
                    mySeatIndex, result.names(), result.aiFlags(), result.totalPacks());
            NetworkDraftLog.logDraftStart(
                    event.getParticipants(), result.totalPacks(),
                    event.getProductDescription(), mySeatIndex);
            lastPackNumber = 0;
        }
    }

    private void openEventConfigDialog() {
        if (!(lobby instanceof ServerGameLobby serverLobby)) return;

        // Step 0: If past events exist, offer a choice between creating new and loading one
        if (!eventIdsByDropdownIndex.isEmpty()) {
            String[] setupOptions = {
                    localizer.getMessage("lblNetworkSetUpEventCreate"),
                    localizer.getMessage("lblNetworkSetUpEventLoadPast")
            };
            String setupChoice = GuiChoose.oneOrNone(
                    localizer.getMessage("lblNetworkSetUpEventPrompt"), setupOptions);
            if (setupChoice == null) return;
            if (setupChoice.equals(setupOptions[1])) {
                openLoadPastEventDialog();
                return;
            }
        }

        // Step 1: Choose event type (Draft / Sealed)
        String[] formatNames = { localizer.getMessage("lblNetworkModeDraft"), localizer.getMessage("lblNetworkModeSealed") };
        String chosenFormatName = GuiChoose.oneOrNone(
                localizer.getMessage("lblNetworkChooseEventType"), formatNames);
        if (chosenFormatName == null) return;
        EventFormat chosenFormat = formatNames[0].equals(chosenFormatName)
                ? EventFormat.BOOSTER_DRAFT : EventFormat.SEALED;

        // Create event with chosen format
        serverLobby.createEvent(chosenFormat);
        configuredFormat = chosenFormat;

        // Step 2: Choose pool type
        boolean isDraft = (chosenFormat == EventFormat.BOOSTER_DRAFT);
        LimitedPoolType[] poolTypes = LimitedPoolType.values(isDraft);
        LimitedPoolType chosen = GuiChoose.oneOrNone(
                localizer.getMessage("lblNetworkChooseDraftFormat"), poolTypes);
        if (chosen == null) return;

        // Step 3: For draft, build the BoosterDraft now so block/set/cube/theme sub-dialogs
        // pop before the timer prompt — matches the offline CSubmenuDraft flow.
        BoosterDraft draft = null;
        if (isDraft) {
            draft = BoosterDraft.createDraftForNetwork(chosen);
            if (draft == null) return;
        }

        // Re-fetch current event after modal dialog — earlier reference may be stale
        // if a lobby update has run during EDT event pump
        NetworkEvent event = serverLobby.getCurrentEvent();
        if (event == null) return;

        // Step 4: Pick timer + disconnect grace period (draft only, combined prompt)
        int timerSeconds = event.getPickTimerSeconds();
        int graceSeconds = event.getDisconnectGraceSeconds();
        if (isDraft) {
            FTextField pickField = new FTextField.Builder().text(String.valueOf(timerSeconds)).build();
            FTextField graceField = new FTextField.Builder().text(String.valueOf(graceSeconds)).build();
            FLabel pickLbl = new FLabel.Builder().fontSize(12).text(localizer.getMessage("lblNetworkPickTimerPrompt")).build();
            FLabel graceLbl1 = new FLabel.Builder().fontSize(12).text(localizer.getMessage("lblNetworkGraceTimerPromptLine1")).build();
            FLabel graceLbl2 = new FLabel.Builder().fontSize(12).text(localizer.getMessage("lblNetworkGraceTimerPromptLine2")).build();

            JPanel panel = new JPanel(new MigLayout("insets 4, gap 2 4, wrap 1"));
            panel.setOpaque(false);
            panel.add(pickLbl);
            panel.add(pickField, "w 80!");
            panel.add(graceLbl1, "gaptop 10");
            panel.add(graceLbl2);
            panel.add(graceField, "w 80!");

            int result = FOptionPane.showOptionDialog(
                    null,
                    localizer.getMessage("lblNetworkDraftTimersTitle"),
                    null,
                    panel,
                    java.util.Arrays.asList(
                            localizer.getMessage("lblOK"),
                            localizer.getMessage("lblCancel")));
            if (result == 0) {
                try {
                    int parsed = Integer.parseInt(pickField.getText().trim());
                    if (parsed >= 0) timerSeconds = parsed;
                } catch (NumberFormatException ignored) { }
                try {
                    int parsed = Integer.parseInt(graceField.getText().trim());
                    if (parsed >= 0) graceSeconds = parsed;
                } catch (NumberFormatException ignored) { }
            }
        }

        // Delegate server-side configuration
        if (!serverLobby.configureEvent(chosen, draft, timerSeconds, graceSeconds)) {
            return;
        }
        updateEventPanelState();
        updateActionButtons();
    }

    private void openLoadPastEventDialog() {
        if (eventIdsByDropdownIndex.isEmpty()) return;
        List<EventChoice> choices = new ArrayList<>(eventIdsByDropdownIndex.size());
        for (String id : eventIdsByDropdownIndex) {
            choices.add(new EventChoice(id, getEventDisplayLabel(id)));
        }
        EventChoice chosen = GuiChoose.oneOrNone(
                localizer.getMessage("lblNetworkLoadPastEventPrompt"), choices);
        if (chosen == null) return;
        activeEventId = chosen.id();
        updateEventPanelState();
        updateActionButtons();
        updateDeckListFilter();
        broadcastEventSelection();
    }

    private record EventChoice(String id, String label) {
        @Override public String toString() { return label; }
    }

    private void updateEventPanelState() {
        if (!lobby.isAllowNetworking()) return;

        final boolean isHost = lobby.hasControl();
        final NetworkEvent currentEvent = (lobby instanceof ServerGameLobby sgl) ? sgl.getCurrentEvent() : null;
        final boolean inState2 = activeEventId != null;
        final boolean inState1 = !inState2 && (isHost ? currentEvent != null : lastEventView != null);

        // Row 1 — corner X dismiss visible for host whenever an event is active
        if (isHost) {
            btnDismissEvent.setVisible(inState1 || inState2);
        }

        // Row 2 — status subtitle (visible in States 0/1; hidden in State 2 since data rows speak)
        String statusText = "";
        if (isHost) {
            if (inState1) {
                boolean isSealed = currentEvent != null && currentEvent.getFormat() == EventFormat.SEALED;
                statusText = localizer.getMessage(
                        isSealed ? "lblNetworkNewEventNoPools" : "lblNetworkNewEventNotDrafted");
            } else if (!inState2) {
                statusText = localizer.getMessage("lblNetworkNoEventStatus");
            }
        } else if (inState1) {
            statusText = localizer.getMessage("lblNetworkHostSettingUpEvent");
        } else if (!inState2) {
            statusText = localizer.getMessage("lblNetworkWaitingForHost");
        }
        lblEventStatus.setText(statusText);
        lblEventStatus.setVisible(!statusText.isEmpty());

        // Rows 3-6 — value cells for Format / Product / Pick timer / Date
        String formatText = "\u2014";
        String productText = "\u2014";
        String timerText = "\u2014";
        String dateText = "\u2014";
        if (inState2) {
            String[] tags = findEventTags(activeEventId);
            if (tags != null) {
                if (tags[0] != null) {
                    formatText = EventFormat.BOOSTER_DRAFT.name().equals(tags[0])
                            ? localizer.getMessage("lblNetworkModeDraft")
                            : localizer.getMessage("lblNetworkModeSealed");
                }
                if (tags[1] != null && !tags[1].isEmpty()) productText = tags[1];
                if (tags[2] != null && !tags[2].isEmpty()) dateText = tags[2];
            }
        } else if (inState1) {
            EventFormat evFormat;
            int timerSec;
            String desc;
            LimitedPoolType pool = null;
            if (isHost) {
                evFormat = currentEvent.getFormat();
                timerSec = currentEvent.getPickTimerSeconds();
                desc = currentEvent.getProductDescription();
                pool = currentEvent.getPoolType();
            } else {
                evFormat = lastEventView.getFormat();
                timerSec = lastEventView.getPickTimerSeconds();
                desc = lastEventView.getProductDescription();
            }
            formatText = (evFormat == EventFormat.BOOSTER_DRAFT)
                    ? localizer.getMessage("lblNetworkModeDraft")
                    : localizer.getMessage("lblNetworkModeSealed");
            if (desc != null && !desc.isEmpty()) {
                productText = desc;
            } else if (pool != null) {
                productText = pool.toString();
            }
            if (evFormat == EventFormat.BOOSTER_DRAFT) {
                timerText = timerSec > 0 ? timerSec + "s" : "\u2014";
            } else {
                timerText = localizer.getMessage("lblNetworkPickTimerNotApplicable");
            }
        }
        lblEventFormat.setText(formatText);
        lblEventProduct.setText(productText);
        lblEventPickTimer.setText(timerText);
        lblEventDate.setText(dateText);

        // Row 7 — filter checkbox: visible only when an event is loaded (State 2).
        cbDeckConformance.setVisible(inState2);
        cbDeckConformance.setEnabled(isHost && !inState1);

        eventConfigPanel.revalidate();
        eventConfigPanel.repaint();
    }

    private String getEventDisplayLabel(String eventId) {
        String[] tags = findEventTags(eventId);
        if (tags == null) return eventId;
        String displayFormat = EventFormat.BOOSTER_DRAFT.name().equals(tags[0]) ? "Draft" : "Sealed";
        return displayFormat + " \u2014 " + (tags[1] == null ? "" : tags[1]) + " \u2014 (" + (tags[2] == null ? "" : tags[2]) + ")";
    }

    /** Returns {eventFormat, eventProduct, eventDate} for the deck tagged with eventId, or null if not found. */
    private static String[] findEventTags(String eventId) {
        for (Deck d : FModel.getDecks().getNetworkEventDecks()) {
            if (eventId.equals(DeckProxy.getEventTag(d, "eventId"))) {
                return new String[] {
                        DeckProxy.getEventTag(d, "eventFormat"),
                        DeckProxy.getEventTag(d, "eventProduct"),
                        DeckProxy.getEventTag(d, "eventDate"),
                };
            }
        }
        return null;
    }

    private void scanAvailableEvents() {
        LinkedHashSet<String> eventIds = new LinkedHashSet<>();
        for (Deck d : FModel.getDecks().getNetworkEventDecks()) {
            String eventId = DeckProxy.getEventTag(d, "eventId");
            if (eventId != null) eventIds.add(eventId);
        }
        eventIdsByDropdownIndex = new ArrayList<>(eventIds);
    }

    private void onConformanceChanged() {
        activeConformance = cbDeckConformance.isSelected();
        updateDeckListFilter();
        broadcastEventSelection();
    }

    private void broadcastEventSelection() {
        if (lobby.hasControl() && lobby instanceof ServerGameLobby serverLobby) {
            serverLobby.selectEventForMatch(activeEventId, activeConformance);
        }
    }

    private void updateDeckListFilter() {
        if (currentMode == LobbyMode.CONSTRUCTED) return;
        if (playerWithFocus >= playerPanels.size() || !lobby.mayEdit(playerWithFocus)) return;

        final FDeckChooser chooser = getDeckChooser(playerWithFocus);
        if (chooser == null) return;

        if (chooser.getSelectedDeckType() != DeckType.NET_EVENT_DECK) {
            chooser.setSelectedDeckType(DeckType.NET_EVENT_DECK);
        }

        // Re-read pools from disk so edits made in the deck editor are reflected.
        FModel.getDecks().reloadNetworkEventDecks();

        List<DeckProxy> allDecks;
        if (activeEventId == null) {
            // No event loaded — there are no valid decks for a limited match yet.
            allDecks = new ArrayList<>();
        } else if (activeConformance) {
            allDecks = new ArrayList<>(DeckProxy.getAllNetworkEventDecks());
            allDecks.removeIf(dp -> {
                Deck d = dp.getDeck();
                return d == null || !activeEventId.equals(DeckProxy.getEventTag(d, "eventId"));
            });
        } else {
            allDecks = new ArrayList<>(DeckProxy.getAllNetworkEventDecks());
        }

        // Preserve the user's current pick across pool rebuilds. Match by deck name —
        // reloadNetworkEventDecks() rebuilds DeckProxy instances so reference equality
        // would fail on every lobby update, silently resetting selection.
        DeckProxy previouslySelected = chooser.getLstDecks().getSelectedItem();
        String prevName = (previouslySelected != null && previouslySelected.getDeck() != null)
                ? previouslySelected.getDeck().getName() : null;
        chooser.getLstDecks().setPool(allDecks);
        chooser.getLstDecks().setup(ItemManagerConfig.NET_EVENT_DECKS);
        if (prevName != null) {
            for (DeckProxy dp : allDecks) {
                if (dp.getDeck() != null && prevName.equals(dp.getDeck().getName())) {
                    chooser.getLstDecks().setSelectedItem(dp);
                    break;
                }
            }
        }
    }

    /** Saves avatar prefs for players one and two. */
    void updateAvatarPrefs() {
        final int pOneIndex = getPlayerPanel(0).getAvatarIndex();
        final int pTwoIndex = getPlayerPanel(1).getAvatarIndex();

        prefs.setPref(FPref.UI_AVATARS, pOneIndex + "," + pTwoIndex);
        prefs.save();
    }

    /** Saves sleeve prefs for players one and two. */
    void updateSleevePrefs() {
        final int pOneIndex = getPlayerPanel(0).getSleeveIndex();
        final int pTwoIndex = getPlayerPanel(1).getSleeveIndex();

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
            addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    lobby.applyVariant(variantType);
                } else {
                    lobby.removeVariant(variantType);
                }
                VLobby.this.update(false);
            });
        }
    }

    private FDeckChooser createDeckChooser(final GameType type, final int iSlot, final boolean ai) {
        boolean forCommander;
        DeckType deckType;
        FPref prefKey;
        switch (type) {
            case Commander:
                forCommander = true;
                deckType = iSlot == 0 ? DeckType.COMMANDER_DECK : DeckType.RANDOM_CARDGEN_COMMANDER_DECK;
                prefKey = FPref.COMMANDER_DECK_STATES[iSlot];
                break;
            case TinyLeaders:
                forCommander = true;
                deckType = iSlot == 0 ? DeckType.TINY_LEADERS_DECK : DeckType.RANDOM_CARDGEN_COMMANDER_DECK;
                prefKey = FPref.TINY_LEADER_DECK_STATES[iSlot];
                break;
            case Oathbreaker:
                forCommander = true;
                deckType = iSlot == 0 ? DeckType.OATHBREAKER_DECK : DeckType.RANDOM_CARDGEN_COMMANDER_DECK;
                prefKey = FPref.OATHBREAKER_DECK_STATES[iSlot];
                break;
            case Brawl:
                forCommander = true;
                deckType = iSlot == 0 ? DeckType.BRAWL_DECK : DeckType.CUSTOM_DECK;
                prefKey = FPref.BRAWL_DECK_STATES[iSlot];
                break;
            default:
                forCommander = false;
                deckType = iSlot == 0 ? DeckType.PRECONSTRUCTED_DECK : DeckType.COLOR_DECK;
                prefKey = FPref.CONSTRUCTED_DECK_STATES[iSlot];
                break;
        }
        return cachedDeckChoosers.computeIfAbsent(prefKey, (key) -> {
            final GameType gameType = forCommander ? type : GameType.Constructed;
            final FDeckChooser fdc = new FDeckChooser(null, ai, gameType, forCommander);
            fdc.initialize(prefKey, deckType);
            fdc.getLstDecks().setSelectCommand(() -> selectMainDeck(fdc, iSlot, forCommander));
            return fdc;
        });
    }

    final ActionListener nameListener = e -> {
        final FTextField nField = (FTextField)e.getSource();
        nField.transferFocus();
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
        return getPlayerPanel(playernum).isArchenemy();
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

    /** Return the Vanguard avatars not flagged RemoveDeck:Random. */
    public List<PaperCard> getNonRandomHumanAvatars() {
        return nonRandomHumanAvatars;
    }

    /** Return the Vanguard avatars not flagged RemoveDeck:All or RemoveDeck:Random. */
    public List<PaperCard> getNonRandomAiAvatars() {
        return nonRandomAiAvatars;
    }

    /** Return the gamesInMatchBinder */
    public SwingPrefBinders.ComboBox getGamesInMatchBinder() {
      return gamesInMatchBinder;
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

    // Tracks last-seen event view so clients can init the overlay on first pack;
    // updated in update() when GameLobbyData.eventView changes
    private NetworkEventView lastEventView;

    @Override
    public void onDraftPackArrived(int seatIndex, List<PaperCard> pack,
            int packNumber, int pickNumber, int timerDurationSeconds) {
        SwingUtilities.invokeLater(() -> {
            // Init overlay/editor BEFORE processing pack info so pod names are set first.
            if (networkDraftEditor == null) {
                initDraftEditor(seatIndex);
            }

            FDraftOverlay.SINGLETON_INSTANCE.onPackArrived(packNumber, pickNumber, pack.size(), timerDurationSeconds);

            // Log pack header on new pack round
            if (packNumber != lastPackNumber) {
                lastPackNumber = packNumber;
                boolean passingRight = (packNumber % 2 == 1);
                NetworkDraftLog.logPackHeader(packNumber, passingRight);
            }

            networkDraftEditor.showPack(pack, packNumber, pickNumber);
        });
    }

    private void initDraftEditor(int seatIndex) {
        mySeatIndex = seatIndex;

        // Initialize FDraftOverlay if not already done (client path).
        // Always prefer the latest state's eventView over the cached copy — the
        // first lobby broadcast during configureEvent() carries an empty
        // participants list; only the subsequent state broadcast from
        // startDraftEvent carries the populated pod.
        if (lobby.getData() != null && lobby.getData().getEventView() != null) {
            lastEventView = lobby.getData().getEventView();
        }
        if (lastEventView != null) {
            List<EventParticipant> participants = lastEventView.getParticipants();
            int totalPacks = lastEventView.getNumRounds();
            String[] names = new String[participants.size()];
            boolean[] aiFlags = new boolean[participants.size()];
            // Index by seat, not list position — seats are shuffled server-side
            // so list order and seat order diverge.
            for (EventParticipant p : participants) {
                int seat = p.getSeatIndex();
                if (seat >= 0 && seat < names.length) {
                    names[seat] = p.getName();
                    aiFlags[seat] = p.isAI();
                }
            }
            FDraftOverlay.SINGLETON_INSTANCE.initDraft(
                    mySeatIndex, names, aiFlags, totalPacks);
            // Log draft start for client
            NetworkDraftLog.logDraftStart(
                    participants, totalPacks,
                    lastEventView.getProductDescription(), mySeatIndex);
        }

        // Build pick sender based on host vs client
        Consumer<DraftPickEvent> pickSender;
        if (lobby instanceof ServerGameLobby serverLobby) {
            pickSender = ev -> serverLobby.handleDraftPick(ev, -1);
        } else {
            FGameClient gameClient =
                    VSubmenuOnlineLobby.SINGLETON_INSTANCE.getClient();
            if (gameClient == null) {
                return;
            }
            pickSender = gameClient::send;
        }

        networkDraftEditor = new CEditorNetworkDraft(
                mySeatIndex, pickSender, this::cancelActiveDraft,
                CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture());
        VEditorLog.SINGLETON_INSTANCE.resetNewDraft();

        Singletons.getControl().setCurrentScreen(FScreen.DRAFTING_PROCESS);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(networkDraftEditor);
    }

    private String resolveParticipantName(int seatIndex) {
        EventParticipant p = findParticipant(lastEventView != null ? lastEventView.getParticipants() : null, seatIndex);
        if (p == null && lobby instanceof ServerGameLobby sgl && sgl.getCurrentEvent() != null) {
            p = findParticipant(sgl.getCurrentEvent().getParticipants(), seatIndex);
        }
        if (p == null) return localizer.getMessage("lblSeatN", String.valueOf(seatIndex));
        return p.isAI() ? p.getName() + " (" + localizer.getMessage("lblAI") + ")" : p.getName();
    }

    private static EventParticipant findParticipant(List<EventParticipant> list, int seatIndex) {
        if (list == null) return null;
        for (EventParticipant p : list) {
            if (p.getSeatIndex() == seatIndex) return p;
        }
        return null;
    }

    @Override
    public void onDraftSeatPicked(int seatIndex, int[] seatQueueDepths) {
        SwingUtilities.invokeLater(() -> {
            FDraftOverlay.SINGLETON_INSTANCE.onSeatPicked(seatQueueDepths);

            int depth = (seatIndex >= 0 && seatIndex < seatQueueDepths.length) ? seatQueueDepths[seatIndex] : 0;
            if (seatIndex == mySeatIndex) {
                // Flush our own deferred log line with the authoritative depth
                if (networkDraftEditor != null) {
                    networkDraftEditor.flushSelfPickLog(depth);
                }
            } else {
                NetworkDraftLog.logOtherPick(resolveParticipantName(seatIndex), depth);
            }
        });
    }

    @Override
    public void onDraftAutoPicked(int seatIndex, PaperCard card, int packNumber, int pickInPack) {
        SwingUtilities.invokeLater(() -> {
            if (networkDraftEditor != null) {
                networkDraftEditor.addAutoPickedCard(card, packNumber, pickInPack);
            }
        });
    }

    /**
     * Abort any active network draft: release the push-model editor reference
     * and hide the floating overlay. Called on disconnect / stop lobby / when
     * the user manually leaves the draft screen. Safe to call when no draft
     * is in progress.
     */
    public void cancelActiveDraft() {
        networkDraftEditor = null;
        FDraftOverlay.SINGLETON_INSTANCE.reset();
    }

    @Override
    public void onReceiveEventPool(String eventId, Deck pool) {
        SwingUtilities.invokeLater(() -> {
            if (networkDraftEditor != null) {
                networkDraftEditor.completeDraft(pool);
                networkDraftEditor = null;
            } else {
                // Sealed path: save pool and open deck editor
                FModel.getDecks().getNetworkEventDecks().add(pool);
                CEditorLimited<Deck> editor = new CEditorLimited<>(
                        FModel.getDecks().getNetworkEventDecks(), Deck::new,
                        FScreen.DECK_EDITOR_SEALED, CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture());
                Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_SEALED);
                CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(editor);
                editor.getDeckController().load(null, pool.getName());
            }
            lastPackNumber = 0;

            activeEventId = eventId;
            activeConformance = true;
            if (lobby.hasControl()) {
                scanAvailableEvents();
                broadcastEventSelection();
            }
            updateRightPanelForMode();
            updateEventPanelState();
            updateActionButtons();
        });
    }
}
