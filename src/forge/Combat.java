package forge;

import java.util.*;
import java.util.Map.Entry;

public class Combat {
	// key is attacker Card
	// value is CardList of blockers
	private Map<Card, CardList> map = new HashMap<Card, CardList>();
	private Set<Card> blocked = new HashSet<Card>();

	private HashMap<Card, CardList> unblockedMap = new HashMap<Card, CardList>();
	private HashMap<Card, Integer> defendingFirstStrikeDamageMap = new HashMap<Card, Integer>();
	private HashMap<Card, Integer> defendingDamageMap = new HashMap<Card, Integer>();

	private int attackingDamage;
	// private int defendingDamage;

	// private int defendingFirstStrikeDamage;
	// private int trampleDamage;
	// private int trampleFirstStrikeDamage;

	private Player attackingPlayer;
	private Player defendingPlayer;

	private int declaredAttackers;

	private Card planeswalker;

	public Combat() {
		reset();
	}

	public void reset() {
		planeswalker = null;

		map.clear();
		blocked.clear();

		unblockedMap.clear();

		attackingDamage = 0;
		defendingDamageMap.clear();
		defendingFirstStrikeDamageMap.clear();

		declaredAttackers = 0;
		attackingPlayer = null;
		defendingPlayer = null;
	}

	public void setPlaneswalker(Card c) {
		planeswalker = c;
	}

	public Card getPlaneswalker() {
		return planeswalker;
	}

	public int getDeclaredAttackers() {
		return declaredAttackers;
	}

	public void setAttackingPlayer(Player player) {
		attackingPlayer = player;
	}

	public void setDefendingPlayer(Player player) {
		defendingPlayer = player;
	}

	public Player getAttackingPlayer() {
		return attackingPlayer;
	}

	public Player getDefendingPlayer() {
		return defendingPlayer;
	}

	// relates to defending player damage
	// public int getDefendingDamage() {return defendingDamage;}

	public HashMap<Card, Integer> getDefendingDamageMap() {
		return defendingDamageMap;
	}

	public HashMap<Card, Integer> getDefendingFirstStrikeDamageMap() {
		return defendingFirstStrikeDamageMap;
	}

	public int getTotalDefendingDamage() {
		int total = 0;

		Collection<Integer> c = defendingDamageMap.values();

		Iterator<Integer> itr = c.iterator();
		while (itr.hasNext())
			total += itr.next();

		return total;
	}

	public int getTotalFirstStrikeDefendingDamage() {
		int total = 0;

		Collection<Integer> c = defendingFirstStrikeDamageMap.values();

		Iterator<Integer> itr = c.iterator();
		while (itr.hasNext())
			total += itr.next();

		return total;
	}

	public void setDefendingDamage() {
		defendingDamageMap.clear();
		CardList att = new CardList(getAttackers());
		// sum unblocked attackers' power
		for (int i = 0; i < att.size(); i++) {
			if (!isBlocked(att.get(i))
					|| (getBlockers(att.get(i)).size() == 0 && att.get(i)
							.getKeyword().contains("Trample"))) {
				int damageDealt = att.get(i).getNetAttack();
				if (CombatUtil.isDoranInPlay())
					damageDealt = att.get(i).getNetDefense();

				if (damageDealt > 0) {
					// if the creature has first strike do not do damage in the
					// normal combat phase
					// if(att.get(i).hasSecondStrike())
					if (!att.get(i).hasFirstStrike()
							|| (att.get(i).hasFirstStrike() && att.get(i)
									.hasDoubleStrike()))
						addDefendingDamage(damageDealt, att.get(i));
				}
			} // ! isBlocked...
		}// for
	}

