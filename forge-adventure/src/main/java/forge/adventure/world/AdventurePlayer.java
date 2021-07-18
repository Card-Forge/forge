package forge.adventure.world;

import forge.deck.Deck;

public class AdventurePlayer {
    private int worldPosX;
    private int worldPosY;

    private String name;
    private Deck deck;
    private WorldSave.Difficulty difficulty;

    public AdventurePlayer(String startname,Deck dck, WorldSave.Difficulty diff)
    {
        difficulty=diff;
        deck=dck;

        name=startname;
    }

    public Deck getDeck() {
        return deck;
    }

    public WorldSave.Difficulty getDifficulty() {
        return difficulty;
    }

    public String getName() {
        return name;
    }

    public int getWorldPosX() {
        return worldPosX;
    }

    public int getWorldPosY() {
        return worldPosY;
    }

    public void setWorldPosX(int worldPosX) {
        this.worldPosX = worldPosX;
    }

    public void setWorldPosY(int worldPosY) {
        this.worldPosY = worldPosY;
    }
}
