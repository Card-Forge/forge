package forge.gui.home;

import javax.swing.JPanel;

/** Dictates methods required for a submenu view. */
public interface IVSubmenu {
    /** Allows static factory creation by decoupling UI components.
     * @return {@link javax.swing.JPanel} */
    JPanel getPanel();

    /** Returns parent menu grouping of this submenu, useful for
     * functions such as expanding and collapsing in the menu area.
     * 
     * @return {@link javax.swing.JPanel} */
    EMenuGroup getGroup();

    /** Removes all children and (re)populates panel components, independent of constructor.*/
    void populate();
}
