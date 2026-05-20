package forge.menus;

import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.function.Consumer;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

import forge.Singletons;
import forge.gui.framework.FScreen;
import forge.gui.framework.IVTopLevelUI;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.screens.match.VMatchUI;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedCheckBoxMenuItem;
import forge.toolbox.FSkin.SkinnedRadioButtonMenuItem;
import forge.toolbox.imaging.FImageUtil;

public final class MenuUtil {
    private MenuUtil() { }

    // Get appropriate OS standard accelerator key for menu shortcuts.
    private static final int DEFAULT_MenuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    public static void openUrlInBrowser(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        }
        catch (IOException e) {
            // Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
            e.printStackTrace();
        }
    }

    public static FSkin.SkinIcon getMenuIcon(FSkinProp ico) {
        return FImageUtil.getMenuIcon(FSkin.getIcon(ico));
    }

    public static KeyStroke getAcceleratorKey(int key) {
        return KeyStroke.getKeyStroke(key, DEFAULT_MenuShortcutKeyMask);
    }

    public static void setMenuProvider(IMenuProvider provider) {
        Singletons.getControl().getForgeMenu().setProvider(provider);
    }

    public static void setMenuHint(final JMenuItem menu, final String hint) {
        menu.setToolTipText(hint);
    }

    /** Adds a stay-open checkbox that toggles the given preference. Returns the item for further customization. */
    public static JCheckBoxMenuItem addPrefCheckBox(final JMenu menu, final String label, final FPref pref) {
        final ForgePreferences prefs = FModel.getPreferences();
        final JCheckBoxMenuItem item = createStayOpenCheckBox(label);
        item.setState(prefs.getPrefBoolean(pref));
        item.addActionListener(e -> {
            prefs.setPref(pref, !prefs.getPrefBoolean(pref));
            prefs.save();
        });
        menu.add(item);
        return item;
    }

    /** Creates a JCheckBoxMenuItem that stays open on click. */
    public static JCheckBoxMenuItem createStayOpenCheckBox(final String text) {
        return new JCheckBoxMenuItem(text) {
            @Override protected void processMouseEvent(final MouseEvent e) {
                handleStayOpen(this, e, super::processMouseEvent);
            }
        };
    }

    /** Creates a JRadioButtonMenuItem that stays open on click. */
    public static JRadioButtonMenuItem createStayOpenRadioButton(final String text) {
        return new JRadioButtonMenuItem(text) {
            @Override protected void processMouseEvent(final MouseEvent e) {
                handleStayOpen(this, e, super::processMouseEvent);
            }
        };
    }

    /** Creates a SkinnedCheckBoxMenuItem that stays open on click. */
    public static SkinnedCheckBoxMenuItem createStayOpenSkinnedCheckBox(final String text) {
        return new SkinnedCheckBoxMenuItem(text) {
            @Override protected void processMouseEvent(final MouseEvent e) {
                handleStayOpen(this, e, super::processMouseEvent);
            }
        };
    }

    /** Creates a SkinnedRadioButtonMenuItem that stays open on click. */
    public static SkinnedRadioButtonMenuItem createStayOpenSkinnedRadioButton(final String text) {
        return new SkinnedRadioButtonMenuItem(text) {
            @Override protected void processMouseEvent(final MouseEvent e) {
                handleStayOpen(this, e, super::processMouseEvent);
            }
        };
    }

    private static void handleStayOpen(final JMenuItem item, final MouseEvent e,
            final Consumer<MouseEvent> superHandler) {
        if (e.getID() == MouseEvent.MOUSE_RELEASED && item.contains(e.getPoint())) {
            item.doClick(0);
            item.setArmed(true);
        } else {
            superHandler.accept(e);
        }
    }

    /** Runs the action with the active VMatchUI if the current screen is a match screen. */
    public static void withMatchUI(final Consumer<VMatchUI> action) {
        final FScreen screen = Singletons.getControl().getCurrentScreen();
        if (screen != null && screen.isMatchScreen()) {
            final IVTopLevelUI view = screen.getView();
            if (view instanceof VMatchUI vmu) {
                action.accept(vmu);
            }
        }
    }
}
