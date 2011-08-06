
package forge;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;


public class CardList implements Iterable<Card> {
    
    public Iterator<Card> iterator() {
        return list.iterator();
    }
    
    private ArrayList<Card> list = new ArrayList<Card>();
    
    //private LinkedList list = new LinkedList();
    
    public CardList() {}
    
    public CardList(Card... c) {
        addAll(c);
    }
    
    public CardList(Object[] c) {
        addAll(c);
    }
    
//    cardColor is like "R" or "G",  returns a new CardList that is a subset of current CardList
    public CardList getColor(String cardColor) {
        CardList c = new CardList();
        Card card;
        for(int i = 0; i < size(); i++) {
            card = getCard(i);
            
            if(-1 < card.getManaCost().indexOf(cardColor)) //hopefully this line works
            c.add(getCard(i));
        }
        return c;
    }//getColor()
    
    public void reverse() {
        Collections.reverse(list);
    }
    
    public boolean equals(Object a) {
    	if(a instanceof CardList){
    		CardList b = (CardList)a;
	        if(list.size() != b.size()) return false;
	        
	        for(int i = 0; i < list.size(); i++)
	            if(!list.get(i).equals(b.get(i))) return false;
	        
	        return true;
    	} else return false;
    }
    
    //removes one copy of that card
    public void remove(final String cardName) {
        CardList find = this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.getName().equals(cardName);
            }
        });
        
        if(0 < find.size()) this.remove(find.get(0));
        else throw new RuntimeException("CardList : remove(String cardname), error - card name not found: "
                + cardName + " - contents of Arraylist:" + list);
        
    }//remove(String cardName)
    
    public int size() {
        return list.size();
    }
    
    public void add(Card c) {
        list.add(c);
    }
    
    public void add(int n, Card c) {
        list.add(n, c);
    }
    
    /**
     * add(CardList) - lets you add one CardList to another directly
     * @param in - CardList to add to the current CardList
     */
    public void add(CardList in) {
    	addAll(in.toArray());
    }
    
    public boolean contains(Card c) {
        return list.contains(c);
    }
    
    //probably remove getCard() in the future
    public Card getCard(int index) {
        return list.get(index);
    }
    
    public Card get(int i) {
        return getCard(i);
    }
    
    public void addAll(Object c[]) {
        for(int i = 0; i < c.length; i++)
            list.add((Card) c[i]);
    }
    
    public boolean containsName(Card c) {
        return containsName(c.getName());
    }
    
    public boolean containsName(String name) {
        for(int i = 0; i < size(); i++)
            if(getCard(i).getName().equals(name)) return true;
        
        return false;
    }
    
    //returns new subset of all the cards with the same name
    public CardList getName(String name) {
        CardList c = new CardList();
        
        for(int i = 0; i < size(); i++)
            if(getCard(i).getName().equals(name)) c.add(getCard(i));
        
        return c;
    }
    
    //returns new subset of all the cards that have a different name
    public CardList getNotName(String name) {
        CardList c = new CardList();
        
        for(int i = 0; i < size(); i++)
            if(!getCard(i).getName().equals(name)) c.add(getCard(i));
        
        return c;
    }
    
    public CardList getImageName(String name) {
        CardList c = new CardList();
        
        for(int i = 0; i < size(); i++)
            if(getCard(i).getImageName().equals(name)) c.add(getCard(i));
        
        return c;
    }
    
    /* no longer needed
    private String toMixedCase(String s)
    {
    	String fc = "";
    	String lcs = "";
    	
    	fc = s.substring(0,1).toUpperCase();
    	lcs = s.substring(1).toLowerCase();
    	
    	return fc + lcs;
    }
    */
    
  //cardType is like "Land" or "Goblin", returns a new CardList that is a subset of current CardList
    public CardList getType(final String cardType) {
    	return this.filter(new CardListFilter() {
    		public boolean addCard(Card c) {
    			return c.isType(cardType);
    		}
    	});
    }
    
  //cardType is like "Land" or "Goblin", returns a new CardList with cards that do not have this type
    public CardList getNotType(final String cardType) {
    	return this.filter(new CardListFilter() {
    		public boolean addCard(Card c) {
    			return !c.isType(cardType);
    		}
    	});
    }
    
    public CardList getTapState(String TappedOrUntapped)
    {
    	CardList cl = new CardList();
    	Card c;
    	for (int i=0; i<size(); i++)
    	{
    		c = getCard(i);
    		if (TappedOrUntapped.equals("Tapped"))
    		{
    			if (c.isTapped() == true)
    				cl.add(c);
    		}
    		else if (TappedOrUntapped.equals("Untapped"))
    		{
    			if (c.isUntapped() == true)
    				cl.add(c);
    		}
    	}
    	
    	return cl;
    }
    
    public CardList getKeyword(final String keyword) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.getKeyword().contains(keyword);
            }
        });
    }

    public CardList getNotKeyword(final String keyword) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return !c.getKeyword().contains(keyword);
            }
        });
    }
    
    //get all cards that have this string in their keywords
    public CardList getKeywordsContain(final String keyword) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.keywordsContain(keyword);
            }
        });
    }
    
    //get all cards that don't have this string in their keywords
    public CardList getKeywordsDontContain(final String keyword) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return !c.keywordsContain(keyword);
            }
        });
    }
    
    public CardList getTokens() {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isToken();
            }
        });
    }
    
    public CardList canBeDamagedBy(final Card card) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return CardFactoryUtil.canDamage(card, c);
            }
        });
    }
    
    public CardList filter(CardListFilter f) {
        CardList c = new CardList();
        for(int i = 0; i < size(); i++)
            if(f.addCard(getCard(i))) c.add(getCard(i));
        
        return c;
    }
    
    public final Card[] toArray() {
        Card[] c = new Card[list.size()];
        list.toArray(c);
        return c;
    }
    
    @Override
    public String toString() {
        return list.toString();
    }
    
    public boolean isEmpty() {
        return list.isEmpty();
    }
    
    public Card remove(int i) {
        return list.remove(i);
    }
    
    public void remove(Card c) {
        list.remove(c);
    }
    
    public void clear() {
        list.clear();
    }
    
    public void shuffle() {
        Collections.shuffle(list, MyRandom.random);
        Collections.shuffle(list, MyRandom.random);
        Collections.shuffle(list, MyRandom.random);
    }
    
    public void sort(Comparator<Card> c) {
        Collections.sort(list, c);
    }
    
    public CardList getTargetableCards(final Card Source)
    {
    	return this.filter(new CardListFilter() {
    		public boolean addCard(Card c) {
    			return CardFactoryUtil.canTarget(Source, c);
    		}
    	});
    }
    
    
    public CardList getValidCards(final String Restrictions[], final Player Controller, final Card source) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isValidCard(Restrictions, Controller, source);
            }
        });
    } 
    
    public CardList getValidCards(final String Restrictions[], final Card source) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isValidCard(Restrictions, source);
            }
        });
    } 
    
    public CardList getValidCards(final String Restrictions[], final Player Controller) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isValidCard(Restrictions, Controller);
            }
        });
    } 
       
    public CardList getValidCards(final String Restrictions[]) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isValidCard(Restrictions);
            }
        });
    }//getValidCards
    
    
    public CardList getEquipMagnets() {
    	return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
            	return (c.isCreature() && (c.getSVar("EquipMe").equals("Multiple") 
        	    		|| (c.getSVar("EquipMe").equals("Once") && !c.isEquipped())));
            }
        });
    }
    
    public CardList getEnchantMagnets() {
    	return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
            	return (c.isCreature() && (c.getSVar("EnchantMe").equals("Multiple") 
        	    		|| (c.getSVar("EnchantMe").equals("Once") && !c.isEnchanted())));
            }
        });
    }
}
