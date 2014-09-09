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
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.screens.match.CMatchUI;
import forge.screens.match.controllers.CPrompt;
import forge.screens.match.controllers.CStack;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FScrollPanel;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedTextArea;
import forge.view.CardView;
import forge.view.IGameView;
import forge.view.PlayerView;
import forge.view.StackItemView;
import forge.view.arcane.CardPanel;

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
    private static AbilityMenu abilityMenu = new AbilityMenu();

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
     * @param model
     * @param viewer */
    public void updateStack(final IGameView model, final PlayerView localPlayer) {
        final java.util.List<StackItemView> items = model.getStack();
        tab.setText("Stack : " + items.size());

        // No need to update the rest unless it's showing
        if (!parentCell.getSelected().equals(this)) { return; }

        scroller.removeAll();

        boolean isFirst = true;
        for (final StackItemView item : items) {
            final StackInstanceTextArea tar = new StackInstanceTextArea(model, item, localPlayer);

            scroller.add(tar, "pushx, growx" + (isFirst ? "" : ", gaptop 2px"));

            //update the Card Picture/Detail when the spell is added to the stack
            if (isFirst) {
                isFirst = false;
                CMatchUI.SINGLETON_INSTANCE.setCard(item.getSource());
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

        private final CardView sourceCard;

        public StackInstanceTextArea(final IGameView game, final StackItemView item, final PlayerView localPlayer) {
            sourceCard = item.getSource();

            final String txt = (item.isOptionalTrigger() && item.getActivatingPlayer().equals(localPlayer)
                    ? "(OPTIONAL) " : "") + item.getText();

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
                    if (!txt.startsWith("Morph ")) {
                        CMatchUI.SINGLETON_INSTANCE.setCard(item.getSource());
                    }
                }
            });

            if (item.isAbility() && localPlayer != null) {
                addMouseListener(new FMouseAdapter() {
                    @Override
                    public void onLeftClick(MouseEvent e) {
                        onClick(e);
                    }
                    @Override
                    public void onRightClick(MouseEvent e) {
                        onClick(e);
                    }
                    private void onClick(MouseEvent e) {
                        abilityMenu.setStackInstance(game, item, localPlayer);
                        abilityMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                });
            }

            final DetailColors color = CardDetailUtil.getBorderColor(item.getSource().getOriginal());
            setBackground(new Color(color.r, color.g, color.b));
            setForeground(FSkin.getHighContrastColor(getBackground()));
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            final Graphics2D g2d = (Graphics2D) g;

            //draw image for source card
            final BufferedImage img = ImageCache.getImage(sourceCard, CARD_WIDTH, CARD_HEIGHT);
            if (img != null) {
                g2d.drawImage(img, null, PADDING, PADDING);
            }
        }
    }

    //========= Custom class handling

    private final static class AbilityMenu extends JPopupMenu {
        private static final long serialVersionUID = 1548494191627807962L;
        private final JCheckBoxMenuItem jmiAutoYield;
        private final JCheckBoxMenuItem jmiAlwaysYes;
        private final JCheckBoxMenuItem jmiAlwaysNo;
        private IGameView game;
        private StackItemView item;

        private Integer triggerID = 0;

        public AbilityMenu(){
            jmiAutoYield = new JCheckBoxMenuItem("Auto-Yield");
            jmiAutoYield.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    final String key = item.getKey();
                    final boolean autoYield = game.shouldAutoYield(key);
                    game.setShouldAutoYield(key, !autoYield);
                    if (!autoYield && game.peekStack() == item) {
                        //auto-pass priority if ability is on top of stack
                        CPrompt.SINGLETON_INSTANCE.passPriority();
                    }
                }
            });
            add(jmiAutoYield);

            jmiAlwaysYes = new JCheckBoxMenuItem("Always Yes");
            jmiAlwaysYes.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    if (game.shouldAlwaysAcceptTrigger(triggerID)) {
                        game.setShouldAlwaysAskTrigger(triggerID);
                    }
                    else {
                        game.setShouldAlwaysAcceptTrigger(triggerID);
                        if (game.peekStack() == item) {
                            //auto-yes if ability is on top of stack
                            game.confirm();
                        }
                    }
                }
            });
            add(jmiAlwaysYes);

            jmiAlwaysNo = new JCheckBoxMenuItem("Always No");
            jmiAlwaysNo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    if (game.shouldAlwaysDeclineTrigger(triggerID)) {
                        game.setShouldAlwaysAskTrigger(triggerID);
                    }
                    else {
                        game.setShouldAlwaysDeclineTrigger(triggerID);
                        if (game.peekStack() == item) {
                            //auto-no if ability is on top of stack
                            game.confirm();
                        }
                    }
                }
            });
            add(jmiAlwaysNo);
        }

        public void setStackInstance(final IGameView game, final StackItemView item, final PlayerView localPlayer) {
            this.game = game;
            this.item = item;
            triggerID = Integer.valueOf(item.getSourceTrigger());

            jmiAutoYield.setSelected(game.shouldAutoYield(item.getKey()));

            if (item.isOptionalTrigger() && item.getActivatingPlayer().equals(localPlayer)) {
                jmiAlwaysYes.setSelected(game.shouldAlwaysAcceptTrigger(triggerID));
                jmiAlwaysNo.setSelected(game.shouldAlwaysDeclineTrigger(triggerID));
                jmiAlwaysYes.setVisible(true);
                jmiAlwaysNo.setVisible(true);
            }
            else {
                jmiAlwaysYes.setVisible(false);
                jmiAlwaysNo.setVisible(false);
            }
        }
    }
}
