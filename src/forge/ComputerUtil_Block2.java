
package forge;

import java.util.ArrayList;

import forge.card.cardFactory.CardFactoryUtil;


public class ComputerUtil_Block2
{
   
   //finds the creatures able to block the attacker 
   private static CardList getPossibleBlockers(Card attacker, CardList blockersLeft, Combat combat) {
	  CardList blockers = new CardList();
	  
	  for(Card blocker : blockersLeft) {
		  //if the blocker can block a creature with lure it can't block a creature without 
		  if(CombatUtil.canBlock(attacker,blocker,combat)) blockers.add(blocker);
	  }
		  
	  return blockers;   
   }
   
   //finds blockers that won't be destroyed
   private static CardList getSafeBlockers(Card attacker, CardList blockersLeft) {
	  CardList blockers = new CardList();
	   
	  for(Card b : blockersLeft) {
		  if(!CombatUtil.canDestroyBlocker(b,attacker)) blockers.add(b);
	  }
	  
	  return blockers;   
   }
   
   //finds blockers that destroy the attacker
   private static CardList getKillingBlockers(Card attacker, CardList blockersLeft) {
	  CardList blockers = new CardList();
	   
	  for(Card b : blockersLeft) {
		   if(CombatUtil.canDestroyAttacker(attacker,b)) blockers.add(b);
	  }
	   
	  return blockers;   
   }
   
   public static CardList sortPotentialAttackers(Combat combat){
	   CardList[] attackerLists = combat.sortAttackerByDefender();
	   CardList sortedAttackers = new CardList();
	   
	   ArrayList<Object> defenders = combat.getDefenders();
	   
	   //Begin with the attackers that pose the biggest thread
	   CardListUtil.sortAttack(attackerLists[0]);
	   
	   // If I don't have any planeswalkers than sorting doesn't really matter
	   if (defenders.size() == 1)
		   return attackerLists[0];
	   
	   boolean bLifeInDanger = CombatUtil.lifeInDanger(combat);
	   
	   // todo: Add creatures attacking Planeswalkers in order of which we want to protect
       // defend planeswalkers with more loyalty before planeswalkers with less loyalty
	   // if planeswalker will be too difficult to defend don't even bother
	   for(int i = 1; i < attackerLists.length; i++){
		   //Begin with the attackers that pose the biggest thread
		   CardListUtil.sortAttack(attackerLists[i]);
		   for(Card c : attackerLists[i])
			   sortedAttackers.add(c);
	   }
	   
	   if(bLifeInDanger) {
		   // add creatures attacking the Player to the front of the list
		   for(Card c : attackerLists[0])
			   sortedAttackers.add(0, c);

	   }
	   else{
			// add creatures attacking the Player to the back of the list
		   for(Card c : attackerLists[0])
			   sortedAttackers.add(c);
	   }

	   return sortedAttackers;
   }

