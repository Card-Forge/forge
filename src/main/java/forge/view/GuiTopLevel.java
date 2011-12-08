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
package forge.view;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.Display;
import forge.MyButton;
import forge.Player;
import forge.control.ControlAllUI;
import forge.view.toolbox.FOverlay;

/**
 * Parent JFrame for Forge UI.
 * 
 */
@SuppressWarnings("serial")
public class GuiTopLevel extends JFrame implements Display {
    private final JLayeredPane lpnContent;
    private ControlAllUI control;

    /**
     * Parent JFrame for Forge UI.
     */
    public GuiTopLevel() {
        super();
        this.setMinimumSize(new Dimension(800, 600));
        this.setLocationRelativeTo(null);
        this.setExtendedState(this.getExtendedState() | Frame.MAXIMIZED_BOTH);

        this.lpnContent = new JLayeredPane();
        this.lpnContent.setOpaque(true);
        this.setContentPane(this.lpnContent);
        this.addOverlay();
        this.setIconImage(Toolkit.getDefaultToolkit().getImage("res/images/symbols-13/favicon.png"));
        this.setTitle("Forge");


        this.setVisible(true);

        // Init controller
        control = new ControlAllUI(this);
    }

    /**
     * Adds overlay panel to modal layer. Used when removeAll() has been called
     * on the JLayeredPane parent.
     */
    public void addOverlay() {
        final FOverlay pnlOverlay = new FOverlay();
        AllZone.setOverlay(pnlOverlay);
        pnlOverlay.setOpaque(false);
        pnlOverlay.setVisible(false);
        pnlOverlay.setBounds(0, 0, this.getWidth(), this.getHeight());
        this.lpnContent.add(pnlOverlay, JLayeredPane.MODAL_LAYER);
    }

    /** @return ControlAllUI */
    public ControlAllUI getController() {
        return control;
    }

    /* ========================================================
     * 
     * WILL BE DEPRECATED SOON WITH DISPLAY INTERFACE UPDATE!!!
     *
     ========================================================*/

    public void showMessage(String s) { }
    public MyButton getButtonOK() { return new EmptyButton(); }
    public MyButton getButtonCancel() { return new EmptyButton(); }
    public void showCombat(String message) { }
    public void assignDamage(Card attacker, CardList blockers, int damage) { }
    public boolean stopAtPhase(Player turn, String phase) { return true; }
    public boolean loadPrefs() { return true; }
    public boolean savePrefs() { return true; }
    public boolean canLoseByDecking() { return true; }
    public void setCard(Card c) { }
    /** THIS CLASS ONLY EXISTS TO KEEP THE INTERFACE HAPPY TEMPORARILY. */
    private class EmptyButton extends JButton implements MyButton {
        public void reset() {}
        public void setSelectable(boolean b0) {}
        public boolean isSelectable() {return true;}
        public void select() {}
    }
}
