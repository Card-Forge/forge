package forge;
import java.util.*;

//PlayerZone observers the cards that are added to its zone
abstract public class PlayerZone extends MyObservable implements IPlayerZone, Observer
{

}

interface IPlayerZone
{
  public void setUpdate(boolean b);
  public boolean getUpdate();

  public int size();
  public void add(Object o);
  public void add(Card c, int index);
  public void addOnce(Object o);

  public Card get(int index);
  public void remove(Object o);
  public void remove(int index);

  public void setCards(Card c[]);
  public Card[] getCards();

  //removes all cards
  public void reset();

  public boolean is(String zone);
  public boolean is(String zone, Player player);

  public Player getPlayer();//the Player that owns this zone
  public String getZoneName();//returns the Zone's name like Graveyard
}