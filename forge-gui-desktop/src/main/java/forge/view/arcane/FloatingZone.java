/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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
package forge.view.arcane;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.Border;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gui.FThreads;
import forge.gui.framework.DragCell;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.gui.framework.SDisplayUtil;
import forge.gui.framework.SLayoutConstants;
import forge.gui.framework.SLayoutIO;
import forge.gui.framework.SRearrangingUtil;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.screens.match.views.VHand;
import forge.screens.match.views.VZone;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.special.PlayerDetailsPanel;
import forge.util.Localizer;
import forge.util.collect.FCollection;
import forge.view.FView;

public class FloatingZone extends FloatingCardArea {
    private static final long serialVersionUID = 1927906492186378596L;

    private static final Map<Integer, FloatingZone> floatingAreas = new HashMap<>();
    private static final Map<Integer, VZone> dockedZones = new HashMap<>();

    private static int getKey(final PlayerView player, final ZoneType zone) {
        return 40 * player.getId() + zone.hashCode();
    }

    // ========== Tab mode preference ==========

    /** Returns true if the given zone type should open as a docked tab. */
    public static boolean isTabMode(final ZoneType zone, final boolean isOwn) {
        final FPref prefKey = isOwn ? FPref.UI_ZONE_DOCK_ZONES : FPref.UI_ZONE_DOCK_ZONES_OTHER;
        final String pref = FModel.getPreferences().getPref(prefKey);
        if (pref == null || pref.isEmpty()) return false;
        for (final String s : pref.split(",")) {
            if (s.trim().equals(zone.name())) return true;
        }
        return false;
    }

    /** Sets whether the given zone type should open as a docked tab. */
    public static void setTabMode(final ZoneType zone, final boolean tabMode, final boolean isOwn) {
        final ForgePreferences prefs = FModel.getPreferences();
        final FPref prefKey = isOwn ? FPref.UI_ZONE_DOCK_ZONES : FPref.UI_ZONE_DOCK_ZONES_OTHER;
        final String current = prefs.getPref(prefKey);
        final LinkedHashSet<String> zones = new LinkedHashSet<>();
        if (current != null && !current.isEmpty()) {
            for (final String s : current.split(",")) {
                final String trimmed = s.trim();
                if (!trimmed.isEmpty()) zones.add(trimmed);
            }
        }
        if (tabMode) {
            zones.add(zone.name());
        } else {
            zones.remove(zone.name());
        }
        prefs.setPref(prefKey, String.join(",", zones));
        prefs.save();
    }

    // ========== Floating zone API ==========

    public static void showOrHide(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        // If a VHand tab already exists for this player (default layout) and tab mode
        // is active, just select the existing tab instead of creating a duplicate.
        if (zone == ZoneType.Hand && isTabMode(zone, matchUI.isLocalPlayer(player))) {
            final VHand existingHand = matchUI.getHandFor(player);
            if (existingHand != null && existingHand.getParentCell() != null) {
                SDisplayUtil.showTab(existingHand);
                return;
            }
        }

        final int key = getKey(player, zone);
        final VZone docked = dockedZones.get(key);
        if (docked != null) {
            final DragCell cell = docked.getParentCell();
            if (cell != null) {
                // Docked and visible — remove tab but keep dockedZones entry
                cell.removeDoc(docked);
                if (cell.getDocs().isEmpty()) {
                    SRearrangingUtil.fillGap(cell);
                    FView.SINGLETON_INSTANCE.removeDragCell(cell);
                }
                docked.setParentCell(null);
            } else {
                // Was hidden (no parent cell) — re-add as tab on the hand panel's cell
                showDockedTab(docked);
            }
            return;
        }

        // No existing docked zone — check tab mode preference
        final boolean isOwn = matchUI.isLocalPlayer(player);
        if (isTabMode(zone, isOwn)) {
            showAsTab(matchUI, player, zone);
            return;
        }

        final FloatingZone cardArea = _init(matchUI, player, zone);
        cardArea.showOrHideWindow();
    }

