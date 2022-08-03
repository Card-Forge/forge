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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class WorldEditor extends JComponent {

    WorldData  currentData;


    IntSpinner width= new IntSpinner( 0, 100000, 1);
    IntSpinner height= new IntSpinner( 0, 100000, 1);
    FloatSpinner playerStartPosX= new FloatSpinner( 0, 1, .1f);
    FloatSpinner playerStartPosY= new FloatSpinner(0, 1, .1f);
    FloatSpinner noiseZoomBiome= new FloatSpinner( 0, 1000f, 1f);
    IntSpinner tileSize= new IntSpinner( 0, 100000, 1);

    JTextField  biomesSprites       =   new JTextField();
    FloatSpinner maxRoadDistance        =   new FloatSpinner( 0, 100000f, 1f);
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

    /**
     *
     */
    private void updateBiome() {

        int selected=list.getSelectedIndex();
        if(selected<0)
            return;
        edit.setCurrentBiome(model.get(selected));
    }

    public WorldEditor() {
        list.setCellRenderer(new BiomeDataRenderer());
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                WorldEditor.this.updateBiome();
            }
        });
        BorderLayout layout = new BorderLayout();
        setLayout(layout);
        add(tabs);
        JSplitPane biomeData=new JSplitPane();
        tabs.addTab("BiomeData", biomeData);


        FormPanel worldPanel=new FormPanel();
        worldPanel.add("width:",width);
        worldPanel.add("height:",height);
        worldPanel.add("playerStartPosX:",playerStartPosX);
        worldPanel.add("playerStartPosY:",playerStartPosY);
        worldPanel.add("noiseZoomBiome:",noiseZoomBiome);
        worldPanel.add("tileSize:",tileSize);
        worldPanel.add("biomesSprites:",biomesSprites);
        worldPanel.add("maxRoadDistance:",maxRoadDistance);
        worldPanel.add("biomesNames:",biomesNames);
        tabs.addTab("WorldData", worldPanel);


        JScrollPane pane = new JScrollPane(edit);
        biomeData.setLeftComponent(list);    biomeData.setRightComponent(pane);

        load();

        JToolBar toolBar = new JToolBar("toolbar");
        add(toolBar, BorderLayout.PAGE_START);
        JButton newButton=new JButton("save");
        newButton.addActionListener(e -> WorldEditor.this.save());
        toolBar.add(newButton);

         newButton=new JButton("save selected biome");
        newButton.addActionListener(e -> WorldEditor.this.saveBiome());
        toolBar.add(newButton);

        newButton=new JButton("load");
        newButton.addActionListener(e -> WorldEditor.this.load());
        toolBar.add(newButton);

        toolBar.addSeparator();

        newButton=new JButton("test map");
        newButton.addActionListener(e -> WorldEditor.this.test());
        toolBar.add(newButton);
    }

    private void test() {

        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String className = forge.adventure.Main.class.getName();

        ArrayList<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-cp");
        command.add(classpath);
        command.add(className);

        command.add("testMap");

        ProcessBuilder build=  new ProcessBuilder(command);
        build .redirectInput(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT);
        try {
           Process process=  build.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void saveBiome()
    {

        edit.updateTerrain();
        Json json = new Json(JsonWriter.OutputType.json);
        FileHandle handle =  Config.instance().getFile(currentData.biomesNames[list.getSelectedIndex()]);
        handle.writeString(json.prettyPrint(json.toJson(edit.currentData,  BiomeData.class)),false);

    }
    void save()
    {
        currentData.width=width.intValue();
        currentData.height=height.intValue();
        currentData.playerStartPosX=playerStartPosX.floatValue();
        currentData.playerStartPosY=playerStartPosY.floatValue();
        currentData.noiseZoomBiome=noiseZoomBiome.floatValue();
        currentData.tileSize=tileSize.intValue();
        currentData.biomesSprites=biomesSprites.getText();
        currentData.maxRoadDistance=maxRoadDistance.floatValue();
        currentData.biomesNames=  (biomesNames.getList());

        Json json = new Json(JsonWriter.OutputType.json);
        FileHandle handle = Config.instance().getFile(Paths.WORLD);
        handle.writeString(json.prettyPrint(json.toJson(currentData,  WorldData.class)),false);

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
