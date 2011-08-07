
package forge;

import java.util.ArrayList;

import forge.card.cardFactory.CardFactoryUtil;


public class ComputerUtil_Block2
{
	  private static CardList attackers = new CardList(); //all attackers
	  private static CardList attackersLeft = new CardList(); //keeps track of all currently unblocked attackers
	  private static CardList blockedButUnkilled = new CardList(); //blocked attackers that currently wouldn't be destroyed
	  private static CardList blockersLeft = new CardList(); //keeps track of all unassigned blockers
	  private static int diff = 0;
	
	
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
   private static CardList getSafeBlockers(Card attacker, CardList blockersLeft, Combat combat) {
	  CardList blockers = new CardList();
	   
	  for(Card b : blockersLeft) {
		  if(!CombatUtil.canDestroyBlocker(b, attacker, combat, false)) blockers.add(b);
	  }
	  
	  return blockers;   
   }
   
   //finds blockers that destroy the attacker
   private static CardList getKillingBlockers(Card attacker, CardList blockersLeft, Combat combat) {
	  CardList blockers = new CardList();
	   
	  for(Card b : blockersLeft) {
		   if(CombatUtil.canDestroyAttacker(attacker, b, combat, false)) blockers.add(b);
	  }
	   
	  return blockers;   
   }
   
