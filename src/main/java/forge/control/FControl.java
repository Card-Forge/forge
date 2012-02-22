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

import forge.AllZone;
import forge.Singletons;
import forge.control.KeyboardShortcuts.Shortcut;
import forge.view.toolbox.CardFaceSymbols;
import forge.view.toolbox.FSkin;

/**
 * <p>
 * FControl.
 * </p>
 * Controls all Forge UI functionality inside one JFrame. This class switches
 * between various display states in that JFrame. Controllers are instantiated
 * separately by each state's top level view class.
 */
public enum FControl {
    /** */
    SINGLETON_INSTANCE;

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
    private FControl() {
        // "Close" button override during match
        this.waConcede = new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                Singletons.getView().getFrame().setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                Singletons.getControl().getControlMatch().getDockControl().concede();
            }
        };

        // "Close" button override while inside bazaar (will probably be used later for other things)
        this.waLeaveBazaar = new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                Singletons.getView().getFrame().setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                changeState(0);
                Singletons.getControl().getControlHome().getControlQuest().refreshStats();
                Singletons.getView().getViewHome().showQuestMenu();
            }
        };

        // Default action on window close
        this.waDefault = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                Singletons.getView().getFrame().setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            }
         };
    }

    /** After view and model have been initialized, control can start. */
    public void initialize() {
        // Preloads all cards (using progress bar).
        AllZone.getCardFactory();

        // Preloads skin components (using progress bar).
        FSkin.loadFull();

        // Does not use progress bar, due to be deprecated with battlefield refactoring.
        CardFaceSymbols.loadImages();

        this.shortcuts = KeyboardShortcuts.attachKeyboardShortcuts();
        this.display = Singletons.getView().getLayeredContentPane();
        Singletons.getModel().getQuestEventManager().assembleAllEvents();

        //Singletons.getView().initialize();

        // Handles resizing in null layouts of layers in JLayeredPane.
        Singletons.getView().getFrame().addComponentListener(new ComponentAdapter() {
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

        /// TODO should these be here?
        Singletons.getView().getFrame().removeWindowListener(waConcede);
        Singletons.getView().getFrame().removeWindowListener(waLeaveBazaar);
        Singletons.getView().getFrame().addWindowListener(waDefault);

        // Fire up new state
        switch (i0) {
            case HOME_SCREEN:
                display.add(Singletons.getView().getViewHome(), JLayeredPane.DEFAULT_LAYER);
                sizeChildren();
                break;

            case MATCH_SCREEN:
                display.add(Singletons.getView().getViewMatch(), JLayeredPane.DEFAULT_LAYER);
                sizeChildren();
                Singletons.getView().getFrame().addWindowListener(waConcede);
                break;

            case DEFAULT_EDITOR:
                display.add(Singletons.getView().getViewEditor(), JLayeredPane.DEFAULT_LAYER);
                break;

            case QUEST_BAZAAR:
                display.add(Singletons.getView().getViewBazaar(), JLayeredPane.DEFAULT_LAYER);
                sizeChildren();
                Singletons.getView().getFrame().addWindowListener(waLeaveBazaar);
                break;

            default:
        }
    }

    /** Gets the match controller.
     * @return {@link forge.control.match.ControlMatchUI}
     */
    public ControlMatchUI getControlMatch() {
        if (getState() != FControl.MATCH_SCREEN) {
            throw new IllegalArgumentException("FControl$getControlMatch\n"
                    + "may only be called while the match UI is showing.");
        }
        return Singletons.getView().getViewMatch().getControl();
    }

    /** Gets the home controller.
     * @return {@link forge.control.home.ControlHomeUI} */
    public ControlHomeUI getControlHome() {
        if (getState() != FControl.HOME_SCREEN) {
            throw new IllegalArgumentException("FControl$getControlHome\n"
                    + "may only be called while the home UI is showing.");
        }

        return Singletons.getView().getViewHome().getControl();
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

    /**
     * Exhaustively converts object array to string array.
     * Probably a much easier way to do this.
     * And, there must be a better place for this.
     * 
     * @param o0 &emsp; Object[]
     * @return String[]
     */
    public static String[] oa2sa(final Object[] o0) {
        final String[] output = new String[o0.length];

        for (int i = 0; i < o0.length; i++) {
            output[i] = o0[i].toString();
        }

        return output;
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
