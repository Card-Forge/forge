package forge;
import java.util.*;

public class ComputerUtil_Block2
{
	/*
   //private CardList attackers;
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
      //attackers        = new CardList(in_attackers.toArray());
      possibleBlockers = getUntappedCreatures(new CardList(in_possibleBlockers.toArray()));

      //so the computer won't just have 1 life left
      //the computer will try to stay at 3 life
      blockersLife = in_blockersLife;

      
      //attackers.shuffle();
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
	         if(CombatUtil.canBlock(attacker, possibleBlockers.get(i)) && 
	        		 (possibleBlockers.get(i).hasFirstStrike() || possibleBlockers.get(i).hasDoubleStrike()))
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

   //finds a blocker, the attacker lives and blockers dies
   //returns null if no blocker is found
   Card chumpSingleBlock(Card attacker)
   {
	   /*
      if(blockersLife <= CardListUtil.sumAttack(attackers) + 3)
      {
         CardList c = getPossibleBlockers(attacker);
         return CardFactoryUtil.AI_getWorstCreature(c);
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
   /*
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
         shouldBlock = random.nextBoolean() || blockersLife <= sumUnblockedAttackers(combat) + 3;

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
   public static int getAttack(Card c)
   {
      int n = c.getNetAttack();

      if(CombatUtil.isDoranInPlay())
         n = c.getNetDefense();

      if(c.hasDoubleStrike())
         n *= 2;

      return n;
   }

   private static int sumAttack(CardList attackers)
   {
      int sum = 0;
	  for(int i = 0; i < attackers.size(); i++) {
		  Card a = attackers.get(i);
		  if (!a.hasKeyword("Infect")) sum += getAttack(a);
	  }

      return sum;
   }
   
   private static int sumPoison(CardList attackers)
   {
      int sum = 0;
	  for(int i = 0; i < attackers.size(); i++) {
		  Card a = attackers.get(i);
		  if (a.hasKeyword("Infect")) sum += getAttack(a);
		  if (a.hasKeyword("Poisonous")) sum += a.getKeywordMagnitude("Poisonous");
	  }

      return sum;
   }
   
   private static int anticipateDamage(Card attacker, Card blocker) {
	   int sum = 0;
	   
	   if (attacker.hasKeyword("Trample") && !attacker.hasKeyword("Infect") && getAttack(attacker) > blocker.getNetDefense())
			  sum = getAttack(attacker) - blocker.getNetDefense();
	   
	   return sum;
   }
   
   private static int anticipatePoison(Card attacker, Card blocker) {
	   int sum = 0;
	   
	   if (attacker.hasKeyword("Trample")  && getAttack(attacker) > blocker.getNetDefense()) {
		   if(attacker.hasKeyword("Infect")) sum = getAttack(attacker) - blocker.getNetDefense();
		   if(attacker.hasKeyword("Poisonous")) sum += attacker.getKeywordMagnitude("Poisonous");
	   }
	   
	   return sum;
   }
   */
   
   //finds the creatures able to block the attacker 
   private static CardList getPossibleBlockers(Card attacker, CardList blockersLeft) {
	  CardList blockers = new CardList();
	   
	  for(int i = 0; i < blockersLeft.size(); i++) {
		  Card b = blockersLeft.get(i);
		  if(CombatUtil.canBlock(attacker,b)) blockers.add(b);
	  }
		  
   return blockers;   
   }
   
   //finds blockers that won't be destroyed
   private static CardList getSafeBlockers(Card attacker, CardList blockersLeft) {
	  CardList blockers = new CardList();
	   
	  for(int i = 0; i < blockersLeft.size(); i++) {
		  Card b = blockersLeft.get(i);
		  if(!CombatUtil.canDestroyBlocker(b,attacker)) blockers.add(b);
	  }
	  
   return blockers;   
   }
   
   //finds blockers that destroy the attacker
   private static CardList getKillingBlockers(Card attacker, CardList blockersLeft) {
	  CardList blockers = new CardList();
	   
	  for(int i = 0; i < blockersLeft.size(); i++) {
		  Card b = blockersLeft.get(i);
		   if(CombatUtil.canDestroyAttacker(attacker,b)) blockers.add(b);
	  }
	   
   return blockers;   
   }
   