   public static CardList sortPotentialAttackers(Combat combat){
	   CardList[] attackerLists = combat.sortAttackerByDefender();
	   CardList sortedAttackers = new CardList();
	   
	   ArrayList<Object> defenders = combat.getDefenders();
	   
	   //Begin with the attackers that pose the biggest thread
	   CardListUtil.sortByEvaluateCreature(attackerLists[0]);
	   CardListUtil.sortAttack(attackerLists[0]);
	   
	   // If I don't have any planeswalkers than sorting doesn't really matter
	   if (defenders.size() == 1)
		   return attackerLists[0];
	   
	   boolean bLifeInDanger = CombatUtil.lifeInDanger(combat);
	   
	   // TODO: Add creatures attacking Planeswalkers in order of which we want to protect
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
   
   // ======================= block assignment functions ================================
   
   // Good Blocks means a good trade or no trade
   private static Combat makeGoodBlocks(Combat combat){
	   
	  CardList currentAttackers = new CardList(attackersLeft.toArray());
	  
	  for(Card attacker : attackersLeft) {
		  
		  Card blocker = new Card();
		  
		  CardList blockers = getPossibleBlockers(attacker, blockersLeft, combat);
		   
		  CardList safeBlockers = getSafeBlockers(attacker, blockers, combat);
		  CardList killingBlockers;
		   
		  if(safeBlockers.size() > 0) {
			  // 1.Blockers that can destroy the attacker but won't get destroyed 
			  killingBlockers = getKillingBlockers(attacker, safeBlockers, combat);
			  if(killingBlockers.size() > 0) blocker = CardFactoryUtil.AI_getWorstCreature(killingBlockers);

			  // 2.Blockers that won't get destroyed 
			  else {
				  blocker = CardFactoryUtil.AI_getWorstCreature(safeBlockers);
				  blockedButUnkilled.add(attacker);
			  }
		  } // no safe blockers
		  else {
			  killingBlockers = getKillingBlockers(attacker, blockers, combat);
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
	  return combat;
   }
   
   // Good Gang Blocks means a good trade or no trade
   private static Combat makeGangBlocks(Combat combat){
	   
	  CardList currentAttackers = new CardList(attackersLeft.toArray());
	  currentAttackers = currentAttackers.getKeywordsDontContain("Rampage");
	  currentAttackers = currentAttackers.getKeywordsDontContain("CARDNAME can't be blocked by more than one creature.");
	  CardList blockers;
	  
	  //Try to block an attacker without first strike with a gang of first strikers
	  for (Card attacker : attackersLeft) {
		  if (!attacker.hasKeyword("First Strike") 
				  && !attacker.hasKeyword("Double Strike")) {
			  blockers = getPossibleBlockers(attacker, blockersLeft, combat);
			  CardList firstStrikeBlockers = new CardList();
			  CardList blockGang = new CardList();
		      for (int i = 0; i < blockers.size(); i++)
		         if (blockers.get(i).hasFirstStrike() || blockers.get(i).hasDoubleStrike())
		        	 firstStrikeBlockers.add(blockers.get(i));
		      
		      if (firstStrikeBlockers.size() > 1) {
		    	  CardListUtil.sortAttack(firstStrikeBlockers);
		    	  for (Card blocker : firstStrikeBlockers) {
		    		  //if the total damage of the blockgang was not enough without but is enough with this blocker finish the blockgang
		    		  if (CombatUtil.totalDamageOfBlockers(attacker, blockGang) < attacker.getKillDamage()) {
		    			  blockGang.add(blocker);
		    			  if (CombatUtil.totalDamageOfBlockers(attacker, blockGang) >= attacker.getKillDamage()) {
		    				  currentAttackers.remove(attacker);
		    				  for (Card b : blockGang) {
		    					  blockersLeft.remove(b);
		    					  combat.addBlocker(attacker, b);
		    				  }
		    			  }
		    		  }
		    	  }
		      }
		  }
	  }
	  
	  attackersLeft = new CardList(currentAttackers.toArray());
	  currentAttackers = new CardList(attackersLeft.toArray());
	  
	  //Try to block an attacker with two blockers of which only one will die
	  for(final Card attacker : attackersLeft) {
		  blockers = getPossibleBlockers(attacker, blockersLeft, combat);
		  CardList usableBlockers;
		  CardList blockGang = new CardList();
		  int absorbedDamage = 0; //The amount of damage needed to kill the first blocker
		  int currentValue = 0; //The value of the creatures in the blockgang
		  
		  //Try to add blockers that could be destroyed, but are worth less than the attacker
		  //Don't use blockers without First Strike or Double Strike if attacker has it
		  usableBlockers = blockers.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					if ((attacker.hasKeyword("First Strike") || attacker.hasKeyword("Double Strike"))
							&& !(c.hasKeyword("First Strike") || c.hasKeyword("Double Strike")))
						return false;
					return CardFactoryUtil.evaluateCreature(c) + diff < CardFactoryUtil.evaluateCreature(attacker);
				}
		  });
		  if (usableBlockers.size() < 2)
			  return combat;
		  
		  Card leader = CardFactoryUtil.AI_getBestCreature(usableBlockers);
		  blockGang.add(leader);
		  usableBlockers.remove(leader);
		  absorbedDamage = leader.getEnoughDamageToKill(attacker.getNetCombatDamage(), attacker, true);
		  currentValue = CardFactoryUtil.evaluateCreature(leader);

		  for(Card blocker : usableBlockers) {
			  //Add an additional blocker if the current blockers are not enough and the new one would deal the remaining damage
			  int currentDamage = CombatUtil.totalDamageOfBlockers(attacker, blockGang);
			  int additionalDamage = CombatUtil.dealsDamageAsBlocker(attacker, blocker);
			  int absorbedDamage2 = blocker.getEnoughDamageToKill(attacker.getNetCombatDamage(), attacker, true);
			  int addedValue = CardFactoryUtil.evaluateCreature(blocker);
			  if(attacker.getKillDamage() > currentDamage
					  && !(attacker.getKillDamage() > currentDamage + additionalDamage) //The attacker will be killed
					  && (absorbedDamage2 + absorbedDamage > attacker.getNetCombatDamage() //only one blocker can be killed
							  || currentValue + addedValue - 50 <= CardFactoryUtil.evaluateCreature(attacker)) //attacker is worth more
					  && CombatUtil.canBlock(attacker,blocker,combat)) {//this is needed for attackers that can't be blocked by more than 1
				  currentAttackers.remove(attacker);
				  combat.addBlocker(attacker, blocker);
				  combat.addBlocker(attacker, leader);
				  blockersLeft.remove(blocker);
				  blockersLeft.remove(leader);
				  break;
			  }
		  }
	  }
	  
	  attackersLeft = new CardList(currentAttackers.toArray());
	  return combat;
   }
   
