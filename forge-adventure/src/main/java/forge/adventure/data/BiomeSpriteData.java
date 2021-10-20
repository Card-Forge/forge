package forge.adventure.data;

import java.io.Serializable;

/**
 * Data class that will be used to read Json configuration files
 * BiomeSpriteData
 * contains the information for the sprites on the map like trees and rocks
 */
public class BiomeSpriteData implements Serializable {
    public String name;
    public double startArea;
    public double endArea;
    public double density;
    public double resolution;
    public int layer;

    public String key() {
        return "BiomeSprite&" + name;
    }
}
