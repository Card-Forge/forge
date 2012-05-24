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
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.VDeckEditorUI;
import forge.gui.framework.SOverflowUtil;
import forge.gui.framework.SResizingUtil;
import forge.gui.home.VHomeUI;
import forge.gui.home.quest.SSubmenuQuestUtil;
import forge.gui.match.VMatchUI;
import forge.gui.match.controllers.CDock;
import forge.gui.toolbox.CardFaceSymbols;
import forge.gui.toolbox.FSkin;
import forge.view.FView;

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
    private int state = -1;

    private WindowListener waDefault, waConcede, waLeaveBazaar, waLeaveEditor;

    /** */
    public static final int HOME_SCREEN = 0;
    /** */
    public static final int MATCH_SCREEN = 1;
    /** */
    public static final int DECK_EDITOR_CONSTRUCTED = 2;
    /** */
    public static final int QUEST_BAZAAR = 3;
    /** */
    public static final int DECK_EDITOR_LIMITED = 4;
    /** */
    public static final int DECK_EDITOR_QUEST = 5;
    /** */
    public static final int QUEST_CARD_SHOP = 6;
    /** */
    public static final int DRAFTING_PROCESS = 7;

    /**
     * <p>
     * FControl.
     * </p>
     * Controls all Forge UI functionality inside one JFrame. This class
     * switches between various display states in that JFrame. Controllers are
     * instantiated separately by each state's top level view class.
     */
    private FControl() {
        this.waDefault = new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                Singletons.getView().getFrame().setDefaultCloseOperation(
                        WindowConstants.EXIT_ON_CLOSE);

                System.exit(0);
            }
        };

        // "Close" button override during match
        this.waConcede = new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                Singletons.getView().getFrame().setDefaultCloseOperation(
                        WindowConstants.DO_NOTHING_ON_CLOSE);

                CDock.SINGLETON_INSTANCE.concede();
            }
        };

        // "Close" button override while inside bazaar (will probably be used later for other things)
        this.waLeaveBazaar = new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                Singletons.getView().getFrame().setDefaultCloseOperation(
                        WindowConstants.DO_NOTHING_ON_CLOSE);

                changeState(FControl.HOME_SCREEN);
                SSubmenuQuestUtil.updateStatsAndPet();
            }
        };

         this.waLeaveEditor = new WindowAdapter() {
             @Override
             public void windowClosing(final WindowEvent ev) {
                 Singletons.getView().getFrame().setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

                 if (CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().exit()) {
                     changeState(FControl.HOME_SCREEN);
                 }
             }
         };

         FView.SINGLETON_INSTANCE.getLpnDocument().addMouseListener(SOverflowUtil.getHideOverflowListener());
         FView.SINGLETON_INSTANCE.getLpnDocument().addComponentListener(SResizingUtil.getWindowResizeListener());
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
        this.display = FView.SINGLETON_INSTANCE.getLpnDocument();

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

        Singletons.getView().getFrame().removeWindowListener(waDefault);
        Singletons.getView().getFrame().removeWindowListener(waConcede);
        Singletons.getView().getFrame().removeWindowListener(waLeaveBazaar);
        Singletons.getView().getFrame().removeWindowListener(waLeaveEditor);

        // Fire up new state
        switch (i0) {
            case HOME_SCREEN:
                VHomeUI.SINGLETON_INSTANCE.populate();
                FView.SINGLETON_INSTANCE.getPnlInsets().setVisible(true);
                Singletons.getView().getFrame().addWindowListener(waDefault);
                break;

            case MATCH_SCREEN:
                VMatchUI.SINGLETON_INSTANCE.populate();
                FView.SINGLETON_INSTANCE.getPnlInsets().setVisible(true);
                Singletons.getView().getFrame().addWindowListener(waConcede);
                break;

            case DECK_EDITOR_CONSTRUCTED:
            case DECK_EDITOR_LIMITED:
            case DECK_EDITOR_QUEST:
            case QUEST_CARD_SHOP:
            case DRAFTING_PROCESS:
                VDeckEditorUI.SINGLETON_INSTANCE.populate();
                FView.SINGLETON_INSTANCE.getPnlInsets().setVisible(true);
                Singletons.getView().getFrame().addWindowListener(waLeaveEditor);
                break;

            case QUEST_BAZAAR:
                display.add(Singletons.getView().getViewBazaar(), JLayeredPane.DEFAULT_LAYER);
                FView.SINGLETON_INSTANCE.getPnlInsets().setVisible(false);
                sizeChildren();
                Singletons.getView().getFrame().addWindowListener(waLeaveBazaar);
                break;

            default:
        }
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
        final Component[] children = FView.SINGLETON_INSTANCE.getLpnDocument()
                .getComponentsInLayer(layer0);

        for (final Component c : children) {
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
