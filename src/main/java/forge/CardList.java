package forge;


import com.google.code.jyield.Generator;
import com.google.code.jyield.Yieldable;
import forge.card.cardFactory.CardFactoryUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;


/**
 * <p>CardList class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class CardList implements Iterable<Card> {

    /**
     * <p>iterator.</p>
     *
     * @return a {@link java.util.Iterator} object.
     */
    public Iterator<Card> iterator() {
        return list.iterator();
    }

    private ArrayList<Card> list = new ArrayList<Card>();

    /**
     * <p>Constructor for CardList.</p>
     */
    public CardList() {
    }

    /**
     * <p>Constructor for CardList.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public CardList(Card... c) {
        addAll(c);
    }

    /**
     * <p>Constructor for CardList.</p>
     *
     * @param al a {@link java.util.ArrayList} object.
     */
    public CardList(ArrayList<Card> al) {
        addAll(al.toArray());
    }

    /**
     * Make a shallow copy of an Iterable's contents; this could be another
     * CardList.
     *
     * @param iterable  we traverse this and copy its contents into a local
     * field.
     */
    public CardList(Iterable<Card> iterable) {
        for (Card card : iterable) {
        	add(card);
        }
    }

    /**
     * <p>Constructor for CardList.</p>
     *
     * @param c an array of {@link java.lang.Object} objects.
     */
    public CardList(Object[] c) {
        addAll(c);
    }

    /**
     * Create a CardList from a finite generator of Card instances.
     * 
     * We ignore null values produced by the generator.
     * 
     * @param generator  a non-infinite generator of Card instances.
     */
    public CardList(Generator<Card> generator) {
    	// Generators yield their contents to a Yieldable.  Here,
    	// we create a quick Yieldable that adds the information it
    	// receives to this CardList's list field.
    	
    	Yieldable<Card> valueReceiver = new Yieldable<Card>() {
			@Override
			public void yield(Card card) {
				if (card != null) {
					list.add(card);
				}
			}
    	};
    	
    	generator.generate(valueReceiver);
    }

    /**
     * Create a cardlist with an initial estimate of its maximum size.
     * 
     * @param size an initialize estimate of its maximum size
     */
    public CardList(int size) {
    	list = new ArrayList<Card>(size);
	}

    /**
     * <p>Get any cards that exist in the passed in sets list.</p>
     *
     * @param sets a {@link java.util.ArrayList} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getSets(ArrayList<String> sets) {
        CardList list = new CardList();
        for (Card c : this) {
            for (SetInfo set : c.getSets())
                if (sets.contains(set.toString())) {
                    list.add(c);
                    break;
                }
        }

        return list;
    }//getSets()


    /**
     * <p>getColor.</p>
     *
     * @param cardColor a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getColor(String cardColor) {
        CardList list = new CardList();
        for (Card c : this) {
            if (cardColor.equals("Multicolor") && c.getColor().size() > 1)
                list.add(c);
            else if (c.isColor(cardColor) && c.getColor().size() == 1)
                list.add(c);
        }

        return list;
    }//getColor()

    /**
     * <p>getOnly2Colors.</p>
     *
     * @param clr1 a {@link java.lang.String} object.
     * @param clr2 a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getOnly2Colors(final String clr1, final String clr2) {
        CardList list = new CardList();
        list.addAll(this);

        CardListFilter clrF = new CardListFilter() {
            public boolean addCard(Card c) {
                ArrayList<Card_Color> cClrs = c.getColor();
                for (int i = 0; i < cClrs.size(); i++) {
                    if (!cClrs.get(i).toStringArray().get(0).equals(clr1) && !cClrs.get(i).toStringArray().get(0).equals(clr2))
                        return false;
                }
                return true;
            }
        };

        return list.filter(clrF);
    }

    /**
     * <p>reverse.</p>
     */
    public void reverse() {
        Collections.reverse(list);
    }

    /** {@inheritDoc} */
    public boolean equals(Object a) {
        if (a instanceof CardList) {
            CardList b = (CardList) a;
            if (list.size() != b.size()) return false;

            for (int i = 0; i < list.size(); i++)
                if (!list.get(i).equals(b.get(i))) return false;

            return true;
        } else return false;
    }

    //removes one copy of that card
    /**
     * <p>remove.</p>
     *
     * @param cardName a {@link java.lang.String} object.
     */
    public void remove(final String cardName) {
        CardList find = this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.getName().equals(cardName);
            }
        });

        if (0 < find.size()) this.remove(find.get(0));
        else throw new RuntimeException("CardList : remove(String cardname), error - card name not found: "
                + cardName + " - contents of Arraylist:" + list);

    }//remove(String cardName)

    /**
     * <p>size.</p>
     *
     * @return a int.
     */
    public int size() {
        return list.size();
    }

    /**
     * <p>add.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public void add(Card c) {
        list.add(c);
    }

    /**
     * <p>add.</p>
     *
     * @param n a int.
     * @param c a {@link forge.Card} object.
     */
    public void add(int n, Card c) {
        list.add(n, c);
    }

    /**
     * addAll(CardList) - lets you add one CardList to another directly
     *
     * @param in - CardList to add to the current CardList
     */
    public void addAll(CardList in) {
        addAll(in.toArray());
    }

    /**
     * <p>contains.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a boolean.
     */
    public boolean contains(Card c) {
        return list.contains(c);
    }

    //probably remove getCard() in the future
    /**
     * <p>getCard.</p>
     *
     * @param index a int.
     * @return a {@link forge.Card} object.
     */
    public Card getCard(int index) {
        return list.get(index);
    }

    /**
     * <p>get.</p>
     *
     * @param i a int.
     * @return a {@link forge.Card} object.
     */
    public Card get(int i) {
        return getCard(i);
    }

    /**
     * <p>addAll.</p>
     *
     * @param c an array of {@link java.lang.Object} objects.
     */
    public void addAll(Object c[]) {
        for (int i = 0; i < c.length; i++)
            list.add((Card) c[i]);
    }

    /**
     * <p>containsName.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a boolean.
     */
    public boolean containsName(Card c) {
        return containsName(c.getName());
    }

    /**
     * <p>containsName.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean containsName(String name) {
        for (int i = 0; i < size(); i++)
            if (getCard(i).getName().equals(name)) return true;

        return false;
    }

    //returns new subset of all the cards with the same name
    /**
     * <p>getName.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getName(String name) {
        CardList c = new CardList();

        for (int i = 0; i < size(); i++)
            if (getCard(i).getName().equals(name)) c.add(getCard(i));

        return c;
    }

    //returns new subset of all the cards that have a different name
    /**
     * <p>getNotName.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getNotName(String name) {
        CardList c = new CardList();

        for (int i = 0; i < size(); i++)
            if (!getCard(i).getName().equals(name)) c.add(getCard(i));

        return c;
    }

    /**
     * <p>getImageName.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getImageName(String name) {
        CardList c = new CardList();

        for (int i = 0; i < size(); i++)
            if (getCard(i).getImageName().equals(name)) c.add(getCard(i));

        return c;
    }

    /**
     * <p>getController.</p>
     *
     * @param player a {@link forge.Player} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getController(final Player player) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.getController().isPlayer(player);
            }
        });
    }

    /**
     * <p>getOwner.</p>
     *
     * @param player a {@link forge.Player} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getOwner(final Player player) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.getOwner().isPlayer(player);
            }
        });
    }

    /**
     * <p>getRarity.</p>
     *
     * @param rarity a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
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
    /**
     * <p>getType.</p>
     *
     * @param cardType a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getType(final String cardType) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isType(cardType);
            }
        });
    }

    //cardType is like "Land" or "Goblin", returns a new CardList with cards that do not have this type
    /**
     * <p>getNotType.</p>
     *
     * @param cardType a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getNotType(final String cardType) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return !c.isType(cardType);
            }
        });
    }

    /**
     * <p>getPermanents.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardList getPermanents() {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isPermanent();
            }
        });
    }

    /**
     * <p>getKeyword.</p>
     *
     * @param keyword a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getKeyword(final String keyword) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.hasKeyword(keyword);
            }
        });
    }

    /**
     * <p>getNotKeyword.</p>
     *
     * @param keyword a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getNotKeyword(final String keyword) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return !c.hasKeyword(keyword);
            }
        });
    }

    //get all cards that have this string in their keywords
    /**
     * <p>getKeywordsContain.</p>
     *
     * @param keyword a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getKeywordsContain(final String keyword) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.keywordsContain(keyword);
            }
        });
    }

    //get all cards that don't have this string in their keywords
    /**
     * <p>getKeywordsDontContain.</p>
     *
     * @param keyword a {@link java.lang.String} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getKeywordsDontContain(final String keyword) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return !c.keywordsContain(keyword);
            }
        });
    }

    /**
     * <p>getTokens.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardList getTokens() {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isToken();
            }
        });
    }

    /**
     * Create a new list of cards by applying a filter to this one.
     * 
     * @param filt  determines which cards are present in the resulting list
     * 
     * @return a subset of this CardList whose items  meet the filtering 
     * criteria; may be empty, but never null.
     */
    public CardList filter(CardListFilter filt) {
    	return CardFilter.filter(this, filt);
    }

    /**
     * <p>toArray.</p>
     *
     * @return an array of {@link forge.Card} objects.
     */
    public final Card[] toArray() {
        Card[] c = new Card[list.size()];
        list.toArray(c);
        return c;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return list.toString();
    }

    /**
     * <p>isEmpty.</p>
     *
     * @return a boolean.
     */
    public boolean isEmpty() {
        return list.isEmpty();
    }

    /**
     * <p>remove.</p>
     *
     * @param i a int.
     * @return a {@link forge.Card} object.
     */
    public Card remove(int i) {
        return list.remove(i);
    }

    /**
     * <p>remove.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public void remove(Card c) {
        list.remove(c);
    }
    
    /**
     * <p>removeAll.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public void removeAll(Card c) {
        ArrayList<Card> cList = new ArrayList<Card>();
        cList.add(c);
        list.removeAll(cList);
    }

    /**
     * <p>clear.</p>
     */
    public void clear() {
        list.clear();
    }

    /**
     * <p>shuffle.</p>
     */
    public void shuffle() {
        // reseed Random each time we want to Shuffle
        //MyRandom.random = MyRandom.random;
        Collections.shuffle(list, MyRandom.random);
        Collections.shuffle(list, MyRandom.random);
        Collections.shuffle(list, MyRandom.random);
    }

    /**
     * <p>sort.</p>
     *
     * @param c a {@link java.util.Comparator} object.
     */
    public void sort(Comparator<Card> c) {
        Collections.sort(list, c);
    }

    /**
     * <p>getTargetableCards.</p>
     *
     * @param source a {@link forge.Card} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getTargetableCards(final Card source) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return CardFactoryUtil.canTarget(source, c);
            }
        });
    }

    /**
     * <p>getUnprotectedCards.</p>
     *
     * @param source a {@link forge.Card} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getUnprotectedCards(final Card source) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
            	return !CardFactoryUtil.hasProtectionFrom(source, c);
            }
        });
    }
    
    /**
     * <p>getValidCards.</p>
     *
     * @param Restrictions a {@link java.lang.String} object.
     * @param sourceController a {@link forge.Player} object.
     * @param source a {@link forge.Card} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getValidCards(String Restrictions, final Player sourceController, final Card source) {
        return getValidCards(Restrictions.split(","), sourceController, source);
    }

    /**
     * <p>getValidCards.</p>
     *
     * @param Restrictions a {@link java.lang.String} object.
     * @param sourceController a {@link forge.Player} object.
     * @param source a {@link forge.Card} object.
     * @return a {@link forge.CardList} object.
     */
    public CardList getValidCards(final String Restrictions[], final Player sourceController, final Card source) {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c != null && c.isValidCard(Restrictions, sourceController, source);
            }
        });
    }

    /**
     * <p>getEquipMagnets.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardList getEquipMagnets() {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return (c.isCreature() && (c.getSVar("EquipMe").equals("Multiple")
                        || (c.getSVar("EquipMe").equals("Once") && !c.isEquipped())));
            }
        });
    }

    /**
     * <p>getEnchantMagnets.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardList getEnchantMagnets() {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return (c.isCreature() && (c.getSVar("EnchantMe").equals("Multiple")
                        || (c.getSVar("EnchantMe").equals("Once") && !c.isEnchanted())));
            }
        });
    }

    /**
     * <p>getTotalConvertedManaCost.</p>
     *
     * @return a int.
     */
    public int getTotalConvertedManaCost() {
        int total = 0;
        for (int i = 0; i < size(); i++) {
            total += get(i).getCMC();
        }
        return total;
    }

    /**
     * <p>getHighestConvertedManaCost.</p>
     *
     * @return a int.
     * @since 1.0.15
     */
    public int getHighestConvertedManaCost() {
        int total = 0;
        for (int i = 0; i < size(); i++) {
            total = Math.max(total, get(i).getCMC());
        }
        return total;
    }

    /**
     * <p>getColored.</p>
     *
     * @return a {@link forge.CardList} object.
     */
    public CardList getColored() {
        return this.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return (!c.isColorless());
            }
        });
    }

    public boolean getAbove(Card source, Card compared){
    	if (source.equals(compared))
    		return false;
    	
    	for(Card itr : this){
    		if (itr.equals(source))
    			return true;
    		else if (itr.equals(compared))
    			return false;
    	}
    	return false;
    }
    
    public boolean getDirectlyAbove(Card source, Card compared){
    	if (source.equals(compared))
    		return false;
    	
    	boolean checkNext = false;
    	for(Card itr : this){
    		if (checkNext){
    			if (itr.equals(compared))
    				return true;
    			return false;
    		}
    		else if (itr.equals(source))
    			checkNext = true;
    		else if (itr.equals(compared))
    			return false;
    	}
    	return false;
    }
    
}//end class CardList
