package forge.screens.match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import forge.Forge;
import forge.Graphics;
import forge.GuiBase;
import forge.LobbyPlayer;
import forge.assets.FImage;
import forge.assets.FSkin;
import forge.assets.FSkinProp;
import forge.assets.FTextureRegionImage;
import forge.assets.ImageCache;
import forge.card.CardRenderer;
import forge.card.GameEntityPicker;
import forge.deck.CardPool;
import forge.deck.FSideboardDialog;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.IHasIcon;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.match.AbstractGuiGame;
import forge.match.HostedMatch;
import forge.model.FModel;
import forge.player.PlayerZoneUpdate;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.views.VAssignDamage;
import forge.screens.match.views.VPhaseIndicator;
import forge.screens.match.views.VPhaseIndicator.PhaseLabel;
import forge.screens.match.views.VPlayerPanel;
import forge.screens.match.views.VPlayerPanel.InfoTab;
import forge.screens.match.views.VPrompt;
import forge.screens.match.winlose.ViewWinLose;
import forge.toolbox.FButton;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOptionPane;
import forge.trackable.TrackableCollection;
import forge.util.collect.FCollectionView;
import forge.util.ITriggerEvent;
import forge.util.MessageUtil;
import forge.util.WaitCallback;
import forge.util.gui.SGuiChoose;
import forge.util.gui.SOptionPane;

public class MatchController extends AbstractGuiGame {
    private MatchController() { }
    public static final MatchController instance = new MatchController();

    private static final Map<String, FImage> avatarImages = new HashMap<String, FImage>();

    private static HostedMatch hostedMatch;
    private static MatchScreen view;

    private final Map<PlayerView, InfoTab> zonesToRestore = Maps.newHashMap();

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

    public static FImage getPlayerAvatar(final PlayerView p) {
        final String lp = p.getLobbyPlayerName();
        FImage avatar = avatarImages.get(lp);
        if (avatar == null) {
            if (StringUtils.isEmpty(p.getAvatarCardImageKey())) {
                avatar = new FTextureRegionImage(FSkin.getAvatars().get(p.getAvatarIndex()));
            }
            else { //handle lobby players with art from cards
                avatar = CardRenderer.getCardArt(p.getAvatarCardImageKey(), false, false);
            }
        }
        return avatar;
    }

    @Override
    public void refreshCardDetails(final Iterable<CardView> cards) {
        //ensure cards appear in the correct row of the field
        for (final VPlayerPanel pnl : view.getPlayerPanels().values()) {
            pnl.getField().update();
        }
    }

    public boolean hotSeatMode() {
        return FModel.getPreferences().getPrefBoolean(FPref.MATCH_HOT_SEAT_MODE);
    }

    @Override
    public void openView(final TrackableCollection<PlayerView> myPlayers) {
        final boolean noHumans = !hasLocalPlayers();

        final FCollectionView<PlayerView> allPlayers = getGameView().getPlayers();
        final List<VPlayerPanel> playerPanels = new ArrayList<VPlayerPanel>();
        for (final PlayerView p : allPlayers) {
            final boolean isLocal = isLocalPlayer(p);
            final VPlayerPanel playerPanel = new VPlayerPanel(p, isLocal || noHumans);
            if (isLocal && !playerPanels.isEmpty()) {
                playerPanels.add(0, playerPanel); //ensure local player always first among player panels
            }
            else {
                playerPanels.add(playerPanel);
            }
        }
        view = new MatchScreen(playerPanels);

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

        Forge.openScreen(view);
    }

