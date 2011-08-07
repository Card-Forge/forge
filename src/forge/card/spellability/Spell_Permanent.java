
package forge.card.spellability;

import java.util.ArrayList;
import java.util.HashMap;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.Command;
import forge.CommandReturn;
import forge.Constant;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactory;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.trigger.Trigger;
import forge.gui.input.Input;


public class Spell_Permanent extends Spell {
    private static final long serialVersionUID = 2413495058630644447L;
    
    private boolean willChampion = false;
    private String championType = null;
    
    /////////////////////
    ///////
    private final CommandReturn championGetCreature = new CommandReturn() {
        public Object execute() {
            return AllZoneUtil.getPlayerTypeInPlay(getSourceCard().getController(), championType);
        }
    };//CommandReturn
    
    final SpellAbility championAbilityComes = new Ability(getSourceCard(), "0") {
        @Override
        public void resolve() {
            if(getTargetCard() == null || getTargetCard() == getSourceCard()) AllZone.GameAction.sacrifice(getSourceCard());
            
            else if(AllZoneUtil.isCardInPlay(getTargetCard())) {
                AllZone.GameAction.exile(getTargetCard());
            }
        }//resolve()
    };
    
    final Input championInputComes = new Input() {
		private static final long serialVersionUID = -7503268232821397107L;

		@Override
        public void showMessage() {
            CardList choice = (CardList) championGetCreature.execute();
            
            stopSetNext(CardFactoryUtil.input_targetChampionSac(getSourceCard(), championAbilityComes, choice,
                    "Select another "+championType+" you control to exile", false, false));
            ButtonUtil.disableAll(); //target this card means: sacrifice this card
        }
    };
    Command championCommandComes = new Command() {
        
        private static final long serialVersionUID = -3580408066322945328L;
        
        public void execute() {
            CardList creature = (CardList) championGetCreature.execute();
            Player s = getSourceCard().getController();
            if(creature.size() == 0) {
                AllZone.GameAction.sacrifice(getSourceCard());
                return;
            } else if(s.isHuman()) {
            	AllZone.InputControl.setInput(championInputComes);
            }
            else { //Computer
                Card target;
                CardList computer = AllZoneUtil.getPlayerTypeInPlay(AllZone.ComputerPlayer, championType);
                computer.remove(getSourceCard());
                
                computer.shuffle();
                if(computer.size() != 0) {
                    target = computer.get(0);
                    championAbilityComes.setTargetCard(target);
                    AllZone.Stack.addSimultaneousStackEntry(championAbilityComes);
                    if(getSourceCard().getName().equals("Mistbind Clique")) {
                    	//TODO this needs to target
                    	CardList list = AllZoneUtil.getPlayerLandsInPlay(AllZone.HumanPlayer);
                    	for(Card c:list) c.tap();
                    }
                }
                else
                	AllZone.GameAction.sacrifice(getSourceCard());
            }//computer
        }//execute()
    };//championCommandComes
    
    Command championCommandLeavesPlay = new Command() {
        
        private static final long serialVersionUID = -5903638227914705191L;
        
        public void execute() {
            //System.out.println(abilityComes.getTargetCard().getName());
            Object o = championAbilityComes.getTargetCard();
            
            if(o == null || ((Card) o).isToken() || !AllZoneUtil.isCardExiled((Card) o)) return;
            
            SpellAbility ability = new Ability(getSourceCard(), "0") {
                @Override
                public void resolve() {
                    Card c = championAbilityComes.getTargetCard();
                    if(!c.isToken()) {
                    	AllZone.GameAction.moveToPlay(c);
                        
                    }
                }//resolve()
            };//SpellAbility
            
            StringBuilder sb = new StringBuilder();
            sb.append(getSourceCard().getName()).append(" - returning card to battlefield.");
            ability.setStackDescription(sb.toString());
            
            AllZone.Stack.addSimultaneousStackEntry(ability);
        }//execute()
    };//championCommandLeavesPlay
    
    ///////
    ////////////////////
    
    public Spell_Permanent(Card sourceCard) {
    	// Add Costs for all SpellPermanents
    	this(sourceCard, new Cost(sourceCard.getManaCost(), sourceCard.getName(), false), null);
    }//Spell_Permanent()
    
