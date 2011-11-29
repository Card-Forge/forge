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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;

import forge.AllZone;
import forge.view.match.ViewTopLevel;
import forge.view.toolbox.FOverlay;

/**
 * Parent JFrame for Forge UI.
 * 
 */
@SuppressWarnings("serial")
public class GuiTopLevel extends JFrame {
    private final JLayeredPane lpnContent;

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

        this.setVisible(true);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent evt) {
                ViewTopLevel t = (ViewTopLevel) AllZone.getDisplay();
                t.getDockController().concede();
            }
        });
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
}
