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
		defendingFirstStrikeDamageMap.clear();

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
	
	public void setAttackersWithLure(CardList list) {
		attackersWithLure = list;
	}

	public void addAttackersWithLure(Card a) {
		attackersWithLure.add(a);
	}
	
	public CardList getAttackersWithLure() {
		return attackersWithLure;
	}
	
	public boolean isAttackerWithLure(Card c) {
		return attackersWithLure.contains(c);
	}
	
	public void setCanBlockAttackerWithLure(CardList list) {
		canBlockAttackerWithLure = list;
	}

	public void addCanBlockAttackerWithLure(Card a) {
		canBlockAttackerWithLure.add(a);
	}
	
	public CardList getCanBlockAttackerWithLure() {
		return canBlockAttackerWithLure;
	}
	
	public boolean canBlockAttackerWithLure(Card c) {
		return canBlockAttackerWithLure.contains(c);
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
					// if the creature has first strike do not do damage in the normal combat phase
					if (!att.get(i).hasFirstStrike()
							|| (att.get(i).hasFirstStrike() && att.get(i).hasDoubleStrike()))
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
					// if the creature has first strike or double strike do damage in the first strike combat phase
					if (att.get(i).hasFirstStrike()
							|| att.get(i).hasDoubleStrike()) {
						addDefendingFirstStrikeDamage(damageDealt, att.get(i));
					}
				}
			}
		} // for
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

	public void addDefendingFirstStrikeDamage(int n, Card source) {
		String slot = getDefenderByAttacker(source).toString();		
		Object o = defenders.get(Integer.parseInt(slot));
		
		if (o instanceof Card){
			Card pw = (Card)o;
			pw.addAssignedDamage(n, source);
			
			return;
		}
		
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
		if (att.contains(c)){
			map.remove(c);
			attackerToDefender.remove(c);
		}
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

			if (block.size() == 0){
				// this damage is assigned to a player by  setPlayerDamage()
				addUnblockedAttacker(attacking.get(i));
			}

			else if (attacking.get(i).hasFirstStrike() || (attacking.get(i).hasDoubleStrike())) {
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
				} 
				else{
					// human
					if (attacking.get(i).hasFirstStrike() || attacking.get(i).hasDoubleStrike()) {
						int damageDealt = attacking.get(i).getNetAttack();
						if (CombatUtil.isDoranInPlay())
							damageDealt = attacking.get(i).getNetDefense();
						AllZone.Display.assignDamage(attacking.get(i), block, damageDealt);

					}
				}

			}// if(hasFirstStrike || doubleStrike)
		}// for
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
			block = getBlockers(attacking.get(i));

			// attacker always gets all blockers' attack

			for (Card b : block) {
				if (!b.hasFirstStrike()
						|| (b.hasFirstStrike() && b.hasDoubleStrike())) {
					int attack = b.getNetAttack();
					if (CombatUtil.isDoranInPlay())
						attack = b.getNetDefense();
					attacking.get(i).addAssignedDamage(attack, b);
				}
			}

			if (block.size() == 0){
				// this damage is assigned to a player by setPlayerDamage()
				addUnblockedAttacker(attacking.get(i));
			}

			else if (!attacking.get(i).hasFirstStrike() || attacking.get(i).hasDoubleStrike()) {
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

				} 
				else{ // human attacks
					int damageDealt = attacking.get(i).getNetAttack();
					if (CombatUtil.isDoranInPlay())
						damageDealt = attacking.get(i).getNetDefense();

					AllZone.Display.assignDamage(attacking.get(i), block, damageDealt);

				}

			}// if !hasFirstStrike ...
		}// for

		// should first strike affect the following?

	}// assignDamage()

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
	            if(!unblocked.getCard(j).hasFirstStrike() && !unblocked.getCard(j).hasDoubleStrike())
	                CombatUtil.checkUnblockedAttackers(unblocked.get(j));
        	}
        }
        
        // this can be much better below here...
        
        CardList combatants = new CardList();
        combatants.addAll(AllZone.Combat.getAttackers());
        combatants.add(AllZone.Combat.getAllBlockers());
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
                if(CardFactoryUtil.canDamage(crd, c))
                	damageMap.put(crd, entry.getValue());
            }
            c.addCombatDamage(damageMap);
            
            damageMap.clear();
            c.clearAssignedDamage();
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