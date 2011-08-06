package forge;
import java.util.ArrayList;

//import java.util.ArrayList; //unused

public class Input_FirstStrikeDamage extends Input
{
  private static final long serialVersionUID = 6527794462455208533L;

  public Input_FirstStrikeDamage()
  {
    AllZone.Combat.verifyCreaturesInPlay();
    AllZone.pwCombat.verifyCreaturesInPlay();

    CombatUtil.showCombat();
  }

  public void showMessage()
  {
    ButtonUtil.enableOnlyOK();
    AllZone.Display.showMessage("First Strike Combat Damage is on the stack - Play Instants and Abilities");
  }
  public void selectButtonOK()
  {
    damageCreatureAndPlayer();

    AllZone.GameAction.checkStateEffects();

    //AllZone.Combat.reset();
    AllZone.Display.showCombat("");
   
    //do normal combat damage now and then switch to next phase (combat damage dealt - play instants and abilities)
    AllZone.Combat.setAssignedDamage();
    AllZone.pwCombat.setAssignedDamage();
   
    //AllZone.Phase.nextPhase();
    //for debugging: System.out.println("need to nextPhase(Input_Block_Instant.selectButtonOK) = true");
    AllZone.Phase.setNeedToNextPhase(true);
  }
  public void selectCard(Card card, PlayerZone zone)
  {
    InputUtil.playInstantAbility(card, zone);
  }//selectCard()
  @SuppressWarnings("unused") // playerDamage
private void playerDamage(PlayerLife p)
  {
    int n = p.getAssignedDamage();
    p.setAssignedDamage(0);
    p.subtractLife(n);
  }

