package forge;
import java.util.*;

public class Mana_PayCost
{
  //holds Mana_Part objects
  //ManaPartColor is stored before ManaPartColorless
  private ArrayList<Object> manaPart;

//manaCost can be like "0", "3", "G", "GW", "10", "3 GW", "10 GW"
  //or "split hybrid mana" like "2/G 2/G", "2/B 2/B 2/B"
  //"GW" can be paid with either G or W

  //can barely handle Reaper King mana cost "2/W 2/U 2/B 2/R 2/G"
  //to pay the colored costs for Reaper King you have to tap the colored
  //mana in the order shown from right to left (wierd I know)
  //note that when the cost is displayed it is backward "2/G 2/R 2/B 2/U 2/W"
  //so you would have to tap W, then U, then B, then R, then G (order matters)
  public Mana_PayCost(String manaCost)
  {
    manaPart = split(manaCost);
  }
  public boolean isNeeded(String mana)
  {
    Mana_Part m;
    for(int i = 0; i < manaPart.size(); i++)
    {
      m = (Mana_Part)manaPart.get(i);
      if(m.isNeeded(mana))
        return true;
    }
    return false;
  }
  public boolean isPaid()
  {
    Mana_Part m;
    for(int i = 0; i < manaPart.size(); i++)
    {
      m = (Mana_Part)manaPart.get(i);
      if(! m.isPaid())
        return false;
    }
    return true;
  }//isPaid()

  public void addMana(String mana)
  {
    if(! isNeeded(mana))
      throw new RuntimeException("Mana_PayCost : addMana() error, mana not needed - " +mana);

    Mana_Part m;
    for(int i = 0; i < manaPart.size(); i++)
    {
      m = (Mana_Part)manaPart.get(i);
      if(m.isNeeded(mana))
      {
        m.reduce(mana);
        break;
      }
    }//for
  }
  public String toString()
  {
    String s = "";
    ArrayList<Object> list = new ArrayList<Object>(manaPart);
    //need to reverse everything since the colored mana is stored first
    Collections.reverse(list);

    for(int i = 0; i < list.size(); i++)
      s = s +" " +list.get(i).toString();

    return s.trim();
  }

  private ArrayList<Object> split(String cost)
  {
    ArrayList<Object> list = new ArrayList<Object>();

    //handles costs like "3", "G", "GW", "10", "S"
    if(cost.length() == 1 || cost.length() == 2)
    {
      if(Character.isDigit(cost.charAt(0)))
        list.add(new Mana_PartColorless(cost));
      else if(cost.charAt(0) == 'S')
    	  list.add(new Mana_PartSnow());
      else
        list.add(new Mana_PartColor(cost));
    }
    else//handles "3 GW", "10 GW", "1 G G", "G G", "S 1"
    {
      //all costs that have a length greater than 2 have a space
      StringTokenizer tok = new StringTokenizer(cost);

      while(tok.hasMoreTokens())
        list.add(getManaPart(tok.nextToken()));

      //ManaPartColorless needs to be added AFTER the colored mana
      //in order for isNeeded() and addMana() to work correctly
      Object o = list.get(0);
      if(o instanceof Mana_PartSnow)
      {
        //move snow cost to the end of the list
        list.remove(0);
        list.add(o);
      }
      o = list.get(0);
      
      if(o instanceof Mana_PartColorless)
      {
        //move colorless cost to the end of the list
        list.remove(0);
        list.add(o);
      }
    }//else

    return list;
  }//split()

  private Mana_Part getManaPart(String partCost)
  {
    if(partCost.length() == 3)
    {
      return new Mana_PartSplit(partCost);
    }
    else if(Character.isDigit(partCost.charAt(0)))
    {
      return new Mana_PartColorless(partCost);
    }
    else if(partCost.equals("S"))
    {
    	return new Mana_PartSnow();
    }
    else
    {
      return new Mana_PartColor(partCost);
    }
  }
}
