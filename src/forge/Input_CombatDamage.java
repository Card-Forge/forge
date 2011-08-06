
package forge;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class Input_CombatDamage extends Input {
    private static final long serialVersionUID = -8549102582210309044L;
    
    public Input_CombatDamage() {
        AllZone.Combat.verifyCreaturesInPlay();
        AllZone.pwCombat.verifyCreaturesInPlay();
        
        CombatUtil.showCombat();
    }
    
    @Override
    public void showMessage() {
        ButtonUtil.enableOnlyOK();
        AllZone.Display.showMessage("Combat Damage is on the stack - Play Instants and Abilities");
    }
    
    @Override
    public void selectButtonOK() {
        if (!AllZone.GameInfo.isPreventCombatDamageThisTurn())
        	damageCreatureAndPlayer();
        
        AllZone.GameAction.checkStateEffects();
        
        AllZone.Combat.reset();
        AllZone.Display.showCombat("");
        
        //AllZone.Phase.nextPhase();
        //for debugging: 
        System.out.println("need to nextPhase(Input_CombatDamage.selectButtonOK) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    @Override
    public void selectCard(Card card, PlayerZone zone) {
        InputUtil.playInstantAbility(card, zone);
    }//selectCard()
    
    @SuppressWarnings("unused")
    // playerDamage
    private void playerDamage(PlayerLife p) {
        int n = p.getAssignedDamage();
        p.setAssignedDamage(0);
        p.subtractLife(n);
    }
    
    //moves assigned damage to damage for all creatures
    //deals damage to player if needed
    public void damageCreatureAndPlayer() {
        String player = AllZone.Combat.getDefendingPlayer();
        if(player.equals("")) //this is a really bad hack, to allow raging goblin to attack on turn 1
        	player = Constant.Player.Computer;
        PlayerLife life = AllZone.GameAction.getPlayerLife(player);
        
        life.subtractLife(AllZone.Combat.getTotalDefendingDamage());
        

        //why???
        /*
        life = AllZone.GameAction.getPlayerLife(AllZone.Combat.getAttackingPlayer());
        life.subtractLife(AllZone.Combat.getAttackingDamage());
        life.subtractLife(AllZone.pwCombat.getAttackingDamage());
        */

        CardList unblocked = new CardList(AllZone.Combat.getUnblockedAttackers());
        for(int j = 0; j < unblocked.size(); j++) {
            //System.out.println("Unblocked Creature: " +unblocked.get(j).getName());
            //if (unblocked.getCard(j).hasSecondStrike()) {
            if(!unblocked.getCard(j).hasFirstStrike()
                    || (unblocked.getCard(j).hasFirstStrike() && unblocked.getCard(j).hasDoubleStrike())) {
                GameActionUtil.executePlayerCombatDamageEffects(unblocked.get(j));
                CombatUtil.checkUnblockedAttackers(unblocked.get(j));
            }
            
        }
        //GameActionUtil.executePlayerCombatDmgOptionalEffects(unblocked.toArray());
        
        CardList attackers = new CardList(AllZone.Combat.getAttackers());
        CardList blockers = new CardList(AllZone.Combat.getAllBlockers().toArray());
        
        for(int i = 0; i < attackers.size(); i++) {
            
            //this shouldn't trigger if creature has first strike, only if it also has double strike
            if(!attackers.getCard(i).hasFirstStrike()
                    || (attackers.getCard(i).hasFirstStrike() && attackers.getCard(i).hasDoubleStrike())) {
                ArrayList<String> list = attackers.getCard(i).getKeyword();
                
                CardList defend = AllZone.Combat.getBlockers(attackers.getCard(i));
                //System.out.println("creatures blocking " + attackers.getCard(i).getName() + " : " +defend.size());
                
                /*
                //hack, don't get lifelink if a doublestriker kills his blocker with first strike damage
                if (!attackers.getCard(i).hasDoubleStrike() || (defend.size()!=0 && attackers.getCard(i).hasDoubleStrike()) )
                {
                	for (int j=0; j < list.size(); j++)
                    {
                    	if (list.get(j).equals("Lifelink"))
                    		GameActionUtil.executeLifeLinkEffects(attackers.getCard(i));
                    	
                    }
                }
                */

                if(!attackers.getCard(i).hasDoubleStrike()
                        || (attackers.getCard(i).hasDoubleStrike() && !AllZone.Combat.isBlocked(attackers.getCard(i)))
                        || (attackers.getCard(i).hasDoubleStrike()
                                && AllZone.Combat.isBlocked(attackers.getCard(i)) && defend.size() != 0)) {
                    /* 
                    //old stuff: lifelink triggers on multiple instances of the lifelink keyword
                    for (int j=0; j < list.size(); j++)
                    {
                    	if (list.get(j).equals("Lifelink"))
                    		GameActionUtil.executeLifeLinkEffects(attackers.getCard(i));
                    }
                    */

                    CombatUtil.executeCombatDamageEffects(attackers.getCard(i));
                }
                
                //not sure if this will work correctly with multiple blockers?
                int defenderToughness = 0;
                for(int k = 0; k < defend.size(); k++) {
                    defenderToughness += defend.get(k).getNetDefense();
                }
                if((!attackers.getCard(i).hasFirstStrike() || (attackers.getCard(i).hasFirstStrike() && attackers.getCard(
                        i).hasDoubleStrike()))
                        && list.contains("Trample")
                        && defenderToughness < attackers.getCard(i).getNetAttack()
                        && AllZone.Combat.isBlocked(attackers.getCard(i))) {
                    GameActionUtil.executePlayerCombatDamageEffects(attackers.getCard(i));
                }
                
            }
            
        }
        for(int i = 0; i < blockers.size(); i++) {
            //System.out.println("blocker #" + i + ": " + blockers.getCard(i).getName() +" " + blockers.getCard(i).getAttack());
            /*
            if (blockers.getCard(i).getKeyword().contains("Lifelink"))
            {
            	GameActionUtil.executeLifeLinkEffects(blockers.getCard(i));
            }
            */

            //this shouldn't trigger if creature has first strike, only if it also has double strike
            

            //if (blockers.get(i).hasSecondStrike())
            if(!blockers.getCard(i).hasFirstStrike()
                    || (blockers.getCard(i).hasFirstStrike() && blockers.getCard(i).hasDoubleStrike())) {
                
                CombatUtil.executeCombatDamageEffects(blockers.getCard(i));
                /*
                ArrayList<String> list = blockers.getCard(i).getKeyword();
                for (int j=0; j < list.size(); j++)
                {
                	if (list.get(j).equals("Lifelink"))
                		GameActionUtil.executeLifeLinkEffects(blockers.getCard(i));
                }
                
                */
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
        

        Card c;
        for(int i = 0; i < all.size(); i++) {
            c = all.get(i);
            //because this sets off Jackal Pup, and Filthly Cur damage ability
            //and the stack says "Jack Pup causes 0 damage to the Computer"
            if(c.getTotalAssignedDamage() != 0) {
                /*
                //c.addDamage(c.getAssignedDamage());
                System.out.println("Calling addDamage for card " + c.getName());
                AllZone.GameAction.addDamage(c, c.getAssignedDamage());
                c.setAssignedDamage(0);
                
                */
                //AllZone.GameAction.addDamage(c, c.getTotalAssignedDamage());
                
                HashMap<Card, Integer> assignedDamageMap = c.getAssignedDamageHashMap();
                HashMap<Card, Integer> damageMap = new HashMap<Card, Integer>();
                
                Iterator<Card> iter = assignedDamageMap.keySet().iterator();
                while(iter.hasNext()) {
                    Card crd = iter.next();
                    //AllZone.GameAction.addDamage(c, crd , assignedDamageMap.get(crd));
                    /*
                    for (String effect : AllZone.StateBasedEffects.getStateBasedMap().keySet() ) {
                    Command com = GameActionUtil.commands.get(effect);
                    com.execute();
                    }
                    
                    GameActionUtil.executeCardStateEffects();
                    */

                    damageMap.put(crd, assignedDamageMap.get(crd));
                }
                AllZone.GameAction.addDamage(c, damageMap);
                
                AllZone.GameAction.checkWinLoss();
                
                damageMap.clear();
                c.clearAssignedDamage();
            }
        }
    }//moveDamage()
}
