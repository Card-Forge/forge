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
package forge.gui.match.views;

import forge.Singletons;
import forge.game.card.CardUtil;
import forge.game.player.LobbyPlayer;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.MagicStack;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.match.CMatchUI;
import forge.gui.match.controllers.CStack;
import forge.gui.toolbox.FMouseAdapter;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.SkinnedTextArea;
import forge.properties.ForgePreferences.FPref;
import forge.view.arcane.CardArea;
import forge.view.arcane.CardPanel;
import forge.view.arcane.CardPanelContainer;
import forge.view.arcane.util.Animation;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/** 
 * Assembles Swing components of stack report.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VStack implements IVDoc<CStack> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private boolean cardView;
    private final DragTab tab = new DragTab("Stack");

    // Top-level containers
    private final FScrollPane scroller = new FScrollPane(null, true,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    private final StackArea stackArea = new StackArea(scroller);

    // Other fields
    private List<JTextArea> stackTARs = new ArrayList<JTextArea>();
    private OptionalTriggerMenu otMenu = new OptionalTriggerMenu();

    private VStack() {
        scroller.setViewportView(stackArea);
        stackArea.setOpaque(false);
    }

    //========= Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        cardView = Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_STACK_CARD_VIEW);
        if (cardView) {
            final JPanel pnl = parentCell.getBody();
            pnl.setLayout(new MigLayout("insets 0, gap 0"));

            pnl.add(scroller, "w 100%, h 100%!");
        }
    }

    /**
     * Gets the stack area.
     *
     * @return {@link forge.gui.match.views.VStack.StackArea}
     */
    public StackArea getStackArea() {
        return this.stackArea;
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
        return EDocID.REPORT_STACK;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CStack getLayoutControl() {
        return CStack.SINGLETON_INSTANCE;
    }

    //========== Observer update methods

    /**
     * @param stack
     * @param viewer */
    public void updateStack(final MagicStack stack, final LobbyPlayer viewer) {
        // No need to update this unless it's showing
        if (!parentCell.getSelected().equals(this)) { return; }

        int count = 1;
        Border border = null;

        tab.setText("Stack : " + stack.size());

        if (cardView) {
            stackArea.clear();
        }
        else {
            parentCell.getBody().removeAll();
            parentCell.getBody().setLayout(new MigLayout("insets 1%, gap 1%, wrap"));
            border = new EmptyBorder(5, 5, 5, 5);
            stackTARs.clear();
        }
        boolean isFirst = true;
        for (final SpellAbilityStackInstance spell : stack) {
            String isOptional = spell.getSpellAbility().isOptionalTrigger()
                    && spell.getSourceCard().getController().getController().getLobbyPlayer().equals(viewer) ? "(OPTIONAL) " : "";
            String txt = (count++) + ". " + isOptional + spell.getStackDescription();

            if (cardView) {
                Animation.moveCard(stackArea.addCard(spell.getSourceCard()));
            }
            else {
                SkinnedTextArea tar = new SkinnedTextArea(txt);
                tar.setToolTipText(txt);
                tar.setOpaque(true);
                tar.setBorder(border);
                this.setSpellColor(tar, spell);

                tar.setFocusable(false);
                tar.setEditable(false);
                tar.setLineWrap(true);
                tar.setWrapStyleWord(true);

                /*
                 * TODO - we should figure out how to display cards on the stack in
                 * the Picture/Detail panel The problem not is that when a computer
                 * casts a Morph, the real card shows because Picture/Detail checks
                 * isFaceDown() which will be false on for spell.getSourceCard() on
                 * the stack.
                 */

                // this functionality was present in v 1.1.8
                tar.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(final MouseEvent e) {
                        if (!spell.getStackDescription().startsWith("Morph ")) {
                            CMatchUI.SINGLETON_INSTANCE.setCard(spell.getSpellAbility().getSourceCard());
                        }
                    }
                });

                if (spell.getSpellAbility().isOptionalTrigger() && spell.getSpellAbility().getActivatingPlayer().getLobbyPlayer() == viewer) {
                    tar.addMouseListener(new FMouseAdapter() {
                        @Override
                        public void onRightClick(MouseEvent e) {
                            otMenu.setStackInstance(spell);
                            otMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    });
                }

                parentCell.getBody().add(tar, "w 98%!");
                stackTARs.add(tar);
            }

            /*
             * This updates the Card Picture/Detail when the spell is added to
             * the stack. This functionality was not present in v 1.1.8.
             * 
             * Problem is described in TODO right above this.
             */
            if (isFirst) {
                isFirst = false;
                if (!spell.getStackDescription().startsWith("Morph ")) {
                    CMatchUI.SINGLETON_INSTANCE.setCard(spell.getSourceCard());
                }
            }
        }

        if (!cardView) {
            parentCell.getBody().validate();
            parentCell.getBody().repaint();
        }
    }

    private void setSpellColor(SkinnedTextArea tar, SpellAbilityStackInstance s0) {
        if (s0.getStackDescription().startsWith("Morph ")) {
            tar.setBackground(new Color(0, 0, 0, 0));
            tar.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        }
        else if (CardUtil.getColors(s0.getSourceCard()).isMulticolor()) {
            tar.setBackground(new Color(253, 175, 63));
            tar.setForeground(Color.black);
        }
        else if (s0.getSourceCard().isBlack()) {
            tar.setBackground(Color.black);
            tar.setForeground(Color.white);
        }
        else if (s0.getSourceCard().isBlue()) {
            tar.setBackground(new Color(71, 108, 191));
            tar.setForeground(Color.white);
        }
        else if (s0.getSourceCard().isGreen()) {
            tar.setBackground(new Color(23, 95, 30));
            tar.setForeground(Color.white);
        }
        else if (s0.getSourceCard().isRed()) {
            tar.setBackground(new Color(214, 8, 8));
            tar.setForeground(Color.white);
        }
        else if (s0.getSourceCard().isWhite()) {
            tar.setBackground(Color.white);
            tar.setForeground(Color.black);
        }
        else if (s0.getSourceCard().isArtifact() || s0.getSourceCard().isLand()) {
            tar.setBackground(new Color(111, 75, 43));
            tar.setForeground(Color.white);
        }
        else {
            tar.setBackground(new Color(0, 0, 0, 0));
            tar.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        }
    }

    //========= Custom class handling

    private final class OptionalTriggerMenu extends JPopupMenu {
        private static final long serialVersionUID = 1548494191627807962L;
        private final JCheckBoxMenuItem jmiAccept;
        private final JCheckBoxMenuItem jmiDecline;
        private final JCheckBoxMenuItem jmiAsk;
        private PlayerController localPlayer;

        private Integer triggerID = 0;

        public OptionalTriggerMenu(){

            jmiAccept = new JCheckBoxMenuItem("Always Accept");
            jmiDecline = new JCheckBoxMenuItem("Always Decline");
            jmiAsk = new JCheckBoxMenuItem("Always Ask");

            jmiAccept.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    if (localPlayer == null) { return; }
                    localPlayer.setShouldAlwaysAcceptTrigger(triggerID);
                }
            });

            jmiDecline.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    if (localPlayer == null) { return; }
                    localPlayer.setShouldAlwaysDeclineTrigger(triggerID);
                }
            });

            jmiAsk.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    if (localPlayer == null) { return; }
                    localPlayer.setShouldAlwaysAskTrigger(triggerID);
                }
            });

            add(jmiAccept);
            add(jmiDecline);
            add(jmiAsk);
        }

        public void setStackInstance(final SpellAbilityStackInstance SI) {
            localPlayer = SI.getSpellAbility().getActivatingPlayer().getController();

            triggerID = SI.getSpellAbility().getSourceTrigger();

            if (localPlayer.shouldAlwaysAcceptTrigger(triggerID)) {
                jmiAccept.setSelected(true);
                jmiDecline.setSelected(false);
                jmiAsk.setSelected(false);
            }
            else if (localPlayer.shouldAlwaysDeclineTrigger(triggerID)) {
                jmiDecline.setSelected(true);
                jmiAccept.setSelected(false);
                jmiAsk.setSelected(false);
            }
            else {
                jmiAsk.setSelected(true);
                jmiAccept.setSelected(false);
                jmiDecline.setSelected(false);
            }
        }
    }

    @SuppressWarnings("serial")
    public class StackArea extends CardPanelContainer {
        private static final float STACK_SPACING_Y = 0.1f;

        private Dimension size;

        public StackArea(final FScrollPane scrollPane) {
            super(scrollPane);
        }

        /** {@inheritDoc} */
        @Override
        public final void doLayout() {
            if (this.getCardPanels().isEmpty()) {
                return;
            }

            final Rectangle rect = this.getScrollPane().getVisibleRect();
            final Insets insets = this.getScrollPane().getInsets();
            rect.width -= insets.left;
            rect.height -= insets.top;
            rect.width -= insets.right;
            rect.height -= insets.bottom;

            int maxWidth = rect.width - CardArea.GUTTER_X * 2;
            int maxHeight = rect.height - CardArea.GUTTER_Y * 2;
            int cardWidth = Math.min(maxWidth, this.getCardWidthMax());
            int cardHeight = Math.round(cardWidth * CardPanel.ASPECT_RATIO);
            if (cardHeight > maxHeight) {
                cardHeight = maxHeight;
                cardWidth = Math.round(cardHeight / CardPanel.ASPECT_RATIO);
            }
            int x = CardArea.GUTTER_X + (maxWidth - cardWidth) / 2;
            int y = CardArea.GUTTER_Y;
            int cardSpacingY = Math.round(cardHeight * STACK_SPACING_Y);

            List<CardPanel> panels = this.getCardPanels();
            for (int i = panels.size() - 1; i >= 0; i--) {
                CardPanel panel = panels.get(i);
                panel.setCardBounds(x, y, cardWidth, cardHeight);
                this.setComponentZOrder(panel, i);
                y += cardSpacingY;
            }

            maxWidth = rect.width;
            maxHeight = Math.max(rect.height, y - cardSpacingY + cardHeight);
            final Dimension oldPreferredSize = this.getPreferredSize();
            this.setPreferredSize(new Dimension(maxWidth, maxHeight));
            if ((oldPreferredSize.width != maxWidth) || (oldPreferredSize.height != maxHeight)) {
                this.getParent().invalidate();
                this.getParent().validate();
            }
        }

        @Override
        public final void paint(final Graphics g) {
            Dimension newSize = this.getScrollPane().getSize();
            if (this.size == null || !this.size.equals(newSize)) {
                if (this.size != null) {
                    this.revalidate();
                }
                this.size = newSize;
            }
            super.paint(g);
        }

        @Override
        protected CardPanel getCardPanel(int x, int y) {
            // TODO Auto-generated method stub
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public final void mouseOver(final CardPanel panel, final MouseEvent evt) {
            super.mouseOver(panel, evt);
        }

        /** {@inheritDoc} */
        @Override
        public final void mouseLeftClicked(final CardPanel panel, final MouseEvent evt) {
            super.mouseLeftClicked(panel, evt);
        }

        /** {@inheritDoc} */
        @Override
        public final void mouseRightClicked(final CardPanel panel, final MouseEvent evt) {
            super.mouseRightClicked(panel, evt);
        }
    }
}
