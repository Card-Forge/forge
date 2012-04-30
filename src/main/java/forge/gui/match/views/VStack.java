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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.CardUtil;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.game.zone.MagicStack;
import forge.gui.ForgeAction;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.gui.match.CMatchUI;
import forge.gui.match.VMatchUI;
import forge.gui.match.controllers.CStack;
import forge.gui.toolbox.FSkin;
import forge.properties.NewConstants;

/** 
 * Assembles Swing components of stack report.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VStack implements IVDoc {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Stack");

    // Other fields
    private List<JTextArea> stackTARs = new ArrayList<JTextArea>();
    private TriggerReactionMenu triggerMenu = new TriggerReactionMenu();

    //========= Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        // (Panel uses observers to update, no permanent components here.)
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
     * @see forge.gui.framework.IVDoc#getControl()
     */
    @Override
    public ICDoc getControl() {
        return CStack.SINGLETON_INSTANCE;
    }

    //========== Observer update methods

    /** */
    public void updateStack() {
        // No need to update this unless it's showing
        if (!parentCell.getSelected().equals(this)) { return; }

        final MagicStack stack = AllZone.getStack();

        int count = 1;
        JTextArea tar;
        String txt, isOptional;

        parentCell.getBody().removeAll();
        parentCell.getBody().setLayout(new MigLayout("insets 1%, gap 1%, wrap"));

        tab.setText("Stack : " + stack.size());

        final Border border = new EmptyBorder(5, 5, 5, 5);
        Color[] scheme;

        stackTARs.clear();
        for (int i = stack.size() - 1; 0 <= i; i--) {
            final SpellAbilityStackInstance spell = stack.peekInstance(i);
            final int index = i;

            scheme = getSpellColor(spell);

            isOptional = stack.peekAbility(i).isOptionalTrigger()
                    && stack.peekAbility(i).getSourceCard().getController().isHuman() ? "(OPTIONAL) " : "";
            txt = (count++) + ". " + isOptional + spell.getStackDescription();
            tar = new JTextArea(txt);
            tar.setToolTipText(txt);
            tar.setOpaque(true);
            tar.setBorder(border);
            tar.setForeground(scheme[1]);
            tar.setBackground(scheme[0]);

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

            /*
             * This updates the Card Picture/Detail when the spell is added to
             * the stack. This funcaitonality was not present in v 1.1.8.
             * 
             * Problem is described in TODO right above this.
             */
            if (i == 0 && !spell.getStackDescription().startsWith("Morph ")) {
                CMatchUI.SINGLETON_INSTANCE.setCard(spell.getSourceCard());
            }

            parentCell.getBody().add(tar, "w 98%!");
            stackTARs.add(tar);

            if (stack.peekInstance(i).isOptionalTrigger()) {
                tar.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(final MouseEvent e) {

                        if (e.getButton() != MouseEvent.BUTTON3) {
                            return;
                        }

                        triggerMenu.setTrigger(stack.peekAbility(index).getSourceTrigger());
                        triggerMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                });
            }
        }

        VMatchUI.SINGLETON_INSTANCE.getBtnOK().requestFocusInWindow();
    }

    /** Returns array with [background, foreground] colors. */
    private Color[] getSpellColor(SpellAbilityStackInstance s0) {
        if (CardUtil.getColors(s0.getSourceCard()).size() > 1) {
            return new Color[] { new Color(253, 175, 63), Color.black };
        } else if (s0.getSourceCard().isBlack()) {
            return new Color[] { Color.black, Color.white };
        } else if (s0.getSourceCard().isBlue()) {
            return new Color[] { new Color(71, 108, 191), Color.white };
        } else if (s0.getSourceCard().isGreen()) {
            return new Color[] { new Color(23, 95, 30), Color.white };
        } else if (s0.getSourceCard().isRed()) {
            return new Color[] { new Color(214, 8, 8), Color.white };
        } else if (s0.getSourceCard().isWhite()) {
            return new Color[] { Color.white, Color.black };
        } else if (s0.getSourceCard().isArtifact() || s0.getSourceCard().isLand()) {
            return new Color[] { new Color(111, 75, 43), Color.white };
        }

        return new Color[] { new Color(0, 0, 0, 0), FSkin.getColor(FSkin.Colors.CLR_TEXT) };
    }

    //========= Custom class handling

    private class TriggerReactionMenu extends JPopupMenu {
        private static final long serialVersionUID = 6665085414634139984L;
        private int workTrigID;

        public TriggerReactionMenu() {
            super();

            final ForgeAction actAccept = new ForgeAction(NewConstants.Lang.GuiDisplay.Trigger.ALWAYSACCEPT) {
                private static final long serialVersionUID = -3734674058185367612L;

                @Override
                public final void actionPerformed(final ActionEvent e) {
                    AllZone.getTriggerHandler().setAlwaysAcceptTrigger(TriggerReactionMenu.this.workTrigID);
                }
            };

            final ForgeAction actDecline = new ForgeAction(NewConstants.Lang.GuiDisplay.Trigger.ALWAYSDECLINE) {
                private static final long serialVersionUID = -1983295769159971502L;

                @Override
                public final void actionPerformed(final ActionEvent e) {
                    AllZone.getTriggerHandler().setAlwaysDeclineTrigger(TriggerReactionMenu.this.workTrigID);
                }
            };

            final ForgeAction actAsk = new ForgeAction(NewConstants.Lang.GuiDisplay.Trigger.ALWAYSASK) {
                private static final long serialVersionUID = 5045255351332940821L;

                @Override
                public final void actionPerformed(final ActionEvent e) {
                    AllZone.getTriggerHandler().setAlwaysAskTrigger(TriggerReactionMenu.this.workTrigID);
                }
            };

            final JCheckBoxMenuItem jcbmiAccept = new JCheckBoxMenuItem(actAccept);
            final JCheckBoxMenuItem jcbmiDecline = new JCheckBoxMenuItem(actDecline);
            final JCheckBoxMenuItem jcbmiAsk = new JCheckBoxMenuItem(actAsk);

            this.add(jcbmiAccept);
            this.add(jcbmiDecline);
            this.add(jcbmiAsk);
        }

        public void setTrigger(final int trigID) {
            this.workTrigID = trigID;

            if (AllZone.getTriggerHandler().isAlwaysAccepted(trigID)) {
                ((JCheckBoxMenuItem) this.getComponent(0)).setState(true);
                ((JCheckBoxMenuItem) this.getComponent(1)).setState(false);
                ((JCheckBoxMenuItem) this.getComponent(2)).setState(false);
            } else if (AllZone.getTriggerHandler().isAlwaysDeclined(trigID)) {
                ((JCheckBoxMenuItem) this.getComponent(0)).setState(false);
                ((JCheckBoxMenuItem) this.getComponent(1)).setState(true);
                ((JCheckBoxMenuItem) this.getComponent(2)).setState(false);
            } else {
                ((JCheckBoxMenuItem) this.getComponent(0)).setState(false);
                ((JCheckBoxMenuItem) this.getComponent(1)).setState(false);
                ((JCheckBoxMenuItem) this.getComponent(2)).setState(true);
            }
        }
    }
}
