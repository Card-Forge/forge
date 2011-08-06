package forge;
import java.util.*;

public class ComputerUtil_Block2
{
   private CardList attackers;
   private CardList possibleBlockers;
   private int blockersLife;

   //making this static helped in testing
   //random.nextBoolean() wasn't true 50% of the time
   private static Random random = new Random();
   private final int randomInt = random.nextInt();

   public ComputerUtil_Block2(Card[] attackers, Card[] possibleBlockers, int blockerLife)
   {
      this(new CardList(attackers), new CardList(possibleBlockers), blockerLife);
   }

   public ComputerUtil_Block2(CardList in_attackers, CardList in_possibleBlockers, int in_blockersLife)
   {
      attackers        = new CardList(in_attackers.toArray());
      possibleBlockers = getUntappedCreatures(new CardList(in_possibleBlockers.toArray()));

      //so the computer won't just have 1 life left
      //the computer will try to stay at 3 life
      blockersLife = in_blockersLife;
      if(3 < blockersLife)
         blockersLife -= 3;;
      
      attackers.shuffle();
      CardListUtil.sortAttackLowFirst(possibleBlockers);

      possibleBlockers = removeValuableBlockers(possibleBlockers);
   }
   private CardList getUntappedCreatures(CardList list)
   {
      list = list.filter(new CardListFilter()
      {
         public boolean addCard(Card c)
         {
            return c.isCreature() && c.isUntapped();
         }
      });
      return list;
   }//getUntappedCreatures()

   private CardList removeValuableBlockers(CardList in)
   {
      final String[] noBlock = {"Elvish Piper", "Urborg Syphon-Mage", "Sparksmith", "Royal Assassin", "Marble Titan", "Kamahl, Pit Fighter"};

      CardList out = in.filter(new CardListFilter()
      {
         public boolean addCard(Card c)
         {
            for(int i = 0; i < noBlock.length; i++)
            if(c.getName().equals(noBlock[i]))
            return false;

            return true;
         }
      });//CardListFilter

      return out;
   }

   //checks for flying and stuff like that
   private CardList getPossibleBlockers(Card attacker)
   {
      CardList list = new CardList();
      for(int i = 0; i < possibleBlockers.size(); i++)
         if(CombatUtil.canBlock(attacker, possibleBlockers.get(i)))
            list.add(possibleBlockers.get(i));

      return list;
   }
   
   private CardList getPossibleBlockersWithFirstStrike(Card attacker)
   {
	   CardList list = new CardList();
	      for(int i = 0; i < possibleBlockers.size(); i++)
	         if(CombatUtil.canBlock(attacker, possibleBlockers.get(i)) && (attacker.hasFirstStrike() || attacker.hasDoubleStrike()))
	            list.add(possibleBlockers.get(i));

	   return list;
   }

   //finds a blocker that destroys the attacker, the blocker is not destroyed
   //returns null if no blocker is found
   Card safeSingleBlock(Card attacker)
   {
      CardList c = getPossibleBlockers(attacker);

      for(int i = 0; i < c.size(); i++)
    	  if(CombatUtil.canDestroyAttacker(attacker, c.get(i)) &&
            (! CombatUtil.canDestroyBlocker(c.get(i), attacker)))
    		  return c.get(i);

      return null;
   }//safeSingleBlock()

   //finds a blocker, both the attacker and blocker are destroyed
   //returns null if no blocker is found
   Card tradeDownSingleBlock(Card attacker)
   {
      CardList c = getPossibleBlockers(attacker);

      for(int i = 0; i < c.size(); i++)
	      if(CombatUtil.canDestroyAttacker(attacker, c.get(i)))
	      {
	         //do not block a non-flyer with a flyer
	         if((! c.get(i).getKeyword().contains("Flying")) || attacker.getKeyword().contains("Flying"))
	        	 return c.get(i);
	      }
      return null;
   }//tradeSingleBlock()

