package forge.adventure.util;

import forge.adventure.data.ItemData;
import forge.deck.Deck;
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
        Shards,
        CardPack
    }
    Type type;
    PaperCard card;
    ItemData item;
    Deck deck;
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
    public Reward(Deck deck) {
        type      = Type.CardPack;
        this.deck = deck;
        count     = 0;
    }
    public PaperCard getCard() { return card;  }
    public ItemData getItem()  { return item;  }
    public Deck getDeck()      { return deck;  }
    public Type getType()      { return type;  }
    public int getCount()      { return count; }
}
