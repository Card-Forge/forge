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
	
	public boolean chooseTargets(){
		// if not enough targets chosen, reset and cancel Ability
		if (bCancel || bDoneTarget && target.getNumTargeted() < target.getMinTargets()){
			bCancel = true;
			target.resetTargets();
			return false;
		}
		
		// if we haven't reached minimum targets, or we're stil less than Max targets keep choosing
		// targetting, with forward code for multiple target abilities 
		if (!bDoneTarget && target.getMinTargets() > 0 && target.getNumTargeted() < target.getMaxTargets()){
			if (target.canTgtCreature() && target.canTgtPlayer())
				changeInput.stopSetNext(targetCreaturePlayer(ability, Command.Blank, true, this, req));
			else if(target.canTgtCreature()) 
				changeInput.stopSetNext(targetCreature(ability, this, req));
	        else if(target.canTgtPlayer()) 
	        	changeInput.stopSetNext(targetPlayer(ability, this, req));
	        return false;
		}
		
		return true;
	}
	
    public static Input targetCreaturePlayer(final SpellAbility ability, final Command paid, final boolean targeted, 
    		final Target_Selection select, final SpellAbility_Requirements req) {
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
            	select.setCancel(true);
                stop();
                req.finishedTargeting();
            }
            
            @Override     
            public void selectButtonOK() {
            	select.setDoneTarget(true);
                stop();
                req.finishedTargeting();
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
                // if multitarget increment then select again
                done();
            }
            
            void done() {
            	select.getTgt().incrementTargets();
                paid.execute();
                stop();
                req.finishedTargeting();
            }
        };
        return target;
    }//input_targetCreaturePlayer()
    
	public static Input targetCreature(final SpellAbility ability, final Target_Selection select, final SpellAbility_Requirements req) {
        Input target = new Input() {
            private static final long serialVersionUID = 2781418414287281005L;
            
            @Override
            public void showMessage() {
                AllZone.Display.showMessage("Select target Creature");
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
                if(card.isCreature() && zone.is(Constant.Zone.Play) && (CardFactoryUtil.canTarget(ability, card))) {
                    ability.setTargetCard(card);
                    done();
                }
            }//selectCard()
            
            void done() {
            	select.getTgt().incrementTargets();
            	stop();
            	req.finishedTargeting();
            }
        };
        return target;
    }//targetCreature()
    
    public static Input targetPlayer(final SpellAbility ability, final Target_Selection select, final SpellAbility_Requirements req) {
        Input target = new Input() {
            private static final long serialVersionUID = 2781418414287281005L;
            
            @Override
            public void showMessage() {
                AllZone.Display.showMessage("Select target Player or Planeswalker");
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
            	select.getTgt().incrementTargets();
                stop();
                req.finishedTargeting();
            }
        };
        return target;
    }//targetPlayer()
    
    
}
