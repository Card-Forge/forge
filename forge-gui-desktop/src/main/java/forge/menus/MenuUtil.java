package forge.menus;

import forge.Singletons;
import forge.assets.FSkinProp;
import forge.toolbox.FSkin;
import forge.toolbox.imaging.FImageUtil;

import javax.swing.*;

import java.awt.*;
import java.io.IOException;

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
}
