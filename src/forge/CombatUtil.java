
package forge;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.esotericsoftware.minlog.Log;

import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability;
import forge.card.trigger.Trigger;
import forge.gui.GuiUtils;
import forge.gui.input.Input_PayManaCost_Ability;


public class CombatUtil {
	private static boolean Lorthos_Cancelled;
	
	
	//can the creature block given the combat state?
	public static boolean canBlock(Card blocker, Combat combat) {
		
		if(blocker == null) return false;
		
        if (combat.getAllBlockers().size() > 1 && AllZoneUtil.isCardInPlay("Caverns of Despair"))
        	return false;
        
        if (combat.getAllBlockers().size() > 0 && AllZoneUtil.isCardInPlay("Silent Arbiter"))
        	return false;
        
        if (combat.getAllBlockers().size() > 0 && AllZoneUtil.isCardInPlay("Dueling Grounds"))
        	return false;
        
		
		return canBlock(blocker);
	}
	
	
	//can the creature block at all?
	public static boolean canBlock(Card blocker) {
		
		if(blocker == null) return false;
		
		if(blocker.isTapped() && !AllZoneUtil.isCardInPlay("Masako the Humorless",blocker.getController())) return false;
		
        if(blocker.getKeyword().contains("CARDNAME can't block.") || blocker.getKeyword().contains("CARDNAME can't attack or block.")) 
        	return false;
        
        CardList kulrath = AllZoneUtil.getCardsInPlay("Kulrath Knight");
        if (kulrath.size() > 0)
        {
        	for (int i=0; i<kulrath.size(); i++)
        	{
        		Card cKK = kulrath.get(i);
        		Player oppKK = cKK.getController().getOpponent();
        		
        		if (blocker.getController().equals(oppKK) && blocker.hasCounters())
        			return false;
        	}
        }
		
		return true;
	}
	
	//can the attacker be blocked at all?
	public static boolean canBeBlocked(Card attacker, Combat combat) {
    	
        if(attacker == null) return true;
        
        if (attacker.getKeyword().contains("CARDNAME can't be blocked by more than one creature.") 
        		&& combat.getBlockers(attacker).size() > 0)  return false;
        
        return canBeBlocked(attacker);
	}
	
	//can the attacker be blocked at all?
	public static boolean canBeBlocked(Card attacker) {
    	
        if(attacker == null) return true;
        
        if(attacker.getKeyword().contains("Unblockable")) return false;
        
        //Landwalk
        if (!AllZoneUtil.isCardInPlay("Staff of the Ages")) { //"Creatures with landwalk abilities can be blocked as though they didn't have those abilities."
        	PlayerZone blkPZ = AllZone.getZone(Constant.Zone.Battlefield, attacker.getController().getOpponent());
        	CardList blkCL = new CardList(blkPZ.getCards());
        	CardList temp = new CardList();
        
	        if(attacker.getKeyword().contains("Plainswalk")) {
	            temp = blkCL.getType("Plains");
	            if(!AllZoneUtil.isCardInPlay("Lord Magnus") 
	            		&& !AllZoneUtil.isCardInPlay("Great Wall")
	            		&& !temp.isEmpty()) return false;
	        }
	        
	        if(attacker.getKeyword().contains("Islandwalk")) {
	            temp = blkCL.getType("Island");
	            if(!AllZoneUtil.isCardInPlay("Undertow") 
	            		&& !AllZoneUtil.isCardInPlay("Gosta Dirk")
	            		&& !temp.isEmpty()) return false;
	        }
	        
	        if(attacker.getKeyword().contains("Swampwalk")) {
	            temp = blkCL.getType("Swamp");
	            if(!AllZoneUtil.isCardInPlay("Ur-drago") 
	            		&& !AllZoneUtil.isCardInPlay("Quagmire")
	            		&& !temp.isEmpty()) return false;
	        }
	        
	        if(attacker.getKeyword().contains("Mountainwalk")) {
	            temp = blkCL.getType("Mountain");
	            if(!AllZoneUtil.isCardInPlay("Crevasse") 
	            		&& !temp.isEmpty()) return false;
	        }
	        
	        if(attacker.getKeyword().contains("Forestwalk")) {
	            temp = blkCL.getType("Forest");
	            if(!AllZoneUtil.isCardInPlay("Lord Magnus")
	            		&& !AllZoneUtil.isCardInPlay("Deadfall")
	            		&& !temp.isEmpty()) return false;
	        }
	        
	        if(attacker.getKeyword().contains("Legendary landwalk")) {
	            temp = blkCL.filter(new CardListFilter() {
	                public boolean addCard(Card c) {
	                    return c.isLand() && c.getType().contains("Legendary");
	                }
	            });
	            if(!temp.isEmpty()) return false;
	        }
	        
	        if(attacker.getKeyword().contains("Snow swampwalk")) {
	            temp = blkCL.filter(new CardListFilter() {
	                public boolean addCard(Card c) {
	                    return c.isType("Swamp") && c.isSnow();
	                }
	            });
	            if(!temp.isEmpty()) return false;
	        }
	        
	        if(attacker.getKeyword().contains("Snow forestwalk")) {
	            temp = blkCL.filter(new CardListFilter() {
	                public boolean addCard(Card c) {
	                    return c.isType("Forest") && c.isSnow();
	                }
	            });
	            if(!temp.isEmpty()) return false;
	        }
	        
	        if(attacker.getKeyword().contains("Snow islandwalk")) {
	            temp = blkCL.filter(new CardListFilter() {
	                public boolean addCard(Card c) {
	                    return c.isType("Island") && c.isSnow();
	                }
	            });
	            if(!temp.isEmpty()) return false;
	        }
	        
	        if(attacker.getKeyword().contains("Snow plainswalk")) {
	            temp = blkCL.filter(new CardListFilter() {
	                public boolean addCard(Card c) {
	                    return c.isType("Plains") && c.isSnow();
	                }
	            });
	            if(!temp.isEmpty()) return false;
	        }
	        
	        if(attacker.getKeyword().contains("Snow mountainwalk")) {
	            temp = blkCL.filter(new CardListFilter() {
	                public boolean addCard(Card c) {
	                    return c.isType("Mountain") && c.isSnow();
	                }
	            });
	            if(!temp.isEmpty()) return false;
	        }
	        
	        if(attacker.getKeyword().contains("Snow landwalk")) {
	            temp = blkCL.filter(new CardListFilter() {
	                public boolean addCard(Card c) {
	                    return c.isLand() && c.isSnow();
	                }
	            });
	            if(!temp.isEmpty()) return false;
	        }
	        
	        if(attacker.getKeyword().contains("Desertwalk")) {
	            temp = blkCL.filter(new CardListFilter() {
	                public boolean addCard(Card c) {
	                    return c.isLand() && c.getType().contains("Desert");
	                }
	            });
	            if(!temp.isEmpty()) return false;
	        }
	        
	        if(attacker.getKeyword().contains("Nonbasic landwalk")) {
	            temp = blkCL.filter(new CardListFilter() {
	                public boolean addCard(Card c) {
	                    return c.isLand() && !c.isBasicLand();
	                }
	            });
	            if(!temp.isEmpty()) return false;
	        }
        }
        return true;
	}
        
	
	// can the blocker block the attacker given the combat state?
    public static boolean canBlock(Card attacker, Card blocker, Combat combat) {
    	
        if(attacker == null || blocker == null) return false;
        
    	if (canBlock(blocker, combat) == false) return false;
    	if (canBeBlocked(attacker, combat) == false) return false;
    	
    	if (!combat.isAttackerWithLure(attacker) && combat.canBlockAttackerWithLure(blocker)) return false;
        
        return canBlock(attacker, blocker);
    }
	
        
	// can the blocker block the attacker?
    public static boolean canBlock(Card attacker, Card blocker) {
    	
        if(attacker == null || blocker == null) return false;
    	
    	if (canBlock(blocker) == false) return false;
    	if (canBeBlocked(attacker) == false) return false;
        
        if(CardFactoryUtil.hasProtectionFrom(blocker,attacker)) return false;
        
        //rare case:
        if(blocker.getKeyword().contains("Shadow")
                && blocker.getKeyword().contains(
                        "CARDNAME can block creatures with shadow as though they didn't have shadow.")) return false;
        
        if(attacker.getKeyword().contains("Shadow")
                && !blocker.getKeyword().contains("Shadow")
                && !blocker.getKeyword().contains(
                        "CARDNAME can block creatures with shadow as though they didn't have shadow.")) return false;
        
        if(!attacker.getKeyword().contains("Shadow") && blocker.getKeyword().contains("Shadow")) return false;

        if(blocker.hasKeyword("CARDNAME can't block white creatures with power 2 or greater.")) {
        	if(attacker.isWhite() && attacker.getNetAttack() >= 2) return false;
        }
        
        // CARDNAME can't block creatures with power ...
        int powerLimit[] = {0};
        int keywordPosition = 0;
        boolean hasKeyword = false;
        
        ArrayList<String> blockerKeywords = blocker.getKeyword();
        for (int i = 0; i < blockerKeywords.size(); i++) {
        	if (blockerKeywords.get(i).toString().startsWith("CARDNAME can't block creatures with power")) {
        		hasKeyword = true;
        		keywordPosition = i;
        	}
        }
        
        if (attacker.getKeyword().contains("Creatures with power less than CARDNAME's power can't block it.") &&
    			attacker.getNetAttack() > blocker.getNetAttack()) return false;
        
        if (hasKeyword) {    // The keyword "CARDNAME can't block creatures with power" ... is present
        	String tmpString = blocker.getKeyword().get(keywordPosition).toString();
        	String asSeparateWords[]  = tmpString.trim().split(" ");
        	
        	if (asSeparateWords.length >= 9) {
        		if (asSeparateWords[6].matches("[0-9][0-9]?")) {
        			powerLimit[0] = Integer.parseInt((asSeparateWords[6]).trim());
        			
        			if (attacker.getNetAttack() >= powerLimit[0] && blocker.getKeyword().contains
        					("CARDNAME can't block creatures with power " + powerLimit[0] + " or greater.")) return false;
        			if (attacker.getNetAttack() <= powerLimit[0] && blocker.getKeyword().contains
        					("CARDNAME can't block creatures with power " + powerLimit[0] + " or less.")) return false;
        		}
        	}
        	
        	if (attacker.getNetAttack() > blocker.getNetAttack()
					&& blocker.getKeyword().contains("CARDNAME can't block creatures with power greater than CARDNAME's power.")) return false;
        	if (attacker.getNetAttack() >= blocker.getNetDefense()
        			&& blocker.getKeyword().contains("CARDNAME can't block creatures with power equal to or greater than CARDNAME's toughness.")) return false;

        }// hasKeyword CARDNAME can't block creatures with power ...
        
        // CARDNAME can't be blocked by creatures with power ...
        int powerLimit2[] = {0};
        int keywordPosition2 = 0;
        boolean hasKeyword2 = false;
        
        ArrayList<String> attackerKeywords = attacker.getKeyword();
        for (int i = 0; i < attackerKeywords.size(); i++) {
            if (attackerKeywords.get(i).toString().startsWith("CARDNAME can't be blocked by creatures with power")) {
                hasKeyword2 = true;
                keywordPosition2 = i;
            }
        }
        
        if (hasKeyword2) {    // The keyword "CARDNAME can't be blocked by creatures with power" ... is present
            String tmpString = attacker.getKeyword().get(keywordPosition2).toString();
            String asSeparateWords[] = tmpString.trim().split(" ");
        
            if (asSeparateWords.length >= 9) {
                if (asSeparateWords[8].matches("[0-9][0-9]?")) {
                    powerLimit2[0] = Integer.parseInt((asSeparateWords[8]).trim());
        
                    if (blocker.getNetAttack() >= powerLimit2[0] && attacker.getKeyword().contains
                        ("CARDNAME can't be blocked by creatures with power " + powerLimit2[0] + " or greater.")) return false;
                    if (blocker.getNetAttack() <= powerLimit2[0] && attacker.getKeyword().contains
                        ("CARDNAME can't be blocked by creatures with power " + powerLimit2[0] + " or less.")) return false;
                }
            }
        
            if (blocker.getNetAttack() > attacker.getNetAttack() && 
                    blocker.getKeyword().contains("CARDNAME can't be blocked by creatures with power greater than CARDNAME's power.")) 
            	return false;
            if (blocker.getNetAttack() >= attacker.getNetDefense() && blocker.getKeyword().contains(
            		"CARDNAME can't be blocked by creatures with power equal to or greater than CARDNAME's toughness.")) 
            	return false;
        
        }// hasKeyword CARDNAME can't be blocked by creatures with power ...
        
        if(attacker.hasStartOfKeyword("CantBeBlockedBy")) {
        	int KeywordPosition = attacker.getKeywordPosition("CantBeBlockedBy");
        	String parse = attacker.getKeyword().get(KeywordPosition).toString();
    		String k[] = parse.split(" ",2);
    		final String restrictions[] = k[1].split(",");
    		if(blocker.isValidCard(restrictions, attacker.getController(), attacker))
    			return false;
        }
        
        if(attacker.hasKeyword("CARDNAME can't be blocked by black creatures.") && blocker.isBlack()) return false;
        if(attacker.hasKeyword("CARDNAME can't be blocked by blue creatures.") && blocker.isBlue()) return false;
        if(attacker.hasKeyword("CARDNAME can't be blocked by green creatures.") && blocker.isGreen()) return false;
        if(attacker.hasKeyword("CARDNAME can't be blocked by red creatures.") && blocker.isRed()) return false;
        if(attacker.hasKeyword("CARDNAME can't be blocked by white creatures.") && blocker.isWhite()) return false;

        if(blocker.getKeyword().contains("CARDNAME can block only creatures with flying.")
                && !attacker.getKeyword().contains("Flying")) return false;
        
        if(attacker.getKeyword().contains("Flying")) {
            if(!blocker.getKeyword().contains("Flying")
                    && !blocker.getKeyword().contains("CARDNAME can block creatures with flying.")
                    && !blocker.getKeyword().contains("Reach")) return false;
        }

        if(attacker.getKeyword().contains("Horsemanship")) {
            if(!blocker.getKeyword().contains("Horsemanship")) return false;
        }
        
        if(attacker.getKeyword().contains("Fear")) {
            if(!blocker.isArtifact() && !blocker.isBlack())
            return false;
        }
        
        if(attacker.getKeyword().contains("Intimidate")) {
            if(!blocker.isArtifact() && !blocker.sharesColorWith(attacker))
            return false;
        }
        
        if (attacker.getKeyword().contains("CARDNAME can't be blocked by Walls.") && blocker.isType("Wall")) return false;
        
        if (attacker.getKeyword().contains("CARDNAME can't be blocked except by Walls.") && !blocker.isType("Wall")) return false;
        
        if(AllZoneUtil.isCardInPlay("Shifting Sliver")) {
        	if(attacker.isType("Sliver") && !blocker.isType("Sliver")) return false;
        }
        
        return true;
    }//canBlock()
    
