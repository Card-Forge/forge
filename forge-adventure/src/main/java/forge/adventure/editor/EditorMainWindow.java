package forge.adventure.editor;

import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.Lang;
import forge.util.Localizer;

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
        UIManager.LookAndFeelInfo[] var1 = UIManager.getInstalledLookAndFeels();
        FModel.initialize(null, preferences -> {
            preferences.setPref(ForgePreferences.FPref.LOAD_CARD_SCRIPTS_LAZILY, true);
            return null;
        });
        Lang.createInstance(FModel.getPreferences().getPref(ForgePreferences.FPref.UI_LANGUAGE));
        Localizer.getInstance().initialize(FModel.getPreferences().getPref(ForgePreferences.FPref.UI_LANGUAGE), ForgeConstants.LANG_DIR);
        int var2 = var1.length;

        for (UIManager.LookAndFeelInfo info : var1) {
            if ("Nimbus".equals(info.getName())) {
                try {
                    UIManager.setLookAndFeel(info.getClassName());
                } catch (Throwable var6) {
                }
                break;
            }
        }
        BorderLayout layout=new BorderLayout();
        JToolBar toolBar = new JToolBar("toolbar");
        // refer to removal of Swing Particle Editor: https://github.com/libgdx/libgdx/issues/7285
        //todo add New Particle Editor here (needs Java 11+): https://github.com/libgdx/gdx-particle-editor
        //JButton newButton=new JButton("open ParticleEditor");
        //newButton.addActionListener(e -> EventQueue.invokeLater(ParticleEditor::new));
        JButton quit = new JButton("Quit");
        quit.addActionListener(e-> System.exit(0));
        //toolBar.add(newButton);
        toolBar.add(quit);
        setLayout(layout);
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        tabs.addTab("World",worldEditor);
        tabs.addTab("POI",new PointOfInterestEditor());
        tabs.addTab("Items",new ItemsEditor());
        tabs.addTab("Enemies",new EnemyEditor());
        tabs.addTab("Quests",new QuestEditor());

        setVisible(true);
        setSize(800,600);
        GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow( this );

    }
}