    /** Create a new docked VZone and add it as a tab on the hand panel's cell. */
    private static void showAsTab(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        final EDocID docID = EDocID.fromZoneType(zone);
        if (docID == null) return;

        final VZone vZone = new VZone(matchUI, player, zone);
        docID.setDoc(vZone);
        dockedZones.put(getKey(player, zone), vZone);
        showDockedTab(vZone);
    }

    /** Add a docked VZone as a tab on the hand panel's cell. */
    private static void showDockedTab(final VZone vZone) {
        DragCell target = null;
        final IVDoc<? extends ICDoc> handDoc = EDocID.HAND_0.getDoc();
        if (handDoc != null) {
            target = handDoc.getParentCell();
        }
        if (target == null) {
            final List<DragCell> cells = FView.SINGLETON_INSTANCE.getDragCells();
            if (!cells.isEmpty()) {
                target = cells.get(0);
            }
        }
        if (target != null) {
            target.addDoc(vZone);
            target.setSelected(vZone);
            vZone.refresh();
        }
    }

    public static boolean show(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        final FloatingZone cardArea = _init(matchUI, player, zone);

        if (cardArea.isVisible()) {
            return false;
        }

        FThreads.invokeInEdtNowOrLater(cardArea::showWindow);

        return true;
    }

    public static boolean hide(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        final FloatingZone cardArea = _init(matchUI, player, zone);

        if (!cardArea.isVisible()) {
            return false;
        }

        FThreads.invokeInEdtNowOrLater(cardArea::hideWindow);

        return true;
    }

    /** Close any existing display (docked tab or floating window) for this player/zone. */
    public static void closeExisting(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        final int key = getKey(player, zone);

        // Close docked zone if present
        final VZone docked = dockedZones.get(key);
        if (docked != null) {
            removeDocked(docked);
        }

        // Close floating window if present
        final FloatingZone floating = floatingAreas.get(key);
        if (floating != null && floating.isVisible()) {
            floating.hideWindow();
            floatingAreas.remove(key);
        }
    }

    private static FloatingZone _init(final CMatchUI matchUI, final PlayerView player, final ZoneType zone) {
        final int key = getKey(player, zone);
        FloatingZone cardArea = floatingAreas.get(key);
        if (cardArea == null || cardArea.getMatchUI() != matchUI) {
            cardArea = new FloatingZone(matchUI, player, zone);
            floatingAreas.put(key, cardArea);
        } else {
            cardArea.setPlayer(player); //ensure player is updated if needed
        }
        return cardArea;
    }

    public static CardPanel getCardPanel(final CMatchUI matchUI, final CardView card) {
        // Check docked zones first
        final int key = getKey(card.getController(), card.getZone());
        final VZone docked = dockedZones.get(key);
        if (docked != null) {
            final CardPanel panel = docked.getCardPanel(card);
            if (panel != null) {
                return panel;
            }
        }
        final FloatingZone window = _init(matchUI, card.getController(), card.getZone());
        return window.getCardPanel(card.getId());
    }

    public static void refresh(final PlayerView player, final ZoneType zone) {
        // Refresh floating window
        FloatingZone cardArea = floatingAreas.get(getKey(player, zone));
        if (cardArea != null) {
            cardArea.setPlayer(player); //ensure player is updated if needed
            cardArea.refresh();
        }

        // Refresh docked zone tab
        final VZone docked = dockedZones.get(getKey(player, zone));
        if (docked != null) {
            docked.refresh();
        }

        //refresh flashback zone when graveyard, library, or exile zones updated
        switch (zone) {
            case Graveyard:
            case Library:
            case Exile:
                refresh(player, ZoneType.Flashback);
                break;
            default:
                break;
        }
    }

