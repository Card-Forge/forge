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
        setVisible(true);
        setSize(800,600);
    }
}
