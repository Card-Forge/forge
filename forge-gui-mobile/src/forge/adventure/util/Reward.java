package forge.adventure.util;

import forge.adventure.data.ItemData;
import forge.item.PaperCard;

/**
 * Reward class that may contain gold,cards or items
 */
public class Reward {
    public enum Type {
        Card,
        Gold,
        Item,
        Life,
        Mana
    }
    Type type;
    PaperCard card;
    ItemData item;
    private final int count;

    public Reward(ItemData item) {
        type      = Type.Item;
        this.item = item;
        count     = 1;
    }
    public Reward(int count) {
        type       = Type.Gold;
        this.count = count;
    }
    public Reward(PaperCard card) {
        type      = Type.Card;
        this.card = card;
        count     = 0;
    }
    public Reward(Type type, int count) {
        this.type  = type;
        this.count = count;
    }
    public PaperCard getCard() { return card;  }
    public ItemData getItem()  { return item;  }
    public Type getType()      { return type;  }
    public int getCount()      { return count; }
}
