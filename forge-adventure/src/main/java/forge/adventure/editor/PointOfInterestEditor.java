package forge.adventure.editor;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import forge.adventure.data.PointOfInterestData;
import forge.adventure.util.Config;
import forge.adventure.util.Paths;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.HashMap;

public class PointOfInterestEditor extends JComponent {
    DefaultListModel<PointOfInterestData> model = new DefaultListModel<>();
    JList<PointOfInterestData> list = new JList<>(model);
    JToolBar toolBar = new JToolBar("toolbar");
    PointOfInterestEdit edit=new PointOfInterestEdit();
    static HashMap<String,SwingAtlas> atlas=new HashMap<>();



    public class PointOfInterestRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(!(value instanceof PointOfInterestData))
                return label;
            PointOfInterestData poi=(PointOfInterestData) value;
            // Get the renderer component from parent class

            label.setText(poi.name);
            if(!atlas.containsKey(poi.spriteAtlas))
                atlas.put(poi.spriteAtlas,new SwingAtlas(Config.instance().getFile(poi.spriteAtlas)));

            SwingAtlas poiAtlas = atlas.get(poi.spriteAtlas);

            if(poiAtlas.has(poi.sprite))
                label.setIcon(poiAtlas.get(poi.sprite));
            else
            {
                ImageIcon img=poiAtlas.getAny();
                if(img!=null)
                    label.setIcon(img);
            }
            return label;
        }
    }
    public void addButton(String name, ActionListener action)
    {
        JButton newButton=new JButton(name);
        newButton.addActionListener(action);
        toolBar.add(newButton);

    }
    public PointOfInterestEditor()
    {

        list.setCellRenderer(new PointOfInterestEditor.PointOfInterestRenderer());
        list.addListSelectionListener(e -> PointOfInterestEditor.this.updateEdit());
        addButton("add", e -> PointOfInterestEditor.this.addItem());
        addButton("remove", e -> PointOfInterestEditor.this.remove());
        addButton("copy", e -> PointOfInterestEditor.this.copy());
        addButton("load", e -> PointOfInterestEditor.this.load());
        addButton("save", e -> PointOfInterestEditor.this.save());
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
        PointOfInterestData data=new PointOfInterestData(model.get(selected));
        model.add(model.size(),data);
    }
    private void updateEdit() {

        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        edit.setCurrent(model.get(selected));
    }

    void save()
    {
        Array<PointOfInterestData> allEnemies=new Array<>();
        for(int i=0;i<model.getSize();i++)
            allEnemies.add(model.get(i));
        Json json = new Json(JsonWriter.OutputType.json);
        FileHandle handle = Config.instance().getFile(Paths.POINTS_OF_INTEREST);
        handle.writeString(json.prettyPrint(json.toJson(allEnemies,Array.class, PointOfInterestData.class)),false);

    }
    void load()
    {
        model.clear();
        Array<PointOfInterestData> allEnemies=new Array<>();
        Json json = new Json();
        FileHandle handle = Config.instance().getFile(Paths.POINTS_OF_INTEREST);
        if (handle.exists())
        {
            Array readEnemies=json.fromJson(Array.class, PointOfInterestData.class, handle);
            allEnemies = readEnemies;
        }
        for (int i=0;i<allEnemies.size;i++) {
            model.add(i,allEnemies.get(i));
        }
    }
    void addItem()
    {
        PointOfInterestData data=new PointOfInterestData();
        data.name="PoI "+model.getSize();
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
