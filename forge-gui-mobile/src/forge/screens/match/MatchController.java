package forge.screens.match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import forge.adventure.scene.DuelScene;
import forge.adventure.util.Config;
import forge.ai.GameState;
import forge.deck.Deck;
import forge.game.player.Player;
import forge.game.player.PlayerController.FullControlFlag;
import forge.item.IPaperCard;
import forge.util.collect.FCollection;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import forge.Forge;
import forge.Graphics;
import forge.LobbyPlayer;
import forge.assets.FImage;
import forge.assets.FSkin;
import forge.assets.FSkinImage;
import forge.assets.FTextureRegionImage;
import forge.assets.ImageCache;
import forge.card.CardAvatarImage;
import forge.card.GameEntityPicker;
import forge.deck.CardPool;
import forge.deck.FSideboardDialog;
import forge.game.GameEntityView;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.IHasIcon;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.AbstractGuiGame;
import forge.gamemodes.match.HostedMatch;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.util.SGuiChoose;
import forge.gui.util.SOptionPane;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.menu.FCheckBoxMenuItem;
import forge.menu.FMenuItem;
import forge.menu.FPopupMenu;
import forge.model.FModel;
import forge.player.PlayerZoneUpdate;
import forge.player.PlayerZoneUpdates;
import forge.screens.match.views.VAssignCombatDamage;
import forge.screens.match.views.VAssignGenericAmount;
import forge.screens.match.views.VPhaseIndicator;
import forge.screens.match.views.VPlayerPanel;
import forge.screens.match.views.VPlayerPanel.InfoTab;
import forge.screens.match.views.VPrompt;
import forge.screens.match.winlose.ViewWinLose;
import forge.toolbox.FButton;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOptionPane;
import forge.trackable.TrackableCollection;
import forge.util.ITriggerEvent;
import forge.util.Localizer;
import forge.util.MessageUtil;
import forge.util.WaitCallback;
import forge.util.collect.FCollectionView;

public class MatchController extends AbstractGuiGame {
    private MatchController() { }
    public static final MatchController instance = new MatchController();

    private static HostedMatch hostedMatch;
    private static MatchScreen view;
    private static GameState phaseGameState;

    private GameState getPhaseGameState() {
        return phaseGameState;
    }

    private final Map<PlayerView, InfoTab> zonesToRestore = Maps.newHashMap();
    private final Map<PlayerView, InfoTab> lastZonesToRestore = Maps.newHashMap();

    public static MatchScreen getView() {
        return view;
    }

    @Override
    protected void updateCurrentPlayer(final PlayerView player) {
        for (final PlayerView other : getLocalPlayers()) {
            if (!other.equals(player)) {
                updatePromptForAwait(other);
            }
        }
    }

