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
import java.awt.event.ActionListener;
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
import forge.CardUtil;
import forge.card.spellability.SpellAbilityStackInstance;
import forge.game.player.LobbyPlayer;
import forge.game.player.PlayerController;
import forge.game.zone.MagicStack;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.match.CMatchUI;
import forge.gui.match.controllers.CStack;
import forge.gui.toolbox.FSkin;

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

    // Other fields
    private List<JTextArea> stackTARs = new ArrayList<JTextArea>();
    private OptionalTriggerMenu otMenu = new OptionalTriggerMenu();

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
        
        List<JTextArea> list = new ArrayList<JTextArea>();

        parentCell.getBody().removeAll();
        parentCell.getBody().setLayout(new MigLayout("insets 1%, gap 1%, wrap"));

        tab.setText("Stack : " + stack.size());

        final Border border = new EmptyBorder(5, 5, 5, 5);
        Color[] scheme;
        
        stackTARs.clear();
        boolean isFirst = true;
        for (final SpellAbilityStackInstance spell : stack) {
            scheme = getSpellColor(spell);

            String isOptional = spell.getSpellAbility().isOptionalTrigger() && spell.getSourceCard().getController().equals(viewer) ? "(OPTIONAL) " : "";
            String txt = (count++) + ". " + isOptional + spell.getStackDescription();
            JTextArea tar = new JTextArea(txt);
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
            
            if(spell.getSpellAbility().isOptionalTrigger() && spell.getSpellAbility().getActivatingPlayer().getLobbyPlayer() == viewer) {
                tar.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e){
                        if (e.getButton() == MouseEvent.BUTTON3)
                        {
                            otMenu.setStackInstance(spell);
                            otMenu.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                });
            }
            list.add(tar);
            
            /*
             * This updates the Card Picture/Detail when the spell is added to
             * the stack. This functionality was not present in v 1.1.8.
             * 
             * Problem is described in TODO right above this.
             */
            if (isFirst && !spell.getStackDescription().startsWith("Morph ")) {
                CMatchUI.SINGLETON_INSTANCE.setCard(spell.getSourceCard());
            }
            isFirst = false;

            parentCell.getBody().add(tar, "w 98%!");
            stackTARs.add(tar);
        }

        parentCell.getBody().repaint();
    }

    /** Returns array with [background, foreground] colors. */
    private Color[] getSpellColor(SpellAbilityStackInstance s0) {
        if (s0.getStackDescription().startsWith("Morph ")) 
            return new Color[] { new Color(0, 0, 0, 0), FSkin.getColor(FSkin.Colors.CLR_TEXT) };
        if (CardUtil.getColors(s0.getSourceCard()).isMulticolor()) 
            return new Color[] { new Color(253, 175, 63), Color.black };

        if (s0.getSourceCard().isBlack())      return new Color[] { Color.black, Color.white };
        if (s0.getSourceCard().isBlue())       return new Color[] { new Color(71, 108, 191), Color.white };
        if (s0.getSourceCard().isGreen())      return new Color[] { new Color(23, 95, 30), Color.white };
        if (s0.getSourceCard().isRed())        return new Color[] { new Color(214, 8, 8), Color.white };
        if (s0.getSourceCard().isWhite())      return new Color[] { Color.white, Color.black };

        if (s0.getSourceCard().isArtifact() || s0.getSourceCard().isLand())
            return new Color[] { new Color(111, 75, 43), Color.white };

        return new Color[] { new Color(0, 0, 0, 0), FSkin.getColor(FSkin.Colors.CLR_TEXT) };
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
                    if ( localPlayer == null ) return;
                    localPlayer.setShouldAlwaysAcceptTrigger(triggerID);
                }
                
            });
            
            jmiDecline.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    if ( localPlayer == null ) return;
                    localPlayer.setShouldAlwaysDeclineTrigger(triggerID);
                }
                
            });
            
            jmiAsk.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    if ( localPlayer == null ) return;
                    localPlayer.setShouldAlwaysAskTrigger(triggerID);
                }
                
            });
            
            add(jmiAccept);
            add(jmiDecline);
            add(jmiAsk);
        }
        
        public void setStackInstance(final SpellAbilityStackInstance SI)
        {
            localPlayer = SI.getSpellAbility().getActivatingPlayer().getController();
            
            triggerID = SI.getSpellAbility().getSourceTrigger();
            
            if(localPlayer.shouldAlwaysAcceptTrigger(triggerID)) {
                jmiAccept.setSelected(true);
                jmiDecline.setSelected(false);
                jmiAsk.setSelected(false);
            } else if(localPlayer.shouldAlwaysDeclineTrigger(triggerID)) {
                jmiDecline.setSelected(true);
                jmiAccept.setSelected(false);
                jmiAsk.setSelected(false);
            } else {
                jmiAsk.setSelected(true);
                jmiAccept.setSelected(false);
                jmiDecline.setSelected(false);
            }
        }
    }
}
