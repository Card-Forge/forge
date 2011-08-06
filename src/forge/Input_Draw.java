
package forge;


public class Input_Draw extends Input {
    private static final long serialVersionUID = -2341125041806280507L;
    
    @Override
    public void showMessage() {
        if(AllZone.Phase.getActivePlayer().equals(Constant.Player.Computer)) {
            AllZone.GameAction.drawCard(Constant.Player.Computer);
            
            //for debugging: System.out.println("need to nextPhase(from Input_Draw on computer's draw) = true");
            AllZone.Phase.setNeedToNextPhase(true);
            return;
        }
        
        AllZone.GameInfo.setHumanPlayedLands(0);
        
        //check if human should skip their draw phase
        CardList humanCards = new CardList();
        humanCards.addAll(AllZone.Human_Play.getCards());
        boolean humanSkipsDrawPhase = humanCards.containsName("Necropotence")
                || humanCards.containsName("Yawgmoth's Bargain");
        
        if(AllZone.Phase.getPhase().equals(Constant.Phase.Draw) && humanSkipsDrawPhase) {
            AllZone.Phase.setNeedToNextPhase(true);
            
        } else { //continue with draw phase
            boolean drawCard = true;
            //this looks like Human only code, so this should be safe
            final String player = AllZone.Phase.getActivePlayer();
            
            /*
             * Mana Vault - At the beginning of your draw step, if Mana Vault
             * is tapped, it deals 1 damage to you.
             */
            CardList manaVaults = AllZoneUtil.getPlayerCardsInPlay(player, "Mana Vault");
            for(Card manaVault:manaVaults) {
            	final Card vault = manaVault;
            	if(vault.isTapped()) {
            		final Ability damage = new Ability(vault, "0") {
            			@Override
            			public void resolve() {
            				AllZone.GameAction.addDamage(player, vault, 1);
            			}
            		};//Ability
            		damage.setStackDescription(vault+" - does 1 damage to "+player);
            		AllZone.Stack.add(damage);
            	}
            }
            
            if(drawCard && AllZone.Phase.getTurn() > 1) AllZone.GameAction.drawCard(Constant.Player.Human);
            
            if(AllZone.Phase.getPhase().equals(Constant.Phase.Draw)) {
                
                //AllZone.Phase.nextPhase();
                //for debugging: System.out.println("need to nextPhase(from Input_Draw on human's draw) = true");
                AllZone.Phase.setNeedToNextPhase(true);
            } else stop();
        }
    } //end necro check
}
