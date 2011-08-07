package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListUtil;
import forge.CombatUtil;
import forge.ComputerUtil;
import forge.Constant;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

public class AbilityFactory_PreventDamage {
	
	// Ex: A:SP$ PreventDamage | Cost$ W | Tgt$ TgtC | Amount$ 3 | SpellDescription$ Prevent the next 3 damage that would be dealt to target creature this turn.
	// http://www.slightlymagic.net/wiki/Forge_AbilityFactory#PreventDamage

	public static SpellAbility getAbilityPreventDamage(final AbilityFactory af) {

		final SpellAbility abRegenerate = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = -6581723619801399347L;

			@Override
			public boolean canPlayAI() {
				return preventDamageCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				preventDamageResolve(af, this);
				af.getHostCard().setAbilityUsed(af.getHostCard().getAbilityUsed() + 1);
			}
			
			@Override
			public String getStackDescription(){
				return preventDamageStackDescription(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return doPreventDamageTriggerAI(af, this, mandatory);
			}
			
		};//Ability_Activated

		return abRegenerate;
	}

	public static SpellAbility getSpellPreventDamage(final AbilityFactory af){

		final SpellAbility spRegenerate = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = -3899905398102316582L;

			@Override
			public boolean canPlayAI() {
				return preventDamageCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				preventDamageResolve(af, this);
			}
			
			@Override
			public String getStackDescription(){
				return preventDamageStackDescription(af, this);
			}
			
		}; // Spell

		return spRegenerate;
	}
	
	public static SpellAbility createDrawbackPreventDamage(final AbilityFactory af) {
		final SpellAbility dbRegen = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
			private static final long serialVersionUID = -2295483806708528744L;

			@Override
			public String getStackDescription(){
				return preventDamageStackDescription(af, this);
			}

			@Override
			public void resolve() {
				preventDamageResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return doPreventDamageTriggerAI(af, this, mandatory);
			}

		};
		return dbRegen;
	}
	
	private static String preventDamageStackDescription(AbilityFactory af, SpellAbility sa){
		final HashMap<String,String> params = af.getMapParams();
		StringBuilder sb = new StringBuilder();
		Card host = af.getHostCard();

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

			sb.append("Prevent the next ");
			sb.append(params.get("Amount"));
			sb.append(" that would be dealt to ");
			Iterator<Card> it = tgtCards.iterator();
			while(it.hasNext()) {
				Card tgtC = it.next();
				if(tgtC.isFaceDown()) sb.append("Morph");
				else sb.append(tgtC);
				
				if(it.hasNext()) sb.append(" ");
			}
		}
		sb.append(".");

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}

	private static boolean preventDamageCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
		final HashMap<String,String> params = af.getMapParams();
		final Card hostCard = af.getHostCard();
		boolean chance = false;
		
		// temporarily disabled until better AI
		if (af.getAbCost().getSacCost())	 return false;
		if (af.getAbCost().getSubCounter())  return false;
		if (af.getAbCost().getLifeCost())	 return false;

		if (!ComputerUtil.canPayCost(sa))
			return false;

		Target tgt = sa.getTarget();
		if (tgt == null){
			// As far as I can tell these Defined Cards will only have one of them
			ArrayList<Card> list = AbilityFactory.getDefinedCards(hostCard, params.get("Defined"), sa);
			
			if (AllZone.Stack.size() > 0){
			// check stack for something that will kill this
			}
			else{
				if (AllZone.Phase.is(Constant.Phase.Combat_Declare_Blockers_InstantAbility)){
					boolean flag = false;
					
					for(Card c : list){
							flag |= CombatUtil.combatantWouldBeDestroyed(c);
					}
					
					chance = flag;
				}
				else{	// if nothing on the stack, and it's not declare blockers. no need to regen
					return false;
				}
			}
		}
		else{
			tgt.resetTargets();
			// filter AIs battlefield by what I can target
			CardList targetables = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
			targetables = targetables.getValidCards(tgt.getValidTgts(), AllZone.ComputerPlayer, hostCard);
			
			if (targetables.size() == 0)
				return false;
			
			if (AllZone.Stack.size() > 0){
				// check stack for something on the stack will kill anything i control
			}
			else{
				if (AllZone.Phase.is(Constant.Phase.Combat_Declare_Blockers_InstantAbility)){
					CardList combatants = targetables.getType("Creature");
					CardListUtil.sortByEvaluateCreature(combatants);

					for(Card c : combatants){
						if (CombatUtil.combatantWouldBeDestroyed(c)){
							tgt.addTarget(c);
							chance = true;
							break;
						}
					}
				}
			}
		}
		
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();
		
		return chance;
	}

	private static boolean doPreventDamageTriggerAI(final AbilityFactory af, final SpellAbility sa, boolean mandatory) {
		boolean chance = false;

		if (!ComputerUtil.canPayCost(sa))
			return false;

		Target tgt = sa.getTarget();
		if (tgt == null){
			// If there's no target on the trigger, just say yes.
			chance = true;
		}
		else{
			chance = preventDamageMandatoryTarget(af, sa, mandatory);
		}
		
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.doTrigger(mandatory);
		
		return chance;
	}
	
	private static boolean preventDamageMandatoryTarget(AbilityFactory af, SpellAbility sa, boolean mandatory){
		final Card hostCard = af.getHostCard();
		Target tgt = sa.getTarget();
		tgt.resetTargets();
		// filter AIs battlefield by what I can target
		CardList targetables = AllZoneUtil.getCardsInPlay();
		targetables = targetables.getValidCards(tgt.getValidTgts(), AllZone.ComputerPlayer, hostCard);
		CardList compTargetables = targetables.getController(AllZone.ComputerPlayer);
		
		if (targetables.size() == 0)
			return false;
		
		if (!mandatory && compTargetables.size() == 0)
			return false;
		
		if (compTargetables.size() > 0){
			CardList combatants = compTargetables.getType("Creature");
			CardListUtil.sortByEvaluateCreature(combatants);
			if (AllZone.Phase.is(Constant.Phase.Combat_Declare_Blockers_InstantAbility)){
				for(Card c : combatants){
					if (CombatUtil.combatantWouldBeDestroyed(c)){
						tgt.addTarget(c);
						return true;
					}
				}
			}
	
			// TODO see if something on the stack is about to kill something i can target
			
			tgt.addTarget(combatants.get(0));
			return true;
		}

		tgt.addTarget(CardFactoryUtil.AI_getCheapestPermanent(targetables, hostCard, true));
		return true;
	}
	
	private static void preventDamageResolve(final AbilityFactory af, final SpellAbility sa) {
		Card hostCard = af.getHostCard();
		final HashMap<String,String> params = af.getMapParams();
		int numDam = AbilityFactory.calculateAmount(af.getHostCard(), params.get("Amount"), sa);
		
		ArrayList<Card> tgtCards;
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else
			tgtCards = AbilityFactory.getDefinedCards(hostCard, params.get("Defined"), sa);
		
		for(final Card tgtC : tgtCards){
			
			if (AllZoneUtil.isCardInPlay(tgtC) && (tgt == null || CardFactoryUtil.canTarget(hostCard, tgtC))){
				tgtC.addPreventNextDamage(numDam);
			}
		}
		
		if (af.hasSubAbility()) {
			Ability_Sub abSub = sa.getSubAbility();
			if(abSub != null) {
			   abSub.resolve();
			}
		}
	}//doResolve
}
