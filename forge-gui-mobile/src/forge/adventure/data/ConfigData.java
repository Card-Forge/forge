package forge.adventure.data;


/**
 * Data class that will be used to read Json configuration files
 * BiomeData
 * contains general information about the game
 */
public class ConfigData {
    public int screenWidth;
    public int screenHeight;
    public String skin;
    public String font;
    public String fontColor;
    public int minDeckSize;
    public float playerBaseSpeed;
    public String[] colorIds;
    public String[] colorIdNames;
    public String[] starterEditions;
    public String[] starterEditionNames;
    public DifficultyData[] difficulties;
    public RewardData legalCards;
    public String[] restrictedCards;
    public String[] restrictedEditions;
    public String[] allowedEditions;
}
