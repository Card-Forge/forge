package forge.screens.home;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.StringUtils;

import forge.UiCommand;
import forge.deck.DeckProxy;
import forge.deck.DeckType;
import forge.deckchooser.DecksComboBoxEvent;
import forge.deckchooser.FDeckChooser;
import forge.deckchooser.IDecksComboBoxListener;
import forge.game.GameType;
import forge.game.card.CardView;
import forge.gui.CardDetailPanel;
import forge.interfaces.ILobby;
import forge.interfaces.IPlayerChangeListener;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.net.game.LobbySlotType;
import forge.net.game.LobbyState;
import forge.net.game.LobbyState.LobbyPlayerData;
import forge.net.game.server.RemoteClient;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
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
import forge.util.Lang;
import forge.util.NameGenerator;

/**
 * Lobby view. View of a number of players at the deck selection stage.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VLobby implements ILobby {

    static final int MAX_PLAYERS = 8;
    private static final ForgePreferences prefs = FModel.getPreferences();

    public enum LobbyType { LOCAL, SERVER, CLIENT; }

    // General variables
    private final LobbyType type;
    private int localPlayer = 0;
    private IPlayerChangeListener playerChangeListener = null;
    private final LblHeader lblTitle = new LblHeader("Sanctioned Format: Constructed");
    private int activePlayersNum = 2;
    private int playerWithFocus = 0; // index of the player that currently has focus
    private PlayerPanel playerPanelWithFocus;
    private GameType currentGameMode = GameType.Constructed;
    private List<Integer> teams = new ArrayList<Integer>(MAX_PLAYERS);
    private List<Integer> archenemyTeams = new ArrayList<Integer>(MAX_PLAYERS);

    private final StartButton btnStart  = new StartButton();
    private final JPanel pnlStart = new JPanel(new MigLayout("insets 0, gap 0, wrap 2"));
    private final JPanel constructedFrame = new JPanel(new MigLayout("insets 0, gap 0, wrap 2")); // Main content frame

    // Variants frame and variables
    private final Set<GameType> appliedVariants = new TreeSet<GameType>();
    private final FPanel variantsPanel = new FPanel(new MigLayout("insets 10, gapx 10"));
    private final VariantCheckBox vntVanguard = new VariantCheckBox(GameType.Vanguard);
    private final VariantCheckBox vntMomirBasic = new VariantCheckBox(GameType.MomirBasic);
    private final VariantCheckBox vntCommander = new VariantCheckBox(GameType.Commander);
    private final VariantCheckBox vntTinyLeaders = new VariantCheckBox(GameType.TinyLeaders);
    private final VariantCheckBox vntPlanechase = new VariantCheckBox(GameType.Planechase);
    private final VariantCheckBox vntArchenemy = new VariantCheckBox(GameType.Archenemy);
    private final VariantCheckBox vntArchenemyRumble = new VariantCheckBox(GameType.ArchenemyRumble);

    // Player frame elements
    private final JPanel playersFrame = new JPanel(new MigLayout("insets 0, gap 0 5, wrap, hidemode 3"));
    private final FScrollPanel playersScroll = new FScrollPanel(new MigLayout("insets 0, gap 0, wrap, hidemode 3"), true);
    private final List<PlayerPanel> playerPanels = new ArrayList<PlayerPanel>(MAX_PLAYERS);

    private final FLabel addPlayerBtn = new FLabel.ButtonBuilder().fontSize(14).text("Add a Player").build();

    // Deck frame elements
    private final JPanel decksFrame = new JPanel(new MigLayout("insets 0, gap 0, wrap, hidemode 3"));
    private final List<FDeckChooser> deckChoosers = new ArrayList<FDeckChooser>(8);
    private final FCheckBox cbSingletons = new FCheckBox("Singleton Mode");
    private final FCheckBox cbArtifacts = new FCheckBox("Remove Artifacts");

    // Variants
    private final List<FList<Object>> schemeDeckLists = new ArrayList<FList<Object>>();
    private final List<FPanel> schemeDeckPanels = new ArrayList<FPanel>(MAX_PLAYERS);
    private int lastArchenemy = 0;

    private final List<FList<Object>> commanderDeckLists = new ArrayList<FList<Object>>();
    private final List<FPanel> commanderDeckPanels = new ArrayList<FPanel>(MAX_PLAYERS);

    private final List<FList<Object>> planarDeckLists = new ArrayList<FList<Object>>();
    private final List<FPanel> planarDeckPanels = new ArrayList<FPanel>(MAX_PLAYERS);

    private final List<FList<Object>> vgdAvatarLists = new ArrayList<FList<Object>>();
    private final List<FPanel> vgdPanels = new ArrayList<FPanel>(MAX_PLAYERS);
    private final List<CardDetailPanel> vgdAvatarDetails = new ArrayList<CardDetailPanel>();
    private final List<PaperCard> vgdAllAvatars = new ArrayList<PaperCard>();
    private final List<PaperCard> vgdAllAiAvatars = new ArrayList<PaperCard>();
    private final List<PaperCard> nonRandomHumanAvatars = new ArrayList<PaperCard>();
    private final List<PaperCard> nonRandomAiAvatars = new ArrayList<PaperCard>();
    private final Vector<Object> humanListData = new Vector<Object>();
    private final Vector<Object> aiListData = new Vector<Object>();

    // CTR
    public VLobby(final LobbyType type) {
        this.type = type;

        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        ////////////////////////////////////////////////////////
        //////////////////// Variants Panel ////////////////////

        variantsPanel.setOpaque(false);
        variantsPanel.add(newLabel("Variants:"));
        variantsPanel.add(vntVanguard);
        variantsPanel.add(vntMomirBasic);
        variantsPanel.add(vntCommander);
        variantsPanel.add(vntTinyLeaders);
        variantsPanel.add(vntPlanechase);
        variantsPanel.add(vntArchenemy);
        variantsPanel.add(vntArchenemyRumble);

        constructedFrame.add(new FScrollPane(variantsPanel, false, true,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                "w 100%, h 45px!, gapbottom 10px, spanx 2, wrap");

        ////////////////////////////////////////////////////////
        ///////////////////// Player Panel /////////////////////

        // Construct individual player panels
        String constraints = "pushx, growx, wrap, hidemode 3";
        for (int i = 0; i < MAX_PLAYERS; i++) {
            teams.add(i + 1);
            archenemyTeams.add(i == 0 ? 1 : 2);

            final PlayerPanel player = new PlayerPanel(this, i, type);
            if (type == LobbyType.CLIENT) {
                player.setRemote(true);
            }
            playerPanels.add(player);

            // Populate players panel
            player.setVisible(i < activePlayersNum);

            playersScroll.add(player, constraints);

            if (i == 0) {
                constraints += ", gaptop 5px";
            }
        }

        playerPanelWithFocus = playerPanels.get(0);
        playerPanelWithFocus.setFocused(true);

        playersFrame.setOpaque(false);
        playersFrame.add(playersScroll, "w 100%, h 100%-35px");

        if (type != LobbyType.CLIENT) {
            addPlayerBtn.setFocusable(true);
            addPlayerBtn.setCommand(new Runnable() {
                @Override public final void run() {
                    addPlayer();
                }
            });
            playersFrame.add(addPlayerBtn, "height 30px!, growx, pushx");
        }

        constructedFrame.add(playersFrame, "gapright 10px, w 50%-5px, growy, pushy");

        ////////////////////////////////////////////////////////
        ////////////////////// Deck Panel //////////////////////

        for (int i = 0; i < MAX_PLAYERS; i++) {
            buildDeckPanel(i);
        }
        constructedFrame.add(decksFrame, "w 50%-5px, growy, pushy");
        constructedFrame.setOpaque(false);
        decksFrame.setOpaque(false);

        // Start Button
        if (type != LobbyType.CLIENT) {
            pnlStart.setOpaque(false);
            pnlStart.add(btnStart, "align center");
        }
    }

    public void populate() {
        for (final FDeckChooser fdc : deckChoosers) {
            fdc.populate();
            fdc.getDecksComboBox().addListener(new IDecksComboBoxListener() {
                @Override
                public void deckTypeSelected(DecksComboBoxEvent ev) {
                    playerPanelWithFocus.focusOnAvatar();
                }
            });
        }
        populateDeckPanel(GameType.Constructed);
        populateVanguardLists();

    }

    private int addPlayerInFreeSlot(final String name) {
        if (activePlayersNum >= MAX_PLAYERS) {
            return -1;
        }

        for (final PlayerPanel pp : getPlayerPanels()) {
            if (pp.isVisible() && (
                    pp.getType() == LobbySlotType.OPEN || (pp.isLocal() && type == LobbyType.SERVER))) {
                final int index = pp.getIndex();
                addPlayer(index);
                pp.setPlayerName(name);
                System.out.println("Put player " + name + " in slot " + index);

                return index;
            }
        }
        return -1;
    }
    private void addPlayer() {
        if (activePlayersNum >= MAX_PLAYERS) {
            return;
        }

        int freeIndex = -1;
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (!playerPanels.get(i).isVisible()) {
                freeIndex = i;
                break;
            }
        }
        addPlayer(freeIndex);
    }
    private void addPlayer(final int slot) {
        playerPanels.get(slot).setVisible(true);

        activePlayersNum++;
        addPlayerBtn.setEnabled(activePlayersNum < MAX_PLAYERS);

        playerPanels.get(slot).setVisible(true);
        playerPanels.get(slot).focusOnAvatar();

        firePlayerChangeListener();
    }

    void removePlayer(final int playerIndex) {
        if (activePlayersNum <= playerIndex) {
            return;
        }
        activePlayersNum--;
        final FPanel player = playerPanels.get(playerIndex);
        player.setVisible(false);
        addPlayerBtn.setEnabled(true);

        //find closest player still in game and give focus
        int min = MAX_PLAYERS;
        final List<Integer> participants = getParticipants();
        if (!participants.isEmpty()) {
            int closest = 2;

            for (final int participantIndex : getParticipants()) {
                final int diff = Math.abs(playerIndex - participantIndex);

                if (diff < min) {
                    min = diff;
                    closest = participantIndex;
                }
            }

            changePlayerFocus(closest);
            playerPanels.get(closest).focusOnAvatar();
        }
        firePlayerChangeListener();
    }

    @Override
    public int login(final RemoteClient client) {
        return addPlayerInFreeSlot(client.getUsername());
    }

    @Override
    public void logout(final RemoteClient client) {
        removePlayer(client.getIndex());
    }

    @Override
    public LobbyState getState() {
        final LobbyState state = new LobbyState();
        for (int i = 0; i < activePlayersNum; i++) {
            state.addPlayer(getData(i));
        }
        return state;
    }

    public void setState(final LobbyState state) {
        setLocalPlayer(state.getLocalPlayer());

        final List<LobbyPlayerData> players = state.getPlayers();
        final int pSize = players.size();
        activePlayersNum = pSize;
        for (int i = 0; i < pSize; i++) {
            final LobbyPlayerData player = players.get(i);
            final PlayerPanel panel = playerPanels.get(i);

            if (type == LobbyType.CLIENT) {
                panel.setRemote(i != localPlayer);
                panel.setEditableForClient(i == localPlayer);
            } else {
                panel.setRemote(player.getType() == LobbySlotType.REMOTE);
                panel.setEditableForClient(false);
            }
            panel.setPlayerName(player.getName());
            panel.setAvatar(player.getAvatarIndex());
            panel.setVisible(true);
            panel.update();
        }
    }

    private void setLocalPlayer(final int index) {
        localPlayer = index;
    }

    public void setPlayerChangeListener(final IPlayerChangeListener listener) {
        this.playerChangeListener = listener;
    }

    void firePlayerChangeListener() {
        if (playerChangeListener != null) {
            playerChangeListener.update(getData(localPlayer));
        }
    }

    private LobbyPlayerData getData(final int index) {
        final PlayerPanel panel = playerPanels.get(index);
        return new LobbyPlayerData(panel.getPlayerName(), panel.getAvatarIndex(), panel.getType());
    }

    /** Builds the actual deck panel layouts for each player.
     * These are added to a list which can be referenced to populate the deck panel appropriately. */
    @SuppressWarnings("serial")
    private void buildDeckPanel(final int playerIndex) {
        String sectionConstraints = "insets 0, gap 0, wrap";
        String labelConstraints = "gaptop 10px, gapbottom 5px";

        // Main deck
        final FDeckChooser mainChooser = new FDeckChooser(null, isPlayerAI(playerIndex));
        mainChooser.initialize();
        mainChooser.getLstDecks().setSelectCommand(new UiCommand() {
            @Override
            public void run() {
                VLobby.this.onDeckClicked(playerIndex, mainChooser.getSelectedDeckType(), mainChooser.getLstDecks().getSelectedItems());
            }
        });
        deckChoosers.add(mainChooser);

        // Scheme deck list
        FPanel schemeDeckPanel = new FPanel();
        schemeDeckPanel.setBorderToggle(false);
        schemeDeckPanel.setLayout(new MigLayout(sectionConstraints));
        schemeDeckPanel.add(new FLabel.Builder().text("Select Scheme deck:").build(), labelConstraints);
        FList<Object> schemeDeckList = new FList<Object>();
        schemeDeckList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        FScrollPane scrSchemes = new FScrollPane(schemeDeckList, true,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        schemeDeckPanel.add(scrSchemes, "grow, push");
        schemeDeckLists.add(schemeDeckList);
        schemeDeckPanels.add(schemeDeckPanel);

        // Commander deck list
        FPanel commanderDeckPanel = new FPanel();
        commanderDeckPanel.setBorderToggle(false);
        commanderDeckPanel.setLayout(new MigLayout(sectionConstraints));
        commanderDeckPanel.add(new FLabel.Builder().text("Select Commander deck:").build(), labelConstraints);
        FList<Object> commanderDeckList = new FList<Object>();
        commanderDeckList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        FScrollPane scrCommander = new FScrollPane(commanderDeckList, true,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        commanderDeckPanel.add(scrCommander, "grow, push");
        commanderDeckLists.add(commanderDeckList);
        commanderDeckPanels.add(commanderDeckPanel);

        // Planar deck list
        FPanel planarDeckPanel = new FPanel();
        planarDeckPanel.setBorderToggle(false);
        planarDeckPanel.setLayout(new MigLayout(sectionConstraints));
        planarDeckPanel.add(new FLabel.Builder().text("Select Planar deck:").build(), labelConstraints);
        FList<Object> planarDeckList = new FList<Object>();
        planarDeckList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        FScrollPane scrPlanes = new FScrollPane(planarDeckList, true,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        planarDeckPanel.add(scrPlanes, "grow, push");
        planarDeckLists.add(planarDeckList);
        planarDeckPanels.add(planarDeckPanel);

        // Vanguard avatar list
        FPanel vgdDeckPanel = new FPanel();
        vgdDeckPanel.setBorderToggle(false);

        FList<Object> vgdAvatarList = new FList<Object>();
        vgdAvatarList.setListData(isPlayerAI(playerIndex) ? aiListData : humanListData);
        vgdAvatarList.setSelectedIndex(0);
        vgdAvatarList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        vgdAvatarList.addListSelectionListener(vgdLSListener);
        FScrollPane scrAvatars = new FScrollPane(vgdAvatarList, true,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        CardDetailPanel vgdDetail = new CardDetailPanel();
        vgdAvatarDetails.add(vgdDetail);

        vgdDeckPanel.setLayout(new MigLayout(sectionConstraints));
        vgdDeckPanel.add(new FLabel.Builder().text("Select a Vanguard avatar:").build(), labelConstraints);
        vgdDeckPanel.add(scrAvatars, "grow, push");
        vgdDeckPanel.add(vgdDetail, "h 200px, pushx, growx, hidemode 3");
        vgdAvatarLists.add(vgdAvatarList);
        vgdPanels.add(vgdDeckPanel);
    }

    protected void onDeckClicked(int iPlayer, DeckType type, Collection<DeckProxy> selectedDecks) {
        String text = type.toString() + ": " + Lang.joinHomogenous(selectedDecks, DeckProxy.FN_GET_NAME);
        playerPanels.get(iPlayer).setDeckSelectorButtonText(text);
    }

    /** Populates the deck panel with the focused player's deck choices. */
    private void populateDeckPanel(final GameType forGameType) {
        decksFrame.removeAll();

        if (playerPanelWithFocus.getType() == LobbySlotType.OPEN || playerPanelWithFocus.getType() == LobbySlotType.REMOTE) {
            return;
        }

        if (GameType.Constructed == forGameType) {
            decksFrame.add(deckChoosers.get(playerWithFocus), "grow, push");
            if (deckChoosers.get(playerWithFocus).getSelectedDeckType().toString().contains("Random")) {
                final String strCheckboxConstraints = "h 30px!, gap 0 20px 0 0";
                decksFrame.add(cbSingletons, strCheckboxConstraints);
                decksFrame.add(cbArtifacts, strCheckboxConstraints);
            }
        } else if (GameType.Archenemy == forGameType || GameType.ArchenemyRumble == forGameType) {
            if (isPlayerArchenemy(playerWithFocus)) {
                decksFrame.add(schemeDeckPanels.get(playerWithFocus), "grow, push");
            } else {
                populateDeckPanel(GameType.Constructed);
            }
        } else if (GameType.Commander == forGameType || GameType.TinyLeaders == forGameType) {
            decksFrame.add(commanderDeckPanels.get(playerWithFocus), "grow, push");
        } else if (GameType.Planechase == forGameType) {
            decksFrame.add(planarDeckPanels.get(playerWithFocus), "grow, push");
        } else if (GameType.Vanguard == forGameType) {
            updateVanguardList(playerWithFocus);
            decksFrame.add(vgdPanels.get(playerWithFocus), "grow, push");
        }
        refreshPanels(false, true);
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return this.btnStart;
    }

    public LblHeader getLblTitle() { return lblTitle; }
    public JPanel getConstructedFrame() { return constructedFrame; }
    public JPanel getPanelStart() { return pnlStart; }
    public List<FDeckChooser> getDeckChoosers() { return Collections.unmodifiableList(deckChoosers); }

    /** Gets the random deck checkbox for Singletons. */
    public FCheckBox getCbSingletons() { return cbSingletons; }

    /** Gets the random deck checkbox for Artifacts. */
    public FCheckBox getCbArtifacts() { return cbArtifacts; }

    public FCheckBox getVntArchenemy()       { return vntArchenemy; }
    public FCheckBox getVntArchenemyRumble() { return vntArchenemyRumble; }
    public FCheckBox getVntCommander()       { return vntCommander; }
    public FCheckBox getVntMomirBasic()      { return vntMomirBasic; }
    public FCheckBox getVntPlanechase()      { return vntPlanechase; }
    public FCheckBox getVntTinyLeaders()     { return vntTinyLeaders; }
    public FCheckBox getVntVanguard()        { return vntVanguard; }

    public int getLastArchenemy() { return lastArchenemy; }
    public void setLastArchenemy(final int archenemy) { lastArchenemy = archenemy; }

    public final List<PlayerPanel> getPlayerPanels() {
        return playerPanels;
    }
    public final PlayerPanel getPlayerPanelWithFocus() {
        return playerPanelWithFocus;
    }

    public final FDeckChooser getDeckChooser(int playernum) {
        return deckChoosers.get(playernum);
    }

    public List<Integer> getTeams() { return teams; }
    public List<Integer> getArchenemyTeams() { return archenemyTeams; }
    public GameType getCurrentGameMode() { return currentGameMode; }
    public void setCurrentGameMode(final GameType mode) { currentGameMode = mode; }

    public boolean isPlayerAI(int playernum) {
        return playerPanels.get(playernum).isAi();
    }

    public Map<String, String> getAiOptions(int playernum) {
        if (playerPanels.get(playernum).isSimulatedAi()) {
            Map<String, String> options = new HashMap<String, String>();
            options.put("UseSimulation", "True");
            return options;
        }
        return null;
    }

    public int getNumPlayers() {
        return activePlayersNum;
    }

    public final List<Integer> getParticipants() {
        final List<Integer> participants = new ArrayList<Integer>(activePlayersNum);
        for (final PlayerPanel panel : playerPanels) {
            if (panel.isVisible()) {
                participants.add(playerPanels.indexOf(panel));
            }
        }
        return participants;
    }

    /** Revalidates the player and deck sections. Necessary after adding or hiding any panels. */
    private void refreshPanels(boolean refreshPlayerFrame, boolean refreshDeckFrame) {
        if (refreshPlayerFrame) {
            playersScroll.validate();
            playersScroll.repaint();
        }
        if (refreshDeckFrame) {
            decksFrame.validate();
            decksFrame.repaint();
        }
    }

    public void changePlayerFocus(int newFocusOwner) {
        changePlayerFocus(newFocusOwner, appliedVariants.contains(currentGameMode) ? currentGameMode : GameType.Constructed);
    }

    void changePlayerFocus(int newFocusOwner, GameType gType) {
        playerPanelWithFocus.setFocused(false);
        playerWithFocus = newFocusOwner;
        playerPanelWithFocus = playerPanels.get(playerWithFocus);
        playerPanelWithFocus.setFocused(true);

        playersScroll.getViewport().scrollRectToVisible(playerPanelWithFocus.getBounds());
        populateDeckPanel(gType);

        refreshPanels(true, true);
    }

    /** Saves avatar prefs for players one and two. */
    void updateAvatarPrefs() {
        int pOneIndex = playerPanels.get(0).getAvatarIndex();
        int pTwoIndex = playerPanels.get(1).getAvatarIndex();

        prefs.setPref(FPref.UI_AVATARS, pOneIndex + "," + pTwoIndex);
        prefs.save();
    }

    /** Updates the avatars from preferences on update. */
    public void updatePlayersFromPrefs() {
        ForgePreferences prefs = FModel.getPreferences();

        // Avatar
        String[] avatarPrefs = prefs.getPref(FPref.UI_AVATARS).split(",");
        for (int i = 0; i < avatarPrefs.length; i++) {
            int avatarIndex = Integer.parseInt(avatarPrefs[i]);
            playerPanels.get(i).setAvatar(avatarIndex);
        }

        // Name
        String prefName = prefs.getPref(FPref.PLAYER_NAME);
        playerPanels.get(0).setPlayerName(StringUtils.isBlank(prefName) ? "Human" : prefName);
    }

    /** Adds a pre-styled FLabel component with the specified title. */
    FLabel newLabel(String title) {
        return new FLabel.Builder().text(title).fontSize(14).fontStyle(Font.ITALIC).build();
    }

    List<Integer> getUsedAvatars() {
        List<Integer> usedAvatars = Arrays.asList(-1,-1,-1,-1,-1,-1,-1,-1);
        int i = 0;
        for (PlayerPanel pp : playerPanels) {
            usedAvatars.set(i++, pp.getAvatarIndex());
        }
        return usedAvatars;
    }

    final String getNewName() {
        final String title = "Get new random name";
        final String message = "What type of name do you want to generate?";
        final SkinImage icon = FOptionPane.QUESTION_ICON;
        final String[] genderOptions = new String[]{ "Male", "Female", "Any" };
        final String[] typeOptions = new String[]{ "Fantasy", "Generic", "Any" };

        final int genderIndex = FOptionPane.showOptionDialog(message, title, icon, genderOptions, 2);
        if (genderIndex < 0) {
            return null;
        }
        final int typeIndex = FOptionPane.showOptionDialog(message, title, icon, typeOptions, 2);
        if (typeIndex < 0) {
            return null;
        }

        final String gender = genderOptions[genderIndex];
        final String type = typeOptions[typeIndex];

        String confirmMsg, newName;
        List<String> usedNames = getPlayerNames();
        do {
            newName = NameGenerator.getRandomName(gender, type, usedNames);
            confirmMsg = "Would you like to use the name \"" + newName + "\", or try again?";
        } while (!FOptionPane.showConfirmDialog(confirmMsg, title, "Use this name", "Try again", true));

        return newName;
    }

    List<String> getPlayerNames() {
        List<String> names = new ArrayList<String>();
        for (PlayerPanel pp : playerPanels) {
            names.add(pp.getPlayerName());
        }
        return names;
    }

    public String getPlayerName(int i) {
        return playerPanels.get(i).getPlayerName();
    }

    public int getPlayerAvatar(int i) {
        return playerPanels.get(i).getAvatarIndex();
    }

    public boolean isEnoughTeams() {
        int lastTeam = -1;
        final List<Integer> teamList = appliedVariants.contains(GameType.Archenemy) ? archenemyTeams : teams;
                
        for (final int i : getParticipants()) {
            if (lastTeam == -1) {
                lastTeam = teamList.get(i);
            } else if (lastTeam != teamList.get(i)) {
                return true;
            }
        }
        return false;
    }

    /////////////////////////////////////////////
    //========== Various listeners in build order

    @SuppressWarnings("serial") private class VariantCheckBox extends FCheckBox {
        private final GameType variantType;

        private VariantCheckBox(GameType variantType0) {
            super(variantType0.toString());

            variantType = variantType0;

            setToolTipText(variantType.getDescription());

            addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        appliedVariants.add(variantType);
                        currentGameMode = variantType;

                        //ensure other necessary variants are unchecked
                        switch (variantType) {
                        case Archenemy:
                            vntArchenemyRumble.setSelected(false);
                            break;
                        case ArchenemyRumble:
                            vntArchenemy.setSelected(false);
                            break;
                        case Commander:
                            vntTinyLeaders.setSelected(false);
                            vntMomirBasic.setSelected(false);
                            break;
                        case TinyLeaders:
                            vntCommander.setSelected(false);
                            vntMomirBasic.setSelected(false);
                            break;
                        case Vanguard:
                            vntMomirBasic.setSelected(false);
                            break;
                        case MomirBasic:
                            vntCommander.setSelected(false);
                            vntVanguard.setSelected(false);
                            break;
                        default:
                            break;
                        }
                    }
                    else {
                        appliedVariants.remove(variantType);
                        if (currentGameMode == variantType) {
                            currentGameMode = GameType.Constructed;
                        }
                    }

                    for (PlayerPanel pp : playerPanels) {
                        pp.toggleIsPlayerArchenemy();
                    }
                    changePlayerFocus(playerWithFocus, currentGameMode);
                }
            });
        }
    }

    ActionListener nameListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            FTextField nField = (FTextField)e.getSource();
            nField.transferFocus();
        }
    };

    /** This listener will look for a vanguard avatar being selected in the lists
    / and update the corresponding detail panel. */
    private ListSelectionListener vgdLSListener = new ListSelectionListener() {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            int index = vgdAvatarLists.indexOf(e.getSource());
            Object obj = vgdAvatarLists.get(index).getSelectedValue();
            PlayerPanel pp = playerPanels.get(index);
            CardDetailPanel cdp = vgdAvatarDetails.get(index);

            if (obj instanceof PaperCard) {
                pp.setVanguardButtonText(((PaperCard) obj).getName());
                cdp.setCard(CardView.getCardForUi((PaperCard) obj));
                cdp.setVisible(true);
                refreshPanels(false, true);
            }
            else {
                pp.setVanguardButtonText((String) obj);
                cdp.setVisible(false);
            }
        }
    };


    /////////////////////////////////////
    //========== METHODS FOR VARIANTS

    public Set<GameType> getAppliedVariants() {
        return Collections.unmodifiableSet(appliedVariants);
    }

    public int getTeam(final int playerIndex) {
        return appliedVariants.contains(GameType.Archenemy) ? archenemyTeams.get(playerIndex) : teams.get(playerIndex);
    }

    /** Gets the list of planar deck lists. */
    public List<FList<Object>> getPlanarDeckLists() {
        return planarDeckLists;
    }

    /** Gets the list of commander deck lists. */
    public List<FList<Object>> getCommanderDeckLists() {
        return commanderDeckLists;
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
            for (PaperCard c : FModel.getMagicDb().getVariantCards().getAllCards()) {
                if (c.getRules().getType().isVanguard()) {
                    vgdAllAvatars.add(c);
                }
            }
        }
        return vgdAllAvatars;
    }

    /** Return the Vanguard avatars not flagged RemAIDeck. */
    public List<PaperCard> getAllAiAvatars() {
        return vgdAllAiAvatars;
    }

    /** Return the Vanguard avatars not flagged RemRandomDeck. */
    public List<PaperCard> getNonRandomHumanAvatars() {
        return nonRandomHumanAvatars;
    }

    /** Return the Vanguard avatars not flagged RemAIDeck or RemRandomDeck. */
    public List<PaperCard> getNonRandomAiAvatars() {
        return nonRandomAiAvatars;
    }

    /** Populate vanguard lists. */
    private void populateVanguardLists() {
        humanListData.add("Use deck's default avatar (random if unavailable)");
        humanListData.add("Random");
        aiListData.add("Use deck's default avatar (random if unavailable)");
        aiListData.add("Random");
        for (PaperCard cp : getAllAvatars()) {
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
    public void updateVanguardList(int playerIndex) {
        FList<Object> vgdList = getVanguardLists().get(playerIndex);
        Object lastSelection = vgdList.getSelectedValue();
        vgdList.setListData(isPlayerAI(playerIndex) ? aiListData : humanListData);
        if (null != lastSelection) {
            vgdList.setSelectedValue(lastSelection, true);
        }

        if (-1 == vgdList.getSelectedIndex()) {
            vgdList.setSelectedIndex(0);
        }
    }
}
