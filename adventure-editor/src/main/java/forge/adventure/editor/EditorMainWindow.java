package forge.adventure.editor;

import forge.adventure.util.Config;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class EditorMainWindow extends JFrame {
    public final static WorldEditor worldEditor = new WorldEditor();
    JTabbedPane tabs = new JTabbedPane();

    public EditorMainWindow(Config config) {
        UIManager.LookAndFeelInfo[] var1 = UIManager.getInstalledLookAndFeels();

        for (UIManager.LookAndFeelInfo info : var1) {
            if ("Nimbus".equals(info.getName())) {
                try {
                    UIManager.setLookAndFeel(info.getClassName());
                } catch (Throwable ignored) {
                }
                break;
            }
        }
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
                System.exit(0);
            }
        });
        BorderLayout layout = new BorderLayout();
        JToolBar toolBar = new JToolBar("toolbar");
        JButton newButton = new JButton("GDX Particle Editor Tool");
        newButton.addActionListener(e -> EventQueue.invokeLater(() -> {
            newButton.setEnabled(false);
            try {
                CodeSource codeSource = EditorMainWindow.class.getProtectionDomain().getCodeSource();
                File jarFile = new File(codeSource.getLocation().toURI().getPath());
                String jarDir = jarFile.getParentFile().getPath();
                Desktop.getDesktop().open(new File(jarDir + "/gdx-particle-editor.jar"));
            } catch (Exception ex) {
                new ErrorDialog("Error", ex.getMessage());
                newButton.setEnabled(true);
            }
        }));
        JButton quit = new JButton("Quit");
        quit.addActionListener(e -> System.exit(0));
        toolBar.add(newButton);
        toolBar.add(quit);
        setLayout(layout);
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        tabs.addTab("World", worldEditor);
        tabs.addTab("POI", new PointOfInterestEditor());
        tabs.addTab("Items", new ItemsEditor());
        tabs.addTab("Enemies", new EnemyEditor());
        tabs.addTab("Quests", new QuestEditor());
        setSize(config.getSettingData().width, config.getSettingData().height);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    static class ErrorDialog {
        public ErrorDialog(String title, String message) {
            List<Object> options = new ArrayList<>();
            JButton ok = new JButton("OK");
            options.add(ok);
            JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options.toArray());
            JDialog dlg = pane.createDialog(JOptionPane.getRootFrame(), title);
            ok.addActionListener(e -> {
                dlg.setVisible(false);
                System.exit(0);
            });
            dlg.setResizable(false);
            dlg.setVisible(true);
        }
    }
}
