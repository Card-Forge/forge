package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Arrays;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardUtil;
import forge.Command;
import forge.ComputerUtil;
import forge.Constant;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;

public class AbilityFactory_Animate {

	//**************************************************************
	// *************************** Animate *************************
	//**************************************************************

	public static SpellAbility createAbilityAnimate(final AbilityFactory af) {
		final SpellAbility abAnimate = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = 1938171749867735155L;

			public boolean canPlayAI() {
				return animateCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				animateResolve(af, this);
			}

			public String getStackDescription() {
				return animateStackDescription(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return animateTriggerAI(af, this, mandatory);
			}
		};
		return abAnimate;
	}

	public static SpellAbility createSpellAnimate(final AbilityFactory af) {
		final SpellAbility spAnimate = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = -4047747186919390147L;

			public boolean canPlayAI() {
				return animateCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				animateResolve(af, this);
			}

			public String getStackDescription() {
				return animateStackDescription(af, this);
			}
		};
		return spAnimate;
	}

	public static SpellAbility createDrawbackAnimate(final AbilityFactory af) {
		final SpellAbility dbAnimate = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
			private static final long serialVersionUID = -8659938411460952874L;

			@Override
			public void resolve() {
				animateResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return animatePlayDrawbackAI(af, this);
			}

			public String getStackDescription() {
				return animateStackDescription(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return animateTriggerAI(af, this, mandatory);
			}
		};
		return dbAnimate;
	}

	private static String animateStackDescription(final AbilityFactory af, SpellAbility sa) {
		HashMap<String,String> params = af.getMapParams();
		Card host = af.getHostCard();
		Hashtable<String,String> svars = host.getSVars();

		int power = -1;
		if(params.containsKey("Power")) power = AbilityFactory.calculateAmount(host, params.get("Power"), sa);
		int toughness = -1;
		if(params.containsKey("Toughness")) toughness = AbilityFactory.calculateAmount(host, params.get("Toughness"), sa);
		
		boolean permanent = params.containsKey("Permanent") ? true : false;
		final ArrayList<String> types = new ArrayList<String>();
		if(params.containsKey("Types")) types.addAll(Arrays.asList(params.get("Types").split(",")));
		final ArrayList<String> keywords = new ArrayList<String>();
		if(params.containsKey("Keywords")) keywords.addAll(Arrays.asList(params.get("Keywords").split(" & ")));
		//allow SVar substitution for keywords
		for(int i = 0; i < keywords.size(); i++) {
			String k = keywords.get(i);
			if(svars.containsKey(k)) {
				keywords.add("\""+k+"\"");
				keywords.remove(k);
			}
		}
		ArrayList<String> colors = new ArrayList<String>();
		if(params.containsKey("Colors")) colors.addAll(Arrays.asList(params.get("Colors").split(",")));

		StringBuilder sb = new StringBuilder();

		if (sa instanceof Ability_Sub)
			sb.append(" ");
		else
			sb.append(sa.getSourceCard().getName()).append(" - ");

		Target tgt = af.getAbTgt();
		ArrayList<Card> tgts;
		if (tgt != null)
			tgts = tgt.getTargetCards();
		else
			tgts = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);

		for(Card c : tgts) {
			sb.append(c).append(" ");
		}
		sb.append("become");
		if(tgts.size() == 1) sb.append("s a");
		//if power is -1, we'll assume it's not just setting toughness
		if(power != -1) sb.append(" ").append(power).append("/").append(toughness);
		
		if(colors.size() > 0) sb.append(" ");
		for(int i = 0; i < colors.size(); i++) {
			sb.append(colors.get(i));
			if(i < (colors.size() - 1)) sb.append(" and ");
		}
		sb.append(" ");
		for(int i = types.size() - 1; i >= 0; i--) {
			sb.append(types.get(i));
			sb.append(" ");
		}
		if(keywords.size() > 0) sb.append("with ");
		for(int i = 0; i < keywords.size(); i++) {
			sb.append(keywords.get(i));
			if(i < (keywords.size() - 1)) sb.append(" and ");
		}
		//sb.append(abilities)
		//sb.append(triggers)
		if(!permanent) {
			if(params.containsKey("UntilEndOfCombat")) sb.append(" until end of combat.");
			else sb.append(" until end of turn.");
		}
		else sb.append(".");

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null)
			sb.append(abSub.getStackDescription());

		return sb.toString();
	}

	private static boolean animateCanPlayAI(final AbilityFactory af, SpellAbility sa) {
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		HashMap<String,String> params = af.getMapParams();
		Target tgt = af.getAbTgt();
		Card source = sa.getSourceCard();
		
		boolean useAbility = true;
		
		//TODO - add some kind of check to answer "Am I going to attack with this?"
		//TODO - add some kind of check for during human turn to answer "Can I use this to block something?"
		
		//don't activate during main2 unless this effect is permanent
		if(AllZone.Phase.getPhase().equals(Constant.Phase.Main2) && !params.containsKey("Permanent"))
        	return false;
		
		if(null == tgt) {
			ArrayList<Card> defined = AbilityFactory.getDefinedCards(source, params.get("Defined"), sa);
			
			boolean bFlag = false;
			for(Card c : defined) {
				bFlag |= (!c.isCreature() && !c.isTapped());
			}
			
			if (!bFlag)	// All of the defined stuff is animated, not very useful
				return false;
		}
		else{
			tgt.resetTargets();
			useAbility &= animateTgtAI(af, sa);
		}
		
        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	useAbility &= subAb.chkAI_Drawback();
		
		return useAbility;
	}// end animateCanPlayAI()

	private static boolean animatePlayDrawbackAI(final AbilityFactory af, SpellAbility sa) {
		// AI should only activate this during Human's turn
		boolean chance = animateTgtAI(af, sa);

		// TODO: restrict the subAbility a bit

		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();

		return chance;
	}

	private static boolean animateTriggerAI(final AbilityFactory af, SpellAbility sa, boolean mandatory) {
		if(!ComputerUtil.canPayCost(sa))	// If there is a cost payment
			return false;

		boolean chance = animateTgtAI(af, sa);

		// Improve AI for triggers. If source is a creature with:
		// When ETB, sacrifice a creature. Check to see if the AI has something to sacrifice

		// Eventually, we can call the trigger of ETB abilities with not mandatory as part of the checks to cast something


		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();

		return chance || mandatory;
	}

	private static boolean animateTgtAI(AbilityFactory af, SpellAbility sa) {
		//This is reasonable for now.  Kamahl, Fist of Krosa and a sorcery or two are the only things
		//that animate a target.  Those can just use SVar:RemAIDeck:True until this can do a reasonalbly
		//good job of picking a good target
		return false;
	}

	private static void animateResolve(final AbilityFactory af, final SpellAbility sa) {
		HashMap<String,String> params = af.getMapParams();
		Card source = sa.getSourceCard();
		Card host = af.getHostCard();
		Hashtable<String,String> svars = host.getSVars();

		//AF specific params
		int power = -1;
		if(params.containsKey("Power")) power = AbilityFactory.calculateAmount(host, params.get("Power"), sa);
		int toughness = -1;
		if(params.containsKey("Toughness")) toughness = AbilityFactory.calculateAmount(host, params.get("Toughness"), sa);
		
		boolean permanent = params.containsKey("Permanent") ? true : false;
		
		final ArrayList<String> types = new ArrayList<String>();
		if(params.containsKey("Types")) types.addAll(Arrays.asList(params.get("Types").split(",")));
		
		//allow ChosenType - overrides anything else specified
		if(types.contains("ChosenType")) {
			types.clear();
			types.add(host.getChosenType());
		}
		
		final ArrayList<String> keywords = new ArrayList<String>();
		if(params.containsKey("Keywords")) keywords.addAll(Arrays.asList(params.get("Keywords").split(" & ")));
		//allow SVar substitution for keywords
		for(int i = 0; i < keywords.size(); i++) {
			String k = keywords.get(i);
			if(svars.containsKey(k)) {
				keywords.add(svars.get(k));
				keywords.remove(k);
			}
		}
		
		//colors to be added or changed to
		final String finalDesc = params.containsKey("Colors") ? 
			CardUtil.getShortColorsString(new ArrayList<String>(Arrays.asList(params.get("Colors").split(",")))) : "";
		
		//abilities to add to the animated being
		ArrayList<String> abilities = new ArrayList<String>();
		if(params.containsKey("Abilities")) abilities.addAll(Arrays.asList(params.get("Abilities").split(",")));
		
		//triggers to add to the animated being
		ArrayList<String> triggers = new ArrayList<String>();
		if(params.containsKey("Triggers")) triggers.addAll(Arrays.asList(params.get("Triggers").split(",")));

		Target tgt = af.getAbTgt();
		ArrayList<Card> tgts;
		if (tgt != null)
			tgts = tgt.getTargetCards();
		else
			tgts = AbilityFactory.getDefinedCards(source, params.get("Defined"), sa);
		
		for(final Card c : tgts){
			//final ArrayList<Card_Color> originalColors = c.getColor();
			final ArrayList<String> originalTypes = c.getType();
			final int origPower = c.getBaseAttack();
			final int origToughness = c.getBaseDefense();
			
			final long timestamp = doAnimate(c, af, power, toughness, types, finalDesc, keywords);
			
			//give abilities
			final ArrayList<SpellAbility> addedAbilities= new ArrayList<SpellAbility>();
			if(abilities.size() > 0){
				for(String s : abilities) {
					AbilityFactory newAF = new AbilityFactory();
					String actualAbility = host.getSVar(s);
					SpellAbility grantedAbility = newAF.getAbility(actualAbility, c);
					addedAbilities.add(grantedAbility);
					c.addSpellAbility(grantedAbility);
				}
			}
			
			//Grant triggers
			final ArrayList<Trigger> addedTriggers = new ArrayList<Trigger>();
			if(triggers.size() > 0) {
				for(String s : triggers) {
					String actualTrigger = host.getSVar(s);
					Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, c);
					addedTriggers.add(c.addTrigger(parsedTrigger));
					AllZone.TriggerHandler.registerTrigger(parsedTrigger);
				}
			}

			final Command unanimate = new Command() {
				private static final long serialVersionUID = -5861759814760561373L;

				public void execute() {
					doUnanimate(c, origPower, origToughness, originalTypes, finalDesc, keywords, addedAbilities, addedTriggers, timestamp);
				}
			};

			if(!permanent) {
				if(params.containsKey("UntilEndOfCombat")) AllZone.EndOfCombat.addUntil(unanimate);
				else AllZone.EndOfTurn.addUntil(unanimate);
			}
		}
	}//animateResolve

	private static long doAnimate(Card c, AbilityFactory af, int power, int toughness, ArrayList<String> types, String colors, ArrayList<String> keywords) {
		HashMap<String,String> params = af.getMapParams();
		if (power != -1) c.setBaseAttack(power);
		if (toughness != -1) c.setBaseDefense(toughness);

		ArrayList<String> supertypes = new ArrayList<String>();
		if(params.containsKey("KeepSupertypes")) {
			for(String t : c.getType()) {
				if(CardUtil.isASuperType(t)) supertypes.add(t);
			}
		}
		ArrayList<String> cardtypes = new ArrayList<String>();
		if (params.containsKey("KeepCardTypes")) {
			for (String t : c.getType()) {
				if (CardUtil.isACardType(t)) cardtypes.add(t);
			}
		}
		if (params.containsKey("OverwriteTypes")) c.clearAllTypes();
		types.addAll(supertypes);
		types.addAll(cardtypes);
		for (String r : types) {
			// if the card doesn't have that type, add it
			if (!c.isType(r))
				c.addType(r);
		}
		for (String k : keywords) {
        	if (k.startsWith("HIDDEN"))
        		c.addExtrinsicKeyword(k);
			//this maybe should just blindly add since multiple instances of a keyword sometimes have effects
			//practically, this shouldn't matter though, and will display more cleanly
        	else if (!c.getIntrinsicKeyword().contains(k) || CardUtil.isStackingKeyword(k))
				c.addIntrinsicKeyword(k);	
		}

		long timestamp = c.addColor(colors, c, !params.containsKey("OverwriteColors"), true);		
		return timestamp;
	}

	private static void doUnanimate(Card c, int originalPower, int originalToughness, ArrayList<String> originalTypes, String colorDesc, ArrayList<String> originalKeywords, ArrayList<SpellAbility> addedAbilities, ArrayList<Trigger> addedTriggers, long timestamp) {
		c.setBaseAttack(originalPower);
		c.setBaseDefense(originalToughness);

		c.clearAllTypes();
		for(String type : originalTypes) {
			c.addType(type);
		}

		//TODO - this will have to handle adding back original colors
		c.removeColor(colorDesc, c, false, timestamp);

		for(String k : originalKeywords) {
        	if(k.startsWith("HIDDEN"))
        		c.removeExtrinsicKeyword(k);
			//TODO - may want to look at saving off intrinsic and extrinsic separately and add back that way
			c.removeIntrinsicKeyword(k);
		}
		
		for(SpellAbility sa : addedAbilities) {
			c.removeSpellAbility(sa);
		}
		
		for(Trigger t : addedTriggers) {
			AllZone.TriggerHandler.removeRegisteredTrigger(t);
			c.removeTrigger(t);
		}

		//any other unanimate cleanup
		if(!c.isCreature()) c.unEquipAllCards();
	}
	
	//**************************************************************
	// ************************ AnimateAll *************************
	//**************************************************************

	public static SpellAbility createAbilityAnimateAll(final AbilityFactory af) {
		final SpellAbility abAnimateAll = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = -4969632476557290609L;

			public boolean canPlayAI() {
				return animateAllCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				animateAllResolve(af, this);
			}

			public String getStackDescription() {
				return animateAllStackDescription(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return animateAllTriggerAI(af, this, mandatory);
			}
		};
		return abAnimateAll;
	}

	public static SpellAbility createSpellAnimateAll(final AbilityFactory af) {
		final SpellAbility spAnimateAll = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = 2946847609068706237L;

			public boolean canPlayAI() {
				return animateAllCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				animateAllResolve(af, this);
			}

			public String getStackDescription() {
				return animateAllStackDescription(af, this);
			}
		};
		return spAnimateAll;
	}

	public static SpellAbility createDrawbackAnimateAll(final AbilityFactory af) {
		final SpellAbility dbAnimateAll = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
			private static final long serialVersionUID = 2056843302051205632L;

			@Override
			public void resolve() {
				animateAllResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return animateAllPlayDrawbackAI(af, this);
			}

			public String getStackDescription() {
				return animateAllStackDescription(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return animateAllTriggerAI(af, this, mandatory);
			}
		};
		return dbAnimateAll;
	}

	private static String animateAllStackDescription(final AbilityFactory af, SpellAbility sa) {
		HashMap<String,String> params = af.getMapParams();

		StringBuilder sb = new StringBuilder();

		if (sa instanceof Ability_Sub)
			sb.append(" ");
		else
			sb.append(sa.getSourceCard()).append(" - ");

		String desc = "";
		if(params.containsKey("SpellDescription")) {
			desc = params.get("SpellDescription");
		}
		else {
			desc = "Animate all valid cards.";
		}

		sb.append(desc);

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null)
			sb.append(abSub.getStackDescription());

		return sb.toString();
	}

	private static boolean animateAllCanPlayAI(final AbilityFactory af, SpellAbility sa) {
		boolean useAbility = false;
		
        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	useAbility &= subAb.chkAI_Drawback();
		
		return useAbility;
	}// end animateCanPlayAI()

	private static boolean animateAllPlayDrawbackAI(final AbilityFactory af, SpellAbility sa) {
		boolean chance = false;
		
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();

		return chance;
	}

	private static boolean animateAllTriggerAI(final AbilityFactory af, SpellAbility sa, boolean mandatory) {
		if(!ComputerUtil.canPayCost(sa))	// If there is a cost payment
			return false;

		boolean chance = false;

		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();

		return chance || mandatory;
	}

	private static void animateAllResolve(final AbilityFactory af, final SpellAbility sa) {
		HashMap<String,String> params = af.getMapParams();
		Card host = af.getHostCard();
		Hashtable<String,String> svars = host.getSVars();

		//AF specific params
		int power = -1;
		if(params.containsKey("Power")) power = AbilityFactory.calculateAmount(host, params.get("Power"), sa);
		int toughness = -1;
		if(params.containsKey("Toughness")) toughness = AbilityFactory.calculateAmount(host, params.get("Toughness"), sa);
		
		boolean permanent = params.containsKey("Permanent") ? true : false;
		
		final ArrayList<String> types = new ArrayList<String>();
		if(params.containsKey("Types")) types.addAll(Arrays.asList(params.get("Types").split(",")));
		
		//allow ChosenType - overrides anything else specified
		if(types.contains("ChosenType")) {
			types.clear();
			types.add(host.getChosenType());
		}
		
		final ArrayList<String> keywords = new ArrayList<String>();
		if(params.containsKey("Keywords")) keywords.addAll(Arrays.asList(params.get("Keywords").split(" & ")));
		//allow SVar substitution for keywords
		for(int i = 0; i < keywords.size(); i++) {
			String k = keywords.get(i);
			if(svars.containsKey(k)) {
				keywords.add(svars.get(k));
				keywords.remove(k);
			}
		}
		
		//colors to be added or changed to
		final String finalDesc = params.containsKey("Colors") ? 
			CardUtil.getShortColorsString(new ArrayList<String>(Arrays.asList(params.get("Colors").split(",")))) : "";
		
		//abilities to add to the animated being
		ArrayList<String> abilities = new ArrayList<String>();
		if(params.containsKey("Abilities")) abilities.addAll(Arrays.asList(params.get("Abilities").split(",")));
		
		//triggers to add to the animated being
		ArrayList<String> triggers = new ArrayList<String>();
		if(params.containsKey("Triggers")) triggers.addAll(Arrays.asList(params.get("Triggers").split(",")));

		String valid = "";

		if(params.containsKey("ValidCards")) 
			valid = params.get("ValidCards");
    	
    	CardList list = AllZoneUtil.getCardsInPlay();
		list = list.getValidCards(valid.split(","), host.getController(), host);
		
		for(final Card c : list){
			final ArrayList<String> originalTypes = c.getType();
			final int origPower = c.getBaseAttack();
			final int origToughness = c.getBaseDefense();
			
			final long timestamp = doAnimate(c, af, power, toughness, types, finalDesc, keywords);
			
			//give abilities
			final ArrayList<SpellAbility> addedAbilities= new ArrayList<SpellAbility>();
			if(abilities.size() > 0){
				for(String s : abilities) {
					AbilityFactory newAF = new AbilityFactory();
					String actualAbility = host.getSVar(s);
					SpellAbility grantedAbility = newAF.getAbility(actualAbility, c);
					addedAbilities.add(grantedAbility);
					c.addSpellAbility(grantedAbility);
				}
			}
			
			//Grant triggers
			final ArrayList<Trigger> addedTriggers = new ArrayList<Trigger>();
			if(triggers.size() > 0) {
				for(String s : triggers) {
					String actualTrigger = host.getSVar(s);
					Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, c);
					addedTriggers.add(c.addTrigger(parsedTrigger));
					AllZone.TriggerHandler.registerTrigger(parsedTrigger);
				}
			}

			final Command unanimate = new Command() {
				private static final long serialVersionUID = -5861759814760561373L;

				public void execute() {
					doUnanimate(c, origPower, origToughness, originalTypes, finalDesc, keywords, addedAbilities, addedTriggers, timestamp);
				}
			};

			if(!permanent) {
				if(params.containsKey("UntilEndOfCombat")) AllZone.EndOfCombat.addUntil(unanimate);
				else AllZone.EndOfTurn.addUntil(unanimate);
			}
		}
	}//animateResolve

}//end class AbilityFactory_Animate