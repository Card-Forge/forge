
package forge;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import forge.card.cardFactory.CardFactoryUtil;


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
    
    public CardList(ArrayList<Card> al) {
    	addAll(al.toArray());
    }
    
    public CardList(Object[] c) {
        addAll(c);
    }
    
    // get any cards that exist in the passed in sets list
    public CardList getSets(ArrayList<String> sets) {
        CardList list = new CardList();
        for(Card c : this){
        	for(SetInfo set : c.getSets())
        		if (sets.contains(set.toString())){
        			list.add(c);
        			break;
        		}
        }

        return list;
    }//getSets()
    
    
    public CardList getColor(String cardColor) {
        CardList list = new CardList();
        for(Card c : this){
            if (cardColor.equals("Multicolor") && c.getColor().size() > 1)
                list.add(c);
            else if (c.isColor(cardColor) && c.getColor().size() == 1)
                list.add(c);
        }

        return list;
    }//getColor()
    
    public CardList getOnly2Colors(final String clr1, final String clr2) {
    	CardList list = new CardList();
    	list.add(this);
    	
    	CardListFilter clrF = new CardListFilter(){
			public boolean addCard(Card c){
				ArrayList<Card_Color> cClrs = c.getColor();
				for (int i=0; i<cClrs.size(); i++)
				{
					if (!cClrs.get(i).toStringArray().get(0).equals(clr1) && !cClrs.get(i).toStringArray().get(0).equals(clr2))
						return false;
				}
				return true;
			}
		};
		
		return list.filter(clrF);
    }
    
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
    
    /**
     * addAll(CardList) - lets you add one CardList to another directly
     * @param in - CardList to add to the current CardList
     */
    public void addAll(CardList in) {
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
    
    public CardList getController(final Player player) {
    	return this.filter(new CardListFilter() {
    		public boolean addCard(Card c) {
    			return c.getController().isPlayer(player);
    		}
    	});
    }
    
    public CardList getOwner(final Player player) {
    	return this.filter(new CardListFilter() {
    		public boolean addCard(Card c) {
    			return c.getOwner().isPlayer(player);
    		}
    	});
    }

    public CardList getRarity(final String rarity) {
    	return this.filter(new CardListFilter() {
    		public boolean addCard(Card c) {
    			// TODO spin off Mythic from Rare when the time comes
    			String r = c.getSVar("Rarity");
                return r.equals(rarity) ||
                        rarity.equals(Constant.Rarity.Rare) && r.equals(Constant.Rarity.Mythic);
    		}
    	});
    }

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
    
    public CardList getPermanents() {
    	return this.filter(new CardListFilter() {
    		public boolean addCard(Card c) {
    			return c.isPermanent();
    		}
    	});
    }
    
    public CardList getKeyword(final String keyword) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.hasKeyword(keyword);
            }
        });
    }

    public CardList getNotKeyword(final String keyword) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return !c.hasKeyword(keyword);
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
    	// reseed Random each time we want to Shuffle
    	//MyRandom.random = MyRandom.random;
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

    public CardList getValidCards(String Restrictions, final Player sourceController, final Card source) {
    	return getValidCards(Restrictions.split(","), sourceController, source);
    } 
    
    public CardList getValidCards(final String Restrictions[], final Player sourceController, final Card source) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isValidCard(Restrictions, sourceController, source);
            }
        });
    } 
    
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
    
    public int getTotalConvertedManaCost() {
    	int total = 0;
    	for (int i=0; i<size(); i++) {
    		total += get(i).getCMC();
    	}
    	return total;
    }
    
    public CardList getColored() {
    	return this.filter(new CardListFilter() {
    		public boolean addCard(Card c) {
    			return (!c.isColorless());
    		}
    	});
    }
    
}//end class CardList
