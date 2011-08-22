package forge.card.cost;

import forge.Card;
import forge.CardList;

public abstract class CostPartWithList extends CostPart {
    protected CardList list = null;
    public CardList getList() { return list; }
    public void setList(CardList setList) { list = setList; }
    public void resetList() { list = new CardList(); }
    public void addToList(Card c) { list.add(c); }
}
