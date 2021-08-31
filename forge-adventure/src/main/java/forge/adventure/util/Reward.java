package forge.adventure.util;

import forge.item.PaperCard;

public class Reward {



    public PaperCard getCard() {
        return card;
    }


    public enum Type {
        Card,
        Gold,
        Item,
        Life

    }
    Type type;
    PaperCard card;

    private final int count;


    public int getCount() {
        return count;
    }


    public Type getType() {
        return type;
    }
    public Reward(int count) {
        type=Type.Gold;
        this.count =count;
    }
    public Reward(PaperCard card)
    {
        type= Type.Card;
        this.card=card;
        count = 0;
    }
    public Reward(Type type, int count) {
        this.type=type;
        this.count =count;
    }

}