	public void setDefendingFirstStrikeDamage() {
		defendingFirstStrikeDamageMap.clear();
		CardList att = new CardList(getAttackers());
		// sum unblocked attackers' power
		for (int i = 0; i < att.size(); i++) {
			if (!isBlocked(att.get(i))) {
				int damageDealt = att.get(i).getNetAttack();
				if (CombatUtil.isDoranInPlay())
					damageDealt = att.get(i).getNetDefense();

				if (damageDealt > 0) {
					// if the creature has first strike or double strike do
					// damage in the first strike combat phase
					if (att.get(i).hasFirstStrike()
							|| att.get(i).hasDoubleStrike()) {
						addDefendingFirstStrikeDamage(damageDealt, att.get(i));
					}
				}
			}
		} // for
	}

	public void addDefendingDamage(int n, Card source) {
		if (!defendingDamageMap.containsKey(source))
			defendingDamageMap.put(source, n);
		else {
			defendingDamageMap.put(source, defendingDamageMap.get(source) + n);
		}
	}

	public void addDefendingFirstStrikeDamage(int n, Card source) {
		if (!defendingFirstStrikeDamageMap.containsKey(source))
			defendingFirstStrikeDamageMap.put(source, n);
		else {
			defendingFirstStrikeDamageMap.put(source,
					defendingFirstStrikeDamageMap.get(source) + n);
		}
	}

	public void addAttackingDamage(int n) {
		attackingDamage += n;
	}

	public int getAttackingDamage() {
		return attackingDamage;
	}

	public void addAttacker(Card c) {
		map.put(c, new CardList());
		declaredAttackers++;
	}

	public void resetAttackers() {
		map.clear();
	}

	public Card[] getAttackers() {
		CardList out = new CardList();
		Iterator<Card> it = map.keySet().iterator();
		// int i = 0; //unused

		while (it.hasNext()) {
			out.add((Card) it.next());
		}

		return out.toArray();
	}// getAttackers()

	public boolean isBlocked(Card attacker) {
		return blocked.contains(attacker);
	}

	public void addBlocker(Card attacker, Card blocker) {
		blocked.add(attacker);
		getList(attacker).add(blocker);
		// CombatUtil.checkBlockedAttackers(attacker, blocker);
	}

	public void resetBlockers() {
		reset();

		CardList att = new CardList(getAttackers());
		for (int i = 0; i < att.size(); i++)
			addAttacker(att.get(i));
	}

	public CardList getAllBlockers() {
		CardList att = new CardList(getAttackers());
		CardList block = new CardList();

		for (int i = 0; i < att.size(); i++)
			block.addAll(getBlockers(att.get(i)).toArray());

		return block;
	}// getAllBlockers()

	public CardList getBlockers(Card attacker) {
		if (getList(attacker) == null)
			return new CardList();
		else
			return new CardList(getList(attacker).toArray());
	}

	private CardList getList(Card attacker) {
		return (CardList) map.get(attacker);
	}

	public void removeFromCombat(Card c) {
		// is card an attacker?
		CardList att = new CardList(getAttackers());
		if (att.contains(c))
			map.remove(c);
		else// card is a blocker
		{
			for (int i = 0; i < att.size(); i++)
				if (getBlockers(att.get(i)).contains(c))
					getList(att.get(i)).remove(c);
		}
	}// removeFromCombat()

	public void verifyCreaturesInPlay() {
		CardList all = new CardList();
		all.addAll(getAttackers());
		all.addAll(getAllBlockers().toArray());

		for (int i = 0; i < all.size(); i++)
			if (!AllZone.GameAction.isCardInPlay(all.get(i)))
				removeFromCombat(all.get(i));
	}// verifyCreaturesInPlay()

