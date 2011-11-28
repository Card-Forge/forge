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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import forge.AllZone;
import forge.gui.GuiUtils;
import forge.quest.gui.bazaar.QuestBazaarPanel;
import forge.view.swing.GuiHomeScreen;
import forge.view.swing.OldGuiNewGame;

/**
 * <p>
 * QuestFrame class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestFrame extends JFrame {
    /** Constant <code>serialVersionUID=-2832625381531838412L</code>. */
    private static final long serialVersionUID = -2832625381531838412L;

    /** The visible panel. */
    private final JPanel visiblePanel;

    /** The quest layout. */
    private final CardLayout questLayout;

    /** Constant <code>MAIN_PANEL="Main"</code>. */
    public static final String MAIN_PANEL = "Main";

    /** Constant <code>BAZAAR_PANEL="Bazaar"</code>. */
    public static final String BAZAAR_PANEL = "Bazaar";

    /** The sub panel map. */
    private final Map<String, QuestAbstractPanel> subPanelMap = new HashMap<String, QuestAbstractPanel>();

    /**
     * <p>
     * Constructor for QuestFrame.
     * </p>
     * 
     * 
     * the headless exception
     */
    public QuestFrame() {
        this.setTitle("Quest Mode");

        this.visiblePanel = new JPanel(new BorderLayout());
        this.visiblePanel.setBorder(new EmptyBorder(2, 2, 2, 2));
        this.questLayout = new CardLayout();
        this.visiblePanel.setLayout(this.questLayout);

        QuestAbstractPanel newPanel = new QuestMainPanel(this);
        this.visiblePanel.add(newPanel, QuestFrame.MAIN_PANEL);
        this.subPanelMap.put(QuestFrame.MAIN_PANEL, newPanel);

        newPanel = new QuestBazaarPanel(this);
        this.visiblePanel.add(newPanel, QuestFrame.BAZAAR_PANEL);
        this.subPanelMap.put(QuestFrame.BAZAAR_PANEL, newPanel);

        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(this.visiblePanel, BorderLayout.CENTER);
        this.setPreferredSize(new Dimension(1024, 768));
        this.setMinimumSize(new Dimension(800, 600));

        this.questLayout.show(this.visiblePanel, QuestFrame.MAIN_PANEL);

        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.pack();
        this.setVisible(true);

        GuiUtils.centerFrame(this);

    }

    /**
     * <p>
     * showPane.
     * </p>
     * 
     * @param paneName
     *            a {@link java.lang.String} object.
     */
    private void showPane(final String paneName) {
        this.subPanelMap.get(paneName).refreshState();
        this.questLayout.show(this.visiblePanel, paneName);
    }

    /**
     * <p>
     * showMainPane.
     * </p>
     */
    public final void showMainPane() {
        this.showPane(QuestFrame.MAIN_PANEL);
    }

    /**
     * <p>
     * showBazaarPane.
     * </p>
     */
    public final void showBazaarPane() {
        this.showPane(QuestFrame.BAZAAR_PANEL);
    }

    /**
     * <p>
     * returnToMainMenu.
     * </p>
     */
    public final void returnToMainMenu() {
        AllZone.getQuestData().saveData();

        if (System.getenv("NG2") != null) {
            if (System.getenv("NG2").equalsIgnoreCase("true")) {
                final String[] argz = {};
                GuiHomeScreen.main(argz);
            } else {
                new OldGuiNewGame();
            }
        } else {
            new OldGuiNewGame();
        }

        this.dispose();
    }
}
