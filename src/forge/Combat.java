package forge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class Combat {
	// key is attacker Card
	// value is CardList of blockers
	private Map<Card, CardList> map = new HashMap<Card, CardList>();
	private Set<Card> blocked = new HashSet<Card>();

	private HashMap<Card, CardList> unblockedMap = new HashMap<Card, CardList>();
	private HashMap<Card, Integer> defendingDamageMap = new HashMap<Card, Integer>();

	// Defenders are the Defending Player + Each Planeswalker that player controls
	private ArrayList<Object> defenders = new ArrayList<Object>();
	private int currentDefender = 0;
	private int nextDefender = 0;
	
	// This Hash keeps track of 
	private HashMap<Card, Object> attackerToDefender = new HashMap<Card, Object>();
	
	private int attackingDamage;

	private Player attackingPlayer = null;
	private Player defendingPlayer = null;

	private CardList attackersWithLure = new CardList();
	private CardList canBlockAttackerWithLure = new CardList();

	public Combat() {
		// Let the Begin Turn/Untap Phase Reset Combat properly
	}

	public void reset() {
		resetAttackers();
		blocked.clear();

		unblockedMap.clear();

		attackingDamage = 0;
		defendingDamageMap.clear();

		attackingPlayer = null;
		defendingPlayer = null;
		
		attackersWithLure.clear();
		canBlockAttackerWithLure.clear();
		
		defenders.clear();
		currentDefender = 0;
		nextDefender = 0;
		
		initiatePossibleDefenders(AllZone.Phase.getPlayerTurn().getOpponent());
	}

	public void initiatePossibleDefenders(Player defender){
		defenders.add(defender);
		CardList planeswalkers = AllZoneUtil.getPlayerCardsInPlay(defender);
		planeswalkers = planeswalkers.getType("Planeswalker");
		for(Card pw : planeswalkers)
			defenders.add(pw);
	}
	
	public Object nextDefender(){
		if (nextDefender >= defenders.size())
			return null;
		
		currentDefender = nextDefender;
		nextDefender++;

		return defenders.get(currentDefender);
	}
	
	public void setCurrentDefender(int def){
		currentDefender = def;
	}
	
	public int getRemainingDefenders(){
		return defenders.size() - nextDefender;
	}
	
	public ArrayList<Object> getDefenders(){
		return defenders;
	}
	
	public void setDefenders(ArrayList<Object> newDef){
		defenders = newDef;
	}
	
	public Card[] getDefendingPlaneswalkers(){
		Card[] pwDefending = new Card[defenders.size()-1];
		
		int i = 0;
		
		for(Object o : defenders){
			if (o instanceof Card){
				pwDefending[i] = (Card)o;
				i++;
			}
		}
		
		return pwDefending;
	}

	public int getDeclaredAttackers() {
		return attackerToDefender.size();
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

	public HashMap<Card, Integer> getDefendingDamageMap() {
		return defendingDamageMap;
	}
	
	public int getTotalDefendingDamage() {
		int total = 0;

		Collection<Integer> c = defendingDamageMap.values();

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
					|| (getBlockers(att.get(i)).size() == 0 && att.get(i).hasKeyword("Trample"))) {
				
				int damageDealt = att.get(i).getNetCombatDamage();

				if (damageDealt > 0) {
					//if the creature has first strike do not do damage in the normal combat phase
					if (!att.get(i).hasFirstStrike() || att.get(i).hasDoubleStrike())
						addDefendingDamage(damageDealt, att.get(i));
				}
			} // ! isBlocked...
		}// for
	}
	
	
	public boolean setDefendingFirstStrikeDamage() {
		boolean needsFirstStrike = false;
		defendingDamageMap.clear();
		CardList att = new CardList(getAttackers());
		// sum unblocked attackers' power
		for (int i = 0; i < att.size(); i++) {
			if (!isBlocked(att.get(i))) {
				
				int damageDealt = att.get(i).getNetCombatDamage();

				if (damageDealt > 0) {
					// if the creature has first strike or double strike do damage in the first strike combat phase
					if (att.get(i).hasFirstStrike() || att.get(i).hasDoubleStrike()) {
						addDefendingDamage(damageDealt, att.get(i));
						needsFirstStrike = true;
					}
				}
			}
		} // for
		
		return needsFirstStrike;
	}
	

	public void addDefendingDamage(int n, Card source) {
		String slot = getDefenderByAttacker(source).toString();		
		Object o = defenders.get(Integer.parseInt(slot));
		
		if (o instanceof Card){
			Card pw = (Card)o;
			pw.addAssignedDamage(n, source);
			
			return;
		}
		
		if (!defendingDamageMap.containsKey(source))
			defendingDamageMap.put(source, n);
		else {
			defendingDamageMap.put(source, defendingDamageMap.get(source) + n);
		}
	}

	public void addAttackingDamage(int n) {
		attackingDamage += n;
	}

	public int getAttackingDamage() {
		return attackingDamage;
	}

	public CardList[] sortAttackerByDefender(){
		CardList attackers[] = new CardList[defenders.size()];
		for(int i = 0; i < attackers.length; i++)
			attackers[i] = new CardList();

		for(Card atk : attackerToDefender.keySet()){
			Object o = attackerToDefender.get(atk);
			int i = Integer.parseInt(o.toString());
			attackers[i].add(atk);
		}

		return attackers;
	}

	public boolean isAttacking(Card c) {
		return map.get(c) != null;
	}
	
	public void addAttacker(Card c) {
		map.put(c, new CardList());
		attackerToDefender.put(c, currentDefender);
	}
	
	public Object getDefenderByAttacker(Card c) {
		return attackerToDefender.get(c);
	}

	public void resetAttackers() {
		map.clear();
		attackerToDefender.clear();
	}

	public Card[] getAttackers() {
		CardList out = new CardList();
		Iterator<Card> it = map.keySet().iterator();

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
			block.addAll(getBlockers(att.get(i)));

		return block;
	}// getAllBlockers()

	public CardList getBlockers(Card attacker) {
		if (getList(attacker) == null)
			return new CardList();
		else
			return new CardList(getList(attacker));
	}
	
	public Card getAttackerBlockedBy(Card blocker) {
		CardList att = new CardList(getAttackers());
		
		for (int i = 0; i < att.size(); i++) {
			if(getBlockers(att.get(i)).contains(blocker)) return att.get(i);
		} // for
		
		return null;
	}

	private CardList getList(Card attacker) {
		return (CardList) map.get(attacker);
	}

	public void removeFromCombat(Card c) {
		// is card an attacker?
		CardList att = new CardList(getAttackers());
		if (att.contains(c)){
			map.remove(c);
			attackerToDefender.remove(c);
		}
		else// card is a blocker
		{
			for(Card a : att)
				if (getBlockers(a).contains(c)){
					getList(a).remove(c);
					// TODO if Declare Blockers and Declare Blockers (Abilities) merge this logic needs to be tweaked
					if (getBlockers(a).size() == 0 && AllZone.Phase.is(Constant.Phase.Combat_Declare_Blockers))
						blocked.remove(a);
				}
		}
		// update combat 
		CombatUtil.showCombat();
	}// removeFromCombat()

	public void verifyCreaturesInPlay() {
		CardList all = new CardList();
		all.addAll(getAttackers());
		all.addAll(getAllBlockers());

		for (int i = 0; i < all.size(); i++)
			if (!AllZoneUtil.isCardInPlay(all.get(i)))
				removeFromCombat(all.get(i));
	}// verifyCreaturesInPlay()

	public void setUnblocked(){
		CardList attacking = new CardList(getAttackers());
		
		for (Card attacker : attacking) {
			CardList block = getBlockers(attacker);

			if (block.size() == 0){
				// this damage is assigned to a player by  setPlayerDamage()
				addUnblockedAttacker(attacker);
				
				//Run Unblocked Trigger
				HashMap<String,Object> runParams = new HashMap<String,Object>();
				runParams.put("Attacker", attacker);
				AllZone.TriggerHandler.runTrigger("AttackerUnblocked", runParams);

			}
		}
	}
	
	// set Card.setAssignedDamage() for all creatures in combat
	// also assigns player damage by setPlayerDamage()
	public boolean setAssignedFirstStrikeDamage() {
		
		boolean needFirstStrike = setDefendingFirstStrikeDamage();

		CardList block;
		CardList attacking = new CardList(getAttackers());
		
		for (int i = 0; i < attacking.size(); i++) {
			
			Card attacker = attacking.get(i);
			block = getBlockers(attacker);
			
			int damageDealt = attacker.getNetCombatDamage();

			// attacker always gets all blockers' attack

			for (Card b : block) {
				if (b.hasFirstStrike() || b.hasDoubleStrike()) {
					needFirstStrike = true;
					int attack = b.getNetCombatDamage();
					attacker.addAssignedDamage(attack, b);
				}
			}
			
			if (block.size() == 0){
				// this damage is assigned to a player by setDefendingFirstStrikeDamage()
			}
			
			else if (attacker.hasFirstStrike() || attacker.hasDoubleStrike()) {
				needFirstStrike = true;
				if (getAttackingPlayer().isHuman()) {// human attacks
					if (attacker.hasKeyword("Trample") || block.size() > 1)
						AllZone.Display.assignDamage(attacker, block, damageDealt);
					else block.get(0).addAssignedDamage(damageDealt, attacking.get(i));
				}
				else  {// computer attacks
					distributeAIDamage(attacker, block, damageDealt);
				}
			}// if(hasFirstStrike || doubleStrike)
		}// for
		return needFirstStrike;
	}// setAssignedFirstStrikeDamage()
		
	// set Card.setAssignedDamage() for all creatures in combat
	// also assigns player damage by setPlayerDamage()
	public void setAssignedDamage() {
		setDefendingDamage();

		CardList block;
		CardList attacking = new CardList(getAttackers());
		for (int i = 0; i < attacking.size(); i++) {
			
			Card attacker = attacking.get(i);
			block = getBlockers(attacker);
			
			int damageDealt = attacker.getNetCombatDamage();

			// attacker always gets all blockers' attack
			for (Card b : block) {
				if (!b.hasFirstStrike()	|| b.hasDoubleStrike()) {
					int attack = b.getNetCombatDamage();
					attacker.addAssignedDamage(attack, b);
				}
			}

			if (block.size() == 0){
				// this damage is assigned to a player by setDefendingDamage()
			}

			else if (!attacker.hasFirstStrike() || attacker.hasDoubleStrike()) {
				
				if (getAttackingPlayer().isHuman()) {// human attacks
					
					if (attacker.hasKeyword("Trample") || block.size() > 1)
						AllZone.Display.assignDamage(attacker, block, damageDealt);
					else block.get(0).addAssignedDamage(damageDealt, attacking.get(i));
				}
				else  {// computer attacks
						distributeAIDamage(attacker, block, damageDealt);
				}
			}// if !hasFirstStrike ...
		}// for

		// should first strike affect the following?

	}// assignDamage()

	private void distributeAIDamage(Card attacker, CardList block, int damage) {
		Card c = attacker;
		
		if (block.size() == 1) {
			
			Card blocker = block.get(0);
			
			// trample
			if (attacker.hasKeyword("Trample")) {
			
				int damageNeeded = 0;
				
				//TODO: if the human can be killed distribute only the minimum of damage to the blocker
				
				damageNeeded = blocker.getEnoughDamageToKill(damage, attacker, true);
				
				if (damageNeeded > damage) 
					damageNeeded = Math.min(blocker.getLethalDamage(),damage);
				else
					damageNeeded = Math.max(blocker.getLethalDamage(),damageNeeded);
				
				int trample = damage - damageNeeded;
				
				if (0 < trample) 	// If Extra trample damage, assign to defending player/planeswalker
					this.addDefendingDamage(trample, attacker);
				
				blocker.addAssignedDamage(damageNeeded, attacker);
			}
			else blocker.addAssignedDamage(damage, attacker);
		}// 1 blocker
		else {
			boolean killsAllBlockers = true;//Does the attacker deal lethal damage to all blockers
			for (Card b : block) {
				int enoughDamageToKill = b.getEnoughDamageToKill(damage, attacker, true);
				if (enoughDamageToKill <= damage) {
					damage -= enoughDamageToKill;
					CardList cl = new CardList();
					cl.add(attacker);
	
					b.addAssignedDamage(enoughDamageToKill, c);
				}
				else killsAllBlockers = false;
			}// for
	
			// if attacker has no trample, and there's damage left, assign the rest
			// to a random blocker
			if (damage > 0 
					&& !(c.hasKeyword("Trample") 
					&& killsAllBlockers == true)) {
				int index = CardUtil.getRandomIndex(block);
				block.get(index).addAssignedDamage(damage, c);
				damage = 0;
			} else if (c.hasKeyword("Trample") 
							&& killsAllBlockers == true) {
				this.addDefendingDamage(damage, c);
			}
		}
	}// setAssignedDamage()

	public static void dealAssignedDamage(){
		// This function handles both Regular and First Strike combat assignment
        Player player = AllZone.Combat.getDefendingPlayer();
        
        boolean bFirstStrike = AllZone.Phase.is(Constant.Phase.Combat_FirstStrikeDamage);
        
        HashMap<Card, Integer> defMap = AllZone.Combat.getDefendingDamageMap();
        
        for(Entry<Card, Integer> entry : defMap.entrySet()) {
        	player.addCombatDamage(entry.getValue(), entry.getKey());
        }
        
        CardList unblocked = new CardList(bFirstStrike ? AllZone.Combat.getUnblockedAttackers() : 
        	AllZone.Combat.getUnblockedFirstStrikeAttackers());
        
        for(int j = 0; j < unblocked.size(); j++) {
        	if (bFirstStrike)
        		CombatUtil.checkUnblockedAttackers(unblocked.get(j));
        	else{
	            if(!unblocked.getCard(j).hasFirstStrike() && !unblocked.getCard(j).hasDoubleStrike())
	                CombatUtil.checkUnblockedAttackers(unblocked.get(j));
        	}
        }
        
        // this can be much better below here...
        
        CardList combatants = new CardList();
        combatants.addAll(AllZone.Combat.getAttackers());
        combatants.addAll(AllZone.Combat.getAllBlockers());
        combatants.addAll(AllZone.Combat.getDefendingPlaneswalkers());

        Card c;
        for(int i = 0; i < combatants.size(); i++) {
            c = combatants.get(i);
 
            // if no assigned damage to resolve, move to next
            if(c.getTotalAssignedDamage() == 0)
            	continue;

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

        //This was deeper before, but that resulted in the stack entry acting like before.

	}
	
	public boolean isUnblocked(Card att){
		return unblockedMap.containsKey(att);
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