	// set Card.setAssignedDamage() for all creatures in combat
	// also assigns player damage by setPlayerDamage()
	public void setAssignedFirstStrikeDamage() {
		setDefendingFirstStrikeDamage();

		CardList block;
		CardList attacking = new CardList(getAttackers());
		for (int i = 0; i < attacking.size(); i++) {
			// if(attacking.get(i).hasFirstStrike() ||
			// (attacking.get(i).hasDoubleStrike() )){
			block = getBlockers(attacking.get(i));

			// attacker always gets all blockers' attack

			for (Card b : block) {
				if (b.hasFirstStrike() || b.hasDoubleStrike()) {
					int attack = b.getNetAttack();
					if (CombatUtil.isDoranInPlay())
						attack = b.getNetDefense();
					attacking.get(i).addAssignedDamage(attack, b);
				}
			}

			if (block.size() == 0)// this damage is assigned to a player by
									// setPlayerDamage()
			{
				// GameActionUtil.executePlayerCombatDamageEffects(attacking.get(i));
				addUnblockedAttacker(attacking.get(i));
			}

			else if (attacking.get(i).hasFirstStrike()
					|| (attacking.get(i).hasDoubleStrike())) {

				if (block.size() == 1) {
					if (attacking.get(i).hasFirstStrike()
							|| attacking.get(i).hasDoubleStrike()) {
						int damageDealt = attacking.get(i).getNetAttack();
						if (CombatUtil.isDoranInPlay())
							damageDealt = attacking.get(i).getNetDefense();

						CardList cl = new CardList();
						cl.add(attacking.get(i));

						block.get(0).addAssignedDamage(damageDealt,
								attacking.get(i));

						// trample
						int trample = damageDealt
								- block.get(0).getNetDefense();
						if (attacking.get(i).getKeyword().contains("Trample")
								&& 0 < trample) {
							this.addDefendingFirstStrikeDamage(trample,
									attacking.get(i));
							// System.out.println("First Strike trample damage: "
							// + trample);
						}
					}
				}// 1 blocker
				else if (getAttackingPlayer().isComputer()) {
					if (attacking.get(i).hasFirstStrike()
							|| attacking.get(i).hasDoubleStrike()) {
						int damageDealt = attacking.get(i).getNetAttack();
						if (CombatUtil.isDoranInPlay())
							damageDealt = attacking.get(i).getNetDefense();
						addAssignedFirstStrikeDamage(attacking.get(i), block,
								damageDealt);
					}
				} else// human
				{
					if (attacking.get(i).hasFirstStrike()
							|| attacking.get(i).hasDoubleStrike()) {
						// GuiDisplay2 gui = (GuiDisplay2) AllZone.Display;
						int damageDealt = attacking.get(i).getNetAttack();
						if (CombatUtil.isDoranInPlay())
							damageDealt = attacking.get(i).getNetDefense();
						AllZone.Display.assignDamage(attacking.get(i), block,
								damageDealt);

						/*
						 * for (Card b : block) {
						 * AllZone.Display.assignDamage(attacking.get(i), b,
						 * damageDealt);//System.out.println(
						 * "setAssignedFirstStrikeDmg called for:" + damageDealt
						 * + " damage."); }
						 * AllZone.Display.addAssignDamage(attacking
						 * .get(i),damageDealt);
						 */
					}
				}

			}// if(hasFirstStrike || doubleStrike)
		}// for

		// should first strike affect the following?
		if (getPlaneswalker() != null) {
			// System.out.println("defendingDmg (setAssignedFirstStrikeDamage) :"
			// +defendingFirstStrikeDamage);
			//

			Iterator<Card> iter = defendingFirstStrikeDamageMap.keySet()
					.iterator();
			while (iter.hasNext()) {
				Card crd = iter.next();
				planeswalker.addAssignedDamage(defendingFirstStrikeDamageMap
						.get(crd), crd);
			}

			defendingFirstStrikeDamageMap.clear();
		}

	}// setAssignedFirstStrikeDamage()

	private void addAssignedFirstStrikeDamage(Card attacker, CardList block,
			int damage) {

		Card c = attacker;
		for (Card b : block) {
			if (b.getKillDamage() <= damage) {
				damage -= b.getKillDamage();
				CardList cl = new CardList();
				cl.add(attacker);

				b.addAssignedDamage(b.getKillDamage(), c);
			}
		}// for

		// if attacker has no trample, and there's damage left, assign the rest
		// to a random blocker
		if (damage > 0) {
			int index = CardUtil.getRandomIndex(block);
			block.get(index).addAssignedDamage(damage, c);
			damage = 0;
		} else if (c.getKeyword().contains("Trample")) {
			this.addDefendingDamage(damage, c);
		}
	}// setAssignedFirstStrikeDamage()

