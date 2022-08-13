package forge.adventure.editor;

import forge.adventure.data.BiomeData;
import forge.adventure.data.BiomeTerrainData;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class TerrainsEditor extends JComponent{
    DefaultListModel<BiomeTerrainData> model = new DefaultListModel<>();
    JList<BiomeTerrainData> list = new JList<>(model);
    JToolBar toolBar = new JToolBar("toolbar");
    BiomeTerrainEdit edit=new BiomeTerrainEdit();

    BiomeData currentData;

    public class TerrainDataRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(!(value instanceof BiomeTerrainData))
                return label;
            BiomeTerrainData terrainData=(BiomeTerrainData) value;
            StringBuilder builder=new StringBuilder();
            builder.append("Terrain");
            builder.append(" ");
            builder.append(terrainData.spriteName);
            label.setText(builder.toString());
            return label;
        }
    }
    public void addButton(String name, ActionListener action)
    {
        JButton newButton=new JButton(name);
        newButton.addActionListener(action);
        toolBar.add(newButton);

    }

    public TerrainsEditor()
    {

        list.setCellRenderer(new TerrainDataRenderer());
        list.addListSelectionListener(e -> TerrainsEditor.this.updateEdit());
        addButton("add", e -> TerrainsEditor.this.addTerrain());
        addButton("remove", e -> TerrainsEditor.this.remove());
        addButton("copy", e -> TerrainsEditor.this.copy());
        BorderLayout layout=new BorderLayout();
        setLayout(layout);
        add(list, BorderLayout.LINE_START);
        add(toolBar, BorderLayout.PAGE_START);
        add(edit,BorderLayout.CENTER);


        edit.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                emitChanged();
            }
        });
    }
    protected void emitChanged() {
        ChangeListener[] listeners = listenerList.getListeners(ChangeListener.class);
        if (listeners != null && listeners.length > 0) {
            ChangeEvent evt = new ChangeEvent(this);
            for (ChangeListener listener : listeners) {
                listener.stateChanged(evt);
            }
        }
    }
    private void copy() {

        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        BiomeTerrainData data=new BiomeTerrainData(model.get(selected));
        model.add(model.size(),data);
    }

    private void updateEdit() {

        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        edit.setCurrentTerrain(model.get(selected),currentData);
    }

    void addTerrain()
    {
        BiomeTerrainData data=new BiomeTerrainData();
        model.add(model.size(),data);
    }
    void remove()
    {
        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        model.remove(selected);
    }
    public void setTerrains(BiomeData data) {

        currentData=data;
        model.clear();
        if(data==null||data.terrain==null)
            return;
        for (int i=0;i<data.terrain.length;i++) {
            model.add(i,data.terrain[i]);
        }
        list.setSelectedIndex(0);
    }

    public BiomeTerrainData[] getBiomeTerrainData() {

        BiomeTerrainData[] rewards= new BiomeTerrainData[model.getSize()];
        for(int i=0;i<model.getSize();i++)
        {
            rewards[i]=model.get(i);
        }
        return rewards;
    }
    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }
}
