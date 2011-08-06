package forge;
import java.util.*;

public class CombatPlaneswalker
{
  //key is attacker Card
  //value is CardList of blockers
  private Map<Card,CardList> map = new HashMap<Card,CardList>();
  private Set<Card> blocked = new HashSet<Card>();
  private HashMap<Card, Integer> defendingFirstStrikeDamageMap = new HashMap<Card, Integer>();
  private HashMap<Card, Integer> defendingDamageMap = new HashMap<Card, Integer>();

  //private int attackingDamage;
  //private int defendingDamage;

  private Player attackingPlayer;
  private Player defendingPlayer;

  private Card planeswalker;

  public CombatPlaneswalker() {reset();}

  public void reset()
  {
    planeswalker = null;

    map.clear();
    blocked.clear();

    defendingFirstStrikeDamageMap.clear();
    defendingDamageMap.clear();

    attackingPlayer = null;
    defendingPlayer = null;
  }
  public void setPlaneswalker(Card c){planeswalker = c;}
  public Card getPlaneswalker() {return planeswalker;}

  public void setAttackingPlayer(Player player) {attackingPlayer = player;}
  public void setDefendingPlayer(Player player) {defendingPlayer = player;}

  public Player getAttackingPlayer() {return attackingPlayer;}
  public Player getDefendingPlayer() {return defendingPlayer;}

  //relates to defending player damage
  public int getTotalDefendingDamage()         
  {
	  int total = 0;
	  
	  Collection<Integer> c = defendingDamageMap.values();
	  
	  Iterator<Integer> itr = c.iterator();
	  while(itr.hasNext())
		  total+=itr.next();
	  
	  return total;
  }
  
  public void setDefendingDamage()
  {
        defendingDamageMap.clear();
        CardList att = new CardList(getAttackers());
        //sum unblocked attackers' power
        for(int i = 0; i < att.size(); i++) {
          if(! isBlocked(att.get(i))) {
           int damageDealt = att.get(i).getNetAttack();
             if (CombatUtil.isDoranInPlay())
                damageDealt = att.get(i).getNetDefense();

             //if the creature has first strike do not do damage in the normal combat phase
             //if(att.get(i).hasSecondStrike())
                addDefendingDamage(damageDealt, att.get(i));
          }
      }
  }

  public void addDefendingDamage(int n, Card source) 
  {
	  if (!defendingDamageMap.containsKey(source))
		  defendingDamageMap.put(source, n);
	  else 
	  {
		  defendingDamageMap.put(source, defendingDamageMap.get(source)+n);
	  }
  }

  /*//Needed ??
  public void addAttackingDamage(int n) {attackingDamage += n;}
  public int getAttackingDamage() {return attackingDamage;}
  */

  public void addAttacker(Card c) {map.put(c, new CardList());}
  public void resetAttackers()    {map.clear();}
  public Card[] getAttackers()
  {
    CardList out = new CardList();
    Iterator<Card> it = map.keySet().iterator();
    while(it.hasNext())
      out.add((Card)it.next());

    return out.toArray();
  }//getAttackers()

  public boolean isBlocked(Card attacker) {return blocked.contains(attacker);}
  public void addBlocker(Card attacker, Card blocker)
  {
    blocked.add(attacker);
    getList(attacker).add(blocker);
  }
  public void resetBlockers()
  {
    reset();

    CardList att = new CardList(getAttackers());
    for(int i = 0; i < att.size(); i++)
      addAttacker(att.get(i));
  }
  public CardList getAllBlockers()
  {
    CardList att = new CardList(getAttackers());
    CardList block = new CardList();

    for(int i = 0; i < att.size(); i++)
      block.addAll(getBlockers(att.get(i)).toArray());

    return block;
  }//getAllBlockers()

  public  CardList getBlockers(Card attacker) {return new CardList(getList(attacker).toArray());}
  private CardList getList(Card attacker)     {return (CardList)map.get(attacker);}

