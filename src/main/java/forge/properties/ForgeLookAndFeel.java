package forge.properties;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;

import forge.Singletons;
import forge.gui.toolbox.FSkin;
import forge.properties.ForgePreferences.FPref;

/** 
 * Sets the look and feel of the GUI based on the selected Forge theme.
 *
 * @see <a href="http://tips4java.wordpress.com/2008/10/09/uimanager-defaults/">UIManager Defaults</a>
 */
public final class ForgeLookAndFeel {
    
    private Color FORE_COLOR = FSkin.getColor(FSkin.Colors.CLR_TEXT);
    private Color BACK_COLOR = FSkin.getColor(FSkin.Colors.CLR_THEME2);
    private Color HIGHLIGHT_COLOR = BACK_COLOR.brighter();
    private Border LINE_BORDER = BorderFactory.createLineBorder(FORE_COLOR.darker(), 1);
    private Border EMPTY_BORDER = BorderFactory.createEmptyBorder(2,2,2,2);    
    
    /**
     * Sets the look and feel of the GUI based on the selected Forge theme.
     */
    public void setForgeLookAndFeel(JFrame appFrame) {
        if (isUIManagerEnabled()) {
            if (setMetalLookAndFeel(appFrame)) {
                setMenusLookAndFeel();
                setComboBoxLookAndFeel();
                setTabbedPaneLookAndFeel();
                setButtonLookAndFeel();
            }
        }
    }
    
    /**
     * Sets the standard "Java L&F" (also called "Metal") that looks the same on all platforms.
     * <p>
     * If not explicitly set then the Mac uses its native L&F which does
     * not support various settings (eg. combobox background color). 
     */
    private boolean setMetalLookAndFeel(JFrame appFrame) {
        boolean isMetalLafSet = false;
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            SwingUtilities.updateComponentTreeUI(appFrame);            
            isMetalLafSet = true;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            // Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
            e.printStackTrace();
        }
        return isMetalLafSet;
    }
    
    /**
     * Sets the look and feel for a JMenuBar, JMenu, JMenuItem & variations.
     */
    private void setMenusLookAndFeel() {
        // JMenuBar
        Color clrTheme = FSkin.getColor(FSkin.Colors.CLR_THEME);
        Color backgroundColor = FSkin.stepColor(clrTheme, 0);
        Color menuBarEdgeColor = FSkin.stepColor(clrTheme, -80);
        UIManager.put("MenuBar.foreground", FORE_COLOR);
        UIManager.put("MenuBar.gradient", getColorGradients(backgroundColor.darker(), backgroundColor));
        UIManager.put("MenuBar.border", BorderFactory.createMatteBorder(0, 0, 1, 0, menuBarEdgeColor));        
        // JMenu
        UIManager.put("Menu.foreground", FORE_COLOR);
        UIManager.put("Menu.background", BACK_COLOR);
        UIManager.put("Menu.borderPainted", false);                
        UIManager.put("Menu.selectionBackground", HIGHLIGHT_COLOR);
        UIManager.put("Menu.selectionForeground", FORE_COLOR);
        UIManager.put("Menu.border", EMPTY_BORDER);        
        UIManager.put("Menu.opaque", false);
        // JPopupMenu
        UIManager.put("PopupMenu.border", LINE_BORDER);
        UIManager.put("PopupMenu.background", BACK_COLOR);
        UIManager.put("PopupMenu.foreground", FORE_COLOR);        
        // JMenuItem
        UIManager.put("MenuItem.foreground", FORE_COLOR);
        UIManager.put("MenuItem.background", BACK_COLOR);
        UIManager.put("MenuItem.border", EMPTY_BORDER);        
        UIManager.put("MenuItem.selectionBackground", HIGHLIGHT_COLOR);
        UIManager.put("MenuItem.selectionForeground", FORE_COLOR);
        UIManager.put("MenuItem.acceleratorForeground", FORE_COLOR.darker());
        UIManager.put("MenuItem.opaque", true);
        // JSeparator (needs to be opaque!).
        UIManager.put("Separator.foreground", FORE_COLOR.darker());
        UIManager.put("Separator.background", BACK_COLOR);        
        // JRadioButtonMenuItem
        UIManager.put("RadioButtonMenuItem.foreground", FORE_COLOR);
        UIManager.put("RadioButtonMenuItem.background", BACK_COLOR);
        UIManager.put("RadioButtonMenuItem.selectionBackground", HIGHLIGHT_COLOR);
        UIManager.put("RadioButtonMenuItem.selectionForeground", FORE_COLOR);
        UIManager.put("RadioButtonMenuItem.border", EMPTY_BORDER);
        UIManager.put("RadioButtonMenuItem.acceleratorForeground", FORE_COLOR.darker());
        // JCheckboxMenuItem
        UIManager.put("CheckBoxMenuItem.foreground", FORE_COLOR);
        UIManager.put("CheckBoxMenuItem.background", BACK_COLOR);
        UIManager.put("CheckBoxMenuItem.selectionBackground", HIGHLIGHT_COLOR);
        UIManager.put("CheckBoxMenuItem.selectionForeground", FORE_COLOR);
        UIManager.put("CheckBoxMenuItem.border", EMPTY_BORDER);
        UIManager.put("CheckBoxMenuItem.acceleratorForeground", FORE_COLOR.darker());        
    }    
        
    private void setTabbedPaneLookAndFeel() {
        UIManager.put("TabbedPane.selected", HIGHLIGHT_COLOR);
        UIManager.put("TabbedPane.contentOpaque", FSkin.getColor(FSkin.Colors.CLR_THEME));
        UIManager.put("TabbedPane.unselectedBackground", BACK_COLOR);        
    }

    /**
     * Sets the look and feel for a <b>non-editable</b> JComboBox.
     */
    private void setComboBoxLookAndFeel() {        
        UIManager.put("ComboBox.background", BACK_COLOR);
        UIManager.put("ComboBox.foreground", FORE_COLOR);
        UIManager.put("ComboBox.selectionBackground", HIGHLIGHT_COLOR);
        UIManager.put("ComboBox.selectionForeground", FORE_COLOR);
        UIManager.put("ComboBox.disabledBackground", BACK_COLOR);
        UIManager.put("ComboBox.disabledForeground", BACK_COLOR.darker());                                                
        UIManager.put("ComboBox.font", getDefaultFont("ComboBox.font"));
        UIManager.put("Button.select", HIGHLIGHT_COLOR);
    }
    
    private void setButtonLookAndFeel() {
        UIManager.put("Button.foreground", FORE_COLOR);
        UIManager.put("Button.background", BACK_COLOR);        
        UIManager.put("Button.select", HIGHLIGHT_COLOR);
        UIManager.put("Button.focus", FORE_COLOR.darker());
        UIManager.put("Button.rollover", false);
    }    
    
    /**
     * Determines whether theme styles should be applied to GUI.
     * <p>
     * TODO: Currently is using UI_THEMED_COMBOBOX setting but will
     *       eventually want to rename for clarity.
     */
    private boolean isUIManagerEnabled() {
        return Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_THEMED_COMBOBOX);
    }
    
    private Font getDefaultFont(String component) {
        return FSkin.getFont(UIManager.getFont(component).getSize());
    }
    
    private ArrayList<Object> getColorGradients(Color bottom, Color top) {
      ArrayList<Object> gradients = new ArrayList<>();
      gradients.add(0.0);
      gradients.add(0.0);
      gradients.add(top);
      gradients.add(bottom);        
      gradients.add(bottom);
      return gradients;
    }
    
}
