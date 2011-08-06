
package forge;


import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import com.esotericsoftware.minlog.Log;


public class CombatUtil {
	static boolean Lorthos_Cancelled;
    public static boolean canBlock(Card attacker, Card blocker) {
        
        if(attacker == null || blocker == null) return false;
        
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
        
//      if(attacker.getNetAttack() <= 2 && blocker.getName().equals("Sunweb")) return false;
//      if(attacker.getNetAttack() >= 2 && blocker.getName().equals("Ironclaw Orcs")) return false;

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
                    blocker.getKeyword().contains("CARDNAME can't be blocked by creatures with power greater than CARDNAME's power.")) return false;
            if (blocker.getNetAttack() >= attacker.getNetDefense() && 
                    blocker.getKeyword().contains("CARDNAME can't be blocked by creatures with power equal to or greater than CARDNAME's toughness.")) return false;
        
        }// hasKeyword CARDNAME can't be blocked by creatures with power ...

        PlayerZone blkPZ = AllZone.getZone(Constant.Zone.Play, blocker.getController());
        CardList blkCL = new CardList(blkPZ.getCards());
        CardList temp = new CardList();
        
        if(attacker.getKeyword().contains("Plainswalk")) {
            temp = blkCL.getType("Plains");
            if(!AllZoneUtil.isCardInPlay("Lord Magnus") 
            		&& !AllZoneUtil.isCardInPlay("Great Wall")
            		&& !AllZoneUtil.isCardInPlay("Staff of the Ages")
            		&& !temp.isEmpty()) return false;
        }
        
        if(attacker.getKeyword().contains("Islandwalk")) {
            temp = blkCL.getType("Island");
            if(!AllZoneUtil.isCardInPlay("Undertow") 
            		&& !AllZoneUtil.isCardInPlay("Gosta Dirk")
            		&& !AllZoneUtil.isCardInPlay("Staff of the Ages")
            		&& !temp.isEmpty()) return false;
        }
        
        if(attacker.getKeyword().contains("Swampwalk")) {
            temp = blkCL.getType("Swamp");
            if(!AllZoneUtil.isCardInPlay("Ur-drago") 
            		&& !AllZoneUtil.isCardInPlay("Quagmire")
            		&& !AllZoneUtil.isCardInPlay("Staff of the Ages")
            		&& !temp.isEmpty()) return false;
        }
        
        if(attacker.getKeyword().contains("Mountainwalk")) {
            temp = blkCL.getType("Mountain");
            if(!AllZoneUtil.isCardInPlay("Crevasse") 
            		&& !AllZoneUtil.isCardInPlay("Staff of the Ages")
            		&& !temp.isEmpty()) return false;
        }
        
        if(attacker.getKeyword().contains("Forestwalk")) {
            temp = blkCL.getType("Forest");
            if(!AllZoneUtil.isCardInPlay("Lord Magnus")
            		&& !AllZoneUtil.isCardInPlay("Deadfall")
            		&& !AllZoneUtil.isCardInPlay("Staff of the Ages")
            		&& !temp.isEmpty()) return false;
        }
        
        if(attacker.getKeyword().contains("Legendary landwalk")) {
            temp = blkCL.filter(new CardListFilter() {
                public boolean addCard(Card c) {
                    return c.isLand() && c.getType().contains("Legendary");
                }
            });
            if(!temp.isEmpty() && !AllZoneUtil.isCardInPlay("Staff of the Ages")) return false;
        }
        
        if(attacker.getKeyword().contains("Nonbasic landwalk")) {
            temp = blkCL.filter(new CardListFilter() {
                public boolean addCard(Card c) {
                    return c.isLand() && !c.isBasicLand();
                }
            });
            if(!temp.isEmpty() && !AllZoneUtil.isCardInPlay("Staff of the Ages")) return false;
        }
        

        if(blocker.getKeyword().contains("CARDNAME can block only creatures with flying.")
                && !attacker.getKeyword().contains("Flying")) return false;
        
        if (attacker.getKeyword().contains("CARDNAME can't be blocked by creatures with flying.")
        		&& blocker.getKeyword().contains("Flying")) return false;
        
        if(attacker.getKeyword().contains("Unblockable")) return false;
        
        if(blocker.getKeyword().contains("CARDNAME can't block.")
                || blocker.getKeyword().contains("CARDNAME can't attack or block.")) return false;
        
        if(attacker.getKeyword().contains("Flying")) {
            if(!blocker.getKeyword().contains("Flying")
                    && !blocker.getKeyword().contains("CARDNAME can block creatures with flying.")
                    && !blocker.getKeyword().contains("Reach")) return false;
        }
        
        if(attacker.getKeyword().contains("CARDNAME can't be blocked except by creatures with flying.")
                && !blocker.getKeyword().contains("Flying")) return false;
        

        if(attacker.getKeyword().contains("Horsemanship")) {
            if(!blocker.getKeyword().contains("Horsemanship")) return false;
        }
        
        if(attacker.getName().equals("Taoist Warrior") || attacker.getName().equals("Zuo Ci, the Mocking Sage")) {
            if(blocker.getKeyword().contains("Horsemanship")) return false;
        }
        
        if(attacker.getKeyword().contains("Fear")) {
            if(!blocker.getType().contains("Artifact")
                    && !blocker.isBlack() /*&&
                        !CardUtil.getColors(blocker).contains(Constant.Color.Colorless) */) //should not include colorless, right?
            return false;
        }
        
        if (attacker.getKeyword().contains("CARDNAME can't be blocked by white creatures."))
        {
        	if (!blocker.isWhite())
        		return false;
        }
        
        if(attacker.getKeyword().contains("Intimidate")) {
            if(!blocker.getType().contains("Artifact") && !CardFactoryUtil.sharesColorWith(attacker, blocker)) return false;
        }
        
        if(attacker.getName().equals("Barrenton Cragtreads")) {
            if(CardUtil.getColors(blocker).contains(Constant.Color.Red)) return false;
        }
        
        if(attacker.getName().equals("Wanderbrine Rootcutters")) {
            if(CardUtil.getColors(blocker).contains(Constant.Color.Green)) return false;
        }
        
        if(/*attacker.getName().equals("Amrou Seekers")*/ attacker.getKeyword().contains("CARDNAME can't be blocked except by artifact creatures and/or white creatures.")) {
            if(!blocker.getType().contains("Artifact")
                    && !blocker.isWhite()) return false;
        }
        
        if(attacker.getName().equals("Skirk Shaman")) {
            if(!blocker.getType().contains("Artifact")
                    && !blocker.isRed()) return false;
        }
        
        if(attacker.getName().equals("Manta Ray")) {
            if(!blocker.isBlue()) return false;
        }
        
        if(attacker.getKeyword().contains("CARDNAME can't be blocked except by black creatures.")) {
        	if(!blocker.isBlack())return false; 
        }
        
        if (attacker.getKeyword().contains("CARDNAME can't be blocked by Walls.") && blocker.isType("Wall")) return false;
        
        if (attacker.getKeyword().contains("CARDNAME can't be blocked except by Walls.") && !blocker.isType("Wall")) return false;
        
        if (attacker.getKeyword().contains("CARDNAME can't be blocked except by Walls and/or creatures with flying.") && 
        		!(blocker.isType("Wall") || blocker.getKeyword().contains("Flying"))) return false;
        
        if (blocker.getCounters(Counters.BRIBERY) > 0 && isCardInPlay("Gwafa Hazid, Profiteer"))
        	return false;
        
        if (isCardInPlay("Kulrath Knight"))
        {
        	CardList all = new CardList();
        	all.addAll(AllZone.getZone(Constant.Zone.Play, Constant.Player.Human).getCards());
        	all.addAll(AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer). getCards());
        	
        	all = all.getName("Kulrath Knight");
        	for (int i=0; i<all.size(); i++)
        	{
        		Card cKK = all.get(i);
        		String oppKK = AllZone.GameAction.getOpponent(cKK.getController());
        		
        		if (blocker.getController().equals(oppKK) && blocker.hasCounters())
        			return false;
        	}
        }
        
        return true;
    }//canBlock()
    
    public static boolean canAttack(Card c) {
        
        if(isPeaceKeeperInPlay()) return false;
        
        boolean moatPrevented = false;
        if(isMoatInPlay() || isMagusMoatInPlay()) {
            if(!c.getKeyword().contains("Flying")) moatPrevented = true;
        }
        
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
        
        if (hasKeyword) {    // The keyword "CARDNAME can't attack if defending player controls an untapped creature with power" ... is present
            String tmpString = c.getKeyword().get(keywordPosition).toString();
            final String asSeparateWords[]  = tmpString.trim().split(" ");
            
            if (asSeparateWords.length >= 15) {
                if (asSeparateWords[12].matches("[0-9][0-9]?")) {
                    powerLimit[0] = Integer.parseInt((asSeparateWords[12]).trim());
                    
                    CardList list = null;
                    if (c.getController().equals(Constant.Player.Human)) {
                        list = new CardList(AllZone.Computer_Play.getCards());
                    } else {
                        list = new CardList(AllZone.Human_Play.getCards());
                    }
                    
                    list = list.getType("Creature");
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
        
        PlayerZone play = AllZone.getZone(Constant.Zone.Play, AllZone.GameAction.getOpponent(c.getController()));
        CardList list = new CardList(play.getCards());
        CardList temp;
        
        if(c.getKeyword().contains("CARDNAME can't attack unless defending player controls an Island.")) {
            temp = list.getType("Island");
            if(temp.isEmpty()) return false;
        }
    	
        if(c.getName().equals("Harbor Serpent")) {
        	CardList allislands = new CardList();
        	allislands.addAll(AllZone.getZone(Constant.Zone.Play, Constant.Player.Human).getCards());
        	allislands.addAll(AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer).getCards());
        	allislands = allislands.getType("Island");
            if(allislands.size() < 5) return false;
        }
        
        if(c.isTapped() || c.hasSickness() || c.getKeyword().contains("Defender") || moatPrevented
                || oppControlsBlazingArchon(c) || c.getKeyword().contains("CARDNAME can't attack.")
                || c.getKeyword().contains("CARDNAME can't attack or block.")
                || (oppControlsReverence(c) && c.getNetAttack() < 3)) return false;
        
        if (c.getCounters(Counters.BRIBERY) > 0 && isCardInPlay("Gwafa Hazid, Profiteer"))
        	return false;
        
        if (isCardInPlay("Ensnaring Bridge")) {
        	int limit = Integer.MAX_VALUE;
        	CardList Human = new CardList();
        	Human.addAll(AllZone.Human_Play.getCards());
        	if (Human.getName("Ensnaring Bridge").size() > 0) {
        		CardList Hand = new CardList();
        		Hand.addAll(AllZone.getZone(Constant.Zone.Hand, "Human").getCards());
        		limit = Hand.size();
        	}
        	CardList Compi = new CardList();
        	Compi.addAll(AllZone.Computer_Play.getCards());
        	if (Compi.getName("Ensnaring Bridge").size() > 0) {
        		CardList Hand = new CardList();
        		Hand.addAll(AllZone.getZone(Constant.Zone.Hand, "Computer").getCards());
        		if (Hand.size() < limit) limit = Hand.size();
        	}
        	if (c.getNetAttack() > limit) return false;
        }
        
        if (isCardInPlay("Kulrath Knight"))
        {
        	CardList all = new CardList();
        	all.addAll(AllZone.getZone(Constant.Zone.Play, Constant.Player.Human).getCards());
        	all.addAll(AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer).getCards());
        	
        	all = all.getName("Kulrath Knight");
        	for (int i=0; i<all.size(); i++)
        	{
        		Card cKK = all.get(i);
        		String oppKK = AllZone.GameAction.getOpponent(cKK.getController());
        		
        		if (c.getController().equals(oppKK) && c.hasCounters())
        			return false;
        	}
        }

        
        //if Card has Haste, Card.hasSickness() will return false
        return true;
    }//canAttack()
    
    public static int getTotalFirstStrikeAttackPower(Card attacker, String player)
    {
    	final Card att = attacker;
    	int i = 0;
    	
    	CardList list = AllZoneUtil.getCreaturesInPlay(player);
    	list = list.filter(new CardListFilter()
    	{
    		public boolean addCard(Card c)
    		{
    			return c.isUntapped() && canBlock(att, c) && (c.hasFirstStrike() || c.hasDoubleStrike() ) ;
    		}
    	});
    	
    	int flankingMagnitude = 0;
    	if(attacker.getKeyword().contains("Flanking")) {
            
            String kw = "";
            ArrayList<String> lst = attacker.getKeyword();
            
            for(int j = 0; j < lst.size(); j++) {
                kw = lst.get(j);
                if(kw.equals("Flanking")) flankingMagnitude++;
            }
    	}
    	
    	
    	for (Card c:list)
    	{
    		int flankingOffset = 0;
    		if (!c.getKeyword().contains("Flanking"))
    			flankingOffset = flankingMagnitude;
    			
    		if (!isDoranInPlay())
    			i+= c.getNetAttack() - flankingOffset;
    		else
    			i+= c.getNetDefense() - flankingOffset;
    	}
    	
    	return i;
    }
    
    
    public static boolean canDestroyAttacker(Card attacker, Card defender) {
        
        if(defender.hasStartOfKeyword("Whenever CARDNAME blocks a creature, destroy that creature at end of combat")) {
    		int KeywordPosition = defender.getKeywordPosition("Whenever CARDNAME blocks a creature, destroy that creature at end of combat");
    		String parse = defender.getKeyword().get(KeywordPosition).toString();
    		String k[] = parse.split(":");
    		final String restrictions[] = k[1].split(",");
    		if(attacker.isValidCard(restrictions)) return true;
        }
        
        if(attacker.getName().equals("Sylvan Basilisk") && !defender.getKeyword().contains("Indestructible")) return false;
        
        int flankingMagnitude = 0;
        if(attacker.getKeyword().contains("Flanking") && !defender.getKeyword().contains("Flanking")) {
            
            String kw = "";
            ArrayList<String> list = attacker.getKeyword();
            
            for(int i = 0; i < list.size(); i++) {
                kw = list.get(i);
                if(kw.equals("Flanking")) flankingMagnitude++;
            }
            if(flankingMagnitude >= defender.getNetDefense() - defender.getDamage()) return false;
            
        }//flanking
        
        if(attacker.hasStartOfKeyword("Prevent all combat damage that would be dealt to")) return false;
        if(defender.getKeyword().contains("Prevent all combat damage that would be dealt to and dealt by CARDNAME.") ||
        		defender.getKeyword().contains("Prevent all combat damage that would be dealt by CARDNAME")) return false;
        
        if(attacker.getKeyword().contains("Indestructible") && 
        		!(defender.getKeyword().contains("Wither") || defender.getKeyword().contains("Infect"))) return false;
        
        if(!CardFactoryUtil.canDamage(defender, attacker)) return false;
        
        /* Checked by the function above
        //this usually doesn't happen, unless the attacker got pro {color} after being blocked, or the defender became {color}
        if(attacker.getKeyword().contains("Protection from white")
                && defender.isWhite()) return false;
        if(attacker.getKeyword().contains("Protection from blue")
                && defender.isBlue()) return false;
        if(attacker.getKeyword().contains("Protection from black")
                && defender.isBlack()) return false;
        if(attacker.getKeyword().contains("Protection from red")
                && defender.isRed()) return false;
        if(attacker.getKeyword().contains("Protection from green")
                && defender.isGreen()) return false;
        
        if(attacker.getKeyword().contains("Protection from artifacts") && defender.isArtifact()) return false;
        
        if(attacker.getKeyword().contains("Protection from creatures")) return false;
        
        if(attacker.getKeyword().contains("Protection from Dragons")
                && (defender.getType().contains("Dragon") || defender.getKeyword().contains("Changeling"))) return false;
        if(attacker.getKeyword().contains("Protection from Demons")
                && (defender.getType().contains("Demon") || defender.getKeyword().contains("Changeling"))) return false;
        if(attacker.getKeyword().contains("Protection from Goblins")
                && (defender.getType().contains("Goblin") || defender.getKeyword().contains("Changeling"))) return false;
        if(attacker.getKeyword().contains("Protection from Clerics")
                && (defender.getType().contains("Cleric") || defender.getKeyword().contains("Changeling"))) return false;
        */
        
        int defBushidoMagnitude = CardFactoryUtil.getTotalBushidoMagnitude(defender);
        int attBushidoMagnitude = CardFactoryUtil.getTotalBushidoMagnitude(attacker);
        
        int defenderDamage = defender.getNetAttack() - flankingMagnitude + defBushidoMagnitude;
        int attackerDamage = attacker.getNetAttack() + attBushidoMagnitude;
        
        if(isDoranInPlay()) {
            defenderDamage = defender.getNetDefense() - flankingMagnitude + defBushidoMagnitude;
            attackerDamage = attacker.getNetDefense() + attBushidoMagnitude;
        }
        int defenderLife = defender.getNetDefense() - flankingMagnitude + defBushidoMagnitude;
        int attackerLife = attacker.getNetDefense() + attBushidoMagnitude;
        
        if(defender.getKeyword().contains("Double Strike") ) {
            if(defender.getKeyword().contains("Deathtouch") && defenderDamage > 0) return true;
            
            if(attacker.getKeyword().contains("Double Strike")) {
                if(defenderDamage >= attackerLife - attacker.getDamage()) return true;
                if(((defenderLife - defender.getDamage()) > attackerDamage)
                        && (attackerLife - attacker.getDamage() <= 2 * defenderDamage)
                        && !attacker.getKeyword().contains("Deathtouch")) return true;
            } else if(attacker.getKeyword().contains("First Strike")) //hmm, same as previous if ?
            {
                if(defenderDamage >= attackerLife - attacker.getDamage()) return true;
                if(((defenderLife - defender.getDamage()) > attackerDamage)
                        && ((attackerLife - attacker.getDamage()) <= 2 * defenderDamage)
                        && !attacker.getKeyword().contains("Deathtouch")) return true;
            } else //no double nor first strike for attacker
            {
                if(defenderDamage >= attackerLife - attacker.getDamage()) return true;
                if((attackerLife - attacker.getDamage()) <= 2 * defenderDamage) return true;
            }
            
        }//defender double strike
        
        else if(defender.getKeyword().contains("First Strike")) {
            if(defender.getKeyword().contains("Deathtouch") && defenderDamage > 0) return true;
            
            if(defenderDamage >= attackerLife - attacker.getDamage()) return true;
        }//defender first strike
        
        else //no double nor first strike for defender
        {
            if(attacker.getKeyword().contains("Double Strike")) {
                if(defenderDamage >= attackerLife - attacker.getDamage()
                        && attackerDamage * 2 < defenderLife - defender.getDamage()) return true;
                
                if(defender.getKeyword().contains("Deathtouch") && !attacker.getKeyword().contains("Deathtouch")
                        && attackerDamage * 2 < defenderLife - defender.getDamage()) return true;
            } else if(attacker.getKeyword().contains("First Strike")) //same as previous if?
            {
                if(defenderDamage >= attackerLife - attacker.getDamage()
                        && !attacker.getKeyword().contains("Deathtouch")
                        && attackerDamage < defenderLife - defender.getDamage()) return true;
                
                if(defender.getKeyword().contains("Deathtouch") && !attacker.getKeyword().contains("Deathtouch")
                        && attackerDamage < defenderLife - defender.getDamage()) return true;
            } else //no double nor first strike for attacker
            {
                if(defender.getKeyword().contains("Deathtouch") && defenderDamage > 0) return true;
                
                return defenderDamage >= attackerLife - attacker.getDamage();
            }
            
        }//defender no double/first strike
        return false; //should never arrive here
    } //canDestroyAttacker
    
    public static boolean canDestroyBlocker(Card defender, Card attacker) {
    	
        int flankingMagnitude = 0;
        if(attacker.getKeyword().contains("Flanking") && !defender.getKeyword().contains("Flanking")) {
            
            String kw = "";
            ArrayList<String> list = attacker.getKeyword();
            
            for(int i = 0; i < list.size(); i++) {
                kw = list.get(i);
                if(kw.equals("Flanking")) flankingMagnitude++;
            }
            if(flankingMagnitude >= defender.getNetDefense()) return true;
            if((flankingMagnitude >= defender.getNetDefense() - defender.getDamage()) && !defender.getKeyword().contains("Indestructible")) return true;    
        }//flanking
        
        if(defender.getKeyword().contains("Indestructible") && 
        		!(attacker.getKeyword().contains("Wither") || attacker.getKeyword().contains("Infect"))) return false;
        if(attacker.getName().equals("Sylvan Basilisk")) return true;
        
        if(attacker.hasStartOfKeyword("Whenever CARDNAME becomes blocked by a creature, destroy that creature at end of combat")) {
        	int KeywordPosition = attacker.getKeywordPosition("Whenever CARDNAME becomes blocked by a creature, destroy that creature at end of combat");
        	String parse = attacker.getKeyword().get(KeywordPosition).toString();
    		String k[] = parse.split(":");
    		final String restrictions[] = k[1].split(",");
    		if(defender.isValidCard(restrictions)) return true;
        }
        
        if(defender.hasStartOfKeyword("Prevent all combat damage that would be dealt to")) return false;
        if(attacker.getKeyword().contains("Prevent all combat damage that would be dealt to and dealt by CARDNAME.") ||
        		attacker.getKeyword().contains("Prevent all combat damage that would be dealt by CARDNAME")) return false;
        
        if(!CardFactoryUtil.canDamage(attacker,defender)) return false;
        
        /* Checked by the function above
        if(defender.getKeyword().contains("Prevent all damage that would be dealt to CARDNAME by artifact creatures.") 
        		&& attacker.isCreature() && attacker.isArtifact()) return false;
        if(defender.getKeyword().contains("Protection from white")
                && attacker.isWhite()) return false;
        if(defender.getKeyword().contains("Protection from blue")
                && attacker.isBlue()) return false;
        if(defender.getKeyword().contains("Protection from black")
                && attacker.isBlack()) return false;
        if(defender.getKeyword().contains("Protection from red")
                && attacker.isRed()) return false;
        if(defender.getKeyword().contains("Protection from green")
                && attacker.isGreen()) return false;
        
        if(defender.getKeyword().contains("Protection from artifacts") && attacker.isArtifact()) return false;
        
        if(defender.getKeyword().contains("Protection from creatures")) return false;
        
        if(defender.getKeyword().contains("Protection from Dragons")
                && (attacker.getType().contains("Dragon") || attacker.getKeyword().contains("Changeling"))) return false;
        if(defender.getKeyword().contains("Protection from Demons")
                && (attacker.getType().contains("Demon") || attacker.getKeyword().contains("Changeling"))) return false;
        if(defender.getKeyword().contains("Protection from Goblins")
                && (attacker.getType().contains("Goblin") || attacker.getKeyword().contains("Changeling"))) return false;
        */
        
        int defBushidoMagnitude = CardFactoryUtil.getTotalBushidoMagnitude(defender);
        int attBushidoMagnitude = CardFactoryUtil.getTotalBushidoMagnitude(attacker);
        
        int defenderDamage = defender.getNetAttack() - flankingMagnitude + defBushidoMagnitude;
        int attackerDamage = attacker.getNetAttack() + attBushidoMagnitude;
        
        if(isDoranInPlay()) {
            defenderDamage = defender.getNetDefense() - flankingMagnitude + defBushidoMagnitude;
            attackerDamage = attacker.getNetDefense() + attBushidoMagnitude;
        }
        int defenderLife = defender.getNetDefense() - flankingMagnitude + defBushidoMagnitude;
        int attackerLife = attacker.getNetDefense() + attBushidoMagnitude;
        
        if(attacker.getKeyword().contains("Double Strike")) {
            if(attacker.getKeyword().contains("Deathtouch") && attackerDamage > 0) return true;
            
            if(defender.getKeyword().contains("Double Strike")) {
                if(defenderDamage >= attackerLife - attacker.getDamage()) return true;
                if(((attackerLife - attacker.getDamage()) > defenderDamage)
                        && (defenderLife - defender.getDamage() <= 2 * attackerDamage)
                        && !defender.getKeyword().contains("Deathtouch")) return true;
            } else if(defender.getKeyword().contains("First Strike")) //hmm, same as previous if ?
            {
                if(attackerDamage >= defenderLife - defender.getDamage()) return true;
                if(((attackerLife - attacker.getDamage()) > defenderDamage)
                        && ((defenderLife - defender.getDamage()) <= 2 * attackerDamage)
                        && !defender.getKeyword().contains("Deathtouch")) return true;
            } else //no double nor first strike for defender
            {
                if(attackerDamage >= defenderLife - defender.getDamage()) return true;
                if((defenderLife - defender.getDamage()) <= 2 * attackerDamage) return true;
            }
            
        }//attacker double strike
        
        else if(attacker.getKeyword().contains("First Strike")) {
            if(attacker.getKeyword().contains("Deathtouch") && attackerDamage > 0) return true;
            if(attackerDamage >= defenderLife - defender.getDamage()) return true;
        }//attacker first strike
        
        else //no double nor first strike for attacker
        {
            if(defender.getKeyword().contains("Double Strike")) {
                if(attackerDamage >= defenderLife - defender.getDamage()
                        && defenderDamage < attackerLife - attacker.getDamage()) return true;
            } else if(defender.getKeyword().contains("First Strike")) //same as previous if?
            {
                if(attackerDamage >= defenderLife - defender.getDamage()
                        && defenderDamage < attackerLife - attacker.getDamage()) return true;
            } else //no double nor first strike for defender
            {
                if(attacker.getKeyword().contains("Deathtouch") && attackerDamage > 0) return true;
                
                return attackerDamage >= defenderLife - defender.getDamage();
            }
            
        }//attacker no double/first strike
        return false; //should never arrive here
    }//canDestroyBlocker
    
    public static void removeAllDamage() {
        Card[] c = AllZone.Human_Play.getCards();
        for(int i = 0; i < c.length; i++)
            c[i].setDamage(0);
        
        c = AllZone.Computer_Play.getCards();
        for(int i = 0; i < c.length; i++)
            c[i].setDamage(0);
    }
    
    public static void showCombat() {
        //clear
        AllZone.Display.showCombat("");
        
        Card attack[] = AllZone.Combat.getAttackers();
        Card defend[] = null;
        StringBuilder display = new StringBuilder();
        String attackerName = "";
        String blockerName = "";
        
        //loop through attackers
        for(int i = 0; i < attack.length; i++) {
            //GameActionUtil.executeExaltedEffects2(attack[i], AllZone.Combat);
            //checkDeclareAttackers(attack[i]);
            attackerName = attack[i].getName();
            if(attack[i].isFaceDown()) attackerName = "Morph";
            display.append(attackerName);
            display.append(" (");
            display.append(attack[i].getUniqueNumber());
            display.append(") ");
            display.append(attack[i].getNetAttack());
            display.append("/");
            display.append(attack[i].getNetDefense());
            display.append(" is attacking \n");
            
            defend = AllZone.Combat.getBlockers(attack[i]).toArray();
            
            //loop through blockers
            for(int inner = 0; inner < defend.length; inner++) {
                //checkDeclareBlockers(defend[inner]);
                blockerName = defend[inner].getName();
                if(defend[inner].isFaceDown()) blockerName = "Morph";
                
                display.append("     ");
                display.append(blockerName);
                display.append(" (");
                display.append(defend[inner].getUniqueNumber());
                display.append(") ");
                display.append(defend[inner].getNetAttack());
                display.append("/");
                display.append(defend[inner].getNetDefense());
                display.append(" is blocking \n");
                
            }
        }//while - loop through attackers
        String s = display.toString() + getPlaneswalkerBlockers();
        AllZone.Display.showCombat(s.trim());
        
    }//showBlockers()
    
    private static String getPlaneswalkerBlockers() {
        Card attack[] = AllZone.pwCombat.getAttackers();
        Card defend[] = null;
        StringBuilder display = new StringBuilder();
        
        if(attack.length != 0) display.append("Planeswalker Combat\r\n");
        
        String attackerName = "";
        String blockerName = "";
        //loop through attackers
        for(int i = 0; i < attack.length; i++) {
            //GameActionUtil.executeExaltedEffects2(attack[i], AllZone.pwCombat);
            
            //checkDeclareAttackers(attack[i]);
            attackerName = attack[i].getName();
            if(attack[i].isFaceDown()) attackerName = "Morph";
            
            display.append(attackerName);
            display.append(" (");
            display.append(attack[i].getUniqueNumber());
            display.append(") ");
            display.append(attack[i].getNetAttack());
            display.append("/");
            display.append(attack[i].getNetDefense());
            display.append(" is attacking \n");
            
            defend = AllZone.pwCombat.getBlockers(attack[i]).toArray();
            
            //loop through blockers
            for(int inner = 0; inner < defend.length; inner++) {
                //checkDeclareBlockers(defend[inner]);
                blockerName = defend[inner].getName();
                if(defend[inner].isFaceDown()) blockerName = "Morph";
                
                display.append("     ");
                display.append(blockerName);
                display.append(" (");
                display.append(defend[inner].getUniqueNumber());
                display.append(") ");
                display.append(defend[inner].getNetAttack());
                display.append("/");
                display.append(defend[inner].getNetDefense());
                display.append(" is blocking \n");
            }
        }//while - loop through attackers
        
        return display.toString();
    }//getPlaneswalkerBlockers()
    
    public static void executeCombatDamageEffects(Card c) {
        
    	boolean canDamage = true;
    	CardList blockers = AllZone.Combat.getBlockers(c);
    	if (blockers.size() == 1)
    	{
    		if (!CardFactoryUtil.canDamage(c, blockers.get(0)))
    			canDamage = false;
    		
    		//TODO: multiple blockers
    	}
    	
    	if (canDamage)
    	{
	    	// if(c.getKeyword().contains("Lifelink")) GameActionUtil.executeLifeLinkEffects(c);
	        
	        // CardList cl = CardFactoryUtil.getAurasEnchanting(c, "Guilty Conscience");
	        // for(Card crd:cl)
	        //    GameActionUtil.executeGuiltyConscienceEffects(c, crd);
	        
	        /*
	         * Whenever equipped creature deals combat damage, put two
	         * charge counters on Umezawa's Jitte.
	         */
	        if(c.isEquipped() && c.getNetAttack() > 0) {
	        	ArrayList<Card> equips = c.getEquippedBy();
	        	for(Card equip:equips) {
	        		if(equip.getName().equals("Umezawa's Jitte")) {
	        			equip.addCounter(Counters.CHARGE, 2);
	        		}
	        		if(c.getDealtCombatDmgToOppThisTurn() && equip.getName().equals("Sword of Fire and Ice")) {
	        			GameActionUtil.executeSwordOfFireAndIceEffects(equip);
	        		}
	        		if(c.getDealtCombatDmgToOppThisTurn() && equip.getName().equals("Sword of Light and Shadow")) {
	        			GameActionUtil.executeSwordOfLightAndShadowEffects(equip);
	        		}
	        		if(c.getDealtCombatDmgToOppThisTurn() && equip.getName().equals("Sword of Body and Mind")) {
	        			GameActionUtil.executeSwordOfBodyAndMindEffects(equip);
	        		}
	        	}
	        }//isEquipped && getNetAttack > 0
    	}//canDamage
    }
    
    /*no longer needed
    private static boolean canBlockProtection(Card attacker, Card blocker) {
        ArrayList<String> list = attacker.getKeyword();
        
        String kw = "";
        for(int i = 0; i < list.size(); i++) {
            kw = list.get(i);
            
            
            if(kw.equals("Protection from creatures") || kw.equals("Protection from everything")) return false;
            if(kw.equals("Protection from artifacts") && blocker.isArtifact()) return false;
            
            if(kw.equals("Protection from Dragons")
                    && (blocker.getType().contains("Dragon") || blocker.getKeyword().contains("Changeling"))) return false;
            if(kw.equals("Protection from Demons")
                    && (blocker.getType().contains("Demon") || blocker.getKeyword().contains("Changeling"))) return false;
            if(kw.equals("Protection from Goblins")
                    && (blocker.getType().contains("Goblin") || blocker.getKeyword().contains("Changeling"))) return false;
            
            //pro colors:
            if(kw.equals("Protection from white") && blocker.isWhite()) return false;
            if(kw.equals("Protection from blue") && blocker.isBlue()) return false;
            if(kw.equals("Protection from black") && blocker.isBlack()) return false;
            if(kw.equals("Protection from red") && blocker.isRed()) return false;
            if(kw.equals("Protection from green") && blocker.isGreen()) return false;
        }
        return true;
    }
    */
    
    public static boolean isDoranInPlay() {
        CardList all = new CardList();
        all.addAll(AllZone.Human_Play.getCards());
        all.addAll(AllZone.Computer_Play.getCards());
        
        all = all.getName("Doran, the Siege Tower");
        if(all.size() > 0) return true;
        else return false;
    }
    
    public static boolean oppControlsBlazingArchon(Card c) {
        String opp = AllZone.GameAction.getOpponent(c.getController());
        
        CardList list = new CardList();
        list.addAll(AllZone.getZone(Constant.Zone.Play, opp).getCards());
        
        list = list.getName("Blazing Archon");
        if(list.size() > 0) return true;
        else return false;
    }
    
    public static boolean oppControlsReverence(Card c) {
        String opp = AllZone.GameAction.getOpponent(c.getController());
        
        CardList list = new CardList();
        list.addAll(AllZone.getZone(Constant.Zone.Play, opp).getCards());
        
        list = list.getName("Reverence");
        if(list.size() > 0) return true;
        else return false;
    }
    
    public static boolean isCardInPlay(String card)
    {
    	CardList all = new CardList();
    	all.addAll(AllZone.Human_Play.getCards());
    	all.addAll(AllZone.Computer_Play.getCards());
    	
    	return all.containsName(card);
    }
    
    public static boolean isPeaceKeeperInPlay() {
        CardList all = new CardList();
        all.addAll(AllZone.Human_Play.getCards());
        all.addAll(AllZone.Computer_Play.getCards());
        
        all = all.getName("Peacekeeper");
        return all.size() > 0;
    }
    
    public static boolean isMoatInPlay() {
        CardList all = new CardList();
        all.addAll(AllZone.Human_Play.getCards());
        all.addAll(AllZone.Computer_Play.getCards());
        
        all = all.getName("Moat");
        if(all.size() > 0) return true;
        else return false;
    }
    
    public static boolean isMagusMoatInPlay() {
        CardList all = new CardList();
        all.addAll(AllZone.Human_Play.getCards());
        all.addAll(AllZone.Computer_Play.getCards());
        
        all = all.getName("Magus of the Moat");
        if(all.size() > 0) return true;
        else return false;
    }
    
    public static boolean checkPropagandaEffects(Card c) {
        if(CardFactoryUtil.getPropagandaCost(c).equals("0")) return true;
        
        final Card crd = c;
        final boolean[] canAttack = new boolean[1];
        canAttack[0] = false;
        //if (AllZone.Phase.getPhase().equals(Constant.Phase.Combat_Declare_Attackers))
        if( /*AllZone.Phase.getPhase().equals("Declare Blockers") || */
        AllZone.Phase.getPhase().equals(Constant.Phase.Combat_Declare_Attackers_InstantAbility)) {
            if (!c.getCheckedPropagandaThisTurn())
            {
	            String cost = CardFactoryUtil.getPropagandaCost(c);
	            if(!cost.equals("0")) {
	                final Ability ability = new Ability(c, cost) {
	                    @Override
	                    public void resolve() {
	                        canAttack[0] = true;
	                    }
	                };
	                
	                final Command unpaidCommand = new Command() {
	                    
	                    private static final long serialVersionUID = -6483405139208343935L;
	                    
	                    public void execute() {
	                        canAttack[0] = false;
	                        AllZone.Combat.removeFromCombat(crd);
	                        crd.untap();
	                    }
	                };
	                
	                final Command paidCommand = new Command() {
	                    private static final long serialVersionUID = -8303368287601871955L;
	                    
	                    public void execute() {
	                        canAttack[0] = true;
	                    }
	                };
	                
	                if(c.getController().equals(Constant.Player.Human)) {
	                    AllZone.InputControl.setInput(new Input_PayManaCost_Ability("Propaganda " + c + "\r\n",
	                            ability.getManaCost(), paidCommand, unpaidCommand));
	                } else //computer
	                {
	                    if(ComputerUtil.canPayCost(ability)) ComputerUtil.playNoStack(ability);
	                    else {
	                        canAttack[0] = false;
	                        AllZone.Combat.removeFromCombat(crd);
	                        crd.untap();
	                    }
	                }
	            }
	        }
            c.setCheckedPropagandaThisTurn(true);
        }
        return canAttack[0];
    }
    
    public static void checkDeclareAttackers(Card c) //this method checks triggered effects of attacking creatures, right before defending player declares blockers
    {
        //human does not have an "attackers_instantAbility" phase during his turn (yet), so triggers will happen at the beginning of declare blockers
        if( /*AllZone.Phase.getPhase().equals("Declare Blockers") ||*/
        AllZone.Phase.getPhase().equals(Constant.Phase.Combat_Declare_Attackers_InstantAbility)) {
        	AllZone.GameAction.CheckWheneverKeyword(c,"Attacks",null);
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
	                    		if (crd.getController().equals(Constant.Player.Human))
	                    		{
	                    			CardList list = new CardList(AllZone.Computer_Play.getCards());
	                    			CardListUtil.sortCMC(list);
	                    			list.reverse();
	                    			int max = list.size();
	                    			if (max>a)
	                    				max = a;
	                    			
	                    			for (int i=0;i<max;i++)
	                    				AllZone.GameAction.sacrifice(list.get(i));
	                    		}
	                    		else
	                    		{
	                    			AllZone.InputControl.setInput(CardFactoryUtil.input_sacrificePermanents(a));
	                    		}
	                    			
	                    	}
	                    };
	                    ability.setStackDescription("Annihilator - Defending player sacrifices " + a + " permanents.");
	                    AllZone.Stack.add(ability);
	        		} //find
	        	} //for
        	}//creatureAttacked
        	//Annihilator
        	
            //Beastmaster Ascension
            if(!c.getCreatureAttackedThisCombat()) {
                PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
                CardList list = new CardList(play.getCards());
                list = list.getName("Beastmaster Ascension");
                
                for(Card var:list) {
                    var.addCounter(Counters.QUEST, 1);
                }
            } //BMA
            
            //Fervent Charge
            if(!c.getCreatureAttackedThisCombat()) {
                PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
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
                    
                    ability2.setStackDescription(var.getName() + " - " + c.getName() + " gets +2/+2 until EOT.");
                    AllZone.Stack.add(ability2);
                    
                }
            }//Fervent Charge
            
            //Raging Ravine, other future creats?
            if(c.getKeyword().contains("Whenever this creature attacks, put a +1/+1 counter on it.")) {
                ArrayList<String> kw = c.getKeyword();
                int count = 0;
                for(String s:kw) {
                    if(s.equals("Whenever this creature attacks, put a +1/+1 counter on it.")) count++;
                }
                final Card crd = c;
                Ability ability = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        crd.addCounter(Counters.P1P1, 1);
                    }
                };
                ability.setStackDescription(c + " - Whenever this creature attacks, put a +1/+1 counter on it.");
                for(int i = 0; i < count; i++)
                    AllZone.Stack.add(ability);
            }//Raging Ravine
            
            if ((AllZone.Combat.getAttackers().length + AllZone.pwCombat.getAttackers().length) == 1)
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
	                
	                ability2.setStackDescription(c.getName() + " - attacks alone and gets +2/+0 until EOT.");
	                AllZone.Stack.add(ability2);
	            }
            }
           
            
            if(c.getName().equals("Zhang He, Wei General") && !c.getCreatureAttackedThisCombat()) {
                final PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
                
                //final Card crd = c;
                Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        CardList cl = new CardList(play.getCards());
                        cl = cl.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return c.isCreature() && !c.getName().equals("Zhang He, Wei General");
                            }
                        });
                        
                        final CardList creatures = cl;
                        
                        final Command untilEOT = new Command() {
                            
                            private static final long serialVersionUID = 8799962485775380711L;
                            
                            
                            public void execute() {
                                for(Card creat:creatures) {
                                    if(AllZone.GameAction.isCardInPlay(creat)) {
                                        creat.addTempAttackBoost(-1);
                                    }
                                }
                            }
                        };//Command
                        
                        for(Card creat:creatures) {
                            if(AllZone.GameAction.isCardInPlay(creat)) {
                                creat.addTempAttackBoost(1);
                            }
                        }
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                };
                
                ability2.setStackDescription(c.getName()
                        + " - all other creatures you control get +1/+0 until end of turn.");
                AllZone.Stack.add(ability2);
            }//Zhang He
            
            if(c.getName().equals("Soltari Champion") && !c.getCreatureAttackedThisCombat()) {
                final PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
                
                final Card crd = c;
                Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        CardList cl = new CardList(play.getCards());
                        cl = cl.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return c.isCreature() && !c.equals(crd);
                            }
                        });
                        
                        final CardList creatures = cl;
                        
                        final Command untilEOT = new Command() {
                            
                            private static final long serialVersionUID = -8434529949884582940L;
                            
                            public void execute() {
                                for(Card creat:creatures) {
                                    if(AllZone.GameAction.isCardInPlay(creat)) {
                                        creat.addTempAttackBoost(-1);
                                        creat.addTempDefenseBoost(-1);
                                    }
                                }
                            }
                        };//Command
                        
                        for(Card creat:creatures) {
                            if(AllZone.GameAction.isCardInPlay(creat)) {
                                creat.addTempAttackBoost(1);
                                creat.addTempDefenseBoost(1);
                            }
                        }
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                };
                
                ability2.setStackDescription(c.getName()
                        + " - all other creatures you control get +1/+1 until end of turn.");
                AllZone.Stack.add(ability2);
            }//Soltari Champion
            
            if(c.getName().equals("Goblin General") && !c.getCreatureAttackedThisCombat()) {
                final PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
                
                //final Card crd = c;
                Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        CardList cl = new CardList(play.getCards());
                        cl = cl.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return c.isCreature()
                                        && (c.getType().contains("Goblin") || c.getKeyword().contains("Changeling"));
                            }
                        });
                        
                        final CardList creatures = cl;
                        
                        final Command untilEOT = new Command() {
                            
                            private static final long serialVersionUID = -8434529949884582940L;
                            
                            public void execute() {
                                for(Card creat:creatures) {
                                    if(AllZone.GameAction.isCardInPlay(creat)) {
                                        creat.addTempAttackBoost(-1);
                                        creat.addTempDefenseBoost(-1);
                                    }
                                }
                            }
                        };//Command
                        
                        for(Card creat:creatures) {
                            if(AllZone.GameAction.isCardInPlay(creat)) {
                                creat.addTempAttackBoost(1);
                                creat.addTempDefenseBoost(1);
                            }
                        }
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                };
                
                ability2.setStackDescription(c.getName()
                        + " - Goblin creatures you control get +1/+1 until end of turn.");
                AllZone.Stack.add(ability2);
            }//Goblin General
            
            if(c.getName().equals("Pianna, Nomad Captain") && !c.getCreatureAttackedThisCombat()) {
                
                //final Card crd = c;
                Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        CardList cl = new CardList(AllZone.Combat.getAttackers());
                        final CardList creatures = cl;
                        
                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = -7050310805245783042L;
                            
                            public void execute() {
                                for(Card creat:creatures) {
                                    if(AllZone.GameAction.isCardInPlay(creat)) {
                                        creat.addTempAttackBoost(-1);
                                        creat.addTempDefenseBoost(-1);
                                    }
                                }
                            }
                        };//Command
                        
                        for(Card creat:creatures) {
                            if(AllZone.GameAction.isCardInPlay(creat)) {
                                creat.addTempAttackBoost(1);
                                creat.addTempDefenseBoost(1);
                            }
                        }
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                };
                
                ability2.setStackDescription(c.getName() + " - attacking creatures get +1/+1 until end of turn.");
                AllZone.Stack.add(ability2);
            }//Goblin General
            
            if(c.getName().equals("Zur the Enchanter") && !c.getCreatureAttackedThisCombat()) {
                //hack, to make sure this doesn't break grabbing an oblivion ring:
            	c.setCreatureAttackedThisCombat(true);
            	
            	PlayerZone library = AllZone.getZone(Constant.Zone.Library, c.getController());
                PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
                PlayerZone oppPlay = AllZone.getZone(Constant.Zone.Play,
                        AllZone.GameAction.getOpponent(c.getController()));
                
                CardList enchantments = new CardList(library.getCards());
                enchantments = enchantments.filter(new CardListFilter() {
                    
                    public boolean addCard(Card c) {
                        if(c.isEnchantment() && CardUtil.getConvertedManaCost(c.getManaCost()) <= 3) return true;
                        else return false;
                    }
                });
                
                if(enchantments.size() > 0) {
                    if(c.getController().equals("Human")) {
                        Object o = AllZone.Display.getChoiceOptional("Pick an enchantment to put into play",
                                enchantments.toArray());
                        if(o != null) {
                            Card crd = (Card) o;
                            library.remove(crd);
                            play.add(crd);
                            
                            if(crd.isAura()) {
                                Object obj = null;
                                if(crd.getKeyword().contains("Enchant creature")) {
                                    CardList creats = new CardList(play.getCards());
                                    creats.addAll(oppPlay.getCards());
                                    creats = creats.getType("Creature");
                                    obj = AllZone.Display.getChoiceOptional("Pick a creature to attach "
                                            + crd.getName() + " to", creats.toArray());
                                } else if(crd.getKeyword().contains("Enchant land")
                                        || crd.getKeyword().contains("Enchant land you control")) {
                                    CardList lands = new CardList(play.getCards());
                                    //lands.addAll(oppPlay.getCards());
                                    lands = lands.getType("Land");
                                    if(lands.size() > 0) obj = AllZone.Display.getChoiceOptional(
                                            "Pick a land to attach " + crd.getName() + " to", lands.toArray());
                                }
                                if(obj != null) {
                                    Card target = (Card) obj;
                                    if(AllZone.GameAction.isCardInPlay(target)) {
                                        crd.enchantCard(target);
                                    }
                                }
                            }
                            AllZone.GameAction.shuffle(c.getController());
                            //we have to have cards like glorious anthem take effect immediately:
                            for(String effect:AllZone.StaticEffects.getStateBasedMap().keySet()) {
                                Command com = GameActionUtil.commands.get(effect);
                                com.execute();
                            }
                            GameActionUtil.executeCardStateEffects();
                            
                        }
                    } else if(c.getController().equals("Computer")) {
                        enchantments = enchantments.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return !c.isAura();
                            }
                        });
                        if(enchantments.size() > 0) {
                            Card card = enchantments.get(0);
                            library.remove(card);
                            play.add(card);
                            AllZone.GameAction.shuffle(c.getController());
                            //we have to have cards like glorious anthem take effect immediately:
                            GameActionUtil.executeCardStateEffects();
                        }
                    }
                } //enchantments.size > 0
            }//Zur the enchanter
            
            else if(c.getName().equals("Yore-Tiller Nephilim") && !c.getCreatureAttackedThisCombat()) {
                PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, c.getController());
                PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
                
                CardList creatures = new CardList(grave.getCards());
                creatures = creatures.getType("Creature");
                
                if(creatures.size() > 0) {
                    if(c.getController().equals("Human")) {
                        Object o = AllZone.Display.getChoiceOptional("Pick a creature to put into play",
                                creatures.toArray());
                        if(o != null) {
                            Card card = (Card) o;
                            grave.remove(card);
                            play.add(card);
                            
                            card.tap();
                            AllZone.Combat.addAttacker(card);
                            //the card that gets put into play tapped and attacking might trigger another ability:
                            //however, this turns out to be incorrect rules-wise
                            //checkDeclareAttackers(card); 
                        }
                    } else if(c.getController().equals("Computer")) {
                        Card card = creatures.get(0);
                        grave.remove(card);
                        play.add(card);
                        
                        card.tap();
                        AllZone.Combat.addAttacker(card);
                        //checkDeclareAttackers(card);
                    }
                    
                } //if (creatures.size() > 0) 
            }//Yore-Tiller Nephilim
            else if(c.getName().equals("Flowstone Charger") && !c.getCreatureAttackedThisCombat()) {
                final Card charger = c;
                Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        
                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = -1703473800920781454L;
                            
                            public void execute() {
                                if(AllZone.GameAction.isCardInPlay(charger)) {
                                    charger.addTempAttackBoost(-3);
                                    charger.addTempDefenseBoost(3);
                                }
                            }
                        };//Command
                        

                        if(AllZone.GameAction.isCardInPlay(charger)) {
                            charger.addTempAttackBoost(3);
                            charger.addTempDefenseBoost(-3);
                            
                            AllZone.EndOfTurn.addUntil(untilEOT);
                        }
                    }//resolve
                    
                };//ability
                
                ability2.setStackDescription(c.getName() + " - gets +3/-3 until EOT.");
                AllZone.Stack.add(ability2);
                

            }//Flowstone Charger
            
            else if(c.getName().equals("Timbermaw Larva") && !c.getCreatureAttackedThisCombat()) {
                final Card charger = c;
                Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        final String player = charger.getController();
                        PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
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
                        String player = charger.getController();
                        PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                        CardList list = new CardList();
                        list.addAll(play.getCards());
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(Card card) {
                                return (card.isCreature() && !card.isTapped());
                            }
                        });
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
                
                ability2.setStackDescription(c.getName()
                        + " - gets +1/+1 until end of turn for each untapped creature" + c.getController()
                        + "controls.");
                AllZone.Stack.add(ability2);
                

            }//Knotvine Paladin
            

            else if(c.getName().equals("Goblin Piledriver") && !c.getCreatureAttackedThisCombat()) {
                final Card piledriver = c;
                Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        CardList list = new CardList();
                        list.addAll(AllZone.Combat.getAttackers());
                        list.addAll(AllZone.pwCombat.getAttackers());
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(Card card) {
                                return (!card.equals(piledriver) && card.isCreature() && (card.getType().contains(
                                        "Goblin") || card.getKeyword().contains("Changeling")));
                            }
                        });
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
                
                ability2.setStackDescription(c.getName()
                        + " - gets +2/+0 until end of turn for each other attacking Goblin.");
                AllZone.Stack.add(ability2);
                
            }//Goblin Piledriver
            
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
                
                ability2.setStackDescription(c.getName() + " - gets +2/+0 until EOT.");
                AllZone.Stack.add(ability2);
                
            }//+2+0 Chargers
            
            else if(c.getName().equals("Spectral Bears")) {
                String opp = AllZone.GameAction.getOpponent(c.getController());
                PlayerZone play = AllZone.getZone(Constant.Zone.Play, opp);
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
                String opp = AllZone.GameAction.getOpponent(c.getController());
                PlayerZone play = AllZone.getZone(Constant.Zone.Play, opp);
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
                
                ability2.setStackDescription(c.getName()
                        + " - gains trample until end of turn if its power is 10 or greater.");
                AllZone.Stack.add(ability2);
                

            }//Witch-Maw Nephilim
            
            else if(c.getName().equals("Jedit Ojanen of Efrava") && !c.getCreatureAttackedThisCombat()) {
                final Card jedit = c;
                Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        CardFactoryUtil.makeToken("Cat Warrior", "G 2 2 Cat Warrior", jedit, "G", new String[] {
                                "Creature", "Cat", "Warrior"}, 2, 2, new String[] {"Forestwalk"});
                        //(anger) :
                        //GameActionUtil.executeCardStateEffects(); 
                        
                    }
                }; //Ability
                

                ability2.setStackDescription(c.getName()
                        + " - put a 2/2 green Cat Warrior creature token with forestwalk into play.");
                AllZone.Stack.add(ability2);
                
            }//Jedit
            
            else if(c.getName().equals("Grave Titan") && !c.getCreatureAttackedThisCombat()) {
                final Card grave = c;
                Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                    	for (int i=0;i<2;i++)
                    		CardFactoryUtil.makeToken("Zombie", "B 2 2 Zombie", grave, "B", new String[] {
                                "Creature", "Zombie"}, 2, 2, new String[] {""});
                        //(anger) :
                        //GameActionUtil.executeCardStateEffects(); 
                    }
                }; //Ability
                

                ability2.setStackDescription(c.getName()
                        + " - put a 2/2 black Zombie creature token into play.");
                AllZone.Stack.add(ability2);
                
            }//Grave Titan
            
            else if(c.getName().equals("Primeval Titan") && !c.getCreatureAttackedThisCombat()) {
                final Card prim = c;
                Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                    	AllZone.GameAction.searchLibraryTwoLand("Land", prim.getController(), 
    							Constant.Zone.Play, true, 
    							Constant.Zone.Play, true);
                        //GameActionUtil.executeCardStateEffects(); 
                    }
                }; //Ability
                

                ability2.setStackDescription(c.getName()
                        + " - search your library for up to two land cards, put them onto the battlefield tapped, then shuffle your library.");
                AllZone.Stack.add(ability2);
                
            }//Primeval Titan
            
            else if(c.getName().equals("Sun Titan") && !c.getCreatureAttackedThisCombat()) {
                final Card sun = c;
                final Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                    	PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, sun.getController());
                        if(AllZone.GameAction.isCardInZone(getTargetCard(), grave)) {
                            PlayerZone play = AllZone.getZone(Constant.Zone.Play, sun.getController());
                            play.add(getTargetCard());
                            grave.remove(getTargetCard());
                        }
                    }
                }; //Ability
                

                //ability2.setStackDescription(c.getName() + " - ");
                

                Command command = new Command() {
                    private static final long serialVersionUID = 1658050744890095441L;
                    
                    public void execute() {
                        PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, sun.getController());
                        CardList graveList = new CardList(grave.getCards());
                        graveList = graveList.filter(new CardListFilter()
                        {
                        	public boolean addCard(Card crd)
                        	{
                        		return crd.isPermanent() && CardUtil.getConvertedManaCost(crd.getManaCost()) <=3;
                        	}
                        });
                        
                        if(graveList.size() == 0) return;
                        
                        if(sun.getController().equals(Constant.Player.Human)) {
                            Object o = AllZone.Display.getChoiceOptional("Select target card", graveList.toArray());
                            if(o != null) {
                                ability2.setTargetCard((Card) o);
                                AllZone.Stack.add(ability2);
                            }
                        } else//computer
                        {
                            Card best = CardFactoryUtil.AI_getBestCreature(graveList);
                            
                            if(best == null) {
                            	graveList.shuffle();
                                best = graveList.get(0);
                            }
                            ability2.setTargetCard(best);
                            AllZone.Stack.add(ability2);
                        }
                    }//execute()
                };//Command
                command.execute();
                
            }//Sun Titan
            
            
            else if(c.getName().equals("Preeminent Captain") && !c.getCreatureAttackedThisCombat()) {
                System.out.println("Preeminent Captain Attacks");
                PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getController());
                PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
                
                CardList soldiers = new CardList(hand.getCards());
                soldiers = soldiers.getType("Soldier");
                
                if(soldiers.size() > 0) {
                    if(c.getController().equals("Human")) {
                        Object o = AllZone.Display.getChoiceOptional("Pick a soldier to put into play",
                                soldiers.toArray());
                        if(o != null) {
                            Card card = (Card) o;
                            hand.remove(card);
                            play.add(card);
                            
                            card.tap();
                            AllZone.Combat.addAttacker(card);
                            //the card that gets put into play tapped and attacking might trigger another ability:
                            //however, this turns out to be incorrect rules-wise
                            //checkDeclareAttackers(card); 
                            card.setCreatureAttackedThisCombat(true);
                        }
                    } else if(c.getController().equals("Computer")) {
                        Card card = soldiers.get(0);
                        hand.remove(card);
                        play.add(card);
                        
                        card.tap();
                        AllZone.Combat.addAttacker(card);
                        //checkDeclareAttackers(card);
                        card.setCreatureAttackedThisCombat(true);
                    }
                    
                } //if (creatures.size() > 0) 
            }//Preeminent Captain
            
            else if(c.getName().equals("Nemesis of Reason") && !c.getCreatureAttackedThisCombat()) {
                AllZone.GameAction.mill( AllZone.GameAction.getOpponent(c.getController()),10);
            }//Nemesis of Reason
            
            else if(c.getName().equals("Novablast Wurm") && !c.getCreatureAttackedThisCombat()) {
                final Card Novablast_Wurm = c;
                CardList all = new CardList();
                all.addAll(AllZone.Human_Play.getCards());
                all.addAll(AllZone.Computer_Play.getCards());
                CardList wurms = new CardList();
                wurms.addAll(AllZone.Combat.getAttackers());
                wurms = wurms.filter(new CardListFilter()
                {
                	public boolean addCard(Card c)
                	{
                		return c.getName().equals("Novablast Wurm");
                	}
                });  
                if(wurms.size() > 1) {
                for(int i = 0; i < all.size(); i++) {
                    Card Card_Destroy = all.get(i);
                    if(Card_Destroy.isCreature()) AllZone.GameAction.destroy(Card_Destroy);
                }  
                } else {          	
                all = all.filter(new CardListFilter()
                {
                	public boolean addCard(Card check)
                	{
                		return !(check == Novablast_Wurm);
                	}
                });               
                for(int i = 0; i < all.size(); i++) {
                    Card Card_Destroy = all.get(i);
                    if(Card_Destroy.isCreature()) AllZone.GameAction.destroy(Card_Destroy);
                } 
                }
            }//Novablast Wurm
            
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
                        PlayerZone Hplay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                        PlayerZone Cplay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                        final CardList all = new CardList();
                    	all.addAll(Hplay.getCards());
                    	all.addAll(Cplay.getCards());
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
                ability.setStackDescription(c.getName() + " - taps up to 8 target permanents.");
        		CardList Silence = AllZoneUtil.getPlayerCardsInPlay(AllZone.GameAction.getOpponent(c.getController())); 		
        		Silence = Silence.getName("Linvala, Keeper of Silence");
        		if(Silence.size() == 0) {
        			Lorthos_Cancelled = false;
                if(c.getController().equals(Constant.Player.Human)) {
                    AllZone.InputControl.setInput(new Input_PayManaCost_Ability("Activate " + c.getName() + "'s ability: " + "\r\n",
                            ability.getManaCost(), paidCommand, unpaidCommand));
                } else //computer
                {
                    if(ComputerUtil.canPayCost(ability)) {
                    	ComputerUtil.playNoStack(ability);
                        PlayerZone Hplay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
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
                String player = c.getController();
                
                PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
                PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
                if(lib.size() > 0) {
                    CardList cl = new CardList();
                    cl.add(lib.get(0));
                    AllZone.Display.getChoiceOptional("Top card", cl.toArray());
                };
                Card top = lib.get(0);
                if(top.getType().contains("Creature")) {
                    AllZone.GameAction.gainLife(player, top.getBaseDefense());
                    AllZone.GameAction.getPlayerLife(player).subtractLife(top.getBaseAttack(),c);
                    hand.add(top);
                    lib.remove(top);
                };
                

            }//Sapling of Colfenor
            
            else if(c.getName().equals("Goblin Guide") && !c.getCreatureAttackedThisCombat()) {
                final String opp = AllZone.GameAction.getOpponent(c.getController());
                
                Ability ability = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        PlayerZone lib = AllZone.getZone(Constant.Zone.Library, opp);
                        if(lib.size() > 0) {
                            CardList cl = new CardList();
                            cl.add(lib.get(0));
                            AllZone.Display.getChoiceOptional("Top card:", cl.toArray());
                            
                            Card c = cl.get(0);
                            if(c.isLand()) {
                                AllZone.GameAction.moveToHand(c);
                            }
                        }
                    }
                };
                ability.setStackDescription("Goblin Guide - defending player reveals the top card of his or her library. If it's a land card, that player puts it into his or her hand.");
                AllZone.Stack.add(ability);
            }//Goblin Guide
            
            else if(c.getName().equals("Pulse Tracker") && !c.getCreatureAttackedThisCombat()) {
                final String opp = AllZone.GameAction.getOpponent(c.getController());
                final Card F_card = c;
                Ability ability = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        AllZone.GameAction.getPlayerLife(opp).subtractLife(1,F_card);
                    }
                };
                ability.setStackDescription("Pulse Tracker - Whenever Pulse Tracker attacks, each opponent loses 1 life.");
                AllZone.Stack.add(ability);
            }//Pulse Tracker
            
            if(c.getName().equals("Time Elemental")) {
            	final Card source = c;
            	final Ability damage = new Ability(c, "0") {
            		@Override
            		public void resolve() {
            			final String player = source.getController();
            			AllZone.GameAction.addDamage(player, source, 5);
            		}
            	};
            	damage.setStackDescription(c + " - deals 5 damage to controller.");
            	final Ability sacrifice = new Ability(c, "0") {
            		@Override
            		public void resolve() {
            			AllZone.GameAction.sacrifice(source);
            		}
            	};
            	sacrifice.setStackDescription("Sacrifice "+c);
                
                final Command atEOCdamage = new Command() {
    				private static final long serialVersionUID = 1513673469721590317L;

    				public void execute() {
                        AllZone.Stack.add(damage);
                    }
                };
                final Command atEOCsacrifice = new Command() {
    				private static final long serialVersionUID = -510924602971034173L;

    				public void execute() {
                        if(AllZone.GameAction.isCardInPlay(source)) AllZone.Stack.add(sacrifice);
                    }
                };
                AllZone.EndOfCombat.addAt(atEOCdamage);
                AllZone.EndOfCombat.addAt(atEOCsacrifice);
            }

            c.setCreatureAttackedThisCombat(true);
            
        }//if Phase = declare attackers
    }//checkDeclareAttackers
    
    public static void checkUnblockedAttackers(Card c) {
    	
    	AllZone.GameAction.CheckWheneverKeyword(c,"isUnblocked",null);
    	
        if(c.getName().equals("Guiltfeeder")) {
            final String player = c.getController();
            final String opponent = AllZone.GameAction.getOpponent(player);
            final Card F_card = c;
            Ability ability2 = new Ability(c, "0") {
                @Override
                public void resolve() {
                    
                    PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, opponent);
                    CardList cardsInGrave = new CardList(graveyard.getCards());
                    PlayerLife life = AllZone.GameAction.getPlayerLife(opponent);
                    
                    life.subtractLife(cardsInGrave.size(),F_card);
                    
                }
            };// ability2
            
            ability2.setStackDescription(c.getName() + " - " + opponent
                    + " loses life equal to cards in graveyard.");
            AllZone.Stack.add(ability2);
            
        } else if(c.getName().equals("Crypt Cobra") || c.getName().equals("Suq'Ata Assassin")
                || c.getName().equals("Swamp Mosquito")) {
            String controller = c.getController();
            String opp = AllZone.GameAction.getOpponent(controller);
            
            if(opp.equals(Constant.Player.Human)) AllZone.Human_PoisonCounter.addPoisonCounters(1);
            else AllZone.Computer_PoisonCounter.addPoisonCounters(1);
        }
        

    }
    
    static void checkDeclareBlockers(CardList cl) {
    	//System.out.println("Phase during checkDeclareBlockers: " + AllZone.Phase.getPhase());
        if(AllZone.Phase.getPhase().equals(Constant.Phase.Combat_After_Declare_Blockers) ||
           AllZone.Phase.getPhase().equals(Constant.Phase.Combat_Declare_Blockers_InstantAbility) ) {
            
        	for (Card c:cl)
        	{
        		AllZone.GameAction.CheckWheneverKeyword(c,"Blocks",null);
        		
        		if (!c.getCreatureBlockedThisCombat()) {
                	for(Ability ab:CardFactoryUtil.getBushidoEffects(c)) {
                		AllZone.Stack.add(ab);
                	}
                }
        		
	        	if (c.getKeyword().contains("Defender") && !c.getCreatureBlockedThisCombat())
	        	{
	        		final Card crd = c;
	        		CardList pcs = CardFactoryUtil.getCards("Perimeter Captain", c.getController());
		        	for (int i = 0; i < pcs.size();i++)
		        	{
		        		Ability ability = new Ability(pcs.get(i), "0")
		        		{
		        			public void resolve()
		        			{
		        		        AllZone.GameAction.gainLife(crd.getController(), 2);
		        			}
		        		};
		        		ability.setStackDescription(pcs.get(i) + " - " + c.getController() + " gains 2 life.");
		        		
		        		if (c.getController().equals(Constant.Player.Human)) {
			        		String[] choices = {"Yes", "No"};
			        		Object q = null;
			                q = AllZone.Display.getChoiceOptional("Gain 2 life from Perimeter Captain?", choices);
			                if (q != null)
			                	if (q.equals("Yes"))
			                		AllZone.Stack.add(ability);
		        		}
		        		else
		        			AllZone.Stack.add(ability);
		        	}
	        	}
	        	
	            if(c.getName().equals("Jedit Ojanen of Efrava") && !c.getCreatureBlockedThisCombat()) {
	                Card card = new Card();
	                
	                card.setOwner(c.getController());
	                card.setController(c.getController());
	                
	                card.setName("Cat Warrior");
	                card.setImageName("G 2 2 Cat Warrior");
	                card.setManaCost("G");
	                card.setToken(true);
	                card.addIntrinsicKeyword("Forestwalk");
	                
	                card.addType("Creature");
	                card.addType("Cat");
	                card.addType("Warrior");
	                card.setBaseAttack(2);
	                card.setBaseDefense(2);
	                
	                PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
	                play.add(card);
	                
	                //(anger) :
	                GameActionUtil.executeCardStateEffects();
	            }//Jedit
	            else if(c.getName().equals("Shield Sphere") && !c.getCreatureBlockedThisCombat()) {
	                //int toughness = c.getNetDefense();
	                //c.setDefense(toughness-1);
	                c.addCounter(Counters.P0M1, 1);
	                //ability2.setStackDescription(c.getName() + " blocks and gets a 0/-1 counter.");
	                //AllZone.Stack.add(ability2);
	                
	            }//Shield Sphere
	            
	            else if(c.getName().equals("Meglonoth") && !c.getCreatureBlockedThisCombat()) {
	                AllZone.GameAction.addDamage(AllZone.GameAction.getOpponent(c.getController()), c, c.getNetAttack());
	            }//Meglonoth
	            c.setCreatureBlockedThisCombat(true);
        	}//for
            
        }//if Phase == after declare blockers
    }//checkDeclareBlockers
    
    public static void checkBlockedAttackers(Card a, Card b) {
        //System.out.println(a.getName() + " got blocked by " + b.getName());
    	if(!a.getCreatureGotBlockedThisCombat()) 
    		AllZone.GameAction.CheckWheneverKeyword(a,"BecomesBlocked",null);
    	
        if(!a.getCreatureGotBlockedThisCombat()) {
            for(Ability ab:CardFactoryUtil.getBushidoEffects(a))
                AllZone.Stack.add(ab);
        }
        
        ////////////////////Rampage
        //not sure why this is "not" - but I copied from Bushido...
        if(!a.getCreatureGotBlockedThisCombat()) {
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
        	}
        }
        ////////////////////END Rampage
        
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
            b.setCreatureAttackedThisCombat(true);
            
            ability2.setStackDescription(b.getName() + " - gets -" + mag + "/-" + mag + " until EOT.");
            AllZone.Stack.add(ability2);
            Log.debug("Adding Flanking!");
            //AllZone.GameAction.checkStateEffects();
            
        }//flanking
        
        
        if(b.hasStartOfKeyword("Whenever CARDNAME blocks a creature, destroy that creature at end of combat")) {
    		int KeywordPosition = b.getKeywordPosition("Whenever CARDNAME blocks a creature, destroy that creature at end of combat");
    		String parse = b.getKeyword().get(KeywordPosition).toString();
    		String k[] = parse.split(":");
    		final String restrictions[] = k[1].split(",");
    		if(a.isValidCard(restrictions)) {
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
    		if(b.isValidCard(restrictions)) {
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
        
        
        if (a.getName().equals("Slith Strider") && !a.getCreatureGotBlockedThisCombat()) {
            String player = a.getController();
            AllZone.GameAction.drawCard(player);
        } else if(a.getName().equals("Corrupt Official") && !a.getCreatureGotBlockedThisCombat()) {
            String opp = b.getController();
            AllZone.GameAction.discardRandom(opp, a.getSpellAbility()[0]);
        } else if (a.getName().equals("Robber Fly") && !a.getCreatureGotBlockedThisCombat()) {
            String opp = b.getController();
            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, opp);
            CardList list = new CardList(hand.getCards());
            int handSize = list.size();
            
            for (int ctr = 0; ctr < handSize; ctr ++) {   // opponent discards their hand,
                AllZone.GameAction.discardRandom(opp, a.getSpellAbility()[0]);
            }
            for (int ctr = 0; ctr < handSize; ctr ++) {   // then draws that many cards
                AllZone.GameAction.drawCard(opp);;
            }
        } else if(a.getName().equals("Sylvan Basilisk")) {
            AllZone.GameAction.destroy(b);
            System.out.println("destroyed blocker " + b.getName());
        } else if(a.getName().equals("Quagmire Lamprey")) {
            b.addCounter(Counters.M1M1, 1);
        } else if(a.getName().equals("Elven Warhounds")) {
            PlayerZone play = AllZone.getZone(Constant.Zone.Play, b.getController());
            PlayerZone library = AllZone.getZone(Constant.Zone.Library, b.getController());
            
            play.remove(b);
            library.add(b, 0);
        } else if((a.getName().equals("Silkenfist Order") || a.getName().equals("Silkenfist Fighter"))
                && !a.getCreatureBlockedThisCombat()) {
            a.untap();
        } else if (a.getName().equals("Deepwood Tantiv") && !a.getCreatureBlockedThisCombat()) {
        	AllZone.GameAction.gainLife(a.getController(), 2);
        } else if (a.getName().equals("Sacred Prey") && !a.getCreatureBlockedThisCombat()) {
            AllZone.GameAction.gainLife(a.getController(), 1);
        } else if (a.getName().equals("Vedalken Ghoul") && !a.getCreatureBlockedThisCombat()) {
             AllZone.GameAction.getPlayerLife(b.getController()).subtractLife(4,a);
        }
        
        else if ((a.getName().equals("Chambered Nautilus") || a.getName().equals("Saprazzan Heir") 
                || a.getName().equals("Drelnoch")) && !a.getCreatureBlockedThisCombat()) {
            String player = a.getController();
            int numCards = 3;
            if (a.getName().equals("Drelnoch")) numCards = 2;
            if (a.getName().equals("Chambered Nautilus")) numCards = 1;
            int choice = 0;
            int compLibSize = AllZone.getZone(Constant.Zone.Library, Constant.Player.Computer).size();
            int compHandSize = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer).size();
            
            if (player.equals ("Human")) {
            	StringBuilder title = new StringBuilder();
                title.append(a.getName()).append(" Ability");
                StringBuilder message = new StringBuilder();
                message.append("Do you want to draw ").append(numCards).append(" cards?");
                choice = JOptionPane.showConfirmDialog(null, message.toString(), title.toString(), JOptionPane.YES_NO_OPTION);
            }// if player.equals Human
            
            if ((choice == JOptionPane.YES_OPTION && player.equals ("Human")) 
                    || (player.equals ("Computer") && (compLibSize >= (2 * numCards)  && compHandSize <= (7 - numCards)))) {
                for (int i = 0; i < numCards; i++) {
                    AllZone.GameAction.drawCard(player);
                }
            }
        }// if Saprazzan Heir or Drelnoch or Chambered Nautilus was blocked
        
        if(b.getName().equals("Frostweb Spider") && (a.getKeyword().contains("Flying"))) {
            final Card spider = b;
            
            final Ability ability = new Ability(b, "0") {
                @Override
                public void resolve() {
                    spider.addCounter(Counters.P1P1, 1);
                }
            };
            
            ability.setStackDescription(spider + " - gets a +1/+1 counter.");
            
            final Command atEOC = new Command() {
                private static final long serialVersionUID = 6617320324660612694L;
                
                public void execute() {
                    if(AllZone.GameAction.isCardInPlay(spider)) AllZone.Stack.add(ability);
                }
            };
            
            AllZone.EndOfCombat.addAt(atEOC);
        }//Frostweb Spider
        
        /* converted to keyword
        else if(b.getName().equals("Abomination")
                && (CardUtil.getColors(a).contains(Constant.Color.White) || CardUtil.getColors(a).contains(
                        Constant.Color.Green))) {
            final Card attacker = a;
            final Ability ability = new Ability(b, "0") {
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(attacker)) {
                        AllZone.GameAction.destroy(attacker);
                    }
                }
            };
            
            StringBuilder sb = new StringBuilder();
            sb.append(b).append(" - destroy blocked green or white creature.");
            ability.setStackDescription(sb.toString());
            
            final Command atEOC = new Command() {
                private static final long serialVersionUID = 5854485314766349980L;
                
                public void execute() {
                    AllZone.Stack.add(ability);
                }
            };
            
            AllZone.EndOfCombat.addAt(atEOC);
        }//Abomination blocking
        
        if(a.getName().equals("Abomination")
                && (CardUtil.getColors(b).contains(Constant.Color.White) || CardUtil.getColors(b).contains(
                        Constant.Color.Green))) {
            final Card blocker = b;
            final Ability ability = new Ability(a, "0") {
                @Override
                public void resolve() {
                    AllZone.GameAction.destroy(blocker);
                }
            };
            
            StringBuilder sb = new StringBuilder();
            sb.append(a).append(" - destroy blocking green or white creature.");
            ability.setStackDescription(sb.toString());
            
            final Command atEOC = new Command() {
                
                private static final long serialVersionUID = -9077416427198135373L;
                
                public void execute() {
                    if(AllZone.GameAction.isCardInPlay(blocker)) AllZone.Stack.add(ability);
                }
            };
            
            AllZone.EndOfCombat.addAt(atEOC);
        }//Abomination attacking
        
        else if (b.getName().equals("Gorgon Recluse") 
                    && !CardUtil.getColors(a).contains(Constant.Color.Black)) {
            final Card attacker = a;
            final Ability ability = new Ability(b, "0") {
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(attacker)) {
                        AllZone.GameAction.destroy(attacker);
                    }
                }
            };
            
            StringBuilder sb = new StringBuilder();
            sb.append(b).append(" - destroy blocked nonblack creature.");
            ability.setStackDescription(sb.toString());
            
            final Command atEOC = new Command() {
                private static final long serialVersionUID = -2166473476701642189L;

                public void execute() {
                    AllZone.Stack.add(ability);
                }
            };
            
            AllZone.EndOfCombat.addAt(atEOC);
        }//Gorgon Recluse blocking
        
        if (a.getName().equals("Gorgon Recluse")
                && !CardUtil.getColors(b).contains(Constant.Color.Black)) {
            final Card blocker = b;
            final Ability ability = new Ability(a, "0") {
                @Override
                public void resolve() {
                    AllZone.GameAction.destroy(blocker);
                }
            };
            
            StringBuilder sb = new StringBuilder();
            sb.append(a).append(" - destroy blocking nonblack creature.");
            ability.setStackDescription(sb.toString());
            
            final Command atEOC = new Command() {
                private static final long serialVersionUID = -8606306519001580192L;

                public void execute() {
                    if (AllZone.GameAction.isCardInPlay(blocker)) AllZone.Stack.add(ability);
                }
            };
            
            AllZone.EndOfCombat.addAt(atEOC);
        }//Gorgon Recluse attacking
        */
        
        else if (b.getName().equals("Alaborn Zealot")) {
        	final Card blocker = b; 
        	final Card attacker = a;
        	final Ability ability = new Ability(b, "0") {
        		@Override
        		public void resolve() {
        			AllZone.GameAction.destroy(attacker);
        			AllZone.GameAction.destroy(blocker);
        		}
        	};
        	
        	ability.setStackDescription(b + " - destroy attacking creature.");
        	AllZone.Stack.add(ability);
        }

        else if(b.getName().equals("AEther Membrane") || b.getName().equals("Aether Membrane") || b.getName().equals("Wall of Tears")) {
            final Card attacker = a;
            final Ability ability = new Ability(b, "0") {
                @Override
                public void resolve() {
                	if(attacker.isToken())
                		AllZone.GameAction.removeFromGame(attacker);
                	else {
                		PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, attacker.getOwner());
                		AllZone.GameAction.moveTo(hand, attacker);
                	}
                }
            };
            
            ability.setStackDescription(b + " - return blocked creature to owner's hand.");
            
            final Command atEOC = new Command() {
                private static final long serialVersionUID = 5263273480814811314L;
                
                public void execute() {
                    if(AllZone.GameAction.isCardInPlay(attacker)) AllZone.Stack.add(ability);
                }
            };
            
            AllZone.EndOfCombat.addAt(atEOC);
        }
        
        if(b.getName().equals("Time Elemental")) {
        	final Card source = b;
        	final Ability damage = new Ability(b, "0") {
        		@Override
        		public void resolve() {
        			final String player = source.getController();
        			AllZone.GameAction.addDamage(player, source, 5);
        		}
        	};
        	damage.setStackDescription(b + " - deals 5 damage to controller.");
        	final Ability sacrifice = new Ability(b, "0") {
        		@Override
        		public void resolve() {
        			AllZone.GameAction.sacrifice(source);
        		}
        	};
        	sacrifice.setStackDescription("Sacrifice "+b);
            
            final Command atEOCdamage = new Command() {
				private static final long serialVersionUID = -1470724468078097507L;

				public void execute() {
                    AllZone.Stack.add(damage);
                }
            };
            final Command atEOCsacrifice = new Command() {
				private static final long serialVersionUID = 7644622095917060596L;

				public void execute() {
                    if(AllZone.GameAction.isCardInPlay(source)) AllZone.Stack.add(sacrifice);
                }
            };
            AllZone.EndOfCombat.addAt(atEOCdamage);
            AllZone.EndOfCombat.addAt(atEOCsacrifice);
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
            ability.setStackDescription(c + " - (Exalted) gets +1/+1 until EOT.");
            AllZone.Stack.add(ability);
        }
        
        String phasingPlayer = c.getController();
        // Finest Hour untaps the creature on the first combat phase
		if ((GameActionUtil.countFinestHours(phasingPlayer)>0) &&
				AllZone.Phase.isFirstCombat()) {
			// Untap the attacking creature
			Ability fhUntap = new Ability(c, "0") {
				public void resolve() {
				    crd.untap();
				}
			};
			fhUntap.setStackDescription(c + " - (Exalted) untap.");
			AllZone.Stack.add(fhUntap);
		
			// If any Finest Hours, queue up a new combat phase
			for (int ix = 0; ix < GameActionUtil.countFinestHours(phasingPlayer); ix++) {
				Ability fhAddCombat = new Ability(c, "0") {
					public void resolve() {
						AllZone.Phase.addExtraCombat(crd.getController());				
					}
				};
				fhAddCombat.setStackDescription(c + " - (Exalted) " + phasingPlayer + " gets Extra Combat Phase.");
				AllZone.Stack.add(fhAddCombat);
			}
		}
        if(GameActionUtil.isRafiqInPlay(phasingPlayer)) {
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
            ability2.setStackDescription(c + " - (Exalted) gets Double Strike until EOT.");
            AllZone.Stack.add(ability2);
        }
        
        if(GameActionUtil.getBattleGraceAngels(phasingPlayer) > 0) {
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
            ability3.setStackDescription(c + " - (Exalted) gets Lifelink until EOT.");
            AllZone.Stack.add(ability3);
        }
        if(GameActionUtil.get_Sovereigns_of_Lost_Alaras(phasingPlayer) > 0) {
            for(int i = 0; i < GameActionUtil.get_Sovereigns_of_Lost_Alaras(phasingPlayer); i++) { 
            	final Card attacker = c;
            Ability ability4 = new Ability(c, "0") {
                @Override
                public void resolve() {
                	PlayerZone library = AllZone.getZone(Constant.Zone.Library, attacker.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, attacker.getController()); 
                    
                    CardList enchantments = new CardList(library.getCards());
                    final String turn = attacker.getController();
                    enchantments = enchantments.filter(new CardListFilter() {                      
                        public boolean addCard(Card c) {
                        	if(attacker.hasKeyword("Protection from enchantments") || (attacker.hasKeyword("Protection from everything"))) return false;
                        	ArrayList<String> keywords = c.getKeyword();
                        	for(String keyword:keywords) {
                        		if(keyword.startsWith("Enchant creature")) {
                                    if(c.isEnchantment())  {
                                    	String [] colors = new String[6];
                                    ArrayList<String> color = CardUtil.getColors(c);
                                    if(color.contains(Constant.Color.Black)) 	colors[0] = "black";
                                    if(color.contains(Constant.Color.Blue)) 	colors[1] = "blue";
                                    if(color.contains(Constant.Color.Green)) 	colors[2] = "green";
                                    if(color.contains(Constant.Color.Red))	 	colors[3] = "red";
                                    if(color.contains(Constant.Color.White)) 	colors[4] = "white"; 
                                    if(color.contains(Constant.Color.Colorless))colors[5] = "artifacts";                                   
    	                            for(int i = 0; i < colors.length; i++) {
                                    if(attacker.hasKeyword("Protection from " + colors[i]) == true) return false; 
    	                            }
                                		} return true;
                                		}
                        		if(turn == "Human") {
                            		if(keyword.startsWith("Enchant Creature")) {
                        			if(keyword.endsWith("Curse")) {
                                    	String [] colors = new String[6];
                                        ArrayList<String> color = CardUtil.getColors(c);
                                        if(color.contains(Constant.Color.Black)) 	colors[0] = "black";
                                        if(color.contains(Constant.Color.Blue)) 	colors[1] = "blue";
                                        if(color.contains(Constant.Color.Green)) 	colors[2] = "green";
                                        if(color.contains(Constant.Color.Red))	 	colors[3] = "red";
                                        if(color.contains(Constant.Color.White)) 	colors[4] = "white"; 
                                        if(color.contains(Constant.Color.Colorless))colors[5] = "artifacts";                                   
        	                            for(int i = 0; i < colors.length; i++) {
                                        if(attacker.hasKeyword("Protection from " + colors[i]) == true) return false; 
        	                            }
                                    		} return true;
                        		}
                        		}
                        	}
                             return false;
                        }
                    });
	                    String player = attacker.getController();
	                    Card Enchantment = null;
	                    if(player == "Human"){
                            Card[] Target = new Card[enchantments.size()];
                            for(int i = 0; i < enchantments.size(); i++) {
                				Card crd = enchantments.get(i);
                				Target[i] = crd;
                            }
	                        Object check = AllZone.Display.getChoiceOptional("Select enchantment to enchant exalted creature", Target);
	                        if(check != null) {
	                           Enchantment = ((Card) check);	
	                        }
	                    } else {
	                           Enchantment = CardFactoryUtil.AI_getBestEnchantment(enchantments,attacker, false);
	                    }
	                    if(Enchantment != null && AllZone.GameAction.isCardInPlay(attacker)){
	                    	library.remove(Enchantment);
	                    	play.add(Enchantment);
	                    	Enchantment.enchantCard(attacker);
	                    }
                        if(player == "Human") AllZone.GameAction.shuffle(attacker.getController());	                    
                }//resolve
            };// ability4
            ability4.setStackDescription(c + " - (Exalted) searches library for an Aura card that could enchant that creature, put it into play attached to that creature, then shuffles library. ");
            AllZone.Stack.add(ability4);
            } // For
        }
    }
    
    
    /////////////////////////Rampage
    /**
     * executes rampage abilities for a given card
     * 
     * @param c the card to add rampage bonus to
     * @param magnitude the magnitude of rampage (ie Rampage 2 means magnitude should be 2)
     * @param numBlockers - the number of creatures blocking this rampaging creature
     */
    private static void executeRampageAbility(Card c, int magnitude, int numBlockers) {
    	//TODO - possibly can get magnitude from Keyword on the Card here
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
    		ability.setStackDescription(c + " - (Rampage) gets +"+pump+"/+"+pump+" until EOT.");
    		AllZone.Stack.add(ability);
    	}
    }
    /////////////////////////END Rampage
    
}//Class CombatUtil
