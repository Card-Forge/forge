package forge.card.cost;

import forge.Card;
import forge.CardList;
import forge.card.spellability.SpellAbility;

public abstract class CostPartWithList extends CostPart {
    protected CardList list = null;
    public CardList getList() { return list; }
    public void setList(CardList setList) { list = setList; }
    public void resetList() { list = new CardList(); }
    public void addToList(Card c) { 
        if (list == null){ 
               resetList(); 
        }
        list.add(c); 
    }
    
    public void addListToHash(SpellAbility sa, String hash){
        for(Card card : list){
            sa.addCostToHashList(card, hash);
        }
    }
    
    public CostPartWithList(){}
    
    public CostPartWithList(String amount, String type, String description){
        super(amount, type, description);
        resetList();
    }
}
