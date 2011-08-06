package forge;
import java.util.*;

//doesHumanAttackAndWin() uses the global variable AllZone.ComputerPlayer
public class ComputerUtil_Attack2 {
	
//possible attackers and blockers
	private CardList attackers;
	private CardList blockers;
	private int blockerLife;
	
	private Random random = new Random();
	private final int randomInt = random.nextInt();
	
	private CardList humanList;   //holds human player creatures
	private CardList computerList;//holds computer creatures
	
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
		
		attackers = getPossibleAttackers(possibleAttackers);
		blockers  = getPossibleBlockers(possibleBlockers);
		this.blockerLife = blockerLife;
		
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
        public boolean addCard(Card c) { return c.isCreature() && CombatUtil.canAttack(c); }
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
       public int blockersNeeded(Combat combat)
       {
          CardListUtil.sortAttack(humanList);
          int blockersNeeded = computerList.size();
          
          CardList list = getPossibleBlockers(computerList);
          
          for(int i = 0; i < list.size(); i++) {
             if(!doesHumanAttackAndWin(i)) {
                blockersNeeded= i;
                break;
             }
          }
          
          if (blockersNeeded == list.size()) {
        	  // Human will win unless everything is kept back to block
        	  return blockersNeeded;
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
        			  if (blockersNeeded == 0)
        				  blockersNeeded++;
        			  
        			  // Finest Hour allows a second Exalted attack: keep a blocker for that too
            		  if (nFinestHours > 0)
            			  blockersNeeded++;
        		  }
        	  }
           }
         
          if (blockersNeeded > list.size())
        	  blockersNeeded = list.size();
          return blockersNeeded;
       }

       //this uses a global variable, which isn't perfect
       public boolean doesHumanAttackAndWin(int nBlockingCreatures)
       {
          int totalAttack = 0;
          int stop = humanList.size() - nBlockingCreatures;

          for(int i = 0; i < stop; i++)
        	  totalAttack += getAttack(humanList.get(i));
                    
    	  //originally -3 so the computer will try to stay at 3 life
          //+1 now to prevent the AI from not attacking when it's got low life
          //(seems to happen too often)
    	  return (AllZone.ComputerPlayer.getLife() + 1) <= totalAttack;
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

       public Combat getAttackers()
       {
          //if this method is called multiple times during a turn,
          //it will always return the same value
          //randomInt is used so that the computer doesn't always
          //do the same thing on turn 3 if he had the same creatures in play
          //I know this is a little confusing
          random.setSeed(AllZone.Phase.getTurn() + randomInt);

          Combat combat = new Combat();
          
          //Atackers that don't really have a choice
          for (int i=0; i<attackers.size();i++)
          {
             if ( (attackers.get(i).getKeyword().contains("CARDNAME attacks each turn if able.") 
            	   || attackers.get(i).getKeyword().contains("At the beginning of the end step, destroy CARDNAME.")
                   || attackers.get(i).getKeyword().contains("At the beginning of the end step, exile CARDNAME.")
                   || attackers.get(i).getKeyword().contains("At the beginning of the end step, sacrifice CARDNAME.")
                   || attackers.get(i).getSacrificeAtEOT()
            	   || attackers.get(i).getSirenAttackOrDestroy())
            	   && CombatUtil.canAttack(attackers.get(i), combat))
                combat.addAttacker(attackers.get(i));
          }
          
          //Exalted
          if (combat.getAttackers().length == 0 && (countExaltedBonus(AllZone.ComputerPlayer) >= 3 ||
        		  AllZoneUtil.isCardInPlay("Rafiq of the Many", AllZone.ComputerPlayer) ||
                   AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer, "Battlegrace Angel").size() >= 2 ||
                   (AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer, "Finest Hour").size()>=1) && AllZone.Phase.isFirstCombat())
                   && !doAssault())
          {
             int biggest = 0;
             Card att = null;
             for(int i=0; i<attackers.size();i++){
                if (getAttack(attackers.get(i)) > biggest) {
                   biggest = getAttack(attackers.get(i));
                   att = attackers.get(i);
                }
             }
             if (att!= null && CombatUtil.canAttack(att, combat))
             combat.addAttacker(att);
          }
          
          //do assault (all creatures attack) if the computer would win the game
          //or if the computer has 4 creatures and the player has 1
          else if(doAssault() || (humanList.size() == 1 && 3 < attackers.size()))
          {
        	 CardListUtil.sortAttack(attackers);
             for(int i = 0; i < attackers.size(); i++)
            	 if (CombatUtil.canAttack(attackers.get(i), combat)) combat.addAttacker(attackers.get(i));
          }
          
          else
          {
             //should the computer randomly not attack with one attacker?
             //this should only happen 10% of the time when the computer
             //has at least 3 creatures
             boolean notAttack = (Math.abs(random.nextInt(100)) <= 10);
             if(notAttack && 3 <= attackers.size())
             {
                attackers.shuffle();
                attackers.remove(0);
             }

             //this has to be before the sorting below
             //because this sorts attackers
             int i = blockersNeeded(combat); //new

             //so the biggest creature will usually attack
             //I think this works, not sure, may have to change it
             //sortNonFlyingFirst has to be done first, because it reverses everything
             CardListUtil.sortNonFlyingFirst(attackers);
             CardListUtil.sortAttackLowFirst(attackers);

             for (Card c:attackers)
             {
            	 if (c.getKeyword().contains("Vigilance"))
            		 i--;
             }
             if (i < 0)
            	 i = 0;
             
             for(; i < attackers.size(); i++)
             {
            	int totalFirstStrikeBlockPower = 0;
            	if (!attackers.get(i).hasFirstStrike() && !attackers.get(i).hasDoubleStrike())
            		 totalFirstStrikeBlockPower = CombatUtil.getTotalFirstStrikeBlockPower(attackers.get(i), AllZone.HumanPlayer);
        
                if ( shouldAttack(attackers.get(i),blockers, combat) &&	totalFirstStrikeBlockPower < attackers.get(i).getKillDamage() 
                		&& CombatUtil.canAttack(attackers.get(i), combat))
                   combat.addAttacker(attackers.get(i));
                
             }
          }//getAttackers()

    return combat;
  }//getAttackers()

  /*
  //returns null if no blockers found
  public Card getBiggestAttack(Card attack)
  {
    CardListUtil.sortAttack(blockers);
    for(int i = 0; i < blockers.size(); i++)
      if(CombatUtil.canBlock(attack, blockers.get(i)))
        return blockers.get(i);

    return null;
  }

  	//returns null if no blockers found
	public Card getBiggestDefense(Card attack)
	{
		CardListUtil.sortDefense(blockers);
		for(int i = 0; i < blockers.size(); i++)
		  if(CombatUtil.canBlock(attack, blockers.get(i)))
		    return blockers.get(i);
		
		return null;
	}*/

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
      int n = c.getNetAttack();

      if(CombatUtil.isDoranInPlay())
         n = c.getNetDefense();

      if(c.getKeyword().contains("Double Strike"))
         n *= 2;

      return n;
   }   
    
    public boolean shouldAttack(Card attacker, CardList defenders, Combat combat)
    {
    	boolean canBeKilledByOne = false;
    	boolean canKillAll = true;
    	CardList blockers = new CardList(); //all creatures that can block the attacker
    	CardList killingBlockers = new CardList(); //all creatures that can kill the attacker alone
    	
    	for (Card defender:defenders) {
    		if(CombatUtil.canBlock(attacker, defender, combat)) {
    			if(CombatUtil.canDestroyAttacker(attacker, defender)) {
    				canBeKilledByOne = true;
    				killingBlockers.add(defender);
    			}
    			if(!CombatUtil.canDestroyBlocker(defender, attacker)) canKillAll = false;
    			blockers.add(defender);
    		}
    	}
    	// A creature should attack if it can't be killed
    	if (CombatUtil.totalDamageOfBlockers(attacker, blockers) < attacker.getKillDamage()) return true; 
    	
    	return (canKillAll && !canBeKilledByOne); // A creature should attack if it can't be killed or can kill any blocker
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