  public void removeFromCombat(Card c)
  {
    //is card an attacker?
    CardList att = new CardList(getAttackers());
    if(att.contains(c))
      map.remove(c);
    else//card is a blocker
    {
      for(int i = 0; i < att.size(); i++)
        if(getBlockers(att.get(i)).contains(c))
          getList(att.get(i)).remove(c);
    }
  }//removeFromCombat()
  public void verifyCreaturesInPlay()
  {
    CardList all = new CardList();
    all.addAll(getAttackers());
    all.addAll(getAllBlockers().toArray());

    for(int i = 0; i < all.size(); i++)
      if(! AllZone.GameAction.isCardInPlay(all.get(i)))
        removeFromCombat(all.get(i));
  }//verifyCreaturesInPlay()

  //set Card.setAssignedDamage() for all creatures in combat
  //also assigns player damage by setPlayerDamage()
  public void setAssignedDamage()
  {
    setDefendingDamage();

    CardList block;
    CardList attacking = new CardList(getAttackers());
    for(int i = 0; i < attacking.size(); i++)
    {
      block = getBlockers(attacking.get(i));

      
      //attacker always gets all blockers' attack
      for (Card b : block) {
    	  int attack =  b.getNetAttack();
     	  if (CombatUtil.isDoranInPlay())
     		 attack = b.getNetDefense();
    	  AllZone.GameAction.addAssignedDamage(attacking.get(i), b, attack);
      }
      //attacking.get(i).setAssignedDamage(CardListUtil.sumAttack(block));
      if(block.size() == 0)//this damage is assigned to a player by setPlayerDamage()
        ;
      else if(block.size() == 1)
      {
        block.get(0).addAssignedDamage(attacking.get(i).getNetAttack(), attacking.get(i));

        //trample
        int trample = attacking.get(i).getNetAttack() - block.get(0).getNetDefense();
        if (CombatUtil.isDoranInPlay())
        {
        	trample = attacking.get(i).getNetDefense() - block.get(0).getNetDefense();
        }
        if(attacking.get(i).getKeyword().contains("Trample") && 0 < trample)
          this.addDefendingDamage(trample, attacking.get(i));

        /*
        trample = block.get(0).getNetAttack() - attacking.get(i).getNetDefense();
        if(block.get(0).getKeyword().contains("Trample") && 0 < trample)
          this.addAttackingDamage(trample);
          */

      }//1 blocker
      else if(getAttackingPlayer().isComputer())
      {
    	for (Card b : block)
    		addAssignedDamage(b, attacking.get(i), attacking.get(i).getNetAttack());
      }
      else//human
      {
        GuiDisplay2 gui = (GuiDisplay2) AllZone.Display;
        //gui.assignDamage(attacking.get(i), block.get(0), attacking.get(i).getNetAttack());
        gui.assignDamage(attacking.get(i), block, attacking.get(i).getNetAttack());
      }
    }//for

    Iterator<Card> iter = defendingDamageMap.keySet().iterator();
	  while(iter.hasNext()) {
		Card crd = iter.next();
		planeswalker.addAssignedDamage(defendingDamageMap.get(crd), crd);
	  }
	  
	  defendingDamageMap.clear();


  }//assignDamage()
  private void addAssignedDamage(Card b, Card a, int damage)
  {
	/*
    CardListUtil.sortAttack(list);
    Card c;
    for(int i = 0; i < list.size(); i++)
    {
      c = list.get(i);
      if(c.getKillDamage() <= damage)
      {
        damage -= c.getKillDamage();
        c.setAssignedDamage(c.getKillDamage());
      }
    }//for
    */
	if (b.getKillDamage() <= damage)
	{
		//damage -= b.getKillDamage();
        b.addAssignedDamage(b.getKillDamage(), a);
	}
	
	  
	  
	  
  }//assignDamage()
}

