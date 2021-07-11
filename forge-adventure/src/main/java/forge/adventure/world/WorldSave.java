package forge.adventure.world;

import forge.deck.Deck;

public class WorldSave {
    public enum Difficulty
    {
        Easy,
        Medium,
        Hard
    }
    public AdventurePlayer player;
    public World world;
    public Difficulty difficulty;
    static WorldSave currentSave;

    public static WorldSave getCurrentSave()
    {
        return currentSave;
    }
    public static WorldSave GenerateNewWorld(String name , Deck startingDeck, Difficulty diff)
    {
        currentSave=new WorldSave();
        currentSave.world=new World();
        currentSave.player=new AdventurePlayer(name,startingDeck,diff);
        return currentSave;
        //return currentSave = ret;
    }
}
