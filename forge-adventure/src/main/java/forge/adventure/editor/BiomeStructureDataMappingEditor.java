package forge.adventure.editor;

import forge.adventure.data.BiomeStructureData;
import forge.adventure.util.Config;
import forge.adventure.world.BiomeStructure;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class BiomeStructureDataMappingEditor extends JComponent {
    DefaultListModel<BiomeStructureData.BiomeStructureDataMapping> model = new DefaultListModel<>();
    JList<BiomeStructureData.BiomeStructureDataMapping> list = new JList<>(model);
    JToolBar toolBar = new JToolBar("toolbar");
    BiomeStructureDataMappingEdit edit=new BiomeStructureDataMappingEdit();
    private BiomeStructureData data;

    public void setCurrent(BiomeStructureData data) {
        this.data=data;
        model.clear();
        for(int i=0;data.mappingInfo!=null&&i<data.mappingInfo.length;i++)
            model.addElement(data.mappingInfo[i]);
    }

    public BiomeStructureData.BiomeStructureDataMapping[] getCurrent()
    {
        BiomeStructureData.BiomeStructureDataMapping[] array=new BiomeStructureData.BiomeStructureDataMapping[model.size()];
        for(int i=0;i<array.length;i++)
            array[i]=model.get(i);
        return array;
    }

    public class BiomeStructureDataMappingRenderer extends DefaultListCellRenderer {
        private final BiomeStructureDataMappingEditor editor;

        public BiomeStructureDataMappingRenderer(BiomeStructureDataMappingEditor biomeStructureDataMappingEditor) {
            this.editor=biomeStructureDataMappingEditor;
        }

        @Override
        public Component getListCellRendererComponent(
                JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(!(value instanceof BiomeStructureData.BiomeStructureDataMapping))
                return label;
            BiomeStructureData.BiomeStructureDataMapping data=(BiomeStructureData.BiomeStructureDataMapping) value;
            // Get the renderer component from parent class

            label.setText(data.name);
            if(editor.data!=null)
            {
                SwingAtlas itemAtlas=new SwingAtlas(Config.instance().getFile(editor.data.structureAtlasPath));
                if(itemAtlas.has(data.name))
                    label.setIcon(itemAtlas.get(data.name));
                else
                {
                    ImageIcon img=itemAtlas.getAny();
                    if(img!=null)
                        label.setIcon(img);
                }
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
    public BiomeStructureDataMappingEditor()
    {

        list.setCellRenderer(new BiomeStructureDataMappingEditor.BiomeStructureDataMappingRenderer(this));
        list.addListSelectionListener(e -> BiomeStructureDataMappingEditor.this.updateEdit());
        addButton("add", e -> BiomeStructureDataMappingEditor.this.add());
        addButton("remove", e -> BiomeStructureDataMappingEditor.this.remove());
        addButton("copy", e -> BiomeStructureDataMappingEditor.this.copy());
        BorderLayout layout=new BorderLayout();
        setLayout(layout);
        add(new JScrollPane(list), BorderLayout.WEST);
        add(toolBar, BorderLayout.NORTH);
        add(edit,BorderLayout.CENTER);
    }
    private void copy() {

        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        BiomeStructureData.BiomeStructureDataMapping data=new BiomeStructureData.BiomeStructureDataMapping(model.get(selected));
        model.add(model.size(),data);
    }
    private void updateEdit() {

        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        edit.setCurrent(model.get(selected));
    }

    void add()
    {
        BiomeStructureData.BiomeStructureDataMapping data=new BiomeStructureData.BiomeStructureDataMapping();
        data.name="Structure "+model.getSize();
        model.add(model.size(),data);
    }
    void remove()
    {
        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        model.remove(selected);
    }

    private class BiomeStructureDataMappingEdit extends JComponent{
        BiomeStructureData.BiomeStructureDataMapping currentData;


        public JTextField name=new JTextField();
        public JTextField color=new JTextField();
        public JCheckBox collision=new JCheckBox();
        private boolean updating=false;

        public BiomeStructureDataMappingEdit()
        {

            setLayout(new GridLayout(3,2));

            add(new JLabel("name:"));       add(name);
            add(new JLabel("color:"));      add(color);
            add(new JLabel("collision:"));  add(collision);

            name.getDocument().addDocumentListener(new DocumentChangeListener(() -> BiomeStructureDataMappingEdit.this.update()));
            color.getDocument().addDocumentListener(new DocumentChangeListener(() -> BiomeStructureDataMappingEdit.this.update()));
            collision.addChangeListener(e -> BiomeStructureDataMappingEdit.this.update());
            refresh();
        }

        private void update() {
            if(currentData==null||updating)
                return;
            currentData.name        = name.getText();
            currentData.color       = color.getText();
            currentData.collision   = collision.isSelected();
        }

        public void setCurrent(BiomeStructureData.BiomeStructureDataMapping data)
        {
            currentData=data;
            refresh();
        }

        private void refresh() {
            setEnabled(currentData!=null);
            if(currentData==null)
            {
                return;
            }
            updating=true;
            name.setText(currentData.name);
            color.setText(currentData.color);
            collision.setSelected(currentData.collision);
            updating=false;
        }
    }
}
