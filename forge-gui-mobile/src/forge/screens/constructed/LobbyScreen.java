package forge.screens.constructed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;
import forge.player.GamePlayerUtil;
import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.ai.AIOption;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.ImageCache;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.DeckType;
import forge.deck.FDeckChooser;
import forge.game.GameType;
import forge.gamemodes.match.GameLobby;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.gamemodes.net.event.UpdateLobbyPlayerEvent;
import forge.gamemodes.net.server.FServerManager;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.interfaces.ILobbyView;
import forge.interfaces.IPlayerChangeListener;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.menu.FPopupMenu;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.LaunchScreen;
import forge.screens.LoadingOverlay;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FLabel;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.util.MyRandom;
import forge.util.TextUtil;
import forge.util.Utils;
import forge.util.GuiPrefBinders;

public abstract class LobbyScreen extends LaunchScreen implements ILobbyView {
    private static final ForgePreferences prefs = FModel.getPreferences();
    private static final float PADDING = Utils.scale(5);
    public static final int MAX_PLAYERS = 4;
    private static final FSkinFont VARIANTS_FONT = FSkinFont.get(12);

    // General variables
    private GameLobby lobby;
    private IPlayerChangeListener playerChangeListener = null;
    private final FLabel lblPlayers = new FLabel.Builder().text(Forge.getLocalizer().getMessage("lblPlayers") + ":").font(VARIANTS_FONT).build();
    private final FComboBox<Integer> cbPlayerCount;
    private final Deck[] decks = new Deck[MAX_PLAYERS];

    // Variants frame and variables
    private final FLabel lblVariants = new FLabel.Builder().text(Forge.getLocalizer().getMessage("lblVariants") + ":").font(VARIANTS_FONT).build();
    private final FComboBox<Object> cbVariants = new FComboBox<>();

    // Max games in a match frame and variables
    private final FLabel lblGamesInMatch = new FLabel.Builder().text(Forge.getLocalizer().getMessage("lblMatch") + ":").font(VARIANTS_FONT).build();
    private final FComboBox<String> cbGamesInMatch = new FComboBox<>();
    private final GuiPrefBinders.ComboBox cbGamesInMatchBinder =
        new GuiPrefBinders.ComboBox(FPref.UI_MATCHES_PER_GAME, cbGamesInMatch);

