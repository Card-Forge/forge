package forge;
import java.util.ArrayList;

public class PlayerZoneUtil
{	
	//TODO(sol) this whole class is pretty useless 
  public static ArrayList<Card> getCardType(PlayerZone zone, String cardType)
  {
    Card c;
    ArrayList<Card> list = new ArrayList<Card>();

    for(int i = 0; i < zone.size(); i++)
    {
      c = zone.get(i);
      if(c.isType(cardType))
        list.add(c);
    }
    return list;
  }

  public static ArrayList<Card> getUntappedCreatures(PlayerZone zone)
  {
    ArrayList<Card> all = getCardType(zone, "Creature");
    ArrayList<Card> untapped = new ArrayList<Card>();

    for(int i = 0; i < all.size(); i++)
      if(((Card)all.get(i)).isUntapped())
        untapped.add(all.get(i));

    return untapped;
  }

  static public boolean isCardInZone(PlayerZone pz, Card card)
  {
    if(card == null)
      return false;

    Card c[] = pz.getCards();

    for(int i = 0; i < c.length; i++)
      if(c[i].equals(card))
        return true;

    return false;
  }

}

