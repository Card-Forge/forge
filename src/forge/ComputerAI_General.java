package forge;
import static forge.error.ErrorViewer.*;

import java.util.ArrayList;
// import java.util.Collection;
// import java.util.TreeSet;

import com.esotericsoftware.minlog.Log;


public class ComputerAI_General implements Computer {

    public ComputerAI_General() {

    }
    
    public void main1() {
    	ComputerUtil.chooseLandsToPlay();
        playCards(Constant.Phase.Main1);
    }//main1()
    
    public void main2() {
    	ComputerUtil.chooseLandsToPlay();	// in case we can play more lands now, or drew cards since first main phase
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
        CardList hand = new CardList(AllZone.Computer_Hand.getCards());
        
        hand = hand.filter(new CardListFilter() {
        	// Beached As Start
            public boolean addCard(Card c) {
                //Collection<Card> play = playMain1Cards;
            	if (c.getSVar("PlayMain1").equals("TRUE"))
            		return true;
            	
                if(c.isCreature() && (c.getKeyword().contains("Haste")) || c.getKeyword().contains("Exalted")) return true;

                CardList buffed = new CardList(AllZone.Computer_Play.getCards()); //get all cards the computer controls with BuffedBy
                for(int j = 0; j < buffed.size(); j++) {
                    Card buffedcard = buffed.get(j);
                    if (buffedcard.getSVar("BuffedBy").length() > 0) {
                            String buffedby = buffedcard.getSVar("BuffedBy");
                            String bffdby[] = buffedby.split(",");
                            if (c.isValidCard(bffdby,c.getController(),c)) return true;
                    }       
                }//BuffedBy

                CardList antibuffed = new CardList(AllZone.Human_Play.getCards()); //get all cards the human controls with AntiBuffedBy
                for(int k = 0; k < antibuffed.size(); k++) {
                    Card buffedcard = antibuffed.get(k);
                    if (buffedcard.getSVar("AntiBuffedBy").length() > 0) {
                            String buffedby = buffedcard.getSVar("AntiBuffedBy");
                            String bffdby[] = buffedby.split(",");
                            if (c.isValidCard(bffdby,c.getController(),c)) return true;
                    }       
                }//AntiBuffedBy

                if(c.isLand()) return false;

                CardList Vengevines = new CardList();
                Vengevines.addAll(AllZone.getZone(Constant.Zone.Graveyard, AllZone.ComputerPlayer).getCards());       
                Vengevines = Vengevines.getName("Vengevine");
                if(Vengevines.size() > 0) {
                CardList Creatures = new CardList();  
                CardList Creatures2 = new CardList();       
                Creatures.addAll(AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer).getCards());
        		for(int i = 0; i < Creatures.size(); i++) {
        			if(Creatures.get(i).getType().contains("Creature") && CardUtil.getConvertedManaCost(Creatures.get(i).getManaCost()) <= 3) {
        				Creatures2.add(Creatures.get(i));
        			}
        		}
                if(Creatures2.size() + Phase.ComputerCreatureSpellCount > 1 && c.getType().contains("Creature") && CardUtil.getConvertedManaCost(c.getManaCost()) <= 3) return true;	
                } // AI Improvement for Vengevine
            	// Beached As End
                	return false;
            }
        });
        CardList all = new CardList();
        all.addAll(hand.toArray());
        all.addAll(AllZone.Computer_Play.getCards());
        
        CardList humanPlayable = new CardList();
        humanPlayable.addAll(AllZone.Human_Play.getCards());
        humanPlayable = humanPlayable.filter(new CardListFilter()
        {
          public boolean addCard(Card c)
          {
            return (c.canAnyPlayerActivate());
          }
        });
        
        all.addAll(humanPlayable.toArray());
        