   //Main function
   public static Combat getBlockers(Combat originalCombat, CardList possibleBlockers) {
	  
	  Combat combat = originalCombat;
	  
	  CardList attackers = sortPotentialAttackers(combat);
      
	  if (attackers.size() == 0)
		  return combat;
	   
	  CardList attackersLeft = new CardList(attackers.toArray()); //keeps track of all currently unblocked attackers
	  CardList blockedButUnkilled = new CardList(); //keeps track of all blocked attackers that currently wouldn't be destroyed
	  CardList tramplingAttackers = new CardList();
	  CardList blockersLeft = new CardList(possibleBlockers.toArray()); //keeps track of all unassigned blockers
	  CardList blockers = new CardList();
	  CardList safeBlockers = new CardList();
	  CardList killingBlockers = new CardList();
	  CardList chumpBlockers = new CardList();
	  
	  int diff = AllZone.ComputerPlayer.getLife() * 2 + 5; //This is the minimal gain for an unnecessary trade 
	  
	  // remove all attackers that can't be blocked anyway
	  for(Card a : attackers) {
		  if(!CombatUtil.canBeBlocked(a)) { 
			  attackersLeft.remove(a);
		  }
	  }
	  
	  // Lure effects
	  CardList attackersWithLure = attackersLeft.getKeyword("All creatures able to block CARDNAME do so.");
	  combat.setAttackersWithLure(attackersWithLure);
	  CardList canBlockAttackerWithLure = new CardList();
	  for(Card attacker : attackersWithLure) {
		  for(Card b : possibleBlockers) {
			  if(CombatUtil.canBlock(attacker, b)) canBlockAttackerWithLure.add(b);
		  }
	  }
	  combat.setCanBlockAttackerWithLure(canBlockAttackerWithLure);
	  
	  if (attackersLeft.size() == 0)
		  return combat;
	   
	  // remove all blockers that can't block anyway
	  for(Card b : possibleBlockers) {
		  if(!CombatUtil.canBlock(b, combat)) blockersLeft.remove(b);
	  }
	  
	  boolean bLifeInDanger = CombatUtil.lifeInDanger(combat);
	  
	  //These creatures won't prevent any damage
	  if (bLifeInDanger) {
		  //TODO - this keyword is no longer present.  Somehow, this will need to check triggers.
		  blockersLeft = blockersLeft.getNotKeyword("Whenever CARDNAME is dealt damage, you lose that much life.");
	  }
	   
	  if (blockersLeft.size() == 0)
		  return combat;
	  
	  //Begin with the weakest blockers
	  CardListUtil.sortAttackLowFirst(blockersLeft);
	   
	  CardList currentAttackers = new CardList(attackersLeft.toArray());

	  //first choose good blocks only
	  for(Card attacker : attackersLeft) {
		  
		  Card blocker = new Card();
		  
		  blockers = getPossibleBlockers(attacker, blockersLeft, combat);
		   
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
				  Card worst = CardFactoryUtil.AI_getWorstCreature(killingBlockers);
				  
				  if(CardFactoryUtil.evaluateCreature(worst) + diff < CardFactoryUtil.evaluateCreature(attacker)) {
					  blocker = worst;
				  }
			  }
		  }
		  if(blocker.getName() != "") {
			  currentAttackers.remove(attacker);
			  blockersLeft.remove(blocker);
			  combat.addBlocker(attacker, blocker);
		  }
	  }
	   
	  attackersLeft = new CardList(currentAttackers.toArray());
	  
	  if(blockersLeft.size() == 0) return combat;
	  
	  currentAttackers = new CardList(attackersLeft.toArray());
	  
	  //if computer life is not in danger, try to make good gangblocks
	  if(!CombatUtil.lifeInDanger(combat)) 
		  for(Card attacker : attackersLeft) {
			  if(!attacker.getKeyword().contains("First Strike") && !attacker.getKeyword().contains("Double Strike")
					   && !attacker.hasStartOfKeyword("Rampage")) {
				  blockers = getPossibleBlockers(attacker, blockersLeft, combat);
				  CardList firstStrikeBlockers = new CardList();
				  CardList blockGang = new CardList();
			      for(int i = 0; i < blockers.size(); i++)
			         if(blockers.get(i).hasFirstStrike() || blockers.get(i).hasDoubleStrike())
			        	 firstStrikeBlockers.add(blockers.get(i));
			      
			      if(!firstStrikeBlockers.isEmpty()) {
			    	  CardListUtil.sortAttack(firstStrikeBlockers);
			    	  for(Card blocker : firstStrikeBlockers) {
			    		  //if the total damage of the blockgang was not enough without but is enough with this blocker finish the blockgang
			    		  if (CombatUtil.totalDamageOfBlockers(attacker, blockGang) < attacker.getKillDamage()) {
			    			  blockGang.add(blocker);
			    			  if (CombatUtil.totalDamageOfBlockers(attacker, blockGang) >= attacker.getKillDamage()) {
			    				  currentAttackers.remove(attacker);
			    				  for(Card b : blockGang) {
			    					  blockersLeft.remove(b);
			    					  combat.addBlocker(attacker, b);
			    				  }
			    			  }
			    		  }
			    	  }
			      }
			  }
		  }
	  //End gangblocks
	  
	  attackersLeft = new CardList(currentAttackers.toArray());
	  
	  if(blockersLeft.size() == 0) return combat;
	  
	  //choose necessary trade blocks if life is in danger
	  if (CombatUtil.lifeInDanger(combat))
		  for(Card attacker : attackersLeft) {
			  killingBlockers = 
				  getKillingBlockers(attacker, getPossibleBlockers(attacker, blockersLeft, combat));
			  if(killingBlockers.size() > 0 && CombatUtil.lifeInDanger(combat)) {
				  Card blocker = CardFactoryUtil.AI_getWorstCreature(killingBlockers);
				  combat.addBlocker(attacker, blocker);
				  currentAttackers.remove(attacker);
				  blockersLeft.remove(blocker);
			  }
		  }
	   
	  attackersLeft = new CardList(currentAttackers.toArray());
	   
	  //choose necessary chump blocks if life is still in danger
	  if (CombatUtil.lifeInDanger(combat))
		  for(Card attacker : attackersLeft) {
			  chumpBlockers = getPossibleBlockers(attacker, blockersLeft, combat);
			  if(chumpBlockers.size() > 0 && CombatUtil.lifeInDanger(combat)) {
				  Card blocker = CardFactoryUtil.AI_getWorstCreature(chumpBlockers);
				  combat.addBlocker(attacker, blocker);
				  currentAttackers.remove(attacker);
				  blockedButUnkilled.add(attacker);
				  blockersLeft.remove(blocker);
			  }
		  }
	  
	  attackersLeft = new CardList(currentAttackers.toArray()); 
	  
	  //Reinforce blockers blocking attackers with trample if life is still in danger
	  if (CombatUtil.lifeInDanger(combat)) {
		  tramplingAttackers = attackers.getKeyword("Trample");
		  tramplingAttackers = tramplingAttackers.getKeywordsDontContain("Rampage"); 	//Don't make it worse
		  //TODO - should check here for a "rampage-like" trigger that replaced the keyword:
		  // "Whenever CARDNAME becomes blocked, it gets +1/+1 until end of turn for each creature blocking it."

		  for(Card attacker : tramplingAttackers) {
			  chumpBlockers = getPossibleBlockers(attacker, blockersLeft, combat);
			  for(Card blocker : chumpBlockers) {
				  //Add an additional blocker if the current blockers are not enough and the new one would suck some of the damage
				  if(CombatUtil.getAttack(attacker) > CombatUtil.totalShieldDamage(attacker,combat.getBlockers(attacker)) 
						  && CombatUtil.shieldDamage(attacker, blocker) > 0 && CombatUtil.canBlock(attacker,blocker, combat)
						  && CombatUtil.lifeInDanger(combat)) {
					  combat.addBlocker(attacker, blocker);
					  blockersLeft.remove(blocker);
				  }
			  }
		  }
	  }
	  
	  //Support blockers not destroying the attacker with more blockers to try to kill the attacker
	  if (blockedButUnkilled.size() > 0) {
		  CardList targetAttackers = blockedButUnkilled.getKeywordsDontContain("Rampage"); 	//Don't make it worse
		  //TODO - should check here for a "rampage-like" trigger that replaced the keyword:
		  // "Whenever CARDNAME becomes blocked, it gets +1/+1 until end of turn for each creature blocking it."

		  for(Card attacker : targetAttackers) {
			  blockers = getPossibleBlockers(attacker, blockersLeft, combat);

			  //Try to use safe blockers first
			  safeBlockers = getSafeBlockers(attacker, blockers);
			  for(Card blocker : safeBlockers) {
				  //Add an additional blocker if the current blockers are not enough and the new one would deal additional damage
				  if(attacker.getKillDamage() > CombatUtil.totalDamageOfBlockers(attacker,combat.getBlockers(attacker)) 
						  && CombatUtil.dealsDamageAsBlocker(attacker, blocker) > 0 && CombatUtil.canBlock(attacker,blocker, combat)) {
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
			  else safeBlockers = new CardList(blockers.toArray());

			  for(Card blocker : safeBlockers) {
				  //Add an additional blocker if the current blockers are not enough and the new one would deal the remaining damage
				  int currentDamage = CombatUtil.totalDamageOfBlockers(attacker,combat.getBlockers(attacker));
				  int additionalDamage = CombatUtil.dealsDamageAsBlocker(attacker, blocker);
				  if(attacker.getKillDamage() > currentDamage 
						  && !(attacker.getKillDamage() > currentDamage + additionalDamage)
						  && CardFactoryUtil.evaluateCreature(blocker) + diff < CardFactoryUtil.evaluateCreature(attacker)
						  && CombatUtil.canBlock(attacker,blocker,combat)) {
					  combat.addBlocker(attacker, blocker);
					  blockersLeft.remove(blocker);
				  }
			  }
		  }
	  }
	  
	  // assign blockers that have to block 
	  chumpBlockers = blockersLeft.getKeyword("CARDNAME blocks each turn if able.");
	  // if an attacker with lure attacks - all that can block
	  for(Card blocker : blockersLeft) {
		  if(canBlockAttackerWithLure.contains(blocker)) chumpBlockers.add(blocker);
	  }
	  if (!chumpBlockers.isEmpty()) {
		  attackers.shuffle();
		  for(Card attacker : attackers) {
			  blockers = getPossibleBlockers(attacker, chumpBlockers, combat);
			  for(Card blocker : blockers) {
				  if (CombatUtil.canBlock(attacker, blocker, combat)) {
					  combat.addBlocker(attacker, blocker);
					  blockersLeft.remove(blocker);
				  }
			  }
		  }
	  }
	   
	 return combat;
   }
}