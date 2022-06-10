package forge.adventure.editor;

import forge.adventure.data.BiomeData;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class BiomeEdit extends JComponent {
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
    public TextListEdit spriteNames =new TextListEdit();
    public TextListEdit enemies =new TextListEdit();
    public TextListEdit pointsOfInterest =new TextListEdit();

    public TerrainsEditor terrain =new TerrainsEditor();

    private boolean updating=false;

    public BiomeEdit()
    {

        JComponent center=new JComponent() {  };
        center.setLayout(new GridLayout(14,2));

        center.add(new JLabel("startPointX:")); center.add(startPointX);
        center.add(new JLabel("startPointY:")); center.add(startPointY);
        center.add(new JLabel("noiseWeight:")); center.add(noiseWeight);
        center.add(new JLabel("distWeight:")); center.add(distWeight);
        center.add(new JLabel("name:")); center.add(name);
        center.add(new JLabel("tilesetAtlas:")); center.add(tilesetAtlas);
        center.add(new JLabel("tilesetName:")); center.add(tilesetName);
        center.add(new JLabel("width:")); center.add(width);
        center.add(new JLabel("height:")); center.add(height);
        center.add(new JLabel("spriteNames:")); center.add(spriteNames);
        center.add(new JLabel("enemies:")); center.add(enemies);
        center.add(new JLabel("pointsOfInterest:")); center.add(pointsOfInterest);
        center.add(new JLabel("color:")); center.add(color);
        center.add(new JLabel("terrain:")); center.add(terrain);
        BorderLayout layout=new BorderLayout();
        setLayout(layout);
        add(center,BorderLayout.PAGE_START);
        add(terrain,BorderLayout.CENTER);

        name.getDocument().addDocumentListener(new DocumentChangeListener(() -> BiomeEdit.this.updateTerrain()));
        tilesetName.getDocument().addDocumentListener(new DocumentChangeListener(() -> BiomeEdit.this.updateTerrain()));
        color.getDocument().addDocumentListener(new DocumentChangeListener(() -> BiomeEdit.this.updateTerrain()));
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

    private void updateTerrain() {
        if(currentData==null||updating)
            return;
        currentData.startPointX    = (Float) startPointX.getValue();
        currentData.startPointY    = (Float) startPointY.getValue();
        currentData.noiseWeight    = (Float) noiseWeight.getValue();
        currentData.distWeight      = (Float)distWeight.getValue();
        currentData.name            = name.getText();
        currentData.tilesetAtlas    = tilesetAtlas.edit.getText();
        currentData.tilesetName    = tilesetName.getName();
        currentData.terrain    = terrain.getBiomeTerrainData();
        currentData.width    = (Float) width.getValue();
        currentData.height    = (Float) height.getValue();
        currentData.color    = color.getText();
        currentData.spriteNames    = spriteNames.getList();
        currentData.enemies    = Arrays.asList(enemies.getList());
        currentData.pointsOfInterest    = Arrays.asList(pointsOfInterest.getList());
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
        terrain.setTerrains(currentData.terrain);
        width.setValue(currentData.width);
        height.setValue(currentData.height);
        color.setText(currentData.color);
        spriteNames.setText(currentData.spriteNames);
        enemies.setText(currentData.enemies);
        color.setText(currentData.color);
        pointsOfInterest.setText(currentData.pointsOfInterest);
        updating=false;
    }
}