   // Bad Trade Blocks (should only be made if life is in danger)
   private static Combat makeTradeBlocks(Combat combat){
	   
	  CardList currentAttackers = new CardList(attackersLeft.toArray());
	  CardList killingBlockers;
	  
	  for(Card attacker : attackersLeft) {
		  killingBlockers = 
			  getKillingBlockers(attacker, getPossibleBlockers(attacker, blockersLeft, combat), combat);
		  if(killingBlockers.size() > 0 && CombatUtil.lifeInDanger(combat)) {
			  Card blocker = CardFactoryUtil.AI_getWorstCreature(killingBlockers);
			  combat.addBlocker(attacker, blocker);
			  currentAttackers.remove(attacker);
			  blockersLeft.remove(blocker);
		  }
	  }
	  attackersLeft = new CardList(currentAttackers.toArray());
	  return combat;
   }
   
   // Chump Blocks (should only be made if life is in danger)
   private static Combat makeChumpBlocks(Combat combat){
	   
	  CardList currentAttackers = new CardList(attackersLeft.toArray());
	  CardList chumpBlockers;
	  
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
	  return combat;
   }
   
   //Reinforce blockers blocking attackers with trample (should only be made if life is in danger)
   private static Combat reinforceBlockersAgainstTrample(Combat combat){
	   
	  CardList chumpBlockers;
	  
	  CardList tramplingAttackers = attackers.getKeyword("Trample");
	  tramplingAttackers = tramplingAttackers.getKeywordsDontContain("Rampage"); 	//Don't make it worse
	  tramplingAttackers = tramplingAttackers.getKeywordsDontContain("CARDNAME can't be blocked by more than one creature.");
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

	  return combat;
   }
   