    public static Deck getPlayerDeck(final PlayerView playerView) {
        try {
            for (Player p : instance.getGameView().getGame().getPlayers()) {
                if (p.getView() == playerView) {
                    return p.getRegisteredPlayer().getDeck();
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public static FImage getPlayerAvatar(final PlayerView p) {
        final String lp = p.getLobbyPlayerName();
        FImage avatar = Forge.getAssets().avatarImages().get(lp);
        if (avatar == null) {
            if (StringUtils.isEmpty(p.getAvatarCardImageKey())) {
                avatar = new FTextureRegionImage(FSkin.getAvatars().get(p.getAvatarIndex()));
            } else { //handle lobby players with art from cards
                avatar = new CardAvatarImage(p.getAvatarCardImageKey());
            }
        }
        return avatar;
    }

    public static FImage getPlayerSleeve(final PlayerView p) {
        if (p == null)
            return FSkinImage.UNKNOWN;
        return new FTextureRegionImage(FSkin.getSleeves().get(p.getSleeveIndex()));
    }

    @Override
    public void refreshCardDetails(final Iterable<CardView> cards) {
        for (final VPlayerPanel pnl : view.getPlayerPanels().values()) {
            //ensure cards appear in the correct row of the field
            pnl.getField().update(true);
            //ensure flashback zone has updated info ie Snapcaster Mage, etc..
            pnl.updateZone(ZoneType.Flashback);
        }
    }

    @Override
    public void refreshField() {
        if(!GuiBase.isNetworkplay())
            return;
        refreshCardDetails(null);
    }

    @Override
    public GameState getGamestate() {
        return getPhaseGameState();
    }

    public boolean hotSeatMode() {
        return FModel.getPreferences().getPrefBoolean(FPref.MATCH_HOT_SEAT_MODE);
    }

    @Override
    public void openView(final TrackableCollection<PlayerView> myPlayers) {
        final boolean noHumans = !hasLocalPlayers();

        FCollectionView<PlayerView> players = getGameView().getPlayers();
        if (players.size() == 2 && myPlayers != null && myPlayers.size() == 1 && myPlayers.get(0).equals(players.get(1))) {
            players = new FCollection<>(new PlayerView[]{players.get(1), players.get(0)});
        }
        final List<VPlayerPanel> playerPanels = new ArrayList<>();
        boolean init = false;
        for (final PlayerView p : players) {
            final boolean isLocal = isLocalPlayer(p);
            final VPlayerPanel playerPanel = new VPlayerPanel(p, isLocal || noHumans, players.size());
            if (isLocal && !init) {
                playerPanels.add(0, playerPanel); //ensure local player always first among player panels
                playerPanel.setBottomPlayer(true);
                init = true;
            }
            else {
                playerPanels.add(playerPanel);
                if (playerPanel.equals(playerPanels.get(0)))
                    playerPanel.setBottomPlayer(true);
            }
        }
        view = new MatchScreen(playerPanels);
        if(GuiBase.isNetworkplay())
            view.resetFields();
        clearSelectables();  //fix uncleared selection

        if (noHumans) {
            //add special object that pauses game if screen touched
            view.add(new FDisplayObject() {
                @Override
                public void draw(final Graphics g) {
                    //don't draw anything
                }

                @Override
                public void buildTouchListeners(final float screenX, final float screenY, final List<FDisplayObject> listeners) {
                    if (screenY < view.getHeight() - VPrompt.HEIGHT) {
                        hostedMatch.pause();
                    }
                }
            });
        }

        actuateMatchPreferences();
        //reset daytime every match
        updateDayTime(null);
        Forge.openScreen(view);
    }

    @Override
    public void showPromptMessage(final PlayerView player, final String message) {
        view.getPrompt(player).setMessage(message);
    }

    @Override
    public void showCardPromptMessage(final PlayerView player, final String message, final CardView card) {
        view.getPrompt(player).setMessage(message, card);
    }

    @Override
    public void updateButtons(final PlayerView owner, final String label1, final String label2, final boolean enable1, final boolean enable2, final boolean focus1) {
        final VPrompt prompt = view.getPrompt(owner);
        final FButton btn1 = prompt.getBtnOk(), btn2 = prompt.getBtnCancel();
        btn1.setText(label1);
        btn2.setText(label2);
        btn1.setEnabled(enable1);
        btn2.setEnabled(enable2);
    }

    @Override
    public void flashIncorrectAction() {
        //SDisplayUtil.remind(VPrompt.SINGLETON_INSTANCE); //TODO
    }
    @Override
    public void alertUser() {
        //TODO
    }
    private PlayerView lastPlayer;
    @Override
    public void updatePhase(boolean saveState) {
        final PhaseType ph = getGameView().getPhase();
        if (ph != null) {
            if (ph.isBefore(PhaseType.END_OF_TURN))
                lastPlayer = getGameView().getPlayerTurn();
            //reset phase labels
            view.resetAllPhaseButtons();
            if (lastPlayer != null && PhaseType.CLEANUP.equals(ph)) {
                //set phaselabel
                final VPhaseIndicator.PhaseLabel phaseLabel = view.getPlayerPanel(lastPlayer).getPhaseIndicator().getLabel(ph);
                if (phaseLabel != null)
                    phaseLabel.setActive(true);
                if (GuiBase.isNetworkplay())
                    getGameView().updateNeedsPhaseRedrawn(lastPlayer, PhaseType.CLEANUP);
            } else if (getGameView().getPlayerTurn() != null) {
                //set phaselabel
                final VPhaseIndicator.PhaseLabel phaseLabel = view.getPlayerPanel(getGameView().getPlayerTurn()).getPhaseIndicator().getLabel(ph);
                if (phaseLabel != null)
                    phaseLabel.setActive(true);
                if (GuiBase.isNetworkplay())
                    getGameView().updateNeedsPhaseRedrawn(getGameView().getPlayerTurn(), ph);
            }
        }

        if(GuiBase.isNetworkplay())
            checkStack();

        if (ph != null && saveState && ph.isMain()) {
            phaseGameState = new GameState() {
                @Override
                public IPaperCard getPaperCard(final String cardName, final String setCode, final int artID) {
                    return FModel.getMagicDb().getCommonCards().getCard(cardName, setCode, artID);
                }
            };
            try {
                phaseGameState.initFromGame(getGameView().getGame());
            } catch (Exception e) {
            }
        }
    }


    public void checkStack() {
        view.getStack().checkEmptyStack();
    }

    public void showWinlose() {
        if (view.getViewWinLose() != null)
            view.getViewWinLose().setVisible(true);
    }

    @Override
    public void updateTurn(final PlayerView player) {
    }

    @Override
    public void updatePlayerControl() {
        //show/hide hand for top player based on whether the opponent is controlled
        if (getLocalPlayerCount() == 1) {
            final PlayerView player = view.getTopPlayerPanel().getPlayer();
            if (player.getMindSlaveMaster() != null) {
                view.getTopPlayerPanel().setSelectedZone(ZoneType.Hand);
            } else {
                view.getTopPlayerPanel().setSelectedTab(null);
            }
        }
    }

    @Override
    public void disableOverlay() {
    }

    @Override
    public void enableOverlay() {
    }

    @Override
    public void finishGame() {
        if (Forge.isMobileAdventureMode) {
            if (Config.instance().getSettingData().disableWinLose) {
                MatchController.writeMatchPreferences();
                if (getGameView().isMatchOver()){
                    Forge.setCursor(null, "0");
                    DuelScene.instance().GameEnd();
                    if (!DuelScene.instance().hasCallbackExit())
                        DuelScene.instance().exitDuelScene();

                    return;
                }
                else{
                    try { MatchController.getHostedMatch().continueMatch();
                    } catch (NullPointerException e) {}
                    return;
                }
            }
        }
        if (hasLocalPlayers() || getGameView().isMatchOver()) {
            view.setViewWinLose(new ViewWinLose(getGameView()));
            view.getViewWinLose().setVisible(true);
        }
    }

    @Override
    public void updateStack() {
        view.getStack().update();
    }

    @Override
    public void setPanelSelection(final CardView c) {
        //GuiUtils.setPanelSelection(c); //TODO
    }

    @Override
    public SpellAbilityView getAbilityToPlay(final CardView hostCard, final List<SpellAbilityView> abilities, final ITriggerEvent triggerEvent) {
        if (abilities.isEmpty()) {
            return null;
        }
        if (abilities.size() == 1) {
            return abilities.get(0);
        }
        return SGuiChoose.oneOrNone(Forge.getLocalizer().getMessage("lblChooseAbilityToPlay"), abilities);
    }

    @Override
    public void showCombat() {
    }

    @Override
    public void showManaPool(final PlayerView player) {
        final VPlayerPanel playerPanel = view.getPlayerPanel(player);
        final InfoTab selectedTab = playerPanel.getSelectedTab(), manaPoolTab = playerPanel.getManaPoolTab();
        if (!manaPoolTab.equals(selectedTab)) {
            //if mana pool was selected previously, we don't need to switch back to anything
            zonesToRestore.put(player, selectedTab);
        }
        playerPanel.setSelectedTab(manaPoolTab);
    }

    @Override
    public void hideManaPool(final PlayerView player) {
        final VPlayerPanel playerPanel = view.getPlayerPanel(player);
        // value may be null so explicit containsKey call is necessary
        final boolean doRestore = zonesToRestore.containsKey(player);
        final InfoTab zoneToRestore = zonesToRestore.remove(player);
        if (!playerPanel.getManaPoolTab().equals(playerPanel.getSelectedTab()) || !doRestore) {
            return; //if player switched away from mana pool already, don't change anything
        }

        playerPanel.setSelectedTab(zoneToRestore);
    }

    @Override
    public PlayerZoneUpdates openZones(PlayerView controller, final Collection<ZoneType> zones, final Map<PlayerView, Object> playersWithTargetables, boolean backupLastZones) {
        PlayerZoneUpdates updates = new PlayerZoneUpdates();
        if (zones.size() == 1) {
            final ZoneType zoneType = zones.iterator().next();
            switch (zoneType) {
                case Battlefield:
                case Command:
                    playersWithTargetables.clear(); //clear since no zones need to be restored
                default:
                    lastZonesToRestore.clear();
                    //open zone tab for given zone if needed
                    boolean result = true;
                    for (final PlayerView player : playersWithTargetables.keySet()) {
                        final VPlayerPanel playerPanel = view.getPlayerPanel(player);
                        if (backupLastZones)
                            lastZonesToRestore.put(player, playerPanel.getSelectedTab());
                        playersWithTargetables.put(player, playerPanel.getSelectedTab()); //backup selected tab before changing it
                        updates.add(new PlayerZoneUpdate(player, zoneType));
                        playerPanel.setSelectedZone(zoneType);
                    }
            }
        }
        return updates;
    }

    @Override
    public void restoreOldZones(PlayerView playerView, PlayerZoneUpdates playerZoneUpdates) {
        for(PlayerZoneUpdate update : playerZoneUpdates) {
            PlayerView player = update.getPlayer();

            ZoneType zone = null;
            for(ZoneType type : update.getZones()) {
                zone = type;
                break;
            }

            final VPlayerPanel playerPanel = view.getPlayerPanel(player);
            if (zone == null) {
                playerPanel.hideSelectedTab();
                continue;
            }

            //final InfoTab zoneTab = playerPanel.getZoneTab(zone);
            //playerPanel.setSelectedTab(zoneTab);
        }
        for (Map.Entry<PlayerView, InfoTab> e : lastZonesToRestore.entrySet()) {
            if (e.getKey() != null && !e.getKey().getHasLost()) {
                final VPlayerPanel p = view.getPlayerPanel(e.getKey());
                p.setSelectedTab(e.getValue());
            }
        }
    }

    @Override
    public Map<CardView, Integer> assignCombatDamage(final CardView attacker, final List<CardView> blockers, final int damage, final GameEntityView defender, final boolean overrideOrder, final boolean maySkip) {
        return new WaitCallback<Map<CardView, Integer>>() {
            @Override
            public void run() {
                final VAssignCombatDamage v = new VAssignCombatDamage(attacker, blockers, damage, defender, overrideOrder, maySkip, this);
                v.show();
            }
        }.invokeAndWait();
    }

    @Override
    public Map<Object, Integer> assignGenericAmount(final CardView effectSource, final Map<Object, Integer> targets,
            final int amount, final boolean atLeastOne, final String amountLabel) {
        return new WaitCallback<Map<Object, Integer>>() {
            @Override
            public void run() {
                final VAssignGenericAmount v = new VAssignGenericAmount(effectSource, targets, amount, atLeastOne, amountLabel, this);
                v.show();
            }
        }.invokeAndWait();
    }

    @Override
    public void updateManaPool(final Iterable<PlayerView> manaPoolUpdate) {
        for (final PlayerView p : manaPoolUpdate) {
            view.getPlayerPanel(p).updateManaPool();
        }
    }

    @Override
    public void updateLives(final Iterable<PlayerView> livesUpdate) {
        for (final PlayerView p : livesUpdate) {
            view.getPlayerPanel(p).updateLife();
        }
    }

    @Override
    public void updateShards(final Iterable<PlayerView> livesUpdate) {
        for (final PlayerView p : livesUpdate) {
            view.getPlayerPanel(p).updateShards();
        }
    }

    @Override
    public void updateZones(final Iterable<PlayerZoneUpdate> zonesToUpdate) {
        view.updateZones(zonesToUpdate);
    }

    @Override
    public Iterable<PlayerZoneUpdate> tempShowZones(final PlayerView controller, final Iterable<PlayerZoneUpdate> zonesToUpdate) {
        return view.tempShowZones(controller, zonesToUpdate);
    }

    @Override
    public void hideZones(final PlayerView controller, final Iterable<PlayerZoneUpdate> zonesToUpdate) {
	    view.hideZones(controller, zonesToUpdate);
    }

    @Override
    public void updateCards(final Iterable<CardView> cards) {
        for (final CardView card : cards) {
            view.updateSingleCard(card);
        }
    }

    @Override
    public void setSelectables(final Iterable<CardView> cards) {
        super.setSelectables(cards);
        // update zones on tabletop and floating zones - non-selectable cards may be rendered differently
        FThreads.invokeInEdtNowOrLater(() -> {
            for (final PlayerView p : getGameView().getPlayers()) {
                if ( p.getCards(ZoneType.Battlefield) != null ) {
                    updateCards(p.getCards(ZoneType.Battlefield));
                }
                if ( p.getCards(ZoneType.Hand) != null ) {
                    updateCards(p.getCards(ZoneType.Hand));
                }
            }
        });
    }

    @Override
    public void clearSelectables() {
        super.clearSelectables();
        // update zones on tabletop and floating zones - non-selectable cards may be rendered differently
        FThreads.invokeInEdtNowOrLater(() -> {
            for (final PlayerView p : getGameView().getPlayers()) {
                if ( p.getCards(ZoneType.Battlefield) != null ) {
                    updateCards(p.getCards(ZoneType.Battlefield));
                }
                if ( p.getCards(ZoneType.Hand) != null ) {
                    updateCards(p.getCards(ZoneType.Hand));
                }
            }
        });
    }

    @Override
    public void afterGameEnd() {
        super.afterGameEnd();
        Forge.back(true);
        if (Forge.disposeTextures)
            ImageCache.getInstance().disposeTextures();
        //view = null;
    }

    public void resetPlayerPanels() {
        if (view != null)
            view.forceRevalidate();
    }

    private static void actuateMatchPreferences() {
        final ForgePreferences prefs = FModel.getPreferences();

        for (int i=0; i<view.getPlayerPanelsList().size()-1; ++i){
            VPhaseIndicator fvAi = view.getPlayerPanelsList().get(i).getPhaseIndicator();
            fvAi.getLabel(PhaseType.UPKEEP).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_UPKEEP));
            fvAi.getLabel(PhaseType.DRAW).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_DRAW));
            fvAi.getLabel(PhaseType.MAIN1).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_MAIN1));
            fvAi.getLabel(PhaseType.COMBAT_BEGIN).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_BEGINCOMBAT));
            fvAi.getLabel(PhaseType.COMBAT_DECLARE_ATTACKERS).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_DECLAREATTACKERS));
            fvAi.getLabel(PhaseType.COMBAT_DECLARE_BLOCKERS).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_DECLAREBLOCKERS));
            fvAi.getLabel(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_FIRSTSTRIKE));
            fvAi.getLabel(PhaseType.COMBAT_DAMAGE).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_COMBATDAMAGE));
            fvAi.getLabel(PhaseType.COMBAT_END).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_ENDCOMBAT));
            fvAi.getLabel(PhaseType.MAIN2).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_MAIN2));
            fvAi.getLabel(PhaseType.END_OF_TURN).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_EOT));
            fvAi.getLabel(PhaseType.CLEANUP).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_CLEANUP));
        }

        final VPhaseIndicator fvHuman = view.getBottomPlayerPanel().getPhaseIndicator();
        fvHuman.getLabel(PhaseType.UPKEEP).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_UPKEEP));
        fvHuman.getLabel(PhaseType.DRAW).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_DRAW));
        fvHuman.getLabel(PhaseType.MAIN1).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_MAIN1));
        fvHuman.getLabel(PhaseType.COMBAT_BEGIN).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_BEGINCOMBAT));
        fvHuman.getLabel(PhaseType.COMBAT_DECLARE_ATTACKERS).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_DECLAREATTACKERS));
        fvHuman.getLabel(PhaseType.COMBAT_DECLARE_BLOCKERS).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_DECLAREBLOCKERS));
        fvHuman.getLabel(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_FIRSTSTRIKE));
        fvHuman.getLabel(PhaseType.COMBAT_DAMAGE).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_COMBATDAMAGE));
        fvHuman.getLabel(PhaseType.COMBAT_END).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_ENDCOMBAT));
        fvHuman.getLabel(PhaseType.MAIN2).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_MAIN2));
        fvHuman.getLabel(PhaseType.END_OF_TURN).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_EOT));
        fvHuman.getLabel(PhaseType.CLEANUP).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_CLEANUP));
    }

    public static void writeMatchPreferences() {
        final ForgePreferences prefs = FModel.getPreferences();

        final VPhaseIndicator fvAi = view.getTopPlayerPanel().getPhaseIndicator();
        prefs.setPref(FPref.PHASE_AI_UPKEEP, String.valueOf(fvAi.getLabel(PhaseType.UPKEEP).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_DRAW, String.valueOf(fvAi.getLabel(PhaseType.DRAW).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_MAIN1, String.valueOf(fvAi.getLabel(PhaseType.MAIN1).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_BEGINCOMBAT, String.valueOf(fvAi.getLabel(PhaseType.COMBAT_BEGIN).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_DECLAREATTACKERS, String.valueOf(fvAi.getLabel(PhaseType.COMBAT_DECLARE_ATTACKERS).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_DECLAREBLOCKERS, String.valueOf(fvAi.getLabel(PhaseType.COMBAT_DECLARE_BLOCKERS).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_FIRSTSTRIKE, String.valueOf(fvAi.getLabel(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_COMBATDAMAGE, String.valueOf(fvAi.getLabel(PhaseType.COMBAT_DAMAGE).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_ENDCOMBAT, String.valueOf(fvAi.getLabel(PhaseType.COMBAT_END).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_MAIN2, String.valueOf(fvAi.getLabel(PhaseType.MAIN2).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_EOT, String.valueOf(fvAi.getLabel(PhaseType.END_OF_TURN).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_CLEANUP, String.valueOf(fvAi.getLabel(PhaseType.CLEANUP).getStopAtPhase()));

        final VPhaseIndicator fvHuman = view.getBottomPlayerPanel().getPhaseIndicator();
        prefs.setPref(FPref.PHASE_HUMAN_UPKEEP, String.valueOf(fvHuman.getLabel(PhaseType.UPKEEP).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_DRAW, String.valueOf(fvHuman.getLabel(PhaseType.DRAW).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_MAIN1, String.valueOf(fvHuman.getLabel(PhaseType.MAIN1).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_BEGINCOMBAT, String.valueOf(fvHuman.getLabel(PhaseType.COMBAT_BEGIN).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_DECLAREATTACKERS, String.valueOf(fvHuman.getLabel(PhaseType.COMBAT_DECLARE_ATTACKERS).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_DECLAREBLOCKERS, String.valueOf(fvHuman.getLabel(PhaseType.COMBAT_DECLARE_BLOCKERS).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_FIRSTSTRIKE, String.valueOf(fvHuman.getLabel(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_COMBATDAMAGE, String.valueOf(fvHuman.getLabel(PhaseType.COMBAT_DAMAGE).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_ENDCOMBAT, String.valueOf(fvHuman.getLabel(PhaseType.COMBAT_END).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_MAIN2, String.valueOf(fvHuman.getLabel(PhaseType.MAIN2).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_EOT, fvHuman.getLabel(PhaseType.END_OF_TURN).getStopAtPhase());
        prefs.setPref(FPref.PHASE_HUMAN_CLEANUP, fvHuman.getLabel(PhaseType.CLEANUP).getStopAtPhase());

        prefs.save();
    }

    @Override
    public void message(final String message, final String title) {
        SOptionPane.showMessageDialog(message, title);
    }

    @Override
    public void showErrorDialog(final String message, final String title) {
        SOptionPane.showErrorDialog(message, title);
    }

    @Override
    public boolean showConfirmDialog(final String message, final String title, final String yesButtonText, final String noButtonText, final boolean defaultYes) {
        return SOptionPane.showConfirmDialog(message, title, yesButtonText, noButtonText, defaultYes);
    }

    @Override
    public int showOptionDialog(final String message, final String title, final FSkinProp icon, final List<String> options, final int defaultOption) {
        return SOptionPane.showOptionDialog(message, title, icon, options, defaultOption);
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput, final List<String> inputOptions, boolean isNumeric) {
        return SOptionPane.showInputDialog(message, title, icon, initialInput, inputOptions, isNumeric);
    }

    @Override
    public boolean confirm(final CardView c, final String question, final boolean defaultIsYes, final List<String> options) {
        final List<String> optionsToUse;
        if (options == null) {
            optionsToUse = ImmutableList.of(Forge.getLocalizer().getMessage("lblYes"), Forge.getLocalizer().getMessage("lblNo"));
        } else {
            optionsToUse = options;
        }
        return FOptionPane.showCardOptionDialog(c, question, "", SOptionPane.INFORMATION_ICON, optionsToUse, defaultIsYes ? 0 : 1) == 0;
    }

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final List<T> choices, final List<T> selected, final Function<T, String> display) {
        return GuiBase.getInterface().getChoices(message, min, max, choices, selected, display);
    }

    @Override
    public <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax, final List<T> sourceChoices, final List<T> destChoices, final CardView referenceCard, final boolean sideboardingMode) {
        return GuiBase.getInterface().order(title, top, remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices);
    }

    @Override
    public List<PaperCard> sideboard(final CardPool sideboard, final CardPool main, final String message) {
        return new WaitCallback<List<PaperCard>>() {
            @Override
            public void run() {
                final FSideboardDialog sideboardDialog = new FSideboardDialog(sideboard, main, this, message);
                sideboardDialog.show();
            }
        }.invokeAndWait();
    }

    @Override
    public GameEntityView chooseSingleEntityForEffect(final String title, final List<? extends GameEntityView> optionList, final DelayedReveal delayedReveal, final boolean isOptional) {
        if (delayedReveal == null || delayedReveal.getCards().isEmpty()) {
            if (isOptional) {
                return SGuiChoose.oneOrNone(title, optionList);
            }
            return SGuiChoose.one(title, optionList);
        }

        final Collection<CardView> revealList = delayedReveal.getCards();
        final String revealListCaption = StringUtils.capitalize(MessageUtil.formatMessage("{player's} " + delayedReveal.getZone().getTranslatedName(), delayedReveal.getOwner(), delayedReveal.getOwner()));
        final FImage revealListImage = VPlayerPanel.iconFromZone(delayedReveal.getZone());

        //use special dialog for choosing card and offering ability to see all revealed cards at the same time
        return new WaitCallback<GameEntityView>() {
            @Override
            public void run() {
                final GameEntityPicker picker = new GameEntityPicker(title, optionList, revealList, revealListCaption, revealListImage, isOptional, this);
                picker.show();
            }
        }.invokeAndWait();
    }

    @Override
    public List<GameEntityView> chooseEntitiesForEffect(String title, List<? extends GameEntityView> optionList, int min, int max, DelayedReveal delayedReveal) {
        final int m1 = max >= 0 ? optionList.size() - max : -1;
        final int m2 = min >= 0 ? optionList.size() - min : -1;
        return SGuiChoose.order(title, Forge.getLocalizer().getMessage("lblSelected"), m1, m2, (List<GameEntityView>) optionList, null);
    }

    @Override
    public List<CardView> manipulateCardList(final String title, final Iterable<CardView> cards, final Iterable<CardView> manipulable, final boolean toTop, final boolean toBottom, final boolean toAnywhere) {
	System.err.println("Not implemented yet - should never be called");
	return null;
    }

    @Override
    public void setCard(final CardView card) {
        // doesn't need to do anything
    }

    @Override
    public void setPlayerAvatar(final LobbyPlayer player, final IHasIcon ihi) {
        Forge.getAssets().avatarImages().put(player.getName(), ImageCache.getInstance().getIcon(ihi));
    }

    @Override
    public boolean isUiSetToSkipPhase(final PlayerView playerTurn, final PhaseType phase) {
        return !view.stopAtPhase(playerTurn, phase);
    }

    public static HostedMatch hostMatch() {
        hostedMatch = new HostedMatch();
        return hostedMatch;
    }

    public static HostedMatch getHostedMatch() {
        return hostedMatch;
    }

    public void showFullControl(PlayerView selected, float x, float y) {
        if (selected.isAI()) {
            return;
        }
        Set<FullControlFlag> controlFlags = getGameView().getGame().getPlayer(selected).getController().getFullControl();
        FPopupMenu menu = new FPopupMenu() {
            @Override
            protected void buildMenu() {
                addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblFullControl"),
                        e -> {
                            FOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblFullControlDetails"), "Full Control details");
                        }));
                addItem(getFullControlMenuEntry("lblChooseCostOrder", FullControlFlag.ChooseCostOrder, controlFlags));
                addItem(getFullControlMenuEntry("lblChooseCostReductionOrder", FullControlFlag.ChooseCostReductionOrderAndVariableAmount, controlFlags));
                addItem(getFullControlMenuEntry("lblNoPaymentFromManaAbility", FullControlFlag.NoPaymentFromManaAbility, controlFlags));
                addItem(getFullControlMenuEntry("lblNoFreeCombatCostHandling", FullControlFlag.NoFreeCombatCostHandling, controlFlags));
                addItem(getFullControlMenuEntry("lblAllowPaymentStartWithMissingResources", FullControlFlag.AllowPaymentStartWithMissingResources, controlFlags));
                addItem(getFullControlMenuEntry("lblLayerTimestampOrder", FullControlFlag.LayerTimestampOrder, controlFlags));
            }
        };

       menu.show(getView(), getView().getPlayerPanel(selected).localToScreenX(x), getView().getPlayerPanel(selected).localToScreenY(y));        
    }

    private FCheckBoxMenuItem getFullControlMenuEntry(String label, FullControlFlag flag, Set<FullControlFlag> controlFlags) {
        return new FCheckBoxMenuItem(Forge.getLocalizer().getMessage(label), controlFlags.contains(flag),
                e -> {
                    if (controlFlags.contains(flag)) {
                        controlFlags.remove(flag);
                    } else {
                        controlFlags.add(flag);
                    }
                });
    }
}
