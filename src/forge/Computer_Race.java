package forge;
public class Computer_Race
{
  public boolean willWinRace(CardList attackList, int attackLife, CardList blockList, int blockLife)
  {




    return true;
  }//willWinRace()

  //if all of attackList attacked, and blockList goodBlocked(),
  //what attackers would still be attacking?
  public CardList getUnblocked(CardList attackList, CardList blockList)
  {
    //copy
    attackList = new CardList(attackList.toArray());
    blockList = new CardList(blockList.toArray());

    //need to match smallest blocker with biggest attacker
    CardListUtil.sortAttack(attackList);
    CardListUtil.sortAttackLowFirst(blockList);

    CardList unblocked = new CardList();

    for(int i = 0; i < attackList.size(); i++)
    {
      Card attack = attackList.get(i);
      CardList block = getPossibleBlockers(attack, blockList);

      CardList check = goodBlock(attack, block);
      if(check == null)
        unblocked.add(attack);
      else
        for(int z = 0; z < check.size(); z++)
          blockList.remove(check.get(z));
    }

    return unblocked;
  }//getUnblocked()

  //may return null
  //null means there are no good blockers in blockList
  //check for the 3 criteria below
  //(attacker live and blocker live)
  //(attacker die and blocker live)
  //(attacker die and blocker die)
  public CardList goodBlock(Card attacker, CardList blockList)
  {
    Card c;
    CardList check;

    c = safeSingleBlock(attacker, blockList);
    check = checkGoodBlock(c);
    if(check != null)
      return check;

    c = shieldSingleBlock(attacker, blockList);
    check = checkGoodBlock(c);
    if(check != null)
      return check;

   c = tradeSingleBlock(attacker, blockList);
    check = checkGoodBlock(c);
    if(check != null)
      return check;

    Card[] m = this.multipleBlock(attacker, blockList);
    if(m.length != 0)
      return new CardList(m);

    return null;
  }
  //goodBlock() uses this method
  public CardList checkGoodBlock(Card c)
  {
    if(c != null)
    {
      CardList list = new CardList();
      list.add(c);
      return list;
    }
    return null;
  }//checkGoodBlock()

  //(attacker dies, blocker lives)
  //returns null if no blocker is found
  Card safeSingleBlock(Card attacker, CardList blockList)
  {
    CardList c = blockList;

    for(int i = 0; i < c.size(); i++)
      if(CombatUtil.canDestroyAttacker(attacker, c.get(i), null) &&
      (! CombatUtil.canDestroyBlocker(c.get(i), attacker, null)))
        return c.get(i);

    return null;
  }//safeSingleBlock()

  //(attacker dies, blocker dies)
  //returns null if no blocker is found
  Card tradeSingleBlock(Card attacker, CardList blockList)
  {
    CardList c = blockList;

    for(int i = 0; i < c.size(); i++)
      if(CombatUtil.canDestroyAttacker(attacker, c.get(i), null))
        return c.get(i);

    return null;
  }//tradeSingleBlock()

  //(attacker lives, blocker lives)
  //returns null if no blocker is found
  Card shieldSingleBlock(Card attacker, CardList blockList)
  {
    CardList c = blockList;

    for(int i = 0; i < c.size(); i++)
      if(! CombatUtil.canDestroyBlocker(c.get(i), attacker, null))
        return c.get(i);

    return null;
  }//shieldSingleBlock()

  //finds multiple blockers
  //returns an array of size 0 if not multiple blocking
  Card[] multipleBlock(Card attacker, CardList blockList)
  {
    CardList c = blockList;

    int defense = attacker.getNetDefense() - attacker.getDamage();
    //if attacker cannot be destroyed
    if(defense > CardListUtil.sumAttack(c))
      return new Card[] {};

    CardList block = new CardList();
    c.shuffle();

    while(defense > CardListUtil.sumAttack(block))
      block.add(c.remove(0));

    Card check[] = block.toArray();

    //no single blockers, that should be handled somewhere else
    if(check.length == 1)
      return new Card[] {};

    return check;
  }//multipleBlock()

  //checks for flying and stuff like that
  private CardList getPossibleBlockers(Card attacker, CardList blockList)
  {
    CardList list = new CardList();
    for(int i = 0; i < blockList.size(); i++)
      if(CombatUtil.canBlock(attacker, blockList.get(i)))
        list.add(blockList.get(i));

    return list;
  }
}