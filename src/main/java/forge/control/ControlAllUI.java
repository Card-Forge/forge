package forge.control;

import java.awt.Component;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JLayeredPane;

import forge.view.GuiTopLevel;
import forge.view.editor.EditorTopLevel;
import forge.view.home.HomeTopLevel;
import forge.view.match.ViewTopLevel;

/**
 * <p>ControlAllUI.</p>
 * Controls all Forge UI functionality inside one JFrame.
 * This class switches between various display states in that JFrame.
 * Controllers are instantiated separately by each state's top level view class.
 */
public class ControlAllUI {
    private JLayeredPane display;
    private GuiTopLevel view;
    private HomeTopLevel home = null;
    private ViewTopLevel match = null;
    private EditorTopLevel editor = null;

    /** 
     * <p>ControlAllUI.</p>
     * Controls all Forge UI functionality inside one JFrame.
     * This class switches between various display states in that JFrame.
     * Controllers are instantiated separately by each state's top level view class.
     *
     */
    public ControlAllUI() {
        view = new GuiTopLevel();

        view.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                Component[] children;
                children = display.getComponentsInLayer(JLayeredPane.DEFAULT_LAYER);
                children[0].setSize(display.getSize());

                children = display.getComponentsInLayer(JLayeredPane.MODAL_LAYER);
                children[0].setSize(display.getSize());
            }
        });

        display = (JLayeredPane) view.getContentPane();

        changeState(1);
    }

    /** 
     * <p>changeState.</p>
     * Switches between display states in top level JFrame.
     *
     * @param i &emsp; State index: 0 for home, 1 for match, etc.
     */
    public void changeState(int i) {
        home = null;
        match = null;
        editor = null;

        display.removeAll();
        view.addOverlay();

        // Fire up new state
        switch (i) {
            case 0: // Home screen
                home = new HomeTopLevel();
                display.add(home);
                break;

            case 1: // Match screen
                match = new ViewTopLevel();
                display.add(match, JLayeredPane.DEFAULT_LAYER);
                break;

            case 2: // Deck editor screen
                editor = new EditorTopLevel();
                display.add(editor);
                break;

            default: break;
        }
    }

    /** @return ViewTopLevel */
    public ViewTopLevel getMatchView() {
        return match;
    }

    /** @return ControlMatchUI */
    public ControlMatchUI getMatchController() {
        return match.getController();
    }
}
