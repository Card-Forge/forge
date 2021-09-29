package forge.screens.constructed;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.utils.Align;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import forge.Forge;
import forge.Graphics;
import forge.ai.AIOption;
import forge.assets.FSkin;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.assets.FTextureRegionImage;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.deck.DeckType;
import forge.deck.FDeckChooser;
import forge.deck.FVanguardChooser;
import forge.game.GameType;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;
import forge.item.PaperCard;
import forge.itemmanager.CardManager;
import forge.itemmanager.DeckManager;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.toolbox.FComboBox;
import forge.toolbox.FContainer;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.toolbox.FTextField;
import forge.toolbox.FToggleSwitch;
import forge.util.Callback;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.NameGenerator;
import forge.util.TextUtil;
import forge.util.Utils;

public class PlayerPanel extends FContainer {
    private static final ForgePreferences prefs = FModel.getPreferences();
    private static final float PADDING = Utils.scale(5);
    private static final FSkinFont LABEL_FONT = FSkinFont.get(14);

    private final LobbyScreen screen;
    private final int index;
    private final boolean allowNetworking;
    private boolean mayEdit = true;
    private boolean isReady, mayControl, mayRemove, useAiSimulation;
    private LobbySlotType type = LobbySlotType.LOCAL;

    private final FLabel nameRandomiser;
    private final FLabel avatarLabel = new FLabel.Builder().opaque(true).iconScaleFactor(0.99f).selectable().alphaComposite(1).iconInBackground(true).build();
    private final FLabel sleeveLabel = new FLabel.Builder().opaque(true).iconScaleFactor(0.99f).selectable().alphaComposite(1).iconInBackground(true).build();
    private int avatarIndex, sleeveIndex;

    final Localizer localizer = Localizer.getInstance();
    private final FTextField txtPlayerName = new FTextField(localizer.getMessage("lblPlayerName"));
    private final FToggleSwitch humanAiSwitch;
    private final FToggleSwitch devModeSwitch;

    private FComboBox<Object> cbTeam = new FComboBox<>();
    private FComboBox<Object> cbArchenemyTeam = new FComboBox<>();

    private final FLabel btnDeck            = new FLabel.ButtonBuilder().text(localizer.getMessage("lblLoadingDeck")).build();
    private final FLabel btnSchemeDeck      = new FLabel.ButtonBuilder().text(localizer.getMessage("lblSchemeDeckRandomGenerated")).build();
    private final FLabel btnCommanderDeck   = new FLabel.ButtonBuilder().text(localizer.getMessage("lblCommanderDeckRandomGenerated")).build();
    private final FLabel btnOathbreakDeck   = new FLabel.ButtonBuilder().text(localizer.getMessage("lblOathbreakerDeckRandomGenerated")).build();
    private final FLabel btnTinyLeadersDeck = new FLabel.ButtonBuilder().text(localizer.getMessage("lblTinyLeadersDeckRandomGenerated")).build();
    private final FLabel btnBrawlDeck       = new FLabel.ButtonBuilder().text(localizer.getMessage("lblBrawlDeckRandomGenerated")).build();
    private final FLabel btnPlanarDeck      = new FLabel.ButtonBuilder().text(localizer.getMessage("lblPlanarDeckRandomGenerated")).build();
    private final FLabel btnVanguardAvatar  = new FLabel.ButtonBuilder().text(localizer.getMessage("lblVanguardAvatarRandom")).build();

    private final FDeckChooser deckChooser, lstSchemeDecks, lstCommanderDecks, lstOathbreakerDecks, lstTinyLeadersDecks, lstBrawlDecks, lstPlanarDecks;
    private final FVanguardChooser lstVanguardAvatars;

