/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.screens.match;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.tuple.Pair;

import forge.FThreads;
import forge.ImageCache;
import forge.LobbyPlayer;
import forge.Singletons;
import forge.UiCommand;
import forge.game.Game;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.Match;
import forge.game.card.CardView;
import forge.game.combat.CombatView;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gui.FNetOverlay;
import forge.gui.GuiChoose;
import forge.gui.GuiUtils;
import forge.gui.SOverlayUtils;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.gui.framework.SDisplayUtil;
import forge.gui.framework.SLayoutIO;
import forge.interfaces.IButton;
import forge.item.InventoryItem;
import forge.match.IMatchController;
import forge.match.MatchUtil;
import forge.menus.IMenuProvider;
import forge.model.FModel;
import forge.player.LobbyPlayerHuman;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.controllers.CAntes;
import forge.screens.match.controllers.CCombat;
import forge.screens.match.controllers.CDetail;
import forge.screens.match.controllers.CLog;
import forge.screens.match.controllers.CPicture;
import forge.screens.match.controllers.CPrompt;
import forge.screens.match.controllers.CStack;
import forge.screens.match.menus.CMatchUIMenus;
import forge.screens.match.views.VCommand;
import forge.screens.match.views.VField;
import forge.screens.match.views.VHand;
import forge.screens.match.views.VPlayers;
import forge.screens.match.views.VPrompt;
import forge.toolbox.FButton;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.toolbox.MouseTriggerEvent;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.special.PhaseIndicator;
import forge.toolbox.special.PhaseLabel;
import forge.util.ITriggerEvent;
import forge.view.arcane.CardPanel;
import forge.view.arcane.HandArea;
import forge.view.arcane.PlayArea;

