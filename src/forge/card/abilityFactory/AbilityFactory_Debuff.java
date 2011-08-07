package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
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

public class AbilityFactory_Debuff {
	// *************************************************************************
	// ***************************** Debuff ************************************
	// *************************************************************************

	public static SpellAbility createAbilityDebuff(final AbilityFactory af) {
		final SpellAbility abDebuff = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = 3536198601841771383L;

			@Override
			public String getStackDescription() {
				return debuffStackDescription(af, this);
			}

			@Override
			public boolean canPlayAI() {
				return debuffCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				debuffResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return debuffTriggerAI(af, this, mandatory);
			}

		};
		return abDebuff;
	}

	public static SpellAbility createSpellDebuff(final AbilityFactory af) {
		final SpellAbility spDebuff = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = -54573740774322697L;

			@Override
			public String getStackDescription() {
				return debuffStackDescription(af, this);
			}

			@Override
			public boolean canPlayAI() {
				return debuffCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				debuffResolve(af, this);
			}

		};
		return spDebuff;
	}

	public static SpellAbility createDrawbackDebuff(final AbilityFactory af) {
		final SpellAbility dbDebuff = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
			private static final long serialVersionUID = -4728590185604233229L;

			@Override
			public String getStackDescription() {
				return debuffStackDescription(af, this);
			}

			@Override
			public void resolve() {
				debuffResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return debuffDrawbackAI(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return debuffTriggerAI(af, this, mandatory);
			}

		};
		return dbDebuff;
	}

	private static ArrayList<String> getKeywords(HashMap<String,String> params) {
		ArrayList<String> kws = new ArrayList<String>();
		if(params.containsKey("Keywords")) {
			kws.addAll(Arrays.asList(params.get("Keywords").split(" & ")));
		}
		return kws;
	}

	private static String debuffStackDescription(AbilityFactory af, SpellAbility sa) {
		HashMap<String,String> params = af.getMapParams();
		Card host = af.getHostCard();
		ArrayList<String> kws = getKeywords(params);
		StringBuilder sb = new StringBuilder();

		ArrayList<Card> tgtCards;
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else
			tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);

		if(tgtCards.size() > 0) {
			if (sa instanceof Ability_Sub)
				sb.append(" ");
			else
				sb.append(host).append(" - ");

			Iterator<Card> it = tgtCards.iterator();
			while(it.hasNext()) {
				Card tgtC = it.next();
				if(tgtC.isFaceDown()) sb.append("Morph");
				else sb.append(tgtC);

				if(it.hasNext()) sb.append(" ");
			}
			sb.append(" loses ");
			/*
			Iterator<String> kwit = kws.iterator();
			while(it.hasNext()) {
				String kw = kwit.next();
				sb.append(kw);
				if(it.hasNext()) sb.append(" ");
			}*/
			sb.append(kws);
			if(!params.containsKey("Permanent")) {
				sb.append(" until end of turn");
			}
			sb.append(".");
		}

		Ability_Sub abSub = sa.getSubAbility();
		if(abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}

	private static boolean debuffCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
		// if there is no target and host card isn't in play, don't activate
    	if (af.getAbTgt() == null && !AllZoneUtil.isCardInPlay(af.getHostCard())) 
    		return false;
    	
    	// temporarily disabled until AI is improved
    	if (af.getAbCost().getSacCost() && sa.getSourceCard().isCreature()) return false;	
    	if (af.getAbCost().getLifeCost()) {
				return false;
    	}
    	if (af.getAbCost().getSubCounter()){
    		// instead of never removing counters, we will have a random possibility of failure.
    		// all the other tests still need to pass if a counter will be removed
    		Counters count = af.getAbCost().getCounterType();
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
    	
    	HashMap<String,String> params = af.getMapParams();
    	SpellAbility_Restriction restrict = sa.getRestrictions();
    	
    	// Phase Restrictions
    	if (AllZone.Stack.size() == 0 && AllZone.Phase.isBefore(Constant.Phase.Combat_Begin)){
    		// Instant-speed pumps should not be cast outside of combat when the stack is empty
    		if (!AbilityFactory.isSorcerySpeed(sa))
    			return false;
    	}
    	
    	int activations = restrict.getNumberTurnActivations();
    	int sacActivations = restrict.getActivationNumberSacrifice();
    	//don't risk sacrificing a creature just to pump it
    	if(sacActivations != -1 && activations >= (sacActivations - 1)) {
    		return false;
		}
        
        if(af.getAbTgt() == null || !af.getAbTgt().doesTarget()) {
        	ArrayList<Card> cards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
        	if (cards.size() == 0)
        		return false;
        }
        else
        	return debuffTgtAI(af, sa, getKeywords(params), false);
        
        return false;
	}

	private static boolean debuffDrawbackAI(AbilityFactory af, SpellAbility sa) {
		HashMap<String,String> params = af.getMapParams();
		if(af.getAbTgt() == null || !af.getAbTgt().doesTarget()) {
			//TODO - copied from AF_Pump.pumpDrawbackAI() - what should be here?
		}
		else
			return debuffTgtAI(af, sa, getKeywords(params), false);

		return true; 
	}//debuffDrawbackAI()

	private static boolean debuffTgtAI(AbilityFactory af, SpellAbility sa, ArrayList<String> kws, boolean mandatory) {
		//this would be for evasive things like Flying, Unblockable, etc
		if(!mandatory && AllZone.Phase.isAfter(Constant.Phase.Combat_Declare_Blockers_InstantAbility))
			return false;

		Target tgt = af.getAbTgt();
		tgt.resetTargets();
		CardList list = getCurseCreatures(af, sa, kws);
		list = list.getValidCards(tgt.getValidTgts(), sa.getActivatingPlayer(), sa.getSourceCard());

		//several uses here:
		//1. make human creatures lose evasion when they are attacking
		//2. make human creatures lose Flying/Horsemanship/Shadow/etc. when Comp is attacking
		//3. remove Indestructible keyword so it can be destroyed?
		//3a. remove Persist?

		if (list.isEmpty())
			return mandatory && debuffMandatoryTarget(af, sa, mandatory);

		while(tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)){ 
			Card t = null;
			//boolean goodt = false;

			if (list.isEmpty()){
				if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0){
					if (mandatory)
						return debuffMandatoryTarget(af, sa, mandatory);

					tgt.resetTargets();
					return false;
				}
				else{
					// TODO is this good enough? for up to amounts?
					break;
				}
			}

			t = CardFactoryUtil.AI_getBestCreature(list);
			tgt.addTarget(t);
			list.remove(t);
		}

		return true;
	}//pumpTgtAI()

	private static CardList getCurseCreatures(AbilityFactory af, SpellAbility sa, final ArrayList<String> kws) {
		Card hostCard = af.getHostCard();
		CardList list = AllZoneUtil.getCreaturesInPlay(AllZone.HumanPlayer);
		list = list.filter(AllZoneUtil.getCanTargetFilter(hostCard));

		if (!list.isEmpty()) {
			list = list.filter(new CardListFilter() {
				public boolean addCard(Card c) {
					return c.hasAnyKeyword(kws);    // don't add duplicate negative keywords
				}
			});
		}

		return list;
	}//getCurseCreatures()

	private static boolean debuffMandatoryTarget(AbilityFactory af, SpellAbility sa, boolean mandatory){
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

		CardList pref = list.getController(AllZone.HumanPlayer);
		CardList forced = list.getController(AllZone.ComputerPlayer);
		Card source = sa.getSourceCard();

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

			//TODO - if forced targeting, just pick something without the given keyword
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
	}//pumpMandatoryTarget()

	private static boolean debuffTriggerAI(final AbilityFactory af, final SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa))
			return false;

		HashMap<String,String> params = af.getMapParams();

		ArrayList<String> kws = getKeywords(params);

		if (sa.getTarget() == null) {
			if (mandatory)
				return true;
		}
		else{
			return debuffTgtAI(af, sa, kws, mandatory);
		}

		return true;
	}

	private static void debuffResolve(final AbilityFactory af, final SpellAbility sa) {
		HashMap<String,String> params = af.getMapParams();
		Card host = af.getHostCard();

		ArrayList<String> kws = getKeywords(params);

		ArrayList<Card> tgtCards;
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else
			tgtCards = AbilityFactory.getDefinedCards(host, params.get("Defined"), sa);

		for(final Card tgtC : tgtCards) {
			final ArrayList<String> hadIntrinsic = new ArrayList<String>();
			if(AllZoneUtil.isCardInPlay(tgtC) && CardFactoryUtil.canTarget(host, tgtC)) {
				for(String kw : kws) {
					if(tgtC.getIntrinsicKeyword().contains(kw)) hadIntrinsic.add(kw);
					tgtC.removeIntrinsicKeyword(kw);
					tgtC.removeExtrinsicKeyword(kw);
				}
			}
			if(!params.containsKey("Permanent")) {
				AllZone.EndOfTurn.addUntil(new Command() {
					private static final long serialVersionUID = 5387486776282932314L;

					public void execute() {
						if(AllZoneUtil.isCardInPlay(tgtC)){
							for(String kw : hadIntrinsic) {
								tgtC.addIntrinsicKeyword(kw);
							}
						}
					}
				});
			}
		}

	}//debuffResolve
	
	
	// *************************************************************************
	// ***************************** DebuffAll *********************************
	// *************************************************************************
	
	public static SpellAbility createAbilityDebuffAll(final AbilityFactory af) {
        final SpellAbility abDebuffAll = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = -1977027530713097149L;

			@Override
            public boolean canPlayAI() {
            	return debuffAllCanPlayAI(af, this);
            }
            
			@Override
			public String getStackDescription(){
				return debuffAllStackDescription(af, this);
			}
            
            @Override
            public void resolve() {
                debuffAllResolve(af, this);
            }

			@Override
			public boolean doTrigger(boolean mandatory) {
				return debuffAllTriggerAI(af, this, mandatory);
			}
            
        };//SpellAbility

        return abDebuffAll;
	}
    
    public static SpellAbility createSpellDebuffAll(final AbilityFactory af) {
    	SpellAbility spDebuffAll = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = 399707924254248213L;

			@Override
    		public boolean canPlayAI() {
    			return debuffAllCanPlayAI(af, this);
    		}
    		
			@Override
			public String getStackDescription() {
				return debuffAllStackDescription(af, this);
			}

			@Override
    		public void resolve() {
    			debuffAllResolve(af, this);
    		}
    	};//SpellAbility

    	return spDebuffAll;
    }
    
    public static SpellAbility createDrawbackDebuffAll(final AbilityFactory af) {
        SpellAbility dbDebuffAll = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
			private static final long serialVersionUID = 3262199296469706708L;

			@Override
			public String getStackDescription() {
				return debuffAllStackDescription(af, this);
			}
            
			@Override
            public void resolve() {
            	debuffAllResolve(af, this);
            }

			@Override
			public boolean chkAI_Drawback() {
				return debuffAllChkDrawbackAI(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return debuffAllTriggerAI(af, this, mandatory);
			}
        };//SpellAbility
        
        return dbDebuffAll;
	}
    
    private static boolean debuffAllCanPlayAI(final AbilityFactory af, SpellAbility sa) {
    	String valid = "";
		Random r = MyRandom.random;
		final Card source = sa.getSourceCard();
		Card hostCard = af.getHostCard();
		HashMap<String,String> params = af.getMapParams();
		
		boolean chance = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed()); //to prevent runaway activations 
    	
    	if(params.containsKey("ValidCards")) {
			valid = params.get("ValidCards");
    	}
    	
    	CardList comp = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
    	comp = comp.getValidCards(valid, hostCard.getController(), hostCard);
    	CardList human = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
    	human = human.getValidCards(valid,hostCard.getController(), hostCard);
    	
    	//only count creatures that can attack
    	human = human.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				return CombatUtil.canAttack(c) && !af.isCurse();
			}
		});
    	
    	if(af.isCurse()) {
    		//evaluate both lists and pass only if human creatures are more valuable
    		if(CardFactoryUtil.evaluateCreatureList(comp) + 200 >= CardFactoryUtil.evaluateCreatureList(human))
    			return false;

    		return chance;
    	}//end Curse
    	
    	//don't use non curse DebuffAll after Combat_Begin until AI is improved
    	if(AllZone.Phase.isAfter(Constant.Phase.Combat_Begin))
    		return false;
    	
    	if (comp.size() > human.size())
    		return false;
    	
    	return (r.nextFloat() < .6667) && chance;
    }//pumpAllCanPlayAI()
    
    private static void debuffAllResolve(AbilityFactory af, SpellAbility sa) {
    	HashMap<String,String> params = af.getMapParams();
    	Card hostCard = af.getHostCard();
    	ArrayList<String> kws = getKeywords(params);
    	String valid = "";

		if(params.containsKey("ValidCards")) 
			valid = params.get("ValidCards");
    	
    	CardList list = AllZoneUtil.getCardsInPlay();
		list = list.getValidCards(valid.split(","), hostCard.getController(), hostCard);

		for(final Card tgtC : list){
			final ArrayList<String> hadIntrinsic = new ArrayList<String>();
			if(AllZoneUtil.isCardInPlay(tgtC) && CardFactoryUtil.canTarget(hostCard, tgtC)) {
				for(String kw : kws) {
					if(tgtC.getIntrinsicKeyword().contains(kw)) hadIntrinsic.add(kw);
					tgtC.removeIntrinsicKeyword(kw);
					tgtC.removeExtrinsicKeyword(kw);
				}
			}
			if(!params.containsKey("Permanent")) {
				AllZone.EndOfTurn.addUntil(new Command() {
					private static final long serialVersionUID = 7486231071095628674L;

					public void execute() {
						if(AllZoneUtil.isCardInPlay(tgtC)){
							for(String kw : hadIntrinsic) {
								tgtC.addIntrinsicKeyword(kw);
							}
						}
					}
				});
			}
		}
    }//pumpAllResolve()
    
    private static boolean debuffAllTriggerAI(AbilityFactory af, SpellAbility sa, boolean mandatory) {
		if (!ComputerUtil.canPayCost(sa))
			return false;
    	
    	return true;
    }

    private static boolean debuffAllChkDrawbackAI(AbilityFactory af, SpellAbility sa) {
    	return true;
    }
    
    private static String debuffAllStackDescription(AbilityFactory af, SpellAbility sa) {
    	HashMap<String,String> params = af.getMapParams();
    	StringBuilder sb = new StringBuilder();

    	String desc = "";
    	if(params.containsKey("SpellDescription")) {
    		desc = params.get("SpellDescription");
    	}
    	else if(params.containsKey("DebuffAllDescription")) {
    		desc = params.get("DebuffAllDescription");
    	}
    	
    	if (sa instanceof Ability_Sub)
    		sb.append(" ");
		else
			sb.append(sa.getSourceCard()).append(" - ");
    	
    	sb.append(desc);
    	
    	Ability_Sub abSub = sa.getSubAbility();
    	if (abSub != null) {
    		sb.append(abSub.getStackDescription());
    	}

    	return sb.toString();
    }//debuffAllStackDescription()

}//end class AbilityFactory_Debuff
