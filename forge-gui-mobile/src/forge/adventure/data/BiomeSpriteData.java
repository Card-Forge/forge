package forge.adventure.data;

import forge.adventure.util.SaveFileContent;
import forge.adventure.util.SaveFileData;

/**
 * Data class that will be used to read Json configuration files
 * BiomeSpriteData
 * contains the information for the sprites on the map like trees and rocks
 */
public class BiomeSpriteData implements SaveFileContent {
    public String name;
    public double startArea;
    public double endArea;
    public double density;
    public double resolution;
    public int layer;

    public String key() {
        return "BiomeSprite&" + name;
    }

    @Override
    public void load(SaveFileData data) {
        name=data.readString("name");
        startArea=data.readDouble("startArea");
        endArea=data.readDouble("endArea");
        density=data.readDouble("density");
        resolution=data.readDouble("resolution");;
        layer=data.readInt("layer");
    }

    @Override
    public SaveFileData save() {
        SaveFileData data=new SaveFileData();
        data.store("name",name);
        data.store("startArea",startArea);
        data.store("endArea",endArea);
        data.store("density",density);
        data.store("resolution",resolution);
        data.store("layer",layer);
        return data;
    }
}
