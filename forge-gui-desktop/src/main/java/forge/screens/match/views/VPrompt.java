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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.framework.RectangleOfDouble;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.screens.match.controllers.CPrompt;
import forge.toolbox.ArenaTheme;
import forge.toolbox.FButton;
import forge.toolbox.FHtmlViewer;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import forge.view.FView;
import net.miginfocom.swing.MigLayout;

/**
 * Assembles Swing components of message report.
 * 
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VPrompt implements IVDoc<CPrompt> {

    // Fields used with interface IVDoc
    private DragCell parentCell;
    final Localizer localizer = Localizer.getInstance();
    private final DragTab tab = new DragTab(localizer.getMessage("lblPrompt"));

    // Various components
    private final FButton btnOK = new FButton(localizer.getMessage("lblOK"));
    private final FButton btnCancel = new FButton(localizer.getMessage("lblCancel"));
    private final FHtmlViewer tarMessage = new FHtmlViewer();
    private final FScrollPane messageScroller = new FScrollPane(tarMessage, false,
    		ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    private final JLabel lblGames;
    private CardView card = null;
    // Arena: where the prompt cell sits (the decision spot matches res/defaults/match_arena.xml)
    private static final RectangleOfDouble ARENA_DECISION_BOUNDS = new RectangleOfDouble(0.34, 0.695, 0.32, 0.095);
    private static final RectangleOfDouble ARENA_PRIORITY_BOUNDS = new RectangleOfDouble(0.835, 0.775, 0.16, 0.06);
    private static final double ARENA_DECISION_WIDTH = 0.36;
    private static final int ARENA_BUTTON_ROW = 46;
    private enum ArenaPromptMode { HIDDEN, PRIORITY, DECISION }
    private ArenaPromptMode arenaMode;
    private boolean arenaPriorityOnly;

    public void setCardView(final CardView card) {
        this.card = card;
    }

    /** The card the current prompt is about, if any. */
    public CardView getCardView() {
        return card;
    }

    private KeyAdapter buttonKeyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(final KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE
                    && btnCancel.isEnabled()
                    && (FModel.getPreferences().getPrefBoolean(FPref.UI_ALLOW_ESC_TO_END_TURN)
                            || !btnCancel.getText().equals(Localizer.getInstance().getMessage("lblEndTurn")))) {
                btnCancel.doClick();
                e.consume();
            }
        }
    };

    private final CPrompt controller;

    public VPrompt(final CPrompt controller) {
        this.controller = controller;

        lblGames = new FLabel.Builder()
                .fontSize(12)
                .fontStyle(Font.PLAIN)
                .fontAlign(SwingConstants.CENTER)
                .opaque()
                .build();

        btnOK.addKeyListener(buttonKeyAdapter);
        btnCancel.addKeyListener(buttonKeyAdapter);

        tarMessage.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        tarMessage.setMargin(new Insets(3, 3, 3, 3));
        tarMessage.getAccessibleContext().setAccessibleName("Prompt");
        tarMessage.setFocusable(true); // Allow tab to navigate to the prompt.
        messageScroller.getViewport().getView().addMouseListener(new MouseAdapter() {
        	@Override 
        	public void mouseEntered(final MouseEvent e) {
        		if (card != null) {
                    controller.getMatchUI().setCard(card);
        		}
        	}
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
    	ForgePreferences prefs = FModel.getPreferences();
        JPanel container = parentCell.getBody();

        if (ArenaTheme.isEnabled()) {
            populateArena(container, prefs);
            // a rebuilt layout puts the cell where the layout file has it; put it back where it belongs
            SwingUtilities.invokeLater(this::placeArena);
            return;
        }

        // wrap   : 2 columns required for btnOk and btnCancel.
        container.setLayout(new MigLayout("wrap 2, gap 0px!, insets 1px 1px 3px 1px"));
        if (prefs.getPrefBoolean(FPref.UI_COMPACT_PROMPT)) { //hide header and use smaller font if compact prompt
            tarMessage.setFont(FSkin.getFont());
        }
        else {
        	container.add(lblGames, "span 2, w 10:100%, h 22px!");
            tarMessage.setFont(FSkin.getRelativeFont(14));
        }
        lblGames.setText(localizer.getMessage("lblGameSetup"));

        container.add(messageScroller, "span 2, w 10:100%, h 0:100%");

        boolean largerButtons = prefs.getPrefBoolean(FPref.UI_TOUCHSCREEN_OPTIMIZE);
        String constraints = largerButtons ? "w 10:50%, h 40%:40%:60px" : "w 10:50%, hmin 24px";
        constraints += ", gaptop 2px!";

        container.add(btnOK, constraints);
        container.add(btnCancel, constraints);
    }

    /**
     * Arena layout: the prompt floats over the local portrait as a slim message pill with
     * the action buttons side by side beneath it; there is no panel behind it.
     */
    private void populateArena(final JPanel container, final ForgePreferences prefs) {
        btnOK.setPrimary(true);
        if (arenaPriorityOnly) {
            // just passing priority: a compact pair of buttons, no message
            container.setLayout(new MigLayout("insets 0, gap 8, ax right, ay center"));
            btnOK.setFont(FSkin.getBoldFont(16));
            btnCancel.setFont(FSkin.getBoldFont(13));
            container.add(btnOK, "w 52%!, h 40!");
            container.add(btnCancel, "w 42%!, h 34!");
            return;
        }
        container.setLayout(new MigLayout("wrap 1, gap 0, insets 0, ax center, hidemode 3", "[center]", "[grow, fill]6[" + ARENA_BUTTON_ROW + "!]"));
        tarMessage.setFont(FSkin.getRelativeFont(13));
        tarMessage.setOpaque(false);
        messageScroller.setOpaque(false);
        messageScroller.getViewport().setOpaque(false);
        final JPanel pill = new JPanel(new MigLayout("insets 2 12 2 12, gap 0, fill")) {
            @Override
            protected void paintComponent(final Graphics g) {
                final Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(8, 10, 16, 205));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.setColor(ArenaTheme.GLASS_EDGE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            }
        };
        pill.setOpaque(false);
        arenaPill = pill;
        pill.add(messageScroller, "grow");
        container.add(pill, "w 96%!, growy");

        final JPanel buttons = new JPanel(new MigLayout("insets 0, gap 10, ax center"));
        buttons.setOpaque(false);
        btnOK.setFont(FSkin.getBoldFont(17));
        btnCancel.setFont(FSkin.getBoldFont(14));
        buttons.add(btnOK, "w 48%!, h 42!");
        buttons.add(btnCancel, "w 34%!, h 34!");
        container.add(buttons, "w 100%!");
    }

    /**
     * Arena: while only waiting on someone else the prompt is hidden; when it only passes
     * priority (OK with End Turn or Undo) it becomes a compact pair of buttons beside the
     * hand; anything that asks for a decision sits over the local portrait with its message.
     */
    public void updateArenaMode() {
        if (!ArenaTheme.isMatchBoard() || parentCell == null) {
            return;
        }
        final String cancel = btnCancel.getText();
        final boolean priorityOnly = localizer.getMessage("lblOK").equals(btnOK.getText()) && cancel != null
                && (cancel.equals(localizer.getMessage("lblEndTurn")) || cancel.startsWith(localizer.getMessage("lblUndo")));
        final ArenaPromptMode mode = !btnOK.isEnabled() && !btnCancel.isEnabled() ? ArenaPromptMode.HIDDEN
                : priorityOnly ? ArenaPromptMode.PRIORITY : ArenaPromptMode.DECISION;
        if (mode == arenaMode) {
            return;
        }
        arenaMode = mode;
        parentCell.setVisible(mode != ArenaPromptMode.HIDDEN);
        if (mode == ArenaPromptMode.HIDDEN) {
            return;
        }
        arenaPriorityOnly = priorityOnly;
        final JPanel container = parentCell.getBody();
        container.removeAll();
        populateArena(container, FModel.getPreferences());
        container.revalidate();
        container.repaint();
        placeArena();
    }

    /**
     * Arena: puts the prompt where it belongs. Priority-only buttons sit beside the hand; a
     * decision is as tall as its whole message (scrolling only past a third of the window) and
     * sits centred just above the local portrait, never over it.
     */
    public void placeArena() {
        if (arenaMode == null) {
            updateArenaMode(); // shown before any buttons were set: settle what kind of prompt it is first
            return;
        }
        if (!ArenaTheme.isMatchBoard() || parentCell == null || arenaMode == ArenaPromptMode.HIDDEN) {
            return;
        }
        final JPanel content = FView.SINGLETON_INSTANCE.getPnlContent();
        final int cw = content.getWidth(), ch = content.getHeight();
        if (cw <= 0 || ch <= 0) {
            return;
        }
        int x, y, w, h;
        if (arenaMode == ArenaPromptMode.PRIORITY) {
            final RectangleOfDouble r = ARENA_PRIORITY_BOUNDS;
            x = (int) Math.round(r.getX() * cw);
            y = (int) Math.round(r.getY() * ch);
            w = (int) Math.round(r.getW() * cw);
            h = (int) Math.round(r.getH() * ch);
        } else {
            w = Math.min(cw - 16, Math.max(440, (int) Math.round(ARENA_DECISION_WIDTH * cw)));
            // the viewer's own wrapped height at the width it will get (pill insets 12+12, viewer margin 3+3)
            final int textW = Math.round(w * 0.96f) - 44;
            tarMessage.setSize(textW, Short.MAX_VALUE);
            final int textH = tarMessage.getPreferredSize().height;
            final int pillH = Math.max(30, Math.min(textH + 16, ch / 3));
            // no message yet (the game is still setting up): just the buttons, not an empty bar
            final boolean blank = isMessageBlank();
            if (arenaPill != null) {
                arenaPill.setVisible(!blank);
            }
            h = (blank ? 0 : pillH + 6) + ARENA_BUTTON_ROW;
            int centreX = cw / 2;
            int bottom = (int) Math.round((ARENA_DECISION_BOUNDS.getY() + ARENA_DECISION_BOUNDS.getH()) * ch);
            final JComponent avatar = localAvatar();
            followAvatar(avatar);
            // laid out is enough: the board may be placed before the match screen is on show
            if (avatar != null && avatar.getHeight() > 0 && SwingUtilities.isDescendingFrom(avatar, content)) {
                final Point top = SwingUtilities.convertPoint(avatar, avatar.getWidth() / 2, 0, content);
                centreX = top.x;
                bottom = top.y - 6;
            }
            x = Math.max(8, Math.min(cw - w - 8, centreX - w / 2));
            y = Math.max(8, bottom - h);
        }
        if (followedCell != parentCell) {
            // a layout reload (4-player boards do one) puts the cell back where the layout file has it
            if (followedCell != null) {
                followedCell.removeComponentListener(avatarMoved);
            }
            followedCell = parentCell;
            parentCell.addComponentListener(avatarMoved);
        }
        parentCell.setRoughBounds(new RectangleOfDouble(x / (double) cw, y / (double) ch, w / (double) cw, h / (double) ch));
        parentCell.setBounds(x, y, w, h);
        parentCell.validate();
        content.repaint();
    }

    /** The board lays out after the prompt is first placed, and again on every resize; the prompt moves with the portrait. */
    private void followAvatar(final JComponent avatar) {
        if (avatar == trackedAvatar) {
            return;
        }
        if (trackedAvatar != null) {
            trackedAvatar.removeComponentListener(avatarMoved);
            trackedAvatar.removeHierarchyBoundsListener(avatarAncestorMoved);
        }
        trackedAvatar = avatar;
        if (avatar != null) {
            avatar.addComponentListener(avatarMoved);
            avatar.addHierarchyBoundsListener(avatarAncestorMoved);
        }
    }

    private JComponent trackedAvatar;
    private JPanel arenaPill;

    private boolean isMessageBlank() {
        try {
            return tarMessage.getDocument().getText(0, tarMessage.getDocument().getLength()).trim().isEmpty();
        } catch (final javax.swing.text.BadLocationException e) {
            return false;
        }
    }
    private DragCell followedCell;
    private final ComponentAdapter avatarMoved = new ComponentAdapter() {
        @Override
        public void componentMoved(final ComponentEvent e) {
            SwingUtilities.invokeLater(VPrompt.this::placeArena);
        }

        @Override
        public void componentResized(final ComponentEvent e) {
            SwingUtilities.invokeLater(VPrompt.this::placeArena);
        }

        @Override
        public void componentShown(final ComponentEvent e) {
            SwingUtilities.invokeLater(VPrompt.this::placeArena);
        }
    };
    private final HierarchyBoundsAdapter avatarAncestorMoved = new HierarchyBoundsAdapter() {
        @Override
        public void ancestorMoved(final HierarchyEvent e) {
            SwingUtilities.invokeLater(VPrompt.this::placeArena);
        }

        @Override
        public void ancestorResized(final HierarchyEvent e) {
            SwingUtilities.invokeLater(VPrompt.this::placeArena);
        }
    };

    /** The portrait of the player at this screen. */
    private JComponent localAvatar() {
        final CMatchUI matchUI = controller.getMatchUI();
        for (final PlayerView p : matchUI.getLocalPlayers()) {
            final VField field = matchUI.getFieldViewFor(p);
            if (field != null) {
                return field.getAvatarArea();
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell()
     */
    @Override
    public void setParentCell(final DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.REPORT_MESSAGE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CPrompt getLayoutControl() {
        return controller;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    //========= Retrieval methods
    /** @return {@link javax.swing.JButton} */
    public FButton getBtnOK() {
        return this.btnOK;
    }

    /** @return {@link javax.swing.JButton} */
    public FButton getBtnCancel() {
        return this.btnCancel;
    }

    /** @return {@link javax.swing.JTextArea} */
    public FHtmlViewer getTarMessage() {
        return this.tarMessage;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblGames() {
        return this.lblGames;
    }
}