    private final List<PlayerPanel> playerPanels = new ArrayList<>(MAX_PLAYERS);
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
            g.drawLine(FList.LINE_THICKNESS, FList.getLineColor(), 0, y, getWidth(), y);
        }
    };

    int lastArchenemy = 0;

    public LobbyScreen(String headerCaption, FPopupMenu menu, GameLobby lobby0) {
        super(headerCaption, menu);

        btnStart.setEnabled(false); //disable start button until decks loaded

        add(lblPlayers);
        cbPlayerCount = add(new FComboBox<>());
        cbPlayerCount.setFont(VARIANTS_FONT);
        for (int i = 2; i <= MAX_PLAYERS; i++) {
            cbPlayerCount.addItem(i);
        }
        cbPlayerCount.setSelectedItem(2);
        cbPlayerCount.setChangedHandler(event -> {
            int numPlayers = getNumPlayers();
            while(lobby.getNumberOfSlots() < getNumPlayers()){
                lobby.addSlot();
            }
            while(lobby.getNumberOfSlots() > getNumPlayers()){
                lobby.removeSlot(lobby.getNumberOfSlots()-1);
            }
            for (int i = 0; i < MAX_PLAYERS; i++) {
                if(i<playerPanels.size()) {
                    playerPanels.get(i).setVisible(i < numPlayers);
                }
            }
            playersScroll.revalidate();
        });

        initLobby(lobby0);

        add(lblGamesInMatch);
        add(cbGamesInMatch);
        cbGamesInMatch.setFont(VARIANTS_FONT);
        cbGamesInMatch.addItem("1");
        cbGamesInMatch.addItem("3");
        cbGamesInMatch.addItem("5");

        add(lblVariants);
        add(cbVariants);
        cbVariants.setFont(VARIANTS_FONT);
        cbVariants.addItem("(" + Forge.getLocalizer().getMessage("lblNone") + ")");
        cbVariants.addItem(GameType.Vanguard);
        cbVariants.addItem(GameType.MomirBasic);
        cbVariants.addItem(GameType.MoJhoSto);
        cbVariants.addItem(GameType.Commander);
        cbVariants.addItem(GameType.Oathbreaker);
        cbVariants.addItem(GameType.TinyLeaders);
        cbVariants.addItem(GameType.Brawl);
        cbVariants.addItem(GameType.Planechase);
        cbVariants.addItem(GameType.Archenemy);
        cbVariants.addItem(GameType.ArchenemyRumble);
        cbVariants.addItem(Forge.getLocalizer().getMessage("lblMore"));
        cbVariants.setChangedHandler(event -> {
            if (cbVariants.getSelectedIndex() <= 0) {
                lobby.clearVariants();
                updateLayoutForVariants();
                Set<GameType> gameTypes = new HashSet<>();
                FModel.getPreferences().setGameType(FPref.UI_APPLIED_VARIANTS, gameTypes);
                FModel.getPreferences().save();
            }
            else if (cbVariants.getSelectedIndex() == cbVariants.getItemCount() - 1) {
                Forge.openScreen(new MultiVariantSelect());
                updateVariantSelection();
            }
            else {
                lobby.clearVariants();
                lobby.applyVariant((GameType)cbVariants.getSelectedItem());
                updateLayoutForVariants();
                Set<GameType> gameTypes = new HashSet<>();
                for (GameType variant: lobby.getAppliedVariants()) {
                    gameTypes.add(variant);
                }
                FModel.getPreferences().setGameType(FPref.UI_APPLIED_VARIANTS, gameTypes);
                FModel.getPreferences().save();
            }
        });

        update(false);

        add(playersScroll);

        updatePlayersFromPrefs();

        FThreads.invokeInBackgroundThread(() -> {
            playerPanels.get(0).initialize(FPref.CONSTRUCTED_P1_DECK_STATE, FPref.COMMANDER_P1_DECK_STATE, FPref.OATHBREAKER_P1_DECK_STATE, FPref.TINY_LEADER_P1_DECK_STATE, FPref.BRAWL_P1_DECK_STATE, DeckType.PRECONSTRUCTED_DECK);
            playerPanels.get(1).initialize(FPref.CONSTRUCTED_P2_DECK_STATE, FPref.COMMANDER_P2_DECK_STATE, FPref.OATHBREAKER_P2_DECK_STATE, FPref.TINY_LEADER_P2_DECK_STATE, FPref.BRAWL_P2_DECK_STATE, DeckType.COLOR_DECK);
            try {
                if (getNumPlayers() > 2) {
                    playerPanels.get(2).initialize(FPref.CONSTRUCTED_P3_DECK_STATE, FPref.COMMANDER_P3_DECK_STATE, FPref.OATHBREAKER_P3_DECK_STATE, FPref.TINY_LEADER_P3_DECK_STATE, FPref.BRAWL_P3_DECK_STATE, DeckType.COLOR_DECK);
                }
                if (getNumPlayers() > 3) {
                    playerPanels.get(3).initialize(FPref.CONSTRUCTED_P4_DECK_STATE, FPref.COMMANDER_P4_DECK_STATE, FPref.OATHBREAKER_P3_DECK_STATE, FPref.TINY_LEADER_P4_DECK_STATE, FPref.BRAWL_P4_DECK_STATE, DeckType.COLOR_DECK);
                }
            } catch (Exception e) {}
            /*playerPanels.get(4).initialize(FPref.CONSTRUCTED_P5_DECK_STATE, DeckType.COLOR_DECK);
            playerPanels.get(5).initialize(FPref.CONSTRUCTED_P6_DECK_STATE, DeckType.COLOR_DECK);
            playerPanels.get(6).initialize(FPref.CONSTRUCTED_P7_DECK_STATE, DeckType.COLOR_DECK);
            playerPanels.get(7).initialize(FPref.CONSTRUCTED_P8_DECK_STATE, DeckType.COLOR_DECK);*/ //TODO: Improve performance of loading this screen by using background thread

            FThreads.invokeInEdtLater(() -> {
                btnStart.setEnabled(lobby.hasControl());

                Set<GameType> gameTypes = FModel.getPreferences().getGameType(FPref.UI_APPLIED_VARIANTS);
                if (!gameTypes.isEmpty()) {
                    for (GameType gameType : gameTypes) {
                        lobby.applyVariant(gameType);
                    }
                    updateVariantSelection();
                    updateLayoutForVariants();
                }
            });
        });

        lblPlayers.setEnabled(true);
        cbPlayerCount.setEnabled(true);
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
        lblGamesInMatch.setEnabled(hasControl);
        cbGamesInMatch.setEnabled(hasControl);
        lblPlayers.setEnabled(hasControl);
        cbPlayerCount.setEnabled(hasControl);
        while (lobby.getNumberOfSlots() < getNumPlayers()){
            lobby.addSlot();
        }
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
            StringBuilder text = new StringBuilder();
            for (GameType variantType : appliedVariants) {
                if (text.length() > 0) {
                    text.append(", ");
                }
                text.append(variantType.toString());
            }
            cbVariants.setText(text.toString());
        }
    }

    void updateLayoutForVariants() {
        for (int i = 0; i < cbPlayerCount.getSelectedItem(); i++) {
            playerPanels.get(i).updateVariantControlsVisibility();
        }
        playersScroll.revalidate();
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING;
        float fieldHeight = cbPlayerCount.getHeight();
        if (Forge.isLandscapeMode()) {
            lblVariants.setBounds(x, y, lblVariants.getAutoSizeBounds().width + PADDING / 2, fieldHeight);
            x += lblVariants.getWidth();
            cbVariants.setBounds(x, y, width - (x + lblGamesInMatch.getAutoSizeBounds().width + PADDING/2
                    + lblPlayers.getAutoSizeBounds().width + (Utils.AVG_FINGER_WIDTH + PADDING)*2), fieldHeight);
            x += cbVariants.getWidth();
            lblPlayers.setBounds(x, y, lblPlayers.getAutoSizeBounds().width + PADDING / 2, fieldHeight);
            x += lblPlayers.getWidth();
            cbPlayerCount.setBounds(x, y, Utils.AVG_FINGER_WIDTH, fieldHeight);
            x += cbPlayerCount.getWidth() + PADDING;
            lblGamesInMatch.setBounds(x, y, lblGamesInMatch.getAutoSizeBounds().width + PADDING / 2, fieldHeight);
            x += lblGamesInMatch.getWidth();
            cbGamesInMatch.setBounds(x, y, Utils.AVG_FINGER_WIDTH, fieldHeight);
        } else {
            lblVariants.setBounds(x, y, lblVariants.getAutoSizeBounds().width + PADDING / 2, fieldHeight);
            x += lblVariants.getWidth();
            cbVariants.setBounds(x, y, width - x - PADDING, fieldHeight);
            x = PADDING;
            y += cbVariants.getHeight() + PADDING;
            lblPlayers.setBounds(x, y, lblPlayers.getAutoSizeBounds().width + PADDING / 2, fieldHeight);
            x += lblPlayers.getWidth();
            cbPlayerCount.setBounds(x, y, Utils.AVG_FINGER_WIDTH, fieldHeight);
            x += cbPlayerCount.getWidth() + PADDING;
            lblGamesInMatch.setBounds(x, y, lblGamesInMatch.getAutoSizeBounds().width + PADDING / 2, fieldHeight);
            x += lblGamesInMatch.getWidth();
            cbGamesInMatch.setBounds(x, y, Utils.AVG_FINGER_WIDTH, fieldHeight);
        }
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
            if(!lobby.isAllowNetworking()) //on networkplay, update deck will be handled differently
                updateDeck(i);

            //TODO: Investigate why AI names cannot be overriden?
            updateName(i, getPlayerName(i));
            if(i == 0 && Forge.isMobileAdventureMode) {
                updateName(0, GamePlayerUtil.getGuiPlayer().getName());
            }
        }
        //must call startGame in background thread in case there are alerts
        FThreads.invokeInBackgroundThread(() -> {
            final Runnable startGame = lobby.startGame();
            if (startGame != null) {
                //set this so we cant get any multi/rapid tap on start button
                Forge.setLoadingaMatch(true);
                FThreads.invokeInEdtLater(() -> LoadingOverlay.show(Forge.getLocalizer().getMessage("lblLoadingNewGame"), true, startGame));
            }
        });
    }

    /** Saves avatar prefs for players one and two. */
    void  updateAvatarPrefs() {
        int pOneIndex = playerPanels.get(0).getAvatarIndex();
        int pTwoIndex = playerPanels.get(1).getAvatarIndex();

        prefs.setPref(FPref.UI_AVATARS, pOneIndex + "," + pTwoIndex);
        prefs.save();
    }
    void  updateSleevePrefs() {
        int pOneIndex = playerPanels.get(0).getSleeveIndex();
        int pTwoIndex = playerPanels.get(1).getSleeveIndex();

        prefs.setPref(FPref.UI_SLEEVES, pOneIndex + "," + pTwoIndex);
        prefs.save();
    }

    /** Updates the avatars from preferences on update. */
    private void updatePlayersFromPrefs() {
        ForgePreferences prefs = FModel.getPreferences();

        // Avatar
        String[] avatarPrefs = prefs.getPref(FPref.UI_AVATARS).split(",");
        for (int i = 0; i < avatarPrefs.length; i++) {
            int avatarIndex = Integer.parseInt(avatarPrefs[i]);
            if (avatarIndex < 0) {
                int random = 0;
                List<Integer> usedAvatars = getUsedAvatars();
                do {
                    random = MyRandom.getRandom().nextInt(GuiBase.getInterface().getAvatarCount());
                } while (usedAvatars.contains(random));

                avatarIndex = random;
            }
            playerPanels.get(i).setAvatarIndex(avatarIndex);
        }

        // Sleeves
        String[] sleevePrefs = prefs.getPref(FPref.UI_SLEEVES).split(",");
        for (int i = 0; i < sleevePrefs.length; i++) {
            int sleeveIndex = Integer.parseInt(sleevePrefs[i]);
            if (sleeveIndex < 0) {
                int random = 0;
                List<Integer> usedSleeves = getUsedSleeves();
                do {
                    random = MyRandom.getRandom().nextInt(GuiBase.getInterface().getSleevesCount());
                } while (usedSleeves.contains(random));

                sleeveIndex = random;
            }
            playerPanels.get(i).setSleeveIndex(sleeveIndex);
        }

        // Name
        String prefName = prefs.getPref(FPref.PLAYER_NAME);
        playerPanels.get(0).setPlayerName(StringUtils.isBlank(prefName) ? Forge.getLocalizer().getInstance().getMessage("lblHuman") : prefName);
    }

    List<Integer> getUsedAvatars() {
        List<Integer> usedAvatars = Arrays.asList(-1,-1,-1,-1,-1,-1,-1,-1);
        int i = 0;
        for (PlayerPanel pp : playerPanels) {
            usedAvatars.set(i++, pp.getAvatarIndex());
        }
        return usedAvatars;
    }

    List<Integer> getUsedSleeves() {
        List<Integer> usedSleeves = Arrays.asList(-1,-1,-1,-1,-1,-1,-1,-1);
        int i = 0;
        for (PlayerPanel pp : playerPanels) {
            usedSleeves.set(i++, pp.getSleeveIndex());
        }
        return usedSleeves;
    }

    List<String> getPlayerNames() {
        List<String> names = new ArrayList<>();
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

    public int getPlayerSleeve(int i) {
        return playerPanels.get(i).getSleeveIndex();
    }

    /////////////////////////////////////////////
    //========== Various listeners in build order
    
    private class MultiVariantSelect extends FScreen {
        private final FList<Variant> lstVariants = add(new FList<>());

        private MultiVariantSelect() {
            super(Forge.getLocalizer().getInstance().getMessage("lblSelectVariants"));

            lstVariants.setListItemRenderer(new VariantRenderer());
            lstVariants.addItem(new Variant(GameType.Vanguard));
            lstVariants.addItem(new Variant(GameType.MomirBasic));
            lstVariants.addItem(new Variant(GameType.MoJhoSto));
            lstVariants.addItem(new Variant(GameType.Commander));
            lstVariants.addItem(new Variant(GameType.Oathbreaker));
            lstVariants.addItem(new Variant(GameType.TinyLeaders));
            lstVariants.addItem(new Variant(GameType.Brawl));
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
                Set<GameType> gameTypes = new HashSet<>();
                for (GameType variant: lobby.getAppliedVariants()) {
                    gameTypes.add(variant);
                }
                FModel.getPreferences().setGameType(FPref.UI_APPLIED_VARIANTS, gameTypes);
                FModel.getPreferences().save();
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

                g.drawText(text, font, foreColor, x, y, w, h, false, Align.left, false);
                value.draw(g, font, foreColor, x, y, w, h);
                h += SettingsScreen.SETTING_PADDING;
                g.drawText(value.gameType.getDescription(), SettingsScreen.DESC_FONT, SettingsScreen.DESC_COLOR, x, y + h, w, totalHeight - h + SettingsScreen.getInsets(w), true, Align.left, false);
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
    public final void update(final int slot, final LobbySlotType type) {
        final FDeckChooser deckChooser = playerPanels.get(slot).getDeckChooser();
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
                deckChooser.refreshDeckListForAI();
                break;
            default:
                break;
        }
        final FDeckChooser commanderDeckChooser = playerPanels.get(slot).getCommanderDeckChooser();
        selectedDeckType = commanderDeckChooser.getSelectedDeckType();
        switch (selectedDeckType){
            case RANDOM_CARDGEN_COMMANDER_DECK:
            case RANDOM_COMMANDER_DECK:
                commanderDeckChooser.refreshDeckListForAI();
                break;
            default:
                break;
        }
        final FDeckChooser tinyLeaderDeckChooser = playerPanels.get(slot).getTinyLeadersDeckChooser();
        selectedDeckType = tinyLeaderDeckChooser.getSelectedDeckType();
        switch (selectedDeckType){
            case RANDOM_CARDGEN_COMMANDER_DECK:
            case RANDOM_COMMANDER_DECK:
                tinyLeaderDeckChooser.refreshDeckListForAI();
                break;
            default:
                break;
        }
        final FDeckChooser brawlDeckChooser = playerPanels.get(slot).getBrawlDeckChooser();
        selectedDeckType = brawlDeckChooser.getSelectedDeckType();
        switch (selectedDeckType){
            case RANDOM_CARDGEN_COMMANDER_DECK:
            case RANDOM_COMMANDER_DECK:
                brawlDeckChooser.refreshDeckListForAI();
                break;
            default:
                break;
        }
    }

    @Override
    public void onActivate() {
        cbGamesInMatchBinder.load();
    }

    @Override
    public void update(final boolean fullUpdate) {
        int playerCount = lobby.getNumberOfSlots();

        updateVariantSelection();

        final boolean allowNetworking = lobby.isAllowNetworking();

        setStartButtonAvailability();

        for (int i = 0; i < cbPlayerCount.getSelectedItem(); i++) {
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
                    if (i == 2) {
                        panel.initialize(FPref.CONSTRUCTED_P3_DECK_STATE, FPref.COMMANDER_P3_DECK_STATE, FPref.OATHBREAKER_P3_DECK_STATE, FPref.TINY_LEADER_P3_DECK_STATE, FPref.BRAWL_P3_DECK_STATE, DeckType.COLOR_DECK);
                    } else if (i == 3) {
                        panel.initialize(FPref.CONSTRUCTED_P4_DECK_STATE, FPref.COMMANDER_P4_DECK_STATE, FPref.OATHBREAKER_P4_DECK_STATE, FPref.TINY_LEADER_P4_DECK_STATE, FPref.BRAWL_P4_DECK_STATE, DeckType.COLOR_DECK);
                    }
                    playerPanels.add(panel);
                    playersScroll.add(panel);
                    isNewPanel = true;
                }

                if (i == 0) {
                    slot.setIsDevMode(slot.isDevMode());
                }

                final LobbySlotType type = slot.getType();
                panel.setType(type);
                if (type != LobbySlotType.AI) {
                    panel.setPlayerName(slot.getName());
                    panel.setAvatarIndex(slot.getAvatarIndex());
                    panel.setSleeveIndex(slot.getSleeveIndex());
                } else {
                    //AI: this one overrides the setplayername if blank
                    if (panel.getPlayerName().isEmpty())
                        panel.setPlayerName(slot.getName());
                    //AI: override settings if somehow player changes it for AI
                    slot.setAvatarIndex(panel.getAvatarIndex());
                    slot.setSleeveIndex(panel.getSleeveIndex());
                }
                panel.setTeam(slot.getTeam());
                panel.setIsReady(slot.isReady());
                panel.setIsDevMode(slot.isDevMode());
                panel.setIsArchenemy(slot.isArchenemy());
                panel.setUseAiSimulation(slot.getAiOptions().contains(AIOption.USE_SIMULATION));
                panel.setMayEdit(lobby.mayEdit(i));
                panel.setMayControl(lobby.mayControl(i));
                panel.setMayRemove(lobby.mayRemove(i));
                if(allowNetworking) {
                    if(slot.getDeckName() != null)
                        panel.setDeckSelectorButtonText(slot.getDeckName());

                    if(slot.getPlanarDeckName()!= null)
                        panel.setPlanarDeckName(slot.getPlanarDeckName());

                    if(slot.getSchemeDeckName()!= null)
                        panel.setSchemeDeckName(slot.getSchemeDeckName());

                    if(slot.getAvatarVanguard()!= null)
                        panel.setVanguarAvatarName(slot.getAvatarVanguard());
                }

                if (fullUpdate && (type == LobbySlotType.LOCAL || type == LobbySlotType.AI) && !allowNetworking) {
                    updateDeck(i);
                }
                if (isNewPanel) {
                    panel.setVisible(true);
                }
                if (Forge.gameInProgress) {
                    /*preload deck to cache*/
                    if(slot.getType() == LobbySlotType.LOCAL)
                        ImageCache.getInstance().preloadCache(decks[i]);
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
        String deckName = "";

        Deck deck;
        if (hasVariant(GameType.Commander)) {
            deck = playerPanel.getCommanderDeck();
            if (deck != null) {
                playerPanel.getCommanderDeckChooser().saveState();
                deckName =  Forge.getLocalizer().getMessage("lblCommanderDeck") + ": "
                        + playerPanel.getCommanderDeckChooser().getDeck().getName();
            }
        }
        else if (hasVariant(GameType.Oathbreaker)) {
            deck = playerPanel.getOathbreakerDeck();
            if (deck != null) {
                playerPanel.getOathbreakerDeckChooser().saveState();
                deckName =  Forge.getLocalizer().getMessage("lblOathbreakerDeck") + ": "
                        + playerPanel.getOathbreakerDeckChooser().getDeck().getName();
            }
        }
        else if (hasVariant(GameType.TinyLeaders)) {
            deck = playerPanel.getTinyLeadersDeck();
            if (deck != null) {
                playerPanel.getTinyLeadersDeckChooser().saveState();
                deckName =  Forge.getLocalizer().getMessage("lblTinyLeadersDeck") + ": "
                        + playerPanel.getTinyLeadersDeckChooser().getDeck().getName();
            }
        }
        else if (hasVariant(GameType.Brawl)) {
            deck = playerPanel.getBrawlDeck();
            if (deck != null) {
                playerPanel.getBrawlDeckChooser().saveState();
                deckName =  Forge.getLocalizer().getMessage("lblBrawlDeck") + ": "
                        + playerPanel.getBrawlDeckChooser().getDeck().getName();
            }
        }else {
            deck = playerPanel.getDeck();
            if (deck != null) {
                playerPanel.getDeckChooser().saveState();
                deckName =  playerPanel.getDeckChooser().getSelectedDeckType().toString() + ": "
                        + playerPanel.getDeckChooser().getDeck().getName();
            }
        }

        if (deck == null) {
            return;
        }

        //playerPanel.setDeckSelectorButtonText(deckName);

        Deck playerDeck = deck;
        String VanguardAvatar = null;
        String SchemeDeckName= null;
        String PlanarDeckname= null;
        if (hasVariant(GameType.Archenemy) || hasVariant(GameType.ArchenemyRumble)) {
            if (playerDeck == deck) {
                playerDeck = new Deck(deck); //create copy that can be modified
            }
            playerDeck.putSection(DeckSection.Schemes, playerPanel.getSchemeDeck().get(DeckSection.Schemes));
            if (!playerPanel.getSchemeDeck().getName().isEmpty()) {
                SchemeDeckName = Forge.getLocalizer().getMessage("lblSchemeDeck") + ": " + playerPanel.getSchemeDeck().getName();
                playerPanel.setSchemeDeckName(SchemeDeckName);
            }
        }
        if (hasVariant(GameType.Planechase)) {
            if (playerDeck == deck) {
                playerDeck = new Deck(deck); //create copy that can be modified
            }
            playerDeck.putSection(DeckSection.Planes, playerPanel.getPlanarDeck().get(DeckSection.Planes));
            if(!playerPanel.getPlanarDeck().getName().isEmpty()) {
                PlanarDeckname = Forge.getLocalizer().getMessage("lblPlanarDeck") + ": " + playerPanel.getPlanarDeck().getName();
                playerPanel.setPlanarDeckName(PlanarDeckname);
            }
        }
        if (hasVariant(GameType.Vanguard)) {
            if (playerDeck == deck) {
                playerDeck = new Deck(deck); //create copy that can be modified
            }
            CardPool avatarPool = new CardPool();
            avatarPool.add(playerPanel.getVanguardAvatar());
            playerDeck.putSection(DeckSection.Avatar, avatarPool);
            VanguardAvatar = Forge.getLocalizer().getMessage("lblVanguard") + ": " + playerPanel.getVanguardAvatar().getDisplayName();
            playerPanel.setVanguarAvatarName(VanguardAvatar);
        }

        decks[playerIndex] = playerDeck;
        if (playerChangeListener != null) {
            playerChangeListener.update(playerIndex, UpdateLobbyPlayerEvent.deckUpdate(playerDeck));
            playerChangeListener.update(playerIndex, UpdateLobbyPlayerEvent.setDeckSchemePlaneVanguard(TextUtil.fastReplace(deckName," Generated Deck", ""), SchemeDeckName, PlanarDeckname, VanguardAvatar));
        }
    }

    private void updateName(final int playerIndex, final String name) {
        if (playerChangeListener != null) {
            playerChangeListener.update(playerIndex, UpdateLobbyPlayerEvent.nameUpdate(name));
        }
    }

    void updateAvatar(final int index, final int avatarIndex) {
        if (playerChangeListener != null) {
            playerChangeListener.update(index, UpdateLobbyPlayerEvent.avatarUpdate(avatarIndex));
        }
    }

    void updateSleeve(final int index, final int sleeveIndex) {
        if (playerChangeListener != null) {
            playerChangeListener.update(index, UpdateLobbyPlayerEvent.sleeveUpdate(sleeveIndex));
        }
    }

    void setReady(final int index, final boolean ready) {
        if (lobby.isAllowNetworking()){
            updateDeck(index);
            fireReady(index, ready);
            return;
        }
        if (ready) {
            updateDeck(index);
            if (decks[index] == null) {
                FOptionPane.showErrorDialog(Forge.getLocalizer().getMessage("msgSelectAdeckBeforeReadying"));
                update(false);
                return;
            }
        }
        firePlayerChangeListener(index);
    }
    void setDevMode(final int index) {
        int playerCount = lobby.getNumberOfSlots();
        // clear ready for everyone
        for (int i = 0; i < playerCount; i++) {
            final PlayerPanel panel = playerPanels.get(i);
            panel.setIsReady(false);
            firePlayerChangeListener(i);
        }
    }
    void firePlayerChangeListener(final int index) {
        if (playerChangeListener != null) {
            playerChangeListener.update(index, getSlot(index));
        }
    }
    void fireReady(final int index, boolean ready){
        playerPanels.get(index).setIsReady(ready);
        if (playerChangeListener != null) {
            playerChangeListener.update(index, UpdateLobbyPlayerEvent.isReadyUpdate(ready));
        }
    }
    void updatemyTeam(int index, int team) {
        if (playerChangeListener != null) {
            playerChangeListener.update(index, UpdateLobbyPlayerEvent.teamUpdate(team));
        }
    }
    void updateMyDeck(int index) {
        /* updateMyDeck is called via button handler when the user set their deck on network play*/

        //safety check
        if(playerPanels.size() < 2)
            return;

        updateDeck(index);
        //fireReady(index, playerPanels.get(index).isReady());
    }

    public void removePlayer(final int index) {
        lobby.removeSlot(index);
    }
    public boolean hasVariant(final GameType variant) {
        return lobby.hasVariant(variant);
    }

    private UpdateLobbyPlayerEvent getSlot(final int index) {
        final PlayerPanel panel = playerPanels.get(index);
        return UpdateLobbyPlayerEvent.create(panel.getType(),
                panel.getPlayerName(),
                panel.getAvatarIndex(),
                panel.getSleeveIndex(),
                panel.getTeam(),
                panel.isArchenemy(),
                panel.isReady(),
                panel.isDevMode(),
                panel.getAiOptions(),
                null); // TODO implement AI profile support for mobile
    }

    public List<PlayerPanel> getPlayerPanels() {
        return playerPanels;
    }

    public FScrollPane getPlayersScroll() {
        return playersScroll;
    }

    public void setStartButtonAvailability() {
        if (lobby.isAllowNetworking() && FServerManager.getInstance() != null)
            btnStart.setVisible(FServerManager.getInstance().isHosting());
        else
            btnStart.setVisible(true);
    }
}
