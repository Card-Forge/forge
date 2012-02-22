package forge.view.home;

import javax.swing.JPanel;

import forge.control.home.IControlSubmenu;
import forge.model.home.MenuGroup;

/** Dictates methods required for a submenu view. */
public interface IViewSubmenu {
    /** @return {@link forge.control.home.IControlSubmenu} */
    IControlSubmenu getControl();

    /** Allows static factory creation by decoupling UI components.
     * @return {@link javax.swing.JPanel} */
    JPanel getPanel();

    /** Returns parent menu grouping of this submenu, useful for
     * functions such as expanding and collapsing in the menu area.
     * 
     * @return {@link javax.swing.JPanel} */
    MenuGroup getGroup();
}
