package forge.gui.menus;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import forge.gui.menubar.MenuUtil;
import forge.util.FileUtil;

public final class HelpMenu {
    private HelpMenu() { }    

    public static JMenu getMenu() {        
        JMenu menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        menu.add(getMenu_GettingStarted());
        menu.add(getMenu_Articles());
        menu.add(getMenu_Troubleshooting());
        menu.addSeparator();
        menu.add(getMenuItem_ReleaseNotes());
        menu.add(getMenuItem_License());        
        return menu;        
    }
    
    private static JMenu getMenu_Troubleshooting() {
        JMenu mnu = new JMenu("Troubleshooting");
        mnu.add(getMenuItem_UrlLink("How to Provide a Useful Bug Report", "http://www.slightlymagic.net/forum/viewtopic.php?f=26&t=9621"));
        mnu.addSeparator();        
        mnu.add(getMenuItem_ReadMeFile());
        return mnu;
    }
    
    private static JMenu getMenu_Articles() {
        JMenu mnu = new JMenu("Articles");
        mnu.add(getMenuItem_UrlLink("HOW-TO: Customize your Sealed Deck games with fantasy blocks", "http://www.slightlymagic.net/forum/viewtopic.php?f=26&t=8164"));
        mnu.add(getMenuItem_UrlLink("Quest Mode: Guide to Formats, Worlds, and everything", "http://www.slightlymagic.net/forum/viewtopic.php?f=26&t=9258"));
        return mnu;
    }
    
    private static JMenu getMenu_GettingStarted() {
        JMenu mnu = new JMenu("Getting Started");
        mnu.add(getMenuItem_UrlLink("Forge Wiki", "http://www.slightlymagic.net/wiki/Forge"));
        mnu.add(getMenuItem_UrlLink("What is Forge?", "http://www.slightlymagic.net/forum/viewtopic.php?f=26&t=468"));
        return mnu;        
    }
    
    private static JMenuItem getMenuItem_ReadMeFile() {
        JMenuItem menuItem = new JMenuItem("README.txt");
        menuItem.addActionListener(getOpenFileAction(getFile("README.txt")));
        return menuItem;
    }
    
    private static JMenuItem getMenuItem_License() {
        JMenuItem menuItem = new JMenuItem("Forge License");
        menuItem.addActionListener(getOpenFileAction(getFile("LICENSE.txt")));
        return menuItem;
    }

    private static JMenuItem getMenuItem_ReleaseNotes() {
        JMenuItem menuItem = new JMenuItem("Release Notes");
        menuItem.addActionListener(getOpenFileAction(getFile("CHANGES.txt")));
        return menuItem;
    }

    private static ActionListener getOpenFileAction(final File file) {
        return new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    openFile(file);
                } catch (IOException e1) {
                    // Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
                    e1.printStackTrace();
                }
            }
        };
    }
    
    protected static File getFile(String filename) {
        // !! Linux is case-sensitive so file name and extension need to match exactly !!
        File file = null;
        String filePath = FileUtil.pathCombine(System.getProperty("user.dir"), filename);                        
        if (FileUtil.doesFileExist(filePath)) {
            file = new File(filePath);   
        }
        return file;        
    }

    /**
     * @see http://stackoverflow.com/questions/6273221/open-a-text-file-in-the-default-text-editor-via-java
     */
    private static void openFile(File file) throws IOException {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            String cmd = "rundll32 url.dll,FileProtocolHandler " + file.getCanonicalPath();
            Runtime.getRuntime().exec(cmd);
          } 
          else {
            Desktop.getDesktop().open(file);
          }        
    }
    
    
    private static JMenuItem getMenuItem_UrlLink(String caption, String url) {
        JMenuItem menuItem = new JMenuItem(caption);
        menuItem.addActionListener(getLaunchUrlAction(url));
        return menuItem;
    }
        
    private static ActionListener getLaunchUrlAction(final String url) {
        return new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) { 
                MenuUtil.openUrlInBrowser(url);
            }
        };
    }
    
}