  //moves assigned damage to damage for all creatures
  //deals damage to player if needed
  private void damageCreatureAndPlayer()
  {
	String player =  AllZone.Combat.getDefendingPlayer();
	if (player.equals("")) //this is a really bad hack, to allow raging goblin to attack on turn 1
		player = Constant.Player.Computer;
    PlayerLife life = AllZone.GameAction.getPlayerLife(player);
    life.subtractLife(AllZone.Combat.getDefendingFirstStrikeDamage());

    //What is this even for? doesn't look like it's used.
    /*
    life = AllZone.GameAction.getPlayerLife(AllZone.Combat.getAttackingPlayer());
    life.subtractLife(AllZone.Combat.getAttackingDamage());
    life.subtractLife(AllZone.pwCombat.getAttackingDamage());
	*/
    CardList unblocked = new CardList(AllZone.Combat.getUnblockedFirstStrikeAttackers());
    for(int j = 0; j < unblocked.size(); j++)
    {
       //System.out.println("Unblocked Creature: " +unblocked.get(j).getName());
       GameActionUtil.executePlayerCombatDamageEffects(unblocked.get(j));       
       
       /*if (unblocked.get(j).getKeyword().contains("Lifelink"))
       {
          
       }
       */
    }
   
    CardList attackers = new CardList(AllZone.Combat.getAttackers());
    CardList blockers = new CardList(AllZone.Combat.getAllBlockers().toArray());
     
    
    for (int i=0; i < attackers.size(); i++){
       //System.out.println("attacker #" + i + ": " + attackers.getCard(i).getName() +" " + attackers.getCard(i).getAttack());
    	
       CardList defend = AllZone.Combat.getBlockers(attackers.getCard(i));
       ArrayList<String> list = attackers.getCard(i).getKeyword();
       
        
       if ((attackers.getCard(i).hasFirstStrike() || attackers.getCard(i).hasDoubleStrike()))
        {
    	   if (attackers.getCard(i).getKeyword().contains("Lifelink"))
           {
       			GameActionUtil.executeLifeLinkEffects(attackers.getCard(i));
           }
    	   
    	   for(int j=0; j < CardFactoryUtil.hasNumberEnchantments(attackers.getCard(i), "Guilty Conscience"); j++)
        	   GameActionUtil.executeGuiltyConscienceEffects(attackers.getCard(i));
    	   /*
    	   
    	   //old stuff: gain life for each instance of lifelink
    	   for (int j=0; j < list.size(); j++)
		    {
		    	if (list.get(j).equals("Lifelink"))
		    		GameActionUtil.executeLifeLinkEffects(attackers.getCard(i));
		    	
		    }
		    */
        }

	     //not sure if this will work correctly with multiple blockers?
	   	int defenderToughness = 0;
	   	for (int k=0; k<defend.size(); k++)
	   	{
	   		defenderToughness += defend.get(k).getNetDefense();
	   	}
	   	if (( attackers.getCard(i).hasFirstStrike() || attackers.getCard(i).hasDoubleStrike() ) &&
	   		list.contains("Trample") && defenderToughness < attackers.getCard(i).getNetAttack() && AllZone.Combat.isBlocked(attackers.getCard(i)) )
	   		
	   	{
	   		GameActionUtil.executePlayerCombatDamageEffects(attackers.getCard(i));
	   	}
    }
    for (int i=0; i < blockers.size(); i++){
       //System.out.println("blocker #" + i + ": " + blockers.getCard(i).getName() +" " + blockers.getCard(i).getAttack());
       if ( (blockers.getCard(i).hasFirstStrike() || blockers.getCard(i).hasDoubleStrike()))
        {
    	   if (blockers.getCard(i).getKeyword().contains("Lifelink"))
    		   GameActionUtil.executeLifeLinkEffects(blockers.getCard(i));
    	   
    	   for(int j=0; j < CardFactoryUtil.hasNumberEnchantments(blockers.getCard(i), "Guilty Conscience"); j++)
        	   GameActionUtil.executeGuiltyConscienceEffects(blockers.getCard(i));
        }
    }
    
    
    CardList pwAttackers = new CardList(AllZone.pwCombat.getAttackers());
    CardList pwBlockers = new CardList(AllZone.pwCombat.getAllBlockers().toArray());
     
    
    for (int i=0; i < pwAttackers.size(); i++){
       //System.out.println("attacker #" + i + ": " + attackers.getCard(i).getName() +" " + attackers.getCard(i).getAttack());
       if ( (pwAttackers.getCard(i).hasFirstStrike() || pwAttackers.getCard(i).hasDoubleStrike()))
        {
    	   if (pwAttackers.getCard(i).getKeyword().contains("Lifelink"))
    		   GameActionUtil.executeLifeLinkEffects(pwAttackers.getCard(i));
    	   
    	   for(int j=0; j < CardFactoryUtil.hasNumberEnchantments(pwAttackers.getCard(i), "Guilty Conscience"); j++)
        	   GameActionUtil.executeGuiltyConscienceEffects(pwAttackers.getCard(i));
        }
    }
    for (int i=0; i < pwBlockers.size(); i++){
       //System.out.println("blocker #" + i + ": " + blockers.getCard(i).getName() +" " + blockers.getCard(i).getAttack());
       if ((pwBlockers.getCard(i).hasFirstStrike() || pwBlockers.getCard(i).hasDoubleStrike()))
        {
    	  if ( pwAttackers.getCard(i).getKeyword().contains("Lifelink"))
    		  GameActionUtil.executeLifeLinkEffects(pwBlockers.getCard(i));
    	  
   	   	  for(int j=0; j < CardFactoryUtil.hasNumberEnchantments(pwBlockers.getCard(i), "Guilty Conscience"); j++)
   	   		  GameActionUtil.executeGuiltyConscienceEffects(pwBlockers.getCard(i));
        }
    }
   
    //get all attackers and blockers
    CardList check = new CardList();
    check.addAll(AllZone.Human_Play.getCards());
    check.addAll(AllZone.Computer_Play.getCards());

    CardList all = check.getType("Creature");

    if(AllZone.pwCombat.getPlaneswalker() != null)
      all.add(AllZone.pwCombat.getPlaneswalker());

    Card c;
    for(int i = 0; i < all.size(); i++)
    {
      c = all.get(i);
      //because this sets off Jackal Pup, and Filthly Cur damage ability
      //and the stack says "Jack Pup causes 0 damage to the Computer"
      if(c.getAssignedDamage() != 0)
      {
        //c.addDamage(c.getAssignedDamage());
    	AllZone.GameAction.addDamage(c, c.getAssignedDamage());
        c.setAssignedDamage(0);
      }
    }
  }//moveDamage()
}//class