   Card tradeUpSingleBlock(Card attacker)
   {
      CardList c = getPossibleBlockers(attacker);

      for(int i = 0; i < c.size(); i++){
    	  Card defender = c.get(i);
	      if(CombatUtil.canDestroyAttacker(attacker, defender))
	      {
	    	  // If Attacker has a higher Evaluation then defender, trade up!
	    	  if (CardFactoryUtil.evaluateCreature(defender) <= CardFactoryUtil.evaluateCreature(attacker))
	    		  return defender;
	      }
      }
      return null;
   }//tradeSingleBlock()
   

   //finds a blocker, neither attacker and blocker are destroyed
   //returns null if no blocker is found
   Card shieldSingleBlock(Card attacker)
   {
      CardList c = getPossibleBlockers(attacker);

      for(int i = 0; i < c.size(); i++)
	      if(! CombatUtil.canDestroyBlocker(c.get(i), attacker))
	    	  return c.get(i);

      return null;
   }//shieldSingleBlock()


   //finds multiple blockers
   //returns an array of size 0 if not multiple blocking
   Card[] multipleBlock(Card attacker)
   {
	  CardList c;
	  if (attacker.hasDoubleStrike() || attacker.hasFirstStrike())
		 c = getPossibleBlockersWithFirstStrike(attacker); 
	  else
		  c = getPossibleBlockers(attacker);

      int defense = getDefense(attacker) - attacker.getDamage();
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

   Card[] multipleTrampleBlock(Card attacker)
   {
	  CardList c;
	  if (attacker.hasDoubleStrike() || attacker.hasFirstStrike())
		 c = getPossibleBlockersWithFirstStrike(attacker); 
	  else
		  c = getPossibleBlockers(attacker);

      int attack = getAttack(attacker);
      //if computer can't save himself:
      if(attack > (CardListUtil.sumDefense(c) + AllZone.ComputerPlayer.getLife() ))
    	  return new Card[] {};
		
      CardList block = new CardList();
      c.shuffle();

      while(attack >= (CardListUtil.sumDefense(block) + AllZone.ComputerPlayer.getLife() )
    		&& c.size() != 0)
    	  block.add(c.remove(0));

      Card check[] = block.toArray();

      //no single blockers, that should be handled somewhere else
      if(check.length == 1)
    	  return new Card[] {};

      return check; 
   }

   //finds a blocker, both the attacker lives and blockers dies
   //returns null if no blocker is found
   Card chumpSingleBlock(Card attacker)
   {
      if(blockersLife <= CardListUtil.sumAttack(attackers))
      {
         CardList c = getPossibleBlockers(attacker);
         CardListUtil.sortAttackLowFirst(c);
         if(! c.isEmpty())
            return c.get(0);
      }
      return null;
   }//tradeSingleBlock()

   //force block if lethal damage somehow slips through checks (happens often with doublestrike)
   Card forceBlock(Card attacker)
   {
      CardList c = getPossibleBlockers(attacker);
      CardListUtil.sortAttackLowFirst(c);
      
      if(! c.isEmpty())
         return c.get(0);
      return null;
   }


   private void testing(String s)
   {
      boolean print = false;
      if(print)
         System.out.println("combat testing: " +s);
   }


   private int sumUnblockedAttackers(Combat combat)
   {
      int sum = 0;
      Card c[] = combat.getAttackers();
      for(int i = 0; i < c.length; i++)
         if(! combat.isBlocked(c[i]))
            sum += getAttack(c[i]);

      return sum;
   }

   public Combat getBlockers()
   {
	   // TODO: this function fucking sucks
	   
	  Combat combat = new Combat();
	  if (attackers.size() == 0)
		  return combat;
	   
	  // Random seed set with time, turn and a randomInt. Should be random enough
      random.setSeed(AllZone.Phase.getTurn() + System.currentTimeMillis() + randomInt );

      boolean shouldBlock;

      //if this isn't done, the attackers are shown backwards 3,2,1,
      //reverse the order here because below reverses the order again
      //attackers.reverse();

      //add attackers to combat
      //this has to be done before the code below because sumUnblockedAttackers()
      //uses combat to get the attackers and total their power
      for(int i  = 0; i < attackers.size(); i++)
         combat.addAttacker(attackers.get(i));   

      for(int i = 0; i < attackers.size(); i++)
      {
    	  Card c = null;
    	  Card attack = attackers.get(i);

         boolean doubleStrike = attack.hasDoubleStrike();
         boolean trample = attack.getKeyword().contains("Trample");

         //the computer blocks 50% of the time or if the computer would lose the game
         shouldBlock = random.nextBoolean() || blockersLife <= sumUnblockedAttackers(combat);

         testing("shouldBlock - " +shouldBlock);

         //System.out.println("Computer checking to block: "+attack.getName());
         //Lure
         if(attack.isEnchantedBy("Lure")) {
        	 for(int j = 0; j < possibleBlockers.size(); j++) {
        		 Card blocker = possibleBlockers.get(j);
        		 if(CombatUtil.canBlock(attack, blocker)) {
        			 possibleBlockers.remove(blocker);
        			 combat.addBlocker(attack, blocker);
        		 }
        	 }
         }


         //safe block - attacker dies, blocker lives
         //if there is only one attacker it might be a trap
         //if holding a Giant Growth and a 2/2 attacking into a 3/3
         Random random = new Random();
         int randomInt = random.nextInt(100);
         
         //multiblocks for trample
         if (trample && AllZone.ComputerPlayer.getLife() <= getAttack(attack) )
         {
        	 // If attacker has trample, block with a few creatures to prevent damage to player
        	 Card[] m = multipleTrampleBlock(attack);
             for(int inner = 0; inner < m.length; inner++)
             {
                if(m.length != 1)
                {
                   possibleBlockers.remove(m[inner]);
                   combat.addBlocker(attack, m[inner]);
                }
             }//for
             continue;
         }
         
         do{	// inside a do-while "loop", so we can break out of it
	         if (randomInt >= 10)
	         { 	 
	        	 // Safe Block - Attacker dies, blocker lives
		         c = safeSingleBlock(attack);
		         if(c != null)
		        	 break;
	         }
	         if(randomInt >= 15)
	         {
	            //shield block - attacker lives, blocker lives
	            c = shieldSingleBlock(attack);
	            if(c != null)
	            	break;
	         }
	
	         if(randomInt >= 20)
	         {
	            //trade block - attacker dies, blocker dies
	            c = tradeUpSingleBlock(attack);
	
	            if(c != null)
	            	break;
	         }
	         
	         if(randomInt >= 25)
	         {
	            //trade block - attacker dies, blocker dies
	            c = tradeDownSingleBlock(attack);
	
	            if(c != null)
	            	break;
	         }
	
	         if(shouldBlock)
	         {
	            //chump block - attacker lives, blocker dies
	            c = chumpSingleBlock(attack);
	            if(c != null)
	            	break;
	         }
	
	         if(doubleStrike && AllZone.ComputerPlayer.getLife() <= (getAttack(attack)*2))
	         {
	            c = forceBlock(attack);
	            if (c != null)
	            	break;
	         }
         }while(false);
         
         if(c != null)
         {
        	 // TODO: creatures that can block more than one don't necessarily get removed
			possibleBlockers.remove(c);
			combat.addBlocker(attack, c);
         }

         //multiple blockers
         else if(shouldBlock)
         {
            Card[] m = multipleBlock(attack);
            for(int inner = 0; inner < m.length; inner++)
            {
               //to prevent a single flyer from blocking a single non-flyer
               //tradeSingleBlock() checks for a flyer blocking a non-flyer also
               if(m.length != 1)
               {
                  possibleBlockers.remove(m[inner]);
                  combat.addBlocker(attack, m[inner]);
               }
            }//for
         }//if
         else{

         }
      }//for attackers

      return combat;
   }//getBlockers()

   //Doran, the Siege Tower doesn't change a card's defense
   public int getDefense(Card c)
   {
      return c.getNetDefense();
   }

   //this doesn't take into account first strike,
   //use CombatUtil.canDestroyAttacker()
   public int getAttack(Card c)
   {
      int n = c.getNetAttack();

      if(CombatUtil.isDoranInPlay())
         n = c.getNetDefense();

      if(c.hasDoubleStrike())
         n *= 2;

      return n;
   }
}