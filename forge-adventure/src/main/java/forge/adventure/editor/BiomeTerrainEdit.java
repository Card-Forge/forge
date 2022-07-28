package forge.adventure.editor;

import forge.adventure.data.BiomeData;
import forge.adventure.data.BiomeTerrainData;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class BiomeTerrainEdit extends JComponent {
    SwingAtlasPreview preview=new SwingAtlasPreview(128);
    private boolean updating=false;
    BiomeTerrainData currentData;
    BiomeData currentBiomeData;
    public JTextField spriteName=new JTextField();
    public JSpinner min= new JSpinner(new SpinnerNumberModel(0.0f, 0.f, 1f, 0.1f));
    public JSpinner max= new JSpinner(new SpinnerNumberModel(0.0f, 0.f, 1f, 0.1f));
    public JSpinner resolution= new JSpinner(new SpinnerNumberModel(0.0f, 0.f, 1f, 0.1f));

    public BiomeTerrainEdit()
    {
        JComponent center=new JComponent() {  };
        center.setLayout(new GridLayout(4,2));

        center.add(new JLabel("spriteName:")); center.add(spriteName);
        center.add(new JLabel("min:")); center.add(min);
        center.add(new JLabel("max:")); center.add(max);
        center.add(new JLabel("resolution:")); center.add(resolution);
        BorderLayout layout=new BorderLayout();
        setLayout(layout);
        add(preview,BorderLayout.LINE_START);
        add(center,BorderLayout.CENTER);

        spriteName.getDocument().addDocumentListener(new DocumentChangeListener(() -> BiomeTerrainEdit.this.updateTerrain()));

        min.addChangeListener(e -> BiomeTerrainEdit.this.updateTerrain());
        max.addChangeListener(e -> BiomeTerrainEdit.this.updateTerrain());
        resolution.addChangeListener(e -> BiomeTerrainEdit.this.updateTerrain());


        refresh();
    }
    private void refresh() {
        setEnabled(currentData!=null);
        if(currentData==null)
        {
            return;
        }
        updating=true;
        spriteName.setText(currentData.spriteName);
        min.setValue(currentData.min);
        max.setValue(currentData.max);
        resolution.setValue(currentData.resolution);
        preview.setSpritePath(currentBiomeData.tilesetAtlas,currentData.spriteName);
        updating=false;
    }
    public void updateTerrain()
    {

        if(currentData==null||updating)
            return;
        currentData.spriteName=spriteName.getText();
        currentData.min= (float) min.getValue();
        currentData.max= (float) max.getValue();
        currentData.resolution= (float) resolution.getValue();
        preview.setSpritePath(currentBiomeData.tilesetAtlas,currentData.spriteName);
        emitChanged();
    }
    public void setCurrentTerrain(BiomeTerrainData biomeTerrainData, BiomeData data) {
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
