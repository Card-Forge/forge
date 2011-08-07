package forge;
import static forge.error.ErrorViewer.showError;

import java.util.ArrayList;

import com.esotericsoftware.minlog.Log;

import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Spell_Permanent;


public class ComputerAI_General implements Computer {

    public ComputerAI_General() {

    }
    
    public void main1() {
    	if (!ComputerUtil.chooseLandsToPlay()){
    		if (AllZone.Phase.getPriorityPlayer().isComputer())
    			stackResponse();
    		return;
    	}
        playCards(Constant.Phase.Main1);
    }//main1()
    
    public void main2() {
    	if (!ComputerUtil.chooseLandsToPlay()){	// in case we can play more lands now, or drew cards since first main phase
    		if (AllZone.Phase.getPriorityPlayer().isComputer())
    			stackResponse();
    		return;
    	}
        playCards(Constant.Phase.Main2);
   }
    
    private void playCards(final String phase) {
        SpellAbility[] sp = phase.equals(Constant.Phase.Main1)? getMain1():getMain2();
        
        boolean nextPhase = ComputerUtil.playCards(sp);
        
        if(nextPhase) {
        	AllZone.Phase.passPriority();
        }
    }//playCards()

    private SpellAbility[] getMain1() {
    	//Card list of all cards to consider
    	CardList hand = AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer);
    	