/**
 * Constructs instance of match UI controller, used as a single point of
 * top-level control for child UIs. Tasks targeting the view of individual
 * components are found in a separate controller for that component and
 * should not be included here.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public enum CMatchUI implements ICDoc, IMenuProvider, IMatchController {
    SINGLETON_INSTANCE;

    private List<PlayerView> sortedPlayers;
    private VMatchUI view;
    private boolean allHands;
    private boolean showOverlay = true;

    private IVDoc<? extends ICDoc> selectedDocBeforeCombat;
    public final Map<LobbyPlayer, String> avatarImages = new HashMap<LobbyPlayer, String>();

    private SkinImage getPlayerAvatar(final LobbyPlayer p, final int defaultIndex) {
         if (avatarImages.containsKey(p)) {
            return ImageCache.getIcon(avatarImages.get(p));
        }

        int avatarIdx = p.getAvatarIndex();
        return FSkin.getAvatars().get(avatarIdx >= 0 ? avatarIdx : defaultIndex);
    }

    private void setAvatar(VField view, SkinImage img) {
        view.getLblAvatar().setIcon(img);
        view.getLblAvatar().getResizeTimer().start();
    }

    /**
     * Instantiates at a match.
     */
    public void initMatch(final List<PlayerView> sortedPlayers0, final boolean allHands0) {
        sortedPlayers = sortedPlayers0;
        allHands = allHands0;
        view = VMatchUI.SINGLETON_INSTANCE;
        // TODO fix for use with multiplayer

        final String[] indices = FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",");

        final List<VField> fields = new ArrayList<VField>();
        final List<VCommand> commands = new ArrayList<VCommand>();

        int i = 0;
        for (final PlayerView p : sortedPlayers) {
            // A field must be initialized after it's instantiated, to update player info.
            // No player, no init.
            VField f = new VField(EDocID.Fields[i], p);
            VCommand c = new VCommand(EDocID.Commands[i], p);
            fields.add(f);
            commands.add(c);

            //setAvatar(f, new ImageIcon(FSkin.getAvatars().get()));
            setAvatar(f, getPlayerAvatar(p.getLobbyPlayer(), Integer.parseInt(indices[i > 2 ? 1 : 0])));
            f.getLayoutControl().initialize();
            c.getLayoutControl().initialize();
            i++;
        }

        // Replace old instances
        view.setCommandViews(commands);
        view.setFieldViews(fields);

        VPlayers.SINGLETON_INSTANCE.init(sortedPlayers0);

        initHandViews();
    }

    public void initHandViews() {
        final List<VHand> hands = new ArrayList<VHand>();

        int i = 0;
        for (final PlayerView p : sortedPlayers) {
            if (allHands || p.getLobbyPlayer() instanceof LobbyPlayerHuman || CardView.mayViewAny(p.getHand())) {
                VHand newHand = new VHand(EDocID.Hands[i], p);
                newHand.getLayoutControl().initialize();
                hands.add(newHand);
            }
            i++;
        }

        view.setHandViews(hands);
    }

    /**
     * Resets all phase buttons in all fields to "inactive", so highlight won't
     * be drawn on them. "Enabled" state remains the same.
     */
    // This method is in the top-level controller because it affects ALL fields
    // (not just one).
    public void resetAllPhaseButtons() {
        for (final VField v : view.getFieldViews()) {
            v.getPhaseIndicator().resetPhaseButtons();
        }
    }

    /** @param s0 &emsp; {@link java.lang.String} */
    public void showMessage(final String s0) {
        CPrompt.SINGLETON_INSTANCE.setMessage(s0);
    }

    public VField getFieldViewFor(PlayerView p) {
        int idx = getPlayerIndex(p);
        return idx < 0 ? null :view.getFieldViews().get(idx);
    }

    public VCommand getCommandFor(PlayerView p) {
        int idx = getPlayerIndex(p);
        return idx < 0 ? null :view.getCommandViews().get(idx);
    }

    public VHand getHandFor(PlayerView p) {
        int idx = getPlayerIndex(p);
        List<VHand> allHands = view.getHands();
        return idx < 0 || idx >= allHands.size() ? null : allHands.get(idx);
    }

    /**
     * 
     * Checks if game control should stop at a phase, for either
     * a forced programmatic stop, or a user-induced phase toggle.
     * @param turn &emsp; {@link forge.game.player.Player}
     * @param phase &emsp; {@link java.lang.String}
     * @return boolean
     */
    public final boolean stopAtPhase(final PlayerView turn, final PhaseType phase) {
        VField vf = getFieldViewFor(turn);
        PhaseLabel label = vf.getPhaseIndicator()
                .getLabelFor(phase);
        return label == null || label.getEnabled();
    }

    public void setCard(final CardView c) {
        this.setCard(c, false);
    }

    public void setCard(final CardView c, final boolean isInAltState) {
        FThreads.assertExecutedByEdt(true);
        CDetail.SINGLETON_INSTANCE.showCard(c, isInAltState);
        CPicture.SINGLETON_INSTANCE.showCard(c, isInAltState);
    }

    public void setCard(final InventoryItem c) {
        CDetail.SINGLETON_INSTANCE.showCard(c);
        CPicture.SINGLETON_INSTANCE.showImage(c);
    }

    private int getPlayerIndex(PlayerView player) {
        return sortedPlayers.indexOf(player);
    }

    @Override
    public void showCombat() {
        CombatView combat = MatchUtil.getGameView().getCombat();
        if (combat != null && combat.getNumAttackers() > 0 && MatchUtil.getGameView().peekStack() == null) {
            if (selectedDocBeforeCombat == null) {
                IVDoc<? extends ICDoc> combatDoc = EDocID.REPORT_COMBAT.getDoc();
                if (combatDoc.getParentCell() != null) {
                    selectedDocBeforeCombat = combatDoc.getParentCell().getSelected();
                    if (selectedDocBeforeCombat != combatDoc) {
                        SDisplayUtil.showTab(combatDoc);
                    }
                    else {
                        selectedDocBeforeCombat = null; //don't need to cache combat doc this way
                    }
                }
            }
        }
        else if (selectedDocBeforeCombat != null) { //re-select doc that was selected before once combat finished
            SDisplayUtil.showTab(selectedDocBeforeCombat);
            selectedDocBeforeCombat = null;
        }
        CCombat.SINGLETON_INSTANCE.setModel(combat);
        CCombat.SINGLETON_INSTANCE.update();
    } // showCombat(CombatView)

    public void updateZones(List<Pair<PlayerView, ZoneType>> zonesToUpdate) {
        //System.out.println("updateZones " + zonesToUpdate);
        for (Pair<PlayerView, ZoneType> kv : zonesToUpdate) {
            PlayerView owner = kv.getKey();
            ZoneType zt = kv.getValue();

            if (zt == ZoneType.Command) {
                getCommandFor(owner).getTabletop().setupPlayZone();
            } else if (zt == ZoneType.Hand) {
                VHand vHand = getHandFor(owner);
                if (null != vHand) {
                    vHand.getLayoutControl().updateHand();
                }
                getFieldViewFor(owner).getDetailsPanel().updateZones();
            } else if (zt == ZoneType.Battlefield) {
                getFieldViewFor(owner).getTabletop().setupPlayZone();
            } else if (zt == ZoneType.Ante) {
                CAntes.SINGLETON_INSTANCE.update();
            } else {
                final VField vf = getFieldViewFor(owner);
                if (vf != null) {
                    vf.getDetailsPanel().updateZones();
                }
            }
        }
    }

    // Player's mana pool changes
    public void updateManaPool(final Iterable<PlayerView> manaPoolUpdate) {
        for (final PlayerView p : manaPoolUpdate) {
            getFieldViewFor(p).getDetailsPanel().updateManaPool();
        }

    }

    // Player's lives and poison counters
    public void updateLives(final Iterable<PlayerView> livesUpdate) {
        for (final PlayerView p : livesUpdate) {
            getFieldViewFor(p).updateDetails();
        }
    }

    public void updateSingleCard(final CardView c) {
        switch (c.getZone()) {
        case Battlefield:
            VField battlefield = getFieldViewFor(c.getController());
            if (battlefield != null) {
                battlefield.getTabletop().updateCard(c, false);
            }
            break;
        case Hand:
            final VHand hand = getHandFor(c.getController());
            if (hand != null) {
                CardPanel cp = hand.getHandArea().getCardPanel(c.getId());
                if (cp != null) {
                    cp.repaintOverlays();
                }
            }
            break;
        case Command:
            VCommand command = getCommandFor(c.getController());
            if (command != null) {
                command.getTabletop().updateCard(c, false);
            }
            break;
        default:
            break;
        }
    }

    public void refreshCardDetails(final Iterable<CardView> cards) {
        for (final CardView c : cards) {
            if (ZoneType.Battlefield.equals(c.getZone())) {
                PlayArea pa = getFieldViewFor(c.getController()).getTabletop();
                CardPanel pnl = pa.getCardPanel(c.getId());
                if (pnl != null) {
                    pnl.updatePTOverlay();
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.menubar.IMenuProvider#getMenus()
     */
    @Override
    public List<JMenu> getMenus() {
        return new CMatchUIMenus().getMenus();
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
        Singletons.getControl().getForgeMenu().setProvider(this);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() { }

    public void repaintCardOverlays() {
        List<CardPanel> panels = getVisibleCardPanels();
        for (CardPanel panel : panels) {
            panel.repaintOverlays();
        }
    }
    private List<CardPanel> getVisibleCardPanels() {
        List<CardPanel> panels = new ArrayList<CardPanel>();
        for (VHand h : view.getHands()) {
            panels.addAll(h.getHandArea().getCardPanels());
        }
        for (VField f : view.getFieldViews()) {
            panels.addAll(f.getTabletop().getCardPanels());
        }
        for (VCommand c : view.getCommandViews()) {
            panels.addAll(c.getTabletop().getCardPanels());
        }
        return panels;
    }

    @Override
    public boolean resetForNewGame() {
        if (MatchUtil.getGame() != null) {
            Singletons.getControl().setCurrentScreen(FScreen.MATCH_SCREEN);
            SOverlayUtils.hideOverlay();
            FOptionPane.showMessageDialog("Cannot start a new game while another game is already in progress.");
            return false; //TODO: See if it's possible to run multiple games at once without crashing
        }
        return true;
    }

    @Override
    public boolean hotSeatMode() {
        return true; //Desktop game only supports hot seat mode for Human v. Human
    }

    @Override
    public IButton getBtnOK(PlayerView playerView) {
        return VMatchUI.SINGLETON_INSTANCE.getBtnOK();
    }

    @Override
    public IButton getBtnCancel(PlayerView playerView) {
        return VMatchUI.SINGLETON_INSTANCE.getBtnCancel();
    }

    @Override
    public void focusButton(final IButton button) {
        // ensure we don't steal focus from an overlay
        if (!SOverlayUtils.overlayHasFocus()) {
            FThreads.invokeInEdtLater(new Runnable() {
                @Override
                public void run() {
                    ((FButton)button).requestFocusInWindow();
                }
            });
        }
    }

    @Override
    public void flashIncorrectAction() {
        SDisplayUtil.remind(VPrompt.SINGLETON_INSTANCE);
    }

    @Override
    public void updatePhase() {
        GameView gameView = MatchUtil.getGameView();
        final PlayerView p = gameView.getPlayerTurn();
        final PhaseType ph = gameView.getPhase();
        final CMatchUI matchUi = CMatchUI.SINGLETON_INSTANCE;
        PhaseLabel lbl = matchUi.getFieldViewFor(p).getPhaseIndicator().getLabelFor(ph);

        matchUi.resetAllPhaseButtons();
        if (lbl != null) {
            lbl.setActive(true);
        }
    }

    @Override
    public void updateTurn(final PlayerView player) {
        VField nextField = CMatchUI.SINGLETON_INSTANCE.getFieldViewFor(player);
        SDisplayUtil.showTab(nextField);
        CPrompt.SINGLETON_INSTANCE.updateText();
        CMatchUI.SINGLETON_INSTANCE.repaintCardOverlays();
    }

    @Override
    public void updatePlayerControl() {
        CMatchUI.SINGLETON_INSTANCE.initHandViews();
        SLayoutIO.loadLayout(null);
        VMatchUI.SINGLETON_INSTANCE.populate();
        for (VHand h : VMatchUI.SINGLETON_INSTANCE.getHands()) {
            h.getLayoutControl().updateHand();
        }
    }

    @Override
    public void disableOverlay() {
        showOverlay = false;
    }
    
    @Override
    public void enableOverlay() {
        showOverlay = true;
    }

    @Override
    public void finishGame() {
        new ViewWinLose(MatchUtil.getGameView());
        if (showOverlay) {
            SOverlayUtils.showOverlay();
        }
    }

    @Override
    public void updateStack() {
        CStack.SINGLETON_INSTANCE.update();
    }

    @Override
    public void setPanelSelection(final CardView c) {
        GuiUtils.setPanelSelection(c);
    }

    @Override
    public SpellAbility getAbilityToPlay(List<SpellAbility> abilities, ITriggerEvent triggerEvent) {
        if (triggerEvent == null) {
            if (abilities.isEmpty()) {
                return null;
            }
            if (abilities.size() == 1) {
                return abilities.get(0);
            }
            return GuiChoose.oneOrNone("Choose ability to play", abilities);
        }

        if (abilities.isEmpty()) {
            return null;
        }
        if (abilities.size() == 1 && !abilities.get(0).promptIfOnlyPossibleAbility()) {
            if (abilities.get(0).canPlay()) {
                return abilities.get(0); //only return ability if it's playable, otherwise return null
            }
            return null;
        }

        //show menu if mouse was trigger for ability
        final JPopupMenu menu = new JPopupMenu("Abilities");

        boolean enabled;
        boolean hasEnabled = false;
        int shortcut = KeyEvent.VK_1; //use number keys as shortcuts for abilities 1-9
        for (final SpellAbility ab : abilities) {
            enabled = ab.canPlay();
            if (enabled) {
                hasEnabled = true;
            }
            GuiUtils.addMenuItem(menu, FSkin.encodeSymbols(ab.toString(), true),
                    shortcut > 0 ? KeyStroke.getKeyStroke(shortcut, 0) : null,
                    new Runnable() {
                        @Override
                        public void run() {
                            CPrompt.SINGLETON_INSTANCE.selectAbility(ab);
                        }
                    }, enabled);
            if (shortcut > 0) {
                shortcut++;
                if (shortcut > KeyEvent.VK_9) {
                    shortcut = 0; //stop adding shortcuts after 9
                }
            }
        }
        if (hasEnabled) { //only show menu if at least one ability can be played
            SwingUtilities.invokeLater(new Runnable() { //use invoke later to ensure first ability selected by default
                public void run() {
                    MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[]{menu, menu.getSubElements()[0]});
                }
            });
            MouseEvent mouseEvent = ((MouseTriggerEvent)triggerEvent).getMouseEvent();
            menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
        }

        return null; //delay ability until choice made
    }

    @Override
    public void showPromptMessage(final PlayerView playerView, final String message) {
        CMatchUI.SINGLETON_INSTANCE.showMessage(message);
    }

    public Object showManaPool(final PlayerView player) {
        return null; //not needed since mana pool icons are always visible
    }

    @Override
    public void hideManaPool(final PlayerView player, final Object zoneToRestore) {
        //not needed since mana pool icons are always visible
    }

    @Override
    public boolean openZones(final Collection<ZoneType> zones, final Map<PlayerView, Object> players) {
        if (zones.size() == 1) {
            switch (zones.iterator().next()) {
            case Battlefield:
            case Hand:
                return true; //don't actually need to open anything, but indicate that zone can be opened
            default:
                return false;
            }
        }
        return false;
    }

    @Override
    public void restoreOldZones(final Map<PlayerView, Object> playersToRestoreZonesFor) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<CardView, Integer> assignDamage(final CardView attacker,
            final List<CardView> blockers, final int damage,
            final GameEntityView defender, final boolean overrideOrder) {
        final Object[] result = { null }; // how else can I extract a value from EDT thread?
        FThreads.invokeInEdtAndWait(new Runnable() {
            @Override
            public void run() {
                VAssignDamage v = new VAssignDamage(attacker, blockers, damage, defender, overrideOrder);
                result[0] = v.getDamageMap();
            }});
        return (Map<CardView, Integer>)result[0];
    }

    @Override
    public void hear(LobbyPlayer player, String message) {
        FNetOverlay.SINGLETON_INSTANCE.addMessage(player.getName(), message);
    }

    @Override
    public void startNewMatch(final Match match) {
        SOverlayUtils.startGameOverlay();
        SOverlayUtils.showOverlay();
        FThreads.invokeInEdtLater(new Runnable() {
            @Override
            public void run() {
                MatchUtil.startGame(match);
            }
        });
    }

    @Override
    public void openView(List<Player> sortedPlayers) {
        Game game = MatchUtil.getGame();
        game.getGameLog().addObserver(CLog.SINGLETON_INSTANCE);

        List<PlayerView> sortedPlayerViews = new ArrayList<PlayerView>();
        for (Player p : sortedPlayers) {
            sortedPlayerViews.add(PlayerView.get(p));
        }
        CMatchUI.SINGLETON_INSTANCE.initMatch(sortedPlayerViews, MatchUtil.getHumanCount() != 1);

        actuateMatchPreferences();

        Singletons.getControl().setCurrentScreen(FScreen.MATCH_SCREEN);
        SDisplayUtil.showTab(EDocID.REPORT_LOG.getDoc());

        // per player observers were set in CMatchUI.SINGLETON_INSTANCE.initMatch
        //Set Field shown to current player.
        if (MatchUtil.getHumanCount() > 0) {
            final VField nextField = CMatchUI.SINGLETON_INSTANCE.getFieldViewFor(sortedPlayerViews.get(0));
            SDisplayUtil.showTab(nextField);
        }
        SOverlayUtils.hideOverlay();
    }

    @Override
    public void afterGameEnd() {
        Singletons.getView().getNavigationBar().closeTab(FScreen.MATCH_SCREEN);
    }

    /**
     * TODO: Needs to be reworked for efficiency with rest of prefs saves in
     * codebase.
     */
    public void writeMatchPreferences() {
        final ForgePreferences prefs = FModel.getPreferences();
        final List<VField> fieldViews = VMatchUI.SINGLETON_INSTANCE.getFieldViews();

        // AI field is at index [1]
        PhaseIndicator fvAi = fieldViews.get(1).getPhaseIndicator();
        prefs.setPref(FPref.PHASE_AI_UPKEEP, String.valueOf(fvAi.getLblUpkeep().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_DRAW, String.valueOf(fvAi.getLblDraw().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_MAIN1, String.valueOf(fvAi.getLblMain1().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_BEGINCOMBAT, String.valueOf(fvAi.getLblBeginCombat().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_DECLAREATTACKERS, String.valueOf(fvAi.getLblDeclareAttackers().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_DECLAREBLOCKERS, String.valueOf(fvAi.getLblDeclareBlockers().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_FIRSTSTRIKE, String.valueOf(fvAi.getLblFirstStrike().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_COMBATDAMAGE, String.valueOf(fvAi.getLblCombatDamage().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_ENDCOMBAT, String.valueOf(fvAi.getLblEndCombat().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_MAIN2, String.valueOf(fvAi.getLblMain2().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_EOT, String.valueOf(fvAi.getLblEndTurn().getEnabled()));
        prefs.setPref(FPref.PHASE_AI_CLEANUP, String.valueOf(fvAi.getLblCleanup().getEnabled()));

        // Human field is at index [0]
        PhaseIndicator fvHuman = fieldViews.get(0).getPhaseIndicator();
        prefs.setPref(FPref.PHASE_HUMAN_UPKEEP, String.valueOf(fvHuman.getLblUpkeep().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_DRAW, String.valueOf(fvHuman.getLblDraw().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_MAIN1, String.valueOf(fvHuman.getLblMain1().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_BEGINCOMBAT, String.valueOf(fvHuman.getLblBeginCombat().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_DECLAREATTACKERS, String.valueOf(fvHuman.getLblDeclareAttackers().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_DECLAREBLOCKERS, String.valueOf(fvHuman.getLblDeclareBlockers().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_FIRSTSTRIKE, String.valueOf(fvHuman.getLblFirstStrike().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_COMBATDAMAGE, String.valueOf(fvHuman.getLblCombatDamage().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_ENDCOMBAT, String.valueOf(fvHuman.getLblEndCombat().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_MAIN2, String.valueOf(fvHuman.getLblMain2().getEnabled()));
        prefs.setPref(FPref.PHASE_HUMAN_EOT, fvHuman.getLblEndTurn().getEnabled());
        prefs.setPref(FPref.PHASE_HUMAN_CLEANUP, fvHuman.getLblCleanup().getEnabled());

        prefs.save();
    }

    /**
     * TODO: Needs to be reworked for efficiency with rest of prefs saves in
     * codebase.
     */
    private void actuateMatchPreferences() {
        final ForgePreferences prefs = FModel.getPreferences();
        final List<VField> fieldViews = VMatchUI.SINGLETON_INSTANCE.getFieldViews();

        // Human field is at index [0]
        PhaseIndicator fvHuman = fieldViews.get(0).getPhaseIndicator();
        fvHuman.getLblUpkeep().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_UPKEEP));
        fvHuman.getLblDraw().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_DRAW));
        fvHuman.getLblMain1().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_MAIN1));
        fvHuman.getLblBeginCombat().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_BEGINCOMBAT));
        fvHuman.getLblDeclareAttackers().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_DECLAREATTACKERS));
        fvHuman.getLblDeclareBlockers().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_DECLAREBLOCKERS));
        fvHuman.getLblFirstStrike().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_FIRSTSTRIKE));
        fvHuman.getLblCombatDamage().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_COMBATDAMAGE));
        fvHuman.getLblEndCombat().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_ENDCOMBAT));
        fvHuman.getLblMain2().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_MAIN2));
        fvHuman.getLblEndTurn().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_EOT));
        fvHuman.getLblCleanup().setEnabled(prefs.getPrefBoolean(FPref.PHASE_HUMAN_CLEANUP));

        // AI field is at index [1], ...
        for (int i = 1; i < fieldViews.size(); i++) {
            PhaseIndicator fvAi = fieldViews.get(i).getPhaseIndicator();
            fvAi.getLblUpkeep().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_UPKEEP));
            fvAi.getLblDraw().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_DRAW));
            fvAi.getLblMain1().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_MAIN1));
            fvAi.getLblBeginCombat().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_BEGINCOMBAT));
            fvAi.getLblDeclareAttackers().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_DECLAREATTACKERS));
            fvAi.getLblDeclareBlockers().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_DECLAREBLOCKERS));
            fvAi.getLblFirstStrike().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_FIRSTSTRIKE));
            fvAi.getLblCombatDamage().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_COMBATDAMAGE));
            fvAi.getLblEndCombat().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_ENDCOMBAT));
            fvAi.getLblMain2().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_MAIN2));
            fvAi.getLblEndTurn().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_EOT));
            fvAi.getLblCleanup().setEnabled(prefs.getPrefBoolean(FPref.PHASE_AI_CLEANUP));
        }

        //Singletons.getView().getViewMatch().setLayoutParams(prefs.getPref(FPref.UI_LAYOUT_PARAMS));
    }
}
