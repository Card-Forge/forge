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
package forge.control;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.JLayeredPane;
import javax.swing.WindowConstants;

import forge.control.KeyboardShortcuts.Shortcut;
import forge.quest.gui.bazaar.QuestBazaarPanel;
import forge.view.GuiTopLevel;
import forge.view.editor.EditorTopLevel;
import forge.view.home.HomeTopLevel;
import forge.view.match.MatchTopLevel;

/**
 * <p>
 * FControl.
 * </p>
 * Controls all Forge UI functionality inside one JFrame. This class switches
 * between various display states in that JFrame. Controllers are instantiated
 * separately by each state's top level view class.
 */
public class FControl {
    private final JLayeredPane display;
    private final GuiTopLevel view;
    private List<Shortcut> shortcuts;
    private int state;

    private HomeTopLevel home = null;
    private MatchTopLevel match = null;
    private EditorTopLevel editor = null;
    private WindowAdapter waDefault, waConcede, waLeaveBazaar;
    private QuestBazaarPanel bazaar;

    /** */
    public static final int HOME_SCREEN = 0;
    /** */
    public static final int MATCH_SCREEN = 1;
    /** */
    public static final int DEFAULT_EDITOR = 2;
    /** */
    public static final int QUEST_BAZAAR = 3;

    /**
     * <p>
     * FControl.
     * </p>
     * Controls all Forge UI functionality inside one JFrame. This class
     * switches between various display states in that JFrame. Controllers are
     * instantiated separately by each state's top level view class.
     * 
     * @param v0 &emsp; GuiTopLevel
     */
    public FControl(GuiTopLevel v0) {
        this.view = v0;

        this.display = (JLayeredPane) this.view.getContentPane();
        this.shortcuts = KeyboardShortcuts.attachKeyboardShortcuts(this.view);

        // "Close" button override during match
        this.waConcede = new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                view.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                getMatchView().getDockController().concede();
            }
        };

        // "Close" button override while inside bazaar (will probably be used later for other things)
        this.waLeaveBazaar = new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                view.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                changeState(0);
                getHomeView().showQuestMenu();
            }
        };

        // Default action on window close
        this.waDefault = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                view.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            }
         };

         // Handles resizing in null layouts of layers in JLayeredPane.
        view.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) {
                sizeChildren();
            }
        });
    }

    /**
     * <p>
     * changeState.
     * </p>
     * Switches between display states in top level JFrame.
     * 
     * @param i0
     *            &emsp; State index: 0 for home, 1 for match, etc.
     */
    public void changeState(final int i0) {
        this.home = null;
        this.match = null;
        this.editor = null;
        this.state = i0;

        this.display.removeAll();
        this.view.removeWindowListener(waConcede);
        this.view.removeWindowListener(waLeaveBazaar);
        this.view.addWindowListener(waDefault);
        this.view.addOverlay();

        // Fire up new state
        switch (i0) {
            case HOME_SCREEN:
                this.home = new HomeTopLevel();
                this.display.add(this.home, JLayeredPane.DEFAULT_LAYER);
                sizeChildren();
                break;

            case MATCH_SCREEN:
                this.match = new MatchTopLevel();
                this.display.add(this.match, JLayeredPane.DEFAULT_LAYER);
                sizeChildren();
                view.addWindowListener(waConcede);
                break;

            case DEFAULT_EDITOR:
                this.editor = new EditorTopLevel();
                this.display.add(this.editor);
                break;

            case QUEST_BAZAAR:
                this.bazaar = new QuestBazaarPanel(null);
                this.display.add(bazaar, JLayeredPane.DEFAULT_LAYER);
                sizeChildren();
                view.addWindowListener(waLeaveBazaar);
                break;

            default:
                break;
        }
    }

    /**
     * Gets the match view.
     * 
     * @return MatchTopLevel
     */
    public MatchTopLevel getMatchView() {
        return this.match;
    }

    /**
     * Gets the match controller.
     * 
     * @return ControlMatchUI
     */
    public ControlMatchUI getMatchController() {
        return this.match.getController();
    }

    /** @return HomeTopLevel */
    public HomeTopLevel getHomeView() {
        return this.home;
    }

    /** @return HomeTopLevel */
    public ControlHomeUI getHomeController() {
        return this.home.getController();
    }

    /**
     * Gets the match view.
     * 
     * @return MatchTopLevel
     */
    public QuestBazaarPanel getBazaarView() {
        return this.bazaar;
    }

    /** 
     * Returns the int reflecting the current state of the top level frame
     * (see field definitions and class methods for details).
     * 
     * @return {@link java.lang.Integer}
     * */
    public int getState() {
        return this.state;
    }

    /** @return List<Shortcut> A list of attached keyboard shortcut descriptions and properties. */
    public List<Shortcut> getShortcuts() {
        return this.shortcuts;
    }

    /** Sizes children of JLayeredPane to fully fit their layers. */
    private void sizeChildren() {
        Component[] children;
        children = FControl.this.display.getComponentsInLayer(JLayeredPane.DEFAULT_LAYER);

        if (children.length == 0) { return; }

        children[0].setSize(FControl.this.display.getSize());

        children = FControl.this.display.getComponentsInLayer(JLayeredPane.MODAL_LAYER);
        children[0].setSize(FControl.this.display.getSize());
    }
}
