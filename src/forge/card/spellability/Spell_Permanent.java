
package forge.card.spellability;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.Command;
import forge.CommandReturn;
import forge.Constant;
import forge.Player;
import forge.card.cardFactory.CardFactory;
import forge.card.cardFactory.CardFactoryUtil;
import forge.gui.input.Input;


public class Spell_Permanent extends Spell {
    private static final long serialVersionUID = 2413495058630644447L;
    
    private boolean willChampion = false;
    private String championType = null;
    private int loseLifeAmount;
    
    public void setLoseLifeAmount(int a) {
    	loseLifeAmount = a;
    }
    
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
            
            else if(AllZone.GameAction.isCardInPlay(getTargetCard())) {
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
            } else if(s.equals(AllZone.HumanPlayer)) {
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
            
            if(o == null || ((Card) o).isToken() || !AllZone.GameAction.isCardExiled((Card) o)) return;
            
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
        super(sourceCard);
        
        setManaCost(sourceCard.getManaCost());
        
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
        if(card.getType().contains("Legendary")) {
        	CardList list = new CardList(AllZone.Computer_Battlefield.getCards());
            if (list.containsName(card.getName()))
            	return false;
        }
        if(card.getType().contains("Planeswalker")) {
        	CardList list = new CardList(AllZone.Computer_Battlefield.getCards());
        	list = list.getType("Planeswalker");
        	
        	for (int i=0;i<list.size();i++)
        	{
        		String subtype = card.getType().get(card.getType().size() - 1);
        		CardList cl = list.getType(subtype);
        		
        		 if(cl.size() > 0) {
                     return false;
                 }
        	}
        }
        if(card.getType().contains("World")) {
        	CardList list = new CardList(AllZone.Computer_Battlefield.getCards());
        	list = list.getType("World");
        	if(list.size() > 0) return false;
        }
        
        if(card.getType().contains("Creature") && card.getNetDefense() <= 0 && 
        		!card.hasStartOfKeyword("etbCounter") && !card.getText().contains("Modular"))
        	return false;
        
        if(willChampion) {
        	Object o = championGetCreature.execute();
            if(o == null) return false;
            
            CardList cl = (CardList) championGetCreature.execute();
            if( (o == null) || !(cl.size() > 0) || !AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand))
            	return false;
        }

        if(loseLifeAmount > 0 && AllZone.ComputerPlayer.getLife() <= (loseLifeAmount+3)) {
    		return false;
    	}
        
        return super.canPlayAI();
    }//canPlayAI()
    
    @Override
    public void resolve() {
        Card c = getSourceCard();
        AllZone.GameAction.moveToPlay(c);
    }
}
