package forge.adventure.world;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import forge.adventure.data.DifficultyData;
import forge.adventure.data.HeroListData;
import forge.adventure.util.Config;
import forge.adventure.util.Reward;
import forge.adventure.util.SaveFileContent;
import forge.adventure.util.SignalList;
import forge.deck.Deck;
import forge.item.PaperCard;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that represents the player (not the player sprite)
 */
public class AdventurePlayer implements Serializable, Disposable, SaveFileContent {
    private  Deck deck;
    private  int avatarIndex;
    private  int heroRace;
    private  boolean isFemale;
    private float worldPosX;
    private float worldPosY;
    private String name;
    private int gold=0;
    private int maxLife=20;
    private int life=20;
    private DifficultyData difficultyData;
    static public AdventurePlayer current()
    {
        return WorldSave.currentSave.getPlayer();
    }
    private List<PaperCard> cards=new ArrayList<>();

    public void create(String n, Deck startingDeck, boolean male, int race, int avatar,DifficultyData difficultyData) {

        deck = startingDeck;
        gold =difficultyData.staringMoney;
        cards.addAll(deck.getAllCardsInASinglePool().toFlatList());
        maxLife=difficultyData.startingLife;
        this.difficultyData=difficultyData;
        life=maxLife;
        avatarIndex = avatar;
        heroRace = race;
        isFemale = !male;
        name = n;
        onGoldChangeList.emit();
        onLifeTotalChangeList.emit();
    }

    public Deck getDeck() {
        return deck;
    }
    public List<PaperCard> getCards() {
        return cards;
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

    @Override
    public void writeToSaveFile(java.io.ObjectOutputStream out) throws IOException {


        out.writeUTF(name);
        out.writeFloat(worldPosX);
        out.writeFloat(worldPosY);
        out.writeInt(avatarIndex);
        out.writeInt(heroRace);
        out.writeBoolean(isFemale);
        out.writeInt(gold);
        out.writeInt(life);
        out.writeInt(maxLife);
        out.writeObject(deck);
        out.writeObject(cards);
    }

    @Override
    public void readFromSaveFile(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        name = in.readUTF();
        worldPosX = in.readFloat();
        worldPosY = in.readFloat();

        avatarIndex = in.readInt();
        heroRace = in.readInt();
        isFemale = in.readBoolean();
        gold = in.readInt();
        life = in.readInt();
        maxLife = in.readInt();
        deck = (Deck) in.readObject();
        cards = (List) in.readObject();


        onLifeTotalChangeList.emit();
        onGoldChangeList.emit();
    }

    public void dispose() {

    }

    public String spriteName() {
        return HeroListData.getHero(heroRace, isFemale);
    }

    public FileHandle sprite() {
        return Config.instance().getFile(HeroListData.getHero(heroRace, isFemale));
    }

    public TextureRegion avatar() {
        return HeroListData.getAvatar(heroRace, isFemale, avatarIndex);
    }

    public void addReward(Reward reward) {

        switch (reward.getType())
        {
            case Card:
                cards.add(reward.getCard());
                break;
            case Gold:
                addGold(reward.getCount());
                break;
            case Item:
                break;
            case Life:
                addMaxLife(reward.getCount());
                break;
        }

    }

    SignalList onLifeTotalChangeList=new SignalList();
    SignalList onGoldChangeList=new SignalList();
    SignalList onPlayerChangeList=new SignalList();

    private void addGold(int goldCount) {
        gold+=goldCount;
        onGoldChangeList.emit();
    }

    public int getGold() {
        return gold;
    }

    public void onLifeChange(Runnable  o) {
        onLifeTotalChangeList.add(o);
        o.run();
    }
    public void onPlayerChanged(Runnable  o) {
        onPlayerChangeList.add(o);
        o.run();
    }

    public void onGoldChange(Runnable  o) {
        onGoldChangeList.add(o);
        o.run();
    }

    public int getLife() {
        return life;
    }

    public int getMaxLife() {
        return maxLife;
    }

    public void heal() {
        life=maxLife;
        onLifeTotalChangeList.emit();
    }
    public void defeated() {
        gold=gold/2;
        life=Math.max(1,(int)(life-(maxLife*0.2f)));
        onLifeTotalChangeList.emit();
        onGoldChangeList.emit();
    }
    public void addMaxLife(int count) {
        maxLife+=count;
        life+=count;
        onLifeTotalChangeList.emit();
    }
    public void takeGold(int price) {
        gold-=price;
        onGoldChangeList.emit();
    }

    public DifficultyData getDifficulty() {
        return difficultyData;
    }
}
