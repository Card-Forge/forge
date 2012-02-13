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
import java.awt.event.WindowListener;
import java.util.List;

import javax.swing.JLayeredPane;
import javax.swing.WindowConstants;

import forge.Singletons;
import forge.control.KeyboardShortcuts.Shortcut;

/**
 * <p>
 * FControl.
 * </p>
 * Controls all Forge UI functionality inside one JFrame. This class switches
 * between various display states in that JFrame. Controllers are instantiated
 * separately by each state's top level view class.
 */
public final class FControl {
    private List<Shortcut> shortcuts;
    private JLayeredPane display;
    private int state;

    private WindowListener waDefault, waConcede, waLeaveBazaar;

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
     */
    public FControl() {
        // "Close" button override during match
        this.waConcede = new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                Singletons.getView().setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                Singletons.getControl().getMatchControl().getDockControl().concede();
            }
        };

        // "Close" button override while inside bazaar (will probably be used later for other things)
        this.waLeaveBazaar = new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                Singletons.getView().setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                changeState(0);
                Singletons.getView().getHomeView().showQuestMenu();
            }
        };

        // Default action on window close
        this.waDefault = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                Singletons.getView().setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            }
         };
    }

    /** After view and model have been initialized, control can start. */
    public void initialize() {
        this.shortcuts = KeyboardShortcuts.attachKeyboardShortcuts();
        this.display = Singletons.getView().getLayeredContentPane();

        // Handles resizing in null layouts of layers in JLayeredPane.
        Singletons.getView().addComponentListener(new ComponentAdapter() {
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
        clearChildren(JLayeredPane.DEFAULT_LAYER);
        this.state = i0;

        /// out out out ghandi asdf
        Singletons.getView().removeWindowListener(waConcede);
        Singletons.getView().removeWindowListener(waLeaveBazaar);
        Singletons.getView().addWindowListener(waDefault);
        ////////////////

        // Fire up new state
        switch (i0) {
            case HOME_SCREEN:
                display.add(Singletons.getView().getHomeView(), JLayeredPane.DEFAULT_LAYER);
                sizeChildren();
                break;

            case MATCH_SCREEN:
                display.add(Singletons.getView().getMatchView(), JLayeredPane.DEFAULT_LAYER);
                sizeChildren();
                Singletons.getView().addWindowListener(waConcede);
                break;

            case DEFAULT_EDITOR:
                display.add(Singletons.getView().getEditorView(), JLayeredPane.DEFAULT_LAYER);
                break;

            case QUEST_BAZAAR:
                display.add(Singletons.getView().getBazaarView(), JLayeredPane.DEFAULT_LAYER);
                sizeChildren();
                Singletons.getView().addWindowListener(waLeaveBazaar);
                break;

            default:
        }
    }

    /** Gets the match controller.
     * @return {@link forge.control.match.ControlMatchUI}
     */
    public ControlMatchUI getMatchControl() {
        if (getState() != FControl.MATCH_SCREEN) {
            throw new IllegalArgumentException("FControl$getMatchControl\n"
                    + "may only be called while the match UI is showing.");
        }
        return Singletons.getView().getMatchView().getControl();
    }

    /** Gets the home controller.
     * @return {@link forge.control.home.ControlHomeUI} */
    public ControlHomeUI getHomeControl() {
        if (getState() != FControl.HOME_SCREEN) {
            throw new IllegalArgumentException("FControl$getHomeControl\n"
                    + "may only be called while the home UI is showing.");
        }
        return Singletons.getView().getHomeView().getControl();
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

    /** Remove all children from a specified layer. */
    private void clearChildren(final int layer0) {
        final Component[] children = Singletons.getView()
                .getLayeredContentPane().getComponentsInLayer(layer0);

        for (Component c : children) {
            display.remove(c);
        }
    }

    /** Sizes children of JLayeredPane to fully fit their layers. */
    private void sizeChildren() {
        Component[] children = display.getComponentsInLayer(JLayeredPane.DEFAULT_LAYER);
        if (children.length == 0) { return; }
        children[0].setSize(display.getSize());

        children = display.getComponentsInLayer(JLayeredPane.MODAL_LAYER);
        if (children.length == 0) { return; }
        children[0].setSize(display.getSize());
    }
}
