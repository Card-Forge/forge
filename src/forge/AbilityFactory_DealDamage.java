package forge;

import java.util.Random;

public class AbilityFactory_DealDamage {

	private AbilityFactory AF = null;
	
	private AbilityFactory getAF(){
		return AF;
	}
	private int nDamage = -1;
	
	private String XDamage = "none";
		
	private boolean TgtOpp = false;
	
	public SpellAbility getAbility(final AbilityFactory af, final int NumDmg, final String NumDmgX)
	{
		AF = af;
		nDamage = NumDmg;
		XDamage = NumDmgX;
		
		if (AF.getMapParams().get("Tgt").equals("TgtOpp"))
			TgtOpp = true;
		
        final SpellAbility abDamage = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt())
        {
        	private static final long serialVersionUID = -7560349014757367722L;
        	
            @Override
            public boolean canPlay(){
                return (Cost_Payment.canPayAdditionalCosts(AF.getAbCost(), this) 
                		&& CardFactoryUtil.canUseAbility(AF.getHostCard()) 
                		&& super.canPlay());
            }
            
            @Override
            public boolean canPlayAI() {
            	return doCanPlayAI(this);
            	
            }
            
            @Override
            public void resolve() {
            	doResolve(this);
            	AF.getHostCard().setAbilityUsed(AF.getHostCard().getAbilityUsed() + 1);
                
            }
        };//Ability_Activated
		
		return abDamage;
	}
	
	public SpellAbility getSpell(final AbilityFactory af, final int NumDmg, final String NumDmgX)
	{
		AF = af;
		nDamage = NumDmg;
		XDamage = NumDmgX;
		
		if (AF.getMapParams().get("Tgt").equals("TgtOpp"))
			TgtOpp = true;
		
		final SpellAbility spDealDamage = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
            private static final long serialVersionUID = 7239608350643325111L;

            @Override
            public boolean canPlay(){
                return (Cost_Payment.canPayAdditionalCosts(AF.getAbCost(), this) 
                		&& super.canPlay());
            }
            
            @Override
            public boolean canPlayAI() {
            	return doCanPlayAI(this);
            	
            }
            
            @Override
            public void resolve() {
            	doResolve(this);
                
            }

        
        }; // Spell
        
		return spDealDamage;
	}

    private int getNumDamage(SpellAbility saMe) {
        if(nDamage != -1) return nDamage;
        
		String calcX[] = XDamage.split("\\$");
		
		if (calcX.length == 1 || calcX[1].equals("none"))
			return 0;
		
		if (calcX[0].startsWith("Count"))
		{
			return CardFactoryUtil.xCount(AF.getHostCard(), calcX[1]);
		}
		else if (calcX[0].startsWith("Sacrificed"))
		{
			return CardFactoryUtil.handlePaid(saMe.getSacrificedCost(), calcX[1]);
		}
		
		return 0;
    }
    
    private boolean shouldTgtP(int d) {
        PlayerZone compHand = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer);
        CardList hand = new CardList(compHand.getCards());
        
        if(AF.isSpell() && hand.size() > 7) // anti-discard-at-EOT
        	return true;
        
        if(AllZone.Human_Life.getLife() - d < 10) // if damage from this spell would drop the human to less than 10 life
        	return true;
        
        return false;
    }
    
    private Card chooseTgtC(final int d) {
        // Combo alert!!
        PlayerZone compy = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
        CardList cPlay = new CardList(compy.getCards());
        if(cPlay.size() > 0) for(int i = 0; i < cPlay.size(); i++)
            if(cPlay.get(i).getName().equals("Stuffy Doll")) return cPlay.get(i);
        
        PlayerZone human = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
        CardList hPlay = new CardList(human.getCards());
        hPlay = hPlay.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                // will include creatures already dealt damage
                return c.isCreature() && ((c.getNetDefense() + c.getDamage()) <= d)
                        && CardFactoryUtil.canTarget(AF.getHostCard(), c);
            }
        });
        
        if(hPlay.size() > 0) {
            Card best = hPlay.get(0);
            
            if(hPlay.size() > 1) {
                for(int i = 1; i < hPlay.size(); i++) {
                    Card b = hPlay.get(i);
                    // choose best overall creature?
                    if(b.getSpellAbility().length > best.getSpellAbility().length
                            || b.getKeyword().size() > best.getKeyword().size()
                            || b.getNetAttack() > best.getNetAttack()) best = b;
                }
            }
            return best;
        }
        return null;
    }

    private boolean doCanPlayAI(SpellAbility saMe)
    {
    	// temporarily disabled until better AI
    	if (AF.getAbCost().getSacCost())	 return false;
    	if (AF.getAbCost().getSubCounter())  return false;
    	if (AF.getAbCost().getLifeCost())	 return false;
    	
    	if (!ComputerUtil.canPayCost(saMe))
    		return false;

		// TODO handle proper calculation of X values based on Cost
        int damage = getNumDamage(saMe);
        
        boolean rr = false;
        
        if (AF.isAbility())
        {
        	Random r = new Random(); // prevent run-away activations 
        	if(r.nextFloat() <= Math.pow(.6667, AF.getHostCard().getAbilityUsed())) 
        		rr = true;
        }
        else if (AF.isSpell())
        	rr = true;
        
        if(AF.getAbTgt().canTgtCreaturePlayer()) {
            if(shouldTgtP(damage)) {
                saMe.setTargetPlayer(Constant.Player.Human);
                return rr;
            }
            
            Card c = chooseTgtC(damage);
            if(c != null) {
                saMe.setTargetCard(c);
                return rr;
            }
        }
        
        if(AF.getAbTgt().canTgtPlayer() || TgtOpp == true) {
            saMe.setTargetPlayer(Constant.Player.Human);
            return rr;
        }
        
        if(AF.getAbTgt().canTgtCreature()) {
            Card c = chooseTgtC(damage);
            if(c != null) {
                saMe.setTargetCard(c);
                return rr;
            }
        }
        return false;
    	
    }
    
    private void doResolve(SpellAbility saMe)
    {
        int damage = getNumDamage(saMe);
        String tgtP = "";
        
        if(TgtOpp == true) {
            tgtP = AllZone.GameAction.getOpponent(AF.getHostCard().getController());
            saMe.setTargetPlayer(tgtP);
        }
        
        Card c = saMe.getTargetCard();
        if(c != null) {
            if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(AF.getHostCard(), c)) {
                AllZone.GameAction.addDamage(c, AF.getHostCard(), damage);
                tgtP = c.getController();
                
                if(AF.hasSubAbility())
                	CardFactoryUtil.doDrawBack(AF.getMapParams().get("SubAbility"), damage,
                        AF.getHostCard().getController(), AllZone.GameAction.getOpponent(AF.getHostCard().getController()),
                        tgtP, AF.getHostCard(), c, saMe);

            }
        } else {
            tgtP = saMe.getTargetPlayer();
            AllZone.GameAction.addDamage(tgtP, AF.getHostCard(), damage);
            
            if(AF.hasSubAbility()) 
            	CardFactoryUtil.doDrawBack(AF.getMapParams().get("SubAbility"), damage,
                    AF.getHostCard().getController(), AllZone.GameAction.getOpponent(AF.getHostCard().getController()),
                    tgtP, AF.getHostCard(), null, saMe);

        }

    }
}
