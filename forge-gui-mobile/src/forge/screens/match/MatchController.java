package forge.screens.match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;

import forge.Forge;
import forge.Graphics;
import forge.GuiBase;
import forge.LobbyPlayer;
import forge.assets.FImage;
import forge.assets.FSkin;
import forge.assets.FTextureRegionImage;
import forge.game.Match;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.interfaces.IButton;
import forge.match.IMatchController;
import forge.match.MatchUtil;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.views.VAssignDamage;
import forge.screens.match.views.VPhaseIndicator;
import forge.screens.match.views.VPlayerPanel;
import forge.screens.match.views.VCardDisplayArea.CardAreaPanel;
import forge.screens.match.views.VPhaseIndicator.PhaseLabel;
import forge.screens.match.views.VPlayerPanel.InfoTab;
import forge.screens.match.views.VPrompt;
import forge.screens.match.winlose.ViewWinLose;
import forge.toolbox.FDisplayObject;
import forge.util.ITriggerEvent;
import forge.util.WaitCallback;
import forge.util.gui.SGuiChoose;
import forge.view.CardView;
import forge.view.CombatView;
import forge.view.GameEntityView;
import forge.view.LocalGameView;
import forge.view.PlayerView;
import forge.view.SpellAbilityView;

public class MatchController implements IMatchController {
    private MatchController() { }
    public static final MatchController instance = new MatchController();

    private static final Map<LobbyPlayer, FImage> avatarImages = new HashMap<LobbyPlayer, FImage>();

    private static MatchScreen view;

    public static MatchScreen getView() {
        return view;
    }

    public static FImage getPlayerAvatar(final PlayerView p) {
        LobbyPlayer lp = p.getLobbyPlayer();
        FImage avatar = avatarImages.get(lp);
        if (avatar == null) {
            avatar = new FTextureRegionImage(FSkin.getAvatars().get(lp.getAvatarIndex()));
        }
        return avatar;
    }

    public static void setPlayerAvatar(final LobbyPlayer lp, final FImage avatarImage) {
        avatarImages.put(lp, avatarImage);
    }

    public void refreshCardDetails(Iterable<CardView> cards) {
        //ensure cards appear in the correct row of the field
        for (VPlayerPanel pnl : view.getPlayerPanels().values()) {
            pnl.getField().update();
        }
    }

    @Override
    public void startNewMatch(Match match) {
        MatchUtil.startGame(match);
    }

    @Override
    public boolean resetForNewGame() {
        CardAreaPanel.resetForNewGame(); //ensure card panels reset between games
        return true;
    }

    @Override
    public boolean hotSeatMode() {
        return FModel.getPreferences().getPrefBoolean(FPref.MATCH_HOT_SEAT_MODE);
    }

    @Override
    public void openView(List<Player> sortedPlayers) {
        boolean noHumans = MatchUtil.getHumanCount() == 0;
        List<VPlayerPanel> playerPanels = new ArrayList<VPlayerPanel>();
        for (Player p : sortedPlayers) {
            playerPanels.add(new VPlayerPanel(MatchUtil.getGameView(p).getPlayerView(p), noHumans || p.getController() instanceof PlayerControllerHuman));
        }
        view = new MatchScreen(playerPanels);

        if (noHumans) {
            //add special object that pauses game if screen touched
            view.add(new FDisplayObject() {
                @Override
                public void draw(Graphics g) {
                    //don't draw anything
                }

                @Override
                public void buildTouchListeners(float screenX, float screenY, ArrayList<FDisplayObject> listeners) {
                    if (screenY < view.getHeight() - VPrompt.HEIGHT) {
                        MatchUtil.pause();
                    }
                }
            });
        }

        actuateMatchPreferences();

        Forge.openScreen(view);
    }

    @Override
    public IButton getBtnOK(PlayerView player) {
        return view.getPrompt(player).getBtnOk();
    }

    @Override
    public IButton getBtnCancel(PlayerView player) {
        return view.getPrompt(player).getBtnCancel();
    }

    @Override
    public void showPromptMessage(final PlayerView player, String message) {
        view.getPrompt(player).setMessage(message);
    }

    @Override
    public void focusButton(final IButton button) {
        //not needed for mobile game
    }

    @Override
    public void flashIncorrectAction() {
        //SDisplayUtil.remind(VPrompt.SINGLETON_INSTANCE); //TODO
    }

    @Override
    public void updatePhase() {
        LocalGameView gameView = MatchUtil.getGameView();
        final PlayerView p = gameView.getPlayerTurn();
        final PhaseType ph = gameView.getPhase();

        PhaseLabel lbl = view.getPlayerPanel(p).getPhaseIndicator().getLabel(ph);

        view.resetAllPhaseButtons();
        if (lbl != null) {
            lbl.setActive(true);
        }
    }

    @Override
    public void updateTurn(final PlayerView player) {
    }

