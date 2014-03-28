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

import forge.ImageCache;
import forge.game.card.Card;
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
import forge.gui.toolbox.FScrollPanel;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.SkinnedTextArea;
import forge.view.arcane.CardPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

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
    private final DragTab tab = new DragTab("Stack");

    // Top-level containers
    private final FScrollPanel scroller = new FScrollPanel(new MigLayout("insets 0, gap 0, wrap"), true,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    // Other fields
    private static OptionalTriggerMenu otMenu = new OptionalTriggerMenu();

    private VStack() {
    }

    //========= Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        parentCell.getBody().setLayout(new MigLayout("insets 3px, gap 0"));
        parentCell.getBody().add(scroller, "grow, push");
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
        tab.setText("Stack : " + stack.size());

        // No need to update the rest unless it's showing
        if (!parentCell.getSelected().equals(this)) { return; }

        scroller.removeAll();

        boolean isFirst = true;
        for (final SpellAbilityStackInstance spell : stack) {
            StackInstanceTextArea tar = new StackInstanceTextArea(spell, viewer);

            scroller.add(tar, "pushx, growx" + (isFirst ? "" : ", gaptop 2px"));

            //update the Card Picture/Detail when the spell is added to the stack
            if (isFirst) {
                isFirst = false;
                CMatchUI.SINGLETON_INSTANCE.setCard(spell.getSourceCard());
            }
        }

        scroller.revalidate();
        scroller.repaint();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                scroller.scrollToTop();
            }
        });
    }

    @SuppressWarnings("serial")
    private static class StackInstanceTextArea extends SkinnedTextArea {
        private static final int PADDING = 3;
        private static final int CARD_WIDTH = 50;
        private static final int CARD_HEIGHT = Math.round((float)CARD_WIDTH * CardPanel.ASPECT_RATIO);

        private final Card sourceCard;

        public StackInstanceTextArea(final SpellAbilityStackInstance spell, final LobbyPlayer viewer) {
            sourceCard = spell.getSourceCard();

            String txt = spell.getStackDescription();
            if (spell.getSpellAbility().isOptionalTrigger()
                    && spell.getSourceCard().getController().getController().getLobbyPlayer().equals(viewer)) {
                txt = "(OPTIONAL) " + txt;
            }
            setText(txt);
            setToolTipText(txt);
            setOpaque(true);
            setBorder(new EmptyBorder(PADDING, CARD_WIDTH + 2 * PADDING, PADDING, PADDING));
            setFocusable(false);
            setEditable(false);
            setLineWrap(true);
            setFont(FSkin.getFont(12));
            setWrapStyleWord(true);
            setMinimumSize(new Dimension(CARD_WIDTH + 2 * PADDING, CARD_HEIGHT + 2 * PADDING));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    if (!spell.getStackDescription().startsWith("Morph ")) {
                        CMatchUI.SINGLETON_INSTANCE.setCard(spell.getSpellAbility().getHostCard());
                    }
                }
            });

            if (spell.getSpellAbility().isOptionalTrigger() && spell.getSpellAbility().getActivatingPlayer().getLobbyPlayer() == viewer) {
                addMouseListener(new FMouseAdapter() {
                    @Override
                    public void onRightClick(MouseEvent e) {
                        otMenu.setStackInstance(spell);
                        otMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                });
            }
            
            if (spell.getStackDescription().startsWith("Morph ")) {
                setBackground(new Color(0, 0, 0, 0));
                setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            }
            else if (CardUtil.getColors(spell.getSourceCard()).isMulticolor()) {
                setBackground(new Color(253, 175, 63));
                setForeground(Color.black);
            }
            else if (sourceCard.isBlack()) {
                setBackground(Color.black);
                setForeground(Color.white);
            }
            else if (sourceCard.isBlue()) {
                setBackground(new Color(71, 108, 191));
                setForeground(Color.white);
            }
            else if (sourceCard.isGreen()) {
                setBackground(new Color(23, 95, 30));
                setForeground(Color.white);
            }
            else if (sourceCard.isRed()) {
                setBackground(new Color(214, 8, 8));
                setForeground(Color.white);
            }
            else if (sourceCard.isWhite()) {
                setBackground(Color.white);
                setForeground(Color.black);
            }
            else if (sourceCard.isArtifact() || sourceCard.isLand()) {
                setBackground(new Color(111, 75, 43));
                setForeground(Color.white);
            }
            else {
                setBackground(new Color(0, 0, 0, 0));
                setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            }
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            final Graphics2D g2d = (Graphics2D) g;

            //draw image for source card
            BufferedImage img = ImageCache.getImage(sourceCard, CARD_WIDTH, CARD_HEIGHT);
            if (img != null) {
                g2d.drawImage(img, null, PADDING, PADDING);
            }
        }
    }

    //========= Custom class handling

    private final static class OptionalTriggerMenu extends JPopupMenu {
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
}
