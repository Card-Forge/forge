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
    boolean isNoSell;
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
        this(card,false);
    }
    public Reward(PaperCard card, boolean isNoSell) {
        type      = Type.Card;
        this.card = card;
        count     = 0;
        this.isNoSell = isNoSell;
    }
    public Reward(Type type, int count) {
        this.type  = type;
        this.count = count;
    }
    public Reward(Deck deck) {
        this(deck, false);
    }
    public Reward(Deck deck, boolean isNoSell) {
        type      = Type.CardPack;
        this.deck = deck;
        count     = 0;
        this.isNoSell = isNoSell;
    }
    public PaperCard getCard() { return card;  }
    public ItemData getItem()  { return item;  }
    public Deck getDeck()      { return deck;  }
    public Type getType()      { return type;  }
    public int getCount()      { return count; }
    public boolean isNoSell()      { return isNoSell; }
}