    @Override
    public void showPromptMessage(final PlayerView player, final String message) {
        view.getPrompt(player).setMessage(message);
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
    public void updatePhase() {
        final GameView gameView = getGameView();
        final PlayerView p = gameView.getPlayerTurn();
        final PhaseType ph = gameView.getPhase();

        final PhaseLabel lbl = view.getPlayerPanel(p).getPhaseIndicator().getLabel(ph);

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
        if (hasLocalPlayers() || getGameView().isMatchOver()) {
            new ViewWinLose(getGameView()).setVisible(true);
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
        return SGuiChoose.oneOrNone("Choose ability to play", abilities);
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
    public boolean openZones(final Collection<ZoneType> zones, final Map<PlayerView, Object> players) {
        if (zones.size() == 1) {
            final ZoneType zoneType = zones.iterator().next();
            switch (zoneType) {
                case Battlefield:
                case Command:
                    players.clear(); //clear since no zones need to be restored
                    return true; //Battlefield is always open
                default:
                    //open zone tab for given zone if needed
                    boolean result = true;
                    for (final PlayerView player : players.keySet()) {
                        final VPlayerPanel playerPanel = view.getPlayerPanel(player);
                        players.put(player, playerPanel.getSelectedTab()); //backup selected tab before changing it
                        final InfoTab zoneTab = playerPanel.getZoneTab(zoneType);
                        if (zoneTab == null) {
                            result = false;
                        } else {
                            playerPanel.setSelectedTab(zoneTab);
                        }
                    }
                    return result;
            }
        }
        return false;
    }

    @Override
    public void restoreOldZones(final Map<PlayerView, Object> playersToRestoreZonesFor) {
        for (final Entry<PlayerView, Object> player : playersToRestoreZonesFor.entrySet()) {
            final VPlayerPanel playerPanel = view.getPlayerPanel(player.getKey());
            playerPanel.setSelectedTab((InfoTab)player.getValue());
        }
    }

    @Override
    public Map<CardView, Integer> assignDamage(final CardView attacker, final List<CardView> blockers, final int damage, final GameEntityView defender, final boolean overrideOrder) {
        return new WaitCallback<Map<CardView, Integer>>() {
            @Override
            public void run() {
                final VAssignDamage v = new VAssignDamage(attacker, blockers, damage, defender, overrideOrder, this);
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
    public void updateZones(final Iterable<PlayerZoneUpdate> zonesToUpdate) {
        view.updateZones(zonesToUpdate);
    }

    @Override
    public void updateCards(final Iterable<CardView> cards) {
        for (final CardView card : cards) {
            view.updateSingleCard(card);
        }
    }

    @Override
    public void afterGameEnd() {
        Forge.back();
        //view = null;
    }

    private static void actuateMatchPreferences() {
        final ForgePreferences prefs = FModel.getPreferences();

        final VPhaseIndicator fvAi = view.getTopPlayerPanel().getPhaseIndicator();
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
    public String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput, final List<String> inputOptions) {
        return SOptionPane.showInputDialog(message, title, icon, initialInput, inputOptions);
    }

    @Override
    public boolean confirm(final CardView c, final String question, final boolean defaultIsYes, final List<String> options) {
        final List<String> optionsToUse;
        if (options == null) {
            optionsToUse = ImmutableList.of("Yes", "No");
        } else {
            optionsToUse = options;
        }
        return FOptionPane.showCardOptionDialog(c, question, "", SOptionPane.INFORMATION_ICON, optionsToUse, defaultIsYes ? 0 : 1) == 0;
    }

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final List<T> choices, final T selected, final Function<T, String> display) {
        return GuiBase.getInterface().getChoices(message, min, max, choices, selected, display);
    }

    @Override
    public <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax, final List<T> sourceChoices, final List<T> destChoices, final CardView referenceCard, final boolean sideboardingMode) {
        return GuiBase.getInterface().order(title, top, remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices);
    }

    @Override
    public List<PaperCard> sideboard(final CardPool sideboard, final CardPool main) {
        return new WaitCallback<List<PaperCard>>() {
            @Override
            public void run() {
                final FSideboardDialog sideboardDialog = new FSideboardDialog(sideboard, main, this);
                sideboardDialog.show();
            }
        }.invokeAndWait();
    }

    @Override
    public GameEntityView chooseSingleEntityForEffect(final String title, final List<? extends GameEntityView> optionList, final DelayedReveal delayedReveal, final boolean isOptional) {
        if (delayedReveal == null || Iterables.isEmpty(delayedReveal.getCards())) {
            if (isOptional) {
                return SGuiChoose.oneOrNone(title, optionList);
            }
            return SGuiChoose.one(title, optionList);
        }

        final Collection<CardView> revealList = delayedReveal.getCards();
        final String revealListCaption = StringUtils.capitalize(MessageUtil.formatMessage("{player's} " + delayedReveal.getZone().name(), delayedReveal.getOwner(), delayedReveal.getOwner()));
        final FImage revealListImage = MatchController.getView().getPlayerPanels().values().iterator().next().getZoneTab(delayedReveal.getZone()).getIcon();

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
    public void setCard(final CardView card) {
        // doesn't need to do anything
    }

    @Override
    public void setPlayerAvatar(final LobbyPlayer player, final IHasIcon ihi) {
        avatarImages.put(player.getName(), ImageCache.getIcon(ihi));
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
}