    	if (AllZone.Computer_ManaPool.isEmpty())
	    	hand = hand.filter(new CardListFilter() {
	    		public boolean addCard(Card c) {

	    			if (c.getSVar("PlayMain1").equals("TRUE"))
	    				return true;
	    			
	    			if (c.isSorcery()) //timing should be handled by the AF's
	    				return true;
	
	    			if (c.isCreature() 
	    					&& (c.hasKeyword("Haste")) || c.hasKeyword("Exalted")) return true;
	
	    			CardList buffed = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer); //get all cards the computer controls with BuffedBy
	    			for (int j = 0; j < buffed.size(); j++) {
	    				Card buffedcard = buffed.get(j);
	    				if (buffedcard.getSVar("BuffedBy").length() > 0) {
	    					String buffedby = buffedcard.getSVar("BuffedBy");
	    					String bffdby[] = buffedby.split(",");
	    					if (c.isValidCard(bffdby,c.getController(),c)) return true;
	    				}       
	    			}//BuffedBy
	
	    			CardList antibuffed = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer); //get all cards the human controls with AntiBuffedBy
	    			for(int k = 0; k < antibuffed.size(); k++) {
	    				Card buffedcard = antibuffed.get(k);
	    				if (buffedcard.getSVar("AntiBuffedBy").length() > 0) {
	    					String buffedby = buffedcard.getSVar("AntiBuffedBy");
	    					String bffdby[] = buffedby.split(",");
	    					if (c.isValidCard(bffdby,c.getController(),c)) return true;
	    				}       
	    			}//AntiBuffedBy
	
	    			if(c.isLand()) return false;
	
	    			CardList vengevines = AllZoneUtil.getPlayerGraveyard(AllZone.ComputerPlayer, "Vengevine");
	    			if (vengevines.size() > 0) {
	    				CardList creatures = AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer);
	    				CardList creatures2 = new CardList();
	    				for (int i = 0; i < creatures.size(); i++) {
	    					if (creatures.get(i).isCreature() 
	    							&& CardUtil.getConvertedManaCost(creatures.get(i).getManaCost()) <= 3) {
	    						creatures2.add(creatures.get(i));
	    					}
	    				}
	    				if (creatures2.size() + Phase.ComputerCreatureSpellCount > 1 
	    						&& c.isCreature() 
	    						&& CardUtil.getConvertedManaCost(c.getManaCost()) <= 3) return true;	
	    			} // AI Improvement for Vengevine
	    			// Beached As End
	    			return false;
	    		}
	    	});
    	CardList all = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
    	all.addAll(hand);

    	CardList humanPlayable = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
    	humanPlayable = humanPlayable.filter(new CardListFilter()
    	{
    		public boolean addCard(Card c)
    		{
    			return (c.canAnyPlayerActivate());
    		}
    	});

    	all.addAll(humanPlayable);

    	return getPlayable(all);
    }//getMain1()
    

    private SpellAbility[] getMain2() {
    	//Card list of all cards to consider
    	CardList all = AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer);
    	//Don't play permanents with Flash before humans declare attackers step
    	all = all.filter(new CardListFilter() {
    		public boolean addCard(Card c) {
    			if (c.isPermanent() 
    					&& c.hasKeyword("Flash") 
    					&& (AllZone.Phase.isPlayerTurn(AllZone.ComputerPlayer)
    							|| AllZone.Phase.isBefore(Constant.Phase.Combat_Declare_Attackers_InstantAbility)))
    				return false;
    			return true;
    		}
    	});
    	all.addAll(AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer));
    	all.addAll(CardFactoryUtil.getGraveyardActivationCards(AllZone.ComputerPlayer));

    	// Prevent the computer from summoning Ball Lightning type creatures during main phase 2
    	all = all.getNotKeyword("At the beginning of the end step, sacrifice CARDNAME.");

    	all = all.filter(new CardListFilter() {
    		public boolean addCard(Card c) {
    			if(c.isLand()) return false;
    			return true;
    		}
    	});

    	CardList humanPlayable = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
    	humanPlayable = humanPlayable.filter(new CardListFilter()
    	{
    		public boolean addCard(Card c)
    		{
    			return (c.canAnyPlayerActivate());
    		}
    	});
    	all.addAll(humanPlayable);

    	return getPlayable(all);
    }//getMain2()
    
    private CardList getAvailableSpellAbilities(){
    	CardList all = AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer);
    	//Don't play permanents with Flash before humans declare attackers step
    	all = all.filter(new CardListFilter() {
    		public boolean addCard(Card c) {
    			if (c.isPermanent() 
    					&& c.hasKeyword("Flash") 
    					&& (AllZone.Phase.isPlayerTurn(AllZone.ComputerPlayer)
    							|| AllZone.Phase.isBefore(Constant.Phase.Combat_Declare_Attackers_InstantAbility)))
    				return false;
    			return true;
    		}
    	});
    	all.addAll(AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer));
    	all.addAll(CardFactoryUtil.getGraveyardActivationCards(AllZone.ComputerPlayer));


    	CardList humanPlayable = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
    	humanPlayable = humanPlayable.filter(new CardListFilter()
    	{
    		public boolean addCard(Card c)
    		{
    			return (c.canAnyPlayerActivate());
    		}
    	});
    	all.addAll(humanPlayable);
    	return all;
    }
    
    private SpellAbility[] getOtherPhases(){
        return getPlayable(getAvailableSpellAbilities());
    }
    
    private ArrayList<SpellAbility> getPossibleCounters(){
        return getPlayableCounters(getAvailableSpellAbilities());
    }
    
    private ArrayList<SpellAbility> getPossibleETBCounters(){
        return getETBCounters(getAvailableSpellAbilities());
    }
    
    /**
     * Returns the spellAbilities from the card list that the computer is able to play
     */
    private SpellAbility[] getPlayable(CardList l) {
        ArrayList<SpellAbility> spellAbility = new ArrayList<SpellAbility>();
        for(Card c:l)
            for(SpellAbility sa:c.getSpellAbility())
            	// if SA is from AF_Counter don't add to getPlayable
                //This try/catch should fix the "computer is thinking" bug
                try {
                	sa.setActivatingPlayer(AllZone.ComputerPlayer);
                    if(sa.canPlay() && ComputerUtil.canPayCost(sa) && sa.canPlayAI()){
                    	spellAbility.add(sa);
                    }
                } catch(Exception ex) {
                    showError(ex, "There is an error in the card code for %s:%n", c.getName(), ex.getMessage());
                }
        return spellAbility.toArray(new SpellAbility[spellAbility.size()]);
    }
    
    private ArrayList<SpellAbility> getPlayableCounters(CardList l) {
        ArrayList<SpellAbility> spellAbility = new ArrayList<SpellAbility>();
        for(Card c:l){
            for(SpellAbility sa:c.getSpellAbility()){
            	// Check if this AF is a Counterpsell
            	if (sa.getAbilityFactory() != null && sa.getAbilityFactory().getAPI().equals("Counter"))
            		spellAbility.add(sa);
            }
        }
        
        return spellAbility;
    }
    
    private ArrayList<SpellAbility> getETBCounters(CardList l) {
        ArrayList<SpellAbility> spellAbility = new ArrayList<SpellAbility>();
        for(Card c:l){
            for(SpellAbility sa:c.getSpellAbility()){
            	// Or if this Permanent has an ETB ability with Counter
            	if (sa instanceof Spell_Permanent){
            		if (Spell_Permanent.checkETBEffects(c, sa, "Counter"))
            			spellAbility.add(sa);
            	}
            }
        }
        
        return spellAbility;
    }
    
	public void begin_combat() {
		stackResponse();
	}
    
    public void declare_attackers() {
    	// 12/2/10(sol) the decision making here has moved to getAttackers()
    	
    	AllZone.Combat = ComputerUtil.getAttackers();

        Card[] att = AllZone.Combat.getAttackers();
        if (att.length > 0)
            AllZone.Phase.setCombat(true);
        
        for(int i = 0; i < att.length; i++) {
        	// tapping of attackers happens after Propaganda is paid for
            //if (!att[i].hasKeyword("Vigilance")) att[i].tap();
            Log.debug("Computer just assigned " + att[i].getName() + " as an attacker.");
        }
        
        AllZone.Computer_Battlefield.updateObservers();
        CombatUtil.showCombat();
        
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    public void declare_attackers_after()
    {
    	stackResponse();
    }
    
    public void declare_blockers() {
        CardList blockers = AllZoneUtil.getCreaturesInPlay(AllZone.ComputerPlayer);

        AllZone.Combat = ComputerUtil_Block2.getBlockers(AllZone.Combat, blockers);
        
        CombatUtil.showCombat();
        
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    public void declare_blockers_after() {
    	stackResponse();
    }

    public void end_of_combat(){
    	stackResponse();
    }
    
    //end of Human's turn
    public void end_of_turn() {
    	stackResponse();
    }
    
    public void stack_not_empty() {
    	stackResponse();
    }
    
    public void stackResponse(){
    	// if top of stack is empty 
    	SpellAbility[] sas = null;
    	if (AllZone.Stack.size() == 0){
    		sas = getOtherPhases();
    		
    		boolean pass = (sas.length == 0) || AllZone.Phase.is(Constant.Phase.Upkeep, AllZone.ComputerPlayer) ||
    			AllZone.Phase.is(Constant.Phase.Draw, AllZone.ComputerPlayer) || 
    			AllZone.Phase.is(Constant.Phase.End_Of_Turn, AllZone.ComputerPlayer);
    		if (!pass){		// Each AF should check the phase individually
    	        pass = ComputerUtil.playCards(sas);
    		}
    			
    		if (pass)
    			AllZone.Phase.passPriority();
    		return;
    	}
    	
    	// if top of stack is owned by me
    	if (AllZone.Stack.peekInstance().getActivatingPlayer().isComputer()){
    		// probably should let my stuff resolve to force Human to respond to it
    		AllZone.Phase.passPriority();
    		return;
    	}
    	
    	// top of stack is owned by human,
    	ArrayList<SpellAbility> possibleCounters = getPossibleCounters();
    	
    	if (possibleCounters.size() > 0 && ComputerUtil.playCounterSpell(possibleCounters)){
    		// Responding CounterSpell is on the Stack trying to Counter the Spell
    		// If playCounterSpell returns true, a Spell is hitting the Stack
    		return;
    	}
    	
    	possibleCounters.clear();
    	possibleCounters = getPossibleETBCounters();
    	if (possibleCounters.size() > 0 && !ComputerUtil.playCards(possibleCounters)){
    		// Responding Permanent w/ ETB Counter is on the Stack
    		// AllZone.Phase.passPriority();
    		return;
    	}
    	
    	sas = getOtherPhases();
    	if (sas.length > 0){
			// Spell not Countered 
    		// each AF should check the Stack/Phase on it's own
			
			//ArrayList<Object> targets = topSA.getTarget().getTargets();
	    	// does it target me or something I own?
	    	// can i protect it? can I counter it?
	    	
	    	// if i can't save it, can I activate an ability on that card in response? sacrifice etc?
	    	
	    	// does it target his stuff? can I kill it in response?

    	}
    	// if this hasn't been covered above, just PassPriority()
    	AllZone.Phase.passPriority();
    }
}
