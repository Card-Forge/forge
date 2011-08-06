
package forge;


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
                    AllZone.Stack.add(championAbilityComes);
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
            
            AllZone.Stack.add(ability);
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
    	Card perm = getSourceCard();
    	Player turn = AllZone.Phase.getPlayerTurn();
    	
    	if(perm.getName().equals("Serra Avenger")) {
        	if (turn.equals(perm.getController()) && turn.getTurn() <= 3)
        		return false;
    	}
    	
        return super.canPlay()
                || (getSourceCard().getKeyword().contains("Flash") && !AllZone.GameAction.isCardInPlay(getSourceCard())
                    && !getSourceCard().isUnCastable());
    }
    
    @Override
    public boolean canPlayAI() {
    	
    	Card card = getSourceCard();
    	
        //check on legendary
        if(card.getType().contains("Legendary")) {
        	CardList list = new CardList(AllZone.Computer_Battlefield.getCards());
            if (list.containsName(card.getName()) /*&&
            	!getSourceCard().getName().equals("Flagstones of Trokair")*/)
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
        
        if(card.getType().contains("Creature") && card.getNetDefense() <= 0) {
        	 return false;
        }
        
        if(willChampion) {
        	Object o = championGetCreature.execute();
            if(o == null) return false;
            
            CardList cl = (CardList) championGetCreature.execute();
            if( (o == null) || !(cl.size() > 0) || !AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand))
            	return false;
        }

        if(!(AllZone.ComputerPlayer.getLife() > (loseLifeAmount+3))) {
    		return false;
    	}
        
        return super.canPlayAI();
    }//canPlayAI()
    
    @Override
    public void resolve() {
        Card c = getSourceCard();
        PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, c.getController());
        play.add(c);
    }
}
