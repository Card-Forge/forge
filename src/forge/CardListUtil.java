package forge;
import java.util.*;

public class CardListUtil
{
  public static CardList filterToughness(CardList in, int atLeastToughness)
  {
    CardList out = new CardList();
    for(int i = 0; i < in.size(); i++)
      if(in.get(i).getNetDefense() <= atLeastToughness)
        out.add(in.get(i));

    return out;
  }

  //the higher the defense the better
  @SuppressWarnings("unchecked") // Comparator needs <type>
public static void sortDefense(CardList list)
  {
    Comparator com = new Comparator()
    {
      public int compare(Object a1, Object b1)
      {
        Card a = (Card)a1;
        Card b = (Card)b1;

        return b.getNetDefense() - a.getNetDefense();
      }
    };
    list.sort(com);
  }//sortDefense()

  //the higher the attack the better
  @SuppressWarnings("unchecked") // Comparator needs type
public static void sortAttack(CardList list)
  {
    Comparator com = new Comparator()
    {
      public int compare(Object a1, Object b1)
      {
        Card a = (Card)a1;
        Card b = (Card)b1;
        
        if (CombatUtil.isDoranInPlay())
        	return b.getNetDefense() - a.getNetDefense();
        else
        	return b.getNetAttack() - a.getNetAttack();
      }
    };
    list.sort(com);
  }//sortAttack()


  //the lower the attack the better
  @SuppressWarnings("unchecked") // Comparator needs <type>
public static void sortAttackLowFirst(CardList list)
  {
    Comparator com = new Comparator()
    {
      public int compare(Object a1, Object b1)
      {
        Card a = (Card)a1;
        Card b = (Card)b1;
        
        if (CombatUtil.isDoranInPlay())
        	return a.getNetDefense() - b.getNetDefense();
        else
        	return a.getNetAttack() - b.getNetAttack();
      }
    };
    list.sort(com);
  }//sortAttackLowFirst()

  public static void sortNonFlyingFirst(CardList list)
  {
    sortFlying(list);
    list.reverse();
  }//sortNonFlyingFirst

  //the creature with flying are better
  @SuppressWarnings("unchecked") // Comparator needs <type>
public static void sortFlying(CardList list)
  {
    Comparator com = new Comparator()
    {
      public int compare(Object a1, Object b1)
      {
        Card a = (Card)a1;
        Card b = (Card)b1;

        if(a.getKeyword().contains("Flying") && b.getKeyword().contains("Flying"))
          return 0;
        else if(a.getKeyword().contains("Flying"))
          return -1;
        else if(b.getKeyword().contains("Flying"))
          return 1;

        return 0;
      }
    };
    list.sort(com);
  }//sortFlying()
  
  @SuppressWarnings("unchecked") // Comparator needs <type>
  public static void sortCMC(CardList list)
  {
     Comparator com = new Comparator()
     {
        public int compare(Object a1, Object b1)
        {
           Card a = (Card)a1;
           Card b = (Card)b1;
           
           int cmcA = CardUtil.getConvertedManaCost(a.getManaCost());
           int cmcB = CardUtil.getConvertedManaCost(b.getManaCost());
           
           if (cmcA == cmcB)
              return 0;
           if (cmcA > cmcB)
              return -1;
           if (cmcB > cmcA)
              return 1;
              
           return 0;
        }
     };
     list.sort(com);
  }//sortCMC
  
  
  public static CardList getColor(CardList list, final String color)
  {
    return list.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {
        return CardUtil.getColor(c).equals(color);
      }
    });
  }//getColor()
  
  public static CardList getGoldCards(CardList list)
  {
	  return list.filter(new CardListFilter()
	  {
		 public boolean addCard(Card c)
		 {
			 return CardUtil.getColors(c).size() >= 2;
		 }
	  });
  }
  
  public static int sumAttack(CardList c)
  {
    int attack = 0;
    
    for(int i  = 0; i < c.size(); i++){
      if(c.get(i).isCreature() && (!c.get(i).hasFirstStrike() || (c.get(i).hasDoubleStrike() && c.get(i).hasFirstStrike())) ) {
    	  if (!CombatUtil.isDoranInPlay())	
    		  attack += c.get(i).getNetAttack();
    	  else if(CombatUtil.isDoranInPlay())
    		  attack += c.get(i).getNetDefense();       
      }
    }
    //System.out.println("Total attack: " +attack);
    return attack;
  }//sumAttack()
  
  public static int sumFirstStrikeAttack(CardList c)
  {
    int attack = 0;
   
    for(int i  = 0; i < c.size(); i++){
      if(c.get(i).isCreature() && (c.get(i).hasFirstStrike() || c.get(i).hasDoubleStrike())) {
         if (!CombatUtil.isDoranInPlay())   
            attack += c.get(i).getNetAttack();
         else if(CombatUtil.isDoranInPlay())
            attack += c.get(i).getNetDefense(); 
      }
    }
    System.out.println("Total First Strike attack: " +attack);
    return attack;
  }//sumFirstStrikeAttack()
}