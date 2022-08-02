package forge.adventure.editor;

import forge.adventure.data.BiomeData;
import forge.adventure.data.BiomeStructureData;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.image.BufferedImage;

public class BiomeStructureEdit extends JComponent {
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
        JComponent center=new JComponent() {  };
        center.setLayout(new GridLayout(11,2));

        center.add(new JLabel("structureAtlasPath:")); center.add(structureAtlasPath);
        center.add(new JLabel("x:")); center.add(x);
        center.add(new JLabel("y:")); center.add(y);
        center.add(new JLabel("width:")); center.add(width);
        center.add(new JLabel("height:")); center.add(height);
        center.add(new JLabel("N:")); center.add(N);
        center.add(new JLabel("sourcePath:")); center.add(sourcePath);
        center.add(new JLabel("periodicInput:")); center.add(periodicInput);
        center.add(new JLabel("ground:")); center.add(ground);
        center.add(new JLabel("symmetry:")); center.add(symmetry);
        center.add(new JLabel("periodicOutput:")); center.add(periodicOutput);
        BorderLayout layout=new BorderLayout();
        setLayout(layout);
        add(center,BorderLayout.CENTER);
        add(data,BorderLayout.SOUTH);

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
