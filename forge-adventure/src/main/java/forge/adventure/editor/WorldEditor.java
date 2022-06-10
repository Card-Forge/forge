package forge.adventure.editor;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import forge.adventure.data.BiomeData;
import forge.adventure.data.WorldData;
import forge.adventure.util.Config;
import forge.adventure.util.Paths;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class WorldEditor extends JComponent {

    WorldData  currentData;


    JSpinner width= new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
    JSpinner height= new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));
    JSpinner playerStartPosX= new JSpinner(new SpinnerNumberModel(0, 0, 1, .1));
    JSpinner playerStartPosY= new JSpinner(new SpinnerNumberModel(0, 0, 1, .1));
    JSpinner noiseZoomBiome= new JSpinner(new SpinnerNumberModel(0, 0, 1000f, 1f));
    JSpinner tileSize= new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));

    JTextField  biomesSprites       =   new JTextField();
    JSpinner maxRoadDistance        =   new JSpinner(new SpinnerNumberModel(0, 0, 100000f, 1f));
    TextListEdit biomesNames        =   new TextListEdit();

    DefaultListModel<BiomeData> model = new DefaultListModel<>();
    JList<BiomeData> list = new JList<>(model);
    BiomeEdit edit=new BiomeEdit();
    JTabbedPane tabs =new JTabbedPane();
    static HashMap<String,SwingAtlas> atlas=new HashMap<>();

    public class BiomeDataRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if(!(value instanceof BiomeData))
                return label;
            BiomeData biome=(BiomeData) value;
            // Get the renderer component from parent class

            label.setText(biome.name);
            if(!atlas.containsKey(biome.tilesetAtlas))
                atlas.put(biome.tilesetAtlas,new SwingAtlas(Config.instance().getFile(biome.tilesetAtlas)));

            SwingAtlas poiAtlas = atlas.get(biome.tilesetAtlas);

            if(poiAtlas.has(biome.tilesetName))
                label.setIcon(poiAtlas.get(biome.tilesetName));
            else
            {
                ImageIcon img=poiAtlas.getAny();
                if(img!=null)
                    label.setIcon(img);
            }
            return label;
        }
    }
    public WorldEditor() {
        list.setCellRenderer(new BiomeDataRenderer());
        BorderLayout layout = new BorderLayout();
        setLayout(layout);
        add(tabs);
        JPanel worldPanel=new JPanel();
        JPanel biomeData=new JPanel();
        tabs.addTab("BiomeData", biomeData);
        tabs.addTab("WorldData", worldPanel);


        JPanel worldData=new JPanel();
        worldData.setLayout(new GridLayout(9,2)) ;

        worldData.add(new JLabel("width:"));            worldData.add(width);
        worldData.add(new JLabel("height:"));           worldData.add(height);
        worldData.add(new JLabel("playerStartPosX:"));  worldData.add(playerStartPosX);
        worldData.add(new JLabel("playerStartPosY:"));  worldData.add(playerStartPosY);
        worldData.add(new JLabel("noiseZoomBiome:"));   worldData.add(noiseZoomBiome);
        worldData.add(new JLabel("tileSize:"));         worldData.add(tileSize);
        worldData.add(new JLabel("biomesSprites:"));    worldData.add(biomesSprites);
        worldData.add(new JLabel("maxRoadDistance:"));  worldData.add(maxRoadDistance);
        worldData.add(new JLabel("biomesNames:"));      worldData.add(biomesNames);


        worldPanel.setLayout(new BoxLayout(worldPanel,BoxLayout.Y_AXIS));
        worldPanel.add(worldData);
        worldPanel.add(new Box.Filler(new Dimension(0,0),new Dimension(0,Integer.MAX_VALUE),new Dimension(0,Integer.MAX_VALUE)));


        biomeData.setLayout(new GridLayout(1,2)) ;
        biomeData.add(list);    biomeData.add(edit);

        load();

        JToolBar toolBar = new JToolBar("toolbar");
        add(toolBar, BorderLayout.PAGE_START);
        JButton newButton=new JButton("save");
        newButton.addActionListener(e -> WorldEditor.this.save());
        toolBar.add(newButton);
        newButton=new JButton("load");
        newButton.addActionListener(e -> WorldEditor.this.load());
        toolBar.add(newButton);
    }

    void save()
    {
        Json json = new Json(JsonWriter.OutputType.json);
        FileHandle handle = Config.instance().getFile(Paths.WORLD);
        handle.writeString(json.prettyPrint(json.toJson(currentData,Array.class, WorldData.class)),false);

    }
    void load()
    {
        model.clear();
        Json json = new Json();
        FileHandle handle = Config.instance().getFile(Paths.WORLD);
        if (handle.exists())
        {
              currentData=json.fromJson(WorldData.class, WorldData.class, handle);
        }
        update();
    }

    private void update() {
        width.setValue(currentData.width);
        height.setValue(currentData.height);
        playerStartPosX.setValue(currentData.playerStartPosX);
        playerStartPosY.setValue(currentData.playerStartPosY);
        noiseZoomBiome.setValue(currentData.noiseZoomBiome);
        tileSize.setValue(currentData.tileSize);
        biomesSprites.setText(currentData.biomesSprites);
        maxRoadDistance.setValue(currentData.maxRoadDistance);
        biomesNames.setText(currentData.biomesNames);

        for(String path:currentData.biomesNames)
        {
            Json json = new Json();
            FileHandle handle = Config.instance().getFile(path);
            if (handle.exists())
            {
                BiomeData data=json.fromJson(BiomeData.class, BiomeData.class, handle);
                model.addElement(data);
            }
        }

    }
}