    public PlayerPanel(final LobbyScreen screen0, final boolean allowNetworking0, final int index0, final LobbySlot slot, final boolean mayEdit0, final boolean mayControl0) {
        super();
        screen = screen0;
        allowNetworking = allowNetworking0;
        if (allowNetworking) {
            humanAiSwitch = new FToggleSwitch(localizer.getMessage("lblNotReady"), localizer.getMessage("lblReady"));
        }
        else {
            humanAiSwitch = new FToggleSwitch(localizer.getMessage("lblHuman"), localizer.getMessage("lblAI"));
        }
        index = index0;
        populateTeamsComboBoxes();
        setTeam(slot.getTeam());
        setIsArchenemy(slot.isArchenemy());
        setType(slot.getType());
        setPlayerName(slot.getName());
        setAvatarIndex(slot.getAvatarIndex());
        setSleeveIndex(slot.getSleeveIndex());

        devModeSwitch = new FToggleSwitch(localizer.getMessage("lblNormal"), localizer.getMessage("lblDevMode"));
        devModeSwitch.setVisible(isNetworkHost());

        cbTeam.setEnabled(true);

        btnDeck.setEnabled(false); //disable deck button until done loading decks

        boolean isAi = isAi();
        deckChooser = new FDeckChooser(GameType.Constructed, isAi, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                btnDeck.setEnabled(mayEdit);
                btnDeck.setText(deckChooser.getSelectedDeckType().toString() + ":" + (Forge.isLandscapeMode() ? " " : "\n") +
                        Lang.joinHomogenous(((DeckManager)e.getSource()).getSelectedItems(), DeckProxy.FN_GET_NAME));
                if (allowNetworking && btnDeck.isEnabled() && humanAiSwitch.isToggled()) { //if its ready but changed the deck, update it
                    screen.updateMyDeck(index);
                }
            }
        });
        lstCommanderDecks = new FDeckChooser(GameType.Commander, isAi, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if( ((DeckManager)e.getSource()).getSelectedItem() != null) {
                    btnCommanderDeck.setText(localizer.getMessage("lblCommanderDeck")
                            + ":" + (Forge.isLandscapeMode() ? " " : "\n") + ((DeckManager) e.getSource()).getSelectedItem().getName());
                    lstCommanderDecks.saveState();
                    if (allowNetworking && btnCommanderDeck.isEnabled() && humanAiSwitch.isToggled()) {
                        screen.updateMyDeck(index);
                    }
                }else{
                    btnCommanderDeck.setText(localizer.getMessage("lblCommanderDeck"));
                }
            }
        });
        lstOathbreakerDecks = new FDeckChooser(GameType.Oathbreaker, isAi, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if( ((DeckManager)e.getSource()).getSelectedItem() != null) {
                    btnOathbreakDeck.setText(localizer.getMessage("lblOathbreakerDeck")
                            + ":" + (Forge.isLandscapeMode() ? " " : "\n") + ((DeckManager) e.getSource()).getSelectedItem().getName());
                    lstOathbreakerDecks.saveState();
                    if (allowNetworking && btnOathbreakDeck.isEnabled() && humanAiSwitch.isToggled()) {
                        screen.updateMyDeck(index);
                    }
                }else{
                    btnOathbreakDeck.setText(localizer.getMessage("lblOathbreakerDeck"));
                }
            }
        });
        lstTinyLeadersDecks = new FDeckChooser(GameType.TinyLeaders, isAi, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if( ((DeckManager)e.getSource()).getSelectedItem() != null) {
                    btnTinyLeadersDeck.setText(localizer.getMessage("lblTinyLeadersDeck")
                            + ":" + (Forge.isLandscapeMode() ? " " : "\n") + ((DeckManager) e.getSource()).getSelectedItem().getName());
                    lstTinyLeadersDecks.saveState();
                    if (allowNetworking && btnTinyLeadersDeck.isEnabled() && humanAiSwitch.isToggled()) {
                        screen.updateMyDeck(index);
                    }
                }else{
                    btnTinyLeadersDeck.setText(localizer.getMessage("lblTinyLeadersDeck"));
                }
            }
        });
        lstBrawlDecks = new FDeckChooser(GameType.Brawl, isAi, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if( ((DeckManager)e.getSource()).getSelectedItem() != null) {
                    btnBrawlDeck.setText(localizer.getMessage("lblBrawlDeck")
                            + ":" + (Forge.isLandscapeMode() ? " " : "\n") + ((DeckManager) e.getSource()).getSelectedItem().getName());
                    lstBrawlDecks.saveState();
                    if (allowNetworking && btnBrawlDeck.isEnabled() && humanAiSwitch.isToggled()) {
                        screen.updateMyDeck(index);
                    }
                }else{
                    btnBrawlDeck.setText(localizer.getMessage("lblBrawlDeck"));
                }
            }
        });
        lstSchemeDecks = new FDeckChooser(GameType.Archenemy, isAi, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if( ((DeckManager)e.getSource()).getSelectedItem() != null){
                    btnSchemeDeck.setText(localizer.getMessage("lblSchemeDeck")
                            + ":" + (Forge.isLandscapeMode() ? " " : "\n") + ((DeckManager)e.getSource()).getSelectedItem().getName());
                    if (allowNetworking && btnSchemeDeck.isEnabled() && humanAiSwitch.isToggled()) {
                        screen.updateMyDeck(index);
                    }
                }else{
                    btnSchemeDeck.setText(localizer.getMessage("lblSchemeDeck"));
                }
            }
        });
        lstPlanarDecks = new FDeckChooser(GameType.Planechase, isAi, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                if( ((DeckManager)e.getSource()).getSelectedItem() != null){
                    btnPlanarDeck.setText(localizer.getMessage("lblPlanarDeck")
                            + ":" + (Forge.isLandscapeMode() ? " " : "\n") + ((DeckManager)e.getSource()).getSelectedItem().getName());
                    if (allowNetworking && btnPlanarDeck.isEnabled() && humanAiSwitch.isToggled()) {
                        screen.updateMyDeck(index);
                    }
                }else{
                    btnPlanarDeck.setText(localizer.getMessage("lblPlanarDeck"));
                }
            }
        });
        lstVanguardAvatars = new FVanguardChooser(isAi, new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                btnVanguardAvatar.setText(localizer.getMessage("lblVanguard")
                        + ":" + (Forge.isLandscapeMode() ? " " : "\n") + ((CardManager)e.getSource()).getSelectedItem().getName());
                if (allowNetworking && btnVanguardAvatar.isEnabled() && humanAiSwitch.isToggled()) {
                    screen.updateMyDeck(index);
                }
            }
        });

        createAvatar();
        add(avatarLabel);

        createSleeve();
        add(sleeveLabel);

        createNameEditor();
        add(newLabel(localizer.getMessage("lblName") + ":"));
        add(txtPlayerName);

        nameRandomiser = createNameRandomizer();
        add(nameRandomiser);

        humanAiSwitch.setChangedHandler(humanAiSwitched);
        add(humanAiSwitch);

        add(newLabel(localizer.getMessage("lblTeam") + ":"));
        cbTeam.setChangedHandler(teamChangedHandler);
        cbArchenemyTeam.setChangedHandler(teamChangedHandler);
        add(cbTeam);
        add(cbArchenemyTeam);

        if (isNetworkHost()) {
            devModeSwitch.setChangedHandler(devModeSwitched);
            add(devModeSwitch);
        }
        add(btnDeck);
        btnDeck.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                deckChooser.setHeaderCaption(localizer.getMessage("lblSelectDeckFor").replace("%s", txtPlayerName.getText()));
                Forge.openScreen(deckChooser);
            }
        });
        add(btnCommanderDeck);
        btnCommanderDeck.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                lstCommanderDecks.setHeaderCaption(localizer.getMessage("lblSelectCommanderDeckFor").replace("%s", txtPlayerName.getText()));
                Forge.openScreen(lstCommanderDecks);
            }
        });
        add(btnOathbreakDeck);
        btnOathbreakDeck.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                lstOathbreakerDecks.setHeaderCaption(localizer.getMessage("lblSelectOathbreakerDeckFor").replace("%s", txtPlayerName.getText()));
                Forge.openScreen(lstOathbreakerDecks);
            }
        });
        add(btnTinyLeadersDeck);
        btnTinyLeadersDeck.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                lstTinyLeadersDecks.setHeaderCaption(localizer.getMessage("lblSelectTinyLeadersDeckFor").replace("%s", txtPlayerName.getText()));
                Forge.openScreen(lstTinyLeadersDecks);
            }
        });
        add(btnBrawlDeck);
        btnBrawlDeck.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                lstBrawlDecks.setHeaderCaption(localizer.getMessage("lblSelectBrawlDeckFor").replace("%s", txtPlayerName.getText()));
                Forge.openScreen(lstBrawlDecks);
            }
        });
        add(btnSchemeDeck);
        btnSchemeDeck.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                lstSchemeDecks.setHeaderCaption(localizer.getMessage("lblSelectSchemeDeckFor").replace("%s", txtPlayerName.getText()));
                Forge.openScreen(lstSchemeDecks);
            }
        });
        add(btnPlanarDeck);
        btnPlanarDeck.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                lstPlanarDecks.setHeaderCaption(localizer.getMessage("lblSelectPlanarDeckFor").replace("%s", txtPlayerName.getText()));
                Forge.openScreen(lstPlanarDecks);
            }
        });
        add(btnVanguardAvatar);
        btnVanguardAvatar.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                lstVanguardAvatars.setHeaderCaption(localizer.getMessage("lblSelectVanguardFor").replace("%s", txtPlayerName.getText()));
                Forge.openScreen(lstVanguardAvatars);
            }
        });

        if (mayEdit == mayEdit0) {
            updateVariantControlsVisibility();
        }
        else {
            setMayEdit(false);
        }
        setMayControl(mayControl0);
    }

    public void initialize(FPref savedStateSetting, FPref savedStateSettingCommander, FPref savedStateSettingOathbreaker, FPref savedStateSettingTinyLeader, FPref savedStateSettingBrawl, DeckType defaultDeckType) {
        deckChooser.initialize(savedStateSetting, defaultDeckType);
        lstCommanderDecks.initialize(savedStateSettingCommander, DeckType.COMMANDER_DECK);
        lstOathbreakerDecks.initialize(savedStateSettingOathbreaker, DeckType.OATHBREAKER_DECK);
        lstTinyLeadersDecks.initialize(savedStateSettingTinyLeader, DeckType.TINY_LEADERS_DECK);
        lstBrawlDecks.initialize(savedStateSettingBrawl, DeckType.BRAWL_DECK);
        lstPlanarDecks.initialize(null, DeckType.RANDOM_DECK);
        lstSchemeDecks.initialize(null, DeckType.RANDOM_DECK);
    }

    @Override
    protected void doLayout(float width, float height) {
        float x = PADDING;
        float y = PADDING;
        float fieldHeight = txtPlayerName.getHeight();
        float avatarSize = 2 * fieldHeight + PADDING;
        float sleeveSize = 2 * fieldHeight + PADDING;
        float sleeveSizeW = (sleeveSize/4)*3;
        float dy = fieldHeight + PADDING;

        avatarLabel.setBounds(x, y, avatarSize, avatarSize);
        x += avatarSize + PADDING;
        sleeveLabel.setBounds(x, y, sleeveSizeW, sleeveSize);
        x += sleeveSizeW + PADDING;
        float w = width - x - fieldHeight - 2 * PADDING;
        txtPlayerName.setBounds(x, y, w, fieldHeight); //add space for card back
        x += w + PADDING;
        nameRandomiser.setBounds(x, y, fieldHeight, fieldHeight);

        if (Forge.isLandscapeMode()){
            y += dy;
            humanAiSwitch.setSize(humanAiSwitch.getAutoSizeWidth(fieldHeight), fieldHeight);
            x = width - humanAiSwitch.getWidth() - PADDING;
            humanAiSwitch.setPosition(x, y);
            w = x - (avatarSize+sleeveSizeW+PADDING) - 3 * PADDING;
            x = (avatarSize+sleeveSizeW+PADDING) + 2 * PADDING;
            if (cbArchenemyTeam.isVisible()) {
                cbArchenemyTeam.setBounds(x, y, w, fieldHeight);
            }
            else {
                cbTeam.setBounds(x, y, w, fieldHeight);
            }
        } else {
            y += dy;
            w = x - (avatarSize+sleeveSizeW+PADDING) - 3 * PADDING;
            x = (avatarSize+sleeveSizeW+PADDING) + 2 * PADDING;
            if (cbArchenemyTeam.isVisible()) {
                cbArchenemyTeam.setBounds(x, y, w, fieldHeight);
            }
            else {
                cbTeam.setBounds(x, y, w, fieldHeight);
            }
            y += dy;

            humanAiSwitch.setSize(humanAiSwitch.getAutoSizeWidth(fieldHeight), fieldHeight);
            x = width - humanAiSwitch.getWidth() - PADDING;
            humanAiSwitch.setPosition(x, y);
        }


        if (devModeSwitch.isVisible()) {
            if(Forge.isLandscapeMode())
                y += dy;
            devModeSwitch.setSize(devModeSwitch.getAutoSizeWidth(fieldHeight), fieldHeight);
            devModeSwitch.setPosition(0, y);
        }

        if (Forge.isLandscapeMode()) {
            y += dy;
            x = PADDING;
            w = width - 2 * PADDING;
        } else {
            if (devModeSwitch.isVisible()) {
                y += dy;
                x = PADDING;
                w = width - 2 * PADDING;
            } else {
                x = PADDING;
                w = (width - 2 * PADDING) - humanAiSwitch.getWidth();
            }
        }

        if (btnCommanderDeck.isVisible()) {
            btnCommanderDeck.setBounds(x, y, w, fieldHeight);
            y += dy;
        }
        else if (btnOathbreakDeck.isVisible()) {
            btnOathbreakDeck.setBounds(x, y, w, fieldHeight);
            y += dy;
        }
        else if (btnTinyLeadersDeck.isVisible()) {
            btnTinyLeadersDeck.setBounds(x, y, w, fieldHeight);
            y += dy;
        }
        else if (btnBrawlDeck.isVisible()) {
            btnBrawlDeck.setBounds(x, y, w, fieldHeight);
            y += dy;
        }
        else if (btnDeck.isVisible()) {
            btnDeck.setBounds(x, y, w, fieldHeight);
            y += dy;
        }
        if (btnSchemeDeck.isVisible()) {
            btnSchemeDeck.setBounds(x, y, w, fieldHeight);
            y += dy;
        }
        if (btnPlanarDeck.isVisible()) {
            btnPlanarDeck.setBounds(x, y, w, fieldHeight);
            y += dy;
        }
        if (btnVanguardAvatar.isVisible()) {
            btnVanguardAvatar.setBounds(x, y, w, fieldHeight);
        }
    }

    public float getPreferredHeight() {
        int rows = 3;
        if (!btnDeck.isVisible()) {
            if(Forge.isLandscapeMode())
                rows--;
        }
        if (btnCommanderDeck.isVisible() || btnOathbreakDeck.isVisible() || btnTinyLeadersDeck.isVisible() || btnBrawlDeck.isVisible()) {
            if(Forge.isLandscapeMode())
                rows++;
        }
        if (btnSchemeDeck.isVisible()) {
            rows++;
        }
        if (btnPlanarDeck.isVisible()) {
            rows++;
        }
        if (btnVanguardAvatar.isVisible()) {
            rows++;
        }
        if (devModeSwitch.isVisible()) {
            rows++;
        }
        return rows * (txtPlayerName.getHeight() + PADDING) + PADDING;
    }

    @Override
    protected void drawOverlay(Graphics g) {
        float y = getHeight() - FList.LINE_THICKNESS / 2;
        g.drawLine(FList.LINE_THICKNESS, FList.LINE_COLOR, 0, y, getWidth(), y);
    }

    private final FEventHandler humanAiSwitched = new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            boolean toggled = humanAiSwitch.isToggled();
            if (allowNetworking) {
                setIsReady(toggled);
                screen.setReady(index, toggled);
            }
            else {
                type = toggled ? LobbySlotType.AI : LobbySlotType.LOCAL;
                onIsAiChanged(toggled);

                LobbySlot slot = screen.getLobby().getSlot(index);
                slot.setType(type);
                screen.update(index,type);

                //update may edit in-case it changed as a result of the AI change
                setMayEdit(screen.getLobby().mayEdit(index));
                setAvatarIndex(slot.getAvatarIndex());
                setSleeveIndex(slot.getSleeveIndex());
                setPlayerName(slot.getName());
            }
        }
    };

    private final FEventHandler devModeSwitched = new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            boolean toggled = devModeSwitch.isToggled();
            prefs.setPref(FPref.DEV_MODE_ENABLED, String.valueOf(toggled));
            ForgePreferences.DEV_MODE = toggled;

            // ensure that preferences panel reflects the change
            prefs.save();

            screen.setDevMode(index);
        }
    };

    private void onIsAiChanged(boolean isAi) {
        deckChooser.setIsAi(isAi);
        lstCommanderDecks.setIsAi(isAi);
        lstTinyLeadersDecks.setIsAi(isAi);
        lstBrawlDecks.setIsAi(isAi);
        lstPlanarDecks.setIsAi(isAi);
        lstSchemeDecks.setIsAi(isAi);
        lstVanguardAvatars.setIsAi(isAi);
    }

    //Listens to name text fields and gives the appropriate player focus.
    //Also saves the name preference when leaving player one's text field. */
    private FEventHandler nameChangedHandler = new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            final Object source = e.getSource();
            if (source instanceof FTextField) { // the text box
                FTextField nField = (FTextField)source;
                String newName = nField.getText().trim();
                if (index == 0 && !StringUtils.isBlank(newName)
                        && StringUtils.isAlphanumericSpace(newName) && prefs.getPref(FPref.PLAYER_NAME) != newName) {
                    prefs.setPref(FPref.PLAYER_NAME, newName);
                    prefs.save();
                    if (allowNetworking) {
                        screen.firePlayerChangeListener(index);
                    }
                }
            }
        }
    };

    private FEventHandler avatarCommand = new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            AvatarSelector.show(getPlayerName(), avatarIndex, screen.getUsedAvatars(), new Callback<Integer>() {
                @Override
                public void run(Integer result) {
                    setAvatarIndex(result);

                    if (index < 2) {
                        screen.updateAvatar(index, result);
                        screen.updateAvatarPrefs();
                    }
                    if (allowNetworking) {
                        screen.firePlayerChangeListener(index);
                    }
                }
            });
        }
    };

    private FEventHandler sleeveCommand = new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            SleevesSelector.show(getPlayerName(), sleeveIndex, screen.getUsedSleeves(), new Callback<Integer>() {
                @Override
                public void run(Integer result) {
                    setSleeveIndex(result);

                    if (index < 2) {
                        screen.updateSleeve(index, result);
                        screen.updateSleevePrefs();
                    }
                    if (allowNetworking) {
                        screen.firePlayerChangeListener(index);
                    }
                }
            });
        }
    };

    public void setDeckSelectorButtonText(String text) {
        if (!Forge.isLandscapeMode())
            text = TextUtil.fastReplace(text, ": ", ":\n");
        if (btnDeck.isVisible())
            btnDeck.setText(text);

        if (btnCommanderDeck.isVisible())
            btnCommanderDeck.setText(text);

        if (btnOathbreakDeck.isVisible())
            btnOathbreakDeck.setText(text);

        if (btnTinyLeadersDeck.isVisible())
            btnTinyLeadersDeck.setText(text);

        if (btnBrawlDeck.isVisible())
            btnBrawlDeck.setText(text);
    }

    public void setVanguarAvatarName(String text) {
        if (!Forge.isLandscapeMode())
            text = TextUtil.fastReplace(text, ": ", ":\n");
        btnVanguardAvatar.setText(text);
    }

    public void setSchemeDeckName(String text) {
        if (!Forge.isLandscapeMode())
            text = TextUtil.fastReplace(text, ": ", ":\n");
        btnSchemeDeck.setText(text);
    }

    public void setPlanarDeckName(String text) {
        if (!Forge.isLandscapeMode())
            text = TextUtil.fastReplace(text, ": ", ":\n");
        btnPlanarDeck.setText(text);
    }

    public void updateVariantControlsVisibility() {
        boolean isCommanderApplied = false;
        boolean isOathbreakerApplied = false;
        boolean isTinyLeadersApplied = false;
        boolean isBrawlApplied = false;
        boolean isPlanechaseApplied = false;
        boolean isVanguardApplied = false;
        boolean isArchenemyApplied = false;
        boolean archenemyVisiblity = false;
        boolean isDeckBuildingAllowed = mayEdit;
        boolean replacedbasicdeck = false;

        for (GameType variant : screen.getLobby().getAppliedVariants()) {
            switch (variant) {
            case Archenemy:
                isArchenemyApplied = true;
                if (isArchenemy()) {
                    archenemyVisiblity = true;
                }
                break;
            case ArchenemyRumble:
                archenemyVisiblity = true;
                break;
            case Commander:
                isCommanderApplied = true;
                isDeckBuildingAllowed = false; //Commander deck replaces basic deck, so hide that
                replacedbasicdeck = true;
                break;
            case Oathbreaker:
                isOathbreakerApplied = true;
                isDeckBuildingAllowed = false; //Oathbreaker deck replaces basic deck, so hide that
                replacedbasicdeck = true;
                break;
            case TinyLeaders:
                isTinyLeadersApplied = true;
                isDeckBuildingAllowed = false; //Tiny Leaders deck replaces basic deck, so hide that
                replacedbasicdeck = true;
                break;
            case Brawl:
                isBrawlApplied = true;
                isDeckBuildingAllowed = false; //Tiny Leaders deck replaces basic deck, so hide that
                replacedbasicdeck = true;
                break;
            case Planechase:
                isPlanechaseApplied = true;
                break;
            case Vanguard:
                isVanguardApplied = true;
                break;
            default:
                if (variant.isAutoGenerated()) {
                    isDeckBuildingAllowed = false;
                    replacedbasicdeck = true;
                }
                break;
            }
        }

        if(allowNetworking) {
            if (replacedbasicdeck) {
                btnDeck.setVisible(false);
            } else {
                btnDeck.setVisible(true);
                btnDeck.setEnabled(mayEdit);
            }
            if (isCommanderApplied) {
                btnCommanderDeck.setVisible(true);
                btnCommanderDeck.setEnabled(mayEdit);
            } else {
                btnCommanderDeck.setVisible(false);
            }
            if (isOathbreakerApplied) {
                btnOathbreakDeck.setVisible(true);
                btnOathbreakDeck.setEnabled(mayEdit);
            } else {
                btnOathbreakDeck.setVisible(false);
            }
            if (isTinyLeadersApplied) {
                btnTinyLeadersDeck.setVisible(true);
                btnTinyLeadersDeck.setEnabled(mayEdit);
            } else {
                btnTinyLeadersDeck.setVisible(false);
            }
            if (isBrawlApplied) {
                btnBrawlDeck.setVisible(true);
                btnBrawlDeck.setEnabled(mayEdit);
            } else {
                btnBrawlDeck.setVisible(false);
            }
            if (archenemyVisiblity) {
                btnSchemeDeck.setVisible(true);
                btnSchemeDeck.setEnabled(mayEdit);
            } else {
                btnSchemeDeck.setVisible(false);
            }
            if (!isArchenemyApplied) {
                cbTeam.setVisible(true);
                cbTeam.setEnabled(mayEdit);
                cbArchenemyTeam.setVisible(false);
            } else {
                cbTeam.setVisible(false);
                cbArchenemyTeam.setVisible(true);
                cbArchenemyTeam.setEnabled(mayEdit);
            }
            if (isPlanechaseApplied) {
                btnPlanarDeck.setVisible(true);
                btnPlanarDeck.setEnabled(mayEdit);
            } else {
                btnPlanarDeck.setVisible(false);
            }
            if (isVanguardApplied) {
                btnVanguardAvatar.setVisible(true);
                btnVanguardAvatar.setEnabled(mayEdit);
            } else {
                btnVanguardAvatar.setVisible(false);
            }
        } else {
            btnDeck.setVisible(isDeckBuildingAllowed);
            btnCommanderDeck.setVisible(isCommanderApplied && mayEdit);
            btnOathbreakDeck.setVisible(isOathbreakerApplied && mayEdit);
            btnTinyLeadersDeck.setVisible(isTinyLeadersApplied && mayEdit);
            btnBrawlDeck.setVisible(isBrawlApplied && mayEdit);

            btnSchemeDeck.setVisible(archenemyVisiblity && mayEdit);

            cbTeam.setVisible(!isArchenemyApplied);
            cbArchenemyTeam.setVisible(isArchenemyApplied);

            btnPlanarDeck.setVisible(isPlanechaseApplied && mayEdit);
            btnVanguardAvatar.setVisible(isVanguardApplied && mayEdit);
        }
    }

    public boolean isNetworkHost() {
        return allowNetworking && index == 0;
    }

    public boolean isAi() {
        return type == LobbySlotType.AI;
    }

    public boolean isArchenemy() {
        return cbArchenemyTeam.getSelectedIndex() == 0;
    }
    public void setIsArchenemy(boolean isArchenemy0) {
        cbArchenemyTeam.setSelectedIndex(isArchenemy0 ? 0 : 1);
    }

    private void populateTeamsComboBoxes() {
        cbArchenemyTeam.addItem(localizer.getMessage("lblArchenemy"));
        cbArchenemyTeam.addItem(localizer.getMessage("lblHeroes"));

        for (int i = 1; i <= LobbyScreen.MAX_PLAYERS; i++) {
            cbTeam.addItem(localizer.getMessage("lblTeam") + " " + i);
        }
        cbTeam.setEnabled(mayEdit);
    }

    private FEventHandler teamChangedHandler = new FEventHandler() {
        @Override
        public void handleEvent(FEvent e) {
            @SuppressWarnings("unchecked")
            FComboBox<Object> cb = (FComboBox<Object>)e.getSource();
            if (cb.getSelectedIndex() == -1) {
                return;
            }
            if (screen.hasVariant(GameType.Archenemy)) {
                String sel = (String) cb.getSelectedItem();
                if (sel.contains("Archenemy")) {
                    screen.lastArchenemy = index;
                    for (PlayerPanel pp : screen.getPlayerPanels()) {
                        int i = pp.index;
                        int team = i == screen.lastArchenemy ? 0 : 1;
                        pp.setArchenemyTeam(team);
                        pp.toggleIsPlayerArchenemy();
                    }
                }
            } else {
                screen.updatemyTeam(index, getTeam());
            }
        }
    };

    public void toggleIsPlayerArchenemy() {
        if (screen.hasVariant(GameType.Archenemy)) {
            setIsArchenemy(screen.lastArchenemy == index);
        }
        else {
            setIsArchenemy(screen.hasVariant(GameType.ArchenemyRumble));
        }
        screen.updateLayoutForVariants();
    }

    private FLabel createNameRandomizer() {
        final FLabel newNameBtn = new FLabel.Builder().iconInBackground(false)
                .icon(Forge.hdbuttons ? FSkinImage.HDEDIT : FSkinImage.EDIT).opaque(false).build();
        newNameBtn.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                getNewName(new Callback<String>() {
                    @Override
                    public void run(String newName) {
                        if (newName == null) { return; }

                        txtPlayerName.setText(newName);

                        if (index == 0) {
                            prefs.setPref(FPref.PLAYER_NAME, newName);
                            prefs.save();
                        }
                        if (allowNetworking) {
                            screen.firePlayerChangeListener(index);
                        }
                    }
                });
            }
        });
        return newNameBtn;
    }

    private void createNameEditor() {
        String name;
        if (index == 0) {
            name = FModel.getPreferences().getPref(FPref.PLAYER_NAME);
            if (name.isEmpty()) {
                name = localizer.getMessage("lblHuman");
            }
        }
        else {
            name = NameGenerator.getRandomName("Any", "Any", screen.getPlayerNames());
        }

        txtPlayerName.setText(name);
        txtPlayerName.setFont(LABEL_FONT);
        txtPlayerName.setChangedHandler(nameChangedHandler);
    }

    private void createAvatar() {
        String[] currentPrefs = prefs.getPref(FPref.UI_AVATARS).split(",");
        if (index < currentPrefs.length) {
            setAvatarIndex(Integer.parseInt(currentPrefs[index]));
        }
        else {
            setAvatarIndex(AvatarSelector.getRandomAvatar(screen.getUsedAvatars()));
        }
        avatarLabel.setCommand(avatarCommand);
    }

    private void createSleeve() {
        String[] currentPrefs = prefs.getPref(FPref.UI_SLEEVES).split(",");
        if (index < currentPrefs.length) {
            setSleeveIndex(Integer.parseInt(currentPrefs[index]));
        }
        else {
            setSleeveIndex(SleevesSelector.getRandomSleeves(screen.getUsedSleeves()));
        }
        sleeveLabel.setCommand(sleeveCommand);
    }

    public void setAvatarIndex(int newAvatarIndex) {
        avatarIndex = newAvatarIndex;
        if (avatarIndex != -1) {
            avatarLabel.setIcon(new FTextureRegionImage(FSkin.getAvatars().get(newAvatarIndex)));
        }
        else {
            avatarLabel.setIcon(null);
        }
    }

    public void setSleeveIndex(int newSleeveIndex) {
        sleeveIndex = newSleeveIndex;
        if (sleeveIndex != -1) {
            sleeveLabel.setIcon(new FTextureRegionImage(FSkin.getSleeves().get(newSleeveIndex)));
        }
        else {
            sleeveLabel.setIcon(null);
        }
    }

    public int getAvatarIndex() {
        return avatarIndex;
    }

    public int getSleeveIndex() {
        return sleeveIndex;
    }

    public void setPlayerName(String string) {
        txtPlayerName.setText(string);
    }

    public String getPlayerName() {
        return txtPlayerName.getText();
    }

    public LobbySlotType getType() {
        return type;
    }
    public void setType(LobbySlotType type0) {
        if (type == type0) { return; }

        boolean wasAi = isAi();

        type = type0;

        switch (type) {
        case LOCAL:
            humanAiSwitch.setToggled(false);
            break;
        case AI:
            humanAiSwitch.setToggled(true);
            break;
        case OPEN:
            isReady = false;
            humanAiSwitch.setToggled(false);
            break;
        case REMOTE:
            humanAiSwitch.setToggled(isReady);
            break;
        }

        boolean isAi = isAi();
        if (isAi != wasAi && deckChooser != null) {
            onIsAiChanged(isAi);
        }
    }

    public Set<AIOption> getAiOptions() {
        return isSimulatedAi()
                ? ImmutableSet.of(AIOption.USE_SIMULATION)
                : Collections.emptySet();
    }
    private boolean isSimulatedAi() {
        return isAi() && useAiSimulation;
    }
    public void setUseAiSimulation(final boolean useAiSimulation0) {
        useAiSimulation = useAiSimulation0;
    }

    public int getTeam() {
        return cbTeam.getSelectedIndex();
    }
    public void setTeam(int team0) {
        cbTeam.setSelectedIndex(team0);
    }

    public int getArchenemyTeam() {
        return cbTeam.getSelectedIndex();
    }
    public void setArchenemyTeam(int team0) {
        cbTeam.setSelectedIndex(team0);
    }

    public boolean isReady() {
        return isReady;
    }
    public void setIsReady(boolean isReady0) {
        if (isReady == isReady0) { return; }
        isReady = isReady0;
        if (allowNetworking) {
            humanAiSwitch.setToggled(isReady);
        }
    }

    public void setMayEdit(boolean mayEdit0) {
        if (mayEdit == mayEdit0) { return; }
        mayEdit = mayEdit0;
        avatarLabel.setEnabled(mayEdit);
        sleeveLabel.setEnabled(mayEdit);
        txtPlayerName.setEnabled(mayEdit);
        nameRandomiser.setEnabled(mayEdit);
        humanAiSwitch.setEnabled(mayEdit);
        cbTeam.setEnabled(mayEdit);
        if (devModeSwitch != null) {
            devModeSwitch.setEnabled(mayEdit);
        }
        if(allowNetworking) {
            btnDeck.setEnabled(mayEdit);
            btnCommanderDeck.setEnabled(mayEdit);
            btnOathbreakDeck.setEnabled(mayEdit);
            btnTinyLeadersDeck.setEnabled(mayEdit);
            btnBrawlDeck.setEnabled(mayEdit);
            btnSchemeDeck.setEnabled(mayEdit);
            btnPlanarDeck.setEnabled(mayEdit);
            cbArchenemyTeam.setEnabled(mayEdit);
        }
        updateVariantControlsVisibility();

        //if panel has height already, ensure height updated to account for button visibility changes
        if (getHeight() > 0) {
            screen.getPlayersScroll().revalidate();
        }
    }

    public void setMayControl(boolean mayControl0) {
        if (mayControl == mayControl0) { return; }
        mayControl = mayControl0;
    }

    public void setMayRemove(boolean mayRemove0) {
        if (mayRemove == mayRemove0) { return; }
        mayRemove = mayRemove0;
    }

    public FDeckChooser getDeckChooser() {
        return deckChooser;
    }

    public FDeckChooser getCommanderDeckChooser() {
        return lstCommanderDecks;
    }

    public FDeckChooser getOathbreakerDeckChooser() {
        return lstOathbreakerDecks;
    }

    public FDeckChooser getTinyLeadersDeckChooser() {
        return lstTinyLeadersDecks;
    }

    public FDeckChooser getBrawlDeckChooser() {
        return lstBrawlDecks;
    }

    public Deck getDeck() {
        return deckChooser.getDeck();
    }

    public Deck getCommanderDeck() { return lstCommanderDecks.getDeck(); }

    public Deck getOathbreakerDeck() {
        return lstOathbreakerDecks.getDeck();
    }

    public Deck getTinyLeadersDeck() {
        return lstTinyLeadersDecks.getDeck();
    }

    public Deck getBrawlDeck() {
        return lstBrawlDecks.getDeck();
    }

    public Deck getSchemeDeck() {
        return lstSchemeDecks.getDeck();
    }

    public Deck getPlanarDeck() {
        return lstPlanarDecks.getDeck();
    }

    public PaperCard getVanguardAvatar() {
        return lstVanguardAvatars.getLstVanguards().getSelectedItem();
    }

    /** Adds a pre-styled FLabel component with the specified title. */
    private FLabel newLabel(String title) {
        return new FLabel.Builder().text(title).font(LABEL_FONT).align(Align.right).build();
    }
    
    private static final ImmutableList<String> genderOptions = ImmutableList.of(Localizer.getInstance().getMessage("lblMale"), Localizer.getInstance().getMessage("lblFemale"), Localizer.getInstance().getMessage("lblAny"));
    private static final ImmutableList<String> typeOptions   = ImmutableList.of(Localizer.getInstance().getMessage("lblFantasy"), Localizer.getInstance().getMessage("lblGeneric"), Localizer.getInstance().getMessage("lblAny"));
    private final void getNewName(final Callback<String> callback) {
        final String title = localizer.getMessage("lblGetNewRandomName");
        final String message = localizer.getMessage("lbltypeofName");
        final FSkinImage icon = FOptionPane.QUESTION_ICON;

        FOptionPane.showOptionDialog(message, title, icon, genderOptions, 2, new Callback<Integer>() {
            @Override
            public void run(final Integer genderIndex) {
                if (genderIndex == null || genderIndex < 0) {
                    callback.run(null);
                    return;
                }
                
                FOptionPane.showOptionDialog(message, title, icon, typeOptions, 2, new Callback<Integer>() {
                    @Override
                    public void run(final Integer typeIndex) {
                        if (typeIndex == null || typeIndex < 0) {
                            callback.run(null);
                            return;
                        }

                        generateRandomName(genderOptions.get(genderIndex), typeOptions.get(typeIndex), screen.getPlayerNames(), title, callback);
                    }
                });
            }
        });
    }

    private void generateRandomName(final String gender, final String type, final List<String> usedNames, final String title, final Callback<String> callback) {
        final String newName = NameGenerator.getRandomName(gender, type, usedNames);
        String confirmMsg = localizer.getMessage("lblconfirmName").replace("%s", newName);
        FOptionPane.showConfirmDialog(confirmMsg, title, localizer.getMessage("lblUseThisName"), localizer.getMessage("lblTryAgain"), true, new Callback<Boolean>() {
            @Override
            public void run(Boolean result) {
                if (result) {
                    callback.run(newName);
                }
                else {
                    generateRandomName(gender, type, usedNames, title, callback);
                }
            }
        });
    }

    public boolean isDevMode() {
        return devModeSwitch.isVisible() && devModeSwitch.isToggled();
    }
    public void setIsDevMode(final boolean isDevMode) {
        if (devModeSwitch.isVisible()) {
            devModeSwitch.setToggled(isDevMode);
        }
    }
}
