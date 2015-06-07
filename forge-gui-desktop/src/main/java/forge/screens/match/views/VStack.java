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
package forge.screens.match.views;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;
import forge.ImageCache;
import forge.card.CardDetailUtil;
import forge.card.CardDetailUtil.DetailColors;
import forge.card.CardStateName;
import forge.game.GameView;
import forge.game.card.CardView.CardStateView;
import forge.game.spellability.StackItemView;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.screens.match.controllers.CStack;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FScrollPanel;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedTextArea;
import forge.util.collect.FCollectionView;

/**
 * Assembles Swing components of stack report.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VStack implements IVDoc<CStack> {

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Stack");

    // Top-level containers
    private final FScrollPanel scroller = new FScrollPanel(new MigLayout("insets 0, gap 0, wrap"), true,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    // Other fields
    private final AbilityMenu abilityMenu = new AbilityMenu();

    private StackInstanceTextArea hoveredItem;

    public StackInstanceTextArea getHoveredItem() {
        return hoveredItem;
    }

    private final CStack controller;
    public VStack(final CStack controller) {
        this.controller = controller;
    }

    @Override
    public void populate() {
        parentCell.getBody().setLayout(new MigLayout("insets 3px, gap 0"));
        parentCell.getBody().add(scroller, "grow, push");
    }

    @Override
    public void setParentCell(final DragCell cell0) {
        parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return parentCell;
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.REPORT_STACK;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CStack getLayoutControl() {
        return controller;
    }

    public void updateStack() {
        final GameView model = controller.getMatchUI().getGameView();

        if (model == null) {
            return;
        }

        final FCollectionView<StackItemView> items = model.getStack();
        tab.setText("Stack : " + items.size());

        // No need to update the rest unless it's showing
        if (!parentCell.getSelected().equals(this)) { return; }

        hoveredItem = null;
        scroller.removeAll();

        boolean isFirst = true;
        for (final StackItemView item : items) {
            final StackInstanceTextArea tar = new StackInstanceTextArea(item);

            scroller.add(tar, "pushx, growx" + (isFirst ? "" : ", gaptop 2px"));

            //update the Card Picture/Detail when the spell is added to the stack
            if (isFirst) {
                isFirst = false;
                controller.getMatchUI().setCard(item.getSourceCard());
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
    public class StackInstanceTextArea extends SkinnedTextArea {
        public static final int PADDING = 3;
        public static final int CARD_WIDTH = 50;
        public static final int CARD_HEIGHT = 70;//Math.round((float)CARD_WIDTH * CardPanel.ASPECT_RATIO);

        private final StackItemView item;

        public StackItemView getItem() {
            return item;
        }

        @Override
        public Point getLocationOnScreen() {
            try {
                return super.getLocationOnScreen();
            } catch (final Exception e) {
                //suppress exception that can occur if stack hidden while over an item
                if (hoveredItem == this) {
                    hoveredItem = null; //reset this if this happens
                }
                return null;
            }
        }

        public StackInstanceTextArea(final StackItemView item0) {
            item = item0;

            final String txt = (item.isOptionalTrigger() && controller.getMatchUI().isLocalPlayer(item.getActivatingPlayer())
                    ? "(OPTIONAL) " : "") + item.getText();

            setText(txt);
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
                    hoveredItem = StackInstanceTextArea.this;
                    controller.getMatchUI().setCard(item.getSourceCard());
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    if (hoveredItem == StackInstanceTextArea.this) {
                        hoveredItem = null;
                    }
                }
            });

            if (item.isAbility()) {
                addMouseListener(new FMouseAdapter() {
                    @Override
                    public void onLeftClick(final MouseEvent e) {
                        onClick(e);
                    }
                    @Override
                    public void onRightClick(final MouseEvent e) {
                        onClick(e);
                    }
                    private void onClick(final MouseEvent e) {
                        abilityMenu.setStackInstance(item);
                        abilityMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                });
            }

            // TODO: A hacky workaround is currently used to make the game not leak the color information for Morph cards.
            final CardStateView curState = item.getSourceCard().getCurrentState();
            final boolean isFaceDown = curState.getState() == CardStateName.FaceDown;
            final DetailColors color = isFaceDown ? CardDetailUtil.DetailColors.FACE_DOWN : CardDetailUtil.getBorderColor(curState, true); // otherwise doesn't work correctly for face down Morphs
            setBackground(new Color(color.r, color.g, color.b));
            setForeground(FSkin.getHighContrastColor(getBackground()));
        }

        @Override
        public void paintComponent(final Graphics g) {
            super.paintComponent(g);

            final Graphics2D g2d = (Graphics2D) g;

            //draw image for source card
            final BufferedImage img = ImageCache.getImage(item.getSourceCard(), controller.getMatchUI().getLocalPlayers(), CARD_WIDTH, CARD_HEIGHT);
            if (img != null) {
                g2d.drawImage(img, null, PADDING, PADDING);
            }
        }
    }

    //========= Custom class handling

    private final class AbilityMenu extends JPopupMenu {
        private static final long serialVersionUID = 1548494191627807962L;
        private final JCheckBoxMenuItem jmiAutoYield;
        private final JCheckBoxMenuItem jmiAlwaysYes;
        private final JCheckBoxMenuItem jmiAlwaysNo;
        private StackItemView item;

        private Integer triggerID = 0;

        public AbilityMenu(){
            jmiAutoYield = new JCheckBoxMenuItem("Auto-Yield");
            jmiAutoYield.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    final String key = item.getKey();
                    final boolean autoYield = controller.getMatchUI().shouldAutoYield(key);
                    controller.getMatchUI().setShouldAutoYield(key, !autoYield);
                    if (!autoYield && controller.getMatchUI().getGameView().peekStack() == item) {
                        //auto-pass priority if ability is on top of stack
                        controller.getMatchUI().getGameController().passPriority();
                    }
                }
            });
            add(jmiAutoYield);

            jmiAlwaysYes = new JCheckBoxMenuItem("Always Yes");
            jmiAlwaysYes.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    if (controller.getMatchUI().shouldAlwaysAcceptTrigger(triggerID)) {
                        controller.getMatchUI().setShouldAlwaysAskTrigger(triggerID);
                    }
                    else {
                        controller.getMatchUI().setShouldAlwaysAcceptTrigger(triggerID);
                        if (controller.getMatchUI().getGameView().peekStack() == item) {
                            //auto-yes if ability is on top of stack
                            controller.getMatchUI().getGameController().selectButtonOk();
                        }
                    }
                }
            });
            add(jmiAlwaysYes);

            jmiAlwaysNo = new JCheckBoxMenuItem("Always No");
            jmiAlwaysNo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent arg0) {
                    if (controller.getMatchUI().shouldAlwaysDeclineTrigger(triggerID)) {
                        controller.getMatchUI().setShouldAlwaysAskTrigger(triggerID);
                    }
                    else {
                        controller.getMatchUI().setShouldAlwaysDeclineTrigger(triggerID);
                        if (controller.getMatchUI().getGameView().peekStack() == item) {
                            //auto-no if ability is on top of stack
                            controller.getMatchUI().getGameController().selectButtonCancel();
                        }
                    }
                }
            });
            add(jmiAlwaysNo);
        }

        public void setStackInstance(final StackItemView item0) {
            item = item0;
            triggerID = Integer.valueOf(item.getSourceTrigger());

            jmiAutoYield.setSelected(controller.getMatchUI().shouldAutoYield(item.getKey()));

            if (item.isOptionalTrigger() && controller.getMatchUI().isLocalPlayer(item.getActivatingPlayer())) {
                jmiAlwaysYes.setSelected(controller.getMatchUI().shouldAlwaysAcceptTrigger(triggerID));
                jmiAlwaysNo.setSelected(controller.getMatchUI().shouldAlwaysDeclineTrigger(triggerID));
                jmiAlwaysYes.setVisible(true);
                jmiAlwaysNo.setVisible(true);
            } else {
                jmiAlwaysYes.setVisible(false);
                jmiAlwaysNo.setVisible(false);
            }
        }
    }
}
