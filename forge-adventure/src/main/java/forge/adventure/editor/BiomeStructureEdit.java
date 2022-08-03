package forge.adventure.editor;

import forge.adventure.data.BiomeData;
import forge.adventure.data.BiomeStructureData;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.image.BufferedImage;

public class BiomeStructureEdit extends FormPanel {
    private boolean updating=false;
    BiomeStructureData currentData;
    BiomeData currentBiomeData;
    public JTextField structureAtlasPath=new JTextField();
    public FloatSpinner x= new FloatSpinner();
    public FloatSpinner y= new FloatSpinner();
    public FloatSpinner width= new FloatSpinner();
    public FloatSpinner height= new FloatSpinner();
    public JCheckBox randomPosition=new JCheckBox();
    public IntSpinner N= new IntSpinner();
    public JTextField sourcePath= new JTextField();
    public JCheckBox periodicInput= new JCheckBox();
    public IntSpinner ground= new IntSpinner();
    public IntSpinner symmetry= new IntSpinner();
    public JCheckBox periodicOutput= new JCheckBox();
    public BiomeStructureDataMappingEditor data=new BiomeStructureDataMappingEditor();
    public BiomeStructureEdit()
    {
        FormPanel center=new FormPanel();

        center.add("structureAtlasPath:",structureAtlasPath);
        center.add("x:",x);
        center.add("y:",y);
        center.add("width:",width);
        center.add("height:",height);
        center.add("N:",N);
        center.add("sourcePath:",sourcePath);
        center.add("periodicInput:",periodicInput);
        center.add("ground:",ground);
        center.add("symmetry:",symmetry);
        center.add("periodicOutput:",periodicOutput);

        add(center);
        add(data);

        structureAtlasPath.getDocument().addDocumentListener(new DocumentChangeListener(() -> BiomeStructureEdit.this.updateStructure()));


        x.addChangeListener(e -> BiomeStructureEdit.this.updateStructure());
        y.addChangeListener(e -> BiomeStructureEdit.this.updateStructure());
        width.addChangeListener(e -> BiomeStructureEdit.this.updateStructure());
        height.addChangeListener(e -> BiomeStructureEdit.this.updateStructure());
        randomPosition.addChangeListener(e -> BiomeStructureEdit.this.updateStructure());

        N.addChangeListener(e -> BiomeStructureEdit.this.updateStructure());
        sourcePath.getDocument().addDocumentListener(new DocumentChangeListener(() -> BiomeStructureEdit.this.updateStructure()));
        periodicInput.addChangeListener(e -> BiomeStructureEdit.this.updateStructure());
        ground.addChangeListener(e -> BiomeStructureEdit.this.updateStructure());
        symmetry.addChangeListener(e -> BiomeStructureEdit.this.updateStructure());
        periodicOutput.addChangeListener(e -> BiomeStructureEdit.this.updateStructure());
        refresh();
    }
    private void refresh() {
        setEnabled(currentData!=null);
        if(currentData==null)
        {
            data.setCurrent(null);
            return;
        }
        updating=true;
        structureAtlasPath.setText(currentData.structureAtlasPath);
        x.setValue(currentData.x);
        y.setValue(currentData.y);
        width.setValue(currentData.width);
        height.setValue(currentData.height);
        randomPosition.setSelected(currentData.randomPosition);
        N.setValue(currentData.N);
        sourcePath.setText(currentData.sourcePath);
        periodicInput.setSelected(currentData.periodicInput);
        ground.setValue(currentData.ground);
        symmetry.setValue(currentData.symmetry);
        periodicOutput.setSelected(currentData.periodicOutput);

        data.setCurrent(currentData);



        updating=false;
    }
    public void updateStructure()
    {

        if(currentData==null||updating)
            return;
        currentData.structureAtlasPath=structureAtlasPath.getText();

        currentData.x= x.floatValue();
        currentData.y= y.floatValue();
        currentData.width= width.floatValue();
        currentData.height= height.floatValue();
        currentData.randomPosition=randomPosition.isSelected();
        currentData.mappingInfo= data.getCurrent();

        currentData.N= N.intValue();
        currentData.sourcePath= sourcePath.getText();
        currentData.periodicInput= periodicInput.isSelected();
        currentData.ground= ground.intValue();
        currentData.symmetry= symmetry.intValue();
        currentData.periodicOutput= periodicOutput.isSelected();
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