   //Support blockers not destroying the attacker with more blockers to try to kill the attacker
   private static Combat reinforceBlockersToKill(Combat combat){
	   
	  CardList safeBlockers;
	  CardList blockers;
	  CardList targetAttackers = blockedButUnkilled.getKeywordsDontContain("Rampage"); 	//Don't make it worse
	  targetAttackers = targetAttackers.getKeywordsDontContain("CARDNAME can't be blocked by more than one creature.");
	  //TODO - should check here for a "rampage-like" trigger that replaced the keyword:
	  // "Whenever CARDNAME becomes blocked, it gets +1/+1 until end of turn for each creature blocking it."
	  
	  for(Card attacker : targetAttackers) {
		  blockers = getPossibleBlockers(attacker, blockersLeft, combat);

		  //Try to use safe blockers first
		  safeBlockers = getSafeBlockers(attacker, blockers, combat);
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
			  safeBlockers.addAll(blockers.getKeyword("Double Strike"));
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

	  return combat;
   }
   
   private static Combat resetBlockers(Combat combat, CardList possibleBlockers) {
	   
	  CardList oldBlockers = combat.getAllBlockers();
	  for (Card blocker:oldBlockers) {
		  combat.removeFromCombat(blocker);
	  }
	  
	  attackersLeft = new CardList(attackers.toArray()); //keeps track of all currently unblocked attackers
	  blockersLeft = new CardList(possibleBlockers.toArray()); //keeps track of all unassigned blockers
	  blockedButUnkilled = new CardList(); //keeps track of all blocked attackers that currently wouldn't be destroyed
	  
	  return combat;
   }

   //Main function
   static public Combat getBlockers(Combat originalCombat, CardList possibleBlockers) {
	  
	  Combat combat = originalCombat;
	  
	  attackers = sortPotentialAttackers(combat);
      
	  if (attackers.size() == 0)
		  return combat;
	   
	  attackersLeft = new CardList(attackers.toArray()); //keeps track of all currently unblocked attackers
	  blockersLeft = new CardList(possibleBlockers.toArray()); //keeps track of all unassigned blockers
	  blockedButUnkilled = new CardList(); //keeps track of all blocked attackers that currently wouldn't be destroyed
	  CardList blockers;
	  CardList chumpBlockers;
	  
	  diff = AllZone.ComputerPlayer.getLife() * 2 - 5; //This is the minimal gain for an unnecessary trade 
	  
	  // remove all attackers that can't be blocked anyway
	  for(Card a : attackers) {
		  if(!CombatUtil.canBeBlocked(a)) { 
			  attackersLeft.remove(a);
		  }
	  }
	  
	  // remove all blockers that can't block anyway
	  for(Card b : possibleBlockers) {
		  if(!CombatUtil.canBlock(b, combat)) blockersLeft.remove(b);
	  }
	  
	  if (attackersLeft.size() == 0)
		  return combat;
	  
	  //Begin with the weakest blockers
	  CardListUtil.sortAttackLowFirst(blockersLeft);

	  //== 1. choose best blocks first ==
	  combat = makeGoodBlocks(combat);
	  combat = makeGangBlocks(combat);
	  if (CombatUtil.lifeInDanger(combat)) combat = makeTradeBlocks(combat); //choose necessary trade blocks if life is in danger
	  if (CombatUtil.lifeInDanger(combat)) combat = makeChumpBlocks(combat); //choose necessary chump blocks if life is still in danger
	  //Reinforce blockers blocking attackers with trample if life is still in danger
	  if (CombatUtil.lifeInDanger(combat)) combat = reinforceBlockersAgainstTrample(combat);
	  //Support blockers not destroying the attacker with more blockers to try to kill the attacker
	  if (!CombatUtil.lifeInDanger(combat)) combat = reinforceBlockersToKill(combat);
	  
	  
	  //== 2. If the AI life would still be in danger make a safer approach ==
	  if (CombatUtil.lifeInDanger(combat)) {
		  combat = resetBlockers(combat, possibleBlockers); // reset every block assignment
		  combat = makeTradeBlocks(combat); //choose necessary trade blocks if life is in danger
		  combat = makeGoodBlocks(combat);
		  if (CombatUtil.lifeInDanger(combat)) combat = makeChumpBlocks(combat); //choose necessary chump blocks if life is still in danger
		  //Reinforce blockers blocking attackers with trample if life is still in danger
		  if (CombatUtil.lifeInDanger(combat)) combat = reinforceBlockersAgainstTrample(combat);
		  combat = makeGangBlocks(combat);
		  combat = reinforceBlockersToKill(combat);
	  }
	  
	  //== 3. If the AI life would be in serious danger make an even safer approach ==
	  if (CombatUtil.lifeInSeriousDanger(combat)) {
		  combat = resetBlockers(combat, possibleBlockers); // reset every block assignment
		  combat = makeChumpBlocks(combat); //choose chump blocks
		  if (CombatUtil.lifeInDanger(combat)) combat = makeTradeBlocks(combat); //choose necessary trade blocks if life is in danger
		  if (!CombatUtil.lifeInDanger(combat)) combat = makeGoodBlocks(combat);
		  //Reinforce blockers blocking attackers with trample if life is still in danger
		  if (CombatUtil.lifeInDanger(combat)) combat = reinforceBlockersAgainstTrample(combat);
		  combat = makeGangBlocks(combat);
		  //Support blockers not destroying the attacker with more blockers to try to kill the attacker
		  combat = reinforceBlockersToKill(combat);
	  }
	  
	  // assign blockers that have to block 
	  chumpBlockers = blockersLeft.getKeyword("CARDNAME blocks each turn if able.");
	  // if an attacker with lure attacks - all that can block
	  for(Card blocker : blockersLeft) {
		  if(CombatUtil.canBlockAnAttackerWithLure(blocker,combat)) chumpBlockers.add(blocker);
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