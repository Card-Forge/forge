package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class AbilityFactory_Pump {
	
	private final ArrayList<String> Keywords = new ArrayList<String>();

	private String numAttack;
	private String numDefense;
	
	private AbilityFactory AF = null;
	private HashMap<String,String> params = null;
	private Card hostCard = null;
	
	public AbilityFactory_Pump (AbilityFactory newAF){
		AF = newAF;
		
		params = AF.getMapParams();
		
		hostCard = AF.getHostCard();
		
		numAttack = (params.containsKey("NumAtt")) ? params.get("NumAtt") : "0";
		numDefense = (params.containsKey("NumDef")) ? params.get("NumDef") : "0";
		
		// Start with + sign now optional
		if (numAttack.startsWith("+"))
			numAttack = numAttack.substring(1);
		if (numDefense.startsWith("+"))
			numDefense = numDefense.substring(1);
		
		if (params.containsKey("KW"))
		{
			String tmp = params.get("KW");
			String kk[] = tmp.split(" & ");
			
			Keywords.clear();
			for (int i=0; i<kk.length; i++)
				Keywords.add(kk[i]);
		}
		else
			Keywords.add("none");
	}
	
	public SpellAbility getSpell()
	{
        SpellAbility spPump = new Spell(hostCard, AF.getAbCost(), AF.getAbTgt()) {
            private static final long serialVersionUID = 42244224L;
                        
            public boolean canPlayAI() {
            	return doTgtAI(this);
            }
            
			@Override
			public String getStackDescription(){
				return pumpStackDescription(AF, this);
			}
            
            public void resolve() {
            	doResolve(this);
            }//resolve
        };//SpellAbility
        
        return spPump;
	}

	public SpellAbility getAbility()
	{
        final SpellAbility abPump = new Ability_Activated(hostCard, AF.getAbCost(), AF.getAbTgt()) {
            private static final long serialVersionUID = -1118592153328758083L;
                        
            @Override
            public boolean canPlayAI() {
            	if (!AllZone.GameAction.isCardInPlay(hostCard)) return false;
            	
            	// temporarily disabled until AI is improved
            	if (AF.getAbCost().getSacCost()) return false;	
            	if (AF.getAbCost().getLifeCost())	 return false;
            	if (AF.getAbCost().getSubCounter()){
            		// instead of never removing counters, we will have a random possibility of failure.
            		// all the other tests still need to pass if a counter will be removed
            		Counters count = AF.getAbCost().getCounterType();
            		double chance = .66;
            		if (count.equals(Counters.P1P1)){	// 10% chance to remove +1/+1 to pump
            			chance = .1;
            		}
            		else if (count.equals(Counters.CHARGE)){ // 50% chance to remove +1/+1 to pump
            			chance = .5;
            		}
                    Random r = new Random();
                    if(r.nextFloat() > chance)
                    	return false;
            	}
            	//if (bPumpEquipped && card.getEquippingCard() == null) return false;
            	
            	if (!ComputerUtil.canPayCost(this))
            		return false;
            	
                int defense = getNumDefense(this);
                
                if(AllZone.Phase.getPhase().equals(Constant.Phase.Main2)) return false;
                
                if(AF.getAbTgt() == null || !AF.getAbTgt().doesTarget()) {
                	Card creature;
                    //if (bPumpEquipped)
                    //	creature = card.getEquippingCard();
                    //else 
                    creature = hostCard;
                    
                    if((creature.getNetDefense() + defense > 0) && (!creature.hasAnyKeyword(Keywords))) {
                    	if(creature.hasSickness() && Keywords.contains("Haste")) 
                    		return true;
                    	else if (creature.hasSickness() ^ Keywords.contains("Haste"))
                            return false;
                    	else {
                            Random r = new Random();
                            if(r.nextFloat() <= Math.pow(.6667, hostCard.getAbilityUsed())) 
                            	return CardFactoryUtil.AI_doesCreatureAttack(creature);
                        }
                    }
                }
                else
                	return doTgtAI(this);
                
                return false;
            }
            
            @Override
            public boolean canPlay() {
                return super.canPlay();
            }
                        
			@Override
			public String getStackDescription(){
				return pumpStackDescription(AF, this);
			}
            
            @Override
            public void resolve() {
                doResolve(this);
                
                hostCard.setAbilityUsed(hostCard.getAbilityUsed() + 1);
            }//resolve()
            
            
        };//SpellAbility

        return abPump;
	}
	
    private int getNumAttack(SpellAbility sa) {
    	return AbilityFactory.calculateAmount(hostCard, numAttack, sa);
    }
    
    private int getNumDefense(SpellAbility sa) {
    	return AbilityFactory.calculateAmount(hostCard, numDefense, sa);
    }

    private CardList getPumpCreatures() {
    	
        final boolean kHaste = Keywords.contains("Haste");
        final boolean kSize = Keywords.size() > 0;
        String KWpump[] = {"none"};
        if (!Keywords.get(0).equals("none"))
        	KWpump = Keywords.toArray(new String[Keywords.size()]);
        final String KWs[] = KWpump;
    	
        CardList list = new CardList(AllZone.Computer_Play.getCards());
        list = list.getType("Creature");
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                boolean hSick = c.hasSickness();
                boolean cTgt = CardFactoryUtil.canTarget(hostCard, c);
            	
                if(hSick && kHaste) 
                    return cTgt;
                    
                boolean cAtt = CardFactoryUtil.AI_doesCreatureAttack(c);
                boolean hKW = c.hasAnyKeyword(KWs);
                
                return (cAtt && cTgt && (kSize && !hKW) && !(!hSick && kHaste)); //Don't add duplicate keywords
                    
                //return false;
            }
        });
        return list;
    }
    
    private CardList getCurseCreatures(SpellAbility sa)
    {
    	final int defense = getNumDefense(sa);
    	
    	CardList list = new CardList(AllZone.Human_Play.getCards());
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) { 
                    	return CardFactoryUtil.canTarget(hostCard, c) && c.isCreature(); 
                }
        });
        
    	if (defense < 0 && !list.isEmpty()) { // with spells that give -X/-X, compi will try to destroy a creature
    		list = list.filter(new CardListFilter() {
                public boolean addCard(Card c) {
                	if (c.getNetDefense() <= -defense ) return true; // can kill indestructible creatures
                    return (c.getKillDamage() <= -defense && !c.hasKeyword("Indestructible"));
                }
        	}); // leaves all creatures that will be destroyed
    	} // -X/-X end
    	else if (!list.isEmpty()) {
            String KWpump[] = {"none"};
            if (!Keywords.get(0).equals("none"))
            	KWpump = Keywords.toArray(new String[Keywords.size()]);
            final String KWs[] = KWpump;
            final boolean addsKeywords = Keywords.size() > 0;
            
            if (addsKeywords) {
            	list = list.filter(new CardListFilter() {
            		public boolean addCard(Card c) {
            			return !c.hasAnyKeyword(KWs);    // don't add duplicate negative keywords
            		}
            	});
            }
    	}
    	
    	
    	return list;
    }

    private boolean doTgtAI(SpellAbility sa)
    {
        int defense = getNumDefense(sa);
        
        String curPhase = AllZone.Phase.getPhase();
        if(curPhase.equals(Constant.Phase.Main2) && !(AF.isCurse() && defense < 0))
        	return false;
        
		Target tgt = AF.getAbTgt();
		CardList list;
        if (AF.isCurse())  // Curse means spells with negative effect
        	list = getCurseCreatures(sa);
        else
        	list = getPumpCreatures();
		
        if (list.isEmpty())
        	return false;
        
		while(tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)){ 
			Card t = null;
			boolean goodt = false;
			
			if (list.isEmpty()){
				if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0){
					tgt.resetTargets();
					return false;
				}
				else{
					// todo is this good enough? for up to amounts?
					break;
				}
			}
			
			if (AF.isCurse()){
				t = CardFactoryUtil.AI_getBestCreature(list);
				goodt = true;
			}
			else{
        		while(!goodt && !list.isEmpty()) {
        			t = CardFactoryUtil.AI_getBestCreature(list);
        			if((t.getNetDefense() + defense) > t.getDamage()) goodt = true;
        			else list.remove(t);
        		}
	        }
	        
	        if(goodt){
	        	tgt.addTarget(t);
	        	list.remove(t);
	        }
	        
			if (list.isEmpty()){
				if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0){
					tgt.resetTargets();
					return false;
				}
				else{
					// todo is this good enough? for up to amounts?
					break;
				}
			}
		}
        
        return true;

    }
    
    private String pumpStackDescription(AbilityFactory af, SpellAbility sa){
		// when damageStackDescription is called, just build exactly what is happening
		 StringBuilder sb = new StringBuilder();
		 String name = af.getHostCard().getName();

		ArrayList<Card> tgtCards;
		Target tgt = AF.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = new ArrayList<Card>();
			tgtCards.add(hostCard);
		}
	        
		 sb.append(name).append(" - ");
		 for(Card c : tgtCards){
			 sb.append(c.getName());
			 sb.append(" ");
		 }
	     final int atk = getNumAttack(sa);
	     final int def = getNumDefense(sa);
	     
		 sb.append("gains ");
	     if (atk != 0 || def != 0){
	    	 if (atk >= 0)
	    		 sb.append("+");
	    	 sb.append(atk);
	    	 sb.append("/");
	    	 if (def >= 0)
	    		 sb.append("+");
	    	 sb.append(def);
		     sb.append(" ");
	     }

		if(Keywords.size() > 0)
		{
			for (int i=0; i<Keywords.size(); i++)
			{
				if (!Keywords.get(i).equals("none"))
					sb.append(Keywords.get(i)).append(" ");
			}
	    }
		 
		 sb.append("until end of turn.");
		 
		 
		 Ability_Sub abSub = sa.getSubAbility();
		 if (abSub != null) {
		 	abSub.setParent(sa);
		 	sb.append(abSub.getStackDescription());
		 }
		 
		 return sb.toString();
    }
    
    private void doResolve(SpellAbility sa)
    {
		ArrayList<Card> tgtCards;
		Target tgt = AF.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = new ArrayList<Card>();
			tgtCards.add(hostCard);
		}

		int size = tgtCards.size();
		for(int j = 0; j < size; j++){
			final Card tgtC = tgtCards.get(j);
			
			// only pump things in play
			if (!AllZone.GameAction.isCardInPlay(tgtC))
				continue;

			// if pump is a target, make sure we can still target now
			if (tgt != null && !CardFactoryUtil.canTarget(AF.getHostCard(), tgtC))
				continue;
			
	        final int a = getNumAttack(sa);
	        final int d = getNumDefense(sa);
	        
	        final Command untilEOT = new Command() {
	            private static final long serialVersionUID = -42244224L;
	            
	            public void execute() {
	                if(AllZone.GameAction.isCardInPlay(tgtC)) {
	                	tgtC.addTempAttackBoost(-1 * a);
	                	tgtC.addTempDefenseBoost(-1 * d);
	                    
	                    if(Keywords.size() > 0)
	                    {
	                    	for (int i=0; i<Keywords.size(); i++)
	                    	{
	                    		if (!Keywords.get(i).equals("none"))
	                    			tgtC.removeExtrinsicKeyword(Keywords.get(i));
	                    	}
	                    }
	                    	
	                }
	            }
	        };
	        
	        tgtC.addTempAttackBoost(a);
	        tgtC.addTempDefenseBoost(d);
	        if(Keywords.size() > 0)
	        {
	        	for (int i=0; i<Keywords.size(); i++)
	        	{
	        		if (!Keywords.get(i).equals("none"))
	        			tgtC.addExtrinsicKeyword(Keywords.get(i));
	        	}
	        }
	        
	        AllZone.EndOfTurn.addUntil(untilEOT);
        
		}
		
		
		
		if (AF.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
			   if (abSub.getParent() == null)
				  abSub.setParent(sa);
			   abSub.resolve();
			}
			else{
				Card first = tgtCards.get(0);
	        	CardFactoryUtil.doDrawBack(params.get("SubAbility"), 0,
	                hostCard.getController(), hostCard.getController().getOpponent(),
	                first.getController(), hostCard, first, sa);
			}
		}
        
    }
}
