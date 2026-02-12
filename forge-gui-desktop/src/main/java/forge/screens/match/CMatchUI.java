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

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import forge.ImageCache;
import forge.LobbyPlayer;
import forge.Singletons;
import forge.StaticData;
import forge.ai.GameState;
import forge.card.CardStateName;
import forge.control.KeyboardShortcuts;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deckchooser.FDeckViewer;
import forge.game.GameEntity;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.combat.CombatView;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.event.GameEventSpellRemovedFromStack;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.IHasIcon;
import forge.game.player.Player;
import forge.game.player.PlayerController.FullControlFlag;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.SpellAbilityView;
import forge.game.spellability.StackItemView;
import forge.game.spellability.TargetChoices;
import forge.game.zone.ZoneType;
import forge.gamemodes.net.NetworkDebugLogger;
import forge.gamemodes.net.NetworkGuiGame;
import forge.gui.FNetOverlay;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;
import forge.gui.GuiUtils;
import forge.gui.MenuScroller;
import forge.gui.SOverlayUtils;
import forge.gui.framework.DragCell;
import forge.gui.framework.EDocID;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.gui.framework.SDisplayUtil;
import forge.gui.framework.SLayoutIO;
import forge.gui.framework.VEmptyDoc;
import forge.gui.util.SOptionPane;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.menus.IMenuProvider;
import forge.model.FModel;
import forge.player.PlayerZoneUpdate;
import forge.player.PlayerZoneUpdates;
import forge.screens.match.controllers.CAntes;
import forge.screens.match.controllers.CCombat;
import forge.screens.match.controllers.CDependencies;
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
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.FTextArea;
import forge.toolbox.imaging.FImagePanel;
import forge.toolbox.imaging.FImagePanel.AutoSizeImageMode;
import forge.toolbox.imaging.FImageUtil;
import forge.toolbox.special.PhaseIndicator;
import forge.toolbox.special.PhaseLabel;
import forge.trackable.Tracker;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableTypes;
import forge.util.FSerializableFunction;
import forge.util.ITriggerEvent;
import forge.util.Localizer;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import forge.view.FView;
import forge.view.arcane.CardPanel;
import forge.view.arcane.FloatingZone;
import net.miginfocom.layout.LinkHandler;
import net.miginfocom.swing.MigLayout;

