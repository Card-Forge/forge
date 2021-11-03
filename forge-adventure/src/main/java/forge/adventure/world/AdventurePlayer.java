package forge.adventure.world;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.google.common.collect.Lists;
import forge.adventure.data.DifficultyData;
import forge.adventure.data.HeroListData;
import forge.adventure.util.*;
import forge.deck.CardPool;
import forge.deck.Deck;

import java.io.Serializable;

/**
 * Class that represents the player (not the player sprite)
 */
public class AdventurePlayer implements Serializable, SaveFileContent {
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
    private final DifficultyData difficultyData=new DifficultyData();
    static public AdventurePlayer current()
    {
        return WorldSave.currentSave.getPlayer();
    }
    private final CardPool cards=new CardPool();

    public void create(String n, Deck startingDeck, boolean male, int race, int avatar,DifficultyData difficultyData) {

        deck = startingDeck;
        gold =difficultyData.staringMoney;
        cards.clear();
        cards.addAllFlat(deck.getAllCardsInASinglePool().toFlatList());
        maxLife=difficultyData.startingLife;
        this.difficultyData.startingLife=difficultyData.startingLife;
        this.difficultyData.staringMoney=difficultyData.staringMoney;
        this.difficultyData.startingDifficulty=difficultyData.startingDifficulty;
        this.difficultyData.name=difficultyData.name;
        this.difficultyData.enemyLifeFactor=difficultyData.enemyLifeFactor;
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
    public CardPool getCards() {
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
    public void load(SaveFileData data) {

        this.difficultyData.startingLife=data.readInt("startingLife");
        this.difficultyData.staringMoney=data.readInt("staringMoney");
        this.difficultyData.startingDifficulty=data.readBool("startingDifficulty");
        this.difficultyData.name=data.readString("difficultyName");
        this.difficultyData.enemyLifeFactor=data.readFloat("enemyLifeFactor");


        name = data.readString("name");
        worldPosX = data.readFloat("worldPosX");
        worldPosY = data.readFloat("worldPosY");

        avatarIndex = data.readInt("avatarIndex");
        heroRace = data.readInt("heroRace");
        isFemale = data.readBool("isFemale");
        gold = data.readInt("gold");
        life = data.readInt("life");
        maxLife = data.readInt("maxLife");

        deck = new Deck(data.readString("deckName"));
        deck.getMain().addAll(CardPool.fromCardList(Lists.newArrayList((String[])data.readObject("deckCards"))));

        cards.clear();
        cards.addAll(CardPool.fromCardList(Lists.newArrayList((String[])data.readObject("cards"))));

        onLifeTotalChangeList.emit();
        onGoldChangeList.emit();
    }

    @Override
    public SaveFileData save() {
        SaveFileData data= new SaveFileData();


        data.store("startingLife",this.difficultyData.startingLife);
        data.store("staringMoney",this.difficultyData.staringMoney);
        data.store("startingDifficulty",this.difficultyData.startingDifficulty);
        data.store("difficultyName",this.difficultyData.name);
        data.store("enemyLifeFactor",this.difficultyData.enemyLifeFactor);


        data.store("name",name);
        data.store("worldPosX",worldPosX);
        data.store("worldPosY",worldPosY);
        data.store("avatarIndex",avatarIndex);
        data.store("heroRace",heroRace);
        data.store("isFemale",isFemale);
        data.store("gold",gold);
        data.store("life",life);
        data.store("maxLife",maxLife);
        data.store("deckName",deck.getName());


        data.storeObject("deckCards",deck.getMain().toCardList("\n").split("\n"));
        data.storeObject("cards",cards.toCardList("\n").split("\n"));

        return data;
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
