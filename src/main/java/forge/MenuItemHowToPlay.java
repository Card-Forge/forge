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
package forge;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * <p>
 * MenuItem_HowToPlay class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class MenuItemHowToPlay extends JMenuItem {
    /** Constant <code>serialVersionUID=5552000208438248428L</code>. */
    private static final long serialVersionUID = 5552000208438248428L;

    /**
     * <p>
     * Constructor for MenuItem_HowToPlay.
     * </p>
     */
    public MenuItemHowToPlay() {
        super(ForgeProps.getLocalized(NewConstants.Lang.HowTo.TITLE));

        this.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent a) {
                final String text = ForgeProps.getLocalized(NewConstants.Lang.HowTo.MESSAGE);

                final JTextArea area = new JTextArea(text, 25, 40);
                area.setWrapStyleWord(true);
                area.setLineWrap(true);
                area.setEditable(false);

                area.setOpaque(false);

                JOptionPane.showMessageDialog(null, new JScrollPane(area),
                        ForgeProps.getLocalized(NewConstants.Lang.HowTo.TITLE), JOptionPane.INFORMATION_MESSAGE);
            }
        });
    } // constructor
} // MenuItem_HowToPlay

