import java.util.*;

public class CombatPlaneswalker
{
  //key is attacker Card
  //value is CardList of blockers
  private Map<Card,CardList> map = new HashMap<Card,CardList>();
  private Set<Card> blocked = new HashSet<Card>();

  private int attackingDamage;
  private int defendingDamage;

  private String attackingPlayer;
  private String defendingPlayer;

  private Card planeswalker;

  public CombatPlaneswalker() {reset();}

  public void reset()
  {
    planeswalker = null;

    map.clear();
    blocked.clear();

    attackingDamage = 0;
    defendingDamage = 0;

    attackingPlayer = "";
    defendingPlayer = "";
  }
  public void setPlaneswalker(Card c){planeswalker = c;}
  public Card getPlaneswalker() {return planeswalker;}

  public void setAttackingPlayer(String player) {attackingPlayer = player;}
  public void setDefendingPlayer(String player) {defendingPlayer = player;}

  public String getAttackingPlayer() {return attackingPlayer;}
  public String getDefendingPlayer() {return defendingPlayer;}

  //relates to defending player damage
  public int getDefendingDamage() {return defendingDamage;}
  public void setDefendingDamage()
  {
    defendingDamage = 0;
    CardList att = new CardList(getAttackers());
    //sum unblocked attackers' power
    for(int i = 0; i < att.size(); i++)
      if(! isBlocked(att.get(i)))
        defendingDamage += att.get(i).getNetAttack();
  }
  public void addDefendingDamage(int n) {defendingDamage += n;}

  public void addAttackingDamage(int n) {attackingDamage += n;}
  public int getAttackingDamage() {return attackingDamage;}

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
      AllZone.GameAction.setAssignedDamage(attacking.get(i), block, CardListUtil.sumAttack(block));
      //attacking.get(i).setAssignedDamage(CardListUtil.sumAttack(block));
      if(block.size() == 0)//this damage is assigned to a player by setPlayerDamage()
        ;
      else if(block.size() == 1)
      {
        block.get(0).setAssignedDamage(attacking.get(i).getNetAttack());

        //trample
        int trample = attacking.get(i).getNetAttack() - block.get(0).getNetDefense();
        if(attacking.get(i).getKeyword().contains("Trample") && 0 < trample)
          this.addDefendingDamage(trample);

        trample = block.get(0).getNetAttack() - attacking.get(i).getNetDefense();
        if(block.get(0).getKeyword().contains("Trample") && 0 < trample)
          this.addAttackingDamage(trample);

      }//1 blocker
      else if(getAttackingPlayer().equals(Constant.Player.Computer))
      {
        setAssignedDamage(block, attacking.get(i).getNetAttack());
      }
      else//human
      {
        GuiDisplay2 gui = (GuiDisplay2) AllZone.Display;
        gui.assignDamage(attacking.get(i), block, attacking.get(i).getNetAttack());
      }
    }//for

    planeswalker.setAssignedDamage(defendingDamage);
    defendingDamage = 0;

  }//assignDamage()
  private void setAssignedDamage(CardList list, int damage)
  {
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
  }//assignDamage()
}