    public static void closeAll() {
        for (final FloatingZone cardArea : floatingAreas.values()) {
            cardArea.window.setVisible(false);
        }
        floatingAreas.clear();

        // Remove docked zone tabs and deregister
        for (final VZone vZone : dockedZones.values()) {
            final DragCell cell = vZone.getParentCell();
            if (cell != null) {
                cell.removeDoc(vZone);
                if (cell.getDocs().isEmpty()) {
                    SRearrangingUtil.fillGap(cell);
                    FView.SINGLETON_INSTANCE.removeDragCell(cell);
                }
            }
            final EDocID docID = vZone.getDocumentID();
            if (docID != null) {
                docID.setDoc(null);
            }
        }
        dockedZones.clear();
    }

    public static void refreshAll() {
        for (final FloatingZone cardArea : floatingAreas.values()) {
            cardArea.refresh();
        }
        for (final VZone vZone : dockedZones.values()) {
            vZone.refresh();
        }
    }

    /** Returns the docked VZone for a player/zone, or null. */
    public static VZone getDockedZone(final PlayerView player, final ZoneType zone) {
        return dockedZones.get(getKey(player, zone));
    }

    // ========== Dock/undock transitions ==========

    /** Dock this floating zone into an existing cell as a new tab. */
    private void dockIntoCell(final DragCell targetCell) {
        final VZone vZone = createDockedZone();
        if (vZone == null) return;

        targetCell.addDoc(vZone);
        targetCell.setSelected(vZone);
        vZone.refresh();

        SLayoutIO.saveLayout(null);
    }

    /** Create a VZone from this floating zone's state and close the floating window. */
    private VZone createDockedZone() {
        final EDocID docID = EDocID.fromZoneType(zone);
        if (docID == null) return null;

        final VZone vZone = new VZone(getMatchUI(), player, zone);
        docID.setDoc(vZone);

        final int key = getKey(player, zone);
        dockedZones.put(key, vZone);

        // Close the floating window
        hideWindow();
        floatingAreas.remove(key);

        return vZone;
    }

    /** Undock a VZone tab and reopen it as a floating window. */
    public static void undockZone(final VZone vZone) {
        final DragCell cell = vZone.getParentCell();
        if (cell != null) {
            cell.removeDoc(vZone);
            if (cell.getDocs().isEmpty()) {
                SRearrangingUtil.fillGap(cell);
                FView.SINGLETON_INSTANCE.removeDragCell(cell);
            }
        }

        final EDocID docID = vZone.getDocumentID();
        if (docID != null) {
            docID.setDoc(null);
        }

        final int key = getKey(vZone.getPlayer(), vZone.getZone());
        dockedZones.remove(key);

        SLayoutIO.saveLayout(null);

        // Reopen as floating window
        show(vZone.getMatchUI(), vZone.getPlayer(), vZone.getZone());
    }

    /** Remove a docked zone tab without reopening as floating. */
    private static void removeDocked(final VZone vZone) {
        final DragCell cell = vZone.getParentCell();
        if (cell != null) {
            cell.removeDoc(vZone);
            if (cell.getDocs().isEmpty()) {
                SRearrangingUtil.fillGap(cell);
                FView.SINGLETON_INSTANCE.removeDragCell(cell);
            }
        }

        final EDocID docID = vZone.getDocumentID();
        if (docID != null) {
            docID.setDoc(null);
        }

        dockedZones.remove(getKey(vZone.getPlayer(), vZone.getZone()));

        SLayoutIO.saveLayout(null);
    }

    /** Register VZone instances for layout persistence. Called during match init. */
    public static void registerZoneDocs(final CMatchUI matchUI, final Iterable<PlayerView> localPlayers) {
        for (final PlayerView player : localPlayers) {
            for (final ZoneType zone : CMatchUI.FLOATING_ZONE_TYPES) {
                final EDocID docID = EDocID.fromZoneType(zone);
                if (docID != null) {
                    final VZone vZone = new VZone(matchUI, player, zone);
                    docID.setDoc(vZone);
                    dockedZones.put(getKey(player, zone), vZone);
                }
            }
            break; // Only the first local player's zones
        }
    }

    /** Deregister all zone docs. Called on game end before closeAll. */
    public static void deregisterZoneDocs() {
        for (final VZone vZone : dockedZones.values()) {
            final EDocID docID = vZone.getDocumentID();
            if (docID != null) {
                docID.setDoc(null);
            }
        }
        // Don't clear dockedZones here — closeAll() handles that
    }

