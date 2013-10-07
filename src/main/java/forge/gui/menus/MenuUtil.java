package forge.gui.menus;

import java.awt.Toolkit;
import java.io.IOException;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import forge.Singletons;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.SkinProp;
import forge.gui.toolbox.imaging.ImageUtil;

public final class MenuUtil {
    private MenuUtil() { }

    // Get appropriate OS standard accelerator key for menu shortcuts.
    private static final int DEFAULT_MenuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    
    public static void openUrlInBrowser(String url) { 
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (IOException e) {
            // Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
            e.printStackTrace();
        }
    }
        
    public static FSkin.SkinIcon getMenuIcon(SkinProp ico) {
        return ImageUtil.getMenuIcon(FSkin.getIcon(ico));       
    }
    
    public static KeyStroke getAcceleratorKey(int key) {
        return KeyStroke.getKeyStroke(key, DEFAULT_MenuShortcutKeyMask);
    }
            
    public static boolean getUserConfirmation(String prompt, String dialogTitle) {
        Object[] options = {"Yes", "No"};                    
        int reply = JOptionPane.showOptionDialog(
                null, 
                prompt, 
                dialogTitle, 
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[1]);
        return (reply == JOptionPane.YES_OPTION);                      
    }    

    public static void setMenuProvider(IMenuProvider provider) {
        Singletons.getControl().getForgeMenu().setProvider(provider);
    }
    public static void setMenuHint(final JMenuItem menu, final String hint) {
        menu.setToolTipText(hint);
    }
}
