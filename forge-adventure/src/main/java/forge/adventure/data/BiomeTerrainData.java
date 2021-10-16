package forge.adventure.data;


/**
 * Data class that will be used to read Json configuration files
 * BiomeData
 * contains the information for the terrain distribution
 */
public class BiomeTerrainData {
    //sprite name in the biome atlas file
    public String spriteName;
    //minimum noise value where to place the terrain
    public float min;
    //maximum noise value where to place the terrain
    public float max;
    // factor for the noise resolution
    public float resolution;

}