        return getPlayable(all);
    }//getMain1()
    

    private SpellAbility[] getMain2() {
        //Card list of all cards to consider
        CardList all = new CardList();
        all.addAll(AllZone.Computer_Hand.getCards());
        all.addAll(AllZone.Computer_Play.getCards());
        all.addAll(CardFactoryUtil.getFlashbackCards(AllZone.ComputerPlayer).toArray());
        
        // Prevent the computer from summoning Ball Lightning type creatures during main phase 2
        all = all.getNotKeyword("At the beginning of the end step, sacrifice CARDNAME.");
        
        all = all.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                if(c.isLand()) return false;
                return true;
            }
        });
        
        CardList humanPlayable = new CardList();
        humanPlayable.addAll(AllZone.Human_Play.getCards());
        humanPlayable = humanPlayable.filter(new CardListFilter()
        {
          public boolean addCard(Card c)
          {
            return (c.canAnyPlayerActivate());
          }
        });
        all.addAll(humanPlayable.toArray());
        
        return getPlayable(all);
    }//getMain2()
    
    private SpellAbility[] getOtherPhases(){
        CardList all = new CardList();
        all.addAll(AllZone.Computer_Hand.getCards());
        all.addAll(AllZone.Computer_Play.getCards());
        all.addAll(CardFactoryUtil.getFlashbackCards(AllZone.ComputerPlayer).toArray());

        
        CardList humanPlayable = new CardList();
        humanPlayable.addAll(AllZone.Human_Play.getCards());
        humanPlayable = humanPlayable.filter(new CardListFilter()
        {
          public boolean addCard(Card c)
          {
            return (c.canAnyPlayerActivate());
          }
        });
        all.addAll(humanPlayable.toArray());
        
        return getPlayable(all);
    }
    
    /**
     * Returns the spellAbilities from the card list that the computer is able to play
     */
    private SpellAbility[] getPlayable(CardList l) {
        ArrayList<SpellAbility> spellAbility = new ArrayList<SpellAbility>();
        for(Card c:l)
            for(SpellAbility sa:c.getSpellAbility())
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
    
	public void begin_combat() {
		stackResponse();
	}
    
    public void declare_attackers() {
        final Combat c = ComputerUtil.getAttackers();
        c.setAttackingPlayer(AllZone.Combat.getAttackingPlayer());
        c.setDefendingPlayer(AllZone.Combat.getDefendingPlayer());
        
        //check for planeswalker
        Card walker = AllZone.HumanPlayer.getPlaneswalker();
        
        if(walker != null && MyRandom.random.nextBoolean()) {
            c.setPlaneswalker(walker);
            AllZone.pwCombat = c;
        } else AllZone.Combat = c;
        

        Card[] att = c.getAttackers();
        if (att.length > 0)
            AllZone.Phase.setCombat(true);
        
        for(int i = 0; i < att.length; i++) {
            if(!att[i].getKeyword().contains("Vigilance")) att[i].tap();
            Log.debug("Computer just assigned " + att[i].getName() + " as an attacker.");
        }
        
        AllZone.Computer_Play.updateObservers();
        CombatUtil.showCombat();
        
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    public void declare_attackers_after()
    {
    	stackResponse();
    }
    
    public void declare_blockers() {
        CardList blockers = AllZoneUtil.getCreaturesInPlay(AllZone.ComputerPlayer);
        
        //If Player life is in danger protect it first
        if(CombatUtil.lifeInDanger(AllZone.Combat)) {
        	AllZone.Combat = ComputerUtil_Block2.getBlockers(AllZone.Combat, blockers);
            CardList remove = AllZone.Combat.getAllBlockers();
            for(int i = 0; i < remove.size(); i++)
                blockers.remove(remove.get(i));
            
            AllZone.pwCombat = ComputerUtil_Block2.getBlockers(AllZone.pwCombat, blockers);
        } else { // Otherwise protect Planeswalkers first
	        AllZone.pwCombat = ComputerUtil_Block2.getBlockers(AllZone.pwCombat, blockers);
	        
	        CardList remove = AllZone.pwCombat.getAllBlockers();
	        for(int i = 0; i < remove.size(); i++)
	            blockers.remove(remove.get(i));
	        
	        AllZone.Combat = ComputerUtil_Block2.getBlockers(AllZone.Combat, blockers);
        }
        
        CombatUtil.showCombat();
        
        AllZone.Phase.setNeedToNextPhase(true);
    }
    
    public void declare_blockers_after() {
    	stackResponse();
    }
    
    /*
    private Combat getCombat(Card[] attackers, CardList availableBlockers) {
    	
    	
        ComputerUtil_Block2 com = new ComputerUtil_Block2(attackers, availableBlockers.toArray(),
                AllZone.ComputerPlayer.getLife());
        
        Combat c = com.getBlockers();
        c.setAttackingPlayer(AllZone.Combat.getAttackingPlayer());
        c.setDefendingPlayer(AllZone.Combat.getDefendingPlayer());
        
    	
    	CardList attacks = new CardList(attackers);
    	
    	Combat c = ComputerUtil_Block2.getBlockers(attacks,availableBlockers);
        
        return c;
    }
    */
    
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
    	SpellAbility[] sas;
    	if (AllZone.Stack.size() == 0){
    		sas = getOtherPhases();
    		
    		if (sas.length > 0){
    		// do things dependent on the phase, 
    		
    		// if beginCombat tap best attackers etc
    		
    		// if declareBlockers pump my guys to kill his guys or save my guys or regen my guys
    		
    		// if end of Human's turn, feel free to use tap/mana abilities that will untap next turn
    		
    		// if you don't or can't play anything
    		}
    		
    		AllZone.Phase.passPriority();	
    		return;
    	}
    	
    	SpellAbility topSA = AllZone.Stack.peek();
    	// if top of stack is owned by me
    	if (topSA.getActivatingPlayer().isComputer()){
    		// probably should let my stuff resolve to force Human to respond to it
    		AllZone.Phase.passPriority();
    		return;
    	}
    	
    	// top of stack is owned by human, 
    	sas = getOtherPhases();
    	
    	if (sas.length > 0){
    		if (topSA.getTarget() != null){
    			ArrayList<Object> targets = topSA.getTarget().getTargets();
		    	// does it target me or something I own?
		    	// can i protect it? can I counter it?
		    	
		    	// if i can't save it, can I activate an ability on that card in response? sacrifice etc?
		    	
		    	// does it target his stuff? can I kill it in response?
	    	}
    		else{
    			// no target, figure out what type of spell it is and react according
    		}
    	}
    	// if this hasn't been covered above, just PassPriority()
    	AllZone.Phase.passPriority();
    }
}
