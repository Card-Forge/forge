package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CombatUtil;
import forge.Command;
import forge.ComputerUtil;
import forge.Constant;
import forge.Counters;
import forge.MyRandom;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbility_Restriction;
import forge.card.spellability.Target;

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
            	return pumpPlayAI(this);
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
            	return pumpPlayAI(this);
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

			@Override
			public boolean doTrigger(boolean mandatory) {
				return pumpTriggerAI(AF, this, mandatory);
			}
            
            
        };//SpellAbility

        return abPump;
	}
	
	public SpellAbility getDrawback()
	{
        SpellAbility dbPump = new Ability_Sub(hostCard, AF.getAbTgt()) {
            private static final long serialVersionUID = 42244224L;
                        
            public boolean canPlayAI() {
            	return pumpPlayAI(this);
            }
            
			@Override
			public String getStackDescription(){
				return pumpStackDescription(AF, this);
			}
            
            public void resolve() {
            	doResolve(this);
            }//resolve

			@Override
			public boolean chkAI_Drawback() {
				return doDrawbackAI(this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return pumpTriggerAI(AF, this, mandatory);
			}
        };//SpellAbility
        
        return dbPump;
	}
	
    private int getNumAttack(SpellAbility sa) {
    	return AbilityFactory.calculateAmount(hostCard, numAttack, sa);
    }
    
    private int getNumDefense(SpellAbility sa) {
    	return AbilityFactory.calculateAmount(hostCard, numDefense, sa);
    }

    private CardList getPumpCreatures(final int defense, final int attack) {
    	
        final boolean kHaste = Keywords.contains("Haste");
        final boolean evasive = (Keywords.contains("Flying") || Keywords.contains("Horsemanship") || 
        		Keywords.contains("HIDDEN Unblockable"));
        final boolean kSize = !Keywords.get(0).equals("none");
        String KWpump[] = {"none"};
        if (!Keywords.get(0).equals("none"))
        	KWpump = Keywords.toArray(new String[Keywords.size()]);
        final String KWs[] = KWpump;
    	
        CardList list = AllZoneUtil.getCreaturesInPlay(AllZone.ComputerPlayer);
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                if (!CardFactoryUtil.canTarget(hostCard, c))
                	return false;
                
                if (c.getNetDefense() + defense <= 0) //don't kill the creature
                	return false;
                
                //Don't add duplicate keywords
                boolean hKW = c.hasAnyKeyword(KWs);
                if (kSize && hKW) return false;
            	
                //give haste to creatures that could attack with it
                if(c.hasSickness() && kHaste && AllZone.Phase.isPlayerTurn(AllZone.ComputerPlayer) && CombatUtil.canAttackNextTurn(c)
                		&& AllZone.Phase.isBefore(Constant.Phase.Combat_Declare_Attackers))
                    return true;
                
                //give evasive keywords to creatures that can attack
                if(evasive && AllZone.Phase.isPlayerTurn(AllZone.ComputerPlayer) && CombatUtil.canAttack(c) 
                		&& AllZone.Phase.isBefore(Constant.Phase.Combat_Declare_Attackers) && c.getNetCombatDamage()>0)
                    return true;
                    
                //will the creature attack (only relevant for sorcery speed)?
                if (CardFactoryUtil.AI_doesCreatureAttack(c) && AllZone.Phase.isBefore(Constant.Phase.Combat_Declare_Attackers)
                		&& AllZone.Phase.isPlayerTurn(AllZone.ComputerPlayer))
                	return true;
                
                //is the creature blocking and unable to destroy the attacker or would be destroyed itself?
                if (c.isBlocking() && (CombatUtil.blockerWouldBeDestroyed(c) 
                		|| CombatUtil.attackerWouldBeDestroyed(AllZone.Combat.getAttackerBlockedBy(c))))
                	return true;
                
                //is the creature unblocked and the spell will pump its power?
                if (AllZone.Phase.isAfter(Constant.Phase.Combat_Declare_Blockers) && AllZone.Combat.isAttacking(c)
                		&& AllZone.Combat.isUnblocked(c) && attack > 0)
                	return true;
                
                //is the creature in blocked and the blocker would survive
                if (AllZone.Phase.isAfter(Constant.Phase.Combat_Declare_Blockers) && AllZone.Combat.isAttacking(c)
                		&& AllZone.Combat.isBlocked(c)
                		&& CombatUtil.blockerWouldBeDestroyed(AllZone.Combat.getBlockers(c).get(0)))
                	return true;
                
                //if the life of the computer is in danger, try to pump potential blockers before declaring blocks
                if (CombatUtil.lifeInDanger(AllZone.Combat) && AllZone.Phase.isAfter(Constant.Phase.Combat_Declare_Attackers) 
                		&& CombatUtil.canBlock(c, AllZone.Combat) && AllZone.Phase.isPlayerTurn(AllZone.HumanPlayer))
                	return true;
                
                return false; 
            }
        });
        return list;
    }
    
    private CardList getCurseCreatures(SpellAbility sa, final int defense, int attack)
    {
    	CardList list = AllZoneUtil.getCreaturesInPlay(AllZone.HumanPlayer);
    	list = list.filter(AllZoneUtil.getCanTargetFilter(hostCard));
        
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
    
    private boolean pumpPlayAI(SpellAbility sa){
    	// if there is no target and host card isn't in play, don't activate
    	if (AF.getAbTgt() == null && !AllZoneUtil.isCardInPlay(hostCard)) 
    		return false;
    	
    	// temporarily disabled until AI is improved
    	if (AF.getAbCost().getSacCost() && sa.getSourceCard().isCreature()) return false;	
    	if (AF.getAbCost().getLifeCost()) {
    		if (!AF.isCurse()) return false; //Use life only to kill creatures
    		if (AllZone.ComputerPlayer.getLife() - AF.getAbCost().getLifeAmount() < 4)
				return false;
    	}
    	if (AF.getAbCost().getSubCounter()){
    		// instead of never removing counters, we will have a random possibility of failure.
    		// all the other tests still need to pass if a counter will be removed
    		Counters count = AF.getAbCost().getCounterType();
    		double chance = .66;
    		if (count.equals(Counters.P1P1)){	// 10% chance to remove +1/+1 to pump
    			chance = .1;
    		}
    		else if (count.equals(Counters.CHARGE)){ // 50% chance to remove charge to pump
    			chance = .5;
    		}
            Random r = MyRandom.random;
            if(r.nextFloat() > chance)
            	return false;
    	}
    	
    	if (!ComputerUtil.canPayCost(sa))
    		return false;
    	
    	SpellAbility_Restriction restrict = sa.getRestrictions();
    	
    	// Phase Restrictions
    	if (AllZone.Stack.size() == 0 && AllZone.Phase.isBefore(Constant.Phase.Combat_Begin)){
    		// Instant-speed pumps should not be cast outside of combat when the stack is empty
    		if (!AF.isCurse()){
    			if (!AbilityFactory.isSorcerySpeed(sa))
    				return false;
    		}
    	}
    	else if (AllZone.Stack.size() > 0){
    		// TODO: pump something only if the top thing on the stack will kill it via damage
    		// or if top thing on stack will pump it/enchant it and I want to kill it
    		return false;
    	}
    	
    	int activations = restrict.getNumberTurnActivations();
    	int sacActivations = restrict.getActivationNumberSacrifice();
    	//don't risk sacrificing a creature just to pump it
    	if(sacActivations != -1 && activations >= (sacActivations - 1)) {
    		return false;
		}
    	
    	Card source = sa.getSourceCard();
    	if (source.getSVar("X").equals("Count$xPaid"))
    		source.setSVar("PayX", "");
    	
    	int defense;
		if (numDefense.contains("X") && source.getSVar("X").equals("Count$xPaid")){
			// Set PayX here to maximum value.
			int xPay = ComputerUtil.determineLeftoverMana(sa);
			source.setSVar("PayX", Integer.toString(xPay));
			defense = xPay;
		}
		else
			defense = getNumDefense(sa);

    	int attack;
		if (numAttack.contains("X") && source.getSVar("X").equals("Count$xPaid")){
			// Set PayX here to maximum value.
			String toPay = source.getSVar("PayX");
			
			if (toPay.equals("")){
				int xPay = ComputerUtil.determineLeftoverMana(sa);
				source.setSVar("PayX", Integer.toString(xPay));
				attack = xPay;
			}
			else
				attack = Integer.parseInt(toPay);
		}
		else
			attack = getNumAttack(sa);
        
        if(AF.getAbTgt() == null || !AF.getAbTgt().doesTarget()) {
        	ArrayList<Card> cards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);

        	if (cards.size() == 0)
        		return false;

        	// when this happens we need to expand AI to consider if its ok for everything?
			for(Card card : cards){      
				// TODO: if AI doesn't control Card and Pump is a Curse, than maybe use? 
	            if((card.getNetDefense() + defense > 0) && (!card.hasAnyKeyword(Keywords))) {
	            	if(card.hasSickness() && Keywords.contains("Haste")) 
	            		return true;
	            	else if (card.hasSickness() ^ Keywords.contains("Haste"))
	                    return false;
	            	else if (hostCard.equals(card)){
	                    Random r = MyRandom.random;
	                    if(r.nextFloat() <= Math.pow(.6667, activations)) 
	                    	return CardFactoryUtil.AI_doesCreatureAttack(card) && !sa.getPayCosts().getTap();
	                }
	            	else{
	            		Random r = MyRandom.random;
	                    return (r.nextFloat() <= Math.pow(.6667, activations));
	            	}
	            }
			}
        }
        else
        	return doTgtAI(sa, defense, attack, false);
        
        return false;
    }

    private boolean doTgtAI(SpellAbility sa, int defense, int attack, boolean mandatory)
    {
        if(!mandatory && AllZone.Phase.isAfter(Constant.Phase.Combat_Declare_Blockers_InstantAbility) && !(AF.isCurse() && defense < 0))
        	return false;
        
		Target tgt = AF.getAbTgt();
		tgt.resetTargets();
		CardList list;
        if (AF.isCurse())  // Curse means spells with negative effect
        	list = getCurseCreatures(sa, defense, attack);
        else
        	list = getPumpCreatures(defense, attack);
		
        list = list.getValidCards(tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());
        
        if (AllZone.Stack.size() == 0){
        	// If the cost is tapping, don't activate before declare attack/block
        	if (sa.getPayCosts() != null && sa.getPayCosts().getTap()){
	        	if (AllZone.Phase.isBefore(Constant.Phase.Combat_Declare_Attackers) && AllZone.Phase.isPlayerTurn(AllZone.ComputerPlayer))
	        		list.remove(sa.getSourceCard());
	        	if (AllZone.Phase.isBefore(Constant.Phase.Combat_Declare_Blockers) && AllZone.Phase.isPlayerTurn(AllZone.HumanPlayer))
	        		list.remove(sa.getSourceCard());
        	}
        }
        
        if (list.isEmpty())
        	return mandatory && pumpMandatoryTarget(AF, sa, mandatory);
        
		while(tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)){ 
			Card t = null;
			//boolean goodt = false;
			
			if (list.isEmpty()){
				if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0){
					if (mandatory)
						return pumpMandatoryTarget(AF, sa, mandatory);
					
					tgt.resetTargets();
					return false;
				}
				else{
					// TODO is this good enough? for up to amounts?
					break;
				}
			}
			
			/*Not needed
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
	        }*/
	        
			t = CardFactoryUtil.AI_getBestCreature(list);
	        tgt.addTarget(t);
	        list.remove(t);
		}
        
        return true;
    }
    
    private boolean pumpMandatoryTarget(AbilityFactory af, SpellAbility sa, boolean mandatory){
    	CardList list = AllZoneUtil.getCardsInPlay();
    	Target tgt = sa.getTarget();
    	list = list.getValidCards(tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());
    	
    	if (list.size() < tgt.getMinTargets(sa.getSourceCard(), sa)){
    		tgt.resetTargets();
    		return false;
    	}
    	
    	// Remove anything that's already been targeted
    	for(Card c : tgt.getTargetCards())
    		list.remove(c);
    	
    	CardList pref;
    	CardList forced;
    	Card source = sa.getSourceCard();
    	
    	if (af.isCurse()){
    		pref = list.getController(AllZone.HumanPlayer);
    		forced = list.getController(AllZone.ComputerPlayer);
    	}
    	else{
    		pref = list.getController(AllZone.ComputerPlayer);
    		forced = list.getController(AllZone.HumanPlayer);
    	}
    	
    	while(tgt.getNumTargeted() < tgt.getMaxTargets(source, sa)){
    		if (pref.isEmpty())
    			break;
    		
    		Card c;
    		if (pref.getNotType("Creature").size() == 0)
    			c = CardFactoryUtil.AI_getBestCreature(pref);
    		else
    			c = CardFactoryUtil.AI_getMostExpensivePermanent(pref, source, true);
    		
    		pref.remove(c);
    		
    		tgt.addTarget(c);
    	}
    	
    	while(tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)){
    		if (forced.isEmpty())
    			break;
    		
    		Card c;
    		if (forced.getNotType("Creature").size() == 0)
    			c = CardFactoryUtil.AI_getWorstCreature(forced);
    		else
    			c = CardFactoryUtil.AI_getCheapestPermanent(forced, source, true);
    		
    		forced.remove(c);
    		
    		tgt.addTarget(c);
    	}
    	
    	if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)){
    		tgt.resetTargets();
    		return false;
    	}
    	
    	return true;
    }
    
    
    private boolean pumpTriggerAI(AbilityFactory af, SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa))
			return false;
    	
		Card source = sa.getSourceCard();
		
    	int defense;
		if (numDefense.contains("X") && source.getSVar("X").equals("Count$xPaid")){
			// Set PayX here to maximum value.
			int xPay = ComputerUtil.determineLeftoverMana(sa);
			source.setSVar("PayX", Integer.toString(xPay));
			defense = xPay;
		}
		else
			defense = getNumDefense(sa);

    	int attack;
		if (numAttack.contains("X") && source.getSVar("X").equals("Count$xPaid")){
			// Set PayX here to maximum value.
			String toPay = source.getSVar("PayX");
			
			if (toPay.equals("")){
				int xPay = ComputerUtil.determineLeftoverMana(sa);
				source.setSVar("PayX", Integer.toString(xPay));
				attack = xPay;
			}
			else
				attack = Integer.parseInt(toPay);
		}
		else
			attack = getNumAttack(sa);
		
    	if (sa.getTarget() == null){
    		if (mandatory)
    			return true;
    	}
    	else{
    		return doTgtAI(sa, defense, attack, mandatory);
    	}
    	
    	return true;
    }
    
    private boolean doDrawbackAI(SpellAbility sa)
    {
    	Card source = sa.getSourceCard();
    	int defense;
		if (numDefense.contains("X") && source.getSVar("X").equals("Count$xPaid")){
			defense = Integer.parseInt(source.getSVar("PayX"));
		}
		else
			defense = getNumDefense(sa);

    	int attack;
		if (numAttack.contains("X") && source.getSVar("X").equals("Count$xPaid")){
			attack = Integer.parseInt(source.getSVar("PayX"));
		}
		else
			attack = getNumAttack(sa);
    	
    	 if(AF.getAbTgt() == null || !AF.getAbTgt().doesTarget()) {
    		 if (hostCard.isCreature()){
    			 if (!hostCard.hasKeyword("Indestructible") && hostCard.getNetDefense() + defense <= hostCard.getDamage())
    				 return false;
    			 if (hostCard.getNetDefense() + defense <= 0)
    				 return false;
    		 }
    	 }
    	 else
    		 return doTgtAI(sa, defense, attack, false);
    	 
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
		else
			tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
		
		if(tgtCards.size() > 0) {

			if (sa instanceof Ability_Sub)
				sb.append(" ");
			else
				sb.append(name).append(" - ");

			for(Card c : tgtCards)
				sb.append(c.getName()).append(" ");

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

			for (int i=0; i<Keywords.size(); i++)
			{
				if (!Keywords.get(i).equals("none"))
					sb.append(Keywords.get(i)).append(" ");
			}

			if (!params.containsKey("Permanent"))
				sb.append("until end of turn.");
		}
		
		 Ability_Sub abSub = sa.getSubAbility();
		 if (abSub != null) {
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
		else
			tgtCards = AbilityFactory.getDefinedCards(hostCard, params.get("Defined"), sa);

		int size = tgtCards.size();
		for(int j = 0; j < size; j++){
			final Card tgtC = tgtCards.get(j);
			
			// only pump things in play
			if (!AllZoneUtil.isCardInPlay(tgtC))
				continue;

			// if pump is a target, make sure we can still target now
			if (tgt != null && !CardFactoryUtil.canTarget(AF.getHostCard(), tgtC))
				continue;
			
	        final int a = getNumAttack(sa);
	        final int d = getNumDefense(sa);
    
	        tgtC.addTempAttackBoost(a);
	        tgtC.addTempDefenseBoost(d);

        	for (int i=0; i<Keywords.size(); i++)
        	{
        		if (!Keywords.get(i).equals("none"))
        			tgtC.addExtrinsicKeyword(Keywords.get(i));
        	}

	        if (!params.containsKey("Permanent")){
	        	// If not Permanent, remove Pumped at EOT
		        final Command untilEOT = new Command() {
		            private static final long serialVersionUID = -42244224L;
		            
		            public void execute() {
		                if(AllZoneUtil.isCardInPlay(tgtC)) {
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
		        
		        AllZone.EndOfTurn.addUntil(untilEOT);
	        }
		}
		
		if (AF.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
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
    
    
    /////////////////////////////////////
    //
    // PumpAll
    //
    //////////////////////////////////////
    
    public SpellAbility getPumpAllAbility() {
        final SpellAbility abPumpAll = new Ability_Activated(hostCard, AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = -8299417521903307630L;

			@Override
            public boolean canPlayAI() {
            	return pumpAllCanPlayAI(this);
            }
            
			@Override
			public String getStackDescription(){
				return pumpAllStackDescription(AF, this);
			}
            
            @Override
            public void resolve() {
                doPumpAllResolve(this);
                hostCard.setAbilityUsed(hostCard.getAbilityUsed() + 1);
            }//resolve()


			@Override
			public boolean doTrigger(boolean mandatory) {
				return pumpAllTriggerAI(AF, this, mandatory);
			}
            
        };//SpellAbility

        return abPumpAll;
	}
    
    public SpellAbility getPumpAllSpell() {
    	SpellAbility spPumpAll = new Spell(hostCard, AF.getAbCost(), AF.getAbTgt()) {
    		private static final long serialVersionUID = -4055467978660824703L;

    		public boolean canPlayAI() {
    			return pumpAllCanPlayAI(this);
    		}
    		
			@Override
			public String getStackDescription(){
				return pumpAllStackDescription(AF, this);
			}

    		public void resolve() {
    			doPumpAllResolve(this);
    		}//resolve
    	};//SpellAbility

    	return spPumpAll;
    }
    
    public SpellAbility getPumpAllDrawback() {
        SpellAbility dbPumpAll = new Ability_Sub(hostCard, AF.getAbTgt()) {
			private static final long serialVersionUID = 6411531984691660342L;

			@Override
			public String getStackDescription(){
				return pumpAllStackDescription(AF, this);
			}
            
            public void resolve() {
            	doPumpAllResolve(this);
            }//resolve

			@Override
			public boolean chkAI_Drawback() {
				return chkPumpAllDrawbackAI(this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return pumpAllTriggerAI(AF, this, mandatory);
			}
        };//SpellAbility
        
        return dbPumpAll;
	}
    
    private boolean pumpAllCanPlayAI(SpellAbility sa) {
    	String valid = "";
    	
    	if(params.containsKey("ValidCards")) {
			valid = params.get("ValidCards");
    	}
    	
    	CardList comp = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
    	comp = comp.getValidCards(valid, hostCard.getController(), hostCard);
    	CardList human = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
    	human = human.getValidCards(valid,hostCard.getController(), hostCard);
    	
    	if(AF.isCurse()) {
    		return human.size() > comp.size();
    	}
    	else return comp.size() > human.size();
    }
    
    private void doPumpAllResolve(SpellAbility sa) {
    	String valid = "";

		if(params.containsKey("ValidCards")) 
			valid = params.get("ValidCards");
    	
    	CardList list = AllZoneUtil.getCardsInPlay();
		list = list.getValidCards(valid.split(","), hostCard.getController(), hostCard);

		for(Card c:list){
			final Card tgtC = c;
			
			// only pump things in play
			if (!AllZoneUtil.isCardInPlay(tgtC))
				continue;
			
	        final int a = getNumAttack(sa);
	        final int d = getNumDefense(sa);
    
	        tgtC.addTempAttackBoost(a);
	        tgtC.addTempDefenseBoost(d);

        	for (int i=0; i<Keywords.size(); i++) {
        		if (!Keywords.get(i).equals("none"))
        			tgtC.addExtrinsicKeyword(Keywords.get(i));
        	}

	        if (!params.containsKey("Permanent")){
	        	// If not Permanent, remove Pumped at EOT
		        final Command untilEOT = new Command() {
					private static final long serialVersionUID = 5415795460189457660L;

					public void execute() {
		                if(AllZoneUtil.isCardInPlay(tgtC)) {
		                	tgtC.addTempAttackBoost(-1 * a);
		                	tgtC.addTempDefenseBoost(-1 * d);
		                    
		                    if(Keywords.size() > 0) {
		                    	for (int i=0; i<Keywords.size(); i++) {
		                    		if (!Keywords.get(i).equals("none")) {
		                    			tgtC.removeExtrinsicKeyword(Keywords.get(i));
		                    		}
		                    	}
		                    }
		                }
		            }
		        };
		        
		        AllZone.EndOfTurn.addUntil(untilEOT);
	        }
		}
		
		if (AF.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
			   abSub.resolve();
			}
			else{
				Card first = list.get(0);
	        	CardFactoryUtil.doDrawBack(params.get("SubAbility"), 0,
	                hostCard.getController(), hostCard.getController().getOpponent(),
	                first.getController(), hostCard, first, sa);
			}
		}
        
    }
    
    private boolean pumpAllTriggerAI(AbilityFactory af, SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		// TODO: add targeting consideration such as "Creatures target player controls gets"
    	
    	return true;
    }

    private boolean chkPumpAllDrawbackAI(SpellAbility sa) {
    	return true;
    }
    
    private String pumpAllStackDescription(AbilityFactory af, SpellAbility sa){
    	StringBuilder sb = new StringBuilder();

    	String desc = "";
    	if(params.containsKey("SpellDescription")) {
    		desc = params.get("SpellDescription");
    	}
    	else if(params.containsKey("PumpAllDescription")) {
    		desc = params.get("PumpAllDescription");
    	}

    	/*
    	if (sa instanceof Ability_Sub)
    		sb.append(" ").append(desc);
    	sb.append(name).append(" -");
    	*/
    	
    	if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");
    	sb.append(desc);
    	

    	Ability_Sub abSub = sa.getSubAbility();
    	if (abSub != null) {
    		sb.append(abSub.getStackDescription());
    	}

    	return sb.toString();
    }
}
