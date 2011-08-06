package forge;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;

//doesHumanAttackAndWin() uses the global variable AllZone.ComputerPlayer
public class ComputerUtil_Attack2 {
	
//possible attackers and blockers
	private CardList attackers;
	private CardList blockers;
    private CardList playerCreatures;
	private int blockerLife;
	
	private Random random = new Random();
	private final int randomInt = random.nextInt();
	
	private CardList humanList;   //holds human player creatures
	private CardList computerList;//holds computer creatures

    private int aiAggression = 0; // added by me, how aggressive the ai attack will be depending on circumstances
	
	public ComputerUtil_Attack2(Card[] possibleAttackers, Card[] possibleBlockers, int blockerLife)
	{
		this(new CardList(possibleAttackers), new CardList(possibleBlockers), blockerLife);
	}

	public ComputerUtil_Attack2(CardList possibleAttackers, CardList possibleBlockers, int blockerLife)
	{
		humanList = new CardList(possibleBlockers.toArray());
		humanList = humanList.getType("Creature");
		
		computerList = new CardList(possibleAttackers.toArray()); 
		computerList = computerList.getType("Creature");
        playerCreatures = new CardList(possibleBlockers.toArray());
        playerCreatures = playerCreatures.getType("Creature");
		
		attackers = getPossibleAttackers(possibleAttackers);
		blockers  = getPossibleBlockers(possibleBlockers);
		this.blockerLife = blockerLife;

		// todo: get rid of valuable
		final ArrayList<String> valuable = new ArrayList<String>();
		valuable.add("Kamahl, Pit Fighter");
		valuable.add("Elvish Piper");
		
		attackers = attackers.filter(new CardListFilter()
		{
		  public boolean addCard(Card c)
		  {
		    return (0 < getAttack(c) || c.getName().equals("Guiltfeeder")) && ! valuable.contains(c.getName());
		  }
		});
	}//constructor
       
	public CardList getPossibleAttackers(CardList in)
    {
      CardList list = new CardList(in.toArray());
      list = list.filter(new CardListFilter()
      {
        public boolean addCard(Card c) { return CombatUtil.canAttack(c); }
      });
      return list;
    }//getUntappedCreatures()
	
      public CardList getPossibleBlockers(CardList in)
      {
        CardList list = new CardList(in.toArray());
        list = list.filter(new CardListFilter()
        {
          public boolean addCard(Card c) { return c.isCreature() && CombatUtil.canBlock(c); }

        });
        return list;
      }//getUntappedCreatures()

       //this checks to make sure that the computer player
       //doesn't lose when the human player attacks
       //this method is used by getAttackers()
       public CardList notNeededAsBlockers(CardList attackers, Combat combat)
       {
    	  CardList notNeededAsBlockers = new CardList(attackers.toArray());
          CardListUtil.sortAttackLowFirst(attackers);
          int blockersNeeded = attackers.size();
          
          CardList list = getPossibleBlockers(attackers);
          
          for(int i = 0; i < list.size(); i++) {
             if(!doesHumanAttackAndWin(i)) {
                blockersNeeded= i;
                break;
             }
             else notNeededAsBlockers.remove(list.get(i));
          }
          
          if (blockersNeeded == list.size()) {
        	  // Human will win unless everything is kept back to block
        	  return notNeededAsBlockers;
          }
          
          // Increase the total number of blockers needed by 1 if Finest Hour in play
          // (human will get an extra first attack with a creature that untaps)
          // In addition, if the computer guesses it needs no blockers, make sure that
          // it won't be surprised by Exalted
          int humanExaltedBonus = countExaltedBonus(AllZone.HumanPlayer);

          if (humanExaltedBonus > 0) {
        	  int nFinestHours = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer, "Finest Hour").size();
        	  
        	  if ( (blockersNeeded == 0 || nFinestHours > 0) && humanList.size() > 0) {
        		  //
        		  // total attack = biggest creature + exalted, *2 if Rafiq is in play
        		  int humanBaseAttack = getAttack(humanList.get(0)) + humanExaltedBonus;
        		  if (nFinestHours > 0) {
        			  // For Finest Hour, one creature could attack and get the bonus TWICE
        			  humanBaseAttack = humanBaseAttack + humanExaltedBonus;
        		  }	
        		  int totalExaltedAttack = AllZoneUtil.isCardInPlay("Rafiq of the Many", AllZone.HumanPlayer) ? 
        				  2 * humanBaseAttack: humanBaseAttack;
        		  if ((AllZone.ComputerPlayer.getLife() - 3) <= totalExaltedAttack) {
        			  // We will lose if there is an Exalted attack -- keep one blocker
        			  if (blockersNeeded == 0 && notNeededAsBlockers.size() > 0)
        				  notNeededAsBlockers.remove(0);
        			  
        			  // Finest Hour allows a second Exalted attack: keep a blocker for that too
            		  if (nFinestHours > 0 && notNeededAsBlockers.size() > 0)
            			  notNeededAsBlockers.remove(0);
        		  }
        	  }
           }
          