    //can a creature attack given the combat state
    public static boolean canAttack(Card c, Combat combat) {
    	
        if (combat.getAttackers().length > 1 && AllZoneUtil.isCardInPlay("Crawlspace",c.getController().getOpponent()))
        	return false;
        
        if (combat.getAttackers().length > 1 && AllZoneUtil.isCardInPlay("Caverns of Despair"))
        	return false;
        
        if (combat.getAttackers().length > 0 && AllZoneUtil.isCardInPlay("Silent Arbiter"))
        	return false;
        
        if (combat.getAttackers().length > 0 && AllZoneUtil.isCardInPlay("Dueling Grounds"))
        	return false;
    	
    	return canAttack(c);
    }
    
    //can a creature attack at the moment?
    public static boolean canAttack(Card c) {
    	if(c.isTapped() || (c.isSick() && !c.isEnchantedBy("Instill Energy"))) return false;
    	
    	return canAttackNextTurn(c);
    }
    
    //can a creature attack if untapped and without summoning sickness?
    public static boolean canAttackNextTurn(Card c) {
    	if (!c.isCreature()) return false;
    	
        if(AllZoneUtil.isCardInPlay("Peacekeeper")) return false;
                
        // CARDNAME can't attack if defending player controls an untapped creature with power ...
        final int powerLimit[] = {0};
        int keywordPosition = 0;
        boolean hasKeyword = false;
        
        ArrayList<String> attackerKeywords = c.getKeyword();
        for (int i = 0; i < attackerKeywords.size(); i++) {
            if (attackerKeywords.get(i).toString().startsWith("CARDNAME can't attack if defending player controls an untapped creature with power")) {
                hasKeyword = true;
                keywordPosition = i;
           }
        }
        
        // The keyword "CARDNAME can't attack if defending player controls an untapped creature with power" ... is present
        if (hasKeyword) {    
            String tmpString = c.getKeyword().get(keywordPosition).toString();
            final String asSeparateWords[]  = tmpString.trim().split(" ");
            
            if (asSeparateWords.length >= 15) {
                if (asSeparateWords[12].matches("[0-9][0-9]?")) {
                    powerLimit[0] = Integer.parseInt((asSeparateWords[12]).trim());
                    
                    CardList list = AllZoneUtil.getCreaturesInPlay(c.getController().getOpponent());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card ct) {
                            return ((ct.isUntapped() && ct.getNetAttack() >= powerLimit[0] && asSeparateWords[14].contains("greater")) ||
                                    (ct.isUntapped() && ct.getNetAttack() <= powerLimit[0] && asSeparateWords[14].contains("less")));
                        }
                    });
                    if (!list.isEmpty()) return false;
                }
            }
        } // hasKeyword = CARDNAME can't attack if defending player controls an untapped creature with power ...
        
        CardList list = AllZoneUtil.getPlayerCardsInPlay(c.getController().getOpponent());
        CardList temp;
        
        if(c.getKeyword().contains("CARDNAME can't attack unless defending player controls an Island.")) {
            temp = list.getType("Island");
            if(temp.isEmpty()) return false;
        }
        
        if(c.getKeyword().contains("CARDNAME can't attack unless defending player controls a Forest.")) {
            temp = list.getType("Forest");
            if(temp.isEmpty()) return false;
        }
        
        if(c.getKeyword().contains("CARDNAME can't attack unless defending player controls a Swamp.")) {
            temp = list.getType("Swamp");
            if(temp.isEmpty()) return false;
        }
        
        if(c.getKeyword().contains("CARDNAME can't attack unless defending player controls a blue permanent.")) {
            temp = list.getColor(Constant.Color.Blue);
            if(temp.isEmpty()) return false;
        }
    	
        if(c.getName().equals("Harbor Serpent")) {
        	CardList allislands = AllZoneUtil.getTypeInPlay("Island");
            if(allislands.size() < 5) return false;
        }
        
        //The creature won't untap next turn
        if(c.isTapped() && !PhaseUtil.canUntap(c)) return false;
        
        if(AllZoneUtil.isCardInPlay("Blazing Archon", c.getController().getOpponent()) 
                || c.getKeyword().contains("CARDNAME can't attack.")
                || c.getKeyword().contains("CARDNAME can't attack or block.")
                || (AllZoneUtil.isCardInPlay("Reverence", c.getController().getOpponent()) && c.getNetAttack() < 3))
        	return false;
        
        if(c.getKeyword().contains("Defender") && !c.hasKeyword("CARDNAME can attack as though it didn't have defender.")) {
        	return false;
        }
        
        if (AllZoneUtil.isCardInPlay("Ensnaring Bridge")) {
        	int limit = Integer.MAX_VALUE;
        	CardList Human = new CardList();
        	Human.addAll(AllZone.Human_Battlefield.getCards());
        	if (Human.getName("Ensnaring Bridge").size() > 0) {
        		CardList Hand = new CardList();
        		Hand.addAll(AllZone.getZone(Constant.Zone.Hand, AllZone.HumanPlayer).getCards());
        		limit = Hand.size();
        	}
        	CardList Compi = new CardList();
        	Compi.addAll(AllZone.Computer_Battlefield.getCards());
        	if (Compi.getName("Ensnaring Bridge").size() > 0) {
        		CardList Hand = new CardList();
        		Hand.addAll(AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer).getCards());
        		if (Hand.size() < limit) limit = Hand.size();
        	}
        	if (c.getNetAttack() > limit) return false;
        }
        
        if (AllZoneUtil.isCardInPlay("Kulrath Knight"))
        {
        	CardList all = AllZoneUtil.getCardsInPlay("Kulrath Knight");
        	for (int i=0; i<all.size(); i++)
        	{
        		Card cKK = all.get(i);
        		Player oppKK = cKK.getController().getOpponent();
        		
        		if (c.getController().equals(oppKK) && c.hasCounters())
        			return false;
        	}
        }

        return true;
    }//canAttack()
    
    
    public static int getTotalFirstStrikeBlockPower(Card attacker, Player player)
    {
    	final Card att = attacker;
    	
    	CardList list = AllZoneUtil.getCreaturesInPlay(player);
    	list = list.filter(new CardListFilter()
    	{
    		public boolean addCard(Card c)
    		{
    			return canBlock(att, c) && (c.hasFirstStrike() || c.hasDoubleStrike() ) ;
    		}
    	});
    	
    	return totalDamageOfBlockers(attacker, list);
    	
    }
    
    //This function takes Doran and Double Strike into account
    public static int getAttack(Card c)
    {
       int n = c.getNetCombatDamage();

       if(c.hasDoubleStrike())
          n *= 2;

       return n;
    }
    
    //Returns the damage unblocked attackers would deal
    private static int sumAttack(CardList attackers, Player attacked)
    {
       int sum = 0;
 	  for(int i = 0; i < attackers.size(); i++) {
 		  Card a = attackers.get(i);
 		  if (!a.hasKeyword("Infect")) sum += attacked.predictDamage(getAttack(a), a, true);
 	  }

       return sum;
    }
    
  //Returns the number of poison counters unblocked attackers would deal
    private static int sumPoison(CardList attackers, Player attacked)
    {
       int sum = 0;
 	  for(int i = 0; i < attackers.size(); i++) {
 		  Card a = attackers.get(i);
 		  int damage = attacked.predictDamage(getAttack(a), a, true);
 		  if (a.hasKeyword("Infect")) sum += damage;
 		  if (a.hasKeyword("Poisonous") && damage > 0) sum += a.getKeywordMagnitude("Poisonous");
 	  }

       return sum;
    }
    
    //Checks if the life of the attacked Player/Planeswalker is in danger 
    public static boolean lifeInDanger(Combat combat) {
  	   // life in danger only cares about the player's life. Not about a Planeswalkers life
    	
 	   int damage = 0;
 	   int poison = 0;

 	   CardList attackers = combat.sortAttackerByDefender()[0];
 	   CardList unblocked = new CardList();
 	   
 	   for(Card attacker : attackers) {
 			  
 			  CardList blockers = combat.getBlockers(attacker);
 			  
 			  if(blockers.size() == 0) unblocked.add(attacker);
 			  else if(attacker.hasKeyword("Trample") && getAttack(attacker) > CombatUtil.totalShieldDamage(attacker,blockers)) {
 				  if(!attacker.hasKeyword("Infect"))	  
 					  damage += getAttack(attacker) - CombatUtil.totalShieldDamage(attacker,blockers);
 				  if(attacker.hasKeyword("Infect"))
 					  poison += getAttack(attacker) - CombatUtil.totalShieldDamage(attacker,blockers);
 				  if(attacker.hasKeyword("Poisonous"))
 					  poison += attacker.getKeywordMagnitude("Poisonous");
 			  }
 	   }
 	   
 	   damage += sumAttack(unblocked, AllZone.ComputerPlayer);
 	   poison += sumPoison(unblocked, AllZone.ComputerPlayer);
 	   
	   return (damage + 3 > AllZone.ComputerPlayer.getLife() || poison + 2 > 10 - AllZone.ComputerPlayer.getPoisonCounters());
    }
    
    // This calculates the amount of damage a blockgang can deal to the attacker (first strike not supported)
    public static int totalDamageOfBlockers(Card attacker, CardList defenders) {
    	int damage = 0;
    	
    	for (Card defender:defenders) damage += dealsDamageAsBlocker(attacker, defender);
    	
    	return damage;
    }
    	
    
    // This calculates the amount of damage a blocker in a blockgang can deal to the attacker
    public static int dealsDamageAsBlocker(Card attacker, Card defender) {
    	
    	if(attacker.getName().equals("Sylvan Basilisk") && !defender.getKeyword().contains("Indestructible")) 
			return 0;
    
    	int flankingMagnitude = 0;
    	if(attacker.getKeyword().contains("Flanking") && !defender.getKeyword().contains("Flanking")) {
        
    		flankingMagnitude = attacker.getAmountOfKeyword("Flanking");

    		if(flankingMagnitude >= defender.getNetDefense()) return 0;
    		if(flankingMagnitude >= defender.getNetDefense() - defender.getDamage() && !defender.getKeyword().contains("Indestructible")) 
        	return 0;
        
    	}//flanking
        if(attacker.getKeyword().contains("Indestructible") && 
        		!(defender.getKeyword().contains("Wither") || defender.getKeyword().contains("Infect"))) return 0;
        
        int defBushidoMagnitude = defender.getKeywordMagnitude("Bushido");
        
        int defenderDamage = defender.getNetCombatDamage() - flankingMagnitude + defBushidoMagnitude;
        
        // consider static Damage Prevention
        defenderDamage = attacker.predictDamage(defenderDamage, defender, true);
        
        if (defender.hasKeyword("Double Strike")) defenderDamage += attacker.predictDamage(defenderDamage, defender, true);
        
        return defenderDamage;
    }
    
    // This calculates the amount of damage a blocker in a blockgang can take from the attacker (for trampling attackers)
    public static int totalShieldDamage(Card attacker, CardList defenders) {
    
    	int defenderDefense = 0;
    	
    	for (Card defender:defenders) defenderDefense += shieldDamage(attacker, defender);
        
        return defenderDefense;
    }
    
    // This calculates the amount of damage a blocker in a blockgang can take from the attacker (for trampling attackers)
    public static int shieldDamage(Card attacker, Card defender) {
    	
    	if (!canDestroyBlocker(defender,attacker)) return 100;
    
    	int flankingMagnitude = 0;
    	if(attacker.getKeyword().contains("Flanking") && !defender.getKeyword().contains("Flanking")) {
        
    		flankingMagnitude = attacker.getAmountOfKeyword("Flanking");

    		if(flankingMagnitude >= defender.getNetDefense()) return 0;
    		if(flankingMagnitude >= defender.getNetDefense() - defender.getDamage() && !defender.getKeyword().contains("Indestructible")) 
        	return 0;
        
    	}//flanking
        
        int defBushidoMagnitude = defender.getKeywordMagnitude("Bushido");
        
        int defenderDefense = defender.getNetDefense() - flankingMagnitude + defBushidoMagnitude;
        
        return defenderDefense;
    }//shieldDamage
    
    //For AI safety measures like Regeneration
    public static boolean combatantWouldBeDestroyed(Card combatant) {

    	if(combatant.isAttacking())
    		return attackerWouldBeDestroyed(combatant);
    	if(combatant.isBlocking())
    		return blockerWouldBeDestroyed(combatant);
    	return false;
    }
    
    //For AI safety measures like Regeneration
    public static boolean attackerWouldBeDestroyed(Card attacker) {
    	CardList blockers = AllZone.Combat.getBlockers(attacker);
    	
    	for (Card defender:blockers) {
    			if(CombatUtil.canDestroyAttacker(attacker, defender) && 
    					!(defender.getKeyword().contains("Wither") || defender.getKeyword().contains("Infect")))
    				return true;
    	}
    	
    	return totalDamageOfBlockers(attacker, blockers) >= attacker.getKillDamage();
    }
    
    //Will this trigger trigger?
    public static boolean combatTriggerWillTrigger(Card attacker, Card defender, Trigger trigger) {
		HashMap<String,String> trigParams = trigger.getMapParams();
		boolean willTrigger = false;
		Card source = trigger.getHostCard();
		
		if (!trigger.zonesCheck()) return false;
		if (!trigger.requirementsCheck()) return false;
		
		if (trigParams.get("Mode").equals("Attacks")) {
			willTrigger = true;
			if (attacker.isAttacking()) return false; //The trigger should have triggered already
			if(trigParams.containsKey("ValidCard"))
				if(!trigger.matchesValid(attacker, trigParams.get("ValidCard").split(","), source))
					return false;
		}
		
		if (trigParams.get("Mode").equals("Blocks")) {
			willTrigger = true;
			if(trigParams.containsKey("ValidBlocked"))
				if(!trigger.matchesValid(attacker, trigParams.get("ValidBlocked").split(","), source))
					return false;
			if(trigParams.containsKey("ValidCard"))
				if(!trigger.matchesValid(defender, trigParams.get("ValidCard").split(","), source))
					return false;
		}
		
		else if (trigParams.get("Mode").equals("AttackerBlocked")) {
			willTrigger = true;
			if(trigParams.containsKey("ValidBlocker"))
				if(!trigger.matchesValid(defender, trigParams.get("ValidBlocker").split(","), source))
					return false;
			if(trigParams.containsKey("ValidCard"))
				if(!trigger.matchesValid(attacker, trigParams.get("ValidCard").split(","), source))
					return false;
		}
		
		return willTrigger;
    }
    
    //Predict the Power bonus of the blocker if blocking the attacker (Flanking, Bushido and other triggered abilities)
    public static int predictPowerBonusOfBlocker(Card attacker, Card defender) {
    	int power = 0;
    	
        if(attacker.getKeyword().contains("Flanking") && !defender.getKeyword().contains("Flanking"))          
        	power -= attacker.getAmountOfKeyword("Flanking");
        	
        power += defender.getKeywordMagnitude("Bushido");
        
        ArrayList<Trigger> registeredTriggers = AllZone.TriggerHandler.getRegisteredTriggers();
		for(Trigger trigger : registeredTriggers)
		{
			HashMap<String,String> trigParams = trigger.getMapParams();
			Card source = trigger.getHostCard();
				
			if(combatTriggerWillTrigger(attacker, defender, trigger) && trigParams.containsKey("Execute")) {
				String ability = source.getSVar(trigParams.get("Execute"));
				AbilityFactory AF = new AbilityFactory();
        		HashMap<String,String> abilityParams = AF.getMapParams(ability, source);
        		if (abilityParams.containsKey("AB")) { 
					if (abilityParams.get("AB").equals("Pump"))
						if (!abilityParams.containsKey("ValidTgts") && !abilityParams.containsKey("Tgt"))
						if (AbilityFactory.getDefinedCards(source, trigParams.get("Defined"), null).contains(defender))
						if (abilityParams.containsKey("NumAtt")){
							String att = abilityParams.get("NumAtt");
							if (att.startsWith("+"))
								att = att.substring(1);
							power += Integer.parseInt(att);
						}
        		}
			}
		}
    	return power;
    }
    
    //Predict the Toughness bonus of the blocker if blocking the attacker (Flanking, Bushido and other triggered abilities)
    public static int predictToughnessBonusOfBlocker(Card attacker, Card defender) {
    	int toughness = 0;
    	
        if(attacker.getKeyword().contains("Flanking") && !defender.getKeyword().contains("Flanking"))          
        	toughness -= attacker.getAmountOfKeyword("Flanking");
        	
        toughness += defender.getKeywordMagnitude("Bushido");
        
        ArrayList<Trigger> registeredTriggers = AllZone.TriggerHandler.getRegisteredTriggers();
		for(Trigger trigger : registeredTriggers)
		{
			HashMap<String,String> trigParams = trigger.getMapParams();
			Card source = trigger.getHostCard();

			if(combatTriggerWillTrigger(attacker, defender, trigger)  && trigParams.containsKey("Execute")) {
				String ability = source.getSVar(trigParams.get("Execute"));
				AbilityFactory AF = new AbilityFactory();
        		HashMap<String,String> abilityParams = AF.getMapParams(ability, source);
        		if (abilityParams.containsKey("AB")) {
					if (abilityParams.get("AB").equals("Pump"))
						if (!abilityParams.containsKey("ValidTgts") && !abilityParams.containsKey("Tgt"))
						if (AbilityFactory.getDefinedCards(source, trigParams.get("Defined"), null).contains(defender))
						if (abilityParams.containsKey("NumDef")) {
							String def = abilityParams.get("NumDef");
							if (def.startsWith("+"))
								def = def.substring(1);
							toughness += Integer.parseInt(def);
						}
        		}
			}
		}
    	return toughness;
    }
    
    //Predict the Power bonus of the blocker if blocking the attacker (Flanking, Bushido and other triggered abilities)
    public static int predictPowerBonusOfAttacker(Card attacker, Card defender) {
    	int power = 0;
        	
        power += attacker.getKeywordMagnitude("Bushido");
        
        ArrayList<Trigger> registeredTriggers = AllZone.TriggerHandler.getRegisteredTriggers();
		for(Trigger trigger : registeredTriggers)
		{
			HashMap<String,String> trigParams = trigger.getMapParams();
			Card source = trigger.getHostCard();

			if(combatTriggerWillTrigger(attacker, defender, trigger)  && trigParams.containsKey("Execute")) {
				String ability = source.getSVar(trigParams.get("Execute"));
				AbilityFactory AF = new AbilityFactory();
        		HashMap<String,String> abilityParams = AF.getMapParams(ability, source);
        		if (abilityParams.containsKey("AB")) { 
					if (abilityParams.get("AB").equals("Pump"))
						if (!abilityParams.containsKey("ValidTgts") && !abilityParams.containsKey("Tgt"))
						if (AbilityFactory.getDefinedCards(source, trigParams.get("Defined"), null).contains(attacker))
						if (abilityParams.containsKey("NumAtt")){
							String att = abilityParams.get("NumAtt");
							if (att.startsWith("+"))
								att = att.substring(1);
							try {
								power += Integer.parseInt(att);
							}
							catch(NumberFormatException nfe) {
								//can't parse the number (X for example)
								power += 0;
							}
						}
        		}
			}
		}
    	return power;
    }
    
    //Predict the Toughness bonus of the blocker if blocking the attacker (Flanking, Bushido and other triggered abilities)
    public static int predictToughnessBonusOfAttacker(Card attacker, Card defender) {
    	int toughness = 0;
        	
        toughness += attacker.getKeywordMagnitude("Bushido");
        
        ArrayList<Trigger> registeredTriggers = AllZone.TriggerHandler.getRegisteredTriggers();
		for(Trigger trigger : registeredTriggers)
		{
			HashMap<String,String> trigParams = trigger.getMapParams();
			Card source = trigger.getHostCard();

			if(combatTriggerWillTrigger(attacker, defender, trigger)  && trigParams.containsKey("Execute")) {
				String ability = source.getSVar(trigParams.get("Execute"));
				AbilityFactory AF = new AbilityFactory();
        		HashMap<String,String> abilityParams = AF.getMapParams(ability, source);
        		if (abilityParams.containsKey("AB")) {
					if (abilityParams.get("AB").equals("Pump"))
						if (!abilityParams.containsKey("ValidTgts") && !abilityParams.containsKey("Tgt"))
						if (AbilityFactory.getDefinedCards(source, trigParams.get("Defined"), null).contains(attacker))
						if (abilityParams.containsKey("NumDef")) {
							String def = abilityParams.get("NumDef");
							if (def.startsWith("+"))
								def = def.substring(1);
							try{
								toughness += Integer.parseInt(def);
							}
							catch(NumberFormatException nfe) {
								//can't parse the number (X for example)
								toughness += 0;
							}
						}
        		}
			}
		}
    	return toughness;
    }
    
    //can the blocker destroy the attacker?
    public static boolean canDestroyAttacker(Card attacker, Card defender) {
        
        if(defender.hasStartOfKeyword("Whenever CARDNAME blocks a creature, destroy that creature at end of combat")) {
    		int KeywordPosition = defender.getKeywordPosition("Whenever CARDNAME blocks a creature, destroy that creature at end of combat");
    		String parse = defender.getKeyword().get(KeywordPosition).toString();
    		String k[] = parse.split(":");
    		final String restrictions[] = k[1].split(",");
    		if(attacker.isValidCard(restrictions, defender.getController(), defender) && !attacker.getKeyword().contains("Indestructible"))
    			return true;
        }
        
        if(attacker.getName().equals("Sylvan Basilisk") && !defender.getKeyword().contains("Indestructible")) return false;
        
        int flankingMagnitude = 0;
        if(attacker.getKeyword().contains("Flanking") && !defender.getKeyword().contains("Flanking")) {
            
        	flankingMagnitude = attacker.getAmountOfKeyword("Flanking");

            if(flankingMagnitude >= defender.getNetDefense()) return false;
            if(flankingMagnitude >= defender.getNetDefense() - defender.getDamage() && !defender.getKeyword().contains("Indestructible")) 
            	return false;
            
        }//flanking
        
        if(attacker.getKeyword().contains("Indestructible") && 
        		!(defender.getKeyword().contains("Wither") || defender.getKeyword().contains("Infect"))) return false;
        
        //unused
        //int attBushidoMagnitude = attacker.getKeywordMagnitude("Bushido");
        
        int defenderDamage = defender.getNetAttack() + predictPowerBonusOfBlocker(attacker, defender);
        int attackerDamage = attacker.getNetAttack() + predictPowerBonusOfAttacker(attacker, defender);
        if (AllZoneUtil.isCardInPlay("Doran, the Siege Tower")) {
        	defenderDamage = defender.getNetDefense() + predictToughnessBonusOfBlocker(attacker, defender);
        	attackerDamage = attacker.getNetDefense() + predictToughnessBonusOfAttacker(attacker, defender);
        }
        
        // consider Damage Prevention/Replacement
        defenderDamage = attacker.predictDamage(defenderDamage, defender, true);
        attackerDamage = defender.predictDamage(attackerDamage, attacker, true);
        
        int defenderLife = defender.getKillDamage() + predictToughnessBonusOfBlocker(attacker, defender);
        int attackerLife = attacker.getKillDamage() + predictToughnessBonusOfAttacker(attacker, defender);
        
        if(defender.getKeyword().contains("Double Strike") ) {
            if(defender.getKeyword().contains("Deathtouch") && defenderDamage > 0) return true;
            if(defender.hasStartOfKeyword("Whenever CARDNAME deals combat damage to a creature, destroy that creature") 
            		&& defenderDamage > 0 && !attacker.getKeyword().contains("Indestructible")) return true;
            if(defenderDamage >= attackerLife) return true;
            
            //Attacker may kill the blocker before he can deal normal (secondary) damage
            if((attacker.getKeyword().contains("Double Strike") || attacker.getKeyword().contains("First Strike")) 
            		&& !defender.getKeyword().contains("Indestructible")) {
                if(attackerDamage >= defenderLife) return false;
                if(attackerDamage > 0 && attacker.getKeyword().contains("Deathtouch")) return false;
            } 
            if(attackerLife <= 2 * defenderDamage) return true;
        }//defender double strike
        
        else //no double strike for defender
        {	
        	//Attacker may kill the blocker before he can deal any damage
            if(attacker.getKeyword().contains("Double Strike") || attacker.getKeyword().contains("First Strike")
            		&& !defender.getKeyword().contains("Indestructible") && !defender.getKeyword().contains("First Strike")) {
            	
            	if(attackerDamage >= defenderLife) return false;
            	if(attackerDamage > 0 && attacker.getKeyword().contains("Deathtouch") ) return false;
            }
            
            if(defender.getKeyword().contains("Deathtouch") && defenderDamage > 0) return true;
            if(defender.getKeyword().contains("Whenever CARDNAME deals combat damage to a creature, destroy that creature at end of combat.") 
            		&& defenderDamage > 0) return true;
                
            return defenderDamage >= attackerLife;
            
        }//defender no double strike
        return false; //should never arrive here
    } //canDestroyAttacker
    
    
    //For AI safety measures like Regeneration
    public static boolean blockerWouldBeDestroyed(Card blocker) {
    	Card attacker = AllZone.Combat.getAttackerBlockedBy(blocker);
    	
    	if(canDestroyBlocker(blocker, attacker) && 
    					!(attacker.getKeyword().contains("Wither") || attacker.getKeyword().contains("Infect")))
    			return true;
    	return false;
    }
    
    //can the attacker destroy this blocker?
    public static boolean canDestroyBlocker(Card defender, Card attacker) {
    	
        int flankingMagnitude = 0;
        if(attacker.getKeyword().contains("Flanking") && !defender.getKeyword().contains("Flanking")) {
            
        	flankingMagnitude = attacker.getAmountOfKeyword("Flanking");
        	
            if(flankingMagnitude >= defender.getNetDefense()) return true;
            if((flankingMagnitude >= defender.getKillDamage()) && !defender.getKeyword().contains("Indestructible")) return true;    
        }//flanking
        
        if(defender.getKeyword().contains("Indestructible") && 
        		!(attacker.getKeyword().contains("Wither") || attacker.getKeyword().contains("Infect"))) return false;
        
        if(attacker.getName().equals("Sylvan Basilisk") && !defender.getKeyword().contains("Indestructible")) return true;
        
        if(attacker.hasStartOfKeyword("Whenever CARDNAME becomes blocked by a creature, destroy that creature at end of combat")) {
        	int KeywordPosition = attacker.getKeywordPosition(
        			"Whenever CARDNAME becomes blocked by a creature, destroy that creature at end of combat");
        	String parse = attacker.getKeyword().get(KeywordPosition).toString();
    		String k[] = parse.split(":");
    		final String restrictions[] = k[1].split(",");
    		if(defender.isValidCard(restrictions,attacker.getController(),attacker) && !defender.getKeyword().contains("Indestructible"))
    			return true;
        }
        
        //unused
        //int attBushidoMagnitude = attacker.getKeywordMagnitude("Bushido");
        
        int defenderDamage = defender.getNetAttack() + predictPowerBonusOfBlocker(attacker, defender);
        int attackerDamage = attacker.getNetAttack() + predictPowerBonusOfAttacker(attacker, defender);
        if (AllZoneUtil.isCardInPlay("Doran, the Siege Tower")) {
        	defenderDamage = defender.getNetDefense() + predictToughnessBonusOfBlocker(attacker, defender);
        	attackerDamage = attacker.getNetDefense() + predictToughnessBonusOfAttacker(attacker, defender);
        }
        
        // consider Damage Prevention/Replacement
        defenderDamage = attacker.predictDamage(defenderDamage, defender, true);
        attackerDamage = defender.predictDamage(attackerDamage, attacker, true);
        
        int defenderLife = defender.getKillDamage() + predictToughnessBonusOfBlocker(attacker, defender);
        int attackerLife = attacker.getKillDamage() + predictToughnessBonusOfAttacker(attacker, defender);
        
        if(attacker.getKeyword().contains("Double Strike") ) {
            if(attacker.getKeyword().contains("Deathtouch") && attackerDamage > 0) return true;
            if(attacker.hasStartOfKeyword("Whenever CARDNAME deals combat damage to a creature, destroy that creature") 
            		&& attackerDamage > 0 && !defender.getKeyword().contains("Indestructible")) return true;
            if(attackerDamage >= defenderLife) return true;
            
            //Attacker may kill the blocker before he can deal normal (secondary) damage
            if((defender.getKeyword().contains("Double Strike") || defender.getKeyword().contains("First Strike")) 
            		&& !attacker.getKeyword().contains("Indestructible")) {
                if(defenderDamage >= attackerLife) return false;
                if(defenderDamage > 0 && defender.getKeyword().contains("Deathtouch")) return false;
            } 
            if(defenderLife <= 2 * attackerDamage) return true;
        }//attacker double strike
        
        else //no double strike for attacker
        {	
        	//Defender may kill the attacker before he can deal any damage
            if(defender.getKeyword().contains("Double Strike") || defender.getKeyword().contains("First Strike")
            		&& !attacker.getKeyword().contains("Indestructible") && !attacker.getKeyword().contains("First Strike")) {
            	
            	if(defenderDamage >= attackerLife) return false;
            	if(defenderDamage > 0 && defender.getKeyword().contains("Deathtouch") ) return false;
            }
            
            if(attacker.getKeyword().contains("Deathtouch") && attackerDamage > 0) return true;
            if(attacker.getKeyword().contains("Whenever CARDNAME deals combat damage to a creature, destroy that creature at end of combat.") 
            		&& attackerDamage > 0) return true;
                
            return attackerDamage >= defenderLife;
            
        }//attacker no double strike
        return false; //should never arrive here
    }//canDestroyBlocker
    
    public static void removeAllDamage() {
        Card[] c = AllZone.Human_Battlefield.getCards();
        for(int i = 0; i < c.length; i++)
            c[i].setDamage(0);
        
        c = AllZone.Computer_Battlefield.getCards();
        for(int i = 0; i < c.length; i++)
            c[i].setDamage(0);
    }
    
    public static void showCombat() {
        AllZone.Display.showCombat("");

        Card defend[] = null;
        StringBuilder display = new StringBuilder();
        
        // Loop through Defenders
        // Append Defending Player/Planeswalker
        ArrayList<Object> defenders = AllZone.Combat.getDefenders();
        CardList attackers[] = AllZone.Combat.sortAttackerByDefender();
        
        // Not a big fan of the triple nested loop here
        for(int def = 0; def < defenders.size(); def++){
        	if (attackers[def] == null || attackers[def].size() == 0)
        		continue;
        	
        	if (def > 0) 
        		display.append("\n");
        	
        	display.append("Defender - ");
        	display.append(defenders.get(def).toString());
        	display.append("\n");
        	
        	CardList list = attackers[def];
        	
        	for(Card c : list){
        		//loop through attackers
	            display.append("-> ");
	        	display.append(combatantToString(c)).append("\n");
	
	            defend = AllZone.Combat.getBlockers(c).toArray();
	            
	            //loop through blockers
	            for(int inner = 0; inner < defend.length; inner++) {
	                display.append(" [ ");
	                display.append(combatantToString(defend[inner])).append("\n"); 
	            }
	        }//loop through attackers
        }
        AllZone.Display.showCombat(display.toString().trim());
        
    }//showBlockers()
    
    private static String combatantToString(Card c){
    	StringBuilder sb = new StringBuilder();
    	
        String name = (c.isFaceDown()) ? "Morph" : c.getName();

        sb.append(name);
        sb.append(" (").append(c.getUniqueNumber()).append(") ");
        sb.append(c.getNetAttack()).append("/").append(c.getNetDefense());
    	
    	return sb.toString();
    }
    
    public static boolean isDoranInPlay() {
    	return AllZoneUtil.isCardInPlay("Doran, the Siege Tower");
    }
    
    public static void checkPropagandaEffects(Card c, final boolean bLast) {
    	String cost = CardFactoryUtil.getPropagandaCost(c);
        if(cost.equals("0")){
        	if(!c.getKeyword().contains("Vigilance")) 
        		c.tap();
        	
        	if (bLast)
            	PhaseUtil.handleAttackingTriggers();
        	return;
        }
        
        final Card crd = c;

        String phase = AllZone.Phase.getPhase();
        
        if(phase.equals(Constant.Phase.Combat_Declare_Attackers) || phase.equals(Constant.Phase.Combat_Declare_Attackers_InstantAbility)) {
            if(!cost.equals("0")) {
                final Ability ability = new Ability(c, cost) {
                    @Override
                    public void resolve() {
                        
                    }
                };
                
                final Command unpaidCommand = new Command() {
                    
                    private static final long serialVersionUID = -6483405139208343935L;
                    
                    public void execute() {
                        AllZone.Combat.removeFromCombat(crd);
                        
                        if (bLast)
                        	PhaseUtil.handleAttackingTriggers();
                    }
                };
                
                final Command paidCommand = new Command() {
                    private static final long serialVersionUID = -8303368287601871955L;
                    
                    public void execute() {
                    	// if Propaganda is paid, tap this card
                    	if(!crd.getKeyword().contains("Vigilance")) 
                    		crd.tap();   

                        if (bLast)
                        	PhaseUtil.handleAttackingTriggers();
                    }
                };
                
                if(c.getController().isHuman()) {
                    AllZone.InputControl.setInput(new Input_PayManaCost_Ability(c + " - Pay to Attack\r\n",
                            ability.getManaCost(), paidCommand, unpaidCommand));
                } 
                else{ //computer
                    if(ComputerUtil.canPayCost(ability)){
                    	ComputerUtil.playNoStack(ability);
                    	if(!crd.getKeyword().contains("Vigilance")) 
                    		crd.tap();
                    }
                    else {
                        // TODO: remove the below line after Propaganda occurs during Declare_Attackers
                        AllZone.Combat.removeFromCombat(crd);
                    }
                }
            }
        }
    }
    
    public static void checkDeclareAttackers(Card c) //this method checks triggered effects of attacking creatures, right before defending player declares blockers
    {
        	AllZone.GameAction.checkWheneverKeyword(c,"Attacks",null);
        	
        	//Run triggers
        	HashMap<String,Object> runParams = new HashMap<String,Object>();
        	runParams.put("Attacker", c);
        	AllZone.TriggerHandler.runTrigger("Attacks", runParams);
        	
        	//Annihilator:
        	if (!c.getCreatureAttackedThisCombat())
        	{
	        	ArrayList<String> kws = c.getKeyword();
	        	Pattern p = Pattern.compile("Annihilator [0-9]+");
	        	Matcher m;
	        	for (String key : kws)
	        	{
	        		m = p.matcher(key);
	        		if (m.find())
	        		{
	        			String k[] = key.split(" ");
	                    final int a = Integer.valueOf(k[1]);
	                    final Card crd = c;
	                    
	                    final Ability ability = new Ability(c, "0")
	                    {
	                    	public void resolve()
	                    	{
	                    		if (crd.getController().isHuman())
	                    		{
	                    			CardList list = new CardList(AllZone.Computer_Battlefield.getCards());
	                    			ComputerUtil.sacrificePermanents(a, list);
	                    		}
	                    		else
	                    		{
	                    			AllZone.InputControl.setInput(CardFactoryUtil.input_sacrificePermanents(a));
	                    		}
	                    			
	                    	}
	                    };
	                    StringBuilder sb = new StringBuilder();
	                    sb.append("Annihilator - Defending player sacrifices ").append(a).append(" permanents.");
	                    ability.setStackDescription(sb.toString());
	                    
	                    AllZone.Stack.add(ability);
	        		} //find
	        	} //for
        	}//creatureAttacked
        	//Annihilator
        	
            //Beastmaster Ascension
            if(!c.getCreatureAttackedThisCombat()) {
                PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, c.getController());
                CardList list = new CardList(play.getCards());
                list = list.getName("Beastmaster Ascension");
                
                for(Card var:list) {
                    var.addCounter(Counters.QUEST, 1);
                }
            } //BMA
            
            /*
            //Fervent Charge
            if(!c.getCreatureAttackedThisCombat()) {
                PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, c.getController());
                CardList list = new CardList(play.getCards());
                list = list.getName("Fervent Charge");
                
                for(Card var:list) {
                    final Card crd = c;
                    Ability ability2 = new Ability(var, "0") {
                        @Override
                        public void resolve() {
                            
                            final Command untilEOT = new Command() {
                                
                                private static final long serialVersionUID = 4495506596523335907L;
                                
                                public void execute() {
                                    if(AllZone.GameAction.isCardInPlay(crd)) {
                                        crd.addTempAttackBoost(-2);
                                        crd.addTempDefenseBoost(-2);
                                    }
                                }
                            };//Command
                            

                            if(AllZone.GameAction.isCardInPlay(crd)) {
                                crd.addTempAttackBoost(2);
                                crd.addTempDefenseBoost(2);
                                
                                AllZone.EndOfTurn.addUntil(untilEOT);
                            }
                        }//resolve
                        
                    };//ability
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append(var.getName()).append(" - ").append(c.getName()).append(" gets +2/+2 until EOT.");
                    ability2.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability2);
                    
                }
            }//Fervent Charge */
            
            //Mijae Djinn
            if(c.getName().equals("Mijae Djinn")) {
  				if( GameActionUtil.flipACoin(c.getController(), c)) {
  					//attack as normal
  				}
  				else{
  					AllZone.Combat.removeFromCombat(c);
  					c.tap();
  				}
            }//Mijae Djinn
            
            if (AllZone.Combat.getAttackers().length == 1)
            {
	            if (c.getKeyword().contains("Whenever this creature attacks alone, it gets +2/+0 until end of turn.") || 
	            	c.getKeyword().contains("Whenever CARDNAME attacks alone, it gets +2/+0 until end of turn."))
	            {
	            	final Card charger = c;
	            	Ability ability2 = new Ability(c, "0") {
	                    @Override
	                    public void resolve() {
	                        
	                        final Command untilEOT = new Command() {
								private static final long serialVersionUID = -6039349249335745813L;

								public void execute() {
	                                if(AllZone.GameAction.isCardInPlay(charger)) {
	                                    charger.addTempAttackBoost(-2);
	                                    charger.addTempDefenseBoost(0);
	                                }
	                            }
	                        };//Command
	                        

	                        if(AllZone.GameAction.isCardInPlay(charger)) {
	                            charger.addTempAttackBoost(2);
	                            charger.addTempDefenseBoost(0);
	                            
	                            AllZone.EndOfTurn.addUntil(untilEOT);
	                        }
	                    }//resolve
	                    
	                };//ability
	                
	                StringBuilder sb2 = new StringBuilder();
	                sb2.append(c.getName()).append(" - attacks alone and gets +2/+0 until EOT.");
	                ability2.setStackDescription(sb2.toString());
	                
	                AllZone.Stack.add(ability2);
	            }
	            
	            if (c.getKeyword().contains("Whenever CARDNAME attacks alone, it gets +1/+0 until end of turn."))
	            {
	            	final Card charger = c;
	            	Ability ability2 = new Ability(c, "0") {
	            		@Override
	            		public void resolve() {

	            			final Command untilEOT = new Command() {
	            				private static final long serialVersionUID = -6039349249335745813L;

	            				public void execute() {
	            					if(AllZone.GameAction.isCardInPlay(charger)) {
	            						charger.addTempAttackBoost(-1);
	            						charger.addTempDefenseBoost(0);
	            					}
	            				}
	            			};//Command


	            			if(AllZone.GameAction.isCardInPlay(charger)) {
	            				charger.addTempAttackBoost(1);
	            				charger.addTempDefenseBoost(0);

	            				AllZone.EndOfTurn.addUntil(untilEOT);
	            			}
	            		}//resolve

	            	};//ability

	            	StringBuilder sb2 = new StringBuilder();
	            	sb2.append(c.getName()).append(" - attacks alone and gets +1/+0 until EOT.");
	            	ability2.setStackDescription(sb2.toString());

	            	AllZone.Stack.add(ability2);
	            }
            }
            
            if(c.getName().equals("Zur the Enchanter") && !c.getCreatureAttackedThisCombat()) {
                //hack, to make sure this doesn't break grabbing an oblivion ring:
            	c.setCreatureAttackedThisCombat(true);
            	
            	PlayerZone library = AllZone.getZone(Constant.Zone.Library, c.getController());
                PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, c.getController());
                PlayerZone oppPlay = AllZone.getZone(Constant.Zone.Battlefield,
                        c.getController().getOpponent());
                
                CardList enchantments = new CardList(library.getCards());
                enchantments = enchantments.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        if(c.isEnchantment() && c.getCMC() <= 3) return true;
                        else return false;
                    }
                });
                
                if(enchantments.size() > 0) {
                    if(c.getController().isHuman()) {
                        Object o = GuiUtils.getChoiceOptional("Pick an enchantment to put onto the battlefield",
                                enchantments.toArray());
                        if(o != null) {
                            Card crd = (Card) o;
                            AllZone.GameAction.moveToPlay(crd);
                            
                            if(crd.isAura()) {
                                Object obj = null;
                                if(crd.getKeyword().contains("Enchant creature")) {
                                    CardList creats = new CardList(play.getCards());
                                    creats.addAll(oppPlay.getCards());
                                    creats = creats.getType("Creature");
                                    obj = GuiUtils.getChoiceOptional("Pick a creature to attach "
                                            + crd.getName() + " to", creats.toArray());
                                } else if(crd.getKeyword().contains("Enchant land")
                                        || crd.getKeyword().contains("Enchant land you control")) {
                                    CardList lands = new CardList(play.getCards());
                                    //lands.addAll(oppPlay.getCards());
                                    lands = lands.getType("Land");
                                    if(lands.size() > 0) obj = GuiUtils.getChoiceOptional(
                                            "Pick a land to attach " + crd.getName() + " to", lands.toArray());
                                }
                                if(obj != null) {
                                    Card target = (Card) obj;
                                    if(AllZone.GameAction.isCardInPlay(target)) {
                                        crd.enchantCard(target);
                                    }
                                }
                            }
                            c.getController().shuffle();
                            //we have to have cards like glorious anthem take effect immediately:
                            for(String effect:AllZone.StaticEffects.getStateBasedMap().keySet()) {
                                Command com = GameActionUtil.commands.get(effect);
                                com.execute();
                            }
                            GameActionUtil.executeCardStateEffects();
                            
                        }
                    } else if(c.getController().isComputer()) {
                        enchantments = enchantments.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return !c.isAura();
                            }
                        });
                        if(enchantments.size() > 0) {
                            Card card = CardFactoryUtil.AI_getBestEnchantment(enchantments, c, false);
                            AllZone.GameAction.moveToPlay(card);
                            c.getController().shuffle();
                            //we have to have cards like glorious anthem take effect immediately:
                            GameActionUtil.executeCardStateEffects();
                        }
                    }
                } //enchantments.size > 0
            }//Zur the enchanter
            
            else if(c.getName().equals("Yore-Tiller Nephilim") && !c.getCreatureAttackedThisCombat()) {
                PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, c.getController());
                
                CardList creatures = new CardList(grave.getCards());
                creatures = creatures.getType("Creature");
                
                if(creatures.size() > 0) {
                    if(c.getController().isHuman()) {
                        Object o = GuiUtils.getChoiceOptional("Pick a creature to put onto the battlefield",
                                creatures.toArray());
                        if(o != null) {
                            Card card = (Card) o;
                            AllZone.GameAction.moveToPlay(card);
                            
                            card.tap();
                            AllZone.Combat.addAttacker(card);
                        }
                    } else if(c.getController().isComputer()) {
                        Card card = CardFactoryUtil.AI_getBestCreature(creatures);
                        if (card != null){
	                        AllZone.GameAction.moveToPlay(card);
	                        
	                        card.tap();
	                        AllZone.Combat.addAttacker(card);
                        }
                    }
                    
                } //if (creatures.size() > 0) 
            }//Yore-Tiller Nephilim
            
            else if(c.getName().equals("Timbermaw Larva") && !c.getCreatureAttackedThisCombat()) {
                final Card charger = c;
                Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        final Player player = charger.getController();
                        PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, player);
                        CardList list = new CardList();
                        list.addAll(play.getCards());
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(Card card) {
                                return (card.getType().contains("Forest"));
                            }
                        });
                        final int x = list.size();
                        
                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = -1703473800920781454L;
                            
                            public void execute() {
                                if(AllZone.GameAction.isCardInPlay(charger)) {
                                    charger.addTempAttackBoost(-1 * x);
                                    charger.addTempDefenseBoost(-1 * x);
                                }
                            }
                        };//Command
                        

                        if(AllZone.GameAction.isCardInPlay(charger)) {
                            charger.addTempAttackBoost(x);
                            charger.addTempDefenseBoost(x);
                            
                            AllZone.EndOfTurn.addUntil(untilEOT);
                        }
                    }//resolve
                    
                };//ability
                
                StringBuilder sb = new StringBuilder();
                sb.append(c.getName());
                sb.append(" - +1/+1 until end of turn for each Forest ");
                sb.append(charger.getController());
                sb.append(" controls.");
                ability2.setStackDescription(sb.toString());
                
                AllZone.Stack.add(ability2);
                
            }//Timbermaw Larva
            

            else if(c.getName().equals("Knotvine Paladin") && !c.getCreatureAttackedThisCombat()) {
                final Card charger = c;
                Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        CardList list = AllZoneUtil.getCreaturesInPlay(charger.getController());
                        list = list.filter(AllZoneUtil.untapped);
                        final int k = list.size();
                        
                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = -1703473800920781454L;
                            
                            public void execute() {
                                if(AllZone.GameAction.isCardInPlay(charger)) {
                                    charger.addTempAttackBoost(-1 * k);
                                    charger.addTempDefenseBoost(-1 * k);
                                }
                            }
                        };//Command
                        
                        if(AllZone.GameAction.isCardInPlay(charger)) {
                            charger.addTempAttackBoost(k);
                            charger.addTempDefenseBoost(k);
                            
                            AllZone.EndOfTurn.addUntil(untilEOT);
                        }
                    }//resolve
                    
                };//ability
                
                StringBuilder sb2 = new StringBuilder();
                sb2.append(c.getName()).append(" - gets +1/+1 until end of turn for each untapped creature ");
                sb2.append(c.getController()).append(" controls.");
                ability2.setStackDescription(sb2.toString());
                
                AllZone.Stack.add(ability2);
                
            }//Knotvine Paladin
            

            else if(c.getName().equals("Goblin Piledriver") && !c.getCreatureAttackedThisCombat()) {
                final Card piledriver = c;
                Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        CardList list = new CardList();
                        list.addAll(AllZone.Combat.getAttackers());
                        list = list.getType("Goblin");
                        list.remove(piledriver);
                        
                        final int otherGoblins = list.size();
                        
                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = -4154121199693045635L;
                            
                            public void execute() {
                                if(AllZone.GameAction.isCardInPlay(piledriver)) {
                                    piledriver.addTempAttackBoost(-2 * otherGoblins);
                                }
                            }
                        };//Command
                        
                        if(AllZone.GameAction.isCardInPlay(piledriver)) {
                            piledriver.addTempAttackBoost(2 * otherGoblins);
                            AllZone.EndOfTurn.addUntil(untilEOT);
                        }
                    }//resolve
                };//ability
                
                StringBuilder sb2 = new StringBuilder();
                sb2.append(c.getName()).append(" - gets +2/+0 until end of turn for each other attacking Goblin.");
                ability2.setStackDescription(sb2.toString());
                
                AllZone.Stack.add(ability2);
                
            }//Goblin Piledriver
            
            /*
            else if((c.getName().equals("Charging Bandits") || c.getName().equals("Wei Ambush Force")
                    || c.getName().equals("Ravenous Skirge") || c.getName().equals("Vicious Kavu")
                    || c.getName().equals("Lurking Nightstalker") || c.getName().equals("Hollow Dogs"))
                    && !c.getCreatureAttackedThisCombat()) {
                final Card charger = c;
                Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        
                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = -1703473800920781454L;
                            
                            public void execute() {
                                if(AllZone.GameAction.isCardInPlay(charger)) {
                                    charger.addTempAttackBoost(-2);
                                    charger.addTempDefenseBoost(0);
                                }
                            }
                        };//Command
                        

                        if(AllZone.GameAction.isCardInPlay(charger)) {
                            charger.addTempAttackBoost(2);
                            charger.addTempDefenseBoost(0);
                            
                            AllZone.EndOfTurn.addUntil(untilEOT);
                        }
                    }//resolve
                    
                };//ability
                
                StringBuilder sb2 = new StringBuilder();
                sb2.append(c.getName()).append(" - gets +2/+0 until EOT.");
                ability2.setStackDescription(sb2.toString());
                
                AllZone.Stack.add(ability2);
                
            }//+2+0 Chargers */
            
            else if(c.getName().equals("Spectral Bears")) {
                Player opp = c.getController().getOpponent();
                PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, opp);
                CardList list = new CardList(play.getCards());
                list = list.filter(new CardListFilter() {
                    public boolean addCard(Card crd) {
                        return crd.isBlack() && !crd.isToken();
                    }
                });
                if(list.size() == 0) {
                    c.addExtrinsicKeyword("This card doesn't untap during your next untap step.");
                }
            }

            else if(c.getName().equals("Spectral Force")) {
                Player opp = c.getController().getOpponent();
                PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, opp);
                CardList list = new CardList(play.getCards());
                list = list.filter(new CardListFilter() {
                    public boolean addCard(Card crd) {
                        return crd.isBlack();
                    }
                });
                if(list.size() == 0) {
                    c.addExtrinsicKeyword("This card doesn't untap during your next untap step.");
                }
            }

            else if(c.getName().equals("Witch-Maw Nephilim") && !c.getCreatureAttackedThisCombat()
                    && c.getNetAttack() >= 10) {
                final Card charger = c;
                Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        
                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = -1703473800920781454L;
                            
                            public void execute() {
                                if(AllZone.GameAction.isCardInPlay(charger)) {
                                    charger.removeIntrinsicKeyword("Trample");
                                }
                            }
                        };//Command
                        
                        if(AllZone.GameAction.isCardInPlay(charger)) {
                            charger.addIntrinsicKeyword("Trample");
                            
                            AllZone.EndOfTurn.addUntil(untilEOT);
                        }
                    }//resolve
                };//ability
                
                StringBuilder sb2 = new StringBuilder();
                sb2.append(c.getName()).append(" - gains trample until end of turn if its power is 10 or greater.");
                ability2.setStackDescription(sb2.toString());
                
                AllZone.Stack.add(ability2);
                
            }//Witch-Maw Nephilim
            
            /*
            else if(c.getName().equals("Jedit Ojanen of Efrava") && !c.getCreatureAttackedThisCombat()) {
                final Card jedit = c;
                Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        CardFactoryUtil.makeToken("Cat Warrior", "G 2 2 Cat Warrior", jedit.getController(), "G", new String[] {
                                "Creature", "Cat", "Warrior"}, 2, 2, new String[] {"Forestwalk"});
                        //(anger) :
                        //GameActionUtil.executeCardStateEffects(); 
                        
                    }
                }; //Ability
                
                StringBuilder sb2 = new StringBuilder();
                sb2.append(c.getName()).append(" - put a 2/2 green Cat Warrior creature token with forestwalk onto the battlefield.");
                ability2.setStackDescription(sb2.toString());
                
                AllZone.Stack.add(ability2);
                
            }//Jedit */
            
            else if(c.getName().equals("Preeminent Captain") && !c.getCreatureAttackedThisCombat()) {
                System.out.println("Preeminent Captain Attacks");
                PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getController());
                
                CardList soldiers = new CardList(hand.getCards());
                soldiers = soldiers.getType("Soldier");
                
                if(soldiers.size() > 0) {
                    if(c.getController().equals(AllZone.HumanPlayer)) {
                        Object o = GuiUtils.getChoiceOptional("Pick a soldier to put onto the battlefield",
                                soldiers.toArray());
                        if(o != null) {
                            Card card = (Card) o;
                            AllZone.GameAction.moveToPlay(card);
                            
                            card.tap();
                            AllZone.Combat.addAttacker(card);

                            card.setCreatureAttackedThisCombat(true);
                        }
                    } else if(c.getController().equals(AllZone.ComputerPlayer)) {
                        Card card = CardFactoryUtil.AI_getBestCreature(soldiers);
                        if (card != null){
	                        AllZone.GameAction.moveToPlay(card);
	                        
	                        card.tap();
	                        AllZone.Combat.addAttacker(card);
	                        card.setCreatureAttackedThisCombat(true);
                        }
                    }
                    
                } //if (creatures.size() > 0) 
            }//Preeminent Captain
            
            else if(c.getName().equals("Lorthos, the Tidemaker") && !c.getCreatureAttackedThisCombat()) {
            	final Card[] Targets = new Card[8];
            	final int[] index = new int[1];
            	final Ability ability = new Ability(c, "8") {
            		@Override
            		public void resolve() {
            			for(int i = 0; i < 8; i++) {
            				if(Targets[i] != null) {
            					Targets[i].tap();
            					if(!Targets[i].hasKeyword("This card doesn't untap during your next untap step.")) {
            						Targets[i].addExtrinsicKeyword("This card doesn't untap during your next untap step.");
            					}
            				}
            			}
            		}
            	};
                final Command unpaidCommand = new Command() {                   
                    private static final long serialVersionUID = -6483124208343935L;                  
                    public void execute() {
                    }
                };
                
                final Command paidCommand = new Command() {
                    private static final long serialVersionUID = -83034517601871955L;
                    
                    public void execute() {
                        final CardList all = AllZoneUtil.getCardsInPlay();
                        for(int i = 0; i < 8; i++) {
                       	 AllZone.InputControl.setInput(CardFactoryUtil.Lorthos_input_targetPermanent(ability , all , i ,new Command() {
                       	
                             private static final long serialVersionUID = -328305150127775L;
                             
                             public void execute() {
                            	 all.remove(ability.getTargetCard());
                            	 Targets[index[0]] = ability.getTargetCard();
                                 index[0]++;                                
                            	 }
                         }));
                        } 
                    	AllZone.Stack.add(ability);
                    }
                };
                
                StringBuilder sb = new StringBuilder();
                sb.append(c.getName()).append(" - taps up to 8 target permanents.");
                ability.setStackDescription(sb.toString());
               
        		CardList Silence = AllZoneUtil.getPlayerCardsInPlay(c.getController().getOpponent()); 		
        		Silence = Silence.getName("Linvala, Keeper of Silence");
        		if(Silence.size() == 0) {
        			setLorthosCancelled(false);
                if(c.getController().equals(AllZone.HumanPlayer)) {
                    AllZone.InputControl.setInput(new Input_PayManaCost_Ability("Activate " + c.getName() + "'s ability: " + "\r\n",
                            ability.getManaCost(), paidCommand, unpaidCommand));
                } else //computer
                {
                    if(ComputerUtil.canPayCost(ability)) {
                    	ComputerUtil.playNoStack(ability);
                        PlayerZone Hplay = AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer);
                        final CardList all = new CardList();
                    	all.addAll(Hplay.getCards());
                    	CardList Creats = all.getType("Creature");
                    	CardListUtil.sortAttack(Creats);
                    	for(int i = 0; i < Creats.size(); i++) {
                    		if(index[0] < 8 && CardFactoryUtil.canTarget(ability, Creats.get(i))) {
                    		Targets[index[0]] = Creats.get(i);
                    		index[0]++;
                    	}
                    	}
                    	CardList Land = all.getType("Land");
                    	for(int i = 0; i < Land.size(); i++) {
                    		if(index[0] < 8 && CardFactoryUtil.canTarget(ability, Land.get(i))) {
                    		Targets[index[0]] = Land.get(i);
                    		index[0]++;
                    	}
                    	}  
                    	CardList Artifacts = all.getType("Artifact");
                    	for(int i = 0; i < Artifacts.size(); i++) {
                    		if(index[0] < 8 && CardFactoryUtil.canTarget(ability, Artifacts.get(i))) {
                    		Targets[index[0]] = Artifacts.get(i);
                    		index[0]++;
                    	}
                    	} 
                    	AllZone.Stack.add(ability);
                    }
                }
        		} // Silenced
            }// Lorthos, the Tidemaker
            
            else if(c.getName().equals("Sapling of Colfenor") && !c.getCreatureAttackedThisCombat()) {
                Player player = c.getController();
                
                PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);

                if(lib.size() > 0) {
                    CardList cl = new CardList();
                    cl.add(lib.get(0));
                    GuiUtils.getChoiceOptional("Top card", cl.toArray());
	                Card top = lib.get(0);
	                if(top.getType().contains("Creature")) {
	                    player.gainLife(top.getBaseDefense(), c);
	                    player.loseLife(top.getBaseAttack(), c);
	
	                    AllZone.GameAction.moveToHand(top);
	                }
                }
            }//Sapling of Colfenor
            
            else if(c.getName().equals("Goblin Guide") && !c.getCreatureAttackedThisCombat()) {
                final Player opp = c.getController().getOpponent();
                
                Ability ability = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        PlayerZone lib = AllZone.getZone(Constant.Zone.Library, opp);
                        if(lib.size() > 0) {
                            CardList cl = new CardList();
                            cl.add(lib.get(0));
                            GuiUtils.getChoiceOptional("Top card:", cl.toArray());
                            
                            Card c = cl.get(0);
                            if(c.isLand()) {
                                AllZone.GameAction.moveToHand(c);
                            }
                        }
                    }
                };
                
                StringBuilder sb = new StringBuilder();
                sb.append("Goblin Guide - defending player reveals the top card of his or her library. ");
                sb.append("If it's a land card, that player puts it into his or her hand.");
                ability.setStackDescription(sb.toString()); 
                
                AllZone.Stack.add(ability);
                
            }//Goblin Guide

            c.setCreatureAttackedThisCombat(true);
    }//checkDeclareAttackers
    
    public static void checkUnblockedAttackers(Card c) {
    	
    	AllZone.GameAction.checkWheneverKeyword(c,"isUnblocked",null);
    	
    	//Run triggers
    	HashMap<String,Object> runParams = new HashMap<String,Object>();
    	runParams.put("Card", c);
    	AllZone.TriggerHandler.runTrigger("AttackerUnblocked", runParams);
    	
        if(c.getName().equals("Guiltfeeder")) {
            final Player player = c.getController();
            final Player opponent = player.getOpponent();
            final Card F_card = c;
            Ability ability2 = new Ability(c, "0") {
                @Override
                public void resolve() {
                    
                    PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, opponent);
                    CardList cardsInGrave = new CardList(graveyard.getCards());
                    opponent.loseLife(cardsInGrave.size(),F_card);
                    
                }
            };// ability2
            
            StringBuilder sb2 = new StringBuilder();
            sb2.append(c.getName()).append(" - ").append(opponent).append(" loses life equal to cards in graveyard.");
            ability2.setStackDescription(sb2.toString());
            
            AllZone.Stack.add(ability2);
            
        }
        
    }
    
    public static void checkDeclareBlockers(CardList cl) {
    	for (Card c:cl) {
    		if (!c.getCreatureBlockedThisCombat()) {
    			for(Ability ab:CardFactoryUtil.getBushidoEffects(c)) {
    				AllZone.Stack.add(ab);
    			}
    		}
    		
    		c.setCreatureBlockedThisCombat(true);
    	}//for

    }//checkDeclareBlockers
    
    public static void checkBlockedAttackers(final Card a, Card b) {
        //System.out.println(a.getName() + " got blocked by " + b.getName());
    	
		//Run triggers
		HashMap<String, Object> runParams = new HashMap<String,Object>();
		runParams.put("Attacker",a);
		runParams.put("Blocker",b);
		AllZone.TriggerHandler.runTrigger("Blocks", runParams);
    	
        if(!a.getCreatureGotBlockedThisCombat()) {
        	final int blockers = AllZone.Combat.getBlockers(a).size();
        	
        	runParams.put("NumBlockers", blockers);
    		AllZone.TriggerHandler.runTrigger("AttackerBlocked", runParams);
        	
    		//AllZone.GameAction.checkWheneverKeyword(a,"BecomesBlocked",null); No longer needed
    	
            for(Ability ab:CardFactoryUtil.getBushidoEffects(a))
                AllZone.Stack.add(ab);
        	
        	//Rampage
        	ArrayList<String> keywords = a.getKeyword();
        	Pattern p = Pattern.compile("Rampage [0-9]+");
        	Matcher m;
        	for (String keyword : keywords) {
        		m = p.matcher(keyword);
        		if (m.find()){
        			String k[] = keyword.split(" ");
        			final int magnitude = Integer.valueOf(k[1]);
        			final int numBlockers = AllZone.Combat.getBlockers(a).size();
        			if(numBlockers > 1) {
        				executeRampageAbility(a, magnitude, numBlockers);
        			}
        		} //find
        	}//Rampage
        }
        
        if(a.getKeyword().contains("Flanking") && !b.getKeyword().contains("Flanking")) {
            int flankingMagnitude = 0;
            String kw = "";
            ArrayList<String> list = a.getKeyword();
            
            for(int i = 0; i < list.size(); i++) {
                kw = list.get(i);
                if(kw.equals("Flanking")) flankingMagnitude++;
            }
            final int mag = flankingMagnitude;
            final Card blocker = b;
            Ability ability2 = new Ability(b, "0") {
                @Override
                public void resolve() {
                    
                    final Command untilEOT = new Command() {
                        
                        private static final long serialVersionUID = 7662543891117427727L;
                        
                        public void execute() {
                            if(AllZone.GameAction.isCardInPlay(blocker)) {
                                blocker.addTempAttackBoost(mag);
                                blocker.addTempDefenseBoost(mag);
                            }
                        }
                    };//Command
                    

                    if(AllZone.GameAction.isCardInPlay(blocker)) {
                        blocker.addTempAttackBoost(-mag);
                        blocker.addTempDefenseBoost(-mag);
                        
                        AllZone.EndOfTurn.addUntil(untilEOT);
                        System.out.println("Flanking!");
                    }
                }//resolve
                
            };//ability
            
            StringBuilder sb2 = new StringBuilder();
            sb2.append(b.getName()).append(" - gets -").append(mag).append("/-").append(mag).append(" until EOT.");
            ability2.setStackDescription(sb2.toString());
            
            AllZone.Stack.add(ability2);
            Log.debug("Adding Flanking!");
            //AllZone.GameAction.checkStateEffects();
            
        }//flanking
        
        
        if(b.hasStartOfKeyword("Whenever CARDNAME blocks a creature, destroy that creature at end of combat")) {
    		int KeywordPosition = b.getKeywordPosition("Whenever CARDNAME blocks a creature, destroy that creature at end of combat");
    		String parse = b.getKeyword().get(KeywordPosition).toString();
    		String k[] = parse.split(":");
    		final String restrictions[] = k[1].split(",");
    		if(a.isValidCard(restrictions,b.getController(),b)) {
            	final Card attacker = a;
            	final Ability ability = new Ability(b, "0") {
                	@Override
               	public void resolve() {
                		//this isCardInPlay is probably not necessary since
                		//if is checked in the atEOC before being put on stack
                    	if(AllZone.GameAction.isCardInPlay(attacker)) {
                        	AllZone.GameAction.destroy(attacker);
                    	}
                	}
            	};
            
            	StringBuilder sb = new StringBuilder();
            	sb.append(b).append(" - destroy blocked creature.");
            	ability.setStackDescription(sb.toString());
            
            	final Command atEOC = new Command() {
                	private static final long serialVersionUID = 5854485314766349980L;
                
                	public void execute() {
                		if(AllZone.GameAction.isCardInPlay(attacker)) {
                			AllZone.Stack.add(ability);
                		}
                	}
            	};
            
            	AllZone.EndOfCombat.addAt(atEOC);
    		}
        }// Whenever CARDNAME blocks a creature, destroy that creature at end of combat 

        if(a.hasStartOfKeyword("Whenever CARDNAME becomes blocked by a creature, destroy that creature at end of combat")) {
        	int KeywordPosition = a.getKeywordPosition("Whenever CARDNAME becomes blocked by a creature, destroy that creature at end of combat");
        	String parse = a.getKeyword().get(KeywordPosition).toString();
    		String k[] = parse.split(":");
    		final String restrictions[] = k[1].split(",");
    		if(b.isValidCard(restrictions,a.getController(),a)) {
                final Card blocker = b;
                final Ability ability = new Ability(a, "0") {
                    @Override
                    public void resolve() {
                        AllZone.GameAction.destroy(blocker);
                    }
                };
                
                StringBuilder sb = new StringBuilder();
                sb.append(a).append(" - destroy blocking creature.");
                ability.setStackDescription(sb.toString());
                
                final Command atEOC = new Command() {
                    
                    private static final long serialVersionUID = -9077416427198135373L;
                    
                    public void execute() {
                        if(AllZone.GameAction.isCardInPlay(blocker)) AllZone.Stack.add(ability);
                    }
                };
                
                AllZone.EndOfCombat.addAt(atEOC);
    		}
        }//Whenever CARDNAME becomes blocked by a creature, destroy that creature at end of combat
        
        
        if (a.getName().equals("Robber Fly") && !a.getCreatureGotBlockedThisCombat()) {
        	Player opp = b.getController();
        	PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, opp);
        	CardList list = new CardList(hand.getCards());
        	int handSize = list.size();

        	// opponent discards their hand,
        	opp.discardRandom(handSize, a.getSpellAbility()[0]);
        	opp.drawCards(handSize);
        } else if(a.getName().equals("Sylvan Basilisk")) {
            AllZone.GameAction.destroy(b);
            System.out.println("destroyed blocker " + b.getName());
        } else if(a.getName().equals("Elven Warhounds")) {
        	AllZone.GameAction.moveToLibrary(b);
        }
        
        a.setCreatureGotBlockedThisCombat(true);
    }
    
    public static void executeExaltedAbility(Card c, int magnitude) {
        final Card crd = c;
        Ability ability;
        
        for(int i = 0; i < magnitude; i++) {
            ability = new Ability(c, "0") {
                @Override
                public void resolve() {
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = 1497565871061029469L;
                        
                        public void execute() {
                            if(AllZone.GameAction.isCardInPlay(crd)) {
                                crd.addTempAttackBoost(-1);
                                crd.addTempDefenseBoost(-1);
                            }
                        }
                    };//Command
                    
                    if(AllZone.GameAction.isCardInPlay(crd)) {
                        crd.addTempAttackBoost(1);
                        crd.addTempDefenseBoost(1);
                        
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }//resolve
                
            };//ability
            
            StringBuilder sb = new StringBuilder();
            sb.append(c).append(" - (Exalted) gets +1/+1 until EOT.");
            ability.setStackDescription(sb.toString());
            
            AllZone.Stack.add(ability);
        }
        
        Player phasingPlayer = c.getController();
        // Finest Hour untaps the creature on the first combat phase
		if ((AllZoneUtil.getPlayerCardsInPlay(phasingPlayer, "Finest Hour").size() > 0) &&
				AllZone.Phase.isFirstCombat()) {
			// Untap the attacking creature
			Ability fhUntap = new Ability(c, "0") {
				public void resolve() {
				    crd.untap();
				}
			};
			
			StringBuilder sbUntap = new StringBuilder();
			sbUntap.append(c).append(" - (Exalted) untap.");
			fhUntap.setStackDescription(sbUntap.toString());
			
			AllZone.Stack.add(fhUntap);
		
			// If any Finest Hours, queue up a new combat phase
			for (int ix = 0; ix < AllZoneUtil.getPlayerCardsInPlay(phasingPlayer, "Finest Hour").size(); ix++) {
				Ability fhAddCombat = new Ability(c, "0") {
					public void resolve() {
						AllZone.Phase.addExtraCombat();				
					}
				};
				
				StringBuilder sbACom = new StringBuilder();
				sbACom.append(c).append(" - (Exalted) ").append(phasingPlayer).append(" gets Extra Combat Phase.");
				fhAddCombat.setStackDescription(sbACom.toString());
				
				AllZone.Stack.add(fhAddCombat);
			}
		}
		
        if(AllZoneUtil.isCardInPlay("Rafiq of the Many", phasingPlayer)) {
            Ability ability2 = new Ability(c, "0") {
                @Override
                public void resolve() {
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = -8943526706248389725L;
                        
                        public void execute() {
                            if(AllZone.GameAction.isCardInPlay(crd)) crd.removeExtrinsicKeyword("Double Strike");
                        }
                    };//Command
                    
                    if(AllZone.GameAction.isCardInPlay(crd)) {
                        crd.addExtrinsicKeyword("Double Strike");
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }//resolve
                
            };//ability2
            
            StringBuilder sb2 = new StringBuilder();
            sb2.append(c).append(" - (Exalted) gets Double Strike until EOT.");
            ability2.setStackDescription(sb2.toString());
            
            AllZone.Stack.add(ability2);
        }
        
        if(AllZoneUtil.getPlayerCardsInPlay(phasingPlayer, "Battlegrace Angel").size() > 0) {
            Ability ability3 = new Ability(c, "0") {
                @Override
                public void resolve() {
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = -8154692281049657338L;
                        
                        public void execute() {
                            if(AllZone.GameAction.isCardInPlay(crd)) crd.removeExtrinsicKeyword("Lifelink");
                        }
                    };//Command
                    
                    if(AllZone.GameAction.isCardInPlay(crd)) {
                        crd.addExtrinsicKeyword("Lifelink");
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }//resolve
                
            };//ability2
            
            StringBuilder sb3 = new StringBuilder();
            sb3.append(c).append(" - (Exalted) gets Lifelink until EOT.");
            ability3.setStackDescription(sb3.toString());
            
            AllZone.Stack.add(ability3);
        }
        
        if(AllZoneUtil.getPlayerCardsInPlay(phasingPlayer, "Sovereigns of Lost Alara").size() > 0) {
            for(int i = 0; i < AllZoneUtil.getPlayerCardsInPlay(phasingPlayer, "Sovereigns of Lost Alara").size(); i++) { 
            	final Card attacker = c;
            Ability ability4 = new Ability(c, "0") {
                @Override
                public void resolve() {
                	PlayerZone library = AllZone.getZone(Constant.Zone.Library, attacker.getController());
                    
                    CardList enchantments = new CardList(library.getCards());
                    //final String turn = attacker.getController();
                    enchantments = enchantments.filter(new CardListFilter() {                      
                        public boolean addCard(Card c) {
                        	if(attacker.hasKeyword("Protection from enchantments") || (attacker.hasKeyword("Protection from everything"))) return false;
                        	return(c.isEnchantment() && c.getKeyword().contains("Enchant creature") 
                        			&& !CardFactoryUtil.hasProtectionFrom(c,attacker));
                        }
                    });
	                    Player player = attacker.getController();
	                    Card Enchantment = null;
	                    if(player.isHuman()){
                            Card[] Target = new Card[enchantments.size()];
                            for(int i = 0; i < enchantments.size(); i++) {
                				Card crd = enchantments.get(i);
                				Target[i] = crd;
                            }
	                        Object check = GuiUtils.getChoiceOptional("Select enchantment to enchant exalted creature", Target);
	                        if(check != null) {
	                           Enchantment = ((Card) check);	
	                        }
	                    } else {
	                    	//enchantments = enchantments.getKeywordsContain("enPump"); Doesn't seem to work
	                    	//enchantments = enchantments.getKeywordsDontContain("enPumpCurse");
	                        Enchantment = CardFactoryUtil.AI_getBestEnchantment(enchantments,attacker, false);
	                    }
	                    if(Enchantment != null && AllZone.GameAction.isCardInPlay(attacker)){
	                    	AllZone.GameAction.moveToPlay(Enchantment);
	                    	Enchantment.enchantCard(attacker);
	                    }
                        attacker.getController().shuffle();	                    
                }//resolve
            };// ability4
            
            StringBuilder sb4 = new StringBuilder();
            sb4.append(c).append(" - (Exalted) searches library for an Aura card that could enchant that creature, ");
            sb4.append("put it onto the battlefield attached to that creature, then shuffles library.");
            ability4.setStackDescription(sb4.toString());
            
            AllZone.Stack.add(ability4);
            } // For
        }
    }
    
    /**
     * executes Rampage abilities for a given card
     * 
     * @param c the card to add rampage bonus to
     * @param magnitude the magnitude of rampage (ie Rampage 2 means magnitude should be 2)
     * @param numBlockers - the number of creatures blocking this rampaging creature
     */
    private static void executeRampageAbility(Card c, int magnitude, int numBlockers) {
    	final Card crd = c;
    	final int pump = magnitude;
    	Ability ability;

    	//numBlockers -1 since it is for every creature beyond the first
    	for(int i = 0; i < numBlockers - 1; i++) {
    		ability = new Ability(c, "0") {
    			@Override
    			public void resolve() {
    				final Command untilEOT = new Command() {
    					private static final long serialVersionUID = -3215615538474963181L;

    					public void execute() {
    						if(AllZone.GameAction.isCardInPlay(crd)) {
    							crd.addTempAttackBoost(-pump);
    							crd.addTempDefenseBoost(-pump);
    						}
    					}
    				};//Command

    				if(AllZone.GameAction.isCardInPlay(crd)) {
    					crd.addTempAttackBoost(pump);
    					crd.addTempDefenseBoost(pump);

    					AllZone.EndOfTurn.addUntil(untilEOT);
    				}
    			}//resolve

    		};//ability
    		
    		StringBuilder sb = new StringBuilder();
    		sb.append(c).append(" - (Rampage) gets +").append(pump).append("/+").append(pump).append(" until EOT.");
    		ability.setStackDescription(sb.toString());
    		
    		AllZone.Stack.add(ability);
    	}
    }


	public static void setLorthosCancelled(boolean lorthos_Cancelled) {
		Lorthos_Cancelled = lorthos_Cancelled;
	}


	public static boolean isLorthosCancelled() {
		return Lorthos_Cancelled;
	}
    
}//Class CombatUtil