/**
 * Constructs instance of match UI controller, used as a single point of
 * top-level control for child UIs. Tasks targeting the view of individual
 * components are found in a separate controller for that component and
 * should not be included here.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public final class CMatchUI
    extends NetworkGuiGame
    implements ICDoc, IMenuProvider {

    public static final EnumSet<ZoneType> FLOATING_ZONE_TYPES = EnumSet.of(ZoneType.Library, ZoneType.Graveyard, ZoneType.Exile,
            ZoneType.Flashback, ZoneType.Command, ZoneType.Ante, ZoneType.Sideboard, ZoneType.PlanarDeck,
            ZoneType.SchemeDeck, ZoneType.AttractionDeck, ZoneType.ContraptionDeck, ZoneType.Junkyard);

    private final FScreen screen;
    private final VMatchUI view;
    private final CMatchUIMenus menus = new CMatchUIMenus(this);
    private final Map<EDocID, IVDoc<? extends ICDoc>> myDocs;
    private final TargetingOverlay targetingOverlay = new TargetingOverlay(this);

    private FCollectionView<PlayerView> sortedPlayers;
    private final Map<String, String> avatarImages = new HashMap<>();
    private boolean allHands;
    private boolean showOverlay = true;
    private JPopupMenu openAbilityMenu;

    private IVDoc<? extends ICDoc> selectedDocBeforeCombat;

    private final CAntes cAntes = new CAntes(this);
    private final CCombat cCombat = new CCombat();
    private final CDependencies cDependencies = new CDependencies(this);
    private final CDetailPicture cDetailPicture = new CDetailPicture(this);
    private final CDev cDev = new CDev(this);
    private final CDock cDock = new CDock(this);
    private final CLog cLog = new CLog(this);
    private final CPrompt cPrompt = new CPrompt(this);
    private final CStack cStack = new CStack(this);
    private int nextNotifiableStackIndex = 0;

    public CMatchUI() {
        this.view = new VMatchUI(this);
        this.screen = FScreen.getMatchScreen(this, view);
        this.myDocs = new EnumMap<>(EDocID.class);
        this.myDocs.put(EDocID.CARD_PICTURE, cDetailPicture.getCPicture().getView());
        this.myDocs.put(EDocID.CARD_DETAIL, cDetailPicture.getCDetail().getView());
        // only create an ante doc if playing for ante
        if (isPreferenceEnabled(FPref.UI_ANTE)) {
            this.myDocs.put(EDocID.CARD_ANTES, cAntes.getView());
        } else {
            this.myDocs.put(EDocID.CARD_ANTES, null);
        }
        this.myDocs.put(EDocID.REPORT_MESSAGE, getCPrompt().getView());
        this.myDocs.put(EDocID.REPORT_STACK, getCStack().getView());
        this.myDocs.put(EDocID.REPORT_COMBAT, cCombat.getView());
        this.myDocs.put(EDocID.REPORT_DEPENDENCIES, cDependencies.getView());
        this.myDocs.put(EDocID.REPORT_LOG, cLog.getView());
        this.myDocs.put(EDocID.DEV_MODE, getCDev().getView());
        this.myDocs.put(EDocID.BUTTON_DOCK, getCDock().getView());
    }

    private void registerDocs() {
        for (final Entry<EDocID, IVDoc<? extends ICDoc>> doc : myDocs.entrySet()) {
            doc.getKey().setDoc(doc.getValue());
        }
    }

    private static boolean isPreferenceEnabled(final ForgePreferences.FPref preferenceName) {
        return FModel.getPreferences().getPrefBoolean(preferenceName);
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
            FThreads.invokeInEdtNowOrLater(() -> {
                for (final VField f : getFieldViews()) {
                    f.updateDetails();
                    f.updateZones();
                    f.updateManaPool();
                    f.getTabletop().update();
                }
                for (final VHand h : getHandViews()) {
                    h.getLayoutControl().updateHand();
                }
            });
        }
    }

    @Override
    protected void updateCurrentPlayer(final PlayerView player) {
        // Update toggle buttons in dev mode panel
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
        if (!isInGame() || getCurrentPlayer() == null) {
            return;
        }
        final Deck deck = getGameView().getDeck(getCurrentPlayer());
        if (deck != null) {
            FDeckViewer.show(deck);
        }
    }

    public SkinImage getPlayerAvatar(final PlayerView p, final int defaultIndex) {
        if (avatarImages.containsKey(p.getLobbyPlayerName())) {
            return ImageCache.getIcon(avatarImages.get(p.getLobbyPlayerName()));
        }

        final int avatarIdx = p.getAvatarIndex();
        return FSkin.getAvatars().get(avatarIdx >= 0 ? avatarIdx : defaultIndex);
    }

    private void initMatch(final FCollectionView<PlayerView> sortedPlayers, final Collection<PlayerView> myPlayers) {
        this.sortedPlayers = sortedPlayers;
        allHands = sortedPlayers.size() == getLocalPlayerCount();

        // Debug logging for network play
        NetworkDebugLogger.debug("[CMatchUI.initMatch] sortedPlayers count=%d", sortedPlayers.size());
        for (PlayerView p : sortedPlayers) {
            NetworkDebugLogger.debug("[CMatchUI.initMatch]   Player ID=%d, hash=%d, isLocal=%b",
                    p.getId(), System.identityHashCode(p), (myPlayers != null && myPlayers.contains(p)));
        }

        final String[] indices = FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",");

        final List<VField> fields = new ArrayList<>();
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
        final List<VHand> hands = new ArrayList<>();
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
        if (view == null) {
            return null;
        }
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
        FThreads.invokeInEdtNowOrLater(() -> cDetailPicture.showCard(c, isInAltState));
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
                    } else {
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
    public void updateDependencies() {
        cDependencies.update();
    }

    @Override
    public void updateDayTime(String daytime) {
        super.updateDayTime(daytime);
        if ("Day".equals(daytime)) {
            FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage(FSkin.getIcon(FSkinProp.BG_DAY), true);
            getScreen().setDaytime("Day");
        } else {
            FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage(FSkin.getIcon(FSkinProp.BG_NIGHT), true);
            getScreen().setDaytime("Night");
        }
        FView.SINGLETON_INSTANCE.getPnlInsets().repaint();
    }

    @Override
    public void updateZones(final Iterable<PlayerZoneUpdate> zonesToUpdate) {
        for (final PlayerZoneUpdate update : zonesToUpdate) {
            final PlayerView owner = update.getPlayer();

            // Debug logging
            NetworkDebugLogger.debug("[CMatchUI.updateZones] Processing update for player %d, zones=%s, ownerHash=%d",
                    owner.getId(), update.getZones(), System.identityHashCode(owner));

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
                    updateZones = true;
                    FloatingZone.refresh(owner, zone);
                    break;
                default:
                    updateZones = true;
                    FloatingZone.refresh(owner, zone);
                    break;
                }
            }

            if (updateAnte) {
                cAntes.update();
            }
            final VField vField = getFieldViewFor(owner);
            if(vField == null) {
                NetworkDebugLogger.error("[CMatchUI.updateZones] ERROR: vField is null for player %d, sortedPlayers.indexOf=%d",
                        owner.getId(), sortedPlayers.indexOf(owner));
                return;
            }
            if (setupPlayZone) {
                vField.getTabletop().update();
            }
            if (updateHand) {
                final VHand vHand = getHandFor(owner);
                NetworkDebugLogger.debug("[CMatchUI.updateZones] updateHand for player %d, vHand=%s, handSize=%s",
                        owner.getId(), (vHand != null ? "exists" : "NULL"),
                        (owner.getHand() != null ? String.valueOf(owner.getHand().size()) : "null"));
                if (vHand != null) {
                    vHand.getLayoutControl().updateHand();
                }
                // update Cards in Hand
                vField.updateDetails();
            }
            if (updateZones) {
                vField.updateZones();
            }
        }
    }

    @Override
    public Iterable<PlayerZoneUpdate> tempShowZones(final PlayerView controller, final Iterable<PlayerZoneUpdate> zonesToUpdate) {
        List<PlayerZoneUpdate> updatedPlayerZones = Lists.newArrayList();

        for (final PlayerZoneUpdate update : zonesToUpdate) {
            final PlayerView player = update.getPlayer();
                for (final ZoneType zone : update.getZones()) {
                    switch (zone) {
                        case Battlefield: // always shown
                            break;
                        case Hand:  // controller hand always shown
                            if (controller != player) {
                                if (FloatingZone.show(this,player,zone)) {
                                    updatedPlayerZones.add(update);
                                }
                            }
                            break;
                        default:
                            if(!FLOATING_ZONE_TYPES.contains(zone))
                                break;
                            if (FloatingZone.show(this,player,zone)) {
                                updatedPlayerZones.add(update);
                            }
                            break;
                    }
                }
            }
        return updatedPlayerZones;
    }

    @Override
    public void hideZones(final PlayerView controller, final Iterable<PlayerZoneUpdate> zonesToUpdate) {
        if (zonesToUpdate != null) {
            for (final PlayerZoneUpdate update : zonesToUpdate) {
                final PlayerView player = update.getPlayer();
                for (final ZoneType zone : update.getZones()) {
                    if(FLOATING_ZONE_TYPES.contains(zone))
                        FloatingZone.hide(this,player,zone);
                }
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
    public void updateShards(Iterable<PlayerView> shardsUpdate) {
        //mobile adventure only..
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
                FloatingZone.refresh(c.getController(),zone); // in case the card is visible in the zone
                break;
            }
        }
    }

    @Override
    public void setSelectables(final Iterable<CardView> cards) {
        super.setSelectables(cards);
        // update zones on tabletop and floating zones - non-selectable cards may be rendered differently
        FThreads.invokeInEdtNowOrLater(() -> {
            for (final PlayerView p : getGameView().getPlayers()) {
                if (p.getCards(ZoneType.Battlefield) != null) {
                    updateCards(p.getCards(ZoneType.Battlefield));
                }
                if (p.getCards(ZoneType.Hand) != null) {
                    updateCards(p.getCards(ZoneType.Hand));
                }
            }
            FloatingZone.refreshAll();
        });
    }

    @Override
    public void clearSelectables() {
        super.clearSelectables();
        // update zones on tabletop and floating zones - non-selectable cards may be rendered differently
        FThreads.invokeInEdtNowOrLater(() -> {
            for (final PlayerView p : getGameView().getPlayers()) {
                if (p.getCards(ZoneType.Battlefield) != null) {
                    updateCards(p.getCards(ZoneType.Battlefield));
                }
                if (p.getCards(ZoneType.Hand) != null) {
                    updateCards(p.getCards(ZoneType.Hand));
                }
            }
            FloatingZone.refreshAll();
        });
    }

    @Override
    public void refreshField() {
        super.refreshField();
        FThreads.invokeInEdtNowOrLater(() -> {
            for (final PlayerView p : getGameView().getPlayers()) {
                if (p.getCards(ZoneType.Battlefield) != null) {
                    updateCards(p.getCards(ZoneType.Battlefield));
                }
            }
            FloatingZone.refreshAll();
        });
    }

    @Override
    public GameState getGamestate() {
        return null;
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
            if (view == null) {
                continue;
            }
            final ICDoc layoutControl = view.getLayoutControl();
            layoutControl.initialize();
            layoutControl.update();
        }
        FloatingZone.closeAll();
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
        final List<CardPanel> panels = new ArrayList<>();
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
        ZoneType zone = card.getZone();
        if (zone == ZoneType.Battlefield) {
            for (final VField f : view.getFieldViews()) {
                final CardPanel panel = f.getTabletop().getCardPanel(id);
                if (panel != null) {
                    SDisplayUtil.showTab(f);
                    return panel;
                }
            }
        }
        else if (zone == ZoneType.Hand) {
            for (final VHand h : view.getHands()) {
                final CardPanel panel = h.getHandArea().getCardPanel(id);
                if (panel != null) {
                    SDisplayUtil.showTab(h);
                    return panel;
                }
            }
        }
        else if (FLOATING_ZONE_TYPES.contains(zone))
            return FloatingZone.getCardPanel(this, card);
        return null;
    }

    @Override
    public void updateButtons(final PlayerView owner, final String label1, final String label2, final boolean enable1, final boolean enable2, final boolean focus1) {
        final FButton btn1 = view.getBtnOK(), btn2 = view.getBtnCancel();
        btn1.setText(label1);
        btn2.setText(label2);

        final FButton toFocus = enable1 && focus1 ? btn1 : (enable2 ? btn2 : null);

        //pfps This seems wrong so I've commented it out for now and put a replacement in the runnable
        // Remove focusable so the right button grabs focus properly
        //if (toFocus == btn2)
        //btn1.setFocusable(false);
        //else if (toFocus == btn1)
        //btn2.setFocusable(false);

        final Runnable focusRoutine = () -> {
            // The only button that is focusable is the enabled default button
            // This prevents the user from somehow focusing on on some other button
            // and then using the keyboard to try to select it
            btn1.setEnabled(enable1);
            btn2.setEnabled(enable2);
            btn1.setFocusable(enable1 && focus1);
            btn2.setFocusable(enable2 && !focus1);
            // ensure we don't steal focus from an overlay
            if (toFocus != null && !FNetOverlay.SINGLETON_INSTANCE.getTxtInput().hasFocus() ) {
                toFocus.requestFocus(); // focus here even if another window has focus - shouldn't have to do it this way but some popups grab window focus
            }
        };

        if (FThreads.isGuiThread()) { // run this now whether in EDT or not so that it doesn't clobber later stuff
            FThreads.invokeInEdtNowOrLater(focusRoutine);
        } else {
            FThreads.invokeInEdtAndWait(focusRoutine);
        }
    }

    @Override
    public void flashIncorrectAction() {
        getCPrompt().remind();
    }

    @Override
    public void alertUser() {
        getCPrompt().alert();
    }

    @Override
    public void updatePhase(boolean saveState) {
        final PlayerView p = getGameView().getPlayerTurn();
        final PhaseType ph = getGameView().getPhase();
        // this should never happen, but I've seen it periodically... so, need to get to the bottom of it
        PhaseLabel lbl = null;
        if (ph != null) {
            lbl = p == null ? null : getFieldViewFor(p).getPhaseIndicator().getLabelFor(ph);
        } else {
            // not sure what debugging information would help here, log for now
            System.err.println("getGameView().getPhase() returned 'null'");
        }

        resetAllPhaseButtons();
        if (lbl != null) {
            lbl.setActive(true);
        }

        if (openAbilityMenu != null) { //ensure ability menu can't remain open between phases
            openAbilityMenu.setVisible(false);
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
        final PlayerZoneUpdates zones = new PlayerZoneUpdates();
        for (final PlayerView p : sortedPlayers) {
        	zones.add(new PlayerZoneUpdate(p, ZoneType.Hand));
        }
        updateZones(zones);
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
        FloatingZone.closeAll(); //ensure floating card areas cleared and closed after the game
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
        FThreads.invokeInEdtNowOrLater(() -> getCStack().update());
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
            return GuiChoose.oneOrNone(Localizer.getInstance().getMessage("lblChooseAbilityToPlay"), abilities);
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
        final JPopupMenu menu = new JPopupMenu(Localizer.getInstance().getMessage("lblAbilities"));

        boolean enabled;
        int firstEnabled = -1;
        int shortcut = KeyEvent.VK_1; //use number keys as shortcuts for abilities 1-9
        int index = 0;
        for (final SpellAbilityView ab : abilities) {
            enabled = ab.canPlay();
            if (enabled && firstEnabled < 0) {
                firstEnabled = index;
            }
            String s = ab.toString();
            if (s.contains("\n")) {
                s = s.substring(0, s.indexOf("\n"));
            }
            GuiUtils.addMenuItem(menu, FSkin.encodeSymbols(s, true),
                    shortcut > 0 ? KeyStroke.getKeyStroke(shortcut, 0) : null,
                    () -> getGameController().selectAbility(ab), enabled);
            if (shortcut > 0) {
                shortcut++;
                if (shortcut > KeyEvent.VK_9) {
                    shortcut = 0; //stop adding shortcuts after 9
                }
            }
            index++;
        }

        if (firstEnabled >= 0) { //only show menu if at least one ability can be played
            //add scroll area when too big
            // TODO: do we need a user setting for the scrollCount?
            MenuScroller.setScrollerFor(menu, 8, 125, 3, 1);

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
                    FloatingZone.show(this, hostCard.getController(), zone);
                }
                menuParent = panel.getParent();
                x = triggerEvent.getX();
                y = triggerEvent.getY();
            }
            menu.addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                }
                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    openAbilityMenu = null;
                }
                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                    openAbilityMenu = null;
                }
            });
            menu.show(menuParent, x, y);
            openAbilityMenu = menu;

            // TODO seems 1 would now always lead to the first enabled one?
            MenuScroller.setMenuSelectedIndex(menu, firstEnabled, false);
        }

        return null; //delay ability until choice made
    }

    @Override
    public void showPromptMessage(final PlayerView playerView, final String message) {
        cancelWaitingTimer();
        cPrompt.setMessage(message);
    }
    public void showPromptMessageNoCancel(final PlayerView playerView, final String message) {
        cPrompt.setMessage(message);
    }

    @Override
    public void showCardPromptMessage(PlayerView playerView, String message, CardView card) {
        cancelWaitingTimer();
        cPrompt.setMessage(message, card);
    }

    //  no override for now
    public void showPromptMessage(final PlayerView playerView, final String message, final CardView card ) {
        cancelWaitingTimer();
        cPrompt.setMessage(message,card);
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
    public Map<CardView, Integer> assignCombatDamage(final CardView attacker,
            final List<CardView> blockers, final int damage,
            final GameEntityView defender, final boolean overrideOrder, final boolean maySkip) {
        if (damage <= 0) {
            return Collections.emptyMap();
        }

        // If the first blocker can absorb all of the damage, don't show the Assign Damage dialog
        final CardView firstBlocker = blockers.get(0);
        if (!overrideOrder && !attacker.getCurrentState().hasDeathtouch() && firstBlocker.getLethalDamage() >= damage) {
            return ImmutableMap.of(firstBlocker, damage);
        }

        final AtomicReference<Map<CardView, Integer>> result = new AtomicReference<>();
        FThreads.invokeInEdtAndWait(() -> {
            final VAssignCombatDamage v = new VAssignCombatDamage(CMatchUI.this, attacker, blockers, damage, defender, overrideOrder, maySkip);
            result.set(v.getDamageMap());
        });
        return result.get();
    }

    @Override
    public Map<Object, Integer> assignGenericAmount(final CardView effectSource, final Map<Object, Integer> target,
            final int amount, final boolean atLeastOne, final String amountLabel) {
        if (amount <= 0) {
            return Collections.emptyMap();
        }

        final AtomicReference<Map<Object, Integer>> result = new AtomicReference<>();
        FThreads.invokeInEdtAndWait(() -> {
            final VAssignGenericAmount v = new VAssignGenericAmount(CMatchUI.this, effectSource, target, amount, atLeastOne, amountLabel);
            result.set(v.getAssignedMap());
        });
        return result.get();
    }

    @Override
    public void openView(final TrackableCollection<PlayerView> myPlayers) {
        NetworkDebugLogger.log("[CMatchUI.openView] Called");
        final GameView gameView = getGameView();
        gameView.getGameLog().addObserver(cLog);

        // Sort players
        FCollectionView<PlayerView> players = gameView.getPlayers();

        // Debug: Log PlayerView instances from gameView
        NetworkDebugLogger.debug("[CMatchUI.openView] gameView.getPlayers() count=%d", players.size());
        for (PlayerView pv : players) {
            Tracker t = pv.getTracker();
            PlayerView inTracker = t != null ? t.getObj(TrackableTypes.PlayerViewType, pv.getId()) : null;
            NetworkDebugLogger.debug("[CMatchUI.openView]   Player %d: hash=%d, tracker=%s, inTracker=%b, sameInstance=%b",
                    pv.getId(), System.identityHashCode(pv),
                    t != null ? "exists" : "null",
                    inTracker != null,
                    pv == inTracker);
        }

        if (players.size() == 2 && myPlayers != null && myPlayers.size() == 1 && myPlayers.get(0).equals(players.get(1))) {
            players = new FCollection<>(new PlayerView[]{players.get(1), players.get(0)});
        }
        initMatch(players, myPlayers);
        clearSelectables(); //fix uncleared selection

        actuateMatchPreferences();

        Singletons.getControl().setCurrentScreen(screen);
        SDisplayUtil.showTab(EDocID.REPORT_LOG.getDoc());

        SOverlayUtils.hideOverlay();
        //reset every match
        getScreen().setDaytime(null);
        if (FModel.getPreferences().getPrefBoolean(FPref.UI_MATCH_IMAGE_VISIBLE))
            FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage(FSkin.getIcon(FSkinProp.BG_MATCH), true);
        else
            FView.SINGLETON_INSTANCE.getPnlInsets().setForegroundImage((Image)null);
    }

    @Override
    public void afterGameEnd() {
        super.afterGameEnd();
        Singletons.getView().getLpnDocument().remove(targetingOverlay.getPanel());
        FThreads.invokeInEdtNowOrLater(() -> {
            Singletons.getView().getNavigationBar().closeTab(screen);
            LinkHandler.clearWeakReferencesNow();
        });
    }

    @Override
    public int showOptionDialog(final String message, final String title, final FSkinProp icon, final List<String> options, final int defaultOption) {
        return FOptionPane.showOptionDialog(message, title, icon == null ? null : FSkin.getImage(icon), options, defaultOption);
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput, final List<String> inputOptions, boolean isNumeric) {
        return FOptionPane.showInputDialog(message, title, icon == null ? null : FSkin.getImage(icon), initialInput, inputOptions);
    }

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final List<T> choices, final List<T> selected, final FSerializableFunction<T, String> display) {
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
    public List<PaperCard> sideboard(final CardPool sideboard, final CardPool main, final String message) {
        return GuiChoose.sideboard(this, sideboard.toFlatList(), main.toFlatList(), message);
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
    public List<GameEntityView> chooseEntitiesForEffect(final String title, final List<? extends GameEntityView> optionList, final int min, final int max, final DelayedReveal delayedReveal) {
        if (delayedReveal != null) {
            reveal(delayedReveal.getMessagePrefix(), delayedReveal.getCards()); //TODO: Merge this into search dialog
        }
        return (List<GameEntityView>) order(title,Localizer.getInstance().getMessage("lblSelected"), optionList.size() - max, optionList.size() - min, optionList, null, null, false);
    }

    @Override
    public List<CardView> manipulateCardList(final String title, final Iterable<CardView> cards, final Iterable<CardView> manipulable, final boolean toTop, final boolean toBottom, final boolean toAnywhere) {
        return GuiChoose.manipulateCardList(this, title, cards, manipulable, toTop, toBottom, toAnywhere);
    }

    @Override
    public void setPlayerAvatar(final LobbyPlayer player, final IHasIcon ihi) {
        avatarImages.put(player.getName(), ihi.getIconImageKey());
    }

    @Override
    public PlayerZoneUpdates openZones(PlayerView controller, final Collection<ZoneType> zones, final Map<PlayerView, Object> playersWithTargetables, boolean backupLastZones) {
        final PlayerZoneUpdates zonesToUpdate = new PlayerZoneUpdates();
        for (final PlayerView view : playersWithTargetables.keySet()) {
            for(final ZoneType zone : zones) {
                if (zone.equals(ZoneType.Battlefield) || zone.equals(ZoneType.Hand)) {
                    continue;
                }

                if (zone.equals(ZoneType.Stack)) {
                    // TODO: Remove this if we have ever have a Stack zone that's displayable for Counters
                    continue;
                }

                zonesToUpdate.add(new PlayerZoneUpdate(view, zone));
            }
        }

        tempShowZones(controller, zonesToUpdate);
        return zonesToUpdate;
    }

    @Override
    public void restoreOldZones(PlayerView playerView, PlayerZoneUpdates playerZoneUpdates) {
        hideZones(playerView, playerZoneUpdates);
    }

    @Override
    public boolean isUiSetToSkipPhase(final PlayerView playerTurn, final PhaseType phase) {
        PlayerView controlledPlayer = playerTurn.getMindSlaveMaster();
        boolean skippedPhase = true;
        if (controlledPlayer != null) {
            final PhaseLabel controlledLabel = getFieldViewFor(controlledPlayer).getPhaseIndicator().getLabelFor(phase);
            skippedPhase = controlledLabel != null && !controlledLabel.getEnabled();
        }

        final PhaseLabel label = getFieldViewFor(playerTurn).getPhaseIndicator().getLabelFor(phase);
        return skippedPhase && label != null && !label.getEnabled();
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
        //TODO: Rework without that assumption; not true in 4 AI game or hotseat game.
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

    @Override
    public void notifyStackAddition(GameEventSpellAbilityCast event) {
        SpellAbility sa = event.sa();
        String stackNotificationPolicy = FModel.getPreferences().getPref(FPref.UI_STACK_EFFECT_NOTIFICATION_POLICY);
        boolean isAi = sa.getActivatingPlayer().isAI();
        boolean isTrigger = sa.isTrigger();
        int stackIndex = event.stackIndex();
        if (stackIndex == nextNotifiableStackIndex) {
            if (ForgeConstants.STACK_EFFECT_NOTIFICATION_ALWAYS.equals(stackNotificationPolicy) || (ForgeConstants.STACK_EFFECT_NOTIFICATION_AI_AND_TRIGGERED.equals(stackNotificationPolicy) && (isAi || isTrigger))) {
                // We can go and show the modal
                SpellAbilityStackInstance si = event.si();

                MigLayout migLayout = new MigLayout("insets 15, left, gap 30, fill");
                JPanel mainPanel = new JPanel(migLayout);
                final Dimension parentSize = JOptionPane.getRootFrame().getSize();
                Dimension maxSize = new Dimension(1400, parentSize.height - 100);
                mainPanel.setMaximumSize(maxSize);
                mainPanel.setOpaque(false);

                // Big Image
                addBigImageToStackModalPanel(mainPanel, si);

                // Text
                addTextToStackModalPanel(mainPanel,sa,si);

                // Small images
                int numSmallImages = 0;

                // If current effect is a triggered/activated ability of an enchantment card, I want to show the enchanted card
                GameEntityView enchantedEntityView = null;
                Card hostCard = sa.getHostCard();
                if (hostCard.isEnchantment()) {
                    GameEntity enchantedEntity = hostCard.getEntityAttachedTo();
                    if (enchantedEntity != null) {
                        enchantedEntityView = enchantedEntity.getView();
                        numSmallImages++;
                    } else if ((sa.getRootAbility() != null)
                            && (sa.getRootAbility().getPaidList("Sacrificed", true) != null)
                            && !sa.getRootAbility().getPaidList("Sacrificed", true).isEmpty()) {
                        // If the player activated its ability by sacrificing the enchantment, the enchantment has not anything attached anymore and the ex-enchanted card has to be searched in other ways.. for example, the green enchantment "Carapace"
                        enchantedEntity = sa.getRootAbility().getPaidList("Sacrificed", true).get(0).getEnchantingCard();
                        if (enchantedEntity != null) {
                            enchantedEntityView = enchantedEntity.getView();
                            numSmallImages++;
                        }
                    }
                }

                // If current effect is a triggered ability, I want to show the triggering card if present
                SpellAbility sourceSA = (SpellAbility) si.getTriggeringObject(AbilityKey.SourceSA);
                CardView sourceCardView = null;
                if (sourceSA != null) {
                    sourceCardView = sourceSA.getHostCard().getView();
                    numSmallImages++;
                }

                // I also want to show each type of targets (both cards and players)
                List<GameEntityView> targets = getTargets(si,new ArrayList<GameEntityView>());
                numSmallImages = numSmallImages + targets.size();

                // Now I know how many small images - on to render them
                if (enchantedEntityView != null) {
                    addSmallImageToStackModalPanel(enchantedEntityView,mainPanel,numSmallImages);
                }
                if (sourceCardView != null) {
                    addSmallImageToStackModalPanel(sourceCardView,mainPanel,numSmallImages);
                }
                for (GameEntityView gev : targets) {
                    addSmallImageToStackModalPanel(gev, mainPanel, numSmallImages);
                }

                FOptionPane.showOptionDialog(null, "Forge", null, mainPanel, ImmutableList.of(Localizer.getInstance().getMessage("lblOK")));
                // here the user closed the modal - time to update the next notifiable stack index

            }
            // In any case, I have to increase the counter
            nextNotifiableStackIndex++;
        } else {
            // Not yet time to show the modal - schedule the method again, and try again later
            Runnable tryAgainThread = () -> notifyStackAddition(event);
            GuiBase.getInterface().invokeInEdtLater(tryAgainThread);

        }
    }

    private List<GameEntityView> getTargets(SpellAbilityStackInstance si, List<GameEntityView> result){
        if (si == null) {
            return result;
        }
        FCollectionView<CardView> targetCards = CardView.getCollection(si.getTargetChoices().getTargetCards());
        for (CardView currCardView: targetCards) {
            result.add(currCardView);
        }

        for (SpellAbility currSA : si.getTargetChoices().getTargetSpells()) {
            CardView currCardView = currSA.getCardView();
            result.add(currCardView);
        }

        FCollectionView<PlayerView> targetPlayers = PlayerView.getCollection(si.getTargetChoices().getTargetPlayers());
        for (PlayerView currPlayerView : targetPlayers) {
            result.add(currPlayerView);
        }

        return getTargets(si.getSubInstance(),result);
    }

    private void addBigImageToStackModalPanel(JPanel mainPanel, SpellAbilityStackInstance si) {
        StackItemView siv = si.getView();
        int rotation = getRotation(si.getCardView());

        FImagePanel imagePanel = new FImagePanel();
        BufferedImage bufferedImage = FImageUtil.getImage(siv.getSourceCard().getCurrentState());
        imagePanel.setImage(bufferedImage, rotation, AutoSizeImageMode.SOURCE);
        int imageWidth = 433;
        int imageHeight = 600;
        Dimension imagePanelDimension = new Dimension(imageWidth,imageHeight);
        imagePanel.setMinimumSize(imagePanelDimension);

        mainPanel.add(imagePanel, "cell 0 0, spany 3");
    }

    private void addTextToStackModalPanel(JPanel mainPanel, SpellAbility sa, SpellAbilityStackInstance si) {
        String who = sa.getActivatingPlayer().getName();
        String action = sa.isSpell() ? " cast " : sa.isTrigger() ? " triggered " : " activated ";
        String what = sa.getStackDescription().startsWith("Morph ") ? "Morph" : sa.getHostCard().toString();

        StringBuilder sb = new StringBuilder();
        sb.append(who).append(action).append(what);

        if (sa.getTargetRestrictions() != null) {
            sb.append(" targeting ");
            TargetChoices targets = si.getTargetChoices();
            sb.append(targets);
        }
        sb.append(".");
        String message1 = sb.toString();
        String message2 = si.getStackDescription();
        String messageTotal = message1 + "\n\n" + message2;

        final FTextArea prompt1 = new FTextArea(messageTotal);
        prompt1.setFont(FSkin.getFont(21));
        prompt1.setAutoSize(true);
        prompt1.setMinimumSize(new Dimension(475,200));
        mainPanel.add(prompt1, "cell 1 0, aligny top");
    }

    private void addSmallImageToStackModalPanel(GameEntityView gameEntityView, JPanel mainPanel, int numTarget) {
        if (gameEntityView instanceof CardView) {
            CardView cardView = (CardView) gameEntityView;
            int currRotation = getRotation(cardView);
            FImagePanel targetPanel = new FImagePanel();
            BufferedImage bufferedImage = FImageUtil.getImage(cardView.getCurrentState());
            targetPanel.setImage(bufferedImage, currRotation, AutoSizeImageMode.SOURCE);
            int imageWidth = 217;
            int imageHeight = 300;
            Dimension targetPanelDimension = new Dimension(imageWidth,imageHeight);
            targetPanel.setMinimumSize(targetPanelDimension);
            mainPanel.add(targetPanel, "cell 1 1, split " + numTarget+ ",  aligny bottom");
        } else if (gameEntityView instanceof PlayerView) {
            PlayerView playerView = (PlayerView) gameEntityView;
            SkinImage playerAvatar = getPlayerAvatar(playerView, 0);
            final FLabel lblIcon = new FLabel.Builder().icon(playerAvatar).build();
            Dimension dimension = playerAvatar.getSizeForPaint(JOptionPane.getRootFrame().getGraphics());
            mainPanel.add(lblIcon, "cell 1 1, split " + numTarget+ ", w " + dimension.getWidth() + ", h " + dimension.getHeight() + ", aligny bottom");
        }
    }

    private int getRotation(CardView cardView) {
        final int rotation;
        if (cardView.isSplitCard()) {
            String cardName = cardView.getOracleName();
            if (cardName.isEmpty()) { cardName = cardView.getAlternateState().getOracleName(); }

            PaperCard pc = StaticData.instance().getCommonCards().getCard(cardName);
            boolean hasKeywordAftermath = pc != null && Card.getCardForUi(pc).hasKeyword(Keyword.AFTERMATH);

            rotation = cardView.isFaceDown() ? 0 : hasKeywordAftermath ? (CardStateName.LeftSplit.equals(cardView.getState(false).getState()) ? 0 : 270) : 90; // rotate Aftermath splits the other way to correctly show the right split (graveyard) half
        } else {
            CardStateView cardStateView = cardView.getState(false);
            rotation = cardStateView.isPlane() || cardStateView.isPhenomenon() ? 90 : 0;
        }

        return rotation;
    }

    @Override
    public void notifyStackRemoval(GameEventSpellRemovedFromStack event) {
        // I always decrease the counter
        nextNotifiableStackIndex--;
    }

    @Override
    public void handleLandPlayed(Card land) {
        Runnable createPopupThread = () -> createLandPopupPanel(land);
        GuiBase.getInterface().invokeInEdtAndWait(createPopupThread);
    }

    private void createLandPopupPanel(Card land) {
        String landPlayedNotificationPolicy = FModel.getPreferences().getPref(FPref.UI_LAND_PLAYED_NOTIFICATION_POLICY);
        Player cardController = land.getController();
        boolean isAi = cardController.isAI();
        if (ForgeConstants.LAND_PLAYED_NOTIFICATION_ALWAYS.equals(landPlayedNotificationPolicy)
                || (ForgeConstants.LAND_PLAYED_NOTIFICATION_AI.equals(landPlayedNotificationPolicy) && (isAi))
                || (ForgeConstants.LAND_PLAYED_NOTIFICATION_ALWAYS_FOR_NONBASIC_LANDS.equals(landPlayedNotificationPolicy) && !land.isBasicLand())
                || (ForgeConstants.LAND_PLAYED_NOTIFICATION_AI_FOR_NONBASIC_LANDS.equals(landPlayedNotificationPolicy) && !land.isBasicLand()) && (isAi)) {
            String title = "Forge";
            List<String> options = ImmutableList.of(Localizer.getInstance().getMessage("lblOK"));

            MigLayout migLayout = new MigLayout("insets 15, left, gap 30, fill");
            JPanel mainPanel = new JPanel(migLayout);
            final Dimension parentSize = JOptionPane.getRootFrame().getSize();
            Dimension maxSize = new Dimension(1400, parentSize.height - 100);
            mainPanel.setMaximumSize(maxSize);
            mainPanel.setOpaque(false);

            int rotation = getRotation(land.getView());

            FImagePanel imagePanel = new FImagePanel();
            BufferedImage bufferedImage = FImageUtil.getImage(land.getCurrentState().getView());
            imagePanel.setImage(bufferedImage, rotation, AutoSizeImageMode.SOURCE);
            int imageWidth = 433;
            int imageHeight = 600;
            Dimension imagePanelDimension = new Dimension(imageWidth,imageHeight);
            imagePanel.setMinimumSize(imagePanelDimension);

            mainPanel.add(imagePanel, "cell 0 0, spany 3");

            String msg = cardController.toString() + " puts " + land.toString() + " into play into " + ZoneType.Battlefield.toString() + ".";

            final FTextArea prompt1 = new FTextArea(msg);
            prompt1.setFont(FSkin.getFont(21));
            prompt1.setAutoSize(true);
            prompt1.setMinimumSize(new Dimension(475,200));
            mainPanel.add(prompt1, "cell 1 0, aligny top");

            FOptionPane.showOptionDialog(null, title, null, mainPanel, options);
        }
    }

    public void showFullControl(PlayerView pv, MouseEvent e) {
        if (pv.isAI()) {
            return;
        }
        Set<FullControlFlag> controlFlags = getGameView().getGame().getPlayer(pv).getController().getFullControl();
        final String lblFullControl = Localizer.getInstance().getMessage("lblFullControl");
        final JPopupMenu menu = new JPopupMenu(lblFullControl);
        menu.add(
                GuiUtils.createMenuItem("- " + lblFullControl + " -", null, null, false, true)
        );

        addFullControlEntry(menu, "lblChooseCostOrder", FullControlFlag.ChooseCostOrder, controlFlags);
        addFullControlEntry(menu, "lblChooseCostReductionOrder", FullControlFlag.ChooseCostReductionOrderAndVariableAmount, controlFlags);
        addFullControlEntry(menu, "lblNoPaymentFromManaAbility", FullControlFlag.NoPaymentFromManaAbility, controlFlags);
        addFullControlEntry(menu, "lblNoFreeCombatCostHandling", FullControlFlag.NoFreeCombatCostHandling, controlFlags);
        addFullControlEntry(menu, "lblAllowPaymentStartWithMissingResources", FullControlFlag.AllowPaymentStartWithMissingResources, controlFlags);
        addFullControlEntry(menu, "lblLayerTimestampOrder", FullControlFlag.LayerTimestampOrder, controlFlags);

        menu.show(view.getControl().getFieldViewFor(pv).getAvatarArea(), e.getX(), e.getY());
    }

    private void addFullControlEntry(JPopupMenu menu, String label, FullControlFlag flag, Set<FullControlFlag> controlFlags) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(Localizer.getInstance().getMessage(label));
        if (controlFlags.contains(flag)) {
            item.setSelected(true);
        }
        item.addActionListener(arg0 -> {
            if (controlFlags.contains(flag)) {
                controlFlags.remove(flag);
            } else {
                controlFlags.add(flag);
            }
        });
        menu.add(item);
    }
}
