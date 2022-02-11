package forge.adventure.data;

/**
 * Data class that will be used to read Json configuration files
 * BiomeData
 * contains the information for the generated deck
 */
public class GeneratedDeckTemplateData {
    public String[] colors;
    public int    count;
    public float rares;
    public String tribe;
    public float tribeSynergyCards=0.6f;
    public float tribeCards=1.0f;
}