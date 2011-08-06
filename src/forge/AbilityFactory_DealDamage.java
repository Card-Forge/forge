package forge;

import java.util.ArrayList;
import java.util.Random;

public class AbilityFactory_DealDamage {

	private AbilityFactory AF = null;
	
	private int nDamage = -1;
	
	private String XDamage = "none";
		
	private boolean TgtOpp = false;
	
	public SpellAbility getAbility(final AbilityFactory af, final int NumDmg, final String NumDmgX)
	{
		AF = af;
		nDamage = NumDmg;
		XDamage = NumDmgX;
		
		if(af.getMapParams().containsKey("Tgt"))
			if (AF.getMapParams().get("Tgt").equals("TgtOpp"))
				TgtOpp = true;
		
        final SpellAbility abDamage = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt())
        {
        	private static final long serialVersionUID = -7560349014757367722L;
        	
            @Override
            public boolean canPlay(){
                return super.canPlay();
            }
            
            @Override
            public boolean canPlayAI() {
            	return doCanPlayAI(this);
            	
            }
            
			@Override
			public String getStackDescription(){
				return damageStackDescription(AF, this);
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
		
		if(af.getMapParams().containsKey("Tgt"))
			if (AF.getMapParams().get("Tgt").equals("TgtOpp"))
				TgtOpp = true;
		
		final SpellAbility spDealDamage = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
            private static final long serialVersionUID = 7239608350643325111L;

            @Override
            public boolean canPlay(){
                return super.canPlay();
            }
            
            @Override
            public boolean canPlayAI() {
            	return doCanPlayAI(this);
            	
            }
            
			@Override
			public String getStackDescription(){
				return damageStackDescription(AF, this);
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
        PlayerZone compHand = AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer);
        CardList hand = new CardList(compHand.getCards());
        
        if(AF.isSpell() && hand.size() > 7) // anti-discard-at-EOT
        	return true;
        
        if(AllZone.HumanPlayer.getLife() - d < 10) // if damage from this spell would drop the human to less than 10 life
        	return true;
        
        return false;
    }
    
    private Card chooseTgtC(final int d) {
        // Combo alert!!
        PlayerZone compy = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
        CardList cPlay = new CardList(compy.getCards());
        if(cPlay.size() > 0) for(int i = 0; i < cPlay.size(); i++)
            if(cPlay.get(i).getName().equals("Stuffy Doll")) return cPlay.get(i);
        
        PlayerZone human = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
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
        
        boolean rr = AF.isSpell();
        
        if (AF.isAbility())
        {
        	Random r = new Random(); // prevent run-away activations 
        	if(r.nextFloat() <= Math.pow(.6667, AF.getHostCard().getAbilityUsed())) 
        		rr = true;
        }
        
        Target tgt = AF.getAbTgt();
        // AI handle multi-targeting?
        tgt.resetTargets();
		 // target loop
		while(tgt.getNumTargeted() < tgt.getMaxTargets()){ 
	        // TODO: Consider targeting the planeswalker
	        if(tgt.canTgtCreatureAndPlayer()) {

	            if(shouldTgtP(damage)) {
	            	tgt.addTarget(AllZone.HumanPlayer);
	                continue;
	            }
	            
	            Card c = chooseTgtC(damage);
	            if(c != null) {
	            	tgt.addTarget(c);
	                continue;
	            }
	        }
	        
	        if(tgt.canTgtPlayer() || TgtOpp) {
	        	tgt.addTarget(AllZone.HumanPlayer);
	            continue;
	        }
	        
	        if(tgt.canTgtCreature()) {
	            Card c = chooseTgtC(damage);
	            if(c != null) {
	            	tgt.addTarget(c);
	                continue;
	            }
	        }
	        // fell through all the choices, no targets left?
			if (tgt.getNumTargeted() < tgt.getMinTargets() || tgt.getNumTargeted() == 0){
				tgt.resetTargets();
				return false;
			}
			else{
				// todo is this good enough? for up to amounts?
				break;
			}
		}
        
        return rr;
    	
    }
    
    private String damageStackDescription(AbilityFactory af, SpellAbility sa){
		// when damageStackDescription is called, just build exactly what is happening
		 StringBuilder sb = new StringBuilder();
		 String name = af.getHostCard().getName();
		 int damage = getNumDamage(sa);

		 ArrayList<Object> tgts;
		 Target tgt = AF.getAbTgt();
		 if (tgt != null)
			tgts = tgt.getTargets();
		 else{
			tgts = new ArrayList<Object>();
			if (TgtOpp)
				tgts.add(AF.getHostCard().getController().getOpponent());
		 }
	        
		 sb.append(name).append(" - ");
		 sb.append("Deals ").append(damage).append(" damage to ");
		 for(int i = 0; i < tgts.size(); i++){
			 Object o = tgts.get(0);
			 if (o instanceof Player){
				 sb.append(((Player)o).getName());
			 }
			 else{
				 sb.append(((Card)o).getName());
			 }
			 sb.append(" ");
		 }

		 return sb.toString();
    }
    
    private void doResolve(SpellAbility saMe)
    {
        int damage = getNumDamage(saMe);
        
        ArrayList<Object> tgts;
        Target tgt = AF.getAbTgt();
        if (tgt != null)
        	tgts = tgt.getTargets();
        else{
        	tgts = new ArrayList<Object>();
        	if (TgtOpp)
        		tgts.add(AF.getHostCard().getController().getOpponent());
        }

        if (tgts.size() == 0){
        	System.out.println("No targets?");
        	return;
        }
        
        for(Object o : tgts){
        	if (o instanceof Card){
        		 Card c = (Card)o;
        		 if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(AF.getHostCard(), c))
                     c.addDamage(damage, AF.getHostCard());
        	}
        	else if (o instanceof Player){
        		Player p = (Player) o;
        		if (p.canTarget(AF.getHostCard()))
        			p.addDamage(damage, AF.getHostCard());
        	}
        }
        
        Object obj = tgts.get(0);
        
        Player pl = null;
        Card c = null;
        
        if (obj instanceof Card){
        	c = (Card)obj;
        	pl = c.getController();
        }
        else{
        	pl = (Player)obj;
        }
        
        if(AF.hasSubAbility())
        	CardFactoryUtil.doDrawBack(AF.getMapParams().get("SubAbility"), damage,
                AF.getHostCard().getController(), AF.getHostCard().getController().getOpponent(),
                pl, AF.getHostCard(), c, saMe);
    }
}
