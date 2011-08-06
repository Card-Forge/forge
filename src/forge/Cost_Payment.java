package forge;

public class Cost_Payment {
	Ability_Cost cost = null;
	SpellAbility ability = null;
	Card card = null;

	public Ability_Cost getCost() { return cost; }
	public SpellAbility getAbility() { return ability; }
	public void setCancel(boolean cancel) { bCancel = cancel; } 
	public void setDoneTarget(boolean done) { bCancel = done; } 
	
	private boolean payTap = false;
	private boolean payMana = false;
	private boolean paySubCounter = false;
	private boolean paySac = false;
	private boolean bCancel = false;
	private boolean bCasting = false;
	private boolean bDoneTarget = false;
	private PlayerZone fromZone = null;

	public void setPayMana(boolean bPay){	payMana = bPay;	}
	public void setPaySac(boolean bSac){	paySac = bSac;	}
	
	final private Input changeInput = new Input() {
		private static final long serialVersionUID = -5750122411788688459L; };
	
	public Cost_Payment(Ability_Cost cost, SpellAbility abil){
		this.cost = cost;
		this.ability = abil;
		card = this.ability.getSourceCard();
		payTap = !cost.getTap();
		payMana = cost.hasNoManaCost();
		paySubCounter = !cost.getSubCounter();
		paySac = !cost.getSacCost();
	}
	
    public boolean canPayAdditionalCosts(){
    	if (cost.getTap() && (card.isTapped() || card.isSick()))
    		return false;
    	
    	int countersLeft = 0;
    	if (cost.getSubCounter()){
			Counters c = cost.getCounterType();
			countersLeft = card.getCounters(c) - cost.getCounterNum();
			if (countersLeft < 0){
	    		return false;
			}
    	}
    	
		if (cost.getSacCost()){
			if (!cost.getSacThis()){
			    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
			    CardList typeList = new CardList(play.getCards());
			    typeList = typeList.getType(cost.getSacType());
				if (typeList.size() == 0)
					return false;
			}
			else if (cost.getSacThis() && !AllZone.GameAction.isCardInPlay(card))
				return false;
		}  
    	
    	return true;
    }
	
	public boolean payCost(){
		if (bCancel || bDoneTarget && cost.getNumTargeted() < cost.getMinTargets()){
			cancelPayment();
			return false;
		}
		
		if (ability instanceof Spell && !bCasting){
			// remove from hand, todo(sol) be careful of spell copies when spells start using this
			bCasting = true;
			Card c = ability.getSourceCard();
			fromZone = AllZone.getZone(c);
			fromZone.remove(c);
		}
		
		// targetting, with forward code for multiple target abilities 
		if (!bDoneTarget && cost.getMinTargets() > 0 && cost.getNumTargeted() < cost.getMaxTargets()){
			if (cost.canTgtCreature() && cost.canTgtPlayer())
				changeInput.stopSetNext(targetCreaturePlayer(ability, Command.Blank, true, this));
			else if(cost.canTgtCreature()) 
				changeInput.stopSetNext(targetCreature(ability, this));
	        else if(cost.canTgtPlayer()) 
	        	changeInput.stopSetNext(targetPlayer(ability, this));
	        return false;
		}
		
		if (!payTap && cost.getTap()){
			if (card.isUntapped()){
				card.tap();
				payTap = true;
			}
			else
				return false;
		}

		// insert untap here
		if (!payMana && !cost.hasNoManaCost()){
			// pay mana here
			changeInput.stopSetNext(new Input_PayCostMana(this));
			return false;
		}
		if (!paySubCounter && cost.getSubCounter()){
			// subtract counters here. 
			Counters c = cost.getCounterType();
			int countersLeft = card.getCounters(c) - cost.getCounterNum();
			if (countersLeft >= 0){
				card.setCounter(c, countersLeft);
				paySubCounter = true;
			}
			else{
				cancelPayment();
				return false;
			}
		}
		if (!paySac && cost.getSacCost())
    	{
    		// sacrifice stuff here
    		if (cost.getSacThis())
    			changeInput.stopSetNext(sacrificeThis(ability, this));
    		else
    			changeInput.stopSetNext(sacrificeType(ability, cost.getSacType(), cost.sacString(true), this));
    		return false;
    	}

		if (isAllPaid())
			allPaid();
		return true;
	}

