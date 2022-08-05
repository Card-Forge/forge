package forge.adventure.editor;

import forge.adventure.data.BiomeData;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class BiomeEdit extends FormPanel {
    BiomeData currentData;

    public JSpinner startPointX= new JSpinner(new SpinnerNumberModel(0.0f, 0.f, 1f, 0.1f));
    public JSpinner startPointY= new JSpinner(new SpinnerNumberModel(0.0f, 0.f, 1f, 0.1f));
    public JSpinner noiseWeight= new JSpinner(new SpinnerNumberModel(0.0f, 0.f, 1f, 0.1f));
    public JSpinner distWeight= new JSpinner(new SpinnerNumberModel(0.0f, 0.f, 1f, 0.1f));
    public JTextField name=new JTextField();
    public FilePicker tilesetAtlas=new FilePicker(new String[]{"atlas"});
    public JTextField tilesetName=new JTextField();
    public JSpinner width= new JSpinner(new SpinnerNumberModel(0.0f, 0.f, 1f, 0.1f));
    public JSpinner height= new JSpinner(new SpinnerNumberModel(0.0f, 0.f, 1f, 0.1f));
    public JTextField color=new JTextField();
    public JCheckBox collision=new JCheckBox();
    public TextListEdit spriteNames =new TextListEdit();
    public TextListEdit enemies =new TextListEdit();
    public TextListEdit pointsOfInterest =new TextListEdit();

    public TerrainsEditor terrain =new TerrainsEditor();
    public StructureEditor structures =new StructureEditor();
    private boolean updating=false;

    public BiomeEdit()
    {

        FormPanel center=new FormPanel() {  };

        center.add("startPointX:",startPointX);
        center.add("startPointY:",startPointY);
        center.add("noiseWeight:",noiseWeight);
        center.add("distWeight:",distWeight);
        center.add("name:",name);
        center.add("tilesetAtlas:",tilesetAtlas);
        center.add("tilesetName:",tilesetName);
        center.add("width:",width);
        center.add("height:",height);
        center.add("spriteNames:",spriteNames);
        center.add("enemies:",enemies);
        center.add("pointsOfInterest:",pointsOfInterest);
        center.add("color:",color);
        center.add("collision:",collision);
        center.add("terrain/structures:",new JLabel(""));

        add(center);
        add(terrain);
        add(structures);

        name.getDocument().addDocumentListener(new DocumentChangeListener(() -> BiomeEdit.this.updateTerrain()));
        tilesetName.getDocument().addDocumentListener(new DocumentChangeListener(() -> BiomeEdit.this.updateTerrain()));
        color.getDocument().addDocumentListener(new DocumentChangeListener(() -> BiomeEdit.this.updateTerrain()));
        collision.addChangeListener(e -> BiomeEdit.this.updateTerrain());
        spriteNames.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(() -> BiomeEdit.this.updateTerrain()));
        enemies.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(() -> BiomeEdit.this.updateTerrain()));
        terrain.addChangeListener(e -> BiomeEdit.this.updateTerrain());


        startPointX.addChangeListener(e -> BiomeEdit.this.updateTerrain());
        startPointY.addChangeListener(e -> BiomeEdit.this.updateTerrain());
        noiseWeight.addChangeListener(e -> BiomeEdit.this.updateTerrain());
        distWeight.addChangeListener(e -> BiomeEdit.this.updateTerrain());
        tilesetAtlas.getEdit().getDocument().addDocumentListener(new DocumentChangeListener(() -> BiomeEdit.this.updateTerrain()));
        width.addChangeListener(e -> BiomeEdit.this.updateTerrain());
        height.addChangeListener(e -> BiomeEdit.this.updateTerrain());
        refresh();
    }

    protected void updateTerrain() {
        if(currentData==null||updating)
            return;
        currentData.startPointX    = (Float) startPointX.getValue();
        currentData.startPointY    = (Float) startPointY.getValue();
        currentData.noiseWeight    = (Float) noiseWeight.getValue();
        currentData.distWeight      = (Float)distWeight.getValue();
        currentData.name            = name.getText();
        currentData.tilesetAtlas    = tilesetAtlas.edit.getText();
        currentData.tilesetName    = tilesetName.getText();
        currentData.terrain    = terrain.getBiomeTerrainData();
        currentData.structures    = structures.getBiomeStructureData();
        currentData.width    = (Float) width.getValue();
        currentData.height    = (Float) height.getValue();
        currentData.color    = color.getText();
        currentData.collision    = collision.isSelected();
        currentData.spriteNames    = spriteNames.getList();
        currentData.enemies    = enemies.getList();
        currentData.pointsOfInterest    = pointsOfInterest.getList();
    }

    public void setCurrentBiome(BiomeData data)
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
        startPointX.setValue(currentData.startPointX);
        startPointY.setValue(currentData.startPointY);
        noiseWeight.setValue(currentData.noiseWeight);
        distWeight.setValue(currentData.distWeight);
        name.setText(currentData.name);
        tilesetAtlas.edit.setText( currentData.tilesetAtlas);
        tilesetName.setText(currentData.tilesetName);
        terrain.setTerrains(currentData);
        structures.setStructures(currentData);
        width.setValue(currentData.width);
        height.setValue(currentData.height);
        color.setText(currentData.color);
        spriteNames.setText(currentData.spriteNames);
        enemies.setText(currentData.enemies);
        collision.setSelected(currentData.collision);
        pointsOfInterest.setText(currentData.pointsOfInterest);
        updating=false;
    }
}
