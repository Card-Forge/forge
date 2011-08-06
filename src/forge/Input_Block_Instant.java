
package forge;


public class Input_Block_Instant extends Input {
    private static final long serialVersionUID = 6024555691502280746L;
    
    @Override
    public void showMessage() {
        //GameActionUtil.executeExaltedEffects();
        
        AllZone.Combat.verifyCreaturesInPlay();
        CombatUtil.showCombat();
        
        ButtonUtil.enableOnlyOK();
        AllZone.Display.showMessage("Declare Blockers: Play Instants and Abilities");
    }
    
    @Override
    public void selectButtonOK() {
    	
    	CardList list = new CardList();
        list.addAll(AllZone.Combat.getAllBlockers().toArray());
        list.addAll(AllZone.pwCombat.getAllBlockers().toArray());
        list = list.filter(new CardListFilter(){
        	public boolean addCard(Card c)
        	{
        		return !c.getCreatureBlockedThisCombat();
        	}
        });
        
        CardList attList = new CardList();
        attList.addAll(AllZone.Combat.getAttackers());
        
        CardList pwAttList = new CardList();
        pwAttList.addAll(AllZone.pwCombat.getAttackers());

        CombatUtil.checkDeclareBlockers(list);
        
        for (Card a:attList){
        	CardList blockList = AllZone.Combat.getBlockers(a);
        	for (Card b:blockList)
        		CombatUtil.checkBlockedAttackers(a, b);
        }
        
        for (Card a:pwAttList){
        	CardList blockList = AllZone.pwCombat.getBlockers(a);
        	for (Card b:blockList)
        		CombatUtil.checkBlockedAttackers(a, b);
        }
    	
    	
        //AllZone.Combat.setAssignedFirstStrikeDamage();
        //AllZone.pwCombat.setAssignedFirstStrikeDamage();
        

        //AllZone.Phase.nextPhase();
        //for debugging: System.out.println("need to nextPhase(Input_Block_Instant.selectButtonOK) = true");
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    @Override
    public void selectCard(Card card, PlayerZone zone) {
        InputUtil.playInstantAbility(card, zone);
    }//selectCard()
}