	public boolean isAllPaid(){
		return (payTap && payMana && paySubCounter && paySac);
	}
	
	public void allPaid(){
		AllZone.ManaPool.clearPay(false);
		AllZone.Stack.add(ability);
		cost.resetTargets();
	}
	
	public void cancelPayment(){
		// unpay anything we can.
		cost.resetTargets();
		if (bCasting){
			// add back to hand
			fromZone.add(ability.getSourceCard());
		}
		if (cost.getTap() && payTap){
			// untap if tapped
			card.untap();
		}
		// refund mana
        AllZone.ManaPool.unpaid();
        
        // refund counters
        if (cost.getSubCounter() && paySubCounter){
			Counters c = cost.getCounterType();
			int countersLeft = card.getCounters(c) + cost.getCounterNum();
			card.setCounter(c, countersLeft);
        }
        
		// can't really unsacrifice things
	}
	
    public static Input sacrificeThis(final SpellAbility spell, final Cost_Payment payment) {
        Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;
            
            @Override
            public void showMessage() {
            	Card card = spell.getSourceCard();
                String[] choices = {"Yes", "No"};
                if(card.getController().equals(Constant.Player.Human)) {
                    Object o = AllZone.Display.getChoice("Sacrifice " + card.getName() + " ?", choices);
                    if(o.equals("Yes")) {
                    	AllZone.GameAction.sacrifice(card);
                    	payment.setPaySac(true);
                    	stop();
                    	payment.payCost();
                    }
                    else{
                    	payment.setCancel(true);
                    	stop();
                    	payment.payCost();
                    }
                }
            }
        };
        return target;
    }//input_sacrifice()
    
    public static Input sacrificeType(final SpellAbility spell, final String type, final String message, final Cost_Payment payment){
    // This input should be setAfterManaPaid so it can add the spell to the stack
        Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;
            private CardList typeList;
            
            @Override
            public void showMessage() {
                PlayerZone play = AllZone.getZone(Constant.Zone.Play, spell.getSourceCard().getController());
                typeList = new CardList(play.getCards());
                typeList = typeList.getType(type);
                AllZone.Display.showMessage(message);
                ButtonUtil.enableOnlyCancel();
            }
            
            @Override
            public void selectButtonCancel() {
            	payment.setCancel(true);
            	stop();
            	payment.payCost();
            }
            
            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if(typeList.contains(card)) {
                	AllZone.GameAction.sacrifice(card);
                	payment.setPaySac(true);
                	stop();
                	payment.payCost();
                }
            }
        };
        return target;
    }//sacrificeType()
    
    public static Input targetCreaturePlayer(final SpellAbility ability, final Command paid, final boolean targeted, final Cost_Payment payment) {
        Input target = new Input() {
            private static final long serialVersionUID = 2781418414287281005L;
            
            @Override
            public void showMessage() {
                AllZone.Display.showMessage("Select target Creature, Player, or Planeswalker");
                // when multi targets (Arc Mage) are added, need this: 
                // if payment.targeted < mintarget only enable cancel
                // else if payment.targeted < maxtarget enable cancel and ok
                ButtonUtil.enableOnlyCancel();
            }
            
            @Override
            public void selectButtonCancel() {
            	payment.setCancel(true);
                stop();
                payment.payCost();
            }
            
            @Override     
            public void selectButtonOK() {
            	payment.setDoneTarget(true);
                stop();
                payment.payCost();
            }
            
            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if((card.isCreature() || card.isPlaneswalker()) && zone.is(Constant.Zone.Play)
                        && (!targeted || CardFactoryUtil.canTarget(ability, card))) {
                    ability.setTargetCard(card);
                    done();
                }
            }//selectCard()
            
            @Override
            public void selectPlayer(String player) {
                ability.setTargetPlayer(player);
                done();
            }
            
            void done() {
            	payment.getCost().incrementTargets();
                paid.execute();
                stop();
                payment.payCost();
            }
        };
        return target;
    }//input_targetCreaturePlayer()
    
	public static Input targetCreature(final SpellAbility ability, final Cost_Payment payment) {
        Input target = new Input() {
            private static final long serialVersionUID = 2781418414287281005L;
            
            @Override
            public void showMessage() {
                AllZone.Display.showMessage("Select target Creature");
                ButtonUtil.enableOnlyCancel();
            }
            
            @Override
            public void selectButtonCancel() {
            	payment.setCancel(true);
                stop();
                payment.payCost();
            }
            
            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if(card.isCreature() && zone.is(Constant.Zone.Play) && (CardFactoryUtil.canTarget(ability, card))) {
                    ability.setTargetCard(card);
                    done();
                }
            }//selectCard()
            
            void done() {
            	payment.getCost().incrementTargets();
            	stop();
                payment.payCost();
            }
        };
        return target;
    }//targetCreature()
    
    public static Input targetPlayer(final SpellAbility ability, final Cost_Payment payment) {
        Input target = new Input() {
            private static final long serialVersionUID = 2781418414287281005L;
            
            @Override
            public void showMessage() {
                AllZone.Display.showMessage("Select target Player or Planeswalker");
                ButtonUtil.enableOnlyCancel();
            }
            
            @Override
            public void selectButtonCancel() {
            	payment.setCancel(true);
                stop();
                payment.payCost();
            }
            
            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if(card.isPlaneswalker() && zone.is(Constant.Zone.Play) && (!CardFactoryUtil.canTarget(ability, card))) {
                    ability.setTargetCard(card);
                    done();
                }
            }//selectCard()
            
            @Override
            public void selectPlayer(String player) {
                ability.setTargetPlayer(player);
                done();
            }
            
            void done() {
            	payment.getCost().incrementTargets();
                stop();
                payment.payCost();
            }
        };
        return target;
    }//targetPlayer()
    
    public void payComputerCosts(){
    	Card sacCard = null;
    	ability.setActivatingPlayer(Constant.Player.Computer);
    	if (cost.doesTarget())
    		ability.chooseTargetAI();
    	
    	// double check if something can be sacrificed here. Real check is in ComputerUtil.canPayAdditionalCosts()
    	if (cost.getSacCost()){
    		if (cost.getSacThis())
    			sacCard = card;
    		else
    			sacCard = ComputerUtil.chooseSacrificeType(cost.getSacType(), card, ability.getTargetCard());
    		
	    	if (sacCard == null){
	    		System.out.println("Couldn't find a valid card to sacrifice for: "+card.getName());
	    		return;
	    	}
    	}
    	// double check if counters available? Real check is in ComputerUtil.canPayAdditionalCosts()
    	int countersLeft = 0;
    	if (cost.getSubCounter()){
			Counters c = cost.getCounterType();
			countersLeft = card.getCounters(c) - cost.getCounterNum();
			if (countersLeft < 0){
	    		System.out.println("Not enough " + c.getName() + " on "+card.getName());
	    		return;
			}
    	}
    	
    	if (cost.getTap())
    		card.tap();
    	if (!cost.hasNoManaCost())
    		ComputerUtil.payManaCost(ability);
    	if (cost.getSubCounter())
    		card.setCounter(cost.getCounterType(), countersLeft);
		if (cost.getSacCost())
			AllZone.GameAction.sacrifice(sacCard);

        AllZone.Stack.add(ability);
    }
}
