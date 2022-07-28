package forge.adventure.editor;

import forge.adventure.data.BiomeData;
import forge.adventure.data.BiomeStructureData;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class BiomeStructureEdit extends JComponent {
    SwingAtlasPreview preview=new SwingAtlasPreview(128);
    private boolean updating=false;
    BiomeStructureData currentData;
    BiomeData currentBiomeData;
    public JTextField structureAtlasPath=new JTextField();
    public FloatSpinner x= new FloatSpinner();
    public FloatSpinner y= new FloatSpinner();
    public FloatSpinner size= new FloatSpinner();
    public JCheckBox randomPosition=new JCheckBox();
    public JCheckBox collision=new JCheckBox();

    public BiomeStructureEdit()
    {
        JComponent center=new JComponent() {  };
        center.setLayout(new GridLayout(6,2));

        center.add(new JLabel("structureAtlasPath:")); center.add(structureAtlasPath);
        center.add(new JLabel("x:")); center.add(x);
        center.add(new JLabel("y:")); center.add(y);
        center.add(new JLabel("size:")); center.add(size);
        center.add(new JLabel("randomPosition:")); center.add(randomPosition);
        center.add(new JLabel("collision:")); center.add(collision);
        BorderLayout layout=new BorderLayout();
        setLayout(layout);
        add(preview,BorderLayout.LINE_START);
        add(center,BorderLayout.CENTER);

        structureAtlasPath.getDocument().addDocumentListener(new DocumentChangeListener(() -> BiomeStructureEdit.this.updateStructure()));


        x.addChangeListener(e -> BiomeStructureEdit.this.updateStructure());
        y.addChangeListener(e -> BiomeStructureEdit.this.updateStructure());
        size.addChangeListener(e -> BiomeStructureEdit.this.updateStructure());
        randomPosition.addChangeListener(e -> BiomeStructureEdit.this.updateStructure());
        collision.addChangeListener(e -> BiomeStructureEdit.this.updateStructure());
        refresh();
    }
    private void refresh() {
        setEnabled(currentData!=null);
        if(currentData==null)
        {
            return;
        }
        updating=true;
        structureAtlasPath.setText(currentData.structureAtlasPath);
        x.setValue(currentData.x);
        y.setValue(currentData.y);
        size.setValue(currentData.size);
        randomPosition.setSelected(currentData.randomPosition);
        collision.setSelected(currentData.collision);
        preview.setSpritePath(currentBiomeData.tilesetAtlas,currentData.structureAtlasPath);
        updating=false;
    }
    public void updateStructure()
    {

        if(currentData==null||updating)
            return;
        currentData.structureAtlasPath=structureAtlasPath.getText();

        currentData.x= x.floatValue();
        currentData.y= y.floatValue();
        currentData.size= size.floatValue();
        currentData.randomPosition=randomPosition.isSelected();
        currentData.collision=collision.isSelected();
        preview.setSpritePath(currentBiomeData.tilesetAtlas,currentData.structureAtlasPath);
        emitChanged();
    }
    public void setCurrentStructure(BiomeStructureData biomeTerrainData, BiomeData data) {
        currentData =biomeTerrainData;
        currentBiomeData=data;
        refresh();
    }

    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
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
}
