package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class AbilityFactory_Pump {
	
	private final int NumAttack[] = {-1138};
	private final int NumDefense[] = {-1138};
	private final String AttackX[] = {"none"};
	private final String DefenseX[] = {"none"};
	private final ArrayList<String> Keywords = new ArrayList<String>();

	private AbilityFactory AF = null;
	private HashMap<String,String> params = null;
	private Card hostCard = null;
	
	public AbilityFactory_Pump (AbilityFactory newAF){
		AF = newAF;
		
		params = AF.getMapParams();
		
		hostCard = AF.getHostCard();
		
		if (params.containsKey("NumAtt"))
		{
			String tmp = params.get("NumAtt");
            if(tmp.matches("[\\+\\-][XY]"))
            {
                String xy = hostCard.getSVar(tmp.replaceAll("[\\+\\-]", ""));
                if(xy.startsWith("Count$")) {
                    String kk[] = xy.split("\\$");
                    AttackX[0] = kk[1];
                    
                    if(tmp.contains("-"))
                    {
                    	if(AttackX[0].contains("/"))
                    		AttackX[0] = AttackX[0].replace("/", "/Negative");
                    	else 
                    		AttackX[0] += "/Negative";
                    }
                }
            } 
            else if(tmp.matches("[\\+\\-][0-9]"))
            	NumAttack[0] = Integer.parseInt(tmp.replace("+", ""));
            
		}
		
		if (params.containsKey("NumDef"))
		{
			String tmp = params.get("NumDef");
            if(tmp.matches("[\\+\\-][XY]"))
            {
                String xy = hostCard.getSVar(tmp.replaceAll("[\\+\\-]", ""));
                if(xy.startsWith("Count$")) {
                    String kk[] = xy.split("\\$");
                    DefenseX[0] = kk[1];
                    
                    if(tmp.contains("-"))
                    {
                    	if(DefenseX[0].contains("/"))
                    		DefenseX[0] = DefenseX[0].replace("/", "/Negative");
                    	else 
                    		DefenseX[0] += "/Negative";
                    }
                }
            } 
            else if(tmp.matches("[\\+\\-][0-9]"))
            	NumDefense[0] = Integer.parseInt(tmp.replace("+", ""));
            
		}
		
		Keywords.add("none");
		if (params.containsKey("KW"))
		{
			String tmp = params.get("KW");
			String kk[] = tmp.split(" & ");
			
			Keywords.clear();
			for (int i=0; i<kk.length; i++)
				Keywords.add(kk[i]);
		}
	}
	
	public SpellAbility getSpell()
	{
        SpellAbility spPump = new Spell(hostCard, AF.getAbCost(), AF.getAbTgt()) {
            private static final long serialVersionUID = 42244224L;
                        
            public boolean canPlayAI() {
            	return doTgtAI(this);
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
            		if (count.equals("P1P1")){	// 10% chance to remove +1/+1 to pump
            			chance = .1;
            		}
            		else if (count.equals("CHARGE")){ // 50% chance to remove +1/+1 to pump
            			chance = .5;
            		}
                    Random r = new Random();
                    if(r.nextFloat() > chance)
                    	return false;
            	}
            	//if (bPumpEquipped && card.getEquippingCard() == null) return false;
            	
            	if (!ComputerUtil.canPayCost(this))
            		return false;
            	
                int defense = getNumDefense();
                
                if(AllZone.Phase.getPhase().equals(Constant.Phase.Main2)) return false;
                
                if(!AF.getAbTgt().doesTarget()) {
                	Card creature;
                    //if (bPumpEquipped)
                    //	creature = card.getEquippingCard();
                    //else 
                    creature = hostCard;
                    
                    if((creature.getNetDefense() + defense > 0) && (!creature.hasAnyKeyword((String[]) Keywords.toArray()))) {
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
                return (Cost_Payment.canPayAdditionalCosts(AF.getAbCost(), this) && CardFactoryUtil.canUseAbility(hostCard) && super.canPlay());
            }
                        
            @Override
            public void resolve() {
//                final Card[] creature = new Card[1];
//                if(abTgt.doesTarget()) 
//                	creature[0] = getTargetCard();
//                else if (bPumpEquipped)
//                	creature[0] = card.getEquippingCard();
//                else 
//                	creature[0] = card;

                doResolve(this);
                
                hostCard.setAbilityUsed(hostCard.getAbilityUsed() + 1);
            }//resolve()
            
            
        };//SpellAbility

        return abPump;
	}
	
    private int getNumAttack() {
        if(NumAttack[0] != -1138)
        	return NumAttack[0];
        
        if(!AttackX[0].equals("none"))
        	return CardFactoryUtil.xCount(hostCard, AttackX[0]);
        
        return 0;
    }
    
    private int getNumDefense() {
        if(NumDefense[0] != -1138)
        	return NumDefense[0];
        
        if(!DefenseX[0].equals("none"))
        	return CardFactoryUtil.xCount(hostCard, DefenseX[0]);
        
        return 0;
    }

    private CardList getPumpCreatures() {
        CardList list = new CardList(AllZone.Computer_Play.getCards());
        list = list.getType("Creature");
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                boolean hSick = c.hasSickness();
                boolean kHaste = Keywords.contains("Haste");
                boolean cTgt = CardFactoryUtil.canTarget(hostCard, c);
            	
                if(hSick && kHaste) 
                    return cTgt;
                    
                boolean cAtt = CardFactoryUtil.AI_doesCreatureAttack(c); 
                boolean kSize = Keywords.size() > 0;
                String KWs[] = {"none"};
                if (!Keywords.get(0).equals("none"))
                	KWs = (String[]) Keywords.toArray();
                
                boolean hKW = c.hasAnyKeyword(KWs);
                return (cAtt && cTgt && (kSize && !hKW) && !(!hSick && kHaste));
                    
                //return false;
            }
        });
        return list;
    }
    
    private CardList getCurseCreatures()
    {
    	final int defense = getNumDefense();
    	
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
    	
    	return list;
    }

    private boolean doTgtAI(SpellAbility sa)
    {
        int defense = getNumDefense();
        
        String curPhase = AllZone.Phase.getPhase();
        if(curPhase.equals(Constant.Phase.Main2) && !(AF.isCurse() && defense < 0))
        	return false;
        
		boolean goodt = false;
		Card t = new Card();
		
        if (AF.isCurse()) {  // Curse means spells with negative effect
        	CardList list = getCurseCreatures();
        	if (!list.isEmpty()) {
        		t = CardFactoryUtil.AI_getBestCreature(list);
        		goodt = true;
        	}
        }
        else { // no Curse means spell with positive effect
        	CardList list = getPumpCreatures();
        	if(!list.isEmpty()) {
        		while(goodt == false && !list.isEmpty()) {
        			t = CardFactoryUtil.AI_getBestCreature(list);
        			if((t.getNetDefense() + defense) > 0) goodt = true;
        			else list.remove(t);
        		}
            }
        }
        if(goodt == true) {
                sa.setTargetCard(t);
                return true;  
        }
        
        return false;

    }
    
    private void doResolve(SpellAbility sa)
    {
        Card tgtCard = sa.getTargetCard();
        
        final Card[] creature = new Card[1];
        if (AF.isTargeted())
        {
        	if (!CardFactoryUtil.canTarget(hostCard, tgtCard))
        		return;
        	else
        		creature[0] = tgtCard;
        }
        //else if equipped
        
        else
        	creature[0] = hostCard;

        if(!AllZone.GameAction.isCardInPlay(creature[0])) 
        	return;
        
        final int a = getNumAttack();
        final int d = getNumDefense();
        
        final Command untilEOT = new Command() {
            private static final long serialVersionUID = -42244224L;
            
            public void execute() {
                if(AllZone.GameAction.isCardInPlay(creature[0])) {
                    creature[0].addTempAttackBoost(-1 * a);
                    creature[0].addTempDefenseBoost(-1 * d);
                    
                    if(Keywords.size() > 0)
                    {
                    	for (int i=0; i<Keywords.size(); i++)
                    	{
                    		if (!Keywords.get(i).equals("none"))
                    			creature[0].removeExtrinsicKeyword(Keywords.get(i));
                    	}
                    }
                    	
                }
            }
        };
        
        creature[0].addTempAttackBoost(a);
        creature[0].addTempDefenseBoost(d);
        if(Keywords.size() > 0)
        {
        	for (int i=0; i<Keywords.size(); i++)
        	{
        		if (!Keywords.get(i).equals("none"))
        			creature[0].addExtrinsicKeyword(Keywords.get(i));
        	}
        }
        
        AllZone.EndOfTurn.addUntil(untilEOT);
        
        if(AF.hasSubAbility()) 
        	CardFactoryUtil.doDrawBack(params.get("SubAbility"), 0,
                hostCard.getController(), hostCard.getController().getOpponent(),
                creature[0].getController(), hostCard, creature[0], sa);
        
    }
}