          //re-add creatures with vigilance
          for (Card c:attackers)
          {
         	 if (c.getKeyword().contains("Vigilance"))
         		notNeededAsBlockers.add(c);
          }
         
          return notNeededAsBlockers;
       }

       //this uses a global variable, which isn't perfect
       public boolean doesHumanAttackAndWin(int nBlockingCreatures)
       {
          int totalAttack = 0;
          int stop = humanList.size() - nBlockingCreatures;

          for(int i = 0; i < stop; i++)
        	  totalAttack += getAttack(humanList.get(i));
                    
    	  //originally -3 so the computer will try to stay at 3 life
          //0 now to prevent the AI from not attacking when it's got low life
          //(seems to happen too often)
    	  return AllZone.ComputerPlayer.getLife() <= totalAttack;
       }

       private boolean doAssault()
       {
          //I think this is right but the assault code may still be a little off
          CardListUtil.sortAttackLowFirst(attackers);

          int totalAttack = 0;
          //presumes the Human will block
          for(int i = 0; i < (attackers.size() - blockers.size()); i++)
             totalAttack += getAttack(attackers.get(i));

          return blockerLife <= totalAttack;
       }//doAssault()

       public void chooseDefender(Combat c, boolean bAssault){
    	// TODO: split attackers to different planeswalker/human
    	   // AI will only attack one Defender per combat for now
    	   ArrayList<Object> defs = c.getDefenders();

    	   if (defs.size() == 1 || bAssault){
    		   c.setCurrentDefender(0);
    		   return;
    	   }
    	   
    	   // Randomnly determine who EVERYONE is attacking
    	   // would be better to determine more individually
    	   int n = MyRandom.random.nextInt(defs.size());
    	   c.setCurrentDefender(n);
    	   return;
       }
       
       
       public Combat getAttackers()
       {
          //if this method is called multiple times during a turn,
          //it will always return the same value
          //randomInt is used so that the computer doesn't always
          //do the same thing on turn 3 if he had the same creatures in play
          //I know this is a little confusing
          random.setSeed(AllZone.Phase.getTurn() + randomInt);

          Combat combat = new Combat();
          combat.setAttackingPlayer(AllZone.Combat.getAttackingPlayer());
          combat.setDefendingPlayer(AllZone.Combat.getDefendingPlayer());
          
          combat.setDefenders(AllZone.Combat.getDefenders());

          boolean bAssault = doAssault();
       // Determine who will be attacked
          chooseDefender(combat, bAssault);

          CardList attackersLeft = new CardList(attackers.toArray());
          
          //Atackers that don't really have a choice
          for (int i=0; i < attackersLeft.size(); i++)
          {
        	  Card attacker = attackersLeft.get(i);
             if ( (attacker.getKeyword().contains("CARDNAME attacks each turn if able.") 
            	   || attacker.getKeyword().contains("At the beginning of the end step, destroy CARDNAME.")
                   || attacker.getKeyword().contains("At the beginning of the end step, exile CARDNAME.")
                   || attacker.getKeyword().contains("At the beginning of the end step, sacrifice CARDNAME.")
                   || attacker.getSacrificeAtEOT()
            	   || attacker.getSirenAttackOrDestroy())
            	   && CombatUtil.canAttack(attacker, combat)) {
                combat.addAttacker(attacker);
                attackersLeft.remove(attacker);
             }
          }

           // *******************
           // start of edits
           // *******************

           int computerForces = 0;
           int playerForces = 0;
           int playerForcesForAttritionalAttack = 0;

           // examine the potential forces
           CardList nextTurnAttackers = new CardList();
           int candidateCounterAttackDamage = 0;
           int candidateTotalBlockDamage = 0;
           for(Card pCard:playerCreatures){
        	   
               // if the creature can attack next turn add it to counter attackers list
               if(CombatUtil.canAttackNextTurn(pCard)){
                   nextTurnAttackers.add(pCard);
                   if(pCard.getNetCombatDamage() > 0){
                       candidateCounterAttackDamage += pCard.getNetCombatDamage();
                       candidateTotalBlockDamage += pCard.getNetCombatDamage();
                       playerForces += 1; // player forces they might use to attack
                   }
               }
               // increment player forces that are relevant to an attritional attack - includes walls
               if(CombatUtil.canBlock(pCard)){playerForcesForAttritionalAttack += 1;}
           }

           // find the potential counter attacking damage compared to AI life total
           double aiLifeToPlayerDamageRatio = 1000000;
           if(candidateCounterAttackDamage > 0)
               aiLifeToPlayerDamageRatio = (double) AllZone.ComputerPlayer.life / candidateCounterAttackDamage;

           // get the potential damage and strength of the AI forces
           CardList candidateAttackers = new CardList();
           int candidateUnblockedDamage = 0;
           for(Card pCard:computerList){
               // if the creature can attack then it's a potential attacker this turn, assume summoning sickness creatures will be able to
               if(CombatUtil.canAttack(pCard) || pCard.isSick()){
                   boolean wasSick = false;
                   if(pCard.isSick()){
                       wasSick = true;
                       pCard.setSickness(false);
                   }
                   candidateAttackers.add(pCard);
                   if(pCard.getNetCombatDamage() > 0){
                       candidateUnblockedDamage += pCard.getNetCombatDamage();
                       computerForces += 1;
                   }
                   if(wasSick){pCard.setSickness(true);}
               }
           }

           // find the potential damage ratio the AI can cause
           double playerLifeToDamageRatio = 1000000;
           if(candidateUnblockedDamage > 0)
               playerLifeToDamageRatio = (double) AllZone.HumanPlayer.life / candidateUnblockedDamage;

           /*System.out.println(String.valueOf(aiLifeToPlayerDamageRatio) + " = ai life to player damage ratio");
           System.out.println(String.valueOf(playerLifeToDamageRatio) + " = player life ai player damage ratio");*/

           // determine if the ai outnumbers the player
           int outNumber = computerForces - playerForces;

           // compare the ratios, higher = better for ai
           double ratioDiff = aiLifeToPlayerDamageRatio - playerLifeToDamageRatio;
           /* System.out.println(String.valueOf(ratioDiff) + " = ratio difference, higher = better for ai");
           System.out.println(String.valueOf(outNumber) + " = outNumber, higher = better for ai"); */

           // *********************
           // if outnumber and superior ratio work out whether attritional all out attacking will work
           // attritional attack will expect some creatures to die but to achieve victory by sheer weight
           // of numbers attacking turn after turn. It's not calculate very carefully, the accuracy
           // can probably be improved
           // *********************
           boolean doAttritionalAttack = false;
           // get list of attackers ordered from low power to high
           CardListUtil.sortAttackLowFirst(attackers);
           // get player life total
           int playerLife = AllZone.HumanPlayer.life;
           // get the list of attackers up to the first blocked one
           CardList attritionalAttackers = new CardList();
           for(int x = 0; x < attackers.size() - playerForces; x++){
               attritionalAttackers.add(attackers.getCard(x));
           }
           // until the attackers are used up or the player would run out of life
           int attackRounds = 1;
           while(attritionalAttackers.size() > 0 && playerLife > 0){
               //   sum attacker damage
               int damageThisRound = 0;
               for(int y = 0; y < attritionalAttackers.size(); y++){
                  damageThisRound += attritionalAttackers.getCard(y).getNetCombatDamage();
               }
               //   remove from player life
               playerLife -= damageThisRound;
               //   shorten attacker list by the length of the blockers - assuming all blocked are killed for convenience
               for(int z = 0; z < playerForcesForAttritionalAttack; z++){
                   if(attritionalAttackers.size() > 0){
                        attritionalAttackers.remove(attritionalAttackers.size() - 1);
                   }
               }
               attackRounds += 1;
               if(playerLife <= 0){doAttritionalAttack = true;}
           }
           //System.out.println(doAttritionalAttack + " = do attritional attack");
           // *********************
           // end attritional attack calculation
           // *********************

           // *********************
           // see how long until unblockable attackers will be fatal
           // *********************
           double unblockableDamage = 0;
           double turnsUntilDeathByUnblockable = 0;
           boolean doUnblockableAttack = false;
           for(Card attacker:attackers){
               boolean isUnblockableCreature = true;
               // check blockers individually, as the bulk canBeBlocked doesn't check all circumstances
               for(Card blocker:blockers){
                    if(!blocker.isTapped() && CombatUtil.canBlock(attacker, blocker)){
                        isUnblockableCreature = false;
                    }
               }
               if(isUnblockableCreature){
                   unblockableDamage += attacker.getNetCombatDamage();
               }
           }
           if(unblockableDamage > 0){turnsUntilDeathByUnblockable = AllZone.HumanPlayer.life/unblockableDamage;}
           if(unblockableDamage > AllZone.HumanPlayer.life){doUnblockableAttack = true;}
           // *****************
           // end see how long until unblockable attackers will be fatal
           // *****************

           // decide on attack aggression based on a comparison of forces, life totals and other considerations
           // some bad "magic numbers" here, todo replace with nice descriptive variable names
           if((ratioDiff > 0 && doAttritionalAttack)){ // (playerLifeToDamageRatio <= 1 && ratioDiff >= 1 && outNumber > 0)  ||
               aiAggression = 5; // attack at all costs
            }else if((playerLifeToDamageRatio < 2 && ratioDiff >= 0) || ratioDiff > 3 || (ratioDiff > 0 && outNumber > 0)){
               aiAggression = 3;  // attack expecting to kill creatures or damage player.
            }  else if(ratioDiff >= 0 || ratioDiff + outNumber >= -1){ // at 0 ratio expect to potentially gain an advantage by attacking first
                // if the ai has a slight advantage
                // or the ai has a significant advantage numerically but only a slight disadvantage damage/life
                aiAggression = 2; // attack expecting to destroy creatures/be unblockable
            }else if(doUnblockableAttack || ((ratioDiff * -1) < turnsUntilDeathByUnblockable)){
                aiAggression = 4; // look for unblockable creatures that might be able to attack for a bit of
                // fatal damage even if the player is significantly better
            }else if(ratioDiff < 0 && aiLifeToPlayerDamageRatio > 1){
               // the player is overmatched but there are a few turns before death
                aiAggression = 2; // attack expecting to destroy creatures/be unblockable
            }else if(ratioDiff < 0){
                aiAggression = 1;
            } // stay at home to block
            System.out.println(String.valueOf(aiAggression) + " = ai aggression");

           // ****************
           // End of edits
           // ****************
          
          //Exalted
          if (combat.getAttackers().length == 0 && (countExaltedBonus(AllZone.ComputerPlayer) >= 3 ||
        		  AllZoneUtil.isCardInPlay("Rafiq of the Many", AllZone.ComputerPlayer) ||
                   AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer, "Battlegrace Angel").size() >= 2 ||
                   (AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer, "Finest Hour").size()>=1) &&
                   AllZone.Phase.isFirstCombat())
                   && !bAssault)
          {
             int biggest = 0;
             Card att = null;
             for(int i=0; i<attackersLeft.size();i++){
                if (getAttack(attackersLeft.get(i)) > biggest) {
                   biggest = getAttack(attackersLeft.get(i));
                   att = attackersLeft.get(i);
                }
             }
             if (att!= null && CombatUtil.canAttack(att, combat))
             combat.addAttacker(att);

              System.out.println("Exalted");
          }
          
          //do assault (all creatures attack) if the computer would win the game
          //or if the computer has 4 creatures and the player has 1
          else if(bAssault || (humanList.size() == 1 && 3 < attackers.size()))
          {
              System.out.println("Assault");
        	 CardListUtil.sortAttack(attackersLeft);
             for(int i = 0; i < attackersLeft.size(); i++)
            	 if (CombatUtil.canAttack(attackersLeft.get(i), combat)) combat.addAttacker(attackersLeft.get(i));
          }
          
          else
          {
              System.out.println("Normal attack");
             //so the biggest creature will usually attack
             //I think this works, not sure, may have to change it
             //sortNonFlyingFirst has to be done first, because it reverses everything
             CardListUtil.sortNonFlyingFirst(attackersLeft);
             CardListUtil.sortAttackLowFirst(attackersLeft);
             
             attackersLeft = notNeededAsBlockers(attackersLeft, combat);
                                     System.out.println(attackersLeft.size());
             
             for(int i = 0; i < attackersLeft.size(); i++)
             {
            	Card attacker = attackersLeft.get(i);
            	int totalFirstStrikeBlockPower = 0;
            	if (!attacker.hasFirstStrike() && !attacker.hasDoubleStrike())
            		 totalFirstStrikeBlockPower = CombatUtil.getTotalFirstStrikeBlockPower(attacker, AllZone.HumanPlayer);
        
                if ( shouldAttack(attacker,blockers, combat) &&	(totalFirstStrikeBlockPower < attacker.getKillDamage() || aiAggression == 5)
                		&& CombatUtil.canAttack(attacker, combat))
                   combat.addAttacker(attacker);
             }
          }//getAttackers()

          
          
    return combat;
  }//getAttackers()

   public int countExaltedBonus(Player player)
   {
      PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, player);
      CardList list = new CardList();
      list.addAll(play.getCards());
      list = list.filter(new CardListFilter(){
    	 public boolean addCard(Card c) {
    		 return c.getKeyword().contains("Exalted");
    	 }
	  });
  
      return list.size();
  }
    
   public int getAttack(Card c)
   {
      int n = c.getNetCombatDamage();

      if(c.getKeyword().contains("Double Strike"))
         n *= 2;

      return n;
   }   
    
    public boolean shouldAttack(Card attacker, CardList defenders, Combat combat)
    {
    	boolean canBeKilledByOne = false;
        boolean canKillOne = false;
    	boolean canKillAllDangerous = true; // indicates if the attacker can kill all creatures that are currently capable of dealing damage to it or the player
        boolean isWorthLessThanAllKillers = true;
    	CardList blockers = new CardList(); //all creatures that can block the attacker
    	CardList killingBlockers = new CardList(); //all creatures that can kill the attacker alone
        boolean canBeBlocked = false;
        boolean canCauseDamage = true;
        
        // ensure that this attacker can actually cause some damage
        if(attacker.getNetCombatDamage() <= 0){
        	canCauseDamage = false;
        }

        // look at the attacker in relation to the blockers to establish a number of factors about the attacking
        // context that will be relevant to the attackers decision according to the selected strategy
    	for (Card defender:defenders) {
    		if(CombatUtil.canBlock(attacker, defender)){ //, combat )) {
                canBeBlocked = true;
    			if(CombatUtil.canDestroyAttacker(attacker, defender)) {
    				canBeKilledByOne = true;  // there is a single creature on the battlefield that can kill the creature
    				killingBlockers.add(defender);
                    // see if the defending creature is of higher or lower value. We don't want to attack only to lose value
                    if(CardFactoryUtil.evaluateCreature(defender) <= CardFactoryUtil.evaluateCreature(attacker)){
                    	isWorthLessThanAllKillers = false;
                    } 
    			}
                // see if this attacking creature can destroy this defender, if not record that it can't kill everything
    			if(!CombatUtil.canDestroyBlocker(defender, attacker)){
                    // make exception for walls, they are usually extra tough but not dangerous unless they have
                    // high power, so check for walls that can kill but ignore others
                    if(!(defender.isWall() && defender.getNetCombatDamage() <= 0)){
                    	canKillAllDangerous = false; // there is a dangerous creature that can survive an attack from this creature
                    }
                } else {
                    canKillOne = true; // there is at least one opposition creature on the battlefield the creature can kill
                }
    			blockers.add(defender);
    		}
    	}

        // if the creature cannot block and can kill all opponents they might as well attack, they do nothing staying back
        if(canKillAllDangerous && !CombatUtil.canBlock(attacker) && isWorthLessThanAllKillers){
            System.out.println(attacker.getName() + " = attacking because they can't block, expecting to kill or damage player");
            return true;
        }

        // decide if the creature should attack based on the prevailing strategy choice in aiAggression
        switch(aiAggression){
            case 5: // all out attacking
            	if(canCauseDamage){
            		System.out.println(attacker.getName() + " = all out attacking");
                	return true;
            	}
            case 4: // unblockable creatures only
                if(!canBeBlocked && canCauseDamage){
                    System.out.println(attacker.getName() + " = attacking expecting not to be blocked");
                    return true;
                }
            case 3: // expecting to at least kill a creature of equal value, not be blocked or to attract a group block. A standard attacking strategy
                if(((canKillAllDangerous && isWorthLessThanAllKillers) || !canBeBlocked || !canBeKilledByOne) && canCauseDamage){
                    System.out.println(attacker.getName() + " = attacking expecting to kill creature or cause damage, or is unblockable");
                    return true;
                }
            case 2: // attack expecting to attract even a group block or destroying single blockers and surviving
                if(((canKillAllDangerous && !canBeKilledByOne) || !canBeBlocked) && canCauseDamage){
                    System.out.println(attacker.getName() + " = attacking expecting to survive or attract group block");
                    return true;
                }
        }
        return false; // don't attack
    }

    //
	public static Combat getAttackers(CardList attackerPermanents, CardList defenderPermanents) {

		Combat combat = new Combat();
		CardList attackerCreatures = attackerPermanents.getType("Creature");
		CardList attackersLeft = new CardList(); //keeps track of all undecided attackers
		CardList plannedBlockers = new CardList(); //creatures that should be held back to block

		CardList humanBlockers = new CardList();

		for(Card c:defenderPermanents) {
			if(c.isCreature() && CombatUtil.canBlock(c)) humanBlockers.add(c);
		}
		
		for(Card c:attackerCreatures) {
			if(CombatUtil.canAttack(c, combat)) attackersLeft.add(c);
			else if(CombatUtil.canBlock(c)) plannedBlockers.add(c);
		}

		return combat;
    }

 }



