  public static Combat getBlockers(Combat originalCombat, CardList possibleBlockers) {
	  
	  Combat combat = originalCombat;
	  
	  CardList attackers = new CardList(combat.getAttackers());
      
	  if (attackers.size() == 0)
		  return combat;
	   
	  CardList attackersLeft = attackers; //keeps track of all currently unblocked attackers
	  CardList blockedButUnkilled = new CardList(); //keeps track of all blocked attackers that currently wouldn't be destroyed
	  CardList tramplingAttackers = new CardList();
	  CardList blockersLeft = possibleBlockers; //keeps track of all unassigned blockers
	  CardList blockers = new CardList();
	  CardList safeBlockers = new CardList();
	  CardList killingBlockers = new CardList();
	  CardList chumpBlockers = new CardList();
	  int diff = AllZone.ComputerPlayer.getLife() * 2 + 5; //This is the minimal gain for an unnecessary trade 
	  Card a = new Card();
	  Card b = new Card();
	  Card attacker = new Card();
	  Card blocker = new Card();
	  Card worst = new Card();
	   
	  // remove all attackers that can't be blocked anyway
	  for(int i = 0; i < attackers.size(); i++) {
		  a = attackers.get(i);
		  if(!CombatUtil.canBeBlocked(a)) { 
			  attackersLeft.remove(a);
		  }
	  }
	   
	  if (attackersLeft.size() == 0)
		  return combat;
	   
	  // remove all blockers that can't block anyway
	  for(int i = 0; i < possibleBlockers.size(); i++) {
		  b = possibleBlockers.get(i);
		  if(!CombatUtil.canBlock(b)) blockersLeft.remove(b);
	  }
	  
	  //These creatures won't prevent any damage
	  if (CombatUtil.lifeInDanger(combat)) 
		  blockersLeft = blockersLeft.getNotKeyword("Whenever CARDNAME is dealt damage, you lose that much life.");
	   
	  if (blockersLeft.size() == 0)
		  return combat;
	   
	  //Begin with the attackers that pose the biggest thread
	  CardListUtil.sortAttack(attackersLeft);
	  
	  //Begin with the weakest blockers
	  CardListUtil.sortAttackLowFirst(blockersLeft);
	   
	  CardList currentAttackers = attackersLeft;

	  //first choose good blocks only
	  for(int i = 0; i < attackersLeft.size(); i++) {
		  attacker = attackersLeft.get(i);
		  
		  blocker = new Card();
		  
		  blockers = getPossibleBlockers(attacker, blockersLeft);
		   
		  safeBlockers = getSafeBlockers(attacker, blockers);
		   
		  if(safeBlockers.size() > 0) {
			  // 1.Blockers that can destroy the attacker but won't get destroyed 
			  killingBlockers = getKillingBlockers(attacker, safeBlockers);
			  if(killingBlockers.size() > 0) blocker = CardFactoryUtil.AI_getWorstCreature(killingBlockers);

			  // 2.Blockers that won't get destroyed 
			  else {
				  blocker = CardFactoryUtil.AI_getWorstCreature(safeBlockers);
				  blockedButUnkilled.add(attacker);
			  }
		  } // no safe blockers
		  else {
			  killingBlockers = getKillingBlockers(attacker, blockers);
			  if(killingBlockers.size() > 0) {
				  // 3.Blockers that can destroy the attacker and are worth less
				  worst = CardFactoryUtil.AI_getWorstCreature(killingBlockers);
				  
				  if(CardFactoryUtil.evaluateCreature(worst) + diff < CardFactoryUtil.evaluateCreature(attacker)) {
					  blocker = worst;
				  }
			  // TODO: 4.good Gangblocks 
			  }
		  }
		  if(blocker.getName() != "") {
			  currentAttackers.remove(attacker);
			  blockersLeft.remove(blocker);
			  combat.addBlocker(attacker, blocker);
		  }
	  }
	   
	  attackersLeft = currentAttackers;
	  
	  if(blockersLeft.size() == 0) return combat;
	  
	  //choose necessary trade blocks if life is in danger
	  if (CombatUtil.lifeInDanger(combat))
		  for(int i = 0; i < attackersLeft.size(); i++) {
		  	  attacker = attackersLeft.get(i);
			  killingBlockers = getKillingBlockers(attacker, getPossibleBlockers(attacker, blockersLeft));
			  if(killingBlockers.size() > 0) {
				  blocker = CardFactoryUtil.AI_getWorstCreature(killingBlockers);
				  combat.addBlocker(attacker, blocker);
				  currentAttackers.remove(attacker);
				  blockersLeft.remove(blocker);
			  }
		  }
	   
	  attackersLeft = currentAttackers;
	   
	  //choose necessary chump blocks if life is still in danger
	  if (CombatUtil.lifeInDanger(combat))
		  for(int i = 0; i < attackersLeft.size(); i++) {
			  attacker = attackersLeft.get(i);	  
			  chumpBlockers = getPossibleBlockers(attacker, blockersLeft);
			  if(chumpBlockers.size() > 0) {
				  blocker = CardFactoryUtil.AI_getWorstCreature(chumpBlockers);
				  combat.addBlocker(attacker, blocker);
				  currentAttackers.remove(attacker);
				  blockedButUnkilled.add(attacker);
				  blockersLeft.remove(blocker);
			  }
		  }
	  
	 attackersLeft = currentAttackers; 
	  
	 //Reinforce blockers blocking attackers with trample if life is still in danger
	  if (CombatUtil.lifeInDanger(combat)) {
		  tramplingAttackers = attackers.getKeyword("Trample");
		  tramplingAttackers = tramplingAttackers.getKeywordsDontContain("Rampage"); 	//Don't make it worse 
		  for(int i = 0; i < tramplingAttackers.size(); i++) {
			  attacker = tramplingAttackers.get(i);
			  chumpBlockers = getPossibleBlockers(attacker, blockersLeft);
			  for(int j = 0; j < chumpBlockers.size(); j++) {
				  blocker = chumpBlockers.get(j);
			  	  //Add an additional blocker if the current blockers are not enough and the new one would suck some of the damage
			  	  if(CombatUtil.getAttack(attacker) > CombatUtil.totalShieldDamage(attacker,combat.getBlockers(attacker)) 
			  			  && CombatUtil.shieldDamage(attacker, blocker) > 0) {
			  		  combat.addBlocker(attacker, blocker);
			  		  blockersLeft.remove(blocker);
				  }
			  }
		  }
	  }
	  
	  //Support blockers not destroying the attacker with more blockers to try to kill the attacker
	  if (blockedButUnkilled.size() > 0) {
		  CardList targetAttackers = blockedButUnkilled.getKeywordsDontContain("Rampage"); 	//Don't make it worse 
		  for(int i = 0; i < targetAttackers.size(); i++) {
			  attacker = targetAttackers.get(i);
			  blockers = getPossibleBlockers(attacker, blockersLeft);
			  
			  //Try to use safe blockers first
			  safeBlockers = getSafeBlockers(attacker, blockers);
			  for(int j = 0; j < safeBlockers.size(); j++) {
				  blocker = safeBlockers.get(j);
			  	  //Add an additional blocker if the current blockers are not enough and the new one would deal additional damage
			  	  if(attacker.getKillDamage() > CombatUtil.totalDamageOfBlockers(attacker,combat.getBlockers(attacker)) 
			  			  && CombatUtil.dealsDamageAsBlocker(attacker, blocker) > 0) {
			  		  combat.addBlocker(attacker, blocker);
			  		  blockersLeft.remove(blocker);
			  	  }
			  	  blockers.remove(blocker); //Don't check them again next
			  }
			  
			  //Try to add blockers that could be destroyed, but are worth less than the attacker
			  //Don't use blockers without First Strike or Double Strike if attacker has it
			  if (attacker.hasKeyword("First Strike") || attacker.hasKeyword("Double Strike")) {
				  safeBlockers = blockers.getKeyword("First Strike");
				  safeBlockers.addAll(blockers.getKeyword("Double Strike").toArray());
			  }
			  else safeBlockers = blockers;
			  
			  for(int j = 0; j < safeBlockers.size(); j++) {
				  blocker = safeBlockers.get(j);
			  	  //Add an additional blocker if the current blockers are not enough and the new one would deal the remaining damage
				  int currentDamage = CombatUtil.totalDamageOfBlockers(attacker,combat.getBlockers(attacker));
				  int additionalDamage = CombatUtil.dealsDamageAsBlocker(attacker, blocker);
			  	  if(attacker.getKillDamage() > currentDamage 
			  			  && !(attacker.getKillDamage() > currentDamage + additionalDamage)
			  			  && CardFactoryUtil.evaluateCreature(blocker) + diff < CardFactoryUtil.evaluateCreature(attacker)) {
			  		  combat.addBlocker(attacker, blocker);
			  		  blockersLeft.remove(blocker);
				  }
			  }
		  }
	  }
	   
	 return combat;
   }
}