	// set Card.setAssignedDamage() for all creatures in combat
	// also assigns player damage by setPlayerDamage()
	public void setAssignedDamage() {
		setDefendingDamage();

		CardList block;
		CardList attacking = new CardList(getAttackers());
		for (int i = 0; i < attacking.size(); i++) {
			// if(!attacking.get(i).hasSecondStrike() ){
			// if(!attacking.get(i).hasFirstStrike() ||
			// (attacking.get(i).hasFirstStrike() &&
			// attacking.get(i).hasDoubleStrike() )){
			block = getBlockers(attacking.get(i));

			// attacker always gets all blockers' attack
			// attacking.get(i).setAssignedDamage(CardListUtil.sumAttack(block));
			// AllZone.GameAction.setAssignedDamage(attacking.get(i), block,
			// CardListUtil.sumAttack(block));

			for (Card b : block) {
				if (!b.hasFirstStrike()
						|| (b.hasFirstStrike() && b.hasDoubleStrike())) {
					int attack = b.getNetAttack();
					if (CombatUtil.isDoranInPlay())
						attack = b.getNetDefense();
					attacking.get(i).addAssignedDamage(attack, b);
				}
			}

			if (block.size() == 0)// this damage is assigned to a player by
									// setPlayerDamage()
			{
				// GameActionUtil.executePlayerCombatDamageEffects(attacking.get(i));
				addUnblockedAttacker(attacking.get(i));
			}

			else if (!attacking.get(i).hasFirstStrike()
					|| (attacking.get(i).hasFirstStrike() && attacking.get(i)
							.hasDoubleStrike())) {

				if (block.size() == 1) {
					int damageDealt = attacking.get(i).getNetAttack();
					if (CombatUtil.isDoranInPlay())
						damageDealt = attacking.get(i).getNetDefense();

					block.get(0).addAssignedDamage(damageDealt,
							attacking.get(i));

					// trample
					int trample = damageDealt - block.get(0).getNetDefense();
					if (attacking.get(i).getKeyword().contains("Trample")
							&& 0 < trample) {
						this.addDefendingDamage(trample, attacking.get(i));
					}
				}// 1 blocker

				else if (getAttackingPlayer().isComputer()) {
					int damageDealt = attacking.get(i).getNetAttack();
					if (CombatUtil.isDoranInPlay())
						damageDealt = attacking.get(i).getNetDefense();
					addAssignedDamage(attacking.get(i), block, damageDealt);

				} else// human attacks
				{
					// GuiDisplay2 gui = (GuiDisplay2) AllZone.Display;
					int damageDealt = attacking.get(i).getNetAttack();
					if (CombatUtil.isDoranInPlay())
						damageDealt = attacking.get(i).getNetDefense();

					AllZone.Display.assignDamage(attacking.get(i), block,
							damageDealt);

					/*
					 * 
					 * 
					 * for (Card b :block)
					 * AllZone.Display.addAssignDamage(attacking.get(i), b,
					 * damageDealt);
					 * //System.out.println("setAssignedDmg called for:" +
					 * damageDealt + " damage.");
					 */
				}

			}// if !hasFirstStrike ...
			// hacky code, to ensure surviving non-first-strike blockers will
			// hit first strike attackers:
			/*
			 * else { block = getBlockers(attacking.get(i));
			 * //System.out.println("block size: " + block.size()); if(
			 * (attacking.get(i).hasFirstStrike() ||
			 * attacking.get(i).hasDoubleStrike()) ) { for(int j=0; j <
			 * block.size(); j++) { //blockerDamage +=
			 * block.get(j).getNetAttack(); int damage =
			 * block.get(j).getNetAttack(); if (CombatUtil.isDoranInPlay())
			 * damage = block.get(j).getNetDefense();
			 * AllZone.GameAction.addAssignedDamage(attacking.get(i),
			 * block.get(j), damage); }
			 * //attacking.get(i).setAssignedDamage(blockerDamage);
			 * //AllZone.GameAction.setAssignedDamage(attacking.get(i), block ,
			 * blockerDamage); } }
			 */
		}// for

		// should first strike affect the following?
		if (getPlaneswalker() != null) {
			// System.out.println("defendingDmg (setAssignedDamage): " +
			// defendingDamage);
			Iterator<Card> iter = defendingDamageMap.keySet().iterator();
			while (iter.hasNext()) {
				Card crd = iter.next();
				planeswalker
						.addAssignedDamage(defendingDamageMap.get(crd), crd);
			}
			defendingDamageMap.clear();
		}
	}// assignDamage()

