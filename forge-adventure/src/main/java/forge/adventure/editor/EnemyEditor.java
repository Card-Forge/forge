package forge.adventure.editor;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import forge.adventure.data.EnemyData;
import forge.adventure.util.Config;
import forge.adventure.util.Paths;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class EnemyEditor extends JComponent {
    DefaultListModel<EnemyData> model = new DefaultListModel<>();
    JList<EnemyData> list = new JList<>(model);
    JToolBar toolBar = new JToolBar("toolbar");
    EnemyEdit edit=new EnemyEdit();



    public class EnemyDataRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(!(value instanceof EnemyData))
                return label;
            EnemyData enemy=(EnemyData) value;
            // Get the renderer component from parent class

            label.setText(enemy.name);
            SwingAtlas atlas=new SwingAtlas(Config.instance().getFile(enemy.sprite));
            if(atlas.has("Avatar"))
                label.setIcon(atlas.get("Avatar"));
            else
            {
                ImageIcon img=atlas.getAny();
                if(img!=null)
                    label.setIcon(img);
            }
            return label;
        }
    }
    public void addButton(String name,ActionListener action)
    {
        JButton newButton=new JButton(name);
        newButton.addActionListener(action);
        toolBar.add(newButton);

    }
    public EnemyEditor()
    {

        list.setCellRenderer(new EnemyDataRenderer());
        list.addListSelectionListener(e -> updateEdit());
        addButton("add",e->addEnemy());
        addButton("remove",e->remove());
        addButton("copy",e->copy());
        addButton("load",e->load());
        addButton("save",e->save());
        BorderLayout layout=new BorderLayout();
        setLayout(layout);
        add(new JScrollPane(list), BorderLayout.LINE_START);
        add(toolBar, BorderLayout.PAGE_START);
        add(edit,BorderLayout.CENTER);
        load();
    }
    private void copy() {

        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        EnemyData data=new EnemyData(model.get(selected));
        model.add(model.size(),data);
    }
    private void updateEdit() {

        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        edit.setCurrentEnemy(model.get(selected));
    }

    void save()
    {
        Array<EnemyData> allEnemies=new Array<>();
        for(int i=0;i<model.getSize();i++)
            allEnemies.add(model.get(i));
        Json json = new Json(JsonWriter.OutputType.json);
        FileHandle handle = Config.instance().getFile(Paths.ENEMIES);
        handle.writeString(json.prettyPrint(json.toJson(allEnemies,Array.class, EnemyData.class)),false);

    }
    void load()
    {
        model.clear();
        Array<EnemyData> allEnemies=new Array<>();
        Json json = new Json();
        FileHandle handle = Config.instance().getFile(Paths.ENEMIES);
        if (handle.exists())
        {
            Array readEnemies=json.fromJson(Array.class, EnemyData.class, handle);
            allEnemies = readEnemies;
        }
        for (int i=0;i<allEnemies.size;i++) {
            model.add(i,allEnemies.get(i));
        }
    }
    void addEnemy()
    {
        EnemyData data=new EnemyData();
        data.name="Enemy "+model.getSize();
        model.add(model.size(),data);
    }
    void remove()
    {
        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        model.remove(selected);
    }
}
