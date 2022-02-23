package forge.adventure.data;


/**
 * Data class that will be used to read Json configuration files
 * BiomeData
 * contains the information for a generated deck
 *
 * if the template is null then it will use just the reward information of mainDeck and sideBoard
 */
public class GeneratedDeckData {
    public String name;
    public GeneratedDeckTemplateData template;
    public RewardData[] mainDeck;
    public RewardData[] sideBoard;

}
