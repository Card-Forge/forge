package forge;
import java.util.*;

public class DefaultPlayerZone extends PlayerZone implements java.io.Serializable
{
  private static final long serialVersionUID = -5687652485777639176L;

  private ArrayList<Card> cards = new ArrayList<Card>();
  private String zoneName;
  private Player player;
  private boolean update = true;

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
       	for(Card gc : grave.getCards())
       		lib.add(gc);
       	grave.reset();
       	c.getOwner().shuffle();
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
  
  //hack... use for adding Dread / Serra Avenger to grave
  public void addOnce(Object o)
  {
	  Card c = (Card)o;
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
  public boolean is(String zone, Player player)
  {
    return (zone.equals(zoneName) && player.isPlayer(player));
  }
  public Player getPlayer()
  {
    return player;
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
