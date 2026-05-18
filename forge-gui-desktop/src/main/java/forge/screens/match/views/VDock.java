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
package forge.screens.match.views;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import forge.gui.UiCommand;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ILocalRepaint;
import forge.gui.framework.IVDoc;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.screens.match.controllers.CDock;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinColor;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.FSkin.SkinnedLabel;
import forge.util.Localizer;

/**
 * Assembles Swing components of button dock area.
 * 
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VDock implements IVDoc<CDock> {
    final Localizer localizer = Localizer.getInstance();
    private DragCell parentCell;
    private final DragTab tab = new DragTab(localizer.getMessage("lblDock"));
    private final CDock controller;

    private final EnumMap<DockButtonId, DockButton> buttons = new EnumMap<>(DockButtonId.class);
    // entry order is the on-screen left→right order
    private LinkedHashMap<DockButtonId, Boolean> state;

    public VDock(final CDock controller) {
        this.controller = controller;
        for (DockButtonId id : DockButtonId.values()) {
            buttons.put(id, new DockButton(FSkin.getIcon(id.icon), localizer.getMessage(id.labelKey)));
        }
    }

    @Override
    public void populate() {
        final JPanel pnl = parentCell.getBody();
        pnl.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        state = loadLayout();
        installInteractionHandlers(pnl);
        relayout();
    }

    private void installInteractionHandlers(final JPanel pnl) {
        for (Map.Entry<DockButtonId, DockButton> e : buttons.entrySet()) {
            final DockButtonId id = e.getKey();
            final DockButton btn = e.getValue();
            installRightClickMenu(btn, id);
            btn.setDragOverAction(ev -> moveDraggedToCursor(id, ev, false));
            btn.setDropAction(ev -> moveDraggedToCursor(id, ev, true));
        }
        installRightClickMenu(pnl, null);
    }

    private void installRightClickMenu(final JComponent c, final DockButtonId id) {
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent ev) { maybeShow(ev); }
            @Override
            public void mouseReleased(final MouseEvent ev) { maybeShow(ev); }
            private void maybeShow(final MouseEvent ev) {
                if (ev.isPopupTrigger() || SwingUtilities.isRightMouseButton(ev)) {
                    showRightClickMenu(c, id, ev);
                }
            }
        });
    }

    private void relayout() {
        if (parentCell == null) {
            return;
        }
        final JPanel pnl = parentCell.getBody();
        pnl.removeAll();
        for (Map.Entry<DockButtonId, Boolean> e : state.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) {
                DockButton b = buttons.get(e.getKey());
                if (b != null) {
                    pnl.add(b);
                }
            }
        }
        pnl.revalidate();
        pnl.repaint();
    }

    private void showRightClickMenu(final JComponent anchor, final DockButtonId id, final MouseEvent ev) {
        final JPopupMenu menu = new JPopupMenu();

        if (id != null) {
            final JMenuItem hide = new JMenuItem(localizer.getMessage("lblDockHideButton", displayName(id)));
            hide.addActionListener(a -> setVisible(id, false));
            menu.add(hide);
        }

        final List<DockButtonId> hidden = orderWhere(false);
        if (!hidden.isEmpty()) {
            if (menu.getComponentCount() > 0) {
                menu.addSeparator();
            }
            for (final DockButtonId hid : hidden) {
                final JMenuItem show = new JMenuItem(localizer.getMessage("lblDockShowButton", displayName(hid)));
                show.addActionListener(a -> setVisible(hid, true));
                menu.add(show);
            }
        }

        if (menu.getComponentCount() > 0) {
            menu.addSeparator();
        }
        final JMenuItem reset = new JMenuItem(localizer.getMessage("lblDockResetDefaults"));
        reset.addActionListener(a -> resetDefaults());
        menu.add(reset);

        menu.show(anchor, ev.getX(), ev.getY());
    }

    private List<DockButtonId> orderWhere(final boolean visible) {
        final List<DockButtonId> out = new ArrayList<>();
        for (Map.Entry<DockButtonId, Boolean> e : state.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue()) == visible) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    private String displayName(final DockButtonId id) {
        final DockButton b = buttons.get(id);
        final String tip = b != null ? b.getToolTipText() : null;
        return tip != null ? tip : id.name();
    }

    private void setVisible(final DockButtonId id, final boolean visible) {
        if (state.containsKey(id) && Boolean.TRUE.equals(state.get(id)) != visible) {
            state.put(id, visible);
            saveLayout(state);
            relayout();
        }
    }

    /**
     * Move the dragged button to the slot whose midpoint sits closest to the
     * cursor X. Called continuously during drag with {@code persist=false} for
     * live shift, and once on release with {@code persist=true} to commit.
     */
    private void moveDraggedToCursor(final DockButtonId draggedId, final MouseEvent ev, final boolean persist) {
        if (parentCell != null) {
            final Point cursor = SwingUtilities.convertPoint(ev.getComponent(), ev.getPoint(), parentCell.getBody());
            final List<DockButtonId> visible = orderWhere(true);
            final int currentIdx = visible.indexOf(draggedId);
            if (currentIdx >= 0 && visible.size() >= 2) {
                final int targetIdx = computeTargetIdx(visible, currentIdx, cursor.x);
                if (targetIdx != currentIdx) {
                    visible.remove(currentIdx);
                    visible.add(Math.min(targetIdx, visible.size()), draggedId);
                    rebuildWithVisible(visible);
                    relayout();
                }
            }
        }
        if (persist) saveLayout(state);
    }

    /**
     * Walk the other visible buttons left→right; the target index is the
     * number of those whose midpoint lies left of the cursor.
     */
    private int computeTargetIdx(final List<DockButtonId> visible, final int currentIdx, final int cursorPanelX) {
        int seen = 0;
        for (int i = 0; i < visible.size(); i++) {
            if (i == currentIdx) continue;
            final DockButton b = buttons.get(visible.get(i));
            if (b == null) continue;
            final int mid = b.getX() + b.getWidth() / 2;
            if (cursorPanelX < mid) {
                return seen;
            }
            seen++;
        }
        return visible.size() - 1;
    }

    /**
     * Replace the visible-button sequence in {@link #state} with the supplied
     * order, leaving hidden buttons interleaved at their existing positions.
     */
    private void rebuildWithVisible(final List<DockButtonId> newVisible) {
        final LinkedHashMap<DockButtonId, Boolean> rebuilt = new LinkedHashMap<>();
        int visIdx = 0;
        for (Map.Entry<DockButtonId, Boolean> e : state.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) {
                rebuilt.put(newVisible.get(visIdx++), Boolean.TRUE);
            } else {
                rebuilt.put(e.getKey(), Boolean.FALSE);
            }
        }
        state = rebuilt;
    }

    private void resetDefaults() {
        state = resetLayoutToDefaults();
        relayout();
    }

    @Override
    public void setParentCell(final DragCell cell0) {
        this.parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.BUTTON_DOCK;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CDock getLayoutControl() {
        return controller;
    }

    public DockButton getButton(final DockButtonId id) {
        return buttons.get(id);
    }

    /**
     * Declarative table of dock buttons. Enum order is the default presentation
     * order; {@code defaultVisible} is the per-button visibility seed. New
     * buttons added in future builds are inserted into the user's saved layout
     * right after their predecessor in this declaration order.
     */
    public enum DockButtonId {
        AUTO_PASS      (FSkinProp.ICO_AUTOPASS,         "lblYieldBtnAutoPassTooltip", true),
        YIELD_SETTINGS (FSkinProp.ICO_DOCK_SETTINGS,    "lblYieldSettings",           true),
        END_TURN       (FSkinProp.ICO_DOCK_ENDTURN,     "lblEndTurn",                 true),
        ALPHA_STRIKE   (FSkinProp.ICO_DOCK_ALPHASTRIKE, "lblAlphaStrike",             true),
        TARGETING      (FSkinProp.ICO_ARCSOFF,          "lblTargetingArcs",           true),
        AUTO_YIELDS    (FSkinProp.ICO_AUTOYIELDS,       "lblAutoYieldsAndTriggers",   true),
        VIEW_DECK_LIST (FSkinProp.ICO_DOCK_DECKLIST,    "lblViewDeckList",            true),
        CONCEDE        (FSkinProp.ICO_DOCK_CONCEDE,     "lblConcedeGame",             false);

        final FSkinProp icon;
        final String labelKey;
        final boolean defaultVisible;

        DockButtonId(FSkinProp icon, String labelKey, boolean defaultVisible) {
            this.icon = icon;
            this.labelKey = labelKey;
            this.defaultVisible = defaultVisible;
        }
    }

    private static LinkedHashMap<DockButtonId, Boolean> defaultLayout() {
        LinkedHashMap<DockButtonId, Boolean> out = new LinkedHashMap<>();
        for (DockButtonId id : DockButtonId.values()) {
            out.put(id, id.defaultVisible);
        }
        return out;
    }

    private static LinkedHashMap<DockButtonId, Boolean> loadLayout() {
        LinkedHashMap<DockButtonId, Boolean> shipped = defaultLayout();
        LinkedHashMap<DockButtonId, Boolean> user = parseLayout(
                FModel.getPreferences().getPref(FPref.UI_DOCK_LAYOUT));
        if (user.isEmpty()) return shipped;
        return mergeFromShipped(user, shipped);
    }

    private static LinkedHashMap<DockButtonId, Boolean> resetLayoutToDefaults() {
        LinkedHashMap<DockButtonId, Boolean> defaults = defaultLayout();
        FModel.getPreferences().setPref(FPref.UI_DOCK_LAYOUT, serializeLayout(defaults));
        FModel.getPreferences().save();
        return defaults;
    }

    private static void saveLayout(final LinkedHashMap<DockButtonId, Boolean> state) {
        FModel.getPreferences().setPref(FPref.UI_DOCK_LAYOUT, serializeLayout(state));
        FModel.getPreferences().save();
    }

    private static LinkedHashMap<DockButtonId, Boolean> parseLayout(final String s) {
        LinkedHashMap<DockButtonId, Boolean> out = new LinkedHashMap<>();
        if (s == null || s.isEmpty()) return out;
        for (String entry : s.split(",")) {
            int colon = entry.indexOf(':');
            if (colon < 0) continue;
            try {
                out.put(DockButtonId.valueOf(entry.substring(0, colon).trim()),
                        !"0".equals(entry.substring(colon + 1).trim()));
            } catch (IllegalArgumentException ignored) {
                // Unknown id — likely from a newer build; skip
            }
        }
        return out;
    }

    private static String serializeLayout(final LinkedHashMap<DockButtonId, Boolean> state) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<DockButtonId, Boolean> e : state.entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(e.getKey().name()).append(':').append(Boolean.TRUE.equals(e.getValue()) ? '1' : '0');
        }
        return sb.toString();
    }

    private static LinkedHashMap<DockButtonId, Boolean> mergeFromShipped(
            final LinkedHashMap<DockButtonId, Boolean> user,
            final LinkedHashMap<DockButtonId, Boolean> shipped) {
        LinkedHashMap<DockButtonId, Boolean> result = new LinkedHashMap<>();
        for (Map.Entry<DockButtonId, Boolean> e : user.entrySet()) {
            if (shipped.containsKey(e.getKey())) {
                result.put(e.getKey(), e.getValue());
            }
        }
        DockButtonId prev = null;
        for (Map.Entry<DockButtonId, Boolean> e : shipped.entrySet()) {
            if (!result.containsKey(e.getKey())) {
                result = insertAfter(result, prev, e.getKey(), e.getValue());
            }
            prev = e.getKey();
        }
        return result;
    }

    private static LinkedHashMap<DockButtonId, Boolean> insertAfter(
            final LinkedHashMap<DockButtonId, Boolean> source,
            final DockButtonId predecessor,
            final DockButtonId newId,
            final boolean newVisible) {
        LinkedHashMap<DockButtonId, Boolean> rebuilt = new LinkedHashMap<>();
        boolean inserted = predecessor == null;
        if (inserted) rebuilt.put(newId, newVisible);
        for (Map.Entry<DockButtonId, Boolean> r : source.entrySet()) {
            rebuilt.put(r.getKey(), r.getValue());
            if (!inserted && r.getKey() == predecessor) {
                rebuilt.put(newId, newVisible);
                inserted = true;
            }
        }
        if (!inserted) rebuilt.put(newId, newVisible);
        return rebuilt;
    }

    /**
     * Buttons in Dock. JLabels are used to allow hover effects.
     */
    @SuppressWarnings("serial")
    public class DockButton extends SkinnedLabel implements ILocalRepaint {
        /** Shared highlight tile colour for active dock buttons. Slightly darker
         *  than goldenrod so it reads as an accent rather than a UI element. */
        private static final Color HIGHLIGHT_BG = new Color(0xB8860B);
        private static final int TILE_CORNER_RADIUS = 6;
        private SkinImage img;
        private final SkinColor hoverBG = FSkin.getColor(FSkin.Colors.CLR_HOVER);
        private final Color defaultBG = new Color(0, 0, 0, 0);
        private final SkinColor dragBorderColor = FSkin.getColor(FSkin.Colors.CLR_BORDERS);
        private UiCommand command;
        private Consumer<MouseEvent> dropAction;
        private Consumer<MouseEvent> dragOverAction;
        private boolean dragging;
        private boolean active;
        // Translucent overlay on the root layered pane that follows the cursor
        // while dragging; the slot itself renders empty (dotted outline).
        private JComponent dragGhost;
        private int dragOffsetX, dragOffsetY;

        public DockButton(final SkinImage i0, final String s0) {
            super();
            this.setToolTipText(s0);
            this.setOpaque(false);
            this.setBackground(this.defaultBG);
            this.img = i0;

            Dimension size = new Dimension(30, 30);
            this.setMinimumSize(size);
            this.setMaximumSize(size);
            this.setPreferredSize(size);

            // FMouseAdapter(true): drag past 3px suppresses the click action
            final FMouseAdapter adapter = new FMouseAdapter(true) {
                @Override
                public void onLeftClick(final MouseEvent e) {
                    if (DockButton.this.command != null) {
                        DockButton.this.command.run();
                    }
                }

                @Override
                public void onLeftMouseDragging(final MouseEvent e) {
                    if (!DockButton.this.dragging) {
                        DockButton.this.dragging = true;
                        DockButton.this.startDragGhost(e);
                        DockButton.this.repaintSelf();
                    } else {
                        DockButton.this.updateDragGhost(e);
                    }
                    if (DockButton.this.dragOverAction != null) {
                        DockButton.this.dragOverAction.accept(e);
                    }
                }

                // onLeftMouseDragDrop needs >3px; mouseUp always fires — clear here so sub-3px twitches don't stick
                @Override
                public void onLeftMouseUp(final MouseEvent e) {
                    if (DockButton.this.dragging) {
                        DockButton.this.dragging = false;
                        DockButton.this.endDragGhost();
                        DockButton.this.repaintSelf();
                    }
                }

                @Override
                public void onLeftMouseDragDrop(final MouseEvent e) {
                    if (DockButton.this.dropAction != null) {
                        DockButton.this.dropAction.accept(e);
                    }
                }

                @Override
                public void onMouseEnter(final MouseEvent e) {
                    DockButton.this.setBackground(DockButton.this.hoverBG);
                }

                @Override
                public void onMouseExit(final MouseEvent e) {
                    DockButton.this.setBackground(DockButton.this.defaultBG);
                }
            };
            this.addMouseListener(adapter);
            // Permanent motion listener — required for onLeftMouseDragging to fire
            this.addMouseMotionListener(adapter);
        }

        public void setCommand(UiCommand command0) {
            this.command = command0;
        }

        public void setDropAction(Consumer<MouseEvent> dropAction0) {
            this.dropAction = dropAction0;
        }

        public void setDragOverAction(Consumer<MouseEvent> dragOverAction0) {
            this.dragOverAction = dragOverAction0;
        }

        /** Paint the highlight tile when active, transparent otherwise. */
        public void setActive(final boolean a) {
            if (this.active != a) {
                this.active = a;
                repaintSelf();
            }
        }

        /** Swap the glyph image. Used for buttons whose shape changes with state. */
        public void setImage(final SkinImage i) {
            if (this.img != i) {
                this.img = i;
                repaintSelf();
            }
        }

        private void startDragGhost(final MouseEvent e) {
            JRootPane root = SwingUtilities.getRootPane(this);
            if (root == null) return;
            JLayeredPane layered = root.getLayeredPane();

            this.dragOffsetX = e.getX();
            this.dragOffsetY = e.getY();

            this.dragGhost = new JComponent() {
                @Override
                protected void paintComponent(final Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.65f));
                    DockButton.this.paintTileAndGlyph(g2, getWidth(), getHeight());
                    g2.dispose();
                }
            };
            this.dragGhost.setSize(this.getSize());
            this.dragGhost.setOpaque(false);
            layered.add(this.dragGhost, JLayeredPane.DRAG_LAYER);
            updateDragGhost(e);
        }

        private void updateDragGhost(final MouseEvent e) {
            if (this.dragGhost == null) return;
            Container layered = this.dragGhost.getParent();
            if (layered == null) return;
            Point inLayered = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), layered);
            this.dragGhost.setLocation(inLayered.x - this.dragOffsetX, inLayered.y - this.dragOffsetY);
            layered.repaint();
        }

        private void endDragGhost() {
            if (this.dragGhost != null) {
                Container layered = this.dragGhost.getParent();
                if (layered != null) {
                    layered.remove(this.dragGhost);
                    layered.repaint();
                }
                this.dragGhost = null;
            }
        }

        @Override
        public void repaintSelf() {
            final Dimension d = getSize();
            repaint(0, 0, d.width, d.height);
        }

        @Override
        public void paintComponent(final Graphics g) {
            final int w = getWidth();
            final int h = getHeight();
            g.setColor(this.getBackground());
            g.fillRect(0, 0, w, h);

            if (this.dragging) {
                // Icon flies as a ghost on the layered pane — outline the empty slot to show the drop target
                FSkin.setGraphicsColor(g, this.dragBorderColor);
                g.drawRect(0, 0, w - 1, h - 1);
                g.drawRect(1, 1, w - 3, h - 3);
            }
            else {
                if (!this.active && this.getSkin().getBackground() == this.hoverBG) {
                    FSkin.setGraphicsColor(g, FSkin.getColor(FSkin.Colors.CLR_BORDERS));
                    g.drawRect(0, 0, w - 1, h - 1);
                }
                paintTileAndGlyph(g, w, h);
            }
            super.paintComponent(g);
        }

        /**
         * Draws the rounded-rect tile (when active) and the glyph on top. Shared
         * between the live button paint path and the drag-ghost overlay so both
         * stay visually consistent.
         */
        private void paintTileAndGlyph(final Graphics g, final int width, final int height) {
            // Bilinear keeps the 80→30 sprite downscale smooth; antialiasing softens the tile edges
            if (g instanceof Graphics2D g2) {
                if (this.active) {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                }
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            }
            if (this.active) {
                g.setColor(HIGHLIGHT_BG);
                g.fillRoundRect(0, 0, width, height, TILE_CORNER_RADIUS, TILE_CORNER_RADIUS);
            }
            FSkin.drawImage(g, this.img, 0, 0, width, height);
        }
    }
}
