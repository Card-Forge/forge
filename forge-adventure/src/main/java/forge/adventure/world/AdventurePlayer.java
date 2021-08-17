package forge.adventure.world;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import forge.adventure.data.HeroListData;
import forge.adventure.util.Res;
import forge.deck.Deck;

import java.io.IOException;
import java.io.Serializable;

public class AdventurePlayer implements Serializable, Disposable {
    private final Deck deck;
    private final int avatarIndex;
    private final int heroRace;
    private final boolean isFemale;
    private float worldPosX;
    private float worldPosY;
    private String name;


    public AdventurePlayer(String n, Deck startingDeck, boolean male, int race, int avatar) {

        deck = startingDeck;

        avatarIndex = avatar;
        heroRace = race;
        isFemale = !male;
        name = n;
    }

    public Deck getDeck() {
        return deck;
    }


    public String getName() {
        return name;
    }

    public float getWorldPosX() {
        return worldPosX;
    }

    public void setWorldPosX(float worldPosX) {
        this.worldPosX = worldPosX;
    }

    public float getWorldPosY() {
        return worldPosY;
    }

    public void setWorldPosY(float worldPosY) {
        this.worldPosY = worldPosY;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {


        out.writeUTF(name);
        out.writeFloat(worldPosX);
        out.writeFloat(worldPosY);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        name = in.readUTF();
        worldPosX = in.readFloat();
        worldPosY = in.readFloat();


    }

    public void dispose() {

    }

    public String spriteName() {
        return HeroListData.getHero(heroRace, isFemale);
    }

    public FileHandle sprite() {
        return Res.CurrentRes.GetFile(HeroListData.getHero(heroRace, isFemale));
    }

    public TextureRegion avatar() {
        return HeroListData.getAvatar(heroRace, isFemale, avatarIndex);
    }
}
