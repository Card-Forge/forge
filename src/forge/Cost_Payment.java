package forge;

import java.util.ArrayList;
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
	private boolean payExile;
	private boolean payLife;
	private boolean payDiscard;
	private boolean payTapXType;
	private boolean payReturn;
	
	private boolean bCancel = false;
	
	private static CardList payTapXTypeTappedList = new CardList();
	static void addPayTapXTypeTappedList(Card c)
	{
		payTapXTypeTappedList.add(c);
	}

	public void setPayMana(boolean bPay){	payMana = bPay;	}
	public void setPayDiscard(boolean bSac){	payDiscard = bSac;	}
	public void setPaySac(boolean bSac){	paySac = bSac;	}
	public void setPayExile(boolean bExile) { payExile = bExile; }
	public void setPayTapXType(boolean bTapX) { payTapXType = bTapX; }
	public void setPayReturn(boolean bReturn){	payReturn = bReturn; }
	
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
		payExile = !cost.getExileCost();
		payLife = !cost.getLifeCost();
		payDiscard = !cost.getDiscardCost();
		payTapXType = !cost.getTapXTypeCost();
		payReturn = !cost.getReturnCost();
	}
    
	public static boolean canPayAdditionalCosts(Ability_Cost cost, SpellAbility ability){
		if (cost == null)
			return true;
		
		final Card card = ability.getSourceCard();
    	if (cost.getTap() && (card.isTapped() || card.isSick()))
    		return false;
    	
    	if (cost.getUntap() && (card.isUntapped() || card.isSick()))
    		return false;
    	
		if (cost.getTapXTypeCost()){
			PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
			CardList typeList = new CardList(play.getCards());
			    
			typeList = typeList.getValidCards(cost.getTapXType().split(","));
			
			if (cost.getTap()) {
				typeList = typeList.filter(new CardListFilter()
				{
					public boolean addCard(Card c)
					{
						return !c.equals(card) && c.isUntapped();
					}
				});
			}
			if (typeList.size() == 0)
			 	return false;
		}
    	
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
    	
    	if (cost.getDiscardCost()){
    		PlayerZone zone = AllZone.getZone(Constant.Zone.Hand, card.getController());
    		CardList handList = new CardList(zone.getCards());
    		String discType = cost.getDiscardType();
    		int discAmount = cost.getDiscardAmount();
    		if (discType.equals("Hand")){
    			// this will always work
    		}
    		else{
    			if (!discType.equals("Any") && !discType.equals("Random")){
    				String validType[] = discType.split(",");
    				handList = handList.getValidCards(validType);
    			}
	    		if (discAmount > handList.size()){
	    			// not enough cards in hand to pay
	    			return false;
	    		}
    		}
    	}
    	
		if (cost.getSacCost()){
			if (!cost.getSacThis()){
			    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
			    CardList typeList = new CardList(play.getCards());
			    
			    typeList = typeList.getValidCards(cost.getSacType().split(",")); 
				if (typeList.size() < cost.getSacAmount())
					return false;
			}
			else if (!AllZone.GameAction.isCardInPlay(card))
				return false;
		}
		
		if (cost.getExileCost()){
			if (!cost.getExileThis()){
			    CardList typeList = AllZoneUtil.getPlayerCardsInPlay(card.getController());
			    
			    typeList = typeList.getValidCards(cost.getExileType().split(",")); 
				if (typeList.size() < cost.getExileAmount())
					return false;
			}
			else if (!AllZone.GameAction.isCardInPlay(card))
				return false;
		}
		
		if (cost.getReturnCost()){
			if (!cost.getReturnThis()){
			    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
			    CardList typeList = new CardList(play.getCards());
			    
			    typeList = typeList.getValidCards(cost.getReturnType().split(",")); 
				if (typeList.size() < cost.getReturnAmount())
					return false;
			}
			else if (!AllZone.GameAction.isCardInPlay(card))
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
		
		if (!payTapXType && cost.getTapXTypeCost()){
			PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
            CardList typeList = new CardList(play.getCards());
            typeList = typeList.getValidCards(cost.getTapXType().split(","));
            
			changeInput.stopSetNext(input_tapXCost(cost.getTapXTypeAmount(),cost.getTapXType(), typeList, ability, this));
			return false;
		}
		
		if (!paySubCounter && cost.getSubCounter()){	// pay counters here. 
			Counters c = cost.getCounterType();
			if (card.getCounters(c) >= cost.getCounterNum()){
				card.subtractCounter(c, cost.getCounterNum());
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
		
		if (!payDiscard && cost.getDiscardCost()){			// discard here
    		PlayerZone zone = AllZone.getZone(Constant.Zone.Hand, card.getController());
    		CardList handList = new CardList(zone.getCards());
    		String discType = cost.getDiscardType();
    		int discAmount = cost.getDiscardAmount();
    		if (discType.equals("Hand")){
    			AllZone.GameAction.discardHand(card.getController(), ability);
    			payDiscard = true;
    		}
    		else{
    			if (discType.equals("Random")){
    				AllZone.GameAction.discardRandom(card.getController(), discAmount, ability);
    				payDiscard = true;
    			}
    			else{
	    			if (!discType.equals("Any")){
	    				String validType[] = discType.split(",");
	    				handList = handList.getValidCards(validType);
	    			}
	    			changeInput.stopSetNext(input_discardCost(discAmount, discType, handList, ability, this));
	    			return false;
    			}
    		}
		}
		
		if (!paySac && cost.getSacCost()){					// sacrifice stuff here
    		if (cost.getSacThis())
    			changeInput.stopSetNext(sacrificeThis(ability, this));
    		else
    			changeInput.stopSetNext(sacrificeType(ability, cost.getSacType(), this));
    		return false;
    	}
		
		if (!payExile && cost.getExileCost()){					// exile stuff here
    		if (cost.getExileThis())
    			changeInput.stopSetNext(exileThis(ability, this));
    		else
    			changeInput.stopSetNext(exileType(ability, cost.getExileType(), this));
    		return false;
    	}
		
		if (!payReturn && cost.getReturnCost()){					// return stuff here
    		if (cost.getReturnThis())
    			changeInput.stopSetNext(returnThis(ability, this));
    		else
    			changeInput.stopSetNext(returnType(ability, cost.getReturnType(), this));
    		return false;
    	}

		req.finishPaying();
		return true;
	}

	public boolean isAllPaid(){
		return (payTap && payUntap && payMana && paySubCounter && paySac && payExile && payLife && payDiscard && payTapXType && payReturn);
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
        
		if (cost.getTapXTypeCost() /*&& payTapXType*/){

			for (Card c:payTapXTypeTappedList)
				c.untap();	
			//needed?
			payTapXTypeTappedList = new CardList();
		}
        
        // refund counters
        if (cost.getSubCounter() && paySubCounter){
			Counters c = cost.getCounterType();
			int countersLeft = card.getCounters(c) + cost.getCounterNum();
			card.setCounter(c, countersLeft, true);
        }
        
        // refund life
        if (cost.getLifeCost() && payLife){
        	PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
        	life.payLife(cost.getLifeAmount()*-1);
        }
        
        // can't really undiscard things
        
		// can't really unsacrifice things
        
        //can't really unexile things
        
        // can't really unreturn things
	}
    
    public void payComputerCosts(){
    	// make sure ComputerUtil.canPayAdditionalCosts() is updated when updating new Costs
    	ArrayList<Card> sacCard = new ArrayList<Card>();
    	ArrayList<Card> exileCard = new ArrayList<Card>();
    	ArrayList<Card> tapXCard = new ArrayList<Card>();
    	ArrayList<Card> returnCard = new ArrayList<Card>();
    	ability.setActivatingPlayer(Constant.Player.Computer);
    	
    	// double check if something can be sacrificed here. Real check is in ComputerUtil.canPayAdditionalCosts()
    	if (cost.getSacCost()){
    		if (cost.getSacThis())
    			sacCard.add(card);
    		else{
    			for(int i = 0; i < cost.getSacAmount(); i++)
    				sacCard.add(ComputerUtil.chooseSacrificeType(cost.getSacType(), card, ability.getTargetCard()));
    		}
    		
	    	if (sacCard.size() != cost.getSacAmount()){
	    		System.out.println("Couldn't find a valid card to sacrifice for: "+card.getName());
	    		return;
	    	}
    	}
    	
    	// double check if something can be exiled here. Real check is in ComputerUtil.canPayAdditionalCosts()
    	if (cost.getExileCost()){
    		if (cost.getExileThis())
    			exileCard.add(card);
    		else{
    			for(int i = 0; i < cost.getExileAmount(); i++)
    				exileCard.add(ComputerUtil.chooseExileType(cost.getExileType(), card, ability.getTargetCard()));
    		}
    		
	    	if (exileCard.size() != cost.getExileAmount()){
	    		System.out.println("Couldn't find a valid card to exile for: "+card.getName());
	    		return;
	    	}
    	}
    	
    	if (cost.getReturnCost()){
    		if (cost.getReturnThis())
    			returnCard.add(card);
    		else{
    			for(int i = 0; i < cost.getReturnAmount(); i++)
    				returnCard.add(ComputerUtil.chooseReturnType(cost.getReturnType(), card, ability.getTargetCard()));
    		}
    		
	    	if (returnCard.size() != cost.getReturnAmount()){
	    		System.out.println("Couldn't find a valid card to return for: "+card.getName());
	    		return;
	    	}
    	}
    	
    	if (cost.getTapXTypeCost()) {
    		boolean tap = cost.getTap();
    		
    		for(int i = 0; i < cost.getTapXTypeAmount(); i++)
    			tapXCard.add(ComputerUtil.chooseTapType(cost.getTapXType(), card, tap, i));
    		
    		if (tapXCard.size() != cost.getTapXTypeAmount()){
	    		System.out.println("Couldn't find a valid card to tap for: "+card.getName());
	    		return;
	    	}
    	}
    	
    	// double check if counters available? Real check is in ComputerUtil.canPayAdditionalCosts()
    	if (cost.getSubCounter() && cost.getCounterNum() > card.getCounters(cost.getCounterType())){
    		System.out.println("Not enough " + cost.getCounterType() + " on " + card.getName());
    		return;
    	}
    	
    	if (cost.getTap())
    		card.tap();

    	if (cost.getUntap())
    		card.untap();
    	
    	if (!cost.hasNoManaCost())
    		ComputerUtil.payManaCost(ability);
    	
		if (cost.getTapXTypeCost()){
			for (Card c : tapXCard)
				c.tap();
		}
    	
    	if (cost.getSubCounter())
    		card.subtractCounter(cost.getCounterType(), cost.getCounterNum());
    	
    	if (cost.getLifeCost())
    		AllZone.GameAction.getPlayerLife(card.getController()).payLife(cost.getLifeAmount());
    	
    	if (cost.getDiscardCost()){
    		String discType = cost.getDiscardType();
    		int discAmount = cost.getDiscardAmount();
    		if (discType.equals("Hand")){
    			AllZone.GameAction.discardHand(card.getController(), ability);
    		}
    		else{
    			if (discType.equals("Random")){
    				AllZone.GameAction.discardRandom(card.getController(), discAmount, ability);
    			}
    			else{
	    			if (!discType.equals("Any")){
	    				String validType[] = discType.split(",");
	    				AllZone.GameAction.AI_discardNumType(discAmount, validType, ability);
	    			}
	    			else{
	    				AllZone.GameAction.AI_discardNum(discAmount, ability);
	    			}
    			}
    		}
    	}
    	
		if (cost.getSacCost()){
			for(Card c : sacCard)
				AllZone.GameAction.sacrifice(c);
		}
		
		if (cost.getExileCost()){
			for(Card c : exileCard)
				AllZone.GameAction.exile(c);
		}
		
		if (cost.getReturnCost()){
			for(Card c : returnCard)
				AllZone.GameAction.moveToHand(c);
		}

        AllZone.Stack.add(ability);
    }
    
	public void changeCost(){
		cost.changeCost(ability);
	}
    
    public static Input input_discardCost(final int nCards, final String discType, final CardList handList, SpellAbility sa, final Cost_Payment payment) {
        final SpellAbility sp = sa;
    	Input target = new Input() {
            private static final long serialVersionUID = -329993322080934435L;
            
            int                       nDiscard                = 0;
            
            @Override
            public void showMessage() {
            	if (AllZone.Human_Hand.getCards().length == 0) stop();
            	StringBuilder type = new StringBuilder("");
            	if (!discType.equals("Any")){
            		type.append(" ").append(discType);
            	}
                AllZone.Display.showMessage("Select a"+ type.toString() + " card to discard");
                ButtonUtil.enableOnlyCancel();
            }
            
            @Override
            public void selectButtonCancel() {
            	cancel();
            }
            
            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if(zone.is(Constant.Zone.Hand) && handList.contains(card) ) {
                	// send in CardList for Typing
                    AllZone.GameAction.discard(card, sp);
                    handList.remove(card);
                    nDiscard++;
                    
                    //in case no more cards in hand
                    if(nDiscard == nCards) 
                    	done();
                    else if (AllZone.Human_Hand.getCards().length == 0)	// this really shouldn't happen
                    	cancel();
                    else
                    	showMessage();
                }
            }
            
            public void cancel(){
            	payment.setCancel(true);
            	stop();
            	payment.payCost();
            }
            
            public void done(){
            	payment.setPayDiscard(true);
            	stop();
            	payment.payCost();
            }
        };
        return target;
    }//input_discard() 
	
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
    
    public static Input sacrificeType(final SpellAbility spell, final String type, final Cost_Payment payment){
        Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;
            private CardList typeList;
            private int nSacrifices = 0;
            private int nNeeded = payment.getCost().getSacAmount();
            
            @Override
            public void showMessage() {
            	StringBuilder msg = new StringBuilder("Sacrifice ");
            	int nLeft = nNeeded - nSacrifices;
            	msg.append(nLeft).append(" ");
            	msg.append(type);
            	if (nLeft > 1){
            		msg.append("s");
            	}
            	
                PlayerZone play = AllZone.getZone(Constant.Zone.Play, spell.getSourceCard().getController());
                typeList = new CardList(play.getCards());
                typeList = typeList.getValidCards(type.split(","));
                AllZone.Display.showMessage(msg.toString());
                ButtonUtil.enableOnlyCancel();
            }
            
            @Override
            public void selectButtonCancel() {
            	cancel();
            }
            
            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if(typeList.contains(card)) {
                	nSacrifices++;
                	AllZone.GameAction.sacrifice(card);
                	typeList.remove(card);
                    //in case nothing else to sacrifice
                    if(nSacrifices == nNeeded) 
                    	done();
                    else if (typeList.size() == 0)	// this really shouldn't happen
                    	cancel();
                    else
                    	showMessage();
                }
            }
            
            public void done(){
            	payment.setPaySac(true);
            	stop();
            	payment.payCost();
            }
            
            public void cancel(){
            	payment.setCancel(true);
            	stop();
            	payment.payCost();
            }
        };
        return target;
    }//sacrificeType()
    
    public static Input exileThis(final SpellAbility spell, final Cost_Payment payment) {
        Input target = new Input() {
			private static final long serialVersionUID = 678668673002725001L;

			@Override
            public void showMessage() {
            	Card card = spell.getSourceCard();
                if(card.getController().equals(Constant.Player.Human) && AllZone.GameAction.isCardInPlay(card)) {
        			StringBuilder sb = new StringBuilder();
        			sb.append(card.getName());
        			sb.append(" - Exile?");
        			Object[] possibleValues = {"Yes", "No"};
                	Object choice = JOptionPane.showOptionDialog(null, sb.toString(), card.getName() + " - Cost",  
                			JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                			null, possibleValues, possibleValues[0]);
                    if(choice.equals(0)) {
                    	payment.setPayExile(true);
                    	AllZone.GameAction.exile(card);
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
    }//input_exile()
    
    public static Input exileType(final SpellAbility spell, final String type, final Cost_Payment payment){
        Input target = new Input() {
			private static final long serialVersionUID = 1403915758082824694L;
			
			private CardList typeList;
            private int nExiles = 0;
            private int nNeeded = payment.getCost().getExileAmount();
            
            @Override
            public void showMessage() {
            	StringBuilder msg = new StringBuilder("Exile ");
            	int nLeft = nNeeded - nExiles;
            	msg.append(nLeft).append(" ");
            	msg.append(type);
            	if (nLeft > 1){
            		msg.append("s");
            	}
            	
                PlayerZone play = AllZone.getZone(Constant.Zone.Play, spell.getSourceCard().getController());
                typeList = new CardList(play.getCards());
                typeList = typeList.getValidCards(type.split(","));
                AllZone.Display.showMessage(msg.toString());
                ButtonUtil.enableOnlyCancel();
            }
            
            @Override
            public void selectButtonCancel() {
            	cancel();
            }
            
            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if(typeList.contains(card)) {
                	nExiles++;
                	AllZone.GameAction.exile(card);
                	typeList.remove(card);
                    //in case nothing else to exile
                    if(nExiles == nNeeded) 
                    	done();
                    else if (typeList.size() == 0)	// this really shouldn't happen
                    	cancel();
                    else
                    	showMessage();
                }
            }
            
            public void done(){
            	payment.setPayExile(true);
            	stop();
            	payment.payCost();
            }
            
            public void cancel(){
            	payment.setCancel(true);
            	stop();
            	payment.payCost();
            }
        };
        return target;
    }//exileType()
    
    public static Input input_tapXCost(final int nCards, final String cardType, final CardList cardList, SpellAbility sa, final Cost_Payment payment) {
        //final SpellAbility sp = sa;
    	Input target = new Input() {

			private static final long serialVersionUID = 6438988130447851042L;
			int                       nTapped                = 0;
            
            @Override
            public void showMessage() {
            	if (cardList.size() == 0) stop();
            	
            	int left = nCards - nTapped;
                AllZone.Display.showMessage("Select a "+ cardType + " to tap (" +left + " left)");
                ButtonUtil.enableOnlyCancel();
            }
            
            @Override
            public void selectButtonCancel() {
            	cancel();
            }
            
            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if(zone.is(Constant.Zone.Play) && cardList.contains(card) && card.isUntapped() ) {
                	// send in CardList for Typing
                    card.tap();
                    payTapXTypeTappedList.add(card);
                    cardList.remove(card);
                    nTapped++;
                    
                    if(nTapped == nCards) 
                    	done();
                    else if (cardList.size() == 0)	// this really shouldn't happen
                    	cancel();
                    else
                    	showMessage();
                }
            }
            
            public void cancel(){
            	payment.setCancel(true);
            	stop();
            	payment.payCost();
            }
            
            public void done(){
            	payment.setPayTapXType(true);
            	stop();
            	payment.payCost();
            }
        };
        return target;
    }//input_tapXCost() 
    
    public static Input returnThis(final SpellAbility spell, final Cost_Payment payment) {
        Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;
            
            @Override
            public void showMessage() {
            	Card card = spell.getSourceCard();
                if(card.getController().equals(Constant.Player.Human) && AllZone.GameAction.isCardInPlay(card)) {
        			StringBuilder sb = new StringBuilder();
        			sb.append(card.getName());
        			sb.append(" - Return to Hand?");
        			Object[] possibleValues = {"Yes", "No"};
                	Object choice = JOptionPane.showOptionDialog(null, sb.toString(), card.getName() + " - Cost",  
                			JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE,
                			null, possibleValues, possibleValues[0]);
                    if(choice.equals(0)) {
                    	payment.setPayReturn(true);
                    	AllZone.GameAction.moveToHand(card);
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
    
    public static Input returnType(final SpellAbility spell, final String type, final Cost_Payment payment){
        Input target = new Input() {
            private static final long serialVersionUID = 2685832214519141903L;
            private CardList typeList;
            private int nReturns = 0;
            private int nNeeded = payment.getCost().getReturnAmount();
            
            @Override
            public void showMessage() {
            	StringBuilder msg = new StringBuilder("Return ");
            	int nLeft = nNeeded - nReturns;
            	msg.append(nLeft).append(" ");
            	msg.append(type);
            	if (nLeft > 1){
            		msg.append("s");
            	}
            	
                PlayerZone play = AllZone.getZone(Constant.Zone.Play, spell.getSourceCard().getController());
                typeList = new CardList(play.getCards());
                typeList = typeList.getValidCards(type.split(","));
                AllZone.Display.showMessage(msg.toString());
                ButtonUtil.enableOnlyCancel();
            }
            
            @Override
            public void selectButtonCancel() {
            	cancel();
            }
            
            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if(typeList.contains(card)) {
                	nReturns++;
                	AllZone.GameAction.moveToHand(card);
                	typeList.remove(card);
                    //in case nothing else to return
                    if(nReturns == nNeeded) 
                    	done();
                    else if (typeList.size() == 0)	// this really shouldn't happen
                    	cancel();
                    else
                    	showMessage();
                }
            }
            
            public void done(){
            	payment.setPayReturn(true);
            	stop();
            	payment.payCost();
            }
            
            public void cancel(){
            	payment.setCancel(true);
            	stop();
            	payment.payCost();
            }
        };
        return target;
    }//returnType()  
}
