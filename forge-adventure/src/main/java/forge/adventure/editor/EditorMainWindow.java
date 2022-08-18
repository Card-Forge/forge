package forge.adventure.editor;


import javax.swing.*;
import java.awt.*;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class EditorMainWindow extends JFrame {
    public final static WorldEditor worldEditor = new WorldEditor();
    JTabbedPane tabs =new JTabbedPane();

    public EditorMainWindow()
    {
        BorderLayout layout=new BorderLayout();
        setLayout(layout);
        add(tabs);
        tabs.addTab("World",worldEditor);
        tabs.addTab("POI",new PointOfInterestEditor());
        tabs.addTab("Items",new ItemsEditor());
        tabs.addTab("Enemies",new EnemyEditor());

        UIManager.LookAndFeelInfo[] var1 = UIManager.getInstalledLookAndFeels();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            UIManager.LookAndFeelInfo info = var1[var3];
            if ("Nimbus".equals(info.getName())) {
                try {
                    UIManager.setLookAndFeel(info.getClassName());
                } catch (Throwable var6) {
                }
                break;
            }
        }

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ParticleEditor();
            }
        });

        setVisible(true);
        setSize(800,600);
    }
}
