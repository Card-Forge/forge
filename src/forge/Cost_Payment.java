package forge;

import javax.swing.JOptionPane;

public class Cost_Payment {
	private Ability_Cost cost = null;
	private SpellAbility ability = null;
	private Card card = null;
	private SpellAbility_Requirements req = null;

	public Ability_Cost getCost() { return cost; }
	public SpellAbility getAbility() { return ability; }
	public Card getCard() { return card; }
	
	public void setRequirements(SpellAbility_Requirements reqs) { req = reqs; } 
	public void setCancel(boolean cancel) { bCancel = cancel; } 
	public boolean isCanceled() { return bCancel; }
	
	// No default values so an error will be kicked if not set properly in constructor
	private boolean payTap;
	private boolean payUntap; 
	private boolean payMana;
	private boolean paySubCounter;
	private boolean paySac;
	private boolean payLife;
	
	private boolean bCancel = false;

	public void setPayMana(boolean bPay){	payMana = bPay;	}
	public void setPaySac(boolean bSac){	paySac = bSac;	}
	
	final private Input changeInput = new Input() {
		private static final long serialVersionUID = -5750122411788688459L; };
	
	public Cost_Payment(Ability_Cost cost, SpellAbility abil){
		this.cost = cost;
		this.ability = abil;
		card = this.ability.getSourceCard();
		payTap = !cost.getTap();
		payUntap = !cost.getUntap();
		payMana = cost.hasNoManaCost();
		paySubCounter = !cost.getSubCounter();
		paySac = !cost.getSacCost();
		payLife = !cost.getLifeCost();
	}
	
    public boolean canPayAdditionalCosts(){
    	if (cost.getTap() && (card.isTapped() || card.isSick()))
    		return false;
    	
    	if (cost.getUntap() && (card.isUntapped() || card.isSick()))
    		return false;
    	
    	int countersLeft = 0;
    	if (cost.getSubCounter()){
			Counters c = cost.getCounterType();
			countersLeft = card.getCounters(c) - cost.getCounterNum();
			if (countersLeft < 0){
	    		return false;
			}
    	}
    	
    	if (cost.getLifeCost()){
    		int curLife = AllZone.GameAction.getPlayerLife(card.getController()).getLife();
    		if (curLife < cost.getLifeAmount())
    			return false;
    	}
    	
		if (cost.getSacCost()){
			if (!cost.getSacThis()){
			    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
			    CardList typeList = new CardList(play.getCards());
			    
			    // todo(sol) switch this in
			    //typeList = typeList.getValidCards(cost.getSacType()); 
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
		if (bCancel){
			req.finishPaying();
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

		if (!payUntap && cost.getUntap()){
			if (card.isTapped()){
				card.untap();
				payUntap = true;
			}
			else
				return false;
		}
		
		if (!payMana && !cost.hasNoManaCost()){		// pay mana here
			changeInput.stopSetNext(new Input_PayCostMana(this));
			return false;
		}
		if (!paySubCounter && cost.getSubCounter()){	// pay counters here. 
			Counters c = cost.getCounterType();
			int countersLeft = card.getCounters(c) - cost.getCounterNum();
			if (countersLeft >= 0){
				card.setCounter(c, countersLeft);
				paySubCounter = true;
			}
			else{
				bCancel = true;
				req.finishPaying();
				return false;
			}
		}
		
		if (!payLife && cost.getLifeCost()){			// pay life here
			StringBuilder sb = new StringBuilder();
			sb.append(getCard().getName());
			sb.append(" - Pay ");
			sb.append(cost.getLifeAmount());
			sb.append(" Life?");
			Object[] possibleValues = {"Yes", "No"};
        	Object choice = JOptionPane.showOptionDialog(null, sb.toString(), getCard().getName() + " - Cost",  
        			JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
        			null, possibleValues, possibleValues[0]);
            if(choice.equals(0)) {
            	  AllZone.GameAction.getPlayerLife(card.getController()).payLife(cost.getLifeAmount());
            	  payLife = true;
            }
			else{
				bCancel = true;
				req.finishPaying();
				return false;
			}
		}
		
		if (!paySac && cost.getSacCost()){
    		// sacrifice stuff here
    		if (cost.getSacThis())
    			changeInput.stopSetNext(sacrificeThis(ability, this));
    		else
    			changeInput.stopSetNext(sacrificeType(ability, cost.getSacType(), cost.sacString(true), this));
    		return false;
    	}

		req.finishPaying();
		return true;
	}

	public boolean isAllPaid(){
		return (payTap && payUntap && payMana && paySubCounter && paySac && payLife);
	}
	
	public void cancelPayment(){
		// unpay anything we can.
		if (cost.getTap() && payTap){
			// untap if tapped
			card.untap();
		}
		if (cost.getUntap() && payUntap){
			// tap if untapped
			card.tap();
		}
		// refund mana
        AllZone.ManaPool.unpaid();
        
        // refund counters
        if (cost.getSubCounter() && paySubCounter){
			Counters c = cost.getCounterType();
			int countersLeft = card.getCounters(c) + cost.getCounterNum();
			card.setCounter(c, countersLeft);
        }
        
        // refund life
        if (cost.getLifeCost() && payLife){
        	PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
        	life.payLife(cost.getLifeAmount()*-1);
        }
        
		// can't really unsacrifice things
	}
    
    public void payComputerCosts(){
    	Card sacCard = null;
    	ability.setActivatingPlayer(Constant.Player.Computer);
    	
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

    	if (cost.getUntap())
    		card.untap();
    	
    	if (!cost.hasNoManaCost())
    		ComputerUtil.payManaCost(ability);
    	
    	if (cost.getSubCounter())
    		card.setCounter(cost.getCounterType(), countersLeft);
    	
    	if (cost.getLifeCost())
    		AllZone.GameAction.getPlayerLife(card.getController()).payLife(cost.getLifeAmount());
    	
		if (cost.getSacCost())
			AllZone.GameAction.sacrifice(sacCard);

        AllZone.Stack.add(ability);
    }
    
	
    public static Input sacrificeThis(final SpellAbility spell, final Cost_Payment payment) {
        Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;
            
            @Override
            public void showMessage() {
            	Card card = spell.getSourceCard();
                if(card.getController().equals(Constant.Player.Human) && AllZone.GameAction.isCardInPlay(card)) {
        			StringBuilder sb = new StringBuilder();
        			sb.append(card.getName());
        			sb.append(" - Sacrifice?");
        			Object[] possibleValues = {"Yes", "No"};
                	Object choice = JOptionPane.showOptionDialog(null, sb.toString(), card.getName() + " - Cost",  
                			JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                			null, possibleValues, possibleValues[0]);
                    if(choice.equals(0)) {
                    	payment.setPaySac(true);
                    	AllZone.GameAction.sacrifice(card);
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
                	payment.setPaySac(true);
                	AllZone.GameAction.sacrifice(card);
                	stop();
                	payment.payCost();
                }
            }
        };
        return target;
    }//sacrificeType()
}
