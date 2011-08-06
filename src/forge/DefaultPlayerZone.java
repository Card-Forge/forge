package forge;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;

public class DefaultPlayerZone extends PlayerZone implements java.io.Serializable
{
  private static final long serialVersionUID = -5687652485777639176L;

  private ArrayList<Card> cards = new ArrayList<Card>();
  private String zoneName;
  private Player player;
  private boolean update = true;

  private CardList cardsAddedThisTurn = new CardList();
  private ArrayList<String> cardsAddedThisTurnSource = new ArrayList<String>();

  public DefaultPlayerZone(String zone, Player inPlayer)
  {
    zoneName = zone;
    player = inPlayer;
  }
  //************ BEGIN - these methods fire updateObservers() *************
  public void add(Object o)
  {
	/*
	if (is("Graveyard"))
			System.out.println("GRAAAAAAAAAAAAAAAVE");
    */
	  
    Card c = (Card)o;

    cardsAddedThisTurn.add(c);
    if(AllZone.getZone(c) != null)
    {
      cardsAddedThisTurnSource.add(AllZone.getZone(c).getZoneName());
    }
    else
    {
      cardsAddedThisTurnSource.add("None");
    }

    if (is("Graveyard")
    	&& c.getKeyword().contains("When CARDNAME is put into a graveyard from anywhere, reveal CARDNAME and shuffle it into its owner's library instead."))
    {
    	PlayerZone lib = AllZone.getZone(Constant.Zone.Library, c.getOwner());
    	lib.add(c);
    	c.getOwner().shuffle();
    	return;
    }
    //slight difference from above I guess, the card gets put into the grave first, then shuffled into library.
    //key is that this would trigger abilities that trigger on cards hitting the graveyard
    else if (is("Graveyard") && c.getKeyword().contains("When CARDNAME is put into a graveyard from anywhere, shuffle it into its owner's library."))
    {
    	PlayerZone lib = AllZone.getZone(Constant.Zone.Library, c.getOwner());
       	PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, c.getOwner());
       	
       	grave.addOnce(c);
       	grave.remove(c);
       	lib.add(c);
       	c.getOwner().shuffle();
       	return;
    }
    	
    
    if (is("Graveyard")
        	&& c.getKeyword().contains("When CARDNAME is put into a graveyard from anywhere, reveal CARDNAME and its owner shuffles his or her graveyard into his or her library."))
    {
       	PlayerZone lib = AllZone.getZone(Constant.Zone.Library, c.getOwner());
       	PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, c.getOwner());
       	lib.add(c);
       	for(Card gc : AllZoneUtil.getPlayerGraveyard(c.getOwner()))
       		lib.add(gc);
       	grave.reset();
       	c.getOwner().shuffle();
       	return;
    }
    
    
    
    if (c.isUnearthed() && (is("Graveyard") || is("Hand") || is("Library")))
    {
    	PlayerZone removed = AllZone.getZone(Constant.Zone.Exile, c.getOwner());
    	removed.add(c);
    	c.setUnearthed(false);
    	return;
    }
    	
    
    c.addObserver(this);

    c.setTurnInZone(AllZone.Phase.getTurn());

    cards.add((Card)c);
    update();
  }
  
  //hack... use for adding Dread / Serra Avenger to grave
  public void addOnce(Object o)
  {
	  Card c = (Card)o;

      cardsAddedThisTurn.add(c);
      if(AllZone.getZone(c) != null)
      {
        cardsAddedThisTurnSource.add(AllZone.getZone(c).getZoneName());
      }
      else
      {
        cardsAddedThisTurnSource.add("None");
      }

	  c.addObserver(this);
	  
	  cards.add((Card)c);
	  update();
  }
  public void update(Observable ob, Object object)
  {
    this.update();
  }
  public void add(Card c, int index)
  {
    cardsAddedThisTurn.add(c);
    if(AllZone.getZone(c) != null)
    {
      cardsAddedThisTurnSource.add(AllZone.getZone(c).getZoneName());
    }
    else
    {
      cardsAddedThisTurnSource.add("None");
    }

    cards.add(index, c);
    c.setTurnInZone(AllZone.Phase.getTurn());
    update();
  }
  public void remove(Object c)
  {
    cards.remove((Card)c);
    update();
  }
  public void setCards(Card c[])
  {
    cards = new ArrayList<Card>(Arrays.asList(c));
    update();
  }
  //removes all cards
  public void reset()
  {
    cards.clear();
    update();
  }
  //************ END - these methods fire updateObservers() *************

  public boolean is(String zone)
  {
    return zone.equals(zoneName);
  }
  public boolean is(String zone, Player player)
  {
    return (zone.equals(zoneName) && player.isPlayer(player));
  }
  public Player getPlayer()
  {
    return player;
  }
  public String getZoneName()
  {
    return zoneName;
  }
  public int size()
  {
    return cards.size();
  }
  public Card get(int index)
  {
    return (Card)cards.get(index);
  }

  public Card[] getCards()
  {
    Card c[] = new Card[cards.size()];
    cards.toArray(c);
    return c;
  }
  public void update()
  {
    if(update)
      updateObservers();
  }
  public void setUpdate(boolean b) {update = b;}
  public boolean getUpdate() {return update;}
  
  public String toString(){
	  StringBuilder sb = new StringBuilder();
	  if (player != null)
		  sb.append(player.toString()).append(" ");
	  sb.append(zoneName);
	  return sb.toString();
  }

    public CardList getCardsAddedThisTurn(String origin)
    {
        System.out.print("Request cards put into " + getZoneName() + " from " + origin + ".Amount: ");
        CardList ret = new CardList();
        for(int i=0;i<cardsAddedThisTurn.size();i++)
        {
            if(origin.equals(cardsAddedThisTurnSource.get(i)) || origin.equals("Any"))
            {
                ret.add(cardsAddedThisTurn.get(i));
            }
        }
        System.out.println(ret.size());
        return ret;
    }

    public void resetCardsAddedThisTurn()
    {
        cardsAddedThisTurn.clear();
        cardsAddedThisTurnSource.clear();
    }
}
