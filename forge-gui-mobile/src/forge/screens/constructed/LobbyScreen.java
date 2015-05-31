package forge.screens.constructed;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.AIOption;
import forge.FThreads;
import forge.Forge;
import forge.Graphics;
import forge.LobbyPlayer;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckProxy;
import forge.deck.DeckSection;
import forge.deck.DeckType;
import forge.deck.FDeckChooser;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.interfaces.ILobbyView;
import forge.interfaces.IPlayerChangeListener;
import forge.item.PaperCard;
import forge.match.GameLobby;
import forge.match.LobbySlot;
import forge.match.LobbySlotType;
import forge.menu.FPopupMenu;
import forge.model.FModel;
import forge.net.event.UpdateLobbyPlayerEvent;
import forge.player.GamePlayerUtil;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.FScreen;
import forge.screens.LaunchScreen;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FEvent;
import forge.toolbox.FList;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.util.Lang;
import forge.util.Utils;
import forge.util.gui.SOptionPane;

public abstract class LobbyScreen extends LaunchScreen implements ILobbyView {
    private static final ForgePreferences prefs = FModel.getPreferences();
    private static final float PADDING = Utils.scale(5);
    public static final int MAX_PLAYERS = 2; //8; //TODO: Support multiplayer
    private static final FSkinFont VARIANTS_FONT = FSkinFont.get(12);

    // General variables
    private GameLobby lobby;
    private IPlayerChangeListener playerChangeListener = null;
    private final FLabel lblPlayers = new FLabel.Builder().text("Players:").font(VARIANTS_FONT).build();
    private final FComboBox<Integer> cbPlayerCount;
    private final Deck[] decks = new Deck[MAX_PLAYERS];

    // Variants frame and variables
    private final FLabel lblVariants = new FLabel.Builder().text("Variants:").font(VARIANTS_FONT).build();
    private final FComboBox<Object> cbVariants;
    private final Set<GameType> appliedVariants = new TreeSet<GameType>();

    private final List<PlayerPanel> playerPanels = new ArrayList<PlayerPanel>(MAX_PLAYERS);
    private final FScrollPane playersScroll = new FScrollPane() {
        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float y = 0;
            float height;
            for (int i = 0; i < getNumPlayers(); i++) {
                height = playerPanels.get(i).getPreferredHeight();
                playerPanels.get(i).setBounds(0, y, visibleWidth, height);
                y += height;
            }
            return new ScrollBounds(visibleWidth, y);
        }