	/*
	 * private void setAssignedDamage(Card attacker, CardList list, int damage)
	 * { CardListUtil.sortAttack(list); Card c; for(int i = 0; i < list.size();
	 * i++) { c = list.get(i); //if(!c.hasFirstStrike() || (c.hasFirstStrike()
	 * && c.hasDoubleStrike()) ){ if(c.getKillDamage() <= damage) { damage -=
	 * c.getKillDamage(); CardList cl = new CardList(); cl.add(attacker);
	 * AllZone.GameAction.addAssignedDamage(c, cl, c.getKillDamage());
	 * //c.setAssignedDamage(c.getKillDamage()); } //} }//for }//assignDamage()
	 */

	private void addAssignedDamage(Card attacker, CardList block, int damage) {
		Card c = attacker;
		for (Card b : block) {
			if (b.getKillDamage() <= damage) {
				damage -= b.getKillDamage();
				CardList cl = new CardList();
				cl.add(attacker);

				b.addAssignedDamage(b.getKillDamage(), c);
				// c.setAssignedDamage(c.getKillDamage());
			}
		}// for

		// if attacker has no trample, and there's damage left, assign the rest
		// to a random blocker
		if (damage > 0 && !c.getKeyword().contains("Trample")) {
			int index = CardUtil.getRandomIndex(block);
			block.get(index).addAssignedDamage(damage, c);
			damage = 0;
		} else if (c.getKeyword().contains("Trample")) {
			this.addDefendingDamage(damage, c);
		}
	}// setAssignedDamage()

