package forge.adventure.editor;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import forge.adventure.data.ItemData;
import forge.adventure.util.Config;
import forge.adventure.util.Paths;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ItemsEditor extends JComponent {
    DefaultListModel<ItemData> model = new DefaultListModel<>();
    JList<ItemData> list = new JList<>(model);
    JToolBar toolBar = new JToolBar("toolbar");
    ItemEdit edit=new ItemEdit();
    static SwingAtlas itemAtlas;



    public class ItemDataRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(!(value instanceof ItemData))
                return label;
            ItemData Item=(ItemData) value;
            // Get the renderer component from parent class

            label.setText(Item.name);
            if(itemAtlas==null)
                itemAtlas=new SwingAtlas(Config.instance().getFile(Paths.ITEMS_ATLAS));
            
            if(itemAtlas.has(Item.iconName))
                label.setIcon(itemAtlas.get(Item.iconName));
            else
            {
                ImageIcon img=itemAtlas.getAny();
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
    public ItemsEditor()
    {

        list.setCellRenderer(new ItemsEditor.ItemDataRenderer());
        list.addListSelectionListener(e -> ItemsEditor.this.updateEdit());
        addButton("add", e -> ItemsEditor.this.addItem());
        addButton("remove", e -> ItemsEditor.this.remove());
        addButton("copy", e -> ItemsEditor.this.copy());
        addButton("load", e -> ItemsEditor.this.load());
        addButton("save", e -> ItemsEditor.this.save());
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
        ItemData data=new ItemData(model.get(selected));
        model.add(model.size(),data);
    }
    private void updateEdit() {

        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        edit.setCurrentItem(model.get(selected));
    }

    void save()
    {
        Array<ItemData> allEnemies=new Array<>();
        for(int i=0;i<model.getSize();i++)
            allEnemies.add(model.get(i));
        Json json = new Json(JsonWriter.OutputType.json);
        FileHandle handle = Config.instance().getFile(Paths.ITEMS);
        handle.writeString(json.prettyPrint(json.toJson(allEnemies,Array.class, ItemData.class)),false);

    }
    void load()
    {
        model.clear();
        Array<ItemData> allEnemies=new Array<>();
        Json json = new Json();
        FileHandle handle = Config.instance().getFile(Paths.ITEMS);
        if (handle.exists())
        {
            Array readEnemies=json.fromJson(Array.class, ItemData.class, handle);
            allEnemies = readEnemies;
        }
        for (int i=0;i<allEnemies.size;i++) {
            model.add(i,allEnemies.get(i));
        }
    }
    void addItem()
    {
        ItemData data=new ItemData();
        data.name="Item "+model.getSize();
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
