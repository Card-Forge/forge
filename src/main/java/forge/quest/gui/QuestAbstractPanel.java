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
package forge.quest.gui;

import javax.swing.JPanel;

/**
 * <p>
 * Abstract QuestAbstractPanel class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class QuestAbstractPanel extends JPanel {
    /** Constant <code>serialVersionUID=-6378675010346615367L</code>. */
    private static final long serialVersionUID = -6378675010346615367L;

    /** The main frame. */
    private QuestFrame mainFrame;

    /**
     * <p>
     * Constructor for QuestAbstractPanel.
     * </p>
     * 
     * @param mainFrame
     *            a {@link forge.quest.gui.QuestFrame} object.
     */
    protected QuestAbstractPanel(final QuestFrame mainFrame) {
        this.setMainFrame(mainFrame);
    }

    /**
     * <p>
     * refreshState.
     * </p>
     */
    public abstract void refreshState();

    /**
     * Gets the main frame.
     * 
     * @return the mainFrame
     */
    public QuestFrame getMainFrame() {
        return this.mainFrame;
    }

    /**
     * Sets the main frame.
     * 
     * @param mainFrame
     *            the mainFrame to set
     */
    public void setMainFrame(final QuestFrame mainFrame) {
        this.mainFrame = mainFrame; // TODO: Add 0 to parameter's name.
    }
}
