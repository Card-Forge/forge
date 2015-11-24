package forge.screens.constructed;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.google.common.collect.Iterables;

import forge.AIOption;
import forge.FThreads;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.DeckType;
import forge.game.GameType;
import forge.interfaces.ILobbyView;
import forge.interfaces.IPlayerChangeListener;
import forge.match.GameLobby;
import forge.match.LobbySlot;
import forge.match.LobbySlotType;
import forge.menu.FPopupMenu;
import forge.model.FModel;
import forge.net.event.UpdateLobbyPlayerEvent;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.FScreen;
import forge.screens.LaunchScreen;
import forge.screens.LoadingOverlay;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FEvent;
import forge.toolbox.FList;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.util.Utils;

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
    private final FComboBox<Object> cbVariants = new FComboBox<Object>();

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
        add(cbVariants);
        cbVariants.setFont(VARIANTS_FONT);
        cbVariants.addItem("(None)");
        cbVariants.addItem(GameType.Vanguard);
        cbVariants.addItem(GameType.MomirBasic);
        cbVariants.addItem(GameType.Commander);
        cbVariants.addItem(GameType.TinyLeaders);
        cbVariants.addItem(GameType.Planechase);
        cbVariants.addItem(GameType.Archenemy);
        cbVariants.addItem(GameType.ArchenemyRumble);
        cbVariants.addItem("More....");
        cbVariants.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if (cbVariants.getSelectedIndex() <= 0) {
                    lobby.clearVariants();
                    updateLayoutForVariants();
                }
                else if (cbVariants.getSelectedIndex() == cbVariants.getItemCount() - 1) {
                    Forge.openScreen(new MultiVariantSelect());
                    updateVariantSelection();
                }
                else {
                    lobby.clearVariants();
                    lobby.applyVariant((GameType)cbVariants.getSelectedItem());
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
                        btnStart.setEnabled(lobby.hasControl());
                    }
                });
            }
        });

        //disable player count for now until multiplayer supported
        lblPlayers.setEnabled(false);
        cbPlayerCount.setEnabled(false);
    }

    public GameLobby getLobby() {
        return lobby;
    }
    protected void initLobby(GameLobby lobby0) {
        lobby = lobby0;
        lobby.setListener(this);

        boolean hasControl = lobby.hasControl();
        btnStart.setEnabled(hasControl);
        lblVariants.setEnabled(hasControl);
        cbVariants.setEnabled(hasControl);
    }

    private void updateVariantSelection() {
        if (lobby == null) {
            cbVariants.setSelectedIndex(0);
            return;
        }

        Iterable<GameType> appliedVariants = lobby.getAppliedVariants();
        int size = Iterables.size(appliedVariants);
        if (size == 0) {
            cbVariants.setSelectedIndex(0);
        }
        else if (size == 1) {
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

    GameType getCurrentGameMode() {
        return lobby.getGameType();
    }
    void setCurrentGameMode(final GameType mode) {
        lobby.setGameType(mode);
        update(true);
    }

    public int getNumPlayers() {
        return cbPlayerCount.getSelectedItem();
    }
    public void setNumPlayers(int numPlayers) {
        cbPlayerCount.setSelectedItem(numPlayers);
    }

    @Override
    protected void startMatch() {
        for (int i = 0; i < getNumPlayers(); i++) {
            updateDeck(i);
        }
        FThreads.invokeInBackgroundThread(new Runnable() { //must call startGame in background thread in case there are alerts
            @Override
            public void run() {
                final Runnable startGame = lobby.startGame();
                if (startGame != null) {
                    FThreads.invokeInEdtLater(new Runnable() {
                        @Override
                        public void run() {
                            LoadingOverlay.show("Loading new game...", startGame);
                        }
                    });
                }
            }
        });
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
            lstVariants.addItem(new Variant(GameType.TinyLeaders));
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
                FCheckBox.drawCheckBox(g, SettingsScreen.DESC_COLOR, color, lobby.hasVariant(gameType), x, y, w, h);
            }

            private void toggle() {
                if (lobby.hasVariant(gameType)) {
                    lobby.removeVariant(gameType);
                }
                else {
                    lobby.applyVariant(gameType);
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
                    updateDeck(i);
                }
                if (isNewPanel) {
                    panel.setVisible(true);
                }
                Gdx.graphics.requestRendering();
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

    private void updateDeck(final int playerIndex) {
        if (playerIndex >= getNumPlayers()) { return; }

        PlayerPanel playerPanel = playerPanels.get(playerIndex);

        Deck deck;
        if (hasVariant(GameType.Commander)) {
            deck = playerPanel.getCommanderDeck();
        }
        else if (hasVariant(GameType.TinyLeaders)) {
            deck = playerPanel.getTinyLeadersDeck();
        }
        else {
            deck = playerPanel.getDeck();
        }

        Deck playerDeck = deck;
        if (hasVariant(GameType.Archenemy) || hasVariant(GameType.ArchenemyRumble)) {
            if (playerDeck == deck) {
                playerDeck = new Deck(deck); //create copy that can be modified
            }
            playerDeck.putSection(DeckSection.Schemes, playerPanel.getSchemeDeck().get(DeckSection.Schemes));
        }
        if (hasVariant(GameType.Planechase)) {
            if (playerDeck == deck) {
                playerDeck = new Deck(deck); //create copy that can be modified
            }
            playerDeck.putSection(DeckSection.Planes, playerPanel.getPlanarDeck().get(DeckSection.Planes));
        }
        if (hasVariant(GameType.Vanguard)) {
            if (playerDeck == deck) {
                playerDeck = new Deck(deck); //create copy that can be modified
            }
            CardPool avatarPool = new CardPool();
            avatarPool.add(playerPanel.getVanguardAvatar());
            playerDeck.putSection(DeckSection.Avatar, avatarPool);
        }

        playerPanel.getDeckChooser().saveState();

        decks[playerIndex] = playerDeck;
        if (playerChangeListener != null) {
            playerChangeListener.update(playerIndex, UpdateLobbyPlayerEvent.deckUpdate(playerDeck));
        }
    }

    void setReady(final int index, final boolean ready) {
        if (ready) {
            updateDeck(index);
            if (decks[index] == null) {
                FOptionPane.showErrorDialog("Select a deck before readying!");
                update(false);
                return;
            }
        }

        firePlayerChangeListener(index);
    }
    void firePlayerChangeListener(final int index) {
        if (playerChangeListener != null) {
            playerChangeListener.update(index, getSlot(index));
        }
    }

    public void removePlayer(final int index) {
        lobby.removeSlot(index);
    }
    public boolean hasVariant(final GameType variant) {
        return lobby.hasVariant(variant);
    }

    private UpdateLobbyPlayerEvent getSlot(final int index) {
        final PlayerPanel panel = playerPanels.get(index);
        return UpdateLobbyPlayerEvent.create(panel.getType(), panel.getPlayerName(), panel.getAvatarIndex(), panel.getTeam(), panel.isArchenemy(), panel.isReady(), panel.getAiOptions());
    }

    public List<PlayerPanel> getPlayerPanels() {
        return playerPanels;
    }

    public FScrollPane getPlayersScroll() {
        return playersScroll;
    }
}
