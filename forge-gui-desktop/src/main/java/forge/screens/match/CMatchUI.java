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
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

import forge.FThreads;
import forge.ImageCache;
import forge.LobbyPlayer;
import forge.Singletons;
import forge.UiCommand;
import forge.assets.FSkinProp;
import forge.control.KeyboardShortcuts;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deckchooser.FDeckViewer;
import forge.game.GameEntity;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.combat.CombatView;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.IHasIcon;
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
import forge.item.PaperCard;
import forge.match.AbstractGuiGame;
import forge.menus.IMenuProvider;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.controllers.CAntes;
import forge.screens.match.controllers.CCombat;
import forge.screens.match.controllers.CDetailPicture;
import forge.screens.match.controllers.CDev;
import forge.screens.match.controllers.CDock;
import forge.screens.match.controllers.CLog;
import forge.screens.match.controllers.CPlayers;
import forge.screens.match.controllers.CPrompt;
import forge.screens.match.controllers.CStack;
import forge.screens.match.menus.CMatchUIMenus;
import forge.screens.match.views.VCommand;
import forge.screens.match.views.VField;
import forge.screens.match.views.VHand;
import forge.toolbox.FButton;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.MouseTriggerEvent;
import forge.toolbox.special.PhaseIndicator;
import forge.toolbox.special.PhaseLabel;
import forge.util.FCollectionView;
import forge.util.ITriggerEvent;
import forge.util.gui.SGuiChoose;
import forge.util.gui.SOptionPane;
import forge.view.FView;
import forge.view.arcane.CardPanel;
import forge.view.arcane.FloatingCardArea;