    /** Remove dockedZones entries that weren't placed into cells by loadLayout. */
    public static void pruneUnparentedDocks() {
        dockedZones.values().removeIf(vZone -> vZone.getParentCell() == null);
    }

    // ========== Drag-to-dock detection ==========

    private DragCell dockTargetCell;
    private DragCell highlightedCell;
    private Border dockOriginalBorder;

    private void setupDockDetection() {
        getWindow().getTitleBar().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(final MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                if (dockTargetCell != null) {
                    clearDockHighlight();
                    dockIntoCell(dockTargetCell);
                    dockTargetCell = null;
                }
            }
        });

        getWindow().getTitleBar().addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(final MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                detectDockTarget(e);
            }
        });
    }

    private void detectDockTarget(final MouseEvent e) {
        final int ex = (int) e.getLocationOnScreen().getX();
        final int ey = (int) e.getLocationOnScreen().getY();

        DragCell newTarget = null;
        for (final DragCell cell : FView.SINGLETON_INSTANCE.getDragCells()) {
            final int cx = cell.getAbsX();
            final int cy = cell.getAbsY();
            final int cw = cell.getW();

            if (ex >= cx && ey >= cy && ex <= cx + cw && ey <= cy + SLayoutConstants.HEAD_H * 3 / 2) {
                newTarget = cell;
                break;
            }
        }

        if (newTarget != dockTargetCell) {
            clearDockHighlight();
            dockTargetCell = newTarget;
            if (dockTargetCell != null) {
                applyDockHighlight();
            }
        }
    }

    private static final Border DOCK_HIGHLIGHT_BORDER = BorderFactory.createLineBorder(new Color(70, 130, 230), 2);

    private void applyDockHighlight() {
        if (dockTargetCell == null) return;
        highlightedCell = dockTargetCell;
        dockOriginalBorder = dockTargetCell.getBody().getBorder();
        dockTargetCell.getBody().setBorder(DOCK_HIGHLIGHT_BORDER);
    }

    private void clearDockHighlight() {
        if (highlightedCell != null) {
            highlightedCell.getBody().setBorder(dockOriginalBorder);
            highlightedCell = null;
        }
        dockOriginalBorder = null;
    }

    // ========== Instance fields and methods ==========

    /** Sort order for zone-grouped display: Command first, then by zone origin, then CMC/color/name. */
    private static int zoneOrder(final ZoneType zone) {
        if (zone == null) return 99;
        switch (zone) {
            case Command:   return 0;
            case Graveyard: return 1;
            case Exile:     return 2;
            case Library:   return 3;
            case Sideboard: return 4;
            default:        return 5;
        }
    }

    public static final Comparator<CardView> ZONE_ORDER_COMPARATOR =
            Comparator.comparingInt((CardView cv) -> zoneOrder(cv.getZone()))
                    .thenComparingInt(cv -> cv.getCurrentState().getManaCost().getCMC())
                    .thenComparing(cv -> cv.getCurrentState().getColors().getOrderWeight())
                    .thenComparing(cv -> cv.getCurrentState().getName());

    private final ZoneType zone;
    private PlayerView player;

    protected boolean sortedByName = false;
    protected FCollection<CardView> cardList;

    private final Comparator<CardView> comp = (lhs, rhs) -> {
        if (!getMatchUI().mayView(lhs)) {
            return (getMatchUI().mayView(rhs)) ? 1 : 0;
        } else if (!getMatchUI().mayView(rhs)) {
            return -1;
        } else {
            return lhs.getName().compareTo(rhs.getName());
        }
    };

    protected Iterable<CardView> getCards() {
        Iterable<CardView> zoneCards = player.getCards(zone);
        if (zoneCards != null) {
            cardList = new FCollection<>(zoneCards);
            if (sortedByName) {
                cardList.sort(comp);
            } else if (zone == ZoneType.Flashback) {
                cardList.sort(ZONE_ORDER_COMPARATOR);
            }
            return cardList;
        } else {
            return null;
        }
    }

    private FloatingZone(final CMatchUI matchUI, final PlayerView player0, final ZoneType zone0) {
        super(matchUI, new FScrollPane(false, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
        window.add(getScrollPane(), "grow, push");
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); //pfps so that old content does not reappear?
        getScrollPane().setViewportView(this);
        setOpaque(false);
        window.setIconImage(FSkin.getImage(PlayerDetailsPanel.iconFromZone(zone0)));
        zone = zone0;
        setPlayer(player0);
        setVertical(true);
    }

    @Override
    protected void doRefresh() {
        List<CardPanel> cardPanels = new ArrayList<>();
        Iterable<CardView> cards = getCards();
        if (cards != null) {
            for (final CardView card : cards) {
                CardPanel cardPanel = getCardPanel(card.getId());
                if (cardPanel == null) {
                    cardPanel = new CardPanel(getMatchUI(), card);
                    cardPanel.setDisplayEnabled(true);
                } else {
                    cardPanel.setCard(card);
                }
                if (zone == ZoneType.Flashback) {
                    final ZoneType cardZone = card.getZone();
                    if (cardZone != null) {
                        cardPanel.setZoneBanner(cardZone.getTranslatedName().toUpperCase(), cardZone);
                    }
                }
                cardPanels.add(cardPanel);
            }
        }
        setCardPanels(cardPanels);
        getWindow().setTitle(String.format(title, cardPanels.size()));
    }

    private void toggleSorted() {
        sortedByName = !sortedByName;
        setTitle();
        refresh();
        // revalidation does not appear to be necessary here
        getWindow().repaint();
    }

    @Override
    protected void onShow() {
        super.onShow();
        if (!hasBeenShown) {
            getWindow().getTitleBar().addMouseListener(new FMouseAdapter() {
                @Override
                public void onRightClick(final MouseEvent e) {
                    toggleSorted();
                }
            });
            setupDockDetection();
        }
    }

    private void setTitle() {
        final String sort_detail = sortedByName ? Localizer.getInstance().getMessage("lblRightClickToUnSort") : Localizer.getInstance().getMessage("lblRightClickToSort");
        title = Localizer.getInstance().getMessage("lblPlayerZoneNCardSortStatus", player.getName(), zone.getTranslatedName(), "%d" , sort_detail);
    }

    private void setPlayer(PlayerView player0) {
        if (player == player0) {
            return;
        }
        player = player0;
        setTitle();

        boolean isAi = player0.isAI();
        switch (zone) {
            case Exile:
                locPref = isAi ? FPref.ZONE_LOC_AI_EXILE : FPref.ZONE_LOC_HUMAN_EXILE;
                break;
            case Graveyard:
                locPref = isAi ? FPref.ZONE_LOC_AI_GRAVEYARD : FPref.ZONE_LOC_HUMAN_GRAVEYARD;
                break;
            case Hand:
                locPref = isAi ? FPref.ZONE_LOC_AI_HAND : FPref.ZONE_LOC_HUMAN_HAND;
                break;
            case Library:
                locPref = isAi ? FPref.ZONE_LOC_AI_LIBRARY : FPref.ZONE_LOC_HUMAN_LIBRARY;
                break;
            case Flashback:
                locPref = isAi ? FPref.ZONE_LOC_AI_FLASHBACK : FPref.ZONE_LOC_HUMAN_FLASHBACK;
                break;
            case Command:
                locPref = isAi ? FPref.ZONE_LOC_AI_COMMAND : FPref.ZONE_LOC_HUMAN_COMMAND;
                break;
            case Ante:
                locPref = isAi ? FPref.ZONE_LOC_AI_ANTE : FPref.ZONE_LOC_HUMAN_ANTE;
                break;
            case Sideboard:
                locPref = isAi ? FPref.ZONE_LOC_AI_SIDEBOARD : FPref.ZONE_LOC_HUMAN_SIDEBOARD;
                break;
            default:
                locPref = null;
                break;
        }
    }

}
