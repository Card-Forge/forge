package forge;
import java.util.*;

public class DefaultPlayerZone extends PlayerZone implements java.io.Serializable
{
  private static final long serialVersionUID = -5687652485777639176L;

  private ArrayList<Card> cards = new ArrayList<Card>();
  private String zoneName;
  private String playerName;
  private boolean update = true;

  public DefaultPlayerZone(String zone, String player)
  {
    zoneName = zone;
    playerName = player;
  }
  //************ BEGIN - these methods fire updateObservers() *************
  public void add(Object o)
  {
	/*
	if (is("Graveyard"))
			System.out.println("GRAAAAAAAAAAAAAAAVE");
    */
	  
    Card c = (Card)o;
    
    if (is("Graveyard")
    	&& c.getKeyword().contains("When CARDNAME is put into a graveyard from anywhere, reveal CARDNAME and shuffle it into its owner's library instead."))
    {
    	PlayerZone lib = AllZone.getZone(Constant.Zone.Library, c.getOwner());
    	lib.add(c);
    	AllZone.GameAction.shuffle(c.getOwner());
    	return;
    }
    
    if (is("Graveyard")
        	&& c.getKeyword().contains("When CARDNAME is put into a graveyard from anywhere, reveal CARDNAME and its owner shuffles his or her graveyard into his or her library."))
    {
       	PlayerZone lib = AllZone.getZone(Constant.Zone.Library, c.getOwner());
       	PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, c.getOwner());
       	lib.add(c);
       	lib.add(grave);
       	grave.reset();
       	AllZone.GameAction.shuffle(c.getOwner());
       	return;
    }
    
    if (c.isUnearthed() && (is("Graveyard") || is("Hand")))
    {
    	PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, c.getOwner());
    	removed.add(c);
    	c.setUnearthed(false);
    	return;
    }
    	
    
    c.addObserver(this);

    c.setTurnInZone(AllZone.Phase.getTurn());

    cards.add((Card)c);
    update();
  }
  public void update(Observable ob, Object object)
  {
    this.update();
  }
  public void add(Card c, int index)
  {
    cards.add(index, c);
    c.setTurnInZone(AllZone.Phase.getTurn());
    update();
  }
  public void remove(Object c)
  {
    //TODO: put leaves play checks here
    cards.remove((Card)c);
    update();
  }
  public void remove(int index)
  {
    cards.remove(index);
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
  public boolean is(String zone, String player)
  {
    return (zone.equals(zoneName) && player.equals(playerName));
  }
  public String getPlayer()
  {
    return playerName;
  }
  public String getZone()
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
}
