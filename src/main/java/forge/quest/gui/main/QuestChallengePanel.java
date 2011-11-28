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
package forge.quest.gui.main;

import javax.swing.JLabel;

import forge.gui.GuiUtils;

/**
 * <p>
 * QuestQuestPanel.
 * </p>
 * VIEW - Creates a QuestSelectablePanel instance for a "quest" style event.
 */

@SuppressWarnings("serial")
public class QuestChallengePanel extends QuestSelectablePanel {

    private JLabel repeatabilityLabel;

    /**
     * <p>
     * QuestChallengePanel.
     * </p>
     * Constructor, using challenge data instance.
     * 
     * @param q
     *            the q
     */
    public QuestChallengePanel(final QuestChallenge q) {
        super(q);

        GuiUtils.addGap(super.getRootPanel(), 7);

        if (q.getRepeatable()) {
            this.repeatabilityLabel = new JLabel("This challenge is repeatable");
        } else {
            this.repeatabilityLabel = new JLabel("This challenge is not repeatable");
        }

        super.getRootPanel().add(this.repeatabilityLabel);

    }
}
