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

import java.awt.Component;
import java.awt.event.KeyEvent;
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
import javax.swing.SwingUtilities;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import forge.FThreads;
import forge.ImageCache;
import forge.LobbyPlayer;
import forge.Singletons;
import forge.assets.FSkinProp;
import forge.control.KeyboardShortcuts;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deckchooser.FDeckViewer;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.combat.CombatView;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.IHasIcon;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;
import forge.gui.GuiUtils;
import forge.gui.SOverlayUtils;
import forge.gui.framework.DragCell;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.gui.framework.SDisplayUtil;
import forge.gui.framework.SLayoutIO;
import forge.gui.framework.VEmptyDoc;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.match.AbstractGuiGame;
import forge.menus.IMenuProvider;
import forge.model.FModel;
import forge.player.PlayerZoneUpdate;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.controllers.CAntes;
import forge.screens.match.controllers.CCombat;
import forge.screens.match.controllers.CDetailPicture;
import forge.screens.match.controllers.CDev;
import forge.screens.match.controllers.CDock;
import forge.screens.match.controllers.CLog;
import forge.screens.match.controllers.CPrompt;
import forge.screens.match.controllers.CStack;
import forge.screens.match.menus.CMatchUIMenus;
import forge.screens.match.views.VField;
import forge.screens.match.views.VHand;
import forge.toolbox.FButton;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.special.PhaseIndicator;
import forge.toolbox.special.PhaseLabel;
import forge.trackable.TrackableCollection;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import forge.util.ITriggerEvent;
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
    private final VMatchUI view;
    private final CMatchUIMenus menus = new CMatchUIMenus(this);
    private final Map<EDocID, IVDoc<? extends ICDoc>> myDocs;
    private final TargetingOverlay targetingOverlay = new TargetingOverlay(this);

    private FCollectionView<PlayerView> sortedPlayers;
    private final Map<String, String> avatarImages = new HashMap<String, String>();
    private boolean allHands;
    private boolean showOverlay = true;

    private IVDoc<? extends ICDoc> selectedDocBeforeCombat;

    private final CAntes cAntes = new CAntes(this);
    private final CCombat cCombat = new CCombat();
    private final CDetailPicture cDetailPicture = new CDetailPicture(this);
    private final CDev cDev = new CDev(this);
    private final CDock cDock = new CDock(this);
    private final CLog cLog = new CLog(this);
    private final CPrompt cPrompt = new CPrompt(this);
    private final CStack cStack = new CStack(this);

    public CMatchUI() {
        this.view = new VMatchUI(this);
        this.screen = FScreen.getMatchScreen(this, view);
        this.myDocs = new EnumMap<EDocID, IVDoc<? extends ICDoc>>(EDocID.class);
        this.myDocs.put(EDocID.CARD_PICTURE, cDetailPicture.getCPicture().getView());
        this.myDocs.put(EDocID.CARD_DETAIL, cDetailPicture.getCDetail().getView());
        this.myDocs.put(EDocID.CARD_ANTES, cAntes.getView());
        this.myDocs.put(EDocID.REPORT_MESSAGE, getCPrompt().getView());
        this.myDocs.put(EDocID.REPORT_STACK, getCStack().getView());
        this.myDocs.put(EDocID.REPORT_COMBAT, cCombat.getView());
        this.myDocs.put(EDocID.REPORT_LOG, cLog.getView());
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

    public String getAvatarImage(final String playerName) {
        return avatarImages.get(playerName);
    }

    @Override
    public void setGameView(GameView gameView0) {
        super.setGameView(gameView0);
        gameView0 = getGameView(); //ensure updated game view used for below logic
        if (gameView0 == null) { return; }

        cDetailPicture.setGameView(gameView0);
        screen.setTabCaption(gameView0.getTitle());
        if (sortedPlayers != null) {
            FThreads.invokeInEdtNowOrLater(new Runnable() {
                @Override public final void run() {
                    for (final VField f : getFieldViews()) {
                        f.updateDetails();
                        f.updateZones();
                        f.updateManaPool();
                        f.getTabletop().update();
                    }
                    for (final VHand h : getHandViews()) {
                        h.getLayoutControl().updateHand();
                    }
                }
            });
        }
    }

    @Override
    protected void updateCurrentPlayer(final PlayerView player) {
        // Update toggle buttons in dev mdoe panel
        getCDev().update();
    }

    public CDev getCDev() {
        return cDev;
    }
    public CDock getCDock() {
        return cDock;
    }
    CPrompt getCPrompt() {
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

        final int avatarIdx = p.getAvatarIndex();
        return FSkin.getAvatars().get(avatarIdx >= 0 ? avatarIdx : defaultIndex);
    }

    private void initMatch(final FCollectionView<PlayerView> sortedPlayers, final Collection<PlayerView> myPlayers) {
        this.sortedPlayers = sortedPlayers;
        allHands = sortedPlayers.size() == getLocalPlayerCount();

        final String[] indices = FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",");

        final List<VField> fields = new ArrayList<VField>();
        Singletons.getView().getLpnDocument().add(targetingOverlay.getPanel(), FView.TARGETING_LAYER);
        targetingOverlay.getPanel().setSize(Singletons.getControl().getDisplaySize());

        int i = 0;
        for (final PlayerView p : sortedPlayers) {
            final boolean mirror = !isLocalPlayer(p);
            // A field must be initialized after it's instantiated, to update player info.
            // No player, no init.
            final EDocID fieldDoc = EDocID.Fields[i];
            final VField f = new VField(this, fieldDoc, p, mirror);
            fields.add(f);
            myDocs.put(fieldDoc, f);

            f.setAvatar(getPlayerAvatar(p, Integer.parseInt(indices[i > 2 ? 1 : 0])));
            f.getLayoutControl().initialize();
            i++;
        }

        view.setFieldViews(fields);

        initHandViews();
        registerDocs();

        screen.setCloseButtonTooltip(getConcedeCaption() + " Game");
    }

    private void initHandViews() {
        final List<VHand> hands = new ArrayList<VHand>();
        final Iterable<PlayerView> localPlayers = getLocalPlayers();

        int i = 0;
        for (final PlayerView p : sortedPlayers) {
            if (allHands || isLocalPlayer(p) || CardView.mayViewAny(p.getHand(), localPlayers)) {
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

    public List<VHand> getHandViews() {
        return view.getHands();
    }
    public VHand getHandFor(final PlayerView p) {
        final int idx = getPlayerIndex(p);
        final List<VHand> allHands = getHandViews();
        return idx < 0 || idx >= allHands.size() ? null : allHands.get(idx);
    }

    @Override
    public void setCard(final CardView c) {
        this.setCard(c, false);
    }

    public void setCard(final CardView c, final boolean isInAltState) {
        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override public void run() {
                cDetailPicture.showCard(c, isInAltState);
            }
        });
    }

    public void setCard(final InventoryItem item) {
        cDetailPicture.showItem(item);
    }

    private int getPlayerIndex(final PlayerView player) {
        return sortedPlayers.indexOf(player);
    }

    @Override
    public void showCombat() {
        final CombatView combat = getGameView().getCombat();
        if (combat != null && combat.getNumAttackers() > 0 && getGameView().peekStack() == null) {
            if (selectedDocBeforeCombat == null) {
                final IVDoc<? extends ICDoc> combatDoc = EDocID.REPORT_COMBAT.getDoc();
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
        cCombat.setModel(combat);
        cCombat.update();
    } // showCombat(CombatView)

    @Override
    public void updateZones(final Iterable<PlayerZoneUpdate> zonesToUpdate) {
        for (final PlayerZoneUpdate update : zonesToUpdate) {
            final PlayerView owner = update.getPlayer();

            boolean setupPlayZone = false, updateHand = false, updateAnte = false, updateZones = false;
            for (final ZoneType zone : update.getZones()) {
                switch (zone) {
                case Battlefield:
                    setupPlayZone = true;
                    break;
                case Ante:
                    updateAnte = true;
                    break;
                case Hand:
                    updateHand = true;
                    //$FALL-THROUGH$
                default:
                    updateZones = true;
                    FloatingCardArea.refresh(owner, zone);
                    break;
                }
            }

            final VField vField = getFieldViewFor(owner);
            if (setupPlayZone) {
                vField.getTabletop().update();
            }
            if (updateHand) {
                final VHand vHand = getHandFor(owner);
                if (vHand != null) {
                    vHand.getLayoutControl().updateHand();
                }
            }
            if (updateAnte) {
                cAntes.update();
            }
            if (updateZones) {
                vField.updateZones();
            }
        }
    }

    // Player's mana pool changes
    @Override
    public void updateManaPool(final Iterable<PlayerView> manaPoolUpdate) {
        for (final PlayerView p : manaPoolUpdate) {
            getFieldViewFor(p).updateManaPool();
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
    public void updateCards(final Iterable<CardView> cards) {
        for (final CardView c : cards) {
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
                    final CardPanel cp = hand.getHandArea().getCardPanel(c.getId());
                    if (cp != null) {
                        cp.setCard(c); //ensure card view updated
                        cp.repaintOverlays();
                    }
                }
                break;
            default:
                break;
            }
        }
    }

    @Override
    public List<JMenu> getMenus() {
        return menus.getMenus();
    }

    @Override
    public void register() {
        initHandViews();
        registerDocs();
        for (final EDocID fieldDoc : EDocID.VarDocs) {
            // Remove unnecessary docs for this match
            if (!myDocs.containsKey(fieldDoc)) {
                final DragCell parent = fieldDoc.getDoc().getParentCell();
                if (parent != null) {
                    parent.removeDoc(fieldDoc.getDoc());
                    fieldDoc.setDoc(new VEmptyDoc(fieldDoc));
                }
            }
        }
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
        FloatingCardArea.closeAll();
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
        return panels;
    }

    /**
     * Find the card panel belonging to a card, bringing up the corresponding
     * window or tab if necessary.
     *
     * @param card
     *            the {@link CardView} to find a panel for.
     * @return a {@link CardPanel}, or {@code null} if no corresponding panel is
     *         found.
     */
    private CardPanel findCardPanel(final CardView card) {
        final int id = card.getId();
        switch (card.getZone()) {
        case Battlefield:
            for (final VField f : view.getFieldViews()) {
                final CardPanel panel = f.getTabletop().getCardPanel(id);
                if (panel != null) {
                    SDisplayUtil.showTab(f);
                    return panel;
                }
            }
            break;
        case Hand:
            for (final VHand h : view.getHands()) {
                final CardPanel panel = h.getHandArea().getCardPanel(id);
                if (panel != null) {
                    SDisplayUtil.showTab(h);
                    return panel;
                }
            }
            break;
        case Command:
        case Exile:
        case Graveyard:
        case Library:
            return FloatingCardArea.getCardPanel(this, card);
        default:
            break;
        }
        return null;
    }

    @Override
    public void updateButtons(final PlayerView owner, final String label1, final String label2, final boolean enable1, final boolean enable2, final boolean focus1) {
        final FButton btn1 = view.getBtnOK(), btn2 = view.getBtnCancel();
        btn1.setText(label1);
        btn2.setText(label2);

        final FButton toFocus = enable1 && focus1 ? btn1 : (enable2 ? btn2 : null);

        // Remove focusable so the right button grabs focus properly
        if (toFocus == btn2)
            btn1.setFocusable(false);
        else if (toFocus == btn1)
            btn2.setFocusable(false);

        btn1.setEnabled(enable1);
        btn2.setEnabled(enable2);

        // ensure we don't steal focus from an overlay
        if (toFocus != null) {
            FThreads.invokeInEdtLater(new Runnable() {
                @Override public final void run() {
                    btn1.setFocusable(true);
                    btn2.setFocusable(true);
                    toFocus.requestFocus();
                }
            });
        }
    }

    @Override
    public void flashIncorrectAction() {
        getCPrompt().remind();
    }

    @Override
    public void updatePhase() {
        final PlayerView p = getGameView().getPlayerTurn();
        final PhaseType ph = getGameView().getPhase();
        final PhaseLabel lbl = p == null ? null : getFieldViewFor(p).getPhaseIndicator().getLabelFor(ph);

        resetAllPhaseButtons();
        if (lbl != null) {
            lbl.setActive(true);
        }
    }

    @Override
    public void updateTurn(final PlayerView player) {
        final VField nextField = getFieldViewFor(player);
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
            new ViewWinLose(gameView, this).show();
        }
        if (showOverlay) {
            SOverlayUtils.showOverlay();
        }
    }

    @Override
    public void updateStack() {
        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override public final void run() {
                getCStack().update();
            }
        });
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
    public SpellAbilityView getAbilityToPlay(final CardView hostCard, final List<SpellAbilityView> abilities, final ITriggerEvent triggerEvent) {
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
        int firstEnabled = -1;
        int shortcut = KeyEvent.VK_1; //use number keys as shortcuts for abilities 1-9
        int index = 0;
        for (final SpellAbilityView ab : abilities) {
            enabled = ab.canPlay();
            if (enabled && firstEnabled < 0) {
                firstEnabled = index;
            }
            GuiUtils.addMenuItem(menu, FSkin.encodeSymbols(ab.toString(), true),
                    shortcut > 0 ? KeyStroke.getKeyStroke(shortcut, 0) : null,
                    new Runnable() {
                        @Override
                        public void run() {
                            getGameController().selectAbility(ab);
                        }
                    }, enabled);
            if (shortcut > 0) {
                shortcut++;
                if (shortcut > KeyEvent.VK_9) {
                    shortcut = 0; //stop adding shortcuts after 9
                }
            }
            index++;
        }

        if (firstEnabled >= 0) { //only show menu if at least one ability can be played
            final CardPanel panel = findCardPanel(hostCard);
            final Component menuParent;
            final int x, y;
            if (panel == null) {
                // Fall back to showing in VPrompt if no panel can be found
                menuParent = getCPrompt().getView().getTarMessage();
                x = 0;
                y = 0;
                SDisplayUtil.showTab(getCPrompt().getView());
            } else {
                final ZoneType zone = hostCard.getZone();
                if (ImmutableList.of(ZoneType.Command, ZoneType.Exile, ZoneType.Graveyard, ZoneType.Library).contains(zone)) {
                    FloatingCardArea.show(this, hostCard.getController(), zone);
                }
                menuParent = panel.getParent();
                x = triggerEvent.getX();
                y = triggerEvent.getY();
            }
            menu.show(menuParent, x, y);

            final int _firstEnabled = firstEnabled;
            SwingUtilities.invokeLater(new Runnable() { //use invoke later to ensure first enabled ability selected by default
                @Override public final void run() {
                    for (int i = 0; i <= _firstEnabled; i++) {
                        menu.dispatchEvent(new KeyEvent(menu, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED));
                    }
                }
            });
        }

        return null; //delay ability until choice made
    }

    @Override
    public void showPromptMessage(final PlayerView playerView, final String message) {
        cPrompt.setMessage(message);
    }

    @Override
    public void showManaPool(final PlayerView player) {
        //not needed since mana pool icons are always visible
    }

    @Override
    public void hideManaPool(final PlayerView player) {
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
    public void openView(final TrackableCollection<PlayerView> myPlayers) {
        final GameView gameView = getGameView();
        gameView.getGameLog().addObserver(cLog);

        // Sort players
        FCollectionView<PlayerView> players = gameView.getPlayers();
        if (players.size() == 2 && myPlayers != null && myPlayers.size() == 1 && myPlayers.get(0).equals(players.get(1))) {
            players = new FCollection<PlayerView>(new PlayerView[] { players.get(1), players.get(0) });
        }
        initMatch(players, myPlayers);

        actuateMatchPreferences();

        Singletons.getControl().setCurrentScreen(screen);
        SDisplayUtil.showTab(EDocID.REPORT_LOG.getDoc());

        SOverlayUtils.hideOverlay();
    }

    @Override
    public void afterGameEnd() {
        Singletons.getView().getLpnDocument().remove(targetingOverlay.getPanel());
        FThreads.invokeInEdtNowOrLater(new Runnable() {
            @Override public void run() {
                Singletons.getView().getNavigationBar().closeTab(screen);
            }
        });
    }

    @Override
    public int showOptionDialog(final String message, final String title, final FSkinProp icon, final List<String> options, final int defaultOption) {
        return FOptionPane.showOptionDialog(message, title, icon == null ? null : FSkin.getImage(icon), options, defaultOption);
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput, final List<String> inputOptions) {
        return FOptionPane.showInputDialog(message, title, icon == null ? null : FSkin.getImage(icon), initialInput, inputOptions);
    }

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final List<T> choices, final T selected, final Function<T, String> display) {
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
    public GameEntityView chooseSingleEntityForEffect(final String title, final List<? extends GameEntityView> optionList, final DelayedReveal delayedReveal, final boolean isOptional) {
        if (delayedReveal != null) {
            reveal(delayedReveal.getMessagePrefix(), delayedReveal.getCards()); //TODO: Merge this into search dialog
        }
        if (isOptional) {
            return oneOrNone(title, optionList);
        }
        return one(title, optionList);
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
        final PhaseIndicator fvAi = fieldViews.get(1).getPhaseIndicator();
        prefs.setPref(FPref.PHASE_AI_UPKEEP,           fvAi.getLblUpkeep().getEnabled());
        prefs.setPref(FPref.PHASE_AI_DRAW,             fvAi.getLblDraw().getEnabled());
        prefs.setPref(FPref.PHASE_AI_MAIN1,            fvAi.getLblMain1().getEnabled());
        prefs.setPref(FPref.PHASE_AI_BEGINCOMBAT,      fvAi.getLblBeginCombat().getEnabled());
        prefs.setPref(FPref.PHASE_AI_DECLAREATTACKERS, fvAi.getLblDeclareAttackers().getEnabled());
        prefs.setPref(FPref.PHASE_AI_DECLAREBLOCKERS,  fvAi.getLblDeclareBlockers().getEnabled());
        prefs.setPref(FPref.PHASE_AI_FIRSTSTRIKE,      fvAi.getLblFirstStrike().getEnabled());
        prefs.setPref(FPref.PHASE_AI_COMBATDAMAGE,     fvAi.getLblCombatDamage().getEnabled());
        prefs.setPref(FPref.PHASE_AI_ENDCOMBAT,        fvAi.getLblEndCombat().getEnabled());
        prefs.setPref(FPref.PHASE_AI_MAIN2,            fvAi.getLblMain2().getEnabled());
        prefs.setPref(FPref.PHASE_AI_EOT,              fvAi.getLblEndTurn().getEnabled());
        prefs.setPref(FPref.PHASE_AI_CLEANUP,          fvAi.getLblCleanup().getEnabled());

        // Human field is at index [0]
        final PhaseIndicator fvHuman = fieldViews.get(0).getPhaseIndicator();
        prefs.setPref(FPref.PHASE_HUMAN_UPKEEP,           fvHuman.getLblUpkeep().getEnabled());
        prefs.setPref(FPref.PHASE_HUMAN_DRAW,             fvHuman.getLblDraw().getEnabled());
        prefs.setPref(FPref.PHASE_HUMAN_MAIN1,            fvHuman.getLblMain1().getEnabled());
        prefs.setPref(FPref.PHASE_HUMAN_BEGINCOMBAT,      fvHuman.getLblBeginCombat().getEnabled());
        prefs.setPref(FPref.PHASE_HUMAN_DECLAREATTACKERS, fvHuman.getLblDeclareAttackers().getEnabled());
        prefs.setPref(FPref.PHASE_HUMAN_DECLAREBLOCKERS,  fvHuman.getLblDeclareBlockers().getEnabled());
        prefs.setPref(FPref.PHASE_HUMAN_FIRSTSTRIKE,      fvHuman.getLblFirstStrike().getEnabled());
        prefs.setPref(FPref.PHASE_HUMAN_COMBATDAMAGE,     fvHuman.getLblCombatDamage().getEnabled());
        prefs.setPref(FPref.PHASE_HUMAN_ENDCOMBAT,        fvHuman.getLblEndCombat().getEnabled());
        prefs.setPref(FPref.PHASE_HUMAN_MAIN2,            fvHuman.getLblMain2().getEnabled());
        prefs.setPref(FPref.PHASE_HUMAN_EOT,              fvHuman.getLblEndTurn().getEnabled());
        prefs.setPref(FPref.PHASE_HUMAN_CLEANUP,          fvHuman.getLblCleanup().getEnabled());

        prefs.save();
    }

    /**
     * TODO: Needs to be reworked for efficiency with rest of prefs saves in codebase.
     */
    private void actuateMatchPreferences() {
        final ForgePreferences prefs = FModel.getPreferences();
        final List<VField> fieldViews = getFieldViews();

        // Human field is at index [0]
        final PhaseIndicator fvHuman = fieldViews.get(0).getPhaseIndicator();
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
            final PhaseIndicator fvAi = fieldViews.get(i).getPhaseIndicator();
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
    public boolean confirm(final CardView c, final String question, final boolean defaultIsYes, final List<String> options) {
        return GuiDialog.confirm(c, question, defaultIsYes, options, this);
    }

    @Override
    public boolean showConfirmDialog(final String message, final String title, final String yesButtonText, final String noButtonText, final boolean defaultYes) {
        final List<String> options = ImmutableList.of(yesButtonText, noButtonText);
        final int reply = SOptionPane.showOptionDialog(message, title, SOptionPane.QUESTION_ICON, options, defaultYes ? 0 : 1);
        return reply == 0;
    }

}