	public static void dealAssignedDamage(){
		// This function handles both Regular and First Strike combat assignment
        Player player = AllZone.Combat.getDefendingPlayer();
        
        boolean bFirstStrike = AllZone.Phase.is(Constant.Phase.Combat_FirstStrikeDamage);
        
        HashMap<Card, Integer> defMap = bFirstStrike ? AllZone.Combat.getDefendingFirstStrikeDamageMap() : 
        	AllZone.Combat.getDefendingDamageMap();
        
        for(Entry<Card, Integer> entry : defMap.entrySet()) {
        	player.addCombatDamage(entry.getValue(), entry.getKey());
        }
        
        CardList unblocked = new CardList(bFirstStrike ? AllZone.Combat.getUnblockedAttackers() : 
        	AllZone.Combat.getUnblockedFirstStrikeAttackers());
        
        for(int j = 0; j < unblocked.size(); j++) {
        	if (bFirstStrike)
        		CombatUtil.checkUnblockedAttackers(unblocked.get(j));
        	else{
	            if(!unblocked.getCard(j).hasFirstStrike() || unblocked.getCard(j).hasDoubleStrike())
	                CombatUtil.checkUnblockedAttackers(unblocked.get(j));
        	}
        }

        if (bFirstStrike){
            CardList pwAttackers = new CardList(AllZone.pwCombat.getAttackers());
            CardList pwBlockers = new CardList(AllZone.pwCombat.getAllBlockers().toArray());
            

            for(int i = 0; i < pwAttackers.size(); i++) {
                if((pwAttackers.getCard(i).hasFirstStrike() || pwAttackers.getCard(i).hasDoubleStrike())) {
                    CombatUtil.executeCombatDamageEffects(pwAttackers.getCard(i));
                }
            }
            for(int i = 0; i < pwBlockers.size(); i++) {
                if((pwBlockers.getCard(i).hasFirstStrike() || pwBlockers.getCard(i).hasDoubleStrike())) {
                    CombatUtil.executeCombatDamageEffects(pwBlockers.getCard(i));
                }
            }
        }
        
        //get all attackers and blockers
        CardList check = new CardList();
        check.addAll(AllZone.Human_Play.getCards());
        check.addAll(AllZone.Computer_Play.getCards());
        
        CardList all = check.getType("Creature");
        
        if(AllZone.pwCombat.getPlaneswalker() != null) all.add(AllZone.pwCombat.getPlaneswalker());
        

        CardList pwAttackers = new CardList(AllZone.pwCombat.getAttackers());
        CardList pwBlockers = new CardList(AllZone.pwCombat.getAllBlockers().toArray());
        
        if (!bFirstStrike){
	        for(int i = 0; i < pwAttackers.size(); i++) {
	            //System.out.println("attacker #" + i + ": " + attackers.getCard(i).getName() +" " + attackers.getCard(i).getAttack());
	            if((!pwAttackers.getCard(i).hasFirstStrike() || (pwAttackers.getCard(i).hasFirstStrike() && pwAttackers.getCard(
	                    i).hasDoubleStrike()))) {
	                CombatUtil.executeCombatDamageEffects(pwAttackers.getCard(i));
	            }
	        }
	        for(int i = 0; i < pwBlockers.size(); i++) {
	            if((!pwBlockers.getCard(i).hasFirstStrike() || (pwBlockers.getCard(i).hasFirstStrike() && pwBlockers.getCard(
	                    i).hasDoubleStrike()))) {
	                CombatUtil.executeCombatDamageEffects(pwBlockers.getCard(i));
	                
	            }
	        }
	        
	        //hacky stuff, hope it won't cause any bugs:
	        for(int i = 0; i < pwAttackers.size(); i++) {
	            AllZone.pwCombat.removeFromCombat(pwAttackers.get(i));
	        }
	        
	        for(int i = 0; i < pwBlockers.size(); i++) {
	            AllZone.pwCombat.removeFromCombat(pwBlockers.get(i));
	        }
        }
        

        Card c;
        for(int i = 0; i < all.size(); i++) {
            c = all.get(i);
            //because this sets off Jackal Pup, and Filthly Cur damage ability
            //and the stack says "Jack Pup causes 0 damage to the Computer"
            if(c.getTotalAssignedDamage() != 0) {
                HashMap<Card, Integer> assignedDamageMap = c.getAssignedDamageHashMap();
                HashMap<Card, Integer> damageMap = new HashMap<Card, Integer>();
                
                for(Entry<Card, Integer> entry : assignedDamageMap.entrySet()){
                    Card crd = entry.getKey();

                    damageMap.put(crd, entry.getValue());
                }
                c.addCombatDamage(damageMap);
                
                damageMap.clear();
                c.clearAssignedDamage();
            }
        }
	}
	
	
	public Card[] getUnblockedAttackers() {
		CardList out = new CardList();
		Iterator<Card> it = unblockedMap.keySet().iterator();
		while (it.hasNext()) { // only add creatures without firstStrike to this
								// list.
			Card c = (Card) it.next();
			if (!c.hasFirstStrike()) {
				out.add(c);
			}
		}

		return out.toArray();
	}// getUnblockedAttackers()

	public Card[] getUnblockedFirstStrikeAttackers() {
		CardList out = new CardList();
		Iterator<Card> it = unblockedMap.keySet().iterator();
		while (it.hasNext()) { // only add creatures without firstStrike to this
								// list.
			Card c = (Card) it.next();
			if (c.hasFirstStrike() || c.hasDoubleStrike()) {
				out.add(c);
			}
		}

		return out.toArray();
	}// getUnblockedAttackers()

	public void addUnblockedAttacker(Card c) {
		unblockedMap.put(c, new CardList());
	}

}// Class Combat