    public Spell_Permanent(Card sourceCard, Cost cost, Target tgt) {
        super(sourceCard, cost, tgt);
        
        if(CardFactory.hasKeyword(sourceCard,"Champion") != -1) {
        	int n = CardFactory.hasKeyword(sourceCard, "Champion");
            
            String parse = sourceCard.getKeyword().get(n).toString();
        	willChampion = true;
        	championType = parse.split(":")[1];
        }
        
        if(sourceCard.isCreature()) {
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(sourceCard.getName()).append(" - Creature ").append(sourceCard.getNetAttack());
        	sb.append(" / ").append(sourceCard.getNetDefense());
        	setStackDescription(sb.toString());
        }
        else setStackDescription(sourceCard.getName());
        
        setDescription(getStackDescription());
        if(willChampion) {
        	sourceCard.addComesIntoPlayCommand(championCommandComes);
        	sourceCard.addLeavesPlayCommand(championCommandLeavesPlay);
        }
        
    }//Spell_Permanent()
    
    
    @Override
    public boolean canPlay() {
    	Card source = getSourceCard();
    	if(AllZone.Stack.isSplitSecondOnStack() || source.isUnCastable()) return false;
    	
    	Player turn = AllZone.Phase.getPlayerTurn();

    	if(source.getName().equals("Serra Avenger")) {
        	if (turn.equals(source.getController()) && turn.getTurn() <= 3)
        		return false;
    	}
    	else if(source.getName().equals("Blizzard")) {
    		CardList lands = AllZoneUtil.getPlayerLandsInPlay(source.getController());
    		lands = lands.getType("Snow");
    		if(lands.size() == 0) return false;
    	}
    
    	// Flash handled by super.canPlay
        return super.canPlay();
    }
    
    @Override
    public boolean canPlayAI() {
    	
    	Card card = getSourceCard();
    	
        //check on legendary
        if (card.isType("Legendary")) {
        	CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
            if (list.containsName(card.getName()))
            	return false;
        }
        if (card.isPlaneswalker()) {
        	CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
        	list = list.getType("Planeswalker");
        	
        	for (int i=0;i<list.size();i++)
        	{
        		String subtype = card.getType().get(card.getType().size() - 1);
        		CardList cl = list.getType(subtype);
        		
        		 if (cl.size() > 0) {
                     return false;
                 }
        	}
        }
        if (card.isType("World")) {
        	CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
        	list = list.getType("World");
        	if(list.size() > 0) return false;
        }
        
        if (card.isCreature() 
        		&& card.getNetDefense() <= 0 
        		&& !card.hasStartOfKeyword("etbCounter") 
        		&& !card.getText().contains("Modular"))
        	return false;
        
        if (willChampion) {
        	Object o = championGetCreature.execute();
            if (o == null) return false;
            
            CardList cl = (CardList) championGetCreature.execute();
            if ( (o == null) || !(cl.size() > 0) || !AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand))
            	return false;
        }
        
        if (!checkETBEffects(card, this, null))
        	return false;
        
        return super.canPlayAI();
    }//canPlayAI()
    
    public static boolean checkETBEffects(Card card, SpellAbility sa, String api){
        // Trigger play improvements
        ArrayList<Trigger> triggers = card.getTriggers();
        for(Trigger tr : triggers){
        	// These triggers all care for ETB effects
        	
        	HashMap<String, String> params = tr.getMapParams();
        	if (!params.get("Mode").equals("ChangesZone"))
        		continue;
        	
        	if (!params.get("Destination").equals("Battlefield"))
        		continue;
        	
        	if (params.containsKey("ValidCard") && !params.get("ValidCard").contains("Self"))
        		continue;
        	
        	if(!tr.requirementsCheck())
        		continue;
        	
        	if (tr.getOverridingAbility() != null)	// Don't look at Overriding Abilities yet
        		continue;
        	
        	// Maybe better considerations 
        	AbilityFactory af = new AbilityFactory();
        	SpellAbility exSA = af.getAbility(card.getSVar(params.get("Execute")), card);
        	
        	if (api != null && !af.getAPI().equals(api))
        		continue;
        	
        	exSA.setActivatingPlayer(sa.getActivatingPlayer());

        	// Run non-mandatory trigger.
        	// These checks only work if the Executing SpellAbility is an Ability_Sub.
        	if (exSA instanceof Ability_Sub && !exSA.doTrigger(false)){
        		// AI would not run this trigger if given the chance
        		
        		// if trigger is mandatory, return false
        		if (params.get("OptionalDecider") == null){
        			return false;
        		}
        		// else
        		// otherwise, return false 50% of the time?
        	}
        }    	
    	
    	return true;
    }
    
    
    @Override
    public void resolve() {
        Card c = getSourceCard();
        AllZone.GameAction.moveToPlay(c);
    }
}