        @Override
        public void drawOnContainer(Graphics g) {
            //draw top border above items
            float y = playersScroll.getTop() - FList.LINE_THICKNESS / 2;
            g.drawLine(FList.LINE_THICKNESS, FList.LINE_COLOR, 0, y, getWidth(), y);
        }
    };

    int lastArchenemy = 0;

    public LobbyScreen(String headerCaption, FPopupMenu menu, GameLobby lobby0) {
        super(headerCaption, menu);

        initLobby(lobby0);

        btnStart.setEnabled(false); //disable start button until decks loaded

        add(lblPlayers);
        cbPlayerCount = add(new FComboBox<Integer>());
        cbPlayerCount.setFont(VARIANTS_FONT);
        for (int i = 2; i <= MAX_PLAYERS; i++) {
            cbPlayerCount.addItem(i);
        }
        cbPlayerCount.setSelectedItem(2);
        cbPlayerCount.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                int numPlayers = getNumPlayers();
                for (int i = 0; i < MAX_PLAYERS; i++) {
                    playerPanels.get(i).setVisible(i < numPlayers);
                }
                playersScroll.revalidate();
            }
        });

        add(lblVariants);
        cbVariants = add(new FComboBox<Object>());
        cbVariants.setFont(VARIANTS_FONT);
        cbVariants.addItem("(None)");
        cbVariants.addItem(GameType.Vanguard);
        cbVariants.addItem(GameType.MomirBasic);
        cbVariants.addItem(GameType.Commander);
        cbVariants.addItem(GameType.Planechase);
        cbVariants.addItem(GameType.Archenemy);
        cbVariants.addItem(GameType.ArchenemyRumble);
        cbVariants.addItem("More....");
        cbVariants.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (cbVariants.getSelectedIndex() <= 0) {
                    appliedVariants.clear();
                    updateLayoutForVariants();
                }
                else if (cbVariants.getSelectedIndex() == cbVariants.getItemCount() - 1) {
                    Forge.openScreen(new MultiVariantSelect());
                    updateVariantSelection();
                }
                else {
                    appliedVariants.clear();
                    appliedVariants.add((GameType)cbVariants.getSelectedItem());
                    updateLayoutForVariants();
                }
            }
        });

        update(false);

        add(playersScroll);

        updatePlayersFromPrefs();

        FThreads.invokeInBackgroundThread(new Runnable() {
            @Override
            public void run() {
                playerPanels.get(0).initialize(FPref.CONSTRUCTED_P1_DECK_STATE, DeckType.PRECONSTRUCTED_DECK);
                playerPanels.get(1).initialize(FPref.CONSTRUCTED_P2_DECK_STATE, DeckType.COLOR_DECK);
                /*playerPanels.get(2).initialize(FPref.CONSTRUCTED_P3_DECK_STATE, DeckType.COLOR_DECK);
                playerPanels.get(3).initialize(FPref.CONSTRUCTED_P4_DECK_STATE, DeckType.COLOR_DECK);
                playerPanels.get(4).initialize(FPref.CONSTRUCTED_P5_DECK_STATE, DeckType.COLOR_DECK);
                playerPanels.get(5).initialize(FPref.CONSTRUCTED_P6_DECK_STATE, DeckType.COLOR_DECK);
                playerPanels.get(6).initialize(FPref.CONSTRUCTED_P7_DECK_STATE, DeckType.COLOR_DECK);
                playerPanels.get(7).initialize(FPref.CONSTRUCTED_P8_DECK_STATE, DeckType.COLOR_DECK);*/ //TODO: Support multiplayer and improve performance of loading this screen by using background thread

                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        btnStart.setEnabled(true);
                    }
                });
            }
        });

        //disable player count and variants for now until they work properly
        lblPlayers.setEnabled(false);
        cbPlayerCount.setEnabled(false);
    }

    public GameLobby getLobby() {
        return lobby;
    }
    protected void initLobby(GameLobby lobby0) {
        lobby = lobby0;
        if (lobby == null) { return; }

        lobby.setListener(this);
    }

    private void updateVariantSelection() {
        if (appliedVariants.isEmpty()) {
            cbVariants.setSelectedIndex(0);
        }
        else if (appliedVariants.size() == 1) {
            cbVariants.setSelectedItem(appliedVariants.iterator().next());
        }
        else {
            String text = "";
            for (GameType variantType : appliedVariants) {
                if (text.length() > 0) {
                    text += ", ";
                }
                text += variantType.toString();
            }
            cbVariants.setText(text);
        }
    }

    void updateLayoutForVariants() {
        for (int i = 0; i < MAX_PLAYERS; i++) {
            playerPanels.get(i).updateVariantControlsVisibility();
        }
        playersScroll.revalidate();
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING;
        float fieldHeight = cbPlayerCount.getHeight();
        lblPlayers.setBounds(x, y, lblPlayers.getAutoSizeBounds().width + PADDING / 2, fieldHeight);
        x += lblPlayers.getWidth();
        cbPlayerCount.setBounds(x, y, Utils.AVG_FINGER_WIDTH, fieldHeight);
        x += cbPlayerCount.getWidth() + PADDING;
        lblVariants.setBounds(x, y, lblVariants.getAutoSizeBounds().width + PADDING / 2, fieldHeight);
        x += lblVariants.getWidth();
        cbVariants.setBounds(x, y, width - x - PADDING, fieldHeight);

        y += cbPlayerCount.getHeight() + PADDING;
        playersScroll.setBounds(0, y, width, height - y);
    }

    public final FDeckChooser getDeckChooser(int playernum) {
        return playerPanels.get(playernum).getDeckChooser();
    }

    public int getNumPlayers() {
        return cbPlayerCount.getSelectedItem();
    }
    public void setNumPlayers(int numPlayers) {
        cbPlayerCount.setSelectedItem(numPlayers);
    }

    @Override
    protected boolean buildLaunchParams(LaunchParams launchParams) {
        launchParams.gameType = GameType.Constructed;

        if (!isEnoughTeams()) {
            FOptionPane.showMessageDialog("There are not enough teams! Please adjust team allocations.");
            return false;
        }

        for (int i = 0; i < getNumPlayers(); i++) {
            if (getDeckChooser(i).getPlayer(true) == null) {
                FOptionPane.showMessageDialog("Please specify a deck for " + getPlayerName(i));
                return false;
            }
        } // Is it even possible anymore? I think current implementation assigns decks automatically.

        GameType autoGenerateVariant = null;
        boolean isCommanderMatch = false;
        if (!appliedVariants.isEmpty()) {
            launchParams.appliedVariants.addAll(appliedVariants);

            isCommanderMatch = appliedVariants.contains(GameType.Commander);
            if (!isCommanderMatch) {
                for (GameType variant : appliedVariants) {
                    if (variant.isAutoGenerated()) {
                        autoGenerateVariant = variant;
                        break;
                    }
                }
            }
        }

        boolean checkLegality = FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY);

        //Auto-generated decks don't need to be checked here
        //Commander deck replaces regular deck and is checked later
        if (checkLegality && autoGenerateVariant == null && !isCommanderMatch) { 
            for (int i = 0; i < getNumPlayers(); i++) {
                String name = getPlayerName(i);
                String errMsg = GameType.Constructed.getDeckFormat().getDeckConformanceProblem(getDeckChooser(i).getPlayer(false).getDeck());
                if (errMsg != null) {
                    FOptionPane.showErrorDialog(name + "'s deck " + errMsg, "Invalid Deck");
                    return false;
                }
            }
        }

        int playerCount = getNumPlayers();
        for (int i = 0; i < playerCount; i++) {
            PlayerPanel playerPanel = playerPanels.get(i);
            String name = getPlayerName(i);
            LobbyPlayer lobbyPlayer = playerPanel.isAi() ? GamePlayerUtil.createAiPlayer(name,
                    getPlayerAvatar(i)) : GamePlayerUtil.getGuiPlayer(name, playerPanel.getAvatarIndex(), i == 0);
            RegisteredPlayer rp = playerPanel.getDeckChooser().getPlayer(false);

            if (appliedVariants.isEmpty()) {
                rp.setTeamNumber(playerPanel.getTeam());
                launchParams.players.add(rp.setPlayer(lobbyPlayer));
            }
            else {
                Deck deck = null;
                PaperCard vanguardAvatar = null;
                if (isCommanderMatch) {
                    deck = playerPanel.getCommanderDeck();
                    if (checkLegality) {
                        String errMsg = GameType.Commander.getDeckFormat().getDeckConformanceProblem(deck);
                        if (errMsg != null) {
                            FOptionPane.showErrorDialog(name + "'s deck " + errMsg, "Invalid Commander Deck");
                            return false;
                        }
                    }
                }
                else if (autoGenerateVariant != null) {
                    deck = autoGenerateVariant.autoGenerateDeck(rp);
                    CardPool avatarPool = deck.get(DeckSection.Avatar);
                    if (avatarPool != null) {
                        vanguardAvatar = avatarPool.get(0);
                    }
                }

                // Initialize variables for other variants
                deck = deck == null ? rp.getDeck() : deck;
                Iterable<PaperCard> schemes = null;
                boolean playerIsArchenemy = isPlayerArchenemy(i);
                Iterable<PaperCard> planes = null;

                //Archenemy
                if (appliedVariants.contains(GameType.ArchenemyRumble)
                        || (appliedVariants.contains(GameType.Archenemy) && playerIsArchenemy)) {
                    Deck schemeDeck = playerPanel.getSchemeDeck();
                    CardPool schemePool = schemeDeck.get(DeckSection.Schemes);
                    if (checkLegality) {
                        String errMsg = DeckFormat.getSchemeSectionConformanceProblem(schemePool);
                        if (errMsg != null) {
                            FOptionPane.showErrorDialog(name + "'s deck " + errMsg, "Invalid Scheme Deck");
                            return false;
                        }
                    }
                    schemes = schemePool.toFlatList();
                }

                //Planechase
                if (appliedVariants.contains(GameType.Planechase)) {
                    Deck planarDeck = playerPanel.getPlanarDeck();
                    CardPool planePool = planarDeck.get(DeckSection.Planes);
                    if (checkLegality) {
                        String errMsg = DeckFormat.getPlaneSectionConformanceProblem(planePool);
                        if (null != errMsg) {
                            FOptionPane.showErrorDialog(name + "'s deck " + errMsg, "Invalid Planar Deck");
                            return false;
                        }
                    }
                    planes = planePool.toFlatList();
                }

                //Vanguard
                if (appliedVariants.contains(GameType.Vanguard)) {
                    vanguardAvatar = playerPanel.getVanguardAvatar();
                    if (vanguardAvatar == null) {
                        FOptionPane.showErrorDialog("No Vanguard avatar selected for " + name
                                + ". Please choose one or disable the Vanguard variant");
                        return false;
                    }
                }

                rp = RegisteredPlayer.forVariants(playerCount, appliedVariants, deck, schemes, playerIsArchenemy, planes, vanguardAvatar);
                rp.setTeamNumber(playerPanel.getTeam());
                launchParams.players.add(rp.setPlayer(lobbyPlayer));
            }
            if (!playerPanel.isAi()) {
                launchParams.humanPlayers.add(rp);
            }
            getDeckChooser(i).saveState();
        }

        return true;
    }

    /** Saves avatar prefs for players one and two. */
    void updateAvatarPrefs() {
        int pOneIndex = playerPanels.get(0).getAvatarIndex();
        int pTwoIndex = playerPanels.get(1).getAvatarIndex();

        prefs.setPref(FPref.UI_AVATARS, pOneIndex + "," + pTwoIndex);
        prefs.save();
    }

    /** Updates the avatars from preferences on update. */
    private void updatePlayersFromPrefs() {
        ForgePreferences prefs = FModel.getPreferences();

        // Avatar
        String[] avatarPrefs = prefs.getPref(FPref.UI_AVATARS).split(",");
        for (int i = 0; i < avatarPrefs.length; i++) {
            int avatarIndex = Integer.parseInt(avatarPrefs[i]);
            playerPanels.get(i).setAvatarIndex(avatarIndex);
        }

        // Name
        String prefName = prefs.getPref(FPref.PLAYER_NAME);
        playerPanels.get(0).setPlayerName(StringUtils.isBlank(prefName) ? "Human" : prefName);
    }

    List<Integer> getUsedAvatars() {
        List<Integer> usedAvatars = Arrays.asList(-1,-1,-1,-1,-1,-1,-1,-1);
        int i = 0;
        for (PlayerPanel pp : playerPanels) {
            usedAvatars.set(i++, pp.getAvatarIndex());
        }
        return usedAvatars;
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
        boolean isArchenemy = hasVariant(GameType.Archenemy);
        int team;

        for (int i = 0; i < getNumPlayers(); i++) {
            PlayerPanel panel = playerPanels.get(i);
            team = isArchenemy ? panel.getArchenemyTeam() : panel.getTeam();
            if (lastTeam == -1) {
                lastTeam = team;
            }
            else if (lastTeam != team) {
                return true;
            }
        }
        return false;
    }

    /////////////////////////////////////////////
    //========== Various listeners in build order
    
    private class MultiVariantSelect extends FScreen {
        private final FList<Variant> lstVariants = add(new FList<Variant>());

        private MultiVariantSelect() {
            super("Select Variants");

            lstVariants.setListItemRenderer(new VariantRenderer());
            lstVariants.addItem(new Variant(GameType.Vanguard));
            lstVariants.addItem(new Variant(GameType.MomirBasic));
            lstVariants.addItem(new Variant(GameType.Commander));
            lstVariants.addItem(new Variant(GameType.Planechase));
            lstVariants.addItem(new Variant(GameType.Archenemy));
            lstVariants.addItem(new Variant(GameType.ArchenemyRumble));
        }

        @Override
        protected void doLayout(float startY, float width, float height) {
            lstVariants.setBounds(0, startY, width, height - startY);
        }

        private class Variant {
            private final GameType gameType;

            private Variant(GameType gameType0) {
                gameType = gameType0;
            }

            private void draw(Graphics g, FSkinFont font, FSkinColor color, float x, float y, float w, float h) {
                x += w - h;
                w = h;
                FCheckBox.drawCheckBox(g, SettingsScreen.DESC_COLOR, color, appliedVariants.contains(gameType), x, y, w, h);
            }

            private void toggle() {
                if (appliedVariants.contains(gameType)) {
                    appliedVariants.remove(gameType);
                }
                else {
                    appliedVariants.add(gameType);

                    //only allow setting one of Archenemy or ArchenemyRumble
                    //don't allow setting MomirBasic along with Vanguard or Commander 
                    switch (gameType) {
                    case Archenemy:
                        appliedVariants.remove(GameType.ArchenemyRumble);
                        break;
                    case ArchenemyRumble:
                        appliedVariants.remove(GameType.Archenemy);
                        break;
                    case Commander:
                        appliedVariants.remove(GameType.MomirBasic);
                        break;
                    case Vanguard:
                        appliedVariants.remove(GameType.MomirBasic);
                        break;
                    case MomirBasic:
                        appliedVariants.remove(GameType.Commander);
                        appliedVariants.remove(GameType.Vanguard);
                        break;
                    default:
                        break;
                    }
                }
                updateVariantSelection();
                updateLayoutForVariants();
            }
        }

        private class VariantRenderer extends FList.ListItemRenderer<Variant> {
            @Override
            public float getItemHeight() {
                return SettingsScreen.SETTING_HEIGHT;
            }

            @Override
            public boolean tap(Integer index, Variant value, float x, float y, int count) {
                value.toggle();
                return true;
            }

            @Override
            public void drawValue(Graphics g, Integer index, Variant value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
                float offset = SettingsScreen.getInsets(w) - FList.PADDING;
                x += offset;
                y += offset;
                w -= 2 * offset;
                h -= 2 * offset;

                String text = value.gameType.toString();
                float totalHeight = h;
                h = font.getMultiLineBounds(text).height + SettingsScreen.SETTING_PADDING;

                g.drawText(text, font, foreColor, x, y, w, h, false, HAlignment.LEFT, false);
                value.draw(g, font, foreColor, x, y, w, h);
                h += SettingsScreen.SETTING_PADDING;
                g.drawText(value.gameType.getDescription(), SettingsScreen.DESC_FONT, SettingsScreen.DESC_COLOR, x, y + h, w, totalHeight - h + SettingsScreen.getInsets(w), true, HAlignment.LEFT, false);            
            }
        }
    }

    /////////////////////////////////////
    //========== METHODS FOR VARIANTS

    public Set<GameType> getAppliedVariants() {
        return appliedVariants;
    }

    public boolean isPlayerAI(final int playernum) {
        return playerPanels.get(playernum).getType() == LobbySlotType.AI;
    }

    public boolean isPlayerArchenemy(final int playernum) {
        return playerPanels.get(playernum).isArchenemy();
    }

    @Override
    public void update(final boolean fullUpdate) {
        int playerCount = lobby.getNumberOfSlots();
        cbPlayerCount.setSelectedItem(playerCount);

        updateVariantSelection();

        final boolean allowNetworking = lobby.isAllowNetworking();
        for (int i = 0; i < MAX_PLAYERS; i++) {
            final boolean hasPanel = i < playerPanels.size();
            if (i < playerCount) {
                // visible panels
                final LobbySlot slot = lobby.getSlot(i);
                final PlayerPanel panel;
                final boolean isNewPanel;
                if (hasPanel) {
                    panel = playerPanels.get(i);
                    isNewPanel = !panel.isVisible();
                }
                else {
                    panel = new PlayerPanel(this, allowNetworking, i, slot, lobby.mayEdit(i), lobby.hasControl());
                    playerPanels.add(panel);
                    playersScroll.add(panel);
                    isNewPanel = true;
                }

                final LobbySlotType type = slot.getType();
                panel.setType(type);
                panel.setPlayerName(slot.getName());
                panel.setAvatarIndex(slot.getAvatarIndex());
                panel.setTeam(slot.getTeam());
                panel.setIsReady(slot.isReady());
                panel.setIsArchenemy(slot.isArchenemy());
                panel.setUseAiSimulation(slot.getAiOptions().contains(AIOption.USE_SIMULATION));
                panel.setMayEdit(lobby.mayEdit(i));
                panel.setMayControl(lobby.mayControl(i));
                panel.setMayRemove(lobby.mayRemove(i));

                if (fullUpdate && (type == LobbySlotType.LOCAL || type == LobbySlotType.AI)) {
                    selectDeck(i);
                }
                if (isNewPanel) {
                    panel.setVisible(true);
                }
            }
            else if (hasPanel) {
                playerPanels.get(i).setVisible(false);
            }
        }
    }

    @Override
    public void setPlayerChangeListener(IPlayerChangeListener playerChangeListener0) {
        playerChangeListener = playerChangeListener0;
    }
    
    private void selectDeck(final int playerIndex) {
        // Full deck selection
        selectMainDeck(playerIndex);
        selectCommanderDeck(playerIndex);

        // Deck section selection
        selectSchemeDeck(playerIndex);
        selectPlanarDeck(playerIndex);
        selectVanguardAvatar(playerIndex);
    }

    private void selectMainDeck(final int playerIndex) {
        if (hasVariant(GameType.Commander) || hasVariant(GameType.TinyLeaders)) {
            // These game types use specific deck panel
            return;
        }
        final FDeckChooser mainChooser = getDeckChooser(playerIndex);
        onDeckClicked(playerIndex, mainChooser.getSelectedDeckType(), mainChooser.getDeck(), mainChooser.getLstDecks().getSelectedItems());
        getDeckChooser(playerIndex).saveState();
    }

    private void selectSchemeDeck(final int playerIndex) {
        if (playerIndex >= getNumPlayers() || !(hasVariant(GameType.Archenemy) || hasVariant(GameType.ArchenemyRumble))) {
            return;
        }

        CardPool schemePool = playerPanels.get(playerIndex).getSchemeDeck().get(DeckSection.Schemes);
        fireDeckSectionChangeListener(playerIndex, DeckSection.Schemes, schemePool);
        getDeckChooser(playerIndex).saveState();
    }

    private void selectCommanderDeck(final int playerIndex) {
        if (playerIndex >= getNumPlayers() || !(hasVariant(GameType.Commander) || hasVariant(GameType.TinyLeaders))) {
            return;
        }

        Deck deck = playerPanels.get(playerIndex).getCommanderDeck();
        fireDeckChangeListener(playerIndex, deck);
        getDeckChooser(playerIndex).saveState();
    }

    private void selectPlanarDeck(final int playerIndex) {
        if (playerIndex >= getNumPlayers() || !hasVariant(GameType.Planechase)) {
            return;
        }

        CardPool planePool = playerPanels.get(playerIndex).getPlanarDeck().get(DeckSection.Planes);
        fireDeckSectionChangeListener(playerIndex, DeckSection.Planes, planePool);
        getDeckChooser(playerIndex).saveState();
    }

    private void selectVanguardAvatar(final int playerIndex) {
        if (playerIndex >= getNumPlayers() || !hasVariant(GameType.Vanguard)) {
            return;
        }

        final CardPool avatarOnce = new CardPool();
        avatarOnce.add(playerPanels.get(playerIndex).getVanguardAvatar());
        fireDeckSectionChangeListener(playerIndex, DeckSection.Avatar, avatarOnce);
        getDeckChooser(playerIndex).saveState();
    }

    protected void onDeckClicked(final int iPlayer, final DeckType type, final Deck deck, final Collection<DeckProxy> selectedDecks) {
        if (iPlayer < getNumPlayers() && lobby.mayEdit(iPlayer)) {
            final String text = type.toString() + ": " + Lang.joinHomogenous(selectedDecks, DeckProxy.FN_GET_NAME);
            playerPanels.get(iPlayer).setDeckSelectorButtonText(text);
            fireDeckChangeListener(iPlayer, deck);
        }
    }

    void setReady(final int index, final boolean ready) {
        if (ready && decks[index] == null) {
            SOptionPane.showErrorDialog("Select a deck before readying!");
            update(false);
            return;
        }

        firePlayerChangeListener(index);
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
        return UpdateLobbyPlayerEvent.create(panel.getType(), panel.getPlayerName(), panel.getAvatarIndex(), panel.getTeam(), panel.isArchenemy(), panel.isReady(), panel.getAiOptions());
    }

    public List<PlayerPanel> getPlayerPanels() {
        return playerPanels;
    }
}
