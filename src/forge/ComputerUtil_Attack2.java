package forge;
import java.util.*;

    //doesHumanAttackAndWin() uses the global variable AllZone.ComputerPlayer
    public class ComputerUtil_Attack2
    {
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

          attackers = getUntappedCreatures(possibleAttackers, true);
          blockers  = getUntappedCreatures(possibleBlockers , false);
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
       
      public CardList getUntappedCreatures(CardList in, final boolean checkCanAttack)
      {
        CardList list = new CardList(in.toArray());
        list = list.filter(new CardListFilter()
        {
          public boolean addCard(Card c)
          {
            boolean b = c.isCreature() && c.isUntapped();

            if(checkCanAttack)
            	return b && CombatUtil.canAttack(c);

            return b;
          }
        });
        return list;
      }//getUntappedCreatures()

       //this checks to make sure that the computer player
       //doesn't lose when the human player attacks
       //this method is used by getAttackers()
       public int getStartIndex()
       {
          CardListUtil.sortAttack(humanList);
          int blockersNeeded = computerList.size();
          
          CardList list = computerList;
          
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

          for (int i=0; i<attackers.size();i++)
          {
             if ( attackers.get(i).getKeyword().contains("CARDNAME attacks each turn if able.") 
            	   || attackers.get(i).getKeyword().contains("At the beginning of the end step, destroy CARDNAME.")
                   || attackers.get(i).getKeyword().contains("At the beginning of the end step, exile CARDNAME.")
                   || attackers.get(i).getKeyword().contains("At the beginning of the end step, sacrifice CARDNAME.")
            	   || attackers.get(i).getSirenAttackOrDestroy())
                combat.addAttacker(attackers.get(i));
          }

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
             if (att!= null)
             combat.addAttacker(att);
          }
          //do assault (all creatures attack) if the computer would win the game
          //or if the computer has 4 creatures and the player has 1
          else if(doAssault() || (humanList.size() == 1 && 3 < attackers.size()))
          {
             for(int i = 0; i < attackers.size(); i++)
                combat.addAttacker(attackers.get(i));
          }
          else
          {
             Card bigDef;
             Card bigAtt;

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
             int i = getStartIndex(); //new

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

            	 bigAtt = getBiggestAttack(attackers.get(i));
            	 bigDef = getBiggestDefense(attackers.get(i));
        
		        /*
		        System.out.println("bigDef: " + bigDef.getName());
		        System.out.println("attackers.get(i): " + attackers.get(i).getName());
		        if (CombatUtil.canDestroyBlocker(bigDef, attackers.get(i)))
		        	System.out.println(attackers.get(i).getName() + " can destroy blocker " +bigDef.getName());
		         */
            	 
            	 int totalFirstStrikeBlockPower = 0;
            	 if (!attackers.get(i).hasFirstStrike() && !attackers.get(i).hasDoubleStrike())
            		 totalFirstStrikeBlockPower = CombatUtil.getTotalFirstStrikeBlockPower(attackers.get(i), AllZone.HumanPlayer);
        
                //if attacker can destroy biggest blocker or
                //biggest blocker cannot destroy attacker
            	if (bigDef == null) {
            		combat.addAttacker(attackers.get(i));
            	}
            	else if ( (CombatUtil.canDestroyBlocker(bigDef, attackers.get(i)) ||
            			( bigAtt != null && !CombatUtil.canDestroyAttacker(attackers.get(i), bigAtt) ) ) &&
            			totalFirstStrikeBlockPower < attackers.get(i).getKillDamage() ){
                   combat.addAttacker(attackers.get(i));
                }
                else if(attackers.get(i).getSacrificeAtEOT()){
                   combat.addAttacker(attackers.get(i));
                }
             }
          }//getAttackers()

    return combat;
  }//getAttackers()

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
  }

       public int countExaltedBonus(Player player)
       {
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
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
    }

    /*   
       //this returns the attacking power that is needed to destroy
       //Card c and takes into account first and double strike
       //
       //Doran, the Siege Tower doesn't change a card's defense
       //used by sortDefense()
       private int getDefense(Card c)
       {
          int n = c.getNetDefense();

          //is the defense is less than attack and the card has
          //first or double strike?
          if(hasStrike(c) && n < getAttack_FirstStrike(c))
              n = getAttack_FirstStrike(c);
          
          return n;
       }   
       
       //does this card have first or double strike?
       private boolean hasStrike(Card c)
       {
          return c.getKeyword.contains("First Strike") ||
                 c.getKeyword.contains("Double Strike")
       }
       
       //the higher the defense the better
       @SuppressWarnings("unchecked") // Comparator needs <type>
       private void sortDefense(CardList list)
       {
          Comparator com = new Comparator()
          {
             public int compare(Object a1, Object b1)
             {
                Card a = (Card)a1;
                Card b = (Card)b1;

                return getDefense(b) - getDefense(a);
             }
          };
          list.sort(com);
       }//sortDefense()
       
       //use this method if you need to know about first strike
       public int getAttack_FirstStrike(Card c)
       {
          int n = getAttack(c);
       
          //adding 1 is a little bit hacky bit it shows
          //that first strike is a little better
          //than a average creature
          if(c.getKeyword().contains("First Strike"))
             n += 1;
       
          return n;
       }

       //returns lowest attack first
       private void sortAttackLowFirst(CardList list)
       {
          sortAttack(list);
          list.reverse();
          return list;
       }

       //the higher the attack the better
       @SuppressWarnings("unchecked") // Comparator needs type
       private void sortAttack(CardList list)
       {
          Comparator com = new Comparator()
          {
             public int compare(Object a1, Object b1)
             {
                Card a = (Card)a1;
                Card b = (Card)b1;

                return getAttack_FirstStrike(b) - getAttack_FirstStrike(a);
             }
          };
          list.sort(com);
       }//sortAttack() 
    */
