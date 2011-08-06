package forge;

public class Target_Selection {
	private Target target = null;
	private SpellAbility ability = null;
	private Card card = null;

	public Target getTgt() { return target; }
	public SpellAbility getAbility() { return ability; }
	public Card getCard() { return card; }
	
	private SpellAbility_Requirements req = null;
	public void setRequirements(SpellAbility_Requirements reqs) { req = reqs; } 
	
	private boolean bCancel = false;
	public void setCancel(boolean done) { bCancel = done; }
	public boolean isCanceled() { return bCancel; }
	private boolean bDoneTarget = false;
	public void setDoneTarget(boolean done) { bDoneTarget = done; } 
	
	final private Input changeInput = new Input() {
		private static final long serialVersionUID = -5750122411788688459L; };
	
	public Target_Selection(Target tgt, SpellAbility sa){
		target = tgt;
		ability = sa;
		card = sa.getSourceCard();
	}
	
	public boolean doesTarget(){
		if (target == null)
			return false;
		return target.doesTarget();
	}
	
	public void resetTargets(){
		if (target != null)
			target.resetTargets();
	}
	
	public void incrementTargets(){
		if (target != null)
			target.incrementTargets();
	}
	
	public boolean chooseTargets(){
		// if not enough targets chosen, reset and cancel Ability
		if (bCancel || bDoneTarget && target.getNumTargeted() < target.getMinTargets()){
			bCancel = true;
			target.resetTargets();
			return false;
		}
		
		// if we haven't reached minimum targets, or we're still less than Max targets keep choosing
		// targeting, with forward code for multiple target abilities
		if (!bDoneTarget && target.getMinTargets() > 0 && target.getNumTargeted() < target.getMaxTargets()){
	        changeInput.stopSetNext(input_targetValid(ability, target.getValidTgts(), target.getVTSelection(), this, req));
	        return false;
		}
		
		return true;
	}

    // these have been copied over from CardFactoryUtil as they need two extra parameters for target selection.
	// however, due to the changes necessary for SA_Requirements this is much different than the original
    public static Input input_targetValid(final SpellAbility sa, final String[] Tgts, final String message, 
    		final Target_Selection select, final SpellAbility_Requirements req)
    {
    	return new Input() {
			private static final long serialVersionUID = -2397096454771577476L;

			@Override
	        public void showMessage() {
				String zone = select.getTgt().getZone();
				
				CardList choices = AllZoneUtil.getCardsInZone(zone).getValidCards(Tgts, sa.getSourceCard().getController(), sa.getSourceCard());

				if (zone.equals(Constant.Zone.Play)){
		            boolean canTargetPlayer = false;
		            for(String s : Tgts)
		            	if (s.equals("player") || s.equals("Player"))
		            		canTargetPlayer = true;
	
		            stopSetNext(input_targetSpecific(sa, choices, message, true, canTargetPlayer, select, req));
				}
				else{
					stopSetNext(input_cardFromList(sa, choices, message, true, select, req));
				}
	        }
    	};
    }//input_targetValid

    //CardList choices are the only cards the user can successful select
    public static Input input_targetSpecific(final SpellAbility spell, final CardList choices, final String message, 
    		final boolean targeted, final boolean bTgtPlayer, final Target_Selection select, final SpellAbility_Requirements req) {
        Input target = new Input() {
			private static final long serialVersionUID = -1091595663541356356L;

			@Override
            public void showMessage() {
                AllZone.Display.showMessage(message);
                ButtonUtil.enableOnlyCancel();
            }
            
            @Override
            public void selectButtonCancel() {
            	select.setCancel(true);
                stop();
                req.finishedTargeting();
            }
            
            @Override
            public void selectCard(Card card, PlayerZone zone) {
                if(targeted && !CardFactoryUtil.canTarget(spell, card)) {
                    AllZone.Display.showMessage("Cannot target this card (Shroud? Protection? Restrictions?).");
                } 
                else if(choices.contains(card)) {
                    spell.setTargetCard(card);
                    done();
                }
            }//selectCard()
            
            @Override
            public void selectPlayer(Player player) {
            	if (bTgtPlayer && !player.hasShroud()){	// todo: check if the player has Shroud too
	            	spell.setTargetPlayer(player);
	                done();
            	}
            }
            
            void done() {
            	select.incrementTargets();
                stop();
                req.finishedTargeting();
            }
        };
        return target;
    }//input_targetSpecific()
    
    
    public static Input input_cardFromList(final SpellAbility spell, final CardList choices, final String message, 
    		final boolean targeted, final Target_Selection select, final SpellAbility_Requirements req){
    	// Send in a list of valid cards, and popup a choice box to target 
	    Input target = new Input() {
	        private static final long serialVersionUID = 9027742835781889044L;
	        
	        @Override
	        public void showMessage() {
	            Object check = AllZone.Display.getChoiceOptional(message, choices.toArray());
	            if(check != null) {
	            	spell.setTargetCard((Card) check);
	            	select.incrementTargets();
	            } 
	            else
	            	select.setCancel(true);
	            
	            done();
	        }//showMessage()
	        
	        public void done(){
                stop();
                req.finishedTargeting();
	        }
	    };//Input
	    return target;
    } 
}