    @Override
    public void updatePlayerControl() {
        //show/hide hand for top player based on whether the opponent is controlled
        if (MatchUtil.getHumanCount() == 1) {
            Player player = MatchUtil.getGameView().getPlayer(view.getTopPlayerPanel().getPlayer());
            if (player.getMindSlaveMaster() != null) {
                view.getTopPlayerPanel().setSelectedZone(ZoneType.Hand);
            }
            else {
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
        new ViewWinLose(MatchUtil.getGameView()).setVisible(true);
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
    public int getAbilityToPlay(List<SpellAbilityView> abilities, ITriggerEvent triggerEvent) {
        if (abilities.isEmpty()) {
            return -1;
        }
        if (abilities.size() == 1) {
            return abilities.get(0).getId();
        }
        final SpellAbilityView choice = SGuiChoose.oneOrNone(GuiBase.getInterface(), "Choose ability to play", abilities);
        return choice == null ? -1 : choice.getId();
    }

    @Override
    public void showCombat(final CombatView combat) {
    }

    @Override
    public Object showManaPool(final PlayerView player) {
        VPlayerPanel playerPanel = view.getPlayerPanel(player);
        InfoTab oldSelectedTab = playerPanel.getSelectedTab();
        playerPanel.setSelectedTab(playerPanel.getManaPoolTab());
        return oldSelectedTab;
    }

    @Override
    public void hideManaPool(final PlayerView player, final Object zoneToRestore) {
        VPlayerPanel playerPanel = view.getPlayerPanel(player);
        if (zoneToRestore == playerPanel.getManaPoolTab()) {
            return; //if mana pool was selected previously, we don't need to switch back to anything
        }
        if (playerPanel.getSelectedTab() != playerPanel.getManaPoolTab()) {
            return; //if player switch away from mana pool already, don't change anything
        }
        playerPanel.setSelectedTab((InfoTab)zoneToRestore);
    }

    @Override
    public boolean openZones(Collection<ZoneType> zones, Map<PlayerView, Object> players) {
        if (zones.size() == 1) {
            ZoneType zoneType = zones.iterator().next();
            switch (zoneType) {
            case Battlefield:
            case Command:
                players.clear(); //clear since no zones need to be restored
                return true; //Battlefield is always open
            default:
                //open zone tab for given zone if needed
                boolean result = true;
                for (PlayerView player : players.keySet()) {
                    VPlayerPanel playerPanel = view.getPlayerPanel(player);
                    players.put(player, playerPanel.getSelectedTab()); //backup selected tab before changing it
                    InfoTab zoneTab = playerPanel.getZoneTab(zoneType);
                    if (zoneTab == null) {
                        result = false;
                    }
                    else {
                        playerPanel.setSelectedTab(zoneTab);
                    }
                }
                return result;
            }
        }
        return false;
    }

    @Override
    public void restoreOldZones(Map<PlayerView, Object> playersToRestoreZonesFor) {
        for (Entry<PlayerView, Object> player : playersToRestoreZonesFor.entrySet()) {
            VPlayerPanel playerPanel = view.getPlayerPanel(player.getKey());
            playerPanel.setSelectedTab((InfoTab)player.getValue());
        }
    }

    @Override
    public Map<CardView, Integer> assignDamage(final CardView attacker, final List<CardView> blockers, final int damage, final GameEntityView defender, final boolean overrideOrder) {
        return new WaitCallback<Map<CardView, Integer>>() {
            @Override
            public void run() {
                VAssignDamage v = new VAssignDamage(attacker, blockers, damage, defender, overrideOrder, this);
                v.show();
            }
        }.invokeAndWait();
    }

    @Override
    public void updateManaPool(List<PlayerView> manaPoolUpdate) {
        for (PlayerView p : manaPoolUpdate) {
            view.getPlayerPanel(p).updateManaPool();
        }
    }

    @Override
    public void updateLives(List<PlayerView> livesUpdate) {
        for (PlayerView p : livesUpdate) {
            view.getPlayerPanel(p).updateLife();
        }
    }

    @Override
    public void hear(LobbyPlayer player, String message) {
    }

    @Override
    public boolean stopAtPhase(PlayerView turn, PhaseType phase) {
        return view.stopAtPhase(turn, phase);
    }

    @Override
    public void updateZones(List<Pair<PlayerView, ZoneType>> zonesToUpdate) {
        view.updateZones(zonesToUpdate);
    }

    @Override
    public void updateSingleCard(CardView card) {
        view.updateSingleCard(card);
    }

    @Override
    public void afterGameEnd() {
        Forge.back();
        view = null;
    }

    private static void actuateMatchPreferences() {
        ForgePreferences prefs = FModel.getPreferences();

        VPhaseIndicator fvAi = view.getTopPlayerPanel().getPhaseIndicator();
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

        VPhaseIndicator fvHuman = view.getBottomPlayerPanel().getPhaseIndicator();
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
        ForgePreferences prefs = FModel.getPreferences();

        VPhaseIndicator fvAi = view.getTopPlayerPanel().getPhaseIndicator();
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

        VPhaseIndicator fvHuman = view.getBottomPlayerPanel().getPhaseIndicator();
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
}
