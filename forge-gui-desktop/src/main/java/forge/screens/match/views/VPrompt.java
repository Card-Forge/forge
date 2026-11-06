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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import forge.game.card.CardView;
import forge.game.GameView;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.framework.RectangleOfDouble;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
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
     * Arena layout. Passing priority is the action button in the bottom-right corner, its caption
     * under it and the end-turn button small beside that. A decision sits just above the local
     * portrait, between the wings of the back row: a few lines of centred text on a soft shadow
     * with two equal buttons beneath.
     */
    private void populateArena(final JPanel container, final ForgePreferences prefs) {
        btnOK.setPrimary(true);
        final int buttonH = arenaButtonHeight();
        if (arenaPriorityOnly) {
            final int windowH = FView.SINGLETON_INSTANCE.getPnlContent().getHeight();
            container.setLayout(new MigLayout("wrap 1, insets 0, gap 0, ax center", "[100%!]"));
            container.add(arenaStrip, "w 100%!, h " + Math.round(windowH * 0.032f) + "!");
            container.add(btnOK, "w 100%!, h " + Math.round(windowH * 0.07f) + "!");
            arenaCaption.setFont(ArenaTheme.displayFont(Font.PLAIN, Math.max(13f, windowH * 0.0165f)));
            container.add(arenaCaption, "w 100%!, h " + Math.round(windowH * 0.026f) + "!");
            container.add(btnCancel, "w " + Math.round(windowH * 0.062f) + "!, h " + Math.round(windowH * 0.038f) + "!, al right");
            return;
        }
        container.setLayout(new MigLayout("wrap 1, gap 0, insets 0, ax center, hidemode 3", "[center]", "[grow, fill]6[" + buttonH + "!]"));
        tarMessage.setFont(ArenaTheme.displayFont(Font.BOLD, Math.max(13f, buttonH * 0.34f)));
        tarMessage.setForeground(java.awt.Color.WHITE);
        tarMessage.setOpaque(false);
        messageScroller.setOpaque(false);
        messageScroller.getViewport().setOpaque(false);
        final JPanel pill = new JPanel(new MigLayout("insets 9 14 9 14, gap 0, fill")) {
            @Override
            public void paint(final Graphics g) {
                final float shown = arenaFade.get();
                if (shown >= 1f) {
                    super.paint(g);
                    return;
                }
                final Graphics2D c = (Graphics2D) g.create();
                c.setComposite(java.awt.AlphaComposite.SrcOver.derive(shown));
                super.paint(c);
                c.dispose();
            }

            @Override
            protected void paintComponent(final Graphics g) {
                // a soft shadow behind the words, fading out on every side
                final Graphics2D g2 = (Graphics2D) g;
                final int soft = Math.min(20, getHeight() / 2 - 1);
                ArenaTheme.paintGlow(g2, soft, soft, getWidth() - 2 * soft, getHeight() - 2 * soft, soft * 2, Color.BLACK, soft, 95);
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 92));
                g2.fillRoundRect(soft, soft, getWidth() - 2 * soft, getHeight() - 2 * soft, soft * 2, soft * 2);
            }
        };
        pill.setOpaque(false);
        pill.add(messageScroller, "grow");
        arenaPill = pill;
        container.add(pill, "w 100%!, growy");

        final JPanel buttons = new JPanel(new MigLayout("insets 0, gap 8, ax center", "[50%!][50%!]", "[grow, fill]"));
        buttons.setOpaque(false);
        buttons.add(btnOK, "growx, h " + buttonH + "!");
        buttons.add(btnCancel, "growx, h " + buttonH + "!");
        container.add(buttons, "w 100%!, h " + buttonH + "!");
    }

    private static int arenaButtonHeight() {
        return Math.max(30, Math.round(FView.SINGLETON_INSTANCE.getPnlContent().getHeight() * 0.058f));
    }

    /** "Next" with what it leads to, or "Resolve" while something waits on the stack; the game logic keeps reading OK and End Turn. */
    private void updateArenaLabels(final boolean priorityOnly) {
        if (!priorityOnly) {
            btnOK.setArenaLabel(null);
            btnCancel.setArenaLabel(null);
            btnCancel.setToolTipText(null);
            btnCancel.setPrimary(false);
            return;
        }
        arenaStrip.repaint();
        final GameView game = controller.getMatchUI().getGameView();
        final boolean mine = game != null && game.getPlayerTurn() != null && controller.getMatchUI().isLocalPlayer(game.getPlayerTurn());
        final boolean stackEmpty = game == null || game.getStack() == null || game.getStack().isEmpty();
        String label = localizer.getMessage(mine ? "lblArenaNext" : "lblArenaPass");
        String caption = "";
        if (!stackEmpty) {
            label = localizer.getMessage("lblArenaResolve");
        } else if (mine && game.getPhase() != null) {
            caption = switch (game.getPhase()) {
                case UNTAP, UPKEEP, DRAW -> localizer.getMessage("lblArenaToMain");
                case MAIN1 -> localizer.getMessage("lblArenaToCombat");
                case COMBAT_BEGIN, COMBAT_DECLARE_ATTACKERS, COMBAT_DECLARE_BLOCKERS, COMBAT_FIRST_STRIKE_DAMAGE, COMBAT_DAMAGE, COMBAT_END -> localizer.getMessage("lblArenaToMain2");
                default -> localizer.getMessage("lblArenaToEnd");
            };
        }
        btnOK.setArenaLabel(label);
        arenaCaption.setText(caption);
        final String cancel = btnCancel.getText();
        if (cancel != null && cancel.equals(localizer.getMessage("lblEndTurn"))) {
            btnCancel.setArenaLabel("\u00BB");
            btnCancel.setToolTipText(cancel);
            btnCancel.setPrimary(true);
        } else {
            btnCancel.setArenaLabel(null);
            btnCancel.setToolTipText(null);
            btnCancel.setPrimary(false);
        }
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
        updateArenaLabels(priorityOnly);
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
        arenaFade.set(0f);
        arenaFade.to(1f, 220);
        btnOK.arenaAppear();
        btnCancel.arenaAppear();
    }

    /**
     * Arena: puts the prompt where it belongs. Priority-only buttons go in the corner. A decision
     * is as tall as its whole message (scrolling only past a third of the window) and stays
     * within the gap the back row leaves above the portrait, sitting on top of the portrait.
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
        final int buttonH = arenaButtonHeight();
        if (buttonH != arenaLaidOutButtonH) {
            // the window's size has changed, and the fixed sizes in the layout with it
            arenaLaidOutButtonH = buttonH;
            final JPanel container = parentCell.getBody();
            container.removeAll();
            populateArena(container, FModel.getPreferences());
        }
        int x, y, w, h;
        if (arenaMode == ArenaPromptMode.PRIORITY) {
            w = Math.round(cw * 0.125f);
            h = Math.round(ch * 0.032f) + Math.round(ch * 0.07f) + Math.round(ch * 0.026f) + Math.round(ch * 0.038f) + 2;
            x = cw - w - Math.round(cw * 0.012f);
            y = Math.round(ch * 0.985f) - h;
        } else {
            w = Math.max(340, Math.round(ch * 0.4f));
            // the viewer's own wrapped height at the width it will get (pill insets 6+6, viewer margin 3+3, a little to spare)
            final int textW = w - 40;
            tarMessage.setSize(textW, Short.MAX_VALUE);
            final int textH = tarMessage.getPreferredSize().height;
            final int pillH = Math.min(textH + 24, ch / 3);
            // a scrollbar only when the message really is that long
            messageScroller.setVerticalScrollBarPolicy(textH + 24 > ch / 3 ? ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED : ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            // no message yet (the game is still setting up): just the buttons, not an empty shadow
            final boolean blank = isMessageBlank();
            if (arenaPill != null) {
                arenaPill.setVisible(!blank);
            }
            h = (blank ? 0 : pillH + 6) + buttonH;
            x = (cw - w) / 2;
            y = Math.max(8, Math.round(ch * ARENA_FRAME_TOP) - h);
        }
        if (followedCell != parentCell) {
            // a layout reload (4-player boards do one) puts the cell back where the layout file has it
            if (followedCell != null) {
                followedCell.removeComponentListener(cellMoved);
            }
            followedCell = parentCell;
            parentCell.addComponentListener(cellMoved);
        }
        parentCell.setRoughBounds(new RectangleOfDouble(x / (double) cw, y / (double) ch, w / (double) cw, h / (double) ch));
        parentCell.setBounds(x, y, w, h);
        parentCell.validate();
        content.repaint();
    }

    private DragCell followedCell;
    private JPanel arenaPill;
    /** How far the prompt has come into view since it last changed kind. */
    private final forge.toolbox.ArenaMotion.Fade arenaFade = new forge.toolbox.ArenaMotion.Fade(this::repaintArenaWords, 1f);
    private final forge.toolbox.ArenaMotion.Fade arenaStripMix = new forge.toolbox.ArenaMotion.Fade(this::repaintArenaWords, 1f);
    private int arenaStripLit = -1;
    private int arenaStripWas = -1;
    private int arenaLaidOutButtonH;
    private void repaintArenaWords() {
        if (arenaPill != null) {
            arenaPill.repaint();
        }
        arenaCaption.repaint();
        arenaStrip.repaint();
    }

    private final JLabel arenaCaption = new JLabel() {
        @Override
        protected void paintComponent(final Graphics g) {
            // white words with a dark edge, readable over whatever the corner of the board is doing
            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(java.awt.AlphaComposite.SrcOver.derive(arenaFade.get()));
            g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            final java.awt.FontMetrics fm = g2.getFontMetrics();
            final int x = (getWidth() - fm.stringWidth(getText())) / 2, y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.setColor(new Color(0, 0, 0, 220));
            g2.drawString(getText(), x + 1, y + 1);
            g2.drawString(getText(), x - 1, y + 1);
            g2.drawString(getText(), x, y - 1);
            g2.setColor(new Color(218, 222, 230));
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    };
    private static final float ARENA_FRAME_TOP = 0.71f; // where the local portrait's frame starts, as a fraction of the window's height

    /** The turn at a glance above the action button: a step, main, combat, main, a step, with the current one alight. */
    private final javax.swing.JComponent arenaStrip = new javax.swing.JComponent() {
        @Override
        protected void paintComponent(final Graphics g0) {
            final Graphics2D g = (Graphics2D) g0.create();
            try {
                g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                final GameView game = controller.getMatchUI().getGameView();
                int lit = -1;
                if (game != null && game.getPhase() != null) {
                    lit = switch (game.getPhase()) {
                        case UNTAP, UPKEEP, DRAW -> 0;
                        case MAIN1 -> 1;
                        case MAIN2 -> 3;
                        case END_OF_TURN, CLEANUP -> 4;
                        default -> 2;
                    };
                }
                if (lit != arenaStripLit) {
                    // the light moves along from the part of the turn just left
                    arenaStripWas = arenaStripLit;
                    arenaStripLit = lit;
                    arenaStripMix.set(0f);
                    arenaStripMix.to(1f, 300);
                }
                final float mix = arenaStripMix.get();
                g.setComposite(java.awt.AlphaComposite.SrcOver.derive(arenaFade.get()));
                final java.awt.Composite whole = g.getComposite();
                final float wholeAlpha = arenaFade.get();
                final int[] kinds = {0, 1, 2, 1, 0};
                final int size = getHeight();
                final int gap = size / 4;
                final int total = kinds.length * size + (kinds.length - 1) * gap;
                int x = (getWidth() - total) / 2;
                for (int i = 0; i < kinds.length; i++) {
                    final float light = i == arenaStripLit ? mix : i == arenaStripWas ? 1f - mix : 0f;
                    if (light < 1f) {
                        forge.toolbox.ArenaArt.paintPhaseGlyph(g, x, 0, size, kinds[i], false, true, false);
                    }
                    if (light > 0f) {
                        g.setComposite(java.awt.AlphaComposite.SrcOver.derive(wholeAlpha * light));
                        forge.toolbox.ArenaArt.paintPhaseGlyph(g, x, 0, size, kinds[i], true, true, false);
                        g.setComposite(whole);
                    }
                    x += size + gap;
                }
            } finally {
                g.dispose();
            }
        }
    };

    private final ComponentAdapter cellMoved = new ComponentAdapter() {
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

    private boolean isMessageBlank() {
        try {
            return tarMessage.getDocument().getText(0, tarMessage.getDocument().getLength()).trim().isEmpty();
        } catch (final javax.swing.text.BadLocationException e) {
            return false;
        }
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