/**
 * Constructs instance of match UI controller, used as a single point of
 * top-level control for child UIs. Tasks targeting the view of individual
 * components are found in a separate controller for that component and
 * should not be included here.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public final class CMatchUI
    extends AbstractGuiGame
    implements ICDoc, IMenuProvider {

    private final FScreen screen;

    private FCollectionView<PlayerView> sortedPlayers;
    /** Players attached to this UI */
    private VMatchUI view;
    private boolean allHands;
    private boolean showOverlay = true;

    private IVDoc<? extends ICDoc> selectedDocBeforeCombat;
    public final Map<String, String> avatarImages = new HashMap<String, String>();
    private final CAntes cAntes = new CAntes(this);
    private final CCombat cCombat = new CCombat();
    private final CDetailPicture cDetailPicture = new CDetailPicture(this);
    private final CDev cDev = new CDev(this);
    private final CDock cDock = new CDock(this);
    private final CLog cLog = new CLog(this);
    private final CPlayers cPlayers = new CPlayers(this);
    private final CPrompt cPrompt = new CPrompt(this);
    private final CStack cStack = new CStack(this);
    private final TargetingOverlay targetingOverlay = new TargetingOverlay(this);
    private final Map<EDocID, IVDoc<? extends ICDoc>> myDocs;

    public CMatchUI() {
        this.view = new VMatchUI(this);
        this.screen = FScreen.getMatchScreen(this, view);
        Singletons.getView().getLpnDocument().add(targetingOverlay.getPanel(), FView.TARGETING_LAYER);
        targetingOverlay.getPanel().setSize(Singletons.getControl().getDisplaySize());
        this.myDocs = new EnumMap<EDocID, IVDoc<? extends ICDoc>>(EDocID.class);
        this.myDocs.put(EDocID.CARD_PICTURE, getCDetailPicture().getCPicture().getView());
        this.myDocs.put(EDocID.CARD_DETAIL, getCDetailPicture().getCDetail().getView());
        this.myDocs.put(EDocID.CARD_ANTES, getCAntes().getView());
        this.myDocs.put(EDocID.REPORT_MESSAGE, getCPrompt().getView());
        this.myDocs.put(EDocID.REPORT_STACK, getCStack().getView());
        this.myDocs.put(EDocID.REPORT_COMBAT, getCCombat().getView());
        this.myDocs.put(EDocID.REPORT_LOG, getCLog().getView());
        this.myDocs.put(EDocID.REPORT_PLAYERS, getCPlayers().getView());
        this.myDocs.put(EDocID.DEV_MODE, getCDev().getView());
        this.myDocs.put(EDocID.BUTTON_DOCK, getCDock().getView());;
    }

    private void registerDocs() {
        for (final Entry<EDocID, IVDoc<? extends ICDoc>> doc : myDocs.entrySet()) {
            doc.getKey().setDoc(doc.getValue());
        }
    }

    FScreen getScreen() {
        return this.screen;
    }
    public boolean isCurrentScreen() {
        return Singletons.getControl().getCurrentScreen() == this.screen;
    }

    private boolean isInGame() {
        return getGameView() != null;
    }

    @Override
    public void setGameView(final GameView gameView) {
        super.setGameView(gameView);
        screen.setTabCaption(gameView.getTitle());
    }

    @Override
    protected void updateCurrentPlayer(final PlayerView player) {
        // No action necessary
    }

    public CAntes getCAntes() {
        return cAntes;
    }
    public CCombat getCCombat() {
        return cCombat;
    }
    public CDetailPicture getCDetailPicture() {
        return cDetailPicture;
    }
    public CDev getCDev() {
        return cDev;
    }
    public CDock getCDock() {
        return cDock;
    }
    public CLog getCLog() {
        return cLog;
    }
    public CPlayers getCPlayers() {
        return cPlayers;
    }
    public CPrompt getCPrompt() {
        return cPrompt;
    }
    public CStack getCStack() {
        return cStack;
    }
    public TargetingOverlay getTargetingOverlay() {
        return targetingOverlay;
    }

    /**
     * View deck list.
     */
    public void viewDeckList() {
        if (!isInGame()) {
            return;
        }
        final Deck deck = getGameView().getDeck(getCurrentPlayer().getLobbyPlayerName());
        if (deck != null) {
            FDeckViewer.show(deck);
        }
    }

    private SkinImage getPlayerAvatar(final PlayerView p, final int defaultIndex) {
         if (avatarImages.containsKey(p.getLobbyPlayerName())) {
            return ImageCache.getIcon(avatarImages.get(p.getLobbyPlayerName()));
        }

        int avatarIdx = p.getAvatarIndex();
        return FSkin.getAvatars().get(avatarIdx >= 0 ? avatarIdx : defaultIndex);
    }

    private void setAvatar(VField view, SkinImage img) {
        view.getLblAvatar().setIcon(img);
        view.getLblAvatar().getResizeTimer().start();
    }

    public void initMatch(final FCollectionView<PlayerView> sortedPlayers, final FCollectionView<PlayerView> myPlayers) {
        this.sortedPlayers = sortedPlayers;
        this.setLocalPlayers(myPlayers);
        allHands = sortedPlayers.size() == getLocalPlayerCount();

        final String[] indices = FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",");

        final List<VField> fields = new ArrayList<VField>();
        final List<VCommand> commands = new ArrayList<VCommand>();

        int i = 0;
        for (final PlayerView p : sortedPlayers) {
            final boolean mirror = !isLocalPlayer(p);
            // A field must be initialized after it's instantiated, to update player info.
            // No player, no init.
            final EDocID fieldDoc = EDocID.Fields[i];
            final VField f = new VField(this, fieldDoc, p, mirror);
            fields.add(f);
            myDocs.put(fieldDoc, f);

            final EDocID commandDoc = EDocID.Commands[i];
            final VCommand c = new VCommand(this, commandDoc, p, mirror);
            commands.add(c);
            myDocs.put(commandDoc, c);

            //setAvatar(f, new ImageIcon(FSkin.getAvatars().get()));
            setAvatar(f, getPlayerAvatar(p, Integer.parseInt(indices[i > 2 ? 1 : 0])));
            f.getLayoutControl().initialize();
            c.getLayoutControl().initialize();
            i++;
        }

        view.setCommandViews(commands);
        view.setFieldViews(fields);

        getCPlayers().getView().init(this.sortedPlayers);

        initHandViews();
        registerDocs();
    }

    private void initHandViews() {
        final List<VHand> hands = new ArrayList<VHand>();

        int i = 0;
        for (final PlayerView p : sortedPlayers) {
            if (allHands || isLocalPlayer(p) || CardView.mayViewAny(p.getHand(), getCurrentPlayer())) {
                final EDocID doc = EDocID.Hands[i];
                final VHand newHand = new VHand(this, doc, p);
                newHand.getLayoutControl().initialize();
                hands.add(newHand);
                myDocs.put(doc, newHand);
            }
            i++;
        }

        view.setHandViews(hands);
    }

    /**
     * Resets all phase buttons in all fields to "inactive", so highlight won't
     * be drawn on them. "Enabled" state remains the same.
     */
    public void resetAllPhaseButtons() {
        for (final VField v : view.getFieldViews()) {
            v.getPhaseIndicator().resetPhaseButtons();
        }
    }

    public List<VField> getFieldViews() {
        return view.getFieldViews();
    }
    public VField getFieldViewFor(final PlayerView p) {
        final int idx = getPlayerIndex(p);
        return idx < 0 ? null : getFieldViews().get(idx);
    }

    public List<VCommand> getCommandViews() {
        return view.getCommandViews();
    }
    public VCommand getCommandFor(final PlayerView p) {
        final int idx = getPlayerIndex(p);
        return idx < 0 ? null : getCommandViews().get(idx);
    }

    public List<VHand> getHandViews() {
        return view.getHands();
    }
    public VHand getHandFor(final PlayerView p) {
        final int idx = getPlayerIndex(p);
        final List<VHand> allHands = getHandViews();
        return idx < 0 || idx >= allHands.size() ? null : allHands.get(idx);
    }

    /**
     * Checks if game control should stop at a phase, for either a forced
     * programmatic stop, or a user-induced phase toggle.
     * 
     * @param turn
     *            the {@link Player} at whose phase might be stopped.
     * @param phase
     *            the {@link PhaseType} at which might be stopped.
     * @return boolean whether the current GUI calls for a stop at the specified
     *         phase of the specified player.
     */
    @Override
    public final boolean stopAtPhase(final PlayerView turn, final PhaseType phase) {
        final VField vf = getFieldViewFor(turn);
        final PhaseLabel label = vf.getPhaseIndicator() .getLabelFor(phase);
        return label == null || label.getEnabled();
    }

    @Override
    public void setCard(final CardView c) {
        this.setCard(c, false);
    }

    public void setCard(final CardView c, final boolean isInAltState) {
        FThreads.assertExecutedByEdt(true);
        cDetailPicture.showCard(c, isInAltState);
    }

    public void setCard(final InventoryItem item) {
        cDetailPicture.showItem(item);
    }

    private int getPlayerIndex(PlayerView player) {
        return sortedPlayers.indexOf(player);
    }

    @Override
    public void showCombat() {
        final CombatView combat = getGameView().getCombat();
        if (combat != null && combat.getNumAttackers() > 0 && getGameView().peekStack() == null) {
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
        getCCombat().setModel(combat);
        getCCombat().update();
    } // showCombat(CombatView)

    @Override
    public void updateZones(final List<Pair<PlayerView, ZoneType>> zonesToUpdate) {
        for (final Pair<PlayerView, ZoneType> kv : zonesToUpdate) {
            final PlayerView owner = kv.getKey();
            final ZoneType zt = kv.getValue();

            switch (zt) {
            case Battlefield:
                getFieldViewFor(owner).getTabletop().setupPlayZone();
                break;
            case Hand:
                final VHand vHand = getHandFor(owner);
                if (vHand != null) {
                    vHand.getLayoutControl().updateHand();
                }
                getFieldViewFor(owner).getDetailsPanel().updateZones();
                FloatingCardArea.refresh(owner, zt);
                break;
            case Command:
                getCommandFor(owner).getTabletop().setupPlayZone();
                break;
            case Ante:
                cAntes.update();
                break;
            default:
                final VField vf = getFieldViewFor(owner);
                if (vf != null) {
                    vf.getDetailsPanel().updateZones();
                }
                FloatingCardArea.refresh(owner, zt);
                break;
            }
        }
    }

    // Player's mana pool changes
    @Override
    public void updateManaPool(final Iterable<PlayerView> manaPoolUpdate) {
        for (final PlayerView p : manaPoolUpdate) {
            getFieldViewFor(p).getDetailsPanel().updateManaPool();
        }

    }

    // Player's lives and poison counters
    @Override
    public void updateLives(final Iterable<PlayerView> livesUpdate) {
        for (final PlayerView p : livesUpdate) {
            getFieldViewFor(p).updateDetails();
        }
    }

    @Override
    public void updateSingleCard(final CardView c) {
        final ZoneType zone = c.getZone();
        if (zone == null) { return; }

        switch (zone) {
        case Battlefield:
            final VField battlefield = getFieldViewFor(c.getController());
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
            final VCommand command = getCommandFor(c.getController());
            if (command != null) {
                command.getTabletop().updateCard(c, false);
            }
            break;
        default:
            break;
        }
    }

    @Override
    public List<JMenu> getMenus() {
        return new CMatchUIMenus(this).getMenus(cDev);
    }

    @Override
    public UiCommand getCommandOnSelect() {
        return null;
    }

    @Override
    public void register() {
        initHandViews();
        registerDocs();
    }

    @Override
    public void initialize() {
        Singletons.getControl().getForgeMenu().setProvider(this);
        updatePlayerControl();
        KeyboardShortcuts.attachKeyboardShortcuts(this);
        for (final IVDoc<? extends ICDoc> view : myDocs.values()) {
            final ICDoc layoutControl = view.getLayoutControl();
            layoutControl.initialize();
            layoutControl.update();
        }
    }

    @Override
    public void update() {
    }

    public void repaintCardOverlays() {
        final List<CardPanel> panels = getVisibleCardPanels();
        for (final CardPanel panel : panels) {
            panel.repaintOverlays();
        }
    }
    private List<CardPanel> getVisibleCardPanels() {
        final List<CardPanel> panels = new ArrayList<CardPanel>();
        for (final VHand h : view.getHands()) {
            panels.addAll(h.getHandArea().getCardPanels());
        }
        for (final VField f : view.getFieldViews()) {
            panels.addAll(f.getTabletop().getCardPanels());
        }
        for (final VCommand c : view.getCommandViews()) {
            panels.addAll(c.getTabletop().getCardPanels());
        }
        return panels;
    }

    @Override
    public boolean resetForNewGame() {
        return true;
    }

    @Override
    public IButton getBtnOK(final PlayerView playerView) {
        return view.getBtnOK();
    }

    @Override
    public IButton getBtnCancel(final PlayerView playerView) {
        return view.getBtnCancel();
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
        SDisplayUtil.remind(getCPrompt().getView());
    }

    @Override
    public void updatePhase() {
        final PlayerView p = getGameView().getPlayerTurn();
        final PhaseType ph = getGameView().getPhase();
        PhaseLabel lbl = getFieldViewFor(p).getPhaseIndicator().getLabelFor(ph);

        resetAllPhaseButtons();
        if (lbl != null) {
            lbl.setActive(true);
        }
    }

    @Override
    public void updateTurn(final PlayerView player) {
        VField nextField = getFieldViewFor(player);
        SDisplayUtil.showTab(nextField);
        cPrompt.updateText();
        repaintCardOverlays();
    }

    @Override
    public void updatePlayerControl() {
        initHandViews();
        SLayoutIO.loadLayout(null);
        view.populate();
        for (final VHand h : getHandViews()) {
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
        FloatingCardArea.closeAll(); //ensure floating card areas cleared and closed after the game
        final GameView gameView = getGameView();
        if (hasLocalPlayers() || gameView.isMatchOver()) {
            new ViewWinLose(gameView, this);
        }
        if (showOverlay) {
            SOverlayUtils.showOverlay();
        }
    }

    @Override
    public void updateStack() {
        getCStack().update();
    }

    /**
     * Clear all visually highlighted card panels on the battlefield.
     */
    public void clearPanelSelections() {
        final List<VField> fields = view.getFieldViews();
        for (final VField field : fields) {
            for (final CardPanel p : field.getTabletop().getCardPanels()) {
                p.setSelected(false);
            }
        }
    }

    /**
     * Highlight a card on the playfield.
     * 
     * @param card
     *            a card to be highlighted
     */
    @Override
    public void setPanelSelection(final CardView card) {
        for (final VField v : view.getFieldViews()) {
            final List<CardPanel> panels = v.getTabletop().getCardPanels();
            for (final CardPanel p : panels) {
                if (p.getCard().equals(card)) {
                    p.setSelected(true);
                    return;
                }
            }
        }
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
                            cPrompt.selectAbility(ab);
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
        cPrompt.setMessage(message);
    }

    public Object showManaPool(final PlayerView player) {
        return null; //not needed since mana pool icons are always visible
    }

    @Override
    public void hideManaPool(final PlayerView player, final Object zoneToRestore) {
        //not needed since mana pool icons are always visible
    }

    @Override
    public Map<CardView, Integer> assignDamage(final CardView attacker,
            final List<CardView> blockers, final int damage,
            final GameEntityView defender, final boolean overrideOrder) {
        if (damage <= 0) {
            return Collections.emptyMap();
        }

        // If the first blocker can absorb all of the damage, don't show the Assign Damage dialog
        final CardView firstBlocker = blockers.get(0);
        if (!overrideOrder && !attacker.getCurrentState().hasDeathtouch() && firstBlocker.getLethalDamage() >= damage) {
            return ImmutableMap.of(firstBlocker, damage);
        }

        final AtomicReference<Map<CardView, Integer>> result = new AtomicReference<Map<CardView,Integer>>();
        FThreads.invokeInEdtAndWait(new Runnable() {
            @Override
            public void run() {
                final VAssignDamage v = new VAssignDamage(CMatchUI.this, attacker, blockers, damage, defender, overrideOrder);
                result.set(v.getDamageMap());
            }});
        return result.get();
    }

    @Override
    public void hear(LobbyPlayer player, String message) {
        FNetOverlay.SINGLETON_INSTANCE.addMessage(player.getName(), message);
    }

    @Override
    public void openView(final FCollectionView<PlayerView> myPlayers) {
        final GameView gameView = getGameView();
        gameView.getGameLog().addObserver(getCLog());

        initMatch(gameView.getPlayers(), myPlayers);

        actuateMatchPreferences();

        Singletons.getControl().setCurrentScreen(screen);
        SDisplayUtil.showTab(EDocID.REPORT_LOG.getDoc());

        // per player observers were set in CMatchUI.SINGLETON_INSTANCE.initMatch
        //Set Field shown to current player.
        //if (util.getHumanCount() > 0) {
            final VField nextField = getFieldViewFor(gameView.getPlayers().get(0));
            SDisplayUtil.showTab(nextField);
        //}
        SOverlayUtils.hideOverlay();
    }

    @Override
    public void afterGameEnd() {
        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override public void run() {
                Singletons.getView().getNavigationBar().closeTab(screen);
            }
        });
    }

    @Override
    public int showOptionDialog(final String message, final String title, final FSkinProp icon, final String[] options, final int defaultOption) {
        return FOptionPane.showOptionDialog(message, title, icon == null ? null : FSkin.getImage(icon), options, defaultOption);
    }

    @Override
    public int showCardOptionDialog(final CardView card, final String message, final String title, final FSkinProp skinIcon, final String[] options, final int defaultOption) {
        if (card != null) {
            setCard(card);
        }
        return showOptionDialog(message, title, skinIcon, options, defaultOption);
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput, final String[] inputOptions) {
        return FOptionPane.showInputDialog(message, title, icon == null ? null : FSkin.getImage(icon), initialInput, inputOptions);
    }

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices, final T selected, final Function<T, String> display) {
        /*if ((choices != null && !choices.isEmpty() && choices.iterator().next() instanceof GameObject) || selected instanceof GameObject) {
            System.err.println("Warning: GameObject passed to GUI! Printing stack trace.");
            Thread.dumpStack();
        }*/
        return GuiChoose.getChoices(message, min, max, choices, selected, display, this);
    }

    @Override
    public <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax,
            final List<T> sourceChoices, final List<T> destChoices, final CardView referenceCard, final boolean sideboardingMode) {
        /*if ((sourceChoices != null && !sourceChoices.isEmpty() && sourceChoices.iterator().next() instanceof GameObject)
                || (destChoices != null && !destChoices.isEmpty() && destChoices.iterator().next() instanceof GameObject)) {
            System.err.println("Warning: GameObject passed to GUI! Printing stack trace.");
            Thread.dumpStack();
        }*/
        return GuiChoose.order(title, top, remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices, referenceCard, sideboardingMode, this);
    }

    @Override
    public List<PaperCard> sideboard(final CardPool sideboard, final CardPool main) {
        return GuiChoose.sideboard(this, sideboard.toFlatList(), main.toFlatList());
    }

    @Override
    public GameEntityView chooseSingleEntityForEffect(final String title, final FCollectionView<? extends GameEntity> optionList, final DelayedReveal delayedReveal, final boolean isOptional, final PlayerControllerHuman controller) {
        if (delayedReveal != null) {
            delayedReveal.reveal(controller); //TODO: Merge this into search dialog
        }
        controller.tempShow(optionList);
        final List<GameEntityView> gameEntityViews = GameEntityView.getEntityCollection(optionList);
        if (isOptional) {
            return oneOrNone(title, gameEntityViews);
        }
        return one(title, gameEntityViews);
    }

    @Override
    public void setPlayerAvatar(final LobbyPlayer player, final IHasIcon ihi) {
        avatarImages.put(player.getName(), ihi.getIconImageKey());
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

    @Override
    public boolean isUiSetToSkipPhase(final PlayerView playerTurn, final PhaseType phase) {
        final PhaseLabel label = getFieldViewFor(playerTurn).getPhaseIndicator().getLabelFor(phase);
        return label != null && !label.getEnabled();
    }

    /**
     * TODO: Needs to be reworked for efficiency with rest of prefs saves in codebase.
     */
    public void writeMatchPreferences() {
        final ForgePreferences prefs = FModel.getPreferences();
        final List<VField> fieldViews = getFieldViews();

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
     * TODO: Needs to be reworked for efficiency with rest of prefs saves in codebase.
     */
    private void actuateMatchPreferences() {
        final ForgePreferences prefs = FModel.getPreferences();
        final List<VField> fieldViews = getFieldViews();

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

    @Override
    public void message(String message, String title) {
        SOptionPane.showMessageDialog(message, title);
    }

    @Override
    public void showErrorDialog(String message, String title) {
        SOptionPane.showErrorDialog(message, title);
    }

    @Override
    public boolean confirm(final CardView c, final String question, final boolean defaultIsYes, final String[] options) {
        final String title = c == null ? "Question" : c + " - Ability";
        final String questionToUse = StringUtils.isBlank(question) ? "Activate card's ability?" : question;
        final String[] opts = options == null ? SGuiChoose.defaultConfirmOptions : options;
        final int reply = FOptionPane.showOptionDialog(questionToUse, title, FOptionPane.QUESTION_ICON, opts, defaultIsYes ? 0 : 1);
        return reply == 0;
    }

    @Override
    public boolean showConfirmDialog(final String message, final String title, final String yesButtonText, final String noButtonText, final boolean defaultYes) {
        final String[] options = {yesButtonText, noButtonText};
        final int reply = SOptionPane.showOptionDialog(message, title, SOptionPane.QUESTION_ICON, options, defaultYes ? 0 : 1);
        return reply == 0;
    }

}
